import type { Page, Request } from '@playwright/test';
import { expect, test } from './baseFixtures';

// FR-60 (Issue #70): compare two different PUBLISHED recipes per serving (BR-76).

const nutrition = (perServing: Record<string, number>, complete = true, missing: string[] = []) => ({
  complete,
  ingredientsMissingData: missing,
  total: {},
  perServing,
});

const detail = (recipeId: number, overrides: Record<string, unknown>) => ({
  recipeId,
  description: null,
  instructions: 'Nấu chín.',
  dishCategory: 'MAIN',
  dishCategoryLabel: 'Món chính',
  vegetarianType: 'VEGAN',
  vegetarianTypeLabel: 'Thuần chay',
  difficulty: 'EASY',
  difficultyLabel: 'Dễ',
  youtubeUrl: null,
  publishedAt: '2026-10-13T08:00:00',
  nutritionComplete: true,
  ingredientsWithoutNutrition: [],
  author: { userId: 22, displayName: 'Bếp Chay Lan', avatarUrl: null },
  statistics: { likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null, viewCount: 0 },
  media: [],
  ...overrides,
});

const tofu = detail(701, {
  title: 'Đậu hũ sốt cà chua',
  servings: 2,
  prepTimeMinutes: 10,
  cookTimeMinutes: 15,
  statistics: { likes: 3, dislikes: 1, reactionCount: 4, likePercentage: 75, viewCount: 12 },
  media: [{ blobUrl: 'https://example.test/701.jpg', mimeType: 'image/jpeg', displayOrder: 0, cover: true }],
  ingredients: [
    { ingredientId: 1, name: 'Đậu hũ non', quantity: 300, unitId: 1, unitCode: 'g', unitName: 'gram', nutritionSupported: true },
    { ingredientId: 3, name: 'Cà chua', quantity: 2, unitId: 5, unitCode: 'quả', unitName: 'quả', nutritionSupported: true },
    { ingredientId: 7, name: 'Hành lá', quantity: 10, unitId: 1, unitCode: 'g', unitName: 'gram', nutritionSupported: true },
  ],
  nutrition: nutrition({
    ENERGY_KCAL: 210.5, PROTEIN_G: 14.2, CARBOHYDRATE_G: 18, TOTAL_FAT_G: 6.8, FIBER_G: 4.2,
    CALCIUM_MG: 160, IRON_MG: 2.8, VITAMIN_B12_MCG: 0, ZINC_MG: 1.5,
  }),
});

const soup = detail(702, {
  title: 'Canh cà chua nấm rơm',
  dishCategory: 'SOUP',
  dishCategoryLabel: 'Món canh',
  vegetarianType: 'LACTO_OVO',
  vegetarianTypeLabel: 'Chay trứng sữa',
  servings: 4,
  prepTimeMinutes: 5,
  cookTimeMinutes: 20,
  author: { userId: 23, displayName: 'Bếp Nhà Mây', avatarUrl: null },
  ingredients: [
    { ingredientId: 3, name: 'Cà chua', quantity: 150, unitId: 1, unitCode: 'g', unitName: 'gram', nutritionSupported: true },
    { ingredientId: 9, name: 'Hành lá', quantity: 1, unitId: 6, unitCode: 'bó', unitName: 'bó', nutritionSupported: true },
    { ingredientId: 11, name: 'Nấm rơm', quantity: 200, unitId: 1, unitCode: 'g', unitName: 'gram', nutritionSupported: true },
  ],
  nutrition: nutrition({
    ENERGY_KCAL: 120, PROTEIN_G: 5.7, CARBOHYDRATE_G: 12.5, TOTAL_FAT_G: 2, FIBER_G: 6,
    CALCIUM_MG: 45, IRON_MG: 1.2, VITAMIN_B12_MCG: 0.4, ZINC_MG: 0.9,
  }),
});

