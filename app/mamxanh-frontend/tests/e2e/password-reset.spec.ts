import type { Page, Route } from '@playwright/test';
import { expect, test } from './baseFixtures';

// FR-03-E (#9) forgot/reset password pages. The API is replaced with page.route stubs, so these tests check the
// Frontend flow only; they are not full FE–BE end-to-end evidence.
const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'content-type, accept',
};
const NEUTRAL = 'Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hộp thư của bạn.';

async function stubApi(page: Page, path: string, respond: (route: Route) => Promise<void>) {
  await page.route(`**/api/v1${path}`, async route => {
    if (route.request().method() === 'OPTIONS') return route.fulfill({ status: 204, headers: corsHeaders });
    return respond(route);
  });
}

function problem(status: number, code: string, extra: Record<string, unknown> = {}) {
  return { status, contentType: 'application/problem+json', headers: corsHeaders, body: JSON.stringify({ title: code, status, code, ...extra }) };
}

test('forgot password sends the email to the API and shows its neutral message (mock API)', async ({ page }) => {
  const submitted: unknown[] = [];
  await stubApi(page, '/auth/password-resets', async route => {
    submitted.push(route.request().postDataJSON());
    await route.fulfill({ status: 202, contentType: 'application/json', headers: corsHeaders, body: JSON.stringify({ message: NEUTRAL }) });
  });
  await page.goto('/dang-nhap');
  await page.getByRole('link', { name: 'Quên mật khẩu?' }).click();
  await expect(page.getByRole('heading', { name: 'Quên mật khẩu?' })).toBeVisible();
  await expect(page.getByText('Bản demo')).toHaveCount(0);
  await page.getByLabel('Email', { exact: true }).fill('khong-phai-email');
  await page.getByRole('button', { name: 'Yêu cầu đặt lại mật khẩu' }).click();
  await expect(page.locator('#email-error')).toContainText('Vui lòng nhập địa chỉ email hợp lệ.');
  expect(submitted).toEqual([]);
  await page.getByLabel('Email', { exact: true }).fill('  An@Example.com ');
  await page.getByRole('button', { name: 'Yêu cầu đặt lại mật khẩu' }).click();
  await expect(page.getByRole('status')).toHaveText(NEUTRAL);
  expect(submitted).toEqual([{ email: 'An@Example.com' }]);
});

test('forgot password shows field errors from the API and network failures (mock API)', async ({ page }) => {
  let calls = 0;
  await stubApi(page, '/auth/password-resets', async route => {
    calls += 1;
    if (calls === 1) return route.fulfill(problem(400, 'VALIDATION_FAILED', { errors: [{ field: 'email', message: 'Email tối đa 255 ký tự.' }] }));
    return route.abort('connectionrefused');
  });
  await page.goto('/quen-mat-khau');
  await page.getByLabel('Email', { exact: true }).fill('an@example.com');
  await page.getByRole('button', { name: 'Yêu cầu đặt lại mật khẩu' }).click();
  await expect(page.locator('#email-error')).toContainText('Email tối đa 255 ký tự.');
  await page.getByRole('button', { name: 'Yêu cầu đặt lại mật khẩu' }).click();
  await expect(page.getByRole('alert')).toContainText('Không kết nối được máy chủ');
});

test('reset link sets the new password once and removes the token from the address bar (mock API)', async ({ page }) => {
  const submitted: unknown[] = [];
  await stubApi(page, '/auth/password-resets/confirm', async route => {
    submitted.push(route.request().postDataJSON());
    await route.fulfill({ status: 204, headers: corsHeaders });
  });
  await page.goto('/dat-lai-mat-khau?token=tok_123-abc');
  await expect(page).toHaveURL(/\/dat-lai-mat-khau$/);
  await expect(page.getByText('hiệu lực 15 phút')).toBeVisible();
  await expect(page.getByText('Bản demo')).toHaveCount(0);
  await page.getByLabel('Mật khẩu mới', { exact: true }).fill('short');
  await page.getByLabel('Xác nhận mật khẩu').fill('short');
  await page.getByRole('button', { name: 'Lưu mật khẩu mới' }).click();
  await expect(page.getByText('Mật khẩu cần ít nhất 8 ký tự.')).toBeVisible();
  await page.getByLabel('Mật khẩu mới', { exact: true }).fill('NewPass123');
  await page.getByLabel('Xác nhận mật khẩu').fill('NewPass124');
  await page.getByRole('button', { name: 'Lưu mật khẩu mới' }).click();
  await expect(page.getByText('Mật khẩu xác nhận chưa khớp.')).toBeVisible();
  expect(submitted).toEqual([]);
  await page.getByLabel('Xác nhận mật khẩu').fill('NewPass123');
  await page.getByRole('button', { name: 'Lưu mật khẩu mới' }).click();
  await expect(page.getByRole('status')).toContainText('Mật khẩu của bạn đã được cập nhật.');
  expect(submitted).toEqual([{ token: 'tok_123-abc', newPassword: 'NewPass123', confirmPassword: 'NewPass123' }]);
  await expect(page.getByRole('button', { name: 'Lưu mật khẩu mới' })).toHaveCount(0);
  await page.getByRole('link', { name: 'Đến trang đăng nhập' }).click();
  await expect(page).toHaveURL(/\/dang-nhap$/);
});

