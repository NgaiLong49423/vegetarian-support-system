import type { Page } from '@playwright/test';
import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

const detail = (id: number) => ({
  recipeId: id, title: `Món nguồn ${id}`, description: null, instructions: 'Nấu chín các nguyên liệu.',
  dishCategory: 'SOUP', dishCategoryLabel: 'Món canh', vegetarianType: 'VEGAN', vegetarianTypeLabel: 'Thuần chay',
  difficulty: 'EASY', difficultyLabel: 'Dễ', servings: 2, prepTimeMinutes: 5, cookTimeMinutes: 10,
  youtubeUrl: null, publishedAt: '2026-10-12T08:00:00', nutritionComplete: false,
  ingredientsWithoutNutrition: [], ingredients: [], media: [],
  author: { userId: 902, displayName: 'Tác giả', avatarUrl: null },
  statistics: { likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null, viewCount: 0 },
});

const card = (id: number, title: string) => ({
  id, title, authorId: 902, authorName: 'Tác giả', authorAvatarUrl: null, description: null,
  instructions: 'Nấu các nguyên liệu.', dishCategory: 'SOUP', vegetarianType: 'VEGAN', difficulty: 'EASY',
  servings: 2, prepTimeMin: 5, cookTimeMin: 10, youtubeUrl: null, status: 'PUBLISHED',
  media: [], ingredients: [], likes: 0, dislikes: 0, likePercentage: null, viewCount: 0,
});
const options = {
  dishCategories: [{ code: 'SOUP', label: 'Món canh' }],
  vegetarianTypes: [{ code: 'VEGAN', label: 'Thuần chay' }], difficulties: [], units: [],
};

async function api(page: Page, settings: { ordinaryEmpty?: boolean; failOrdinary?: boolean; failOptions?: boolean;
  aiStatus?: number; aiCode?: string; aiEmpty?: boolean; delayAi?: boolean;
  ordinaryItems?: ReturnType<typeof card>[]; aiItems?: ReturnType<typeof card>[] } = {}) {
  const requests = { ai: 0, ordinary: 0 };
  await page.route('**/api/v1/**', async (route) => {
    const url = new URL(route.request().url());
    if (url.pathname.endsWith('/related/ai')) {
      requests.ai++;
      if (settings.delayAi) await new Promise((resolve) => setTimeout(resolve, 700));
      await route.fulfill({ status: settings.aiStatus || 200, json: settings.aiStatus
        ? { status: settings.aiStatus, code: settings.aiCode ?? 'AI_PROVIDER_UNAVAILABLE', detail: 'Cannot recommend' }
        : { items: settings.aiEmpty ? [] : settings.aiItems ?? [card(502, 'Canh nấm AI chọn')] } });
    } else if (url.pathname.endsWith('/related')) {
      requests.ordinary++;
      await route.fulfill({ status: settings.failOrdinary && requests.ordinary === 1 ? 500 : 200,
        json: settings.failOrdinary && requests.ordinary === 1 ? { code: 'INTERNAL_ERROR', detail: 'Lỗi truy vấn' }
          : { items: settings.ordinaryEmpty ? [] : settings.ordinaryItems ?? [card(501, 'Canh rau liên quan')] } });
    } else if (url.pathname.endsWith('/form-options')) {
      await route.fulfill({ status: settings.failOptions ? 500 : 200, json: options });
    } else if (/\/recipes\/\d+$/.test(url.pathname)) {
      await route.fulfill({ json: detail(Number(url.pathname.split('/').at(-1))) });
    } else await route.fulfill({ json: { content: [], items: [], status: 'AVAILABLE' } });
  });
  return requests;
}

test('Guest sees API recommendations without a Gemini request and can open a real recipe link', async ({ page }) => {
  const requests = await api(page);
  await page.goto('/cong-thuc/id/500');
  const section = page.getByRole('region', { name: 'Công thức liên quan' });
  await expect(section.getByText('Canh rau liên quan')).toBeVisible();
  await expect(section.getByRole('link', { name: 'Đăng nhập để dùng gợi ý AI' })).toBeVisible();
  expect(requests.ai).toBe(0);
  await section.getByText('Canh rau liên quan').click();
  await expect(page).toHaveURL(/\/cong-thuc\/id\/501$/);
});

test('Member explicitly requests AI; ordinary recipes remain visible while loading', async ({ page }) => {
  await seedDemoSession(page, 'CUSTOMER');
  const requests = await api(page, { delayAi: true });
  await page.goto('/cong-thuc/id/500');
  const section = page.getByRole('region', { name: 'Công thức liên quan' });
  await expect(section.getByText('Canh rau liên quan')).toBeVisible();
  expect(requests.ai).toBe(0);
  await section.getByRole('button', { name: 'AI gợi ý món ăn kèm hoặc biến tấu' }).click();
  await expect(section.getByRole('button', { name: 'Đang tìm món phù hợp…' })).toBeDisabled();
  await expect(section.getByText('Canh rau liên quan')).toBeVisible();
  await expect(section.getByText('Canh nấm AI chọn')).toBeVisible();
  expect(requests.ai).toBe(1);
});

