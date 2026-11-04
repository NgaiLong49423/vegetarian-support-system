import { expect, test } from './baseFixtures';

async function signInAsMember(page: import('@playwright/test').Page, role = 'CUSTOMER') {
  await page.addInitScript((role) => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
    accessToken: 'playwright-member-token', expiresAt: Date.now() + 3_600_000,
    account: { id: 7, displayName: 'Member test', email: 'member@example.test', role, avatarUrl: null },
  })), role);
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

test('complete long AI answers remain in history for the next question', async ({ page }) => {
  const answer = 'Nấu nhỏ lửa để giữ hương vị. '.repeat(70);
  const requests: Array<{ history: AiChatHistoryTurn[] }> = [];
  type AiChatHistoryTurn = { speaker: string; content: string };
  await page.route('**/api/v1/recipes/ai/chat', async (route) => {
    requests.push(route.request().postDataJSON());
    await route.fulfill({ json: { contextType: 'GENERAL', recipeId: null,
      answer: requests.length === 1 ? answer : 'Bạn có thể thêm rau.', recommendations: [] } });
  });
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Cách nấu canh?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByText(answer, { exact: true })).toBeVisible();
  await page.getByLabel('Câu hỏi của bạn').fill('Thêm gì?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByText('Bạn có thể thêm rau.', { exact: true })).toBeVisible();
  expect(requests[1].history).toContainEqual({ speaker: 'ASSISTANT', content: answer });
});

test('follow-up chat includes only IDs of recipe cards shown in assistant history', async ({ page }) => {
  const requests: Array<{ history: Array<{ speaker: string; recommendedRecipeIds?: number[] }> }> = [];
  await page.route('**/api/v1/recipes/ai/chat', async (route) => {
    requests.push(route.request().postDataJSON());
    await route.fulfill({ json: { contextType: 'GENERAL', recipeId: null, answer: 'Món kho công khai', recommendations: [
      { id: 3, title: 'Nấm kho tiêu', description: null, coverUrl: null, totalTimeMinutes: 20, difficulty: 'EASY', reason: 'Món kho' },
    ] } });
  });
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('các món kho thử xem');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByRole('link', { name: /Nấm kho tiêu/ })).toBeVisible();
  await page.getByLabel('Câu hỏi của bạn').fill('còn món khác không');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect.poll(() => requests.length).toBe(2);
  expect(requests[1].history[0]).not.toHaveProperty('recommendedRecipeIds');
  expect(requests[1].history[1]).toMatchObject({ speaker: 'ASSISTANT', recommendedRecipeIds: [3] });
});

for (const tier of ['PLUS', 'PRO'] as const) {
  test(`advanced chat denial names the server-required ${tier} plan without a futile retry`, async ({ page }) => {
    await signInAsMember(page);
    await page.route('**/api/v1/recipes/ai/chat', route => route.fulfill({ status: 403, json: {
      status: 403, code: 'AI_SUBSCRIPTION_REQUIRED', requiredTier: tier, upgradeUrl: '/goi-ai',
      detail: 'Gói hiện tại chưa hỗ trợ tính năng này.',
    } }));
    await page.goto('/trang-chu');
    await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
    await page.getByLabel('Câu hỏi của bạn').fill(tier === 'PLUS' ? 'Lập thực đơn tuần' : 'Soạn bài đăng công thức');
    await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
    const alert = page.getByRole('dialog').getByRole('alert');
    await expect(alert).toContainText(`yêu cầu gói AI ${tier}`);
    await expect(alert.getByRole('link', { name: `Nâng cấp gói AI ${tier}` })).toHaveAttribute('href', '/goi-ai');
    await expect(alert).not.toContainText(tier === 'PLUS' ? 'PRO' : 'PLUS');
    await expect(alert.getByRole('button', { name: 'Thử lại' })).toHaveCount(0);
  });
}

