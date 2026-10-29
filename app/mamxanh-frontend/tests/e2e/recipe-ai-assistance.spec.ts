import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

const options = {
  dishCategories: [{ code: 'BRAISED', label: 'Món kho' }],
  vegetarianTypes: [{ code: 'VEGAN', label: 'Thuần chay' }],
  difficulties: [{ code: 'EASY', label: 'Dễ' }, { code: 'MEDIUM', label: 'Trung bình' }, { code: 'HARD', label: 'Khó' }],
  units: [{ unitId: 1, code: 'g', name: 'gram', dimension: 'MASS' }],
};

const recipe = {
  id: 527, authorId: 901, authorName: 'Demo EXPERT', authorAvatarUrl: null,
  title: 'Đậu hũ kho', description: 'Mô tả cũ', instructions: 'Nấu đậu hũ với cà chua cho đến khi mềm.',
  dishCategory: 'BRAISED', vegetarianType: 'VEGAN', difficulty: 'EASY', servings: 2,
  prepTimeMin: 5, cookTimeMin: 15, youtubeUrl: null, status: 'PUBLISHED', media: [],
  ingredients: [{ ingredientId: 11, name: 'Đậu hũ', customName: null, unitId: 1, unitCode: 'g', unitName: 'gram', quantity: 200 }],
  likes: 0, dislikes: 0, likePercentage: null, viewCount: 0,
};

test('create form places AI suggestions in editable fields and does not publish automatically', async ({ page }) => {
  await seedDemoSession(page, 'EXPERT');
  let publishRequests = 0;
  let assistanceRequests: Record<string, unknown>[] = [];
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: options }));
  await page.route('**/api/v1/recipes/ingredient-options**', (route) => route.fulfill({ json: [{ ingredientId: 11, name: 'Đậu hũ' }] }));
  await page.route('**/api/v1/recipes/ai-assistance', async (route) => {
    const body = route.request().postDataJSON() as Record<string, unknown>;
    assistanceRequests.push(body);
    return route.fulfill({ json: { contentType: body.contentType, content: body.contentType === 'DESCRIPTION' ? 'Mô tả do AI gợi ý.' : '1. Kho đậu hũ trong nước sốt cà chua.' } });
  });
  await page.route('**/api/v1/recipes', async (route) => {
    if (route.request().method() === 'POST') publishRequests += 1;
    return route.fulfill({ status: 201, json: { recipeId: 600, title: 'Đậu hũ kho', status: 'PUBLISHED', publishedAt: '2026-10-11T10:00:00' } });
  });

  await page.goto('/dang-cong-thuc');
  await page.getByLabel('Tên món *').fill('Đậu hũ kho');
  await page.getByLabel('Thể loại món').selectOption('BRAISED');
  await page.getByLabel('Loại ăn chay').selectOption('VEGAN');
  await page.getByLabel('Khẩu phần').fill('2');
  await page.getByLabel('Chọn nguyên liệu 1').fill('Đậu hũ');
  await page.getByRole('button', { name: 'Đậu hũ', exact: true }).click();
  await page.getByLabel('Số lượng nguyên liệu 1').fill('200');
  await page.getByLabel('Đơn vị nguyên liệu 1').selectOption('1');

  await page.getByRole('button', { name: 'AI gợi ý viết mô tả' }).click();
  await expect(page.getByRole('textbox', { name: 'Mô tả' })).toHaveValue('Mô tả do AI gợi ý.');
  await page.getByRole('button', { name: 'AI gợi ý viết hướng dẫn chế biến' }).click();
  await expect(page.getByLabel('Hướng dẫn * (10–5.000 ký tự)')).toHaveValue('1. Kho đậu hũ trong nước sốt cà chua.');
  expect(assistanceRequests).toHaveLength(2);
  expect(assistanceRequests[0]).toMatchObject({ title: 'Đậu hũ kho', contentType: 'DESCRIPTION', ingredients: [{ ingredientId: 11, quantity: 200 }] });
  expect(publishRequests).toBe(0);
});

test('edit form keeps AI text editable and sends it only after the author saves', async ({ page }) => {
  await seedDemoSession(page, 'EXPERT');
  let updateBody: Record<string, unknown> | undefined;
  await page.route('**/api/v1/recipes/527/manage', (route) => route.fulfill({ json: recipe }));
  await page.route('**/api/v1/recipes/reference-data', (route) => route.fulfill({ json: {
    ingredients: [{ ingredientId: 11, name: 'Đậu hũ', unitId: 1, unitCode: 'g', unitName: 'gram' }],
  } }));
  await page.route('**/api/v1/recipes/ai-assistance', (route) => route.fulfill({ json: {
    contentType: 'INSTRUCTIONS', content: '1. Kho đậu hũ theo cách AI gợi ý.',
  } }));
  await page.route('**/api/v1/recipes/527', async (route) => {
    if (route.request().method() === 'PUT') updateBody = route.request().postDataJSON() as Record<string, unknown>;
    return route.fulfill({ json: recipe });
  });

  await page.goto('/cong-thuc/527/chinh-sua');
  const instructions = page.getByLabel('Hướng dẫn nấu *');
  await page.getByRole('button', { name: 'AI gợi ý viết hướng dẫn chế biến' }).click();
  await expect(instructions).toHaveValue('1. Kho đậu hũ theo cách AI gợi ý.');
  expect(updateBody).toBeUndefined();
  await instructions.fill('Tác giả đã tự chỉnh nội dung hướng dẫn này.');
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect.poll(() => updateBody?.instructions).toBe('Tác giả đã tự chỉnh nội dung hướng dẫn này.');
});

