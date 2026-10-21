import { expect, test } from "./baseFixtures"
import type { Page } from "@playwright/test"

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

for (const width of [360, 768, 1440])
  test(`FR-33 move remains usable at ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 1000 })
    await setup(page)
    await page
      .getByRole("button", { name: "Chuyển món Đậu phụ kho sả", exact: true })
      .click()
    await expect(page.getByLabel("Ngày đích")).toBeFocused()
    await page.keyboard.press("Escape")
    await expect(page.getByRole("dialog")).not.toBeVisible()
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true)
    await page.screenshot({
      path: `test-results/meal-plan-${width}.png`,
    })
  })
