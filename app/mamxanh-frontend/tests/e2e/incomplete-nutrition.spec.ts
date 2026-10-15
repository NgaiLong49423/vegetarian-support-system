import type { Page } from '@playwright/test';
import { expect, test } from './baseFixtures';

// FR-40 (Issue #45): a published recipe may contain catalog ingredients without nutrition data;
// each such ingredient is labelled "Chưa hỗ trợ tính dinh dưỡng" (AC-40.2).

const detail = {
  recipeId: 945,
  title: 'Nấm tràm xào sả',
  description: 'Đặc sản Phú Quốc.',
  instructions: 'Phi thơm sả, cho nấm tràm vào xào đến khi chín.',
  dishCategory: 'STIR_FRY',
  dishCategoryLabel: 'Món xào',
  vegetarianType: 'VEGAN',
  vegetarianTypeLabel: 'Thuần chay',
  difficulty: 'EASY',
  difficultyLabel: 'Dễ',
  servings: 2,
  prepTimeMinutes: 10,
  cookTimeMinutes: 15,
  youtubeUrl: null,
  publishedAt: '2026-10-13T08:00:00',
  nutritionComplete: false,
  ingredientsWithoutNutrition: ['Nấm tràm Phú Quốc'],
  author: { userId: 22, displayName: 'Bếp Chay Lan', avatarUrl: null },
  nutrition: { complete: false, ingredientsMissingData: ['Nấm tràm Phú Quốc'], total: {}, perServing: {} },
  statistics: { likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null, viewCount: 0 },
  ingredients: [
    { ingredientId: 1, name: 'Sả cây', quantity: 20, unitId: 1, unitCode: 'g', unitName: 'gram', nutritionSupported: true },
    { ingredientId: 2, name: 'Nấm tràm Phú Quốc', quantity: 200, unitId: 1, unitCode: 'g', unitName: 'gram', nutritionSupported: false },
  ],
  media: [],
};

async function routeRecipe(page: Page) {
  await page.route(/\/api\/v1\/recipes\/945$/, (route) => route.fulfill({ json: detail }));
  await page.route(/\/api\/v1\/recipes\/945\/(comments|related)/, (route) => route.fulfill({ json: { items: [], content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, commentCount: 0 } }));
}

for (const path of ['/cong-thuc/id/945', '/cong-thuc/945']) {
  test(`AC-40.2: ${path} labels only the ingredients without nutrition data`, async ({ page }) => {
    await routeRecipe(page);
    await page.goto(path);
    await expect(page.getByRole('heading', { name: 'Nấm tràm xào sả', level: 1 })).toBeVisible();

    const unsupported = page.getByRole('listitem').filter({ hasText: 'Nấm tràm Phú Quốc' });
    await expect(unsupported.getByText('Chưa hỗ trợ tính dinh dưỡng')).toBeVisible();
    const supported = page.getByRole('listitem').filter({ hasText: 'Sả cây' });
    await expect(supported.getByText('Chưa hỗ trợ tính dinh dưỡng')).toHaveCount(0);
  });
}
