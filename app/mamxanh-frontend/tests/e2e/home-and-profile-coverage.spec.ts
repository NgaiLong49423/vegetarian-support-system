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

  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Canh rau củ từ Backend' })).toBeVisible();
  await expect(page.locator('img[alt="Canh rau củ từ Backend"]')).toHaveAttribute('src', 'https://example.test/first-photo.jpg');
  await page.getByPlaceholder('Nhập nguyên liệu sẵn có hoặc món chay bạn đang tìm kiếm...').fill('đậu & nấm');
  await page.getByRole('button', { name: 'Tìm công thức' }).click();
  await expect(page).toHaveURL(/\/kham-pha\?q=%C4%91%E1%BA%ADu%20%26%20n%E1%BA%A5m$/);

  await page.goto('/');
  await page.getByRole('button', { name: 'Phở chay' }).click();
  await expect(page).toHaveURL(/\/kham-pha\?q=Ph%E1%BB%9F%20chay$/);

  await page.goto('/');
  await page.getByRole('button', { name: 'Khám phá ngay' }).click();
  await expect(page).toHaveURL(/\/kham-pha$/);
  await page.goto('/');
  await page.getByRole('button', { name: 'Gợi ý thực đơn của tôi' }).click();
  await expect(page).toHaveURL(/\/ke-hoach$/);
});

test('explore paginates API results, handles image variants and clears its search query', async ({ page }) => {
  const requestedPages: number[] = [];
  await page.route('**/api/v1/recipes?**', (route) => {
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
      }], page: pageNumber, size: 12, totalElements: 13, totalPages: 2,
    } });
  });

  await page.goto('/kham-pha?q=tofu');
  await expect(page.getByRole('heading', { name: 'Công thức trang 1' })).toBeVisible();
  await expect(page.getByText('Ảnh công thức')).toBeVisible();
  await expect(page.getByText('Trang 1/2')).toBeVisible();
  await page.getByRole('button', { name: 'Trang sau' }).click();
  await expect(page.getByRole('heading', { name: 'Công thức trang 2' })).toBeVisible();
  await expect(page.locator('img[alt="Công thức trang 2"]')).toHaveAttribute('src', 'https://example.test/cover.jpg');
  await expect(page.getByText('Trang 2/2')).toBeVisible();

  await page.getByPlaceholder('Tìm món chay hoặc nguyên liệu...').fill('other');
  await expect(page).toHaveURL(/\/kham-pha\?q=other$/);
  await expect(page.getByRole('heading', { name: 'Công thức trang 1' })).toBeVisible();
  await page.getByRole('button', { name: 'Xóa từ khóa' }).click();
  await expect(page.getByPlaceholder('Tìm món chay hoặc nguyên liệu...')).toHaveValue('');
  await expect(page).toHaveURL(/\/kham-pha$/);
  expect(requestedPages).toContain(1);
});

test('home explains when Backend has no published recipes', async ({ page }) => {
  await page.route('**/api/v1/recipes?**', (route) => route.fulfill({ json: {
    items: [], page: 0, size: 4, totalElements: 0, totalPages: 0,
  } }));

  await page.goto('/');

  await expect(page.getByText('Backend chưa có công thức công khai hoặc chưa thể kết nối.')).toBeVisible();
});

test('saved-recipe demo handles corrupt storage, toggle events and meal-plan button state', async ({ page }) => {
  await seedDemoSession(page, 'CUSTOMER');
  await page.addInitScript(() => localStorage.setItem('mamxanh_saved_recipes', '{invalid-json'));
  await page.goto('/ho-so');
  await expect(page.getByText('Chức năng lưu đang ở chế độ demo UI')).toBeVisible();

  await page.getByRole('button', { name: 'Món yêu thích' }).click();
  const recipeCards = page.locator('a[href^="/cong-thuc/"]').filter({ has: page.getByRole('button', { name: 'Lưu công thức' }) });
  const firstCard = recipeCards.first();
  await expect(firstCard).toBeVisible();
  await firstCard.getByRole('button', { name: 'Lưu công thức' }).click();
  await expect(firstCard.getByRole('button', { name: 'Bỏ lưu công thức' })).toBeVisible();
  await firstCard.getByRole('button', { name: 'Thêm vào lịch ăn' }).click();
  await expect(firstCard.getByRole('button', { name: 'Đã thêm vào lịch ăn' })).toBeVisible();

  await page.getByRole('button', { name: 'Công thức đã lưu' }).click();
  const savedCard = page.locator('a[href^="/cong-thuc/"]').filter({ has: page.getByRole('button', { name: 'Bỏ lưu công thức' }) }).first();
  await expect(savedCard).toBeVisible();
  await savedCard.getByRole('button', { name: 'Bỏ lưu công thức' }).click();
  await expect(page.getByText('Chức năng lưu đang ở chế độ demo UI')).toBeVisible();
});

test('guest recipe-card actions explain the login requirement and link to sign-in', async ({ page }) => {
  await page.goto('/ho-so');
  await page.getByRole('button', { name: 'Món yêu thích' }).click();
  const firstCard = page.locator('a[href^="/cong-thuc/"]').first();

  await firstCard.getByRole('button', { name: 'Lưu công thức' }).click();
  await expect(firstCard.getByRole('alert')).toContainText('Vui lòng đăng nhập để lưu công thức.');
  await firstCard.getByRole('alert').click();
  await expect(page).toHaveURL(/\/dang-nhap$/);
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
