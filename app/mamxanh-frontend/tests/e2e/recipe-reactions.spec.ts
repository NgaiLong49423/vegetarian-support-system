import type { Page } from '@playwright/test';
import { expect, test } from './baseFixtures';

const detail = {
  recipeId: 901, title: 'Canh nấm thanh mát', description: 'Món chay cho bữa cơm gia đình.',
  instructions: 'Nấu nấm với nước dùng.', author: { userId: 22, displayName: 'Bếp Chay Lan', avatarUrl: null },
  dishCategory: 'SOUP', dishCategoryLabel: 'Món canh', vegetarianType: 'VEGAN', vegetarianTypeLabel: 'Thuần chay',
  difficulty: 'EASY', difficultyLabel: 'Dễ', servings: 2, prepTimeMinutes: 10, cookTimeMinutes: 15,
  youtubeUrl: null, publishedAt: '2026-10-11T08:00:00', nutritionComplete: true, ingredientsWithoutNutrition: [],
  ingredients: [{ ingredientId: 1, name: 'Nấm', quantity: 100, unitId: 1, unitCode: 'g', unitName: 'gram' }], media: [],
};

async function session(page: Page, role = 'CUSTOMER', id = 33) {
  await page.addInitScript(({ role, id }) => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
    accessToken: 'browser-test-token', expiresAt: Date.now() + 3600000,
    account: { id, displayName: 'Thành viên', email: 'member@example.test', role },
  })), { role, id });
}

async function api(page: Page, initialLikes = 9, initialDislikes = 1) {
  let likes = initialLikes;
  let dislikes = initialDislikes;
  let selected: 'LIKE' | 'DISLIKE' | null = null;
  const mutations: string[] = [];
  const stats = () => ({ likes, dislikes, reactionCount: likes + dislikes, likePercentage: likes + dislikes ? Math.round(likes / (likes + dislikes) * 100) : null, viewCount: 0 });
  await page.route('**/api/v1/recipes/901', (route) => route.fulfill({ json: { ...detail, statistics: stats() } }));
  await page.route('**/api/v1/recipes/901/related**', (route) => route.fulfill({ json: { items: [] } }));
  await page.route('**/api/v1/recipes?**', (route) => route.fulfill({ json: {
    items: [{ id: 901, authorId: 22, authorName: detail.author.displayName, title: detail.title, vegetarianType: 'VEGAN', dishCategory: 'SOUP', prepTimeMin: 10, cookTimeMin: 15, servings: 2, media: [], ingredients: [], ...stats() }],
    page: 0, size: 12, totalElements: 1, totalPages: 1,
  } }));
  await page.route('**/api/v1/recipes/901/reaction', (route) => {
    const method = route.request().method();
    expect(route.request().headers().authorization).toBe('Bearer browser-test-token');
    if (method !== 'GET') {
      mutations.push(method);
      if (selected === 'LIKE') likes--;
      if (selected === 'DISLIKE') dislikes--;
      selected = method === 'DELETE' ? null : route.request().postDataJSON().reactionType;
      if (selected === 'LIKE') likes++;
      if (selected === 'DISLIKE') dislikes++;
    }
    return route.fulfill({ json: { recipeId: 901, currentReaction: selected, ...stats() } });
  });
  return { mutations };
}

test('Member thêm, đổi, hủy; tải lại giữ lựa chọn và card nhận tỷ lệ mới', async ({ page }) => {
  await session(page);
  const { mutations } = await api(page);
  await page.goto('/cong-thuc/id/901');
  const like = page.getByRole('button', { name: 'Thích', exact: true });
  const dislike = page.getByRole('button', { name: 'Không thích', exact: true });
  await expect(like).toBeEnabled();
  await like.click();
  await expect(like).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '91');
  await page.reload();
  await expect(like).toHaveAttribute('aria-pressed', 'true');
  await dislike.click();
  await expect(dislike).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByText('9 lượt thích · 2 lượt không thích · 11 bình chọn')).toBeVisible();
  await dislike.click();
  await expect(dislike).toHaveAttribute('aria-pressed', 'false');
  await expect(page.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '90');
  await like.click();
  await page.getByRole('link', { name: 'Khám phá', exact: true }).last().click();
  await expect(page.getByLabel('Tỷ lệ Like: 91%')).toBeVisible();
  expect(mutations).toEqual(['PUT', 'PUT', 'DELETE', 'PUT']);
});

