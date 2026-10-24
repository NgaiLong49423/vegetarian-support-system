import type { Page, Route } from '@playwright/test';
import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

// FR-49 (Issue #51): bell with the unread count, notification center, read state and email preference.

const RECIPE_ID = 3201;
const LINKED_REPLY = 25;

type Notification = {
  notificationId: number; type: string; title: string; message: string; targetPath: string | null;
  read: boolean; createdAt: string; readAt: string | null;
};

const reply = (id: number, minute: number, read = false): Notification => ({
  notificationId: id,
  type: 'COMMENT_REPLY',
  title: 'Có phản hồi mới cho bình luận của bạn',
  message: `Lan Chay đã trả lời bình luận của bạn trong công thức “Canh nấm chay” (${id}).`,
  targetPath: `/cong-thuc/id/${RECIPE_ID}#comment-${LINKED_REPLY}`,
  read,
  createdAt: `2026-10-24T08:${String(minute).padStart(2, '0')}:00`,
  readAt: read ? '2026-10-24T09:00:00' : null,
});

/** In-memory stand-in for the FR-49 endpoints, owner-scoped like the Backend. */
class NotificationServer {
  items: Notification[] = [reply(3, 30), reply(2, 20), reply(1, 10, true)];
  replyEmailEnabled = true;
  requests: string[] = [];
  failList = false;
  failPreferenceSave = false;

  async install(page: Page) {
    await page.route('**/api/v1/**', (route) => route.fulfill({ status: 404, contentType: 'application/problem+json',
      json: { status: 404, code: 'NOT_FOUND', detail: 'Không có trong bộ giả lập.' } }));
    await page.route('**/api/v1/me/notifications**', (route) => this.onNotifications(route));
    await page.route('**/api/v1/me/notification-preferences', (route) => this.onPreferences(route));
  }

  unread() {
    return this.items.filter((item) => !item.read).length;
  }

  private onNotifications(route: Route) {
    const request = route.request();
    const url = new URL(request.url());
    this.requests.push(`${request.method()} ${url.pathname}${url.search}`);
    if (url.pathname.endsWith('/unread-count')) return route.fulfill({ json: { unreadCount: this.unread() } });
    if (url.pathname.endsWith('/read-all')) {
      this.items = this.items.map((item) => ({ ...item, read: true, readAt: item.readAt ?? '2026-10-24T09:30:00' }));
      return route.fulfill({ status: 204 });
    }
    const markRead = /\/me\/notifications\/(\d+)\/read$/.exec(url.pathname);
    if (markRead) {
      const item = this.items.find((candidate) => candidate.notificationId === Number(markRead[1]));
      if (!item) return route.fulfill({ status: 404, contentType: 'application/problem+json',
        json: { status: 404, code: 'NOTIFICATION_NOT_FOUND', detail: 'Không tìm thấy thông báo.' } });
      item.read = true;
      item.readAt = '2026-10-24T09:10:00';
      return route.fulfill({ json: item });
    }
    if (this.failList) return route.fulfill({ status: 503, contentType: 'application/problem+json',
      json: { status: 503, detail: 'Dịch vụ tạm thời gián đoạn.' } });
    const page = Number(url.searchParams.get('page') ?? 0);
    const size = Number(url.searchParams.get('size') ?? 20);
    const filtered = url.searchParams.get('unreadOnly') === 'true' ? this.items.filter((item) => !item.read) : this.items;
    return route.fulfill({ json: {
      content: filtered.slice(page * size, page * size + size), page, size,
      totalElements: filtered.length, totalPages: Math.ceil(filtered.length / size),
    } });
  }

  private onPreferences(route: Route) {
    const request = route.request();
    this.requests.push(`${request.method()} /api/v1/me/notification-preferences`);
    if (request.method() === 'PUT') {
      if (this.failPreferenceSave) return route.fulfill({ status: 503, contentType: 'application/problem+json',
        json: { status: 503, detail: 'Không lưu được tùy chọn lúc này.' } });
      this.replyEmailEnabled = (request.postDataJSON() as { replyEmailEnabled: boolean }).replyEmailEnabled;
    }
    return route.fulfill({ json: { replyEmailEnabled: this.replyEmailEnabled } });
  }
}

