import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

const sampleRecipe = {
  id: 901,
  authorId: 12,
  authorName: 'Bếp Xanh',
  authorAvatarUrl: null,
  title: 'Đậu hũ non sốt nấm hương',
  description: 'Món chay thanh đạm cho bữa tối.',
  instructions: 'Nấu đậu hũ non với nấm hương đến khi thấm vị.',
  dishCategory: 'BRAISED',
  vegetarianType: 'VEGAN',
  difficulty: 'EASY',
  prepTimeMin: 10,
  cookTimeMin: 15,
  servings: 2,
  youtubeUrl: null,
  status: 'PUBLISHED',
  media: [],
  ingredients: [],
  publishedAt: '2026-10-20T10:00:00Z',
};

const recipePage = {
  items: [sampleRecipe],
  page: 0,
  size: 12,
  totalElements: 1,
  totalPages: 1,
  hasNext: false,
};

const formOptions = {
  dishCategories: [{ code: 'BRAISED', label: 'Món kho' }],
  vegetarianTypes: [{ code: 'VEGAN', label: 'Thuần chay' }],
  difficulties: [{ code: 'EASY', label: 'Dễ' }],
  units: [],
};

async function mockDiscoveryApis(page: import('@playwright/test').Page) {
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: formOptions }));
  await page.route('**/api/v1/recipes/discovery?**', (route) => route.fulfill({ json: recipePage }));
  await page.route('**/api/v1/recipes?**', (route) => route.fulfill({ json: recipePage }));
}

test('Mobile bottom navigation switches tabs on phone, hides on desktop and onboarding, and keeps AI FAB clear', async ({ page }) => {
  await mockDiscoveryApis(page);
  await page.setViewportSize({ width: 390, height: 844 });

  await page.goto('/trang-chu');
  const bottomNav = page.getByRole('navigation', { name: 'Điều hướng di động' });
  await expect(bottomNav).toBeVisible();
  await expect(bottomNav.getByRole('link', { name: 'Mục Trang chủ' })).toHaveAttribute('aria-current', 'page');

  const fab = page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' });
  await expect(fab).toBeVisible();
  const [fabBox, navBox] = await Promise.all([fab.boundingBox(), bottomNav.boundingBox()]);
  expect(fabBox).not.toBeNull();
  expect(navBox).not.toBeNull();
  expect(fabBox!.y + fabBox!.height).toBeLessThanOrEqual(navBox!.y);

  await bottomNav.getByRole('link', { name: 'Mục Khám phá' }).click();
  await expect(page).toHaveURL(/\/kham-pha$/);
  await expect(bottomNav.getByRole('link', { name: 'Mục Khám phá' })).toHaveAttribute('aria-current', 'page');

  await bottomNav.getByRole('link', { name: 'Mục Lịch ăn' }).click();
  await expect(page).toHaveURL(/\/ke-hoach$/);

  await bottomNav.getByRole('link', { name: 'Mục Mua sắm' }).click();
  await expect(page).toHaveURL(/\/di-cho$/);

  await bottomNav.getByRole('link', { name: 'Mục Tài khoản' }).click();
  await expect(page).toHaveURL(/\/dang-nhap$/);

  await page.setViewportSize({ width: 1280, height: 720 });
  await page.goto('/trang-chu');
  await expect(bottomNav).toBeHidden();
});

test('Authenticated member sees Cá nhân tab on mobile, onboarding hides bottom nav, and drawer quick search works', async ({ page }) => {
  await seedDemoSession(page, 'CUSTOMER');
  await mockDiscoveryApis(page);
  await page.route('**/api/v1/saved-recipes**', (route) => route.fulfill({ json: { content: [], totalPages: 0, totalElements: 0, page: 0, size: 50 } }));
  await page.route('**/api/v1/nutrition/dietary-preferences', (route) => route.fulfill({
    json: { vegetarianType: 'VEGAN', allergies: [], dislikedIngredients: [], spiceLevel: 'MILD', onboardingCompleted: false },
  }));
  await page.route('**/api/v1/subscriptions/me', (route) => route.fulfill({
    json: { tier: 'FREE', status: 'ACTIVE', startsAt: null, endsAt: null },
  }));

  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/khoi-tao-so-thich');
  await expect(page.getByRole('navigation', { name: 'Điều hướng di động' })).toHaveCount(0);

  await page.goto('/trang-chu');
  const bottomNav = page.getByRole('navigation', { name: 'Điều hướng di động' });
  await bottomNav.getByRole('link', { name: 'Mục Cá nhân' }).click();
  await expect(page).toHaveURL(/\/ho-so$/);
  await expect(bottomNav.getByRole('link', { name: 'Mục Cá nhân' })).toHaveAttribute('aria-current', 'page');

  await page.getByRole('button', { name: 'Menu' }).click();
  const drawer = page.getByRole('dialog', { name: 'Menu Mâm Xanh' });
  const searchInput = drawer.getByLabel('Tìm công thức trong menu');
  await searchInput.fill('đậu hũ');
  await searchInput.press('Enter');
  await expect(page).toHaveURL(/\/kham-pha\?q=%C4%91%E1%BA%ADu%20h%C5%A9/);
});

