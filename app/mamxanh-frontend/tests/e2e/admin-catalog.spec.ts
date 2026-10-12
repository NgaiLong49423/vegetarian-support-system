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
  await page.getByLabel('Tên chuẩn tiếng Việt').fill('Nấm đùi gà');
  await page.getByRole('combobox', { name: 'Nhóm nguyên liệu' }).last().selectOption('Đậu & Chế phẩm');
  await page.getByRole('combobox', { name: 'Tên nguồn dữ liệu' }).selectOption('USDA FoodData Central');
  await page.getByRole('button', { name: 'Thêm nguyên liệu', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Đã thêm nguyên liệu');

  const nameSearch = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return url.pathname.endsWith('/admin/ingredients/search') && url.searchParams.get('query') === 'Nấm';
  });
  await page.getByPlaceholder('Tìm tên nguyên liệu').fill('Nấm');
  await nameSearch;
  await expect(page.getByText('Nấm đùi gà', { exact: true })).toBeVisible();
  await expect(page.getByText('Chuối tây', { exact: true })).toHaveCount(0);

  await page.getByRole('button', { name: 'Sửa Nấm đùi gà' }).click();
  await page.getByLabel('Tên chuẩn tiếng Việt').fill('Nấm đùi gà tươi');
  await page.getByLabel('Tên nguồn dữ liệu').selectOption('NIH/Viện Dinh dưỡng');
  await page.getByLabel('Đường dẫn nguồn (không bắt buộc)').fill('https://example.org/ingredient');
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByText('Nấm đùi gà tươi', { exact: true })).toBeVisible();
  await expect(page.getByText('NIH/Viện Dinh dưỡng', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Ngừng sử dụng Nấm đùi gà tươi' }).click();
  await expect(page.getByRole('status')).toContainText('liên kết cũ được giữ nguyên');
  await expect(page.getByRole('button', { name: 'Bật lại Nấm đùi gà tươi' })).toBeVisible();
  await page.getByRole('button', { name: 'Bật lại Nấm đùi gà tươi' }).click();
  await expect(page.getByRole('status')).toContainText('Đã bật lại nguyên liệu');
  await expect(page.getByRole('button', { name: 'Ngừng sử dụng Nấm đùi gà tươi' })).toBeVisible();
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
  await page.getByLabel('Nhóm nguyên liệu').selectOption('Rau củ');
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
  await expect(page.getByText('1 công thức đang dùng')).toBeVisible();
  await page.getByRole('button', { name: 'Xem chi tiết Nấm bào ngư' }).click();
  await expect(page.getByRole('heading', { name: 'Chi tiết · Nấm bào ngư' })).toBeVisible();
  await expect(page.getByText('Ngày đối chiếu:')).toBeVisible();
  await page.getByRole('button', { name: 'Đóng' }).click();
  await page.getByRole('button', { name: 'Xem công thức dùng Nấm bào ngư' }).click();
  await expect(page.getByText('Canh nấm rau củ', { exact: true })).toBeVisible();
});
