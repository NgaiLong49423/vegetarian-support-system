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
const emails = { admin: `fr28-admin-${marker}@example.invalid`, reporter: `fr28-reporter-${marker}@example.invalid`, author: `fr28-author-${marker}@example.invalid` }
const names = { admin: 'Quản trị kiểm thử FR28', reporter: 'Người báo cáo FR28', author: 'Tác giả FR28' }
const password = `Aa1!${randomUUID()}z`
const title = `FR28 ${marker}`
const literal = (value: string) => `N'${value.replaceAll("'", "''")}'`
function sql(query: string): string {
  const envFile = path.join(root, 'app/mamxanh-backend/.env')
  return execFileSync('docker', ['compose', '-p', project, '-f', path.join(root, 'docker-compose.yml'), ...(existsSync(envFile) ? ['--env-file', envFile] : []), 'exec', '-T', 'sqlserver', '/opt/mssql-tools18/bin/sqlcmd', '-S', 'sqlserver', '-U', 'sa', '-C', '-d', 'MamXanhDB', '-h', '-1', '-W', '-b', '-Q', `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`], { cwd: root, encoding: 'utf8', timeout: 30000 }).trim()
}
// Real credentials and JWT remain in memory; do not retain network traces.
test.use({ trace: 'off' })
test('FR-28 administrator takes a real report, hides the post and the author is told without the reporter', async ({ page, request }) => {
  test.setTimeout(240000)
  const users = `SELECT user_id FROM [USER] WHERE email IN (${Object.values(emails).map(literal).join(', ')})`
  try {
    for (const key of ['admin', 'reporter', 'author'] as const) {
      const register = await request.post(`${backend}/auth/register`, { data: { displayName: names[key], email: emails[key], password, confirmPassword: password } })
      expect(register.status()).toBe(201)
    }
    sql(`UPDATE [USER] SET email_verified=1, onboarding_invited_at=SYSUTCDATETIME() WHERE user_id IN (${users});
      UPDATE [USER] SET role='ADMIN' WHERE email=${literal(emails.admin)};
      UPDATE [USER] SET role='EXPERT' WHERE email=${literal(emails.author)};`)
    const id = (key: keyof typeof emails) => Number(sql(`SELECT user_id FROM [USER] WHERE email=${literal(emails[key])}`))
    const [adminId, reporterId, authorId] = [id('admin'), id('reporter'), id('author')]
    sql(`INSERT INTO [RECIPE_POST](author_id,title,instructions,dish_category,vegetarian_type,difficulty,servings,prep_time_min,cook_time_min,status,published_at) VALUES(${authorId},${literal(title)},N'Nấu món kiểm thử báo cáo.','SOUP','VEGAN','EASY',2,10,15,'PUBLISHED',SYSUTCDATETIME());`)
    const recipeId = Number(sql(`SELECT recipe_id FROM [RECIPE_POST] WHERE title=${literal(title)}`))

    const reporterLogin = await request.post(`${backend}/auth/login`, { data: { email: emails.reporter, password } })
    expect(reporterLogin.status()).toBe(200)
    const reporterToken = (await reporterLogin.json()).accessToken as string
    const submitted = await request.post(`${backend}/recipes/${recipeId}/reports`, {
      headers: { Authorization: `Bearer ${reporterToken}` },
      data: { reasonCode: 'RECIPE_INFO_OR_DIET_LABEL', description: 'Món có nước mắm cá nên không phải món chay' },
    })
    expect(submitted.status()).toBe(201)
    const reportId = (await submitted.json()).reportId as number

    await page.goto('/dang-nhap')
    await page.getByLabel('Email').fill(emails.admin)
    await page.getByLabel('Mật khẩu', { exact: true }).fill(password)
    const login = page.waitForResponse((response) => response.url().endsWith('/auth/login') && response.request().method() === 'POST', { timeout: 30000 })
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    expect((await login).status()).toBe(200)
    await page.waitForURL((url) => !url.pathname.startsWith('/dang-nhap'), { timeout: 15000 })
    await page.goto('/admin/bao-cao')
    const card = page.locator('div.border-l-4').filter({ hasText: title })
    await expect(card).toContainText(`Người báo cáo: ${names.reporter}`, { timeout: 20000 })
    await expect(card).toContainText(`Tác giả: ${names.author}`)

    const claim = page.waitForResponse((response) => response.url().endsWith(`/admin/reports/${reportId}/claim`))
    await card.getByRole('button', { name: 'Tiếp nhận xem xét' }).click()
    expect((await claim).status()).toBe(200)
    const dialog = page.getByRole('dialog', { name: `Báo cáo #${reportId}` })
    await expect(dialog).toContainText(`Người xử lý: ${names.admin}`)
    expect(sql(`SELECT CONCAT(status, ':', handled_by) FROM [REPORT] WHERE report_id=${reportId}`)).toBe(`IN_REVIEW:${adminId}`)

    await dialog.getByRole('radio', { name: /Ẩn bài viết/ }).check()
    await dialog.getByRole('button', { name: 'Hoàn tất xử lý' }).click()
    await expect(dialog.getByText('Nhập lý do kết luận từ 10 đến 500 ký tự.')).toBeVisible()
    expect(sql(`SELECT status FROM [REPORT] WHERE report_id=${reportId}`)).toBe('IN_REVIEW')

    await dialog.getByLabel('Lý do kết luận').fill('Bài dùng nước mắm cá nên không đạt nhãn thuần chay.')
    const resolve = page.waitForResponse((response) => response.url().endsWith(`/admin/reports/${reportId}/resolve`))
    await dialog.getByRole('button', { name: 'Hoàn tất xử lý' }).click()
    expect((await resolve).status()).toBe(200)
    await expect(page.getByRole('status')).toContainText(`Đã giải quyết báo cáo #${reportId} với kết luận "Ẩn bài viết".`)

    expect(sql(`SELECT CONCAT(status, ':', decision, ':', handled_by) FROM [REPORT] WHERE report_id=${reportId}`)).toBe(`RESOLVED:HIDE_RECIPE:${adminId}`)
    expect(sql(`SELECT status FROM [RECIPE_POST] WHERE recipe_id=${recipeId}`)).toBe('HIDDEN')
    expect((await request.get(`${backend}/recipes/${recipeId}`)).status()).toBe(404)
    const authorNotice = sql(`SELECT title + N' ' + message FROM [NOTIFICATION] WHERE user_id=${authorId} AND report_id=${reportId}`)
    expect(authorNotice).toContain('Bài dùng nước mắm cá nên không đạt nhãn thuần chay.')
    expect(authorNotice).not.toContain(names.reporter)
    expect(authorNotice).not.toContain('nước mắm cá nên không phải món chay')
    expect(sql(`SELECT notification_type FROM [NOTIFICATION] WHERE user_id=${reporterId} AND report_id=${reportId}`)).toBe('REPORT_RESULT')
  } finally {
    sql(`DELETE FROM [NOTIFICATION] WHERE user_id IN (${users});
      DELETE FROM [REPORT] WHERE reporter_id IN (${users}) OR handled_by IN (${users});
      DELETE FROM [RECIPE_POST] WHERE author_id IN (${users});
      DELETE FROM [USER] WHERE user_id IN (${users});`)
  }
})
