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
    nutritionFail: false,
    nutritionIncomplete: false,
    aiFail: false,
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
  await page.route("**/api/v1/recipes/11/views", (route) => {
    expect(route.request().method()).toBe("POST")
    return route.fulfill({ json: { recipeId: 11, counted: false, viewCount: 0 } })
  })
  await page.route("**/api/v1/recipes/11/related", (route) => route.fulfill({ json: { items: [] } }))
  await page.route("**/api/v1/recipes/11/reaction", (route) => {
    expect(route.request().method()).toBe("GET")
    return route.fulfill({ json: { recipeId: 11, currentReaction: null, likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null } })
  })
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
      if (url.pathname.includes("/nutrition/report"))
        return route.fulfill({ status: 200, contentType: "application/pdf", body: "%PDF-1.4 fixture" })
      if (url.pathname.includes("/nutrition")) {
        if (state.nutritionFail)
          return route.fulfill({ status: 503, contentType: "application/problem+json", body: JSON.stringify({ code: "SERVICE_UNAVAILABLE", detail: "Chưa tải được bảng dinh dưỡng." }) })
        const selectedDate = url.searchParams.get("date")
        const end = new Date(`${selectedWeek}T12:00:00`)
        end.setDate(end.getDate() + 6)
        const lastDay = `${end.getFullYear()}-${String(end.getMonth() + 1).padStart(2, "0")}-${String(end.getDate()).padStart(2, "0")}`
        const days = Array.from({ length: selectedDate ? 1 : 7 }, (_, offset) => {
          const date = new Date(`${selectedDate ?? selectedWeek}T12:00:00`)
          date.setDate(date.getDate() + offset)
          const dateKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
          const dayEntries = state.entries.filter((item) => item.mealDate === dateKey && item.mealDate >= selectedWeek && item.mealDate <= lastDay)
          const keys = ["ENERGY_KCAL", "PROTEIN_G", "CARBOHYDRATE_G", "TOTAL_FAT_G", "FIBER_G", "CALCIUM_MG", "IRON_MG", "VITAMIN_B12_MCG", "ZINC_MG"]
          const base: Record<string, number> = { ENERGY_KCAL: 100, PROTEIN_G: 2, CARBOHYDRATE_G: 10, TOTAL_FAT_G: 1, FIBER_G: 3, CALCIUM_MG: 20, IRON_MG: 0.5, VITAMIN_B12_MCG: 0, ZINC_MG: 1 }
          const complete = !state.nutritionIncomplete
          const nutrients = Object.fromEntries(keys.map((key) => [key, complete ? dayEntries.reduce((sum, item) => sum + base[key] * item.plannedServings, 0) : null]))
          return { date: dateKey, nutrients, entries: dayEntries.map((item) => ({ entryId: item.entryId, date: dateKey, mealType: item.mealType, recipeId: item.recipeId, recipeTitle: item.recipeTitle ?? "Công thức không còn khả dụng", plannedServings: item.plannedServings, complete, ingredientsMissingData: complete ? [] : ["Nấm"], nutrients: Object.fromEntries(keys.map((key) => [key, base[key] * item.plannedServings])) })) }
        })
        const keys = ["ENERGY_KCAL", "PROTEIN_G", "CARBOHYDRATE_G", "TOTAL_FAT_G", "FIBER_G", "CALCIUM_MG", "IRON_MG", "VITAMIN_B12_MCG", "ZINC_MG"]
        const targets = keys.map((key, index) => ({ key, label: ["Năng lượng", "Chất đạm", "Carbohydrate", "Tổng chất béo", "Chất xơ", "Canxi", "Sắt", "Vitamin B12", "Kẽm"][index], minimum: 50, maximum: 100, unit: key === "ENERGY_KCAL" ? "kcal" : "g", referenceType: "FDA_DV", source: "FDA Daily Values", sourceUrl: "https://www.fda.gov", version: "current" }))
        const weeklyAverage = Object.fromEntries(keys.map((key) => { const values = days.map((item) => item.nutrients[key]); return [key, values.some((value) => value == null) ? null : values.reduce((sum, value) => sum + (value as number), 0) / 7] }))
        return route.fulfill({ json: { weekStartDate: selectedWeek, scope: selectedDate ? "DAY" : "WEEK", selectedDate, referenceBasis: "FDA Daily Values", source: "U.S. FDA", sourceUrl: "https://www.fda.gov", disclaimer: "Thông tin dinh dưỡng trong báo cáo chỉ mang tính tham khảo, được ước tính từ thực đơn dự kiến; không phải kết quả khám, chẩn đoán hay điều trị y tế, và không thay thế tư vấn của bác sĩ hoặc chuyên gia dinh dưỡng. Kết quả không có giá trị pháp lý hoặc y tế.", targets, days, weeklyAverage, attainmentDays: Object.fromEntries(keys.map((key) => [key, 0])), incompleteEntries: days.flatMap((day) => day.entries.filter((item) => !item.complete).map((item) => ({ entryId: item.entryId, date: item.date, mealType: item.mealType, recipeTitle: item.recipeTitle, ingredientsMissingData: item.ingredientsMissingData }))) } })
      }
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
    if (request.method() === "POST" && url.pathname.includes("/nutrition/recommendations/")) {
      if (state.aiFail) return route.fulfill({ status: 503, contentType: "application/problem+json", body: JSON.stringify({ code: "AI_PROVIDER_UNAVAILABLE", detail: "Dịch vụ AI hiện không khả dụng." }) })
      return route.fulfill({ json: { entryId: Number(url.pathname.split("/").pop()), analysis: "Thực đơn có thể điều chỉnh bằng các công thức được gợi ý.", recommendations: [{ recipeId: 12, title: "Đậu phụ kho", complete: true, ingredientsMissingData: [] }] } })
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
  await expect(page.getByRole("button", { name: /^Xem ngày/ }).first()).toContainText("Hôm nay")

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
    dialog.getByRole("button", { name: "Đã lưu", exact: true }),
  ).toBeVisible()

  await expect(dialog.getByRole("button", { name: "Canh nấm" })).toBeVisible()

  await dialog.getByRole("button", { name: "Trang sau" }).click()

  await expect(
    dialog.getByRole("button", { name: "Đậu phụ kho" }),
  ).toBeVisible()

  expect(publicSearches).toBe(0)
})

