import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

// These screens are explicitly local previews, not evidence of Backend administration.
test.beforeEach(async ({ page }) => {
  await seedDemoSession(page, 'ADMIN');
  await page.route('**/api/v1/admin/**', route => route.fulfill({ json: {} }));
});

test('account preview filters combine and empty results do not expose unrelated accounts', async ({ page }) => {
  await page.goto('/admin/nguoi-dung');
  await expect(page.getByRole('status').filter({ hasText: 'Giao diện minh họa' })).toBeVisible();
  const search = page.getByPlaceholder('Tìm theo họ tên, email hoặc mã thành viên...');
  const rows = page.getByRole('row');
  await search.fill('USR-101');
  await expect(rows.filter({ hasText: 'Nguyễn Thị Lan' })).toBeVisible();
  await page.getByRole('combobox').nth(0).selectOption('USER');
  await expect(page.getByText('Không tìm thấy thành viên nào phù hợp với bộ lọc tìm kiếm.')).toBeVisible();
  await page.getByRole('combobox').nth(0).selectOption('EXPERT');
  await expect(rows.filter({ hasText: 'Nguyễn Thị Lan' })).toBeVisible();
  await search.fill('lan.nguyen');
  await expect(rows.filter({ hasText: 'Nguyễn Thị Lan' })).toBeVisible();
  await search.clear();
  await page.getByRole('combobox').nth(0).selectOption('ALL');
  await page.getByRole('combobox').nth(1).selectOption('LOCKED');
  await expect(rows.filter({ hasText: 'Phạm Văn Hải' })).toBeVisible();
  await expect(rows.filter({ hasText: 'Nguyễn Thị Lan' })).toHaveCount(0);
  await page.getByRole('combobox').nth(1).selectOption('ACTIVE');
  await expect(rows.filter({ hasText: 'Nguyễn Thị Lan' })).toBeVisible();
});

test('account details show the lock reason and can be closed without changing the preview', async ({ page }) => {
  await page.goto('/admin/nguoi-dung');
  const row = page.getByRole('row').filter({ hasText: 'Phạm Văn Hải' });
  await row.getByRole('button', { name: 'Xem chi tiết hồ sơ' }).click();
  await expect(page.getByRole('heading', { name: 'Hồ sơ chi tiết thành viên' })).toBeVisible();
  await expect(page.getByText('Tạm thời bị khóa', { exact: true })).toBeVisible();
  await expect(page.getByText('Lý do khóa tài khoản:', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Đóng', exact: true }).click();
  await expect(row.getByText('Đã khóa', { exact: true })).toBeVisible();
  await page.getByRole('row').filter({ hasText: 'Nguyễn Thị Lan' }).getByRole('button', { name: 'Xem chi tiết hồ sơ' }).click();
  await expect(page.getByText('Đang hoạt động', { exact: true }).last()).toBeVisible();
  await expect(page.getByText('Lý do khóa tài khoản:', { exact: true })).toHaveCount(0);
  await page.getByRole('button', { name: 'Đóng', exact: true }).click();
});

for (const reason of ['', 'Lý do kiểm thử bản xem trước']) {
  test(`lock/unlock stays local and reload restores the account (${reason ? 'custom reason' : 'default reason'})`, async ({ page }) => {
    const writes: string[] = [];
    page.on('request', request => {
      if (request.url().includes('/api/') && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method())) writes.push(request.url());
    });
    await page.goto('/admin/nguoi-dung');
    const row = page.getByRole('row').filter({ hasText: 'Nguyễn Thị Lan' });
    await row.getByRole('button', { name: 'Khóa tài khoản', exact: true }).click();
    await page.getByRole('button', { name: 'Hủy bỏ', exact: true }).click();
    await expect(row.getByText('Hoạt động', { exact: true })).toBeVisible();
    await row.getByRole('button', { name: 'Khóa tài khoản', exact: true }).click();
    if (reason) await page.getByPlaceholder('Nhập chi tiết vi phạm tiêu chuẩn cộng đồng, spam quảng cáo...').fill(reason);
    await page.getByRole('button', { name: 'Xác nhận khóa', exact: true }).click();
    await expect(row.getByText('Đã khóa', { exact: true })).toBeVisible();
    await row.getByRole('button', { name: 'Xem chi tiết hồ sơ' }).click();
    await expect(page.getByText(reason || 'Vi phạm tiêu chuẩn cộng đồng Mâm Xanh', { exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Đóng', exact: true }).click();
    await row.getByRole('button', { name: 'Mở khóa tài khoản', exact: true }).click();
    await page.getByRole('button', { name: 'Hủy bỏ', exact: true }).click();
    await expect(row.getByText('Đã khóa', { exact: true })).toBeVisible();
    await row.getByRole('button', { name: 'Mở khóa tài khoản', exact: true }).click();
    await page.getByRole('button', { name: 'Xác nhận mở khóa', exact: true }).click();
    await expect(row.getByText('Hoạt động', { exact: true })).toBeVisible();
    await row.getByRole('button', { name: 'Khóa tài khoản', exact: true }).click();
    await page.getByRole('button', { name: 'Xác nhận khóa', exact: true }).click();
    await page.reload();
    await expect(row.getByText('Hoạt động', { exact: true })).toBeVisible();
    expect(writes).toEqual([]);
  });
}

test('role changes can be previewed and reverted without mutating Backend authorization', async ({ page }) => {
  const writes: string[] = [];
  page.on('request', request => {
    if (request.url().includes('/api/') && request.method() !== 'GET') writes.push(request.url());
  });
  await page.goto('/admin/nguoi-dung');
  const row = page.getByRole('row').filter({ hasText: 'Trần Minh Đức' });
  await row.getByRole('button', { name: 'Phân quyền vai trò', exact: true }).click();
  await page.getByRole('button', { name: 'Hủy bỏ', exact: true }).click();
  await expect(row.getByText('Thành viên', { exact: true })).toBeVisible();
  for (const [role, label] of [['EXPERT', 'Chuyên gia'], ['ADMIN', 'Quản trị viên'], ['USER', 'Thành viên']] as const) {
    await row.getByRole('button', { name: 'Phân quyền vai trò', exact: true }).click();
    await page.getByRole('radio', { name: new RegExp(role) }).check();
    await page.getByRole('button', { name: 'Lưu phân quyền', exact: true }).click();
    await expect(row.getByText(label, { exact: true })).toBeVisible();
  }
  await page.reload();
  await expect(row.getByText('Thành viên', { exact: true })).toBeVisible();
  expect(writes).toEqual([]);
});
