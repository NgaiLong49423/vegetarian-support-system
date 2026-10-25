import type { Page, Route } from '@playwright/test';
import { expect, test } from './baseFixtures';

const recipeId = 901;
const detail = {
  recipeId,
  title: 'Canh nấm thanh mát',
  description: 'Món chay cho bữa cơm gia đình.',
  instructions: 'Nấu nấm với nước dùng.',
  author: { userId: 22, displayName: 'Bếp Chay Lan', avatarUrl: null },
  dishCategory: 'SOUP',
  dishCategoryLabel: 'Món canh',
  vegetarianType: 'VEGAN',
  vegetarianTypeLabel: 'Thuần chay',
  difficulty: 'EASY',
  difficultyLabel: 'Dễ',
  servings: 2,
  prepTimeMinutes: 10,
  cookTimeMinutes: 15,
  youtubeUrl: null,
  publishedAt: '2026-10-11T08:00:00',
  nutritionComplete: true,
  ingredientsWithoutNutrition: [],
  ingredients: [],
  media: [],
  statistics: { likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null, viewCount: 0 },
};

const reasons = [
  'Sai thông tin công thức hoặc nhãn ăn chay',
  'Nguy cơ an toàn thực phẩm',
  'Spam / Quảng cáo không phù hợp',
  'Nội dung xúc phạm / Không phù hợp',
  'Sao chép / Nghi vi phạm bản quyền',
  'Khác',
];

async function memberSession(page: Page) {
  await page.addInitScript(() => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
    accessToken: 'browser-test-token',
    expiresAt: Date.now() + 3600000,
    account: { id: 33, displayName: 'Thành viên', email: 'member@example.test', role: 'CUSTOMER' },
  })));
}

async function installRecipeApi(page: Page) {
  await page.route(`**/api/v1/recipes/${recipeId}`, (route) => route.fulfill({ json: detail }));
  await page.route(`**/api/v1/recipes/${recipeId}/views`, (route) => route.fulfill({ json: { recipeId, counted: true, viewCount: 1 } }));
  await page.route(`**/api/v1/recipes/${recipeId}/reaction`, (route) => route.fulfill({ json: { currentReaction: null, likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null } }));
  await page.route(`**/api/v1/recipes/${recipeId}/comments**`, (route) => route.fulfill({ json: { content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, commentCount: 0 } }));
  await page.route(`**/api/v1/recipes/${recipeId}/related**`, (route) => route.fulfill({ json: { items: [] } }));
  await page.route('**/api/v1/recipes/discovery**', (route) => route.fulfill({ json: { items: [], page: 0, size: 12, totalElements: 0, totalPages: 0, hasNext: false } }));
  await page.route('**/api/v1/saved-recipes**', (route) => route.fulfill({ json: [] }));
}

async function openReport(page: Page) {
  await page.getByRole('button', { name: 'Báo cáo công thức' }).click();
  return page.getByRole('dialog', { name: 'Báo cáo công thức' });
}

