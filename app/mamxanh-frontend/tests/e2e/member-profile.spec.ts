import type { Page, Route } from '@playwright/test';
import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

// FR-23 (Issue #29): public member profile, author block on a Recipe Post and own profile settings.

const recipe = (id: number, title: string) => ({
  id,
  authorId: 22,
  authorName: 'Bếp Chay Lan',
  authorAvatarUrl: null,
  title,
  description: null,
  instructions: 'Nấu các nguyên liệu đến khi chín mềm.',
  dishCategory: 'SOUP',
  vegetarianType: 'VEGAN',
  difficulty: 'EASY',
  servings: 2,
  prepTimeMin: 10,
  cookTimeMin: 15,
  youtubeUrl: null,
  status: 'PUBLISHED',
  media: [],
  ingredients: [],
  likes: 0,
  dislikes: 0,
  likePercentage: null,
  viewCount: 0,
});

const profile = {
  userId: 22,
  displayName: 'Bếp Chay Lan',
  avatarUrl: null as string | null,
  bio: 'Nấu chay mỗi ngày cho cả nhà.\nThích món miền Tây.',
  joinedMonth: '2026-09',
};

const PNG_1X1 = Buffer.from("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=", "base64");

const problem = (status: number, code: string, detail: string, errors?: Array<{ field: string; message: string }>) => ({
  status,
  contentType: 'application/problem+json',
  body: JSON.stringify({ status, code, detail, errors }),
});

async function routeMember(page: Page, member: typeof profile, pages: Array<ReturnType<typeof recipe>[]>) {
  await page.route(`**/api/v1/members/${member.userId}`, (route) => route.fulfill({ json: member }));
  await page.route(`**/api/v1/members/${member.userId}/recipes**`, (route) => {
    const index = Number(new URL(route.request().url()).searchParams.get('page') ?? '0');
    const items = pages[index] ?? [];
    const total = pages.reduce((sum, list) => sum + list.length, 0);
    return route.fulfill({ json: { items, page: index, size: 12, totalElements: total, totalPages: pages.length } });
  });
}

test('public member profile shows only public fields, the default avatar and published recipes (AC-23.2, AC-23.3)', async ({ page }) => {
  await routeMember(page, profile, [
    Array.from({ length: 12 }, (_, i) => recipe(i + 1, `Món chay số ${i + 1}`)),
    [recipe(13, 'Món chay số 13')],
  ]);
  await page.goto('/thanh-vien/22');

  await expect(page.getByRole('heading', { name: 'Bếp Chay Lan', level: 1 })).toBeVisible();
  await expect(page.getByText('Tham gia Tháng 9/2026')).toBeVisible();
  await expect(page.getByText('Thích món miền Tây.')).toBeVisible();
  await expect(page.getByRole('img', { name: 'Ảnh đại diện mặc định của Bếp Chay Lan' })).toBeVisible();
  await expect(page.getByText(/@|Chuyên gia|Bác sĩ|Đã xác minh/)).toHaveCount(0);
  await expect(page.getByText('13 công thức')).toBeVisible();
  await expect(page.getByRole('link', { name: /Món chay số 1\b/ })).toHaveAttribute('href', '/cong-thuc/id/1');
  await expect(page.getByText('Tác giả: Bếp Chay Lan')).toHaveCount(0);

  await page.getByRole('button', { name: 'Trang sau' }).click();
  await expect(page.getByText('Trang 2/2')).toBeVisible();
  await expect(page.getByRole('link', { name: /Món chay số 13/ })).toBeVisible();
  await page.getByRole('button', { name: 'Trang trước' }).click();
  await expect(page.getByText('Trang 1/2')).toBeVisible();
});

test('a member without recipes and with a broken avatar link still shows a clean profile (AC-23.3)', async ({ page }) => {
  await page.route('https://example.test/broken.png', (route) => route.abort());
  await routeMember(page, { ...profile, userId: 23, bio: null, avatarUrl: 'https://example.test/broken.png' }, [[]]);
  await page.goto('/thanh-vien/23');

  await expect(page.getByRole('img', { name: 'Ảnh đại diện mặc định của Bếp Chay Lan' })).toBeVisible();
  await expect(page.getByText('Thành viên chưa có công thức công khai nào.')).toBeVisible();
  await expect(page.getByText('Nấu chay mỗi ngày')).toHaveCount(0);
});

