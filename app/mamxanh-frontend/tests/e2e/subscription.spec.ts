import type { Page } from '@playwright/test';
import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

const mockPlans = [
  {
    tier: 'FREE',
    name: 'FREE',
    priceVnd: 0,
    billingPeriod: 'Hàng tháng',
    description: 'Dùng các chức năng AI phù hợp với vai trò và điều kiện tài khoản',
    features: ['Các chức năng AI theo vai trò và điều kiện nghiệp vụ', '50.000 token mỗi tháng'],
  },
  {
    tier: 'PLUS',
    name: 'PLUS',
    priceVnd: 99000,
    billingPeriod: 'Hàng tháng',
    description: 'Tăng hạn mức token AI theo tháng',
    features: ['Các chức năng AI theo vai trò và điều kiện nghiệp vụ', '200.000 token mỗi tháng'],
  },
  {
    tier: 'PRO',
    name: 'PRO',
    priceVnd: 199000,
    billingPeriod: 'Hàng tháng',
    description: 'Tăng hạn mức token AI theo tháng',
    features: [
      'Các chức năng AI theo vai trò và điều kiện nghiệp vụ',
      '500.000 token mỗi tháng',
    ],
  },
];

const mockFreeSubscription = {
  tier: 'FREE',
  status: 'ACTIVE',
  active: true,
  startsAt: null,
  endsAt: null,
  unlockedFeatures: ['AI Chatbot cơ bản', 'Gợi ý món chay cơ bản'],
};

const mockPlusSubscription = {
  tier: 'PLUS',
  status: 'ACTIVE',
  active: true,
  startsAt: '2026-10-04T12:00:00Z',
  endsAt: '2026-11-04T12:00:00Z',
  unlockedFeatures: ['AI Chatbot hỗ trợ ẩm thực chay', 'AI hỗ trợ soạn bài viết và công thức', 'AI gợi ý biến tấu món chay sáng tạo'],
};

const mockTransactions = [
  {
    paymentTransactionId: 1,
    orderCode: '1728038400123',
    tier: 'PLUS',
    amountVnd: 49000,
    status: 'PAID',
    createdAt: '2026-10-04T12:00:00Z',
    paidAt: '2026-10-04T12:01:00Z',
    endsAt: '2026-11-03T12:01:00Z',
  },
  {
    paymentTransactionId: 2,
    orderCode: '1728038400999',
    tier: 'PRO',
    amountVnd: 99000,
    status: 'CANCELLED',
    createdAt: '2026-10-04T12:05:00Z',
    paidAt: null,
    endsAt: null,
  },
];

async function setupMockSubscriptionApis(page: Page, subscriptionData = mockFreeSubscription) {
  await seedDemoSession(page);

  await page.route('**/api/v1/subscriptions/plans', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockPlans),
    });
  });

  await page.route('**/api/v1/subscriptions/my-subscription', async (route) => {
    expect(route.request().headers().authorization).toBe('Bearer header.payload.signature');
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(subscriptionData),
    });
  });

  await page.route('**/api/v1/subscriptions/transactions/*', async route => {
    const orderCode = new URL(route.request().url()).pathname.split('/').pop()!;
    const cancelled = orderCode === '1728038400999';
    return route.fulfill({ json: { orderCode, tier: 'PLUS', amountVnd: 99000,
      status: cancelled ? 'CANCELLED' : 'PAID', activated: !cancelled,
      paidAt: cancelled ? null : '2026-10-17T00:00:00', subscription: subscriptionData } });
  });

  await page.route('**/api/v1/subscriptions/transactions', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockTransactions),
    });
  });
}

