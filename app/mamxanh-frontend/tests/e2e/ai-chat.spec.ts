import { expect, test } from './baseFixtures';

async function signInAsMember(page: import('@playwright/test').Page) {
  await page.addInitScript(() => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
    accessToken: 'playwright-member-token', expiresAt: Date.now() + 3_600_000,
    account: { id: 7, displayName: 'Member test', email: 'member@example.test', role: 'CUSTOMER', avatarUrl: null },
  })));
}

test('Guest uses the same chatbot with a stable session-cookie ID and can register after rate limiting', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 800 });
  const identities: string[] = [];
  await page.route('**/api/v1/recipes/ai/chat', async (route) => {
    const body = route.request().postDataJSON();
    expect(route.request().headers().authorization).toBeUndefined();
    identities.push(body.anonymousViewerId);
    await route.fulfill(identities.length === 1
      ? { json: { contextType: 'GENERAL', recipeId: null, answer: 'Bạn có thể nấu nhỏ lửa.' } }
      : { status: 429, json: { status: 429, code: 'AI_GUEST_RATE_LIMITED', detail: 'Thử lại sau.' } });
  });
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  const chat = page.getByRole('dialog');
  await expect(chat.getByText('Bạn đang trò chuyện với tư cách khách.', { exact: false })).toBeVisible();
  await page.getByLabel('Câu hỏi của bạn').fill('Cách nấu canh?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByText('Bạn có thể nấu nhỏ lửa.')).toBeVisible();
  await page.getByLabel('Câu hỏi của bạn').fill('Thêm gì?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(chat.getByRole('alert')).toContainText('Bạn đã gửi nhiều câu hỏi');
  expect(identities[0]).toMatch(/^[a-f0-9-]{36}$/i);
  expect(identities[1]).toBe(identities[0]);
  await expect(chat.getByRole('alert').getByRole('link', { name: 'Đăng ký', exact: true })).toHaveAttribute('href', '/dang-ky');
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
});

test('general chat keeps session history in the page and labels the answer as AI', async ({ page }) => {
  await signInAsMember(page);
  const requests: Array<Record<string, unknown>> = [];
  await page.route('**/api/v1/recipes/ai/chat', async (route) => {
    const body = route.request().postDataJSON() as Record<string, unknown>;
    requests.push(body);
    await route.fulfill({ json: { contextType: 'GENERAL', recipeId: null, answer: 'Có thể dùng sữa đậu nành.' } });
  });

  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await expect(page.getByRole('heading', { name: 'Bạn cần AI hỗ trợ gì?' })).toBeVisible();
  await page.getByLabel('Câu hỏi của bạn').fill('Có thể thay sữa bò bằng gì?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByText('Có thể dùng sữa đậu nành.')).toBeVisible();
  await expect(page.getByText('AI', { exact: true })).toBeVisible();
  await page.getByLabel('Câu hỏi của bạn').fill('Còn món tráng miệng thì sao?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect.poll(() => requests.length).toBe(2);
  expect(requests[1].history).toEqual([
    { speaker: 'USER', content: 'Có thể thay sữa bò bằng gì?' },
    { speaker: 'ASSISTANT', content: 'Có thể dùng sữa đậu nành.' },
  ]);
  expect(requests[0]).toMatchObject({ contextType: 'GENERAL', question: 'Có thể thay sữa bò bằng gì?' });
});

test('Enter sends a question once while Shift+Enter inserts a newline and empty input stays idle', async ({ page }) => {
  await signInAsMember(page);
  let requestCount = 0;
  await page.route('**/api/v1/recipes/ai/chat', async (route) => {
    requestCount += 1;
    await route.fulfill({ json: { contextType: 'GENERAL', recipeId: null, answer: 'Mình đã nhận câu hỏi.' } });
  });

  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  const input = page.getByLabel('Câu hỏi của bạn');

  await input.press('Enter');
  await expect.poll(() => requestCount).toBe(0);

  await input.fill('Cách nấu món chay');
  await input.press('Shift+Enter');
  await expect(input).toHaveValue('Cách nấu món chay\n');
  await input.press('End');
  await input.type(' dễ nhất là gì?');
  await input.press('Enter');

  await expect(page.getByText('Mình đã nhận câu hỏi.')).toBeVisible();
  await expect.poll(() => requestCount).toBe(1);
  await expect(page.getByText('Cách nấu món chay\n dễ nhất là gì?')).toBeVisible();
});

test('chat window can be resized with its accessible keyboard handle', async ({ page }) => {
  await signInAsMember(page);
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  const resizeHandle = page.getByRole('button', { name: 'Thay đổi kích thước khung chat' });
  await expect(resizeHandle).toBeVisible();
  const initialSize = await dialog.evaluate((element) => ({
    width: Number.parseFloat(getComputedStyle(element).width),
    height: Number.parseFloat(getComputedStyle(element).height),
  }));
  expect(initialSize.width).toBeGreaterThan(0);

  await resizeHandle.press('ArrowLeft');
  await resizeHandle.press('ArrowUp');

  await expect.poll(() => dialog.evaluate((element) => Number.parseFloat(getComputedStyle(element).width)))
    .toBe(initialSize.width + 24);
  await expect.poll(() => dialog.evaluate((element) => Number.parseFloat(getComputedStyle(element).height)))
    .toBe(initialSize.height + 24);
});

test('recipe detail opens the same chat with recipe ID context', async ({ page }) => {
  await signInAsMember(page);
  let chatRequest: Record<string, unknown> | null = null;
  await page.route('**/api/v1/recipes/42', (route) => route.fulfill({ json: {
    recipeId: 42, title: 'Đậu hũ kho tiêu', description: 'Món kho đậm vị.', instructions: 'Kho đậu hũ.',
    dishCategory: 'BRAISED', dishCategoryLabel: 'Món kho', vegetarianType: 'VEGAN', vegetarianTypeLabel: 'Thuần chay',
    difficulty: 'EASY', difficultyLabel: 'Dễ', servings: 2, prepTimeMinutes: 10, cookTimeMinutes: 15,
    youtubeUrl: null, publishedAt: '2026-10-14T10:00:00', author: { userId: 8, displayName: 'Tác giả', avatarUrl: null },
    ingredients: [], nutritionComplete: false, ingredientsWithoutNutrition: [], nutrition: { complete: false, ingredientsMissingData: [], total: {}, perServing: {} },
    statistics: { likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null, viewCount: 0 }, media: [],
  } }));
  await page.route('**/api/v1/recipes/ai/chat', async (route) => {
    chatRequest = route.request().postDataJSON() as Record<string, unknown>;
    await route.fulfill({ json: { contextType: 'RECIPE', recipeId: 42, answer: 'Bài viết hướng dẫn kho đậu hũ.' } });
  });

  await page.goto('/cong-thuc/id/42');
  await page.getByRole('button', { name: 'Hỏi AI về công thức này' }).click();
  await expect(page).toHaveURL('/cong-thuc/id/42');
  await expect(page.getByText('Đang hỗ trợ theo công thức #42')).toBeVisible();
  await page.getByLabel('Câu hỏi của bạn').fill('Bài viết ghi cách nấu thế nào?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByText('Bài viết hướng dẫn kho đậu hũ.')).toBeVisible();
  expect(chatRequest).toMatchObject({ contextType: 'RECIPE', recipeId: 42 });
});
