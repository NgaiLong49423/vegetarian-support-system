import { randomUUID } from 'node:crypto';
import { execFileSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { expect, test } from '@playwright/test';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../../..');
const backend = process.env.MAMXANX_E2E_BACKEND_URL ?? 'http://localhost:8080/api/v1';
const project = process.env.MAMXANX_E2E_COMPOSE_PROJECT ?? 'mamxanh-dev';
const quote = (value: string) => `N'${value.replaceAll("'", "''")}'`;
test.use({ trace: 'off' }); // Real session tokens must never be persisted in a trace.

function sql(query: string) {
  const envFile = path.join(root, 'app/mamxanh-backend/.env');
  return execFileSync('docker', ['compose', '-p', project, ...(existsSync(envFile) ? ['--env-file', envFile] : []),
    '-f', path.join(root, 'docker-compose.yml'), 'exec', '-T', 'sqlserver', '/opt/mssql-tools18/bin/sqlcmd',
    '-S', 'sqlserver', '-U', 'sa', '-C', '-d', 'MamXanhDB', '-h', '-1', '-W', '-b', '-Q',
    `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`], { cwd: root, encoding: 'utf8', timeout: 30_000 }).trim();
}

test('FR-10: the same chat workflows remain available across Free, Plus and Pro', async ({ request }) => {
  test.setTimeout(180_000);
  const email = `fr10-${randomUUID()}@example.invalid`;
  const password = `Aa1!${randomUUID()}z`;
  let member = 0;
  try {
    const registration = await request.post(`${backend}/auth/register`, {
      data: { displayName: 'FR10 kiểm thử', email, password, confirmPassword: password },
    });
    expect(registration.status()).toBe(201);
    sql(`UPDATE [USER] SET email_verified=1,role='EXPERT',onboarding_invited_at=SYSUTCDATETIME() WHERE email=${quote(email)}`);
    member = Number(sql(`SELECT user_id FROM [USER] WHERE email=${quote(email)}`));
    const login = await request.post(`${backend}/auth/login`, { data: { email, password } });
    expect(login.status()).toBe(200);
    const session = await login.json();
    const headers = { Authorization: `Bearer ${session.accessToken}` };
    const chat = (question: string) => request.post(`${backend}/recipes/ai/chat`, {
      headers, data: { contextType: 'GENERAL', question, history: [] },
    });

    for (const tier of ['FREE', 'PLUS', 'PRO'] as const) {
      if (tier === 'FREE') sql(`DELETE FROM [SUBSCRIPTION] WHERE user_id=${member}`);
      else sql(`MERGE [SUBSCRIPTION] AS target USING (SELECT ${member} AS user_id) AS source ON target.user_id=source.user_id
        WHEN MATCHED THEN UPDATE SET tier='${tier}',status='ACTIVE',starts_at=DATEADD(day,-1,SYSUTCDATETIME()),ends_at=DATEADD(day,30,SYSUTCDATETIME()),token_quota=600000
        WHEN NOT MATCHED THEN INSERT (user_id,tier,status,starts_at,ends_at,token_quota)
          VALUES (${member},'${tier}','ACTIVE',DATEADD(day,-1,SYSUTCDATETIME()),DATEADD(day,30,SYSUTCDATETIME()),600000);`);

      const menu = await chat('Lập thực đơn tuần 7 ngày theo dinh dưỡng cá nhân');
      expect(menu.status(), `${tier} weekly-menu workflow`).toBe(200);
      expect(await menu.json()).toMatchObject({ workflowUrl: '/ke-hoach?view=for-you', recommendations: [] });

      const authoring = await chat('Viết mô tả cho bài đăng công thức');
      expect(authoring.status(), `${tier} Expert-authoring workflow`).toBe(200);
      expect(await authoring.json()).toMatchObject({ workflowUrl: '/dang-cong-thuc', recommendations: [] });
    }
  } finally {
    if (member) {
      sql(`DELETE FROM [SUBSCRIPTION] WHERE user_id=${member}; DELETE FROM [USER] WHERE user_id=${member}`);
    }
  }
});