const bell = (page: Page) => page.getByRole('link', { name: /^Thông báo/ });
const list = (page: Page) => page.getByRole('list', { name: 'Danh sách thông báo' });

test('AC-49.1: the bell shows the unread count and the center lists the newest notification first', async ({ page }) => {
  const server = new NotificationServer();
  await seedDemoSession(page);
  await server.install(page);
  await page.goto('/trang-chu');

  await expect(bell(page)).toHaveAccessibleName('Thông báo, 2 chưa đọc');
  await bell(page).click();
  await expect(page).toHaveURL(/\/thong-bao$/);
  await expect(page.getByRole('heading', { level: 1, name: 'Thông báo' })).toBeVisible();
  await expect(list(page).getByRole('listitem')).toHaveCount(3);
  await expect(list(page).getByRole('listitem').first()).toContainText('(3)');
  await expect(list(page).getByRole('listitem').first()).toContainText('Chưa đọc');
  await expect(list(page).getByRole('listitem').last()).not.toContainText('Chưa đọc');
  // Opening the center does not change the read state (UC-49.1 postcondition).
  expect(server.requests.filter((item) => item.startsWith('PUT'))).toEqual([]);
});

test('UC-49.2: opening an unread notification marks it read and opens the reply, even on a later comment page', async ({ page }) => {
  const server = new NotificationServer();
  await seedDemoSession(page);
  await server.install(page);
  await mockRecipeWithReplyOnSecondPage(page);
  await page.goto('/thong-bao');

  await list(page).getByRole('button', { name: /\(3\)/ }).click();

  await expect(page).toHaveURL(new RegExp(`/cong-thuc/id/${RECIPE_ID}#comment-${LINKED_REPLY}$`));
  expect(server.requests).toContain('PUT /api/v1/me/notifications/3/read');
  const linked = page.locator(`#comment-${LINKED_REPLY}`);
  await expect(linked).toBeVisible();
  await expect(linked).toContainText('Câu trả lời được nhắc trong thông báo');
  await expect(linked).toBeFocused();
  await expect(bell(page)).toHaveAccessibleName('Thông báo, 1 chưa đọc');
});

