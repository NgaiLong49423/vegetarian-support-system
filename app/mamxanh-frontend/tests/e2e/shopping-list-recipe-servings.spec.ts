import { expect, test } from "./baseFixtures"
import { seedDemoSession } from "./demo-session"

const recipe = {
  recipeId: 11,
  title: "Đậu hũ sốt nấm",
  description: "Món chay dễ nấu.",
  instructions: "Nấu chín nguyên liệu.",
  dishCategory: "SOUP",
  dishCategoryLabel: "Món canh",
  vegetarianType: "VEGAN",
  vegetarianTypeLabel: "Thuần chay",
  difficulty: "EASY",
  difficultyLabel: "Dễ",
  servings: 2,
  prepTimeMinutes: 10,
  cookTimeMinutes: 15,
  youtubeUrl: null,
  publishedAt: null,
  author: { userId: 17, displayName: "Tác giả", avatarUrl: null },
  media: [],
  ingredients: [],
  statistics: { likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null, viewCount: 0 },
  nutritionComplete: true,
  ingredientsWithoutNutrition: [],
  nutrition: { complete: true, ingredientsMissingData: [], perServing: {} },
}

async function setup(page: import("@playwright/test").Page, sourceOverrides: Record<string, unknown> = {}) {
  await seedDemoSession(page, "CUSTOMER")
  const state = {
    source: {
      sourceId: "recipe-11",
      recipeId: 11,
      occurrenceCount: 1,
      title: recipe.title,
      available: true,
      coverUrl: null,
      servings: 2,
      mealDate: null,
      mealType: null,
      ...sourceOverrides,
    },
    readFail: false,
    writeFail: false,
    writes: [] as unknown[],
    reads: 0,
  }
  await page.route("**/api/v1/recipes/11", (route) => route.fulfill({ json: recipe }))
  await page.route("**/api/v1/recipes/11/views", (route) => route.fulfill({ json: { recipeId: 11, counted: false, viewCount: 0 } }))
  await page.route("**/api/v1/recipes/11/related", (route) => route.fulfill({ json: { items: [] } }))
  await page.route("**/api/v1/recipes/11/reaction", (route) => route.fulfill({ json: { recipeId: 11, currentReaction: null, likes: 0, dislikes: 0, reactionCount: 0, likePercentage: null } }))
  await page.route("**/api/v1/recipes/11/comments**", (route) => route.fulfill({ json: { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, commentCount: 0 } }))
  await page.route("**/api/v1/shopping-lists/42/recipes/11/servings", async (route) => {
    if (route.request().method() === "PUT") {
      const body = route.request().postDataJSON()
      state.writes.push(body)
      if (state.writeFail) return route.fulfill({ status: 503, contentType: "application/problem+json", json: { status: 503, code: "SERVICE_UNAVAILABLE", detail: "Chưa lưu được khẩu phần." } })
      state.source.servings = body.servings
    } else {
      return route.fallback()
    }
    return route.fulfill({ json: {
      id: 42,
      name: "Danh sách tuần",
      createdAt: "",
      updatedAt: "",
      items: [],
      recipeSources: [state.source],
    } })
  })
  await page.route("**/api/v1/shopping-lists/42", async (route) => {
    state.reads++
    if (state.readFail) return route.fulfill({ status: 503, contentType: "application/problem+json", json: { status: 503, code: "SERVICE_UNAVAILABLE", detail: "Chưa tải được danh sách." } })
    return route.fulfill({ json: {
      id: 42, name: "Danh sách tuần", createdAt: "", updatedAt: "", items: [], recipeSources: [state.source],
    } })
  })
  return state
}

test("shopping recipe serving stepper changes total, saves and returns to its list", async ({ page }) => {
  const state = await setup(page)
  await page.goto("/cong-thuc/id/11?shoppingListId=42")
  const save = page.getByRole("button", { name: "Lưu khẩu phần vào danh sách" })
  await expect(page.getByText("2 phần", { exact: true })).toBeVisible()
  await expect(save).toBeDisabled()
  await page.getByRole("button", { name: "Giảm khẩu phần" }).click()
  await expect(page.getByText("1,5 phần", { exact: true })).toBeVisible()
  await expect(save).toBeEnabled()
  await save.click()
  await expect(page).toHaveURL(/\/di-cho\?listId=42$/)
  expect(state.writes).toEqual([{ servings: 1.5 }])
})

test("shopping recipe serving minimum follows the number of occurrences", async ({ page }) => {
  await setup(page, { occurrenceCount: 2, servings: 1 })
  await page.goto("/cong-thuc/id/11?shoppingListId=42")
  await expect(page.getByRole("button", { name: "Giảm khẩu phần" })).toBeDisabled()
  await expect(page.getByRole("button", { name: "Lưu khẩu phần vào danh sách" })).toBeDisabled()
  await page.getByRole("button", { name: "Tăng khẩu phần" }).click()
  await expect(page.getByText("1,5 phần", { exact: true })).toBeVisible()
  await expect(page.getByRole("button", { name: "Lưu khẩu phần vào danh sách" })).toBeEnabled()
})

test("shopping recipe serving editor reports missing source and can retry loading", async ({ page }) => {
  const state = await setup(page)
  state.readFail = true
  await page.goto("/cong-thuc/id/11?shoppingListId=42")
  await expect(page.getByRole("alert")).toContainText("Chưa tải được danh sách.")
  state.readFail = false
  await page.getByRole("button", { name: "Thử lại" }).click()
  await expect(page.getByText("2 phần", { exact: true })).toBeVisible()
  expect(state.reads).toBeGreaterThanOrEqual(2)
})

test("shopping recipe serving editor handles a missing or incomplete source", async ({ page }) => {
  const state = await setup(page, { servings: null })
  await page.goto("/cong-thuc/id/11?shoppingListId=42")
  await expect(page.getByRole("alert")).toContainText("không có đủ dữ liệu khẩu phần")
  expect(state.writes).toEqual([])
})

test("shopping recipe serving changes preserve the editor after a failed save and allow retry", async ({ page }) => {
  const state = await setup(page)
  state.writeFail = true
  await page.goto("/cong-thuc/id/11?shoppingListId=42")
  await page.getByRole("button", { name: "Tăng khẩu phần" }).click()
  await page.getByRole("button", { name: "Lưu khẩu phần vào danh sách" }).click()
  await expect(page.getByRole("alert")).toContainText("Chưa lưu được khẩu phần.")
  await expect(page).toHaveURL(/cong-thuc\/id\/11/)
  expect(state.writes).toEqual([{ servings: 2.5 }])
  state.writeFail = false
  await page.getByRole("button", { name: "Lưu khẩu phần vào danh sách" }).click()
  await expect(page).toHaveURL(/\/di-cho\?listId=42$/)
  expect(state.writes).toEqual([{ servings: 2.5 }, { servings: 2.5 }])
})

test("invalid shopping context is rejected without loading or writing a list", async ({ page }) => {
  const state = await setup(page)
  await page.goto("/cong-thuc/id/11?shoppingListId=bad")
  await expect(page.getByRole("alert")).toContainText("không hợp lệ")
  expect(state.reads).toBe(0)
  expect(state.writes).toEqual([])
})
