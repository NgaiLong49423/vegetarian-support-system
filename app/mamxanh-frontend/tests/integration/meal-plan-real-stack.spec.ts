import { randomUUID } from "node:crypto"
import { execFileSync } from "node:child_process"
import { existsSync } from "node:fs"
import { readFile } from "node:fs/promises"
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

test("FR-09/FR-33 move and mutations persist in Compose SQL and preserve private recipe references", async ({
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
    sql(`INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity)
      VALUES (${firstId}, (SELECT TOP 1 ingredient_id FROM [INGREDIENT] WHERE status = 'ACTIVE'
        AND nutrition_supported = 1 AND energy_kcal_100g > 0 ORDER BY ingredient_id),
        (SELECT unit_id FROM [UNIT] WHERE code = N'g'), 100);`)
    sql(
      `INSERT INTO [SAVED_RECIPE] (user_id, recipe_id) VALUES (${ownerId}, ${secondId});`,
    )

    await page.goto("/dang-nhap")
    await page.getByLabel("Email").fill(ownerEmail)
    await page.getByRole("textbox", { name: "Mật khẩu" }).fill(password)
    const login = page.waitForResponse((response) =>
      response.url().endsWith("/auth/login"),
    )
    await page.getByRole("button", { name: "Đăng nhập", exact: true }).click()
    expect((await login).status()).toBe(200)
    await expect(page).toHaveURL(/\/trang-chu$/)
    const session = await page.evaluate(() =>
      JSON.parse(sessionStorage.getItem("mamxanh.auth")!),
    )
    const headers = { Authorization: `Bearer ${session.accessToken}` }
    const eligibility = await request.put(`${backend}/nutrition/profile/eligibility`, {
      headers,
      data: { status: "ELIGIBLE" },
    })
    expect(eligibility.status()).toBe(200)
    const read = page.waitForResponse(
      (response) =>
        response.request().method() === "GET" &&
        new URL(response.url()).pathname.endsWith("/meal-plans"),
    )
    await page.goto("/ke-hoach")
    const readResponse = await read
    expect(readResponse.status()).toBe(200)
    const selectedWeek = (await readResponse.json()).weekStartDate as string
    await page.goto(
      `/ke-hoach?weekStartDate=${selectedWeek}&mealDate=${selectedWeek}`,
    )
    const selectedDateLabel = new Date(`${selectedWeek}T12:00:00`).toLocaleDateString("vi-VN")
    const selectedDay = page.getByRole("region", { name: selectedDateLabel })
    await expect(
      selectedDay.getByRole("button", { name: /^Thêm món Bữa/ }),
    ).toHaveCount(3)
    expect(
      Number(
        sql(`SELECT COUNT(*) FROM [MEAL_PLAN] WHERE user_id = ${ownerId}`),
      ),
    ).toBe(0)

    await selectedDay.getByRole("button", { name: /^Thêm món Bữa trưa/ }).click()
    const dialog = page.getByRole("dialog")
    // #154: firstId is public but has never been saved by this Member.
    await dialog.getByRole("button", { name: "Tìm công thức", exact: true }).click()
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
    expect(Number(sql(`SELECT COUNT(*) FROM [SAVED_RECIPE] WHERE user_id = ${ownerId} AND recipe_id = ${firstId}`))).toBe(0)
    expect(Number(sql(`SELECT COUNT(*) FROM [SAVED_RECIPE] WHERE user_id = ${ownerId}`))).toBe(1)
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

    await page.getByRole("button", { name: "Dinh dưỡng cả tuần", exact: true }).click()
    const nutritionDialog = page.getByRole("dialog", { name: "Dinh dưỡng theo lịch ăn" })
    await expect(nutritionDialog).toBeVisible()
    const modalWeekPdf = page.waitForResponse((response) => {
      const url = new URL(response.url())
      return url.pathname.endsWith("/nutrition/report") && !url.searchParams.has("date")
    })
    const modalWeekDownload = page.waitForEvent("download")
    await nutritionDialog.getByRole("button", { name: "Xuất báo cáo PDF" }).click()
    expect((await modalWeekPdf).status()).toBe(200)
    const modalWeekFile = await modalWeekDownload
    expect(modalWeekFile.suggestedFilename()).toBe(`bao-cao-dinh-duong-${selectedWeek}.pdf`)
    expect((await readFile((await modalWeekFile.path())!)).subarray(0, 5).toString()).toBe("%PDF-")

    await nutritionDialog.getByRole("button", { name: "Theo ngày" }).click()
    await nutritionDialog.getByLabel("Ngày xem").selectOption(selectedWeek)
    const modalDayPdf = page.waitForResponse((response) => {
      const url = new URL(response.url())
      return url.pathname.endsWith("/nutrition/report") && url.searchParams.get("date") === selectedWeek
    })
    const modalDayDownload = page.waitForEvent("download")
    await nutritionDialog.getByRole("button", { name: "Xuất báo cáo PDF" }).click()
    expect((await modalDayPdf).status()).toBe(200)
    const modalDayFile = await modalDayDownload
    expect(modalDayFile.suggestedFilename()).toBe(`bao-cao-dinh-duong-${selectedWeek}.pdf`)
    expect((await readFile((await modalDayFile.path())!)).subarray(0, 5).toString()).toBe("%PDF-")

    await page.keyboard.press("Escape")
    await page.goto("/dinh-duong")
    const nutritionDayLabel = new Date(`${selectedWeek}T12:00:00`).toLocaleDateString("vi-VN", {
      weekday: "long", day: "numeric", month: "numeric",
    })
    await page.getByRole("button", { name: `Ngày ${nutritionDayLabel}` }).click()
    await expect(page.getByText(firstTitle)).toBeVisible()
    await page.getByRole("button", { name: "So sánh & thống kê 7 ngày" }).click()
    const trackerWeekPdf = page.waitForResponse((response) => {
      const url = new URL(response.url())
      return url.pathname.endsWith("/nutrition/report") && !url.searchParams.has("date")
    })
    const trackerWeekDownload = page.waitForEvent("download")
    await page.getByRole("button", { name: "Xuất báo cáo PDF" }).click()
    expect((await trackerWeekPdf).status()).toBe(200)
    const trackerWeekFile = await trackerWeekDownload
    expect(trackerWeekFile.suggestedFilename()).toBe(`bao-cao-dinh-duong-${selectedWeek}.pdf`)
    expect((await readFile((await trackerWeekFile.path())!)).subarray(0, 5).toString()).toBe("%PDF-")
    await page.getByRole("button", { name: "Theo ngày" }).click()
    const trackerDayPdf = page.waitForResponse((response) => {
      const url = new URL(response.url())
      return url.pathname.endsWith("/nutrition/report") && url.searchParams.get("date") === selectedWeek
    })
    const trackerDayDownload = page.waitForEvent("download")
    await page.getByRole("button", { name: "Xuất báo cáo PDF" }).click()
    expect((await trackerDayPdf).status()).toBe(200)
    const trackerDayFile = await trackerDayDownload
    expect(trackerDayFile.suggestedFilename()).toBe(`bao-cao-dinh-duong-${selectedWeek}.pdf`)
    expect((await readFile((await trackerDayFile.path())!)).subarray(0, 5).toString()).toBe("%PDF-")
    await page.goto(`/ke-hoach?weekStartDate=${selectedWeek}&mealDate=${selectedWeek}`)
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
    // FR-33: move through the UI, persist the same row/servings, and reject cross-week or foreign moves.
    sql(
      `UPDATE [MEAL_PLAN_ENTRY] SET planned_servings = 2.5 WHERE meal_plan_entry_id = ${entryId}`,
    )
    await page.reload()
    await page
      .getByRole("button", { name: `Chuyển món ${firstTitle}`, exact: true })
      .click()
    await page.getByLabel("Bữa đích").selectOption("DINNER")
    const moved = page.waitForResponse(
      (response) =>
        response.request().method() === "PATCH" &&
        response.url().endsWith("/position"),
    )
    await page
      .getByRole("button", { name: "Xác nhận chuyển", exact: true })
      .click()
    expect((await moved).status()).toBe(200)
    expect(
      Number(
        sql(
          `SELECT COUNT(*) FROM [MEAL_PLAN_ENTRY] WHERE meal_plan_entry_id = ${entryId} AND meal_type = 'DINNER' AND planned_servings = 2.5`,
        ),
      ),
    ).toBe(1)
    const foreignMove = await request.patch(`${entryUrl}/position`, {
      headers: { Authorization: `Bearer ${otherToken}` },
      data: { mealDate: selectedWeek, mealType: "BREAKFAST" },
    })
    expect(foreignMove.status()).toBe(403)
    const outside = new Date(`${selectedWeek}T12:00:00`)
    outside.setDate(outside.getDate() + 7)
    const outsideDate = `${outside.getFullYear()}-${String(outside.getMonth() + 1).padStart(2, "0")}-${String(outside.getDate()).padStart(2, "0")}`
    expect(
      (
        await request.patch(`${entryUrl}/position`, {
          headers,
          data: { mealDate: outsideDate, mealType: "BREAKFAST" },
        })
      ).status(),
    ).toBe(400)
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
      page.locator(`a[href^="/cong-thuc/id/${firstId}"]`),
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
    ).toBe(0)
    expect(Number(sql(`SELECT COUNT(*) FROM [SAVED_RECIPE] WHERE user_id = ${ownerId} AND recipe_id = ${secondId}`))).toBe(1)
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
