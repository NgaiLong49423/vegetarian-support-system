import type { Locator } from '@playwright/test';
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
      status: 403, contentType: 'application/problem+json', json: { status: 403, code: 'AI_SUBSCRIPTION_REQUIRED', detail: 'Plus or Pro required' },
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
  await expect(page.getByRole('alert').getByRole('link', { name: 'Xem các gói AI' })).toHaveAttribute('href', '/goi-ai');
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

async function prepareModal(page: import('@playwright/test').Page) {
  await seedDemoSession(page, 'EXPERT');
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: options }));
  await page.route('**/api/v1/recipes/ingredient-options**', (route) => route.fulfill({
    json: [{ ingredientId: 11, name: 'Đậu hũ' }],
  }));
  await page.goto('/dang-cong-thuc');
  await page.getByLabel('Tên món *').fill('Đậu hũ kho');
  await page.getByLabel('Thể loại món').selectOption('BRAISED');
  await page.getByLabel('Loại ăn chay').selectOption('VEGAN');
  await page.getByLabel('Khẩu phần').fill('2');
  await page.getByLabel('Chọn nguyên liệu 1').fill('Đậu hũ');
  await page.getByRole('button', { name: 'Đậu hũ', exact: true }).click();
  await page.getByLabel('Số lượng nguyên liệu 1').fill('200');
  await page.getByLabel('Đơn vị nguyên liệu 1').selectOption('1');
  await page.getByRole('button', { name: 'Soạn với Trợ lý AI' }).click();
  return page.getByRole('dialog', { name: 'Trợ lý AI đồng hành cùng Chuyên gia ẩm thực' });
}

const draft = {
  title: 'Đậu hũ kho', description: 'Mô tả Gemini từ API.', instructions: 'Kho đậu hũ cho chín đều rồi tắt bếp.',
  dishCategory: 'BRAISED', vegetarianType: 'VEGAN', difficulty: 'EASY', servings: 2,
  prepTimeMinutes: 5, cookTimeMinutes: 15, mealType: 'BREAKFAST', targetCaloriesPerServing: 350,
  totalCalories: 300, caloriesPerServing: 150,
  ingredients: [{ ingredientId: 11, unitId: 1, name: 'Đậu hũ', quantity: 300, unitCode: 'g', unitName: 'gram',
    calories: 300, energyKcalPer100g: 100, sourceName: 'Danh mục kiểm thử' }],
};
const generate = (dialog: Locator) => dialog.getByRole('button', { name: 'Phân tích nguyên liệu & gợi ý định lượng', exact: true });

test('full assistant uses the draft API and applies verified IDs only after explicit confirmation', async ({ page }) => {
  const requests: Record<string, unknown>[] = [];
  let publications = 0;
  await page.route('**/api/v1/recipes/ai/draft', route => {
    requests.push(route.request().postDataJSON());
    return route.fulfill({ json: draft });
  });
  await page.route('**/api/v1/recipes', route => { publications++; return route.fulfill({ status: 500 }); });
  const dialog = await prepareModal(page);
  await generate(dialog).click();
  await expect(dialog.getByRole('button', { name: /Nạp vào biểu mẫu công thức/ })).toBeVisible();
  expect(requests).toEqual([{ title: 'Đậu hũ kho', rawIngredients: 'Đậu hũ', mealType: 'BREAKFAST', targetCaloriesPerServing: 350, servings: 2, cookingMethod: 'STIR_FRY' }]);
  await expect(page.getByLabel('Số lượng nguyên liệu 1', { exact: true })).toHaveValue('200');
  await expect(page.getByRole('textbox', { name: 'Mô tả', exact: true })).toHaveValue('');
  await dialog.getByRole('button', { name: /Nạp vào biểu mẫu công thức/ }).click();
  await expect(page.getByLabel('Số lượng nguyên liệu 1', { exact: true })).toHaveValue('300');
  await expect(page.getByRole('textbox', { name: 'Mô tả', exact: true })).toHaveValue(draft.description);
  await expect(page.getByLabel('Hướng dẫn * (10–5.000 ký tự)')).toHaveValue(draft.instructions);
  expect(publications).toBe(0);
});

test('Pro denial leaves the original recipe intact and shows the upgrade link', async ({ page }) => {
  await page.route('**/api/v1/recipes/ai/draft', route => route.fulfill({ status: 403, contentType: 'application/problem+json', json: {
    status: 403, code: 'AI_SUBSCRIPTION_REQUIRED', requiredTier: 'PRO', detail: 'Cần gói Pro còn hiệu lực.', upgradeUrl: '/goi-ai',
  } }));
  const dialog = await prepareModal(page);
  await generate(dialog).click();
  await expect(dialog.getByRole('alert')).toContainText('Cần gói Pro');
  await expect(dialog.getByRole('link', { name: 'Xem các gói AI' })).toHaveAttribute('href', '/goi-ai');
  await expect(dialog.getByRole('button', { name: /Nạp vào biểu mẫu công thức/ })).toHaveCount(0);
  await page.keyboard.press('Escape');
  await expect(page.getByLabel('Số lượng nguyên liệu 1', { exact: true })).toHaveValue('200');
});

