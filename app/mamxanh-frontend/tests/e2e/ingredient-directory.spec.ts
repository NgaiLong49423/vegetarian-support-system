import { expect, test } from './baseFixtures';

const ingredients = [
  { ingredientId: 11, name: 'Bánh phở', ingredientGroup: 'Khác' },
  { ingredientId: 12, name: 'Bắp cải', ingredientGroup: 'Rau' },
];

test('public ingredient directory loads every page and transfers all selected IDs to recipe discovery', async ({ page }) => {
  const requestedPages: number[] = [];
  await page.route('**/api/v1/recipes/ingredient-options**', (route) => route.fulfill({ json: ingredients }));
  await page.route('**/api/v1/ingredients?**', (route) => {
    const index = Number(new URL(route.request().url()).searchParams.get('page'));
    requestedPages.push(index);
    return route.fulfill({ json: {
      content: [ingredients[index]], page: index, size: 100, totalElements: 2, totalPages: 2,
    } });
  });
  await page.route('**/api/v1/recipes/discovery?**', (route) => route.fulfill({ json: {
    items: [], page: 0, size: 12, hasNext: false,
  } }));
  await page.goto('/danh-muc-nguyen-lieu');
  const find = page.getByRole('button', { name: 'Tìm công thức', exact: true });
  await expect(find).toBeDisabled();
  const labels = page.getByRole('button', { name: 'Bánh phở', exact: true });
  await labels.press('Enter');
  await expect(labels).toHaveAttribute('aria-pressed', 'true');
  await page.getByRole('button', { name: 'Bỏ chọn Bánh phở', exact: true }).click();
  await expect(labels).toHaveAttribute('aria-pressed', 'false');
  const search = page.getByRole('textbox', { name: 'Tìm nguyên liệu trong danh mục' });
  await search.fill('BÁNH PHỞ');
  await expect(labels).toBeVisible();
  await expect(page.getByRole('button', { name: 'Bắp cải', exact: true })).toHaveCount(0);
  await labels.click();
  await search.fill('không tồn tại');
  await expect(page.getByRole('heading', { name: 'Không tìm thấy nguyên liệu' })).toBeVisible();
  await search.clear();
  await page.getByRole('button', { name: 'Bắp cải', exact: true }).click();
  await page.getByRole('button', { name: 'Bỏ chọn tất cả' }).click();
  await expect(find).toBeDisabled();
  await labels.click();
  await page.getByRole('button', { name: 'Bắp cải', exact: true }).click();
  const request = page.waitForRequest((request) => {
    const url = new URL(request.url());
    return url.pathname === '/api/v1/recipes/discovery'
      && url.searchParams.getAll('ingredientIds').length === 2;
  });
  await page.getByRole('button', { name: 'Tìm công thức (2)', exact: true }).click();
  expect(new Set(new URL((await request).url()).searchParams.getAll('ingredientIds'))).toEqual(new Set(['11', '12']));
  await expect(page).toHaveURL(/\/kham-pha\?/);
  expect(requestedPages).toEqual([0, 1]);
});

test('public ingredient directory retries a failed request and explains an empty catalog', async ({ page }) => {
  let attempts = 0;
  await page.route('**/api/v1/ingredients?**', (route) => {
    attempts += 1;
    return attempts === 1
      ? route.fulfill({ status: 503, json: { title: 'Unavailable' } })
      : route.fulfill({ json: { content: [], page: 0, size: 100, totalElements: 0, totalPages: 0 } });
  });
  await page.goto('/danh-muc-nguyen-lieu');
  await expect(page.getByRole('heading', { name: 'Không tải được danh mục' })).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Chưa có nguyên liệu' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Tìm công thức', exact: true })).toHaveCount(0);
  expect(attempts).toBe(2);
});
