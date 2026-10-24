import { expect, test } from "./baseFixtures"
import { seedDemoSession } from "./demo-session"
import type { Page } from "@playwright/test"

const recipe = (id: number) => ({
  id,
  title: `Món đi chợ ${id}`,
  authorId: 22,
  authorName: "Tác giả",
  authorAvatarUrl: null,
  dishCategory: "SOUP",
  vegetarianType: "VEGAN",
  difficulty: "EASY",
  servings: 2,
  prepTimeMin: 10,
  cookTimeMin: 20,
  media: [],
  ingredients: [{ ingredientId: 1, name: "Đậu phụ", quantity: 300, unitId: 1, unitCode: "g", unitName: "gram", customName: null }],
  likes: 0,
  dislikes: 0,
  likePercentage: null,
  viewCount: 0,
})

const detail = (id: number) => ({
  recipeId: id,
  title: `Món đi chợ ${id}`,
  servings: 2,
  ingredients: [{ ingredientId: 1, name: "Đậu phụ", quantity: 300, unitId: 1, unitCode: "g", unitName: "gram", nutritionSupported: true }],
})

const list = {
  id: 77,
  name: "Danh sách tuần",
  createdAt: "",
  updatedAt: "",
  items: [{
    id: 1,
    ingredientId: 1,
    name: "Đậu phụ",
    ingredientGroup: "Đậu",
    quantity: 450,
    unitId: 1,
    unitCode: "g",
    unitName: "gram",
    bought: false,
    sources: [{ recipeId: 1, title: "Món đi chợ 1", available: true, originalQuantity: 450, originalUnitId: 1, originalUnitCode: "g" }],
  }],
}

async function setup(page: Page, role: "CUSTOMER" | "EXPERT" | "ADMIN" | null = "CUSTOMER") {
  if (role) await seedDemoSession(page, role)
  const state = { adds: [] as unknown[], creates: [] as unknown[], fail: false }
  await page.route("**/api/v1/recipes/form-options", (r) => r.fulfill({ json: { dishCategories: [], vegetarianTypes: [], difficulties: [], units: [] } }))
  await page.route("**/api/v1/recipes/discovery?**", (r) => {
    const pageNumber = Number(new URL(r.request().url()).searchParams.get("page") ?? 0)
    return r.fulfill({ json: { items: [recipe(pageNumber + 1)], page: pageNumber, size: 12, hasNext: pageNumber === 0 } })
  })
  await page.route("**/api/v1/recipes/1", (r) => r.fulfill({ json: detail(1) }))
  await page.route("**/api/v1/shopping-lists?**", (r) => {
    const pageNumber = Number(new URL(r.request().url()).searchParams.get("page") ?? 0)
    return r.fulfill({ json: { content: pageNumber === 0 ? [list] : [{ ...list, id: 78, name: "Danh sách khác" }], page: pageNumber, size: 20, totalElements: 21, totalPages: 2 } })
  })
  await page.route("**/api/v1/shopping-lists/from-recipe", (r) => {
    state.creates.push(r.request().postDataJSON())
    return state.fail
      ? r.fulfill({ status: 404, json: { code: "NOT_FOUND", detail: "Công thức đã ẩn. Hãy tải lại." } })
      : r.fulfill({ json: { shoppingList: { ...list, name: r.request().postDataJSON().name }, warnings: [], incompatibleItemIds: [] } })
  })
  await page.route("**/api/v1/shopping-lists/*/recipes", (r) => {
    state.adds.push(r.request().postDataJSON())
    return state.fail
      ? r.fulfill({ status: 404, json: { code: "NOT_FOUND", detail: "Công thức đã ẩn. Hãy tải lại." } })
      : r.fulfill({ json: { shoppingList: list, warnings: [], incompatibleItemIds: [] } })
  })
  await page.route("**/api/v1/shopping-lists/77", (r) => r.fulfill({ json: list }))
  await page.route("**/api/v1/shopping-lists/options", (r) => r.fulfill({ json: [{ id: 1, code: "g", name: "gram", active: true }] }))
  return state
}

async function openShoppingDialog(page: Page, title = "Món đi chợ 1") {
  await page.getByRole("button", { name: `Tùy chọn cho công thức ${title}` }).click()
  await page.getByRole("menuitem", { name: "Thêm vào danh sách mua sắm" }).click()
}