test('provider failure can be retried without a local fabricated recipe or lost author text', async ({ page }) => {
  let requests = 0;
  await page.route('**/api/v1/recipes/ai/draft', route => ++requests === 1
    ? route.fulfill({ status: 503, contentType: 'application/problem+json', json: { status: 503, code: 'AI_PROVIDER_UNAVAILABLE', detail: 'Dịch vụ AI tạm bận.' } })
    : route.fulfill({ json: draft }));
  const dialog = await prepareModal(page);
  await generate(dialog).click();
  await expect(dialog.getByRole('alert')).toContainText('Dịch vụ AI tạm bận');
  await expect(dialog.getByRole('button', { name: /Nạp vào biểu mẫu công thức/ })).toHaveCount(0);
  await generate(dialog).click();
  await expect(dialog.getByRole('button', { name: /Nạp vào biểu mẫu công thức/ })).toBeVisible();
  expect(requests).toBe(2);
});

test('desktop keeps the complete develop layout and meal presets without calling AI automatically', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1440, height: 1100 });
  let requests = 0;
  await page.route('**/api/v1/recipes/ai/draft', route => { requests++; return route.fulfill({ json: draft }); });
  const dialog = await prepareModal(page);
  await expect(dialog.getByText('Gợi ý nhanh 1-click (Rút ngắn thao tác):')).toBeVisible();
  for (const label of ['Bữa sáng', 'Bữa trưa', 'Bữa tối', 'Bữa phụ / Ăn nhẹ', 'Tùy chỉnh tự do']) {
    await expect(dialog.getByRole('button', { name: new RegExp(label) })).toBeVisible();
  }
  await dialog.getByRole('button', { name: /Đậu hũ sốt cà chua/ }).click();
  await expect(dialog.getByLabel(/Tên món ăn hoặc ý tưởng chế biến/)).toHaveValue('Đậu hũ sốt cà chua');
  await expect(dialog.getByLabel(/Ràng buộc Calo mục tiêu/)).toHaveValue('550');
  await expect(dialog.getByLabel('Phương pháp chế biến chính')).toHaveValue('BRAISED');
  await expect(dialog.getByLabel(/Ràng buộc Số khẩu phần ăn/)).toHaveValue('2');
  expect(requests).toBe(0);
  for (const theme of ['light', 'dark']) {
    await page.emulateMedia({ colorScheme: theme as 'light' | 'dark' });
    await page.evaluate(value => { document.documentElement.dataset.theme = value; }, theme);
    await dialog.evaluate(element => { element.scrollTop = 0; });
    await dialog.screenshot({ path: testInfo.outputPath(`assistant-develop-${theme}-top.png`), animations: 'disabled' });
    await generate(dialog).scrollIntoViewIfNeeded();
    await dialog.screenshot({ path: testInfo.outputPath(`assistant-develop-${theme}-controls.png`), animations: 'disabled' });
  }
});

test('mobile supports the full form and quantity changes use returned catalog energy', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.route('**/api/v1/recipes/ai/draft', route => route.fulfill({ json: draft }));
  const dialog = await prepareModal(page);
  await generate(dialog).click();
  await dialog.getByLabel('Định lượng Đậu hũ', { exact: true }).fill('350');
  await expect(dialog.getByRole('row').filter({ hasText: 'Đậu hũ' }).getByText('350 kcal', { exact: true })).toBeVisible();
  await expect(dialog.getByTitle('Nguồn năng lượng: Danh mục kiểm thử')).toBeVisible();
  const box = await dialog.boundingBox();
  expect(box!.x).toBeGreaterThanOrEqual(0);
  expect(box!.x + box!.width).toBeLessThanOrEqual(390);
  await dialog.evaluate(element => { element.scrollTop = 0; });
  await dialog.screenshot({ path: testInfo.outputPath('assistant-full-mobile.png'), animations: 'disabled' });
});

test('unresolved catalog input reports an actionable error without fabricated output', async ({ page }) => {
  await page.route('**/api/v1/recipes/ai/draft', route => route.fulfill({ status: 400, contentType: 'application/problem+json', json: {
    status: 400, code: 'RECIPE_DATA_INVALID', detail: 'Chưa xác định duy nhất nguyên liệu. Hãy dùng đúng tên trong danh mục.',
  } }));
  const dialog = await prepareModal(page);
  await generate(dialog).click();
  await expect(dialog.getByRole('alert')).toContainText('đúng tên trong danh mục');
  await expect(dialog.getByRole('button', { name: /Nạp vào biểu mẫu công thức/ })).toHaveCount(0);
});

test('closing an in-flight draft prevents its late response from changing the form', async ({ page }) => {
  let release!: () => void;
  const pending = new Promise<void>(resolve => { release = resolve; });
  await page.route('**/api/v1/recipes/ai/draft', async route => { await pending; await route.fulfill({ json: draft }); });
  const dialog = await prepareModal(page);
  const sent = page.waitForRequest(request => request.url().endsWith('/recipes/ai/draft'));
  await generate(dialog).click();
  await sent;
  await page.keyboard.press('Escape');
  const completed = page.waitForResponse(response => response.url().endsWith('/recipes/ai/draft'));
  release();
  await completed;
  await expect(page.getByLabel('Số lượng nguyên liệu 1', { exact: true })).toHaveValue('200');
  await expect(page.getByRole('textbox', { name: 'Mô tả', exact: true })).toHaveValue('');
  await page.getByRole('button', { name: 'Soạn với Trợ lý AI' }).click();
  await expect(dialog.getByRole('button', { name: /Nạp vào biểu mẫu công thức/ })).toHaveCount(0);
});
