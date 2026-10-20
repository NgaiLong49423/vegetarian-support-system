import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

async function openRecipeFormAsDemoExpert(page: import('@playwright/test').Page) {
  await seedDemoSession(page, 'EXPERT');
  await page.goto('/dang-cong-thuc');
}

test('recipe exploration uses only API search and reports an empty result honestly', async ({ page }) => {
  await page.route('**/api/v1/recipes/discovery?**', (route) => route.fulfill({ json: {
    items: [], page: 0, size: 12, hasNext: false,
  } }));
  await page.goto('/kham-pha');
  const search = page.getByRole('textbox', { name: 'Từ khóa', exact: true });
  await search.fill('không có món này');
  await expect(page.getByRole('heading', { name: 'Không tìm thấy công thức phù hợp' })).toBeVisible();
  await expect(page.getByText('Phở chay', { exact: true })).toHaveCount(0);
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

  await expect(page.getByRole('alert')).toContainText('Phiên đăng nhập không');
});

test('recipe creation uploads images, selects one cover and publishes the gallery', async ({ page }) => {
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: {
    dishCategories: [{ code: 'BRAISED', label: 'Món kho' }],
    vegetarianTypes: [{ code: 'VEGAN', label: 'Thuần chay' }],
    difficulties: [{ code: 'EASY', label: 'Dễ' }],
    units: [{ unitId: 1, code: 'g', name: 'gram', dimension: 'MASS' }],
  } }));
  await page.route('**/api/v1/recipes/ingredient-options**', (route) => route.fulfill({ json: [
    { ingredientId: 1, name: 'Đậu hũ' },
  ] }));
  let uploadIndex = 0;
  await page.route('**/api/v1/recipes/media/upload', async (route) => {
    uploadIndex += 1;
    const fileName = uploadIndex === 1 ? 'one.png' : 'two.png';
    return route.fulfill({ json: { success: true, data: {
      blobUrl: `https://images.example.test/${fileName}`, mimeType: 'image/png', sizeBytes: 3,
    } } });
  });
  let publishedMedia: Array<{ blobUrl: string; mimeType: string; cover: boolean }> | undefined;
  await page.route('**/api/v1/recipes', async (route) => {
    if (route.request().method() === 'POST') {
      publishedMedia = (route.request().postDataJSON() as { media: typeof publishedMedia }).media;
      return route.fulfill({ status: 201, json: {
        recipeId: 620, title: 'Đậu hũ kho cà chua', status: 'PUBLISHED', publishedAt: '2026-10-11T10:00:00',
      } });
    }
    return route.fallback();
  });

  await openRecipeFormAsDemoExpert(page);
  await page.getByLabel('Tên món *').fill('Đậu hũ kho cà chua');
  await page.getByLabel('Thể loại món').selectOption('BRAISED');
  await page.getByLabel('Loại ăn chay').selectOption('VEGAN');
  await page.getByLabel('Độ khó').selectOption('EASY');
  await page.getByLabel('Khẩu phần').fill('2');
  await page.getByLabel('Thời gian chuẩn bị').fill('10');
  await page.getByLabel('Thời gian nấu').fill('15');
  await page.getByLabel('Chọn nguyên liệu 1').fill('Đậu');
  await page.getByRole('button', { name: 'Đậu hũ', exact: true }).click();
  await page.getByLabel('Số lượng nguyên liệu 1').fill('200');
  await page.getByLabel('Đơn vị nguyên liệu 1').selectOption('1');
  await page.getByLabel('Hướng dẫn * (10–5.000 ký tự)').fill('Cắt đậu hũ rồi kho cùng cà chua đến khi mềm.');
  await page.locator('#recipe-media-upload-input').setInputFiles([
    { name: 'one.png', mimeType: 'image/png', buffer: Buffer.from('one') },
    { name: 'two.png', mimeType: 'image/png', buffer: Buffer.from('two') },
  ]);
  await expect(page.getByTitle('Đang là ảnh bìa')).toHaveCount(1);
  await page.getByTitle('Đặt làm ảnh bìa').click();
  await expect(page.getByTitle('Đang là ảnh bìa')).toHaveCount(1);
  await page.getByRole('button', { name: 'Xuất bản công thức' }).click();
  await expect.poll(() => publishedMedia).toHaveLength(2);
  expect(publishedMedia?.filter((media) => media.cover)).toHaveLength(1);
  expect(publishedMedia?.map((media) => media.blobUrl)).toEqual([
    'https://images.example.test/one.png', 'https://images.example.test/two.png',
  ]);
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