const mushroom = detail(703, {
  title: 'Nấm tràm xào sả',
  servings: 2,
  prepTimeMinutes: 10,
  cookTimeMinutes: 10,
  nutritionComplete: false,
  ingredientsWithoutNutrition: ['Nấm tràm Phú Quốc'],
  ingredients: [
    { ingredientId: 1, name: 'Đậu hũ non', quantity: 100, unitId: 1, unitCode: 'g', unitName: 'gram', nutritionSupported: true },
    { ingredientId: 15, name: 'Nấm tràm Phú Quốc', quantity: 200, unitId: 1, unitCode: 'g', unitName: 'gram', nutritionSupported: false },
  ],
  nutrition: nutrition({
    ENERGY_KCAL: 15, PROTEIN_G: 1.5, CARBOHYDRATE_G: 1, TOTAL_FAT_G: 1, FIBER_G: 0.5,
    CALCIUM_MG: 10, IRON_MG: 0.3, VITAMIN_B12_MCG: 0, ZINC_MG: 0.2,
  }, false, ['Nấm tràm Phú Quốc']),
});

const details: Record<number, ReturnType<typeof detail>> = { 701: tofu, 702: soup, 703: mushroom };

const card = (recipe: ReturnType<typeof detail>) => ({
  id: recipe.recipeId,
  authorId: 22,
  authorName: (recipe.author as { displayName: string }).displayName,
  authorAvatarUrl: null,
  title: recipe.title,
  description: null,
  instructions: 'Nấu chín.',
  dishCategory: recipe.dishCategory,
  vegetarianType: recipe.vegetarianType,
  difficulty: 'EASY',
  servings: recipe.servings,
  prepTimeMin: recipe.prepTimeMinutes,
  cookTimeMin: recipe.cookTimeMinutes,
  youtubeUrl: null,
  status: 'PUBLISHED',
  media: [],
  ingredients: [],
  likes: 0,
  dislikes: 0,
  likePercentage: null,
  viewCount: 0,
});

async function publicApi(page: Page, search: (keyword: string) => Array<ReturnType<typeof detail>> = () => [tofu, soup]) {
  await page.route(/\/api\/v1\/recipes\/(\d+)$/, (route) => {
    const id = Number(new URL(route.request().url()).pathname.split('/').pop());
    const recipe = details[id];
    return recipe
      ? route.fulfill({ json: recipe })
      : route.fulfill({ status: 404, contentType: 'application/problem+json', json: {
        status: 404, code: 'RECIPE_NOT_FOUND', detail: 'Không tìm thấy công thức công khai.',
      } });
  });
  await page.route((url) => url.pathname === '/api/v1/recipes', (route) => {
    const keyword = new URL(route.request().url()).searchParams.get('keyword') ?? '';
    const items = search(keyword).map(card);
    return route.fulfill({ json: { items, page: 0, size: 8, totalElements: items.length, totalPages: 1 } });
  });
}

const nutritionTable = (page: Page) => page.getByRole('table', { name: 'Dinh dưỡng trên một khẩu phần' });
const ingredientTable = (page: Page) => page.getByRole('table', { name: 'Đối chiếu nguyên liệu' });
const overviewTable = (page: Page) => page.getByRole('table', { name: 'Thông tin tổng quan' });

