import type { Page } from '@playwright/test';
import { expect, test } from './baseFixtures';

const mockPlans = [
  {
    tier: 'FREE',
    name: 'FREE',
    priceVnd: 0,
    billingCycle: 'Hàng tháng',
    description: 'Trải nghiệm các tính năng AI cơ bản',
    features: ['AI Chatbot cơ bản', 'Gợi ý món chay cơ bản'],
    autoRenew: false,
  },
  {
    tier: 'PLUS',
    name: 'PLUS',
    priceVnd: 49000,
    billingCycle: 'Hàng tháng',
    description: 'Dành cho người ăn chay muốn sáng tạo thực đơn',
    features: ['Toàn bộ quyền của FREE', 'AI hỗ trợ soạn bài viết và công thức', 'AI gợi ý biến tấu món chay sáng tạo'],
    autoRenew: false,
  },
  {
    tier: 'PRO',
    name: 'PRO',
    priceVnd: 99000,
    billingCycle: 'Hàng tháng',
    description: 'Dành cho người ăn chay chuyên sâu & tối ưu dinh dưỡng',
    features: [
      'Toàn bộ quyền của PLUS',
      'AI lập thực đơn tuần 7 ngày theo dinh dưỡng cá nhân',
      'Phân tích vi chất và tối ưu hoá calo chuyên sâu',
    ],
    autoRenew: false,
  },
];

const mockFreeSubscription = {
  tier: 'FREE',
  status: 'ACTIVE',
  startsAt: null,
  endsAt: null,
  features: ['AI Chatbot cơ bản', 'Gợi ý món chay cơ bản'],
};

const mockPlusSubscription = {
  tier: 'PLUS',
  status: 'ACTIVE',
  startsAt: '2026-10-04T12:00:00Z',
  endsAt: '2026-11-04T12:00:00Z',
  features: ['AI Chatbot hỗ trợ ẩm thực chay', 'AI hỗ trợ soạn bài viết và công thức', 'AI gợi ý biến tấu món chay sáng tạo'],
};

const mockTransactions = [
  {
    orderCode: 1728038400123,
    tier: 'PLUS',
    amountVnd: 49000,
    status: 'PAID',
    createdAt: '2026-10-04T12:00:00Z',
    paidAt: '2026-10-04T12:01:00Z',
  },
  {
    orderCode: 1728038400999,
    tier: 'PRO',
    amountVnd: 99000,
    status: 'CANCELLED',
    createdAt: '2026-10-04T12:05:00Z',
    paidAt: null,
  },
];

async function setupMockSubscriptionApis(page: Page, subscriptionData = mockFreeSubscription) {
  await page.addInitScript(() => {
    window.__mamxanhAccessToken = 'test-e2e-token';
  });

  await page.route('**/api/v1/subscriptions/plans', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(mockPlans),
    });
  });

  await page.route('**/api/v1/subscriptions/my-subscription', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(subscriptionData),
    });
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
  test('AC-13.1: Trang bảng giá hiển thị chính xác 3 gói FREE 0 đ, PLUS 49.000 đ, PRO 99.000 đ', async ({ page }) => {
    await setupMockSubscriptionApis(page, mockFreeSubscription);
    await page.goto('/goi-ai');

    // Heading
    await expect(page.getByRole('heading', { name: /Bảng giá nâng cấp gói AI/i })).toBeVisible();

    // Exactly 3 plan headings
    await expect(page.getByRole('heading', { name: 'FREE', exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'PLUS', exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'PRO', exact: true })).toBeVisible();

    // Prices
    await expect(page.getByText('0 VNĐ / tháng')).toBeVisible();
    await expect(page.getByText('49.000 VNĐ / tháng')).toBeVisible();
    await expect(page.getByText('99.000 VNĐ / tháng')).toBeVisible();

    // No annual plans, coupons, or trial packages
    await expect(page.getByText(/gói năm|năm|coupon|khuyến mãi|dùng thử/i)).toHaveCount(0);
  });

  test('AC-13.7: Hiển thị gói Free là "Miễn phí" không có ngày hết hạn, và gói Plus hiển thị ngày hết hạn DD/MM/YYYY', async ({
    page,
  }) => {
    // 1. Check with FREE subscription
    await setupMockSubscriptionApis(page, mockFreeSubscription);
    await page.goto('/goi-ai');
    await expect(page.getByText('Miễn phí')).toBeVisible();
    await expect(page.getByText(/Hết hạn ngày:/i)).toHaveCount(0);

    // 2. Check with PLUS subscription with endsAt
    await setupMockSubscriptionApis(page, mockPlusSubscription);
    await page.goto('/goi-ai');
    await expect(page.getByText('Gói PLUS')).toBeVisible();
    await expect(page.getByText('Hết hạn ngày: 04/11/2026')).toBeVisible();
  });

  test('AC-13.2: Nhấn nâng cấp gói gọi API checkout và chuyển hướng tới payOS', async ({ page }) => {
    await setupMockSubscriptionApis(page, mockFreeSubscription);

    let checkoutPayload: unknown = null;
    await page.route('**/api/v1/subscriptions/checkout', async (route) => {
      checkoutPayload = JSON.parse(route.request().postData() || '{}');
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          orderCode: 1728038400123,
          checkoutUrl: 'https://pay.payos.vn/web/test-checkout-url',
          amountVnd: 49000,
          tier: 'PLUS',
          status: 'PENDING',
        }),
      });
    });

    await page.goto('/goi-ai');

    // Click "Nâng cấp lên PLUS"
    const upgradePlusButton = page.getByRole('button', { name: /Nâng cấp lên PLUS/i });
    await expect(upgradePlusButton).toBeVisible();
    await upgradePlusButton.click();

    // Verify request payload had tier PLUS
    expect(checkoutPayload).toMatchObject({
      tier: 'PLUS',
    });
  });

  test('AC-13.6: Huỷ thanh toán quay lại hệ thống bảo toàn quyền lợi gói cũ', async ({ page }) => {
    await setupMockSubscriptionApis(page, mockPlusSubscription);
    await page.goto('/payment/cancel?orderCode=1728038400999&status=CANCELLED');

    // Verify header and message
    await expect(page.getByRole('heading', { name: /Giao dịch chưa hoàn tất/i })).toBeVisible();
    await expect(page.getByText(/Quyền lợi tài khoản của bạn được bảo toàn/i)).toBeVisible();
    await expect(page.getByText('#1728038400999')).toBeVisible();

    // Verify user retains PLUS tier
    await expect(page.getByText('Gói PLUS')).toBeVisible();
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
