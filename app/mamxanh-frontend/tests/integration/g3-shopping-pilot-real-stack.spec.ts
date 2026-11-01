import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, test } from '@playwright/test';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../../..');
const backend = process.env.MAMXANX_E2E_BACKEND_URL ?? 'http://localhost:18080/api/v1';
const project = process.env.MAMXANX_E2E_COMPOSE_PROJECT ?? 'mamxanh-g3';
const quote = (value: string) => `N'${value.replaceAll("'", "''")}'`;

// Never retain traces containing real JWT session tokens.
test.use({ trace: 'off' });

function sql(query: string): string {
  const envFile = path.join(root, 'app/mamxanh-backend/.env');
  return execFileSync(
    'docker',
    [
      'compose',
      '-p',
      project,
      ...(existsSync(envFile) ? ['--env-file', envFile] : []),
      '-f',
      path.join(root, 'docker-compose.yml'),
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

test('G3 real-stack E2E: CUSTOMER and EXPERT use ADK Shopping List pilot across consent, QA/CONFIRM/AUTO modes, and persist real lists in SQL', async ({ page, request }) => {
  test.setTimeout(300_000);
  const marker = randomUUID().slice(0, 8);
  const customerEmail = `g3-cust-${marker}@example.invalid`;
  const expertEmail = `g3-exp-${marker}@example.invalid`;
  const password = `Aa1!${randomUUID()}z`;

  const createdUserIds: number[] = [];

  try {
    const proQuota = Number(sql("SELECT token_quota FROM [AI_PLAN_CONFIGURATION] WHERE plan_tier = 'PRO'"));
    expect(proQuota).toBeGreaterThan(0);

    for (const [email, role] of [
      [customerEmail, 'CUSTOMER'],
      [expertEmail, 'EXPERT'],
    ] as const) {
      const reg = await request.post(`${backend}/auth/register`, {
        data: { displayName: `G3 ${role}`, email, password, confirmPassword: password },
      });
      expect(reg.status()).toBe(201);
      sql(`UPDATE [USER]
           SET email_verified = 1,
               role = '${role}',
               preferred_ai_model_id = 'gemini-3.8-flash',
               onboarding_invited_at = SYSUTCDATETIME()
           WHERE email = ${quote(email)}`);
      const userId = Number(sql(`SELECT user_id FROM [USER] WHERE email = ${quote(email)}`));
      expect(userId).toBeGreaterThan(0);
      createdUserIds.push(userId);
      sql(`INSERT INTO [SUBSCRIPTION] (user_id, tier, status, token_quota, starts_at, ends_at)
           VALUES (${userId}, 'PRO', 'ACTIVE', ${proQuota}, DATEADD(day, -1, SYSUTCDATETIME()), DATEADD(day, 30, SYSUTCDATETIME()))`);
    }

    // 1. Browser flow as CUSTOMER: login -> open chat -> consent -> QA preview -> CONFIRM reject -> CONFIRM approve (create -> read)
    await page.goto('/dang-nhap');
    await page.getByLabel('Email').fill(customerEmail);
    await page.getByLabel('Mật khẩu', { exact: true }).fill(password);
    const loginResp = page.waitForResponse((r) => r.url().endsWith('/auth/login') && r.request().method() === 'POST');
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
    expect((await loginResp).status()).toBe(200);
    await expect(page).toHaveURL(/trang-chu/, { timeout: 15_000 });

    await page.getByRole('button', { name: 'Mở khung chat hỗ trợ AI' }).click();
    const dialog = page.getByRole('dialog');
    await expect(dialog).toBeVisible({ timeout: 15_000 });

    // Switch mode to QA first
    await dialog.getByRole('button', { name: /Chế độ AI:/ }).click();
    await dialog.getByRole('radio', { name: /Hỏi đáp/ }).click();

    const qaListName = `Nháp nấm tươi ${marker}`;
    await dialog.getByLabel('Câu hỏi của bạn').fill(`Hãy tạo danh sách mua sắm rỗng tên là ${qaListName}`);
    await dialog.getByRole('button', { name: 'Gửi câu hỏi' }).click();

    // Expect Shopping List Consent prompt first (before any model/embedding call)
    await expect(dialog.getByText('Cho phép dùng dữ liệu cá nhân?')).toBeVisible({ timeout: 15_000 });
    await expect(dialog.getByText('danh sách mua sắm (nội dung yêu cầu, tên, ID và kết quả danh sách — mục đích tìm hướng dẫn và điều phối)')).toBeVisible();

    // Grant consent -> executes QA preview
    await dialog.getByRole('button', { name: 'Đồng ý và tiếp tục' }).click();
    await expect(dialog.getByText('Chỉ xem trước', { exact: true }).first()).toBeVisible({ timeout: 35_000 });
    expect(Number(sql(`SELECT COUNT(*) FROM [SHOPPING_LIST] WHERE user_id = ${createdUserIds[0]}`))).toBe(0);

    // Switch mode to CONFIRM
    await dialog.getByRole('button', { name: /Chế độ AI:/ }).click();
    await dialog.getByRole('radio', { name: /Có xác nhận/ }).click();

    const rejectedName = `Đồ khô hủy ${marker}`;
    await dialog.getByLabel('Câu hỏi của bạn').fill(`Hãy tạo danh sách mua sắm rỗng tên là ${rejectedName}`);
    await dialog.getByRole('button', { name: 'Gửi câu hỏi' }).click();

    await expect(dialog.getByText('Xem lại trước khi thực hiện')).toBeVisible({ timeout: 35_000 });
    await expect(dialog.getByText(`Tên danh sách dự kiến: “${rejectedName}”`)).toBeVisible();

    // Reject confirmation -> no SQL row
    await dialog.getByRole('button', { name: 'Hủy', exact: true }).click();
    await expect(dialog.getByText(`Đã hủy thao tác tạo danh sách mua sắm "${rejectedName}"`, { exact: false }).first()).toBeVisible({ timeout: 15_000 });
    expect(Number(sql(`SELECT COUNT(*) FROM [SHOPPING_LIST] WHERE user_id = ${createdUserIds[0]}`))).toBe(0);

    // Now request creation in CONFIRM mode and APPROVE
    const confirmedName = `Đi chợ cuối tuần ${marker}`;
    const questionInput = dialog.getByLabel('Câu hỏi của bạn');
    await expect(questionInput).toBeEnabled({ timeout: 10_000 });
    await questionInput.fill(`Hãy tạo danh sách mua sắm rỗng tên là ${confirmedName}`);
    const sendButton = dialog.getByRole('button', { name: 'Gửi câu hỏi' });
    await expect(sendButton).toBeEnabled({ timeout: 10_000 });
    await sendButton.click();

    await expect(dialog.getByText(`Tên danh sách dự kiến: “${confirmedName}”`)).toBeVisible({ timeout: 35_000 });
    await dialog.getByRole('button', { name: 'Xác nhận thay đổi' }).click();

    await expect(dialog.getByText(`Đã tạo danh sách mua sắm rỗng "${confirmedName}"`, { exact: false }).first()).toBeVisible({ timeout: 35_000 });
    await expect(dialog.getByText(/Đọc lại danh sách mua sắm/).first()).toBeVisible();

    const customerListId = Number(sql(`SELECT shopping_list_id FROM [SHOPPING_LIST] WHERE user_id = ${createdUserIds[0]} AND name = ${quote(confirmedName)}`));
    expect(customerListId).toBeGreaterThan(0);

    // Verify real Shopping List page shows the created list
    await page.goto(`/di-cho?listId=${customerListId}`);
    await expect(page.getByRole('heading', { name: confirmedName, exact: true })).toBeVisible({ timeout: 15_000 });

    // 2. Verify EXPERT on the same tier/model in AUTO mode creates and reads back their own list
    const expLogin = await request.post(`${backend}/auth/login`, {
      data: { email: expertEmail, password },
    });
    expect(expLogin.status()).toBe(200);
    const expToken = (await expLogin.json()).accessToken as string;
    const autoListName = `Thực đơn chuyên gia ${marker}`;

    const expAutoResp = await request.post(`${backend}/recipes/ai/chat`, {
      headers: { Authorization: `Bearer ${expToken}` },
      data: {
        contextType: 'GENERAL',
        clientRequestId: randomUUID(),
        executionMode: 'AUTO',
        consentDecision: {
          action: 'GRANT',
          scopes: [
            { category: 'SHOPPING_LIST', purpose: 'AI_CHAT' },
            { category: 'SHOPPING_LIST', purpose: 'SHOPPING_LIST_READ' },
            { category: 'SHOPPING_LIST', purpose: 'SHOPPING_LIST_WRITE' },
          ],
        },
        question: `Hãy tạo danh sách mua sắm rỗng tên là ${autoListName}`,
        history: [],
      },
    });
    expect(expAutoResp.status()).toBe(200);
    const expAutoBody = await expAutoResp.json();
    expect(expAutoBody.engine).toBe('ADK_SHOPPING_PILOT');
    expect(expAutoBody.runStatus).toBe('COMPLETED');
    expect(expAutoBody.answer).toContain(autoListName);

    const expertListId = Number(sql(`SELECT shopping_list_id FROM [SHOPPING_LIST] WHERE user_id = ${createdUserIds[1]} AND name = ${quote(autoListName)}`));
    expect(expertListId).toBeGreaterThan(0);
    expect(expertListId).not.toBe(customerListId);

    // Verify usage accounting recorded in AI_TOKEN_USAGE_LEDGER for both synthetic accounts
    const usageCount = Number(sql(`SELECT COUNT(*) FROM [AI_TOKEN_USAGE_LEDGER] WHERE user_id IN (${createdUserIds.join(',')}) AND feature = 'FR-51'`));
    expect(usageCount).toBeGreaterThanOrEqual(4);
  } finally {
    if (createdUserIds.length > 0) {
      const ids = createdUserIds.join(',');
      sql(`
        DELETE FROM [SHOPPING_LIST] WHERE user_id IN (${ids});
        DELETE FROM [AI_CREDIT_LEDGER] WHERE user_id IN (${ids});
        DELETE FROM [AI_CREDIT_WALLET] WHERE user_id IN (${ids});
        DELETE FROM [AI_TOKEN_USAGE_LEDGER] WHERE user_id IN (${ids});
        DELETE FROM [AI_USAGE_TELEMETRY] WHERE user_id IN (${ids});
        DELETE FROM [SUBSCRIPTION] WHERE user_id IN (${ids});
        DELETE FROM [USER] WHERE user_id IN (${ids});
      `);
    }
  }
});
