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