test("#154: add a public recipe with an empty saved collection, search and pagination", async ({ page }) => {
  const state = await setup(page)
  const savedWrites: string[] = []
  const searches: URLSearchParams[] = []
  page.on("request", request => {
    if (request.url().includes("/saved-recipes") && request.method() !== "GET") savedWrites.push(request.method())
  })
  await page.route("**/api/v1/saved-recipes**", route => route.fulfill({ json: { content: [], totalPages: 0 } }))
  await page.route("**/api/v1/recipes?**", route => {
    const params = new URL(route.request().url()).searchParams
    searches.push(params)
    const index = params.get("keyword") || params.get("page") === "1" ? 1 : 0
    return route.fulfill({ json: { items: [recipes[index]], totalPages: params.get("keyword") ? 1 : 2 } })
  })
  await page.goto("/ke-hoach")
  await openMonday(page)
  await lunch(page).click()
  const dialog = page.getByRole("dialog")
  await expect(dialog.getByText("Bạn chưa lưu công thức nào.")).toBeVisible()
  await dialog.getByRole("button", { name: "Tìm công thức công khai", exact: true }).click()
  await expect(dialog.getByRole("button", { name: "Canh nấm", exact: true })).toBeVisible()
  await dialog.getByRole("button", { name: "Trang sau" }).click()
  await expect(dialog.getByRole("button", { name: "Đậu phụ kho", exact: true })).toBeVisible()
  await dialog.getByLabel("Tìm món cho lịch ăn").fill("Đậu")
  await dialog.getByRole("button", { name: "Tìm", exact: true }).click()
  await expect.poll(() => searches.at(-1)?.get("keyword")).toBe("Đậu")
  expect(searches.at(-1)?.get("page")).toBe("0")
  await page.screenshot({ path: test.info().outputPath("detail-meal-plan-public-picker-desktop.png"), fullPage: true })
  await dialog.getByRole("button", { name: "Đậu phụ kho", exact: true }).click()
  await expect(dialog).not.toBeVisible()
  expect(state.entries).toHaveLength(1)
  expect(state.entries[0]).toMatchObject({ mealDate: week, mealType: "LUNCH", recipeId: 12 })
  await page.reload()
  await openMonday(page)
  await expect(page.getByRole("article", { name: "Đậu phụ kho" })).toBeVisible()
  expect(savedWrites).toEqual([])
  expect(state.previews).toBe(0)
})