test('Member profile links to actual nutrition and preferences screens', async ({ page }) => {
  await seedDemoSession(page, 'CUSTOMER');
  await page.goto('/ho-so');
  await page.getByRole('button', { name: 'Hồ sơ của tôi', exact: true }).click();
  await expect(page.getByRole('main').getByRole('link', { name: 'Mở hồ sơ dinh dưỡng' })).toHaveAttribute('href', '/ho-so/dinh-duong');
  await expect(page.getByRole('main').getByRole('link', { name: 'Mở sở thích ăn uống' })).toHaveAttribute('href', '/ho-so/so-thich-an-uong');
  await page.getByRole('button', { name: 'Công thức đã lưu', exact: true }).click();
  await expect(page.getByRole('heading', { name: 'Chưa có công thức đã lưu' })).toBeVisible();
});
test('nutrition day selection updates the displayed day and opens a recipe', async ({ page }) => {
  await seedDemoSession(page, 'CUSTOMER');
  await page.route('**/api/v1/meal-plans/*/nutrition', async (route) => {
    const match = new URL(route.request().url()).pathname.match(/meal-plans\/(\d{4}-\d{2}-\d{2})\/nutrition$/);
    if (!match) return route.fulfill({ status: 404 });
    const weekStartDate = match[1];
    const monday = new Date(`${weekStartDate}T12:00:00`);
    const days = Array.from({ length: 7 }, (_, offset) => {
      const date = new Date(monday);
      date.setDate(date.getDate() + offset);
      const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
      const entries = offset === 0 ? [{
        entryId: 11, date: key, mealType: 'BREAKFAST', recipeId: 11,
        recipeTitle: 'Canh rau', plannedServings: 1, complete: true,
        ingredientsMissingData: [], nutrients: { ENERGY_KCAL: 150 },
      }] : [];
      return { date: key, nutrients: { ENERGY_KCAL: entries.length ? 150 : 0 }, entries };
    });
    return route.fulfill({ json: {
      weekStartDate, scope: 'WEEK', selectedDate: null,
      referenceBasis: 'Mức tham khảo kiểm thử', source: 'Catalog', sourceUrl: '',
      disclaimer: 'Thông tin tham khảo.',
      targets: [{ key: 'ENERGY_KCAL', label: 'Năng lượng', minimum: 2000,
        maximum: 2000, unit: 'kcal', referenceType: 'RDA', source: 'Catalog',
        sourceUrl: '', version: '1' }],
      days, weeklyAverage: { ENERGY_KCAL: 150 }, attainmentDays: { ENERGY_KCAL: 0 },
      incompleteEntries: [],
    } });
  });
  await page.goto('/dinh-duong');
  const day = page.getByRole('button', { name: /Thứ Hai/ }).first();
  await expect(day).toBeVisible();
  await day.click();
  await expect(day).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByRole('heading', { name: /Thực đơn ngày/ })).toBeVisible();
  const recipeLink = page.getByRole('link', { name: 'Chi tiết công thức →' }).first();
  await expect(recipeLink).toHaveAttribute('href', '/cong-thuc/id/11');
  await recipeLink.click();
  await expect(page).toHaveURL(/\/cong-thuc\/id\/11/);
});
