import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, test } from '@playwright/test';

// FR-49 (Issue #51) through the real Frontend, Backend and SQL Server: B replies to A's comment, A sees
// the bell count, opens the notification and lands on the reply; the read state is stored in SQL Server.

const frontendDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const repositoryRoot = path.resolve(frontendDir, '../..');
const composeProject = process.env.MAMXANX_E2E_COMPOSE_PROJECT ?? 'mamxanh-dev';
const backend = process.env.MAMXANX_E2E_BACKEND_URL ?? 'http://localhost:8080/api/v1';
const demoPassword = process.env.MAMXANH_DEMO_PASSWORD;
const memberA = 'demo-customer-18@mamxanh.local';
const memberB = 'demo-customer-19@mamxanh.local';

function sql(query: string): string {
  return execFileSync('docker', [
    'compose', '-p', composeProject, '-f', path.join(repositoryRoot, 'docker-compose.yml'),
    'exec', '-T', 'sqlserver', '/opt/mssql-tools18/bin/sqlcmd',
    '-S', 'sqlserver', '-U', 'sa', '-C', '-d', 'MamXanhDB', '-h', '-1', '-W', '-b',
    '-Q', `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`,
  ], { cwd: repositoryRoot, encoding: 'utf8', timeout: 30_000 }).trim();
}

test('FR-49: a reply notifies the comment author in the bell and the notification opens the reply', async ({ page, request }) => {
  test.skip(!demoPassword, 'Compose integration requires MAMXANH_DEMO_PASSWORD for the seeded Members.');
  // The oldest published demo-expert recipe; other real-stack specs only publish and remove newer recipes.
  const [recipeId, recipeTitle] = sql(`SELECT TOP 1 CONCAT(p.recipe_id, '|', p.title) FROM [RECIPE_POST] p
    JOIN [USER] u ON u.user_id = p.author_id
    WHERE u.email = 'demo-expert@mamxanh.local' AND p.status = 'PUBLISHED' ORDER BY p.recipe_id`).split('|');
  const token = async (email: string) => {
    const login = await request.post(`${backend}/auth/login`, { data: { email, password: demoPassword } });
    expect(login.status()).toBe(200);
    return (await login.json()).accessToken as string;
  };
  const marker = randomUUID().slice(0, 8);
  const commentIds: number[] = [];
  try {
    const root = await request.post(`${backend}/recipes/${recipeId}/comments`, {
      headers: { Authorization: `Bearer ${await token(memberA)}` },
      data: { content: `Câu hỏi kiểm thử FR-49 ${marker}` },
    });
    expect(root.status()).toBe(201);
    const rootId = (await root.json()).commentId as number;
    commentIds.push(rootId);
    const reply = await request.post(`${backend}/recipes/${recipeId}/comments`, {
      headers: { Authorization: `Bearer ${await token(memberB)}` },
      data: { content: `Trả lời kiểm thử FR-49 ${marker}`, parentCommentId: rootId },
    });
    expect(reply.status()).toBe(201);
    const replyId = (await reply.json()).commentId as number;
    commentIds.push(replyId);
    const replierName = sql(`SELECT display_name FROM [USER] WHERE email = '${memberB}'`);
    expect(sql(`SELECT CONCAT(notification_type, '|', target_path, '|', CONVERT(varchar(1), is_read)) FROM [NOTIFICATION]
      WHERE comment_id = ${replyId}`)).toBe(`COMMENT_REPLY|/cong-thuc/id/${recipeId}#comment-${replyId}|0`);

    await page.goto('/dang-nhap');
    await page.getByLabel('Email').fill(memberA);
    await page.getByRole('textbox', { name: 'Mật khẩu' }).fill(demoPassword!);
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
    await expect(page).toHaveURL(/\/trang-chu$/);
    await expect(page.getByRole('link', { name: /^Thông báo, \d+ chưa đọc$/ })).toBeVisible();

    await page.getByRole('link', { name: /^Thông báo/ }).click();
    const item = page.getByRole('list', { name: 'Danh sách thông báo' }).getByRole('button')
      .filter({ hasText: `${replierName} đã trả lời bình luận của bạn trong công thức “${recipeTitle}”.` }).first();
    await expect(item).toBeVisible();
    await item.click();

    await expect(page).toHaveURL(new RegExp(`/cong-thuc/id/${recipeId}#comment-${replyId}$`));
    const linked = page.locator(`#comment-${replyId}`);
    await expect(linked).toContainText(`Trả lời kiểm thử FR-49 ${marker}`);
    await expect(linked).toBeFocused();
    await expect.poll(() => sql(`SELECT CONVERT(varchar(1), is_read) FROM [NOTIFICATION] WHERE comment_id = ${replyId}`)).toBe('1');
  } finally {
    if (commentIds.length) {
      const ids = commentIds.join(',');
      sql(`DELETE FROM [NOTIFICATION] WHERE comment_id IN (${ids});
        DELETE FROM [COMMENT] WHERE comment_id IN (${ids}) AND parent_comment_id IS NOT NULL;
        DELETE FROM [COMMENT] WHERE comment_id IN (${ids});`);
    }
  }
});
