import { test, expect, type Page, type APIRequestContext } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Dedicated root Compose project, real services/SQL, G4FixtureApplication model only.
test.skip(process.env.MAMXANH_G4_FIXTURE !== 'true', 'Requires isolated G4 fixture stack');
test.use({ trace: 'off' });
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../../..');
const backend = process.env.MAMXANX_E2E_BACKEND_URL ?? 'http://localhost:18084/api/v1';
const project = 'mamxanh-g4';
function compose(args: string[]) {
  return execFileSync('docker', ['compose', '-p', project, '--env-file', path.join(root, 'app/mamxanh-backend/.env'),
    '-f', path.join(root, 'docker-compose.yml'), '-f', path.join(root, 'app/mamxanh-frontend/tests/integration/g4-compose.override.yml'), ...args],
    { cwd: root, encoding: 'utf8', timeout: 120_000, env: { ...process.env, MAMXANH_SQL_PORT: '12434', MAMXANH_BACKEND_PORT: '18084', MAMXANH_FRONTEND_PORT: '15174' } });
}
function sql(query: string) {
  return compose(['exec', '-T', 'sqlserver', '/opt/mssql-tools18/bin/sqlcmd', '-S', 'sqlserver', '-U', 'sa', '-C',
    '-d', 'MamXanhDB', '-h', '-1', '-W', '-b', '-Q', `SET NOCOUNT ON; SET QUOTED_IDENTIFIER ON; ${query}`]).trim();
}
const scopes = ['AI_CHAT', 'SHOPPING_LIST_READ', 'SHOPPING_LIST_WRITE'].map(purpose => ({ category: 'SHOPPING_LIST', purpose }));
async function account(page: Page, request: APIRequestContext) {
  const email = `g4-${randomUUID()}@example.invalid`; const password = `Aa1!${randomUUID()}z`;
  expect((await request.post(`${backend}/auth/register`, { data: { displayName: 'G4 fixture', email, password, confirmPassword: password } })).status()).toBe(201);
  sql(`UPDATE [USER] SET email_verified=1, preferred_ai_model_id='gemini-3.8-flash', onboarding_invited_at=SYSUTCDATETIME() WHERE email=N'${email}'`);
  const owner = Number(sql(`SELECT user_id FROM [USER] WHERE email=N'${email}'`));
  sql(`INSERT INTO SUBSCRIPTION(user_id,tier,status,token_quota,starts_at,ends_at) VALUES (${owner},'PRO','ACTIVE',1000000,DATEADD(day,-1,SYSUTCDATETIME()),DATEADD(day,30,SYSUTCDATETIME()))`);
  const login = await request.post(`${backend}/auth/login`, { data: { email, password } });
  expect(login.status()).toBe(200);
  const token = (await login.json()).accessToken as string;
  await page.goto('/dang-nhap');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Mật khẩu', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/trang-chu/);
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  return { owner, token };
}
async function pending(page: Page, name: string) {
  await page.getByLabel('Câu hỏi của bạn').fill(`Hãy tạo danh sách mua sắm rỗng tên là ${name}`);
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await page.getByRole('radio', { name: 'Đồng ý và tiếp tục', exact: true }).check();
  await page.getByRole('button', { name: 'Gửi', exact: true }).click();
  await expect(page.getByText(`Tên danh sách dự kiến: “${name}”`)).toBeVisible({ timeout: 25_000 });
}

test('reload pending then approve creates one real list and owner-only status', async ({ page, request }) => {
  const { owner, token } = await account(page, request);
  const name = `G4 reload ${randomUUID().slice(0, 8)}`;
  await pending(page, name);
  await page.reload();
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await expect(page.getByText(`Tên danh sách dự kiến: “${name}”`)).toBeVisible();
  const sent = page.waitForRequest(r => r.url().endsWith('/recipes/ai/chat') && r.postDataJSON()?.confirmationDecision === 'APPROVE');
  await page.getByRole('radio', { name: 'Xác nhận thay đổi', exact: true }).check();
  await page.getByRole('button', { name: 'Gửi', exact: true }).click();
  const id = (await sent).postDataJSON().clientRequestId;
  await expect.poll(() => Number(sql(`SELECT COUNT(*) FROM SHOPPING_LIST WHERE user_id=${owner}`))).toBe(1);
  const result = await request.get(`${backend}/recipes/ai/chat/executions/${id}`, { headers: { Authorization: `Bearer ${token}` } });
  expect((await result.json()).state).toBe('SUCCEEDED');
  expect((await request.get(`${backend}/recipes/ai/chat/executions/${id}`)).status()).toBe(401);
});

