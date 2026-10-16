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
  title: string;
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
  // Use the two oldest published recipes of the seeded demo Expert. The newest recipes may belong to a
  // real-stack spec running in another worker (FR-09, FR-08) that hides or deletes them mid-test.
  const ids = sql(`SELECT TOP 2 p.recipe_id FROM [RECIPE_POST] p JOIN [USER] u ON u.user_id = p.author_id
    WHERE u.email = 'demo-expert@mamxanh.local' AND p.status = 'PUBLISHED' ORDER BY p.recipe_id`)
    .split(/\s+/).map(Number);
  expect(ids, 'Compose local demo data must include two published demo Expert recipes').toHaveLength(2);
  const [a, b] = await Promise.all(ids.map(async (recipeId) => {
    const response = await request.get(`${backendUrl}/recipes/${recipeId}`);
    expect(response.status(), `GET /recipes/${recipeId}`).toBe(200);
    return response.json() as Promise<Detail>;
  }));
  const views = () => sql(`SELECT COUNT_BIG(*) FROM [RECIPE_VIEW] WHERE recipe_id IN (${a.recipeId}, ${b.recipeId})`);
  const viewsBefore = views();
  const writes: string[] = [];
  page.on('request', (sent) => {
    if (sent.url().includes('/api/v1/') && sent.method() !== 'GET') writes.push(`${sent.method()} ${sent.url()}`);
  });

  // Choose the second recipe on the separate picker page through the real FR-08 search.
  await page.goto(`/so-sanh?a=${a.recipeId}`);
  await page.getByRole('region', { name: 'Công thức thứ hai' }).getByRole('link', { name: 'Chọn công thức thứ hai' }).click();
  await expect(page).toHaveURL(new RegExp(`/so-sanh/chon-mon\\?a=${a.recipeId}$`));
  await expect(page.getByText(`Món bạn chọn sẽ được so sánh với ${a.title}.`)).toBeVisible();
  const searched = page.waitForResponse((response) => {
    const url = new URL(response.url());
    return url.pathname.endsWith('/api/v1/recipes') && url.searchParams.get('keyword') === b.title;
  });
  await page.getByRole('searchbox', { name: 'Tìm công thức' }).fill(b.title);
  expect((await searched).status()).toBe(200);
  const choices = page.getByRole('list', { name: 'Công thức có thể chọn' });
  await choices.getByRole('button', { name: `Chọn ${b.title}`, exact: true }).click();
  await expect(page).toHaveURL(new RegExp(`/so-sanh\\?a=${a.recipeId}&b=${b.recipeId}$`));

  const table = page.getByRole('table', { name: 'Dinh dưỡng trên một khẩu phần' });
  await expect(table).toBeVisible();
  await expect(page.getByText(`${a.title} có ${a.servings} khẩu phần gốc, ${b.title} có ${b.servings} khẩu phần gốc.`)).toBeVisible();
  for (const [key, label, unit] of NUTRIENTS) {
    const cells = table.getByRole('row', { name: new RegExp(label) }).getByRole('cell');
    const valueA = a.nutrition.complete ? a.nutrition.perServing[key] : null;
    const valueB = b.nutrition.complete ? b.nutrition.perServing[key] : null;
    await expect(cells.nth(0)).toHaveText(valueA === null ? 'Chưa đủ dữ liệu' : `${amount(valueA)} ${unit}`);
    await expect(cells.nth(1)).toHaveText(valueB === null ? 'Chưa đủ dữ liệu' : `${amount(valueB)} ${unit}`);
    await expect(cells.nth(2)).toHaveText(valueA === null || valueB === null ? '—' : `${amount(valueA - valueB, true)} ${unit}`);
  }

  await page.goto(`/so-sanh?a=${a.recipeId}&b=999999999999999`);
  await expect(page.getByRole('alert')).toContainText('Công thức thứ hai không còn công khai. Hãy chọn công thức khác.');
  expect(writes).toEqual([]);
  expect(views()).toBe(viewsBefore);
});
