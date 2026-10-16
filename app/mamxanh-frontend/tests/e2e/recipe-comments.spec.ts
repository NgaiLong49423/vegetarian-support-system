import type { Page, Route } from '@playwright/test';
import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

const RECIPE_ID = 3101;
const VIEWER_ID = 901;
const OTHER = { userId: 902, displayName: 'Lan Chay', avatarUrl: null };
const VIEWER = { userId: VIEWER_ID, displayName: 'Demo CUSTOMER', avatarUrl: null };

const recipeDetail = {
  recipeId: RECIPE_ID,
  title: 'Canh nấm chay',
  description: 'Món canh thanh đạm.',
  instructions: 'Nấu nấm với nước dùng rau củ.',
  dishCategory: 'SOUP',
  dishCategoryLabel: 'Món canh',
  vegetarianType: 'VEGAN',
  vegetarianTypeLabel: 'Thuần chay',
  difficulty: 'EASY',
  difficultyLabel: 'Dễ',
  servings: 2,
  prepTimeMinutes: 10,
  cookTimeMinutes: 20,
  youtubeUrl: null,
  publishedAt: '2026-10-05T12:00:00',
  nutritionComplete: true,
  ingredientsWithoutNutrition: [],
  ingredients: [],
  media: [],
  statistics: { likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null, viewCount: 0 },
  author: { userId: 902, displayName: 'Lan Chay', avatarUrl: null },
};

type Author = { userId: number; displayName: string; avatarUrl: string | null };
type StoredComment = {
  commentId: number;
  parentCommentId: number | null;
  depth: number;
  content: string;
  author: Author;
  deleted: boolean;
  edited: boolean;
  createdAt: string;
};

/** In-memory stand-in for the FR-46 endpoints with the same thread rules as the Backend. */
class CommentServer {
  comments: StoredComment[] = [];
  requests: Array<{ method: string; body: unknown }> = [];
  private nextId = 1;
  private tick = 0;

  add(author: Author, content: string, parentCommentId: number | null = null): number {
    const parent = this.comments.find((item) => item.commentId === parentCommentId);
    const attachTo = parent && parent.depth >= 5 ? parent.parentCommentId : parentCommentId;
    const depth = parent ? Math.min(parent.depth + 1, 5) : 1;
    const comment: StoredComment = {
      commentId: this.nextId++, parentCommentId: attachTo, depth, content, author,
      deleted: false, edited: false, createdAt: `2026-10-11T08:${String(this.tick++).padStart(2, '0')}:00`,
    };
    this.comments.push(comment);
    return comment.commentId;
  }

  private view(comment: StoredComment): Record<string, unknown> | null {
    const replies = this.comments
      .filter((item) => item.parentCommentId === comment.commentId)
      .map((item) => this.view(item))
      .filter((item) => item !== null);
    if (comment.deleted && replies.length === 0) return null;
    return {
      commentId: comment.commentId, parentCommentId: comment.parentCommentId, depth: comment.depth,
      content: comment.deleted ? null : comment.content, author: comment.deleted ? null : comment.author,
      deleted: comment.deleted, edited: comment.edited, createdAt: comment.createdAt, replies,
    };
  }

  page(page: number, size: number) {
    const roots = this.comments
      .filter((item) => item.parentCommentId === null)
      .reverse()
      .map((item) => this.view(item))
      .filter((item) => item !== null);
    return {
      content: roots.slice(page * size, page * size + size),
      page, size, totalElements: roots.length, totalPages: Math.ceil(roots.length / size),
      commentCount: this.comments.filter((item) => !item.deleted).length,
    };
  }

  async install(page: Page) {
    await page.route(new RegExp(`/api/v1/recipes/${RECIPE_ID}$`), (route) => route.fulfill({ json: recipeDetail }));
    await page.route(new RegExp(`/api/v1/recipes/${RECIPE_ID}/comments`), (route) => this.onRecipeComments(route));
    await page.route(/\/api\/v1\/comments\/\d+$/, (route) => this.onComment(route));
  }

