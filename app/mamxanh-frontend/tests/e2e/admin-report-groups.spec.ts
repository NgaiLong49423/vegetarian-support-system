import type { Page } from '@playwright/test';
import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

// FR-30 AC-30.4: the Administrator board groups reports by recipe and shows each report with its supplements.
// FR-06 UC-06.1: the groups can be ranked by report count, show their most common reason and be searched.
// Grouping, counts, ordering and search on SQL Server are proven by AdminReportGroupIntegrationTest and
// ModerationDashboardIntegrationTest.
type Status = 'OPEN' | 'IN_REVIEW' | 'RESOLVED';
const account = (id: number, displayName: string, role = 'CUSTOMER') => ({
  id, displayName, email: `user-${id}@mamxanh.local`, role, accountStatus: 'ACTIVE',
});
const author = account(30, 'Chuyên gia Nam', 'EXPERT');
const hotPot = { id: 601, title: 'Lẩu nấm ba báo cáo', status: 'PUBLISHED' };
const springRoll = { id: 602, title: 'Gỏi cuốn một báo cáo', status: 'PUBLISHED' };

function report(id: number, status: Status, recipe: typeof hotPot, reporter: ReturnType<typeof account>, createdAt: string) {
  return {
    id, status, reasonCode: 'FOOD_SAFETY_HAZARD', description: 'Món dùng nước mắm cá', createdAt, updatedAt: createdAt,
    recipe, reporter, author,
    handledBy: status === 'OPEN' ? null : { id: 902, displayName: 'Quản trị viên Lan' },
    decision: status === 'RESOLVED' ? 'NO_VIOLATION' : null,
    decisionReason: status === 'RESOLVED' ? 'Đã kiểm tra lại công thức.' : null,
    handledAt: status === 'RESOLVED' ? '2026-10-25T05:00:00' : null,
    supplements: [] as Array<{ supplementId: number; content: string; createdAt: string }>,
  };
}

function fixtures() {
  const open = report(21, 'OPEN', hotPot, account(41, 'Người báo cáo An'), '2026-10-25T01:00:00');
  open.supplements = [
    { supplementId: 2, content: 'Bước 3 hướng dẫn dùng nước mắm cá', createdAt: '2026-10-25T02:00:00' },
    { supplementId: 3, content: 'Ảnh bìa có logo cửa hàng bán nước mắm', createdAt: '2026-10-25T03:30:00' },
  ];
  return [
    report(24, 'OPEN', springRoll, account(41, 'Người báo cáo An'), '2026-10-25T06:00:00'),
    report(23, 'RESOLVED', hotPot, account(43, 'Người báo cáo Chi'), '2026-10-25T04:00:00'),
    { ...report(22, 'IN_REVIEW', hotPot, account(42, 'Người báo cáo Bảo'), '2026-10-25T03:00:00'), reasonCode: 'SPAM_ADVERTISING' },
    open,
  ];
}

async function mockApi(page: Page) {
  const reports = fixtures();
  const requests: string[] = [];
  await page.route('**/api/v1/admin/reports**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace(/^.*\/api\/v1\/admin\/reports/, '');
    requests.push(`${path}?${url.searchParams.toString()}`);
    const status = url.searchParams.get('status');
    const matches = (item: { status: Status }) => !status || item.status === status;
    const count = (items: Array<{ status: Status }>, value: Status) => items.filter((item) => item.status === value).length;
    if (path === '/status-counts') {
      return route.fulfill({ json: { open: count(reports, 'OPEN'), inReview: count(reports, 'IN_REVIEW'), resolved: count(reports, 'RESOLVED') } });
    }
    if (path === '/groups') {
      const query = (url.searchParams.get('query') ?? '').toLowerCase();
      const content = [hotPot, springRoll]
        .filter((recipe) => `${recipe.title} ${author.displayName}`.toLowerCase().includes(query))
        .map((recipe) => {
          const all = reports.filter((item) => item.recipe.id === recipe.id);
          const matching = all.filter(matches);
          const reasons = matching.map((item) => item.reasonCode);
          const topReasonCode = [...reasons].sort((a, b) => reasons.filter((r) => r === b).length - reasons.filter((r) => r === a).length)[0] ?? null;
          return {
            recipe, author, reportCount: matching.length,
            statusCounts: { open: count(all, 'OPEN'), inReview: count(all, 'IN_REVIEW'), resolved: count(all, 'RESOLVED') },
            latestReportAt: matching.map((item) => item.createdAt).sort().at(-1) ?? null,
            topReasonCode, topReasonCount: matching.filter((item) => item.reasonCode === topReasonCode).length,
          };
        })
        .filter((group) => group.reportCount > 0)
        .sort((a, b) => url.searchParams.get('sort') === 'MOST_REPORTED' && b.reportCount !== a.reportCount
          ? b.reportCount - a.reportCount
          : String(b.latestReportAt).localeCompare(String(a.latestReportAt)));
      return route.fulfill({ json: { content, page: 0, size: 20, totalElements: content.length, totalPages: content.length ? 1 : 0 } });
    }
    if (path === '') {
      const recipeId = url.searchParams.get('recipeId');
      const content = reports.filter((item) => matches(item) && (!recipeId || item.recipe.id === Number(recipeId)));
      return route.fulfill({ json: { content, page: 0, size: 20, totalElements: content.length, totalPages: content.length ? 1 : 0 } });
    }
    const found = reports.find((item) => `/${item.id}` === path);
    return found ? route.fulfill({ json: found }) : route.fulfill({ status: 404, json: { status: 404, code: 'REPORT_NOT_FOUND' } });
  });
  return { requests };
}

