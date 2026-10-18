import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, test } from '@playwright/test';

const frontendDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(frontendDir, '../..');
const backendUrl = process.env.MAMXANX_E2E_BACKEND_URL ?? 'http://localhost:8080/api/v1';
const composeProject = process.env.MAMXANX_E2E_COMPOSE_PROJECT ?? 'mamxanh-dev';

// The opt-in test uses a real local session; do not persist its credentials in a trace.
test.use({ trace: 'off' });

function sql(query: string): string {
  return execFileSync('docker', ['compose', '-p', composeProject, '-f', path.join(repositoryRoot, 'docker-compose.yml'),
    'exec', '-T', 'sqlserver', '/opt/mssql-tools18/bin/sqlcmd', '-S', 'sqlserver', '-U', 'sa', '-C', '-d',
    'MamXanhDB', '-h', '-1', '-W', '-b', '-Q', `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`],
  { cwd: repositoryRoot, encoding: 'utf8', timeout: 30_000 }).trim();
}

test('real recipe detail recommendations match current SQL Server data without calling AI', async ({ page, request }) => {
  // Pick a stable seed recipe. The FR-58 test inserts and deletes a newest recipe in this
  // shared database, so relying on the first catalog result races with that fixture cleanup.
  const sourceId = Number(sql(
    "SELECT TOP (1) recipe_id FROM [RECIPE_POST] WHERE status='PUBLISHED' ORDER BY recipe_id ASC",
  ));
  expect(sourceId, 'Compose local demo data must include a published recipe').toBeGreaterThan(0);
  const sourceResponse = await request.get(`${backendUrl}/recipes/${sourceId}`);
  expect(sourceResponse.ok(), `GET source recipe returned HTTP ${sourceResponse.status()}`).toBeTruthy();
  const source = await sourceResponse.json() as { title: string };
  let aiRequests = 0;
  page.on('request', (request) => { if (request.url().endsWith('/related/ai')) aiRequests++; });
  const relatedResponsePromise = page.waitForResponse((response) =>
    response.url().endsWith(`/recipes/${sourceId}/related`) && response.request().method() === 'GET');
  await page.goto(`/cong-thuc/id/${sourceId}`);
  await expect(page.getByRole('heading', { name: source.title, exact: true })).toBeVisible();
  const response = await relatedResponsePromise;
  const responseBody = await response.text();
  expect(response.ok(), `GET related recipes returned HTTP ${response.status()}: ${responseBody}`)
    .toBeTruthy();
  const payload = JSON.parse(responseBody) as { items: Array<{ id: number; title: string }> };
  const section = page.getByRole('region', { name: 'Công thức liên quan' });
  for (const item of payload.items) {
    expect(item.id).not.toBe(sourceId);
    expect(Number.isSafeInteger(item.id)).toBeTruthy();
    expect(sql(`SELECT CONCAT(status,'|',title) FROM [RECIPE_POST] WHERE recipe_id=${item.id}`))
      .toBe(`PUBLISHED|${item.title}`);
    await expect(section.getByRole('heading', { name: item.title, exact: true })).toBeVisible();
  }
  if (payload.items.length === 0) await expect(section.getByText('Chưa có công thức liên quan.')).toBeVisible();
  expect(aiRequests).toBe(0);
});

test('live Gemini selects only existing recipes after the demo Pro Expert explicitly clicks', async ({ page, request }) => {
  test.skip(process.env.MAMXANH_E2E_LIVE_GEMINI !== '1', 'Live provider test is opt-in; CI has no Gemini credential.');
  const password = process.env.MAMXANH_DEMO_PASSWORD;
  if (!password) throw new Error('MAMXANH_DEMO_PASSWORD must be supplied through the local environment.');
  const login = await request.post(`${backendUrl}/auth/login`, {
    data: { email: 'demo-expert@mamxanh.local', password },
  });
  expect(login.ok()).toBeTruthy();
  const session = await login.json();
  const demoExpertId = session.account.id as number;
  expect(Number.isSafeInteger(demoExpertId)).toBeTruthy();
  await page.addInitScript((session) => {
    sessionStorage.setItem('mamxanh.auth', JSON.stringify({
      accessToken: session.accessToken, expiresAt: Date.now() + session.expiresInSeconds * 1000, account: session.account,
    }));
  }, session);
  const catalog = await (await request.get(`${backendUrl}/recipes?size=1`)).json();
  const sourceId = catalog.items[0].id as number;
  const before = sql(`SELECT COUNT(*) FROM [RECIPE_POST] WHERE author_id=${demoExpertId}`);
  await page.goto(`/cong-thuc/id/${sourceId}`);
  const section = page.getByRole('region', { name: 'Công thức liên quan' });
  const responsePromise = page.waitForResponse((response) => response.url().endsWith(`/recipes/${sourceId}/related/ai`));
  await section.getByRole('button', { name: 'AI gợi ý món ăn kèm hoặc biến tấu' }).click();
  const response = await responsePromise;
  expect(response.status(), `Live Gemini returned HTTP ${response.status()}`).toBe(200);
  const payload = await response.json() as { items: Array<{ id: number; title: string; vegetarianType: string }> };
  expect(payload.items.length, 'Demo recipes provide pairing/variation candidates').toBeGreaterThan(0);
  const sourceDiet = sql(`SELECT vegetarian_type FROM [RECIPE_POST] WHERE recipe_id=${sourceId}`);
  for (const item of payload.items) {
    expect(Number.isSafeInteger(item.id)).toBeTruthy();
    expect(item.id).not.toBe(sourceId);
    expect(sql(`SELECT CONCAT(status,'|',vegetarian_type,'|',title) FROM [RECIPE_POST] WHERE recipe_id=${item.id}`))
      .toBe(`PUBLISHED|${sourceDiet}|${item.title}`);
  }
  await expect(section.getByRole('heading', { name: 'Gợi ý từ AI', exact: true })).toBeVisible();
  expect(sql(`SELECT COUNT(*) FROM [RECIPE_POST] WHERE author_id=${demoExpertId}`)).toBe(before);
});
