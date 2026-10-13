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
const fixture = randomUUID()
const ownerEmail = `fr09-owner-${fixture}@example.invalid`
const otherEmail = `fr09-other-${fixture}@example.invalid`
const password = `Aa1!${randomUUID()}z`
const firstTitle = `FR09 canh ${fixture}`
const secondTitle = `FR09 kho ${fixture}`
const literal = (value: string) => `N'${value.replaceAll("'", "''")}'`

function sql(query: string): string {
  const envFile = path.join(root, "app/mamxanh-backend/.env")
  return execFileSync(
    "docker",
    [
      "compose",
      "-p",
      project,
      "-f",
      path.join(root, "docker-compose.yml"),
      ...(existsSync(envFile) ? ["--env-file", envFile] : []),
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
      "-b",
      "-Q",
      `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`,
    ],
    { cwd: root, encoding: "utf8", timeout: 30000 },
  ).trim()
}

function cleanup() {
  const members = `SELECT user_id FROM [USER] WHERE email IN (${literal(ownerEmail)}, ${literal(otherEmail)})`
  sql(`DELETE FROM [MEAL_PLAN] WHERE user_id IN (${members});
    DELETE FROM [SAVED_RECIPE] WHERE user_id IN (${members});
    DELETE FROM [RECIPE_POST] WHERE author_id IN (${members});
    DELETE FROM [USER] WHERE email IN (${literal(ownerEmail)}, ${literal(otherEmail)});`)
}

