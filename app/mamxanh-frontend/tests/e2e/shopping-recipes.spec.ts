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
  ingredients: [{ ingredientId: 1, quantity: 300, unitId: 1 }],
  likes: 0,
  dislikes: 0,
  likePercentage: null,
  viewCount: 0,
})
const list = {
  id: 77,
  name: "Danh sách cá nhân từ công thức",
  createdAt: "",
  updatedAt: "",
  items: [
    {
      id: 1,
      ingredientId: 1,
      name: "Đậu phụ",
      ingredientGroup: "Đậu",
      quantity: 1100,
      unitId: 1,
      unitCode: "g",
      unitName: "gram",
      bought: false,
      sources: [
        {
          recipeId: 1,
          title: "Món đi chợ 1",
          available: true,
          originalQuantity: 300,
          originalUnitId: 1,
          originalUnitCode: "g",
        },
        {
          recipeId: 2,
          title: null,
          available: false,
          originalQuantity: 800,
          originalUnitId: 1,
          originalUnitCode: "g",
        },
      ],
    },
  ],
}
async function setup(
  page: Page,
  role: "CUSTOMER" | "EXPERT" | "ADMIN" | null = "CUSTOMER",
) {
  if (role) await seedDemoSession(page, role)
  const state = { fail: false, payloads: [] as { recipeIds: number[] }[] }
  await page.route("**/api/v1/recipes/form-options", (r) =>
    r.fulfill({
      json: {
        dishCategories: [],
        vegetarianTypes: [],
        difficulties: [],
        units: [],
      },
    }),
  )
  await page.route("**/api/v1/recipes/discovery?**", (r) => {
    const current = Number(
      new URL(r.request().url()).searchParams.get("page") ?? 0,
    )
    return r.fulfill({
      json: {
        items: [recipe(current + 1)],
        page: current,
        size: 12,
        hasNext: current === 0,
      },
    })
  })
  await page.route("**/api/v1/shopping-lists/from-recipes", (r) => {
    state.payloads.push(r.request().postDataJSON())
    return state.fail
      ? r.fulfill({
          status: 404,
          json: { code: "NOT_FOUND", detail: "Công thức đã ẩn. Hãy tải lại." },
        })
      : r.fulfill({
          json: { shoppingList: list, warnings: [], incompatibleItemIds: [] },
        })
  })
  await page.route("**/api/v1/shopping-lists?**", (r) =>
    r.fulfill({
      json: {
        content: [list],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      },
    }),
  )
  await page.route("**/api/v1/shopping-lists/77", (r) =>
    r.fulfill({ json: list }),
  )
  await page.route("**/api/v1/shopping-lists/options", (r) =>
    r.fulfill({ json: [{ id: 1, code: "g", name: "gram", active: true }] }),
  )
  return state
}

test("AC-53.6 selects recipes across Explore pages and opens a persisted personal checklist with sources", async ({
  page,
}) => {
  const state = await setup(page, "EXPERT")
  await page.goto("/kham-pha")
  await page.getByLabel("Chọn Món đi chợ 1 để đi chợ").check()
  await page.getByRole("button", { name: "Trang sau", exact: true }).click()
  await page.getByLabel("Chọn Món đi chợ 2 để đi chợ").check()
  await page
    .getByRole("button", { name: "Thêm 2 công thức vào danh sách đi chợ" })
    .click()
  await expect(page).toHaveURL(/di-cho\?listId=77/)
  expect(state.payloads).toEqual([{ recipeIds: [1, 2] }])
  await expect(page.getByTestId("shopping-item-1")).toContainText("1.100 g")
  await expect(
    page.getByRole("link", { name: "Món đi chợ 1", exact: true }),
  ).toHaveAttribute("href", "/cong-thuc/id/1")
  await expect(
    page.getByText("Công thức không còn khả dụng", { exact: true }),
  ).toBeVisible()
  await page.reload()
  await expect(page.getByLabel("Nguồn của Đậu phụ")).toContainText(
    "300 g lúc thêm",
  )
  expect(state.payloads).toHaveLength(1)
})

test("AC-53.6 failed additions retain selection and can be retried or cleared", async ({
  page,
}) => {
  const state = await setup(page)
  state.fail = true
  await page.goto("/kham-pha")
  await expect(
    page.getByRole("button", { name: "Thêm 0 công thức vào danh sách đi chợ" }),
  ).toBeDisabled()
  await page.getByLabel("Chọn Món đi chợ 1 để đi chợ").check()
  await page
    .getByRole("button", { name: "Thêm 1 công thức vào danh sách đi chợ" })
    .click()
  await expect(page.getByRole("alert")).toContainText("Công thức đã ẩn")
  await expect(page.getByLabel("Chọn Món đi chợ 1 để đi chợ")).toBeChecked()
  await page.getByRole("button", { name: "Bỏ chọn tất cả" }).click()
  await expect(page.getByLabel("Chọn Món đi chợ 1 để đi chợ")).not.toBeChecked()
  await page.getByLabel("Chọn Món đi chợ 1 để đi chợ").check()
  state.fail = false
  await page
    .getByRole("button", { name: "Thêm 1 công thức vào danh sách đi chợ" })
    .click()
  await expect(page).toHaveURL(/di-cho\?listId=77/)
})

test("AC-53.6 saved recipes allow selection of available sources only", async ({
  page,
}) => {
  const state = await setup(page)
  await page.route("**/api/v1/saved-recipes?**", (r) =>
    r.fulfill({
      json: {
        content: [
          {
            recipeId: 1,
            title: "Món đã lưu",
            available: true,
            authorName: "Tác giả",
            coverUrl: null,
          },
          {
            recipeId: 2,
            title: null,
            available: false,
            unavailableMessage: "Công thức không còn khả dụng",
          },
        ],
        page: 0,
        size: 20,
        totalElements: 2,
        totalPages: 1,
      },
    }),
  )
  await page.goto("/ho-so")
  await page.getByLabel("Chọn Món đã lưu để đi chợ").check()
  await expect(page.getByRole("checkbox")).toHaveCount(1)
  await page
    .getByRole("button", { name: "Thêm 1 công thức vào danh sách đi chợ" })
    .click()
  await expect(page).toHaveURL(/di-cho\?listId=77/)
  expect(state.payloads).toEqual([{ recipeIds: [1] }])
})

test("AC-53.6 Guest sees sign-in and Admin cannot select or submit recipes", async ({
  page,
}) => {
  const state = await setup(page, null)
  await page.goto("/kham-pha")
  await expect(
    page.getByRole("link", {
      name: "Đăng nhập để tạo danh sách đi chợ từ công thức",
    }),
  ).toHaveAttribute("href", "/dang-nhap")
  await expect(page.getByRole("checkbox", { name: /để đi chợ/ })).toHaveCount(0)
  expect(state.payloads).toHaveLength(0)
})

test("AC-53.6 Admin has no shopping selection", async ({ page }) => {
  const state = await setup(page, "ADMIN")
  await page.goto("/kham-pha")
  await expect(
    page.getByRole("heading", { name: "Món đi chợ 1", exact: true }),
  ).toBeVisible()
  await expect(
    page.getByRole("region", { name: "Tạo danh sách từ công thức" }),
  ).toHaveCount(0)
  expect(state.payloads).toHaveLength(0)
})
