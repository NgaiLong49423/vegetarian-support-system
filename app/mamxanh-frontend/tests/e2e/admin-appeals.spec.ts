import type { Page, Route } from '@playwright/test';
import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

// FR-28 / BR-27 Administrator appeal queue against a stateful mocked API. Decision rules (ALLOW_EDIT only on
// PENDING, row lock, restore) are proven by AdminAppealIntegrationTest on SQL Server.
type Status = 'PENDING' | 'EDIT_ALLOWED' | 'RESUBMITTED' | 'RESTORED' | 'KEPT_HIDDEN';

const recipe = (id: number, title: string) => ({
  id, authorId: 30, authorName: 'Chuyên gia Bình', authorAvatarUrl: null, title, description: 'Món chay gia đình',
  instructions: 'Nấu nấm với hạt sen trong 20 phút.', dishCategory: 'SOUP', vegetarianType: 'VEGAN', difficulty: 'EASY',
  servings: 2, prepTimeMin: 5, cookTimeMin: 20, youtubeUrl: null, status: 'HIDDEN', media: [],
  ingredients: [{ ingredientId: 21, name: 'Nấm rơm', customName: null, unitId: 1, unitCode: 'g', unitName: 'gram', quantity: 200 }],
  likes: 0, dislikes: 0, likePercentage: null, viewCount: 0,
});

function appeal(reportId: number, recipeId: number, title: string, appealStatus: Status) {
  return {
    appeal: {
      reportId, recipeId, recipeTitle: title, hideReason: 'Bài ghi sai nhãn thuần chay vì có nước mắm.',
      hiddenAt: '2026-11-20T08:00:00', appealStatus,
      appealMessage: 'Em đã dùng nước tương chay, không phải nước mắm, mong Admin xem lại.',
      appealedAt: '2026-11-21T09:00:00', resubmittedAt: appealStatus === 'RESUBMITTED' ? '2026-11-22T09:00:00' : null,
      adminReason: null as string | null, decidedAt: null as string | null,
    },
    author: { id: 30, displayName: 'Chuyên gia Bình', email: 'binh@example.test', accountStatus: 'ACTIVE' },
    recipe: null as ReturnType<typeof recipe> | null,
  };
}

type Api = { appeals: ReturnType<typeof appeal>[]; bodies: unknown[]; listQueries: string[]; conflict: boolean };

async function mockApi(page: Page): Promise<Api> {
  const api: Api = {
    appeals: [appeal(77, 48, 'Canh nấm hạt sen', 'PENDING'), appeal(78, 49, 'Gỏi cuốn rau củ', 'RESUBMITTED')],
    bodies: [], listQueries: [], conflict: false,
  };
  await page.route('**/api/v1/admin/appeals**', async (route: Route) => {
    const url = new URL(route.request().url());
    const path = url.pathname.replace(/^.*\/api\/v1\/admin\/appeals/, '');
    if (route.request().method() === 'GET' && path === '') {
      api.listQueries.push(url.search);
      const statuses = url.searchParams.getAll('status');
      const matching = api.appeals.filter((item) => !statuses.length || statuses.includes(item.appeal.appealStatus));
      const pageNumber = Number(url.searchParams.get('page') ?? 0);
      const size = Number(url.searchParams.get('size') ?? 20);
      const content = matching.slice(pageNumber * size, (pageNumber + 1) * size);
      return route.fulfill({ json: { content, page: pageNumber, size, totalElements: matching.length, totalPages: Math.ceil(matching.length / size) } });
    }
    const id = Number(path.split('/')[1]);
    const current = api.appeals.find((item) => item.appeal.reportId === id)!;
    if (route.request().method() === 'GET') {
      return route.fulfill({ json: { ...current, recipe: recipe(current.appeal.recipeId, current.appeal.recipeTitle) } });
    }
    if (api.conflict) {
      return route.fulfill({ status: 409, contentType: 'application/problem+json',
        body: JSON.stringify({ status: 409, code: 'APPEAL_STATE_INVALID', detail: 'Khiếu nại đang ở bước khác. Hãy tải lại trang.' }) });
    }
    const body = route.request().postDataJSON() as { decision: string; reason: string };
    api.bodies.push(body);
    const next: Status = body.decision === 'RESTORE' ? 'RESTORED' : body.decision === 'KEEP_HIDDEN' ? 'KEPT_HIDDEN' : 'EDIT_ALLOWED';
    current.appeal = { ...current.appeal, appealStatus: next, adminReason: body.reason, decidedAt: '2026-11-23T09:00:00' };
    return route.fulfill({ json: current });
  });
  return api;
}

const REASON = 'Đã đối chiếu nguyên liệu, bài đúng nhãn thuần chay.';

test.beforeEach(async ({ page }) => {
  await seedDemoSession(page, 'ADMIN');
});

test('queue opens on appeals awaiting a decision and shows the hidden post', async ({ page }) => {
  const api = await mockApi(page);
  await page.goto('/admin/khieu-nai');

  await expect(page.getByRole('heading', { name: 'Khiếu nại bài bị ẩn' })).toBeVisible();
  await expect(page.getByText('Canh nấm hạt sen')).toBeVisible();
  await expect(page.getByText('Gỏi cuốn rau củ')).toBeVisible();
  expect(api.listQueries[0]).toContain('status=PENDING&status=RESUBMITTED');

  await page.getByRole('button', { name: 'Xem khiếu nại Canh nấm hạt sen' }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByText('Em đã dùng nước tương chay, không phải nước mắm, mong Admin xem lại.')).toBeVisible();
  await expect(dialog.getByText('Nấu nấm với hạt sen trong 20 phút.')).toBeVisible();
  await expect(dialog.getByText('Bài ghi sai nhãn thuần chay vì có nước mắm.')).toBeVisible();
});