test('an invalid or expired reset link offers a new link (mock API)', async ({ page }) => {
  await stubApi(page, '/auth/password-resets/confirm', route => route.fulfill(problem(400, 'PASSWORD_RESET_TOKEN_INVALID')));
  await page.goto('/dat-lai-mat-khau?token=old-token');
  await page.getByLabel('Mật khẩu mới', { exact: true }).fill('NewPass123');
  await page.getByLabel('Xác nhận mật khẩu').fill('NewPass123');
  await page.getByRole('button', { name: 'Lưu mật khẩu mới' }).click();
  await expect(page.getByRole('alert')).toContainText('Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.');
  await expect(page.getByRole('button', { name: 'Lưu mật khẩu mới' })).toHaveCount(0);
  await page.getByRole('link', { name: 'Yêu cầu liên kết mới' }).click();
  await expect(page.getByRole('heading', { name: 'Quên mật khẩu?' })).toBeVisible();
});

test('opening the reset page without a token asks for a new link', async ({ page }) => {
  await page.goto('/dat-lai-mat-khau');
  await expect(page.getByRole('alert')).toContainText('Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.');
  await expect(page.getByLabel('Mật khẩu mới', { exact: true })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Yêu cầu liên kết mới' })).toHaveAttribute('href', '/quen-mat-khau');
});

test('reset errors follow the problem code and never keep the password (mock API)', async ({ page }) => {
  const responses = [
    problem(400, 'NEW_PASSWORD_SAME_AS_CURRENT', { detail: 'Mật khẩu mới phải khác mật khẩu hiện tại.' }),
    problem(400, 'VALIDATION_FAILED', { errors: [{ field: 'newPassword', message: 'Mật khẩu vượt quá 72 byte; ký tự có dấu chiếm nhiều byte hơn, hãy rút ngắn mật khẩu.' }] }),
    problem(500, 'INTERNAL_ERROR', { detail: 'Hệ thống gặp lỗi. Vui lòng thử lại sau.' }),
    problem(403, 'ACCOUNT_LOCKED', { detail: 'Tài khoản đã bị quản trị viên khóa.' }),
  ];
  await stubApi(page, '/auth/password-resets/confirm', route => route.fulfill(responses.shift()!));
  await page.goto('/dat-lai-mat-khau?token=tok');
  const save = async () => {
    await page.getByLabel('Mật khẩu mới', { exact: true }).fill('SamePass123');
    await page.getByLabel('Xác nhận mật khẩu').fill('SamePass123');
    await page.getByRole('button', { name: 'Lưu mật khẩu mới' }).click();
  };
  await save();
  await expect(page.locator('#password-error')).toHaveText('Mật khẩu mới phải khác mật khẩu hiện tại.');
  await expect(page.getByLabel('Mật khẩu mới', { exact: true })).toHaveValue('');
  await expect(page.getByLabel('Xác nhận mật khẩu')).toHaveValue('');
  await save();
  await expect(page.locator('#password-error')).toContainText('vượt quá 72 byte');
  await save();
  await expect(page.getByRole('alert').filter({ hasText: 'Hệ thống gặp lỗi' })).toBeVisible();
  await save();
  await expect(page.getByRole('alert')).toContainText('Tài khoản đã bị quản trị viên khóa. Vui lòng liên hệ quản trị viên.');
  await expect(page.getByRole('button', { name: 'Lưu mật khẩu mới' })).toHaveCount(0);
});
