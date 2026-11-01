import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

test('home renders Backend featured recipes and routes both search actions to exploration', async ({ page }) => {
  await page.route('**/api/v1/recipes?**', (route) => route.fulfill({ json: {
    items: [{
      id: 804, authorId: 18, authorName: 'Chuyên gia demo', authorAvatarUrl: null,
      title: 'Canh rau củ từ Backend', description: 'Công thức dùng cho kiểm tra trang chủ.',
      instructions: 'Nấu rau củ đến khi chín mềm.', dishCategory: 'SOUP', vegetarianType: 'VEGAN',
      difficulty: 'EASY', servings: 3, prepTimeMin: 10, cookTimeMin: 20,
      youtubeUrl: null, status: 'PUBLISHED', media: [{
        url: 'https://example.test/first-photo.jpg', mimeType: 'image/jpeg', displayOrder: 0, cover: false,
      }], ingredients: [],
    }], page: 0, size: 4, totalElements: 1, totalPages: 1,
  } }));

  await page.goto('/trang-chu');
  await expect(page.getByRole('heading', { name: 'Canh rau củ từ Backend' })).toHaveCount(2);
  await expect(page.locator('img[alt="Canh rau củ từ Backend"]').first()).toHaveAttribute('src', 'https://example.test/first-photo.jpg');
  await page.getByRole('textbox', { name: 'Tìm công thức trang chủ' }).fill('đậu & nấm');
  await page.getByRole('button', { name: 'Tìm', exact: true }).click();
  await expect(page).toHaveURL(/\/kham-pha\?q=%C4%91%E1%BA%ADu%20%26%20n%E1%BA%A5m$/);

  await page.goto('/trang-chu');
  await page.getByRole('link', { name: 'Xem tất cả', exact: true }).last().click();
  await expect(page).toHaveURL(/\/kham-pha\?sort=MOST_LIKED$/);

  await page.goto('/trang-chu');
  await page.getByRole('link', { name: 'Khám phá công thức', exact: true }).click();
  await expect(page).toHaveURL(/\/kham-pha$/);
  await page.goto('/trang-chu');
  await page.getByRole('link', { name: 'Mở lịch ăn' }).click();
  await expect(page).toHaveURL(/\/ke-hoach$/);
});

