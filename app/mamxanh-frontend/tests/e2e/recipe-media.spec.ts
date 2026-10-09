import { Buffer } from 'node:buffer';
import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

const imageFile = (name: string) => ({ name, mimeType: 'image/png', buffer: Buffer.from('test image') });

test.beforeEach(async ({ page }) => {
  await seedDemoSession(page, 'EXPERT');
  // Kiểm tra preview khi upload lỗi; không phải bằng chứng Azure/Backend thật.
  await page.route('**/api/v1/recipes/media/upload', (route) => route.fulfill({ status: 503, json: {} }));
  await page.goto('/dang-cong-thuc');
});

test('FR-14: từ chối định dạng và dung lượng ảnh không hợp lệ trước khi upload', async ({ page }) => {
  let uploads = 0;
  page.on('request', (request) => { if (request.url().endsWith('/recipes/media/upload')) uploads += 1; });
  const input = page.locator('#recipe-media-upload-input');
  await input.setInputFiles({ name: 'invalid.gif', mimeType: 'image/gif', buffer: Buffer.from('invalid') });
  await expect(page.getByText(/invalid.gif: Định dạng tệp không hợp lệ/)).toBeVisible();
  await input.setInputFiles({ name: 'large.png', mimeType: 'image/png', buffer: Buffer.alloc(5 * 1024 * 1024 + 1) });
  await expect(page.getByText(/large.png: Dung lượng tệp vượt quá/)).toBeVisible();
  await expect(page.getByAltText('Ảnh công thức 1', { exact: true })).toHaveCount(0);
  expect(uploads).toBe(0);
});

test('FR-14: preview giữ đúng ảnh bìa và thứ tự khi đổi vị trí, xóa bìa và xóa hết ảnh', async ({ page }) => {
  await page.locator('#recipe-media-upload-input').setInputFiles([imageFile('one.png'), imageFile('two.png')]);
  await expect(page.getByText('Đã tải 2/5 ảnh • Bắt buộc chọn đúng 1 ảnh bìa')).toBeVisible();
  const firstUrl = await page.getByAltText('Ảnh công thức 1', { exact: true }).getAttribute('src');
  const secondUrl = await page.getByAltText('Ảnh công thức 2', { exact: true }).getAttribute('src');
  expect(firstUrl).toMatch(/^blob:/);
  expect(secondUrl).toMatch(/^blob:/);
  expect(firstUrl).not.toBe(secondUrl);

  await expect(page.getByTitle('Di chuyển sang trái').first()).toBeDisabled();
  await expect(page.getByTitle('Di chuyển sang phải').last()).toBeDisabled();
  await page.getByTitle('Đặt làm ảnh bìa', { exact: true }).click();
  await page.getByTitle('Di chuyển sang trái').last().click();
  await expect(page.getByAltText('Ảnh công thức 1', { exact: true })).toHaveAttribute('src', secondUrl!);
  await expect(page.getByTitle('Đang là ảnh bìa')).toHaveCount(1);
  await page.getByTitle('Di chuyển sang phải').first().click();
  await expect(page.getByAltText('Ảnh công thức 1', { exact: true })).toHaveAttribute('src', firstUrl!);

  await page.getByTitle('Xóa ảnh này').last().click();
  await expect(page.getByText('Đã tải 1/5 ảnh • Bắt buộc chọn đúng 1 ảnh bìa')).toBeVisible();
  await expect(page.getByTitle('Đang là ảnh bìa')).toHaveCount(1);
  await expect(page.getByAltText('Ảnh công thức 1', { exact: true })).toHaveAttribute('src', firstUrl!);
  await page.getByTitle('Xóa ảnh này').click();
  await expect(page.getByAltText('Ảnh công thức 1', { exact: true })).toHaveCount(0);
  await expect(page.locator('#recipe-media-upload-input')).toBeEnabled();
});

test('FR-14: giới hạn preview năm ảnh và mở lại bộ chọn sau khi xóa một ảnh', async ({ page }) => {
  const input = page.locator('#recipe-media-upload-input');
  await input.setInputFiles(Array.from({ length: 6 }, (_, index) => imageFile(`${index}.png`)));
  await expect(page.getByText('Đã tải 5/5 ảnh • Bắt buộc chọn đúng 1 ảnh bìa')).toBeVisible();
  await expect(page.getByText(/Chỉ tải lên tối đa 5 ảnh còn lại/)).toBeVisible();
  await expect(input).toBeDisabled();
  await expect(page.getByTitle('Đang là ảnh bìa')).toHaveCount(1);
  await page.getByTitle('Xóa ảnh này').last().click();
  await expect(input).toBeEnabled();
  await input.setInputFiles(imageFile('replacement.png'));
  await expect(page.getByText('Đã tải 5/5 ảnh • Bắt buộc chọn đúng 1 ảnh bìa')).toBeVisible();
  await expect(page.getByText(/Chỉ tải lên tối đa/)).toHaveCount(0);
  await expect(page.getByTitle('Đang là ảnh bìa')).toHaveCount(1);
});
