import { Buffer } from 'node:buffer';
import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';
const imageFile = (name: string) => ({
  name,
  mimeType: 'image/png',
  buffer: Buffer.from('test image'),
});
async function successfulUploads(page: import('@playwright/test').Page) {
  let count = 0;
  await page.route('**/api/v1/recipes/media/upload', (route) =>
    route.fulfill({
      status: 201,
      json: {
        success: true,
        data: {
          blobUrl: `https://images.test/${++count}.png`,
          mimeType: 'image/png',
          sizeBytes: 10,
        },
      },
    }),
  );
}

test.beforeEach(async ({ page }) => {
  await seedDemoSession(page, 'EXPERT');
  await page.route('**/api/v1/recipes/form-options', (route) =>
    route.fulfill({
      json: {
        dishCategories: [{ code: 'BRAISED', label: 'Món kho' }],
        vegetarianTypes: [{ code: 'VEGAN', label: 'Thuần chay' }],
        difficulties: [{ code: 'EASY', label: 'Dễ' }],
        units: [{ unitId: 1, code: 'g', name: 'gram', dimension: 'MASS' }],
      },
    }),
  );
  // Kiểm tra preview khi upload lỗi; không phải bằng chứng Azure/Backend thật.
  await page.route('**/api/v1/recipes/media/upload', (route) =>
    route.fulfill({ status: 503, json: {} }),
  );
  await page.goto('/dang-cong-thuc');
});
test('FR-14: từ chối định dạng và dung lượng ảnh không hợp lệ trước khi upload', async ({
  page,
}) => {
  let uploads = 0;
  page.on('request', (request) => {
    if (request.url().endsWith('/recipes/media/upload')) uploads += 1;
  });
  const input = page.locator('#recipe-media-upload-input');
  await input.setInputFiles({
    name: 'invalid.gif',
    mimeType: 'image/gif',
    buffer: Buffer.from('invalid'),
  });
  await expect(
    page.getByText(/invalid.gif: Định dạng tệp không hợp lệ/),
  ).toBeVisible();
  await input.setInputFiles({
    name: 'large.png',
    mimeType: 'image/png',
    buffer: Buffer.alloc(5 * 1024 * 1024 + 1),
  });
  await expect(
    page.getByText(/large.png: Dung lượng tệp vượt quá/),
  ).toBeVisible();
  await expect(
    page.getByAltText('Ảnh công thức 1', { exact: true }),
  ).toHaveCount(0);
  expect(uploads).toBe(0);
});
test('FR-14: preview giữ đúng ảnh bìa và thứ tự khi đổi vị trí, xóa bìa và xóa hết ảnh', async ({
  page,
}) => {
  await successfulUploads(page);
  await page
    .locator('#recipe-media-upload-input')
    .setInputFiles([imageFile('one.png'), imageFile('two.png')]);
  await expect(
    page.getByText('Đã tải 2/5 ảnh • Bắt buộc chọn đúng 1 ảnh bìa'),
  ).toBeVisible();
  const firstUrl = await page
    .getByAltText('Ảnh công thức 1', { exact: true })
    .getAttribute('src');
  const secondUrl = await page
    .getByAltText('Ảnh công thức 2', { exact: true })
    .getAttribute('src');
  expect(firstUrl).toMatch(/^https:/);
  expect(secondUrl).toMatch(/^https:/);
  expect(firstUrl).not.toBe(secondUrl);

  await expect(page.getByTitle('Di chuyển sang trái').first()).toBeDisabled();
  await expect(page.getByTitle('Di chuyển sang phải').last()).toBeDisabled();
  await page.getByTitle('Đặt làm ảnh bìa', { exact: true }).click();
  await page.getByTitle('Di chuyển sang trái').last().click();
  await expect(
    page.getByAltText('Ảnh công thức 1', { exact: true }),
  ).toHaveAttribute('src', secondUrl!);
  await expect(page.getByTitle('Đang là ảnh bìa')).toHaveCount(1);
  await page.getByTitle('Di chuyển sang phải').first().click();
  await expect(
    page.getByAltText('Ảnh công thức 1', { exact: true }),
  ).toHaveAttribute('src', firstUrl!);

  await page.getByTitle('Xóa ảnh này').last().click();
  await expect(
    page.getByText('Đã tải 1/5 ảnh • Bắt buộc chọn đúng 1 ảnh bìa'),
  ).toBeVisible();
  await expect(page.getByTitle('Đang là ảnh bìa')).toHaveCount(1);
  await expect(
    page.getByAltText('Ảnh công thức 1', { exact: true }),
  ).toHaveAttribute('src', firstUrl!);
  await page.getByTitle('Xóa ảnh này').click();
  await expect(
    page.getByAltText('Ảnh công thức 1', { exact: true }),
  ).toHaveCount(0);
  await expect(page.locator('#recipe-media-upload-input')).toBeEnabled();
});
test('FR-14: giới hạn preview năm ảnh và mở lại bộ chọn sau khi xóa một ảnh', async ({
  page,
}) => {
  await successfulUploads(page);
  const input = page.locator('#recipe-media-upload-input');
  await input.setInputFiles(
    Array.from({ length: 6 }, (_, index) => imageFile(`${index}.png`)),
  );
  await expect(
    page.getByText('Đã tải 5/5 ảnh • Bắt buộc chọn đúng 1 ảnh bìa'),
  ).toBeVisible();
  await expect(
    page.getByText(/Chỉ tải lên tối đa 5 ảnh còn lại/),
  ).toBeVisible();
  await expect(input).toBeDisabled();
  await expect(page.getByTitle('Đang là ảnh bìa')).toHaveCount(1);
  await page.getByTitle('Xóa ảnh này').last().click();
  await expect(input).toBeEnabled();
  await input.setInputFiles(imageFile('replacement.png'));
  await expect(
    page.getByText('Đã tải 5/5 ảnh • Bắt buộc chọn đúng 1 ảnh bìa'),
  ).toBeVisible();
  await expect(page.getByText(/Chỉ tải lên tối đa/)).toHaveCount(0);
  await expect(page.getByTitle('Đang là ảnh bìa')).toHaveCount(1);
});
test('FR-25: ảnh xem trước khi upload lỗi không được gửi để xuất bản', async ({
  page,
}) => {
  let publications = 0;
  await page.route('**/api/v1/recipes', (route) => {
    if (route.request().method() === 'POST') publications += 1;
    return route.fulfill({ status: 500, json: {} });
  });
  await page
    .locator('#recipe-media-upload-input')
    .setInputFiles(imageFile('failed-upload.png'));
  await expect(
    page.getByAltText('Ảnh công thức 1', { exact: true }),
  ).toHaveAttribute('src', /^blob:/);
  await expect(page.getByText('Chưa tải lên', { exact: true })).toBeVisible();
  await expect(page.getByText(/Đã tải 0\/5 ảnh/)).toBeVisible();
  await page.getByRole('button', { name: 'Xuất bản công thức' }).click();
  await expect(
    page
      .getByRole('alert')
      .filter({ hasText: /blob|upload|tải lên thành công/ })
      .last(),
  ).toContainText('Một số ảnh chưa tải lên thành công');
  expect(publications).toBe(0);
});
test('fix #158: xóa ảnh lỗi rồi chọn lại tải thành công', async ({ page }) => {
  const input = page.locator('#recipe-media-upload-input');
  await input.setInputFiles(imageFile('retry.png'));
  await expect(page.getByText('Chưa tải lên', { exact: true })).toBeVisible();
  await page.getByTitle('Xóa ảnh này').click();
  await successfulUploads(page);
  await input.setInputFiles(imageFile('retry.png'));
  await expect(page.getByText(/Đã tải 1\/5 ảnh/)).toBeVisible();
  await expect(page.getByText('Chưa tải lên', { exact: true })).toHaveCount(0);
  await expect(page.getByRole('alert')).toHaveCount(0);
});
test('fix #158: chặn xuất bản trong lúc ảnh đang tải', async ({ page }) => {
  let finish!: () => void;
  const pending = new Promise<void>((resolve) => {
    finish = resolve;
  });
  await page.route('**/api/v1/recipes/media/upload', async (route) => {
    await pending;
    await route.fulfill({ status: 503, json: {} });
  });
  await page
    .locator('#recipe-media-upload-input')
    .setInputFiles(imageFile('pending.png'));
  await expect(
    page.getByRole('button', { name: 'Đang tải ảnh…', exact: true }),
  ).toBeDisabled();
  await expect(page.locator('#recipe-media-upload-input')).toBeDisabled();
  finish();
  await expect(page.getByText('Chưa tải lên', { exact: true })).toBeVisible();
  await expect(
    page.getByRole('button', { name: 'Xuất bản công thức' }),
  ).toBeEnabled();
});
test('fix #158: phản hồi upload thiếu URL không được tính là thành công', async ({
  page,
}) => {
  await page.route('**/api/v1/recipes/media/upload', (route) =>
    route.fulfill({ status: 201, json: { success: true, data: {} } }),
  );
  await page
    .locator('#recipe-media-upload-input')
    .setInputFiles(imageFile('invalid-response.png'));
  await expect(page.getByRole('alert')).toContainText(
    'Phản hồi tải ảnh không hợp lệ',
  );
  await expect(page.getByText(/Đã tải 0\/5 ảnh/)).toBeVisible();
});
