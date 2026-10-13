import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, test } from '@playwright/test';

const frontendDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(frontendDir, '../..');
const composeProject = process.env.MAMXANX_E2E_COMPOSE_PROJECT ?? 'mamxanh-dev';
const ingredientName = `Issue46 integration ${randomUUID()}`;
const demoPassword = process.env.MAMXANH_DEMO_PASSWORD;

function sql(query: string): string {
  return execFileSync('docker', [
    'compose', '-p', composeProject, '-f', path.join(repositoryRoot, 'docker-compose.yml'),
    'exec', '-T', 'sqlserver', '/opt/mssql-tools18/bin/sqlcmd',
    '-S', 'sqlserver', '-U', 'sa', '-C', '-d', 'MamXanhDB', '-h', '-1', '-W', '-b',
    '-Q', `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`,
  ], { cwd: repositoryRoot, encoding: 'utf8', timeout: 30_000 }).trim();
}

function textLiteral(value: string): string {
  return `N'${value.replaceAll("'", "''")}'`;
}

test('Administrator manages ingredient nutrition through the real Frontend, Backend and SQL Server', async ({ page }) => {
  test.skip(!demoPassword, 'Compose integration requires MAMXANH_DEMO_PASSWORD for the seeded Administrator.');
  const ingredientSqlName = textLiteral(ingredientName);
  try {
    await page.goto('/dang-nhap');
    await page.getByLabel('Email').fill('demo-admin@mamxanh.local');
    await page.getByRole('textbox', { name: 'Mật khẩu' }).fill(demoPassword!);
    const loginResponse = page.waitForResponse((response) => response.request().method() === 'POST' && response.url().endsWith('/auth/login'));
    await page.getByRole('button', { name: 'Đăng nhập' }).click();
    expect((await loginResponse).status()).toBe(200);

    await page.goto('/quan-tri/danh-muc');
    await expect(page.getByRole('heading', { name: 'Danh mục nguyên liệu dinh dưỡng' })).toBeVisible();
    await page.getByRole('button', { name: 'Mở biểu mẫu thêm nguyên liệu' }).click();
    await expect(page.getByRole('heading', { name: 'Thêm nguyên liệu' })).toBeVisible();
    await page.getByLabel('Tên chuẩn tiếng Việt').fill(ingredientName);
    const ingredientGroup = page.getByRole('combobox', { name: 'Nhóm nguyên liệu' }).last();
    const availableGroupOption = ingredientGroup.locator('option:not([disabled])').first();
    await expect(availableGroupOption).toBeAttached();
    const ingredientGroupOption = await availableGroupOption.getAttribute('value');
    expect(ingredientGroupOption).toBeTruthy();
    await ingredientGroup.selectOption(ingredientGroupOption!);
    const ingredientSource = page.getByRole('combobox', { name: 'Tên nguồn dữ liệu' });
    const availableSourceOption = ingredientSource.locator('option:not([disabled])').first();
    await expect(availableSourceOption).toBeAttached();
    const ingredientSourceOption = await availableSourceOption.getAttribute('value');
    expect(ingredientSourceOption).toBeTruthy();
    await ingredientSource.selectOption(ingredientSourceOption!);
    await page.getByLabel('Đường dẫn nguồn (không bắt buộc)').fill('https://fdc.nal.usda.gov/');
    const createResponse = page.waitForResponse((response) => response.request().method() === 'POST' && response.url().endsWith('/admin/ingredients'));
    await page.getByRole('button', { name: 'Thêm nguyên liệu', exact: true }).click();
    expect((await createResponse).status()).toBe(201);

    await page.getByRole('button', { name: `Sửa dinh dưỡng ${ingredientName}` }).click();
    const values: Record<string, string> = {
      'Năng lượng trên 100 g': '100',
      'Chất đạm trên 100 g': '10',
      'Carbohydrate trên 100 g': '20',
      'Chất béo trên 100 g': '3',
      'Chất xơ trên 100 g': '4',
      'Canxi trên 100 g': '20',
      'Sắt trên 100 g': '2',
      'Vitamin B12 trên 100 g': '0.1',
      'Kẽm trên 100 g': '1.3',
    };
    for (const [label, value] of Object.entries(values)) await page.getByLabel(label).fill(value);
    const nutritionResponse = page.waitForResponse((response) => response.request().method() === 'PUT' && /\/admin\/ingredients\/\d+\/nutrition$/.test(new URL(response.url()).pathname));
    await page.getByRole('button', { name: 'Lưu chỉ tiêu' }).click();
    expect((await nutritionResponse).status()).toBe(200);

    const supportResponse = page.waitForResponse((response) => response.request().method() === 'PATCH' && response.url().endsWith('/nutrition-support'));
    await page.getByRole('button', { name: 'Bật hỗ trợ tính' }).click();
    expect((await supportResponse).status()).toBe(200);
    await expect(page.getByText('Đang hỗ trợ tính', { exact: true })).toBeVisible();

    await page.reload();
    await expect(page.getByText(ingredientName, { exact: true })).toBeVisible();
    await expect(page.getByText('Có dữ liệu tính toán', { exact: true })).toBeVisible();
    expect(sql(`SELECT CONCAT(CONVERT(varchar(20), energy_kcal_100g), '|', CONVERT(varchar(1), nutrition_supported)) FROM INGREDIENT WHERE name = ${ingredientSqlName}`)).toBe('100.00|1');
  } finally {
    sql(`DELETE FROM INGREDIENT WHERE name = ${ingredientSqlName}`);
  }
});
