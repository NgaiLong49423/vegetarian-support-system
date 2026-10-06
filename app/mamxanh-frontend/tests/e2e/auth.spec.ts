import type { Page, Route } from '@playwright/test';
import { expect, test } from './baseFixtures';

// Registration, email verification and login call the backend API. These browser tests replace the API with
// page.route stubs, so they check the Frontend flow only; they are not full FE–BE end-to-end evidence.
const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'content-type, accept',
  'Access-Control-Expose-Headers': 'Retry-After',
};

async function stubApi(page: Page, path: string, respond: (route: Route) => Promise<void>) {
  await page.route(`**/api/v1${path}`, async route => {
    if (route.request().method() === 'OPTIONS') return route.fulfill({ status: 204, headers: corsHeaders });
    return respond(route);
  });
}

function problem(status: number, code: string, extra: Record<string, unknown> = {}, headers: Record<string, string> = {}) {
  return { status, contentType: 'application/problem+json', headers: { ...corsHeaders, ...headers }, body: JSON.stringify({ title: code, status, code, ...extra }) };
}

test('registration validates password rules, submits to the API and links to email verification (mock API)', async ({ page }) => {
  let submitted: Record<string, string> | null = null;
  await stubApi(page, '/auth/register', async route => {
    submitted = route.request().postDataJSON();
    await route.fulfill({ status: 201, contentType: 'application/json', headers: corsHeaders, body: JSON.stringify({ accountStatus: 'ACTIVE', emailVerified: false, message: 'Đăng ký thành công. Vui lòng kiểm tra email để xác minh tài khoản.' }) });
  });
  await page.goto('/');
  await page.getByRole('link', { name: 'Đăng ký', exact: true }).click();
  await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
  await expect(page.getByText('Tên hiển thị cần từ 3 đến 50 ký tự.')).toBeVisible();
  await expect(page.getByText('Vui lòng nhập địa chỉ email hợp lệ.')).toBeVisible();
  await page.getByLabel('Tên hiển thị').fill('  Nguyễn An ');
  await page.getByLabel('Email', { exact: true }).fill('an@example.com');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('demopass');
  await page.getByLabel('Xác nhận mật khẩu').fill('demopass');
  await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
  await expect(page.locator('#password-error')).toContainText('chữ in hoa');
  await expect(page.locator('#password-error')).toContainText('chữ số');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('DemoPass123!');
  await page.getByLabel('Xác nhận mật khẩu').fill('Different123!');
  await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
  await expect(page.getByText('Mật khẩu xác nhận chưa khớp.')).toBeVisible();
  expect(submitted).toBeNull();
  await page.getByLabel('Xác nhận mật khẩu').fill('DemoPass123!');
  await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Đăng ký thành công');
  expect(submitted).toEqual({ displayName: 'Nguyễn An', email: 'an@example.com', password: 'DemoPass123!', confirmPassword: 'DemoPass123!' });
  await expect(page.getByLabel('Mật khẩu', { exact: true })).toHaveValue('');
  await page.getByRole('link', { name: 'Chưa nhận được email? Gửi lại email xác minh' }).click();
  await expect(page.getByRole('heading', { name: 'Xác minh email của bạn' })).toBeVisible();
});

test('registration shows the duplicate-email error returned by the API (mock API)', async ({ page }) => {
  await stubApi(page, '/auth/register', route => route.fulfill(problem(409, 'EMAIL_ALREADY_USED', { detail: 'Email này đã được sử dụng.' })));
  await page.goto('/dang-ky');
  await page.getByLabel('Tên hiển thị').fill('Nguyễn An');
  await page.getByLabel('Email', { exact: true }).fill('an@example.com');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('DemoPass123!');
  await page.getByLabel('Xác nhận mật khẩu').fill('DemoPass123!');
  await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
  await expect(page.locator('#email-error')).toContainText('Email này đã được sử dụng');
});

test('registration maps Backend field validation errors to the matching form controls (mock API)', async ({ page }) => {
  await stubApi(page, '/auth/register', route => route.fulfill(problem(400, 'VALIDATION_FAILED', {
    errors: [
      { field: 'displayName', message: 'Tên hiển thị đã bị từ chối.' },
      { field: 'email', message: 'Email không được chấp nhận.' },
      { field: 'confirmPassword', message: 'Xác nhận mật khẩu không khớp.' },
    ],
  })));
  await page.goto('/dang-ky');
  await page.getByLabel('Tên hiển thị').fill('Nguyễn An');
  await page.getByLabel('Email', { exact: true }).fill('an@example.com');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('DemoPass123!');
  await page.getByLabel('Xác nhận mật khẩu').fill('DemoPass123!');
  await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();

  await expect(page.locator('#name-error')).toHaveText('Tên hiển thị đã bị từ chối.');
  await expect(page.locator('#email-error')).toHaveText('Email không được chấp nhận.');
  await expect(page.locator('#confirm-error')).toHaveText('Xác nhận mật khẩu không khớp.');
});

