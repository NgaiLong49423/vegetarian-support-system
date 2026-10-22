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

test('ingredient recommendations are available as a separate mode inside Meal Plan', async ({ page }) => {
  await seedDemoSession(page);
  let requestBody: { ingredientIds: number[] } | null = null;
  const recipe = {
    id: 201, authorId: 9, authorName: 'Demo Expert', authorAvatarUrl: null,
    title: 'Canh nấm rau củ', description: null, instructions: 'Nấu chín.', dishCategory: 'SOUP',
    vegetarianType: 'VEGAN', difficulty: 'EASY', servings: 2, prepTimeMin: 10, cookTimeMin: 15,
    youtubeUrl: null, status: 'PUBLISHED', media: [], ingredients: [], likes: 2, dislikes: 0,
    likePercentage: 100, viewCount: 8,
  };
  await page.route('**/api/v1/nutrition/dietary-preferences', (route) => route.fulfill({ json: profile }));
  await page.route('**/api/v1/recipes/ingredient-options**', (route) => route.fulfill({ json: [
    { ingredientId: 11, name: 'Nấm' },
  ] }));
  await page.route('**/api/v1/recipes/ai/recommendations', async (route) => {
    requestBody = route.request().postDataJSON();
    return route.fulfill({ json: { items: [{ recipe, reason: 'Dùng nguyên liệu bạn đã chọn.' }] } });
  });

  await page.goto('/ke-hoach?view=for-you');
  await page.getByRole('tab', { name: 'Món theo nguyên liệu' }).click();
  await page.getByLabel('Tìm nguyên liệu').fill('Nấm');
  await page.getByRole('option', { name: 'Nấm' }).click();
  await page.getByRole('button', { name: 'Tìm món phù hợp' }).click();

  await expect(page.getByRole('link', { name: 'Canh nấm rau củ' })).toBeVisible();
  expect(requestBody).toEqual({ ingredientIds: [11] });
  await expect(page.getByRole('tab', { name: 'Theo nhu cầu dinh dưỡng' })).toBeVisible();
});

test('AI menu stays a preview until explicit apply and applies all 21 slots', async ({ page }) => {
  await seedDemoSession(page);
  let applyCalls = 0;
  const menuBodies: unknown[] = [];
  let appliedBody: { slots: unknown[] } | null = null;
  await page.route('**/api/v1/nutrition/dietary-preferences', (route) => route.fulfill({ json: profile }));
  await page.route('**/api/v1/recipes/ingredient-options**', (route) => route.fulfill({ json: [
    { ingredientId: 11, name: 'Nấm' },
  ] }));
  await page.route('**/api/v1/recipes/ai/weekly-menu', async (route) => {
    menuBodies.push(route.request().postDataJSON());
    return route.fulfill({ json: { slots } });
  });
  await page.route('**/api/v1/meal-plans/*/ai-menu', async (route) => {
    applyCalls++;
    appliedBody = route.request().postDataJSON();
    return route.fulfill({ json: { weekStartDate: '2026-10-12', entries: [] } });
  });

  await page.goto('/ke-hoach?view=for-you');
  await expect(page.getByRole('tab', { name: 'Dành cho bạn' })).toHaveAttribute('aria-selected', 'true');
  await expect(page.getByRole('link', { name: 'Gợi ý AI' })).toHaveCount(0);
  await page.getByRole('tab', { name: 'Thực đơn tuần' }).click();
  await expect(page.getByText(/Có thể bỏ trống để AI chọn/)).toBeVisible();
  await page.getByRole('button', { name: 'Tạo thực đơn tuần' }).click();
  await expect(page.getByRole('heading', { name: 'Thực đơn tuần dành cho bạn' })).toBeVisible();
  expect(menuBodies[0]).toEqual({});
  expect(applyCalls).toBe(0);

  await page.getByRole('button', { name: 'Bỏ preview' }).click();
  await page.getByLabel('Tìm nguyên liệu').fill('Nấm');
  await page.getByRole('option', { name: 'Nấm' }).click();
  await page.getByRole('button', { name: 'Tạo thực đơn tuần' }).click();
  await expect.poll(() => menuBodies.length).toBe(2);
  expect(menuBodies[1]).toEqual({ ingredientIds: [11] });
  await expect(page.getByRole('heading', { name: 'Thực đơn tuần dành cho bạn' })).toBeVisible();
  await expect(page.getByText('Canh nấm rau củ').first()).toBeVisible();
  expect(applyCalls).toBe(0);

  await page.getByRole('button', { name: 'Bỏ preview' }).click();
  await expect(page.getByRole('heading', { name: 'Thực đơn tuần dành cho bạn' })).toHaveCount(0);
  expect(applyCalls).toBe(0);
  await page.getByRole('button', { name: 'Tạo thực đơn tuần' }).click();
  await expect(page.getByRole('heading', { name: 'Thực đơn tuần dành cho bạn' })).toBeVisible();

  await page.getByRole('button', { name: 'Xác nhận thêm vào Lịch ăn' }).click();
  await expect(page.getByText('Đã thêm đủ 21 bữa cho tuần đã chọn.')).toBeVisible();
  expect(applyCalls).toBe(1);
  expect(appliedBody?.slots).toHaveLength(21);
});
