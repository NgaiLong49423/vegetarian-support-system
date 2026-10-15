import type { Page, Route } from '@playwright/test';
import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

// FR-59 (Issue #69): follow, unfollow, counts and follower/following lists on the public profile.

const VIEWER_ID = 901; // id of the account created by seedDemoSession
const profile = { userId: 22, displayName: 'Bếp Chay Lan', avatarUrl: null, bio: null, joinedMonth: '2026-09' };

const problem = (status: number, code: string, detail: string) => ({
  status,
  contentType: 'application/problem+json',
  body: JSON.stringify({ status, code, detail }),
});

const member = (userId: number, displayName: string) => ({ userId, displayName, avatarUrl: null });

/** Stand-in for the FR-59 endpoints with the Backend's idempotent follow rules. */
class FollowServer {
  following = false;
  followerCount = 3;
  followingCount = 1;
  requests: string[] = [];
  followers = Array.from({ length: 25 }, (_, index) => member(100 + index, `Người theo dõi ${index + 1}`));
  followingList = [member(40, 'Bếp Nhà Mây')];
  failNextFollow: ReturnType<typeof problem> | null = null;

  status() {
    return { userId: profile.userId, following: this.following, followerCount: this.followerCount, followingCount: this.followingCount };
  }

  async install(page: Page, target = profile) {
    await page.route(`**/api/v1/members/${target.userId}`, (route) => route.fulfill({ json: target }));
    await page.route(`**/api/v1/members/${target.userId}/recipes**`, (route) =>
      route.fulfill({ json: { items: [], page: 0, size: 12, totalElements: 0, totalPages: 0 } }));
    await page.route(`**/api/v1/members/${target.userId}/follow`, (route) => this.onFollow(route));
    await page.route(new RegExp(`/api/v1/members/${target.userId}/(followers|following)`), (route) => this.onList(route));
  }

  private async onFollow(route: Route) {
    const method = route.request().method();
    this.requests.push(method);
    if (method === 'GET') return route.fulfill({ json: this.status() });
    if (this.failNextFollow) {
      const failure = this.failNextFollow;
      this.failNextFollow = null;
      return route.fulfill(failure);
    }
    if (method === 'PUT' && !this.following) { this.following = true; this.followerCount += 1; }
    if (method === 'DELETE' && this.following) { this.following = false; this.followerCount -= 1; }
    return route.fulfill({ json: this.status() });
  }

  private async onList(route: Route) {
    const url = new URL(route.request().url());
    this.requests.push(`GET ${url.pathname.split('/').pop()}?page=${url.searchParams.get('page')}`);
    const source = url.pathname.endsWith('/followers') ? this.followers : this.followingList;
    const page = Number(url.searchParams.get('page') ?? 0);
    const size = Number(url.searchParams.get('size') ?? 20);
    return route.fulfill({ json: {
      content: source.slice(page * size, page * size + size), page, size,
      totalElements: source.length, totalPages: Math.ceil(source.length / size),
    } });
  }
}

