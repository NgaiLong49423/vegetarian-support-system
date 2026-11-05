import type { Page } from '@playwright/test';
import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

const user = {
  id: 42,
  displayName: 'Nguyễn An',
  email: 'an@example.test',
  role: 'CUSTOMER',
  accountStatus: 'ACTIVE',
  joinedAt: '2026-10-20T08:00:00',
  publishedRecipeCount: 3,
};

async function mockAccountApi(page: Page, options: { failFirstList?: boolean; failFirstHistory?: boolean } = {}) {
  const current = { ...user };
  const administrator = { ...user, id: 7, displayName: 'Quản trị viên', email: 'admin@example.test', role: 'ADMIN', publishedRecipeCount: 0 };
  const listRequests: URL[] = [];
  const writes: Array<{ method: string; path: string; body: unknown }> = [];
  let listFailed = false;
  let historyFailed = false;
  await page.route('**/api/v1/admin/users**', async (route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace(/^.*\/api\/v1\/admin\/users/, '');
    if (request.method() === 'GET' && path === '') listRequests.push(url);
    if (request.method() === 'GET' && path === '' && options.failFirstList && !listFailed) {
      listFailed = true;
      return route.fulfill({ status: 503, json: { status: 503, code: 'SERVICE_UNAVAILABLE', detail: 'Dịch vụ quản lý người dùng đang tạm thời gián đoạn.' } });
    }
    if (request.method() === 'GET' && path === '/summary') {
      return route.fulfill({ json: { totalUsers: 8, activeUsers: 6, expertUsers: 2, lockedUsers: 2 } });
    }
    if (request.method() === 'GET' && path === '/42/history') {
      if (options.failFirstHistory && !historyFailed) {
        historyFailed = true;
        return route.fulfill({ status: 503, json: { status: 503, code: 'SERVICE_UNAVAILABLE', detail: 'Chưa tải được lịch sử tài khoản.' } });
      }
      return route.fulfill({ json: {
        content: [
          { id: 101, action: 'AI_CREDIT_GRANTED', oldValue: '100', newValue: '350', reason: 'Tặng token bù lỗi hệ thống', occurredAt: '2026-10-21T08:00:00' },
          { id: 102, action: 'ROLE_CHANGED', oldValue: 'Thành viên', newValue: 'Chuyên gia', reason: 'Đã xét duyệt chuyên môn', occurredAt: '2026-10-22T08:00:00' }
        ],
        page: 0,
        size: 20,
        totalElements: 2,
        totalPages: 2
      } });
    }
    if (request.method() === 'GET' && path === '/42') return route.fulfill({ json: current });
    if (request.method() === 'GET' && path === '') {
      const accounts = [current, administrator].filter((account) => (!url.searchParams.get('role') || url.searchParams.get('role') === account.role)
        && (!url.searchParams.get('status') || url.searchParams.get('status') === account.accountStatus)
        && (!url.searchParams.get('query') || `${account.displayName} ${account.email} ${account.id}`.toLowerCase().includes(url.searchParams.get('query')!.toLowerCase())));
      const content = Number(url.searchParams.get('page') ?? 0) === 0 ? accounts : [];
      return route.fulfill({ json: { content, page: Number(url.searchParams.get('page') ?? 0), size: 20, totalElements: accounts.length, totalPages: accounts.length ? 1 : 0 } });
    }
    if (request.method() === 'PATCH') {
      const body = request.postDataJSON() as { role?: string; accountStatus?: string; reason: string };
      writes.push({ method: request.method(), path, body });
      if (body.role) current.role = body.role;
      if (body.accountStatus) current.accountStatus = body.accountStatus;
      return route.fulfill({ json: current });
    }
    return route.fulfill({ status: 404, json: { status: 404, code: 'NOT_FOUND' } });
  });

  await page.route('**/api/v1/admin/ai-accounts/**', async (route) => {
    const req = route.request();
    const url = new URL(req.url());
    if (req.method() === 'GET' && url.pathname.endsWith('/tier')) {
      return route.fulfill({ json: { currentTier: 'FREE', newTier: 'FREE', tokenQuota: 10000, endsAt: null } });
    }
    if (req.method() === 'POST' && url.pathname.endsWith('/package-gifts')) {
      return route.fulfill({ json: { currentTier: 'FREE', newTier: 'PLUS', tokenQuota: 50000, endsAt: '2026-12-01T00:00:00' } });
    }
    if (req.method() === 'POST' && url.pathname.endsWith('/credit-grants')) {
      return route.fulfill({ json: { credits: 500, balance: 1500 } });
    }
    return route.fulfill({ status: 404, json: { status: 404, code: 'NOT_FOUND' } });
  });

  return { listRequests, writes };
}