test('verification link is submitted once and removed from the address bar (mock API)', async ({ page }) => {
  const tokens: string[] = [];
  await stubApi(page, '/auth/email-verifications', async route => {
    tokens.push(route.request().postDataJSON().token);
    await route.fulfill({ status: 204, headers: corsHeaders });
  });
  await page.goto('/xac-minh-email?token=abc123');
  await expect(page.getByRole('status')).toContainText('Email của bạn đã được xác minh.');
  await expect(page).toHaveURL(/\/xac-minh-email$/);
  expect(tokens).toEqual(['abc123']);
  await page.getByRole('link', { name: 'Đến trang đăng nhập' }).click();
  await expect(page).toHaveURL(/\/dang-nhap$/);
});

test('invalid verification link offers a new email and reports the resend cooldown (mock API)', async ({ page }) => {
  await stubApi(page, '/auth/email-verifications', route => route.fulfill(problem(400, 'VERIFICATION_TOKEN_INVALID')));
  await stubApi(page, '/auth/email-verifications/resend', route => route.fulfill(problem(429, 'RESEND_TOO_SOON', {}, { 'Retry-After': '42' })));
  await page.goto('/xac-minh-email?token=expired');
  await expect(page.getByRole('alert')).toContainText('không hợp lệ hoặc đã hết hạn');
  await page.getByLabel('Email', { exact: true }).fill('an@example.com');
  await page.getByRole('button', { name: 'Yêu cầu gửi lại email' }).click();
  await expect(page.getByRole('alert')).toContainText('42 giây');
});

test('resend request shows the neutral message from the API (mock API)', async ({ page }) => {
  await stubApi(page, '/auth/email-verifications/resend', route => route.fulfill({ status: 202, contentType: 'application/json', headers: corsHeaders, body: JSON.stringify({ message: 'Nếu email thuộc một tài khoản chưa xác minh, chúng tôi đã gửi liên kết xác minh mới.' }) }));
  await page.goto('/xac-minh-email');
  await page.getByLabel('Email', { exact: true }).fill('an@example.com');
  await page.getByRole('button', { name: 'Yêu cầu gửi lại email' }).click();
  await expect(page.getByRole('status')).toContainText('Nếu email thuộc một tài khoản chưa xác minh');
});

const account = { id: 101, displayName: 'Nguyễn An', email: 'an@example.com', avatarUrl: null, role: 'CUSTOMER', accountStatus: 'ACTIVE', emailVerified: true };

function authResponse(expiresInSeconds = 3600) {
  return { status: 200, contentType: 'application/json', headers: corsHeaders, body: JSON.stringify({ accessToken: 'header.payload.signature', tokenType: 'Bearer', expiresInSeconds, account }) };
}