test('admin restores an appealed recipe with a reason', async ({ page }) => {
  const api = await mockApi(page);
  await page.goto('/admin/khieu-nai');

  await page.getByRole('button', { name: 'Xem khiếu nại Canh nấm hạt sen' }).click();
  await page.getByLabel('Mở lại bài').check();
  await page.getByLabel('Lý do kết luận').fill(REASON);
  await page.getByRole('button', { name: 'Xác nhận kết luận' }).click();

  await expect(page.getByText('Đã mở lại bài “Canh nấm hạt sen”.')).toBeVisible();
  expect(api.bodies).toEqual([{ decision: 'RESTORE', reason: REASON }]);
});

test('a resubmitted appeal offers no allow-edit option', async ({ page }) => {
  await mockApi(page);
  await page.goto('/admin/khieu-nai');

  await page.getByRole('button', { name: 'Xem khiếu nại Gỏi cuốn rau củ' }).click();
  await expect(page.getByText('Tác giả đã sửa bài và gửi duyệt lại.')).toBeVisible();
  await expect(page.getByLabel('Mở lại bài')).toBeVisible();
  await expect(page.getByLabel('Giữ ẩn')).toBeVisible();
  await expect(page.getByLabel('Cho phép sửa')).toHaveCount(0);
});

test('admin allows an edit on a pending appeal', async ({ page }) => {
  const api = await mockApi(page);
  await page.goto('/admin/khieu-nai');

  await page.getByRole('button', { name: 'Xem khiếu nại Canh nấm hạt sen' }).click();
  await page.getByLabel('Cho phép sửa').check();
  await page.getByLabel('Lý do kết luận').fill('Hãy sửa lại nhãn chế độ chay.');
  await page.getByRole('button', { name: 'Xác nhận kết luận' }).click();

  await expect(page.getByText('Đã cho phép tác giả sửa “Canh nấm hạt sen”. Bài vẫn đang ẩn.')).toBeVisible();
  expect(api.bodies).toEqual([{ decision: 'ALLOW_EDIT', reason: 'Hãy sửa lại nhãn chế độ chay.' }]);
});

test('reason under 10 characters cannot be confirmed', async ({ page }) => {
  const api = await mockApi(page);
  await page.goto('/admin/khieu-nai');

  await page.getByRole('button', { name: 'Xem khiếu nại Canh nấm hạt sen' }).click();
  await page.getByLabel('Giữ ẩn').check();
  await page.getByLabel('Lý do kết luận').fill('  quá ngắn ');
  await expect(page.getByRole('button', { name: 'Xác nhận kết luận' })).toBeDisabled();
  expect(api.bodies).toEqual([]);
});

test('a long queue is paged', async ({ page }) => {
  const api = await mockApi(page);
  api.appeals = Array.from({ length: 22 }, (_, index) =>
    appeal(100 + index, 200 + index, `Món chay số ${index + 1}`, 'PENDING'));
  await page.goto('/admin/khieu-nai');

  await expect(page.getByRole('heading', { name: 'Món chay số 20', exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Món chay số 21', exact: true })).toHaveCount(0);
  await expect(page.getByText('Trang 1/2')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Trang trước' })).toBeDisabled();

  await page.getByRole('button', { name: 'Trang sau' }).click();
  await expect(page.getByRole('heading', { name: 'Món chay số 22', exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Món chay số 1', exact: true })).toHaveCount(0);
  await expect(page.getByText('Trang 2/2')).toBeVisible();
  expect(api.listQueries.at(-1)).toContain('page=1');

  // Changing the filter starts again from the first page.
  await page.getByRole('button', { name: 'Tất cả' }).click();
  await expect(page.getByText('Trang 1/2')).toBeVisible();
  expect(api.listQueries.at(-1)).toContain('page=0');
});

test('deciding the only appeal on the last page goes back a page', async ({ page }) => {
  const api = await mockApi(page);
  api.appeals = Array.from({ length: 21 }, (_, index) =>
    appeal(100 + index, 200 + index, `Món chay số ${index + 1}`, 'PENDING'));
  await page.goto('/admin/khieu-nai');
  await page.getByRole('button', { name: 'Trang sau' }).click();

  await page.getByRole('button', { name: 'Xem khiếu nại Món chay số 21' }).click();
  await page.getByLabel('Mở lại bài').check();
  await page.getByLabel('Lý do kết luận').fill(REASON);
  await page.getByRole('button', { name: 'Xác nhận kết luận' }).click();

  await expect(page.getByText('Đã mở lại bài “Món chay số 21”.')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Món chay số 20', exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Trang sau' })).toHaveCount(0);
});

test('a conflict tells the admin to reload', async ({ page }) => {
  const api = await mockApi(page);
  api.conflict = true;
  await page.goto('/admin/khieu-nai');

  await page.getByRole('button', { name: 'Xem khiếu nại Canh nấm hạt sen' }).click();
  await page.getByLabel('Giữ ẩn').check();
  await page.getByLabel('Lý do kết luận').fill(REASON);
  await page.getByRole('button', { name: 'Xác nhận kết luận' }).click();

  await expect(page.getByRole('alert').filter({ hasText: 'Khiếu nại đang ở bước khác. Hãy tải lại trang.' })).toBeVisible();
});
