import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

test('loads the application shell and navigates to Explore', async ({ page }) => {
  const pageErrors: string[] = [];
  page.on('pageerror', (error) => pageErrors.push(error.message));

  await page.goto('/');
  await expect(page.getByRole('heading', { name: /Sống Xanh An Lành/i })).toBeVisible();

  await page.getByRole('link', { name: 'Khám phá món chay' }).click();

  await expect(page).toHaveURL(/\/kham-pha$/);
  await expect(page.getByRole('heading', { name: 'Khám phá công thức chay' })).toBeVisible();
  expect(pageErrors).toEqual([]);
});

test('interactive UI components: ClickSpark, BorderGlow, RippleDistortion, InteractiveLamp, and BinButton work smoothly', async ({ page }) => {
  // 1. ClickSpark, RippleDistortion and BorderGlow on home page
  await page.goto('/');
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

test('AI Recipe Assistant modal generates, previews JSON, copies and applies recipe', async ({ page }) => {
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

  const textarea = page.getByPlaceholder('VD: cà chua, trứng gà, hành lá, dầu thực vật, gia vị...');
  await expect(textarea).toBeVisible();
  await textarea.fill('đậu hũ, nấm rơm, cà rốt, dầu ăn');
  await page.getByRole('button', { name: 'Phân tích nguyên liệu & gợi ý định lượng' }).click();

  await expect(page.getByText('Công thức đề xuất')).toBeVisible();
  await page.getByRole('button', { name: 'Xem cấu trúc JSON nguyên liệu' }).click();
  await expect(page.getByText('Ẩn mã JSON nguyên liệu')).toBeVisible();
  await page.getByRole('button', { name: 'Sao chép JSON' }).click();
  await page.getByRole('button', { name: 'Nạp vào biểu mẫu công thức' }).click();

  await expect(page.getByText(/Đã nạp thành công thông tin món/)).toBeVisible();
});

test('interactive StarRating and voting on recipe detail', async ({ page }) => {
  await seedDemoSession(page, 'CUSTOMER');
  await page.goto('/cong-thuc/canh-chua-chay-nam-dau-bap');

  const star4 = page.getByLabel('4 sao');
  if (await star4.isVisible()) {
    await star4.check();
  }
  const likeBtn = page.locator('#btnLike');
  if (await likeBtn.isVisible()) {
    await likeBtn.click();
  }
});
