import { expect, test } from "./baseFixtures"
import type { Page } from "@playwright/test"
import { readFile, mkdir } from "node:fs/promises"
import path from "node:path"

const week = "2026-10-12"
function item(id = 1, date = week, meal = "LUNCH") {
  return {
    entryId: id,
    mealDate: date,
    mealType: meal,
    plannedServings: 2.5,
    recipeId: 11,
    recipeTitle: "Đậu phụ kho sả",
    recipeCoverUrl: null,
    dishCategory: "SOUP",
    totalTimeMinutes: 25,
    recipeDeleted: false,
    unavailableMessage: null,
  }
}
async function setup(page: Page, initial = [item()]) {
  const state = {
    entries: initial,
    fail: false,
    moves: 0,
    exportFail: false,
    longExport: false,
  }
  await page.addInitScript(() =>
    sessionStorage.setItem(
      "mamxanh.auth",
      JSON.stringify({
        accessToken: "fr33-test-token",
        expiresAt: Date.now() + 3600000,
        account: {
          id: 20,
          displayName: "Người lập lịch",
          email: "fr33@example.test",
          role: "CUSTOMER",
          accountStatus: "ACTIVE",
          emailVerified: true,
        },
      }),
    ),
  )
  await page.route("**/api/v1/meal-plans**", async (route) => {
    const req = route.request()
    if (req.url().endsWith("/export")) {
      if (state.exportFail)
        return route.fulfill({
          status: 503,
          json: {
            status: 503,
            code: "SERVICE_UNAVAILABLE",
            detail: "Chưa xuất được lịch. Thử lại.",
          },
        })
      const entries = [
        {
          mealDate: week,
          mealType: "LUNCH",
          title: "Đậu phụ kho sả",
          plannedServings: 2.5,
          energyKcal: 250,
          available: true,
        },
        {
          mealDate: week,
          mealType: "LUNCH",
          title: "Canh nấm thiếu dữ liệu",
          plannedServings: 1,
          energyKcal: null,
          available: true,
        },
        {
          mealDate: week,
          mealType: "DINNER",
          title: "Công thức không còn khả dụng",
          plannedServings: null,
          energyKcal: null,
          available: false,
        },
      ]
      if (state.longExport)
        for (let i = 0; i < 80; i++)
          entries.push({
            ...entries[0],
            title: `Món ${i + 1} · Công thức tiếng Việt nhiều nguyên liệu ${"rau xanh và nấm ".repeat(5)}`,
          })
      return route.fulfill({ json: { weekStartDate: week, entries } })
    }
    if (req.method() === "GET")
      return route.fulfill({
        json: { weekStartDate: week, entries: state.entries },
      })
    if (req.url().endsWith("/position")) {
      state.moves++
      if (state.fail)
        return route.fulfill({
          status: 409,
          json: {
            status: 409,
            code: "MEAL_PLAN_RECIPE_DUPLICATE",
            detail: "Món đã có trong bữa đích.",
          },
        })
      state.entries[0] = { ...state.entries[0], ...req.postDataJSON() }
      return route.fulfill({ json: state.entries[0] })
    }
    return route.fulfill({ status: 500 })
  })
  await page.goto(`/ke-hoach?weekStartDate=${week}&mealDate=${week}`)
  await expect(
    page.getByRole("article", { name: "Đậu phụ kho sả" }).first(),
  ).toBeVisible()
  return state
}

test("AC-33.4 moves across days, preserves servings, survives reload and restores focus", async ({
  page,
}) => {
  const state = await setup(page)
  await page
    .getByRole("button", { name: "Chuyển món Đậu phụ kho sả", exact: true })
    .click()
  const dialog = page.getByRole("dialog", { name: "Chuyển món trong tuần" })
  await expect(dialog.getByLabel("Ngày đích").locator("option")).toHaveCount(7)
  await expect(
    dialog.getByRole("button", { name: "Xác nhận chuyển" }),
  ).toBeDisabled()
  await dialog.getByLabel("Ngày đích").selectOption("2026-10-13")
  await dialog.getByLabel("Bữa đích").selectOption("DINNER")
  await dialog.getByRole("button", { name: "Xác nhận chuyển" }).click()
  await expect(dialog).not.toBeVisible()
  // Issue #146: moving opens the destination without collapsing the source day.
  for (const day of [week, "2026-10-13"]) {
    await expect(page.getByRole("button", {
      name: `Xem ngày ${new Date(`${day}T12:00:00`).toLocaleDateString("vi-VN")}`, exact: true,
    })).toHaveAttribute("aria-expanded", "true")
  }
  expect(state.entries[0]).toMatchObject({
    entryId: 1,
    mealDate: "2026-10-13",
    mealType: "DINNER",
    plannedServings: 2.5,
  })
  await expect(
    page.getByRole("article", { name: "Đậu phụ kho sả" }),
  ).toContainText("2,5 phần")
  await expect(
    page.getByRole("heading", { name: "Lịch ăn của bạn" }),
  ).toBeFocused()
  await page.reload()
  await expect(
    page.getByRole("article", { name: "Đậu phụ kho sả" }),
  ).toBeVisible()
  expect(state.moves).toBe(1)
})

