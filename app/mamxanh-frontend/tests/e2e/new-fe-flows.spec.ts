import { expect, test } from '@playwright/test';

test('recipe discussion supports reply and keeps replies after parent deletion', async ({ page }) => {
  await page.goto('/cong-thuc/dau-hu-non-sot-nam-dong-co');
  await page.getByLabel('Viết bình luận').fill('Món này có thể dùng nấm khác không?');
  await page.getByRole('button', { name: 'Gửi bình luận' }).click();
  await expect(page.getByText('Món này có thể dùng nấm khác không?')).toBeVisible();
  await page.getByRole('button', { name: 'Trả lời' }).click();
  await page.getByLabel('Viết bình luận').fill('Tôi đã thử nấm đùi gà.');
  await page.getByRole('button', { name: 'Gửi trả lời' }).click();
  await page.getByRole('button', { name: 'Xóa' }).first().click();
  await expect(page.getByText('Bình luận này đã bị xóa bởi người dùng')).toBeVisible();
  await expect(page.getByText('Tôi đã thử nấm đùi gà.')).toBeVisible();
});

test('meal serving selector accepts half portions', async ({ page }) => {
  await page.goto('/ke-hoach');
  const selector = page.getByLabel(/Khẩu phần .* bữa /).first();
  await selector.selectOption('1.5');
  await expect(selector).toHaveValue('1.5');
});

test('recipe ingredients scale from the author serving count', async ({ page }) => {
  await page.goto('/cong-thuc/pho-chay-nam-huong-rung');
  await expect(page.getByRole('heading', { name: 'Dinh dưỡng cho 1 khẩu phần (8 chỉ tiêu minh họa)' })).toBeVisible();
  await expect(page.getByText('Số liệu đang là mẫu giao diện cho một khẩu phần')).toBeVisible();
  await expect(page.getByText('Công thức gốc: 3 phần')).toBeVisible();
  await page.getByRole('spinbutton', { name: 'Số khẩu phần' }).fill('6');
  await expect(page.getByText('160 g', { exact: true })).toBeVisible();
  await expect(page.getByText('800 g', { exact: true })).toBeVisible();
  await expect(page.getByText('2 gói', { exact: true })).toBeVisible();
});

test('avatar menu shows the demo plan and links to account features', async ({ page }) => {
  await page.goto('/dang-nhap');
  await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
  await page.getByRole('button', { name: 'Tài khoản Lan Anh, gói AI FREE demo' }).click();
  await page.getByRole('link', { name: 'Hồ sơ của tôi', exact: true }).click();
  await expect(page.getByRole('link', { name: 'Hồ sơ dinh dưỡng & BMI' })).toHaveCount(0);
  await page.getByRole('button', { name: 'Tài khoản Lan Anh, gói AI FREE demo' }).click();
  await expect(page.getByText('Gói AI hiện tại: FREE (dữ liệu demo)')).toBeVisible();
  await page.getByRole('link', { name: 'Nâng cấp gói AI' }).click();
  await expect(page).toHaveURL(/\/goi-ai$/);
  await expect(page.getByRole('heading', { name: 'Nâng cấp gói AI' })).toBeVisible();
  await expect(page.getByText('Gói hiện tại: FREE (demo)')).toBeVisible();
});

test('mobile account links remain reachable from the menu', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/dang-nhap');
  await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
  await page.getByRole('button', { name: 'Menu' }).click();
  await page.getByRole('link', { name: 'Lịch sử giao dịch' }).click();
  await expect(page).toHaveURL(/\/giao-dich$/);
});

test('recipe report form validates the six reason groups without claiming a server submission', async ({ page }) => {
  await page.goto('/cong-thuc/pho-chay-nam-huong-rung');
  await page.getByRole('button', { name: 'Báo cáo công thức' }).click();
  await expect(page.getByRole('radio')).toHaveCount(6);
  await page.getByRole('button', { name: 'Kiểm tra biểu mẫu' }).click();
  await expect(page.getByRole('alert')).toContainText('chọn một lý do');
  await page.getByRole('radio', { name: 'Khác' }).check();
  await page.getByLabel('Mô tả bổ sung').fill('Ngắn');
  await page.getByRole('button', { name: 'Kiểm tra biểu mẫu' }).click();
  await expect(page.getByRole('alert')).toContainText('10 đến 500');
  await page.getByLabel('Mô tả bổ sung').fill('Nội dung cần được kiểm tra thêm.');
  await page.getByRole('button', { name: 'Kiểm tra biểu mẫu' }).click();
  await expect(page.getByText('Đã kiểm tra biểu mẫu. Chưa gửi báo cáo tới quản trị viên.')).toBeVisible();
});

test('BMI result is gated and plan/history pages do not simulate payment', async ({ page }) => {
  await page.goto('/ho-so/dinh-duong');
  await page.getByRole('spinbutton', { name: 'Tuổi' }).fill('25');
  await page.getByLabel('Chiều cao').fill('170');
  await page.getByLabel('Cân nặng').fill('65');
  await page.getByRole('button', { name: 'Tính BMI tham khảo' }).click();
  await expect(page.getByRole('alert')).toContainText('xác nhận phạm vi');
  await page.getByRole('checkbox').check();
  await page.getByRole('button', { name: 'Tính BMI tham khảo' }).click();
  await expect(page.getByText('22.5', { exact: true })).toBeVisible();

  await page.goto('/goi-ai');
  await expect(page.getByText('49.000')).toBeVisible();
  await expect(page.getByText('99.000')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Thanh toán chưa khả dụng' }).first()).toBeDisabled();
  await page.getByRole('link', { name: 'Xem lịch sử giao dịch' }).click();
  await expect(page.getByText('Chưa có dữ liệu giao dịch')).toBeVisible();
});
