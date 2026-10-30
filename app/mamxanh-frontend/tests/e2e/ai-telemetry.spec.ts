import { expect, test } from './baseFixtures';
import type { Page } from '@playwright/test';

async function session(page: Page, role = 'ADMIN') {
  await page.addInitScript(role => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
    accessToken: 'synthetic-fr11-test-token', expiresAt: Date.now() + 3600000,
    account: { id: 11, displayName: 'FR11 Admin', email: 'fr11@example.invalid', role, accountStatus: 'ACTIVE', emailVerified: true },
  })), role);
}
const totals = { calls: 2, promptTokens: 150, responseTokens: 300, totalTokens: 500,
  thinkingTokens: 50, cachedTokens: 20, incompleteUsageCalls: 0, unpricedCalls: 0,
  knownEstimatedCostUsd: 0.0014115, estimatedCostUsd: 0.0014115 };
function report(overrides = {}) {
  return { from: '2026-10-23', to: '2026-10-23', granularity: 'DAY', timezone: 'Asia/Ho_Chi_Minh',
    retainedFrom: '2026-07-25T03:00:00Z', currency: 'USD', costBasis: 'STANDARD_PAID_EQUIVALENT',
    totals, buckets: [{ periodStart: '2026-10-23', feature: 'FR-51', totals }], ...overrides };
}

test('FR11 Admin sees aggregate tokens and changes cycle and feature filters', async ({ page }) => {
  await session(page);
  const queries: URLSearchParams[] = [];
  await page.route('**/api/v1/admin/ai-telemetry?**', route => {
    queries.push(new URL(route.request().url()).searchParams);
    return route.fulfill({ json: report() });
  });
  await page.goto('/admin/ai-telemetry');
  await expect(page.getByRole('heading', { name: 'Mức sử dụng AI' })).toBeVisible();
  await expect(page.getByRole('table')).toContainText('Chatbot');
  await expect(page.getByRole('table')).toContainText('500');
  await page.getByRole('combobox', { name: 'Chu kỳ', exact: true }).selectOption('WEEK');
  await page.getByRole('combobox', { name: 'Tính năng', exact: true }).selectOption('FR-21');
  await page.getByRole('button', { name: 'Xem báo cáo' }).click();
  await expect.poll(() => queries.at(-1)?.get('granularity')).toBe('WEEK');
  expect(queries.at(-1)?.get('feature')).toBe('FR-21');
  await page.getByRole('combobox', { name: 'Chu kỳ', exact: true }).selectOption('MONTH');
  await page.getByRole('button', { name: 'Xem báo cáo' }).click();
  await expect.poll(() => queries.at(-1)?.get('granularity')).toBe('MONTH');
});

test('FR11 missing usage does not display a false zero total cost', async ({ page }) => {
  await session(page);
  await page.route('**/api/v1/admin/ai-telemetry?**', route => route.fulfill({ json: report({
    totals: { ...totals, incompleteUsageCalls: 1, unpricedCalls: 1, estimatedCostUsd: null },
    buckets: [{ periodStart: '2026-10-23', feature: 'FR-21', totals: { ...totals, estimatedCostUsd: null } }],
  }) }));
  await page.goto('/admin/ai-telemetry');
  await expect(page.getByRole('note')).toContainText('tổng chi phí chưa đầy đủ');
  await expect(page.getByText('Chưa tính đủ', { exact: true })).toHaveCount(2);
});

test('FR11 error supports retry and then shows the empty state', async ({ page }) => {
  await session(page);
  let calls = 0;
  await page.route('**/api/v1/admin/ai-telemetry?**', route => ++calls === 1
    ? route.fulfill({ status: 503, json: { status: 503, code: 'INTERNAL_ERROR', detail: 'Dịch vụ đang bận.' } })
    : route.fulfill({ json: report({ totals: { ...totals, calls: 0 }, buckets: [] }) }));
  await page.goto('/admin/ai-telemetry');
  await expect(page.getByRole('alert')).toContainText('Dịch vụ đang bận.');
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('heading', { name: 'Chưa có dữ liệu sử dụng AI' })).toBeVisible();
  await expect(page.getByRole('table')).toHaveCount(0);
});

test('FR11 Guest and Member cannot mount a report or request telemetry', async ({ page }) => {
  let requests = 0;
  await page.route('**/api/v1/admin/ai-telemetry?**', route => { requests++; return route.fulfill({ json: report() }); });
  await page.goto('/admin/ai-telemetry');
  await expect(page.getByRole('heading', { name: 'Đăng nhập để tiếp tục' })).toBeVisible();
  await session(page, 'CUSTOMER');
  await page.reload();
  await expect(page.getByRole('heading', { name: 'Không có quyền truy cập' })).toBeVisible();
  expect(requests).toBe(0);
});

test('FR11 narrow screen keeps report controls usable and table scroll contained', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 800 });
  await session(page);
  await page.route('**/api/v1/admin/ai-telemetry?**', route => route.fulfill({ json: report() }));
  await page.goto('/admin/ai-telemetry');
  await expect(page.getByRole('button', { name: 'Xem báo cáo' })).toBeVisible();
  await expect(page.getByRole('region', { name: 'Bảng thống kê token AI' })).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});
