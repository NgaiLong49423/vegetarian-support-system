import { expect, test, type Page } from '@playwright/test';

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

async function installApi(page: Page, getPostResponse: (body: Record<string, any>) => PostResponse) {
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: options }));
  await page.route('**/api/v1/recipes/ingredient-options**', (route) => {
    const query = new URL(route.request().url()).searchParams.get('query')?.toLowerCase() ?? '';
    const matches = ingredients.filter((item) => item.name.toLowerCase().includes(query));
    return route.fulfill({ json: matches });
  });
  await page.route('**/api/v1/recipes', async (route) => {
    const body = route.request().postDataJSON() as Record<string, any>;
    const response = getPostResponse(body);
    await route.fulfill({
      status: response.status,
      contentType: response.status >= 400 ? 'application/problem+json' : 'application/json',
      json: response.body,
    });
  });
}

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

test('Issue 22: g là bội số nguyên 100; kg thập phân phải đổi ra gram chia hết cho 100', async ({ page }) => {
  let publishCount = 0;
  await installApi(page, (body) => {
    publishCount++;
    return { status: 201, body: { recipeId: 2260, title: body.title, status: 'PUBLISHED', publishedAt: '2026-10-04T08:00:00' } };
  });
  await openForm(page);
  await fillValidRecipe(page, 'g');
  const quantity = page.getByLabel('Số lượng nguyên liệu 1');

  for (const invalidGram of ['80', '120', '150', '100.5']) {
    await action(page, `Nhập ${invalidGram} g`, () => quantity.fill(invalidGram));
    await action(page, `Thử đăng với ${invalidGram} g`, () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
    await expect(page.getByText('Số lượng đơn vị g phải là bội số của 100 g (100, 200, 500...).')).toBeVisible();
  }
  expect(publishCount).toBe(0);

  for (const validGram of ['100', '200', '500']) {
    await action(page, `Nhập ${validGram} g`, () => quantity.fill(validGram));
    await action(page, `Đăng với ${validGram} g`, () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
    await expect(page.getByText('Đã đăng công thức #2260.')).toBeVisible();
  }
  expect(publishCount).toBe(3);

  await action(page, 'Đổi đơn vị từ g sang kg', () => page.getByLabel('Đơn vị nguyên liệu 1').selectOption({ label: 'kilogram (kg)' }));
  for (const validKilogram of ['0.5', '1.5']) {
    await action(page, `Nhập ${validKilogram} kg`, () => quantity.fill(validKilogram));
    await action(page, `Đăng với ${validKilogram} kg`, () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
    await expect(page.getByText('Đã đăng công thức #2260.')).toBeVisible();
  }
  expect(publishCount).toBe(5);

  await action(page, 'Nhập 0.15 kg (tương đương 150 g, không chia hết cho 100)', () => quantity.fill('0.15'));
  await action(page, 'Thử đăng với 0.15 kg', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page.getByText('Số lượng kg sau khi đổi ra gram phải chia hết cho 100 g (ví dụ 0,1 kg hoặc 0,5 kg).')).toBeVisible();
  expect(publishCount).toBe(5);
});

test('Issue 22: g/kg đi trực tiếp; đơn vị đếm chỉ qua khi API xác nhận conversion có sẵn', async ({ page }) => {
  let conversionExists = false;
  await installApi(page, (body) => {
    const usesCountUnit = body.ingredients?.some((item: Record<string, number>) => item.unitId === 3);
    if (usesCountUnit && !conversionExists) {
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
  await expect(page.getByText('Đã đăng công thức #2202.')).toBeVisible();
  conversionExists = true;
  await action(page, 'Chọn lại đơn vị quả sau khi mock dữ liệu conversion có sẵn', () => page.getByLabel('Đơn vị nguyên liệu 1').selectOption({ label: 'quả (quả)' }));
  await action(page, 'Gửi lại cặp nguyên liệu–đơn vị có conversion', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page.getByText('Đã đăng công thức #2202.')).toBeVisible();
});

test('Issue 22: 0 ảnh được đăng; giới hạn tối đa 5 và yêu cầu cover đúng một ảnh', async ({ page }) => {
  await installApi(page, (body) => ({ status: 201, body: { recipeId: 2203, title: body.title, status: 'PUBLISHED', publishedAt: '2026-10-04T08:00:00' } }));
  await openForm(page);
  await fillValidRecipe(page);
  await action(page, 'Để trống ảnh và xuất bản', () => page.getByRole('button', { name: 'Xuất bản công thức' }).click());
  await expect(page.getByText('Đã đăng công thức #2203.')).toBeVisible();
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
  await expect(page.getByText('Đã đăng công thức #2204.')).toBeVisible();
  await expect(page.getByLabel('Mô tả')).toHaveValue('');
  await expect(page.getByLabel('Link YouTube')).toHaveValue('');
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
