import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

// The transaction page remains a preview. Account management has dedicated API-backed coverage.
test.beforeEach(async ({ page }) => {
  await seedDemoSession(page, 'ADMIN');
  await page.route('**/api/v1/admin/**', route => route.fulfill({ json: {} }));
});

for (const [status, label] of [['SUCCESS', 'Hoàn tất giao dịch'], ['PENDING', 'Chờ thanh toán'], ['FAILED', 'Giao dịch hết hạn']] as const) {
  test(`transaction preview filters ${status} and opens its invoice`, async ({ page }) => {
    await page.goto('/admin/giao-dich');
    await expect(page.getByRole('status').filter({ hasText: 'Giao diện minh họa' })).toBeVisible();
    await page.getByRole('combobox').nth(1).selectOption(status);
    const row = page.getByRole('row').nth(1);
    await row.getByRole('button', { name: 'Xem hóa đơn', exact: true }).click();
    await expect(page.getByRole('heading', { name: /^Chi tiết giao dịch/ })).toBeVisible();
    await expect(page.getByText(label, { exact: true }).last()).toBeVisible();
    await page.getByRole('button', { name: 'Đóng', exact: true }).click();
    await page.getByRole('combobox').nth(1).selectOption('ALL');
    for (const tier of ['PLUS', 'PRO']) {
      await page.getByRole('combobox').nth(0).selectOption(tier);
      await expect(page.getByRole('row').nth(1)).toContainText(`Gói ${tier}`);
    }
    await page.getByRole('combobox').nth(0).selectOption('ALL');
    await page.getByPlaceholder('Tìm theo mã giao dịch, email khách hàng hoặc mã đối soát...').fill('khong-co-giao-dich');
    await expect(page.getByRole('row')).toHaveCount(2);
    await expect(page.getByRole('row').nth(1)).toContainText(/Không/);
  });
}
