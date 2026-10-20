import { readFile } from "node:fs/promises"

import type { Page } from "@playwright/test"

import { expect, test } from "./baseFixtures"

import { seedDemoSession } from "./demo-session"

import type { ShoppingChecklist, ShoppingItem } from "../../src/api/shopping"

const week = "2026-10-12"

const row = (
  id: number,
  overrides: Partial<ShoppingItem> = {},
): ShoppingItem => ({
  id,
  ingredientId: 5,
  name: "Nước tương",
  ingredientGroup: "Gia vị",
  quantity: 300,
  unitId: 1,
  unitCode: "g",
  unitName: "gram",
  bought: false,
  ...overrides,
})

const summary = {
  id: 42,
  name: "Đi chợ tuần 12/10",
  createdAt: "",
  updatedAt: "",
}

for (const name of [
  "Đi chợ · thực đơn tuần 19–25/10/2026",
  "Đi chợ · thực đơn 19/10/2026",
  "Đi chợ · thực đơn 28/12/2026, 30/12/2026, 01/01/2027, 03/01/2027",
]) {
  test(`shopping preserves the full meal-date title on mobile and reload: ${name}`, async ({ page }) => {
    const state = await setup(page)
    state.list.name = name
    await page.setViewportSize({ width: 375, height: 812 })
    await page.goto("/di-cho?listId=42")
    const heading = page.getByRole("heading", { name, exact: true })
    const saved = page.getByRole("navigation", { name: "Danh sách đã lưu" })
    await expect(heading).toBeVisible()
    await expect(saved.getByRole("button").first()).toContainText(name)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
    await page.reload()
    await expect(heading).toBeVisible()
    await expect(saved.getByRole("button").first()).toContainText(name)
    expect(state.creates).toBe(0)
  })
}

async function setup(
  page: Page,
  role: "CUSTOMER" | "EXPERT" | "ADMIN" = "CUSTOMER",
) {
  await seedDemoSession(page, role)

  const state = {
    list: {
      ...summary,
      items: [row(1), row(2, { quantity: 200, unitId: 2, unitCode: "ml" })],
    } as ShoppingChecklist,
    fail: "",
    readFail: false,
    empty: false,
    pages: 1,
    writes: [] as unknown[],
    createPayload: null as unknown,
    creates: 0,
  }

  await page.route("**/api/v1/meal-plans?**", (route) =>
    route.fulfill({
      json: {
        weekStartDate: week,
        entries: [
          {
            entryId: 7,
            mealDate: week,
            mealType: "LUNCH",
            recipeId: 9,
            recipeTitle: "Món chay",
            plannedServings: 2,
            recipeDeleted: false,
          },
        ],
      },
    }),
  )

  await page.route("**/api/v1/shopping-lists**", async (route) => {
    const request = route.request(),
      url = new URL(request.url()),
      suffix = url.pathname.replace("/api/v1/shopping-lists", ""),
      method = request.method()

    if (method !== "GET") {
      state.writes.push({ method, suffix, body: request.postDataJSON() })

      if (state.fail)
        return route.fulfill({
          status: 400,
          json: { status: 400, code: "VALIDATION_FAILED", detail: state.fail },
          contentType: "application/problem+json",
        })
    }

    if (state.readFail && method === "GET")
      return route.fulfill({
        status: 503,
        json: { status: 503, code: "SERVICE_UNAVAILABLE", detail: "Chưa tải được danh sách." },
        contentType: "application/problem+json",
      })

    if (suffix === "/from-meal-plan") {
      state.creates++
      state.createPayload = request.postDataJSON()

      return route.fulfill({
        status: 201,
        json: {
          shoppingList: state.list,
          warnings: [
            {
              code: "UNAVAILABLE_RECIPE",
              message: "Một công thức không còn khả dụng đã được bỏ qua.",
              recipeId: 8,
              mealPlanEntryId: 6,
            },
          ],
          incompatibleItemIds: [1, 2],
        },
      })
    }

    if (suffix === "/options")
      return route.fulfill({
        json: [
          { id: 1, code: "g", name: "gram", active: true },
          { id: 2, code: "ml", name: "mililít", active: true },
          { id: 3, code: "cuộn", name: "cuộn", active: true },
        ],
      })

    if (suffix === "")
      return route.fulfill({
        json: {
          content: state.empty ? [] : [{ ...summary, name: state.list.name }],
          page: Number(url.searchParams.get("page") ?? 0),
          size: 20,
          totalElements: state.empty ? 0 : 21,
          totalPages: state.pages,
        },
      })

    if (suffix === "/42" && method === "GET")
      return route.fulfill({ json: state.list })

    if (suffix === "/999")
      return route.fulfill({
        status: 404,
        json: { status: 404, code: "NOT_FOUND", detail: "Không tìm thấy danh sách." },
        contentType: "application/problem+json",
      })

    if (suffix === "/42/items" && method === "POST") {
      const body = request.postDataJSON(),
        unit = body.unitId === 3 ? "cuộn" : "g"

      const saved = row(50, {
        ...body,
        ingredientId: null,
        ingredientGroup: "Nhóm khác",
        unitCode: unit,
        bought: false,
      })

      state.list.items.push(saved)
      return route.fulfill({ status: 201, json: saved })
    }

    if (method === "DELETE") {
      state.list.items = suffix.endsWith("/items")
        ? state.list.items.filter((item) => !item.bought)
        : state.list.items.filter(
            (item) => item.id !== Number(suffix.split("/").at(-1)),
          )

      return route.fulfill({ status: 204 })
    }

    const id = Number(suffix.split("/")[3]),
      current = state.list.items.find((item) => item.id === id)!

    const body = request.postDataJSON()

    Object.assign(current, body)

    if (body.unitId)
      current.unitCode =
        body.unitId === 3 ? "cuộn" : body.unitId === 2 ? "ml" : "g"

    return route.fulfill({ json: current })
  })

  return state
}

