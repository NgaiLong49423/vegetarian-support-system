import { randomUUID } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { expect, test } from '@playwright/test'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../../..')
const backend = process.env.MAMXANX_E2E_BACKEND_URL ?? 'http://localhost:8080/api/v1'
const project = process.env.MAMXANX_E2E_COMPOSE_PROJECT ?? 'mamxanh-dev'
const marker = randomUUID()
const reporterEmail = `fr30-reporter-${marker}@example.invalid`, authorEmail = `fr30-author-${marker}@example.invalid`
const password = `Aa1!${randomUUID()}z`
const title = `FR30 ${marker}`
const literal = (value: string) => `N'${value.replaceAll("'", "''")}'`
function sql(query: string): string {
  const envFile = path.join(root, 'app/mamxanh-backend/.env')
  return execFileSync('docker', ['compose', '-p', project, '-f', path.join(root, 'docker-compose.yml'), ...(existsSync(envFile) ? ['--env-file', envFile] : []), 'exec', '-T', 'sqlserver', '/opt/mssql-tools18/bin/sqlcmd', '-S', 'sqlserver', '-U', 'sa', '-C', '-d', 'MamXanhDB', '-h', '-1', '-W', '-b', '-Q', `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`], { cwd: root, encoding: 'utf8', timeout: 30000 }).trim()
}
// Real credentials and JWT remain in memory; do not retain network traces.
test.use({ trace: 'off' })
test('FR-30 a second report on the real page leads to the open report and stores a supplement in SQL', async ({ page, request }) => {
  test.setTimeout(240000)
  const users = `SELECT user_id FROM [USER] WHERE email IN (${literal(reporterEmail)}, ${literal(authorEmail)})`
  try {
    for (const [email, name] of [[reporterEmail, 'Người báo cáo FR30'], [authorEmail, 'Tác giả FR30']]) {
      const register = await request.post(`${backend}/auth/register`, { data: { displayName: name, email, password, confirmPassword: password } })
      expect(register.status()).toBe(201)
    }
    sql(`UPDATE [USER] SET email_verified=1, onboarding_invited_at=SYSUTCDATETIME() WHERE user_id IN (${users});
      UPDATE [USER] SET role='EXPERT' WHERE email=${literal(authorEmail)};`)
    const authorId = Number(sql(`SELECT user_id FROM [USER] WHERE email=${literal(authorEmail)}`))
    sql(`INSERT INTO [RECIPE_POST](author_id,title,instructions,dish_category,vegetarian_type,difficulty,servings,prep_time_min,cook_time_min,status,published_at) VALUES(${authorId},${literal(title)},N'Nấu món kiểm thử báo cáo.','SOUP','VEGAN','EASY',2,10,15,'PUBLISHED',SYSUTCDATETIME());`)
    const recipeId = Number(sql(`SELECT recipe_id FROM [RECIPE_POST] WHERE title=${literal(title)}`))

    await page.goto('/dang-nhap')
    await page.getByLabel('Email').fill(reporterEmail)
    await page.getByLabel('Mật khẩu', { exact: true }).fill(password)
    const login = page.waitForResponse((response) => response.url().endsWith('/auth/login') && response.request().method() === 'POST', { timeout: 30000 })
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    expect((await login).status()).toBe(200)
    await page.waitForURL((url) => !url.pathname.startsWith('/dang-nhap'), { timeout: 15000 })

    await page.goto(`/cong-thuc/id/${recipeId}`)
    await page.getByRole('button', { name: 'Báo cáo công thức' }).click()
    let dialog = page.getByRole('dialog', { name: 'Báo cáo công thức' })
    await dialog.getByRole('radio', { name: 'Sai thông tin công thức hoặc nhãn ăn chay' }).check()
    await dialog.getByLabel('Mô tả thêm').fill('Món dùng nước mắm cá')
    const first = page.waitForResponse((response) => response.url().endsWith(`/recipes/${recipeId}/reports`))
    await dialog.getByRole('button', { name: 'Gửi báo cáo' }).click()
    expect((await first).status()).toBe(201)
    await expect(dialog.getByRole('status')).toHaveText('Đã gửi báo cáo. Quản trị viên sẽ xem xét nội dung này.')
    const reportId = Number(sql(`SELECT report_id FROM [REPORT] WHERE recipe_id=${recipeId}`))

    await page.keyboard.press('Escape')
    await page.getByRole('button', { name: 'Báo cáo công thức' }).click()
    dialog = page.getByRole('dialog', { name: 'Báo cáo công thức' })
    await dialog.getByRole('radio', { name: 'Nguy cơ an toàn thực phẩm' }).check()
    const second = page.waitForResponse((response) => response.url().endsWith(`/recipes/${recipeId}/reports`))
    await dialog.getByRole('button', { name: 'Gửi báo cáo' }).click()
    expect((await second).status()).toBe(409)
    await expect(dialog).toContainText('Chờ xử lý')
    await expect(dialog).toContainText('Món dùng nước mắm cá')
    await expect(dialog).toContainText('Còn 5 lần bổ sung')

    await dialog.getByLabel('Thông tin bổ sung').fill('  Ảnh bước 3 cho thấy chai nước mắm cá  ')
    const supplement = page.waitForResponse((response) => response.url().endsWith(`/me/reports/${reportId}/supplements`))
    await dialog.getByRole('button', { name: 'Lưu bổ sung' }).click()
    expect((await supplement).status()).toBe(201)
    await expect(dialog.getByRole('status')).toHaveText('Đã lưu phần bổ sung.')
    await expect(dialog).toContainText('Còn 4 lần bổ sung')

    expect(sql(`SELECT content FROM [REPORT_SUPPLEMENT] WHERE report_id=${reportId}`)).toBe('Ảnh bước 3 cho thấy chai nước mắm cá')
    expect(sql(`SELECT COUNT(*) FROM [REPORT] WHERE recipe_id=${recipeId}`)).toBe('1')
    expect(sql(`SELECT description FROM [REPORT] WHERE report_id=${reportId}`)).toBe('Món dùng nước mắm cá')
  } finally {
    sql(`DELETE FROM [REPORT_SUPPLEMENT] WHERE report_id IN (SELECT report_id FROM [REPORT] WHERE reporter_id IN (${users}));
      DELETE FROM [REPORT] WHERE reporter_id IN (${users});
      DELETE FROM [RECIPE_VIEW] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST] WHERE author_id IN (${users}));
      DELETE FROM [RECIPE_POST] WHERE author_id IN (${users});
      DELETE FROM [USER] WHERE user_id IN (${users});`)
  }
})
