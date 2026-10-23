import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

for (const timezoneId of ['UTC', 'America/Los_Angeles', 'Asia/Ho_Chi_Minh']) {
  test.describe(`Vietnam payment dates in ${timezoneId}`, () => {
    test.use({ timezoneId, viewport: { width: 1440, height: 900 } });
    test('history shows Vietnam time and purchased expiry', async ({ page }) => {
    await seedDemoSession(page);
    await page.route('**/api/v1/subscriptions/transactions', route => route.fulfill({ json: [
      { paymentTransactionId: 1, orderCode: '123', tier: 'PLUS', amountVnd: 49000, status: 'PAID',
        createdAt: '2026-10-20T05:07:00.000Z', paidAt: '2026-10-20T05:08:00.000Z', endsAt: '2026-11-19T05:08:00.000Z' },
      { paymentTransactionId: 2, orderCode: '124', tier: 'PRO', amountVnd: 99000, status: 'CANCELLED',
        createdAt: '2026-10-20T17:05:00Z', paidAt: null, endsAt: null },
    ] }));
    await page.goto('/giao-dich');
    const paid = page.getByRole('row').filter({ hasText: '#123' });
    await expect(paid.getByRole('cell', { name: '20/10/2026 12:07', exact: true })).toBeVisible();
    await expect(paid.getByRole('cell', { name: '20/10/2026 12:08', exact: true })).toBeVisible();
    await expect(paid.getByRole('cell', { name: '19/11/2026 12:08', exact: true })).toBeVisible();
    const cancelled = page.getByRole('row').filter({ hasText: '#124' });
    await expect(cancelled.getByRole('cell', { name: '21/10/2026 00:05', exact: true })).toBeVisible();
    await expect(cancelled.getByRole('cell', { name: '—', exact: true })).toHaveCount(2);
    await expect(page.getByText('Thời gian hiển thị theo giờ Việt Nam (UTC+7).')).toBeVisible();
    await page.screenshot({ path: test.info().outputPath(`detail-payment-vietnam-${timezoneId.replaceAll('/', '-')}.png`), fullPage: true });
    });
  });
}