test('UC-59.3: a Guest sees the counts but must sign in to follow or open the lists', async ({ page }) => {
  const server = new FollowServer();
  await server.install(page);
  await page.goto('/thanh-vien/22');

  await expect(page.getByRole('button', { name: '3 người theo dõi' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Đang theo dõi 1' })).toBeVisible();
  await page.getByRole('button', { name: '3 người theo dõi' }).click();
  await expect(page.getByText('Đăng nhập để xem danh sách người theo dõi.')).toBeVisible();
  expect(server.requests.filter((request) => request.startsWith('GET followers'))).toHaveLength(0);

  await page.getByRole('link', { name: 'Theo dõi' }).click();
  await expect(page).toHaveURL(/\/dang-nhap$/);
});

test('AC-59.1, AC-59.5: a member follows and unfollows, and the follower count follows the Backend', async ({ page }) => {
  const server = new FollowServer();
  await server.install(page);
  await seedDemoSession(page, 'CUSTOMER');
  await page.goto('/thanh-vien/22');

  const follow = page.getByRole('button', { name: 'Theo dõi', exact: true });
  await follow.click();
  await expect(page.getByRole('button', { name: 'Đang theo dõi', exact: true })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByRole('button', { name: '4 người theo dõi' })).toBeVisible();

  await page.getByRole('button', { name: 'Đang theo dõi', exact: true }).click();
  await expect(page.getByRole('button', { name: 'Theo dõi', exact: true })).toHaveAttribute('aria-pressed', 'false');
  await expect(page.getByRole('button', { name: '3 người theo dõi' })).toBeVisible();
  expect(server.requests).toEqual(['GET', 'PUT', 'DELETE']);
});

test('AC-59.7: following a locked member shows the Backend message and keeps the state', async ({ page }) => {
  const server = new FollowServer();
  server.failNextFollow = problem(409, 'FOLLOW_TARGET_INACTIVE', 'Tài khoản này hiện không hoạt động nên không thể theo dõi.');
  await server.install(page);
  await seedDemoSession(page, 'EXPERT');
  await page.goto('/thanh-vien/22');

  await page.getByRole('button', { name: 'Theo dõi', exact: true }).click();
  await expect(page.getByRole('alert')).toHaveText('Tài khoản này hiện không hoạt động nên không thể theo dõi.');
  await expect(page.getByRole('button', { name: 'Theo dõi', exact: true })).toHaveAttribute('aria-pressed', 'false');
  await expect(page.getByRole('button', { name: '3 người theo dõi' })).toBeVisible();
});

test('AC-59.3: no follow button on the own profile or for an administrator', async ({ page }) => {
  const server = new FollowServer();
  await server.install(page, { ...profile, userId: VIEWER_ID, displayName: 'Demo CUSTOMER' });
  await seedDemoSession(page, 'CUSTOMER');
  await page.goto(`/thanh-vien/${VIEWER_ID}`);
  await expect(page.getByRole('button', { name: '3 người theo dõi' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Theo dõi', exact: true })).toHaveCount(0);

  const admin = await page.context().newPage();
  await new FollowServer().install(admin);
  await seedDemoSession(admin, 'ADMIN');
  await admin.goto('/thanh-vien/22');
  await expect(admin.getByRole('button', { name: '3 người theo dõi' })).toBeVisible();
  await expect(admin.getByRole('button', { name: 'Theo dõi', exact: true })).toHaveCount(0);
});

test('UC-59.4, AC-59.6: members browse paginated follower and following lists with public profile links', async ({ page }) => {
  const server = new FollowServer();
  await server.install(page);
  await seedDemoSession(page, 'CUSTOMER');
  await page.goto('/thanh-vien/22');

  await page.getByRole('button', { name: '3 người theo dõi' }).click();
  const list = page.getByRole('region', { name: 'Người theo dõi' });
  await expect(list.getByRole('link', { name: 'Người theo dõi 1', exact: true })).toHaveAttribute('href', '/thanh-vien/100');
  await expect(list.getByRole('listitem')).toHaveCount(20);
  await list.getByRole('button', { name: 'Xem thêm' }).click();
  await expect(list.getByRole('listitem')).toHaveCount(25);
  await expect(list.getByRole('button', { name: 'Xem thêm' })).toHaveCount(0);

  await page.getByRole('button', { name: 'Đang theo dõi 1' }).click();
  const following = page.getByRole('region', { name: 'Đang theo dõi' });
  await expect(following.getByRole('link', { name: 'Bếp Nhà Mây' })).toHaveAttribute('href', '/thanh-vien/40');
  server.followingList = [];
  await page.getByRole('button', { name: '3 người theo dõi' }).click();
  await page.getByRole('button', { name: 'Đang theo dõi 1' }).click();
  await expect(page.getByText('Chưa theo dõi thành viên nào.')).toBeVisible();
});

test('a failed follow summary does not break the profile and can be retried', async ({ page }) => {
  const server = new FollowServer();
  await server.install(page);
  let failSummary = true;
  await page.route('**/api/v1/members/22/follow', (route) => {
    if (failSummary && route.request().method() === 'GET') {
      failSummary = false;
      return route.fulfill(problem(500, 'INTERNAL_ERROR', 'Hệ thống gặp lỗi.'));
    }
    return route.fallback();
  });
  await page.goto('/thanh-vien/22');

  await expect(page.getByRole('heading', { name: 'Bếp Chay Lan', level: 1 })).toBeVisible();
  await expect(page.getByText('Không tải được thông tin theo dõi.')).toBeVisible();
  await page.getByRole('button', { name: 'Tải lại' }).click();
  await expect(page.getByRole('button', { name: '3 người theo dõi' })).toBeVisible();
});