test('unknown or administrator profiles show not found, and a network failure can be retried', async ({ page }) => {
  await page.route('**/api/v1/members/404**', (route) => route.fulfill(problem(404, 'MEMBER_PROFILE_NOT_FOUND', 'Không tìm thấy hồ sơ thành viên.')));
  await page.goto('/thanh-vien/404');
  await expect(page.getByRole('heading', { name: 'Không tìm thấy hồ sơ thành viên' })).toBeVisible();

  await page.goto('/thanh-vien/abc');
  await expect(page.getByRole('heading', { name: 'Không tìm thấy hồ sơ thành viên' })).toBeVisible();

  let failOnce = true;
  await page.route('**/api/v1/members/24', (route: Route) => {
    if (failOnce) { failOnce = false; return route.abort(); }
    return route.fulfill({ json: { ...profile, userId: 24 } });
  });
  await page.route('**/api/v1/members/24/recipes**', (route) => route.fulfill({ json: { items: [], page: 0, size: 12, totalElements: 0, totalPages: 0 } }));
  await page.goto('/thanh-vien/24');
  await expect(page.getByRole('heading', { name: 'Không tải được hồ sơ' })).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('heading', { name: 'Bếp Chay Lan', level: 1 })).toBeVisible();
});

test('recipe detail shows the author with avatar, publish date and a link to the public profile (AC-23.1)', async ({ page }) => {
  await page.route('https://example.test/author.png', (route) => route.fulfill({ contentType: 'image/png', body: PNG_1X1 }));
  await page.route('**/api/v1/recipes/901', (route) => route.fulfill({ json: {
    recipeId: 901,
    title: 'Canh chua chay',
    description: null,
    instructions: 'Nấu các nguyên liệu đến khi chín mềm.',
    dishCategory: 'SOUP', dishCategoryLabel: 'Món canh',
    vegetarianType: 'VEGAN', vegetarianTypeLabel: 'Thuần chay',
    difficulty: 'EASY', difficultyLabel: 'Dễ',
    servings: 2, prepTimeMinutes: 10, cookTimeMinutes: 15, youtubeUrl: null,
    publishedAt: '2026-09-30T20:00:00',
    author: { userId: 22, displayName: 'Bếp Chay Lan', avatarUrl: 'https://example.test/author.png' },
    nutritionComplete: true, ingredientsWithoutNutrition: [], ingredients: [], media: [],
  } }));
  await page.goto('/cong-thuc/id/901');

  await expect(page.getByRole('link', { name: 'Bếp Chay Lan' })).toHaveAttribute('href', '/thanh-vien/22');
  await expect(page.getByRole('img', { name: 'Ảnh đại diện của Bếp Chay Lan' })).toHaveAttribute('src', 'https://example.test/author.png');
  // 2026-09-30 20:00 UTC is 1 October 2026 in Vietnam.
  await expect(page.getByText('Đăng ngày 1/10/2026')).toBeVisible();
});

test('profile settings is only for a signed-in member', async ({ page }) => {
  await page.goto('/ho-so/cai-dat');
  await expect(page).toHaveURL(/\/dang-nhap$/);

  await seedDemoSession(page, 'ADMIN');
  await page.goto('/ho-so/cai-dat');
  await expect(page.getByRole('heading', { name: 'Tài khoản quản trị không có hồ sơ thành viên' })).toBeVisible();
});