  private async onRecipeComments(route: Route) {
    const request = route.request();
    if (request.method() === 'GET') {
      const url = new URL(request.url());
      return route.fulfill({ json: this.page(Number(url.searchParams.get('page') ?? 0), Number(url.searchParams.get('size') ?? 10)) });
    }
    const body = request.postDataJSON() as { content: string; parentCommentId?: number };
    this.requests.push({ method: 'POST', body });
    if (!body.content?.trim()) {
      return route.fulfill({ status: 400, json: { status: 400, code: 'VALIDATION_FAILED', detail: 'Dữ liệu gửi lên không hợp lệ.', errors: [{ field: 'content', message: 'Bình luận cần từ 1 đến 1.000 ký tự.' }] } });
    }
    const id = this.add(VIEWER, body.content.trim(), body.parentCommentId ?? null);
    return route.fulfill({ status: 201, json: { ...this.view(this.comments.find((item) => item.commentId === id)!), replies: [] } });
  }

  private async onComment(route: Route) {
    const request = route.request();
    const id = Number(new URL(request.url()).pathname.split('/').pop());
    const comment = this.comments.find((item) => item.commentId === id)!;
    this.requests.push({ method: request.method(), body: request.postDataJSON() });
    if (request.method() === 'DELETE') {
      comment.deleted = true;
      return route.fulfill({ status: 204 });
    }
    comment.content = (request.postDataJSON() as { content: string }).content.trim();
    comment.edited = true;
    return route.fulfill({ json: { ...this.view(comment), replies: [] } });
  }
}

async function openRecipe(page: Page) {
  await page.goto(`/cong-thuc/${RECIPE_ID}`);
  await expect(page.getByRole('heading', { name: 'Canh nấm chay' })).toBeVisible();
}

const commentItem = (page: Page, text: string) =>
  page.getByRole('article').filter({ has: page.getByText(text, { exact: true }) }).last();

