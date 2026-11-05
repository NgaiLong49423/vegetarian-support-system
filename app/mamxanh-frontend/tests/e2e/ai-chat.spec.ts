import { expect, test } from './baseFixtures';

async function signInAsMember(page: import('@playwright/test').Page, role = 'CUSTOMER') {
  await page.addInitScript((role) => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
    accessToken: 'playwright-member-token', expiresAt: Date.now() + 3_600_000,
    account: { id: 7, displayName: 'Member test', email: 'member@example.test', role, avatarUrl: null },
  })), role);
}

test('ending the owner session purges chat even after navigating away from the lazy chat component', async ({ page }) => {
  await signInAsMember(page);
  await page.route('**/api/v1/recipes/ai/chat', route => route.fulfill({ json: {
    contextType: 'GENERAL', recipeId: null, answer: 'G4 private transcript',
    sessionId: '11111111-1111-4111-8111-111111111111', recommendations: [],
  } }));
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Giải thích món chay');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByText('G4 private transcript')).toBeVisible();
  await expect.poll(() => page.evaluate(() => sessionStorage.getItem('mamxanh.ai-chat.session.v1.member-7'))).not.toBeNull();
  await page.goto('/dang-nhap');
  await page.evaluate(() => window.dispatchEvent(new CustomEvent('mamxanh:session-ended', { detail: 'expired' })));
  await expect.poll(() => page.evaluate(() => Object.keys(sessionStorage).filter(k => k.startsWith('mamxanh.ai-chat.session.v1.')))).toEqual([]);
});

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

