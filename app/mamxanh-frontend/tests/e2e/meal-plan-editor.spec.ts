import { expect, test } from "./baseFixtures"
import type { Page } from "@playwright/test"
import { mkdir } from "node:fs/promises"
import path from "node:path"

function monday() {
  const date = new Date()
  date.setDate(date.getDate() - ((date.getDay() + 6) % 7))
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
}
const week = monday()
const recipe = (id: number, title: string) => ({
  id,
  title,
  media: [],
  status: "PUBLISHED",
  prepTimeMin: 10,
  cookTimeMin: 15,
})
const recipes = [recipe(11, "Canh nấm"), recipe(12, "Đậu phụ kho")]
const entry = (id = 1, recipeId = 11, unavailable = false) => ({
  entryId: id,
  mealDate: week,
  mealType: "LUNCH",
  plannedServings: 1,
  recipeId,
  recipeTitle: unavailable
    ? null
    : recipes.find((item) => item.id === recipeId)?.title,
  recipeCoverUrl: null,
  dishCategory: unavailable ? null : "SOUP",
  totalTimeMinutes: unavailable ? null : 25,
  recipeDeleted: unavailable,
  unavailableMessage: unavailable ? "Công thức không còn khả dụng" : null,
})

async function setup(page: Page, initial: ReturnType<typeof entry>[] = []) {
  const state = {
    entries: initial,
    writes: 0,
    fail: "",
    searchFail: false,
    hold: null as null | Promise<void>,
  }
  await page.addInitScript(() =>
    sessionStorage.setItem(
      "mamxanh.auth",
      JSON.stringify({
        accessToken: "fr09-browser-fixture-token",
        expiresAt: Date.now() + 3600000,
        account: {
          id: 20,
          displayName: "Người lập lịch",
          email: "fr09-ui@example.test",
          role: "CUSTOMER",
          accountStatus: "ACTIVE",
          emailVerified: true,
        },
      }),
    ),
  )
  await page.route("**/api/v1/recipes?**", (route) => {
    if (state.searchFail)
      return route.fulfill({
        status: 503,
        contentType: "application/problem+json",
        body: JSON.stringify({
          status: 503,
          code: "SERVICE_UNAVAILABLE",
          detail: "Chưa tải được công thức.",
        }),
      })
    const url = new URL(route.request().url())
    const keyword = url.searchParams.get("keyword") ?? ""
    const items = recipes.filter((item) =>
      item.title.toLowerCase().includes(keyword.toLowerCase()),
    )
    return route.fulfill({
      json: {
        items,
        page: 0,
        size: 12,
        totalElements: items.length,
        totalPages: 1,
      },
    })
  })
  await page.route("**/api/v1/saved-recipes**", (route) =>
    route.fulfill({
      json: {
        content: [
          {
            recipeId: 12,
            title: "Đậu phụ kho",
            coverUrl: null,
            available: true,
          },
          { recipeId: 99, title: null, coverUrl: null, available: false },
        ],
        page: 0,
        size: 12,
        totalElements: 2,
        totalPages: 1,
      },
    }),
  )
  await page.route("**/api/v1/meal-plans**", async (route) => {
    const request = route.request()
    const url = new URL(request.url())
    const selectedWeek =
      url.searchParams.get("weekStartDate") ?? url.pathname.split("/")[4]
    if (request.method() === "GET") {
      const end = new Date(`${selectedWeek}T12:00:00`)
      end.setDate(end.getDate() + 6)
      const lastDay = `${end.getFullYear()}-${String(end.getMonth() + 1).padStart(2, "0")}-${String(end.getDate()).padStart(2, "0")}`
      return route.fulfill({
        json: {
          weekStartDate: selectedWeek,
          entries: state.entries.filter(
            (item) => item.mealDate >= selectedWeek && item.mealDate <= lastDay,
          ),
        },
      })
    }
    state.writes++
    expect(request.headers().authorization).toBe(
      "Bearer fr09-browser-fixture-token",
    )
    if (state.hold) await state.hold
    if (state.fail)
      return route.fulfill({
        status:
          state.fail === "conflict" ? 409 : state.fail === "auth" ? 401 : 503,
        contentType: "application/problem+json",
        body: JSON.stringify({
          code:
            state.fail === "conflict"
              ? "MEAL_PLAN_RECIPE_DUPLICATE"
              : "SERVICE_UNAVAILABLE",
          detail:
            state.fail === "conflict"
              ? "Công thức đã có trong bữa ăn này."
              : "Chưa lưu được lịch ăn.",
        }),
      })
    if (url.pathname.endsWith("/meal-plans"))
      return route.fulfill({
        json: { weekStartDate: selectedWeek, entries: [] },
      })
    if (request.method() === "DELETE") {
      const id = Number(url.pathname.split("/").pop())
      state.entries = state.entries.filter((item) => item.entryId !== id)
      return route.fulfill({ status: 204 })
    }
    const payload = request.postDataJSON()
    if (request.method() === "POST") {
      const item = {
        ...entry(state.entries.length + 1, payload.recipeId),
        mealDate: payload.mealDate,
        mealType: payload.mealType,
      }
      state.entries.push(item)
      return route.fulfill({ status: 201, json: item })
    }
    const id = Number(url.pathname.split("/").pop())
    const current = state.entries.find((item) => item.entryId === id)!
    const item = {
      ...entry(id, payload.recipeId),
      mealDate: current.mealDate,
      mealType: current.mealType,
      plannedServings: current.plannedServings,
    }
    state.entries = state.entries.map((value) =>
      value.entryId === id ? item : value,
    )
    return route.fulfill({ json: item })
  })
  return state
}

