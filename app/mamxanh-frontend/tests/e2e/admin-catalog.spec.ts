import { expect, test } from './baseFixtures';

const banana = {
  id: 7,
  name: 'Chuối tây',
  ingredientGroup: 'Trái cây',
  sourceName: 'Nguồn kiểm thử',
  sourceUrl: null,
  referenceDate: '2026-09-01',
  nutritionSupported: false,
  active: true,
  recipeUsageCount: 0,
  nutrition: {
    energyKcalPer100g: null,
    proteinGramsPer100g: null,
    carbohydrateGramsPer100g: null,
    totalFatGramsPer100g: null,
    fiberGramsPer100g: null,
    calciumMgPer100g: null,
    ironMgPer100g: null,
    vitaminB12MicrogramsPer100g: null,
    zincMgPer100g: null,
  },
};

const ingredientPage = (content: typeof banana[]) => ({
  success: true,
  data: { content, page: 0, size: 20, totalElements: content.length, totalPages: content.length ? 1 : 0 },
});

test('catalog management reports backend authorization denial without exposing edit forms as authorized', async ({ page }) => {
  await page.route('**/api/v1/admin/**', (route) => route.fulfill({
    status: 403,
    contentType: 'application/problem+json',
    body: JSON.stringify({ title: 'Forbidden', status: 403, detail: 'Forbidden' }),
  }));

  await page.goto('/quan-tri/danh-muc');
  await expect(page.getByRole('heading', { name: 'Danh mục nguyên liệu dinh dưỡng' })).toBeVisible();
  await expect(page.getByRole('alert')).toContainText('403 Forbidden');
  await expect(page.getByRole('button', { name: 'Thêm nguyên liệu' })).toHaveCount(0);
});

test('administrator can add, search, edit and deactivate a Vietnamese ingredient', async ({ page }) => {
  const data = [banana];
  await page.route('**/api/v1/admin/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    if (url.pathname.endsWith('/ingredients/options')) return route.fulfill({ json: { success: true, data: { groups: ['Rau củ', 'Đậu & Chế phẩm', 'Ngũ cốc', 'Các loại hạt', 'Trái cây', 'Gia vị chay'], sources: ['USDA FoodData Central', 'NIH/Viện Dinh dưỡng'] } } });
    if (url.pathname.endsWith('/ingredients/search') && method === 'GET') {
      const query = (url.searchParams.get('query') ?? '').toLocaleLowerCase('vi');
      return route.fulfill({ json: ingredientPage(data.filter((item) => !query || `${item.name} ${item.ingredientGroup}`.toLocaleLowerCase('vi').includes(query))) });
    }
    if (url.pathname.endsWith('/ingredients') && method === 'GET') {
      const query = (url.searchParams.get('query') ?? '').toLocaleLowerCase('vi');
      return route.fulfill({ json: { success: true, data: data.filter((item) => !query || `${item.name} ${item.ingredientGroup}`.toLocaleLowerCase('vi').includes(query)) } });
    }
    if (url.pathname.endsWith('/ingredients') && method === 'POST') {
      const body = request.postDataJSON();
      const created = { ...banana, ...body, id: 8, active: true };
      data.push(created);
      return route.fulfill({ status: 201, json: { success: true, data: created } });
    }
    if (/\/ingredients\/\d+$/.test(url.pathname) && method === 'PUT') {
      const id = Number(url.pathname.split('/').at(-1));
      const index = data.findIndex((item) => item.id === id);
      data[index] = { ...data[index], ...request.postDataJSON() };
      return route.fulfill({ json: { success: true, data: data[index] } });
    }
    if (/\/ingredients\/\d+\/status$/.test(url.pathname) && method === 'PATCH') {
      const id = Number(url.pathname.match(/\/ingredients\/(\d+)\/status$/)?.[1]);
      const index = data.findIndex((item) => item.id === id);
      data[index].active = request.postDataJSON().active;
      return route.fulfill({ json: { success: true, data: data[index] } });
    }
    return route.fulfill({ status: 404, json: { detail: 'Not found' } });
  });

  await page.goto('/quan-tri/danh-muc');
  await expect(page.getByText('Chuối tây', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Mở biểu mẫu thêm nguyên liệu' }).click();
  await expect(page.getByRole('heading', { name: 'Thêm nguyên liệu' })).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(page.getByRole('heading', { name: 'Thêm nguyên liệu' })).toHaveCount(0);
  await page.getByRole('button', { name: 'Mở biểu mẫu thêm nguyên liệu' }).click();
  await page.getByLabel('Tên chuẩn tiếng Việt').fill('Nấm đùi gà');
  await page.getByRole('combobox', { name: 'Nhóm nguyên liệu' }).last().selectOption('Đậu & Chế phẩm');
  await page.getByRole('combobox', { name: 'Nguồn dữ liệu hoặc cách ước tính' }).fill('USDA FoodData Central');
  await page.getByRole('button', { name: 'Thêm nguyên liệu', exact: true }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Đã thêm nguyên liệu' })).toBeVisible();

  const nameSearch = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return url.pathname.endsWith('/admin/ingredients/search') && url.searchParams.get('query') === 'Nấm';
  });
  await page.getByPlaceholder('Tìm tên nguyên liệu').fill('Nấm');
  await page.getByRole('button', { name: 'Tìm và lọc' }).click();
  await nameSearch;
  await expect(page.getByText('Nấm đùi gà', { exact: true })).toBeVisible();
  await expect(page.getByText('Chuối tây', { exact: true })).toHaveCount(0);

  await page.getByRole('button', { name: 'Sửa Nấm đùi gà' }).click();
  await page.getByRole('button', { name: 'Hủy' }).click();
  await expect(page.getByRole('heading', { name: 'Sửa nguyên liệu' })).toHaveCount(0);
  await page.getByRole('button', { name: 'Sửa Nấm đùi gà' }).click();
  await page.getByLabel('Tên chuẩn tiếng Việt').fill('Nấm đùi gà tươi');
  await page.getByLabel('Nguồn dữ liệu hoặc cách ước tính').fill('NIH/Viện Dinh dưỡng');
  await page.getByLabel('Đường dẫn nguồn (không bắt buộc)').fill('https://example.org/ingredient');
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByText('Nấm đùi gà tươi', { exact: true })).toBeVisible();
  await expect(page.getByText('NIH/Viện Dinh dưỡng', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Ngừng sử dụng Nấm đùi gà tươi' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'liên kết cũ được giữ nguyên' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Bật lại Nấm đùi gà tươi' })).toBeVisible();
  await page.getByRole('button', { name: 'Bật lại Nấm đùi gà tươi' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Đã bật lại nguyên liệu' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Ngừng sử dụng Nấm đùi gà tươi' })).toBeVisible();
});

