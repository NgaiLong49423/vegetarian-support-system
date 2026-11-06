import { mkdir } from 'node:fs/promises';
import { expect, test, type Page, type TestInfo } from '@playwright/test';

type AiChatRequest = {
  contextType: string;
  executionMode?: string;
  recipeId?: number;
  clientRequestId: string;
  question: string;
};

function backendApiBaseUrl(): string {
  return process.env.MAMXANH_AI_AGENT_API_BASE_URL ?? 'http://localhost:18080/api/v1';
}

async function authenticatedApiGet(page: Page, path: string) {
  const accessToken = await page.evaluate(() => {
    const raw = window.sessionStorage.getItem('mamxanh.auth');
    if (!raw) return null;
    try {
      return (JSON.parse(raw) as { accessToken?: string }).accessToken ?? null;
    } catch {
      return null;
    }
  });
  expect(accessToken, 'The signed-in test tab must hold its current session token.').toBeTruthy();
  return page.request.get(new URL(path.replace(/^\/+/, ''), `${backendApiBaseUrl().replace(/\/$/, '')}/`).toString(), {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}

function liveEnabled(): boolean {
  return process.env.MAMXANH_AI_AGENT_LIVE === '1';
}

async function login(page: Page, email: string): Promise<{ role: string }> {
  const password = process.env.MAMXANH_DEMO_PASSWORD;
  if (!password) throw new Error('MAMXANH_DEMO_PASSWORD is required for authenticated live acceptance.');
  await page.goto('/dang-nhap');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Mật khẩu', { exact: true }).fill(password);
  const loginResponsePromise = page.waitForResponse(response =>
    response.request().method() === 'POST' && new URL(response.url()).pathname === '/api/v1/auth/login');
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  const loginResponse = await loginResponsePromise;
  expect(loginResponse.status()).toBe(200);
  await expect(page).not.toHaveURL(/\/dang-nhap(?:\?|$)/, { timeout: 15_000 });
  const payload = await loginResponse.json() as { account?: { role?: string } };
  expect(payload.account?.role).toBeTruthy();
  return { role: payload.account!.role! };
}

async function askRecipe(page: Page, question: string): Promise<AiChatRequest> {
  const input = page.getByLabel('Câu hỏi của bạn');
  const streamResponse = page.waitForResponse(response =>
    response.request().method() === 'POST' && new URL(response.url()).pathname.endsWith('/recipes/ai/chat/stream'));
  await input.fill(question);
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  const response = await streamResponse;
  expect(response.status()).toBe(200);
  return response.request().postDataJSON() as AiChatRequest;
}

async function askInChat(page: Page, question: string): Promise<AiChatRequest> {
  const chat = page.getByLabel('Cuộc trò chuyện');
  const assistantMessages = chat.locator('div.flex.gap-3.justify-start');
  const previousAssistantMessages = await assistantMessages.count();
  const request = await askRecipe(page, question);
  await expect(page.getByRole('button', { name: 'Dừng câu trả lời' })).toBeHidden({ timeout: 90_000 });
  await expect(assistantMessages).toHaveCount(previousAssistantMessages + 1, { timeout: 90_000 });
  return request;
}

async function grantPendingDataConsents(page: Page): Promise<void> {
  const consentHeading = page.getByRole('heading', { name: 'Cho phép dùng dữ liệu cá nhân?' });
  for (let consentRound = 0; consentRound < 4; consentRound++) {
    if (!(await consentHeading.isVisible().catch(() => false))) return;
    const consentCard = consentHeading.locator('xpath=ancestor::form');
    const responsePromise = page.waitForResponse(response =>
      response.request().method() === 'POST' && new URL(response.url()).pathname.endsWith('/recipes/ai/chat/stream'));
    await consentCard.getByRole('radio', { name: 'Đồng ý và tiếp tục' }).check();
    await consentCard.getByRole('button', { name: 'Gửi', exact: true }).click();
    expect((await responsePromise).status()).toBe(200);
    await expect(page.getByRole('button', { name: 'Dừng câu trả lời' })).toBeHidden({ timeout: 90_000 });
  }
  await expect(consentHeading).toBeHidden();
}

async function saveEvidence(page: Page, testInfo: TestInfo, label: string): Promise<void> {
  await mkdir(testInfo.outputDir, { recursive: true });
  await page.screenshot({ path: testInfo.outputPath(`${label}.png`), fullPage: true }).catch(() => undefined);
  await page.context().tracing.stop({ path: testInfo.outputPath(`${label}-trace.zip`) }).catch(() => undefined);
}

function currentWeekStartDate(): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(new Date());
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find(value => value.type === type)?.value ?? '';
  const today = new Date(Date.UTC(Number(part('year')), Number(part('month')) - 1, Number(part('day'))));
  today.setUTCDate(today.getUTCDate() - ((today.getUTCDay() + 6) % 7));
  return today.toISOString().slice(0, 10);
}

function mealPlanTestWeekStartDate(): string {
  const configuredDate = process.env.MAMXANH_AI_AGENT_WEEK_START_DATE;
  if (!configuredDate) return currentWeekStartDate();
  const currentWeek = currentWeekStartDate();

  if (!/^\d{4}-\d{2}-\d{2}$/.test(configuredDate)) {
    throw new Error('MAMXANH_AI_AGENT_WEEK_START_DATE must be the current Monday in YYYY-MM-DD format.');
  }
  const parsedDate = new Date(`${configuredDate}T00:00:00.000Z`);
  if (Number.isNaN(parsedDate.getTime()) || parsedDate.toISOString().slice(0, 10) !== configuredDate
    || parsedDate.getUTCDay() !== 1 || configuredDate !== currentWeek) {
    throw new Error('MAMXANH_AI_AGENT_WEEK_START_DATE must equal the current week Monday; future weeks cannot be applied.');
  }
  return configuredDate;
}

async function readMealPlanWeek(page: Page, weekStartDate: string): Promise<{ weekStartDate: string; entries: Array<{
  mealDate: string; mealType: string; recipeId: number; recipeTitle: string | null;
}> }> {
  const response = await authenticatedApiGet(page,
    `/meal-plans?weekStartDate=${encodeURIComponent(weekStartDate)}`);
  expect(response.status()).toBe(200);
  return await response.json();
}

test('P0-A TC-002: recipe page context reaches ADK for P1, typo P2, and follow-up P3', async ({ page }, testInfo) => {
  test.skip(!liveEnabled(), 'Live Gemini acceptance is opt-in.');
  test.setTimeout(90_000);
  page.on('request', request => {
    const url = new URL(request.url());
    if (url.pathname.startsWith('/api/v1/recipes/ai/chat')) console.log(`AI_HTTP_REQUEST ${request.method()} ${url.pathname}`);
  });
  page.on('requestfailed', request => {
    const url = new URL(request.url());
    if (url.pathname.startsWith('/api/v1/')) console.log(`API_REQUEST_FAILED ${request.method()} ${url.pathname} ${request.failure()?.errorText ?? 'unknown'}`);
  });
  page.on('response', response => {
    const url = new URL(response.url());
    if (url.pathname.startsWith('/api/v1/recipes/ai/chat')) console.log(`AI_HTTP_RESPONSE ${response.status()} ${url.pathname}`);
  });
  await page.goto('/cong-thuc/id/57');
  await expect(page.getByRole('heading', { name: 'Bánh chuối hấp nước cốt dừa lá dứa' })).toBeVisible();
  await page.context().tracing.start({ screenshots: true, snapshots: true, sources: true });
  try {
    await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();

    const prompts = [
      { text: 'Công thức này cần bao nhiêu bột năng?', expected: /120\s*g/i },
      { text: 'Bot năng dùng bao nhiêu vậy?', expected: /120\s*g/i },
      { text: 'Còn lá dứa thì sao?', expected: /8\s*g/i },
    ];
    for (const prompt of prompts) {
      const request = await askInChat(page, prompt.text);
      expect(request).toMatchObject({ contextType: 'RECIPE', recipeId: 57, question: prompt.text });
      await expect(page.getByLabel('Cuộc trò chuyện').locator('div.flex.gap-3.justify-start').last()).toContainText(prompt.expected);
    }
  } finally {
    await saveEvidence(page, testInfo, 'P0-A-TC-002');
  }
});

test('P0-B: Expert Free fills, edits, reviews and publishes one recipe through Chatbox', async ({ page }, testInfo) => {
  test.skip(!liveEnabled(), 'Live Gemini acceptance is opt-in.');
  test.setTimeout(240_000);
  page.on('request', request => {
    const url = new URL(request.url());
    if (url.pathname.startsWith('/api/v1/recipes/ai/chat')) console.log(`AI_HTTP_REQUEST ${request.method()} ${url.pathname}`);
  });
  page.on('response', response => {
    const url = new URL(response.url());
    if (url.pathname.startsWith('/api/v1/recipes/ai/chat')) console.log(`AI_HTTP_RESPONSE ${response.status()} ${url.pathname}`);
  });
  await page.context().tracing.start({ screenshots: true, snapshots: true, sources: true });
  try {
    const account = await login(page, 'demo-expert-02@mamxanh.local');
    expect(account.role).toBe('EXPERT');
    await page.goto('/trang-chu');
    const subscriptionResponse = await authenticatedApiGet(page, '/subscriptions/my-subscription');
    expect(subscriptionResponse.status()).toBe(200);
    expect(await subscriptionResponse.json()).toMatchObject({ tier: 'FREE' });
    await page.goto('/dang-cong-thuc');
    await expect(page.getByRole('heading', { name: 'Đăng công thức món chay mới' })).toBeVisible();
    await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
    const modelButton = page.getByRole('button', { name: /Gemini 3\.(?:5 Flash-Lite|8 Flash)/ });
    if (!(await modelButton.innerText()).includes('3.8')) {
      await modelButton.click();
      await page.getByRole('option').filter({ hasText: 'gemini-3.8-flash' }).click();
    }
    await expect(modelButton).toContainText('Gemini 3.8 Flash');
    await page.getByRole('button', { name: 'Chế độ AI: Chatbot' }).click();
    await page.getByRole('radio', { name: /AI Agent/ }).click();

    const runSuffix = Date.now().toString(36);
    const title = `Bánh khoai tím kiểm thử AI ${runSuffix}`;
    const finalTitle = `Bánh khoai tím dẻo kiểm thử AI ${runSuffix}`;
    const prompts = [
      `Hãy điền mẫu công thức mới với tên “${title}”, mô tả “Món khoai tím thuần chay hấp mềm cho đợt kiểm thử”, hướng dẫn “Nghiền khoai tím đã hấp chín, trộn bột năng và hấp thêm 20 phút đến khi bánh trong.” Chọn món hấp, thuần chay, độ khó dễ, khẩu phần 2 người, chuẩn bị 10 phút và nấu 20 phút. Thêm nguyên liệu bột năng 100 g.`,
      `Bot năng này, sửa tên thành “${finalTitle}” và rút mô tả còn “Bánh khoai tím thuần chay dẻo mềm.”`,
      'Chỉ sửa trường hướng dẫn chế biến: thêm câu “Để bánh nghỉ 5 phút trước khi cắt.” vào cuối nội dung hiện tại. Giữ nguyên mọi trường khác.',
    ];
    for (const prompt of prompts) {
      const request = await askInChat(page, prompt);
      expect(request).toMatchObject({ contextType: 'GENERAL', executionMode: 'AGENT', formContext: { formId: expect.any(String), revision: expect.any(Number) } });
      await expect(page.getByText('Đã cập nhật các trường được đề xuất trong biểu mẫu.').last()).toBeVisible({ timeout: 15_000 });
    }

    await expect(page.getByLabel('Tên món *')).toHaveValue(finalTitle);
    await expect(page.getByRole('textbox', { name: 'Mô tả' })).toHaveValue('Bánh khoai tím thuần chay dẻo mềm.');
    await expect(page.getByLabel('Hướng dẫn * (10–5.000 ký tự)')).toHaveValue(/để bánh nghỉ 5 phút/i);
    await expect(page.getByLabel('Chọn nguyên liệu 1')).toHaveValue(/bột năng/i);

    const publishRequest = await askInChat(page, 'Hãy đăng công thức hiện tại của tôi. Cho tôi xem đầy đủ nội dung sẽ đăng để xác nhận trước.');
    expect(publishRequest).toMatchObject({ contextType: 'GENERAL', formContext: { formId: expect.any(String) } });
    const review = page.getByRole('region', { name: 'Nội dung công thức sẽ đăng' });
    await expect(review).toContainText(finalTitle);
    await expect(review).toContainText('Bánh khoai tím thuần chay dẻo mềm.');
    await page.getByRole('radio', { name: 'Xác nhận thay đổi' }).check();
    const reviewCard = page.getByRole('heading', { name: 'Xem lại trước khi thực hiện' }).locator('xpath=ancestor::form');
    await reviewCard.getByRole('button', { name: 'Gửi', exact: true }).click();
    await expect(page.getByText('Đã công khai bài công thức.', { exact: false }).last()).toBeVisible({ timeout: 30_000 });
  } finally {
    await saveEvidence(page, testInfo, 'P0-B-Expert-Free-publish');
  }
});

test('P0-C: eligible Customer Free creates two ADK previews and applies the confirmed latest menu', async ({ page }, testInfo) => {
  test.skip(!liveEnabled(), 'Live Gemini acceptance is opt-in.');
  test.setTimeout(210_000);
  const customerEmail = process.env.MAMXANH_AI_AGENT_CUSTOMER_EMAIL;
  if (!customerEmail) throw new Error('MAMXANH_AI_AGENT_CUSTOMER_EMAIL must identify an existing eligible Customer Free test account.');

  let legacyPreviewCalls = 0;
  let legacyApplyCalls = 0;
  page.on('request', request => {
    const pathname = new URL(request.url()).pathname;
    if (pathname === '/api/v1/meal-plans/ai-menu/previews') legacyPreviewCalls++;
    if (pathname === '/api/v1/meal-plans/ai-menu') legacyApplyCalls++;
    if (pathname.startsWith('/api/v1/recipes/ai/chat')) console.log(`AI_HTTP_REQUEST ${request.method()} ${pathname}`);
  });
  page.on('response', response => {
    const pathname = new URL(response.url()).pathname;
    if (pathname.startsWith('/api/v1/recipes/ai/chat')) console.log(`AI_HTTP_RESPONSE ${response.status()} ${pathname}`);
  });
  await page.context().tracing.start({ screenshots: true, snapshots: true, sources: true });

  try {
    const account = await login(page, customerEmail);
    expect(account.role).toBe('CUSTOMER');
    await page.goto('/trang-chu');
    const subscriptionResponse = await authenticatedApiGet(page, '/subscriptions/my-subscription');
    expect(subscriptionResponse.status()).toBe(200);
    expect(await subscriptionResponse.json()).toMatchObject({ tier: 'FREE' });
    const nutritionResponse = await authenticatedApiGet(page, '/nutrition/profile');
    expect(nutritionResponse.status()).toBe(200);
    expect(await nutritionResponse.json()).toMatchObject({ hasProfile: true, eligible: true });

    const weekStartDate = mealPlanTestWeekStartDate();
    const mealPlanBefore = await readMealPlanWeek(page, weekStartDate);
    expect(mealPlanBefore.entries, 'Use a dedicated test week with no existing meals; apply replaces future entries.').toHaveLength(0);
    await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
    await page.getByRole('button', { name: 'Chế độ AI: Chatbot' }).click();
    await page.getByRole('radio', { name: /AI Agent/ }).click();

    const prompts = [
      `Tạo bản xem trước thực đơn chay theo hồ sơ dinh dưỡng của tôi cho tuần bắt đầu ${weekStartDate}; chỉ xem trước, chưa lưu vào Lịch ăn.`,
      'Tạo lại thực đơn tuần đó giúp mình nhé: tui muốn nhiều món đậu hủ, đừng chọn món có nấm; chỉ xem trước thôi.',
    ];
    for (const [index, prompt] of prompts.entries()) {
      await askInChat(page, prompt);
      await grantPendingDataConsents(page);
      await expect(page.getByRole('region', { name: 'Xem trước thực đơn AI' })).toHaveCount(index + 1, { timeout: 90_000 });
      await expect(page.getByRole('region', { name: 'Xem trước thực đơn AI' }).last())
        .toContainText('Đây là bản xem trước; chưa lưu vào lịch ăn.');
      await expect(page.getByText('Đã tạo bản xem trước thực đơn; chưa lưu vào lịch ăn.', { exact: true })).toHaveCount(index + 1);
      expect(await readMealPlanWeek(page, weekStartDate)).toEqual(mealPlanBefore);
    }

    const preview = page.getByRole('region', { name: 'Xem trước thực đơn AI' }).last();
    const previewItems = await preview.locator('li').allTextContents();
    expect(previewItems.length).toBeGreaterThan(0);
    await askInChat(page, 'Bản xem trước mới nhất ổn rồi, hãy áp dụng bản đó vào Lịch ăn của mình.');
    const confirmation = page.getByRole('heading', { name: 'Xem lại trước khi thực hiện' }).locator('xpath=ancestor::form');
    await expect(confirmation).toBeVisible();
    await confirmation.getByText('Xác nhận thay đổi', { exact: true }).click();
    await confirmation.getByRole('button', { name: 'Gửi', exact: true }).click();
    await expect(page.getByText('Đã áp dụng thực đơn được xác nhận vào lịch ăn.')).toBeVisible({ timeout: 30_000 });
    expect(legacyPreviewCalls).toBe(0);
    expect(legacyApplyCalls).toBe(0);

    const appliedWeek = await readMealPlanWeek(page, weekStartDate);
    const mealTypeByLabel: Record<string, string> = { Sáng: 'BREAKFAST', Trưa: 'LUNCH', Tối: 'DINNER' };
    for (const item of previewItems) {
      const match = item.match(/^(\d{4}-\d{2}-\d{2}) · (Sáng|Trưa|Tối): (.+)$/);
      expect(match, `Unexpected preview row: ${item}`).not.toBeNull();
      const [, mealDate, mealLabel, recipeTitle] = match!;
      expect(appliedWeek.entries.filter(entry => entry.mealDate === mealDate && entry.mealType === mealTypeByLabel[mealLabel]))
        .toContainEqual(expect.objectContaining({ recipeTitle }));
    }
  } finally {
    await saveEvidence(page, testInfo, 'P0-C-Customer-Free-preview-apply');
  }
});