test('Guest xem Mới, mở đăng nhập và không gửi bình chọn', async ({ page }) => {
  const { mutations } = await api(page, 0, 0);
  await page.goto('/cong-thuc/id/901');
  await expect(page.getByLabel('Chưa có bình chọn')).toHaveText('Mới');
  await page.getByRole('button', { name: 'Thích', exact: true }).click();
  await expect(page.getByRole('dialog', { name: 'Yêu cầu đăng nhập' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Đăng nhập', exact: true }).last()).toHaveAttribute('href', '/dang-nhap');
  await page.getByRole('button', { name: 'Để sau' }).click();
  expect(mutations).toEqual([]);
});

for (const [role, id] of [['EXPERT', 22], ['ADMIN', 33]] as const) {
  test(`${role} không đủ điều kiện không thể bình chọn`, async ({ page }) => {
    await session(page, role, id);
    const { mutations } = await api(page);
    await page.goto('/cong-thuc/id/901');
    await expect(page.getByRole('button', { name: 'Thích', exact: true })).toBeDisabled();
    await expect(page.getByText(role === 'EXPERT' ? /Tác giả không được tự bình chọn/ : /Chỉ thành viên và chuyên gia/)).toBeVisible();
    expect(mutations).toEqual([]);
  });
}

test('Lỗi đọc lựa chọn có tải lại; lỗi ghi giữ nguyên dữ liệu; 403 chặn tiếp tục', async ({ page }) => {
  await session(page, 'EXPERT');
  await api(page);
  let reads = 0;
  let writes = 0;
  await page.route('**/api/v1/recipes/901/reaction', (route) => {
    if (route.request().method() === 'GET') {
      if (++reads === 1) return route.abort();
      return route.fallback();
    }
    if (++writes === 1) return route.abort();
    return route.fulfill({ status: 403, json: { status: 403, code: 'RECIPE_SELF_REACTION_NOT_ALLOWED', detail: 'Tác giả không được tự bình chọn bài viết của chính mình' } });
  });
  await page.goto('/cong-thuc/id/901');
  const like = page.getByRole('button', { name: 'Thích', exact: true });
  await expect(like).toBeDisabled();
  await page.getByRole('button', { name: 'Tải lại bình chọn' }).click();
  await expect(like).toBeEnabled();
  await like.click();
  await expect(page.getByRole('region', { name: 'Bình chọn công thức' }).getByRole('alert')).toBeVisible();
  await expect(like).toHaveAttribute('aria-pressed', 'false');
  await expect(page.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '90');
  await like.click();
  await expect(page.getByRole('region', { name: 'Bình chọn công thức' }).getByRole('alert')).toContainText('Tác giả không được tự bình chọn');
  await expect(like).toBeDisabled();
});

test('Đang ghi khóa hai nút; hủy phiếu cuối trở về Mới, 0% vẫn hiển thị', async ({ page }) => {
  await session(page);
  await api(page, 0, 0);
  let release!: () => void;
  const wait = new Promise<void>((resolve) => { release = resolve; });
  let writes = 0;
  await page.route('**/api/v1/recipes/901/reaction', async (route) => {
    if (route.request().method() !== 'GET' && ++writes === 1) await wait;
    await route.fallback();
  });
  await page.goto('/cong-thuc/id/901');
  const dislike = page.getByRole('button', { name: 'Không thích', exact: true });
  await expect(dislike).toBeEnabled();
  await dislike.click();
  await expect(dislike).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Thích', exact: true })).toBeDisabled();
  release();
  await expect(page.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '0');
  await expect(dislike).toBeEnabled();
  await dislike.click();
  await expect(page.getByLabel('Chưa có bình chọn')).toHaveText('Mới');
  await expect(page.getByRole('progressbar')).toHaveCount(0);
  expect(writes).toBe(2);
});

test('Bố cục 360/768/1440px và bình chọn bằng bàn phím', async ({ page }, testInfo) => {
  await session(page);
  await api(page);
  await page.goto('/cong-thuc/id/901');
  const like = page.getByRole('button', { name: 'Thích', exact: true });
  await expect(like).toBeEnabled();
  for (const width of [360, 768, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    await expect(like).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await page.screenshot({ path: testInfo.outputPath(`detail-${width}.png`), fullPage: true });
  }
  await like.focus();
  await page.keyboard.press('Enter');
  await expect(like).toHaveAttribute('aria-pressed', 'true');
});

test('Card trang chủ và Khám phá đặt Mới ở góc trên trái ảnh', async ({ page }) => {
  await api(page, 0, 0);
  for (const path of ['/trang-chu', '/kham-pha']) {
    await page.goto(path);
    const badges = page.getByLabel('Chưa có bình chọn');
    await expect(badges).toHaveCount(path === '/trang-chu' ? 2 : 1);
    const badge = badges.first();
    await expect(badge).toHaveText('Mới');
    const offset = await badge.evaluate((element) => {
      // Read both rectangles in one frame so card entrance motion cannot skew the offset.
      const bounds = element.getBoundingClientRect();
      const image = element.parentElement!.parentElement!.getBoundingClientRect();
      return { x: bounds.x - image.x, y: bounds.y - image.y };
    });
    expect(offset.x).toBeGreaterThanOrEqual(0);
    expect(offset.y).toBeGreaterThanOrEqual(0);
    expect(offset.x).toBeLessThan(20);
    expect(offset.y).toBeLessThan(20);
  }
});

test('Bài bị ẩn trong lúc bình chọn không cập nhật số và chặn thao tác tiếp', async ({ page }) => {
  await session(page);
  await api(page);
  await page.route('**/api/v1/recipes/901/reaction', (route) => route.request().method() === 'GET'
    ? route.fallback() : route.fulfill({ status: 404, json: { status: 404, code: 'NOT_FOUND', detail: 'Không tìm thấy công thức.' } }));
  await page.goto('/cong-thuc/id/901');
  const like = page.getByRole('button', { name: 'Thích', exact: true });
  await expect(like).toBeEnabled();
  await like.click();
  await expect(page.getByRole('region', { name: 'Bình chọn công thức' }).getByRole('alert')).toHaveText('Công thức không còn công khai hoặc đã bị xóa.');
  await expect(like).toBeDisabled();
  await expect(page.getByRole('progressbar')).toHaveAttribute('aria-valuenow', '90');
});
