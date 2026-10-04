import { expect, test } from './baseFixtures';

test.describe('Issue #3 [FR-01] — Guest xem và tìm kiếm nội dung công khai', () => {
  test('AC-01.1: Guest duyệt danh sách công thức công khai, phân trang và sắp xếp', async ({ page }) => {
    // Guest truy cập trang Khám phá
    await page.goto('/kham-pha');

    // Kiểm tra các bài viết hiển thị (tiêu đề, ảnh, chế độ ăn, % like, lượt xem, tác giả)
    await expect(page.getByText('Khám phá công thức chay')).toBeVisible();
    await expect(page.getByText('Đậu hũ non sốt nấm đông cô tiêu xanh')).toBeVisible();

    // Kiểm tra phân trang hoạt động
    const nextBtn = page.getByRole('button', { name: 'Trang sau' });
    if (await nextBtn.isVisible()) {
      await nextBtn.click();
      await expect(page.getByRole('button', { name: 'Trang 2' })).toHaveAttribute('aria-current', 'page');
      await page.getByRole('button', { name: 'Trang trước' }).click();
    }

    // Kiểm tra đổi chế độ sắp xếp
    const sortSelect = page.getByLabel('Chế độ sắp xếp');
    await sortSelect.selectOption('calories');
    await expect(sortSelect).toHaveValue('calories');
    await sortSelect.selectOption('rating');
    await sortSelect.selectOption('time');
    await sortSelect.selectOption('newest');
    await sortSelect.selectOption('views');
    await sortSelect.selectOption('popular');
  });

  test('AC-01.2: Guest tìm kiếm bài công thức theo từ khóa "canh chua"', async ({ page }) => {
    await page.goto('/kham-pha');
    const searchInput = page.getByPlaceholder(/Tìm món chay/);
    await searchInput.fill('canh chua');

    await expect(page.getByText('Canh chua chay nấm đậu bắp thanh nhiệt')).toBeVisible();
    await expect(page.getByText('Đậu hũ non sốt nấm đông cô tiêu xanh')).toHaveCount(0);
  });

  test('AC-01.3: Guest xem chi tiết công thức công khai đầy đủ thành phần', async ({ page }) => {
    await page.goto('/cong-thuc/canh-chua-chay-nam-dau-bap');

    // Kiểm tra tiêu đề, thông tin tác giả
    await expect(page.getByRole('heading', { level: 1, name: 'Canh chua chay nấm đậu bắp thanh nhiệt' })).toBeVisible();
    await expect(page.locator('article, main, div').getByRole('paragraph').filter({ hasText: 'Bếp Chay Lan' })).toBeVisible();

    // Kiểm tra % Like và lượt xem
    await expect(page.getByText(/98% hài lòng/)).toBeVisible();
    await expect(page.getByText('7.820')).toBeVisible();

    // Kiểm tra nguyên liệu định lượng và các bước thực hiện
    await expect(page.getByText('Nấm rơm tươi')).toBeVisible();
    await expect(page.getByText('Các bước thực hiện')).toBeVisible();


  });

  test('AC-01.4: Không hiển thị bài viết vi phạm hoặc không tồn tại cho Guest', async ({ page }) => {
    // Bài viết vi phạm (status: HIDDEN)
    await page.goto('/cong-thuc/mon-chay-vi-pham-tieu-chuan');
    await expect(page.getByTestId('recipe-not-available')).toBeVisible();
    await expect(page.getByText('Công thức không khả dụng hoặc đã bị ẩn')).toBeVisible();

    // Bài viết không tồn tại
    await page.goto('/cong-thuc/mon-chay-khong-ton-tai-xyz');
    await expect(page.getByTestId('recipe-not-available')).toBeVisible();
  });

  test('AC-01.5: Yêu cầu đăng nhập khi Guest thực hiện hành vi thành viên trên chi tiết công thức', async ({ page }) => {
    await page.goto('/cong-thuc/canh-chua-chay-nam-dau-bap');

    // Nhấn nút Lưu công thức
    await page.getByRole('button', { name: 'Lưu', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Yêu cầu đăng nhập' })).toBeVisible();
    await expect(page.getByText(/Vui lòng đăng nhập để lưu công thức yêu thích/)).toBeVisible();
    await page.getByRole('button', { name: 'Để sau' }).click();

    // Nhấn nút Thêm vào kế hoạch
    await page.getByRole('button', { name: /Thêm vào kế hoạch/ }).click();
    await expect(page.getByRole('heading', { name: 'Yêu cầu đăng nhập' })).toBeVisible();
    await expect(page.getByText(/Vui lòng đăng nhập để thêm món vào kế hoạch tuần/)).toBeVisible();
    await page.getByRole('button', { name: 'Để sau' }).click();
  });
});