test('partial SSE content is marked incomplete when the stream closes without a terminal event', async ({ page }) => {
  let streamRequests = 0;
  await page.route('**/api/v1/recipes/ai/chat/stream', route => {
    streamRequests += 1;
    return route.fulfill({
      status: 200,
      contentType: 'text/event-stream',
      body: streamRequests === 1
        ? 'event: delta\ndata: {"delta":"Phần trả lời còn dang dở"}\n\n'
        : 'event: delta\ndata: {"delta":"Lượt mới đã hoàn tất."}\n\n'
          + 'event: complete\ndata: {"contextType":"GENERAL","answer":"Lượt mới đã hoàn tất.","runStatus":"COMPLETED","steps":[],"sources":[]}\n\n',
    });
  });
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Giải thích một món chay');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();

  const chat = page.getByRole('dialog');
  await expect(chat.getByText('Phần trả lời còn dang dở', { exact: true })).toBeVisible();
  await expect(chat.getByText('Phản hồi chưa hoàn tất', { exact: true })).toBeVisible();
  await expect(chat.getByRole('alert')).toContainText('kết thúc trước khi có sự kiện hoàn tất');
  await page.getByLabel('Câu hỏi của bạn').fill('Thử gửi một lượt mới sau lỗi');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(chat.getByText('Lượt mới đã hoàn tất.', { exact: true })).toBeVisible();
  expect(streamRequests).toBe(2);
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

test('a Pro workflow handoff navigates only to a trusted internal route', async ({ page }) => {
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
  await expect(page).toHaveURL(/\/ke-hoach\?.*view=for-you/);
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Đậu hũ nấu sao?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByText('Câu trả lời khác', { exact: true })).toBeVisible();
  await expect(page.locator('a[href="https://example.invalid/"]')).toHaveCount(0);
  await expect(page).toHaveURL(/\/ke-hoach\?.*view=for-you/);
});

test('authoring handoff for an Expert navigates to the editable recipe form', async ({ page }) => {
  await signInAsMember(page, 'EXPERT');
  await page.route('**/api/v1/recipes/ai/chat', route => route.fulfill({ json: {
    contextType: 'GENERAL', recipeId: null, answer: 'Mở biểu mẫu để chọn nguyên liệu và dùng Trợ lý AI.',
    recommendations: [], workflowUrl: '/dang-cong-thuc',
  } }));
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Viết mô tả cho bài đăng công thức');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page).toHaveURL(/\/dang-cong-thuc$/);
});

test('Agent workflow handoff opens the selected public recipe detail only for a valid internal ID route', async ({ page }) => {
  await signInAsMember(page);
  await page.route('**/api/v1/recipes/56', route => route.fulfill({ json: {
    recipeId: 56, title: 'Đậu hũ sốt cà chua', description: 'Món chay dễ nấu.',
    instructions: 'Sốt cà chua rồi rim đậu hũ.', dishCategory: 'BRAISED', dishCategoryLabel: 'Món kho',
    vegetarianType: 'VEGAN', vegetarianTypeLabel: 'Thuần chay', difficulty: 'EASY', difficultyLabel: 'Dễ',
    servings: 2, prepTimeMinutes: 10, cookTimeMinutes: 15, youtubeUrl: null,
    publishedAt: '2026-11-05T08:00:00', author: { userId: 8, displayName: 'Tác giả', avatarUrl: null },
    ingredients: [], nutritionComplete: false, ingredientsWithoutNutrition: [],
    nutrition: { complete: false, ingredientsMissingData: [], total: {}, perServing: {} },
    statistics: { likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null, viewCount: 0 }, media: [],
  } }));
  let calls = 0;
  await page.route('**/api/v1/recipes/ai/chat', route => route.fulfill({ json: {
    contextType: 'GENERAL', recipeId: null,
    answer: ++calls === 1 ? 'Mở công thức Đậu hũ sốt cà chua.' : 'Không chuyển hướng đến URL không hợp lệ.',
    recommendations: [],
    workflowUrl: calls === 1 ? '/cong-thuc/id/56' : '/cong-thuc/id/57?redirect=https://example.invalid/',
  } }));

  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Mở trang chi tiết công thức Đậu hũ sốt cà chua');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page).toHaveURL('/cong-thuc/id/56');
  await expect(page.getByRole('heading', { name: 'Đậu hũ sốt cà chua' })).toBeVisible();

  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Câu tiếp theo');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByText('Không chuyển hướng đến URL không hợp lệ.')).toBeVisible();
  await expect(page).toHaveURL('/cong-thuc/id/56');
});

const supportedAgentDestinations = [
  { page: 'HOME', path: '/trang-chu', heading: /Mâm Xanh|nấu/i, role: 'EXPERT' },
  { page: 'EXPLORE', path: '/kham-pha', heading: 'Khám phá công thức chay', role: 'EXPERT' },
  { page: 'MEAL_PLAN', path: '/ke-hoach?view=for-you', heading: /lịch ăn|tuần/i, role: 'EXPERT' },
  { page: 'SHOPPING_LIST', path: '/di-cho', heading: 'Danh sách mua sắm', role: 'EXPERT' },
  { page: 'AI_PLANS', path: '/goi-ai', heading: 'Nâng cấp gói AI', role: 'EXPERT' },
  { page: 'AI_CREDIT_TOP_UP', path: '/nap-ai-credit', heading: 'Nạp AI Credit', role: 'EXPERT' },
  { page: 'PROFILE', path: '/ho-so', heading: 'Member test', role: 'EXPERT' },
  { page: 'DIETARY_PREFERENCES', path: '/ho-so/so-thich-an-uong', heading: 'Sở thích ăn uống', role: 'EXPERT' },
  { page: 'HEALTH_PROFILE', path: '/ho-so/dinh-duong', heading: 'Hồ sơ dinh dưỡng cá nhân', role: 'EXPERT' },
  { page: 'RECIPE_CREATE', path: '/dang-cong-thuc', heading: 'Đăng công thức món chay mới', role: 'EXPERT' },
  { page: 'EXPERT_DIRECTORY', path: '/chuyen-gia', heading: 'Chuyên gia ẩm thực chay', role: 'EXPERT' },
  { page: 'COMMUNITY', path: '/cong-dong', heading: 'Bài viết & Kiến thức ẩm thực chay', role: 'EXPERT' },
  { page: 'TRANSACTION_HISTORY', path: '/giao-dich', heading: 'Lịch sử giao dịch', role: 'EXPERT' },
  { page: 'RECIPE_DETAIL', path: '/cong-thuc/id/56', heading: 'Không thể mở công thức', role: 'EXPERT' },
] as const;

for (const destination of supportedAgentDestinations) {
  test(`Agent opens ${destination.page} at its rendered React Router destination`, async ({ page }) => {
    await signInAsMember(page, destination.role);
    await page.route('**/api/v1/recipes/ai/chat', route => route.fulfill({ json: {
      contextType: 'GENERAL', recipeId: null, answer: 'Đã mở trang theo yêu cầu.',
      recommendations: [], workflowUrl: destination.path,
    } }));
    await page.goto('/trang-chu');
    await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
    await page.getByLabel('Câu hỏi của bạn').fill(`Mở trang ${destination.page}`);
    await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();

    await expect(page).toHaveURL(new RegExp(`${destination.path.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}$`));
    await expect(page.getByRole('heading', { name: destination.heading }).first()).toBeVisible();
    if (destination.page !== 'RECIPE_DETAIL')
      await expect(page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' })).toBeVisible();
  });
}

test('a valid route with a data API failure remains on the rendered target and is not rolled back', async ({ page }) => {
  await signInAsMember(page);
  await page.route('**/api/v1/recipes/ai/chat', route => route.fulfill({ json: {
    contextType: 'GENERAL', recipeId: null, answer: 'Đã mở công thức.', recommendations: [], workflowUrl: '/cong-thuc/id/56',
  } }));
  await page.route('**/api/v1/recipes/56', route => route.fulfill({ status: 503, json: { detail: 'Recipe API unavailable' } }));
  await page.goto('/kham-pha');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Mở trang chi tiết công thức 56');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page).toHaveURL('/cong-thuc/id/56');
  await expect(page.getByRole('heading', { name: 'Không thể mở công thức' })).toBeVisible();
});

test('an informational question without a navigation workflow does not change route', async ({ page }) => {
  await page.route('**/api/v1/recipes/ai/chat', route => route.fulfill({ json: {
    contextType: 'GENERAL', recipeId: null, answer: 'Trang Khám phá giúp tìm công thức công khai.', recommendations: [],
  } }));
  await page.goto('/kham-pha');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Trang Khám phá có gì?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page).toHaveURL('/kham-pha');
  await expect(page.getByText('Trang Khám phá giúp tìm công thức công khai.')).toBeVisible();
});

test('Guest navigation to the protected recipe form lands on login without a false success or rollback', async ({ page }) => {
  await page.route('**/api/v1/recipes/ai/chat', route => route.fulfill({ json: {
    contextType: 'GENERAL', recipeId: null, answer: 'Đã mở trang đăng công thức.', recommendations: [], workflowUrl: '/dang-cong-thuc',
  } }));
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Mở trang đăng công thức');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page).toHaveURL('/dang-nhap');
  await expect(page.getByRole('alert')).toContainText('Bạn cần đăng nhập để truy cập trang này.');
  const transcript = await page.evaluate(() => Object.keys(sessionStorage).find(key => key.startsWith('mamxanh.ai-chat.session.v1.guest-')));
  expect(transcript).toBeTruthy();
});

test('a user route change while Agent is responding is never overwritten by the delayed workflow URL', async ({ page }) => {
  await signInAsMember(page);
  let finishResponse!: () => void;
  let signalRequest!: () => void;
  const responseGate = new Promise<void>(resolve => { finishResponse = resolve; });
  const requestSeen = new Promise<void>(resolve => { signalRequest = resolve; });
  await page.route('**/api/v1/recipes/ai/chat', async route => {
    signalRequest();
    await responseGate;
    await route.fulfill({ json: {
      contextType: 'GENERAL', recipeId: null, answer: 'Đang mở trang danh sách mua sắm.',
      recommendations: [], workflowUrl: '/di-cho',
    } });
  });
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Mở trang danh sách mua sắm');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await requestSeen;
  await page.evaluate(() => {
    window.history.pushState({}, '', '/kham-pha');
    window.dispatchEvent(new PopStateEvent('popstate'));
  });
  await expect(page).toHaveURL('/kham-pha');
  finishResponse();
  await expect(page.getByText('Bạn đã chuyển sang trang khác trong lúc trợ lý xử lý; hệ thống giữ nguyên trang bạn chọn.')).toBeVisible();
  await expect(page).toHaveURL('/kham-pha');
  await expect(page.getByRole('heading', { name: 'Khám phá công thức chay' })).toBeVisible();
});

test('a Customer is told when the valid recipe form route is blocked by role permissions', async ({ page }) => {
  await signInAsMember(page, 'CUSTOMER');
  await page.route('**/api/v1/recipes/ai/chat', route => route.fulfill({ json: {
    contextType: 'GENERAL', recipeId: null, answer: 'Đã mở trang đăng công thức.', recommendations: [], workflowUrl: '/dang-cong-thuc',
  } }));
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Mở trang đăng công thức');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page).toHaveURL('/dang-cong-thuc');
  await expect(page.getByRole('heading', { name: 'Đăng công thức chỉ dành cho Chuyên gia' })).toBeVisible();
  await expect(page.getByText('Trang đăng công thức chỉ dành cho tài khoản Chuyên gia.')).toBeVisible();
  await expect(page.getByRole('dialog')).toBeVisible();
});

test('an untrusted or malformed destination is rejected before Router changes the URL', async ({ page }) => {
  await page.route('**/api/v1/recipes/ai/chat', route => route.fulfill({ json: {
    contextType: 'GENERAL', recipeId: null, answer: 'Đã mở trang chi tiết.', recommendations: [],
    workflowUrl: '/trang-chu/cong-thuc/id/56',
  } }));
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Mở trang chi tiết công thức');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page).toHaveURL('/trang-chu');
  await expect(page.getByText(/chưa xác nhận được việc mở trang/)).toBeVisible();
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

test('chat transcript survives workflow navigation, close/reopen, reload, and can be cleared into a new session', async ({ page }) => {
  await signInAsMember(page);
  let chatCalls = 0;
  const deletedSessions: string[] = [];
  await page.route('**/api/v1/recipes/ai/chat', async (route) => {
    chatCalls += 1;
    await route.fulfill({ json: {
      contextType: 'GENERAL', recipeId: null, answer: 'Đã mở lịch ăn cho bạn.',
      workflowUrl: '/ke-hoach', sessionId: '11111111-1111-4111-8111-111111111111',
      recommendations: [],
    } });
  });
  await page.route('**/api/v1/recipes/ai/chat/sessions/*', async (route) => {
    deletedSessions.push(route.request().url());
    await route.fulfill({ status: 204, body: '' });
  });

  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Mở lịch ăn của tôi');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page).toHaveURL(/\/ke-hoach(?:\?.*)?$/);
  await expect(page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' })).toBeVisible();

  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await expect(page.getByText('Mở lịch ăn của tôi')).toBeVisible();
  await expect(page.getByText('Đã mở Lịch ăn trên Mâm Xanh.')).toBeVisible();
  await page.reload();
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await expect(page.getByText('Mở lịch ăn của tôi')).toBeVisible();
  await expect(page.getByText('Đã mở Lịch ăn trên Mâm Xanh.')).toBeVisible();

  page.once('dialog', (dialog) => dialog.accept());
  await page.getByRole('button', { name: 'Bắt đầu cuộc trò chuyện mới' }).click();
  await expect(page.getByText('Mở lịch ăn của tôi')).toHaveCount(0);
  await expect.poll(() => deletedSessions.length).toBe(1);
  expect(chatCalls).toBe(1);
});

test('expired Backend session keeps transcript and retry continues without the stale session ID', async ({ page }) => {
  await signInAsMember(page);
  const requests: Array<Record<string, unknown>> = [];
  await page.route('**/api/v1/recipes/ai/chat', async (route) => {
    const body = route.request().postDataJSON() as Record<string, unknown>;
    requests.push(body);
    if (requests.length === 2) {
      await route.fulfill({ status: 400, json: {
        status: 400, code: 'VALIDATION_FAILED', detail: 'Phiên trợ lý không hợp lệ hoặc đã hết hạn.',
      } });
      return;
    }
    await route.fulfill({ json: {
      contextType: 'GENERAL', recipeId: null,
      answer: requests.length === 1 ? 'Mình vẫn nhớ nội dung trước.' : 'Đã tiếp tục trong phiên mới.',
      sessionId: requests.length === 1
        ? '22222222-2222-4222-8222-222222222222'
        : '33333333-3333-4333-8333-333333333333',
      recommendations: [],
    } });
  });

  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Lần trước tôi hỏi về món đậu hũ');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByText('Mình vẫn nhớ nội dung trước.')).toBeVisible();
  await page.getByLabel('Câu hỏi của bạn').fill('Tiếp tục nhé');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByRole('alert').filter({ hasText: 'Phiên AI đã hết hạn' }))
    .toContainText('lịch sử trò chuyện vẫn được giữ');
  await expect(page.getByText('Lần trước tôi hỏi về món đậu hũ')).toBeVisible();
  await page.getByRole('alert').filter({ hasText: 'Phiên AI đã hết hạn' })
    .getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Đã tiếp tục trong phiên mới.')).toBeVisible();
  expect(requests[1].sessionId).toBe('22222222-2222-4222-8222-222222222222');
  expect(requests[2].sessionId).toBeUndefined();
  expect(requests[2].history).toEqual([]);
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
  await page.getByRole('button', { name: 'Đóng khung chat' }).click();
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await expect(page.getByText('Bài viết hướng dẫn kho đậu hũ.')).toBeVisible();
});
}

for (const width of [360, 1440]) {
test(`Guest sends general questions with a stable cookie identity at ${width}px`, async ({ page, context }) => {
  await page.setViewportSize({ width, height: 900 });
  const identities: Array<string | undefined> = [];
  await page.route('**/api/v1/recipes/ai/chat', async (route) => {
    expect(route.request().headers().authorization).toBeUndefined();
    identities.push(route.request().postDataJSON().anonymousViewerId);
    await route.fulfill({ json: { contextType: 'GENERAL', recipeId: null, answer: 'Guest có thể dùng sữa đậu nành.', recommendations: [] } });
  });
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await expect(page.getByText('Câu trả lời do AI tạo', { exact: false })).toBeVisible();
  await page.getByLabel('Câu hỏi của bạn').fill('Thay sữa bằng gì?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByText('Guest có thể dùng sữa đậu nành.', { exact: true })).toBeVisible();
  await expect(page.getByRole('dialog').getByRole('link', { name: 'Đăng ký', exact: true })).toHaveAttribute('href', '/dang-ky');
  const cookies = await context.cookies();
  expect(cookies.find((cookie) => cookie.name === 'mamxanh.recipe-viewer')?.value).toBe(identities[0]);
  expect(identities[0]).toMatch(/^[0-9a-f-]{36}$/i);
  const dialog = page.getByRole('dialog');
  expect(await dialog.evaluate((element) => element.getBoundingClientRect().right)).toBeLessThanOrEqual(width);
  await page.reload();
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await expect(page.getByText('Guest có thể dùng sữa đậu nành.', { exact: true })).toBeVisible();
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
      status: 429, code: 'AI_GUEST_RATE_LIMITED', detail: 'Tạm dừng thao tác.',
    } });
    else await route.fulfill({ json: { contextType: 'GENERAL', answer: 'Đã có thể hỏi tiếp.', recommendations: [] } });
  });
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Cách nấu?');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByRole('dialog').getByRole('alert')).toContainText('Lượt hỏi mới sẽ được mở lại vào');
  await expect(page.getByRole('dialog').getByRole('button', { name: 'Thử lại', exact: true })).toBeDisabled();
  await expect(page.getByRole('dialog').getByRole('alert').getByRole('link', { name: 'Đăng ký', exact: true })).toBeVisible();
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

test('G3 ADK pilot UI handles Shopping List consent, preview, confirmation, truthful steps, and consent revoke', async ({ page }) => {
  await signInAsMember(page, 'CUSTOMER');
  const requests: Array<Record<string, unknown>> = [];
  const sessionId = '44444444-4444-4444-8444-444444444444';
  const confirmationId = '55555555-5555-4555-8555-555555555555';

  await page.route('**/api/v1/recipes/ai/chat', async (route) => {
    const body = route.request().postDataJSON() as Record<string, unknown>;
    requests.push(body);
    expect(typeof body.clientRequestId).toBe('string');
    expect(body.clientRequestId).toMatch(/^[a-f0-9-]{36}$/i);

    if (requests.length === 1) {
      await route.fulfill({
        json: {
          contextType: 'GENERAL',
          recipeId: null,
          answer: 'Để xử lý yêu cầu về danh sách mua sắm, vui lòng xác nhận cho phép Mâm Xanh gửi nội dung yêu cầu liên quan tới Google Gemini.',
          sessionId,
          executionMode: 'CONFIRM',
          engine: 'ADK_SHOPPING_PILOT',
          runStatus: 'AWAITING_CONSENT',
          knowledgeRevision: 'g3-pilot-1',
          steps: [],
          consentRequest: {
            recipient: 'Google Gemini',
            scopes: [
              { category: 'SHOPPING_LIST', purpose: 'AI_CHAT' },
              { category: 'SHOPPING_LIST', purpose: 'SHOPPING_LIST_READ' },
              { category: 'SHOPPING_LIST', purpose: 'SHOPPING_LIST_WRITE' },
            ],
          },
          sources: [],
        },
      });
      return;
    }

    if (requests.length === 2) {
      await route.fulfill({
        json: {
          contextType: 'GENERAL',
          recipeId: null,
          answer: 'Mình đã chuẩn bị tạo danh sách mua sắm rỗng "Đi chợ cuối tuần". Vui lòng kiểm tra tên danh sách và nhấn Xác nhận thay đổi.',
          sessionId,
          executionMode: 'CONFIRM',
          engine: 'ADK_SHOPPING_PILOT',
          runStatus: 'AWAITING_CONFIRMATION',
          knowledgeRevision: 'g3-pilot-1',
          steps: [
            { sequence: 1, tool: 'search_business_knowledge', status: 'SUCCEEDED', summary: 'Đã tra cứu hướng dẫn nội bộ G3 (hybrid).' },
            { sequence: 2, tool: 'get_business_knowledge', status: 'SUCCEEDED', summary: 'Đã đọc tài liệu hướng dẫn shopping-create@2.0.0.' },
            {
              sequence: 3,
              tool: 'create_shopping_list',
              status: 'AWAITING_CONFIRMATION',
              summary: 'Cần xác nhận trước khi tạo danh sách mua sắm rỗng "Đi chợ cuối tuần".',
              result: { preview: { name: 'Đi chợ cuối tuần' } },
            },
          ],
          pendingConfirmation: {
            confirmationId,
            summary: 'Tạo danh sách mua sắm rỗng "Đi chợ cuối tuần"',
            proposedListName: 'Đi chợ cuối tuần',
          },
          sources: [{ id: 'shopping-create', title: 'Tạo danh sách mua sắm rỗng và đọc lại kết quả (G3)', version: '2.0.0' }],
        },
      });
      return;
    }

    if (requests.length === 3) {
      await route.fulfill({
        json: {
          contextType: 'GENERAL',
          recipeId: null,
          answer: 'Đã tạo danh sách mua sắm rỗng "Đi chợ cuối tuần" (ID #1001) và đọc lại từ hệ thống: danh sách #1001 ("Đi chợ cuối tuần") hiện có 0 mục.',
          sessionId,
          executionMode: 'CONFIRM',
          engine: 'ADK_SHOPPING_PILOT',
          runStatus: 'COMPLETED',
          knowledgeRevision: 'g3-pilot-1',
          steps: [
            {
              sequence: 1,
              tool: 'create_shopping_list',
              status: 'SUCCEEDED',
              summary: 'Đã tạo danh sách mua sắm rỗng "Đi chợ cuối tuần" (ID #1001).',
              result: { id: 1001, name: 'Đi chợ cuối tuần', itemCount: 0, status: 'SUCCEEDED' },
            },
            {
              sequence: 2,
              tool: 'get_shopping_list',
              status: 'SUCCEEDED',
              summary: 'Đã đọc lại danh sách mua sắm "Đi chợ cuối tuần" (ID #1001, 0 mục).',
              result: { id: 1001, name: 'Đi chợ cuối tuần', itemCount: 0 },
            },
          ],
          sources: [{ id: 'shopping-create', title: 'Tạo danh sách mua sắm rỗng và đọc lại kết quả (G3)', version: '2.0.0' }],
        },
      });
      return;
    }

    await route.fulfill({
      json: {
        contextType: 'GENERAL',
        recipeId: null,
        answer: 'Đã thu hồi đồng ý sử dụng dữ liệu danh sách mua sắm, hủy thao tác đang chờ và xóa ngữ cảnh danh sách mua sắm khỏi phiên.',
        sessionId,
        executionMode: 'CONFIRM',
        engine: 'ADK_SHOPPING_PILOT',
        runStatus: 'CONSENT_REVOKED',
        knowledgeRevision: 'g3-pilot-1',
        steps: [],
        sources: [],
      },
    });
  });

  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Hãy tạo danh sách mua sắm rỗng tên là Đi chợ cuối tuần');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();

  await expect(page.getByText('Cho phép dùng dữ liệu cá nhân?')).toBeVisible();
  await expect(page.getByText('danh sách mua sắm (nội dung yêu cầu, tên, ID và kết quả danh sách — mục đích tìm hướng dẫn và điều phối)')).toBeVisible();
  await page.getByRole('radio', { name: 'Đồng ý và tiếp tục', exact: true }).check();
  await page.getByRole('button', { name: 'Gửi', exact: true }).click();

  await expect(page.getByText('Xem lại trước khi thực hiện')).toBeVisible();
  await expect(page.getByText('Tên danh sách dự kiến: “Đi chợ cuối tuần”')).toBeVisible();
  await expect(page.getByText('Danh sách mua sắm · thử nghiệm').first()).toBeVisible();
  await page.getByRole('radio', { name: 'Xác nhận thay đổi', exact: true }).check();
  await page.getByRole('button', { name: 'Gửi', exact: true }).click();

  await expect(page.getByText('Đã tạo danh sách mua sắm rỗng "Đi chợ cuối tuần" (ID #1001) và đọc lại từ hệ thống', { exact: false })).toBeVisible();
  await expect(page.getByText(/^1\. Tạo danh sách mua sắm/).last()).toBeVisible();
  await expect(page.getByText(/^2\. Đọc lại danh sách mua sắm/).last()).toBeVisible();

  await page.getByRole('button', { name: 'Thu hồi đồng ý dữ liệu' }).last().click();
  await expect(page.getByText('Đã thu hồi đồng ý sử dụng dữ liệu danh sách mua sắm', { exact: false })).toBeVisible();

  expect(requests).toHaveLength(4);
  expect(requests[1].consentDecision).toMatchObject({ action: 'GRANT' });
  expect(requests[2]).toMatchObject({ confirmationId, confirmationDecision: 'APPROVE' });
  expect(requests[3].consentDecision).toMatchObject({ action: 'REVOKE' });
  await expect.poll(() => page.evaluate(() => sessionStorage.getItem('mamxanh.ai-chat.session.v1.member-7')))
    .not.toContain('Đi chợ cuối tuần');
});

test('G3 chat disconnect or failure does not auto-retry create request', async ({ page }) => {
  await signInAsMember(page, 'CUSTOMER');
  let postAttempts = 0;
  await page.route('**/api/v1/recipes/ai/chat', async (route) => {
    postAttempts += 1;
    await route.abort('failed');
  });

  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Hãy tạo danh sách mua sắm rỗng tên là Bữa trưa thanh đạm');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();

  await expect(page.getByRole('dialog').getByRole('alert')).toContainText('Dịch vụ trả lời đang gặp sự cố');
  expect(postAttempts).toBe(1);
});
