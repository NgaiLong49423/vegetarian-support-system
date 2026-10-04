import { readFile } from 'node:fs/promises';
import { expect, test } from './baseFixtures';

async function openRecipeFormAsDemoExpert(page: import('@playwright/test').Page) {
  await page.goto('/dang-nhap');
  await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
  await page.getByRole('button', { name: /Tài khoản Lan Anh/ }).click();
  await page.getByRole('button', { name: 'Expert', exact: true }).click();
  await page.getByRole('link', { name: 'Đăng công thức mới' }).click();
}

test('recipe search and filters show matching recipes and recover from an empty result', async ({ page }) => {
  await page.goto('/kham-pha');
  const search = page.getByPlaceholder('Tìm món chay hoặc nguyên liệu...');
  await search.fill('không có món này');
  await expect(page.getByText('Không tìm thấy công thức')).toBeVisible();
  await search.fill('');
  await page.getByRole('button', { name: 'Món nước', exact: true }).click();
  await expect(page.getByRole('button', { name: /Xoá lọc/ })).toBeVisible();
  await page.getByRole('button', { name: /Xoá lọc/ }).click();
  await expect(page.getByRole('button', { name: /Xoá lọc/ })).toHaveCount(0);
  await page.getByRole('combobox').selectOption('time');
  await expect(page.getByRole('combobox')).toHaveValue('time');
  await search.fill('Phở');
  await expect(page.getByRole('heading', { name: /Phở chay/i }).first()).toBeVisible();
});

test('shopping demo adds an item, filters purchased items and clears them', async ({ page }) => {
  await page.goto('/di-cho');
  const input = page.getByPlaceholder('Thêm thủ công nguyên liệu...');
  await input.fill('Nấm bào ngư cho bữa tối');
  await input.press('Enter');
  const item = page.locator('div.group').filter({ has: page.getByText('Nấm bào ngư cho bữa tối', { exact: true }) });
  await expect(item).toBeVisible();
  await expect(input).toHaveValue('');
  await item.getByRole('button').first().click();
  await page.getByRole('button', { name: /Cần mua/ }).click();
  await expect(item).toHaveCount(0);
  await page.getByRole('button', { name: /Đã xong/ }).click();
  await expect(item).toBeVisible();
  await page.getByRole('button', { name: /Xoá .* mục đã mua/ }).click();
  await expect(page.getByText('Không có mục nào trong danh mục này.')).toBeVisible();
});

test('shopping export contains the newly added item', async ({ page }) => {
  await page.goto('/di-cho');
  await page.getByPlaceholder('Thêm thủ công nguyên liệu...').fill('Rau cải cho ngày mai');
  await page.getByRole('button', { name: 'Thêm', exact: true }).click();
  const downloadPromise = page.waitForEvent('download');
  await page.getByRole('button', { name: 'Xuất file .TXT' }).click();
  const download = await downloadPromise;
  expect(download.suggestedFilename()).toBe('danh-sach-di-cho.txt');
  const downloadedPath = await download.path();
  expect(downloadedPath).not.toBeNull();
  expect(await readFile(downloadedPath!, 'utf8')).toContain('[ ] Rau cải cho ngày mai — vừa đủ');
});

