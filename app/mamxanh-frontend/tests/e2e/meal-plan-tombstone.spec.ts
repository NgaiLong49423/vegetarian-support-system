import { expect, test } from './baseFixtures';
import type { Page } from '@playwright/test';

async function signInAsCustomer(page: Page) {
  await page.goto('/');
  await page.evaluate(() => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
    accessToken: 'playwright-only-token',
    expiresAt: Date.now() + 60 * 60 * 1000,
    account: {
      id: 20,
      displayName: 'Customer Issue 47',
      email: 'issue47-customer@example.test',
      role: 'CUSTOMER',
      accountStatus: 'ACTIVE',
      emailVerified: true,
    },
  })));
}

function nextDay(date: string) {
  const day = new Date(`${date}T00:00:00`);
  day.setDate(day.getDate() + 1);
  return `${day.getFullYear()}-${String(day.getMonth() + 1).padStart(2, '0')}-${String(day.getDate()).padStart(2, '0')}`;
}

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => {
    if (sessionStorage.getItem('mamx.auth')) return;
    sessionStorage.setItem('mamx.auth', JSON.stringify({
      accessToken: 'playwright-only-token',
      expiresAt: Date.now() + 60 * 60 * 1000,
      account: {
        id: 20,
        displayName: 'Customer Issue 47',
        email: 'issue47-customer@example.test',
        role: 'EXPERT',
        accountStatus: 'ACTIVE',
        emailVerified: true,
      },
    }));
  });
});

test('meal plan retains a deleted recipe slot and displays the author tombstone', async ({ page }) => {
  await page.route('**/api/v1/meal-plans**', (route) => {
    const url = new URL(route.request().url());
    const weekStartDate = url.searchParams.get('weekStartDate') ?? '2026-10-05';
    return route.fulfill({ json: {
      weekStartDate,
      entries: [{
        entryId: 91,
        mealDate: nextDay(weekStartDate),
        mealType: 'LUNCH',
        plannedServings: 2,
        recipeId: 47,
        recipeTitle: null,
        recipeCoverUrl: null,
        dishCategory: null,
        totalTimeMinutes: null,
        recipeDeleted: true,
        unavailableMessage: 'Công thức này đã bị xóa bởi tác giả',
      }],
    } });
  });
  await signInAsCustomer(page);
  await page.goto('/ke-hoach');

  await expect(page.getByText('Lịch ăn của bạn')).toBeVisible();
  await expect(page.getByText('Bữa trưa')).toBeVisible();
  await expect(page.getByText('Công thức này đã bị xóa bởi tác giả')).toBeVisible();
  await expect(page.locator('a[href="/cong-thuc/id/47"]')).toHaveCount(0);
});

test('meal plan links an available recipe and renders an empty week without mock meals', async ({ page }) => {
  await page.route('**/api/v1/meal-plans**', (route) => {
    const weekStartDate = new URL(route.request().url()).searchParams.get('weekStartDate') ?? '2026-10-05';
    return route.fulfill({ json: {
      weekStartDate,
      entries: [{
        entryId: 92,
        mealDate: nextDay(weekStartDate),
        mealType: 'DINNER',
        plannedServings: 2,
        recipeId: 48,
        recipeTitle: 'Canh nấm mới',
        recipeCoverUrl: 'https://images.example.test/canh.jpg',
        dishCategory: 'SOUP',
        totalTimeMinutes: 25,
        recipeDeleted: false,
        unavailableMessage: null,
      }],
    } });
  });
  await signInAsCustomer(page);
  await page.goto('/ke-hoach');
  await expect(page.getByRole('link', { name: 'Canh nấm mới' })).toHaveAttribute('href', '/cong-thuc/id/48');
  await expect(page.getByText('Bữa tối')).toBeVisible();
  await expect(page.getByText('25 phút · 2 phần')).toBeVisible();
  await expect(page.getByText('Chưa có món.').first()).toBeVisible();
});

test('empty meal plan and server errors show recoverable states', async ({ page }) => {
  let fail = false;
  await page.route('**/api/v1/meal-plans**', (route) => {
    if (fail) return route.fulfill({ status: 503, contentType: 'application/problem+json', body: JSON.stringify({ status: 503, code: 'SERVICE_UNAVAILABLE', detail: 'Không tải được Lịch ăn.' }) });
    const weekStartDate = new URL(route.request().url()).searchParams.get('weekStartDate') ?? '2026-10-05';
    return route.fulfill({ json: { weekStartDate, entries: [] } });
  });
  await signInAsCustomer(page);
  await page.goto('/ke-hoach');
  await expect(page.getByText('Tuần này chưa có công thức trong Lịch ăn.')).toBeVisible();

  fail = true;
  await page.reload();
  await expect(page.getByRole('alert')).toContainText('Không tải được Lịch ăn.');
});
