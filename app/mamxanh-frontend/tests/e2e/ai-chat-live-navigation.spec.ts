import { expect, test } from './baseFixtures';

const liveEnabled = process.env.MAMXANH_LIVE_AI_TEST === 'true';

test('live Gemini + ADK finds a public recipe and opens its rendered detail page', async ({ page }, testInfo) => {
  test.setTimeout(70_000); // The application enforces a 60-second absolute turn deadline.

  let selectedModel = 'unknown';
  page.on('response', (response) => {
    if (response.url().includes('/api/v1/ai/chat-settings')) {
      void response.json().then((settings: { selectedModelId?: string; models?: Array<{ modelId: string }> }) => {
        selectedModel = settings.selectedModelId ?? settings.models?.[0]?.modelId ?? 'unknown';
      }).catch(() => undefined);
    }
  });
  await page.goto('http://localhost:5173/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByRole('button', { name: 'Chế độ AI: Chatbot' }).click();
  await page.getByRole('radio', { name: /AI Agent/ }).click();
  await page.getByLabel('Câu hỏi của bạn').fill(
    'Tìm một công thức món chay công khai, dễ nấu trên Mâm Xanh rồi mở trang chi tiết của chính công thức đó.',
  );
  const startedAt = Date.now();
  const streamResponse = page.waitForResponse((response) =>
    response.url().includes('/api/v1/recipes/ai/chat/stream'),
  );
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  const response = await streamResponse;
  expect(response.ok()).toBeTruthy();

  await expect(page).toHaveURL(/\/cong-thuc\/id\/[1-9]\d*$/);
  await expect(page.getByRole('heading', { level: 1 }).first()).toBeVisible();
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await expect(page.getByText(/Đã mở Chi tiết công thức/)).toBeVisible();
  await expect(page.getByText(/1\. search public recipes\s+Hoàn tất/)).toBeVisible();
  await expect(page.getByText(/2\. navigate to page\s+Hoàn tất/)).toBeVisible();

  const totalMs = Date.now() - startedAt;
  const result = {
    model: selectedModel,
    chatStreamRequests: 1,
    totalMs,
    finalRoute: new URL(page.url()).pathname,
    renderedRecipeHeading: await page.getByRole('heading', { level: 1 }).first().innerText(),
    searchToolSucceeded: true,
    navigationToolSucceeded: true,
    chatConfirmationVerified: true,
  };
  console.log(`[BUG-107 LIVE EVIDENCE] ${JSON.stringify(result)}`);
  await testInfo.attach('bug-107-live-summary.json', {
    body: JSON.stringify(result, null, 2),
    contentType: 'application/json',
  });
});
