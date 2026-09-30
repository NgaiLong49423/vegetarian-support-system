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
};

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

  await page.getByPlaceholder('Tìm tên hoặc nhóm nguyên liệu').fill('Nấm');
  await page.getByRole('button', { name: 'Tìm', exact: true }).click();
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
  const units = [unit];
  await page.route('**/api/v1/admin/**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const method = request.method();
    if (url.pathname.endsWith('/ingredients') && method === 'GET') return route.fulfill({ json: { success: true, data: [banana] } });
    if (url.pathname.endsWith('/units') && method === 'GET') return route.fulfill({ json: { success: true, data: units } });
    if (url.pathname.endsWith('/units') && method === 'POST') {
      const created = { ...request.postDataJSON(), id: 4, active: true };
      units.push(created);
      return route.fulfill({ status: 201, json: { success: true, data: created } });
    }
    if (url.pathname.endsWith('/units/4') && method === 'PUT') {
      Object.assign(units[1], request.postDataJSON());
      return route.fulfill({ json: { success: true, data: units[1] } });
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

  await page.getByRole('tab', { name: /Bảng quy đổi/ }).click();
  await page.getByLabel('Nguyên liệu').selectOption('7');
  await page.getByLabel('Đơn vị đo').selectOption('3');
  await page.getByLabel('Gam trên mỗi đơn vị').fill('120');
  await page.getByRole('button', { name: 'Thêm quy đổi' }).click();
  await expect(page.getByRole('alert')).toContainText('đã có tỷ lệ quy đổi');
  await expect(page.getByRole('button', { name: /Xóa/ })).toHaveCount(0);
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
});