test('administrator manages count and volume conversions for one ingredient', async ({ page }) => {
  const data = [{ ...banana, recipeUsageCount: 0 }];
  const units = [
    { id: 1, code: 'mg', name: 'miligram', dimension: 'MASS', baseFactor: 0.001, active: true },
    { id: 2, code: 'g', name: 'gram', dimension: 'MASS', baseFactor: 1, active: true },
    { id: 3, code: 'kg', name: 'kilogram', dimension: 'MASS', baseFactor: 1000, active: true },
    { id: 4, code: 'ml', name: 'mililít', dimension: 'VOLUME', baseFactor: 1, active: true },
    { id: 5, code: 'L', name: 'lít', dimension: 'VOLUME', baseFactor: 1000, active: true },
    { id: 6, code: 'tsp', name: 'muỗng cà phê', dimension: 'VOLUME', baseFactor: 5, active: true },
    { id: 7, code: 'tbsp', name: 'muỗng canh', dimension: 'VOLUME', baseFactor: 15, active: true },
    { id: 8, code: 'cup', name: 'cup', dimension: 'VOLUME', baseFactor: 240, active: true },
    { id: 9, code: 'quả', name: 'quả', dimension: 'COUNT', baseFactor: 1, active: true },
    { id: 10, code: 'củ', name: 'củ', dimension: 'COUNT', baseFactor: 1, active: true },
    { id: 11, code: 'miếng', name: 'miếng', dimension: 'COUNT', baseFactor: 1, active: true },
  ];
  const conversions: Array<{ ingredientId: number; unitId: number; gramsPerUnit: number; approximate: boolean; active: boolean }> = [];
  await page.route('**/api/v1/admin/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    if (url.pathname.endsWith('/ingredients/options')) return route.fulfill({ json: { success: true, data: { groups: ['Rau củ'], sources: ['Nguồn kiểm thử'] } } });
    if (url.pathname.endsWith('/ingredients/search') && method === 'GET') return route.fulfill({ json: ingredientPage(data) });
    if (url.pathname.endsWith('/ingredients') && method === 'GET') return route.fulfill({ json: { success: true, data } });
    if (url.pathname.endsWith('/units') && method === 'GET') return route.fulfill({ json: { success: true, data: units } });
    if (url.pathname.endsWith('/ingredient-unit-conversions') && method === 'GET') {
      const ingredientId = url.searchParams.get('ingredientId');
      return route.fulfill({ json: { success: true, data: ingredientId ? conversions.filter((item) => item.ingredientId === Number(ingredientId)) : conversions } });
    }
    const countMatch = url.pathname.match(/\/ingredients\/(\d+)\/unit-conversions\/(\d+)$/);
    if (countMatch && ['POST', 'PUT'].includes(method)) {
      const conversion = { ingredientId: Number(countMatch[1]), unitId: Number(countMatch[2]), ...request.postDataJSON(), active: true };
      const index = conversions.findIndex((row) => row.ingredientId === conversion.ingredientId && row.unitId === conversion.unitId);
      if (index < 0) conversions.push(conversion); else conversions[index] = conversion;
      return route.fulfill({ json: { success: true, data: conversion } });
    }
    if (countMatch && method === 'PATCH') {
      const conversion = conversions.find((row) => row.ingredientId === Number(countMatch[1]) && row.unitId === Number(countMatch[2]));
      if (conversion) conversion.active = request.postDataJSON().active;
      return route.fulfill({ json: { success: true, data: conversion } });
    }
    if (url.pathname.endsWith('/ingredients/7/volume-conversion') && method === 'PUT') {
      const body = request.postDataJSON();
      for (const unit of units.filter((row) => row.dimension === 'VOLUME')) {
        const conversion = { ingredientId: 7, unitId: unit.id, gramsPerUnit: body.gramsPerMl * unit.baseFactor, approximate: body.approximate, active: true };
        const index = conversions.findIndex((row) => row.ingredientId === 7 && row.unitId === unit.id);
        if (index < 0) conversions.push(conversion); else conversions[index] = conversion;
      }
      return route.fulfill({ json: { success: true, data: conversions.filter((row) => row.ingredientId === 7 && units.find((unit) => unit.id === row.unitId)?.dimension === 'VOLUME') } });
    }
    if (url.pathname.endsWith('/ingredients/7/volume-conversion/status') && method === 'PATCH') {
      const { active } = request.postDataJSON();
      conversions.filter((row) => units.find((unit) => unit.id === row.unitId)?.dimension === 'VOLUME').forEach((row) => { row.active = active; });
      return route.fulfill({ json: { success: true, data: conversions } });
    }
    return route.fulfill({ status: 404, json: { detail: 'Not found' } });
  });

  await page.goto('/quan-tri/danh-muc');
  await expect(page.getByLabel('Đơn vị được hỗ trợ cho Chuối tây')).toContainText('mg');
  await page.getByRole('button', { name: 'Đơn vị' }).click();
  await expect(page.getByRole('heading', { name: 'Quản lý đơn vị · Chuối tây' })).toBeVisible();
  await expect(page.getByText('cup: 0.0000 g')).toBeVisible();
  await page.getByRole('dialog').getByLabel('Đơn vị', { exact: true }).selectOption('9');
  await page.getByLabel('Số gram trên đơn vị').fill('120');
  await page.getByRole('button', { name: 'Lưu quy đổi đếm' }).click();
  await expect(page.getByText('quả: 120 g · xấp xỉ')).toBeVisible();
  await page.getByLabel('1 ml tương đương bao nhiêu gram').fill('0.91');
  await expect(page.getByText('cup: 218.4 g')).toBeVisible();
  await page.getByRole('button', { name: 'Lưu và đồng bộ 5 đơn vị thể tích' }).click();
  await expect(page.getByText('ml: 0.91 g · xấp xỉ')).toBeVisible();
  await page.getByRole('button', { name: 'Ngừng sử dụng', exact: true }).first().click();
  await expect(page.getByRole('button', { name: 'Khôi phục', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Đóng' }).click();
  await expect(page.getByRole('heading', { name: 'Quản lý đơn vị · Chuối tây' })).toHaveCount(0);
});

test('administrator saves nutrition values and backend validates enabling calculation support', async ({ page }) => {
  const data = [{ ...banana, nutrition: { ...banana.nutrition } }];
  await page.route('**/api/v1/admin/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    if (url.pathname.endsWith('/ingredients/options')) return route.fulfill({ json: { success: true, data: { groups: ['Rau củ', 'Đậu & Chế phẩm', 'Ngũ cốc', 'Các loại hạt', 'Trái cây', 'Gia vị chay'], sources: ['USDA FoodData Central', 'NIH/Viện Dinh dưỡng'] } } });
    if (url.pathname.endsWith('/ingredients/search') && method === 'GET') return route.fulfill({ json: ingredientPage(data) });
    if (url.pathname.endsWith('/ingredients') && method === 'GET') {
      return route.fulfill({ json: { success: true, data } });
    }
    if (/\/ingredients\/7\/nutrition$/.test(url.pathname) && method === 'PUT') {
      data[0].nutrition = request.postDataJSON();
      return route.fulfill({ json: { success: true, data: data[0] } });
    }
    if (/\/ingredients\/7\/nutrition-support$/.test(url.pathname) && method === 'PATCH') {
      const { enabled } = request.postDataJSON();
      if (enabled && Object.values(data[0].nutrition).every((value) => value === null)) {
        return route.fulfill({ status: 409, contentType: 'application/problem+json', json: {
          title: 'Conflict', status: 409, code: 'NUTRITION_VALUES_EMPTY', detail: 'Cần nhập ít nhất một chỉ tiêu dinh dưỡng trước khi bật hỗ trợ tính.',
        } });
      }
      data[0].nutritionSupported = enabled;
      return route.fulfill({ json: { success: true, data: data[0] } });
    }
    return route.fulfill({ status: 404, json: { detail: 'Not found' } });
  });

  await page.goto('/quan-tri/danh-muc');
  await page.getByRole('button', { name: 'Sửa dinh dưỡng Chuối tây' }).click();
  await expect(page.getByLabel('Năng lượng trên 100 g')).toHaveValue('');
  await page.getByRole('button', { name: 'Bật hỗ trợ tính' }).click();
  await expect(page.getByRole('alert')).toContainText('Cần nhập ít nhất một chỉ tiêu dinh dưỡng');
  expect(data[0].nutritionSupported).toBe(false);
  await page.getByLabel('Năng lượng trên 100 g').fill('100');
  const saveResponse = page.waitForResponse((response) => response.request().method() === 'PUT' && response.url().endsWith('/admin/ingredients/7/nutrition'));
  await page.getByRole('button', { name: 'Lưu chỉ tiêu' }).click();
  expect((await saveResponse).ok()).toBeTruthy();
  expect(data[0].nutrition.energyKcalPer100g).toBe(100);
  expect(data[0].nutrition.proteinGramsPer100g).toBeNull();
  await expect(page.getByRole('status').filter({ hasText: 'Đã lưu số liệu dinh dưỡng. Các chỉ tiêu để trống sẽ không được tính.' })).toBeVisible();

  const partialEnable = page.waitForResponse((response) => response.request().method() === 'PATCH' && response.url().endsWith('/admin/ingredients/7/nutrition-support'));
  await page.getByRole('button', { name: 'Bật hỗ trợ tính' }).click();
  expect((await partialEnable).status()).toBe(200);
  expect(data[0].nutritionSupported).toBe(true);
  expect(data[0].nutrition.proteinGramsPer100g).toBeNull();
  await expect(page.getByText('Đang tính dữ liệu có sẵn', { exact: true })).toBeVisible();

  const values: Record<string, string> = {
    'Chất đạm trên 100 g': '10',
    'Carbohydrate trên 100 g': '20',
    'Chất béo trên 100 g': '3',
    'Chất xơ trên 100 g': '4',
    'Canxi trên 100 g': '20',
    'Sắt trên 100 g': '2',
    'Vitamin B12 trên 100 g': '0.1',
    'Kẽm trên 100 g': '1.3',
  };
  for (const [label, value] of Object.entries(values)) await page.getByLabel(label).fill(value);
  await page.getByRole('button', { name: 'Lưu chỉ tiêu' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Đã lưu số liệu dinh dưỡng. Các chỉ tiêu để trống sẽ không được tính.' })).toBeVisible();
  expect(data[0].nutritionSupported).toBe(true);
  expect(data[0].nutrition.proteinGramsPer100g).toBe(10);
  await expect(page.getByText('Đang tính dữ liệu có sẵn', { exact: true })).toBeVisible();
});

test('administrator filters the catalog through the paginated endpoint and reads linked recipes', async ({ page }) => {
  const filtered = { ...banana, id: 9, name: 'Nấm bào ngư', ingredientGroup: 'Rau củ', recipeUsageCount: 1 };
  let lastSearchUrl = '';
  await page.route('**/api/v1/admin/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname.endsWith('/ingredients/options')) return route.fulfill({ json: { success: true, data: { groups: ['Rau củ', 'Đậu & Chế phẩm', 'Ngũ cốc', 'Các loại hạt', 'Trái cây', 'Gia vị chay'], sources: ['USDA FoodData Central', 'NIH/Viện Dinh dưỡng'] } } });
    if (url.pathname.endsWith('/ingredients/search') && request.method() === 'GET') {
      lastSearchUrl = url.toString();
      return route.fulfill({ json: {
        success: true,
        data: { content: [filtered], page: Number(url.searchParams.get('page')), size: 20, totalElements: 21, totalPages: 2 },
      } });
    }
    if (url.pathname.endsWith('/ingredients') && request.method() === 'GET') return route.fulfill({ json: { success: true, data: [banana, filtered] } });
    if (/\/ingredients\/9$/.test(url.pathname)) return route.fulfill({ json: { success: true, data: filtered } });
    if (/\/ingredients\/9\/recipes\/search$/.test(url.pathname)) return route.fulfill({ json: { success: true, data: {
      content: [{ recipeId: 30, title: 'Canh nấm rau củ', status: 'PUBLISHED', publishedAt: '2026-10-10T10:00:00' }],
      page: 0, size: 10, totalElements: 1, totalPages: 1,
    } } });
    return route.fulfill({ status: 404, json: { detail: 'Not found' } });
  });

  await page.goto('/quan-tri/danh-muc');
  await page.getByLabel('Phân loại').selectOption('Rau củ');
  await page.getByLabel('Trạng thái danh mục').selectOption('true');
  await page.getByLabel('Hỗ trợ tính dinh dưỡng').selectOption('false');
  const filterResponse = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return url.pathname.endsWith('/admin/ingredients/search') && url.searchParams.get('ingredientGroup') === 'Rau củ';
  });
  await page.getByRole('button', { name: 'Tìm và lọc' }).click();
  await filterResponse;
  await expect(page.getByText('Nấm bào ngư', { exact: true })).toBeVisible();
  expect(new URL(lastSearchUrl).searchParams.get('ingredientGroup')).toBe('Rau củ');
  expect(new URL(lastSearchUrl).searchParams.get('active')).toBe('true');
  expect(new URL(lastSearchUrl).searchParams.get('nutritionSupported')).toBe('false');
  const nextPageResponse = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return url.pathname.endsWith('/admin/ingredients/search') && url.searchParams.get('page') === '1';
  });
  await page.getByRole('button', { name: 'Trang sau' }).click();
  await nextPageResponse;
  expect(new URL(lastSearchUrl).searchParams.get('page')).toBe('1');
  await expect(page.getByRole('button', { name: 'Xem công thức có dùng Nấm bào ngư' })).toBeVisible();
  await page.getByRole('button', { name: 'Xem chi tiết Nấm bào ngư' }).click();
  await expect(page.getByRole('heading', { name: 'Chi tiết · Nấm bào ngư' })).toBeVisible();
  await expect(page.getByText('Ngày đối chiếu:')).toBeVisible();
  await page.getByRole('button', { name: 'Tắt' }).click();
  await expect(page.getByRole('heading', { name: 'Chi tiết · Nấm bào ngư' })).toHaveCount(0);
  await page.getByRole('button', { name: 'Xem chi tiết Nấm bào ngư' }).click();
  await page.getByRole('button', { name: 'Đóng' }).click();
  await expect(page.getByRole('heading', { name: 'Chi tiết · Nấm bào ngư' })).toHaveCount(0);
  await page.getByRole('button', { name: 'Xem công thức có dùng Nấm bào ngư' }).click();
  await expect(page.getByText('Canh nấm rau củ', { exact: true })).toBeVisible();
  await page.getByRole('link', { name: 'Canh nấm rau củ' }).click();
  await expect(page).toHaveURL('/cong-thuc/id/30');
});