test('explore paginates API results, handles image variants and clears its search query', async ({ page }) => {
  const requestedPages: number[] = [];
  await page.route('**/api/v1/recipes/discovery?**', (route) => {
    const url = new URL(route.request().url());
    const pageNumber = Number(url.searchParams.get('page'));
    requestedPages.push(pageNumber);
    const recipeId = pageNumber + 850;
    return route.fulfill({ json: {
      items: [{
        id: recipeId, authorId: 18, authorName: 'Chuyên gia demo', authorAvatarUrl: null,
        title: `Công thức trang ${pageNumber + 1}`, description: null,
        instructions: 'Nấu nguyên liệu đến khi chín.', dishCategory: 'SOUP', vegetarianType: 'VEGAN',
        difficulty: 'EASY', servings: 2, prepTimeMin: 10, cookTimeMin: 20,
        youtubeUrl: null, status: 'PUBLISHED',
        media: pageNumber === 0 ? [] : [{ url: 'https://example.test/cover.jpg', mimeType: 'image/jpeg', displayOrder: 0, cover: true }],
        ingredients: [],
      }], page: pageNumber, size: 12, hasNext: pageNumber === 0,
    } });
  });

  await page.goto('/kham-pha?q=tofu');
  await expect(page.getByRole('heading', { name: 'Công thức trang 1' })).toBeVisible();
  await expect(page.getByText('Chưa có ảnh công thức')).toBeVisible();
  await expect(page.getByText('Trang 1', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Trang sau' }).click();
  await expect(page.getByRole('heading', { name: 'Công thức trang 2' })).toBeVisible();
  await expect(page.locator('img[alt="Công thức trang 2"]')).toHaveAttribute('src', 'https://example.test/cover.jpg');
  await expect(page.getByText('Trang 2', { exact: true })).toBeVisible();

  await page.getByRole('textbox', { name: 'Từ khóa', exact: true }).fill('other');
  await expect(page).toHaveURL(/\/kham-pha\?q=other$/);
  await expect(page.getByRole('heading', { name: 'Công thức trang 1' })).toBeVisible();
  await page.getByRole('button', { name: 'Xóa từ khóa' }).click();
  await expect(page.getByRole('textbox', { name: 'Từ khóa', exact: true })).toHaveValue('');
  await expect(page).toHaveURL(/\/kham-pha$/);
  expect(requestedPages).toContain(1);
});

test('home explains when Backend has no published recipes', async ({ page }) => {
  await page.route('**/api/v1/recipes?**', (route) => route.fulfill({ json: {
    items: [], page: 0, size: 4, totalElements: 0, totalPages: 0,
  } }));

  await page.goto('/trang-chu');

  await expect(page.getByText('Chưa có công thức công khai trong bộ sưu tập này.')).toHaveCount(2);
});

test('Member profile ignores stale local prototype data and reads its private API list', async ({ page }) => {
  await seedDemoSession(page, 'CUSTOMER');
  await page.addInitScript(() => localStorage.setItem('mamxanh_saved_recipes', '{invalid-json'));
  await page.goto('/ho-so');
  await expect(page.getByRole('heading', { name: 'Chưa có công thức đã lưu' })).toBeVisible();
  await expect(page.getByRole('article')).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Trang cá nhân', exact: true })).toHaveAttribute('href', '/thanh-vien/901');
  await page.getByRole('button', { name: 'Sức khỏe & sở thích', exact: true }).click();
  await expect(page.getByRole('main').getByRole('link', { name: 'Mở sở thích ăn uống' })).toHaveAttribute('href', '/ho-so/so-thich-an-uong');
});

test('Guest profile links to sign-in without reading member data', async ({ page }) => {
  let reads = 0;
  page.on('request', (request) => { if (new URL(request.url()).pathname === '/api/v1/saved-recipes') reads++; });
  await page.goto('/ho-so');
  await expect(page.getByRole('heading', { name: 'Góc bếp riêng của bạn' })).toBeVisible();
  await page.getByRole('main').getByRole('link', { name: 'Đăng nhập', exact: true }).click();
  await expect(page).toHaveURL(/\/dang-nhap$/);
  expect(reads).toBe(0);
});
test('guest admin routes explain that an administrator session is required', async ({ page }) => {
  await page.addInitScript(() => sessionStorage.removeItem('mamxanh.auth'));
  await page.goto('/admin/xet-duyet-chuyen-gia');

  await expect(page.getByRole('heading', { name: 'Đăng nhập để tiếp tục' })).toBeVisible();
  await expect(page.getByRole('main').getByRole('link', { name: 'Đăng nhập' })).toHaveAttribute('href', '/dang-nhap');
  await expect(page.getByRole('navigation', { name: 'Điều hướng quản trị' })).toHaveCount(0);
});

test('guest dietary preferences explain that sign-in is required before loading private data', async ({ page }) => {
  let requestCount = 0;
  await page.route('**/api/v1/nutrition/dietary-preferences', (route) => {
    requestCount += 1;
    return route.fulfill({ json: {} });
  });

  await page.goto('/ho-so/so-thich-an-uong');

  await expect(page.getByRole('alert')).toContainText('Đăng nhập tài khoản thật để xem và cập nhật sở thích ăn uống.');
  await expect.poll(() => requestCount).toBe(0);
});

test('administrator can view admin dashboard and pending modules', async ({ page }) => {
  await seedDemoSession(page, 'ADMIN');

  await page.route('**/api/v1/admin/expert-applications**', (route) => route.fulfill({
    json: { content: [], totalElements: 0, totalPages: 0, page: 0, size: 20 },
  }));
  await page.route('**/api/v1/admin/reports**', (route) => route.fulfill({
    json: route.request().url().includes('/status-counts')
      ? { open: 0, inReview: 0, resolved: 0 }
      : { content: [], totalElements: 0, totalPages: 0, page: 0, size: 20 },
  }));
  await page.route('**/api/v1/admin/ingredients/options', (route) => route.fulfill({
    json: { success: true, data: { groups: ['Rau củ'], sources: ['USDA'] } },
  }));
  await page.route('**/api/v1/admin/ingredients/search**', (route) => route.fulfill({
    json: { success: true, data: { content: [], totalElements: 0, totalPages: 0, page: 0, size: 5 } },
  }));
  await page.route('**/api/v1/admin/ingredients**', (route) => route.fulfill({
    json: { success: true, data: { content: [], totalElements: 0, totalPages: 0, page: 0, size: 5 } },
  }));

  await page.goto('/admin');
  await expect(page.getByRole('heading', { name: 'Bảng điều phối hệ thống' })).toBeVisible();
  await expect(page.getByRole('navigation', { name: 'Điều hướng quản trị' })).toBeVisible();
  await page.getByRole('button', { name: 'Tải lại số liệu' }).click();
  await page.getByRole('button', { name: 'Chuyển sang chế độ' }).click();

  await page.setViewportSize({ width: 375, height: 667 });
  await page.getByRole('button', { name: 'Mở menu quản trị' }).click();
  await expect(page.getByRole('navigation', { name: 'Điều hướng quản trị' })).toBeVisible();
  await page.getByRole('button', { name: 'Đóng menu' }).click();

  await page.goto('/admin/bao-cao');
  await expect(page.getByRole('heading', { name: 'Hậu kiểm nội dung', level: 1 })).toBeVisible();

  await page.goto('/admin/nguoi-dung');
  await expect(page.getByRole('heading', { name: 'Quản lý tài khoản & phân quyền', exact: true })).toBeVisible();

  await page.goto('/admin/giao-dich');
  await expect(page.getByRole('heading', { name: 'Giám sát giao dịch gói AI', exact: true })).toBeVisible();
});

test('non-admin member accessing admin route is denied access', async ({ page }) => {
  await page.addInitScript(() => {
    sessionStorage.setItem('mamxanh.auth', JSON.stringify({
      accessToken: 'test-user-token',
      expiresAt: Date.now() + 3600000,
      account: {
        id: 2,
        displayName: 'Khách hàng',
        email: 'user@mamxanh.local',
        role: 'CUSTOMER',
      },
    }));
  });

  await page.goto('/admin');
  await expect(page.getByRole('alert')).toContainText('Không có quyền truy cập');
  await expect(page.getByRole('heading', { name: 'Không có quyền truy cập' })).toBeVisible();
});