test('AI selections take priority without repeating regular recipe IDs or merging different recipes', async ({ page }) => {
  await seedDemoSession(page, 'CUSTOMER');
  const settings = {
    ordinaryItems: [card(501, 'Món chung'), card(503, 'Món riêng')],
    aiItems: [card(501, 'Món chung'), card(504, 'Món riêng')],
  };
  const requests = await api(page, settings);
  await page.goto('/cong-thuc/id/500');
  const section = page.getByRole('region', { name: 'Công thức liên quan' });
  await expect(section.getByText('Món chung', { exact: true })).toHaveCount(1);
  const button = section.getByRole('button', { name: 'AI gợi ý món ăn kèm hoặc biến tấu' });
  await button.click();
  await expect(section.getByRole('heading', { name: 'Gợi ý từ AI' })).toBeVisible();
  await expect(section.getByText('Món chung', { exact: true })).toHaveCount(1);
  await expect(section.getByText('Món riêng', { exact: true })).toHaveCount(2);
  expect(requests.ai).toBe(1);

  // A new AI response releases old selections back into the ordinary list.
  settings.aiItems = [];
  await button.click();
  await expect(section.getByText('AI chưa tìm được món phù hợp trong các công thức hiện có.')).toBeVisible();
  await expect(section.getByText('Món chung', { exact: true })).toHaveCount(1);
  await expect(section.getByText('Món riêng', { exact: true })).toHaveCount(1);
});

test('complete overlap keeps the AI cards without a misleading empty regular message', async ({ page }) => {
  await seedDemoSession(page, 'CUSTOMER');
  const shared = card(501, 'Món chung');
  await api(page, { ordinaryItems: [shared], aiItems: [shared] });
  await page.goto('/cong-thuc/id/500');
  const section = page.getByRole('region', { name: 'Công thức liên quan' });
  await expect(section.getByText('Món chung', { exact: true })).toBeVisible();
  await section.getByRole('button', { name: 'AI gợi ý món ăn kèm hoặc biến tấu' }).click();
  await expect(section.getByRole('heading', { name: 'Gợi ý từ AI' })).toBeVisible();
  await expect(section.getByText('Món chung', { exact: true })).toHaveCount(1);
  await expect(section.getByText('Chưa có công thức liên quan.', { exact: true })).toHaveCount(0);
});

test('Free plan sees upgrade guidance while ordinary results stay available', async ({ page }) => {
  await seedDemoSession(page);
  await api(page, { aiStatus: 403, aiCode: 'AI_SUBSCRIPTION_REQUIRED' });
  await page.goto('/cong-thuc/id/500');
  const section = page.getByRole('region', { name: 'Công thức liên quan' });
  await section.getByRole('button', { name: 'AI gợi ý món ăn kèm hoặc biến tấu' }).click();
  await expect(section.getByText(/AI gợi ý món ăn kèm hoặc biến tấu yêu cầu gói AI PRO/)).toBeVisible();
  await expect(section.getByRole('link', { name: 'Nâng cấp gói AI PRO' })).toHaveAttribute('href', '/goi-ai');
  await expect(section.getByText('Canh rau liên quan')).toBeVisible();
});

test('provider errors and empty AI selections can be retried without losing regular results', async ({ page }) => {
  await seedDemoSession(page, 'EXPERT');
  const settings = { aiStatus: 503, aiEmpty: false };
  await api(page, settings);
  await page.goto('/cong-thuc/id/500');
  const section = page.getByRole('region', { name: 'Công thức liên quan' });
  const button = section.getByRole('button', { name: 'AI gợi ý món ăn kèm hoặc biến tấu' });
  await button.click();
  await expect(section.getByRole('alert')).toContainText('Chưa thể lấy gợi ý từ AI');
  settings.aiStatus = 0;
  settings.aiEmpty = true;
  await button.click();
  await expect(section.getByText('AI chưa tìm được món phù hợp trong các công thức hiện có.')).toBeVisible();
  await expect(section.getByRole('alert')).toHaveCount(0);
  await expect(section.getByText('Canh rau liên quan')).toBeVisible();
});

test('empty regular recommendations and missing labels are safe', async ({ page }) => {
  await api(page, { ordinaryEmpty: true, failOptions: true });
  await page.goto('/cong-thuc/id/500');
  await expect(page.getByText('Chưa có công thức liên quan.')).toBeVisible();
});

test('a failed ordinary lookup has a working retry', async ({ page }) => {
  await api(page, { failOrdinary: true });
  await page.goto('/cong-thuc/id/500');
  const section = page.getByRole('region', { name: 'Công thức liên quan' });
  await section.getByRole('button', { name: 'Thử lại' }).click();
  await expect(section.getByText('Canh rau liên quan')).toBeVisible();
});

test('Admin receives regular results without an AI action', async ({ page }) => {
  await seedDemoSession(page, 'ADMIN');
  await api(page);
  await page.goto('/cong-thuc/id/500');
  const section = page.getByRole('region', { name: 'Công thức liên quan' });
  await expect(section.getByText('Canh rau liên quan')).toBeVisible();
  await expect(section.getByRole('button', { name: /AI gợi ý/ })).toHaveCount(0);
});

test('a late AI response from the previous recipe does not appear on the next recipe', async ({ page }) => {
  await seedDemoSession(page);
  await api(page, { delayAi: true });
  await page.goto('/cong-thuc/id/500');
  const section = page.getByRole('region', { name: 'Công thức liên quan' });
  await expect(section.getByText('Canh rau liên quan')).toBeVisible();
  await section.getByRole('button', { name: 'AI gợi ý món ăn kèm hoặc biến tấu' }).click();
  await section.getByText('Canh rau liên quan').click();
  await expect(page).toHaveURL(/\/cong-thuc\/id\/501$/);
  await expect(page.getByRole('heading', { name: 'Món nguồn 501', exact: true })).toBeVisible();
  await page.waitForTimeout(900);
  await expect(page.getByText('Canh nấm AI chọn')).toHaveCount(0);
});
