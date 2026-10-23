import { expect, test } from './baseFixtures';
import type { Page } from '@playwright/test';

const detail = (recipeId = 901) => ({
  recipeId, title: `Canh nấm ${recipeId}`, description: 'Món chay thanh nhẹ cho bữa cơm gia đình.',
  instructions: 'Đun nước dùng rồi cho nấm vào nấu chín.', dishCategory: 'SOUP', dishCategoryLabel: 'Món canh',
  vegetarianType: 'VEGAN', vegetarianTypeLabel: 'Thuần chay', difficulty: 'EASY', difficultyLabel: 'Dễ',
  servings: 2, prepTimeMinutes: 10, cookTimeMinutes: 15, youtubeUrl: null,
  publishedAt: '2026-10-01T08:00:00', nutritionComplete: true, ingredientsWithoutNutrition: [],
  author: { userId: 22, displayName: 'Bếp Chay', avatarUrl: null },
  nutrition: { complete: true, ingredientsMissingData: [], total: {}, perServing: {} },
  ingredients: [{ ingredientId: 1, name: 'Nấm rơm', quantity: 100, unitId: 1, unitCode: 'g', unitName: 'gram' }],
  media: [], statistics: { likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null, viewCount: 0 },
});

async function mockReads(page: Page, viewCount = 0) {
  const browseItems = [901, 902].map((id) => ({ ...detail(id), id, authorId: 22, authorName: 'Bếp Chay',
    authorAvatarUrl: null, prepTimeMin: 10, cookTimeMin: 15, status: 'PUBLISHED',
    likes: 0, dislikes: 0, likePercentage: null, viewCount }));
  await page.route(/\/api\/v1\/recipes\/90[12]$/, (route) => route.fulfill({ json: detail(Number(route.request().url().split('/').pop())) }));
  await page.route('**/api/v1/recipes/discovery?**', (route) => route.fulfill({ json: {
    items: browseItems,
    page: 0, size: 12, hasNext: false,
  } }));
  await page.route((url) => url.pathname === '/api/v1/recipes', (route) => route.fulfill({ json: {
    items: browseItems, page: 0, size: 4, totalElements: browseItems.length, totalPages: 1,
  } }));
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: {
    dishCategories: [], vegetarianTypes: [], difficulties: [], units: [],
  } }));
}

test('Guest gửi sau khi detail hiện, giữ UUID qua reload và lấy số từ response kể cả counted=false', async ({ page, context }) => {
  await mockReads(page);
  const identities: string[] = [];
  await page.route('**/api/v1/recipes/901/views', async (route) => {
    await expect(page.getByRole('heading', { name: 'Canh nấm 901' })).toBeVisible();
    expect(route.request().headers().authorization).toBeUndefined();
    const body = route.request().postDataJSON();
    expect(Object.keys(body)).toEqual(['anonymousViewerId']);
    identities.push(body.anonymousViewerId);
    expect(body.anonymousViewerId).toMatch(/^[0-9a-f-]{36}$/);
    await route.fulfill({ json: { recipeId: 901, counted: identities.length === 1, viewCount: 12501 } });
  });
  await page.goto('/cong-thuc/id/901');
  await expect(page.getByText('12.501 lượt xem', { exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByText('12.501 lượt xem', { exact: true })).toBeVisible();
  expect(identities).toHaveLength(2);
  expect(identities[1]).toBe(identities[0]);
  const cookie = (await context.cookies()).find((item) => item.name === 'mamxanh.recipe-viewer');
  expect(cookie?.sameSite).toBe('Lax');
  expect(cookie?.path).toBe('/');
});

test('Member gửi Bearer với body rỗng, không tạo định danh Guest', async ({ page, context }) => {
  await page.addInitScript(() => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
    accessToken: 'playwright-only-token', expiresAt: Date.now() + 3600000,
    account: { id: 12, displayName: 'Thành viên', role: 'CUSTOMER', email: 'views@example.test', accountStatus: 'ACTIVE', emailVerified: true },
  })));
  await mockReads(page);
  await page.route('**/api/v1/recipes/901/views', (route) => {
    expect(route.request().headers().authorization).toBe('Bearer playwright-only-token');
    expect(route.request().postDataJSON()).toEqual({});
    return route.fulfill({ json: { recipeId: 901, counted: true, viewCount: 1 } });
  });
  await page.goto('/cong-thuc/id/901');
  await expect(page.getByText('1 lượt xem', { exact: true })).toBeVisible();
  expect((await context.cookies()).some((item) => item.name === 'mamxanh.recipe-viewer')).toBe(false);
});