test('AC-60.1, AC-60.3: Guest compares two published recipes per serving with A − B differences', async ({ page }) => {
  const authHeaders: Array<string | undefined> = [];
  page.on('request', (request) => {
    if (request.url().includes('/api/v1/')) authHeaders.push(request.headers().authorization);
  });
  await publicApi(page);
  await page.goto('/so-sanh?a=701&b=702');

  await expect(page.getByRole('heading', { level: 1, name: 'So sánh hai công thức' })).toBeVisible();
  await expect(page.getByRole('note').filter({ hasText: 'Bản xem thử giao diện' })).toHaveCount(0);

  const overview = overviewTable(page);
  await expect(overview.getByRole('row', { name: /Tác giả/ })).toContainText('Bếp Chay Lan');
  await expect(overview.getByRole('row', { name: /Tác giả/ })).toContainText('Bếp Nhà Mây');
  await expect(overview.getByRole('row', { name: /Loại ăn chay/ })).toContainText('Thuần chay');
  await expect(overview.getByRole('row', { name: /Loại ăn chay/ })).toContainText('Chay trứng sữa');
  await expect(overview.getByRole('row', { name: /Loại món/ })).toContainText('Món canh');
  await expect(overview.getByRole('row', { name: /Khẩu phần gốc/ })).toContainText('2 khẩu phần');
  await expect(overview.getByRole('row', { name: /Khẩu phần gốc/ })).toContainText('4 khẩu phần');
  await expect(overview.getByRole('row', { name: /Thời gian/ })).toContainText('Chuẩn bị 10 phút · Nấu 15 phút');
  await expect(overview.getByRole('row', { name: /Tỷ lệ thích/ })).toContainText('75%');
  await expect(overview.getByRole('row', { name: /Tỷ lệ thích/ })).toContainText('Chưa có đánh giá');
  await expect(page.getByRole('img', { name: 'Đậu hũ sốt cà chua' })).toHaveAttribute('src', 'https://example.test/701.jpg');

  const table = nutritionTable(page);
  await expect(table.getByRole('row')).toHaveCount(10);
  const energy = table.getByRole('row', { name: /Năng lượng/ }).getByRole('cell');
  await expect(energy.nth(0)).toHaveText('210,5 kcal');
  await expect(energy.nth(1)).toHaveText('120 kcal');
  await expect(energy.nth(2)).toHaveText('+90,5 kcal');
  const fiber = table.getByRole('row', { name: /Chất xơ/ }).getByRole('cell');
  await expect(fiber.nth(2)).toHaveText(/^[-−]1,8 g$/);
  const b12 = table.getByRole('row', { name: /Vitamin B12/ }).getByRole('cell');
  await expect(b12.nth(0)).toHaveText('0 mcg');
  await expect(b12.nth(2)).toHaveText(/^[-−]0,4 mcg$/);
  await expect(page.getByText('Mỗi cột tính trên một khẩu phần của công thức đó: A có 2 khẩu phần gốc, B có 4 khẩu phần gốc.')).toBeVisible();
  await expect(page.getByText(/khẩu phần của hai món có thể không có cùng khối lượng thành phẩm/)).toBeVisible();
  expect(authHeaders.every((header) => header === undefined)).toBeTruthy();
});

test('AC-60.4: ingredients line up only by ingredient_id and keep their original quantity and unit', async ({ page }) => {
  await publicApi(page);
  await page.goto('/so-sanh?a=701&b=702');

  const table = ingredientTable(page);
  const tomato = table.getByRole('row', { name: /Cà chua/ });
  await expect(tomato).toHaveCount(1);
  await expect(tomato.getByRole('cell').nth(0)).toHaveText('2 quả');
  await expect(tomato.getByRole('cell').nth(1)).toHaveText('150 gram');
  await expect(tomato).toContainText('Khác đơn vị, không so trực tiếp số lượng');
  // Same display name but different ingredient_id: never merged.
  await expect(table.getByRole('row', { name: /Hành lá/ })).toHaveCount(2);
  const tofuRow = table.getByRole('row', { name: /Đậu hũ non/ }).getByRole('cell');
  await expect(tofuRow.nth(0)).toHaveText('300 gram');
  await expect(tofuRow.nth(1)).toHaveText('—');
  const strawMushroom = table.getByRole('row', { name: /Nấm rơm/ }).getByRole('cell');
  await expect(strawMushroom.nth(0)).toHaveText('—');
  await expect(strawMushroom.nth(1)).toHaveText('200 gram');
});

