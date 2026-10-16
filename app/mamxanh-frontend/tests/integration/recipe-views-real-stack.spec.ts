import { createHash, randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, test } from '@playwright/test';

const repositoryRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../../..');
const composeProject = process.env.MAMXANX_E2E_COMPOSE_PROJECT ?? 'mamxanh-dev';

function literal(value: string): string { return `N'${value.replaceAll("'", "''")}'`; }

function sql(query: string): string {
  return execFileSync('docker', [
    'compose', '-p', composeProject, '-f', path.join(repositoryRoot, 'docker-compose.yml'),
    'exec', '-T', 'sqlserver', '/opt/mssql-tools18/bin/sqlcmd',
    '-S', 'sqlserver', '-U', 'sa', '-C', '-d', 'MamXanhDB', '-h', '-1', '-W', '-b',
    '-Q', `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`,
  ], { cwd: repositoryRoot, encoding: 'utf8', timeout: 30_000 }).trim();
}

test('FR-58: Guest detail, reload and Explore share a real persisted view count', async ({ page, context }) => {
  const fixture = randomUUID();
  const email = `issue57-views-${fixture}@test.local`;
  const title = `Issue57 views ${fixture}`;
  let recipeId: number | undefined;
  try {
    sql(`INSERT INTO [USER] (email, display_name, role, account_status, email_verified)
      VALUES (${literal(email)}, N'Tác giả kiểm thử FR-58', 'EXPERT', 'ACTIVE', 1);
      INSERT INTO [RECIPE_POST] (author_id, title, description, instructions, dish_category,
        vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, status, published_at)
      SELECT user_id, ${literal(title)}, N'Công thức kiểm thử lượt xem', N'Nấu nấm chín rồi nêm vừa ăn.',
        'SOUP', 'VEGAN', 'EASY', 2, 10, 15, 'PUBLISHED', SYSUTCDATETIME()
      FROM [USER] WHERE email = ${literal(email)};`);
    recipeId = Number(sql(`SELECT recipe_id FROM [RECIPE_POST] WHERE title = ${literal(title)}`));
    expect(recipeId).toBeGreaterThan(0);
    const viewUrl = `/api/v1/recipes/${recipeId}/views`;
    const firstResponse = page.waitForResponse((response) => response.request().method() === 'POST' && new URL(response.url()).pathname === viewUrl);
    await page.goto(`/cong-thuc/id/${recipeId}`);
    const recorded = await firstResponse;
    expect(recorded.status()).toBe(200);
    expect(await recorded.json()).toEqual({ recipeId, counted: true, viewCount: 1 });
    await expect(page.getByText('1 lượt xem', { exact: true })).toBeVisible();
    const viewer = (await context.cookies()).find((cookie) => cookie.name === 'mamxanh.recipe-viewer')?.value;
    expect(viewer).toBeTruthy();
    const hash = createHash('sha256').update(viewer!.toLowerCase()).digest('hex');
    expect(Number(sql(`SELECT COUNT(*) FROM [RECIPE_VIEW] WHERE recipe_id = ${recipeId}
      AND user_id IS NULL AND anonymous_viewer_hash = ${literal(hash)}`))).toBe(1);

    const reloadResponse = page.waitForResponse((response) => response.request().method() === 'POST' && new URL(response.url()).pathname === viewUrl);
    await page.reload();
    expect(await (await reloadResponse).json()).toEqual({ recipeId, counted: false, viewCount: 1 });
    await expect(page.getByText('1 lượt xem', { exact: true })).toBeVisible();
    expect(Number(sql(`SELECT COUNT(*) FROM [RECIPE_VIEW] WHERE recipe_id = ${recipeId}`))).toBe(1);
    expect(Number(sql(`SELECT view_count FROM [RECIPE_POST] WHERE recipe_id = ${recipeId}`))).toBe(1);

    await page.getByRole('link', { name: 'Khám phá', exact: true }).click();
    await page.getByRole('textbox', { name: 'Từ khóa', exact: true }).fill(title);
    const card = page.getByRole('link').filter({ has: page.getByRole('heading', { name: title, exact: true }) });
    await expect(card.getByText('1 lượt xem', { exact: true })).toBeVisible();
    const mostViewedResponse = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return response.request().method() === 'GET' && url.pathname === '/api/v1/recipes'
        && url.searchParams.get('keyword') === title && url.searchParams.get('sort') === 'MOST_VIEWED'
        && url.searchParams.get('viewPeriod') === 'ALL_TIME';
    });
    await page.getByRole('combobox', { name: 'Sắp xếp theo' }).selectOption('MOST_VIEWED');
    expect((await mostViewedResponse).status()).toBe(200);
    await expect(page.getByRole('combobox', { name: 'Khung thời gian lượt xem' })).toHaveValue('ALL_TIME');
    for (const period of ['LAST_24_HOURS', 'LAST_7_DAYS', 'LAST_30_DAYS', 'ALL_TIME']) {
      const responsePromise = page.waitForResponse((response) => {
        const url = new URL(response.url());
        return response.request().method() === 'GET' && url.pathname === '/api/v1/recipes' && url.searchParams.get('keyword') === title
          && url.searchParams.get('sort') === 'MOST_VIEWED' && url.searchParams.get('viewPeriod') === period;
      });
      await page.getByRole('combobox', { name: 'Khung thời gian lượt xem' }).selectOption(period);
      const response = await responsePromise;
      expect(response.status()).toBe(200);
      const result = await response.json();
      expect(result.items.map((item: { id: number; viewCount: number }) => [item.id, item.viewCount])).toEqual([[recipeId, 1]]);
      await expect(card.getByText('1 lượt xem', { exact: true })).toBeVisible();
    }
  } finally {
    sql(`DELETE FROM [RECIPE_VIEW] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST]
      WHERE author_id IN (SELECT user_id FROM [USER] WHERE email = ${literal(email)}));
      DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email = ${literal(email)});
      DELETE FROM [USER] WHERE email = ${literal(email)};`);
  }
});