const lunch = (page: Page) =>
  page.getByRole("button", { name: /^Thêm món Bữa trưa/ }).first()

test("empty week shows all 21 meals and requests creation only after an action", async ({
  page,
}) => {
  const state = await setup(page)
  await page.goto("/ke-hoach")
  await expect(page.getByRole("button", { name: /^Thêm món Bữa/ })).toHaveCount(
    21,
  )
  expect(state.writes).toBe(0)
  await page.getByRole("button", { name: "Lưu lịch tuần trống" }).click()
  await expect(page.getByRole("status")).toContainText("Đã lưu lịch tuần")
  expect(state.writes).toBe(1)
})

test("adds two different recipes to the same meal and retains them after reload", async ({
  page,
}) => {
  const state = await setup(page)
  await page.goto("/ke-hoach")
  await lunch(page).click()
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Canh nấm" })
    .click()
  await expect(page.getByRole("article", { name: "Canh nấm" })).toBeVisible()
  await lunch(page).click()
  await expect(
    page.getByRole("dialog").getByRole("button", { name: /Canh nấm/ }),
  ).toBeDisabled()
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Đậu phụ kho" })
    .click()
  expect(state.entries).toHaveLength(2)
  await page.reload()
  await expect(page.getByRole("article")).toHaveCount(2)
})

test("saved picker prevents unavailable choices and replaces a tombstone in place", async ({
  page,
}) => {
  const state = await setup(page, [
    { ...entry(8, 11, true), plannedServings: 2.5 },
  ])
  await page.goto("/ke-hoach")
  await expect(page.locator('a[href="/cong-thuc/id/11"]')).toHaveCount(0)
  await page
    .getByRole("button", { name: "Thay món Công thức không còn khả dụng" })
    .click()
  const dialog = page.getByRole("dialog")
  await dialog.getByRole("button", { name: "Đã lưu", exact: true }).click()
  await expect(
    dialog.getByRole("button", { name: /Công thức không còn khả dụng/ }),
  ).toBeDisabled()
  await dialog.getByRole("button", { name: "Đậu phụ kho" }).click()
  await expect(
    page.getByRole("article", { name: "Đậu phụ kho" }),
  ).toContainText("2.5 phần")
  expect(state.entries[0].entryId).toBe(8)
  expect(state.entries[0].mealType).toBe("LUNCH")
})