test('AC-60.5: a recipe with missing nutrition data shows "Chưa đủ dữ liệu", never 0 and no difference', async ({ page }) => {
  await publicApi(page);
  await page.goto('/so-sanh?a=701&b=703');

  const table = nutritionTable(page);
  await expect(table.getByText('Chưa đủ dữ liệu')).toHaveCount(9);
  for (const label of ['Năng lượng', 'Chất đạm', 'Carbohydrate', 'Chất béo', 'Chất xơ', 'Canxi', 'Sắt', 'Vitamin B12', 'Kẽm']) {
    const cells = table.getByRole('row', { name: new RegExp(label) }).getByRole('cell');
    await expect(cells.nth(1)).toHaveText('Chưa đủ dữ liệu');
    await expect(cells.nth(2)).toHaveText('—');
  }
  await expect(table.getByRole('row', { name: /Năng lượng/ }).getByRole('cell').nth(0)).toHaveText('210,5 kcal');
  await expect(page.getByRole('note').filter({ hasText: 'Công thức B chưa đủ dữ liệu dinh dưỡng' })).toContainText('Nấm tràm Phú Quốc');
});

test('AC-60.6: the comparison has no sodium, health score, percentage difference or verdict', async ({ page }) => {
  await publicApi(page);
  await page.goto('/so-sanh?a=701&b=702');
  await expect(nutritionTable(page)).toBeVisible();

  await expect(page.getByRole('main')).not.toContainText(/Natri|muối|Health Score|điểm sức khỏe|tốt hơn|lành mạnh hơn/i);
  await expect(nutritionTable(page)).not.toContainText('%');
});

test('AC-60.2: the recipe in one slot cannot be chosen for the other slot', async ({ page }) => {
  await publicApi(page);
  await page.goto('/so-sanh?a=701');

  await expect(page.getByText('Chọn thêm một công thức để bắt đầu so sánh.')).toBeVisible();
  await expect(nutritionTable(page)).toHaveCount(0);
  const results = page.getByRole('list', { name: 'Kết quả tìm cho vị trí B' });
  const same = results.getByRole('button', { name: 'Chọn Đậu hũ sốt cà chua cho vị trí B' });
  await expect(same).toBeDisabled();
  await expect(results.getByRole('listitem').filter({ hasText: 'Đậu hũ sốt cà chua' })).toContainText('Đang ở vị trí A');
  await results.getByRole('button', { name: 'Chọn Canh cà chua nấm rơm cho vị trí B' }).click();
  await expect(page).toHaveURL(/\/so-sanh\?a=701&b=702$/);
  await expect(nutritionTable(page)).toBeVisible();

  await page.goto('/so-sanh?a=701&b=701');
  await expect(page.getByRole('alert')).toContainText('Hai vị trí phải là hai công thức khác nhau. Hãy chọn một công thức khác cho vị trí B.');
  await expect(nutritionTable(page)).toHaveCount(0);
});

test('UC-60.2: replacing one recipe recalculates the whole comparison', async ({ page }) => {
  const keywords: string[] = [];
  await publicApi(page, (keyword) => {
    keywords.push(keyword);
    return keyword === 'nấm tràm' ? [mushroom] : [tofu, soup];
  });
  await page.goto('/so-sanh?a=701&b=702');
  await expect(nutritionTable(page)).toBeVisible();

  await page.getByRole('button', { name: 'Thay công thức A' }).click();
  await page.getByRole('searchbox', { name: 'Tìm công thức cho vị trí A' }).fill('nấm tràm');
  await page.getByRole('button', { name: 'Chọn Nấm tràm xào sả cho vị trí A' }).click();

  await expect(page).toHaveURL(/\/so-sanh\?a=703&b=702$/);
  const energy = nutritionTable(page).getByRole('row', { name: /Năng lượng/ }).getByRole('cell');
  await expect(energy.nth(0)).toHaveText('Chưa đủ dữ liệu');
  await expect(energy.nth(1)).toHaveText('120 kcal');
  await expect(energy.nth(2)).toHaveText('—');
  expect(keywords).toContain('nấm tràm');
});