test('UC-49.2: the unread filter and "mark all read" update the list and the bell', async ({ page }) => {
  const server = new NotificationServer();
  await seedDemoSession(page);
  await server.install(page);
  await page.goto('/thong-bao');

  await page.getByRole('button', { name: 'Chưa đọc', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Chưa đọc', exact: true })).toHaveAttribute('aria-pressed', 'true');
  await expect(list(page).getByRole('listitem')).toHaveCount(2);
  expect(server.requests).toContain('GET /api/v1/me/notifications?page=0&size=20&unreadOnly=true');

  await page.getByRole('button', { name: 'Đánh dấu tất cả đã đọc' }).click();
  await expect(page.getByText('Không có thông báo chưa đọc')).toBeVisible();
  await expect(bell(page)).toHaveAccessibleName('Thông báo');
  await expect(page.getByRole('button', { name: 'Đánh dấu tất cả đã đọc' })).toBeDisabled();
  expect(server.requests).toContain('PUT /api/v1/me/notifications/read-all');
});

test('the notification center pages through results, shows an empty state and recovers from a load error', async ({ page }) => {
  const server = new NotificationServer();
  server.items = Array.from({ length: 21 }, (_, index) => reply(100 - index, 59 - index));
  server.failList = true;
  await seedDemoSession(page);
  await server.install(page);
  await page.goto('/thong-bao');

  await expect(page.getByText('Không tải được thông báo')).toBeVisible();
  server.failList = false;
  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(list(page).getByRole('listitem')).toHaveCount(20);
  await page.getByRole('button', { name: 'Trang sau' }).click();
  await expect(page.getByText('Trang 2/2')).toBeVisible();
  await expect(list(page).getByRole('listitem')).toHaveCount(1);

  server.items = [];
  await page.getByRole('button', { name: 'Tất cả', exact: true }).click();
  await page.getByRole('button', { name: 'Chưa đọc', exact: true }).click();
  await page.getByRole('button', { name: 'Tất cả', exact: true }).click();
  await expect(page.getByText('Chưa có thông báo', { exact: true })).toBeVisible();
});

test('a Guest has no bell and the notification center asks to sign in', async ({ page }) => {
  const server = new NotificationServer();
  await server.install(page);
  await page.goto('/trang-chu');
  await expect(bell(page)).toHaveCount(0);
  await page.goto('/thong-bao');
  await expect(page).toHaveURL(/\/dang-nhap$/);
  expect(server.requests).toEqual([]);
});

test('UC-49.4: a Member turns reply emails off and back on from profile settings', async ({ page }) => {
  const server = new NotificationServer();
  await seedDemoSession(page);
  await server.install(page);
  await page.route('**/api/v1/me/profile', (route) => route.fulfill({ json: {
    userId: 901, displayName: 'Demo CUSTOMER', avatarUrl: null, bio: null, role: 'CUSTOMER', publishedRecipeCount: 0,
  } }));
  await page.goto('/thong-bao');
  await page.getByRole('link', { name: 'Cài đặt email thông báo' }).click();
  await expect(page).toHaveURL(/\/ho-so\/cai-dat#email-thong-bao$/);

  const toggle = page.getByRole('checkbox', { name: 'Gửi email cho tôi khi có người trả lời bình luận của tôi' });
  await expect(toggle).toBeChecked();
  await toggle.uncheck();
  await expect(page.getByText('Đã tắt email thông báo.')).toBeVisible();
  expect(server.replyEmailEnabled).toBe(false);

  server.failPreferenceSave = true;
  await toggle.check();
  const section = page.locator('#email-thong-bao');
  await expect(section.getByRole('alert')).toBeVisible();
  await expect(toggle).not.toBeChecked();
  server.failPreferenceSave = false;
  await toggle.check();
  await expect(page.getByText('Đã bật email thông báo.')).toBeVisible();
  expect(server.replyEmailEnabled).toBe(true);
});

test('NFR-13: the notification center fits a 360 px phone without horizontal scroll', async ({ page }) => {
  await page.setViewportSize({ width: 360, height: 780 });
  const server = new NotificationServer();
  await seedDemoSession(page);
  await server.install(page);
  await page.goto('/thong-bao');
  await expect(list(page).getByRole('listitem')).toHaveCount(3);
  expect(await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth))
    .toBeLessThanOrEqual(0);
});

/** Recipe detail whose linked reply is in the second comment page, so the page must load more to show it. */
async function mockRecipeWithReplyOnSecondPage(page: Page) {
  const author = { userId: 902, displayName: 'Lan Chay', avatarUrl: null };
  const comment = (commentId: number, content: string, replies: unknown[] = [], parentCommentId: number | null = null) => ({
    commentId, parentCommentId, depth: parentCommentId ? 2 : 1, content, author, deleted: false, edited: false,
    createdAt: '2026-10-24T08:00:00', replies,
  });
  await page.route(new RegExp(`/api/v1/recipes/${RECIPE_ID}$`), (route) => route.fulfill({ json: {
    recipeId: RECIPE_ID, title: 'Canh nấm chay', description: null, instructions: 'Nấu nấm với nước dùng rau củ.',
    dishCategory: 'SOUP', dishCategoryLabel: 'Món canh', vegetarianType: 'VEGAN', vegetarianTypeLabel: 'Thuần chay',
    difficulty: 'EASY', difficultyLabel: 'Dễ', servings: 2, prepTimeMinutes: 10, cookTimeMinutes: 20, youtubeUrl: null,
    publishedAt: '2026-10-05T12:00:00', nutritionComplete: true, ingredientsWithoutNutrition: [], ingredients: [], media: [],
    statistics: { likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null, viewCount: 0 }, author,
  } }));
  await page.route(new RegExp(`/api/v1/recipes/${RECIPE_ID}/comments`), (route) => {
    const requested = Number(new URL(route.request().url()).searchParams.get('page') ?? 0);
    const content = requested === 0
      ? Array.from({ length: 10 }, (_, index) => comment(index + 1, `Bình luận ${index + 1}`))
      : [comment(20, 'Bình luận của bạn', [comment(LINKED_REPLY, 'Câu trả lời được nhắc trong thông báo', [], 20)])];
    return route.fulfill({ json: { content, page: requested, size: 10, totalElements: 11, totalPages: 2, commentCount: 12 } });
  });
}
