import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

const free = { tier: 'FREE', status: 'ACTIVE', active: true, startsAt: null, endsAt: null,
  unlockedFeatures: ['AI Chatbot hỗ trợ ẩm thực chay'] };
const pro = { ...free, tier: 'PRO', startsAt: '2026-10-17T00:00:00', endsAt: '2026-11-16T00:00:00',
  unlockedFeatures: ['AI lập thực đơn tuần 7 ngày'] };
const result = (status: string, activated = false) => ({ orderCode: '123', tier: 'PRO', amountVnd: 99000,
  status, activated, paidAt: status === 'PAID' ? '2026-10-17T00:00:00' : null, subscription: activated ? pro : free });

for (const width of [360, 768, 1440]) {
  test(`Issue #142: delayed activation is verified and responsive at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 900 });
    await seedDemoSession(page);
    let requests = 0;
    await page.route('**/api/v1/subscriptions/transactions/123', route => {
      requests++;
      return route.fulfill({ json: requests === 1 ? result('PENDING') : result('PAID', true) });
    });
    await page.goto('/payment/success?orderCode=123&status=PAID');
    await expect(page.getByRole('heading', { name: 'Đang xác minh thanh toán', exact: true })).toBeVisible();
    await expect(page.getByText('Gói FREE', { exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Thanh toán thành công!', exact: true })).toBeVisible();
    await expect(page.getByText('Gói PRO', { exact: true })).toBeVisible();
    await expect(page.getByText('AI lập thực đơn tuần 7 ngày', { exact: false })).toBeVisible();
    await expect(page.getByText('Hạn sử dụng đến:', { exact: false })).toBeVisible();
    expect(requests).toBe(2);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.reload();
    await expect(page.getByRole('heading', { name: 'Thanh toán thành công!', exact: true })).toBeVisible();
  });
}

test('Issue #142: API error never reports success and supports keyboard retry', async ({ page }) => {
  await seedDemoSession(page);
  let failed = true;
  await page.route('**/api/v1/subscriptions/transactions/123', route => route.fulfill(failed
    ? { status: 503, json: { status: 503, code: 'PAYMENT_UNAVAILABLE', detail: 'Hiện chưa thể xác minh thanh toán.' } }
    : { json: result('PAID', true) }));
  await page.goto('/payment/success?orderCode=123&status=PAID');
  await expect(page.getByRole('heading', { name: 'Chưa thể xác minh thanh toán' })).toBeVisible();
  await expect(page.getByText('Thanh toán thành công!', { exact: true })).toHaveCount(0);
  failed = false;
  await page.getByRole('button', { name: 'Kiểm tra lại', exact: true }).focus();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Thanh toán thành công!', exact: true })).toBeVisible();
});

test('Issue #142: fake success URL and missing/invalid order do not grant success', async ({ page }) => {
  await seedDemoSession(page);
  let requests = 0;
  await page.route('**/api/v1/subscriptions/transactions/**', route => {
    requests++;
    return route.fulfill({ status: 404, json: { status: 404, code: 'PAYMENT_TRANSACTION_NOT_FOUND', detail: 'Không tìm thấy giao dịch.' } });
  });
  for (const query of ['', '?orderCode=not-a-number', '?orderCode=9007199254740992']) {
    await page.goto('/payment/success' + query);
    await expect(page.getByRole('heading', { name: 'Thiếu hoặc sai mã đơn thanh toán' })).toBeVisible();
    await expect(page.getByText('Thanh toán thành công!', { exact: true })).toHaveCount(0);
  }
  expect(requests).toBe(0);
  await page.goto('/payment/success?orderCode=123&status=PAID&mock=true');
  await expect(page.getByRole('heading', { name: 'Chưa thể xác minh thanh toán' })).toBeVisible();
  await expect(page.getByText('Gói PRO', { exact: true })).toHaveCount(0);
  expect(requests).toBe(1);
});

test('Issue #142: pending checks are bounded and do not request a second payment', async ({ page }) => {
  await page.clock.install();
  await seedDemoSession(page);
  let requests = 0;
  await page.route('**/api/v1/subscriptions/transactions/123', route => {
    requests++; return route.fulfill({ json: result('PENDING') });
  });
  await page.goto('/payment/success?orderCode=123&status=PAID');
  await expect.poll(() => requests).toBe(1);
  for (let count = 2; count <= 10; count++) {
    await expect(page.getByText('Gói FREE', { exact: true })).toBeVisible();
    await page.clock.runFor(3100);
    await expect.poll(() => requests).toBe(count);
  }
  await expect(page.getByRole('heading', { name: 'Chưa nhận được xác nhận thanh toán' })).toBeVisible();
  await page.clock.runFor(10000);
  expect(requests).toBe(10);
  await expect(page.getByRole('button', { name: 'Kiểm tra lại', exact: true })).toBeVisible();
  await expect(page.getByText('Thanh toán thành công!', { exact: true })).toHaveCount(0);
});

test('Issue #142: cancelling in URL cannot override a paid order', async ({ page }) => {
  await seedDemoSession(page);
  await page.route('**/api/v1/subscriptions/transactions/123', route => route.fulfill({ json: result('PAID', true) }));
  await page.goto('/payment/cancel?orderCode=123&status=CANCELLED');
  await expect(page.getByRole('heading', { name: 'Thanh toán thành công!', exact: true })).toBeVisible();
});

test('Issue #142: historical paid order with expired subscription is not advertised as newly activated', async ({ page }) => {
  await seedDemoSession(page);
  await page.route('**/api/v1/subscriptions/transactions/123', route => route.fulfill({ json: result('PAID') }));
  await page.goto('/payment/success?orderCode=123');
  await expect(page.getByRole('heading', { name: 'Giao dịch đã được thanh toán', exact: true })).toBeVisible();
  await expect(page.getByText('Gói FREE', { exact: true })).toBeVisible();
  await expect(page.getByText('Thanh toán thành công!', { exact: true })).toHaveCount(0);
});

test('Issue #142: logged-out result requests login without fetching a private order', async ({ page }) => {
  let requests = 0;
  await page.route('**/api/v1/subscriptions/transactions/*', route => { requests++; return route.abort(); });
  await page.goto('/payment/success?orderCode=123&status=PAID');
  await expect(page.getByRole('heading', { name: 'Đăng nhập để kiểm tra thanh toán' })).toBeVisible();
  expect(requests).toBe(0);
});

test('Issue #142: checkout unavailable stays on plans and fake checkout URL is rejected', async ({ page }) => {
  await seedDemoSession(page);
  const plans = ['FREE', 'PLUS', 'PRO'].map((tier, i) => ({ tier, name: tier, priceVnd: [0, 49000, 99000][i],
    description: 'Gói AI', billingPeriod: 'Tháng', features: ['AI'] }));
  await page.route('**/api/v1/subscriptions/plans', route => route.fulfill({ json: plans }));
  await page.route('**/api/v1/subscriptions/my-subscription', route => route.fulfill({ json: free }));
  let unavailable = true;
  await page.route('**/api/v1/subscriptions/checkout', route => {
    expect(route.request().postDataJSON()).toEqual({ tier: 'PRO' });
    return route.fulfill(unavailable
      ? { status: 503, json: { status: 503, code: 'PAYMENT_UNAVAILABLE', detail: 'Hiện chưa thể tạo thanh toán.' } }
      : { status: 201, json: { orderCode: 123, amountVnd: 99000, tier: 'PRO', checkoutUrl: '/payment/success?mock=true', qrCode: 'fake' } });
  });
  await page.goto('/goi-ai');
  await page.getByRole('button', { name: /Nâng cấp lên PRO/ }).click();
  await expect(page.getByRole('alert')).toContainText('Hiện chưa thể tạo thanh toán.');
  await expect(page).toHaveURL(/\/goi-ai$/);
  unavailable = false;
  await page.getByRole('button', { name: /Nâng cấp lên PRO/ }).click();
  await expect(page.getByRole('alert')).toContainText('Không thể khởi tạo liên kết thanh toán');
  await expect(page).toHaveURL(/\/goi-ai$/);
});