test("shopping blocks Guest and Admin without reading private lists", async ({
  page,
}) => {
  let calls = 0

  await page.route("**/api/v1/shopping-lists**", () => {
    calls++
  })

  await page.goto("/di-cho")

  await expect(
    page.getByText(
      "Vui lòng đăng nhập để tạo và lưu danh sách đi chợ theo tài khoản.",
    ),
  ).toBeVisible()

  expect(calls).toBe(0)

  await seedDemoSession(page, "ADMIN")
  await page.reload()

  await expect(page.getByRole("alert")).toContainText("Quản trị viên")

  expect(calls).toBe(0)
})

test("shopping creates whole week, displays warning and keeps incompatible rows after reload", async ({
  page,
}) => {
  const state = await setup(page, "EXPERT")

  await page.goto(`/ke-hoach?weekStartDate=${week}`)

  await page
    .getByRole("button", { name: "Tạo danh sách đi chợ", exact: true })
    .click()

  await expect(page.getByLabel("Cả tuần", { exact: true })).toBeFocused()
  const dialog = page.getByRole("dialog")
  await dialog.getByRole("button", { name: "Tạo danh sách", exact: true }).focus()
  await page.keyboard.press("Tab")
  await expect(dialog.getByRole("button", { name: "Đóng", exact: true })).toBeFocused()
  await page.keyboard.press("Shift+Tab")
  await expect(dialog.getByRole("button", { name: "Tạo danh sách", exact: true })).toBeFocused()

  await page.getByRole("button", { name: "Tạo danh sách", exact: true }).click()

  await expect(page).toHaveURL(/di-cho\?listId=42/)

  expect(state.createPayload).toEqual({ weekStartDate: week })

  expect(state.creates).toBe(1)

  await expect(
    page.getByRole("region", { name: "Lưu ý khi tạo danh sách" }),
  ).toContainText("bỏ qua")

  await expect(page.getByTestId("shopping-item-1")).toContainText("300 g")

  await expect(page.getByTestId("shopping-item-2")).toContainText("200 ml")

  await page.reload()

  await expect(page.getByText(/Giữ dòng riêng theo đơn vị/)).toHaveCount(2)

  expect(state.creates).toBe(1)
})