test('recipe form uses catalog choices and reports that real login is still required', async ({ page }) => {
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: {
    dishCategories: [{ code: 'BRAISED', label: 'Món kho' }],
    vegetarianTypes: [{ code: 'VEGAN', label: 'Thuần chay' }],
    difficulties: [{ code: 'EASY', label: 'Dễ' }],
    units: [{ unitId: 1, code: 'g', name: 'gram', dimension: 'MASS' }],
  } }));
  await page.route('**/api/v1/recipes/ingredient-options**', (route) => route.fulfill({ json: [
    { ingredientId: 1, name: 'Đậu hũ' },
  ] }));
  await page.route('**/api/v1/recipes', (route) => route.fulfill({ status: 401, contentType: 'application/problem+json', json: {
    status: 401, code: 'UNAUTHENTICATED', title: 'Unauthenticated', detail: 'Bạn cần đăng nhập.',
  } }));

  await openRecipeFormAsDemoExpert(page);
  await page.getByLabel('Tên món *').fill('Đậu hũ kho cà chua');
  await page.getByLabel('Thể loại món').selectOption('BRAISED');
  await page.getByLabel('Loại ăn chay').selectOption('VEGAN');
  await page.getByLabel('Độ khó').selectOption('EASY');
  await page.getByLabel('Khẩu phần').fill('2');
  await page.getByLabel('Thời gian chuẩn bị').fill('10');
  await page.getByLabel('Thời gian nấu').fill('0');
  await page.getByLabel('Chọn nguyên liệu 1').fill('Đậu');
  await page.getByRole('button', { name: 'Đậu hũ', exact: true }).click();
  await page.getByLabel('Số lượng nguyên liệu 1').fill('200');
  await page.getByLabel('Đơn vị nguyên liệu 1').selectOption('1');
  await page.getByLabel('Hướng dẫn * (10–5.000 ký tự)').fill('Cắt đậu hũ, rim với cà chua đến khi thấm vị.');
  await page.getByRole('button', { name: 'Xuất bản công thức' }).click();

  await expect(page.getByRole('alert')).toContainText('Đăng nhập/JWT đang chờ Issue #6');
  await expect(page.getByText('Tài khoản demo không có quyền đăng.')).toBeVisible();
});

test('recipe image list requires one cover and blocks publishing until FR-14 upload is connected', async ({ page }) => {
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: {
    dishCategories: [], vegetarianTypes: [], difficulties: [], units: [],
  } }));
  await openRecipeFormAsDemoExpert(page);
  await page.getByLabel('Chọn tối đa 5 ảnh').setInputFiles([
    { name: 'one.png', mimeType: 'image/png', buffer: Buffer.from('one') },
    { name: 'two.png', mimeType: 'image/png', buffer: Buffer.from('two') },
  ]);
  await page.getByRole('button', { name: 'Xuất bản công thức' }).click();
  await expect(page.getByRole('alert')).toContainText('hãy chọn đúng 1 ảnh bìa');
  await page.getByLabel('Chọn one.png làm ảnh cover').check();
  await page.getByRole('button', { name: 'Xuất bản công thức' }).click();
  await expect(page.getByRole('alert')).toContainText('Upload ảnh thuộc FR-14');
});

test('community topics lead to article content and demo rating updates only once', async ({ page }) => {
  await page.goto('/cong-dong');
  await page.getByRole('button', { name: 'Dinh dưỡng', exact: true }).click();
  const articleLink = page.locator('main a[href^="/bai-viet/"]').first();
  await articleLink.click();
  await expect(page).toHaveURL(/\/bai-viet\//);
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  await page.getByRole('button', { name: '4 sao', exact: true }).click();
  await expect(page.getByText('Cảm ơn bạn đã đánh giá 4 sao cho bài viết!')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Đánh giá bài viết (37)' })).toBeVisible();
  await page.getByRole('button', { name: '5 sao', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Đánh giá bài viết (37)' })).toBeVisible();
});

test('profile demo switches tabs and toggles notification preference in the current page', async ({ page }) => {
  await page.goto('/ho-so');
  await page.getByRole('button', { name: 'Món yêu thích', exact: true }).click();
  await expect(page.locator('main a[href^="/cong-thuc/"]')).toHaveCount(3);
  await page.getByRole('button', { name: 'Sở thích ăn chay', exact: true }).click();
  await expect(page.getByText('Đậu phộng', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Tùy chọn', exact: true }).click();
  const toggle = page.locator('label').filter({ hasText: 'Nhận bản tin cộng đồng Mâm Xanh' }).getByRole('button');
  await expect(toggle).toHaveClass(/bg-brand-200/);
  await toggle.click();
  await expect(toggle).toHaveClass(/bg-brand-600/);
});

test('nutrition day selection updates the displayed day and opens a recipe', async ({ page }) => {
  await page.goto('/dinh-duong');
  const day = page.getByRole('button', { name: /Thứ Hai/ }).first();
  await day.click();
  await expect(day).toHaveClass(/bg-brand-600/);
  await expect(page.getByRole('heading', { name: /Thực đơn 3 Bữa Ngày/ })).toBeVisible();
  await page.getByRole('link', { name: 'Chi tiết công thức →' }).first().click();
  await expect(page).toHaveURL(/\/cong-thuc\//);
});