test('Guest được yêu cầu đăng nhập và không mở form hoặc gọi API gửi báo cáo', async ({ page }) => {
  await installRecipeApi(page);
  let submissions = 0;
  await page.route(`**/api/v1/recipes/${recipeId}/reports`, (route) => { submissions++; return route.fulfill({ status: 201, json: {} }); });

  await page.goto(`/cong-thuc/id/${recipeId}`);
  await page.getByRole('button', { name: 'Báo cáo công thức' }).click();

  await expect(page.getByRole('dialog', { name: 'Yêu cầu đăng nhập' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Đăng nhập', exact: true }).last()).toHaveAttribute('href', '/dang-nhap');
  await expect(page.getByRole('dialog', { name: 'Báo cáo công thức' })).toHaveCount(0);
  expect(submissions).toBe(0);
});

test('Member gửi báo cáo với taxonomy hiện hành, thành công và giữ trang công thức', async ({ page }) => {
  await memberSession(page);
  await installRecipeApi(page);
  const submittedBodies: unknown[] = [];
  await page.route(`**/api/v1/recipes/${recipeId}/reports`, async (route) => {
    submittedBodies.push(route.request().postDataJSON());
    expect(route.request().headers().authorization).toBe('Bearer browser-test-token');
    await route.fulfill({ status: 201, json: { reportId: 123, status: 'OPEN', message: 'Báo cáo đã được tiếp nhận.' } });
  });

  await page.goto(`/cong-thuc/id/${recipeId}`);
  for (const [index, reason] of reasons.entries()) {
    const dialog = await openReport(page);
    for (const visibleReason of reasons) await expect(dialog.getByText(visibleReason, { exact: true })).toBeVisible();
    await dialog.getByRole('radio', { name: reason }).check();
    if (index === reasons.length - 1) {
      await dialog.getByLabel('Mô tả bổ sung (bắt buộc, 10–500 ký tự)').fill('x'.repeat(500));
      await expect(dialog.getByLabel('Mô tả bổ sung (bắt buộc, 10–500 ký tự)')).toHaveValue('x'.repeat(500));
    }
    await dialog.getByRole('button', { name: 'Gửi báo cáo' }).click();
    await expect(dialog).toHaveCount(0);
    await expect(page.getByRole('status').filter({ hasText: 'Báo cáo đã được tiếp nhận' })).toBeVisible();
  }
  await expect(page.getByRole('heading', { name: detail.title })).toBeVisible();
  expect(submittedBodies).toEqual([
    { reasonCode: 'RECIPE_INFO_OR_DIET_LABEL' },
    { reasonCode: 'FOOD_SAFETY_HAZARD' },
    { reasonCode: 'SPAM_ADVERTISING' },
    { reasonCode: 'INAPPROPRIATE_CONTENT' },
    { reasonCode: 'COPYRIGHT_VIOLATION' },
    { reasonCode: 'OTHER', description: 'x'.repeat(500) },
  ]);
});

test('OTHER yêu cầu 10 ký tự sau trim và cho gửi đúng 10 ký tự', async ({ page }) => {
  await memberSession(page);
  await installRecipeApi(page);
  let submissions = 0;
  await page.route(`**/api/v1/recipes/${recipeId}/reports`, async (route) => {
    submissions++;
    await route.fulfill({ status: 201, json: { reportId: 123, status: 'OPEN', message: 'Đã nhận' } });
  });

  await page.goto(`/cong-thuc/id/${recipeId}`);
  const dialog = await openReport(page);
  await dialog.getByRole('radio', { name: 'Khác' }).check();
  await dialog.getByRole('button', { name: 'Gửi báo cáo' }).click();
  await expect(dialog.getByRole('alert')).toContainText('10 đến 500 ký tự');
  expect(submissions).toBe(0);

  await dialog.getByLabel('Mô tả bổ sung (bắt buộc, 10–500 ký tự)').fill('  1234567890  ');
  await dialog.getByRole('button', { name: 'Gửi báo cáo' }).click();
  await expect(page.getByRole('status').filter({ hasText: 'Báo cáo đã được tiếp nhận' })).toBeVisible();
  expect(submissions).toBe(1);
});

test('REPORT_ALREADY_OPEN, lỗi dịch vụ và mất mạng hiện lỗi, không báo thành công', async ({ page }) => {
  await memberSession(page);
  await installRecipeApi(page);
  let response: 'duplicate' | 'server' | 'network' = 'duplicate';
  await page.route(`**/api/v1/recipes/${recipeId}/reports`, async (route: Route) => {
    if (response === 'network') return route.abort();
    if (response === 'duplicate') return route.fulfill({ status: 409, contentType: 'application/problem+json', json: { status: 409, code: 'REPORT_ALREADY_OPEN', detail: 'Đã tồn tại báo cáo.' } });
    return route.fulfill({ status: 503, contentType: 'application/problem+json', json: { status: 503, code: 'SERVICE_UNAVAILABLE', detail: 'Tạm ngưng.' } });
  });

  await page.goto(`/cong-thuc/id/${recipeId}`);
  const dialog = await openReport(page);
  const send = async () => {
    await dialog.getByRole('radio', { name: reasons[1] }).check();
    await dialog.getByRole('button', { name: 'Gửi báo cáo' }).click();
  };
  await send();
  await expect(dialog.getByRole('alert')).toContainText('đang được xem xét');
  await expect(page.getByText('Báo cáo đã được tiếp nhận.')).toHaveCount(0);

  response = 'server';
  await send();
  await expect(dialog.getByRole('alert')).toContainText('Dịch vụ đang gặp sự cố');

  response = 'network';
  await send();
  await expect(dialog.getByRole('alert')).toContainText('Không thể kết nối dịch vụ');
  await expect(page.getByRole('heading', { name: detail.title })).toBeVisible();
});

test('Trong khi gửi, gửi lặp bị khóa', async ({ page }) => {
  await memberSession(page);
  await installRecipeApi(page);
  let submissions = 0;
  await page.route(`**/api/v1/recipes/${recipeId}/reports`, async (route) => {
    submissions++;
    await new Promise((resolve) => setTimeout(resolve, 250));
    await route.fulfill({ status: 201, json: { reportId: 123, status: 'OPEN', message: 'Đã nhận' } });
  });

  await page.goto(`/cong-thuc/id/${recipeId}`);
  const dialog = await openReport(page);
  await dialog.getByRole('radio', { name: reasons[0] }).check();
  const send = dialog.getByRole('button', { name: 'Gửi báo cáo' });
  await send.click();
  const sending = dialog.getByRole('button', { name: 'Đang gửi…' });
  await expect(sending).toBeDisabled();
  await expect(sending).toHaveText('Đang gửi…');
  await expect(dialog).toHaveCount(0);
  expect(submissions).toBe(1);
});
