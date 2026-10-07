import { expect, test } from './baseFixtures';

const recipePage = {
  items: [{
    id: 701, authorId: 22, authorName: 'Chuyên gia demo', authorAvatarUrl: null,
    title: 'Đậu hũ kho tiêu API', description: 'Dữ liệu trả về từ API.',
    instructions: 'Kho đậu hũ với tiêu đến khi thấm gia vị.', dishCategory: 'BRAISED',
    vegetarianType: 'VEGAN', difficulty: 'EASY', servings: 2, prepTimeMin: 10,
    cookTimeMin: 15, youtubeUrl: null, status: 'PUBLISHED', media: [], ingredients: [],
  }], page: 0, size: 12, totalElements: 1, totalPages: 1,
};

test('local demo login creates a real API session and renders the Backend account', async ({ page }) => {
  await page.route('**/api/v1/auth/login', (route) => route.fulfill({ json: {
    accessToken: 'header.payload.signature', tokenType: 'Bearer', expiresInSeconds: 3600,
    account: { id: 15, displayName: 'Member từ Backend', email: 'demo-customer@mamxanh.local', role: 'CUSTOMER', accountStatus: 'ACTIVE', emailVerified: true },
  } }));
  await page.route('**/api/v1/nutrition/dietary-preferences/onboarding/invitation', (route) => route.fulfill({ json: { show: false } }));
  await page.route('**/api/v1/recipes?**', (route) => route.fulfill({ json: { ...recipePage, items: [] } }));

  await page.goto('/dang-nhap');
  await page.getByLabel('Email', { exact: true }).fill('demo-customer@mamxanh.local');
  await page.getByLabel('Mật khẩu', { exact: true }).fill('local-demo-password');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
  await expect(page.getByRole('button', { name: /Member từ Backend/ })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Khám phá tài khoản demo' })).toHaveCount(0);
  expect(await page.evaluate(() => sessionStorage.getItem('mamxanh.auth'))).toContain('demo-customer@mamxanh.local');
});

test('recipe exploration shows API results only and handles empty, loading and failure states', async ({ page }) => {
  let requestCount = 0;
  let releaseInitialResponse!: () => void;
  const initialResponsePaused = new Promise<void>((resolve) => { releaseInitialResponse = resolve; });
  await page.route('**/api/v1/recipes?**', async (route) => {
    requestCount += 1;
    const keyword = new URL(route.request().url()).searchParams.get('keyword');
    if (!keyword) await initialResponsePaused;
    if (keyword === 'api-error') return route.fulfill({ status: 503, json: { title: 'Unavailable' } });
    if (keyword === 'empty') return route.fulfill({ json: { ...recipePage, items: [], totalElements: 0, totalPages: 0 } });
    return route.fulfill({ json: recipePage });
  });

  await page.goto('/kham-pha');
  await expect(page.getByRole('status')).toContainText('Đang tải công thức');
  releaseInitialResponse();
  await expect(page.getByRole('heading', { name: 'Đậu hũ kho tiêu API' })).toBeVisible();
  await expect(page.getByText('Đậu hũ sốt cà chua', { exact: true })).toHaveCount(0);
  await page.getByPlaceholder('Tìm món chay hoặc nguyên liệu...').fill('empty');
  await expect(page.getByRole('heading', { name: 'Chưa có công thức phù hợp' })).toBeVisible();
  await page.getByPlaceholder('Tìm món chay hoặc nguyên liệu...').fill('api-error');
  await expect(page.getByRole('heading', { name: 'Không tải được công thức' })).toBeVisible();
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect.poll(() => requestCount).toBeGreaterThanOrEqual(4);
  await expect(page.getByRole('heading', { name: 'Không tải được công thức' })).toBeVisible();
});