test('UC-60.5: a recipe that is no longer public is removed and the user is asked to choose another', async ({ page }) => {
  await publicApi(page);
  await page.goto('/so-sanh?a=701&b=799');

  await expect(page.getByRole('alert')).toContainText('Công thức ở vị trí B không còn công khai. Hãy chọn công thức khác.');
  await expect(nutritionTable(page)).toHaveCount(0);
  await expect(page.getByRole('list', { name: 'Kết quả tìm cho vị trí B' })).toBeVisible();
});

test('UC-60.2: when no other public recipe matches, the user can go back to explore', async ({ page }) => {
  await publicApi(page, () => [tofu]);
  await page.goto('/so-sanh?a=701');

  await expect(page.getByText('Không còn công thức công khai khác phù hợp.')).toBeVisible();
  await page.getByRole('link', { name: 'Quay lại khám phá' }).click();
  await expect(page).toHaveURL(/\/kham-pha$/);
});

test('A failed recipe load can be retried', async ({ page }) => {
  await publicApi(page);
  let failures = 1;
  await page.route(/\/api\/v1\/recipes\/702$/, (route) => {
    if (failures-- > 0) return route.fulfill({ status: 500, contentType: 'application/problem+json', json: { status: 500, code: 'INTERNAL_ERROR', detail: 'Hệ thống gặp lỗi. Vui lòng thử lại sau.' } });
    return route.fulfill({ json: soup });
  });
  await page.goto('/so-sanh?a=701&b=702');

  await expect(page.getByRole('alert')).toContainText('Hệ thống gặp lỗi. Vui lòng thử lại sau.');
  await page.getByRole('button', { name: 'Thử tải lại công thức B' }).click();
  await expect(nutritionTable(page)).toBeVisible();
});

test('AC-60.7: comparing only reads public data, keeps no history and offers no PDF export', async ({ page }) => {
  const writes: Request[] = [];
  page.on('request', (request) => {
    if (request.url().includes('/api/v1/') && request.method() !== 'GET') writes.push(request);
  });
  await publicApi(page);
  await page.goto('/so-sanh?a=701&b=702');
  await expect(nutritionTable(page)).toBeVisible();

  await expect(page.getByRole('button', { name: /PDF|Xuất|^In / })).toHaveCount(0);
  await expect(page.getByRole('link', { name: /PDF|Xuất|Chia sẻ/ })).toHaveCount(0);
  const stored = await page.evaluate(() => [...Object.keys(localStorage), ...Object.keys(sessionStorage)]);
  expect(stored.filter((key) => /compare|so-sanh|so_sanh/i.test(key))).toEqual([]);

  await page.goto('/kham-pha');
  await page.goto('/so-sanh');
  await expect(page.getByText('Chọn hai công thức công khai khác nhau để xem bảng so sánh.')).toBeVisible();
  await expect(page.getByRole('list', { name: 'Kết quả tìm cho vị trí A' })).toBeVisible();
  expect(writes).toEqual([]);
});

test('AC-60.8: on a phone the comparison is grouped without horizontal page scroll', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 780 });
  await publicApi(page);
  await page.goto('/so-sanh?a=701&b=702');

  const energy = nutritionTable(page).getByRole('row', { name: /Năng lượng/ });
  await expect(energy).toBeVisible();
  await expect(energy.getByText('A', { exact: true })).toBeVisible();
  await expect(energy.getByText('B', { exact: true })).toBeVisible();
  await expect(energy.getByText('A − B', { exact: true })).toBeVisible();
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(0);
});

