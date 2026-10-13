import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

const profile = {
  vegetarianType: 'VEGAN',
  avoid: { noneConfirmed: true, items: [] },
  dislike: { noneConfirmed: true, items: [] },
  cuisinePreference: null,
  maxCookingTimeMinutes: null,
  preferredDifficulty: null,
  onboardingStatus: 'COMPLETED',
  aiPersonalization: { eligible: true, missing: [] },
};

const slots = Array.from({ length: 21 }, (_, index) => ({
  dayOffset: Math.floor(index / 3),
  mealType: ['BREAKFAST', 'LUNCH', 'DINNER'][index % 3],
  recipeId: index % 2 ? 202 : 201,
  recipeTitle: index % 2 ? 'Đậu hũ xào nấm' : 'Canh nấm rau củ',
  coverUrl: null,
  dishCategory: 'SOUP',
  vegetarianType: 'VEGAN',
  reason: 'Phù hợp với hồ sơ ăn chay của bạn.',
}));

test('AI menu stays a preview until explicit apply and applies all 21 slots', async ({ page }) => {
  await seedDemoSession(page);
  let applyCalls = 0;
  let appliedBody: { slots: unknown[] } | null = null;
  await page.route('**/api/v1/nutrition/dietary-preferences', (route) => route.fulfill({ json: profile }));
  await page.route('**/api/v1/recipes/ingredient-options**', (route) => route.fulfill({ json: [
    { ingredientId: 11, name: 'Nấm' },
  ] }));
  await page.route('**/api/v1/recipes/ai/weekly-menu', (route) => route.fulfill({ json: { slots } }));
  await page.route('**/api/v1/meal-plans/*/ai-menu', async (route) => {
    applyCalls++;
    appliedBody = route.request().postDataJSON();
    return route.fulfill({ json: { weekStartDate: '2026-10-12', entries: [] } });
  });

  await page.goto('/ke-hoach?view=for-you');
  await expect(page.getByRole('tab', { name: 'Dành cho bạn' })).toHaveAttribute('aria-selected', 'true');
  await expect(page.getByRole('link', { name: 'Gợi ý AI' })).toHaveCount(0);
  await page.getByLabel('Tìm nguyên liệu').fill('Nấm');
  await page.getByRole('option', { name: 'Nấm' }).click();
  await page.getByRole('button', { name: 'Xem thực đơn tuần' }).click();
  await expect(page.getByRole('heading', { name: 'Thực đơn tuần dành cho bạn' })).toBeVisible();
  await expect(page.getByText('Canh nấm rau củ').first()).toBeVisible();
  expect(applyCalls).toBe(0);

  await page.getByRole('button', { name: 'Bỏ preview' }).click();
  await expect(page.getByRole('heading', { name: 'Thực đơn tuần dành cho bạn' })).toHaveCount(0);
  expect(applyCalls).toBe(0);
  await page.getByRole('button', { name: 'Xem thực đơn tuần' }).click();
  await expect(page.getByRole('heading', { name: 'Thực đơn tuần dành cho bạn' })).toBeVisible();

  await page.getByRole('button', { name: 'Xác nhận thêm vào Lịch ăn' }).click();
  await expect(page.getByText('Đã thêm đủ 21 bữa cho tuần đã chọn.')).toBeVisible();
  expect(applyCalls).toBe(1);
  expect(appliedBody?.slots).toHaveLength(21);
});
