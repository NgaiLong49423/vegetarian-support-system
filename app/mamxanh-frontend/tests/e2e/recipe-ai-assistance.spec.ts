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

test('assistant modal sends selected form ingredients to the guarded API and preserves manual publication', async ({ page }) => {
  const requests: Record<string, unknown>[] = [];
  let publications = 0;
  await page.route('**/api/v1/recipes/ai-assistance', (route) => {
    const body = route.request().postDataJSON();
    requests.push(body);
    return route.fulfill({ json: { contentType: body.contentType,
      content: body.contentType === 'DESCRIPTION' ? 'Mô tả từ API.' : 'Kho đậu hũ đã chọn cho chín đều.' } });
  });
  await page.route('**/api/v1/recipes', (route) => {
    publications++;
    return route.fulfill({ status: 500, json: {} });
  });
  const dialog = await prepareModal(page);
  expect(requests).toHaveLength(0);
  await dialog.getByRole('button', { name: 'AI gợi ý viết mô tả' }).click();
  await expect(dialog.getByText('Mô tả từ API.', { exact: true })).toBeVisible();
  await dialog.getByRole('radio', { name: 'Hướng dẫn chế biến', exact: true }).check();
  await dialog.getByRole('button', { name: 'AI gợi ý viết hướng dẫn chế biến' }).click();
  await expect(dialog.getByText('Kho đậu hũ đã chọn cho chín đều.', { exact: true })).toBeVisible();
  expect(requests).toEqual(['DESCRIPTION', 'INSTRUCTIONS'].map(contentType => ({
    contentType, title: 'Đậu hũ kho', dishCategory: 'BRAISED', vegetarianType: 'VEGAN', servings: 2,
    ingredients: [{ ingredientId: 11, unitId: 1, quantity: 200 }],
  })));
  await dialog.getByRole('button', { name: 'Đóng và trở lại biểu mẫu' }).click();
  await expect(page.getByRole('textbox', { name: 'Mô tả' })).toHaveValue('Mô tả từ API.');
  await expect(page.getByLabel('Hướng dẫn * (10–5.000 ký tự)')).toHaveValue('Kho đậu hũ đã chọn cho chín đều.');
  await expect(page.getByLabel('Số lượng nguyên liệu 1')).toHaveValue('200');
  await expect(page.getByLabel('Chọn nguyên liệu 2')).toHaveCount(0);
  await page.getByRole('textbox', { name: 'Mô tả' }).fill('Tác giả tự chỉnh sửa.');
  expect(publications).toBe(0);
});

test('Free Expert cannot obtain modal AI content after the Backend denies entitlement', async ({ page }) => {
  let requests = 0;
  await page.route('**/api/v1/recipes/ai-assistance', (route) => {
    requests++;
    return route.fulfill({ status: 403, contentType: 'application/problem+json', json: {
      status: 403, code: 'AI_SUBSCRIPTION_REQUIRED', detail: 'Plus or Pro required', requiredTier: 'PRO', upgradeUrl: '/goi-ai',
    } });
  });
  const dialog = await prepareModal(page);
  await dialog.getByRole('button', { name: 'AI gợi ý viết mô tả' }).click();
  await expect(dialog.getByRole('alert')).toContainText('gói Pro');
  await expect(dialog.getByRole('link', { name: 'Xem các gói AI' })).toHaveAttribute('href', '/goi-ai');
  await dialog.getByRole('button', { name: 'Đóng và trở lại biểu mẫu' }).click();
  await expect(page.getByRole('textbox', { name: 'Mô tả' })).toHaveValue('');
  await expect(page.getByRole('textbox', { name: 'Mô tả' })).toBeEnabled();
  await expect(page.getByRole('button', { name: 'Xuất bản công thức' })).toBeEnabled();
  expect(requests).toBe(1);
});

