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
test.use({ trace: 'off' }); // Real session tokens must never be persisted in a trace.

function sql(query: string) {
  const envFile = path.join(root, 'app/mamxanh-backend/.env');
  return execFileSync('docker', ['compose', '-p', project, ...(existsSync(envFile) ? ['--env-file', envFile] : []),
    '-f', path.join(root, 'docker-compose.yml'), 'exec', '-T', 'sqlserver', '/opt/mssql-tools18/bin/sqlcmd',
    '-S', 'sqlserver', '-U', 'sa', '-C', '-d', 'MamXanhDB', '-h', '-1', '-W', '-b', '-Q',
    `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`], { cwd: root, encoding: 'utf8', timeout: 30_000 }).trim();
}

test('FR-10: real JWT and SQL subscription changes select the correct upgrade and preserve the account', async ({ page, request }, testInfo) => {
  test.setTimeout(180_000);
  const email = `fr10-${randomUUID()}@example.invalid`;
  const password = `Aa1!${randomUUID()}z`;
  let member = 0;
  try {
    const registration = await request.post(`${backend}/auth/register`, {
      data: { displayName: 'FR10 kiểm thử', email, password, confirmPassword: password },
    });
    expect(registration.status()).toBe(201);
    sql(`UPDATE [USER] SET email_verified=1,role='EXPERT',onboarding_invited_at=SYSUTCDATETIME() WHERE email=${quote(email)}`);
    member = Number(sql(`SELECT user_id FROM [USER] WHERE email=${quote(email)}`));
    const login = await request.post(`${backend}/auth/login`, { data: { email, password } });
    expect(login.status()).toBe(200);
    const session = await login.json();
    const headers = { Authorization: `Bearer ${session.accessToken}` };
    const recipe = Number(sql("SELECT TOP 1 recipe_id FROM [RECIPE_POST] WHERE status='PUBLISHED' ORDER BY recipe_id"));
    expect(recipe).toBeGreaterThan(0);
    const source = await (await request.get(`${backend}/recipes/${recipe}`)).json();
    const ingredients = source.ingredients.filter((line: { ingredientId: number | null }) => line.ingredientId != null);
    expect(ingredients.length).toBeGreaterThan(0);
    const assistance = { contentType: 'DESCRIPTION', title: source.title, dishCategory: source.dishCategory,
      vegetarianType: source.vegetarianType, servings: source.servings,
      ingredients: ingredients.map((line: { ingredientId: number; unitId: number; quantity: number }) => ({
        ingredientId: line.ingredientId, unitId: line.unitId, quantity: line.quantity,
      })) };
    const denied = await request.post(`${backend}/recipes/ai-assistance`, { headers, data: assistance });
    expect(denied.status()).toBe(403);
    expect(await denied.json()).toMatchObject({ code: 'AI_SUBSCRIPTION_REQUIRED', requiredTier: 'PRO', upgradeUrl: '/goi-ai' });
    const chatMenu = () => request.post(`${backend}/recipes/ai/chat`, { headers,
      data: { contextType: 'GENERAL', question: 'Lập thực đơn tuần 7 ngày theo dinh dưỡng cá nhân', history: [] } });
    const freeChat = await chatMenu();
    expect(freeChat.status()).toBe(403);
    expect(await freeChat.json()).toMatchObject({ code: 'AI_SUBSCRIPTION_REQUIRED', requiredTier: 'PLUS', upgradeUrl: '/goi-ai' });
    // A future paid period must agree between the account API and the paid-feature guard.
    sql(`INSERT INTO [SUBSCRIPTION] (user_id,tier,status,starts_at,ends_at)
      VALUES (${member},'PRO','ACTIVE',DATEADD(day,1,SYSUTCDATETIME()),DATEADD(day,31,SYSUTCDATETIME()))`);
    const futurePlan = await request.get(`${backend}/subscriptions/my-subscription`, { headers });
    expect(futurePlan.status()).toBe(200);
    expect(await futurePlan.json()).toMatchObject({ tier: 'FREE' });
    const futureWeekly = await request.post(`${backend}/recipes/ai/weekly-menu`, { headers,
      data: { ingredientIds: [ingredients[0].ingredientId] } });
    expect(futureWeekly.status()).toBe(403);
    expect(await futureWeekly.json()).toMatchObject({ code: 'AI_SUBSCRIPTION_REQUIRED', requiredTier: 'PLUS' });
    expect(sql(`SELECT COUNT(*) FROM [SUBSCRIPTION] WHERE user_id=${member} AND tier='PRO' AND status='ACTIVE'`)).toBe('1');
    sql(`DELETE FROM [SUBSCRIPTION] WHERE user_id=${member} AND tier='PRO'`);
    sql(`INSERT INTO [SUBSCRIPTION] (user_id,tier,status,starts_at,ends_at)
      VALUES (${member},'PLUS','ACTIVE',DATEADD(day,-1,SYSUTCDATETIME()),DATEADD(day,1,SYSUTCDATETIME()))`);
    const plusRelated = await request.post(`${backend}/recipes/${recipe}/related/ai`, { headers });
    expect(plusRelated.status()).toBe(403);
    expect(await plusRelated.json()).toMatchObject({ code: 'AI_SUBSCRIPTION_REQUIRED', requiredTier: 'PRO' });
    const plusChat = await chatMenu();
    expect(plusChat.status()).toBe(200);
    expect(await plusChat.json()).toMatchObject({ workflowUrl: '/ke-hoach?view=for-you', recommendations: [] });
    sql(`UPDATE [SUBSCRIPTION] SET tier='PRO' WHERE user_id=${member}`);
    const proChat = await chatMenu();
    expect(proChat.status()).toBe(200);
    expect(await proChat.json()).toMatchObject({ workflowUrl: '/ke-hoach?view=for-you', recommendations: [] });
    sql(`UPDATE [SUBSCRIPTION] SET tier='PLUS' WHERE user_id=${member}`);
    await page.addInitScript((value) => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
      accessToken: value.accessToken, expiresAt: Date.now() + value.expiresInSeconds * 1000, account: value.account,
    })), session);
    await page.goto(`/cong-thuc/id/${recipe}`);
    // Revoke the paid period while the real JWT remains valid; the next request must recheck SQL.
    sql(`UPDATE [SUBSCRIPTION] SET ends_at=SYSUTCDATETIME() WHERE user_id=${member}`);
    const expiredChat = await chatMenu();
    expect(expiredChat.status()).toBe(403);
    expect(await expiredChat.json()).toMatchObject({ code: 'AI_SUBSCRIPTION_REQUIRED', requiredTier: 'PLUS' });
    const section = page.getByRole('region', { name: 'Công thức liên quan' });
    await section.getByRole('button', { name: 'AI gợi ý món ăn kèm hoặc biến tấu' }).click();
    await expect(section.getByRole('alert')).toContainText('gói AI PRO');
    await expect(section.getByRole('link', { name: 'Nâng cấp gói AI PRO' })).toHaveAttribute('href', '/goi-ai');
    await page.screenshot({ path: testInfo.outputPath('fr10-upgrade.png') });
    // The inline Gemini action must still reach the server guard after paid entitlement expires.
    await page.goto('/dang-cong-thuc');
    await page.getByLabel('Tên món *').waitFor({ state: 'visible', timeout: 30_000 });
    await page.getByLabel('Tên món *').fill(source.title);
    await page.getByLabel('Thể loại món').selectOption(source.dishCategory);
    await page.getByLabel('Loại ăn chay').selectOption(source.vegetarianType);
    await page.getByLabel('Khẩu phần').fill(String(source.servings));
    await page.getByLabel('Chọn nguyên liệu 1').fill(ingredients[0].name);
    await page.getByRole('button', { name: ingredients[0].name, exact: true }).click();
    await page.getByLabel('Số lượng nguyên liệu 1').fill(String(ingredients[0].quantity));
    await page.getByLabel('Đơn vị nguyên liệu 1').selectOption(String(ingredients[0].unitId));
    const rejectedAssistance = page.waitForResponse(response => response.url().endsWith('/recipes/ai-assistance')
      && response.request().method() === 'POST');
    await page.getByRole('button', { name: 'AI gợi ý viết mô tả' }).click();
    const rejection = await rejectedAssistance;
    expect(rejection.status()).toBe(403);
    expect(await rejection.json()).toMatchObject({ code: 'AI_SUBSCRIPTION_REQUIRED', requiredTier: 'PRO' });
    await expect(page.getByRole('link', { name: 'Nâng cấp gói AI PRO' })).toHaveAttribute('href', '/goi-ai');
    await page.screenshot({ path: testInfo.outputPath('fr10-inline-upgrade.png'), animations: 'disabled' });
    await expect(page.getByRole('textbox', { name: 'Mô tả' })).toHaveValue('');
    await expect(page.getByLabel('Số lượng nguyên liệu 1')).toHaveValue(String(ingredients[0].quantity));
    expect(sql(`SELECT COUNT(*) FROM [SUBSCRIPTION] WHERE user_id=${member} AND tier='PLUS'`)).toBe('1');
    expect(sql(`SELECT COUNT(*) FROM [RECIPE_POST] WHERE author_id=${member}`)).toBe('0');
  } finally {
    if (member) {
      sql(`DELETE FROM [RECIPE_VIEW] WHERE user_id=${member}; DELETE FROM [SUBSCRIPTION] WHERE user_id=${member};
        DELETE FROM [USER] WHERE user_id=${member}`);
    }
  }
});
