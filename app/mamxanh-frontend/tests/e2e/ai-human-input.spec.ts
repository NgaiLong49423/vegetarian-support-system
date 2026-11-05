import { expect, test } from './baseFixtures';

const sessionId = '44444444-4444-4444-8444-444444444444';
const confirmationId = '55555555-5555-4555-8555-555555555555';

async function member(page: import('@playwright/test').Page) {
  await page.addInitScript(() => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
    accessToken: 'playwright-member-token', expiresAt: Date.now() + 3_600_000,
    account: { id: 7, displayName: 'Member test', email: 'member@example.test', role: 'CUSTOMER', avatarUrl: null },
  })));
}

test('Codex-style confirmation requires a selection and Send; mobile layout stays within the viewport', async ({ page }) => {
  await member(page);
  const requests: Array<Record<string, unknown>> = [];
  await page.route('**/api/v1/recipes/ai/chat', async route => {
    const body = route.request().postDataJSON(); requests.push(body);
    await route.fulfill({ json: requests.length === 1 ? {
      contextType: 'GENERAL', recipeId: null, sessionId, executionMode: 'CONFIRM',
      answer: 'Mình đã chuẩn bị danh sách mua sắm cho bạn. Bạn kiểm tra trước khi tạo nhé.',
      steps: [{ sequence: 1, tool: 'create_shopping_list', status: 'AWAITING_CONFIRMATION', summary: 'Cần xác nhận trước khi thay đổi dữ liệu.', result: { preview: { page: 'SHOPPING_LIST' } } }],
      pendingConfirmation: { confirmationId, summary: 'Tạo một danh sách mua sắm rỗng cho chuyến đi chợ cuối tuần', proposedListName: 'Đi chợ cuối tuần', requiresExplicitConfirmation: true },
    } : { contextType: 'GENERAL', recipeId: null, sessionId, answer: 'Đã tạo danh sách sau khi bạn xác nhận.' } });
  });
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Tạo danh sách mua sắm Đi chợ cuối tuần');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  const card = page.getByRole('form', { name: 'Xem lại trước khi thực hiện' });
  await expect(card).toBeVisible();
  await expect(card.getByRole('button', { name: 'Gửi', exact: true })).toBeDisabled();
  await expect(card.getByRole('radio', { name: 'Xác nhận thay đổi', exact: true })).not.toBeChecked();
  await expect(page.getByText('SHOPPING_LIST', { exact: true })).toHaveCount(0);
  await card.getByRole('radio', { name: 'Xác nhận thay đổi', exact: true }).check();
  expect(requests).toHaveLength(1);
  await page.mouse.move(0, 0);
  // Visual capture only: let the app's click-spark canvas finish before taking a preview.
  await page.waitForTimeout(600);
  await card.screenshot({ path: test.info().outputPath('confirmation-desktop.png'), animations: 'disabled', caret: 'hide' });
  await page.setViewportSize({ width: 360, height: 800 });
  const bounds = await card.boundingBox();
  expect(bounds!.x).toBeGreaterThanOrEqual(0);
  expect(bounds!.x + bounds!.width).toBeLessThanOrEqual(360);
  await card.screenshot({ path: test.info().outputPath('confirmation-mobile.png'), animations: 'disabled', caret: 'hide' });
  await card.getByRole('button', { name: 'Gửi', exact: true }).click();
  await expect(page.getByText('Đã tạo danh sách sau khi bạn xác nhận.')).toBeVisible();
  expect(requests[1]).toMatchObject({ confirmationId, confirmationDecision: 'APPROVE' });
});

test('free text rejects the old pending action before sending a new instruction, never approves it', async ({ page }) => {
  await member(page);
  const requests: Array<Record<string, unknown>> = [];
  await page.route('**/api/v1/recipes/ai/chat', async route => {
    const body = route.request().postDataJSON(); requests.push(body);
    await route.fulfill({ json: requests.length === 1 ? {
      contextType: 'GENERAL', recipeId: null, sessionId, answer: 'Bạn muốn tạo danh sách này không?',
      pendingConfirmation: { confirmationId, summary: 'Tạo danh sách Đi chợ cuối tuần', requiresExplicitConfirmation: true },
    } : { contextType: 'GENERAL', recipeId: null, sessionId, answer: requests.length === 2 ? 'Đã hủy đề xuất cũ.' : 'Đã nhận yêu cầu chỉnh sửa.' } });
  });
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Tạo danh sách Đi chợ cuối tuần');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await page.getByRole('radio', { name: 'Xác nhận thay đổi', exact: true }).check();
  await page.getByLabel('Hoặc viết yêu cầu khác').fill('Đổi tên thành Đi chợ ngày mai');
  await expect(page.getByRole('radio', { name: 'Xác nhận thay đổi', exact: true })).not.toBeChecked();
  await page.getByRole('button', { name: 'Gửi', exact: true }).click();
  await expect(page.getByText('Đã nhận yêu cầu chỉnh sửa.')).toBeVisible();
  expect(requests).toHaveLength(3);
  expect(requests[1]).toMatchObject({ confirmationId, confirmationDecision: 'REJECT' });
  expect(requests[2]).toMatchObject({ question: 'Đổi tên thành Đi chợ ngày mai' });
  expect(requests[2].confirmationDecision).toBeUndefined();
  expect(requests[2].consentDecision).toBeUndefined();
  const ids = await page.evaluate(() => JSON.parse(sessionStorage.getItem('mamxanh.ai-chat.session.v1.member-7')!).messages.map((m: { id: number }) => m.id));
  expect(new Set(ids).size).toBe(ids.length);
});

test('skipping consent restores the composer without sending a grant', async ({ page }) => {
  await member(page);
  let calls = 0;
  await page.route('**/api/v1/recipes/ai/chat', async route => {
    calls += 1;
    await route.fulfill({ json: { contextType: 'GENERAL', recipeId: null, sessionId, answer: 'Cần sự đồng ý trước khi dùng hồ sơ.',
      consentRequest: { recipient: 'Google Gemini', scopes: [{ category: 'BASIC_PROFILE', purpose: 'AI_CHAT' }] } } });
  });
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Xem hồ sơ của tôi');
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  await expect(page.getByRole('heading', { name: 'Cho phép dùng dữ liệu cá nhân?' })).toBeVisible();
  await page.getByRole('button', { name: 'Bỏ qua', exact: true }).click();
  await expect(page.getByLabel('Câu hỏi của bạn')).toBeEnabled();
  expect(calls).toBe(1);
});
