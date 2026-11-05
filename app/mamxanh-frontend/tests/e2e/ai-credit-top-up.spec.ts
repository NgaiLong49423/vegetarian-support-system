import type { Page } from '@playwright/test';
import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

const creditBalance = {
  balance: 125.5,
  creditsPer1000Vnd: 1000,
  minimumTopUpVnd: 10_000,
  maximumTopUpVnd: 500_000,
};

async function openTopUpPage(page: Page) {
  await seedDemoSession(page);
  await page.route('**/api/v1/subscriptions/my-ai-credit', route => route.fulfill({ json: creditBalance }));
  await page.goto('/nap-ai-credit');
  await expect(page.getByRole('heading', { name: 'Nạp AI Credit' })).toBeVisible();
}

test.describe('[FR-64] Nạp AI Credit', () => {
  test('AC-64.10: Hiển thị số dư, quy đổi preview và chặn số tiền ngoài min/max', async ({ page }) => {
    await openTopUpPage(page);

    await expect(page.getByText('125,5 Credit', { exact: true })).toBeVisible();
    await expect(page.getByText('10.000 Credit', { exact: true })).toBeVisible();

    const amount = page.getByRole('spinbutton');
    const checkout = page.getByRole('button', { name: 'Nạp tiền qua payOS' });
    await amount.fill('9999');
    await expect(checkout).toBeDisabled();
    await amount.fill('500001');
    await expect(checkout).toBeDisabled();

    await amount.fill('25000');
    await expect(page.getByText('25.000 Credit', { exact: true })).toBeVisible();
    await expect(checkout).toBeEnabled();
  });

  test('AC-64.1/64.10: Tạo đơn theo số tiền đã chọn và điều hướng tới payOS', async ({ page }) => {
    await openTopUpPage(page);
    await page.getByRole('spinbutton').fill('25000');

    let requestedAmount: unknown;
    await page.route('**/api/v1/subscriptions/credit-checkout', async route => {
      requestedAmount = JSON.parse(route.request().postData() || '{}');
      await route.fulfill({ status: 201, json: {
        orderCode: 1728038400123,
        amountVnd: 25_000,
        creditAmount: 25_000,
        creditsPer1000Vnd: 1000,
        expiresAt: '2026-10-31T00:15:00',
        checkoutUrl: 'https://pay.payos.vn/web/test-top-up',
        qrCode: 'test-qr',
      } });
    });
    await page.route('https://pay.payos.vn/**', route => route.fulfill({
      contentType: 'text/html', body: '<h1>Trang thanh toán thử có kiểm soát</h1>',
    }));

    await page.getByRole('button', { name: 'Nạp tiền qua payOS' }).click();

    await expect(page).toHaveURL('https://pay.payos.vn/web/test-top-up');
    expect(requestedAmount).toEqual({ amountVnd: 25_000 });
  });

  test('AC-64.8: Hiển thị trạng thái đối soát và không báo Credit đã cộng khi thanh toán muộn', async ({ page }) => {
    await seedDemoSession(page);
    await page.route('**/api/v1/subscriptions/transactions/123', route => route.fulfill({ json: {
      orderCode: '123',
      tier: 'AI_CREDIT',
      amountVnd: 25_000,
      status: 'NEEDS_RECONCILIATION',
      activated: false,
      paidAt: '2026-10-31T00:16:00Z',
      subscription: {
        tier: 'FREE', status: 'ACTIVE', active: true, startsAt: null, endsAt: null, unlockedFeatures: [],
      },
      productType: 'AI_CREDIT',
      creditAmount: 25_000,
      creditsPer1000Vnd: 1000,
    } }));

    await page.goto('/payment/success?orderCode=123&status=PAID');

    await expect(page.getByRole('heading', { name: 'Giao dịch đang được đối soát' })).toBeVisible();
    await expect(page.getByText(/AI Credit chưa được cộng tự động/)).toBeVisible();
    await expect(page.getByText(/25\.000 AI Credit đã được cộng/)).toHaveCount(0);
  });

  test('AC-64.8: Hiển thị Credit đã cộng sau khi máy chủ xác nhận đơn thành công', async ({ page }) => {
    await seedDemoSession(page);
    await page.route('**/api/v1/subscriptions/transactions/123', route => route.fulfill({ json: {
      orderCode: '123',
      tier: 'AI_CREDIT',
      amountVnd: 25_000,
      status: 'PAID',
      activated: true,
      paidAt: '2026-10-31T00:14:00Z',
      subscription: {
        tier: 'FREE', status: 'ACTIVE', active: true, startsAt: null, endsAt: null, unlockedFeatures: [],
      },
      productType: 'AI_CREDIT',
      creditAmount: 25_000,
      creditsPer1000Vnd: 1000,
    } }));

    await page.goto('/payment/success?orderCode=123&status=PAID');

    await expect(page.getByRole('heading', { name: 'Thanh toán thành công!' })).toBeVisible();
    await expect(page.getByText('25.000 AI Credit đã được cộng vào ví của bạn.')).toBeVisible();
  });
});