test("shopping selected days validates selection, shows creation failure, retries and cancels by keyboard", async ({
  page,
}) => {
  const state = await setup(page)

  await page.goto(`/ke-hoach?weekStartDate=${week}`)

  const trigger = page.getByRole("button", {
    name: "Tạo danh sách đi chợ",
    exact: true,
  })

  await trigger.click()
  await page.getByLabel("Chọn ngày", { exact: true }).check()

  await expect(
    page.getByRole("button", { name: "Tạo danh sách", exact: true }),
  ).toBeDisabled()

  await page.getByRole("checkbox", { name: /Thứ Hai/i }).check()

  await page.getByRole("checkbox", { name: /Thứ Ba/i }).check()

  await page.getByRole("checkbox", { name: /Thứ Ba/i }).uncheck()

  state.fail = "Ngày đã chọn chưa có nguyên liệu hợp lệ."

  await page.getByRole("button", { name: "Tạo danh sách", exact: true }).click()

  await expect(page.getByRole("alert")).toContainText(state.fail)

  await page.keyboard.press("Escape")
  await expect(trigger).toBeFocused()

  await trigger.click()
  await page.getByLabel("Chọn ngày", { exact: true }).check()

  await page.getByRole("checkbox", { name: /Thứ Hai/i }).check()

  state.fail = ""

  await page.getByRole("button", { name: "Tạo danh sách", exact: true }).click()

  await expect(page).toHaveURL(/di-cho\?listId=42/)

  expect(state.createPayload).toEqual({
    weekStartDate: week,
    selectedDates: [week],
  })
})

