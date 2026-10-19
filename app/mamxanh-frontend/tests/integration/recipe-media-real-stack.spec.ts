import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, test } from '@playwright/test';

const root = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  '../../../..',
);
const backend =
  process.env.MAMXANX_E2E_BACKEND_URL ?? 'http://localhost:8080/api/v1';
const project = process.env.MAMXANX_E2E_COMPOSE_PROJECT ?? 'mamxanh-dev';
// Live credentials must never be persisted in Playwright traces.
test.use({ trace: 'off' });

function sql(query: string) {
  return execFileSync(
    'docker',
    [
      'compose',
      '-p',
      project,
      '-f',
      path.join(root, 'docker-compose.yml'),
      '--env-file',
      path.join(root, 'app/mamxanh-backend/.env'),
      'exec',
      '-T',
      'sqlserver',
      '/opt/mssql-tools18/bin/sqlcmd',
      '-S',
      'sqlserver',
      '-U',
      'sa',
      '-C',
      '-d',
      'MamXanhDB',
      '-h',
      '-1',
      '-W',
      '-b',
      '-Q',
      `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`,
    ],
    { cwd: root, encoding: 'utf8', timeout: 30_000 },
  ).trim();
}

test('fix #158: upload Azure thật, xuất bản, tải lại và hiển thị ảnh từ SQL Server', async ({
  page,
  request,
}) => {
  test.skip(
    process.env.MAMXANH_E2E_LIVE_AZURE !== '1',
    'Opt-in: requires a real Azure container with public Blob read access.',
  );
  const marker = randomUUID();
  const email = `fix158-${marker}@example.invalid`;
  const password = `Aa1!${randomUUID()}z`;
  const title = `Fix158 ${marker}`;
  const registered = await request.post(`${backend}/auth/register`, {
    data: {
      displayName: 'Test ảnh Azure',
      email,
      password,
      confirmPassword: password,
    },
  });
  expect(registered.status()).toBe(201);
  sql(
    `UPDATE [USER] SET email_verified=1, role='EXPERT', onboarding_invited_at=SYSUTCDATETIME() WHERE email='${email}'`,
  );
  const login = await request.post(`${backend}/auth/login`, {
    data: { email, password },
  });
  expect(login.status()).toBe(200);
  const session = await login.json();
  const headers = { Authorization: `Bearer ${session.accessToken}` };
  let recipeId: number | undefined;
  let publishedId: number | undefined;
  let blobUrl: string | undefined;
  try {
    // Create an owned cleanup target before uploading, so failed UI assertions can also release the blob.
    const source = await (
      await request.get(`${backend}/recipes?size=1`)
    ).json();
    const detail = await (
      await request.get(`${backend}/recipes/${source.items[0].id}`)
    ).json();
    const created = await request.post(`${backend}/recipes`, {
      headers,
      data: {
        title: `${title} cleanup`,
        instructions: 'Chuẩn bị nguyên liệu và nấu chín kỹ.',
        dishCategory: 'BRAISED',
        vegetarianType: 'VEGAN',
        difficulty: 'EASY',
        servings: 2,
        prepTimeMinutes: 5,
        cookTimeMinutes: 10,
        ingredients: detail.ingredients
          .slice(0, 1)
          .map((item: { ingredientId: number; unitId: number }) => ({
            ingredientId: item.ingredientId,
            unitId: item.unitId,
            quantity: 100,
          })),
        media: [],
      },
    });
    expect(created.status()).toBe(201);
    recipeId = (await created.json()).recipeId;
    expect(Number.isSafeInteger(recipeId)).toBeTruthy();
    await page.addInitScript((session) => {
      sessionStorage.setItem(
        'mamxanh.auth',
        JSON.stringify({
          accessToken: session.accessToken,
          expiresAt: Date.now() + session.expiresInSeconds * 1000,
          account: session.account,
        }),
      );
    }, session);
    await page.goto('/dang-cong-thuc');
    const uploadResponse = page.waitForResponse((response) =>
      response.url().endsWith('/recipes/media/upload'),
    );
    await page
      .locator('#recipe-media-upload-input')
      .setInputFiles({
        name: 'fix158.png',
        mimeType: 'image/png',
        buffer: Buffer.from(
          'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a8gAAAABJRU5ErkJggg==',
          'base64',
        ),
      });
    const uploaded = await uploadResponse;
    expect(uploaded.status()).toBe(201);
    blobUrl = (await uploaded.json()).data.blobUrl;
    // Associate immediately for cleanup even if a later browser/image assertion fails.
    const saved = await request.put(`${backend}/recipes/${recipeId}/media`, {
      headers,
      data: {
        mediaItems: [
          { blobUrl, mimeType: 'image/png', displayOrder: 1, isCover: true },
        ],
      },
    });
    expect(saved.status()).toBe(200);
    await expect(page.getByText(/Đã tải 1\/5 ảnh/)).toBeVisible();
    const preview = page.getByAltText('Ảnh công thức 1', { exact: true });
    await expect(preview).toHaveAttribute('src', blobUrl!);
    await expect
      .poll(() =>
        preview.evaluate((image: HTMLImageElement) => image.naturalWidth),
      )
      .toBeGreaterThan(0);
    const imageResponse = await request.get(blobUrl!);
    expect(imageResponse.status()).toBe(200);
    expect(imageResponse.headers()['content-type']).toContain('image/png');
    expect((await imageResponse.body()).length).toBeGreaterThan(0);
    await page.getByLabel('Tên món *', { exact: true }).fill(title);
    await page
      .getByLabel('Thể loại món', { exact: true })
      .selectOption('BRAISED');
    await page
      .getByLabel('Loại ăn chay', { exact: true })
      .selectOption('VEGAN');
    await page.getByLabel('Độ khó', { exact: true }).selectOption('EASY');
    await page.getByLabel('Khẩu phần', { exact: true }).fill('2');
    await page.getByLabel('Thời gian chuẩn bị', { exact: true }).fill('5');
    await page.getByLabel('Thời gian nấu', { exact: true }).fill('10');
    await page
      .getByLabel('Chọn nguyên liệu 1', { exact: true })
      .fill(detail.ingredients[0].name);
    await page
      .getByRole('button', { name: detail.ingredients[0].name, exact: true })
      .click();
    await page
      .getByLabel('Số lượng nguyên liệu 1', { exact: true })
      .fill('100');
    await page
      .getByLabel('Đơn vị nguyên liệu 1', { exact: true })
      .selectOption(String(detail.ingredients[0].unitId));
    await page
      .getByLabel('Hướng dẫn * (10–5.000 ký tự)', { exact: true })
      .fill('Chuẩn bị nguyên liệu và nấu chín kỹ.');
    const publication = page.waitForResponse(
      (response) =>
        response.url().endsWith('/recipes') &&
        response.request().method() === 'POST',
    );
    await page
      .getByRole('button', { name: 'Xuất bản công thức', exact: true })
      .click();
    const publicationResponse = await publication;
    expect(publicationResponse.status()).toBe(201);
    publishedId = (await publicationResponse.json()).recipeId;
    expect(Number.isSafeInteger(publishedId)).toBeTruthy();
    expect(
      sql(
        `SELECT COUNT(*) FROM RECIPE_MEDIA WHERE recipe_id=${publishedId} AND is_cover=1`,
      ),
    ).toBe('1');
    await page.goto(`/cong-thuc/id/${publishedId}`);
    await page.reload();
    const cover = page.locator(`img[src="${blobUrl}"]`).first();
    await expect(cover).toBeVisible();
    await expect
      .poll(() =>
        cover.evaluate((image: HTMLImageElement) => image.naturalWidth),
      )
      .toBeGreaterThan(0);
  } finally {
    if (publishedId) {
      expect(
        (
          await request.delete(`${backend}/recipes/${publishedId}/media`, {
            headers,
          })
        ).status(),
      ).toBe(200);
      expect(
        (
          await request.delete(`${backend}/recipes/${publishedId}`, { headers })
        ).status(),
      ).toBe(204);
    }
    if (recipeId) {
      const deleted = await request.delete(
        `${backend}/recipes/${recipeId}/media`,
        { headers },
      );
      expect(deleted.status()).toBe(200);
      if (blobUrl) expect((await request.get(blobUrl)).status()).toBe(404);
      expect(
        (
          await request.delete(`${backend}/recipes/${recipeId}`, { headers })
        ).status(),
      ).toBe(204);
    }
    // Preserve the author for FK/audit integrity and deactivate only this fixture account.
    sql(`UPDATE [USER] SET account_status='LOCKED' WHERE email='${email}'`);
  }
});