for (const failure of ['network', '503'] as const) {
  test(`${failure}: đọc được công thức, không tăng giả; thử lại cùng UUID bằng bàn phím`, async ({ page }) => {
    await mockReads(page);
    const identities: string[] = [];
    await page.route('**/api/v1/recipes/901/views', async (route) => {
      identities.push(route.request().postDataJSON().anonymousViewerId);
      if (identities.length === 1) {
        if (failure === 'network') return route.abort('failed');
        return route.fulfill({ status: 503, contentType: 'application/problem+json', json: {
          status: 503, code: 'RECIPE_VIEW_UNAVAILABLE', detail: 'Vui lòng thử lại.',
        } });
      }
      return route.fulfill({ json: { recipeId: 901, counted: false, viewCount: 1 } });
    });
    await page.goto('/cong-thuc/id/901');
    await expect(page.getByText('0 lượt xem', { exact: true })).toBeVisible();
    await expect(page.getByText('Đun nước dùng rồi cho nấm vào nấu chín.')).toBeVisible();
    const retry = page.getByRole('button', { name: 'Thử lại lượt xem' });
    await expect(retry).toBeVisible();
    await retry.focus();
    await page.keyboard.press('Enter');
    await expect(page.getByText('1 lượt xem', { exact: true })).toBeVisible();
    await expect(retry).toHaveCount(0);
    expect(identities).toHaveLength(2);
    expect(identities[0]).toBe(identities[1]);
  });
}

test('Detail không tồn tại không gửi sự kiện', async ({ page }) => {
  let views = 0;
  await page.route('**/api/v1/recipes/901', (route) => route.fulfill({ status: 404, json: { status: 404, detail: 'Không tìm thấy công thức.' } }));
  await page.route('**/api/v1/recipes/*/views', (route) => { views++; return route.fulfill({ json: {} }); });
  await page.goto('/cong-thuc/id/901');
  await expect(page.getByRole('heading', { name: 'Không thể mở công thức' })).toBeVisible();
  expect(views).toBe(0);
});

test('Phản hồi ghi nhận muộn không thay số của công thức tiếp theo; quay lại danh sách đọc số mới', async ({ page }) => {
  await mockReads(page, 77);
  let finish!: () => void;
  let started!: () => void;
  const waiting = new Promise<void>((resolve) => { started = resolve; });
  const released = new Promise<void>((resolve) => { finish = resolve; });
  await page.route('**/api/v1/recipes/901/views', async (route) => {
    started();
    await released;
    await route.fulfill({ json: { recipeId: 901, counted: true, viewCount: 999 } });
  });
  await page.route('**/api/v1/recipes/902/views', (route) => route.fulfill({ json: { recipeId: 902, counted: true, viewCount: 2 } }));
  await page.goto('/cong-thuc/id/901');
  await waiting;
  await page.getByRole('link', { name: 'Khám phá', exact: true }).click();
  await expect(page.getByText('77 lượt xem', { exact: true })).toHaveCount(2);
  await page.getByRole('heading', { name: 'Canh nấm 902' }).click();
  await expect(page.getByText('2 lượt xem', { exact: true })).toBeVisible();
  finish();
  await page.waitForResponse((response) => response.url().endsWith('/901/views'));
  await expect(page.getByText('999 lượt xem', { exact: true })).toHaveCount(0);
});

test('Đường dẫn ID số tương thích cũng ghi nhận lượt xem', async ({ page }) => {
  await mockReads(page);
  await page.route('**/api/v1/recipes/901/views', (route) => route.fulfill({ json: { recipeId: 901, counted: true, viewCount: 1 } }));
  await page.goto('/cong-thuc/901');
  await expect(page.getByText('1 lượt xem', { exact: true })).toBeVisible();
});