test('modal preserves author text across provider failures and retries through the API', async ({ page }) => {
  let requests = 0;
  await page.route('**/api/v1/recipes/ai-assistance', (route) => {
    requests++;
    return requests === 1
      ? route.fulfill({ status: 502, contentType: 'application/problem+json', json: {
        status: 502, code: 'AI_RESPONSE_INVALID', detail: 'Incomplete provider output',
      } })
      : route.fulfill({ json: { contentType: 'INSTRUCTIONS', content: 'Kho đậu hũ chín đều rồi tắt bếp.' } });
  });
  const dialog = await prepareModal(page);
  await dialog.getByRole('button', { name: 'Đóng và trở lại biểu mẫu' }).click();
  await page.getByLabel('Hướng dẫn * (10–5.000 ký tự)').fill('Hướng dẫn do tác giả tự viết.');
  await page.getByRole('button', { name: 'Soạn với Trợ lý AI' }).click();
  await dialog.getByRole('radio', { name: 'Hướng dẫn chế biến', exact: true }).check();
  await dialog.getByRole('button', { name: 'AI gợi ý viết hướng dẫn chế biến' }).click();
  await expect(dialog.getByRole('alert')).toContainText('AI hiện không tạo được nội dung hợp lệ');
  await expect(dialog.getByText('Hướng dẫn do tác giả tự viết.', { exact: true })).toBeVisible();
  await dialog.getByRole('radio', { name: 'Hướng dẫn chế biến', exact: true }).check();
  await dialog.getByRole('button', { name: 'AI gợi ý viết hướng dẫn chế biến' }).click();
  await expect(dialog.getByText('Kho đậu hũ chín đều rồi tắt bếp.', { exact: true })).toBeVisible();
  expect(requests).toBe(2);
});

test('mobile modal validates form context before requesting AI and can return to manual editing', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 360, height: 800 });
  await seedDemoSession(page, 'EXPERT');
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: options }));
  let requests = 0;
  await page.route('**/api/v1/recipes/ai-assistance', (route) => {
    requests++;
    return route.fulfill({ status: 500, json: {} });
  });
  await page.goto('/dang-cong-thuc');
  await page.getByRole('button', { name: 'Soạn với Trợ lý AI' }).click();
  const dialog = page.getByRole('dialog');
  await expect(dialog.getByText('Chưa nhập tên món', { exact: false })).toBeVisible();
  await dialog.getByRole('button', { name: 'AI gợi ý viết mô tả' }).click();
  await expect(dialog.getByRole('alert')).toContainText('hoàn tất nguyên liệu');
  expect(requests).toBe(0);
  const box = await dialog.boundingBox();
  expect(box!.x).toBeGreaterThanOrEqual(0);
  expect(box!.x + box!.width).toBeLessThanOrEqual(360);
  await dialog.evaluate(element => { element.scrollTop = 0; });
  await dialog.screenshot({ path: testInfo.outputPath('assistant-form-mobile.png'), animations: 'disabled' });
  await testInfo.attach('fr10-assistant-mobile', { body: await page.screenshot({ animations: 'disabled' }), contentType: 'image/png' });
  await page.keyboard.press('Escape');
  await expect(dialog).toHaveCount(0);
  await expect(page.getByLabel('Tên món *')).toBeEnabled();
});

test('restored assistant form edits actual recipe context and quantities before calling the API', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1440, height: 1100 });
  const requests: Record<string, unknown>[] = [];
  await page.route('**/api/v1/recipes/ai-assistance', route => {
    requests.push(route.request().postDataJSON());
    return route.fulfill({ json: { contentType: 'DESCRIPTION', content: 'Mô tả thật từ API cho món đã chỉnh.' } });
  });
  const dialog = await prepareModal(page);
  await dialog.getByLabel('Tên món hoặc ý tưởng chế biến *', { exact: true }).fill('Đậu hũ kho tiêu');
  await dialog.getByLabel('Số khẩu phần ăn *', { exact: true }).fill('3');
  await dialog.getByLabel('Số lượng nguyên liệu 1', { exact: true }).fill('300');
  const inputIds = await dialog.locator('input[id], select[id]').evaluateAll(elements => elements.map(element => element.id));
  for (const id of inputIds) await expect(page.locator(`[id="${id}"]`)).toHaveCount(1);
  await dialog.evaluate(element => { element.scrollTop = 0; });
  await dialog.screenshot({ path: testInfo.outputPath('assistant-form-desktop.png'), animations: 'disabled' });
  await dialog.getByRole('button', { name: 'AI gợi ý viết mô tả' }).click();
  await expect(dialog.getByText('Mô tả thật từ API cho món đã chỉnh.', { exact: true })).toBeVisible();
  expect(requests).toEqual([{ contentType: 'DESCRIPTION', title: 'Đậu hũ kho tiêu', dishCategory: 'BRAISED', vegetarianType: 'VEGAN', servings: 3,
    ingredients: [{ ingredientId: 11, unitId: 1, quantity: 300 }] }]);
  await dialog.getByRole('button', { name: 'Đóng và trở lại biểu mẫu' }).click();
  await expect(page.getByLabel('Tên món *', { exact: true })).toHaveValue('Đậu hũ kho tiêu');
  await expect(page.getByLabel('Số lượng nguyên liệu 1', { exact: true })).toHaveValue('300');
  await expect(page.getByRole('textbox', { name: 'Mô tả', exact: true })).toHaveValue('Mô tả thật từ API cho món đã chỉnh.');
});
