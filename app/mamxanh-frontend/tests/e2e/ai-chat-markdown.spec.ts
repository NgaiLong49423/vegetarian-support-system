import { test, expect, type Page } from '@playwright/test';

async function answer(page: Page, content: string) {
  await page.route('**/api/v1/recipes/ai/chat', route => route.fulfill({ json: {
    contextType: 'GENERAL', recipeId: null, answer: content, recommendations: [],
  } }));
  await page.goto('/trang-chu');
  await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
  await page.getByLabel('Câu hỏi của bạn').fill('Hướng dẫn món chay');
  await page.getByRole('button', { name: 'Gửi câu hỏi', exact: true }).click();
  const prose = page.locator('.ai-message-markdown').last();
  await expect(prose).toBeVisible();
  return prose;
}

test('assistant renders structured Vietnamese Markdown with readable blocks and safe links', async ({ page }) => {
  const prose = await answer(page, '### Bữa tối thanh đạm\n\nĐoạn một: **Đậu phụ** và *nấm rơm* 🥬.\nDòng tiếp theo.\n\nĐoạn hai: ăn ngon nhé!\n\n- **Món chính:** Đậu phụ sốt nấm\n- Canh rong biển\n  - Thêm gừng\n\n1. Sơ chế\n2. Nấu nhỏ lửa\n\n[Xem công thức](/kham-pha) và [Nguồn](https://example.com/huong-dan).');
  await expect(prose.getByRole('heading', { level: 3 })).toHaveText('Bữa tối thanh đạm');
  await expect(prose.locator('strong').first()).toHaveText('Đậu phụ');
  await expect(prose.locator('em')).toHaveText('nấm rơm');
  await expect(prose.locator('ul > li')).toHaveCount(3);
  await expect(prose.locator('ol > li')).toHaveCount(2);
  await expect(prose.locator('br')).toHaveCount(1);
  await expect(prose.locator('p').filter({ hasText: 'Đoạn hai' })).toHaveCount(1);
  await expect(prose).toContainText('🥬');
  await expect(prose.getByRole('link', { name: 'Xem công thức' })).toHaveAttribute('href', '/kham-pha');
  await expect(prose.getByRole('link', { name: 'Nguồn' })).toHaveAttribute('rel', 'noopener noreferrer');
  expect(await prose.locator('ul').first().evaluate(el => getComputedStyle(el).listStyleType)).toBe('disc');
  expect(await prose.locator('p').first().evaluate(el => parseFloat(getComputedStyle(el).marginBottom))).toBeGreaterThan(0);
});

test('HTML, executable URLs and embedded tracking images never become active content', async ({ page }) => {
  let trackingRequests = 0;
  await page.route('https://evil.invalid/**', route => { trackingRequests++; return route.abort(); });
  const prose = await answer(page, '**Nội dung an toàn**\n\n<script>document.documentElement.dataset.markdownXss="yes"</script>\n\n<img src="x" onerror="document.documentElement.dataset.markdownXss=1">\n\n<iframe src="https://evil.invalid/frame"></iframe>\n\n[Nguy hiểm](javascript:alert%281%29)\n\n[Data](data:text/html,hello)\n\n![Theo dõi](https://evil.invalid/track)\n\n[Kết thúc](https://example.com)');
  await expect(prose.locator('script,img,iframe,svg,form,input')).toHaveCount(0);
  await expect(prose.locator('a[href^="javascript:"],a[href^="data:"]')).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.dataset.markdownXss)).toBeUndefined();
  expect(trackingRequests).toBe(0);
  await expect(prose.getByRole('link', { name: 'Kết thúc' })).toBeVisible();
});

for (const content of ['**Nội dung chưa đóng', '[Liên kết dang dở](https://example.com', '```\nĐậu phụ\nTiếng Việt chưa đóng code']) {
  test(`incomplete Markdown remains readable: ${content.slice(0, 25)}`, async ({ page }) => {
    const prose = await answer(page, content);
    await expect(prose).toContainText(content.includes('Đậu phụ') ? 'Đậu phụ' : content.includes('Liên kết') ? 'Liên kết dang dở' : 'Nội dung chưa đóng');
    await expect(page.getByLabel('Câu hỏi của bạn')).toBeEnabled();
  });
}

test('long Unicode prose wraps inside a mobile chat without horizontal overflow', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 800 });
  const prose = await answer(page, '### Gợi ý\n\n' + 'Nguyên liệu tiếng Việt 🥬 '.repeat(60) + '\n\n' + 'đậuphụ'.repeat(100));
  expect(await prose.evaluate(el => el.scrollWidth <= el.clientWidth + 1)).toBe(true);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
});

test('user messages retain their literal Markdown and interaction stays enabled', async ({ page }) => {
  await answer(page, '**Mình đã nhận câu hỏi.**');
  await page.getByLabel('Câu hỏi của bạn').fill('**Tên tôi nhập**');
  await page.getByRole('button', { name: 'Gửi câu hỏi', exact: true }).click();
  await expect(page.getByText('**Tên tôi nhập**', { exact: true })).toBeVisible();
  await expect(page.locator('.ai-message-markdown').last().locator('strong')).toHaveText('Mình đã nhận câu hỏi.');
});
