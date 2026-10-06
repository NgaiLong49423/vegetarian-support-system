import { expect, test } from './baseFixtures';

const options = {
  dishCategories: [
    { code: 'BRAISED', label: 'Món kho' },
    { code: 'SOUP', label: 'Món canh' },
  ],
  vegetarianTypes: [
    { code: 'VEGAN', label: 'Thuần chay' },
    { code: 'LACTO_OVO', label: 'Có trứng và sữa' },
  ],
  difficulties: [
    { code: 'EASY', label: 'Dễ' },
    { code: 'MEDIUM', label: 'Trung bình' },
    { code: 'HARD', label: 'Khó' },
  ],
  units: [
    { unitId: 1, code: 'g', name: 'gram', dimension: 'MASS' },
    { unitId: 2, code: 'kg', name: 'kilogram', dimension: 'MASS' },
    { unitId: 3, code: 'quả', name: 'quả', dimension: 'COUNT' },
  ],
};

const ingredients = [
  { ingredientId: 11, name: 'Đậu hũ' },
  { ingredientId: 12, name: 'Cà chua' },
];

test.describe('Issue #21 [FR-15] — Nhúng trình phát YouTube trong bài công thức', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: options }));
    await page.route('**/api/v1/recipes/ingredient-options**', (route) => {
      const query = new URL(route.request().url()).searchParams.get('query')?.toLowerCase() ?? '';
      const matches = ingredients.filter((item) => item.name.toLowerCase().includes(query));
      return route.fulfill({ json: matches });
    });
    await page.route('**/api/v1/recipes', (route) => {
      if (route.request().method() === 'POST') {
        return route.fulfill({
          status: 201,
          json: { recipeId: 2199, title: 'Món Chay Thử Nghiệm', status: 'PUBLISHED', publishedAt: '2026-10-05T12:00:00' },
        });
      }
      return route.fallback();
    });
    await page.route(/\/api\/v1\/recipes\/2199$/, (route) => {
      return route.fulfill({
        status: 200,
        json: {
          recipeId: 2199,
          title: 'Món Chay Thử Nghiệm',
          description: 'Mô tả hương vị thanh đạm hấp dẫn.',
          instructions: 'Hướng dẫn chế biến món ăn ngon lành.',
          dishCategory: 'BRAISED',
          dishCategoryLabel: 'Món kho',
          vegetarianType: 'VEGAN',
          vegetarianTypeLabel: 'Thuần chay',
          difficulty: 'EASY',
          difficultyLabel: 'Dễ',
          servings: 2,
          prepTimeMinutes: 10,
          cookTimeMinutes: 20,
          youtubeUrl: null,
          publishedAt: '2026-10-05T12:00:00',
          nutritionComplete: true,
          ingredientsWithoutNutrition: [],
          ingredients: [],
          media: [],
        },
      });
    });
  });

  test('AC-15.1, AC-15.2, AC-15.3, AC-15.5: YouTube input validation, error handling, preview, and optionality', async ({ page }) => {
    // Đăng nhập vai trò Expert để mở form đăng công thức
    await page.goto('/dang-nhap');
    await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
    await page.getByRole('button', { name: /Tài khoản Lan Anh/ }).click();
    await page.getByRole('button', { name: 'Expert', exact: true }).click();
    await page.getByRole('link', { name: 'Đăng công thức mới' }).click();

    // AC-15.3: Duy nhất 1 trường nhập link YouTube cho toàn bộ bài viết
    const youtubeInput = page.getByLabel(/Liên kết video YouTube/);
    await expect(youtubeInput).toHaveCount(1);
    const youtubeContainer = youtubeInput.locator('xpath=..');

    // AC-15.2: Từ chối liên kết video ngoài YouTube (ví dụ Vimeo)
    await youtubeInput.fill('https://vimeo.com/123456789');
    await expect(youtubeContainer.getByRole('alert')).toContainText('chỉ hỗ trợ video từ YouTube');
    await expect(page.getByTestId('youtube-embed-container')).toHaveCount(0);

    // AC-15.1: Trích xuất Video ID từ đường link YouTube hợp lệ và hiển thị preview
    await youtubeInput.fill('https://youtu.be/dQw4w9WgXcQ');
    await expect(page.getByText('Đã trích xuất YouTube Video ID: dQw4w9WgXcQ')).toBeVisible();
    await expect(page.getByTestId('youtube-embed-iframe')).toBeVisible();

    // Thử định dạng full link www.youtube.com/watch?v=...
    await youtubeInput.fill('https://www.youtube.com/watch?v=dQw4w9WgXcQ');
    await expect(page.getByText('Đã trích xuất YouTube Video ID: dQw4w9WgXcQ')).toBeVisible();
    await expect(page.getByTestId('youtube-embed-iframe')).toBeVisible();

    // AC-15.5: Tính tùy chọn của video YouTube (để trống không báo lỗi khi đăng)
    await youtubeInput.fill('');
    await expect(youtubeContainer.getByRole('alert')).toHaveCount(0);

    // Điền thông tin hợp lệ theo backend schema và nhấn xuất bản
    await page.getByLabel('Tên món *').fill('Món Chay Thử Nghiệm');
    await page.getByLabel('Mô tả').fill('Mô tả hương vị thanh đạm hấp dẫn.');
    await page.getByLabel('Thể loại món').selectOption('BRAISED');
    await page.getByLabel('Loại ăn chay').selectOption('VEGAN');
    await page.getByLabel('Độ khó').selectOption('EASY');
    await page.getByLabel('Khẩu phần').fill('2');
    await page.getByLabel('Thời gian chuẩn bị').fill('10');
    await page.getByLabel('Thời gian nấu').fill('20');
    await page.getByLabel('Chọn nguyên liệu 1').fill('Đậu');
    await page.getByRole('button', { name: 'Đậu hũ', exact: true }).click();
    await page.getByLabel('Số lượng nguyên liệu 1').fill('200');
    await page.getByLabel('Đơn vị nguyên liệu 1').selectOption({ label: 'gram (g)' });
    await page.getByLabel('Hướng dẫn * (10–5.000 ký tự)').fill('Hướng dẫn chế biến món ăn ngon lành chuẩn vị.');
    await page.getByRole('button', { name: 'Xuất bản công thức' }).click();
    await expect(page).toHaveURL(/\/cong-thuc\/2199$/);
    await expect(page.getByRole('heading', { name: 'Món Chay Thử Nghiệm', level: 1 })).toBeVisible();
  });

  test('UC-15.2: Chi tiết công thức nhúng YouTube IFrame Player an toàn', async ({ page }) => {
    // Truy cập bài viết có video YouTube (đậu hũ non sốt nấm đông cô)
    await page.goto('/cong-thuc/dau-hu-non-sot-nam-dong-co');

    // Kiểm tra khu vực Video YouTube nhúng
    const youtubeSection = page.getByTestId('recipe-youtube-section');
    await expect(youtubeSection).toBeVisible();
    await expect(youtubeSection.getByText(/Video hướng dẫn thực hiện \(YouTube\)/)).toBeVisible();

    const iframe = youtubeSection.getByTestId('youtube-embed-iframe');
    await expect(iframe).toBeVisible();
    await expect(iframe).toHaveAttribute('src', /youtube-nocookie\.com\/embed\/dQw4w9WgXcQ/);
  });

  test('Kiểm tra bao phủ các định dạng URL YouTube (shorts, embed, m.youtube) và trường hợp lỗi', async ({ page }) => {
    await page.goto('/dang-nhap');
    await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
    await page.getByRole('button', { name: /Tài khoản Lan Anh/ }).click();
    await page.getByRole('button', { name: 'Expert', exact: true }).click();
    await page.getByRole('link', { name: 'Đăng công thức mới' }).click();

    const youtubeInput = page.getByLabel(/Liên kết video YouTube/);
    const youtubeContainer = youtubeInput.locator('xpath=..');

    // Shorts
    await youtubeInput.fill('https://www.youtube.com/shorts/dQw4w9WgXcQ');
    await expect(page.getByText('Đã trích xuất YouTube Video ID: dQw4w9WgXcQ')).toBeVisible();

    // Embed URL
    await youtubeInput.fill('https://www.youtube.com/embed/dQw4w9WgXcQ');
    await expect(page.getByText('Đã trích xuất YouTube Video ID: dQw4w9WgXcQ')).toBeVisible();

    // m.youtube.com
    await youtubeInput.fill('https://m.youtube.com/watch?v=dQw4w9WgXcQ');
    await expect(page.getByText('Đã trích xuất YouTube Video ID: dQw4w9WgXcQ')).toBeVisible();

    // Protocol không phải http/https (ftp://)
    await youtubeInput.fill('ftp://youtube.com/watch?v=dQw4w9WgXcQ');
    await expect(youtubeContainer.getByRole('alert')).toContainText('bắt đầu bằng https://');

    // Link youtube nhưng thiếu video ID hoặc sai định dạng
    await youtubeInput.fill('https://www.youtube.com/watch?v=');
    await expect(youtubeContainer.getByRole('alert')).toContainText('không hợp lệ');

    // Chuỗi không phải URL
    await youtubeInput.fill('not-a-valid-url');
    await expect(youtubeContainer.getByRole('alert')).toContainText('bắt đầu bằng https://');

    // Thử submit form khi link youtube đang lỗi
    await page.getByLabel('Tên món *').fill('Món Chay Thử Nghiệm');
    await page.getByRole('button', { name: 'Xuất bản công thức' }).click();
    await expect(page).not.toHaveURL(/\/cong-thuc\/2199$/);
  });
});