test('Explore mobile filter opens as a bottom sheet and AI chat renders without desktop resize handle on phone', async ({ page }) => {
  await mockDiscoveryApis(page);
  await page.setViewportSize({ width: 375, height: 812 });

  await page.goto('/kham-pha');
  await page.getByRole('button', { name: /^Bộ lọc/ }).click();
  const filterSheet = page.getByRole('dialog', { name: 'Bộ lọc tìm kiếm di động' });
  await expect(filterSheet).toBeVisible();
  await filterSheet.getByRole('button', { name: /Xem kết quả/ }).click();
  await expect(filterSheet).toBeHidden();

  await page.getByRole('button', { name: /^Bộ lọc/ }).click();
  await expect(filterSheet).toBeVisible();
  await filterSheet.getByRole('button', { name: 'Đóng bộ lọc' }).click();
  await expect(filterSheet).toBeHidden();

  await page.getByRole('button', { name: /^Bộ lọc/ }).click();
  await expect(filterSheet).toBeVisible();
  await page.getByRole('button', { name: 'Đóng nền bộ lọc' }).click({ position: { x: 20, y: 20 } });
  await expect(filterSheet).toBeHidden();

  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  const chat = page.getByRole('dialog');
  await expect(chat).toBeVisible();
  await expect(page.getByRole('button', { name: 'Thay đổi kích thước khung chat' })).toBeHidden();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
});

test('Landing mobile quick-jump strip, top header scroll progress, and Auth mobile lamp toggle work', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'no-preference' });
  await page.setViewportSize({ width: 390, height: 844 });

  await page.goto('/');
  const landingRoot = page.locator('.landing-page');
  await expect(landingRoot).toHaveClass(/has-scroll-motion/);
  await expect(page.locator('.landing-journey, .landing-journey-dot')).toHaveCount(0);

  const quickNav = page.getByRole('navigation', { name: 'Điều hướng nhanh đầu trang' });
  await expect(quickNav).toBeVisible();
  await quickNav.getByRole('link', { name: 'Công thức' }).click();
  await expect(page).toHaveURL(/\/#cong-thuc$/);

  await page.locator('#dinh-duong').scrollIntoViewIfNeeded();
  await expect
    .poll(() =>
      landingRoot.evaluate((el) => parseFloat(el.style.getPropertyValue('--journey-progress') || '0')),
    )
    .toBeGreaterThan(0.25);
  await expect(quickNav.getByRole('link', { name: 'Dinh dưỡng' })).toHaveClass(/is-active/);

  await page.evaluate(() => window.scrollTo({ top: document.documentElement.scrollHeight, behavior: 'instant' }));
  await expect
    .poll(() =>
      landingRoot.evaluate((el) => parseFloat(el.style.getPropertyValue('--journey-progress') || '0')),
    )
    .toBeGreaterThan(0.95);

  await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }));
  await expect
    .poll(() =>
      landingRoot.evaluate((el) => parseFloat(el.style.getPropertyValue('--journey-progress') || '1')),
    )
    .toBeLessThan(0.02);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);

  await page.goto('/dang-nhap');
  const lampToggle = page.getByRole('button', { name: 'Công tắc đèn nền' });
  await expect(lampToggle).toBeVisible();
  await expect(lampToggle).toHaveAttribute('aria-pressed', 'true');
  await lampToggle.click();
  await expect(lampToggle).toHaveAttribute('aria-pressed', 'false');
  await expect(lampToggle).toContainText('Đèn tắt');
  await lampToggle.click();
  await expect(lampToggle).toHaveAttribute('aria-pressed', 'true');
  await expect(lampToggle).toContainText('Đèn bật');
});

test('Admin workspace shows mobile bottom quick-nav bar and opens sidebar drawer from Thêm', async ({ page }) => {
  await seedDemoSession(page, 'ADMIN');
  await page.route('**/api/v1/admin/**', (route) => route.fulfill({
    json: { items: [], content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 },
  }));
  await page.setViewportSize({ width: 390, height: 844 });

  await page.goto('/admin');
  const adminMobileNav = page.getByRole('navigation', { name: 'Thanh tác vụ quản trị di động' });
  await expect(adminMobileNav).toBeVisible();
  await expect(adminMobileNav.getByRole('link', { name: 'Quản trị Bảng điều khiển' })).toHaveAttribute('aria-current', 'page');

  await adminMobileNav.getByRole('button', { name: 'Mở danh mục quản trị' }).click();
  await expect(page.getByRole('button', { name: 'Đóng menu' })).toBeVisible();
  await page.getByRole('button', { name: 'Đóng menu' }).click();

  await page.setViewportSize({ width: 1280, height: 720 });
  await expect(adminMobileNav).toBeHidden();
});
