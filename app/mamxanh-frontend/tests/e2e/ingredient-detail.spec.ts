import { expect, test } from './baseFixtures';

const ingredient = {
  ingredientId: 42, name: 'Phô mai thử nghiệm', ingredientGroup: 'Sữa', imageUrl: null,
  nutrition: {
    energyKcalPer100g: 333, proteinGramsPer100g: 23.81, carbohydrateGramsPer100g: 0,
    totalFatGramsPer100g: 28.57, fiberGramsPer100g: 0, calciumMgPer100g: 200,
    ironMgPer100g: null, vitaminB12MicrogramsPer100g: 0.0001, zincMgPer100g: null,
  },
};

const recipeOptions = { dishCategories: [{ code: 'SOUP', label: 'Món canh' }],
  vegetarianTypes: [{ code: 'VEGAN', label: 'Thuần chay' }, { code: 'LACTO', label: 'Chay có sữa' },
    { code: 'OVO', label: 'Chay có trứng' }, { code: 'LACTO_OVO', label: 'Chay có trứng và sữa' }],
  difficulties: [], units: [] };

test.beforeEach(async ({ page }) => {
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: recipeOptions }));
  await page.route('**/api/v1/recipes/discovery?**', (route) => route.fulfill({ json: {
    items: [], page: 0, size: 9, hasNext: false,
  } }));
});

test('detail keeps known zero, unknown data and small B12 values distinct and opens filtered recipes', async ({ page }) => {
  await page.route('**/api/v1/ingredients/42', (route) => route.fulfill({ json: ingredient }));
  await page.route('**/api/v1/recipes/ingredient-options**', (route) => route.fulfill({ json: [ingredient] }));
  await page.route('**/api/v1/recipes/discovery?**', (route) => route.fulfill({ json: {
    items: [], page: 0, size: 12, hasNext: false,
  } }));
  await page.route('**/api/v1/ingredients?**', (route) => route.fulfill({ json: {
    content: [ingredient], page: 0, size: 100, totalElements: 1, totalPages: 1,
  } }));
  await page.goto('/danh-muc-nguyen-lieu');
  await page.getByRole('link', { name: ingredient.name, exact: true }).click();
  await expect(page).toHaveURL('/nguyen-lieu/42');
  await expect(page.getByRole('heading', { name: ingredient.name, level: 1, exact: true })).toBeVisible();
  await expect(page.getByRole('img', { name: `Chưa có ảnh minh họa cho ${ingredient.name}` })).toBeVisible();
  const table = page.getByRole('region', { name: 'Giá trị trên 100 g' });
  await expect(table.getByText('0,0001 µg', { exact: true })).toBeVisible();
  await expect(table.getByText('0 g', { exact: true })).toHaveCount(2);
  await expect(table.getByText('Chưa có dữ liệu', { exact: true })).toHaveCount(2);
  const request = page.waitForRequest((request) => {
    const url = new URL(request.url());
    return url.pathname === '/api/v1/recipes/discovery' && url.searchParams.getAll('ingredientIds').includes('42')
      && url.searchParams.get('size') === '12';
  });
  await page.getByRole('button', { name: 'Tìm công thức', exact: true }).click();
  await request;
  await expect(page).toHaveURL(/ingredientId=42/);
});

const recipe = (id: number, title: string, vegetarianType = 'VEGAN') => ({
  id, title, vegetarianType, authorId: 1, authorName: 'Bếp Mâm Xanh', authorAvatarUrl: null,
  description: '', instructions: 'Nấu chín nguyên liệu.', dishCategory: 'SOUP', difficulty: 'EASY',
  servings: 2, prepTimeMin: 10, cookTimeMin: 15, youtubeUrl: null, status: 'PUBLISHED', media: [],
  ingredients: [{ ingredientId: 42, name: 'Bắp cải', quantity: 100, unitId: 1, unitCode: 'g', unitName: 'gram', customName: null }],
  likes: 4, dislikes: 0, likePercentage: 100, viewCount: 10,
});

test('ingredient articles load more and filter by recipe labels and sort while preserving the ingredient ID', async ({ page }) => {
  const requests: URLSearchParams[] = [];
  await page.route('**/api/v1/ingredients/42', (route) => route.fulfill({ json: { ...ingredient, name: 'Bắp cải' } }));
  await page.route('**/api/v1/recipes/discovery?**', (route) => {
    const params = new URL(route.request().url()).searchParams;
    requests.push(params);
    const pageIndex = Number(params.get('page'));
    const items = params.get('vegetarianType') === 'LACTO' ? []
      : [recipe(pageIndex === 0 ? 101 : 102, pageIndex === 0 ? 'Canh bắp cải' : 'Bắp cải nấu nấm')];
    return route.fulfill({ json: { items, page: pageIndex, size: 9, hasNext: !params.get('vegetarianType') && pageIndex === 0 } });
  });
  await page.goto('/nguyen-lieu/42');
  const section = page.getByRole('region', { name: 'Nấu gì với Bắp cải?' });
  await expect(section.getByRole('heading', { name: 'Canh bắp cải', exact: true })).toBeVisible();
  await expect(section.getByRole('link').filter({ has: page.getByRole('heading', { name: 'Canh bắp cải', exact: true }) })).toHaveAttribute('href', '/cong-thuc/id/101');
  await section.getByRole('button', { name: 'Xem thêm công thức' }).click();
  await expect(section.getByRole('heading', { name: 'Bắp cải nấu nấm' })).toBeVisible();
  await expect(section.getByRole('heading', { name: 'Canh bắp cải', exact: true })).toBeVisible();
  await expect(section.getByRole('button', { name: 'Xem thêm công thức' })).toHaveCount(0);
  await section.getByRole('button', { name: 'Chay có sữa', exact: true }).click();
  await expect(section.getByText('Chưa có bài công thức thuộc loại ăn chay này với nguyên liệu đang xem.')).toBeVisible();
  await expect(section.getByRole('heading', { name: 'Canh bắp cải', exact: true })).toHaveCount(0);
  await section.getByRole('button', { name: 'Thuần chay', exact: true }).click();
  await expect(section.getByRole('heading', { name: 'Canh bắp cải', exact: true })).toBeVisible();
  await section.getByRole('combobox', { name: 'Sắp xếp công thức theo nguyên liệu' }).selectOption('NEWEST');
  await expect.poll(() => requests.at(-1)?.get('sort')).toBe('NEWEST');
  expect(requests.every((params) => params.getAll('ingredientIds').join(',') === '42')).toBe(true);
  expect(requests.at(-1)?.get('page')).toBe('0');
  expect(requests.at(-1)?.get('vegetarianType')).toBe('VEGAN');
});

