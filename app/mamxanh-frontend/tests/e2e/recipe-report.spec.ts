import type { Page, Route } from '@playwright/test';
import { expect, test } from './baseFixtures';

// FR-26/27 report form and FR-30 supplements on the real recipe page, against a mocked API.
// Duplicate prevention, limits and ownership are proven on SQL Server by the Backend tests.
const detail = {
  recipeId: 901, title: 'Canh nấm thanh mát', description: 'Món chay cho bữa cơm gia đình.',
  instructions: 'Nấu nấm với nước dùng.', author: { userId: 22, displayName: 'Bếp Chay Lan', avatarUrl: null },
  dishCategory: 'SOUP', dishCategoryLabel: 'Món canh', vegetarianType: 'VEGAN', vegetarianTypeLabel: 'Thuần chay',
  difficulty: 'EASY', difficultyLabel: 'Dễ', servings: 2, prepTimeMinutes: 10, cookTimeMinutes: 15,
  youtubeUrl: null, publishedAt: '2026-10-11T08:00:00', nutritionComplete: true, ingredientsWithoutNutrition: [],
  ingredients: [{ ingredientId: 1, name: 'Nấm', quantity: 100, unitId: 1, unitCode: 'g', unitName: 'gram' }], media: [],
  statistics: { likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null, viewCount: 0 },
};

const openReport = {
  reportId: 70, status: 'IN_REVIEW', reasonCode: 'SPAM_ADVERTISING', description: 'Có đường dẫn bán hàng trong bài',
  createdAt: '2026-10-24T02:00:00', recipe: { id: 901, title: detail.title }, supplementLimit: 5,
  supplements: [{ supplementId: 1, content: 'Ảnh bìa có logo cửa hàng', createdAt: '2026-10-24T03:00:00' }],
};

async function session(page: Page, role = 'CUSTOMER', id = 33) {
  await page.addInitScript(({ role, id }) => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
    accessToken: 'browser-test-token', expiresAt: Date.now() + 3600000,
    account: { id, displayName: 'Thành viên', email: 'member@example.test', role },
  })), { role, id });
}

type Api = {
  reportBodies: unknown[];
  supplementBodies: unknown[];
  submit: (route: Route) => Promise<void>;
  supplement: (route: Route) => Promise<void>;
};

const problem = (route: Route, status: number, body: object) =>
  route.fulfill({ status, contentType: 'application/problem+json', body: JSON.stringify({ status, ...body }) });

async function mockApi(page: Page): Promise<Api> {
  const report = structuredClone(openReport);
  const api: Api = {
    reportBodies: [],
    supplementBodies: [],
    submit: (route) => route.fulfill({ status: 201, json: { reportId: 71, status: 'OPEN', message: 'Báo cáo đã được tiếp nhận.' } }),
    supplement: async (route) => {
      const body = route.request().postDataJSON() as { content: string };
      const created = { supplementId: report.supplements.length + 1, content: body.content, createdAt: '2026-10-25T03:00:00' };
      report.supplements.push(created);
      await route.fulfill({ status: 201, json: created });
    },
  };
  await page.route('**/api/v1/recipes/901', (route) => route.fulfill({ json: detail }));
  await page.route('**/api/v1/recipes/901/related**', (route) => route.fulfill({ json: { items: [] } }));
  await page.route('**/api/v1/recipes/901/reaction', (route) =>
    route.fulfill({ json: { recipeId: 901, currentReaction: null, ...detail.statistics } }));
  await page.route('**/api/v1/recipes/901/comments**', (route) =>
    route.fulfill({ json: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, commentCount: 0 } }));
  await page.route('**/api/v1/recipes/901/views', (route) => route.fulfill({ status: 204, body: '' }));
  await page.route('**/api/v1/recipes/901/reports', async (route) => {
    api.reportBodies.push(route.request().postDataJSON());
    await api.submit(route);
  });
  await page.route('**/api/v1/me/reports/70', (route) => route.fulfill({ json: report }));
  await page.route('**/api/v1/me/reports/70/supplements', async (route) => {
    api.supplementBodies.push(route.request().postDataJSON());
    await api.supplement(route);
  });
  return api;
}