const groupCard = (page: Page, title: string) => page.getByRole('article').filter({ hasText: title });
const reportCard = (page: Page, text: string) => page.locator('div.border-l-4').filter({ hasText: text });

test.beforeEach(async ({ page }) => {
  await seedDemoSession(page, 'ADMIN');
});

test('AC-30.4: the board opens with one group per recipe and its report count', async ({ page }) => {
  await mockApi(page);
  await page.goto('/admin/bao-cao');

  await expect(page.getByRole('button', { name: 'Theo bài viết' })).toHaveAttribute('aria-pressed', 'true');
  await page.getByRole('button', { name: /^Tất cả/ }).click();
  await expect(groupCard(page, 'Lẩu nấm ba báo cáo')).toHaveCount(1);
  const hotPotGroup = groupCard(page, 'Lẩu nấm ba báo cáo');
  await expect(hotPotGroup).toContainText('3 báo cáo');
  await expect(hotPotGroup).toContainText('1 chờ xử lý');
  await expect(hotPotGroup).toContainText('1 đang xem xét');
  await expect(hotPotGroup).toContainText('1 đã giải quyết');
  await expect(hotPotGroup).toContainText('Tác giả: Chuyên gia Nam');
  await expect(hotPotGroup).toContainText('Báo cáo mới nhất lúc 25/10/2026 11:00');
  await expect(groupCard(page, 'Gỏi cuốn một báo cáo')).toContainText('1 báo cáo');
  await expect(page.getByRole('article').first()).toContainText('Gỏi cuốn một báo cáo');
});

test('AC-30.4: a group opens each of its reports with reporter, status and decision', async ({ page }) => {
  const api = await mockApi(page);
  await page.goto('/admin/bao-cao');

  await expect(groupCard(page, 'Lẩu nấm ba báo cáo')).toContainText('3 báo cáo');
  await groupCard(page, 'Lẩu nấm ba báo cáo').getByRole('button', { name: 'Xem 3 báo cáo' }).click();

  await expect(page.getByText('Báo cáo của bài “Lẩu nấm ba báo cáo”')).toBeVisible();
  await expect(page.getByRole('button', { name: /^Tất cả/ })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.locator('div.border-l-4')).toHaveCount(3);
  await expect(reportCard(page, 'Người báo cáo An')).toContainText('Chờ xử lý');
  await expect(reportCard(page, 'Người báo cáo Bảo')).toContainText('Đang xem xét');
  await expect(reportCard(page, 'Người báo cáo Chi')).toContainText('Kết luận: Không vi phạm');
  await expect(reportCard(page, 'Gỏi cuốn một báo cáo')).toHaveCount(0);
  const listed = api.requests.filter((request) => request.startsWith('?')).map((request) => new URLSearchParams(request.slice(1)));
  expect(listed.some((params) => params.get('recipeId') === '601' && !params.get('status'))).toBe(true);

  await page.getByRole('button', { name: 'Xem tất cả bài viết' }).click();
  await expect(page.getByRole('button', { name: 'Theo bài viết' })).toHaveAttribute('aria-pressed', 'true');
  await expect(groupCard(page, 'Gỏi cuốn một báo cáo')).toBeVisible();
});

test('every report stays one click away and keeps the FR-28 queue', async ({ page }) => {
  await mockApi(page);
  await page.goto('/admin/bao-cao');

  await page.getByRole('button', { name: 'Từng báo cáo' }).click();
  await expect(page).toHaveURL(/\/admin\/bao-cao\?xem=bao-cao$/);
  await expect(reportCard(page, 'Gỏi cuốn một báo cáo').getByRole('button', { name: 'Tiếp nhận xem xét' })).toBeVisible();
  await expect(page.getByRole('article')).toHaveCount(0);
});

