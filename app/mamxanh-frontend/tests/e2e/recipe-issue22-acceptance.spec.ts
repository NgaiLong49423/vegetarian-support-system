import { type Page } from '@playwright/test';
import { expect, test } from './baseFixtures';

// The scenarios intentionally pause for two seconds after each user action so
// they can be observed. The 50-ingredient boundary scenario therefore needs a
// longer test budget than Playwright's 30-second default.
test.describe.configure({ timeout: 180_000 });

const options = {
  dishCategories: [
    { code: 'BRAISED', label: 'Món kho' },
    { code: 'SOUP', label: 'Món canh' },
  ],
  vegetarianTypes: [
    { code: 'VEGAN', label: 'Thuần chay' },
    { code: 'LACTO_OVO', label: 'Có trứng và sữa' },
  ],
  difficulties: [
    { code: 'EASY', label: 'Dễ' },
    { code: 'MEDIUM', label: 'Trung bình' },
    { code: 'HARD', label: 'Khó' },
  ],
  units: [
    { unitId: 1, code: 'g', name: 'gram', dimension: 'MASS' },
    { unitId: 2, code: 'kg', name: 'kilogram', dimension: 'MASS' },
    { unitId: 3, code: 'quả', name: 'quả', dimension: 'COUNT' },
  ],
};

const ingredients = [
  { ingredientId: 11, name: 'Đậu hũ' },
  { ingredientId: 12, name: 'Cà chua' },
];

type PostResponse = { status: number; body: Record<string, unknown> };

async function action(page: Page, label: string, operation: () => Promise<unknown>) {
  console.log(`BƯỚC: ${label}`);
  await operation();
  await page.waitForTimeout(2000);
}

type PublishedDetail = {
  recipeId: number;
  title: string;
  description: string | null;
  instructions: string;
  dishCategory: string;
  dishCategoryLabel: string;
  vegetarianType: 'VEGAN' | 'LACTO' | 'OVO' | 'LACTO_OVO';
  vegetarianTypeLabel: string;
  difficulty: string;
  difficultyLabel: string;
  servings: number;
  prepTimeMinutes: number;
  cookTimeMinutes: number;
  youtubeUrl: string | null;
  publishedAt: string;
  nutritionComplete: boolean;
  ingredientsWithoutNutrition: string[];
  ingredients: Array<{ ingredientId: number; name: string; quantity: number; unitId: number; unitCode: string; unitName: string }>;
  media: Array<{ blobUrl: string; mimeType: string; displayOrder: number; cover: boolean }>;
};

const detailFixture = (overrides: Partial<PublishedDetail> = {}): PublishedDetail => ({
  recipeId: 2280,
  title: 'Canh cà chua đậu hũ',
  description: 'Món dễ nấu cho bữa tối.',
  instructions: 'Đun nước, cho cà chua và đậu hũ vào nấu chín.',
  dishCategory: 'SOUP',
  dishCategoryLabel: 'Món canh',
  vegetarianType: 'VEGAN',
  vegetarianTypeLabel: 'Thuần chay',
  difficulty: 'EASY',
  difficultyLabel: 'Dễ',
  servings: 2,
  prepTimeMinutes: 10,
  cookTimeMinutes: 15,
  youtubeUrl: 'https://www.youtube.com/watch?v=recipe-1',
  publishedAt: '2026-10-04T08:00:00',
  nutritionComplete: true,
  ingredientsWithoutNutrition: [],
  ingredients: [{ ingredientId: 12, name: 'Cà chua', quantity: 2, unitId: 1, unitCode: 'quả', unitName: 'quả' }],
  media: [],
  ...overrides,
});

