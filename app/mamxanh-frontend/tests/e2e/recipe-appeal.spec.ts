import type { Page, Route } from '@playwright/test';
import { expect, test } from './baseFixtures';

// FR-28 / BR-27 author appeal on a hidden Recipe Post against a stateful mocked API. State rules
// (one appeal, edit unlock, resubmit) are proven by RecipeAppealIntegrationTest on SQL Server.
const hiddenRecipe = {
  id: 48,
  authorId: 12,
  authorName: 'Chuyên gia Bình',
  authorAvatarUrl: null,
  title: 'Canh nấm hạt sen',
  description: 'Món chay gia đình',
  instructions: 'Nấu nấm với hạt sen trong 20 phút.',
  dishCategory: 'SOUP',
  vegetarianType: 'VEGAN',
  difficulty: 'EASY',
  servings: 2,
  prepTimeMin: 5,
  cookTimeMin: 20,
  youtubeUrl: null,
  status: 'HIDDEN',
  media: [],
  ingredients: [{ ingredientId: 21, name: 'Nấm rơm', customName: null, unitId: 1, unitCode: 'g', unitName: 'gram', quantity: 200 }],
  likes: 0,
  dislikes: 0,
  likePercentage: null,
  viewCount: 0,
};

type Appeal = {
  reportId: number;
  recipeId: number;
  recipeTitle: string;
  hideReason: string;
  hiddenAt: string;
  appealStatus: null | 'PENDING' | 'EDIT_ALLOWED' | 'RESUBMITTED' | 'RESTORED' | 'KEPT_HIDDEN';
  appealMessage: string | null;
  appealedAt: string | null;
  resubmittedAt: string | null;
  adminReason: string | null;
  decidedAt: string | null;
};

const MESSAGE = 'Em đã dùng nước tương chay, không phải nước mắm, mong Admin xem lại.';

function appeal(overrides: Partial<Appeal> = {}): Appeal {
  return {
    reportId: 77, recipeId: 48, recipeTitle: 'Canh nấm hạt sen', hideReason: 'Bài ghi sai nhãn thuần chay vì có nước mắm.',
    hiddenAt: '2026-11-20T08:00:00', appealStatus: null, appealMessage: null, appealedAt: null, resubmittedAt: null,
    adminReason: null, decidedAt: null, ...overrides,
  };
}

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    if (sessionStorage.getItem('mamxanh.auth')) return;
    sessionStorage.setItem('mamxanh.auth', JSON.stringify({
      accessToken: 'playwright-only-token',
      expiresAt: Date.now() + 60 * 60 * 1000,
      account: { id: 12, displayName: 'Chuyên gia Bình', email: 'expert@example.test', role: 'EXPERT', accountStatus: 'ACTIVE', emailVerified: true },
    }));
  });
});

async function mockApi(page: Page, initial: Appeal) {
  const api = { appeal: initial, submitted: [] as unknown[], resubmits: 0 };
  await page.route('**/api/v1/recipes/mine**', (route) => route.fulfill({
    json: { items: [hiddenRecipe], page: 0, size: 50, totalElements: 1, totalPages: 1 },
  }));
  await page.route('**/api/v1/recipes/48/manage', (route) => route.fulfill({ json: hiddenRecipe }));
  await page.route('**/api/v1/recipes/reference-data**', (route) => route.fulfill({
    json: { ingredients: [{ ingredientId: 21, name: 'Nấm rơm', unitId: 1, unitCode: 'g', unitName: 'gram' }] },
  }));
  await page.route('**/api/v1/recipes/48/appeal**', async (route: Route) => {
    const request = route.request();
    if (request.method() === 'GET') return route.fulfill({ json: api.appeal });
    if (request.url().endsWith('/resubmit')) {
      api.resubmits += 1;
      api.appeal = { ...api.appeal, appealStatus: 'RESUBMITTED', resubmittedAt: '2026-11-22T09:00:00' };
      return route.fulfill({ json: api.appeal });
    }
    api.submitted.push(request.postDataJSON());
    api.appeal = { ...api.appeal, appealStatus: 'PENDING', appealMessage: MESSAGE, appealedAt: '2026-11-21T09:00:00' };
    return route.fulfill({ status: 201, json: api.appeal });
  });
  return api;
}

test('expert submits an appeal for a hidden recipe and sees it pending', async ({ page }) => {
  const api = await mockApi(page, appeal());
  await page.goto('/ho-so/chuyen-gia');

  await expect(page.getByText('Bài ghi sai nhãn thuần chay vì có nước mắm.')).toBeVisible();
  await page.getByRole('button', { name: 'Khiếu nại' }).click();
  await page.getByLabel('Lời khiếu nại').fill(MESSAGE);
  await page.getByRole('button', { name: 'Gửi khiếu nại' }).click();

  await expect(page.getByText('Đã gửi khiếu nại, đang chờ Admin xem xét.')).toBeVisible();
  expect(api.submitted).toEqual([{ message: MESSAGE }]);
  await expect(page.getByRole('button', { name: 'Khiếu nại' })).toHaveCount(0);
});

