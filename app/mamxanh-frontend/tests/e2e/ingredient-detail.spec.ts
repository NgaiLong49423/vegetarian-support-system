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

test('unit converter offers only the ingredient units and recalculates in both directions', async ({ page }) => {
  await page.route('**/api/v1/ingredients/42', (route) => route.fulfill({ json: { ...ingredient, name: 'Nước' } }));
  await page.route('**/api/v1/ingredients/42/units', (route) => route.fulfill({ json: [
    { id: 1, code: 'mg', name: 'miligram', dimension: 'MASS', gramsPerUnit: 0.001, approximate: false },
    { id: 2, code: 'g', name: 'gram', dimension: 'MASS', gramsPerUnit: 1, approximate: false },
    { id: 3, code: 'ml', name: 'mililit', dimension: 'VOLUME', gramsPerUnit: 1, approximate: false },
    { id: 4, code: 'L', name: 'lít', dimension: 'VOLUME', gramsPerUnit: 1000, approximate: false },
    { id: 5, code: 'cup', name: 'cup', dimension: 'VOLUME', gramsPerUnit: 240, approximate: true },
    { id: 6, code: 'quả', name: 'quả', dimension: 'COUNT', gramsPerUnit: 100, approximate: true },
  ] }));
  await page.goto('/nguyen-lieu/42');
  const converter = page.getByRole('region', { name: 'Quy đổi đơn vị' });
  const leftValue = converter.getByLabel('Số lượng thứ nhất');
  const rightValue = converter.getByLabel('Số lượng quy đổi');
  const leftUnit = converter.getByLabel('Đơn vị thứ nhất');
  const rightUnit = converter.getByLabel('Đơn vị quy đổi');
  await expect(leftUnit.locator('option')).toHaveCount(6);
  await expect(converter.getByText(/ước tính/)).toBeVisible();
  await leftUnit.selectOption('1');
  await leftValue.fill('1000');
  await expect(rightValue).toHaveValue('1');
  await rightUnit.selectOption('4');
  await expect(rightValue).toHaveValue('0.001');
  await rightValue.fill('2');
  await expect(leftValue).toHaveValue('2000000');
  await leftUnit.selectOption('6');
  await expect(rightValue).toHaveValue('200000');
});

test('unit converter handles blank, negative and zero inputs without showing invalid conversions', async ({ page }) => {
  await page.route('**/api/v1/ingredients/42', (route) => route.fulfill({ json: { ...ingredient, name: 'Nước' } }));
  await page.route('**/api/v1/ingredients/42/units', (route) => route.fulfill({ json: [
    { id: 2, code: 'g', name: 'gram', dimension: 'MASS', gramsPerUnit: 1, approximate: false },
    { id: 3, code: 'ml', name: 'mililit', dimension: 'VOLUME', gramsPerUnit: 1, approximate: false },
  ] }));
  await page.goto('/nguyen-lieu/42');
  const left = page.getByLabel('Số lượng thứ nhất');
  const right = page.getByLabel('Số lượng quy đổi');
  await left.fill('');
  await expect(right).toHaveValue('');
  await left.fill('-1');
  await expect(right).toHaveValue('');
  await left.fill('0');
  await expect(right).toHaveValue('0');
  await expect(page.getByText(/ước tính/)).toHaveCount(0);
});

test('unit converter can retry a failed request and reports an ingredient with no units', async ({ page }) => {
  let attempts = 0;
  await page.route('**/api/v1/ingredients/42', (route) => route.fulfill({ json: ingredient }));
  await page.route('**/api/v1/ingredients/42/units', (route) => {
    attempts++;
    return route.fulfill(attempts === 1
      ? { status: 503, json: { status: 503, code: 'SERVICE_UNAVAILABLE' } }
      : { json: [] });
  });
  await page.goto('/nguyen-lieu/42');
  const converter = page.getByRole('region', { name: 'Quy đổi đơn vị' });
  await expect(converter.getByRole('alert')).toContainText('Không tải được đơn vị quy đổi.');
  await converter.getByRole('button', { name: 'Thử lại' }).click();
  await expect(converter.getByText('Nguyên liệu này chưa có đơn vị quy đổi khả dụng.')).toBeVisible();
  expect(attempts).toBe(2);
});