async function installApi(
  page: Page,
  getPostResponse: (body: Record<string, any>) => PostResponse,
  initialDetails: PublishedDetail[] = [],
) {
  const publishedDetails = new Map<number, Record<string, unknown>>(initialDetails.map((detail) => [detail.recipeId, detail]));
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: options }));
  await page.route('**/api/v1/recipes/ingredient-options**', (route) => {
    const query = new URL(route.request().url()).searchParams.get('query')?.toLowerCase() ?? '';
    const matches = ingredients.filter((item) => item.name.toLowerCase().includes(query));
    return route.fulfill({ json: matches });
  });
  await page.route(/\/api\/v1\/recipes\/\d+$/, async (route) => {
    if (route.request().method() !== 'GET') return route.fallback();
    const recipeId = Number(new URL(route.request().url()).pathname.split('/').at(-1));
    const detail = publishedDetails.get(recipeId);
    await route.fulfill(detail
      ? { status: 200, json: detail }
      : { status: 404, json: { status: 404, code: 'NOT_FOUND', detail: 'Không tìm thấy công thức.' }, contentType: 'application/problem+json' });
  });
  await page.route('**/api/v1/recipes', async (route) => {
    const body = route.request().postDataJSON() as Record<string, any>;
    const response = getPostResponse(body);
    if (response.status === 201) {
      const recipeId = Number(response.body.recipeId);
      publishedDetails.set(recipeId, {
        recipeId,
        title: body.title,
        description: body.description || null,
        instructions: body.instructions,
        dishCategory: body.dishCategory,
        dishCategoryLabel: options.dishCategories.find((item) => item.code === body.dishCategory)?.label ?? body.dishCategory,
        vegetarianType: body.vegetarianType,
        vegetarianTypeLabel: options.vegetarianTypes.find((item) => item.code === body.vegetarianType)?.label ?? body.vegetarianType,
        difficulty: body.difficulty,
        difficultyLabel: options.difficulties.find((item) => item.code === body.difficulty)?.label ?? body.difficulty,
        servings: body.servings,
        prepTimeMinutes: body.prepTimeMinutes,
        cookTimeMinutes: body.cookTimeMinutes,
        youtubeUrl: body.youtubeUrl || null,
        publishedAt: response.body.publishedAt,
        nutritionComplete: true,
        ingredientsWithoutNutrition: [],
        ingredients: body.ingredients.map((item: Record<string, number>) => ({
          ingredientId: item.ingredientId,
          name: ingredients.find((option) => option.ingredientId === item.ingredientId)?.name ?? 'Nguyên liệu',
          quantity: item.quantity,
          unitId: item.unitId,
          unitCode: options.units.find((unit) => unit.unitId === item.unitId)?.code ?? 'g',
          unitName: options.units.find((unit) => unit.unitId === item.unitId)?.name ?? 'gram',
        })),
        media: [],
      });
    }
    await route.fulfill({
      status: response.status,
      contentType: response.status >= 400 ? 'application/problem+json' : 'application/json',
      json: response.body,
    });
  });
}

