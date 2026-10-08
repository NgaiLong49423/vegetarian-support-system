import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, test } from '@playwright/test';

const frontendDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(frontendDir, '../..');
const composeProject = process.env.MAMXANX_E2E_COMPOSE_PROJECT ?? 'mamxanh-dev';
const frontendBaseUrl = process.env.MAMXANX_E2E_FRONTEND_URL ?? 'http://localhost:5173';
const fixtureId = randomUUID();
const authorEmail = `issue14-${fixtureId}@test.local`;
const ingredientAName = `Issue14 ingredient A ${fixtureId}`;
const ingredientBName = `Issue14 ingredient B ${fixtureId}`;
const recipeBoth = `Issue14 both ingredients ${fixtureId}`;
const recipeAOnly = `Issue14 ingredient A only ${fixtureId}`;
const recipeBOnly = `Issue14 ingredient B only ${fixtureId}`;

function literal(value: string): string {
  return `N'${value.replaceAll("'", "''")}'`;
}

function sql(query: string): string {
  return execFileSync('docker', [
    'compose', '-p', composeProject, '-f', path.join(repositoryRoot, 'docker-compose.yml'),
    'exec', '-T', 'sqlserver', '/opt/mssql-tools18/bin/sqlcmd',
    '-S', 'sqlserver', '-U', 'sa', '-C', '-d', 'MamXanhDB', '-h', '-1', '-W', '-b',
    '-Q', `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`,
  ], { cwd: repositoryRoot, encoding: 'utf8', timeout: 30_000 }).trim();
}

function cleanupFixture(): void {
  sql(`DELETE FROM [RECIPE_POST] WHERE title IN (${literal(recipeBoth)}, ${literal(recipeAOnly)}, ${literal(recipeBOnly)});
    DELETE FROM [INGREDIENT] WHERE name IN (${literal(ingredientAName)}, ${literal(ingredientBName)});
    DELETE FROM [USER] WHERE email = ${literal(authorEmail)};`);
}

function createRecipe(title: string, ingredientIds: number[], prepMinutes: number, cookMinutes: number): number {
  const authorId = Number(sql(`SELECT user_id FROM [USER] WHERE email = ${literal(authorEmail)}`));
  sql(`INSERT INTO [RECIPE_POST]
      (author_id, title, description, instructions, dish_category, vegetarian_type, difficulty, servings,
       prep_time_min, cook_time_min, status, published_at)
    VALUES (${authorId}, ${literal(title)}, N'Issue 14 real SQL fixture', N'Nấu chín và nêm vừa ăn.',
       'SOUP', 'VEGAN', 'EASY', 2, ${prepMinutes}, ${cookMinutes}, 'PUBLISHED', SYSUTCDATETIME());`);
  const recipeId = Number(sql(`SELECT recipe_id FROM [RECIPE_POST] WHERE title = ${literal(title)}`));
  const gramUnitId = Number(sql("SELECT unit_id FROM [UNIT] WHERE code = N'g'"));
  for (const ingredientId of ingredientIds) {
    sql(`INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
      VALUES (${recipeId}, ${ingredientId}, ${gramUnitId}, 100);`);
  }
  return recipeId;
}

