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
  await expect(page.getByRole('heading', { name: 'Danh mục nguyên liệu' })).toBeVisible();
  await expect(page.getByRole('alert')).toContainText('403 Forbidden');
  await expect(page.getByRole('button', { name: 'Thêm nguyên liệu' })).toHaveCount(0);
});

test('administrator can add, search, edit and deactivate a Vietnamese ingredient', async ({ page }) => {
  const data = [banana];
  await page.route('**/api/v1/admin/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
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
    if (url.pathname.endsWith('/units')) return route.fulfill({ json: { success: true, data: [] } });
    if (url.pathname.endsWith('/ingredient-unit-conversions')) return route.fulfill({ json: { success: true, data: [] } });
    return route.fulfill({ status: 404, json: { detail: 'Not found' } });
  });

  await page.goto('/quan-tri/danh-muc');
  await expect(page.getByText('Chuối tây', { exact: true })).toBeVisible();
  await page.getByLabel('Tên chuẩn tiếng Việt').fill('Nấm đùi gà');
  await page.getByLabel('Nhóm nguyên liệu', { exact: true }).fill('Nấm');
  await page.getByLabel('Tên nguồn dữ liệu').fill('Nguồn kiểm thử');
  await page.getByRole('button', { name: 'Thêm nguyên liệu' }).click();
  await expect(page.getByRole('status')).toContainText('Đã thêm nguyên liệu');

  await page.getByPlaceholder('Tìm tên nguyên liệu').fill('Nấm');
  await page.getByRole('button', { name: 'Tìm và lọc', exact: true }).click();
  await expect(page.getByText('Nấm đùi gà', { exact: true })).toBeVisible();
  await expect(page.getByText('Chuối tây', { exact: true })).toHaveCount(0);

  await page.getByRole('button', { name: 'Sửa Nấm đùi gà' }).click();
  await page.getByLabel('Tên chuẩn tiếng Việt').fill('Nấm đùi gà tươi');
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByText('Nấm đùi gà tươi', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Ngừng sử dụng Nấm đùi gà tươi' }).click();
  await expect(page.getByRole('status')).toContainText('liên kết cũ được giữ nguyên');
  await expect(page.getByRole('button', { name: 'Bật lại Nấm đùi gà tươi' })).toBeVisible();
});

