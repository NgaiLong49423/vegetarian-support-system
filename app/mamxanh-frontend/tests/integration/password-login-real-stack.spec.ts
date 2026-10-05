import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, test } from '@playwright/test';

const frontendDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(frontendDir, '../..');
const backendUrl = process.env.MAMXANX_E2E_BACKEND_URL ?? 'http://localhost:8080/api/v1';
const composeProject = process.env.MAMXANX_E2E_COMPOSE_PROJECT ?? 'mamxanh-dev';
const email = `ci-${randomUUID()}@example.invalid`;
const password = `Aa1!${randomUUID()}z`;

function sql(query: string): string {
  try {
    return execFileSync('docker', [
      'compose', '-p', composeProject, '-f', path.join(repositoryRoot, 'docker-compose.yml'),
      'exec', '-T', 'sqlserver', '/opt/mssql-tools18/bin/sqlcmd',
      '-S', 'sqlserver', '-U', 'sa', '-C', '-d', 'MamXanhDB', '-h', '-1', '-W', '-b',
      '-Q', `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`,
    ], { cwd: repositoryRoot, encoding: 'utf8', timeout: 30_000 }).trim();
  } catch (error) {
    const diagnostic = error instanceof Error
      ? ['stderr', 'stdout']
        .filter(name => name in error)
        .map(name => String(error[name as keyof typeof error]).trim())
        .filter(Boolean)
        .join('\n')
      : '';
    throw new Error(diagnostic ? `SQL Server integration query failed: ${diagnostic}` : 'SQL Server integration query failed');
  }
}

function accountState(): { attempts: number; status: string; blockedUntil: string } {
  const literal = email.replaceAll("'", "''");
  const row = sql(`SELECT CONCAT(failed_login_attempts, '|', account_status, '|', COALESCE(CONVERT(varchar(33), login_blocked_until, 126), 'NULL')) FROM [USER] WHERE email = '${literal}'`);
  const [attempts, status, blockedUntil] = row.split('|');
  expect(attempts, 'SQL Server should return exactly one matching test account').toBeTruthy();
  return { attempts: Number(attempts), status, blockedUntil };
}

test('real browser login updates throttle data in the Compose SQL Server', async ({ page, request }) => {
  const registration = await request.post(`${backendUrl}/auth/register`, {
    data: { displayName: 'CI Login Test', email, password, confirmPassword: password },
  });
  expect(registration.status()).toBe(201);

  const literal = email.replaceAll("'", "''");
  // This test covers the login throttle (NFR-07); mark the FR-31 Onboarding invitation as closed so a
  // successful sign-in lands on the home page instead of the questionnaire offered to new Members.
  sql(`UPDATE [USER] SET email_verified = 1, onboarding_status = 'SKIPPED' WHERE email = '${literal}'`);
  expect(accountState()).toMatchObject({ attempts: 0, status: 'ACTIVE', blockedUntil: 'NULL' });

  await page.goto('/dang-nhap');
  await page.getByLabel('Email').fill(email);
  await page.getByRole('textbox', { name: 'Mật khẩu' }).fill(password);
  const successResponse = page.waitForResponse(response => response.url().endsWith('/auth/login'));
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  expect((await successResponse).status()).toBe(200);
  await expect(page).toHaveURL(/\/$/);
  const session = await page.evaluate(() => ({
    session: window.sessionStorage.getItem('mamxanh.auth'),
    local: window.localStorage.getItem('mamxanh.auth'),
  }));
  expect(session.session).toBeTruthy();
  expect(session.local).toBeNull();

  await page.getByRole('button', { name: 'Tài khoản CI Login Test' }).click();
  await page.getByRole('button', { name: 'Đăng xuất' }).click();
  await page.goto('/dang-nhap');
  for (let attempt = 1; attempt <= 4; attempt += 1) {
    await page.getByLabel('Email').fill(email);
    await page.getByRole('textbox', { name: 'Mật khẩu' }).fill(`Wrong-${attempt}-Aa1!`);
    const response = page.waitForResponse(item => item.url().endsWith('/auth/login'));
    await page.getByRole('button', { name: 'Đăng nhập' }).click();
    expect((await response).status()).toBe(401);
    expect(accountState()).toMatchObject({ attempts: attempt, status: 'ACTIVE', blockedUntil: 'NULL' });
  }

  await page.getByLabel('Email').fill(email);
  await page.getByRole('textbox', { name: 'Mật khẩu' }).fill('Wrong-fifth-Aa1!');
  const blockedResponsePromise = page.waitForResponse(item => item.url().endsWith('/auth/login'));
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  const blockedResponse = await blockedResponsePromise;
  expect(blockedResponse.status()).toBe(429);
  expect(Number(blockedResponse.headers()['retry-after'])).toBeGreaterThan(0);
  const blockedState = accountState();
  expect(blockedState.attempts).toBe(5);
  expect(blockedState.status).toBe('ACTIVE');
  const blockedUntil = Date.parse(`${blockedState.blockedUntil}Z`);
  expect(blockedUntil).toBeGreaterThan(Date.now());
  expect(blockedUntil).toBeLessThan(Date.now() + 11 * 60_000);

  sql(`UPDATE [USER] SET login_blocked_until = DATEADD(minute, -1, SYSUTCDATETIME()) WHERE email = '${literal}'`);
  await page.getByLabel('Email').fill(email);
  await page.getByRole('textbox', { name: 'Mật khẩu' }).fill(password);
  const retryResponsePromise = page.waitForResponse(item => item.url().endsWith('/auth/login'));
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  expect((await retryResponsePromise).status()).toBe(200);
  await expect(page).toHaveURL(/\/$/);
  expect(accountState()).toMatchObject({ attempts: 0, status: 'ACTIVE', blockedUntil: 'NULL' });
});