test('ingredient filters wait for submit and preserve the selected query while results load', async ({ page }) => {
  let releaseSearch!: () => void;
  const searchGate = new Promise<void>((resolve) => { releaseSearch = resolve; });
  const searchQueries: string[] = [];

  await page.route('**/api/v1/admin/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname.endsWith('/ingredients/options')) {
      return route.fulfill({ json: { success: true, data: { groups: ['Rau củ'], sources: ['Nguồn kiểm thử'] } } });
    }
    if (url.pathname.endsWith('/ingredients/search') && request.method() === 'GET') {
      const query = url.searchParams.get('query') ?? '';
      searchQueries.push(query);
      if (query) await searchGate;
      const result = query ? { ...banana, id: 9, name: 'Bắp cải' } : banana;
      return route.fulfill({ json: { success: true, data: {
        content: [result], page: 0, size: 24, totalElements: 1, totalPages: 1,
      } } });
    }
    if (url.pathname.endsWith('/ingredients') && request.method() === 'GET') {
      return route.fulfill({ json: { success: true, data: [banana] } });
    }
    return route.fulfill({ status: 404, json: { detail: 'Not found' } });
  });

  await page.goto('/quan-tri/danh-muc');
  const search = page.getByPlaceholder('Tìm tên nguyên liệu');
  await search.pressSequentially('bắp', { delay: 120 });
  await expect(search).toHaveValue('bắp');
  await expect.poll(() => searchQueries).not.toContain('bắp');
  await page.getByRole('button', { name: 'Tìm và lọc' }).click();
  await expect.poll(() => searchQueries).toContain('bắp');
  await expect(page.getByRole('status')).toContainText('Đang tải danh mục');

  releaseSearch();
  await expect(page.getByText('Bắp cải', { exact: true })).toBeVisible();
  await search.fill('bắp cải');
  await expect(search).toHaveValue('bắp cải');
  await page.getByRole('button', { name: 'Tìm và lọc' }).click();
  await expect.poll(() => searchQueries.at(-1)).toBe('bắp cải');
});