async function openDialog(page: Page) {
  await page.goto('/cong-thuc/id/901');
  await page.getByRole('button', { name: 'Báo cáo công thức' }).click();
  return page.getByRole('dialog', { name: 'Báo cáo công thức' });
}

test('Member sends a report with a reason and a trimmed description', async ({ page }) => {
  await session(page);
  const api = await mockApi(page);
  const dialog = await openDialog(page);

  await dialog.getByRole('radio', { name: 'Spam hoặc quảng cáo không phù hợp' }).check();
  await dialog.getByLabel('Mô tả thêm').fill('  Có đường dẫn bán hàng trong phần mô tả  ');
  await dialog.getByRole('button', { name: 'Gửi báo cáo' }).click();

  await expect(dialog.getByRole('status')).toHaveText('Đã gửi báo cáo. Quản trị viên sẽ xem xét nội dung này.');
  await expect(dialog.getByRole('button', { name: 'Gửi báo cáo' })).toHaveCount(0);
  expect(api.reportBodies).toEqual([{ reasonCode: 'SPAM_ADVERTISING', description: 'Có đường dẫn bán hàng trong phần mô tả' }]);
});

test('a reason is required and Other needs 10 to 500 characters before anything is sent', async ({ page }) => {
  await session(page);
  const api = await mockApi(page);
  const dialog = await openDialog(page);

  await dialog.getByRole('button', { name: 'Gửi báo cáo' }).click();
  await expect(dialog.getByText('Chọn một lý do báo cáo.')).toBeVisible();

  await dialog.getByRole('radio', { name: 'Khác' }).check();
  await dialog.getByLabel('Mô tả thêm').fill('   ngắn  ');
  await dialog.getByRole('button', { name: 'Gửi báo cáo' }).click();
  await expect(dialog.getByText('Mô tả cho lý do Khác cần từ 10 đến 500 ký tự.')).toBeVisible();
  await expect(dialog.getByText('Chọn một lý do báo cáo.')).toHaveCount(0);
  expect(api.reportBodies).toEqual([]);
});

test('a duplicate report leads to the open report and saves a supplement with its time', async ({ page }) => {
  await session(page);
  const api = await mockApi(page);
  api.submit = (route) => problem(route, 409, {
    code: 'REPORT_ALREADY_OPEN', reportId: 70,
    detail: 'Bạn đã gửi báo cáo cho công thức này. Hãy bổ sung thông tin vào báo cáo hiện có.',
  });
  const dialog = await openDialog(page);

  await dialog.getByRole('radio', { name: 'Nguy cơ an toàn thực phẩm' }).check();
  await dialog.getByRole('button', { name: 'Gửi báo cáo' }).click();

  await expect(dialog).toContainText('Bạn đã gửi báo cáo cho công thức này. Hãy bổ sung thông tin vào báo cáo hiện có.');
  await expect(dialog).toContainText('Đang xem xét');
  await expect(dialog).toContainText('Spam hoặc quảng cáo không phù hợp');
  await expect(dialog).toContainText('Có đường dẫn bán hàng trong bài');
  await expect(dialog.getByRole('listitem').filter({ hasText: 'Ảnh bìa có logo cửa hàng' })).toContainText('24/10/2026 10:00');
  await expect(dialog).toContainText('Còn 4 lần bổ sung');

  await dialog.getByLabel('Thông tin bổ sung').fill('  Bình luận thứ ba cũng chứa đường dẫn quảng cáo  ');
  await dialog.getByRole('button', { name: 'Lưu bổ sung' }).click();

  await expect(dialog.getByRole('status')).toHaveText('Đã lưu phần bổ sung.');
  await expect(dialog.getByRole('listitem').filter({ hasText: 'Bình luận thứ ba cũng chứa đường dẫn quảng cáo' })).toContainText('25/10/2026 10:00');
  await expect(dialog).toContainText('Còn 3 lần bổ sung');
  await expect(dialog.getByLabel('Thông tin bổ sung')).toHaveValue('');
  expect(api.supplementBodies).toEqual([{ content: 'Bình luận thứ ba cũng chứa đường dẫn quảng cáo' }]);
});