test.beforeEach(async ({ page }) => {
  await seedDemoSession(page, 'ADMIN');
});

test('loads real account API data and sends server-side filters', async ({ page }) => {
  const { listRequests } = await mockAccountApi(page);
  await page.goto('/admin/nguoi-dung');
  await expect(page.getByText('Nguyễn An', { exact: true })).toBeVisible();
  await expect(page.getByText('8', { exact: true }).first()).toBeVisible();
  await page.getByRole('combobox', { name: 'Lọc vai trò' }).selectOption('CUSTOMER');
  await expect.poll(() => listRequests.some((url) => url.searchParams.get('role') === 'CUSTOMER')).toBe(true);
  await page.getByRole('textbox', { name: 'Tìm người dùng' }).fill('an@example.test');
  await expect.poll(() => listRequests.some((url) => url.searchParams.get('query') === 'an@example.test')).toBe(true);
  await expect(page.getByRole('button', { name: 'Xuất danh sách' })).toHaveCount(0);
});

test('filters users by status using ThemedSelect', async ({ page }) => {
  const { listRequests } = await mockAccountApi(page);
  await page.goto('/admin/nguoi-dung');
  await page.getByRole('combobox', { name: 'Lọc trạng thái' }).selectOption('LOCKED');
  await expect.poll(() => listRequests.some((url) => url.searchParams.get('status') === 'LOCKED')).toBe(true);
});

test('ThemedSelect supports mouse click open, keyboard navigation, Escape and click outside', async ({ page }) => {
  await mockAccountApi(page);
  await page.goto('/admin/nguoi-dung');

  const select = page.getByRole('combobox', { name: 'Lọc vai trò' });

  // 1. Mouse click opens dropdown popup
  await select.dispatchEvent('mousedown');
  const listbox = page.getByRole('listbox');
  await expect(listbox).toBeVisible();

  // 2. Escape closes dropdown
  await page.keyboard.press('Escape');
  await expect(listbox).toHaveCount(0);

  // 3. ArrowDown opens dropdown
  await select.dispatchEvent('keydown', { key: 'ArrowDown' });
  await expect(listbox).toBeVisible();

  // 4. Click option selects value
  await listbox.getByRole('option', { name: 'Chuyên gia' }).click();
  await expect(listbox).toHaveCount(0);

  // 5. Enter opens dropdown, click outside closes
  await select.dispatchEvent('keydown', { key: 'Enter' });
  await expect(listbox).toBeVisible();
  await page.mouse.click(10, 10);
  await expect(listbox).toHaveCount(0);
});

test('role changes require a reason and update only from the API response', async ({ page }) => {
  const { writes } = await mockAccountApi(page);
  await page.goto('/admin/nguoi-dung');
  const row = page.getByRole('row').filter({ hasText: 'Nguyễn An' });
  await row.getByRole('button', { name: 'Đổi vai trò' }).click();
  await page.getByRole('combobox', { name: 'Vai trò mới' }).selectOption('EXPERT');
  await expect(page.getByRole('button', { name: 'Xác nhận' })).toBeDisabled();
  await page.getByRole('textbox', { name: 'Lý do thay đổi' }).fill('Đã xem xét hồ sơ chuyên gia');
  await expect(row.getByText('Thành viên', { exact: true })).toBeVisible();
  await page.getByRole('button', { name: 'Xác nhận' }).click();
  await expect(row.getByText('Chuyên gia', { exact: true })).toBeVisible();
  expect(writes).toHaveLength(1);
  expect(writes[0]).toMatchObject({ method: 'PATCH', path: '/42/role', body: { role: 'EXPERT', reason: 'Đã xem xét hồ sơ chuyên gia' } });
});

test('locking requires a reason, while administrator accounts remain view-only', async ({ page }) => {
  const { writes } = await mockAccountApi(page);
  await page.goto('/admin/nguoi-dung');
  const row = page.getByRole('row').filter({ hasText: 'Nguyễn An' });
  await row.getByRole('button', { name: 'Khóa tài khoản' }).click();
  await page.getByRole('textbox', { name: 'Lý do thay đổi' }).fill('Vi phạm quy định cộng đồng');
  await page.getByRole('button', { name: 'Xác nhận' }).click();
  await expect(row).toContainText('Đã khóa');
  expect(writes[0]).toMatchObject({ method: 'PATCH', path: '/42/status', body: { accountStatus: 'LOCKED', reason: 'Vi phạm quy định cộng đồng' } });
  const adminRow = page.getByRole('row').filter({ hasText: 'Quản trị viên' });
  await expect(adminRow.getByRole('button', { name: 'Khóa tài khoản' })).toHaveCount(0);
  await expect(adminRow.getByRole('button', { name: 'Đổi vai trò' })).toHaveCount(0);
});

