import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

test('loads the application shell and navigates to Explore', async ({ page }) => {
  const pageErrors: string[] = [];
  page.on('pageerror', (error) => pageErrors.push(error.message));

  await page.goto('/trang-chu');
  await expect(page.getByRole('heading', { name: 'Hôm nay bạn muốn nấu gì?' })).toBeVisible();

  await page.getByRole('button', { name: 'Khám phá', exact: true }).press('Enter');
  await page.getByRole('navigation', { name: 'Khám phá', exact: true }).getByRole('link', { name: 'Công thức', exact: true }).click();

  await expect(page).toHaveURL(/\/kham-pha$/);
  await expect(page.getByRole('heading', { name: 'Khám phá công thức chay' })).toBeVisible();
  expect(pageErrors).toEqual([]);
});

test('interactive UI components: ClickSpark, BorderGlow, RippleDistortion, InteractiveLamp, and BinButton work smoothly', async ({ page }) => {
  // 1. ClickSpark, RippleDistortion and BorderGlow on home page
  await page.goto('/trang-chu');
  const borderGlow = page.getByTestId('border-glow-card');
  await expect(borderGlow).toBeVisible();
  await borderGlow.hover();

  // Mouse move and click on screen to trigger RippleDistortion and ClickSpark canvas sparks
  await page.mouse.move(300, 200);
  await page.mouse.down();
  await page.mouse.move(350, 220);
  await page.mouse.up();
  await page.mouse.click(200, 200);
  await page.mouse.click(400, 300);
  await page.waitForTimeout(100);

  // 2. InteractiveLamp on login page
  await page.goto('/dang-nhap');
  const lamp = page.getByTestId('interactive-lamp');
  await expect(lamp).toBeVisible();

  // Toggle lamp off via pull cord, then turn on via overlay
  const pullCord = lamp.getByRole('button', { name: 'Kéo dây công tắc đèn' });
  await pullCord.click();
  await page.waitForTimeout(300);
  const overlay = page.getByRole('button', { name: 'Bật đèn để đăng nhập' });
  if (await overlay.isVisible()) {
    await overlay.click();
    await page.waitForTimeout(300);
  } else {
    await pullCord.click();
    await page.waitForTimeout(300);
  }

  // Also toggle via mushroom dome and keyboard keys (Enter / Space)
  const dome = lamp.getByRole('button', { name: 'Bật / tắt đèn' });
  await dome.click();
  await page.waitForTimeout(200);
  await dome.focus();
  await page.keyboard.press('Enter');
  await page.waitForTimeout(200);
  await pullCord.focus();
  await page.keyboard.press(' ');
  await page.waitForTimeout(200);

  // 3. BinButton in meal planner
  await page.goto('/ke-hoach');
  const deleteBtn = page.getByRole('button', { name: 'Xoá món' }).first();
  if (await deleteBtn.isVisible()) {
    await deleteBtn.click();
  }
});

test('AI Recipe Assistant modal opens guarded authoring actions and returns to manual editing', async ({ page }) => {
  await seedDemoSession(page, 'EXPERT');
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({
    json: {
      dishCategories: [{ code: 'SOUP', label: 'Món canh' }],
      vegetarianTypes: [{ code: 'VEGAN', label: 'Thuần chay' }],
      difficulties: [{ code: 'EASY', label: 'Dễ' }],
      units: [
        { unitId: 1, code: 'g', name: 'gram', dimension: 'MASS' },
        { unitId: 2, code: 'ml', name: 'mililit', dimension: 'VOLUME' },
      ],
    },
  }));
  await page.goto('/dang-cong-thuc');
  await page.getByRole('button', { name: 'Soạn với Trợ lý AI' }).click();

  const dialog = page.getByRole('dialog', { name: 'Trợ lý AI hỗ trợ soạn nội dung' });
  await expect(dialog.getByRole('button', { name: 'AI gợi ý viết mô tả' })).toBeVisible();
  await expect(dialog.getByRole('button', { name: 'AI gợi ý viết hướng dẫn chế biến' })).toBeVisible();
  await expect(dialog.getByText('Chưa nhập tên món', { exact: false })).toBeVisible();
  await page.keyboard.press('Escape');
  await expect(dialog).toHaveCount(0);
  await page.getByLabel('Tên món *').fill('Canh chay tự soạn');
  await expect(page.getByLabel('Tên món *')).toHaveValue('Canh chay tự soạn');
});

test('interactive voting on recipe detail', async ({ page }) => {
  await seedDemoSession(page, 'CUSTOMER');
  await page.goto('/cong-thuc/canh-chua-chay-nam-dau-bap');

  const likeBtn = page.locator('#btnLike');
  await expect(likeBtn).toBeVisible();
  await likeBtn.click();
});