test('UC-60.1: comparison starts from a Recipe Card and from both recipe detail routes', async ({ page }) => {
  await publicApi(page, () => [tofu]);
  await page.route(/\/api\/v1\/recipes\/701\/(comments|related)/, (route) => route.fulfill({ json: {
    items: [], content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, commentCount: 0,
  } }));
  await page.route((url) => url.pathname === '/api/v1/recipes/form-options', (route) => route.fulfill({ json: {
    dishCategories: [], vegetarianTypes: [], difficulties: [], units: [],
  } }));

  await page.goto('/kham-pha');
  await page.getByRole('link', { name: 'So sánh Đậu hũ sốt cà chua' }).click();
  await expect(page).toHaveURL(/\/so-sanh\?a=701$/);
  await expect(page.getByRole('region', { name: 'Công thức A' })).toContainText('Đậu hũ sốt cà chua');

  for (const path of ['/cong-thuc/id/701', '/cong-thuc/701']) {
    await page.goto(path);
    await page.getByRole('link', { name: 'So sánh với công thức khác' }).click();
    await expect(page).toHaveURL(/\/so-sanh\?a=701$/);
  }
});

test('UC-60.1: a Member starts a comparison from the saved recipe list', async ({ page }) => {
  await page.addInitScript(() => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
    accessToken: 'isolated-test-token',
    expiresAt: Date.now() + 600_000,
    account: { id: 17, displayName: 'Thành viên kiểm thử', email: 'member@test.local', role: 'CUSTOMER', avatarUrl: null },
  })));
  await publicApi(page);
  await page.route((url) => url.pathname === '/api/v1/saved-recipes', (route) => route.fulfill({ json: {
    content: [{
      recipeId: 702, title: soup.title, description: null, coverUrl: null, authorName: 'Bếp Nhà Mây',
      available: true, unavailableMessage: null, savedAt: '2026-10-12T08:00:00',
    }],
    page: 0, size: 12, totalElements: 1, totalPages: 1,
  } }));

  await page.goto('/ho-so');
  await page.getByRole('link', { name: 'So sánh Canh cà chua nấm rơm' }).click();
  await expect(page).toHaveURL(/\/so-sanh\?a=702$/);
  await expect(page.getByRole('region', { name: 'Công thức A' })).toContainText('Canh cà chua nấm rơm');
});

test('picker search errors can be retried, a replacement can be cancelled and invalid IDs are ignored', async ({ page }) => {
  await publicApi(page);
  let searchFailures = 1;
  await page.route((url) => url.pathname === '/api/v1/recipes', (route) => {
    if (searchFailures-- > 0) return route.fulfill({ status: 503, contentType: 'application/problem+json', json: {
      status: 503, code: 'SERVICE_UNAVAILABLE', detail: 'Dịch vụ tạm thời gián đoạn.',
    } });
    return route.fulfill({ json: { items: [card(tofu), card(soup), card(mushroom)], page: 0, size: 8, totalElements: 3, totalPages: 1 } });
  });
  await page.goto('/so-sanh?a=701&b=702');
  await expect(nutritionTable(page)).toBeVisible();

  await page.getByRole('button', { name: 'Thay công thức B' }).click();
  await expect(page.getByRole('alert')).toContainText('Dịch vụ tạm thời gián đoạn.');
  await page.getByRole('button', { name: 'Thử tìm lại' }).click();
  await expect(page.getByRole('list', { name: 'Kết quả tìm cho vị trí B' }).getByRole('listitem')).toHaveCount(3);
  await page.getByRole('button', { name: 'Giữ công thức hiện tại' }).click();
  await expect(page.getByRole('list', { name: 'Kết quả tìm cho vị trí B' })).toHaveCount(0);
  await expect(nutritionTable(page).getByRole('row', { name: /Năng lượng/ }).getByRole('cell').nth(1)).toHaveText('120 kcal');

  await page.goto('/so-sanh?a=abc&b=0');
  await expect(page.getByText('Chọn hai công thức công khai khác nhau để xem bảng so sánh.')).toBeVisible();
  await expect(nutritionTable(page)).toHaveCount(0);
});