test("AC-53.6 adds one recipe to a selected list and scales the servings", async ({ page }) => {
  const state = await setup(page, "EXPERT")
  await page.goto("/kham-pha")
  await expect(page.getByRole("heading", { name: "Món đi chợ 1", exact: true })).toBeVisible()
  await openShoppingDialog(page)
  await expect(page.getByRole("radio", { name: "Danh sách tuần" })).toBeVisible()
  await page.getByLabel("Tăng khẩu phần cần mua").click()
  await expect(page.getByText("450 g", { exact: true })).toBeVisible()
  await page.getByRole("radio", { name: "Danh sách tuần" }).check()
  await page.getByRole("button", { name: "Thêm vào danh sách", exact: true }).click()
  await expect(page).toHaveURL(/di-cho\?listId=77/)
  expect(state.adds).toEqual([{ recipeId: 1, servings: 3, selectedIngredientIds: [1], manualItems: [] }])
  expect(state.creates).toHaveLength(0)
})

test("AC-53.6 can load another page and create a named list with the recipe", async ({ page }) => {
  const state = await setup(page)
  await page.goto("/kham-pha")
  await openShoppingDialog(page)
  await page.getByRole("button", { name: "Tải thêm danh sách" }).click()
  await expect(page.getByRole("radio", { name: "Danh sách khác" })).toBeVisible()
  await page.getByRole("button", { name: "Tạo danh sách mới" }).click()
  await page.getByLabel("Tên danh sách").fill("Mua cuối tuần")
  await page.getByRole("button", { name: "Tạo danh sách và thêm" }).click()
  await expect(page).toHaveURL(/di-cho\?listId=77/)
  expect(state.creates).toEqual([{ name: "Mua cuối tuần", recipeId: 1, servings: 2, selectedIngredientIds: [1], manualItems: [] }])
  expect(state.adds).toHaveLength(0)
})

test("AC-53.6 validates and creates a list with manually added ingredients", async ({ page }) => {
  const state = await setup(page)
  await page.goto("/kham-pha")
  await openShoppingDialog(page)
  await page.getByRole("button", { name: "Tạo danh sách mới" }).click()
  const listName = page.getByLabel("Tên danh sách")
  await listName.fill("")
  await page.getByRole("button", { name: "Tạo danh sách và thêm" }).click()
  await expect(page.getByRole("alert")).toHaveText("Tên danh sách phải có từ 1 đến 200 ký tự.")

  await listName.fill("Mua rau")
  await page.getByLabel("Thêm Đậu phụ").uncheck()
  await page.getByRole("button", { name: "Tạo danh sách và thêm" }).click()
  await expect(page.getByRole("alert")).toHaveText("Hãy chọn ít nhất một nguyên liệu hoặc thêm mặt hàng thủ công.")

  await page.getByRole("button", { name: "Thêm mặt hàng" }).click()
  await page.getByRole("button", { name: "Thêm mặt hàng" }).click()
  const ingredientNames = page.getByLabel("Tên nguyên liệu")
  await ingredientNames.nth(0).fill("Hành lá")
  await ingredientNames.nth(1).fill(" hành   lá ")
  await page.getByRole("button", { name: "Tạo danh sách và thêm" }).click()
  await expect(page.getByRole("alert")).toHaveText("Hành lá bị trùng tên trong các mặt hàng đang thêm.")

  await page.getByRole("button", { name: /Xóa/ }).last().click()
  await page.getByRole("button", { name: "Tạo danh sách và thêm" }).click()
  await expect(page).toHaveURL(/di-cho\?listId=77/)
  expect(state.creates).toEqual([{
    name: "Mua rau",
    recipeId: 1,
    servings: 2,
    selectedIngredientIds: [],
    manualItems: [{ name: "Hành lá", quantity: 1, unitId: 1 }],
  }])
})