test('lost response after commit and Backend restart recover by clientRequestId without replay', async ({ page, request }) => {
  test.setTimeout(180_000);
  const { owner } = await account(page, request);
  await pending(page, `G4 lost ${randomUUID().slice(0, 8)}`);
  await page.route('**/api/v1/recipes/ai/chat', async route => {
    if (route.request().postDataJSON().confirmationDecision === 'APPROVE') {
      const real = await route.fetch(); expect(real.status()).toBe(200);
      await route.abort('connectionreset');
    } else await route.continue();
  });
  await page.getByRole('radio', { name: 'Xác nhận thay đổi', exact: true }).check();
  await page.getByRole('button', { name: 'Gửi', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Kiểm tra kết quả' })).toBeVisible();
  expect(Number(sql(`SELECT COUNT(*) FROM SHOPPING_LIST WHERE user_id=${owner}`))).toBe(1);
  compose(['restart', 'backend']);
  await expect.poll(async () => { try { return (await request.get(`${backend.replace('/api/v1','')}/v3/api-docs`)).status(); } catch { return 0; } }, { timeout: 90_000 }).toBe(200);
  await page.reload();
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByRole('button', { name: 'Kiểm tra kết quả' }).click();
  await expect(page.getByText(/Backend xác nhận đã tạo danh sách/)).toBeVisible();
  await expect(page.getByRole('radio', { name: 'Xác nhận thay đổi' })).toHaveCount(0);
  expect(Number(sql(`SELECT COUNT(*) FROM SHOPPING_LIST WHERE user_id=${owner}`))).toBe(1);
});

test('cancel bypasses busy before write and does not start another model run', async ({ page, request }) => {
  const { owner } = await account(page, request);
  await page.getByLabel('Câu hỏi của bạn').fill('Hãy tạo danh sách mua sắm rỗng tên là G4 trước');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await page.getByRole('radio', { name: 'Đồng ý và tiếp tục', exact: true }).check();
  await page.getByRole('button', { name: 'Gửi', exact: true }).click();
  const cancelled = page.waitForResponse(r => r.url().endsWith('/recipes/ai/chat') && r.request().postDataJSON()?.controlAction === 'CANCEL');
  await page.getByRole('button', { name: 'Dừng', exact: true }).click();
  expect((await cancelled).status()).toBe(200);
  await expect(page.getByRole('button', { name: 'Gửi câu hỏi' })).toBeDisabled();
  expect(Number(sql(`SELECT COUNT(*) FROM SHOPPING_LIST WHERE user_id=${owner}`))).toBe(0);
});

test('concurrent HTTP duplicates use one SQL write', async ({ page, request }) => {
  const { owner, token } = await account(page, request);
  const clientRequestId = randomUUID();
  const data = { contextType: 'GENERAL', clientRequestId, executionMode: 'AUTO', history: [],
    question: `Hãy tạo danh sách mua sắm rỗng tên là G4 concurrent ${randomUUID().slice(0,8)}`,
    consentDecision: { action: 'GRANT', scopes } };
  const results = await Promise.all([0, 1].map(() => request.post(`${backend}/recipes/ai/chat`, { data, headers: { Authorization: `Bearer ${token}` } })));
  expect(results.every(r => r.status() === 200)).toBeTruthy();
  expect(Number(sql(`SELECT COUNT(*) FROM SHOPPING_LIST WHERE user_id=${owner}`))).toBe(1);
  const found = await request.get(`${backend}/recipes/ai/chat/executions/${clientRequestId}`, { headers: { Authorization: `Bearer ${token}` } });
  expect((await found.json()).state).toBe('SUCCEEDED');
});

test('cancel while SQL insert is in progress keeps the committed result', async ({ page, request }) => {
  const { owner, token } = await account(page, request);
  await pending(page, `G4 during ${randomUUID().slice(0,8)}`);
  // Test-only SQL barrier: keep the real insert transaction open after the write boundary.
  sql("EXEC(N'CREATE OR ALTER TRIGGER G4_WRITE_BARRIER ON SHOPPING_LIST AFTER INSERT AS BEGIN IF EXISTS(SELECT 1 FROM inserted WHERE name LIKE ''G4 during%'') WAITFOR DELAY ''00:00:04''; END')");
  try {
    const sent = page.waitForRequest(r => r.url().endsWith('/recipes/ai/chat') && r.postDataJSON()?.confirmationDecision === 'APPROVE');
    await page.getByRole('radio', { name: 'Xác nhận thay đổi', exact: true }).check();
  await page.getByRole('button', { name: 'Gửi', exact: true }).click();
    const clientRequestId = (await sent).postDataJSON().clientRequestId;
    await expect.poll(() => sql(`SELECT state FROM AI_ASSISTANT_EXECUTION WITH (NOLOCK) WHERE owner_id=${owner} AND client_request_id='${clientRequestId}'`),
      { timeout: 10_000, intervals: [100] }).toBe('WRITING');
    const cancelled = page.waitForResponse(r => r.url().endsWith('/recipes/ai/chat') && r.request().postDataJSON()?.controlAction === 'CANCEL');
    await page.getByRole('button', { name: 'Dừng', exact: true }).click();
    expect((await cancelled).status()).toBe(200);
    const found = await request.get(`${backend}/recipes/ai/chat/executions/${clientRequestId}`, { headers: { Authorization: `Bearer ${token}` } });
    expect((await found.json()).state).toBe('SUCCEEDED');
    expect(Number(sql(`SELECT COUNT(*) FROM SHOPPING_LIST WHERE user_id=${owner}`))).toBe(1);
  } finally { sql('DROP TRIGGER IF EXISTS G4_WRITE_BARRIER'); }
});