async function submitLogin(page: Page, email = 'an@example.com', password = 'MatKhau123') {
  await page.getByLabel('Email', { exact: true }).fill(email);
  await page.getByLabel('Mật khẩu', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
}

const storedSession = (page: Page) => page.evaluate(() => sessionStorage.getItem('mamxanh.auth'));

// After a successful login the app asks whether to show the FR-31 Onboarding invitation; without it the login lands on the home page.
async function stubAnsweredOnboarding(page: Page) {
  await stubApi(page, '/nutrition/dietary-preferences/onboarding/invitation', async route => route.fulfill({
    status: 200, contentType: 'application/json', headers: corsHeaders, body: JSON.stringify({ show: false }),
  }));
}

test('login keeps the session in this tab and logout clears it without calling the server (mock API)', async ({ page }) => {
  let submitted: Record<string, string> | null = null;
  let loginAuthorization: string | null = null;
  const serverCalls: string[] = [];
  page.on('request', request => {
    if (request.url().includes('/api/v1/') && request.method() !== 'OPTIONS') serverCalls.push(`${request.method()} ${new URL(request.url()).pathname}`);
  });
  await stubApi(page, '/auth/login', async route => {
    submitted = route.request().postDataJSON();
    loginAuthorization = await route.request().headerValue('authorization');
    await route.fulfill(authResponse());
  });
  await stubAnsweredOnboarding(page);

  await page.goto('/dang-nhap');
  await submitLogin(page, '  An@Example.com ');

  await expect(page).toHaveURL(/\/$/);
  expect(submitted).toEqual({ email: 'An@Example.com', password: 'MatKhau123' });
  expect(loginAuthorization).toBeNull();
  const stored = JSON.parse((await storedSession(page)) ?? 'null');
  expect(stored).toMatchObject({ accessToken: 'header.payload.signature', account: { id: 101, email: 'an@example.com' } });
  expect(stored.expiresAt).toBeGreaterThan(Date.now());
  expect(await page.evaluate(() => localStorage.getItem('mamxanh.auth'))).toBeNull();

  await page.reload();
  await page.getByRole('button', { name: 'Tài khoản Nguyễn An' }).click();
  await expect(page.getByText('an@example.com')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Đăng nhập', exact: true })).toHaveCount(0);

  await page.getByRole('button', { name: 'Đăng xuất' }).click();
  await expect(page).toHaveURL(/\/dang-nhap$/);
  expect(await storedSession(page)).toBeNull();
  expect(serverCalls).toEqual(['POST /api/v1/auth/login', 'POST /api/v1/nutrition/dietary-preferences/onboarding/invitation']);
});

test('login errors follow the problem code and never keep the password (mock API)', async ({ page }) => {
  const responses = [
    problem(401, 'INVALID_CREDENTIALS', { detail: 'Email hoặc mật khẩu không chính xác.' }),
    problem(403, 'EMAIL_NOT_VERIFIED'),
    problem(429, 'LOGIN_TEMPORARILY_BLOCKED', {}, { 'Retry-After': '360' }),
    problem(403, 'ACCOUNT_LOCKED'),
  ];
  await stubApi(page, '/auth/login', async route => route.fulfill(responses.shift()!));
  await page.goto('/dang-nhap');

  await submitLogin(page);
  await expect(page.getByRole('alert')).toHaveText('Email hoặc mật khẩu không chính xác.');
  await expect(page.getByLabel('Mật khẩu', { exact: true })).toHaveValue('');

  await submitLogin(page);
  await expect(page.getByRole('alert')).toContainText('Tài khoản chưa xác minh email');
  await expect(page.getByRole('link', { name: 'Gửi lại email xác minh' })).toHaveAttribute('href', '/xac-minh-email');

  await submitLogin(page);
  await expect(page.getByRole('alert')).toContainText('Vui lòng thử lại sau 6 phút.');

  await submitLogin(page);
  await expect(page.getByRole('alert')).toContainText('Tài khoản đã bị quản trị viên khóa');
  expect(await storedSession(page)).toBeNull();
});

test('an expired stored session is dropped and a live session ends when its token expires (mock API)', async ({ page }) => {
  await page.addInitScript(([key, value]) => {
    if (!sessionStorage.getItem('seeded')) {
      sessionStorage.setItem(key, value);
      sessionStorage.setItem('seeded', '1');
    }
  }, ['mamxanh.auth', JSON.stringify({ accessToken: 'old.token.value', expiresAt: 1, account })]);
  await page.goto('/');
  await expect(page.getByRole('link', { name: 'Đăng nhập', exact: true })).toBeVisible();
  expect(await storedSession(page)).toBeNull();

  await stubApi(page, '/auth/login', async route => route.fulfill(authResponse(2)));
  await stubAnsweredOnboarding(page);
  await page.goto('/dang-nhap');
  await submitLogin(page);
  await expect(page.getByRole('button', { name: 'Tài khoản Nguyễn An' })).toBeVisible();
  await expect(page).toHaveURL(/\/dang-nhap$/, { timeout: 5000 });
  await expect(page.getByRole('alert')).toHaveText('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
  expect(await storedSession(page)).toBeNull();
});

test('local account access requires a Backend-issued login session', async ({ page }) => {
  await page.goto('/dang-nhap');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('DemoPass123!');
  await page.getByRole('button', { name: 'Hiện mật khẩu', exact: true }).click();
  await expect(page.getByLabel('Mật khẩu', { exact: true })).toHaveAttribute('type', 'text');
  await page.getByRole('button', { name: 'Tiếp tục với Google' }).click();
  await expect(page.getByRole('status')).toContainText('Google Login chưa được kết nối');
  await expect(page.getByRole('button', { name: 'Khám phá tài khoản demo' })).toHaveCount(0);
  expect(await storedSession(page)).toBeNull();
  await expect(page.getByRole('button', { name: 'Đăng nhập', exact: true })).toBeVisible();
});

test('recovery and mobile layouts remain usable without claiming email delivery', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/');
  await page.getByRole('link', { name: 'Đăng nhập', exact: true }).click();
  await page.getByRole('link', { name: 'Quên mật khẩu?' }).click();
  await expect(page.getByRole('heading', { name: 'Quên mật khẩu?' })).toBeVisible();
  await page.getByLabel('Email', { exact: true }).fill('unknown@example.com');
  await page.getByRole('button', { name: 'Yêu cầu đặt lại mật khẩu' }).click();
  await expect(page.getByRole('status')).toContainText('không kiểm tra địa chỉ này có tài khoản');
  for (const path of ['/dang-nhap', '/dang-ky', '/xac-minh-email', '/dat-lai-mat-khau']) {
    await page.goto(path);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  }
  await page.getByLabel('Mật khẩu mới', { exact: true }).fill('short');
  await page.getByLabel('Xác nhận mật khẩu').fill('short');
  await page.getByRole('button', { name: 'Lưu mật khẩu mới' }).click();
  await expect(page.getByText('Mật khẩu cần ít nhất 8 ký tự.')).toBeVisible();
  await page.getByLabel('Mật khẩu mới', { exact: true }).fill('NewPass123!');
  await page.getByLabel('Xác nhận mật khẩu').fill('NewPass123!');
  await page.getByRole('button', { name: 'Lưu mật khẩu mới' }).click();
  await expect(page.getByRole('status')).toContainText('Chưa xác minh liên kết hoặc thay đổi mật khẩu');
});