test("AC-53.6 prevents an existing manual ingredient from being duplicated", async ({ page }) => {
  const state = await setup(page)
  await page.goto("/kham-pha")
  await openShoppingDialog(page)
  await page.getByRole("button", { name: "Thêm mặt hàng" }).click()
  await page.getByLabel("Tên nguyên liệu").fill(" đậu   phụ ")
  await page.getByRole("radio", { name: "Danh sách tuần" }).check()
  await page.getByRole("button", { name: "Thêm vào danh sách", exact: true }).click()
  await expect(page.getByRole("alert")).toHaveText(/đậu\s+phụ đã có trong danh sách \(450 g\)\./)
  await expect(page.getByRole("dialog")).toBeVisible()

  await page.getByLabel("Tên nguyên liệu").fill("Cà chua")
  await page.getByRole("button", { name: "Thêm vào danh sách", exact: true }).click()
  await expect(page).toHaveURL(/di-cho\?listId=77/)
  expect(state.adds).toEqual([{
    recipeId: 1,
    servings: 2,
    selectedIngredientIds: [1],
    manualItems: [{ name: "Cà chua", quantity: 1, unitId: 1 }],
  }])
})

test("AC-53.6 keeps the dialog open after a failed add and allows retry", async ({ page }) => {
  const state = await setup(page)
  state.fail = true
  await page.goto("/kham-pha")
  await openShoppingDialog(page)
  await page.getByRole("radio", { name: "Danh sách tuần" }).check()
  await page.getByRole("button", { name: "Thêm vào danh sách", exact: true }).click()
  await expect(page.getByRole("alert")).toContainText("Công thức đã ẩn")
  await expect(page.getByRole("dialog")).toBeVisible()
  state.fail = false
  await page.getByRole("button", { name: "Thêm vào danh sách", exact: true }).click()
  await expect(page).toHaveURL(/di-cho\?listId=77/)
})

test("AC-53.6 submits each ingredient ID once when a recipe repeats it across units", async ({ page }) => {
  const state = await setup(page)
  await page.route("**/api/v1/recipes/discovery?**", (route) => route.fulfill({ json: {
    items: [{ ...recipe(1), ingredients: [
      { ingredientId: 1, name: "Đậu phụ", quantity: 300, unitId: 1, unitCode: "g", unitName: "gram" },
      { ingredientId: 1, name: "Đậu phụ", quantity: 100, unitId: 2, unitCode: "ml", unitName: "ml" },
    ] }], page: 0, size: 12, hasNext: false,
  } }))
  await page.goto("/kham-pha")
  await openShoppingDialog(page)
  await expect(page.getByLabel("Thêm Đậu phụ")).toHaveCount(2)
  await page.getByRole("radio", { name: "Danh sách tuần" }).check()
  await page.getByRole("button", { name: "Thêm vào danh sách", exact: true }).click()
  await expect(page).toHaveURL(/di-cho\?listId=77/)
  expect(state.adds).toEqual([{ recipeId: 1, servings: 2, selectedIngredientIds: [1], manualItems: [] }])
})

test("AC-53.6 saved recipe menu loads full recipe details before adding", async ({ page }) => {
  const state = await setup(page)
  await page.route("**/api/v1/saved-recipes?**", (r) => r.fulfill({
    json: { content: [{ recipeId: 1, title: "Món đi chợ 1", available: true, authorName: "Tác giả", coverUrl: null }], page: 0, size: 20, totalElements: 1, totalPages: 1 },
  }))
  await page.goto("/ho-so")
  await openShoppingDialog(page)
  await expect(page.getByText("Đậu phụ", { exact: true })).toBeVisible()
  await page.getByRole("radio", { name: "Danh sách tuần" }).check()
  await page.getByRole("button", { name: "Thêm vào danh sách", exact: true }).click()
  await expect(page).toHaveURL(/di-cho\?listId=77/)
  expect(state.adds).toEqual([{ recipeId: 1, servings: 2, selectedIngredientIds: [1], manualItems: [] }])
})

test("AC-53.6 Guest can sign in and Admin has no shopping action", async ({ page }) => {
  await setup(page, null)
  await page.goto("/kham-pha")
  await openShoppingDialog(page)
  await expect(page.getByRole("dialog", { name: "Thêm vào danh sách mua sắm" }).getByRole("link", { name: "Đăng nhập", exact: true })).toHaveAttribute("href", "/dang-nhap")

  await seedDemoSession(page, "ADMIN")
  await page.reload()
  await expect(page.getByRole("button", { name: /Tùy chọn cho công thức/ })).toHaveCount(0)
})
