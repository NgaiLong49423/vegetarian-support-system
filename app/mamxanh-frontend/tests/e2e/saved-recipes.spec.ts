import type { Page } from "@playwright/test"
import { expect, test } from "./baseFixtures"

const recipe = {
  id: 901,
  authorId: 22,
  authorName: "Tác giả công khai",
  authorAvatarUrl: null,
  title: "Canh nấm từ API",
  description: "Món canh",
  instructions: "Nấu chín",
  dishCategory: "SOUP",
  vegetarianType: "VEGAN",
  difficulty: "EASY",
  servings: 2,
  prepTimeMin: 5,
  cookTimeMin: 10,
  youtubeUrl: null,
  status: "PUBLISHED",
  media: [],
  ingredients: [],
  likes: 0,
  dislikes: 0,
  likePercentage: null,
  viewCount: 0,
}
const saved = {
  recipeId: 901,
  title: recipe.title,
  description: recipe.description,
  coverUrl: null,
  authorName: recipe.authorName,
  available: true,
  unavailableMessage: null,
  savedAt: "2026-10-11T08:00:00",
}
const tombstone = {
  recipeId: 902,
  title: null,
  description: null,
  coverUrl: null,
  authorName: null,
  available: false,
  unavailableMessage: "Công thức không còn khả dụng",
  savedAt: "2026-10-10T08:00:00",
}

async function publicApi(page: Page) {
  await page.route(
    (url) => url.pathname === "/api/v1/recipes",
    (route) =>
      route.fulfill({
        json: {
          items: [recipe],
          page: 0,
          size: 12,
          totalElements: 1,
          totalPages: 1,
        },
      }),
  )
  await page.route(
    (url) => url.pathname === "/api/v1/recipes/form-options",
    (route) =>
      route.fulfill({
        json: {
          vegetarianTypes: [{ code: "VEGAN", label: "Thuần chay" }],
          dishCategories: [{ code: "SOUP", label: "Món canh" }],
          difficulties: [],
          units: [],
        },
      }),
  )
}

test("Guest sees the login prompt without sending a save mutation", async ({
  page,
}) => {
  await publicApi(page)
  let mutations = 0
  page.on("request", (request) => {
    if (request.method() === "PUT" && request.url().includes("/saved-recipes/"))
      mutations += 1
  })
  await page.goto("/kham-pha")
  await page.getByRole("button", { name: "Lưu bài viết" }).click()
  await expect(
    page.getByRole("dialog", { name: "Đăng nhập để lưu công thức" }),
  ).toBeVisible()
  expect(mutations).toBe(0)
})