test("failed removal preserves the item and retry removes only its reference", async ({
  page,
}) => {
  const state = await setup(page, [entry(8, 11, true)])
  await page.goto("/ke-hoach")
  await page
    .getByRole("button", { name: "Gỡ món Công thức không còn khả dụng" })
    .click()
  state.fail = "network"
  await page.getByRole("button", { name: "Xác nhận gỡ" }).click()
  await expect(page.getByRole("dialog").getByRole("alert")).toContainText(
    "Chưa lưu được",
  )
  expect(state.entries).toHaveLength(1)
  state.fail = ""
  await page.getByRole("button", { name: "Xác nhận gỡ" }).click()
  await expect(page.getByRole("article")).toHaveCount(0)
})

test("server conflict stays visible without optimistic insertion", async ({
  page,
}) => {
  const state = await setup(page)
  state.fail = "conflict"
  await page.goto("/ke-hoach")
  await lunch(page).click()
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Canh nấm" })
    .click()
  await expect(page.getByRole("dialog").getByRole("alert")).toContainText(
    "đã có trong bữa",
  )
  await expect(page.getByRole("article")).toHaveCount(0)
})

test("picker search, retry and keyboard focus remain usable", async ({
  page,
}) => {
  const state = await setup(page)
  state.searchFail = true
  await page.goto("/ke-hoach")
  await lunch(page).click()
  const dialog = page.getByRole("dialog")
  await expect(dialog.getByLabel("Tìm món cho lịch ăn")).toBeFocused()
  await expect(dialog.getByRole("alert")).toContainText("Chưa tải")
  state.searchFail = false
  await dialog.getByRole("button", { name: "Thử lại" }).click()
  await expect(dialog.getByRole("button", { name: "Canh nấm" })).toBeVisible()
  await dialog.getByLabel("Tìm món cho lịch ăn").fill("Đậu")
  await dialog.getByRole("button", { name: "Tìm", exact: true }).click()
  await expect(dialog.getByRole("button", { name: "Canh nấm" })).toHaveCount(0)
  await dialog.getByRole("button", { name: "Đậu phụ kho" }).focus()
  await page.keyboard.press("Tab")
  await expect(
    dialog.getByRole("button", { name: "Đóng", exact: true }),
  ).toBeFocused()
  await page.keyboard.press("Escape")
  await expect(lunch(page)).toBeFocused()
})

test("late mutation cannot insert an old recipe into a newly selected week", async ({
  page,
}) => {
  const state = await setup(page)
  let release: () => void
  state.hold = new Promise<void>((resolve) => {
    release = resolve
  })
  const response = page.waitForResponse(value => value.request().method() === "POST" && value.url().includes("/meal-plans/"))
  await page.goto("/ke-hoach")
  await lunch(page).click()
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Canh nấm" })
    .click()
  await expect(page.getByText("Đang lưu món…")).toBeVisible()
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Đóng", exact: true })
    .click()
  await page.getByRole("button", { name: "Tuần sau", exact: true }).click()
  release()
  await response
  await expect(page.getByRole("article")).toHaveCount(0)
  await expect(page.getByText("Đã thêm món vào lịch ăn.")).toHaveCount(0)
})

test("authentication rejection leaves the editor and does not write as guest", async ({
  page,
}) => {
  const state = await setup(page)
  state.fail = "auth"
  await page.goto("/ke-hoach")
  await lunch(page).click()
  await page
    .getByRole("dialog")
    .getByRole("button", { name: "Canh nấm" })
    .click()
  await expect(page).toHaveURL(/\/dang-nhap/)
  expect(state.writes).toBe(1)
})

for (const width of [360, 768, 1440]) {
  test(`meal editor and picker fit ${width}px`, async ({ page }) => {
    await setup(page, [entry()])
    await page.setViewportSize({ width, height: 900 })
    await page.goto("/ke-hoach")
    await expect(page.getByRole("article", { name: "Canh nấm" })).toBeVisible()
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true)
    const directory = path.resolve(
      process.cwd(),
      "../../.agents/outputs/fr09-stage4/ui",
    )
    await mkdir(directory, { recursive: true })
    await page.screenshot({
      path: path.join(directory, `week-${width}.png`),
      fullPage: true,
    })
    await lunch(page).click()
    await expect(
      page.getByRole("dialog").getByRole("button", { name: "Đậu phụ kho" }),
    ).toBeVisible()
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBe(true)
    await page.screenshot({ path: path.join(directory, `picker-${width}.png`) })
  })
}
