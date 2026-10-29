import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, test } from '@playwright/test';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../../..');
const backend = process.env.MAMXANX_E2E_BACKEND_URL ?? 'http://localhost:8080/api/v1';
const project = process.env.MAMXANX_E2E_COMPOSE_PROJECT ?? 'mamxanh-dev';
const quote = (value: string) => `N'${value.replaceAll("'", "''")}'`;
test.use({ trace: 'off' }); // Real JWTs must not be saved in traces.
function compose(args: string[], timeout = 180_000) {
  const envFile = path.join(root, 'app/mamxanh-backend/.env');
  return execFileSync('docker', ['compose', '-p', project,
    ...(existsSync(envFile) ? ['--env-file', envFile] : []), '-f', path.join(root, 'docker-compose.yml'), ...args],
    { cwd: root, encoding: 'utf8', timeout }).trim();
}
function sql(query: string) {
  return compose(['exec','-T','sqlserver','/opt/mssql-tools18/bin/sqlcmd',
    '-S','sqlserver','-U','sa','-C','-d','MamXanhDB','-h','-1','-W','-w','65535','-b','-Q',
    `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`], 30_000);
}

test('FR11 real Admin JWT reports SQL telemetry, Member is denied and scheduled retention removes old data', async ({ page, request }, testInfo) => {
  test.setTimeout(180_000);
  const id = randomUUID();
  const email = `fr11-${id}@example.invalid`;
  const password = `Aa1!${randomUUID()}z`;
  let member = 0;
  try {
    const registration = await request.post(`${backend}/auth/register`, { data: {
      displayName: 'FR11 kiểm thử', email, password, confirmPassword: password,
    } });
    expect(registration.status()).toBe(201);
    sql(`UPDATE [USER] SET email_verified=1,onboarding_invited_at=SYSUTCDATETIME() WHERE email=${quote(email)}`);
    member = Number(sql(`SELECT user_id FROM [USER] WHERE email=${quote(email)}`));
    const login = async () => {
      const response = await request.post(`${backend}/auth/login`, { data: { email, password } });
      expect(response.status()).toBe(200);
      return response.json();
    };
    const customer = await login();
    const date = new Intl.DateTimeFormat('en-CA', { timeZone:'Asia/Ho_Chi_Minh',year:'numeric',month:'2-digit',day:'2-digit' }).format(new Date());
    const url = `${backend}/admin/ai-telemetry?from=${date}&to=${date}&feature=FR-51`;
    expect((await request.get(url)).status()).toBe(401);
    expect((await request.get(url, { headers: { Authorization: `Bearer ${customer.accessToken}` } })).status()).toBe(403);
    sql(`UPDATE [USER] SET role='ADMIN' WHERE user_id=${member};
      INSERT INTO AI_USAGE_TELEMETRY(event_id,user_id,feature,model,service_tier,occurred_at,prompt_tokens,response_tokens,total_tokens,thinking_tokens,cached_tokens,tool_use_prompt_tokens,usage_status,pricing_version,estimated_cost_usd)
      VALUES (${quote(id)},${member},'FR-51','gemini-3.8-flash','STANDARD',SYSUTCDATETIME(),150,300,450,0,0,0,'COMPLETE','gemini-standard-2026-10-23',0.0012375);
      INSERT INTO AI_USAGE_TELEMETRY(event_id,user_id,feature,model,service_tier,occurred_at,prompt_tokens,response_tokens,total_tokens,usage_status)
      VALUES (${quote(randomUUID())},${member},'FR-51','gemini-3.8-flash','STANDARD',DATEADD(day,-91,SYSUTCDATETIME()),0,0,0,'MISSING')`);
    const admin = await login();
    const headers = { Authorization: `Bearer ${admin.accessToken}` };
    const response = await request.get(url, { headers });
    expect(response.status()).toBe(200);
    const report = await response.json();
    expect(report.totals.promptTokens).toBeGreaterThanOrEqual(150);
    expect(report.totals.totalTokens).toBeGreaterThanOrEqual(450);
    expect(report).not.toHaveProperty('userId');
    await page.addInitScript(session => sessionStorage.setItem('mamxanh.auth',JSON.stringify(session)), {
      accessToken: admin.accessToken,expiresAt: Date.now()+admin.expiresInSeconds*1000,account: admin.account,
    });
    await page.goto('/admin/ai-telemetry');
    await expect(page.getByRole('heading', { name: 'Mức sử dụng AI' })).toBeVisible({ timeout: 30_000 });
    await expect(page.getByRole('table')).toContainText('Chatbot', { timeout: 15_000 });
    await page.screenshot({ path: testInfo.outputPath('fr11-admin-report.png'),fullPage:true });
    await page.setViewportSize({ width:360,height:800 });
    await expect.poll(() => page.getByRole('navigation', { name:'Điều hướng quản trị' })
      .evaluate(element => element.getBoundingClientRect().right)).toBeLessThanOrEqual(0);
    await expect(page.getByRole('region', { name:'Bảng thống kê token AI' })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.screenshot({ path: testInfo.outputPath('fr11-admin-report-mobile.png'),fullPage:true });
    await expect.poll(() => Number(sql(`SELECT COUNT_BIG(*) FROM AI_USAGE_TELEMETRY WHERE user_id=${member} AND occurred_at<DATEADD(day,-90,SYSUTCDATETIME())`)),
      { timeout:75_000,intervals:[2000] }).toBe(0);
    expect(Number(sql(`SELECT COUNT_BIG(*) FROM AI_USAGE_TELEMETRY WHERE event_id=${quote(id)}`))).toBe(1);
    sql(`UPDATE [USER] SET account_status='LOCKED' WHERE user_id=${member}`);
    const locked = await request.get(url, { headers });
    expect(locked.status()).toBe(403);
    expect((await locked.json()).code).toBe('ACCOUNT_LOCKED');
  } finally {
    sql(`DELETE FROM AI_USAGE_TELEMETRY WHERE user_id IN (SELECT user_id FROM [USER] WHERE email=${quote(email)});
      DELETE FROM [USER] WHERE email=${quote(email)}`);
  }
});

test('FR11 live Gemini accounting survives a SQL outage and forced Backend restart on the Compose volume', async ({ request }) => {
  test.skip(process.env.MAMXANH_E2E_LIVE_GEMINI !== '1', 'Opt-in live provider and local Compose interruption test.');
  test.skip(!['localhost', '127.0.0.1'].includes(new URL(backend).hostname), 'Interruption test requires a local Compose stack.');
  test.setTimeout(360_000);
  const anonymous = randomUUID();
  const email = `fr11-live-${randomUUID()}@example.invalid`;
  const password = `Aa1!${randomUUID()}z`;
  try {
    const registration = await request.post(`${backend}/auth/register`, { data: {
      displayName: 'FR11 live check', email, password, confirmPassword: password,
    } });
    expect(registration.status()).toBe(201);
    sql(`UPDATE [USER] SET email_verified=1,role='ADMIN',onboarding_invited_at=SYSUTCDATETIME() WHERE email=${quote(email)}`);
    const login = await request.post(`${backend}/auth/login`, { data: { email, password } });
    expect(login.status()).toBe(200);
    const session = await login.json();

    compose(['stop', 'sqlserver']);
    const response = await request.post(`${backend}/recipes/ai/chat`, { data: {
      contextType: 'GENERAL', question: 'Đậu hũ là gì? Giải thích ngắn bằng tiếng Việt.',
      history: [], anonymousViewerId: anonymous,
    }, timeout: 20_000 });
    expect(response.status()).toBe(200);
    expect((await response.json()).answer).toBeTruthy();
    // SQL is offline: the acknowledged response must already have a durable metadata file.
    const pending = compose(['exec', '-T', 'backend', 'sh', '-c',
      `grep -l '${anonymous}' /var/lib/mamxanh/ai-telemetry/*.json`]);
    expect(pending).toContain('.json');
    compose(['kill', '--signal', 'SIGKILL', 'backend']);
    compose(['up', '-d', '--wait', '--wait-timeout', '150']);

    await expect.poll(() => Number(sql(`SELECT COUNT_BIG(*) FROM AI_USAGE_TELEMETRY WHERE anonymous_session_id=${quote(anonymous)}`)),
      { timeout: 20_000, intervals: [1000] }).toBe(1);
    const rows = JSON.parse(sql(`SELECT prompt_tokens, response_tokens, total_tokens, thinking_tokens,
      cached_tokens, tool_use_prompt_tokens, usage_status, estimated_cost_usd, model
      FROM AI_USAGE_TELEMETRY WHERE anonymous_session_id=${quote(anonymous)} FOR JSON PATH`));
    const row = rows[0];
    expect(row.model).toBe('gemini-3.8-flash');
    expect(row.usage_status).toBe('COMPLETE');
    expect(row.prompt_tokens).toBeGreaterThan(0);
    expect(row.response_tokens).toBeGreaterThan(0);
    expect(row.total_tokens).toBe(row.prompt_tokens + row.response_tokens + row.thinking_tokens + row.tool_use_prompt_tokens);
    expect(row.estimated_cost_usd).toBeGreaterThan(0);
    const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
    const report = await request.get(`${backend}/admin/ai-telemetry?from=${today}&to=${today}&feature=FR-51`,
      { headers: { Authorization: `Bearer ${session.accessToken}` } });
    expect(report.status()).toBe(200);
    expect((await report.json()).totals.totalTokens).toBeGreaterThanOrEqual(row.total_tokens);
  } finally {
    compose(['up', '-d', '--wait', '--wait-timeout', '150']);
    sql(`DELETE FROM AI_USAGE_TELEMETRY WHERE anonymous_session_id=${quote(anonymous)};
      DELETE FROM [USER] WHERE email=${quote(email)}`);
  }
});
