import type { Page, Route } from '@playwright/test';
import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

// FR-28 Administrator report queue against a stateful mocked API. Backend rules (single handler,
// immutable RESOLVED reports, sanctions) are proven by AdminReportIntegrationTest on SQL Server.
type Report = {
  id: number;
  status: 'OPEN' | 'IN_REVIEW' | 'RESOLVED';
  reasonCode: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
  recipe: { id: number; title: string; status: string };
  reporter: { id: number; displayName: string; email: string; role: string; accountStatus: string };
  author: { id: number; displayName: string; email: string; role: string; accountStatus: string };
  handledBy: { id: number; displayName: string } | null;
  decision: string | null;
  decisionReason: string | null;
  handledAt: string | null;
  supplements: Array<{ supplementId: number; content: string; createdAt: string }>;
};

const ME = { id: 901, displayName: 'Demo ADMIN' };
const account = (id: number, displayName: string, role = 'CUSTOMER') => ({
  id, displayName, email: `user-${id}@mamxanh.local`, role, accountStatus: 'ACTIVE',
});

function fixtures(): Report[] {
  const base = {
    updatedAt: '2026-10-25T02:00:00', decision: null, decisionReason: null, handledAt: null, handledBy: null, supplements: [],
    author: account(30, 'Chuyên gia Bình', 'EXPERT'),
  };
  return [
    { ...base, id: 11, status: 'OPEN', reasonCode: 'RECIPE_INFO_OR_DIET_LABEL', description: 'Món có dùng nước mắm cá',
      createdAt: '2026-10-25T01:30:00', recipe: { id: 501, title: 'Canh nấm hạt sen', status: 'PUBLISHED' }, reporter: account(41, 'Người báo cáo Hà') },
    { ...base, id: 12, status: 'OPEN', reasonCode: 'SPAM_ADVERTISING', description: null,
      createdAt: '2026-10-25T01:00:00', recipe: { id: 502, title: 'Gỏi cuốn rau củ', status: 'PUBLISHED' }, reporter: account(42, 'Người báo cáo Khoa') },
    { ...base, id: 13, status: 'IN_REVIEW', reasonCode: 'FOOD_SAFETY_HAZARD', description: 'Nấm lạ không rõ nguồn gốc',
      createdAt: '2026-10-24T09:00:00', recipe: { id: 503, title: 'Lẩu nấm rừng', status: 'PUBLISHED' }, reporter: account(43, 'Người báo cáo Mai'),
      handledBy: { id: 902, displayName: 'Quản trị viên Lan' } },
    { ...base, id: 14, status: 'IN_REVIEW', reasonCode: 'OTHER', description: 'Ảnh bìa không phải món trong bài',
      createdAt: '2026-10-24T08:00:00', recipe: { id: 504, title: 'Đậu hũ sốt cà', status: 'PUBLISHED' }, reporter: account(44, 'Người báo cáo Tâm'),
      handledBy: ME },
    { ...base, id: 15, status: 'RESOLVED', reasonCode: 'COPYRIGHT_VIOLATION', description: null,
      createdAt: '2026-10-20T08:00:00', recipe: { id: 505, title: 'Cơm chiên thập cẩm', status: 'PUBLISHED' }, reporter: account(45, 'Người báo cáo Vy'),
      handledBy: { id: 902, displayName: 'Quản trị viên Lan' }, decision: 'NO_VIOLATION',
      decisionReason: 'Ảnh do chính tác giả chụp.', handledAt: '2026-10-21T03:00:00' },
  ];
}

type Api = { reports: Report[]; resolveBodies: unknown[]; claimConflict: boolean; resolveProblem: object | null };