test("#154: public replacement preserves the meal and servings and rejects unavailable or duplicate choices", async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 812 })
  const state = await setup(page, [
    { ...entry(8, 11, true), plannedServings: 2.5 }, entry(9, 12),
  ])
  const savedWrites: string[] = []
  page.on("request", request => {
    if (request.url().includes("/saved-recipes") && request.method() !== "GET") savedWrites.push(request.method())
  })
  await page.route("**/api/v1/recipes?**", route => route.fulfill({ json: {
    items: [...recipes, { ...recipe(99, "Món đã ẩn"), status: "HIDDEN" }], totalPages: 1,
  } }))
  let unavailable = true
  await page.route("**/api/v1/meal-plans/*/entries/8", route => unavailable
    ? route.fulfill({ status: 404, json: { status: 404, code: "NOT_FOUND", detail: "Công thức không còn công khai." } })
    : route.fallback())
  await page.goto("/ke-hoach")
  await openMonday(page)
  await page.getByRole("button", { name: "Thay món Công thức không còn khả dụng" }).click()
  const dialog = page.getByRole("dialog")
  await dialog.getByRole("button", { name: "Tìm công thức", exact: true }).click()
  await expect(dialog.getByRole("button", { name: /Đậu phụ kho.*Đã có trong bữa này/ })).toBeDisabled()
  await expect(dialog.getByRole("button", { name: /Món đã ẩn/ })).toBeDisabled()
  await page.screenshot({ path: test.info().outputPath("detail-meal-plan-public-picker-mobile.png"), fullPage: true })
  expect(await dialog.evaluate(el => el.getBoundingClientRect().right)).toBeLessThanOrEqual(375)
  await dialog.getByRole("button", { name: "Canh nấm", exact: true }).click()
  await expect(dialog.getByRole("alert")).toContainText("không còn công khai")
  expect(state.entries[0].recipeDeleted).toBe(true)
  unavailable = false
  await dialog.getByRole("button", { name: "Canh nấm", exact: true }).click()
  await expect(dialog).not.toBeVisible()
  expect(state.entries[0]).toMatchObject({ entryId: 8, mealDate: week, mealType: "LUNCH", plannedServings: 2.5, recipeId: 11, recipeDeleted: false })
  expect(savedWrites).toEqual([])
})

test("#154: switching sources ignores late results and supports retry, empty results and keyboard close", async ({ page }) => {
  await setup(page)
  let release!: () => void
  const hold = new Promise<void>(resolve => { release = resolve })
  let attempts = 0
  await page.route("**/api/v1/recipes?**", async route => {
    attempts++
    if (attempts === 1) {
      await hold
      return route.fulfill({ json: { items: [recipe(77, "Kết quả cũ")], totalPages: 1 } })
    }
    if (attempts === 2) return route.fulfill({ status: 503, json: { status: 503, code: "SERVICE_UNAVAILABLE", detail: "Chưa tải được công thức công khai." } })
    return route.fulfill({ json: { items: [], totalPages: 0 } })
  })
  await page.goto("/ke-hoach")
  await openMonday(page)
  await lunch(page).click()
  const dialog = page.getByRole("dialog")
  await expect(dialog.getByLabel("Tìm món cho lịch ăn")).toBeFocused()
  await dialog.getByRole("button", { name: "Tìm công thức", exact: true }).click()
  await expect.poll(() => attempts).toBe(1)
  await dialog.getByRole("button", { name: "Đã lưu", exact: true }).click()
  await expect(dialog.getByRole("button", { name: "Canh nấm", exact: true })).toBeVisible()
  const late = page.waitForResponse(response => response.url().includes("/recipes?") && response.status() === 200)
  release()
  await late
  await expect(dialog.getByRole("button", { name: "Kết quả cũ", exact: true })).toHaveCount(0)
  await dialog.getByRole("button", { name: "Tìm công thức", exact: true }).click()
  await expect(dialog.getByRole("alert")).toContainText("Chưa tải được")
  await dialog.getByRole("button", { name: "Thử lại" }).click()
  await expect(dialog.getByText("Không tìm thấy công thức công khai phù hợp.")).toBeVisible()
  await page.keyboard.press("Escape")
  await expect(dialog).not.toBeVisible()
  await expect(lunch(page)).toBeFocused()
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
  await page.getByLabel("Mục tiêu kế hoạch ăn của tuần này (không bắt buộc)").selectOption("LOSE_WEIGHT")
  await page.getByLabel("Bạn muốn lịch ăn có những nguyên liệu nào", { exact: true }).fill("Nấm")
  await page.getByRole("button", { name: "+ Nấm", exact: true }).click()
  await page.getByLabel("Bạn muốn lịch ăn không có nguyên liệu nào", { exact: true }).fill("Đậu")
  await page.getByRole("button", { name: "+ Đậu phụ", exact: true }).click()
  await page.getByRole("button", { name: "Tạo thực đơn tuần" }).click()
  await expect(page.getByText("Bản xem trước · 21 món được chọn")).toBeVisible()

  expect(state.previewRequests).toEqual([{
    weekStartDate: week,
    mode: "THIS_WEEK",
    goal: "LOSE_WEIGHT",
    preferredIngredientIds: [1],
    avoidedIngredientIds: [2],
  }])
  await expect(page.getByLabel("Mục tiêu kế hoạch ăn của tuần này (không bắt buộc)")).toHaveValue("")
  await expect(page.getByRole("button", { name: "Xóa Nấm", exact: true })).toHaveCount(0)
  await expect(page.getByRole("button", { name: "Xóa Đậu phụ", exact: true })).toHaveCount(0)
  expect(state.aiApplies).toBe(0)
  expect(state.entries).toEqual([])
})

