import { expect, test } from './baseFixtures';

const publicRecipe = (id: number, title: string) => ({
  id,
  authorId: 22,
  authorName: 'Bếp Chay Lan',
  authorAvatarUrl: null,
  title,
  description: 'Công thức công khai từ API.',
  instructions: 'Nấu các nguyên liệu đến khi chín mềm.',
  dishCategory: 'SOUP',
  vegetarianType: 'VEGAN',
  difficulty: 'EASY',
  servings: 2,
  prepTimeMin: 10,
  cookTimeMin: 15,
  youtubeUrl: null,
  status: 'PUBLISHED',
  media: [{ url: `https://example.test/${id}.jpg`, mimeType: 'image/jpeg', displayOrder: 0, cover: true }],
  ingredients: [],
});

const detail = {
  recipeId: 901,
  title: 'Canh chua chay nấm đậu bắp thanh nhiệt',
  description: 'Món canh chua thanh mát.',
  instructions: 'Nấu nước dùng, thêm nấm và đậu bắp.',
  dishCategory: 'SOUP',
  dishCategoryLabel: 'Món canh',
  vegetarianType: 'VEGAN',
  vegetarianTypeLabel: 'Thuần chay',
  difficulty: 'EASY',
  difficultyLabel: 'Dễ',
  servings: 2,
  prepTimeMinutes: 10,
  cookTimeMinutes: 15,
  youtubeUrl: null,
  publishedAt: '2026-10-01T08:00:00',
  nutritionComplete: true,
  ingredientsWithoutNutrition: [],
  ingredients: [{ ingredientId: 1, name: 'Nấm rơm tươi', quantity: 100, unitId: 1, unitCode: 'g', unitName: 'gram' }],
  media: [{ blobUrl: 'https://example.test/901.jpg', mimeType: 'image/jpeg', displayOrder: 0, cover: true }],
};

test.describe('Issue #3 [FR-01] — Guest xem và tìm kiếm nội dung công khai', () => {
  test('AC-01.1: Guest duyệt danh sách công khai từ API và phân trang', async ({ page }) => {
    const requestedPages: number[] = [];
    await page.route('**/api/v1/recipes?**', (route) => {
      const request = route.request();
      expect(request.headers().authorization).toBeUndefined();
      const pageNumber = Number(new URL(request.url()).searchParams.get('page'));
      requestedPages.push(pageNumber);
      const items = pageNumber === 0 ? [publicRecipe(901, 'Đậu hũ non sốt nấm đông cô tiêu xanh')]
        : [publicRecipe(902, 'Canh chua chay nấm đậu bắp thanh nhiệt')];
      return route.fulfill({ json: { items, page: pageNumber, size: 12, totalElements: 2, totalPages: 2 } });
    });

    await page.goto('/kham-pha');
    await expect(page.getByRole('heading', { name: 'Khám phá công thức chay' })).toBeVisible();
    await expect(page.getByText('Đậu hũ non sốt nấm đông cô tiêu xanh')).toBeVisible();
    await expect(page.locator('img[alt="Đậu hũ non sốt nấm đông cô tiêu xanh"]')).toHaveAttribute('src', 'https://example.test/901.jpg');
    await page.getByRole('button', { name: 'Trang sau' }).click();
    await expect(page.getByText('Canh chua chay nấm đậu bắp thanh nhiệt')).toBeVisible();
    expect(requestedPages).toEqual([0, 1]);
  });

  test('AC-01.2: Guest tìm công thức theo từ khóa qua API', async ({ page }) => {
    await page.route('**/api/v1/recipes?**', (route) => {
      const keyword = new URL(route.request().url()).searchParams.get('keyword');
      const items = keyword === 'canh chua'
        ? [publicRecipe(902, 'Canh chua chay nấm đậu bắp thanh nhiệt')]
        : [publicRecipe(901, 'Đậu hũ non sốt nấm đông cô tiêu xanh')];
      return route.fulfill({ json: { items, page: 0, size: 12, totalElements: items.length, totalPages: 1 } });
    });

    await page.goto('/kham-pha');
    const searchInput = page.getByPlaceholder(/Tìm món chay/);
    await searchInput.fill('canh chua');
    await expect(page.getByText('Canh chua chay nấm đậu bắp thanh nhiệt')).toBeVisible();
    await expect(page.getByText('Đậu hũ non sốt nấm đông cô tiêu xanh')).toHaveCount(0);
  });

  test('AC-01.3: Guest xem nguyên liệu và hướng dẫn từ chi tiết API công khai', async ({ page }) => {
    await page.route('**/api/v1/recipes/901', (route) => {
      expect(route.request().headers().authorization).toBeUndefined();
      return route.fulfill({ json: detail });
    });

    await page.goto('/cong-thuc/id/901');
    await expect(page.getByRole('heading', { level: 1, name: detail.title })).toBeVisible();
    await expect(page.getByText('Nấm rơm tươi')).toBeVisible();
    await expect(page.getByText(detail.instructions)).toBeVisible();
  });

  test('AC-01.4: Không hiển thị bài viết vi phạm hoặc không tồn tại cho Guest', async ({ page }) => {
    await page.goto('/cong-thuc/mon-chay-vi-pham-tieu-chuan');
    await expect(page.getByTestId('recipe-not-available')).toBeVisible();
    await expect(page.getByText('Công thức không khả dụng hoặc đã bị ẩn')).toBeVisible();

    await page.goto('/cong-thuc/mon-chay-khong-ton-tai-xyz');
    await expect(page.getByTestId('recipe-not-available')).toBeVisible();
  });

  test('AC-01.5: Guest được yêu cầu đăng nhập trước hành vi thành viên', async ({ page }) => {
    await page.goto('/cong-thuc/canh-chua-chay-nam-dau-bap');

    await page.getByRole('button', { name: 'Lưu', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Yêu cầu đăng nhập' })).toBeVisible();
    await expect(page.getByText(/Vui lòng đăng nhập để lưu công thức yêu thích/)).toBeVisible();
    await page.getByRole('button', { name: 'Để sau' }).click();

    await page.getByRole('button', { name: /Thêm vào kế hoạch/ }).click();
    await expect(page.getByRole('heading', { name: 'Yêu cầu đăng nhập' })).toBeVisible();
    await expect(page.getByText(/Vui lòng đăng nhập để thêm món vào kế hoạch tuần/)).toBeVisible();
    await page.getByRole('button', { name: 'Để sau' }).click();
  });
});