test('ingredient image and group resolver handles all vegetarian groups and fallbacks', async ({ page }) => {
  const diverseIngredients = [
    { ...banana, id: 10, name: 'Đậu hũ non', ingredientGroup: 'Đậu & Chế phẩm' },
    { ...banana, id: 11, name: 'Gạo tẻ', ingredientGroup: 'Ngũ cốc' },
    { ...banana, id: 12, name: 'Hạt điều rang', ingredientGroup: 'Các loại hạt' },
    { ...banana, id: 13, name: 'Tiêu đen', ingredientGroup: 'Gia vị chay' },
    { ...banana, id: 14, name: 'Rau muống', ingredientGroup: 'Rau củ' },
    { ...banana, id: 15, name: 'Nguyên liệu tùy chỉnh không hình', ingredientGroup: 'Khác' },
  ];
  await page.route('**/api/v1/admin/**', async (route) => {
    const url = new URL(route.request().url());
    if (url.pathname.endsWith('/ingredients/options')) {
      return route.fulfill({ json: { success: true, data: { groups: ['Rau củ', 'Đậu & Chế phẩm', 'Ngũ cốc', 'Các loại hạt', 'Trái cây', 'Gia vị chay', 'Khác'], sources: ['USDA FoodData Central'] } } });
    }
    if (url.pathname.endsWith('/ingredients/search')) {
      return route.fulfill({ json: ingredientPage(diverseIngredients) });
    }
    if (url.pathname.endsWith('/ingredients')) {
      return route.fulfill({ json: { success: true, data: diverseIngredients } });
    }
    return route.fulfill({ status: 404 });
  });

  await page.goto('/quan-tri/danh-muc');
  await expect(page.getByText('Đậu hũ non')).toBeVisible();
  await expect(page.getByText('Gạo tẻ')).toBeVisible();
  await expect(page.getByText('Hạt điều rang')).toBeVisible();
  await expect(page.getByText('Tiêu đen')).toBeVisible();
  await expect(page.getByText('Rau muống')).toBeVisible();
  await expect(page.getByText('Nguyên liệu tùy chỉnh không hình')).toBeVisible();
});