test('Explore filters actual SQL Server recipes by every selected ingredient', async ({ page }) => {
  let recipeBothId: number | null = null;
  try {
    sql(`INSERT INTO [USER] (email, display_name, role, account_status, email_verified)
      VALUES (${literal(authorEmail)}, N'Issue 14 integration author', 'EXPERT', 'ACTIVE', 1);
      INSERT INTO [INGREDIENT] (name, source_name, reference_date)
      VALUES (${literal(ingredientAName)}, N'Issue #14 real-stack test', '2026-10-08');
      INSERT INTO [INGREDIENT] (name, source_name, reference_date)
      VALUES (${literal(ingredientBName)}, N'Issue #14 real-stack test', '2026-10-08');`);
    const ingredientAId = Number(sql(`SELECT ingredient_id FROM [INGREDIENT] WHERE name = ${literal(ingredientAName)}`));
    const ingredientBId = Number(sql(`SELECT ingredient_id FROM [INGREDIENT] WHERE name = ${literal(ingredientBName)}`));
    recipeBothId = createRecipe(recipeBoth, [ingredientAId, ingredientBId], 5, 10);
    createRecipe(recipeAOnly, [ingredientAId], 5, 10);
    createRecipe(recipeBOnly, [ingredientBId], 5, 10);
    expect(Number(sql(`SELECT COUNT(*) FROM [RECIPE_POST] WHERE recipe_id = ${recipeBothId} AND status = 'PUBLISHED'`))).toBe(1);

    const formOptionsResponsePromise = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return response.request().method() === 'GET' && url.pathname === '/api/v1/recipes/form-options';
    });
    await page.goto('/kham-pha');
    expect((await formOptionsResponsePromise).ok()).toBeTruthy();
    await expect(page.getByRole('heading', { name: 'Khám phá công thức chay' })).toBeVisible();
    await page.getByRole('combobox', { name: 'Trường phái ăn chay' }).selectOption('VEGAN');
    await page.getByRole('combobox', { name: 'Thể loại món' }).selectOption('SOUP');
    await page.getByRole('combobox', { name: 'Tổng thời gian tối đa' }).selectOption('30');

    await page.getByRole('textbox', { name: 'Nguyên liệu (kết quả phải có đủ nguyên liệu đã chọn)' }).fill(ingredientAName);
    await page.getByRole('checkbox', { name: ingredientAName }).check();
    await page.getByRole('textbox', { name: 'Nguyên liệu (kết quả phải có đủ nguyên liệu đã chọn)' }).fill(ingredientBName);
    const filteredRequestPromise = page.waitForRequest((request) => {
      const url = new URL(request.url());
      return request.method() === 'GET' && url.pathname === '/api/v1/recipes';
    });
    await page.getByRole('checkbox', { name: ingredientBName }).check();
    const filteredRequest = await filteredRequestPromise;
    const requestUrl = new URL(filteredRequest.url());
    expect(new Set(requestUrl.searchParams.getAll('ingredientIds'))).toEqual(new Set([String(ingredientAId), String(ingredientBId)]));
    const filteredResponse = await filteredRequest.response();
    if (!filteredResponse) throw new Error('Recipe search request failed before Backend returned a response.');
    expect(filteredResponse.ok()).toBeTruthy();
    expect(requestUrl.searchParams.get('vegetarianType')).toBe('VEGAN');
    expect(requestUrl.searchParams.get('dishCategory')).toBe('SOUP');
    expect(requestUrl.searchParams.get('maxTotalTimeMinutes')).toBe('30');
    const payload = await filteredResponse.json() as { items: Array<{ id: number; title: string }>; totalElements: number };
    expect(payload.totalElements).toBe(1);
    expect(payload.items).toHaveLength(1);
    expect(payload.items[0]).toMatchObject({ id: recipeBothId, title: recipeBoth });
    await expect(page.getByRole('link', { name: new RegExp(recipeBoth) })).toBeVisible();
    await expect(page.getByRole('link', { name: new RegExp(recipeAOnly) })).toHaveCount(0);
    await expect(page.getByRole('link', { name: new RegExp(recipeBOnly) })).toHaveCount(0);

    for (const sort of ['NEWEST', 'MOST_LIKED', 'MOST_VIEWED', 'MOST_COMMENTED', 'MOST_ACTIVE', 'TRENDING']) {
      const sortResponsePromise = page.waitForResponse((response) => {
        const url = new URL(response.url());
        return response.request().method() === 'GET' && url.origin + url.pathname === `${frontendBaseUrl}/api/v1/recipes`
          && url.searchParams.get('sort') === sort;
      });
      await page.getByRole('combobox', { name: 'Sắp xếp theo' }).selectOption(sort);
      const sortResponse = await sortResponsePromise;
      expect(sortResponse.ok()).toBeTruthy();
      if (sort === 'MOST_LIKED') {
        await expect(page.getByText('Mới', { exact: true })).toBeVisible();
      }
      if (sort === 'MOST_VIEWED') {
        const periodResponsePromise = page.waitForResponse((response) => {
          const url = new URL(response.url());
          return response.request().method() === 'GET' && url.origin + url.pathname === `${frontendBaseUrl}/api/v1/recipes`
            && url.searchParams.get('sort') === 'MOST_VIEWED' && url.searchParams.get('viewPeriod') === 'LAST_7_DAYS';
        });
        await page.getByRole('combobox', { name: 'Khung thời gian lượt xem' }).selectOption('LAST_7_DAYS');
        expect((await periodResponsePromise).ok()).toBeTruthy();
      }
    }

    const emptyResponsePromise = page.waitForResponse((response) => {
      const url = new URL(response.url());
      return response.request().method() === 'GET' && url.origin + url.pathname === `${frontendBaseUrl}/api/v1/recipes`
        && url.searchParams.get('vegetarianType') === 'LACTO';
    });
    await page.getByRole('combobox', { name: 'Trường phái ăn chay' }).selectOption('LACTO');
    expect((await emptyResponsePromise).ok()).toBeTruthy();
    await expect(page.getByText('Không tìm thấy công thức phù hợp', { exact: true })).toBeVisible();
    await page.getByRole('button', { name: 'Đặt lại bộ lọc' }).last().click();
    await expect(page.getByRole('link', { name: new RegExp(recipeBoth) })).toBeVisible();
  } finally {
    cleanupFixture();
  }
});