test('the report detail lists the supplements oldest first with their Vietnam time', async ({ page }) => {
  await mockApi(page);
  await page.goto('/admin/bao-cao?xem=bao-cao');

  const card = reportCard(page, 'Người báo cáo An').filter({ hasText: 'Lẩu nấm ba báo cáo' });
  await expect(card).toContainText('2 lần bổ sung');
  await card.getByRole('button', { name: 'Xem chi tiết' }).click();
  const dialog = page.getByRole('dialog');
  const items = dialog.getByRole('list', { name: 'Thông tin bổ sung' }).getByRole('listitem');
  await expect(items).toHaveCount(2);
  await expect(items.nth(0)).toContainText('Bước 3 hướng dẫn dùng nước mắm cá');
  await expect(items.nth(0)).toContainText('25/10/2026 09:00');
  await expect(items.nth(1)).toContainText('Ảnh bìa có logo cửa hàng bán nước mắm');
  await expect(items.nth(1)).toContainText('25/10/2026 10:30');
});

test('a filter without any report shows an empty group list, and the board fits a 375 px phone', async ({ page }) => {
  await mockApi(page);
  await page.setViewportSize({ width: 375, height: 740 });
  await page.goto('/admin/bao-cao');

  await expect(groupCard(page, 'Lẩu nấm ba báo cáo')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.route('**/api/v1/admin/reports/groups**', (route) => route.fulfill({
    json: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
  }));
  await page.getByRole('button', { name: /^Đang xem xét/ }).click();
  await expect(page.getByText('Không có bài viết nào có báo cáo ở trạng thái này.')).toBeVisible();
});

test('FR-06: the moderation page is named Hậu kiểm nội dung in its heading and the admin menu', async ({ page }) => {
  await mockApi(page);
  await page.goto('/admin/bao-cao');

  await expect(page.getByRole('heading', { name: 'Hậu kiểm nội dung', level: 1 })).toBeVisible();
  await expect(page.getByRole('navigation', { name: 'Điều hướng quản trị' }).getByRole('link', { name: 'Hậu kiểm nội dung' }))
    .toHaveAttribute('href', '/admin/bao-cao');
});

test('UC-06.1: each group shows its most common reason, and the most reported post can come first', async ({ page }) => {
  const api = await mockApi(page);
  await page.goto('/admin/bao-cao');
  await page.getByRole('button', { name: /^Tất cả/ }).click();

  const hotPotGroup = groupCard(page, 'Lẩu nấm ba báo cáo');
  await expect(hotPotGroup).toContainText('Lý do nhiều nhất: Nguy cơ an toàn thực phẩm (2/3 báo cáo)');
  await expect(groupCard(page, 'Gỏi cuốn một báo cáo')).toContainText('Lý do: Nguy cơ an toàn thực phẩm');
  await expect(groupCard(page, 'Gỏi cuốn một báo cáo')).not.toContainText('Lý do nhiều nhất');
  await expect(page.getByRole('article').first()).toContainText('Gỏi cuốn một báo cáo');

  await page.getByLabel('Sắp xếp').selectOption('MOST_REPORTED');
  await expect(page.getByRole('article').first()).toContainText('Lẩu nấm ba báo cáo');
  const grouped = api.requests.filter((request) => request.startsWith('/groups?')).map((request) => new URLSearchParams(request.slice(8)));
  expect(grouped.at(-1)?.get('sort')).toBe('MOST_REPORTED');
  expect(grouped[0].get('sort')).toBe('NEWEST');
});

test('UC-06.1: the Administrator finds a reported post by its title or author, and sees when nothing matches', async ({ page }) => {
  const api = await mockApi(page);
  await page.goto('/admin/bao-cao');
  await page.getByRole('button', { name: /^Tất cả/ }).click();
  await expect(page.getByRole('article')).toHaveCount(2);

  const search = page.getByRole('search');
  await search.getByRole('searchbox', { name: 'Tìm theo tên bài hoặc tác giả' }).fill('  lẩu nấm ');
  await search.getByRole('searchbox').press('Enter');
  await expect(page.getByRole('article')).toHaveCount(1);
  await expect(page.getByRole('article')).toContainText('Lẩu nấm ba báo cáo');
  const lastGroups = () => new URLSearchParams(api.requests.filter((request) => request.startsWith('/groups?')).at(-1)?.slice(8));
  expect(lastGroups().get('query')).toBe('lẩu nấm');
  expect(lastGroups().get('page')).toBe('0');

  await search.getByRole('searchbox').fill('Món không có');
  await search.getByRole('button', { name: 'Tìm' }).click();
  await expect(page.getByText('Không tìm thấy bài viết nào khớp “Món không có”.')).toBeVisible();

  await search.getByRole('searchbox').fill('');
  await search.getByRole('button', { name: 'Tìm' }).click();
  await expect(page.getByRole('article')).toHaveCount(2);
  expect(lastGroups().get('query')).toBeNull();

  await page.getByRole('button', { name: 'Từng báo cáo' }).click();
  await expect(page.getByRole('search')).toHaveCount(0);
  await expect(page.getByLabel('Sắp xếp')).toHaveCount(0);
});