test('Issue 22: trang chi tiết tải trực tiếp nội dung công thức, cover, mô tả và YouTube', async ({ page }) => {
  const detail = detailFixture({
    media: [
      { blobUrl: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a+ioAAAAASUVORK5CYII=', mimeType: 'image/png', displayOrder: 0, cover: false },
      { blobUrl: 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a+ioAAAAASUVORK5CYII=', mimeType: 'image/png', displayOrder: 1, cover: true },
    ],
  });
  await installApi(page, () => ({ status: 201, body: {} }), [detail]);

  await action(page, 'Mở trực tiếp đường dẫn chi tiết bằng ID', () => page.goto('/cong-thuc/2280', { waitUntil: 'commit' }));
  await expect(page.getByRole('heading', { name: detail.title, level: 1 })).toBeVisible();
  await expect(page.getByText(detail.description!)).toBeVisible();
  await expect(page.getByRole('img', { name: detail.title })).toHaveAttribute('src', detail.media[1].blobUrl);
  await expect(page.getByRole('link', { name: 'Xem video hướng dẫn' })).toHaveAttribute('href', detail.youtubeUrl!);
  await expect(page.getByText(detail.instructions)).toBeVisible();
  await expect(page.getByText('2 quả', { exact: true })).toBeVisible();
});

test('Issue 25: recipe detail flags catalog ingredients without nutrition data', async ({ page }) => {
  const detail = detailFixture({
    nutritionComplete: false,
    ingredientsWithoutNutrition: ['Cà chua'],
  });
  await installApi(page, () => ({ status: 201, body: {} }), [detail]);

  await page.goto(`/cong-thuc/${detail.recipeId}`, { waitUntil: 'commit' });

  await expect(page.getByRole('heading', { name: 'Chưa đủ dữ liệu dinh dưỡng' })).toBeVisible();
  await expect(page.getByRole('status')).toContainText('Ước tính dinh dưỡng chưa đầy đủ');
  await expect(page.locator('[aria-labelledby="nutrition-status-heading"] li')).toHaveText('Cà chua');
});

test('Issue 22: trang chi tiết không ảnh và trường tùy chọn trống dùng placeholder theo loại ăn chay', async ({ page }) => {
  const types: PublishedDetail['vegetarianType'][] = ['VEGAN', 'LACTO', 'OVO', 'LACTO_OVO'];
  const labels = ['Thuần chay', 'Có sữa', 'Có trứng', 'Có trứng và sữa'];
  const details = types.map((vegetarianType, index) => detailFixture({
    recipeId: 2281 + index,
    title: `Món chay ${index + 1}`,
    description: null,
    youtubeUrl: null,
    vegetarianType,
    vegetarianTypeLabel: labels[index],
    media: [],
  }));
  await installApi(page, () => ({ status: 201, body: {} }), details);

  for (const detail of details) {
    await action(page, `Mở chi tiết món ${detail.vegetarianTypeLabel} không có ảnh`, () => page.goto(`/cong-thuc/${detail.recipeId}`));
    await expect(page.getByRole('heading', { name: detail.title, level: 1 })).toBeVisible();
    await expect(page.getByRole('img', { name: `Ảnh mặc định cho món ${detail.vegetarianTypeLabel}` })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Xem video hướng dẫn' })).toHaveCount(0);
    await expect(page.getByText('Món dễ nấu cho bữa tối.')).toHaveCount(0);
  }
});

test('Issue 22: API không tìm thấy công thức hiển thị trạng thái lỗi', async ({ page }) => {
  await installApi(page, () => ({ status: 201, body: {} }));

  await action(page, 'Mở ID công thức không tồn tại', () => page.goto('/cong-thuc/2299', { waitUntil: 'domcontentloaded' }));
  await expect(page.getByRole('heading', { name: 'Không tìm thấy công thức' })).toBeVisible();
  await expect(page.getByRole('alert')).toContainText('Không tìm thấy công thức.');
  await expect(page.getByRole('link', { name: 'Quay lại khám phá' })).toBeVisible();
});

async function openForm(page: Page) {
  // Existing demo role selection only reveals the Expert form in the prototype;
  // it does not create an authenticated session or bypass Backend authorization.
  await action(page, 'Mở trang tài khoản demo có sẵn', () => page.goto('/dang-nhap'));
  await action(page, 'Chọn tài khoản demo để kiểm tra giao diện', () => page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click());
  await action(page, 'Mở menu vai trò demo', () => page.getByRole('button', { name: /Tài khoản Lan Anh/ }).click());
  await action(page, 'Chọn vai trò EXPERT demo', () => page.getByRole('button', { name: 'Expert', exact: true }).click());
  await action(page, 'Mở trang Đăng công thức', () => page.getByRole('link', { name: 'Đăng công thức mới' }).click());
  await expect(page.getByRole('heading', { name: 'Đăng công thức món chay' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Thông tin món ăn' })).toBeVisible();
}

async function fillValidRecipe(page: Page, unit = 'g') {
  await action(page, 'Nhập tên món', () => page.getByLabel('Tên món *').fill('Đậu hũ kho cà chua'));
  await action(page, 'Chọn thể loại món', () => page.getByLabel('Thể loại món').selectOption('BRAISED'));
  await action(page, 'Chọn loại ăn chay', () => page.getByLabel('Loại ăn chay').selectOption('VEGAN'));
  await action(page, 'Chọn độ khó bắt buộc', () => page.getByLabel('Độ khó').selectOption('EASY'));
  await action(page, 'Nhập khẩu phần', () => page.getByLabel('Khẩu phần').fill('2'));
  await action(page, 'Nhập thời gian chuẩn bị', () => page.getByLabel('Thời gian chuẩn bị').fill('10'));
  await action(page, 'Nhập thời gian nấu', () => page.getByLabel('Thời gian nấu').fill('0'));
  await action(page, 'Tìm nguyên liệu có sẵn', () => page.getByLabel('Chọn nguyên liệu 1').fill('Đậu'));
  await action(page, 'Chọn nguyên liệu từ danh mục gợi ý', () => page.getByRole('button', { name: 'Đậu hũ', exact: true }).click());
  await action(page, 'Nhập số lượng nguyên liệu', () => page.getByLabel('Số lượng nguyên liệu 1').fill('200'));
  await action(page, `Chọn đơn vị ${unit}`, () => page.getByLabel('Đơn vị nguyên liệu 1').selectOption({ label: unit === 'g' ? 'gram (g)' : unit === 'kg' ? 'kilogram (kg)' : 'quả (quả)' }));
  await action(page, 'Nhập hướng dẫn chế biến', () => page.getByLabel('Hướng dẫn * (10–5.000 ký tự)').fill('Cắt đậu hũ, rim với cà chua đến khi thấm vị.'));
}

test('Issue 22: tìm nguyên liệu có sẵn, từ chối tên ngoài danh mục, thêm rồi xóa dòng', async ({ page }) => {
  await installApi(page, () => ({ status: 201, body: { recipeId: 2201, title: 'Đậu hũ kho cà chua', status: 'PUBLISHED', publishedAt: '2026-10-04T08:00:00' } }));
  await openForm(page);
  await action(page, 'Tìm một tên không có trong danh mục', () => page.getByLabel('Chọn nguyên liệu 1').fill('nguyên liệu lạ'));
  await expect(page.getByText('Nguyên liệu này hiện chưa được hỗ trợ.')).toBeVisible();
  await expect(page.getByRole('button', { name: /tạo nguyên liệu mới|thêm nguyên liệu mới/i })).toHaveCount(0);
  await action(page, 'Thay bằng từ khóa nguyên liệu có sẵn', () => page.getByLabel('Chọn nguyên liệu 1').fill('Đậu'));
  await expect(page.getByRole('button', { name: 'Đậu hũ', exact: true })).toBeVisible();
  await action(page, 'Chọn nguyên liệu có sẵn', () => page.getByRole('button', { name: 'Đậu hũ', exact: true }).click());
  await action(page, 'Thêm dòng nguyên liệu', () => page.getByRole('button', { name: 'Thêm nguyên liệu' }).click());
  await expect(page.getByLabel('Chọn nguyên liệu 2')).toBeVisible();
  await action(page, 'Xóa dòng nguyên liệu thứ hai', () => page.getByRole('button', { name: 'Xóa nguyên liệu 2' }).click());
  await expect(page.getByLabel('Chọn nguyên liệu 2')).toHaveCount(0);
});

test('Issue 22: độ khó bắt buộc và lỗi validation từ server hiển thị đúng trường', async ({ page }) => {
  await installApi(page, () => ({
    status: 400,
    body: {
      status: 400,
      code: 'VALIDATION_FAILED',
      title: 'Validation failed',
      detail: 'Dữ liệu công thức chưa hợp lệ.',
      errors: [
        { field: 'title', message: 'Tên món phải dài từ 3 đến 120 ký tự.' },
        { field: 'difficulty', message: 'Độ khó là trường bắt buộc.' },
        { field: 'instructions', message: 'Hướng dẫn phải dài ít nhất 10 ký tự.' },
      ],
    },
  }));
  await openForm(page);
  await fillValidRecipe(page);
  await action(page, 'Để trống độ khó để kiểm tra bắt buộc', () => page.getByLabel('Độ khó').selectOption(''));
  await action(page, 'Nhập tên món ngắn hơn giới hạn', () => page.getByLabel('Tên món *').fill('ab'));
  await action(page, 'Nhập hướng dẫn ngắn hơn giới hạn', () => page.getByLabel('Hướng dẫn * (10–5.000 ký tự)').fill('ngắn'));
  await action(page, 'Gửi để nhận lỗi validation', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page.getByText('Tên món phải dài từ 3 đến 120 ký tự.')).toBeVisible();
  await expect(page.getByText('Độ khó là trường bắt buộc.')).toBeVisible();
  await expect(page.getByText('Hướng dẫn phải dài ít nhất 10 ký tự.')).toBeVisible();
});

test('Issue 22: biên số khẩu phần, thời gian, lượng nguyên liệu và tối đa 50 dòng', async ({ page }) => {
  await installApi(page, () => ({ status: 201, body: { recipeId: 2250, title: 'Giới hạn công thức', status: 'PUBLISHED', publishedAt: '2026-10-04T08:00:00' } }));
  await openForm(page);
  await expect(page.getByLabel('Tên món *')).toHaveAttribute('maxLength', '120');
  await expect(page.getByLabel('Mô tả')).toHaveAttribute('maxLength', '2000');

  const servings = page.getByLabel('Khẩu phần');
  await action(page, 'Nhập khẩu phần dưới mức nhỏ nhất', () => servings.fill('0'));
  expect(await servings.evaluate((input: HTMLInputElement) => input.validity.rangeUnderflow)).toBe(true);
  await action(page, 'Nhập khẩu phần trên mức lớn nhất', () => servings.fill('51'));
  expect(await servings.evaluate((input: HTMLInputElement) => input.validity.rangeOverflow)).toBe(true);

  const prepTime = page.getByLabel('Thời gian chuẩn bị');
  await action(page, 'Nhập thời gian chuẩn bị trên 1.440 phút', () => prepTime.fill('1441'));
  expect(await prepTime.evaluate((input: HTMLInputElement) => input.validity.rangeOverflow)).toBe(true);
  const cookTime = page.getByLabel('Thời gian nấu');
  await action(page, 'Nhập thời gian nấu trên 1.440 phút', () => cookTime.fill('1441'));
  expect(await cookTime.evaluate((input: HTMLInputElement) => input.validity.rangeOverflow)).toBe(true);

  const quantity = page.getByLabel('Số lượng nguyên liệu 1');
  await action(page, 'Nhập định lượng bằng 0', () => quantity.fill('0'));
  expect(await quantity.evaluate((input: HTMLInputElement) => input.validity.rangeUnderflow)).toBe(true);
  await action(page, 'Nhập định lượng có hơn hai chữ số thập phân', () => quantity.fill('0.001'));
  expect(await quantity.evaluate((input: HTMLInputElement) => input.validity.stepMismatch)).toBe(true);

  for (let row = 2; row <= 50; row++) {
    await action(page, `Thêm dòng nguyên liệu ${row}/50`, () => page.getByRole('button', { name: 'Thêm nguyên liệu' }).click());
  }
  await expect(page.getByLabel('Chọn nguyên liệu 50')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Thêm nguyên liệu' })).toBeDisabled();
});

test('Issue 22: chấp nhận định lượng dương không theo bước 100g và mở chi tiết bài mới', async ({ page }) => {
  let publishCount = 0;
  await installApi(page, (body) => {
    publishCount++;
    return { status: 201, body: { recipeId: 2260, title: body.title, status: 'PUBLISHED', publishedAt: '2026-10-04T08:00:00' } };
  });
  await openForm(page);
  await fillValidRecipe(page, 'g');
  const quantity = page.getByLabel('Số lượng nguyên liệu 1');

  await action(page, 'Nhập 80 g', () => quantity.fill('80'));
  await action(page, 'Đăng công thức với 80 g', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page).toHaveURL(/\/cong-thuc\/2260$/);
  await expect(page.getByRole('heading', { name: 'Đậu hũ kho cà chua', level: 1 })).toBeVisible();
  await expect(page.getByText('80 g', { exact: true })).toBeVisible();
  expect(publishCount).toBe(1);
});

test('Issue 22: g/kg đi trực tiếp; đơn vị đếm chỉ qua khi API xác nhận conversion có sẵn', async ({ page }) => {
  await installApi(page, (body) => {
    const usesCountUnit = body.ingredients?.some((item: Record<string, number>) => item.unitId === 3);
    if (usesCountUnit) {
      return {
        status: 400,
        body: { status: 400, code: 'VALIDATION_FAILED', title: 'Validation failed', detail: 'Dữ liệu công thức chưa hợp lệ.', errors: [{ field: 'ingredients[0].unitId', message: 'Chưa có conversion cho nguyên liệu và đơn vị này.' }] },
      };
    }
    return { status: 201, body: { recipeId: 2202, title: body.title, status: 'PUBLISHED', publishedAt: '2026-10-04T08:00:00' } };
  });
  await openForm(page);
  await fillValidRecipe(page, 'quả');
  await action(page, 'Gửi đơn vị quả khi chưa có conversion', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page.getByText('Chưa có conversion cho nguyên liệu và đơn vị này.')).toBeVisible();
  await action(page, 'Đổi sang gram', () => page.getByLabel('Đơn vị nguyên liệu 1').selectOption({ label: 'gram (g)' }));
  await action(page, 'Gửi lại với đơn vị khối lượng', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page).toHaveURL(/\/cong-thuc\/2202$/);
});

test('Issue 22: 0 ảnh được đăng; giới hạn tối đa 5 và yêu cầu cover đúng một ảnh', async ({ page }) => {
  await installApi(page, (body) => ({ status: 201, body: { recipeId: 2203, title: body.title, status: 'PUBLISHED', publishedAt: '2026-10-04T08:00:00' } }));
  await openForm(page);
  await fillValidRecipe(page);
  const chooser = page.getByLabel('Chọn tối đa 5 ảnh');
  await action(page, 'Chọn một ảnh nhưng chưa chọn cover', () => chooser.setInputFiles([{ name: 'cover.png', mimeType: 'image/png', buffer: Buffer.from('cover') }]));
  await action(page, 'Thử xuất bản khi chưa chọn cover', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page.getByRole('alert')).toContainText('hãy chọn đúng 1 ảnh bìa');
  await action(page, 'Chọn cover cho ảnh', () => page.getByLabel('Chọn cover.png làm ảnh cover').check());
  await action(page, 'Thử đăng ảnh đã có cover', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page.getByRole('alert')).toContainText('Upload ảnh thuộc FR-14');
  await action(page, 'Chọn sáu ảnh để kiểm tra giới hạn', () => chooser.setInputFiles(Array.from({ length: 6 }, (_, index) => ({ name: `photo-${index + 1}.png`, mimeType: 'image/png', buffer: Buffer.from(`photo-${index + 1}`) }))));
  await action(page, 'Thử xuất bản quá năm ảnh', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page.getByRole('alert')).toContainText('tối đa 5 ảnh');
  await action(page, 'Chọn đúng năm ảnh', () => chooser.setInputFiles(Array.from({ length: 5 }, (_, index) => ({ name: `five-${index + 1}.png`, mimeType: 'image/png', buffer: Buffer.from(`five-${index + 1}`) }))));
  await action(page, 'Chọn một cover trong năm ảnh', () => page.getByLabel('Chọn five-1.png làm ảnh cover').check());
  await action(page, 'Xác nhận năm ảnh và một cover qua validation của form', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page.getByRole('alert')).toContainText('Upload ảnh thuộc FR-14');
  await action(page, 'Bỏ các ảnh đã chọn', () => chooser.setInputFiles([]));
  await action(page, 'Để trống ảnh và xuất bản', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page).toHaveURL(/\/cong-thuc\/2203$/);
  await expect(page.getByRole('img', { name: 'Ảnh mặc định cho món Thuần chay' })).toBeVisible();
  await expect(page.getByText('Cắt đậu hũ, rim với cà chua đến khi thấm vị.')).toBeVisible();
});

test('Issue 22: YouTube/mô tả tùy chọn; 401 và 403 bị chặn, không có login giả', async ({ page }) => {
  let postStatus = 401;
  await installApi(page, (body) => {
    if (postStatus === 201) return { status: 201, body: { recipeId: 2204, title: body.title, status: 'PUBLISHED', publishedAt: '2026-10-04T08:00:00' } };
    const forbidden = postStatus === 403;
    return {
      status: postStatus,
      body: { status: postStatus, code: forbidden ? 'ACCESS_DENIED' : 'UNAUTHENTICATED', title: forbidden ? 'Forbidden' : 'Unauthenticated', detail: forbidden ? 'Chỉ EXPERT được đăng.' : 'Cần phiên đăng nhập.' },
    };
  });
  await openForm(page);
  await fillValidRecipe(page);
  await action(page, 'Để trống mô tả (trường tùy chọn)', () => page.getByLabel('Mô tả').fill(''));
  await action(page, 'Để trống link YouTube (trường tùy chọn)', () => page.getByLabel('Link YouTube').fill(''));
  await action(page, 'Gửi khi chưa đăng nhập', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page.getByRole('alert')).toContainText('Phiên đăng nhập không hợp lệ hoặc đã hết hạn');
  postStatus = 403;
  await action(page, 'Gửi lại bằng vai trò không phải EXPERT', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page.getByRole('alert')).toContainText('Chỉ Chuyên gia đang hoạt động mới được đăng');
  postStatus = 201;
  await action(page, 'Mô phỏng phản hồi thành công từ phiên EXPERT', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page).toHaveURL(/\/cong-thuc\/2204$/);
  await expect(page.getByRole('heading', { name: 'Đậu hũ kho cà chua', level: 1 })).toBeVisible();
  await expect(page.getByText('Hướng dẫn chế biến')).toBeVisible();
});

test('Issue 22: link YouTube có định dạng URL nhưng sai host bị báo lỗi', async ({ page }) => {
  await installApi(page, () => ({
    status: 400,
    body: { status: 400, code: 'VALIDATION_FAILED', title: 'Validation failed', detail: 'Dữ liệu công thức chưa hợp lệ.', errors: [{ field: 'youtubeUrl', message: 'Nhập link video YouTube hợp lệ hoặc để trống.' }] },
  }));
  await openForm(page);
  await fillValidRecipe(page);
  await action(page, 'Nhập URL từ host không phải YouTube', () => page.getByLabel('Link YouTube').fill('https://video.example.com/watch/123'));
  await action(page, 'Gửi URL để Backend validation phản hồi', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page.getByText('Nhập link video YouTube hợp lệ hoặc để trống.')).toBeVisible();
});
