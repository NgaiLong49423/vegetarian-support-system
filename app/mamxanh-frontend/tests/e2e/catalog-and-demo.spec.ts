import { readFile } from 'node:fs/promises';
import { expect, test } from './baseFixtures';

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

test('recipe ingredient rows support free text, units and direct-mass validation in the demo UI', async ({ page }) => {
  await page.goto('/dang-nhap');
  await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
  await page.getByRole('button', { name: /Tài khoản Lan Anh/ }).click();
  await page.getByRole('button', { name: 'Expert', exact: true }).click();
  await page.getByRole('link', { name: 'Đăng công thức mới' }).click();
  await page.getByPlaceholder('VD: Đậu hũ non sốt nấm đông cô tiêu xanh').fill('Đậu hũ kho nấm');
  await page.getByPlaceholder('Chia sẻ nguồn cảm hứng, hương vị đặc trưng và bí quyết của món ăn...').fill('Công thức bữa tối thuần chay với đậu hũ và nấm.');
  await page.getByRole('button', { name: /Chay Có Sữa/ }).click();
  await expect(page.getByPlaceholder('Tên nguyên liệu')).toHaveCount(1);
  await page.getByRole('button', { name: /Thêm nguyên liệu/ }).click();
  await expect(page.getByPlaceholder('Tên nguyên liệu')).toHaveCount(2);
  await page.getByLabel('Tên nguyên liệu dòng 2').fill('Cà rốt');
  await page.getByLabel('Số lượng nguyên liệu dòng 2').fill('2');
  await page.getByLabel('Đơn vị nguyên liệu dòng 2').selectOption('củ');
  await expect(page.getByLabel('Đơn vị nguyên liệu dòng 2')).toHaveValue('củ');
  await page.getByRole('button', { name: 'Xóa nguyên liệu dòng 1' }).click();
  await expect(page.getByPlaceholder('Tên nguyên liệu')).toHaveCount(1);
  await expect(page.getByText('100%', { exact: true }).first()).toBeVisible();

  await page.getByLabel('Đơn vị nguyên liệu dòng 1').selectOption('g');
  await page.getByLabel('Số lượng nguyên liệu dòng 1').fill('50');
  await page.getByRole('button', { name: 'Xuất bản công thức' }).click();
  await expect(page.getByRole('alert')).toContainText('tối thiểu 100g');

  await page.getByLabel('Số lượng nguyên liệu dòng 1').fill('150');
  await page.getByRole('button', { name: 'Xuất bản công thức' }).click();
  await expect(page.getByRole('alert')).toContainText('theo bước 100g');

  await page.getByLabel('Số lượng nguyên liệu dòng 1').fill('1.5');
  await page.getByLabel('Đơn vị nguyên liệu dòng 1').selectOption('kg');
  await page.getByRole('button', { name: 'Xuất bản công thức' }).click();
  await expect(page.getByRole('status')).toContainText('Recipe API chưa được tích hợp nên chưa thể đăng bài.');
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
