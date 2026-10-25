import { expect, test } from './baseFixtures';

async function signInAsMember(page: import('@playwright/test').Page) {
  await page.addInitScript(() => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
    accessToken: 'playwright-member-token', expiresAt: Date.now() + 3_600_000,
    account: { id: 7, displayName: 'Member test', email: 'member@example.test', role: 'CUSTOMER', avatarUrl: null },
  })));
}

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

for (const actor of ['Guest', 'Member']) {
test(`${actor} recipe detail opens the same chat with recipe ID context`, async ({ page }) => {
  if (actor === 'Member') await signInAsMember(page);
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
  if (actor === 'Guest') {
    let finishRecipe: () => void = () => undefined;
    const pending = new Promise<void>((resolve) => { finishRecipe = resolve; });
    await page.route('**/api/v1/recipes/ai/chat', async (route) => {
      if (route.request().postDataJSON().contextType === 'RECIPE') {
        await pending;
        await route.fulfill({ json: { contextType: 'RECIPE', recipeId: 42, answer: 'Phản hồi cũ đến muộn.' } });
      } else {
        expect(route.request().postDataJSON().history).toEqual([]);
        await route.fulfill({ json: { contextType: 'GENERAL', recipeId: null, answer: 'Phản hồi ngữ cảnh chung.' } });
      }
    });
    await page.getByLabel('Câu hỏi của bạn').fill('Hỏi thêm công thức.');
    await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
    await expect(page.getByLabel('Câu hỏi của bạn')).toBeDisabled();
    await page.getByRole('button', { name: 'Đóng khung chat' }).click();
    await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
    await expect(page.getByText('Bài viết hướng dẫn kho đậu hũ.')).toHaveCount(0);
    const late = page.waitForResponse((response) => response.request().postDataJSON()?.contextType === 'RECIPE'
      && response.url().endsWith('/recipes/ai/chat'));
    finishRecipe();
    await late;
    await page.getByLabel('Câu hỏi của bạn').fill('Hỏi chung.');
    await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
    await expect(page.getByText('Phản hồi ngữ cảnh chung.', { exact: true })).toBeVisible();
    await expect(page.getByText('Phản hồi cũ đến muộn.', { exact: true })).toHaveCount(0);
  }
});
}

for (const width of [360, 1440]) {
test(`Guest sends general questions with a stable cookie identity at ${width}px`, async ({ page, context }) => {
  await page.setViewportSize({ width, height: 900 });
  const identities: Array<string | undefined> = [];
  await page.route('**/api/v1/recipes/ai/chat', async (route) => {
    expect(route.request().headers().authorization).toBeUndefined();
    identities.push(route.request().headers()['x-guest-chat-id']);
    await route.fulfill({ json: { contextType: 'GENERAL', recipeId: null, answer: 'Guest có thể dùng sữa đậu nành.', recommendations: [] } });
  });
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await expect(page.getByText('Câu hỏi được chuyển tới Google Gemini', { exact: false })).toBeVisible();
  await page.getByLabel('Câu hỏi của bạn').fill('Thay sữa bằng gì?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByText('Guest có thể dùng sữa đậu nành.', { exact: true })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Đăng ký tài khoản', exact: true })).toHaveAttribute('href', '/dang-ky');
  const cookies = await context.cookies();
  expect(cookies.find((cookie) => cookie.name === 'mamxanh.guest-chat')?.value).toBe(identities[0]);
  expect(identities[0]).toMatch(/^[0-9a-f-]{36}$/i);
  const dialog = page.getByRole('dialog');
  expect(await dialog.evaluate((element) => element.getBoundingClientRect().right)).toBeLessThanOrEqual(width);
  await page.reload();
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await expect(page.getByText('Guest có thể dùng sữa đậu nành.', { exact: true })).toHaveCount(0);
  await page.getByLabel('Câu hỏi của bạn').fill('Cách nấu đậu?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect.poll(() => identities.length).toBe(2);
  expect(identities[1]).toBe(identities[0]);
});
}

test('Guest rate limit shows registration and waits for Retry-After before retrying', async ({ page }) => {
  await page.clock.install();
  let calls = 0;
  await page.route('**/api/v1/recipes/ai/chat', async (route) => {
    calls++;
    if (calls === 1) await route.fulfill({ status: 429, headers: { 'Retry-After': '3', 'Access-Control-Expose-Headers': 'Retry-After' }, json: {
      status: 429, code: 'GUEST_CHAT_RATE_LIMITED', detail: 'Tạm dừng thao tác.',
    } });
    else await route.fulfill({ json: { contextType: 'GENERAL', answer: 'Đã có thể hỏi tiếp.', recommendations: [] } });
  });
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Cách nấu?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByRole('dialog').getByRole('alert')).toContainText('Thử lại sau 3 giây');
  await expect(page.getByRole('dialog').getByRole('button', { name: 'Thử lại', exact: true })).toBeDisabled();
  await expect(page.getByRole('link', { name: 'Đăng ký tài khoản', exact: true })).toBeVisible();
  expect(calls).toBe(1);
  await page.clock.fastForward(3100);
  await expect(page.getByRole('dialog').getByRole('button', { name: 'Thử lại', exact: true })).toBeEnabled();
  await page.getByRole('dialog').getByRole('button', { name: 'Thử lại', exact: true }).click();
  await expect(page.getByText('Đã có thể hỏi tiếp.', { exact: true })).toBeVisible();
  expect(calls).toBe(2);
});

test('Guest provider failure ends loading and permits a successful retry', async ({ page }) => {
  let calls = 0;
  await page.route('**/api/v1/recipes/ai/chat', async (route) => {
    calls++;
    if (calls === 1) await route.fulfill({ status: 503, json: { status: 503, code: 'AI_PROVIDER_UNAVAILABLE' } });
    else await route.fulfill({ json: { contextType: 'GENERAL', answer: 'Có thể nấu lại.', recommendations: [] } });
  });
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Cách nấu?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByRole('dialog').getByRole('alert')).toContainText('Dịch vụ trả lời đang gặp sự cố');
  await expect(page.getByLabel('Câu hỏi của bạn')).toBeEnabled();
  await page.getByRole('dialog').getByRole('button', { name: 'Thử lại', exact: true }).click();
  await expect(page.getByText('Có thể nấu lại.', { exact: true })).toBeVisible();
});

test('Guest with blocked cookies sees guidance without a provider request', async ({ page }) => {
  let calls = 0;
  await page.addInitScript(() => Object.defineProperty(document, 'cookie', { get: () => '', set: () => undefined }));
  await page.route('**/api/v1/recipes/ai/chat', async (route) => { calls++; await route.abort(); });
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Cách nấu?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByRole('dialog').getByRole('alert')).toContainText('Vui lòng cho phép cookie');
  expect(calls).toBe(0);
});