test("AC-33.4 prevents duplicate destinations and server conflicts keep source until retry succeeds", async ({
  page,
}) => {
  const state = await setup(page, [item(), item(2, week, "DINNER")])
  await page
    .getByRole("button", { name: "Chuyển món Đậu phụ kho sả", exact: true })
    .first()
    .click()
  const dialog = page.getByRole("dialog", { name: "Chuyển món trong tuần" })
  await dialog.getByLabel("Bữa đích").selectOption("DINNER")
  await expect(dialog.getByRole("alert")).toContainText("đã có trong bữa đích")
  await expect(
    dialog.getByRole("button", { name: "Xác nhận chuyển" }),
  ).toBeDisabled()
  expect(state.moves).toBe(0)
  await dialog.getByLabel("Ngày đích").selectOption("2026-10-13")
  state.fail = true
  await dialog.getByRole("button", { name: "Xác nhận chuyển" }).click()
  await expect(dialog.getByRole("alert")).toContainText("Món đã có")
  expect(state.entries[0].mealDate).toBe(week)
  state.fail = false
  await dialog.getByRole("button", { name: "Xác nhận chuyển" }).click()
  await expect(dialog).not.toBeVisible()
  expect(state.entries[0].mealDate).toBe("2026-10-13")
})

test("AC-33.7 downloads UTF-8 TXT with every day/meal, real energy, missing data and tombstone", async ({
  page,
}) => {
  await setup(page)
  await page.getByRole("button", { name: "Xuất lịch ăn", exact: true }).click()
  const pending = page.waitForEvent("download")
  await page.getByRole("button", { name: "Tải TXT", exact: true }).click()
  const download = await pending
  expect(download.suggestedFilename()).toBe(`lich-an-${week}.txt`)
  const content = await readFile((await download.path())!, "utf8")
  expect(content).toContain("Đậu phụ kho sả · 2,5 khẩu phần · 250 kcal")
  expect(content).toContain("Chưa đủ dữ liệu dinh dưỡng")
  expect(content).toContain("Công thức không còn khả dụng")
  expect(content).not.toMatch(/(?:^|\s)0 kcal/)
  for (const meal of ["Bữa sáng", "Bữa trưa", "Bữa tối"])
    expect(content.split(meal)).toHaveLength(8)
  expect(content).toContain("18/10/2026")
})

test("AC-33.7 creates actual multipage PDF and export failure can be retried", async ({
  page,
}) => {
  const state = await setup(page)
  state.exportFail = true
  await page.getByRole("button", { name: "Xuất lịch ăn", exact: true }).click()
  await page.getByRole("button", { name: "Tải PDF A4", exact: true }).click()
  await expect(page.getByRole("dialog").getByRole("alert")).toContainText(
    "Chưa xuất được lịch",
  )
  state.exportFail = false
  state.longExport = true
  const pending = page.waitForEvent("download")
  await page.getByRole("button", { name: "Tải PDF A4", exact: true }).click()
  const download = await pending
  const directory = path.resolve("../../.agents/outputs/fr33")
  await mkdir(directory, { recursive: true })
  await download.saveAs(path.join(directory, "weekly-meal-plan.pdf"))
  const bytes = await readFile((await download.path())!)
  expect(bytes.subarray(0, 5).toString()).toBe("%PDF-")
  expect(bytes.toString("latin1")).toContain("/MediaBox [0 0 595.")
  expect(
    bytes.toString("latin1").match(/\/Type \/Page\b/g)!.length,
  ).toBeGreaterThan(1)
})

for (const width of [360, 768, 1440])
  test(`FR-33 move/export remain usable at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 1000 })
    await setup(page)
    await page
      .getByRole("button", { name: "Chuyển món Đậu phụ kho sả", exact: true })
      .click()
    await expect(page.getByLabel("Ngày đích")).toBeFocused()
    await page.keyboard.press("Escape")
    await expect(page.getByRole("dialog")).not.toBeVisible()
    await page
      .getByRole("button", { name: "Xuất lịch ăn", exact: true })
      .click()
    await expect(
      page.getByRole("button", { name: "Tải PDF A4", exact: true }),
    ).toBeFocused()
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true)
    await page.screenshot({
      path: path.resolve(`../../.agents/outputs/fr33/export-${width}.png`),
    })
  })