test("FR-09 browser mutations persist in Compose SQL and preserve private recipe references", async ({
  page,
  request,
}) => {
  test.setTimeout(180000)
  try {
    for (const email of [ownerEmail, otherEmail]) {
      const result = await request.post(`${backend}/auth/register`, {
        data: {
          displayName: "FR09 kiểm thử lịch",
          email,
          password,
          confirmPassword: password,
        },
      })
      expect(result.status()).toBe(201)
    }
    sql(`UPDATE [USER] SET email_verified = 1, onboarding_invited_at = SYSUTCDATETIME()
      WHERE email IN (${literal(ownerEmail)}, ${literal(otherEmail)});
      UPDATE [USER] SET role = 'EXPERT' WHERE email = ${literal(ownerEmail)};`)
    const ownerId = Number(
      sql(`SELECT user_id FROM [USER] WHERE email = ${literal(ownerEmail)}`),
    )
    const otherLogin = await request.post(`${backend}/auth/login`, {
      data: { email: otherEmail, password },
    })
    expect(otherLogin.status()).toBe(200)
    const otherToken = (await otherLogin.json()).accessToken as string
    for (const title of [firstTitle, secondTitle]) {
      sql(`INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type, difficulty,
        servings, prep_time_min, cook_time_min, status, published_at)
        VALUES (${ownerId}, ${literal(title)}, N'Nấu món kiểm thử.', 'SOUP', 'VEGAN', 'EASY',
          1, 10, 15, 'PUBLISHED', SYSUTCDATETIME());`)
    }
    const firstId = Number(
      sql(
        `SELECT recipe_id FROM [RECIPE_POST] WHERE title = ${literal(firstTitle)}`,
      ),
    )
    const secondId = Number(
      sql(
        `SELECT recipe_id FROM [RECIPE_POST] WHERE title = ${literal(secondTitle)}`,
      ),
    )
    sql(
      `INSERT INTO [SAVED_RECIPE] (user_id, recipe_id) VALUES (${ownerId}, ${firstId});`,
    )

    await page.goto("/dang-nhap")
    await page.getByLabel("Email").fill(ownerEmail)
    await page.getByRole("textbox", { name: "Mật khẩu" }).fill(password)
    const login = page.waitForResponse((response) =>
      response.url().endsWith("/auth/login"),
    )
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click()
    expect((await login).status()).toBe(200)
    await expect(page).toHaveURL(/\/$/)
    const session = await page.evaluate(() =>
      JSON.parse(sessionStorage.getItem("mamxanh.auth")!),
    )
    const headers = { Authorization: `Bearer ${session.accessToken}` }
    const read = page.waitForResponse(
      (response) =>
        response.request().method() === "GET" &&
        new URL(response.url()).pathname.endsWith("/meal-plans"),
    )
    await page.goto("/ke-hoach")
    const readResponse = await read
    expect(readResponse.status()).toBe(200)
    const selectedWeek = (await readResponse.json()).weekStartDate as string
    await expect(
      page.getByRole("heading", { name: "Lịch ăn của bạn" }),
    ).toBeVisible()
    await page.waitForLoadState("networkidle")
    const addButtons = page.getByRole("button", { name: /^Thêm món Bữa/ })
    try {
      await expect(addButtons).toHaveCount(21, { timeout: 15_000 })
    } catch (error) {
      console.error(`Expected 21 add buttons, found ${await addButtons.count()}`)
      await page.screenshot({
        path: `test-results/debug-add-button-count-${fixture}.png`,
        fullPage: true,
      })
      throw error
    }
    expect(
      Number(
        sql(`SELECT COUNT(*) FROM [MEAL_PLAN] WHERE user_id = ${ownerId}`),
      ),
    ).toBe(0)

    await page
      .getByRole("button", { name: /^Thêm món Bữa trưa/ })
      .first()
      .click()
    const dialog = page.getByRole("dialog")
    await dialog.getByRole("button", { name: "Đã lưu", exact: true }).click()
    await dialog.getByLabel("Tìm món cho lịch ăn").fill(fixture)
    await dialog.getByRole("button", { name: "Tìm", exact: true }).click()
    const add = page.waitForResponse(
      (response) =>
        response.request().method() === "POST" &&
        response.url().includes("/meal-plans/"),
    )
    await dialog.getByRole("button", { name: firstTitle }).click()
    const addResponse = await add
    expect(addResponse.status()).toBe(201)
    const item = await addResponse.json()
    expect(item.plannedServings).toBe(1)
    expect(item.mealDate).toBe(selectedWeek)
    const entryId = item.entryId as number
    const entryUrl = `${backend}/meal-plans/${selectedWeek}/entries/${entryId}`
    expect(
      Number(
        sql(`SELECT COUNT(*) FROM [MEAL_PLAN_ENTRY] WHERE meal_plan_entry_id = ${entryId}
      AND recipe_id = ${firstId} AND meal_type = 'LUNCH' AND planned_servings = 1`),
      ),
    ).toBe(1)
    await page.reload()
    await expect(page.getByRole("article", { name: firstTitle })).toBeVisible()

    const duplicate = await request.post(
      `${backend}/meal-plans/${selectedWeek}/entries`,
      {
        headers,
        data: { mealDate: selectedWeek, mealType: "LUNCH", recipeId: firstId },
      },
    )
    expect(duplicate.status()).toBe(409)
    expect((await duplicate.json()).code).toBe("MEAL_PLAN_RECIPE_DUPLICATE")
    for (const method of ["put", "delete"] as const) {
      const denied = await request[method](entryUrl, {
        headers: { Authorization: `Bearer ${otherToken}` },
        ...(method === "put" ? { data: { recipeId: secondId } } : {}),
      })
      expect(denied.status()).toBe(403)
    }
    const foreignRead = await request.get(
      `${backend}/meal-plans?weekStartDate=${selectedWeek}`,
      {
        headers: { Authorization: `Bearer ${otherToken}` },
      },
    )
    expect(foreignRead.status()).toBe(200)
    expect((await foreignRead.json()).entries).toEqual([])

    sql(
      `UPDATE [RECIPE_POST] SET status = 'HIDDEN' WHERE recipe_id = ${firstId};`,
    )
    await page.reload()
    await expect(
      page.getByRole("article", { name: "Công thức không còn khả dụng" }),
    ).toBeVisible()
    await expect(
      page.locator(`a[href="/cong-thuc/id/${firstId}"]`),
    ).toHaveCount(0)
    await page
      .getByRole("button", { name: "Thay món Công thức không còn khả dụng" })
      .click()
    await dialog.getByLabel("Tìm món cho lịch ăn").fill(fixture)
    await dialog.getByRole("button", { name: "Tìm", exact: true }).click()
    const replace = page.waitForResponse(
      (response) =>
        response.request().method() === "PUT" &&
        response.url().endsWith(`/entries/${entryId}`),
    )
    await dialog.getByRole("button", { name: secondTitle }).click()
    expect((await replace).status()).toBe(200)
    await expect(page.getByRole("article", { name: secondTitle })).toBeVisible()
    expect(
      Number(
        sql(
          `SELECT recipe_id FROM [MEAL_PLAN_ENTRY] WHERE meal_plan_entry_id = ${entryId}`,
        ),
      ),
    ).toBe(secondId)

    sql(
      `UPDATE [RECIPE_POST] SET status = 'DELETED' WHERE recipe_id = ${secondId};`,
    )
    await page.reload()
    await page
      .getByRole("button", { name: "Gỡ món Công thức không còn khả dụng" })
      .click()
    const remove = page.waitForResponse(
      (response) =>
        response.request().method() === "DELETE" &&
        response.url().endsWith(`/entries/${entryId}`),
    )
    await page.getByRole("button", { name: "Xác nhận gỡ", exact: true }).click()
    expect((await remove).status()).toBe(204)
    await expect(page.getByRole("article")).toHaveCount(0)
    expect(
      Number(
        sql(
          `SELECT COUNT(*) FROM [MEAL_PLAN_ENTRY] WHERE meal_plan_entry_id = ${entryId}`,
        ),
      ),
    ).toBe(0)
    expect(
      Number(
        sql(
          `SELECT COUNT(*) FROM [SAVED_RECIPE] WHERE user_id = ${ownerId} AND recipe_id = ${firstId}`,
        ),
      ),
    ).toBe(1)
    expect(
      Number(
        sql(
          `SELECT COUNT(*) FROM [RECIPE_POST] WHERE recipe_id IN (${firstId}, ${secondId})`,
        ),
      ),
    ).toBe(2)
  } finally {
    cleanup()
  }
})
