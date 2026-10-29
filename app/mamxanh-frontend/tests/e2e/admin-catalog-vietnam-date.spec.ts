import { expect, test } from './baseFixtures';

// BUG-059 (Issue #159): the default reference date of a new ingredient is today in Vietnam time,
// whatever the time zone of the browser. 2026-10-25T17:30Z is 00:30 on 26/10 in Vietnam.
const NOW = new Date('2026-10-25T17:30:00Z');

for (const timezoneId of ['UTC', 'America/Los_Angeles', 'Asia/Ho_Chi_Minh']) {
  test.describe(`ingredient reference date in ${timezoneId}`, () => {
    test.use({ timezoneId });

    test('a new ingredient defaults to the Vietnam date and sends it', async ({ page }) => {
      await page.clock.setFixedTime(NOW);
      const created: Array<{ referenceDate: string }> = [];
      await page.route('**/api/v1/admin/**', async (route) => {
        const request = route.request();
        const url = new URL(request.url());
        if (url.pathname.endsWith('/ingredients/options')) {
          return route.fulfill({ json: { success: true, data: { groups: ['Trái cây'], sources: ['USDA FoodData Central'] } } });
        }
        if (url.pathname.endsWith('/ingredients/search')) {
          return route.fulfill({ json: { success: true, data: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 } } });
        }
        if (url.pathname.endsWith('/ingredients') && request.method() === 'POST') {
          const body = request.postDataJSON();
          created.push(body);
          return route.fulfill({ status: 201, json: { success: true, data: { ...body, id: 9, active: true, nutritionSupported: false, recipeUsageCount: 0 } } });
        }
        if (url.pathname.endsWith('/ingredients')) return route.fulfill({ json: { success: true, data: [] } });
        return route.fulfill({ status: 404, json: { detail: 'Not found' } });
      });

      await page.goto('/quan-tri/danh-muc');
      await page.getByRole('button', { name: 'Mở biểu mẫu thêm nguyên liệu' }).click();
      const referenceDate = page.getByLabel('Ngày tham khảo');
      await expect(referenceDate).toHaveValue('2026-10-26');
      await expect(referenceDate).toHaveAttribute('max', '2026-10-26');

      await page.getByLabel('Tên chuẩn tiếng Việt').fill('Thanh long ruột đỏ');
      await page.getByRole('combobox', { name: 'Nhóm nguyên liệu' }).last().selectOption('Trái cây');
      await page.getByRole('combobox', { name: 'Nguồn dữ liệu hoặc cách ước tính' }).fill('USDA FoodData Central');
      await page.getByRole('button', { name: 'Thêm nguyên liệu', exact: true }).click();
      await expect(page.getByRole('status').filter({ hasText: 'Đã thêm nguyên liệu' })).toBeVisible();
      expect(created.map((body) => body.referenceDate)).toEqual(['2026-10-26']);
    });
  });
}