test("Member saves, searches, removes and sees safe unavailable references from API", async ({
  page,
}) => {
  await page.addInitScript(() =>
    sessionStorage.setItem(
      "mamxanh.auth",
      JSON.stringify({
        accessToken: "isolated-test-token",
        expiresAt: Date.now() + 600_000,
        account: {
          id: 17,
          displayName: "Thành viên kiểm thử",
          email: "member@test.local",
          role: "CUSTOMER",
          avatarUrl: null,
        },
      }),
    ),
  )
  await publicApi(page)
  let isSaved = false
  let hasTombstone = true
  let saves = 0
  await page.route(
    (url) => url.pathname.startsWith("/api/v1/saved-recipes"),
    (route) => {
      const request = route.request()
      const url = new URL(request.url())
      expect(request.headers().authorization).toBe("Bearer isolated-test-token")
      expect(url.searchParams.has("userId")).toBe(false)
      if (request.method() === "PUT") {
        isSaved = true
        saves += 1
        return route.fulfill({ status: 204 })
      }
      if (request.method() === "DELETE") {
        if (url.pathname.endsWith("/901")) isSaved = false
        else hasTombstone = false
        return route.fulfill({ status: 204 })
      }
      const keyword = url.searchParams.get("keyword") ?? ""
      const items = [
        ...(isSaved ? [saved] : []),
        ...(hasTombstone ? [tombstone] : []),
      ].filter(
        (item) =>
          !keyword || item.title?.toLowerCase().includes(keyword.toLowerCase()),
      )
      return route.fulfill({
        json: {
          content: items,
          page: 0,
          size: 20,
          totalElements: items.length,
          totalPages: items.length ? 1 : 0,
        },
      })
    },
  )
  await page.goto("/kham-pha")
  await page.getByRole("button", { name: "Lưu bài viết" }).click()
  await expect(
    page.getByRole("button", { name: "Bỏ lưu", exact: true }),
  ).toHaveAttribute("aria-pressed", "true")
  expect(saves).toBe(1)
  await page.getByRole("link", { name: "Đã lưu", exact: true }).click()
  await expect(page.getByRole("heading", { name: recipe.title })).toBeVisible()
  await expect(
    page.getByText("Công thức không còn khả dụng", { exact: true }),
  ).toBeVisible()
  await expect(page.locator('a[href="/cong-thuc/id/902"]')).toHaveCount(0)
  await page
    .getByRole("textbox", { name: "Tìm trong công thức đã lưu" })
    .fill("Canh")
  await expect(
    page.getByText("Công thức không còn khả dụng", { exact: true }),
  ).toHaveCount(0)
  await page
    .getByRole("button", { name: `Bỏ lưu ${recipe.title}`, exact: true })
    .click()
  await expect(
    page.getByRole("heading", { name: "Không tìm thấy công thức đã lưu" }),
  ).toBeVisible()
  expect(isSaved).toBe(false)
  await page.getByRole("button", { name: "Xóa tìm kiếm đã lưu" }).click()
  await page
    .getByRole("button", { name: "Bỏ lưu công thức không còn khả dụng" })
    .click()
  await expect(
    page.getByRole("heading", { name: "Chưa có công thức đã lưu" }),
  ).toBeVisible()
})

test("Administrator does not request a member saved recipe list", async ({
  page,
}) => {
  await page.addInitScript(() =>
    sessionStorage.setItem(
      "mamxanh.auth",
      JSON.stringify({
        accessToken: "isolated-admin-token",
        expiresAt: Date.now() + 600_000,
        account: {
          id: 18,
          displayName: "Admin kiểm thử",
          email: "admin@test.local",
          role: "ADMIN",
          avatarUrl: null,
        },
      }),
    ),
  )
  let reads = 0
  page.on("request", (request) => {
    if (new URL(request.url()).pathname === "/api/v1/saved-recipes") reads += 1
  })
  await page.goto("/ho-so")
  await expect(
    page.getByRole("heading", { name: "Danh sách riêng dành cho thành viên" }),
  ).toBeVisible()
  expect(reads).toBe(0)
})

test("Failed save preserves the unsaved state and can be retried", async ({
  page,
}) => {
  await page.addInitScript(() =>
    sessionStorage.setItem(
      "mamxanh.auth",
      JSON.stringify({
        accessToken: "isolated-test-token",
        expiresAt: Date.now() + 600_000,
        account: {
          id: 17,
          displayName: "Thành viên kiểm thử",
          email: "member@test.local",
          role: "CUSTOMER",
          avatarUrl: null,
        },
      }),
    ),
  )
  await publicApi(page)
  let attempts = 0
  await page.route("**/api/v1/saved-recipes/901", (route) => {
    attempts++
    return route.fulfill(
      attempts === 1
        ? {
            status: 503,
            json: {
              status: 503,
              code: "SERVICE_UNAVAILABLE",
              detail: "Chưa lưu được công thức.",
            },
          }
        : { status: 204 },
    )
  })
  await page.goto("/kham-pha")
  await page.getByRole("button", { name: "Lưu bài viết" }).click()
  await expect(page.getByRole("status")).toContainText(
    "Chưa lưu được công thức.",
  )
  await expect(
    page.getByRole("button", { name: "Lưu bài viết" }),
  ).toHaveAttribute("aria-pressed", "false")
  await page.getByRole("button", { name: "Lưu bài viết" }).click()
  await expect(
    page.getByRole("button", { name: "Bỏ lưu", exact: true }),
  ).toHaveAttribute("aria-pressed", "true")
  expect(attempts).toBe(2)
})