test('a Free plan error leaves the recipe fields available for manual entry', async ({ page }) => {
  await seedDemoSession(page, 'EXPERT');
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: options }));
  await page.route('**/api/v1/recipes/ingredient-options**', (route) => route.fulfill({ json: [
    { ingredientId: 11, name: 'Đậu hũ' },
  ] }));
  let assistanceRequests = 0;
  await page.route('**/api/v1/recipes/ai-assistance', (route) => {
    assistanceRequests += 1;
    return route.fulfill({
      status: 403, contentType: 'application/problem+json', json: { status: 403, code: 'AI_SUBSCRIPTION_REQUIRED', detail: 'Pro required' },
    });
  });

  await page.goto('/dang-cong-thuc');
  await page.getByLabel('Tên món *').fill('Đậu hũ kho');
  await page.getByLabel('Thể loại món').selectOption('BRAISED');
  await page.getByLabel('Loại ăn chay').selectOption('VEGAN');
  await page.getByLabel('Khẩu phần').fill('2');
  await page.getByLabel('Chọn nguyên liệu 1').fill('Đậu hũ');
  await page.getByRole('button', { name: 'Đậu hũ', exact: true }).click();
  await page.getByLabel('Số lượng nguyên liệu 1').fill('200');
  await page.getByLabel('Đơn vị nguyên liệu 1').selectOption('1');
  await page.getByRole('button', { name: 'AI gợi ý viết mô tả' }).click();
  await expect(page.getByRole('alert')).toContainText('gói Pro');
  expect(assistanceRequests).toBe(1);
  await expect(page.getByRole('textbox', { name: 'Mô tả' })).toBeEnabled();
  await expect(page.getByRole('button', { name: 'Xuất bản công thức' })).toBeEnabled();
});

test('AI request validates missing required recipe data before calling the API', async ({ page }) => {
  await seedDemoSession(page, 'EXPERT');
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: options }));
  let assistanceRequests = 0;
  await page.route('**/api/v1/recipes/ai-assistance', (route) => {
    assistanceRequests += 1;
    return route.fulfill({ json: { contentType: 'DESCRIPTION', content: 'unexpected' } });
  });

  await page.goto('/dang-cong-thuc');
  await page.getByRole('button', { name: 'AI gợi ý viết mô tả' }).click();
  await expect(page.getByRole('alert')).toContainText('hoàn tất nguyên liệu');
  expect(assistanceRequests).toBe(0);
});

test('edit page reports an invalid recipe id without making API requests', async ({ page }) => {
  await seedDemoSession(page, 'EXPERT');
  let apiRequests = 0;
  await page.route('**/api/v1/recipes/**', (route) => {
    apiRequests += 1;
    return route.fulfill({ status: 500 });
  });

  await page.goto('/cong-thuc/not-a-number/chinh-sua');
  await expect(page.getByText(/ID công thức hợp lệ/)).toBeVisible();
  expect(apiRequests).toBe(0);
});

test('hidden recipe is viewable in edit route but does not load editable references', async ({ page }) => {
  await seedDemoSession(page, 'EXPERT');
  let referenceRequests = 0;
  await page.route('**/api/v1/recipes/527/manage', (route) => route.fulfill({ json: { ...recipe, status: 'HIDDEN' } }));
  await page.route('**/api/v1/recipes/reference-data', (route) => {
    referenceRequests += 1;
    return route.fulfill({ json: { ingredients: [] } });
  });

  await page.goto('/cong-thuc/527/chinh-sua');
  await expect(page.getByRole('status')).toContainText('Đã bị Admin ẩn');
  await expect(page.getByText('Đậu hũ kho', { exact: true }).first()).toBeVisible();
  expect(referenceRequests).toBe(0);
});

test('expert can use AI Recipe Assistant modal to generate and apply a complete recipe', async ({ page }) => {
  await seedDemoSession(page, 'EXPERT');
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: options }));
  await page.route('**/api/v1/recipes/ingredient-options**', (route) => route.fulfill({
    json: [
      { ingredientId: 11, name: 'Cà chua' },
      { ingredientId: 12, name: 'Trứng gà' },
    ],
  }));

  await page.goto('/dang-cong-thuc');
  await page.getByRole('button', { name: 'Soạn với Trợ lý AI' }).click();
  await expect(page.getByText('Trợ lý AI đồng hành cùng Chuyên gia ẩm thực')).toBeVisible();

  // Test selecting meal sessions
  await page.getByRole('button', { name: /Bữa trưa/ }).click();
  await page.getByRole('button', { name: /Bữa tối/ }).click();
  await page.getByRole('button', { name: /Bữa sáng/ }).click();

  // Pick quick template
  await page.getByRole('button', { name: /Xào cà chua với trứng/ }).click();

  // Generate recipe
  await page.getByRole('button', { name: 'Phân tích nguyên liệu & gợi ý định lượng' }).click();

  // Verify proposed result appears
  await expect(page.getByText('Công thức đề xuất')).toBeVisible();

  // Apply to form
  await page.getByRole('button', { name: 'Nạp vào biểu mẫu công thức' }).click();

  // Modal closes and form receives AI values
  await expect(page.getByText('Trợ lý AI đồng hành cùng Chuyên gia ẩm thực')).toHaveCount(0);
  await expect(page.getByLabel('Tên món *')).toHaveValue('Xào cà chua với trứng');
});