test("shopping manual item quantity, edit, bought/unbought, filtering, deletion and TXT use saved values", async ({
  page,
}) => {
  const state = await setup(page)

  await page.goto("/di-cho")

  await expect(page).toHaveURL(/listId=42/)

  await page.getByRole("button", { name: "Thêm mặt hàng", exact: true }).click()

  await page.getByLabel("Tên mặt hàng").fill("Khăn giấy lau bếp")

  await page.getByLabel("Số lượng", { exact: true }).fill("2")

  await page.getByLabel("Đơn vị", { exact: true }).selectOption("3")

  await page.getByRole("button", { name: "Lưu mặt hàng" }).click()

  const item = page.getByTestId("shopping-item-50")

  await expect(item).toContainText("2 cuộn")

  await page
    .getByRole("button", { name: "Sửa Khăn giấy lau bếp cuộn", exact: true })
    .click()

  await page.getByLabel("Số lượng", { exact: true }).fill("3.5")

  await page.getByRole("button", { name: "Lưu mặt hàng" }).click()

  await expect(item).toContainText("3,5 cuộn")

  const check = page.getByRole("button", {
    name: "Đánh dấu Khăn giấy lau bếp cuộn",
    exact: true,
  })

  await check.click()
  await expect(check).toHaveAttribute("aria-pressed", "true")

  await page.getByRole("button", { name: /Cần mua \(/ }).click()
  await expect(item).toHaveCount(0)

  await page.getByRole("button", { name: /Đã mua \(/ }).click()
  await expect(item).toBeVisible()

  const downloadPromise = page.waitForEvent("download")
  await page.getByRole("button", { name: "TXT", exact: true }).click()

  const download = await downloadPromise

  expect(await readFile((await download.path())!, "utf8")).toContain(
    "[x] Khăn giấy lau bếp: 3,5 cuộn",
  )

  await check.click()
  await page.getByRole("button", { name: /Tất cả \(/ }).click()

  await page
    .getByRole("button", { name: "Xoá Khăn giấy lau bếp cuộn", exact: true })
    .click()

  await page.getByRole("button", { name: "Hủy", exact: true }).click()
  await expect(item).toBeVisible()

  await page
    .getByRole("button", { name: "Xoá Khăn giấy lau bếp cuộn", exact: true })
    .click()

  await page.getByRole("button", { name: "Xoá", exact: true }).click()
  await expect(item).toHaveCount(0)

  expect(state.list.items).toHaveLength(2)
})

test("shopping failed mutations preserve data and form, cleanup preserves unbought rows", async ({
  page,
}) => {
  const state = await setup(page)

  await page.goto("/di-cho?listId=42")

  const check = page.getByRole("button", {
    name: "Đánh dấu Nước tương g",
    exact: true,
  })

  state.fail = "Chưa lưu được. Hãy thử lại."

  await check.click()
  await expect(page.getByRole("alert")).toContainText(state.fail)

  await expect(check).toHaveAttribute("aria-pressed", "false")

  await page
    .getByRole("button", { name: "Sửa Nước tương g", exact: true })
    .click()

  await page.getByLabel("Số lượng", { exact: true }).fill("400")

  await page.getByRole("button", { name: "Lưu mặt hàng" }).click()

  await expect(page.getByRole("dialog").getByRole("alert")).toContainText(
    state.fail,
  )

  await expect(page.getByLabel("Số lượng", { exact: true })).toHaveValue("400")

  state.fail = ""
  await page.getByRole("button", { name: "Lưu mặt hàng" }).click()

  await expect(page.getByTestId("shopping-item-1")).toContainText("400 g")

  await check.click()

  await page
    .getByRole("button", { name: "Xoá 1 mục đã mua", exact: true })
    .click()

  state.fail = "Chưa xoá được."

  await page.getByRole("button", { name: "Xoá", exact: true }).click()

  await expect(page.getByRole("alert")).toContainText(state.fail)

  state.fail = ""
  await page.getByRole("button", { name: "Xoá", exact: true }).click()

  await expect(page.getByTestId("shopping-item-1")).toHaveCount(0)

  await expect(page.getByTestId("shopping-item-2")).toBeVisible()
})

test("shopping empty list, invalid ID, foreign list and read retry are explicit", async ({
  page,
}) => {
  const state = await setup(page)
  state.empty = true

  await page.goto("/di-cho")
  await expect(
    page.getByText("Chưa có danh sách đi chợ", { exact: true }),
  ).toBeVisible()

  await page.goto("/di-cho?listId=bad")
  await expect(page.getByRole("alert")).toContainText("không hợp lệ")

  state.empty = false
  state.readFail = true

  await page.goto("/di-cho?listId=42")

  await expect(
    page.getByRole("button", { name: "Thử tải danh sách" }),
  ).toBeVisible()

  state.readFail = false

  await page.getByRole("button", { name: "Thử tải danh sách" }).click()

  await page.getByRole("button", { name: "Thử lại", exact: true }).click()

  await expect(page.getByTestId("shopping-item-1")).toBeVisible()

  await page.goto("/di-cho?listId=999")
  await expect(page.getByRole("alert")).toContainText("Không tìm thấy")

  await expect(page.getByTestId("shopping-item-1")).toHaveCount(0)
})

test("shopping pagination and small viewport preserve usable checklist and copy fallback", async ({
  page,
}) => {
  const state = await setup(page)
  state.pages = 2

  await page.setViewportSize({ width: 375, height: 812 })

  await page.goto("/di-cho?listId=42")

  await expect(page.getByTestId("shopping-item-1")).toBeVisible()

  await page.getByRole("button", { name: "Sau", exact: true }).click()

  await expect(page.getByText("2/2", { exact: true })).toBeVisible()

  await page.getByRole("button", { name: "Trước", exact: true }).click()

  await expect(page.getByText("1/2", { exact: true })).toBeVisible()

  await page.evaluate(() =>
    Object.defineProperty(navigator, "clipboard", {
      configurable: true,
      value: { writeText: () => Promise.reject(new Error("denied")) },
    }),
  )

  await page.getByRole("button", { name: "Sao chép", exact: true }).click()

  await expect(page.getByRole("dialog", { name: "Sao chép danh sách đi chợ" })).toBeVisible()

  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true)

  await page.screenshot({
    path: "../../.agents/outputs/fr53-ui/mobile.png",
    fullPage: true,
  })

  const checkBounds = await page.getByRole("button", { name: "Đánh dấu Nước tương g", exact: true }).boundingBox()
  const deleteBounds = await page.getByRole("button", { name: "Xoá Nước tương g", exact: true }).boundingBox()
  expect(Math.abs(checkBounds!.y - deleteBounds!.y)).toBeLessThan(10)
  await page.setViewportSize({ width: 768, height: 1024 })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await expect(page.getByTestId("shopping-item-1")).toBeVisible()

  await page.setViewportSize({ width: 1440, height: 1000 })

  await page.screenshot({
    path: "../../.agents/outputs/fr53-ui/desktop.png",
    fullPage: true,
  })
})
