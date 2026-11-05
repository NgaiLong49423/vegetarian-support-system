import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

test.beforeEach(async ({ page }) => {
  await seedDemoSession(page, 'ADMIN');
  await page.route('**/api/v1/admin/**', (route) => route.fulfill({ json: {} }));
});

test('transaction monitoring opens the API-backed admin page', async ({ page }) => {
  await page.route('**/api/v1/admin/transactions**', (route) => route.fulfill({
    json: {
      transactions: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
      summary: { total: 0, paid: 0, pending: 0, cancelled: 0, failed: 0, revenueVnd: 0 },
    },
  }));

  await page.goto('/admin/giao-dich');

  await expect(page.getByRole('heading', { name: 'Giám sát giao dịch gói AI', exact: true })).toBeVisible();
  await expect(page.getByText('Giao diện minh họa', { exact: false })).toHaveCount(0);
});

test('admin dashboard renders real metrics, quick links and reloads data', async ({ page }) => {
  await page.route('**/api/v1/admin/expert-applications**', (route) => route.fulfill({
    json: {
      content: [{ id: 1, applicantName: 'Trần Thảo', status: 'PENDING', submittedAt: '2026-10-25T10:00:00' }],
      totalElements: 1,
      totalPages: 1,
    },
  }));
  await page.route('**/api/v1/admin/ingredients/search**', (route) => route.fulfill({
    json: {
      totalElements: 42,
      totalPages: 1,
      content: [{ id: 10, name: 'Đậu phụ', groupName: 'Đậu & Đậu nành', verified: true, active: true }],
    },
  }));
  await page.route('**/api/v1/admin/ingredients/options**', (route) => route.fulfill({
    json: {
      groups: [{ id: 1, name: 'Đậu & Đậu nành' }],
      sources: [{ id: 1, name: 'Viện Dinh Dưỡng' }],
    },
  }));

  await page.goto('/admin');
  await expect(page.getByRole('heading', { name: 'Bảng điều phối hệ thống' })).toBeVisible();
  await expect(page.getByText('Trần Thảo')).toBeVisible();
  await expect(page.getByText('Đậu phụ')).toBeVisible();

  await page.getByRole('button', { name: 'Tải lại số liệu' }).click();
  await expect(page.getByRole('link', { name: 'Xem hàng đợi' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Quản lý kho' })).toBeVisible();
});
