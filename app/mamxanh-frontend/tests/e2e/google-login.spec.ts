import type { Page, Route } from '@playwright/test';
import { expect, test } from './baseFixtures';

// UC-03.5 (#8). Google Identity Services is replaced by a stub script and the Backend by page.route stubs, so these
// tests check the Frontend flow only; they are not evidence of real Google or Backend behaviour.
const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'content-type, accept, authorization',
};
const GOOGLE_CREDENTIAL = 'google-header.google-payload.google-signature';
const account = { id: 202, displayName: 'Nguyễn Văn An', email: 'an.nguyen@gmail.com', avatarUrl: null, role: 'CUSTOMER', accountStatus: 'ACTIVE', emailVerified: true };

// Minimal stand-in for https://accounts.google.com/gsi/client: renders one button that answers like Google.
const gisStub = `window.google = { accounts: { id: {
  initialize(config) { window.__gisConfig = config; },
  renderButton(element) {
    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = 'Google: chọn tài khoản';
    button.addEventListener('click', () => window.__gisConfig.callback(window.__gisNextResponse ?? { credential: '${GOOGLE_CREDENTIAL}', select_by: 'btn' }));
    element.appendChild(button);
  },
  prompt() {}, cancel() {}, disableAutoSelect() {},
} } };`;

async function stubGoogleScript(page: Page, available = true) {
  await page.route('https://accounts.google.com/gsi/client**', route => available
    ? route.fulfill({ status: 200, contentType: 'text/javascript', body: gisStub })
    : route.abort());
}

async function stubApi(page: Page, path: string, respond: (route: Route) => Promise<void>) {
  await page.route(`**/api/v1${path}`, async route => {
    if (route.request().method() === 'OPTIONS') return route.fulfill({ status: 204, headers: corsHeaders });
    return respond(route);
  });
}

async function stubOnboardingInvitation(page: Page, show: boolean) {
  await stubApi(page, '/nutrition/dietary-preferences/onboarding/invitation', route => route.fulfill({
    status: 200, contentType: 'application/json', headers: corsHeaders, body: JSON.stringify({ show }),
  }));
}

const authResponse = { status: 200, contentType: 'application/json', headers: corsHeaders, body: JSON.stringify({ accessToken: 'header.payload.signature', tokenType: 'Bearer', expiresInSeconds: 3600, account }) };
const problem = (status: number, code: string) => ({ status, contentType: 'application/problem+json', headers: corsHeaders, body: JSON.stringify({ title: code, status, code, detail: `Chi tiết ${code}` }) });
const storedSession = (page: Page) => page.evaluate(() => sessionStorage.getItem('mamxanh.auth'));
const googleButton = (page: Page) => page.getByRole('button', { name: 'Google: chọn tài khoản' });

test('Google sign-in sends the ID token, keeps the session in this tab and opens Onboarding for a new member (mock GIS and API)', async ({ page }) => {
  let submitted: Record<string, string> | null = null;
  let authorization: string | null = 'not checked';
  await stubGoogleScript(page);
  await stubApi(page, '/auth/google', async route => {
    submitted = route.request().postDataJSON();
    authorization = await route.request().headerValue('authorization');
    await route.fulfill(authResponse);
  });
  await stubOnboardingInvitation(page, true);

  await page.goto('/dang-nhap');
  await googleButton(page).click();

  await expect(page).toHaveURL(/\/khoi-tao-so-thich$/);
  expect(submitted).toEqual({ idToken: GOOGLE_CREDENTIAL });
  expect(authorization).toBeNull();
  const stored = JSON.parse((await storedSession(page)) ?? 'null');
  expect(stored).toMatchObject({ accessToken: 'header.payload.signature', account: { id: 202, email: 'an.nguyen@gmail.com' } });
  expect(await page.evaluate(() => localStorage.getItem('mamxanh.auth'))).toBeNull();
});

test('Google sign-in from the registration page lands on the home page when Onboarding was already offered (mock GIS and API)', async ({ page }) => {
  await stubGoogleScript(page);
  await stubApi(page, '/auth/google', route => route.fulfill(authResponse));
  await stubOnboardingInvitation(page, false);

  await page.goto('/dang-ky');
  await googleButton(page).click();

  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole('button', { name: 'Tài khoản Nguyễn Văn An' })).toBeVisible();
});

test('Google sign-in errors follow the problem code and create no session (mock GIS and API)', async ({ page }) => {
  const responses = [
    problem(401, 'GOOGLE_TOKEN_INVALID'),
    problem(403, 'ACCOUNT_LOCKED'),
    problem(409, 'GOOGLE_ACCOUNT_CONFLICT'),
    problem(503, 'GOOGLE_LOGIN_UNAVAILABLE'),
  ];
  await stubGoogleScript(page);
  await stubApi(page, '/auth/google', route => route.fulfill(responses.shift()!));
  await page.goto('/dang-nhap');

  await googleButton(page).click();
  await expect(page.getByRole('alert')).toHaveText('Không xác thực được tài khoản Google. Vui lòng thử đăng nhập Google lại.');
  await googleButton(page).click();
  await expect(page.getByRole('alert')).toHaveText('Tài khoản đã bị quản trị viên khóa. Vui lòng liên hệ quản trị viên.');
  await googleButton(page).click();
  await expect(page.getByRole('alert')).toHaveText('Email này đã được liên kết với một tài khoản Google khác. Hãy đăng nhập bằng tài khoản Google đó hoặc bằng email và mật khẩu.');
  await googleButton(page).click();
  await expect(page.getByRole('alert')).toHaveText('Chưa thể đăng nhập bằng Google lúc này. Vui lòng thử lại sau hoặc đăng nhập bằng email.');

  await expect(page).toHaveURL(/\/dang-nhap$/);
  expect(await storedSession(page)).toBeNull();
});

test('a Google sign-in that returns no credential asks to retry without calling the Backend (mock GIS)', async ({ page }) => {
  const calls: string[] = [];
  page.on('request', request => { if (request.url().includes('/api/v1/auth/google')) calls.push(request.method()); });
  await stubGoogleScript(page);
  await page.goto('/dang-nhap');
  await page.evaluate(() => { (window as unknown as { __gisNextResponse: object }).__gisNextResponse = {}; });

  await googleButton(page).click();

  await expect(page.getByRole('alert')).toHaveText('Không đăng nhập được với Google. Vui lòng thử lại.');
  expect(calls).toEqual([]);
});

test('email login stays available when the Google script cannot be loaded (mock GIS)', async ({ page }) => {
  await stubGoogleScript(page, false);
  await page.goto('/dang-nhap');

  await expect(page.getByText('Không tải được đăng nhập Google. Bạn vẫn có thể đăng nhập bằng email.')).toBeVisible();
  await expect(googleButton(page)).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Đăng nhập', exact: true })).toBeEnabled();
});