test('appeal message under 10 characters cannot be sent', async ({ page }) => {
  const api = await mockApi(page, appeal());
  await page.goto('/ho-so/chuyen-gia');

  await page.getByRole('button', { name: 'Khiếu nại' }).click();
  await page.getByLabel('Lời khiếu nại').fill('   ngắn   ');
  await expect(page.getByRole('button', { name: 'Gửi khiếu nại' })).toBeDisabled();
  await expect(page.getByText(/^4\/1000/)).toBeVisible();
  expect(api.submitted).toEqual([]);
});

test('edit allowed shows the admin reason, an edit link and resubmit', async ({ page }) => {
  const api = await mockApi(page, appeal({ appealStatus: 'EDIT_ALLOWED', appealMessage: MESSAGE, adminReason: 'Hãy sửa lại nhãn chế độ chay.' }));
  await page.goto('/ho-so/chuyen-gia');

  await expect(page.getByText('Hãy sửa lại nhãn chế độ chay.')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Sửa bài đang ẩn' })).toHaveAttribute('href', '/cong-thuc/48/chinh-sua');
  await page.getByRole('button', { name: 'Gửi duyệt lại' }).click();

  await expect(page.getByText('Đã gửi duyệt lại, đang chờ Admin xem xét bản đã sửa.')).toBeVisible();
  expect(api.resubmits).toBe(1);
});

test('a step refused as stale reloads the appeal to its current state', async ({ page }) => {
  const api = await mockApi(page, appeal({ appealStatus: 'EDIT_ALLOWED', appealMessage: MESSAGE, adminReason: 'Hãy sửa lại nhãn chế độ chay.' }));
  await page.goto('/ho-so/chuyen-gia');
  await expect(page.getByRole('button', { name: 'Gửi duyệt lại' })).toBeVisible();

  // Another tab already resubmitted: the server moved on and refuses this tab's step.
  api.appeal = { ...api.appeal, appealStatus: 'RESUBMITTED', resubmittedAt: '2026-11-22T09:00:00' };
  await page.route('**/api/v1/recipes/48/appeal/resubmit', (route) => route.fulfill({
    status: 409, json: { code: 'APPEAL_STATE_INVALID', message: 'Khiếu nại đang ở bước khác. Hãy tải lại trang.' },
  }));
  await page.getByRole('button', { name: 'Gửi duyệt lại' }).click();

  await expect(page.getByText('Đã gửi duyệt lại, đang chờ Admin xem xét bản đã sửa.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Gửi duyệt lại' })).toHaveCount(0);
});

test('a failed first load can be retried', async ({ page }) => {
  await mockApi(page, appeal());
  let failures = 1;
  await page.route('**/api/v1/recipes/48/appeal', async (route) => {
    if (route.request().method() === 'GET' && failures > 0) {
      failures -= 1;
      return route.fulfill({ status: 500, json: { code: 'INTERNAL_ERROR', message: 'Lỗi máy chủ.' } });
    }
    return route.fallback();
  });
  await page.goto('/ho-so/chuyen-gia');

  await expect(page.getByRole('alert').filter({ hasText: 'Yêu cầu không thành công' })).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();

  await expect(page.getByText('Bài ghi sai nhãn thuần chay vì có nước mắm.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Khiếu nại' })).toBeVisible();
});

test('kept hidden shows the final decision without actions', async ({ page }) => {
  await mockApi(page, appeal({ appealStatus: 'KEPT_HIDDEN', appealMessage: MESSAGE, adminReason: 'Ảnh bìa vẫn có món mặn.' }));
  await page.goto('/ho-so/chuyen-gia');

  await expect(page.getByText('Admin giữ ẩn bài sau khi xem khiếu nại.')).toBeVisible();
  await expect(page.getByText('Ảnh bìa vẫn có món mặn.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Khiếu nại' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Gửi duyệt lại' })).toHaveCount(0);
});

test('edit page opens for an edit-allowed hidden recipe and says it stays hidden', async ({ page }) => {
  await mockApi(page, appeal({ appealStatus: 'EDIT_ALLOWED', appealMessage: MESSAGE, adminReason: 'Hãy sửa lại nhãn chế độ chay.' }));
  await page.goto('/cong-thuc/48/chinh-sua');

  await expect(page.getByLabel('Tên món *')).toHaveValue('Canh nấm hạt sen');
  await expect(page.getByText('Bài vẫn đang ẩn. Sau khi lưu, hãy gửi duyệt lại để Admin xem xét.')).toBeVisible();
});

test('pending appeal keeps the edit page blocked', async ({ page }) => {
  await mockApi(page, appeal({ appealStatus: 'PENDING', appealMessage: MESSAGE }));
  await page.goto('/cong-thuc/48/chinh-sua');

  await expect(page.getByText(/Bạn có thể xem bài nhưng không thể sửa/)).toBeVisible();
  await expect(page.getByLabel('Tên món *')).toHaveCount(0);
});