test.describe('[FR-13] Gói AI & Thanh toán payOS', () => {
  test('AC-13.1: Trang bảng giá hiển thị chính xác 3 gói FREE 0 đ, PLUS 99.000 đ, PRO 199.000 đ', async ({ page }) => {
    await setupMockSubscriptionApis(page, mockFreeSubscription);
    await page.goto('/goi-ai');

    // Heading
    await expect(page.getByRole('heading', { name: /Nâng cấp gói AI/i })).toBeVisible();

    // Exactly 3 plan headings
    await expect(page.getByRole('heading', { name: 'FREE', exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'PLUS', exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'PRO', exact: true })).toBeVisible();

    // Prices
    await expect(page.getByText('0 VNĐ / tháng', { exact: true })).toBeVisible();
    await expect(page.getByText('99.000 VNĐ / tháng', { exact: true })).toBeVisible();
    await expect(page.getByText('199.000 VNĐ / tháng', { exact: true })).toBeVisible();

    // No annual plans, coupons, or trial packages
    await expect(page.getByText(/gói năm|coupon|khuyến mãi|dùng thử/i)).toHaveCount(0);
  });

  test('AC-13.7: Hiển thị gói Free là "Miễn phí" không có ngày hết hạn, và gói Plus hiển thị ngày hết hạn DD/MM/YYYY', async ({
    page,
  }) => {
    // 1. Check with FREE subscription
    await setupMockSubscriptionApis(page, mockFreeSubscription);
    await page.goto('/goi-ai');
    await expect(page.getByText('Miễn phí', { exact: true })).toBeVisible();
    await expect(page.getByText(/Hết hạn ngày:/i)).toHaveCount(0);

    // 2. Check with PLUS subscription with endsAt
    await setupMockSubscriptionApis(page, mockPlusSubscription);
    await page.goto('/goi-ai');
    await expect(page.getByText('Gói PLUS', { exact: true })).toBeVisible();
    await expect(page.getByText('Hết hạn ngày: 04/11/2026')).toBeVisible();
  });

  test('AC-13.2: Nhấn nâng cấp gói gọi API checkout và chuyển hướng tới payOS', async ({ page }) => {
    await setupMockSubscriptionApis(page, mockFreeSubscription);

    let checkoutPayload: unknown = null;
    await page.route('**/api/v1/subscriptions/checkout', async (route) => {
      expect(route.request().headers().authorization).toBe('Bearer header.payload.signature');
      checkoutPayload = JSON.parse(route.request().postData() || '{}');
      await route.fulfill({
        status: 201,
        contentType: 'application/json',
        body: JSON.stringify({
          orderCode: 1728038400123,
          checkoutUrl: 'https://pay.payos.vn/web/test-checkout-url',
          amountVnd: 99000,
          tier: 'PLUS',
          qrCode: 'test-qr',
        }),
      });
    });

    await page.route('https://pay.payos.vn/**', route => route.fulfill({ contentType: 'text/html', body: '<h1>Trang thanh toán thử có kiểm soát</h1>' }));

    await page.goto('/goi-ai');

    // Click "Nâng cấp lên PLUS"
    const upgradePlusButton = page.getByRole('button', { name: /Nâng cấp lên PLUS/i });
    await expect(upgradePlusButton).toBeVisible();
    await upgradePlusButton.click();

    await expect(page).toHaveURL('https://pay.payos.vn/web/test-checkout-url');

    // Verify request payload had tier PLUS
    expect(checkoutPayload).toMatchObject({
      tier: 'PLUS',
    });
  });

  test('AC-13.5: Thanh toán thành công chuyển hướng đến trang kết quả thành công và hiển thị gói mới', async ({ page }) => {
    await setupMockSubscriptionApis(page, mockPlusSubscription);
    await page.goto('/payment/success?orderCode=1728038400123&status=PAID');

    await expect(page.getByRole('heading', { name: /Thanh toán thành công/i })).toBeVisible();
    await expect(page.getByText('#1728038400123')).toBeVisible();
    await expect(page.getByText('Gói PLUS', { exact: true })).toBeVisible();
    await expect(page.getByRole('link', { name: /Xem hồ sơ/i })).toBeVisible();
  });

  test('AC-13.6: Huỷ thanh toán quay lại hệ thống bảo toàn quyền lợi gói cũ', async ({ page }) => {
    await setupMockSubscriptionApis(page, mockPlusSubscription);
    await page.goto('/payment/cancel?orderCode=1728038400999&status=CANCELLED');

    // Verify header and message
    await expect(page.getByRole('heading', { name: /Giao dịch chưa hoàn tất/i })).toBeVisible();
    await expect(page.getByText(/Quyền lợi tài khoản của bạn được bảo toàn/i)).toBeVisible();
    await expect(page.getByText('#1728038400999')).toBeVisible();

    // Verify user retains PLUS tier
    await expect(page.getByText('Gói PLUS', { exact: true })).toBeVisible();
  });

  test('Lịch sử giao dịch hiển thị mã đơn, số tiền, gói và trạng thái', async ({ page }) => {
    await setupMockSubscriptionApis(page, mockPlusSubscription);
    await page.goto('/giao-dich');

    await expect(page.getByRole('heading', { name: 'Lịch sử giao dịch' })).toBeVisible();
    await expect(page.getByText('#1728038400123')).toBeVisible();
    await expect(page.getByText('49.000 đ')).toBeVisible();
    await expect(page.getByText('Đã thanh toán')).toBeVisible();

    await expect(page.getByText('#1728038400999')).toBeVisible();
    await expect(page.getByText('99.000 đ')).toBeVisible();
    await expect(page.getByText('Đã hủy')).toBeVisible();
  });
});

for (const fallback of [false, true]) {
  test(`Ma trận quyền mới trên bảng giá ${fallback ? 'dự phòng trên mobile' : 'từ API trên desktop'}`, async ({ page }, testInfo) => {
    await setupMockSubscriptionApis(page, mockFreeSubscription);
    if (fallback) {
      await page.setViewportSize({ width: 390, height: 844 });
      await page.route('**/api/v1/subscriptions/plans', route => route.abort());
    }
    await page.goto('/goi-ai');
    const plus = page.locator('[data-tier="PLUS"]');
    const pro = page.locator('[data-tier="PRO"]');
    const free = page.locator('[data-tier="FREE"]');
    await expect(free).toContainText('50.000 token mỗi tháng');
    await expect(plus).toContainText('99.000');
    await expect(plus).toContainText('Các chức năng AI theo vai trò và điều kiện nghiệp vụ');
    await expect(plus).toContainText('200.000 token mỗi tháng');
    await expect(pro).toContainText('199.000');
    await expect(pro).toContainText('Các chức năng AI theo vai trò và điều kiện nghiệp vụ');
    await expect(pro).toContainText('500.000 token mỗi tháng');
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
    await page.screenshot({ path: testInfo.outputPath(`plans-${fallback ? 'mobile-fallback' : 'desktop-api'}.png`), fullPage: true, animations: 'disabled' });
  });
}
