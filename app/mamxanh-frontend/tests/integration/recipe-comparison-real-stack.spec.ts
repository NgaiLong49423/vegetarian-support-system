import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, test } from '@playwright/test';

const frontendDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(frontendDir, '../..');
const backendUrl = process.env.MAMXANX_E2E_BACKEND_URL ?? 'http://localhost:8080/api/v1';
const composeProject = process.env.MAMXANX_E2E_COMPOSE_PROJECT ?? 'mamxanh-dev';

const NUTRIENTS = [
  ['ENERGY_KCAL', 'Năng lượng', 'kcal'], ['PROTEIN_G', 'Chất đạm', 'g'], ['CARBOHYDRATE_G', 'Carbohydrate', 'g'],
  ['TOTAL_FAT_G', 'Chất béo', 'g'], ['FIBER_G', 'Chất xơ', 'g'], ['CALCIUM_MG', 'Canxi', 'mg'],
  ['IRON_MG', 'Sắt', 'mg'], ['VITAMIN_B12_MCG', 'Vitamin B12', 'mcg'], ['ZINC_MG', 'Kẽm', 'mg'],
] as const;

type Detail = {
  recipeId: number;
  servings: number;
  nutrition: { complete: boolean; perServing: Record<string, number> };
};

function sql(query: string): string {
  return execFileSync('docker', ['compose', '-p', composeProject, '-f', path.join(repositoryRoot, 'docker-compose.yml'),
    'exec', '-T', 'sqlserver', '/opt/mssql-tools18/bin/sqlcmd', '-S', 'sqlserver', '-U', 'sa', '-C', '-d',
    'MamXanhDB', '-h', '-1', '-W', '-b', '-Q', `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`],
  { cwd: repositoryRoot, encoding: 'utf8', timeout: 30_000 }).trim();
}

const amount = (value: number, sign = false) => new Intl.NumberFormat('vi-VN', {
  maximumFractionDigits: 2, signDisplay: sign ? 'exceptZero' : 'auto',
}).format(value);

test('FR-60 compares two published SQL Server recipes per serving using only read requests', async ({ page, request }) => {
  const catalog = await request.get(`${backendUrl}/recipes?size=2&sort=NEWEST`);
  expect(catalog.ok()).toBeTruthy();
  const { items } = await catalog.json() as { items: Array<{ id: number }> };
  expect(items.length, 'Compose local demo data must include two published recipes').toBeGreaterThanOrEqual(2);
  const [a, b] = await Promise.all(items.slice(0, 2).map(async (item) =>
    (await request.get(`${backendUrl}/recipes/${item.id}`)).json() as Promise<Detail>));
  expect(sql(`SELECT COUNT(*) FROM [RECIPE_POST] WHERE status = 'PUBLISHED' AND recipe_id IN (${a.recipeId}, ${b.recipeId})`)).toBe('2');
  const views = () => sql(`SELECT COUNT_BIG(*) FROM [RECIPE_VIEW] WHERE recipe_id IN (${a.recipeId}, ${b.recipeId})`);
  const viewsBefore = views();
  const writes: string[] = [];
  page.on('request', (sent) => {
    if (sent.url().includes('/api/v1/') && sent.method() !== 'GET') writes.push(`${sent.method()} ${sent.url()}`);
  });

  await page.goto(`/so-sanh?a=${a.recipeId}&b=${b.recipeId}`);
  const table = page.getByRole('table', { name: 'Dinh dưỡng trên một khẩu phần' });
  await expect(table).toBeVisible();
  await expect(page.getByText(`A có ${a.servings} khẩu phần gốc, B có ${b.servings} khẩu phần gốc.`)).toBeVisible();
  for (const [key, label, unit] of NUTRIENTS) {
    const cells = table.getByRole('row', { name: new RegExp(label) }).getByRole('cell');
    const valueA = a.nutrition.complete ? a.nutrition.perServing[key] : null;
    const valueB = b.nutrition.complete ? b.nutrition.perServing[key] : null;
    await expect(cells.nth(0)).toHaveText(valueA === null ? 'Chưa đủ dữ liệu' : `${amount(valueA)} ${unit}`);
    await expect(cells.nth(1)).toHaveText(valueB === null ? 'Chưa đủ dữ liệu' : `${amount(valueB)} ${unit}`);
    await expect(cells.nth(2)).toHaveText(valueA === null || valueB === null ? '—' : `${amount(valueA - valueB, true)} ${unit}`);
  }

  await page.goto(`/so-sanh?a=${a.recipeId}&b=999999999999999`);
  await expect(page.getByRole('alert')).toContainText('Công thức ở vị trí B không còn công khai. Hãy chọn công thức khác.');
  expect(writes).toEqual([]);
  expect(views()).toBe(viewsBefore);
});