test('member edits name and bio with validation and the header name follows (AC-23.4, AC-23.6)', async ({ page }) => {
  await seedDemoSession(page, 'CUSTOMER');
  const own = { ...profile, userId: 901, displayName: 'Demo CUSTOMER', bio: null };
  await page.route('**/api/v1/me/profile', async (route) => {
    if (route.request().method() === 'GET') return route.fulfill({ json: own });
    const body = route.request().postDataJSON() as { displayName: string; bio: string };
    if (body.bio.length > 20) return route.fulfill(problem(400, 'VALIDATION_FAILED', 'Dữ liệu gửi lên không hợp lệ.', [{ field: 'bio', message: 'Giới thiệu ngắn tối đa 500 ký tự.' }]));
    if (body.displayName === 'Lỗi máy chủ') return route.fulfill(problem(500, 'INTERNAL_ERROR', 'Hệ thống gặp lỗi. Vui lòng thử lại sau.'));
    return route.fulfill({ json: { ...own, displayName: body.displayName, bio: body.bio.trim() || null } });
  });
  await page.goto('/ho-so/cai-dat');
  await expect(page.getByRole('heading', { name: 'Cài đặt hồ sơ' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Xem hồ sơ công khai' })).toHaveAttribute('href', '/thanh-vien/901');
  await expect(page.getByLabel('Tên hiển thị')).toHaveValue('Demo CUSTOMER');

  await page.getByLabel('Tên hiển thị').fill('  ab ');
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByText('Tên hiển thị cần từ 3 đến 50 ký tự.')).toBeVisible();

  await page.getByLabel('Tên hiển thị').fill('Minh An');
  await page.getByLabel(/Giới thiệu ngắn/).fill('Một đoạn giới thiệu quá dài cho máy chủ');
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByText('Giới thiệu ngắn tối đa 500 ký tự.')).toBeVisible();

  await page.getByLabel('Tên hiển thị').fill('Lỗi máy chủ');
  await page.getByLabel(/Giới thiệu ngắn/).fill('');
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByText('Hệ thống gặp lỗi. Vui lòng thử lại sau.')).toBeVisible();

  await page.getByLabel('Tên hiển thị').fill('  Minh An Bếp Chay  ');
  await page.getByLabel(/Giới thiệu ngắn/).fill('Nấu chay');
  await expect(page.getByText('8/500')).toBeVisible();
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByRole('status')).toHaveText('Đã lưu hồ sơ.');
  await expect(page.getByLabel('Tên hiển thị')).toHaveValue('Minh An Bếp Chay');
  expect(await page.evaluate(() => JSON.parse(sessionStorage.getItem('mamxanh.auth') ?? '{}').account.displayName)).toBe('Minh An Bếp Chay');
});

test('member uploads an avatar with client and server checks (AC-23.5, AC-23.7)', async ({ page }) => {
  await seedDemoSession(page, 'EXPERT');
  await page.route('**/api/v1/me/profile', (route) => route.fulfill({ json: { ...profile, userId: 901, displayName: 'Demo EXPERT' } }));
  let serverRejects = true;
  await page.route('**/api/v1/me/profile/avatar', (route) => {
    if (serverRejects) {
      serverRejects = false;
      return route.fulfill(problem(400, 'UNSUPPORTED_IMAGE_TYPE', 'Ảnh đại diện phải là tệp JPEG, PNG hoặc WebP hợp lệ.'));
    }
    return route.fulfill({ json: { ...profile, userId: 901, displayName: 'Demo EXPERT', avatarUrl: 'https://example.test/new-avatar.png' } });
  });
  await page.route('https://example.test/new-avatar.png', (route) => route.fulfill({ contentType: 'image/png', body: PNG_1X1 }));
  await page.goto('/ho-so/cai-dat');
  const picker = page.locator('input[type="file"]');

  await picker.setInputFiles({ name: 'anim.gif', mimeType: 'image/gif', buffer: Buffer.from('GIF89a') });
  await expect(page.getByText('Ảnh đại diện phải là tệp JPEG, PNG hoặc WebP.')).toBeVisible();
  await picker.setInputFiles({ name: 'big.png', mimeType: 'image/png', buffer: Buffer.alloc(2 * 1024 * 1024 + 1) });
  await expect(page.getByText('Ảnh đại diện tối đa 2 MB.')).toBeVisible();

  await picker.setInputFiles({ name: 'renamed.png', mimeType: 'image/png', buffer: Buffer.from('not an image') });
  await expect(page.getByText('Ảnh đại diện phải là tệp JPEG, PNG hoặc WebP hợp lệ.')).toBeVisible();

  await picker.setInputFiles({ name: 'me.png', mimeType: 'image/png', buffer: Buffer.from('89504e470d0a1a0a', 'hex') });
  await expect(page.getByRole('status')).toHaveText('Đã cập nhật ảnh đại diện.');
  await expect(page.getByRole('img', { name: 'Ảnh đại diện của Demo EXPERT' })).toHaveAttribute('src', 'https://example.test/new-avatar.png');
  expect(await page.evaluate(() => JSON.parse(sessionStorage.getItem('mamxanh.auth') ?? '{}').account.avatarUrl)).toBe('https://example.test/new-avatar.png');
});

test('profile settings load failure can be retried', async ({ page }) => {
  await seedDemoSession(page, 'CUSTOMER');
  let failOnce = true;
  await page.route('**/api/v1/me/profile', (route) => {
    if (failOnce) { failOnce = false; return route.abort(); }
    return route.fulfill({ json: { ...profile, userId: 901, displayName: 'Demo CUSTOMER' } });
  });
  await page.goto('/ho-so/cai-dat');
  await expect(page.getByRole('heading', { name: 'Không tải được hồ sơ' })).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByLabel('Tên hiển thị')).toHaveValue('Demo CUSTOMER');
});