test('unlocks a locked user account with reason', async ({ page }) => {
  await mockAccountApi(page);
  await page.goto('/admin/nguoi-dung');
  const row = page.getByRole('row').filter({ hasText: 'Nguyễn An' });

  // First lock
  await row.getByRole('button', { name: 'Khóa tài khoản' }).click();
  await page.getByRole('textbox', { name: 'Lý do thay đổi' }).fill('Khóa tạm thời để kiểm tra tài khoản');
  await page.getByRole('button', { name: 'Xác nhận' }).click();
  await expect(row).toContainText('Đã khóa');

  // Now unlock
  await row.getByRole('button', { name: 'Mở khóa tài khoản' }).click();
  await page.getByRole('textbox', { name: 'Lý do thay đổi' }).fill('Đã hoàn tất xác minh danh tính tài khoản');
  await page.getByRole('button', { name: 'Xác nhận' }).click();
  await expect(row).toContainText('Hoạt động');
});

test('gifts an AI package to a member with reason and confirmation', async ({ page }) => {
  await mockAccountApi(page);
  await page.goto('/admin/nguoi-dung');
  const row = page.getByRole('row').filter({ hasText: 'Nguyễn An' });
  await row.getByRole('button', { name: 'Tặng gói AI / Cộng AI Credit' }).click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  await dialog.getByRole('button', { name: 'Tặng gói AI' }).click();

  await page.getByRole('combobox', { name: 'Gói AI muốn tặng' }).selectOption('PLUS');
  await page.getByRole('textbox', { name: 'Lý do tặng gói AI' }).fill('Tặng gói AI trải nghiệm cho người dùng');
  await page.getByRole('button', { name: 'Xác nhận tặng gói' }).click();

  await expect(page.getByRole('status')).toContainText('Đã tặng gói AI PLUS cho Nguyễn An.');
});

test('grants AI credit with credit amount and reason', async ({ page }) => {
  await mockAccountApi(page);
  await page.goto('/admin/nguoi-dung');
  const row = page.getByRole('row').filter({ hasText: 'Nguyễn An' });
  await row.getByRole('button', { name: 'Tặng gói AI / Cộng AI Credit' }).click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  await dialog.getByRole('button', { name: 'Tặng AI Credit' }).click();

  await page.getByRole('spinbutton', { name: 'Số AI Credit muốn tặng' }).fill('500');
  await page.getByRole('textbox', { name: 'Lý do tặng AI Credit' }).fill('Tặng AI Credit bù đắp phiên lỗi');
  await page.getByRole('button', { name: 'Xác nhận tặng AI Credit' }).click();

  await expect(page.getByRole('status')).toContainText('Đã cộng 500 AI Credit.');
});

test('shows an API error and retries loading accounts', async ({ page }) => {
  const { listRequests } = await mockAccountApi(page, { failFirstList: true });
  await page.goto('/admin/nguoi-dung');

  await expect(page.getByRole('alert')).toContainText('Dịch vụ quản lý người dùng đang tạm thời gián đoạn.');
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByText('Nguyễn An', { exact: true })).toBeVisible();
  expect(listRequests).toHaveLength(2);
});

test('shows and retries account history without losing the selected account', async ({ page }) => {
  await mockAccountApi(page, { failFirstHistory: true });
  await page.goto('/admin/nguoi-dung');
  const row = page.getByRole('row').filter({ hasText: 'Nguyễn An' });
  await row.getByRole('button', { name: 'Xem chi tiết và lịch sử' }).click();

  await expect(page.getByRole('dialog')).toBeVisible();
  await expect(page.getByRole('alert')).toContainText('Chưa tải được lịch sử tài khoản.');
  await dialog.getByRole('button', { name: 'Thử lại' }).click();
  await expect(dialog.getByText('+100 AI Credit')).toBeVisible();
  await expect(dialog.getByText('Mã tài khoản')).toBeVisible();
  await expect(dialog.getByRole('button', { name: 'Sau' })).toBeEnabled();
  await dialog.getByRole('button', { name: 'Sau' }).click();
  await dialog.getByRole('button', { name: 'Đóng' }).click();
  await expect(dialog).toHaveCount(0);
});