test('Guest sees a login action for advanced workflows on a narrow screen', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.route('**/api/v1/recipes/ai/chat', route => route.fulfill({ status: 401, json: {
    status: 401, code: 'UNAUTHENTICATED', detail: 'Bạn cần đăng nhập để sử dụng chức năng AI nâng cao.',
  } }));
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Lập thực đơn tuần');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  const alert = page.getByRole('dialog').getByRole('alert');
  await expect(alert.getByRole('link', { name: 'Đăng nhập' })).toHaveAttribute('href', '/dang-nhap');
  await expect(alert.getByRole('button', { name: 'Thử lại' })).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
});

test('a Pro workflow handoff offers a safe local link without rendering an arbitrary URL', async ({ page }) => {
  await signInAsMember(page);
  let calls = 0;
  await page.route('**/api/v1/recipes/ai/chat', route => route.fulfill({ json: {
    contextType: 'GENERAL', recipeId: null, answer: ++calls === 1 ? 'Mở Lịch ăn để xem trước thực đơn.' : 'Câu trả lời khác',
    recommendations: [], workflowUrl: calls === 1 ? '/ke-hoach?view=for-you' : 'https://example.invalid/',
  } }));
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Lập thực đơn tuần');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByRole('link', { name: 'Mở chức năng lập thực đơn' })).toHaveAttribute('href', '/ke-hoach?view=for-you');
  await page.getByLabel('Câu hỏi của bạn').fill('Đậu hũ nấu sao?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByText('Câu trả lời khác', { exact: true })).toBeVisible();
  await expect(page.locator('a[href="https://example.invalid/"]')).toHaveCount(0);
});

test('authoring handoff for an Expert links to the editable recipe form', async ({ page }) => {
  await signInAsMember(page, 'EXPERT');
  await page.route('**/api/v1/recipes/ai/chat', route => route.fulfill({ json: {
    contextType: 'GENERAL', recipeId: null, answer: 'Mở biểu mẫu để chọn nguyên liệu và dùng Trợ lý AI.',
    recommendations: [], workflowUrl: '/dang-cong-thuc',
  } }));
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Viết mô tả cho bài đăng công thức');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByRole('link', { name: 'Mở biểu mẫu soạn công thức' })).toHaveAttribute('href', '/dang-cong-thuc');
});

test('provider throttling shows the server guidance without suggesting Guest registration', async ({ page }) => {
  const detail = 'Dịch vụ AI đang tạm giới hạn yêu cầu. Vui lòng thử lại sau.';
  await page.route('**/api/v1/recipes/ai/chat', (route) => route.fulfill({ status: 503,
    json: { status: 503, code: 'AI_PROVIDER_UNAVAILABLE', detail } }));
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Cách nấu canh?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  const alert = page.getByRole('dialog').getByRole('alert');
  await expect(alert).toContainText(detail);
  await expect(alert.getByRole('button', { name: 'Thử lại' })).toBeVisible();
  await expect(alert.getByRole('link', { name: 'Đăng ký', exact: true })).toHaveCount(0);
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

for (const actor of ['Guest', 'Member']) {
test(`${actor} recipe detail opens the same chat with recipe title and ID context`, async ({ page }) => {
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
  await expect(page.getByText('Đang hỏi về: Đậu hũ kho tiêu')).toBeVisible();
  await expect(page.getByText('Bạn muốn hỏi về công thức “Đậu hũ kho tiêu” phải không?')).toBeVisible();
  const embeddedRecipe = page.getByRole('article', { name: 'Bài công thức: Đậu hũ kho tiêu' });
  await expect(embeddedRecipe).toBeVisible();
  await expect(embeddedRecipe.getByRole('heading', { name: 'Nguyên liệu', exact: true })).toBeVisible();
  await expect(embeddedRecipe.getByText('Cách chế biến')).toBeVisible();
  await expect(embeddedRecipe.getByText('Kho đậu hũ.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Công thức này gồm những nguyên liệu nào?' })).toBeVisible();
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
  await expect(page.getByRole('dialog').getByRole('alert')).toContainText('Lượt hỏi mới sẽ được mở lại vào');
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
