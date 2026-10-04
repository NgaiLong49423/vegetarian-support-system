import { expect, test } from './baseFixtures';

test.describe('Issue #21 [FR-15] — Nhúng trình phát YouTube trong bài công thức', () => {
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

    // AC-15.2: Từ chối liên kết video ngoài YouTube (ví dụ Vimeo)
    await youtubeInput.fill('https://vimeo.com/123456789');
    await expect(page.getByRole('alert')).toContainText('chỉ hỗ trợ video từ YouTube');
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
    await expect(page.getByRole('alert')).toHaveCount(0);

    // Điền thông tin tối thiểu và nhấn xuất bản
    await page.getByPlaceholder('VD: Đậu hũ non sốt nấm đông cô tiêu xanh').fill('Món Chay Thử Nghiệm');
    await page.getByPlaceholder('Chia sẻ nguồn cảm hứng, hương vị đặc trưng và bí quyết của món ăn...').fill('Mô tả hương vị thanh đạm hấp dẫn.');
    await page.getByRole('button', { name: 'Xuất bản công thức' }).click();
    await expect(page.getByText('Đã đăng!')).toBeVisible();
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
    await expect(page.getByRole('alert')).toContainText('bắt đầu bằng https://');

    // Link youtube nhưng thiếu video ID hoặc sai định dạng
    await youtubeInput.fill('https://www.youtube.com/watch?v=');
    await expect(page.getByRole('alert')).toContainText('không hợp lệ');

    // Chuỗi không phải URL
    await youtubeInput.fill('not-a-valid-url');
    await expect(page.getByRole('alert')).toContainText('bắt đầu bằng https://');

    // Thử submit form khi link youtube đang lỗi
    await page.getByPlaceholder('VD: Đậu hũ non sốt nấm đông cô tiêu xanh').fill('Món Chay Thử Nghiệm');
    await page.getByRole('button', { name: 'Xuất bản công thức' }).click();
    await expect(page.getByText('Đã đăng!')).toHaveCount(0);
  });
});
