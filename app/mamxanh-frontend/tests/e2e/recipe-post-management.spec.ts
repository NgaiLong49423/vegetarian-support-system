import { expect, test } from './baseFixtures';
import type { Page } from '@playwright/test';

const existingRecipe = {
  id: 47,
  authorId: 12,
  authorName: 'Chuyên gia Issue 47',
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

const hiddenRecipe = { ...existingRecipe, id: 48, title: 'Canh đang bị ẩn', status: 'HIDDEN' };
const references = {
  ingredients: [
    { ingredientId: 21, name: 'Nấm đùi gà', unitId: 1, unitCode: 'g', unitName: 'gram' },
    { ingredientId: 22, name: 'Đậu hũ', unitId: 1, unitCode: 'g', unitName: 'gram' },
  ],
};

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    if (sessionStorage.getItem('mamxanh.auth')) return;
    sessionStorage.setItem('mamxanh.auth', JSON.stringify({
      accessToken: 'playwright-only-token',
      expiresAt: Date.now() + 60 * 60 * 1000,
      account: {
        id: 12,
        displayName: 'Chuyên gia Issue 47',
        email: 'issue47@example.test',
        role: 'EXPERT',
        accountStatus: 'ACTIVE',
        emailVerified: true,
      },
    }));
  });
});

async function mockProfileApi(page: Page, recipes = [existingRecipe]) {
  await page.route('**/api/v1/recipes/mine**', (route) => route.fulfill({
    json: { items: recipes, page: 0, size: 50, totalElements: recipes.length, totalPages: recipes.length ? 1 : 0 },
  }));
}

async function mockEditorApi(page: Page, recipe = existingRecipe) {
  await page.route(`**/api/v1/recipes/${recipe.id}/manage`, (route) => route.fulfill({ json: recipe }));
  await page.route('**/api/v1/recipes/reference-data**', (route) => route.fulfill({ json: references }));
}

async function assumeRole(page: Page, role: 'CUSTOMER' | 'EXPERT' | 'ADMIN') {
  await page.goto('/');
  await page.evaluate((nextRole) => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
    accessToken: 'playwright-only-token',
    expiresAt: Date.now() + 60 * 60 * 1000,
    account: {
      id: 12,
      displayName: 'Chuyên gia Issue 47',
      email: 'issue47@example.test',
      role: nextRole,
      accountStatus: 'ACTIVE',
      emailVerified: true,
    },
  })), role);
}

