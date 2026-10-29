import { randomUUID } from "node:crypto"
import { execFileSync } from "node:child_process"
import { existsSync } from "node:fs"
import path from "node:path"
import { fileURLToPath } from "node:url"
import { expect, test } from "@playwright/test"

const root = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "../../../..",
)
const backend =
  process.env.MAMXANX_E2E_BACKEND_URL ?? "http://localhost:8080/api/v1"
const project = process.env.MAMXANX_E2E_COMPOSE_PROJECT ?? "mamxanh-dev"
const quote = (value: string) => `N'${value.replaceAll("'", "''")}'`
test.use({ trace: "off" })
function sql(query: string) {
  const envFile = path.join(root, "app/mamxanh-backend/.env")
  return execFileSync(
    "docker",
    [
      "compose",
      "-p",
      project,
      ...(existsSync(envFile) ? ["--env-file", envFile] : []),
      "-f",
      path.join(root, "docker-compose.yml"),
      "exec",
      "-T",
      "sqlserver",
      "/opt/mssql-tools18/bin/sqlcmd",
      "-S",
      "sqlserver",
      "-U",
      "sa",
      "-C",
      "-d",
      "MamXanhDB",
      "-h",
      "-1",
      "-W",
      "-w",
      "65535",
      "-b",
      "-Q",
      `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`,
    ],
    { cwd: root, encoding: "utf8", timeout: 30_000 },
  ).trim()
}

test("Administrator views real payment rows, filtered totals, details, CSV and Member denial without settlement", async ({
  page,
  request,
}, testInfo) => {
  test.setTimeout(180_000)
  const marker = `audit-e2e-${randomUUID()}`
  const email = `${marker}@example.invalid`
  const password = `Aa1!${randomUUID()}z`
  try {
    expect(
      (
        await request.post(`${backend}/auth/register`, {
          data: {
            displayName: "Kiểm thử giao dịch",
            email,
            password,
            confirmPassword: password,
          },
        })
      ).status(),
    ).toBe(201)
    sql(`UPDATE [USER] SET email_verified=1,onboarding_invited_at=SYSUTCDATETIME() WHERE email=${quote(email)};
      INSERT INTO PAYMENT_TRANSACTION(user_id,order_code,amount_vnd,status,created_at,paid_at)
      SELECT user_id,${quote(marker + "-paid")},99000,'PAID',SYSUTCDATETIME(),SYSUTCDATETIME() FROM [USER] WHERE email=${quote(email)};
      INSERT INTO PAYMENT_TRANSACTION(user_id,order_code,amount_vnd,status,created_at)
      SELECT user_id,${quote(marker + "-pending")},49000,'PENDING',SYSUTCDATETIME() FROM [USER] WHERE email=${quote(email)};`)
    const login = async () => {
      const response = await request.post(`${backend}/auth/login`, {
        data: { email, password },
      })
      expect(response.status()).toBe(200)
      return response.json()
    }
    const member = await login()
    expect(
      (
        await request.get(`${backend}/admin/transactions`, {
          headers: { Authorization: `Bearer ${member.accessToken}` },
        })
      ).status(),
    ).toBe(403)
    expect(
      (await request.get(`${backend}/admin/transactions/export`)).status(),
    ).toBe(401)
    sql(`UPDATE [USER] SET role='ADMIN' WHERE email=${quote(email)}`)
    const session = await login()
    await page.addInitScript(
      (value) => sessionStorage.setItem("mamxanh.auth", JSON.stringify(value)),
      { ...session, expiresAt: Date.now() + 600_000 },
    )
    await page.setViewportSize({ width: 1440, height: 1000 })
    await page.goto("/admin/giao-dich")
    await page.getByRole("textbox", { name: "Tìm giao dịch" }).fill(marker)
    await page.getByRole("button", { name: "Áp dụng bộ lọc" }).click()
    await expect(page.getByText("2 giao dịch phù hợp bộ lọc")).toBeVisible()
    await expect(page.getByRole("row").filter({ hasText: marker })).toHaveCount(
      2,
    )
    const result = await request.get(
      `${backend}/admin/transactions?query=${encodeURIComponent(marker)}`,
      { headers: { Authorization: `Bearer ${session.accessToken}` } },
    )
    expect(result.status()).toBe(200)
    const report = await result.json()
    expect(report.summary).toMatchObject({
      total: 2,
      paid: 1,
      pending: 1,
      revenueVnd: 99000,
    })
    const pending = report.transactions.content.find(
      (row: { status: string }) => row.status === "PENDING",
    )
    await page
      .getByRole("button", { name: `Xem chi tiết TX-${pending.id}` })
      .click()
    await expect(page.getByRole("dialog")).toContainText(marker + "-pending")
    await expect(page.getByRole("dialog")).toContainText("Chờ thanh toán")
    await page.keyboard.press("Escape")
    const download = page.waitForEvent("download")
    await page.getByRole("button", { name: "Xuất đối soát CSV" }).click()
    expect((await download).suggestedFilename()).toBe("MamXanh_giao_dich.csv")
    const table = page.getByRole("region", { name: "Danh sách giao dịch, cuộn ngang để xem đủ cột" })
    await table.evaluate(element => { element.scrollLeft = 0 })
    await page.screenshot({
      path: testInfo.outputPath("transactions-desktop.png"),
      fullPage: true,
      animations: "disabled",
    })
    await page.setViewportSize({ width: 390, height: 844 })
    await table.evaluate(element => { element.scrollLeft = 0 })
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth + 1,
      ),
    ).toBe(true)
    await page.screenshot({
      path: testInfo.outputPath("transactions-mobile.png"),
      fullPage: true,
      animations: "disabled",
    })
    expect(
      sql(
        `SELECT status FROM PAYMENT_TRANSACTION WHERE order_code=${quote(marker + "-pending")}`,
      ),
    ).toBe("PENDING")
    expect(
      Number(
        sql(
          `SELECT COUNT(*) FROM SUBSCRIPTION WHERE user_id=(SELECT user_id FROM [USER] WHERE email=${quote(email)})`,
        ),
      ),
    ).toBe(0)
  } finally {
    sql(`DELETE FROM PAYMENT_TRANSACTION WHERE user_id IN (SELECT user_id FROM [USER] WHERE email=${quote(email)});
      DELETE FROM SUBSCRIPTION WHERE user_id IN (SELECT user_id FROM [USER] WHERE email=${quote(email)});
      DELETE FROM [USER] WHERE email=${quote(email)}`)
  }
})
