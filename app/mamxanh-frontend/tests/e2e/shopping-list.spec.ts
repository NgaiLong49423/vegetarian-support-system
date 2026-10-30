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
  imageUrl: "/ingredients/nuoc-tuong.webp",
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
    await expect(page.getByRole("img", { name: "Ảnh minh họa Nước tương" }).first()).toBeVisible()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
    await page.reload()
    await expect(heading).toBeVisible()
    await expect(saved.getByRole("button").first()).toContainText(name)
    expect(state.creates).toBe(0)
  })
}

test("shopping list explains when an ingredient has no illustration", async ({ page }) => {
  const state = await setup(page)
  state.list.items[0].imageUrl = null
  await page.goto("/di-cho?listId=42")
  await expect(page.getByRole("img", { name: "Chưa có ảnh minh họa cho Nước tương" })).toBeVisible()
})

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
    emptyCreates: 0,
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

  await page.route("**/api/v1/recipes/ingredient-options**", (route) =>
    route.fulfill({ json: [] }),
  )

  await page.route("**/api/v1/shopping-lists**", async (route) => {
    const request = route.request(),
      url = new URL(request.url()),
      suffix = url.pathname.replace("/api/v1/shopping-lists", ""),
      method = request.method()

    if (method !== "GET") {
      state.writes.push({
        method,
        suffix,
        body: request.postData() ? request.postDataJSON() : null,
      })

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

    if (suffix === "" && method === "POST") {
      state.emptyCreates++
      state.list = {
        ...state.list,
        id: 43,
        name: request.postDataJSON().name,
        items: [],
      }
      state.empty = false
      return route.fulfill({ status: 201, json: state.list })
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
          content: state.empty ? [] : [{ ...summary, id: state.list.id, name: state.list.name }],
          page: Number(url.searchParams.get("page") ?? 0),
          size: 20,
          totalElements: state.empty ? 0 : 21,
          totalPages: state.pages,
        },
      })

    if ((suffix === "/42" || suffix === "/43") && method === "GET")
      return route.fulfill({ json: state.list })

    if (suffix === "/42" && method === "PATCH") {
      state.list.name = request.postDataJSON().name
      return route.fulfill({
        json: { ...summary, name: state.list.name, updatedAt: "2026-10-22T10:00:00" },
      })
    }

    if (suffix === "/42" && method === "DELETE") {
      state.empty = true
      return route.fulfill({ status: 204 })
    }

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
      "Vui lòng đăng nhập để tạo và lưu danh sách mua sắm theo tài khoản.",
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
    .getByRole("button", { name: "Tạo danh sách mua sắm", exact: true })
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
    name: "Tạo danh sách mua sắm",
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

for (const width of [375, 1440]) {
  test(`all shopping items move individually below the bought divider at ${width}px`, async ({ page }) => {
    const state = await setup(page)
    state.list.items = [row(1), row(2, { name: "Muối" }), row(3, { name: "Gạo", ingredientGroup: "Ngũ cốc" })]
    await page.setViewportSize({ width, height: 900 })
    await page.goto("/di-cho?listId=42")
    await page.getByRole("button", { name: /Tất cả \(/ }).click()
    const bought = page.getByRole("region", { name: "Đồ đã mua", exact: true })
    await expect(bought).toHaveCount(0)
    await page.getByRole("button", { name: "Đánh dấu Nước tương g", exact: true }).click()
    await expect(bought.getByTestId("shopping-item-1")).toBeVisible()
    await expect(bought.getByRole("heading", { name: "Đã mua (1)", exact: true })).toBeVisible()
    await expect(bought.getByTestId("shopping-item-2")).toHaveCount(0)
    await expect(page.getByRole("heading", { name: "Gia vị", exact: true })).toBeVisible()
    const boughtTop = (await bought.boundingBox())!.y
    for (const id of [2, 3]) {
      const bounds = (await page.getByTestId(`shopping-item-${id}`).boundingBox())!
      expect(bounds.y + bounds.height).toBeLessThanOrEqual(boughtTop)
    }
    await page.reload()
    await page.getByRole("button", { name: /Tất cả \(/ }).click()
    await expect(bought.getByTestId("shopping-item-1")).toBeVisible()
    await bought.getByRole("button", { name: "Đánh dấu Nước tương g", exact: true }).click()
    await expect(bought).toHaveCount(0)
    await expect(page.getByTestId("shopping-item-1")).toBeVisible()
    for (const item of state.list.items) {
      await page.getByRole("button", { name: `Đánh dấu ${item.name} g`, exact: true }).click()
      await expect(bought.getByTestId(`shopping-item-${item.id}`)).toBeVisible()
    }
    await expect(bought.getByRole("heading", { name: "Đã mua (3)", exact: true })).toBeVisible()
    await expect(page.getByText("Không có nguyên liệu trong mục này.", { exact: true })).toHaveCount(0)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  })
}

test("shopping manual item quantity, edit, bought/unbought, filtering and deletion use saved values", async ({
  page,
}) => {
  const state = await setup(page)

  await page.goto("/di-cho")

  await expect(page).toHaveURL(/listId=42/)

  await page.getByRole("button", { name: "Thêm mặt hàng", exact: true }).click()

  await expect(page.getByLabel("Tên mặt hàng")).toBeFocused()
  await page.getByLabel("Tên mặt hàng").fill("Khăn giấy lau bếp")

  await page.getByLabel("Số lượng", { exact: true }).fill("2")

  await page.getByLabel("Đơn vị", { exact: true }).selectOption("3")

  await page.getByRole("button", { name: "Tiếp tục", exact: true }).click()
  await page.getByRole("button", { name: "Xác nhận thêm", exact: true }).click()

  const item = page.getByTestId("shopping-item-50")

  await expect(item).toContainText("2 cuộn")

  await page
    .getByRole("button", { name: "Sửa Khăn giấy lau bếp cuộn", exact: true })
    .click()

  await expect(page.getByLabel("Tổng số lượng mới", { exact: true })).toBeFocused()
  await page.getByLabel("Tổng số lượng mới", { exact: true }).fill("3.5")

  await page.getByRole("button", { name: "Tiếp tục", exact: true }).click()
  await page.getByRole("button", { name: "Xác nhận thay đổi", exact: true }).click()

  await expect(item).toContainText("3,5 cuộn")

  const check = page.getByRole("button", {
    name: "Đánh dấu Khăn giấy lau bếp cuộn",
    exact: true,
  })

  await check.click()
  await page.getByRole("button", { name: /Đã mua \(/ }).click()
  const boughtCheck = page.getByRole("button", {
    name: "Đánh dấu Khăn giấy lau bếp cuộn",
    exact: true,
  })
  await expect(boughtCheck).toHaveAttribute("aria-pressed", "true")

  await page.getByRole("button", { name: /Cần mua \(/ }).click()
  await expect(item).toHaveCount(0)

  await page.getByRole("button", { name: /Đã mua \(/ }).click()
  await expect(item).toBeVisible()

  await boughtCheck.click()
  await page.getByRole("button", { name: /Tất cả \(/ }).click()

  await page
    .getByRole("button", { name: "Xoá Khăn giấy lau bếp cuộn", exact: true })
    .click()

  await expect(page.getByRole("button", { name: "Hủy", exact: true })).toBeFocused()
  await page.getByRole("button", { name: "Hủy", exact: true }).click()
  await expect(item).toBeVisible()

  await page
    .getByRole("button", { name: "Xoá Khăn giấy lau bếp cuộn", exact: true })
    .click()

  await page.getByRole("button", { name: "Xoá", exact: true }).click()
  await expect(item).toHaveCount(0)

  expect(state.list.items).toHaveLength(2)
})

test("shopping catalog item unit selection converts the displayed total", async ({ page }) => {
  const state = await setup(page)
  await page.route("**/api/v1/ingredients/5/units", (route) =>
    route.fulfill({
      json: [
        { id: 1, code: "g", name: "gram", dimension: "MASS", gramsPerUnit: 1, approximate: false },
        { id: 2, code: "ml", name: "mililít", dimension: "VOLUME", gramsPerUnit: 2, approximate: true },
      ],
    }),
  )

  await page.goto("/di-cho?listId=42")
  await page.getByRole("button", { name: "Sửa Nước tương g", exact: true }).click()
  const quantity = page.getByLabel("Tổng số lượng mới", { exact: true })
  const unit = page.getByLabel("Đơn vị", { exact: true })
  await expect(unit).toHaveValue("1")
  await unit.selectOption("2")
  await expect(quantity).toHaveValue("150")

  await page.getByRole("button", { name: "Tiếp tục", exact: true }).click()
  await page.getByRole("button", { name: "Xác nhận thay đổi", exact: true }).click()

  expect(state.writes).toContainEqual({
    method: "PUT",
    suffix: "/42/items/1",
    body: { quantity: 150, unitId: 2 },
  })
  await expect(page.getByTestId("shopping-item-1")).toContainText("150 ml")
})

test("shopping edit normalizes a supported cup quantity to milliliters", async ({ page }) => {
  const state = await setup(page)
  state.list.items[0] = row(1, { quantity: 2, unitId: 3, unitCode: "cup", unitName: "cup" })
  await page.route("**/api/v1/ingredients/5/units", (route) =>
    route.fulfill({
      json: [
        { id: 1, code: "g", name: "gram", dimension: "MASS", gramsPerUnit: 1, approximate: false },
        { id: 2, code: "ml", name: "mililít", dimension: "VOLUME", gramsPerUnit: 1, approximate: false },
        { id: 3, code: "cup", name: "cup", dimension: "VOLUME", gramsPerUnit: 240, approximate: true },
      ],
    }),
  )

  await page.goto("/di-cho?listId=42")
  await page.getByRole("button", { name: "Sửa Nước tương cup", exact: true }).click()
  await expect(page.getByLabel("Đơn vị", { exact: true })).toHaveValue("2")
  await expect(page.getByLabel("Tổng số lượng mới", { exact: true })).toHaveValue("480")
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

  await page.getByLabel("Tổng số lượng mới", { exact: true }).fill("400")

  await page.getByRole("button", { name: "Tiếp tục", exact: true }).click()
  await page.getByRole("button", { name: "Xác nhận thay đổi", exact: true }).click()

  await expect(page.getByRole("dialog").getByRole("alert")).toContainText(
    state.fail,
  )

  await expect(page.getByRole("dialog")).toContainText("thêm 100 g")
  await expect(page.getByRole("dialog")).toContainText("Nước tương")

  state.fail = ""
  await page.getByRole("button", { name: "Xác nhận thay đổi", exact: true }).click()

  await expect(page.getByTestId("shopping-item-1")).toContainText("400 g")

  await check.click()

  await page.locator('summary[aria-label="Thao tác khác"]').click()
  await page
    .getByRole("button", { name: "Xóa 1 mục đã mua", exact: true })
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
    page.getByText("Chưa có danh sách mua sắm", { exact: true }),
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

test("shopping creates an empty list from the empty state and sidebar with validation and retry", async ({ page }) => {
  const state = await setup(page)
  state.empty = true
  await page.goto("/di-cho")
  await page.getByRole("button", { name: "Tạo danh sách trống", exact: true }).last().click()

  const dialog = page.getByRole("dialog", { name: "Tạo danh sách mua sắm" })
  await dialog.getByLabel("Tên danh sách").fill("   ")
  await dialog.getByRole("button", { name: "Tạo danh sách", exact: true }).click()
  await expect(dialog.getByRole("alert")).toContainText("Hãy nhập tên danh sách")

  state.fail = "Tên danh sách đã tồn tại."
  await dialog.getByLabel("Tên danh sách").fill("Mua cuối tuần")
  await dialog.getByRole("button", { name: "Tạo danh sách", exact: true }).click()
  await expect(dialog.getByRole("alert")).toContainText(state.fail)

  state.fail = ""
  await dialog.getByRole("button", { name: "Tạo danh sách", exact: true }).click()
  await expect(page).toHaveURL(/di-cho\?listId=43/)
  await expect(page.getByRole("heading", { name: "Mua cuối tuần", exact: true })).toBeVisible()
  expect(state.emptyCreates).toBe(1)
  expect(state.writes).toContainEqual({
    method: "POST",
    suffix: "",
    body: { name: "Mua cuối tuần" },
  })

  await page.getByRole("button", { name: "Tạo danh sách trống", exact: true }).click()
  await expect(page.getByRole("dialog", { name: "Tạo danh sách mua sắm" })).toBeVisible()
  await page.getByRole("button", { name: "Hủy", exact: true }).click()
})

test("shopping pagination and small viewport preserve usable checklist", async ({
  page,
}, testInfo) => {
  const state = await setup(page)
  state.pages = 2

  await page.setViewportSize({ width: 375, height: 812 })

  await page.goto("/di-cho?listId=42")

  await expect(page.getByTestId("shopping-item-1")).toBeVisible()

  await page.getByRole("button", { name: "Sau", exact: true }).click()

  await expect(page.getByText("2/2", { exact: true })).toBeVisible()

  await page.getByRole("button", { name: "Trước", exact: true }).click()

  await expect(page.getByText("1/2", { exact: true })).toBeVisible()

  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBe(true)

  await page.screenshot({
    path: testInfo.outputPath("detail-shopping-mobile.png"),
    fullPage: true,
  })

  const checkBounds = await page.getByRole("button", { name: "Đánh dấu Nước tương g", exact: true }).boundingBox()
  const deleteBounds = await page.getByRole("button", { name: "Xoá Nước tương g", exact: true }).boundingBox()
  expect(checkBounds).not.toBeNull()
  expect(deleteBounds).not.toBeNull()
  const checkCenter = checkBounds!.y + checkBounds!.height / 2
  const deleteCenter = deleteBounds!.y + deleteBounds!.height / 2
  expect(Math.abs(checkCenter - deleteCenter)).toBeLessThan(10)
  await page.setViewportSize({ width: 768, height: 1024 })
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
  await expect(page.getByTestId("shopping-item-1")).toBeVisible()

  await page.setViewportSize({ width: 1440, height: 1000 })

  await page.screenshot({
    path: testInfo.outputPath("detail-shopping-desktop.png"),
    fullPage: true,
  })
})

test("shopping list can be renamed and keeps its name after reload", async ({ page }) => {
  const state = await setup(page)
  await page.goto("/di-cho?listId=42")

  await page.locator("nav[aria-label='Danh sách đã lưu']").getByRole("button", { name: "Thao tác với Đi chợ tuần 12/10" }).click()
  await page.getByRole("button", { name: "Đổi tên", exact: true }).click()
  const dialog = page.getByRole("dialog", { name: "Đổi tên danh sách" })
  await dialog.getByLabel("Tên danh sách").fill("  Đi chợ cuối tuần  ")
  await dialog.getByRole("button", { name: "Lưu tên" }).click()

  await expect(page.getByRole("heading", { name: "Đi chợ cuối tuần", exact: true })).toBeVisible()
  await expect(page.getByRole("navigation", { name: "Danh sách đã lưu" })).toContainText("Đi chợ cuối tuần")
  expect(state.writes).toContainEqual({
    method: "PATCH",
    suffix: "/42",
    body: { name: "Đi chợ cuối tuần" },
  })

  await page.reload()
  await expect(page.getByRole("heading", { name: "Đi chợ cuối tuần", exact: true })).toBeVisible()
})

test("shopping list deletion confirms scope and keeps the empty state usable after failure", async ({ page }) => {
  const state = await setup(page)
  await page.goto("/di-cho?listId=42")

  await page.locator("nav[aria-label='Danh sách đã lưu']").getByRole("button", { name: "Thao tác với Đi chợ tuần 12/10" }).click()
  await page.getByRole("button", { name: "Xóa danh sách", exact: true }).click()
  const dialog = page.getByRole("dialog", { name: "Xóa danh sách mua sắm?" })
  await expect(dialog).toContainText("Lịch ăn và công thức nguồn vẫn được giữ nguyên")
  state.fail = "Chưa xóa được danh sách."
  await dialog.getByRole("button", { name: "Xóa danh sách", exact: true }).click()
  await expect(dialog.getByRole("alert")).toContainText(state.fail)
  await expect(page.getByTestId("shopping-item-1")).toBeVisible()

  state.fail = ""
  await dialog.getByRole("button", { name: "Xóa danh sách", exact: true }).click()
  await expect(page).toHaveURL(/\/di-cho$/)
  await expect(page.getByText("Chưa có danh sách mua sắm", { exact: true })).toBeVisible()
  expect(state.writes.filter((write) => (write as { method: string }).method === "DELETE")).toHaveLength(2)
})

test("ingredient details group recipe quantities and distinguish unknown from manually added amounts", async ({ page }) => {
  const state = await setup(page)
  state.list.recipeSources = [{
    sourceId: "recipe-9", recipeId: 9, occurrenceCount: 2, title: "Canh nấm", available: false,
    coverUrl: null, servings: 5, mealDate: null, mealType: null,
  }]
  state.list.items[0] = row(1, {
    quantity: 140,
    sources: [
      { sourceId: "recipe-9-a", sourceKind: "RECIPE", recipeId: 9, ingredientId: 5, title: "Canh nấm", available: false, originalQuantity: 60, originalUnitId: 1, originalUnitCode: "g", servings: 2 },
      { sourceId: "recipe-9-b", sourceKind: "RECIPE", recipeId: 9, ingredientId: 5, title: "Canh nấm", available: false, originalQuantity: 30, originalUnitId: 1, originalUnitCode: "g", servings: 3 },
      { sourceId: "manual-1", sourceKind: "MANUAL", recipeId: null, ingredientId: 5, title: null, available: true, originalQuantity: 30, originalUnitId: 1, originalUnitCode: "g", servings: null },
      { sourceId: "unknown-1", sourceKind: "UNKNOWN", recipeId: null, ingredientId: 5, title: "Canh nấm", available: false, originalQuantity: 20, originalUnitId: 1, originalUnitCode: "g", servings: null },
    ],
  })
  await page.goto("/di-cho?listId=42")
  await page.getByRole("button", { name: "Xem món đã dùng Nước tương" }).first().click()
  const dialog = page.getByRole("dialog", { name: "Chi tiết Nước tương" })
  await expect(dialog).toContainText("Tổng trong danh sách")
  await expect(dialog).toContainText("140 g")
  await expect(dialog).toContainText("5 khẩu phần")
  await expect(dialog).toContainText("90 g lúc thêm")
  await expect(dialog).toContainText("Tổng lượng mất xác định")
  await expect(dialog).toContainText("20 g")
  await expect(dialog).toContainText("30 g · Thêm thủ công")
  await expect(dialog.getByRole("link", { name: "Canh nấm" })).toHaveCount(0)
})

test("quick recipe serving change previews ingredient deltas before saving to this list", async ({ page }) => {
  const state = await setup(page)
  const source = {
    sourceId: "recipe-9", recipeId: 9, occurrenceCount: 1, title: "Canh nấm", available: true,
    coverUrl: null, servings: 2, mealDate: null, mealType: null,
  }
  state.list.recipeSources = [source]
  state.list.items[0] = row(1, {
    quantity: 90,
    sources: [
      { sourceId: "recipe-9-a", sourceKind: "RECIPE", recipeId: 9, ingredientId: 5, title: "Canh nấm", available: true, originalQuantity: 60, originalUnitId: 1, originalUnitCode: "g", servings: 2 },
      { sourceId: "manual-1", sourceKind: "MANUAL", recipeId: null, ingredientId: 5, title: null, available: true, originalQuantity: 30, originalUnitId: 1, originalUnitCode: "g", servings: null },
    ],
  })
  await page.route("**/api/v1/shopping-lists/42/recipes/9/servings", async (route) => {
    const body = route.request().postDataJSON()
    state.writes.push({ method: "PUT", suffix: "/42/recipes/9/servings", body })
    source.servings = body.servings
    return route.fulfill({ json: state.list })
  })
  await page.goto("/di-cho?listId=42")
  await page.getByRole("button", { name: "Thao tác với Canh nấm" }).click()
  await page.getByRole("button", { name: "Chỉnh khẩu phần" }).click()
  const dialog = page.getByRole("dialog", { name: "Chỉnh nhanh khẩu phần" })
  const servings = dialog.getByLabel("Tổng khẩu phần mới")
  await servings.fill("1")
  await dialog.getByRole("button", { name: "Xem thay đổi" }).click()
  await expect(dialog).toContainText("Giảm 30 g")
  await expect(dialog).toContainText("Khẩu phần: 2 → 1")
  await expect(dialog).toContainText("phần người dùng thêm và phần mất xác định được giữ nguyên")
  await expect.poll(() => state.writes.length).toBe(0)
  await dialog.getByRole("button", { name: "Xác nhận thay đổi" }).click()
  await expect(dialog).toHaveCount(0)
  await expect(page.getByRole("status")).toContainText("Đã cập nhật khẩu phần và nguyên liệu trong danh sách.")
  expect(state.writes).toContainEqual({ method: "PUT", suffix: "/42/recipes/9/servings", body: { servings: 1 } })
})

test("quick recipe serving increase previews named ingredient additions before saving", async ({ page }) => {
  const state = await setup(page)
  const source = {
    sourceId: "recipe-9", recipeId: 9, occurrenceCount: 1, title: "Canh nấm", available: true,
    coverUrl: null, servings: 2, mealDate: null, mealType: null,
  }
  state.list.recipeSources = [source]
  state.list.items[0] = row(1, {
    quantity: 60,
    sources: [{ sourceId: "recipe-9-a", sourceKind: "RECIPE", recipeId: 9, ingredientId: 5, title: "Canh nấm", available: true, originalQuantity: 60, originalUnitId: 1, originalUnitCode: "g", servings: 2 }],
  })
  await page.route("**/api/v1/shopping-lists/42/recipes/9/servings", async (route) => {
    const body = route.request().postDataJSON()
    state.writes.push({ method: "PUT", suffix: "/42/recipes/9/servings", body })
    source.servings = body.servings
    return route.fulfill({ json: state.list })
  })
  await page.goto("/di-cho?listId=42")
  await page.getByRole("button", { name: "Thao tác với Canh nấm" }).click()
  await page.getByRole("button", { name: "Chỉnh khẩu phần" }).click()
  const dialog = page.getByRole("dialog", { name: "Chỉnh nhanh khẩu phần" })
  await dialog.getByLabel("Tổng khẩu phần mới").fill("3")
  await dialog.getByRole("button", { name: "Xem thay đổi" }).click()
  await expect(dialog).toContainText("Tăng 30 g")
  await expect(dialog).toContainText("Nước tương")
  await expect(dialog.getByRole("button", { name: "Xác nhận thay đổi" })).toBeEnabled()
  await dialog.getByRole("button", { name: "Xác nhận thay đổi" }).click()
  await expect(page.getByRole("status")).toContainText("Đã cập nhật khẩu phần")
  expect(state.writes).toContainEqual({ method: "PUT", suffix: "/42/recipes/9/servings", body: { servings: 3 } })
})

test("removing a recipe previews its full contribution and preserves other sources", async ({ page }) => {
  const state = await setup(page)
  const source = {
    sourceId: "recipe-9", recipeId: 9, occurrenceCount: 1, title: "Canh nấm", available: true,
    coverUrl: null, servings: 2, mealDate: null, mealType: null,
  }
  state.list.recipeSources = [source]
  state.list.items = [
    row(1, {
      quantity: 25,
      sources: [{ sourceId: "recipe-only", sourceKind: "RECIPE", recipeId: 9, ingredientId: 5, title: "Canh nấm", available: true, originalQuantity: 25, originalUnitId: 1, originalUnitCode: "g", servings: 2 }],
    }),
    row(3, {
      quantity: 80,
      sources: [
        { sourceId: "recipe-mixed", sourceKind: "RECIPE", recipeId: 9, ingredientId: 5, title: "Canh nấm", available: true, originalQuantity: 10, originalUnitId: 1, originalUnitCode: "g", servings: 2 },
        { sourceId: "unknown-mixed", sourceKind: "UNKNOWN", recipeId: null, ingredientId: 5, title: null, available: false, originalQuantity: 20, originalUnitId: 1, originalUnitCode: "g", servings: null },
        { sourceId: "manual-mixed", sourceKind: "MANUAL", recipeId: null, ingredientId: 5, title: null, available: true, originalQuantity: 50, originalUnitId: 1, originalUnitCode: "g", servings: null },
      ],
    }),
  ]
  await page.route("**/api/v1/shopping-lists/42/recipes/9", async (route) => {
    state.writes.push({ method: route.request().method(), suffix: "/42/recipes/9" })
    state.list.items = state.list.items.filter((item) => item.id !== 1)
    state.list.items[0].quantity = 70
    state.list.items[0].sources = state.list.items[0].sources?.filter((itemSource) => itemSource.sourceKind !== "RECIPE")
    state.list.recipeSources = []
    return route.fulfill({ json: state.list })
  })
  await page.goto("/di-cho?listId=42")
  await page.getByRole("button", { name: "Thao tác với Canh nấm" }).click()
  await page.getByRole("button", { name: "Gỡ khỏi danh sách" }).click()
  const dialog = page.getByRole("dialog", { name: "Gỡ món khỏi danh sách mua sắm" })
  await expect(dialog).toContainText("Nguyên liệu bị ảnh hưởng · 2 mặt hàng")
  await expect(dialog).toContainText("25 g · sẽ xóa mặt hàng")
  await expect(dialog).toContainText("10 g · sẽ giảm")
  await expect(dialog.getByRole("button", { name: "Xác nhận gỡ món" })).toBeEnabled()
  await dialog.getByRole("button", { name: "Xác nhận gỡ món" }).click()
  await expect(page.getByRole("status")).toContainText("Đã gỡ món và cập nhật nguyên liệu")
  await expect(page.getByTestId("shopping-item-1")).toHaveCount(0)
  await expect(page.getByTestId("shopping-item-3")).toContainText("70 g")
  expect(state.writes).toContainEqual({ method: "DELETE", suffix: "/42/recipes/9" })
})

test("unchanged recipe servings explain that no ingredient quantities will change", async ({ page }) => {
  const state = await setup(page)
  state.list.recipeSources = [{
    sourceId: "recipe-9", recipeId: 9, occurrenceCount: 1, title: "Canh nấm", available: true,
    coverUrl: null, servings: 2, mealDate: null, mealType: null,
  }]
  await page.goto("/di-cho?listId=42")
  await page.getByRole("button", { name: "Thao tác với Canh nấm" }).click()
  await page.getByRole("button", { name: "Chỉnh khẩu phần" }).click()
  const dialog = page.getByRole("dialog", { name: "Chỉnh nhanh khẩu phần" })
  await dialog.getByRole("button", { name: "Xem thay đổi" }).click()
  await expect(dialog).toContainText("Không có định lượng nguyên liệu nào thay đổi.")
  await expect(dialog.getByRole("button", { name: "Xác nhận thay đổi" })).toBeDisabled()
  expect(state.writes).toEqual([])
})

test("manual source edit confirms a named increase and saves only after confirmation", async ({ page }) => {
  const state = await setup(page)
  const manualSource = {
    sourceId: "manual-1", sourceKind: "MANUAL" as const, recipeId: null, ingredientId: 5,
    title: null, available: true, originalQuantity: 30, originalUnitId: 1,
    originalUnitCode: "g", servings: null,
  }
  const updated = row(1, { quantity: 35, sources: [{ ...manualSource, originalQuantity: 35 }] })
  state.list.items[0] = row(1, { quantity: 30, sources: [manualSource] })
  await page.route("**/api/v1/shopping-lists/42/manual-sources/manual-1", async (route) => {
    state.writes.push({ method: route.request().method(), suffix: "/42/manual-sources/manual-1", body: route.request().postDataJSON() })
    return route.fulfill({ json: updated })
  })
  await page.goto("/di-cho?listId=42")
  await page.getByRole("button", { name: "Xem món đã dùng Nước tương" }).first().click()
  const detail = page.getByRole("dialog", { name: "Chi tiết Nước tương" })
  await detail.getByRole("button", { name: "Sửa", exact: true }).click()
  const editor = page.getByRole("dialog", { name: "Sửa phần đã thêm thủ công" })
  await editor.getByLabel("Số lượng g").fill("35")
  await editor.getByRole("button", { name: "Tiếp tục" }).click()
  await expect(editor).toContainText("thêm 5 g")
  await expect(editor).toContainText("Nước tương")
  await expect.poll(() => state.writes.length).toBe(0)
  await editor.getByRole("button", { name: "Xác nhận thay đổi" }).click()
  await expect(page.getByRole("status")).toContainText("Đã cập nhật phần thêm thủ công")
  expect(state.writes).toContainEqual({
    method: "PUT", suffix: "/42/manual-sources/manual-1", body: { quantity: 35, unitId: 1 },
  })
})

test("manual source edit asks before saving an unchanged quantity", async ({ page }) => {
  const state = await setup(page)
  const manualSource = {
    sourceId: "manual-1", sourceKind: "MANUAL" as const, recipeId: null, ingredientId: 5,
    title: null, available: true, originalQuantity: 30, originalUnitId: 1,
    originalUnitCode: "g", servings: null,
  }
  state.list.items[0] = row(1, { quantity: 30, sources: [manualSource] })
  await page.route("**/api/v1/shopping-lists/42/manual-sources/manual-1", async (route) => {
    state.writes.push({ method: route.request().method(), suffix: "/42/manual-sources/manual-1", body: route.request().postDataJSON() })
    return route.fulfill({ json: state.list.items[0] })
  })
  await page.goto("/di-cho?listId=42")
  await page.getByRole("button", { name: "Xem món đã dùng Nước tương" }).first().click()
  const detail = page.getByRole("dialog", { name: "Chi tiết Nước tương" })
  await detail.getByRole("button", { name: "Sửa", exact: true }).click()
  const editor = page.getByRole("dialog", { name: "Sửa phần đã thêm thủ công" })
  await editor.getByRole("button", { name: "Tiếp tục" }).click()
  await expect(editor).toContainText("không thay đổi")
  await expect.poll(() => state.writes.length).toBe(0)
  await editor.getByRole("button", { name: "Xác nhận thay đổi" }).click()
  await expect(page.getByRole("status")).toContainText("Đã cập nhật phần thêm thủ công")
  expect(state.writes).toHaveLength(1)
})

test("manual source edit confirms a named decrease and updates only that contribution", async ({ page }) => {
  const state = await setup(page)
  const manualSource = {
    sourceId: "manual-1", sourceKind: "MANUAL" as const, recipeId: null, ingredientId: 5,
    title: null, available: true, originalQuantity: 30, originalUnitId: 1,
    originalUnitCode: "g", servings: null,
  }
  const updated = row(1, { quantity: 20, sources: [{ ...manualSource, originalQuantity: 20 }] })
  state.list.items[0] = row(1, { quantity: 30, sources: [manualSource] })
  await page.route("**/api/v1/shopping-lists/42/manual-sources/manual-1", async (route) => {
    state.writes.push({ method: route.request().method(), suffix: "/42/manual-sources/manual-1", body: route.request().postDataJSON() })
    return route.fulfill({ json: updated })
  })
  await page.goto("/di-cho?listId=42")
  await page.getByRole("button", { name: "Xem món đã dùng Nước tương" }).first().click()
  const detail = page.getByRole("dialog", { name: "Chi tiết Nước tương" })
  await detail.getByRole("button", { name: "Sửa", exact: true }).click()
  const editor = page.getByRole("dialog", { name: "Sửa phần đã thêm thủ công" })
  await editor.getByLabel("Số lượng g").fill("20")
  await editor.getByRole("button", { name: "Tiếp tục" }).click()
  await expect(editor).toContainText("giảm 10 g")
  await expect(editor).toContainText("Nước tương")
  await editor.getByRole("button", { name: "Xác nhận thay đổi" }).click()
  await expect(page.getByRole("status")).toContainText("Đã cập nhật phần thêm thủ công")
  await expect(page.getByTestId("shopping-item-1")).toContainText("20 g")
  expect(state.writes).toContainEqual({
    method: "PUT", suffix: "/42/manual-sources/manual-1", body: { quantity: 20, unitId: 1 },
  })
})

test("removing a manual source updates the checklist immediately", async ({ page }) => {
  const state = await setup(page)
  const manualSource = {
    sourceId: "manual-1", sourceKind: "MANUAL" as const, recipeId: null, ingredientId: 5,
    title: null, available: true, originalQuantity: 30, originalUnitId: 1,
    originalUnitCode: "g", servings: null,
  }
  state.list.items[0] = row(1, { quantity: 30, sources: [manualSource] })
  await page.route("**/api/v1/shopping-lists/42/manual-sources/manual-1", async (route) => {
    state.writes.push({ method: route.request().method(), suffix: "/42/manual-sources/manual-1" })
    state.list.items = state.list.items.filter((item) => item.id !== 1)
    return route.fulfill({ status: 204 })
  })
  await page.goto("/di-cho?listId=42")
  await page.getByRole("button", { name: "Xem món đã dùng Nước tương" }).first().click()
  await page.getByRole("button", { name: "Xóa phần thêm", exact: true }).click()
  await expect(page.getByRole("status")).toContainText("Đã xóa phần thêm thủ công")
  await expect(page.getByTestId("shopping-item-1")).toHaveCount(0)
  expect(state.writes).toContainEqual({ method: "DELETE", suffix: "/42/manual-sources/manual-1" })
})