test('Rời trang trước khi GET hoàn tất không ghi nhận lượt xem', async ({ page }) => {
  await mockReads(page);
  let views = 0;
  let release!: () => void;
  let notify!: () => void;
  const started = new Promise<void>((resolve) => { notify = resolve; });
  const waiting = new Promise<void>((resolve) => { release = resolve; });
  await page.route('**/api/v1/recipes/901', async (route) => {
    notify();
    await waiting;
    await route.fulfill({ json: detail() });
  });
  await page.route('**/api/v1/recipes/*/views', (route) => { views++; return route.fulfill({ json: {} }); });
  await page.goto('/cong-thuc/id/901');
  await started;
  await page.getByRole('button', { name: 'Khám phá', exact: true }).press('Enter');
  await page.getByRole('navigation', { name: 'Khám phá', exact: true }).getByRole('link', { name: 'Công thức', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Khám phá công thức chay' })).toBeVisible();
  const response = page.waitForResponse((result) => result.url().endsWith('/recipes/901'));
  release();
  await response;
  await expect(page.getByRole('heading', { name: 'Khám phá công thức chay' })).toBeVisible();
  expect(views).toBe(0);
});

test('Bearer bị từ chối kết thúc phiên, không gửi lại dưới danh tính Guest', async ({ page }) => {
  await page.addInitScript(() => {
    if (sessionStorage.getItem('mamxanh.auth')) return;
    sessionStorage.setItem('mamxanh.auth', JSON.stringify({
      accessToken: 'playwright-only-invalid-token', expiresAt: Date.now() + 3600000,
      account: { id: 12, displayName: 'Thành viên', role: 'CUSTOMER' },
    }));
  });
  await mockReads(page);
  let requests = 0;
  await page.route('**/api/v1/recipes/901/views', (route) => {
    requests++;
    expect(route.request().headers().authorization).toBe('Bearer playwright-only-invalid-token');
    return route.fulfill({ status: 401, json: { status: 401, code: 'UNAUTHORIZED', detail: 'Phiên không hợp lệ.' } });
  });
  await page.goto('/cong-thuc/id/901');
  await expect(page).toHaveURL(/\/dang-nhap$/);
  expect(await page.evaluate(() => sessionStorage.getItem('mamxanh.auth'))).toBeNull();
  expect(requests).toBe(1);
});

test('Most Viewed gửi đủ bốn khoảng thời gian; số trên thẻ vẫn là tổng lịch sử từ API', async ({ page }) => {
  await mockReads(page, 123);
  await page.goto('/kham-pha');
  await page.getByRole('combobox', { name: 'Sắp xếp theo' }).selectOption('MOST_VIEWED');
  for (const period of ['LAST_24_HOURS', 'LAST_7_DAYS', 'LAST_30_DAYS', 'ALL_TIME']) {
    const requested = page.waitForRequest((request) => {
      const url = new URL(request.url());
      return url.pathname === '/api/v1/recipes/discovery' && url.searchParams.get('sort') === 'MOST_VIEWED'
        && url.searchParams.get('viewPeriod') === period;
    });
    await page.getByRole('combobox', { name: 'Khung thời gian lượt xem' }).selectOption(period);
    await requested;
    await expect(page.getByText('123 lượt xem', { exact: true })).toHaveCount(2);
  }
});

test('Cookie bị chặn: không tạo lượt xem bằng định danh thay đổi mỗi lần', async ({ page, context }) => {
  await context.addInitScript(() => Object.defineProperty(document, 'cookie', { get: () => '', set: () => {} }));
  await mockReads(page);
  let views = 0;
  await page.route('**/api/v1/recipes/*/views', (route) => { views++; return route.fulfill({ json: {} }); });
  await page.goto('/cong-thuc/id/901');
  await expect(page.getByText(/Vui lòng cho phép cookie/)).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại lượt xem' }).click();
  await expect(page.getByText(/Vui lòng cho phép cookie/)).toBeVisible();
  await expect(page.getByText('0 lượt xem', { exact: true })).toBeVisible();
  expect(views).toBe(0);
});

for (const width of [360, 768, 1440]) {
  test(`Lượt xem trên detail và thẻ danh sách ở ${width}px`, async ({ page }, testInfo) => {
    await page.setViewportSize({ width, height: 900 });
    await mockReads(page, 12500);
    await page.route('**/api/v1/recipes/901/views', (route) => route.fulfill({ json: { recipeId: 901, counted: true, viewCount: 12501 } }));
    await page.goto('/cong-thuc/id/901');
    await expect(page.getByText('12.501 lượt xem', { exact: true })).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.screenshot({ path: testInfo.outputPath(`detail-${width}.png`), fullPage: true });
    await page.getByRole('link', { name: 'Khám phá', exact: true }).click();
    await expect(page.getByText('12.500 lượt xem', { exact: true })).toHaveCount(2);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.screenshot({ path: testInfo.outputPath(`explore-${width}.png`), fullPage: true });
    await page.goto('/trang-chu');
    for (const collection of ['Mới từ cộng đồng', 'Được yêu thích']) {
      await expect(page.locator(`[aria-label="${collection}"]`).getByText('12.500 lượt xem', { exact: true })).toHaveCount(2);
    }
    await page.screenshot({ path: testInfo.outputPath(`home-${width}.png`), fullPage: true });
  });
}
