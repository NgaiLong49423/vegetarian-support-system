import { expect, test } from './baseFixtures';
import type { Page } from '@playwright/test';

const existingRecipe = {
  id: 47,
  authorId: 12,
  authorName: 'Bếp Trưởng Diệu Tâm',
  authorAvatarUrl: null,
  title: 'Nấm kho tiêu',
  description: 'Món chay gia đình',
  instructions: 'Kho nấm với nước dừa trong 15 phút.',
  dishCategory: 'BRAISED',
  vegetarianType: 'VEGAN',
  difficulty: 'EASY',
  servings: 2,
  prepTimeMin: 5,
  cookTimeMin: 15,
  youtubeUrl: null,
  status: 'PUBLISHED',
  media: [{ url: 'https://images.example.test/recipe.jpg', mimeType: 'image/jpeg', displayOrder: 1, cover: true }],
  ingredients: [{ ingredientId: 21, name: 'Nấm đùi gà', customName: null, unitId: 1, unitCode: 'g', unitName: 'gram', quantity: 200 }],
};

const references = {
  ingredients: [{ ingredientId: 21, name: 'Nấm đùi gà', unitId: 1, unitCode: 'g', unitName: 'gram' }],
  customIngredientUnits: [{ unitId: 1, code: 'g', name: 'gram' }],
};

test('public recipe page has no management action; expert profile preview owns that entry', async ({ page }) => {
  await page.route('**/api/v1/recipes/47', (route) => route.fulfill({ json: existingRecipe }));
  await page.goto('/cong-thuc/id/47');

  await expect(page.getByRole('heading', { name: 'Nấm kho tiêu' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Quản lý công thức' })).toHaveCount(0);

  await page.goto('/ho-so/chuyen-gia-demo');
  await expect(page.getByRole('heading', { name: 'Chuyên gia Demo Issue #47' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Công thức đã đăng' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Sửa' })).toHaveAttribute('href', '/cong-thuc/47/chinh-sua');
  await expect(page.getByRole('button', { name: 'Xóa', exact: true })).toBeVisible();
  await expect(page.getByText(/Hồ sơ này không xác thực tài khoản/)).toBeVisible();
});

async function mockEditorApi(page: Page) {
  await page.route('**/api/v1/recipes/47/manage', (route) => route.fulfill({ json: existingRecipe }));
  await page.route('**/api/v1/recipes/reference-data**', (route) => route.fulfill({ json: references }));
}

test('expert edits recipe, saves, and sees success on the expert profile', async ({ page }) => {
  await mockEditorApi(page);
  let savedPayload: Record<string, unknown> | undefined;
  await page.route('**/api/v1/recipes/47', async (route) => {
    if (route.request().method() === 'PUT') {
      savedPayload = route.request().postDataJSON() as Record<string, unknown>;
      await route.fulfill({ json: { ...existingRecipe, title: 'Nấm kho tiêu xanh' } });
    } else {
      await route.fulfill({ json: { ...existingRecipe, title: 'Nấm kho tiêu xanh' } });
    }
  });

  await page.goto('/cong-thuc/47/chinh-sua');
  await page.getByLabel('Tên món *').fill('Nấm kho tiêu xanh');
  await page.getByLabel('Hướng dẫn nấu *').fill('Kho nấm với nước dừa và tiêu xanh trong 20 phút.');
  await page.getByRole('button', { name: 'Lưu thay đổi' }).first().click();

  await expect(page).toHaveURL(/\/ho-so\/chuyen-gia-demo$/);
  await expect(page.getByRole('status')).toContainText('Lưu thay đổi thành công');
  await expect(page.getByRole('heading', { name: 'Nấm kho tiêu xanh' })).toBeVisible();
  expect(savedPayload?.title).toBe('Nấm kho tiêu xanh');
  expect(savedPayload?.instructions).toBe('Kho nấm với nước dừa và tiêu xanh trong 20 phút.');
});

test('hidden recipe blocks the editor and explains Admin recovery', async ({ page }) => {
  await page.route('**/api/v1/recipes/reference-data**', (route) => route.fulfill({ json: references }));
  await page.route('**/api/v1/recipes/47/manage', (route) => route.fulfill({
    status: 403,
    contentType: 'application/problem+json',
    body: JSON.stringify({ status: 403, code: 'RECIPE_HIDDEN', detail: 'Công thức đang bị quản trị viên ẩn.' }),
  }));

  await page.goto('/cong-thuc/47/chinh-sua');

  await expect(page.getByText(/Bạn không thể sửa cho đến khi quản trị viên phục hồi/)).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Không thể mở biểu mẫu sửa' })).toBeVisible();
  await expect(page.getByLabel('Tên món *')).toHaveCount(0);
});

test('delete is separate on profile, asks confirmation, then removes recipe from that list', async ({ page }) => {
  let deleteRequested = false;
  await page.route('**/api/v1/recipes/47', async (route) => {
    if (route.request().method() === 'DELETE') {
      deleteRequested = true;
      await route.fulfill({ status: 204, body: '' });
    } else {
      await route.fulfill({ status: 200, json: { items: [] } });
    }
  });

  await page.goto('/ho-so/chuyen-gia-demo');
  await page.getByRole('button', { name: 'Xóa', exact: true }).click();
  await expect(page.getByRole('dialog')).toContainText('Bạn chắc chắn muốn xóa bài này?');
  await page.getByRole('button', { name: 'Xác nhận xóa' }).click();

  await expect(page.getByRole('heading', { name: 'Nấm kho tiêu' })).toHaveCount(0);
  await expect(page.getByText('Bạn chưa có công thức nào đã đăng.')).toBeVisible();
  await expect(page.getByRole('status')).toContainText('Đã xóa công thức');
  expect(deleteRequested).toBe(true);
});