async function mockApi(page: Page): Promise<Api> {
  const api: Api = { reports: fixtures(), resolveBodies: [], claimConflict: false, resolveProblem: null };
  const problem = (route: Route, status: number, body: object) =>
    route.fulfill({ status, contentType: 'application/problem+json', body: JSON.stringify({ status, ...body }) });
  await page.route('**/api/v1/admin/reports**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace(/^.*\/api\/v1\/admin\/reports/, '');
    const method = route.request().method();
    if (method === 'GET' && path === '/status-counts') {
      const count = (status: Report['status']) => api.reports.filter((report) => report.status === status).length;
      return route.fulfill({ json: { open: count('OPEN'), inReview: count('IN_REVIEW'), resolved: count('RESOLVED') } });
    }
    if (method === 'GET' && path === '') {
      const status = url.searchParams.get('status');
      const content = api.reports.filter((report) => !status || report.status === status);
      return route.fulfill({ json: { content, page: 0, size: 20, totalElements: content.length, totalPages: content.length ? 1 : 0 } });
    }
    const match = /^\/(\d+)(\/claim|\/resolve)?$/.exec(path);
    const report = match ? api.reports.find((item) => item.id === Number(match[1])) : undefined;
    if (!report) return problem(route, 404, { code: 'REPORT_NOT_FOUND', detail: 'Không tìm thấy báo cáo.' });
    if (method === 'GET') return route.fulfill({ json: report });
    if (match?.[2] === '/claim') {
      if (api.claimConflict || report.status !== 'OPEN') {
        report.status = 'IN_REVIEW';
        report.handledBy = { id: 902, displayName: 'Quản trị viên Lan' };
        return problem(route, 409, { code: 'REPORT_STATUS_CONFLICT', detail: 'Báo cáo đã được tiếp nhận hoặc đã giải quyết. Hãy tải lại danh sách.' });
      }
      Object.assign(report, { status: 'IN_REVIEW', handledBy: ME });
      return route.fulfill({ json: report });
    }
    const body = route.request().postDataJSON() as { decision: string; reason: string };
    api.resolveBodies.push(body);
    if (api.resolveProblem) return problem(route, 400, api.resolveProblem);
    Object.assign(report, { status: 'RESOLVED', decision: body.decision, decisionReason: body.reason, handledAt: '2026-10-25T03:00:00' });
    if (body.decision === 'HIDE_RECIPE') report.recipe.status = 'HIDDEN';
    return route.fulfill({ json: report });
  });
  return api;
}

const card = (page: Page, title: string) => page.locator('div.border-l-4').filter({ hasText: title });

test.beforeEach(async ({ page }) => {
  await seedDemoSession(page, 'ADMIN');
});

