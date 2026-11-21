import { mkdirSync } from 'node:fs';
import { resolve } from 'node:path';
import { expect, test } from './baseFixtures';

const cors = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'content-type, accept, authorization',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, OPTIONS',
};

test('recipe detail separates allergen, vegetarian, dislike, and unverified ingredient warnings', async ({ page }) => {
  await page.addInitScript(() => {
    sessionStorage.setItem('mamxanh.auth', JSON.stringify({
      accessToken: 'header.payload.signature', expiresAt: Date.now() + 3_600_000,
      account: { id: 201, displayName: 'Trần Bình', email: 'binh@example.com', role: 'CUSTOMER', accountStatus: 'ACTIVE', emailVerified: true },
    }));
  });
  await page.route('**/api/v1/recipes/56', route => route.fulfill({ headers: cors, json: {
    recipeId: 56, title: 'Món chay thử cảnh báo', description: 'Bản kiểm tra giao diện.', instructions: 'Nấu chín.',
    dishCategory: 'SOUP', dishCategoryLabel: 'Món canh', vegetarianType: 'VEGAN', vegetarianTypeLabel: 'Thuần chay',
    difficulty: 'EASY', difficultyLabel: 'Dễ', servings: 2, prepTimeMinutes: 10, cookTimeMinutes: 15,
    youtubeUrl: null, publishedAt: '2026-11-05T08:00:00', author: { userId: 8, displayName: 'Tác giả', avatarUrl: null },
    ingredients: [
      { ingredientId: 1, name: 'Đậu phộng', imageUrl: null, quantity: 20, unitId: 1, unitCode: 'g', unitName: 'gram', nutritionSupported: false },
      { ingredientId: 2, name: 'Phô mai', imageUrl: null, quantity: 30, unitId: 1, unitCode: 'g', unitName: 'gram', nutritionSupported: false },
      { ingredientId: 3, name: 'Nấm rơm', imageUrl: null, quantity: 40, unitId: 1, unitCode: 'g', unitName: 'gram', nutritionSupported: false },
      { ingredientId: null, name: 'Hương liệu chưa xác minh', imageUrl: null, quantity: 1, unitId: 1, unitCode: 'g', unitName: 'gram', nutritionSupported: false },
    ],
    nutritionComplete: false, ingredientsWithoutNutrition: [],
    nutrition: { complete: false, ingredientsMissingData: [], total: {}, perServing: {} },
    statistics: { likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null, viewCount: 0 }, media: [],
  } }));
  await page.route('**/api/v1/nutrition/dietary-preferences/recipes/56/safety-assessment', route => route.fulfill({ headers: cors, json: {
    recipeId: 56, recipeTitle: 'Món chay thử cảnh báo', recipeTypeCompatible: false,
    hasAllergenOrAvoidConflict: true, hasVegetarianConflict: true, hasDislikedIngredients: true,
    unverifiedIngredients: ['Hương liệu chưa xác minh'], requiresRiskAcknowledgement: true, assessmentToken: 'test-token',
    ingredients: [
      { ingredientId: 1, name: 'Đậu phộng', conflicts: ['ALLERGEN_OR_AVOID'] },
      { ingredientId: 2, name: 'Phô mai', conflicts: ['VEGETARIAN_TYPE'] },
      { ingredientId: 3, name: 'Nấm rơm', conflicts: ['DISLIKE'] },
      { ingredientId: null, name: 'Hương liệu chưa xác minh', conflicts: ['UNVERIFIED'] },
    ],
  } }));
  await page.route('**/api/v1/recipes/56/views', route => route.fulfill({ headers: cors, json: { recipeId: 56, counted: false, viewCount: 0 } }));
  await page.route('**/api/v1/recipes/56/related**', route => route.fulfill({ headers: cors, json: { items: [] } }));
  await page.route('**/api/v1/recipes/56/reaction', route => route.fulfill({ headers: cors, json: {
    recipeId: 56, currentReaction: null, likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null,
  } }));
  await page.route('**/api/v1/recipes/56/comments**', route => route.fulfill({ headers: cors, json: {
    content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, commentCount: 0,
  } }));

  await page.goto('/cong-thuc/id/56');
  await expect(page.getByRole('heading', { name: 'Món chay thử cảnh báo' })).toBeVisible();
  await expect(page.getByText(/Công thức có nguyên liệu bạn đã chọn cần tránh: Đậu phộng/)).toBeVisible();
  await expect(page.getByText(/Công thức có thể không phù hợp kiểu ăn chay đã chọn/)).toBeVisible();
  await expect(page.getByText(/Có nguyên liệu bạn không thích: Nấm rơm/)).toBeVisible();
  await expect(page.getByText(/Chưa xác minh được nguyên liệu: Hương liệu chưa xác minh/)).toBeVisible();

  const ingredients = page.locator('ul').filter({ has: page.getByRole('button', { name: 'Đánh dấu Đậu phộng' }) });
  await expect(ingredients.getByText('Dị ứng / cần tránh')).toBeVisible();
  await expect(ingredients.getByText('Không phù hợp kiểu chay')).toBeVisible();
  await expect(ingredients.getByText('Không thích')).toBeVisible();
  const evidenceDir = resolve(process.cwd(), '../../.agents/outputs/dietary-preference-safety/evidence');
  mkdirSync(evidenceDir, { recursive: true });
  await page.screenshot({ path: resolve(evidenceDir, 'recipe-detail-dietary-warnings.png'), fullPage: true });
});