test("nutrition uses planned servings and divides weekly averages across all seven days", async ({
  page,
}) => {
  const tomorrow = new Date(`${week}T12:00:00`)

  tomorrow.setDate(tomorrow.getDate() + 1)

  const next = `${tomorrow.getFullYear()}-${String(tomorrow.getMonth() + 1).padStart(2, "0")}-${String(tomorrow.getDate()).padStart(2, "0")}`

  const state = await setup(page, [
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

  await expect(dialog.getByText("350 kcal", { exact: true })).toBeVisible()
  await expect(dialog.getByText("7 g", { exact: true })).toBeVisible()

  expect(requests).toBe(0)

  await dialog.getByRole("button", { name: "So sánh & Thống kê 7 ngày" }).click()

  await expect(dialog.getByText("107,14 kcal", { exact: true })).toBeVisible()
  await expect(dialog.getByRole("table").locator("tbody tr")).toHaveCount(9)

  expect(requests).toBe(0)

  await dialog.getByRole("button", { name: "Theo ngày" }).click()

  await dialog.getByLabel("Ngày xem").selectOption(next)

  await expect(dialog.getByText("400 kcal", { exact: true })).toBeVisible()

  state.aiFail = true
  await dialog.getByRole("button", { name: "Phân tích và gợi ý món" }).first().click()
  await expect(dialog.getByRole("alert")).toContainText("Dịch vụ AI hiện không khả dụng")
  await expect(dialog.getByText("400 kcal", { exact: true })).toBeVisible()
  state.aiFail = false
  await dialog.getByRole("button", { name: "Phân tích và gợi ý món" }).first().click()
  await expect(dialog.getByRole("heading", { name: "Phân tích của AI" })).toBeVisible()
  await expect(dialog.getByText("Thực đơn có thể điều chỉnh bằng các công thức được gợi ý.")).toBeVisible()
  await expect(dialog.getByText("Đậu phụ kho", { exact: true })).toBeVisible()
  await dialog.getByRole("button", { name: "Thay món này" }).click()
  await expect(dialog.getByRole("button", { name: "Phân tích và gợi ý món" })).toHaveCount(1)
  expect(state.entries.find((item) => item.entryId === 3)?.recipeId).toBe(12)

  const downloadPromise = page.waitForEvent("download")
  await dialog.getByRole("button", { name: "Xuất báo cáo PDF" }).click()
  expect((await downloadPromise).suggestedFilename()).toContain("bao-cao-dinh-duong")
})

test("nutrition discloses missing data and supports retry without false zero totals", async ({
  page,
}) => {
  const state = await setup(page, [entry()])
  state.nutritionFail = true

  await page.goto("/ke-hoach")

  await openMonday(page)

  await page.getByRole("button", { name: "Xem dinh dưỡng ngày" }).click()

  const dialog = page.getByRole("dialog")

  await expect(dialog.getByRole("alert")).toContainText("Chưa tải được bảng dinh dưỡng")

  state.nutritionFail = false
  state.nutritionIncomplete = true

  await dialog.getByRole("button", { name: "Thử lại" }).click()

  await expect(dialog.getByText("Nấm", { exact: false }).first()).toBeVisible()

  await expect(dialog.getByText("0 kcal", { exact: true })).toHaveCount(0)

  await expect(
    dialog.getByRole("table").getByText("Chưa đủ dữ liệu"),
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
  await expect(page.getByRole("status").filter({ hasText: "Đã lưu lịch tuần" })).toContainText("Đã lưu lịch tuần")
  expect(state.writes).toBe(1)
})

test("recipe detail saves half servings and the calendar recalculates nutrition", async ({ page }) => {
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
  await expect(page.getByText("1,5 phần", { exact: true })).toBeVisible()
  await expect(page.getByText("150 g", { exact: true })).toBeVisible()
  expect(state.writes).toBe(0)
  await page.getByRole("button", { name: "Lưu khẩu phần vào lịch ăn" }).click()
  await expect(page.getByRole("status").filter({ hasText: "Đã lưu 1,5 phần" })).toContainText("Đã lưu 1,5 phần")
  expect(state.entries[0].plannedServings).toBe(1.5)
  expect(state.entries[1].plannedServings).toBe(3)
  await page.getByRole("link", { name: "Về lịch ăn", exact: true }).click()
  await expect(page.getByRole("article", { name: "Canh nấm" }).first()).toContainText("25 phút · 1,5 phần")
  await page.reload()
  await expect(page.getByRole("article", { name: "Canh nấm" }).first()).toContainText("25 phút · 1,5 phần")
  await page.getByRole("button", { name: "Xem dinh dưỡng ngày" }).click()
  await expect(page.getByRole("dialog").getByText("450 kcal", { exact: true })).toBeVisible()
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
  await expect(page.getByRole("alert").filter({ hasText: "Chưa lưu được" })).toContainText("Chưa lưu được")
  expect(state.entries[0].plannedServings).toBe(1)
  state.fail = ""
  await page.getByRole("button", { name: "Lưu khẩu phần vào lịch ăn" }).click()
  await expect(page.getByRole("status").filter({ hasText: "Đã lưu 1,5 phần" })).toContainText("Đã lưu 1,5 phần")
  expect(state.entries[0].plannedServings).toBe(1.5)
})

test("calendar keyboard crosses month boundaries and restores the current week", async ({ page }) => {
  const state = await setup(page)
  await page.goto('/ke-hoach?weekStartDate=2026-01-26&mealDate=2026-01-31')
  const calendar = page.getByRole('region', { name: 'Chọn tuần trong lịch' })
  const january31 = calendar.getByRole('button', { name: 'Chọn ngày 31/1/2026', exact: true })
  await january31.click()
  await january31.press('ArrowRight')
  const february1 = calendar.getByRole('button', { name: 'Chọn ngày 1/2/2026', exact: true })
  await expect(february1).toBeFocused()
  await expect(february1).toHaveAttribute('aria-pressed', 'true')
  await february1.press('ArrowRight')
  const february2 = calendar.getByRole('button', { name: 'Chọn ngày 2/2/2026', exact: true })
  await expect(february2).toBeFocused()
  await february2.press('ArrowUp')
  const january26 = calendar.getByRole('button', { name: 'Chọn ngày 26/1/2026', exact: true })
  await expect(january26).toBeFocused()
  await january26.press('ArrowDown')
  await expect(february2).toBeFocused()
  await february2.press('ArrowLeft')
  await expect(february1).toBeFocused()
  await calendar.getByRole('button', { name: 'Tuần này', exact: true }).click()
  const today = new Date().toLocaleDateString('vi-VN')
  await expect(calendar.getByRole('button', { name: `Chọn ngày ${today}`, exact: true })).toHaveAttribute('aria-pressed', 'true')
  expect(state.writes).toBe(0)
})

test('calendar month navigation keeps one keyboard entry point and honors reduced motion', async ({ page }, testInfo) => {
  await page.emulateMedia({ reducedMotion: 'reduce' })
  const state = await setup(page)
  await page.goto('/ke-hoach?weekStartDate=2026-01-12&mealDate=2026-01-12')
  const calendar = page.getByRole('region', { name: 'Chọn tuần trong lịch' })
  await calendar.getByRole('button', { name: 'Tháng trước', exact: true }).click()
  await expect(calendar.getByRole('heading', { name: 'tháng 12 năm 2025', exact: true })).toBeVisible()
  const activeGrid = calendar.locator('.u030-grid:not([aria-hidden="true"])')
  await expect(activeGrid.locator('button[tabindex="0"]')).toHaveCount(1)
  await calendar.getByRole('button', { name: 'Tháng sau', exact: true }).click()
  await expect(calendar.getByRole('heading', { name: 'tháng 1 năm 2026', exact: true })).toBeVisible()
  await calendar.getByRole('button', { name: 'Tháng sau', exact: true }).click()
  await expect(calendar.getByRole('heading', { name: 'tháng 2 năm 2026', exact: true })).toBeVisible()
  const firstDay = calendar.getByRole('button', { name: 'Chọn ngày 1/2/2026', exact: true })
  await firstDay.click()
  await expect(firstDay).toHaveAttribute('aria-pressed', 'true')
  await expect(activeGrid.locator('button[tabindex="0"]')).toHaveCount(1)
  expect(parseFloat(await activeGrid.evaluate(element => getComputedStyle(element).animationDuration))).toBeLessThanOrEqual(0.001)
  expect(state.writes).toBe(0)
  const screenshot = testInfo.outputPath('animated-calendar.png')
  await page.screenshot({ path: screenshot, fullPage: true })
  await testInfo.attach('Calendar layout', { path: screenshot, contentType: 'image/png' })
})

test("a mismatched plan entry cannot be edited and ordinary recipe viewing does not write a plan", async ({ page }) => {
  const state = await setup(page, [entry()])
  await page.goto(`/cong-thuc/id/11?mealPlanWeek=${week}&mealPlanEntryId=999`)
  await expect(page.getByRole("alert").filter({ hasText: "không còn thuộc lịch ăn" })).toContainText("không còn thuộc lịch ăn")
  await expect(page.getByRole("button", { name: "Lưu khẩu phần vào lịch ăn" })).toBeDisabled()
  expect(state.writes).toBe(0)
  await page.goto("/cong-thuc/id/11")
  await page.getByRole("button", { name: "Tăng khẩu phần" }).click()
  await expect(page.getByText("3 phần", { exact: true })).toBeVisible()
  await expect(page.getByRole("button", { name: "Lưu khẩu phần vào lịch ăn" })).toHaveCount(0)
  expect(state.writes).toBe(0)
})

test("invalid legacy serving values need correction before saving", async ({ page }) => {
  const state = await setup(page, [{ ...entry(), plannedServings: 1.1 }])
  await page.goto(`/cong-thuc/id/11?mealPlanWeek=${week}&mealPlanEntryId=1`)
  const input = page.getByRole("spinbutton", { name: "Số khẩu phần cần nấu" })
  await expect(input).toHaveValue("1.1")
  await expect(page.getByRole("button", { name: "Lưu khẩu phần vào lịch ăn" })).toBeDisabled()
  expect(state.writes).toBe(0)
  await input.fill("1.5")
  await page.getByRole("button", { name: "Lưu khẩu phần vào lịch ăn" }).click()
  await expect(page.getByRole("status").filter({ hasText: "Đã lưu 1,5 phần" })).toContainText("Đã lưu 1,5 phần")
  expect(state.entries[0].plannedServings).toBe(1.5)
})

test("empty saved collection and empty nutrition explain the next step", async ({ page }) => {
  const state = await setup(page)
  await page.route("**/api/v1/saved-recipes**", (route) => route.fulfill({ json: { content: [], page: 0, size: 12, totalElements: 0, totalPages: 0 } }))
  await page.goto("/ke-hoach")
  await openMonday(page)
  await lunch(page).click()
  const dialog = page.getByRole("dialog")
  await expect(dialog.getByText("Bạn chưa lưu công thức nào.")).toBeVisible()
  await expect(dialog.getByRole("button", { name: "Tìm công thức công khai", exact: true })).toBeVisible()
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
  await expect(page.getByRole("article").first()).toHaveAttribute("aria-label", "Đậu phụ kho")
  await page.reload()
  await openMonday(page)
  await expect(page.getByRole("article")).toHaveCount(2)
  await expect(page.getByRole("article").first()).toHaveAttribute("aria-label", "Đậu phụ kho")
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
  await expect(dialog.getByRole("button", { name: "Đã lưu", exact: true })).toHaveAttribute("aria-pressed", "true")
  await expect(
    dialog.getByRole("button", { name: /Công thức không còn khả dụng/ }),
  ).toBeDisabled()
  await dialog.getByRole("button", { name: "Đậu phụ kho" }).click()
  await expect(
    page.getByRole("article", { name: "Đậu phụ kho" }),
  ).toContainText("2,5 phần")
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
    await expect(page.getByRole("dialog").getByRole("table").locator("tbody tr")).toHaveCount(9)
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