test('a short supplement is refused locally and the server limit is explained', async ({ page }) => {
  await session(page);
  const api = await mockApi(page);
  api.submit = (route) => problem(route, 409, { code: 'REPORT_ALREADY_OPEN', reportId: 70, detail: 'Bạn đã gửi báo cáo cho công thức này.' });
  api.supplement = (route) => problem(route, 409, {
    code: 'REPORT_SUPPLEMENT_LIMIT', detail: 'Báo cáo này đã có đủ 5 lần bổ sung.',
  });
  const dialog = await openDialog(page);
  await dialog.getByRole('radio', { name: 'Khác' }).check();
  await dialog.getByLabel('Mô tả thêm').fill('Mô tả đủ dài cho lý do khác');
  await dialog.getByRole('button', { name: 'Gửi báo cáo' }).click();

  await dialog.getByLabel('Thông tin bổ sung').fill('ngắn');
  await dialog.getByRole('button', { name: 'Lưu bổ sung' }).click();
  await expect(dialog.getByText('Nhập nội dung bổ sung từ 10 đến 500 ký tự.')).toBeVisible();
  expect(api.supplementBodies).toEqual([]);

  await dialog.getByLabel('Thông tin bổ sung').fill('Thông tin đủ dài để gửi lên máy chủ');
  await dialog.getByRole('button', { name: 'Lưu bổ sung' }).click();
  await expect(dialog.getByRole('alert')).toHaveText('Báo cáo này đã có đủ 5 lần bổ sung.');
  expect(api.supplementBodies).toEqual([{ content: 'Thông tin đủ dài để gửi lên máy chủ' }]);
});

test('reporting again after a resolved report asks for the new problem', async ({ page }) => {
  await session(page);
  const api = await mockApi(page);
  api.submit = (route) => problem(route, 400, {
    code: 'VALIDATION_FAILED', field: 'description',
    detail: 'Bạn đã báo cáo công thức này trước đây. Hãy mô tả vấn đề mới từ 10 đến 500 ký tự.',
  });
  const dialog = await openDialog(page);

  await dialog.getByRole('radio', { name: 'Sai thông tin công thức hoặc nhãn ăn chay' }).check();
  await dialog.getByRole('button', { name: 'Gửi báo cáo' }).click();

  await expect(dialog.getByText('Bạn đã báo cáo công thức này trước đây. Hãy mô tả vấn đề mới từ 10 đến 500 ký tự.')).toBeVisible();
  await expect(dialog.getByLabel('Mô tả thêm')).toHaveAttribute('aria-invalid', 'true');
  await expect(dialog.getByRole('button', { name: 'Gửi báo cáo' })).toBeEnabled();
});

test('Guest is asked to sign in and the author sees no report action', async ({ page }) => {
  const api = await mockApi(page);
  await page.goto('/cong-thuc/id/901');
  await page.getByRole('button', { name: 'Báo cáo công thức' }).click();
  await expect(page.getByRole('link', { name: 'Đăng nhập để báo cáo' })).toHaveAttribute('href', '/dang-nhap');
  await expect(page.getByRole('dialog')).toHaveCount(0);
  expect(api.reportBodies).toEqual([]);

  await session(page, 'EXPERT', 22);
  await page.reload();
  await expect(page.getByRole('heading', { name: detail.title })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Báo cáo công thức' })).toHaveCount(0);
});
