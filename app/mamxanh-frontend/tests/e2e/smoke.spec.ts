import { expect, test } from '@playwright/test';

test('loads the application shell and navigates to Explore', async ({ page }) => {
  const pageErrors: string[] = [];
  page.on('pageerror', (error) => pageErrors.push(error.message));

  await page.goto('/');
  await expect(page.getByRole('heading', { name: /Sống Xanh An Lành/i })).toBeVisible();

  await page.getByRole('link', { name: 'Khám phá món chay' }).click();

  await expect(page).toHaveURL(/\/kham-pha$/);
  await expect(page.getByRole('heading', { name: 'Khám phá công thức chay' })).toBeVisible();
  expect(pageErrors).toEqual([]);
});
