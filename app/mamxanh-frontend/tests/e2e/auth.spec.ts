import { expect, test } from '@playwright/test';

test('guest can reach registration, validate fields and preview email verification', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('link', { name: 'Đăng ký', exact: true }).click();
  await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
  await expect(page.getByText('Tên hiển thị cần từ 3 đến 50 ký tự.')).toBeVisible();
  await expect(page.getByText('Vui lòng nhập địa chỉ email hợp lệ.')).toBeVisible();
  await page.getByLabel('Tên hiển thị').fill('Nguyễn An');
  await page.getByLabel('Email', { exact: true }).fill('an@example.com');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('DemoPass123!');
  await page.getByLabel('Xác nhận mật khẩu').fill('Different123!');
  await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
  await expect(page.getByText('Mật khẩu xác nhận chưa khớp.')).toBeVisible();
  await page.getByLabel('Xác nhận mật khẩu').fill('DemoPass123!');
  await page.getByRole('button', { name: 'Tạo tài khoản', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Chưa tạo tài khoản hoặc gửi email');
  await expect(page.getByLabel('Mật khẩu', { exact: true })).toHaveValue('');
  await page.getByRole('link', { name: 'Xem bước xác minh email' }).click();
  await expect(page.getByRole('heading', { name: 'Xác minh email của bạn' })).toBeVisible();
  await page.getByLabel('Email', { exact: true }).fill('an@example.com');
  await page.getByRole('button', { name: 'Yêu cầu gửi lại email' }).click();
  await expect(page.getByRole('status')).toContainText('chưa gửi email');
});

test('login does not create a fake session; demo entry and exit are explicit', async ({ page }) => {
  await page.goto('/dang-nhap');
  await page.getByLabel('Email', { exact: true }).fill('an@example.com');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('DemoPass123!');
  await page.getByRole('button', { name: 'Hiện mật khẩu', exact: true }).click();
  await expect(page.getByLabel('Mật khẩu', { exact: true })).toHaveAttribute('type', 'text');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('chưa tạo phiên tài khoản');
  await page.getByRole('button', { name: 'Tiếp tục với Google' }).click();
  await expect(page.getByRole('status')).toContainText('Google Login chưa được kết nối');
  await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
  await page.getByRole('button', { name: 'Tài khoản Lan Anh, gói AI FREE demo' }).click();
  await page.getByRole('button', { name: 'Thoát tài khoản demo' }).click();
  await expect(page).toHaveURL(/\/dang-nhap$/);
});

test('recovery and mobile layouts remain usable without claiming email delivery', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/');
  await page.getByRole('link', { name: 'Đăng nhập', exact: true }).click();
  await page.getByRole('link', { name: 'Quên mật khẩu?' }).click();
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