test('administrator can manage units and conversion rules and sees duplicate-pair errors', async ({ page }) => {
  const unit = { id: 3, code: 'quả', name: 'Quả', dimension: 'COUNT' as const, baseFactor: 1, active: true };
  const massUnit = { id: 2, code: 'g', name: 'Gam', dimension: 'MASS' as const, baseFactor: 1, active: true };
  const units = [unit, massUnit];
  await page.route('**/api/v1/admin/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    if (url.pathname.endsWith('/ingredients/search') && method === 'GET') return route.fulfill({ json: ingredientPage([banana]) });
    if (url.pathname.endsWith('/ingredients') && method === 'GET') return route.fulfill({ json: { success: true, data: [banana] } });
    if (url.pathname.endsWith('/units') && method === 'GET') return route.fulfill({ json: { success: true, data: units } });
    if (url.pathname.endsWith('/units') && method === 'POST') {
      const created = { ...request.postDataJSON(), id: 4, active: true };
      units.push(created);
      return route.fulfill({ status: 201, json: { success: true, data: created } });
    }
    if (/\/units\/\d+$/.test(url.pathname) && method === 'PUT') {
      const id = Number(url.pathname.split('/').at(-1));
      const target = units.find((item) => item.id === id)!;
      Object.assign(target, request.postDataJSON());
      return route.fulfill({ json: { success: true, data: target } });
    }
    if (/\/units\/\d+\/status$/.test(url.pathname) && method === 'PATCH') {
      const id = Number(url.pathname.match(/\/units\/(\d+)\/status$/)?.[1]);
      const target = units.find((item) => item.id === id)!;
      target.active = request.postDataJSON().active;
      return route.fulfill({ json: { success: true, data: target } });
    }
    if (url.pathname.endsWith('/ingredient-unit-conversions') && method === 'GET') return route.fulfill({ json: { success: true, data: [] } });
    if (/\/unit-conversions\/\d+$/.test(url.pathname) && method === 'POST') {
      return route.fulfill({ status: 409, contentType: 'application/problem+json', json: { title: 'Conflict', status: 409, detail: 'Cặp nguyên liệu và đơn vị đã có tỷ lệ quy đổi.' } });
    }
    return route.fulfill({ status: 404, json: { detail: 'Not found' } });
  });

  await page.goto('/quan-tri/danh-muc');
  await page.getByRole('tab', { name: /Đơn vị/ }).click();
  await page.getByLabel('Ký hiệu').fill('ml');
  await page.getByLabel('Tên đơn vị').fill('Mi-li-lít');
  await page.getByLabel('Thứ nguyên').selectOption('VOLUME');
  await page.getByRole('button', { name: 'Thêm đơn vị' }).click();
  await expect(page.getByText('Mi-li-lít', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Sửa Mi-li-lít' }).click();
  await page.getByLabel('Tên đơn vị').fill('Mi-li-lít nước');
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByText('Mi-li-lít nước', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Ngừng sử dụng Mi-li-lít nước' }).click();
  await expect(page.getByRole('button', { name: 'Bật lại Mi-li-lít nước' })).toBeVisible();
  await expect(page.getByText('Gam', { exact: true })).toBeVisible();

  await page.getByRole('tab', { name: /Bảng quy đổi/ }).click();
  await expect(page.getByLabel('Đơn vị đo').locator('option[value="2"]')).toHaveCount(0);
  await page.getByLabel('Nguyên liệu').selectOption('7');
  await page.getByLabel('Đơn vị đo').selectOption('3');
  await page.getByLabel('Gam trên mỗi đơn vị').fill('120');
  await page.getByRole('button', { name: 'Thêm quy đổi' }).click();
  await expect(page.getByRole('alert')).toContainText('đã có tỷ lệ quy đổi');
  await expect(page.getByRole('button', { name: /Xóa/ })).toHaveCount(0);
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
});

test('administrator creates, edits and toggles an ingredient unit conversion', async ({ page }) => {
  const unit = { id: 3, code: 'quả', name: 'Quả', dimension: 'COUNT' as const, baseFactor: 1, active: true };
  const conversions: Array<{ ingredientId: number; unitId: number; gramsPerUnit: number; approximate: boolean; active: boolean }> = [];
  await page.route('**/api/v1/admin/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    if (url.pathname.endsWith('/ingredients/search') && method === 'GET') return route.fulfill({ json: ingredientPage([banana]) });
    if (url.pathname.endsWith('/ingredients') && method === 'GET') {
      return route.fulfill({ json: { success: true, data: [banana] } });
    }
    if (url.pathname.endsWith('/units') && method === 'GET') {
      return route.fulfill({ json: { success: true, data: [unit] } });
    }
    if (url.pathname.endsWith('/ingredient-unit-conversions') && method === 'GET') {
      return route.fulfill({ json: { success: true, data: conversions } });
    }
    if (/\/ingredients\/7\/unit-conversions\/3$/.test(url.pathname) && method === 'POST') {
      const body = request.postDataJSON();
      const created = { ingredientId: 7, unitId: 3, ...body, active: true };
      conversions.push(created);
      return route.fulfill({ status: 201, json: { success: true, data: created } });
    }
    if (/\/ingredients\/7\/unit-conversions\/3$/.test(url.pathname) && method === 'PUT') {
      Object.assign(conversions[0], request.postDataJSON());
      return route.fulfill({ json: { success: true, data: conversions[0] } });
    }
    if (/\/ingredients\/7\/unit-conversions\/3\/status$/.test(url.pathname) && method === 'PATCH') {
      conversions[0].active = request.postDataJSON().active;
      return route.fulfill({ json: { success: true, data: conversions[0] } });
    }
    return route.fulfill({ status: 404, json: { detail: 'Not found' } });
  });

  await page.goto('/quan-tri/danh-muc');
  await page.getByRole('tab', { name: /Bảng quy đổi/ }).click();
  await page.getByLabel('Nguyên liệu').selectOption('7');
  await page.getByLabel('Đơn vị đo').selectOption('3');
  await page.getByLabel('Gam trên mỗi đơn vị').fill('120');
  await expect(page.getByLabel('Đây là tỷ lệ ước lượng')).toBeChecked();
  await page.getByRole('button', { name: 'Thêm quy đổi' }).click();
  await expect(page.getByRole('status')).toContainText('Đã thêm tỷ lệ quy đổi');
  await expect(page.getByText('1 Quả = 120 g')).toBeVisible();
  await expect(page.getByText('≈ Xấp xỉ')).toBeVisible();

  await page.getByRole('button', { name: 'Sửa quy đổi Chuối tây' }).click();
  await expect(page.getByRole('heading', { name: 'Sửa tỷ lệ quy đổi' })).toBeVisible();
  await expect(page.getByLabel('Nguyên liệu')).toBeDisabled();
  await page.getByLabel('Gam trên mỗi đơn vị').fill('130');
  await page.getByLabel('Đây là tỷ lệ ước lượng').uncheck();
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByRole('status')).toContainText('Đã cập nhật tỷ lệ quy đổi');
  await expect(page.getByText('1 Quả = 130 g')).toBeVisible();
  await expect(page.getByText('✓ Chính xác')).toBeVisible();

  await page.getByRole('button', { name: 'Ngừng dùng', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Bật lại', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Bật lại', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Ngừng dùng', exact: true })).toBeVisible();
});

test('administrator saves nutrition values and backend validates enabling calculation support', async ({ page }) => {
  const data = [{ ...banana, nutrition: { ...banana.nutrition } }];
  await page.route('**/api/v1/admin/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    if (url.pathname.endsWith('/ingredients/search') && method === 'GET') return route.fulfill({ json: ingredientPage(data) });
    if (url.pathname.endsWith('/ingredients') && method === 'GET') {
      return route.fulfill({ json: { success: true, data } });
    }
    if (url.pathname.endsWith('/units')) return route.fulfill({ json: { success: true, data: [] } });
    if (url.pathname.endsWith('/ingredient-unit-conversions')) return route.fulfill({ json: { success: true, data: [] } });
    if (/\/ingredients\/7\/nutrition$/.test(url.pathname) && method === 'PUT') {
      data[0].nutrition = request.postDataJSON();
      return route.fulfill({ json: { success: true, data: data[0] } });
    }
    if (/\/ingredients\/7\/nutrition-support$/.test(url.pathname) && method === 'PATCH') {
      const { enabled } = request.postDataJSON();
      if (enabled && Object.values(data[0].nutrition).some((value) => value === null)) {
        return route.fulfill({ status: 409, contentType: 'application/problem+json', json: {
          title: 'Conflict', status: 409, code: 'NUTRITION_VALUES_INCOMPLETE', detail: 'Còn thiếu chỉ tiêu dinh dưỡng: proteinGramsPer100g.',
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
  await page.getByLabel('Năng lượng trên 100 g').fill('100');
  const saveResponse = page.waitForResponse((response) => response.request().method() === 'PUT' && response.url().endsWith('/admin/ingredients/7/nutrition'));
  await page.getByRole('button', { name: 'Lưu chỉ tiêu' }).click();
  expect((await saveResponse).ok()).toBeTruthy();
  expect(data[0].nutrition.energyKcalPer100g).toBe(100);
  expect(data[0].nutrition.proteinGramsPer100g).toBeNull();
  await expect(page.getByRole('status')).toContainText('Đã lưu chỉ tiêu dinh dưỡng');

  await page.getByRole('button', { name: 'Bật hỗ trợ tính' }).click();
  await expect(page.getByRole('alert')).toContainText('Còn thiếu chỉ tiêu dinh dưỡng');

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
  await expect(page.getByRole('status')).toContainText('Đã lưu chỉ tiêu dinh dưỡng');
  const enableResponse = page.waitForResponse((response) => response.request().method() === 'PATCH' && response.url().endsWith('/admin/ingredients/7/nutrition-support'));
  await page.getByRole('button', { name: 'Bật hỗ trợ tính' }).click();
  expect((await enableResponse).ok()).toBeTruthy();
  await expect(page.getByText('Đang hỗ trợ tính', { exact: true })).toBeVisible();
  await expect(page.getByText('Có dữ liệu tính toán', { exact: true })).toBeVisible();
});

test('administrator filters the catalog through the paginated endpoint and reads linked recipes', async ({ page }) => {
  const filtered = { ...banana, id: 9, name: 'Nấm bào ngư' };
  let lastSearchUrl = '';
  await page.route('**/api/v1/admin/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    if (url.pathname.endsWith('/ingredients/search') && request.method() === 'GET') {
      lastSearchUrl = url.toString();
      return route.fulfill({ json: {
        success: true,
        data: { content: [filtered], page: Number(url.searchParams.get('page')), size: 20, totalElements: 21, totalPages: 2 },
      } });
    }
    if (url.pathname.endsWith('/ingredients') && request.method() === 'GET') return route.fulfill({ json: { success: true, data: [banana, filtered] } });
    if (url.pathname.endsWith('/units')) return route.fulfill({ json: { success: true, data: [] } });
    if (url.pathname.endsWith('/ingredient-unit-conversions')) return route.fulfill({ json: { success: true, data: [] } });
    if (/\/ingredients\/9\/recipes$/.test(url.pathname)) return route.fulfill({ json: { success: true, data: [
      { recipeId: 30, title: 'Canh nấm rau củ', status: 'PUBLISHED', publishedAt: '2026-10-10T10:00:00' },
    ] } });
    return route.fulfill({ status: 404, json: { detail: 'Not found' } });
  });

  await page.goto('/quan-tri/danh-muc');
  await page.getByLabel('Nhóm nguyên liệu').fill('Nấm');
  await page.getByLabel('Trạng thái danh mục').selectOption('true');
  await page.getByLabel('Hỗ trợ tính dinh dưỡng').selectOption('false');
  const filterResponse = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return url.pathname.endsWith('/admin/ingredients/search') && url.searchParams.get('ingredientGroup') === 'Nấm';
  });
  await page.getByRole('button', { name: 'Tìm và lọc' }).click();
  await filterResponse;
  await expect(page.getByText('Nấm bào ngư', { exact: true })).toBeVisible();
  expect(new URL(lastSearchUrl).searchParams.get('ingredientGroup')).toBe('Nấm');
  expect(new URL(lastSearchUrl).searchParams.get('active')).toBe('true');
  expect(new URL(lastSearchUrl).searchParams.get('nutritionSupported')).toBe('false');
  const nextPageResponse = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return url.pathname.endsWith('/admin/ingredients/search') && url.searchParams.get('page') === '1';
  });
  await page.getByRole('button', { name: 'Trang sau' }).click();
  await nextPageResponse;
  expect(new URL(lastSearchUrl).searchParams.get('page')).toBe('1');
  await page.getByRole('button', { name: 'Xem công thức dùng Nấm bào ngư' }).click();
  await expect(page.getByText('Canh nấm rau củ', { exact: true })).toBeVisible();
});