test('failed next page keeps existing recipe cards and retries the same page without duplicates', async ({ page }) => {
  let nextPageAttempts = 0;
  await page.route('**/api/v1/ingredients/42', (route) => route.fulfill({ json: ingredient }));
  await page.route('**/api/v1/recipes/discovery?**', (route) => {
    const params = new URL(route.request().url()).searchParams;
    const pageIndex = Number(params.get('page'));
    if (pageIndex === 1 && ++nextPageAttempts === 1) return route.fulfill({ status: 503, json: { status: 503 } });
    return route.fulfill({ json: { items: pageIndex === 0 ? [recipe(101, 'Bài đầu')]
      : [recipe(101, 'Bài đầu'), recipe(102, 'Bài tiếp theo')], page: pageIndex, size: 9, hasNext: pageIndex === 0 } });
  });
  await page.goto('/nguyen-lieu/42');
  const section = page.getByRole('region', { name: `Nấu gì với ${ingredient.name}?` });
  await expect(section.getByRole('heading', { name: 'Bài đầu', exact: true })).toBeVisible();
  await section.getByRole('button', { name: 'Xem thêm công thức' }).click();
  await expect(section.getByRole('alert')).toContainText('Không tải được bài công thức');
  await expect(section.getByRole('heading', { name: 'Bài đầu', exact: true })).toBeVisible();
  await section.getByRole('button', { name: 'Thử tải công thức' }).click();
  await expect(section.getByRole('heading', { name: 'Bài tiếp theo', exact: true })).toBeVisible();
  await expect(section.getByRole('heading', { name: 'Bài đầu', exact: true })).toHaveCount(1);
  expect(nextPageAttempts).toBe(2);
});

test('mobile detail explains missing nutrition and failed images without horizontal overflow', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.route('**/api/v1/ingredients/42', (route) => route.fulfill({ json: {
    ...ingredient, imageUrl: 'https://images.example.test/missing.png',
    nutrition: Object.fromEntries(Object.keys(ingredient.nutrition).map((key) => [key, null])),
  } }));
  await page.route('https://images.example.test/missing.png', (route) => route.fulfill({ status: 404, body: '' }));
  await page.goto('/nguyen-lieu/42');
  await expect(page.getByText('Chưa có dữ liệu dinh dưỡng', { exact: true })).toBeVisible();
  await expect(page.getByRole('img', { name: `Chưa có ảnh minh họa cho ${ingredient.name}` })).toBeVisible();
  const recipeHeading = page.getByRole('heading', { name: `Nấu gì với ${ingredient.name}?`, exact: true });
  await expect(recipeHeading).toBeVisible();
  expect((await recipeHeading.boundingBox())!.width).toBeGreaterThan(240);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
});

test('connection errors can retry while missing and invalid ingredient IDs show not found', async ({ page }) => {
  let attempts = 0;
  await page.route('**/api/v1/ingredients/42', (route) => {
    attempts += 1;
    return route.fulfill(attempts === 1
      ? { status: 503, json: { status: 503, code: 'UNAVAILABLE' } }
      : { json: ingredient });
  });
  await page.goto('/nguyen-lieu/42');
  await expect(page.getByRole('heading', { name: 'Không tải được nguyên liệu' })).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại', exact: true }).click();
  await expect(page.getByRole('heading', { name: ingredient.name, level: 1, exact: true })).toBeVisible();
  await page.route('**/api/v1/ingredients/99', (route) => route.fulfill({ status: 404,
    json: { status: 404, code: 'INGREDIENT_NOT_FOUND' } }));
  await page.goto('/nguyen-lieu/99');
  await expect(page.getByRole('heading', { name: 'Không tìm thấy nguyên liệu' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Thử lại', exact: true })).toHaveCount(0);
  await page.goto('/nguyen-lieu/not-an-id');
  await expect(page.getByRole('heading', { name: 'Không tìm thấy nguyên liệu' })).toBeVisible();
  await page.getByRole('button', { name: 'Về danh mục', exact: true }).click();
  await expect(page).toHaveURL('/danh-muc-nguyen-lieu');
});