test.describe('Issue #48 [FR-46] — Bình luận và reply lồng nhiều cấp', () => {
  test('UC-46.1: the canonical recipe page /cong-thuc/id/:id shows the comment threads', async ({ page }) => {
    const server = new CommentServer();
    server.add(OTHER, 'Bình luận trên trang chuẩn');
    await server.install(page);

    await page.goto(`/cong-thuc/id/${RECIPE_ID}`);
    await expect(page.getByRole('heading', { name: 'Canh nấm chay' })).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Bình luận (1)' })).toBeVisible();
    await expect(page.getByText('Bình luận trên trang chuẩn')).toBeVisible();
  });


  test('UC-46.1: Guest reads nested threads, tombstones and edit marks but must sign in to write', async ({ page }) => {
    const server = new CommentServer();
    const root = server.add(OTHER, 'Nấm rơm thay được không?');
    server.add(VIEWER, 'Được, nấm đùi gà cũng ngon.', root);
    const removed = server.add(OTHER, 'Bình luận sẽ bị xóa');
    server.add(VIEWER, 'Reply còn lại', removed);
    server.comments.find((item) => item.commentId === removed)!.deleted = true;
    server.comments.find((item) => item.commentId === root)!.edited = true;
    await server.install(page);

    await openRecipe(page);
    await expect(page.getByRole('heading', { name: 'Bình luận (3)' })).toBeVisible();
    await expect(page.getByText('Nấm rơm thay được không?')).toBeVisible();
    await expect(commentItem(page, 'Nấm rơm thay được không?')).toContainText('(Đã chỉnh sửa)');
    await expect(page.getByText('Được, nấm đùi gà cũng ngon.')).toBeVisible();
    await expect(page.getByText('Bình luận này đã bị xóa bởi người dùng')).toBeVisible();
    await expect(page.getByText('Reply còn lại')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Trả lời' })).toHaveCount(0);
    await expect(page.getByLabel('Viết bình luận')).toHaveCount(0);
    await page.getByRole('link', { name: 'Đăng nhập để bình luận' }).click();
    await expect(page).toHaveURL(/\/dang-nhap$/);
  });

  test('UC-46.2, UC-46.3: Member comments and replies, and the thread shows the new reply', async ({ page }) => {
    const server = new CommentServer();
    server.add(OTHER, 'Có cần ngâm nấm trước không?');
    await server.install(page);
    await seedDemoSession(page, 'CUSTOMER');

    await openRecipe(page);
    await expect(page.getByText('Chưa có bình luận')).toHaveCount(0);
    await page.getByLabel('Viết bình luận').fill('  Món này rất dễ làm.  ');
    await page.getByRole('button', { name: 'Gửi bình luận' }).click();
    await expect(page.getByText('Món này rất dễ làm.')).toBeVisible();
    await expect(page.getByRole('heading', { name: 'Bình luận (2)' })).toBeVisible();

    await commentItem(page, 'Có cần ngâm nấm trước không?').getByRole('button', { name: 'Trả lời' }).click();
    await expect(page.getByText('Đang trả lời Lan Chay')).toBeVisible();
    await page.getByLabel('Viết bình luận').fill('Ngâm 10 phút là đủ.');
    await page.getByRole('button', { name: 'Gửi trả lời' }).click();
    await expect(commentItem(page, 'Có cần ngâm nấm trước không?')).toContainText('Ngâm 10 phút là đủ.');
    expect(server.requests.at(-1)).toEqual({ method: 'POST', body: { content: 'Ngâm 10 phút là đủ.', parentCommentId: 1 } });
  });

  test('AC-46.1: replying to a level-5 comment mentions its author and keeps the thread at five levels', async ({ page }) => {
    const server = new CommentServer();
    let parent: number | null = null;
    for (let level = 1; level <= 5; level += 1) parent = server.add(level % 2 ? OTHER : VIEWER, `Cấp ${level}`, parent);
    await server.install(page);
    await seedDemoSession(page, 'CUSTOMER');

    await openRecipe(page);
    await commentItem(page, 'Cấp 5').getByRole('button', { name: 'Trả lời' }).click();
    await expect(page.getByLabel('Viết bình luận')).toHaveValue('@Lan Chay ');
    await page.getByLabel('Viết bình luận').press('End');
    await page.getByLabel('Viết bình luận').pressSequentially('đồng ý');
    await page.getByRole('button', { name: 'Gửi trả lời' }).click();
    await expect(page.getByText('@Lan Chay đồng ý')).toBeVisible();
    expect(server.comments.at(-1)).toMatchObject({ depth: 5, parentCommentId: 4 });
  });

  test('AC-46.4, UC-46.4: only the owner edits, and the edit is marked', async ({ page }) => {
    const server = new CommentServer();
    server.add(OTHER, 'Bình luận của Lan');
    server.add(VIEWER, 'Bình luận của tôi');
    await server.install(page);
    await seedDemoSession(page, 'CUSTOMER');

    await openRecipe(page);
    await expect(commentItem(page, 'Bình luận của Lan').getByRole('button', { name: 'Sửa' })).toHaveCount(0);
    await expect(commentItem(page, 'Bình luận của Lan').getByRole('button', { name: 'Xóa' })).toHaveCount(0);

    await commentItem(page, 'Bình luận của tôi').getByRole('button', { name: 'Sửa' }).click();
    await expect(page.getByText('Đang sửa bình luận')).toBeVisible();
    await expect(page.getByLabel('Viết bình luận')).toHaveValue('Bình luận của tôi');
    await page.getByLabel('Viết bình luận').fill('Bình luận đã sửa');
    await page.getByRole('button', { name: 'Lưu chỉnh sửa' }).click();
    await expect(commentItem(page, 'Bình luận đã sửa')).toContainText('(Đã chỉnh sửa)');
    expect(server.requests.at(-1)).toEqual({ method: 'PUT', body: { content: 'Bình luận đã sửa' } });
  });

  test('AC-46.2, UC-46.5: deleting a parent leaves a tombstone above its replies; a deleted leaf disappears', async ({ page }) => {
    const server = new CommentServer();
    const parent = server.add(VIEWER, 'Cha của tôi');
    server.add(OTHER, 'Con thứ nhất', parent);
    server.add(OTHER, 'Con thứ hai', parent);
    server.add(VIEWER, 'Lá của tôi');
    await server.install(page);
    await seedDemoSession(page, 'CUSTOMER');

    await openRecipe(page);
    await commentItem(page, 'Cha của tôi').getByRole('button', { name: 'Xóa' }).first().click();
    await page.getByRole('button', { name: 'Không xóa' }).click();
    await expect(page.getByText('Cha của tôi')).toBeVisible();

    await commentItem(page, 'Cha của tôi').getByRole('button', { name: 'Xóa' }).first().click();
    await page.getByRole('button', { name: 'Xác nhận xóa' }).click();
    await expect(page.getByText('Bình luận này đã bị xóa bởi người dùng')).toBeVisible();
    await expect(page.getByText('Con thứ nhất')).toBeVisible();
    await expect(page.getByText('Con thứ hai')).toBeVisible();

    await commentItem(page, 'Lá của tôi').getByRole('button', { name: 'Xóa' }).click();
    await page.getByRole('button', { name: 'Xác nhận xóa' }).click();
    await expect(page.getByText('Lá của tôi')).toHaveCount(0);
    await expect(page.getByRole('heading', { name: 'Bình luận (2)' })).toBeVisible();
  });

  test('AC-46.3: markup in a comment is shown as text and never runs', async ({ page }) => {
    const server = new CommentServer();
    const payload = '<img src=x onerror="window.__xssRan=true"><script>window.__xssRan=true</script>';
    server.add(OTHER, payload);
    await server.install(page);

    await openRecipe(page);
    await expect(page.getByText(payload, { exact: true })).toBeVisible();
    expect(await page.evaluate(() => (window as Window & { __xssRan?: boolean }).__xssRan)).toBeUndefined();
    await expect(page.locator('section[aria-labelledby="recipe-comments-heading"] img[src="x"]')).toHaveCount(0);
  });

  test('server validation errors are shown and an empty comment is not sent', async ({ page }) => {
    const server = new CommentServer();
    await server.install(page);
    await seedDemoSession(page, 'CUSTOMER');

    await openRecipe(page);
    await expect(page.getByText('Chưa có bình luận. Hãy bắt đầu cuộc trò chuyện.')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Gửi bình luận' })).toBeDisabled();
    await page.getByLabel('Viết bình luận').fill('   ');
    await expect(page.getByRole('button', { name: 'Gửi bình luận' })).toBeDisabled();
    await page.route(new RegExp(`/api/v1/recipes/${RECIPE_ID}/comments$`), (route) => route.request().method() === 'POST'
      ? route.fulfill({ status: 400, json: { status: 400, code: 'VALIDATION_FAILED', detail: 'Dữ liệu gửi lên không hợp lệ.', errors: [{ field: 'content', message: 'Bình luận cần từ 1 đến 1.000 ký tự.' }] } })
      : route.fallback());
    await page.getByLabel('Viết bình luận').fill('Nội dung bị máy chủ từ chối');
    await page.getByRole('button', { name: 'Gửi bình luận' }).click();
    await expect(page.getByRole('alert')).toHaveText('Bình luận cần từ 1 đến 1.000 ký tự.');
    await expect(page.getByLabel('Viết bình luận')).toHaveValue('Nội dung bị máy chủ từ chối');
  });

  test('more threads load on demand, and a failed load can be retried', async ({ page }) => {
    const server = new CommentServer();
    for (let index = 1; index <= 12; index += 1) server.add(OTHER, `Luồng số ${index}`);
    await server.install(page);
    let failNext = true;
    await page.route(new RegExp(`/api/v1/recipes/${RECIPE_ID}/comments`), (route) => {
      if (failNext && route.request().method() === 'GET') {
        failNext = false;
        return route.fulfill({ status: 500, json: { status: 500, code: 'INTERNAL_ERROR', detail: 'Hệ thống gặp lỗi.' } });
      }
      return route.fallback();
    });

    await openRecipe(page);
    await expect(page.getByRole('alert')).toContainText('Không tải được bình luận.');
    await page.getByRole('button', { name: 'Thử lại', exact: true }).click();
    await expect(page.getByText('Luồng số 12', { exact: true })).toBeVisible();
    await expect(page.getByText('Luồng số 2', { exact: true })).toHaveCount(0);
    await page.getByRole('button', { name: 'Xem thêm bình luận' }).click();
    await expect(page.getByText('Luồng số 1', { exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Xem thêm bình luận' })).toHaveCount(0);
  });

  test('an administrator reads comments but has no comment form', async ({ page }) => {
    const server = new CommentServer();
    server.add(OTHER, 'Bình luận công khai');
    await server.install(page);
    await seedDemoSession(page, 'ADMIN');

    await openRecipe(page);
    await expect(page.getByText('Bình luận công khai')).toBeVisible();
    await expect(page.getByLabel('Viết bình luận')).toHaveCount(0);
    await expect(page.getByRole('button', { name: 'Trả lời' })).toHaveCount(0);
    await expect(page.getByText('Tài khoản quản trị viên không đăng bình luận.')).toBeVisible();
  });
});
