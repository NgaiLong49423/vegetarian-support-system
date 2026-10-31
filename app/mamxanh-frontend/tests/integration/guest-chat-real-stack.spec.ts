import { randomUUID } from 'node:crypto';
import { expect, test } from '@playwright/test';

const backend = process.env.MAMXANX_E2E_BACKEND_URL ?? 'http://localhost:8080/api/v1';

test('FR-02: real Backend blocks the fourth Guest request before provider access and UI shows registration', async ({ page, context, request, baseURL }) => {
  const id = randomUUID();
  await context.addCookies([{ name: 'mamxanh.guest-chat', value: id, url: baseURL! }]);
  // A missing public RecipePost is rejected by the real SQL query before calling Gemini.
  // Valid attempts still consume the technical rate window, including failed context lookups.
  for (let i = 0; i < 3; i++) {
    const response = await request.post(`${backend}/recipes/ai/chat`, {
      headers: { 'X-Guest-Chat-Id': id },
      data: { contextType: 'RECIPE', recipeId: 9_000_000_000, question: 'Cách nấu?' },
    });
    expect(response.status()).toBe(404);
  }
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Cách nấu đậu?');
  const blocked = page.waitForResponse((response) => response.url() === `${backend}/recipes/ai/chat`);
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  const response = await blocked;
  expect(response.status()).toBe(429);
  expect(['AI_GUEST_RATE_LIMITED', 'GUEST_CHAT_RATE_LIMITED']).toContain((await response.json()).code);
  expect(Number(response.headers()['retry-after'])).toBeGreaterThan(0);
  await expect(page.getByRole('dialog').getByRole('alert')).toContainText('Thử lại sau');
  await expect(page.getByRole('dialog').getByRole('button', { name: 'Thử lại', exact: true })).toBeDisabled();
  await expect(page.getByRole('link', { name: 'Đăng ký tài khoản', exact: true })).toHaveAttribute('href', '/dang-ky');
});

test('FR-02: live Guest uses general and public-recipe contexts', async ({ page, request }) => {
  test.skip(process.env.MAMXANH_E2E_LIVE_AI !== '1', 'Explicit opt-in required for live Gemini evidence.');
  const recipes = await request.get(`${backend}/recipes`);
  expect(recipes.status()).toBe(200);
  const catalog = await recipes.json();
  const recipeId = catalog.items[0].id as number;
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Luộc rau xanh thế nào? Trả lời ngắn trong một câu.');
  let received = page.waitForResponse((response) => response.url() === `${backend}/recipes/ai/chat`);
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  let response = await received;
  expect(response.status()).toBe(200);
  expect((await response.json()).answer.trim()).not.toBe('');
  await expect(page.getByRole('link', { name: 'Đăng ký tài khoản', exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Đóng khung chat' }).click();
  await page.goto(`/cong-thuc/id/${recipeId}`);
  await page.getByRole('button', { name: 'Hỏi AI về công thức này' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Bài này ghi thời gian nấu bao lâu? Trả lời ngắn trong một câu.');
  received = page.waitForResponse((result) => result.url() === `${backend}/recipes/ai/chat`);
  await page.getByRole('button', { name: 'Gửi câu hỏi' }).click();
  response = await received;
  expect(response.status()).toBe(200);
  const answer = await response.json();
  expect(answer.recipeId).toBe(recipeId);
  expect(answer.answer.trim()).not.toBe('');
});
