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
    previews: 0,
    previewRequests: [] as unknown[],
    aiApplies: 0,
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
  await page.route("**/api/v1/recipes/11", (route) => route.fulfill({ json: { recipeId: 11, title: "Canh nấm", instructions: "Nấu canh nấm trong 15 phút.", description: "Canh nấm", dishCategory: "SOUP", dishCategoryLabel: "Món canh", vegetarianType: "VEGAN", vegetarianTypeLabel: "Thuần chay", difficulty: "EASY", difficultyLabel: "Dễ", servings: 2, prepTimeMinutes: 10, cookTimeMinutes: 15, youtubeUrl: null, publishedAt: null, author: { userId: 17, displayName: "Tác giả", avatarUrl: null }, media: [], ingredients: [{ ingredientId: 1, name: "Nấm", quantity: 200, unitId: 1, unitCode: "g", unitName: "g", nutritionSupported: true }], statistics: { likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null, viewCount: 0 }, nutritionComplete: true, ingredientsWithoutNutrition: [], nutrition: { complete: true, ingredientsMissingData: [], perServing: { ENERGY_KCAL: 100, PROTEIN_G: 2, CARBOHYDRATE_G: 10, TOTAL_FAT_G: 1, FIBER_G: 3, CALCIUM_MG: 20, IRON_MG: 0.5, VITAMIN_B12_MCG: 0, ZINC_MG: 1 } } } }))
  await page.route("**/api/v1/recipes/11/related", (route) => route.fulfill({ json: { items: [] } }))
  await page.route("**/api/v1/recipes/11/comments**", (route) => route.fulfill({ json: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, commentCount: 0 } }))
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
  await page.route("**/api/v1/saved-recipes**", (route) => {
    if (state.searchFail) return route.fulfill({ status: 503, contentType: "application/problem+json", body: JSON.stringify({ status: 503, code: "SERVICE_UNAVAILABLE", detail: "Chưa tải được công thức." }) })
    const keyword = new URL(route.request().url()).searchParams.get("keyword") ?? ""
    const content = recipes.filter((item) => item.title.toLowerCase().includes(keyword.toLowerCase())).map((item) => ({ recipeId: item.id, title: item.title as string | null, coverUrl: null, available: true }))
    if (!keyword) content.push({ recipeId: 99, title: null, coverUrl: null, available: false })
    return route.fulfill({ json: { content, page: 0, size: 12, totalElements: content.length, totalPages: 1 } })
  })
  await page.route("**/api/v1/recipes/ingredient-options**", (route) =>
    route.fulfill({ json: [{ ingredientId: 1, name: "Nấm" }, { ingredientId: 2, name: "Đậu phụ" }] }),
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
    if (url.pathname.endsWith("/ai-menu/previews")) {
      state.previews++
      state.previewRequests.push(request.postDataJSON())
      const meals = Array.from({ length: 21 }, (_, index) => ({
        mealDate: (() => { const date = new Date(`${week}T12:00:00`); date.setDate(date.getDate() + Math.floor(index / 3)); return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}` })(),
        mealType: (["BREAKFAST", "LUNCH", "DINNER"] as const)[index % 3],
        recipeId: index % 2 ? 12 : 11,
        recipeTitle: index % 2 ? "Đậu phụ kho" : "Canh nấm",
        coverUrl: null,
        nutritionPerServing: { ENERGY_KCAL: 100, PROTEIN_G: 5 },
        reason: "Có dữ liệu dinh dưỡng.",
      }))
      return route.fulfill({ json: { weekStartDate: week, mode: "PROFILE", goal: null, meals, nutritionDisclaimer: "Dinh dưỡng tham khảo." } })
    }
    if (url.pathname.endsWith("/ai-menu")) {
      state.aiApplies++
      const payload = request.postDataJSON()
      state.entries = payload.meals.map((meal: { mealDate: string; mealType: string; recipeId: number }, index: number) => ({
        entryId: index + 1, mealDate: meal.mealDate, mealType: meal.mealType,
        plannedServings: 1, recipeId: meal.recipeId,
        recipeTitle: meal.recipeId === 11 ? "Canh nấm" : "Đậu phụ kho",
        recipeCoverUrl: null, dishCategory: "SOUP", totalTimeMinutes: 25,
        recipeDeleted: false, unavailableMessage: null,
      })) as unknown as typeof state.entries
      return route.fulfill({ json: { weekStartDate: week, entries: state.entries } })
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
    if (request.method() === "PATCH") {
      const id = Number(url.pathname.split("/").at(-2))
      const current = state.entries.find((item) => item.entryId === id)!
      const updated = { ...current, plannedServings: payload.plannedServings }
      state.entries = state.entries.map((item) => item.entryId === id ? updated : item)
      return route.fulfill({ json: updated })
    }
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

async function openMonday(page: Page) {
  const button = page.getByRole("button", { name: `Xem ngày ${new Date(`${week}T12:00:00`).toLocaleDateString("vi-VN")}` })
  await expect(button).toBeVisible()
  if (await button.getAttribute("aria-expanded") === "false") await button.click()
}

const lunch = (page: Page) =>
  page.getByRole("button", { name: /^Thêm món Bữa trưa/ }).first()

test("only one day expands, and adding meals queries saved recipes with pagination", async ({
  page,
}) => {
  await setup(page)

  let publicSearches = 0

  page.on("request", (request) => {
    if (/\/api\/v1\/recipes\?/.test(request.url())) publicSearches++
  })

  await page.route("**/api/v1/saved-recipes**", (route) => {
    const url = new URL(route.request().url())

    const pageNumber = Number(url.searchParams.get("page"))

    return route.fulfill({
      json: {
        content: [
          {
            recipeId: pageNumber ? 12 : 11,
            title: pageNumber ? "Đậu phụ kho" : "Canh nấm",
            coverUrl: null,
            available: true,
          },
        ],
        page: pageNumber,
        size: 12,
        totalElements: 13,
        totalPages: 2,
      },
    })
  })

  await page.goto("/ke-hoach")

  await expect(page.getByRole("button", { name: /^Xem ngày/ })).toHaveCount(7)

  await expect(
    page.locator('button[aria-expanded="true"][aria-controls^="meals-"]'),
  ).toHaveCount(1)

  await expect(page.getByRole("button", { name: /^Thêm món Bữa/ })).toHaveCount(
    3,
  )

  await openMonday(page)

  await lunch(page).click()

  const dialog = page.getByRole("dialog")

  await expect(
    dialog.getByText("Công thức đã lưu", { exact: true }),
  ).toBeVisible()

  await expect(dialog.getByRole("button", { name: "Canh nấm" })).toBeVisible()

  await dialog.getByRole("button", { name: "Trang sau" }).click()

  await expect(
    dialog.getByRole("button", { name: "Đậu phụ kho" }),
  ).toBeVisible()

  expect(publicSearches).toBe(0)
})

test("weekly AI preview leaves the plan unchanged until the member confirms whole-week replacement", async ({ page }) => {
  const existing = entry(7, 11)
  const state = await setup(page, [existing])
  await page.goto("/ke-hoach?view=for-you")
  await page.getByRole("tab", { name: "Theo nhu cầu dinh dưỡng" }).click()
  await expect(page.getByRole("heading", { name: "Dành cho bạn" })).toBeVisible()
  await expect(page.getByRole("button", { name: "Dựa trên hồ sơ" })).toBeVisible()
  expect(state.entries).toEqual([existing])

  await page.getByRole("button", { name: "Tạo thực đơn tuần" }).click()
  await expect(page.getByText("Bản xem trước · 21 món được chọn")).toBeVisible()
  await expect(page.getByText("Khi xác nhận, toàn bộ món đã có trong lịch của tuần này sẽ bị thay bằng các món đang được chọn.")).toBeVisible()
  expect(state.previews).toBe(1)
  expect(state.aiApplies).toBe(0)
  expect(state.entries).toEqual([existing])

  await page.getByRole("button", { name: "Áp dụng vào Lịch ăn" }).click()
  await expect(page.getByRole("button", { name: "Xác nhận thay toàn bộ lịch" })).toBeVisible()
  expect(state.aiApplies).toBe(0)
  await page.getByRole("button", { name: "Xác nhận thay toàn bộ lịch" }).click()
  await expect.poll(() => state.aiApplies).toBe(1)
  expect(state.entries).toHaveLength(21)
})

test("weekly AI request can use one-time goals and ingredient preferences without persisting them", async ({ page }) => {
  const state = await setup(page)
  await page.goto("/ke-hoach?view=for-you")
  await page.getByRole("tab", { name: "Theo nhu cầu dinh dưỡng" }).click()
  await page.getByRole("button", { name: "Theo mong muốn tuần này" }).click()
  await page.getByLabel("Mục tiêu tuần này (không bắt buộc)").selectOption("LOSE_WEIGHT")
  await page.getByRole("button", { name: "Ưu tiên Nấm" }).click()
  await page.getByRole("button", { name: "Tránh Đậu phụ" }).click()
  await page.getByRole("button", { name: "Tạo thực đơn tuần" }).click()
  await expect(page.getByText("Bản xem trước · 21 món được chọn")).toBeVisible()

  expect(state.previewRequests).toEqual([{
    weekStartDate: week,
    mode: "THIS_WEEK",
    goal: "LOSE_WEIGHT",
    preferredIngredientIds: [1],
    avoidedIngredientIds: [2],
  }])
  await expect(page.getByLabel("Mục tiêu tuần này (không bắt buộc)")).toHaveValue("")
  await expect(page.getByRole("button", { name: "Ưu tiên Nấm" })).toHaveAttribute("aria-pressed", "false")
  await expect(page.getByRole("button", { name: "Tránh Đậu phụ" })).toHaveAttribute("aria-pressed", "false")
  expect(state.aiApplies).toBe(0)
  expect(state.entries).toEqual([])
})

test("nutrition counts one serving per person despite family portions and switches day/week", async ({
  page,
}) => {
  const tomorrow = new Date(`${week}T12:00:00`)

  tomorrow.setDate(tomorrow.getDate() + 1)

  const next = `${tomorrow.getFullYear()}-${String(tomorrow.getMonth() + 1).padStart(2, "0")}-${String(tomorrow.getDate()).padStart(2, "0")}`

  await setup(page, [
    { ...entry(1), plannedServings: 2.5 },

    { ...entry(2), mealType: "DINNER", plannedServings: 1 },

    { ...entry(3), mealDate: next, plannedServings: 4 },
  ])

  let requests = 0

  await page.route("**/api/v1/recipes/11", (route) => {
    requests++

    return route.fulfill({
      json: {
        servings: 4,
        nutrition: {
          total: { ENERGY_KCAL: 400, PROTEIN_G: 8 },
          complete: true,
          ingredientsMissingData: [],
          perServing: {
            ENERGY_KCAL: 100,
            PROTEIN_G: 2,
            CARBOHYDRATE_G: 10,
            TOTAL_FAT_G: 1,
            FIBER_G: 3,
            CALCIUM_MG: 20,
            IRON_MG: 0.5,
            VITAMIN_B12_MCG: 0,
            ZINC_MG: 1,
          },
        },
      },
    })
  })

  await page.goto("/ke-hoach")

  await openMonday(page)

  expect(requests).toBe(0)

  await page.getByRole("button", { name: "Xem dinh dưỡng ngày" }).click()

  const dialog = page.getByRole("dialog")

  await expect(dialog.getByText("200 kcal", { exact: true })).toBeVisible()
  await expect(dialog.getByText("Dinh dưỡng cho 1 người · mỗi món 1 phần ăn", { exact: true })).toBeVisible()

  await expect(dialog.getByText("4 g", { exact: true })).toBeVisible()

  expect(requests).toBe(1)

  await dialog.getByRole("button", { name: "Cả tuần", exact: true }).click()

  await expect(dialog.getByText("300 kcal", { exact: true })).toBeVisible()
  await expect(dialog.getByText(/không phải trung bình mỗi ngày/)).toBeVisible()

  expect(requests).toBe(1)

  await expect(dialog.locator("dl").first().locator("dt")).toHaveCount(9)

  await dialog.getByRole("button", { name: "Theo ngày" }).click()

  await dialog.getByLabel("Ngày xem").selectOption(next)

  await expect(dialog.getByText("100 kcal", { exact: true })).toBeVisible()
})

test("nutrition discloses missing data and supports retry without false zero totals", async ({
  page,
}) => {
  await setup(page, [entry()])

  let fail = true

  await page.route("**/api/v1/recipes/11", (route) =>
    fail
      ? route.fulfill({ status: 503, json: { detail: "Chưa tải được" } })
      : route.fulfill({
          json: {
            nutrition: {
              complete: false,
              ingredientsMissingData: ["Nấm"],
              perServing: { ENERGY_KCAL: 0 },
            },
          },
        }),
  )

  await page.goto("/ke-hoach")

  await openMonday(page)

  await page.getByRole("button", { name: "Xem dinh dưỡng ngày" }).click()

  const dialog = page.getByRole("dialog")

  await expect(dialog.getByRole("alert")).toContainText("Chưa tải được")

  fail = false

  await dialog.getByRole("button", { name: "Thử lại" }).click()

  await expect(dialog.getByText("Thiếu dữ liệu: Nấm.")).toBeVisible()

  await expect(dialog.getByText("0 kcal", { exact: true })).toHaveCount(0)

  await expect(
    dialog.locator("dd").filter({ hasText: "Chưa đủ dữ liệu" }),
  ).toHaveCount(9)

  await page.keyboard.press("Escape")

  await expect(
    page.getByRole("button", { name: "Xem dinh dưỡng ngày" }),
  ).toBeFocused()
})

test("empty week opens one day with three meals and requests creation only after an action", async ({
  page,
}) => {
  const state = await setup(page)
  await page.goto("/ke-hoach")
  await openMonday(page)
  await expect(page.getByRole("button", { name: /^Thêm món Bữa/ })).toHaveCount(
    3,
  )
  expect(state.writes).toBe(0)
  await page.getByRole("button", { name: "Lưu lịch tuần trống" }).click()
  await expect(page.getByRole("status")).toContainText("Đã lưu lịch tuần")
  expect(state.writes).toBe(1)
})

test("recipe detail saves integer servings and the calendar displays them after return", async ({ page }) => {
  const state = await setup(page, [entry(), { ...entry(2), mealType: "DINNER", plannedServings: 3 }])
  await page.goto("/ke-hoach")
  await openMonday(page)
  await expect(page.getByRole("spinbutton")).toHaveCount(0)
  await expect(page.getByRole("button", { name: /^Lưu khẩu phần/ })).toHaveCount(0)
  await page.getByRole("link", { name: "Canh nấm", exact: true }).first().click()
  await expect(page.getByRole("button", { name: "Tăng khẩu phần" })).toBeEnabled()
  await expect(page.getByText("1 phần", { exact: true })).toBeVisible()
  await expect(page.getByText("100 g", { exact: true })).toBeVisible()
  await page.getByRole("button", { name: "Tăng khẩu phần" }).click()
  await expect(page.getByText("2 phần", { exact: true })).toBeVisible()
  await expect(page.getByText("200 g", { exact: true })).toBeVisible()
  expect(state.writes).toBe(0)
  await page.getByRole("button", { name: "Lưu khẩu phần vào lịch ăn" }).click()
  await expect(page.getByRole("status")).toContainText("Đã lưu 2 phần")
  expect(state.entries[0].plannedServings).toBe(2)
  expect(state.entries[1].plannedServings).toBe(3)
  await page.getByRole("link", { name: "Về lịch ăn", exact: true }).click()
  await expect(page.getByRole("article", { name: "Canh nấm" }).first()).toContainText("25 phút · 2 phần")
  await page.reload()
  await expect(page.getByRole("article", { name: "Canh nấm" }).first()).toContainText("25 phút · 2 phần")
  await page.getByRole("button", { name: "Xem dinh dưỡng ngày" }).click()
  await expect(page.getByRole("dialog").getByText("200 kcal", { exact: true })).toBeVisible()
})

test("failed serving save retains the old calendar value and can be retried in detail", async ({ page }) => {
  const state = await setup(page, [entry()])
  await page.goto("/ke-hoach")
  await openMonday(page)
  await page.getByRole("link", { name: "Canh nấm", exact: true }).click()
  await expect(page.getByRole("button", { name: "Tăng khẩu phần" })).toBeEnabled()
  await page.getByRole("button", { name: "Tăng khẩu phần" }).click()
  state.fail = "network"
  await page.getByRole("button", { name: "Lưu khẩu phần vào lịch ăn" }).click()
  await expect(page.getByRole("alert")).toContainText("Chưa lưu được")
  expect(state.entries[0].plannedServings).toBe(1)
  state.fail = ""
  await page.getByRole("button", { name: "Lưu khẩu phần vào lịch ăn" }).click()
  await expect(page.getByRole("status")).toContainText("Đã lưu 2 phần")
})

test("a mismatched plan entry cannot be edited and ordinary recipe viewing does not write a plan", async ({ page }) => {
  const state = await setup(page, [entry()])
  await page.goto(`/cong-thuc/id/11?mealPlanWeek=${week}&mealPlanEntryId=999`)
  await expect(page.getByRole("alert")).toContainText("không còn thuộc lịch ăn")
  await expect(page.getByRole("button", { name: "Lưu khẩu phần vào lịch ăn" })).toBeDisabled()
  expect(state.writes).toBe(0)
  await page.goto("/cong-thuc/id/11")
  await page.getByRole("button", { name: "Tăng khẩu phần" }).click()
  await expect(page.getByText("3 phần", { exact: true })).toBeVisible()
  await expect(page.getByRole("button", { name: "Lưu khẩu phần vào lịch ăn" })).toHaveCount(0)
  expect(state.writes).toBe(0)
})

test("legacy fractional servings are only changed when an integer is explicitly saved", async ({ page }) => {
  const state = await setup(page, [{ ...entry(), plannedServings: 1.1 }])
  await page.goto(`/cong-thuc/id/11?mealPlanWeek=${week}&mealPlanEntryId=1`)
  await expect(page.getByText(/Lịch trước đang lưu số phần lẻ/)).toBeVisible()
  expect(state.writes).toBe(0)
  await page.getByRole("button", { name: "Tăng khẩu phần" }).click()
  await expect(page.getByText("2 phần", { exact: true })).toBeVisible()
  await page.getByRole("button", { name: "Lưu khẩu phần vào lịch ăn" }).click()
  await expect(page.getByRole("status")).toContainText("Đã lưu 2 phần")
  expect(state.entries[0].plannedServings).toBe(2)
})

test("empty saved collection and empty nutrition explain the next step", async ({ page }) => {
  const state = await setup(page)
  await page.route("**/api/v1/saved-recipes**", (route) => route.fulfill({ json: { content: [], page: 0, size: 12, totalElements: 0, totalPages: 0 } }))
  await page.goto("/ke-hoach")
  await openMonday(page)
  await lunch(page).click()
  const dialog = page.getByRole("dialog")
  await expect(dialog.getByText("Bạn chưa lưu công thức nào.")).toBeVisible()
  await expect(dialog.getByRole("link", { name: "Khám phá và lưu công thức →" })).toHaveAttribute("href", "/kham-pha")
  await page.keyboard.press("Escape")
  await page.getByRole("button", { name: "Xem dinh dưỡng ngày" }).click()
  await expect(page.getByRole("dialog").getByText("Ngày này chưa có món để tính dinh dưỡng.")).toBeVisible()
  expect(state.writes).toBe(0)
})

test("adds two different recipes to the same meal and retains them after reload", async ({
  page,
}) => {
  const state = await setup(page)
  await page.goto("/ke-hoach")
  await openMonday(page)
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
  await openMonday(page)
  await expect(page.getByRole("article")).toHaveCount(2)
})

test("saved picker prevents unavailable choices and replaces a tombstone in place", async ({
  page,
}) => {
  const state = await setup(page, [
    { ...entry(8, 11, true), plannedServings: 2.5 },
  ])
  await page.goto("/ke-hoach")
  await openMonday(page)
  await expect(page.locator('a[href^="/cong-thuc/id/11"]')).toHaveCount(0)
  await page
    .getByRole("button", { name: "Thay món Công thức không còn khả dụng" })
    .click()
  const dialog = page.getByRole("dialog")
  await expect(dialog.getByText("Công thức đã lưu", { exact: true })).toBeVisible()
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
  await openMonday(page)
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
  await openMonday(page)
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
  await openMonday(page)
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
  await openMonday(page)
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
  await openMonday(page)
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
  await openMonday(page)
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
    await page.keyboard.press("Escape")
    await page.getByRole("button", { name: "Xem dinh dưỡng ngày" }).click()
    await expect(page.getByRole("dialog").getByText("100 kcal", { exact: true })).toBeVisible()
    await expect(page.getByRole("dialog").locator("dt")).toHaveCount(9)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await page.screenshot({ path: path.join(directory, `nutrition-${width}.png`) })
    await page.evaluate(() => localStorage.setItem("mamxanh.theme", "light"))
    await page.reload()
    await openMonday(page)
    await page.screenshot({ path: path.join(directory, `week-light-${width}.png`), fullPage: true })
    await page.getByRole("button", { name: "Xem dinh dưỡng ngày" }).click()
    await expect(page.getByRole("dialog").getByText("100 kcal", { exact: true })).toBeVisible()
    await page.screenshot({ path: path.join(directory, `nutrition-light-${width}.png`), animations: "disabled" })
    await page.keyboard.press("Escape")
    await page.getByRole("link", { name: "Canh nấm", exact: true }).click()
    await expect(page.getByRole("button", { name: "Tăng khẩu phần" })).toBeEnabled()
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
    await page.getByRole("button", { name: "Lưu khẩu phần vào lịch ăn" }).scrollIntoViewIfNeeded()
    await page.screenshot({ path: path.join(directory, `servings-detail-light-${width}.png`) })
  })
}