test('queue lists open Recipe Post reports with reporter, author and status counts, and filters by status', async ({ page }) => {
  await mockApi(page);
  await page.goto('/admin/bao-cao?xem=bao-cao');

  await expect(page.getByRole('heading', { name: 'Xử lý báo cáo vi phạm' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Chờ xử lý (2)' })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByRole('button', { name: 'Đang xem xét (2)' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Đã giải quyết (1)' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Tất cả (5)' })).toBeVisible();
  const first = card(page, 'Canh nấm hạt sen');
  await expect(first).toContainText('Sai thông tin công thức hoặc nhãn ăn chay');
  await expect(first).toContainText('Món có dùng nước mắm cá');
  await expect(first).toContainText('Người báo cáo: Người báo cáo Hà');
  await expect(first).toContainText('Tác giả: Chuyên gia Bình');
  await expect(first).toContainText('#11 · Gửi lúc 25/10/2026 08:30');
  await expect(first.getByRole('button', { name: 'Tiếp nhận xem xét' })).toBeVisible();
  await expect(page.getByText('Lẩu nấm rừng')).toHaveCount(0);

  await page.getByRole('button', { name: /^Đã giải quyết/ }).click();
  const resolved = card(page, 'Cơm chiên thập cẩm');
  await expect(resolved).toContainText('Kết luận: Không vi phạm');
  await resolved.getByRole('button', { name: 'Xem chi tiết' }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog).toContainText('Ảnh do chính tác giả chụp.');
  await expect(dialog).toContainText('Ghi lúc 21/10/2026 10:00');
  await expect(dialog.getByRole('button', { name: 'Hoàn tất xử lý' })).toHaveCount(0);
  await dialog.getByRole('button', { name: 'Đóng', exact: true }).click();

  await page.setViewportSize({ width: 375, height: 740 });
  await page.getByRole('button', { name: /^Tất cả/ }).click();
  await expect(card(page, 'Lẩu nấm rừng')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
});

test('a long report description shows two whole lines on the queue card without part of a third', async ({ page }) => {
  const api = await mockApi(page);
  api.reports[0].description = 'Cuối phần hướng dẫn có lời mời mua bộ sốt qua trang bán hàng, không liên quan đến cách làm món. '.repeat(3).trim();
  await page.setViewportSize({ width: 375, height: 740 });
  await page.goto('/admin/bao-cao?xem=bao-cao');

  const text = card(page, 'Canh nấm hạt sen').getByText('Cuối phần hướng dẫn có lời mời');
  await expect(text).toBeVisible();
  const box = await text.evaluate((element) => ({
    visible: element.clientHeight,
    lineHeight: Number.parseFloat(getComputedStyle(element).lineHeight),
  }));
  expect(box.visible).toBeLessThanOrEqual(2 * box.lineHeight + 1);
});

test('taking an open report records the handling administrator and opens the decision form', async ({ page }) => {
  await mockApi(page);
  await page.goto('/admin/bao-cao?xem=bao-cao');

  await card(page, 'Canh nấm hạt sen').getByRole('button', { name: 'Tiếp nhận xem xét' }).click();

  const dialog = page.getByRole('dialog', { name: 'Báo cáo #11' });
  await expect(dialog).toContainText('Đang xem xét');
  await expect(dialog).toContainText('Người xử lý: Demo ADMIN');
  await expect(dialog).toContainText('Người báo cáo Hà');
  await expect(dialog).toContainText('user-41@mamxanh.local');
  await expect(dialog.getByRole('link', { name: 'Mở bài trong thẻ mới' })).toHaveAttribute('href', '/cong-thuc/id/501');
  await expect(dialog.getByRole('group', { name: 'Kết luận xử lý' })).toBeVisible();
  await expect(page.getByRole('status').filter({ hasText: 'Bạn đã tiếp nhận báo cáo #11.' })).toBeVisible();
  await dialog.getByRole('button', { name: 'Đóng', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Chờ xử lý (1)' })).toBeVisible();
});

test('an empty or missing conclusion is refused before anything is saved', async ({ page }) => {
  const api = await mockApi(page);
  await page.goto('/admin/bao-cao?xem=bao-cao');
  await page.getByRole('button', { name: /^Đang xem xét/ }).click();
  await card(page, 'Đậu hũ sốt cà').getByRole('button', { name: 'Ghi kết luận' }).click();
  const dialog = page.getByRole('dialog', { name: 'Báo cáo #14' });

  await dialog.getByRole('button', { name: 'Hoàn tất xử lý' }).click();
  await expect(dialog.getByText('Chọn một kết luận xử lý.')).toBeVisible();
  await expect(dialog.getByText('Nhập lý do kết luận từ 10 đến 500 ký tự.')).toBeVisible();

  await dialog.getByRole('radio', { name: /Ẩn bài viết/ }).check();
  await dialog.getByLabel('Lý do kết luận').fill('   ngắn   ');
  await dialog.getByRole('button', { name: 'Hoàn tất xử lý' }).click();
  await expect(dialog.getByText('Chọn một kết luận xử lý.')).toHaveCount(0);
  await expect(dialog.getByLabel('Lý do kết luận')).toHaveAttribute('aria-invalid', 'true');
  await expect(dialog.getByText('4/500')).toBeVisible();
  expect(api.resolveBodies).toEqual([]);
});

test('hiding the post saves the trimmed reason and moves the report to resolved', async ({ page }) => {
  const api = await mockApi(page);
  await page.goto('/admin/bao-cao?xem=bao-cao');
  await page.getByRole('button', { name: /^Đang xem xét/ }).click();
  await card(page, 'Đậu hũ sốt cà').getByRole('button', { name: 'Ghi kết luận' }).click();
  const dialog = page.getByRole('dialog', { name: 'Báo cáo #14' });

  await dialog.getByRole('radio', { name: /Ẩn bài viết/ }).check();
  await dialog.getByLabel('Lý do kết luận').fill('  Ảnh bìa lấy từ trang khác và không phải món trong bài.  ');
  await dialog.getByRole('button', { name: 'Hoàn tất xử lý' }).click();

  await expect(dialog).toHaveCount(0);
  await expect(page.getByRole('status')).toContainText('Đã giải quyết báo cáo #14 với kết luận "Ẩn bài viết".');
  expect(api.resolveBodies).toEqual([{ decision: 'HIDE_RECIPE', reason: 'Ảnh bìa lấy từ trang khác và không phải món trong bài.' }]);
  await expect(page.getByRole('button', { name: 'Đang xem xét (1)' })).toBeVisible();
  await page.getByRole('button', { name: /^Đã giải quyết/ }).click();
  await card(page, 'Đậu hũ sốt cà').getByRole('button', { name: 'Xem chi tiết' }).click();
  await expect(page.getByRole('dialog')).toContainText('Bài đang bị ẩn khỏi trang công khai.');
});

test('locking the author warns first and shows the server validation message', async ({ page }) => {
  const api = await mockApi(page);
  api.resolveProblem = { code: 'VALIDATION_FAILED', detail: 'Dữ liệu không hợp lệ.', errors: [{ field: 'reason', message: 'Nhập lý do kết luận từ 10 đến 500 ký tự.' }] };
  await page.goto('/admin/bao-cao?xem=bao-cao');
  await page.getByRole('button', { name: /^Đang xem xét/ }).click();
  await card(page, 'Đậu hũ sốt cà').getByRole('button', { name: 'Ghi kết luận' }).click();
  const dialog = page.getByRole('dialog', { name: 'Báo cáo #14' });

  await dialog.getByRole('radio', { name: /Khóa tài khoản tác giả/ }).check();
  await expect(dialog).toContainText('Khóa tài khoản sẽ đăng xuất Chuyên gia Bình khỏi mọi thiết bị.');
  await dialog.getByLabel('Lý do kết luận').fill('Đăng lại nội dung sai nhãn chay nhiều lần.');
  await dialog.getByRole('button', { name: 'Hoàn tất xử lý' }).click();

  await expect(dialog.getByRole('alert')).toHaveText('Nhập lý do kết luận từ 10 đến 500 ký tự.');
  expect(api.resolveBodies).toEqual([{ decision: 'LOCK_AUTHOR', reason: 'Đăng lại nội dung sai nhãn chay nhiều lần.' }]);
  await expect(dialog.getByRole('button', { name: 'Hoàn tất xử lý' })).toBeEnabled();
});

test('a report held by another administrator can only be viewed', async ({ page }) => {
  await mockApi(page);
  await page.goto('/admin/bao-cao?xem=bao-cao');
  await page.getByRole('button', { name: /^Đang xem xét/ }).click();
  const held = card(page, 'Lẩu nấm rừng');
  await expect(held).toContainText('Người xử lý: Quản trị viên Lan');
  await expect(held.getByRole('button', { name: 'Ghi kết luận' })).toHaveCount(0);

  await held.getByRole('button', { name: 'Xem chi tiết' }).click();
  const dialog = page.getByRole('dialog', { name: 'Báo cáo #13' });
  await expect(dialog).toContainText('Quản trị viên Lan đang xem xét báo cáo này. Chỉ người tiếp nhận mới ghi được kết luận.');
  await expect(dialog.getByRole('group', { name: 'Kết luận xử lý' })).toHaveCount(0);
  await expect(dialog.getByRole('button', { name: 'Tiếp nhận xem xét' })).toHaveCount(0);
});

test('a claim that loses the race explains why and reloads the queue', async ({ page }) => {
  const api = await mockApi(page);
  api.claimConflict = true;
  await page.goto('/admin/bao-cao?xem=bao-cao');

  await card(page, 'Gỏi cuốn rau củ').getByRole('button', { name: 'Tiếp nhận xem xét' }).click();

  await expect(page.getByRole('alert')).toHaveText('Báo cáo đã được tiếp nhận hoặc đã giải quyết. Hãy tải lại danh sách.');
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await expect(card(page, 'Gỏi cuốn rau củ')).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Chờ xử lý (1)' })).toBeVisible();
});

test('queue failure is reported instead of an empty queue', async ({ page }) => {
  await page.route('**/api/v1/admin/reports**', (route) => route.fulfill({ status: 503, body: '' }));
  await page.goto('/admin/bao-cao?xem=bao-cao');

  await expect(page.getByRole('alert')).toHaveText('Không thể kết nối dịch vụ. Vui lòng thử lại.');
  await expect(page.getByText('Chưa tải được danh sách báo cáo. Bấm Làm mới để thử lại.')).toBeVisible();
  await expect(page.getByText('Không có báo cáo nào ở trạng thái này.')).toHaveCount(0);
});