test('public recipe page has no management action; expert manages recipes from personal profile', async ({ page }) => {
  await mockProfileApi(page);
  await page.route('**/api/v1/recipes/47', (route) => route.fulfill({ json: existingRecipe }));
  await page.goto('/cong-thuc/id/47');

  await expect(page.getByRole('heading', { name: 'Nấm kho tiêu' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Quản lý công thức' })).toHaveCount(0);

  await page.goto('/ho-so/chuyen-gia-demo');
  await expect(page.getByRole('heading', { name: 'Chuyên gia Issue 47' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Công thức của tôi' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Sửa' })).toHaveAttribute('href', '/cong-thuc/47/chinh-sua');
  await expect(page.getByRole('button', { name: 'Xóa', exact: true })).toBeVisible();
});

test('public detail and edit pages reject invalid recipe IDs without calling the API', async ({ page }) => {
  let apiRequested = false;
  await page.route('**/api/v1/recipes/**', (route) => {
    apiRequested = true;
    return route.fulfill({ status: 500, body: '' });
  });

  await page.goto('/cong-thuc/id/not-a-number');
  await expect(page.getByRole('heading', { name: 'Không thể mở công thức' })).toBeVisible();
  await expect(page.getByText('ID công thức không hợp lệ.')).toBeVisible();

  await page.goto('/cong-thuc/id/0');
  await expect(page.getByText('ID công thức không hợp lệ.')).toBeVisible();

  await page.goto('/cong-thuc/0/chinh-sua');
  await expect(page.getByText('Đường dẫn cần có ID công thức hợp lệ. Ví dụ: /cong-thuc/123/chinh-sua.')).toBeVisible();
  expect(apiRequested).toBe(false);
});

test('expert edits recipe, saves, and sees success on the expert profile', async ({ page }) => {
  await mockEditorApi(page);
  let savedPayload: Record<string, unknown> | undefined;
  let profileRecipe = existingRecipe;
  await page.route('**/api/v1/recipes/mine**', (route) => route.fulfill({
    json: { items: [profileRecipe], page: 0, size: 50, totalElements: 1, totalPages: 1 },
  }));
  await page.route('**/api/v1/recipes/47', async (route) => {
    if (route.request().method() === 'PUT') {
      savedPayload = route.request().postDataJSON() as Record<string, unknown>;
      profileRecipe = { ...existingRecipe, title: 'Nấm kho tiêu xanh' };
      await route.fulfill({ json: profileRecipe });
    } else {
      await route.fulfill({ json: profileRecipe });
    }
  });

  await page.goto('/cong-thuc/47/chinh-sua');
  await page.getByLabel('Tên món *').fill('Nấm kho tiêu xanh');
  await page.getByLabel('Hướng dẫn nấu *').fill('Kho nấm với nước dừa và tiêu xanh trong 20 phút.');
  await page.getByRole('button', { name: '+ Thêm dòng nguyên liệu' }).click();
  await page.getByRole('combobox', { name: 'Nguyên liệu' }).nth(1).selectOption('22');
  await page.getByLabel('Số lượng').nth(1).fill('150');
  await page.getByRole('button', { name: 'Xóa nguyên liệu' }).first().click();
  await page.getByRole('button', { name: 'Lưu thay đổi' }).first().click();

  await expect(page).toHaveURL(/\/ho-so\/chuyen-gia-demo$/);
  await expect(page.getByRole('status')).toContainText('Lưu thay đổi thành công');
  await expect(page.getByRole('heading', { name: 'Nấm kho tiêu xanh' })).toBeVisible();
  expect(savedPayload?.title).toBe('Nấm kho tiêu xanh');
  expect(savedPayload?.instructions).toBe('Kho nấm với nước dừa và tiêu xanh trong 20 phút.');
  expect(savedPayload?.ingredients).toEqual([{ ingredientId: 22, unitId: 1, quantity: 150 }]);
});

test('hidden recipe stays visible to its expert but all edit paths stay disabled', async ({ page }) => {
  await mockProfileApi(page, [hiddenRecipe]);
  await page.goto('/ho-so/chuyen-gia-demo');

  await expect(page.getByText('Đã bị Admin ẩn')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Sửa Canh đang bị ẩn' })).toBeDisabled();
  await expect(page.getByText(/không thể sửa; hãy liên hệ Admin để được phục hồi/)).toBeVisible();

  await mockEditorApi(page, hiddenRecipe);
  await page.goto('/cong-thuc/48/chinh-sua');
  await expect(page.getByText(/Bạn có thể xem bài nhưng không thể sửa/)).toBeVisible();
  await expect(page.getByLabel('Tên món *')).toHaveCount(0);
});

test('delete is separate on profile, asks confirmation, then removes recipe from that list', async ({ page }) => {
  let deleteRequested = false;
  let deleteAttempts = 0;
  await mockProfileApi(page);
  await page.route('**/api/v1/recipes/47', async (route) => {
    if (route.request().method() === 'DELETE') {
      deleteRequested = true;
      deleteAttempts += 1;
      await route.fulfill(deleteAttempts === 1
        ? { status: 503, contentType: 'application/problem+json', body: JSON.stringify({ status: 503, code: 'SERVICE_UNAVAILABLE', detail: 'Tạm thời chưa xóa được.' }) }
        : { status: 204, body: '' });
    } else {
      await route.fulfill({ json: existingRecipe });
    }
  });

  await page.goto('/ho-so/chuyen-gia-demo');
  await page.getByRole('button', { name: 'Xóa', exact: true }).click();
  await expect(page.getByRole('dialog')).toContainText('Bạn chắc chắn muốn xóa bài này?');
  await page.getByRole('button', { name: 'Hủy' }).click();
  await expect(page.getByRole('dialog')).toHaveCount(0);

  await page.getByRole('button', { name: 'Xóa', exact: true }).click();
  await page.getByRole('button', { name: 'Xác nhận xóa' }).click();
  await expect(page.getByRole('alert')).toContainText('Tạm thời chưa xóa được.');
  await expect(page.getByRole('heading', { name: 'Nấm kho tiêu' })).toBeVisible();

  await page.getByRole('button', { name: 'Xác nhận xóa' }).click();

  await expect(page.getByRole('heading', { name: 'Nấm kho tiêu' })).toHaveCount(0);
  await expect(page.getByText('Bạn chưa có công thức nào.')).toBeVisible();
  await expect(page.getByRole('status')).toContainText('Đã xóa công thức');
  expect(deleteRequested).toBe(true);
  expect(deleteAttempts).toBe(2);
});

test('expert profile retry recovers after a temporary load error', async ({ page }) => {
  let attempts = 0;
  await page.route('**/api/v1/recipes/mine**', async (route) => {
    attempts += 1;
    if (attempts === 1) {
      await route.fulfill({ status: 503, contentType: 'application/problem+json', body: JSON.stringify({ status: 503, code: 'SERVICE_UNAVAILABLE', detail: 'Máy chủ tạm thời bận.' }) });
    } else {
      await route.fulfill({ json: { items: [existingRecipe], page: 0, size: 50, totalElements: 1, totalPages: 1 } });
    }
  });

  await page.goto('/ho-so/chuyen-gia-demo');
  await expect(page.getByRole('alert')).toContainText('Máy chủ tạm thời bận.');
  await page.getByRole('button', { name: 'Thử tải lại' }).click();
  await expect(page.getByRole('heading', { name: 'Nấm kho tiêu' })).toBeVisible();
  expect(attempts).toBe(2);
});

test('customers cannot open the Expert recipe management profile', async ({ page }) => {
  let managementRequest = false;
  await page.route('**/api/v1/recipes/mine**', (route) => {
    managementRequest = true;
    return route.fulfill({ json: { items: [], page: 0, size: 50, totalElements: 0, totalPages: 0 } });
  });
  await assumeRole(page, 'CUSTOMER');
  await page.goto('/ho-so/chuyen-gia-demo');

  await expect(page.getByRole('heading', { name: 'Không có quyền quản lý bài công thức' })).toBeVisible();
  expect(managementRequest).toBe(false);
});

test('server validation errors stay visible and preserve edited form values', async ({ page }) => {
  await mockEditorApi(page);
  await page.route('**/api/v1/recipes/47', (route) => route.request().method() === 'PUT'
    ? route.fulfill({
      status: 400,
      contentType: 'application/problem+json',
      body: JSON.stringify({ status: 400, code: 'RECIPE_DATA_INVALID', detail: 'Nguyên liệu chưa hợp lệ.', errors: [{ field: 'ingredients[0].quantity', message: 'Số lượng phải lớn hơn 0.' }] }),
    })
    : route.fulfill({ json: existingRecipe }));

  await page.goto('/cong-thuc/47/chinh-sua');
  await page.getByLabel('Tên món *').fill('Tên vẫn được giữ sau lỗi');
  await page.getByRole('button', { name: 'Lưu thay đổi' }).first().click();

  await expect(page.getByRole('alert').filter({ hasText: 'Số lượng phải lớn hơn 0.' })).toBeVisible();
  await expect(page.getByLabel('Tên món *')).toHaveValue('Tên vẫn được giữ sau lỗi');
});
