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
      content: [{ id: 1, displayName: 'Trần Thảo', vegetarianType: 'Thuần chay', email: 'thao@example.com', status: 'PENDING', submittedAt: '2026-10-25T10:00:00' }],
      totalElements: 1,
      totalPages: 1,
    },
  }));
  await page.route('**/api/v1/admin/ingredients/search**', (route) => route.fulfill({
    json: {
      success: true,
      data: {
        totalElements: 42,
        totalPages: 1,
        content: [{ id: 10, name: 'Đậu phụ', groupName: 'Đậu & Đậu nành', verified: true, active: true }],
      },
    },
  }));
  await page.route('**/api/v1/admin/ingredients/options**', (route) => route.fulfill({
    json: {
      success: true,
      data: {
        groups: ['Đậu & Đậu nành'],
        sources: ['Viện Dinh Dưỡng'],
      },
    },
  }));

  await page.goto('/admin');
  await expect(page.getByRole('heading', { name: 'Bảng điều phối hệ thống' })).toBeVisible();
  await expect(page.getByText('Trần Thảo')).toBeVisible();
  await expect(page.getByText('42')).toBeVisible();

  await page.getByRole('button', { name: 'Tải lại số liệu' }).click();
  await expect(page.getByRole('link', { name: 'Xem hàng đợi' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Quản lý kho' })).toBeVisible();
});

test('transaction monitoring displays error state and retries on failure', async ({ page }) => {
  let callCount = 0;
  await page.route('**/api/v1/admin/transactions**', (route) => {
    callCount++;
    if (callCount === 1) {
      return route.fulfill({ status: 500, json: { message: 'Lỗi máy chủ' } });
    }
    return route.fulfill({
      json: {
        transactions: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
        summary: { total: 0, paid: 0, pending: 0, cancelled: 0, failed: 0, revenueVnd: 0 },
      },
    });
  });

  await page.goto('/admin/giao-dich');
  await expect(page.getByRole('alert')).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('heading', { name: 'Giám sát giao dịch gói AI' })).toBeVisible();
});

test('transaction monitoring validates date ranges, clears filters and handles export errors', async ({ page }) => {
  await page.route('**/api/v1/admin/transactions**', (route) => {
    const url = new URL(route.request().url());
    if (url.pathname.endsWith('/export')) {
      return route.fulfill({ status: 500, json: { message: 'Lỗi xuất file CSV' } });
    }
    return route.fulfill({
      json: {
        transactions: { content: [{ id: 101, orderCode: 'ORD-1', customerName: 'Test', customerEmail: 'test@example.com', tier: 'PRO', amountVnd: 99000, status: 'PAID', createdAt: '2026-11-01T10:00:00Z', paidAt: '2026-11-01T10:01:00Z' }], page: 0, size: 20, totalElements: 1, totalPages: 1 },
        summary: { total: 1, paid: 1, pending: 0, cancelled: 0, failed: 0, revenueVnd: 99000 },
      },
    });
  });

  await page.goto('/admin/giao-dich');
  const dateInputs = page.locator('input[type="date"]');
  await dateInputs.nth(0).fill('2026-11-05');
  await dateInputs.nth(1).fill('2026-11-01');
  await page.getByRole('button', { name: 'Áp dụng bộ lọc' }).click();
  await expect(page.getByText('Ngày bắt đầu không được sau ngày kết thúc.')).toBeVisible();

  await page.getByRole('button', { name: 'Xóa bộ lọc' }).click();
  await expect(page.getByText('Ngày bắt đầu không được sau ngày kết thúc.')).toHaveCount(0);

  await page.getByRole('button', { name: 'Xuất đối soát CSV' }).click();
  await expect(page.getByRole('alert')).toBeVisible();
});

test('admin dashboard handles empty queues and empty ingredient catalog', async ({ page }) => {
  await page.route('**/api/v1/admin/expert-applications**', (route) => route.fulfill({
    json: { content: [], totalElements: 0, totalPages: 0 },
  }));
  await page.route('**/api/v1/admin/ingredients/search**', (route) => route.fulfill({
    json: { success: true, data: { totalElements: 0, totalPages: 0, content: [] } },
  }));
  await page.route('**/api/v1/admin/ingredients/options**', (route) => route.fulfill({
    json: { success: true, data: { groups: [], sources: [] } },
  }));

  await page.goto('/admin');
  await expect(page.getByText('Đã xử lý xong')).toBeVisible();
  await expect(page.getByText('Hàng đợi trống')).toBeVisible();
});

test('admin dashboard displays error alert when data loading fails', async ({ page }) => {
  await page.route('**/api/v1/admin/expert-applications**', (route) => route.fulfill({
    status: 500,
    json: { message: 'Lỗi tải đơn xét duyệt' },
  }));

  await page.goto('/admin');
  await expect(page.getByRole('alert')).toBeVisible();
});
