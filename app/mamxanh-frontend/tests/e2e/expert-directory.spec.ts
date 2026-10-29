import { expect, test } from "./baseFixtures"
import { seedDemoSession } from "./demo-session"
import type { Page } from "@playwright/test"

const expert = (userId = 31, overrides: Record<string, unknown> = {}) => ({
  userId,
  displayName: userId === 31 ? "Chuyên gia An" : `Chuyên gia ${userId}`,
  avatarUrl: null,
  bio: "Chia sẻ công thức chay mỗi ngày.",
  followerCount: 1250,
  following: false,
  ...overrides,
})

async function setup(page: Page, role?: "CUSTOMER" | "EXPERT" | "ADMIN") {
  if (role) await seedDemoSession(page, role)
  const state = { requests: [] as URLSearchParams[], followWrites: [] as string[], failLoad: false, failFollow: false, emptyCatalog: false }
  await page.route("**/api/v1/experts?**", (route) => {
    const params = new URL(route.request().url()).searchParams
    state.requests.push(params)
    if (state.failLoad) return route.fulfill({ status: 503, contentType: "application/problem+json", json: { status: 503, code: "SERVICE_UNAVAILABLE" } })
    const pageIndex = Number(params.get("page"))
    const query = params.get("query") ?? ""
    const content = query
      ? ("chuyên gia an".includes(query.toLocaleLowerCase("vi-VN")) ? [expert(31)] : [])
      : (state.emptyCatalog ? [] : [expert(31)])
    return route.fulfill({ json: { content: pageIndex === 0 ? content : [expert(32)], page: pageIndex, size: 12, totalElements: 13, totalPages: 2 } })
  })
  await page.route("**/api/v1/members/31/follow", async (route) => {
    state.followWrites.push(route.request().method())
    if (state.holdFollow) await new Promise<void>((resolve) => { state.releaseFollow = resolve })
    if (state.failFollow) return route.fulfill({ status: 503, contentType: "application/problem+json", json: { status: 503, code: "SERVICE_UNAVAILABLE" } })
    const following = route.request().method() === "PUT"
    return route.fulfill({ json: { userId: 31, following, followerCount: following ? 1251 : 1250 } })
  })
  return state
}

test("Guest browses experts and is sent to sign in before following", async ({ page }) => {
  await setup(page)
  await page.goto("/chuyen-gia")
  await expect(page.getByRole("heading", { name: "Chuyên gia ẩm thực chay" })).toBeVisible()
  await expect(page.getByRole("link", { name: "Xem hồ sơ Chuyên gia An" })).toHaveAttribute("href", "/thanh-vien/31")
  await page.getByRole("button", { name: "Theo dõi", exact: true }).click()
  await expect(page).toHaveURL("/dang-nhap")
})

test("expert directory trims search, restarts at page zero and paginates", async ({ page }) => {
  const state = await setup(page)
  await page.goto("/chuyen-gia")
  await page.getByRole("button", { name: "Trang sau" }).click()
  await expect(page.getByText("Trang 2/2")).toBeVisible()
  await expect.poll(() => state.requests.at(-1)?.get("page")).toBe("1")
  await page.getByLabel("Tìm chuyên gia").fill("  An  ")
  await expect(page.getByRole("link", { name: "Xem hồ sơ Chuyên gia An" })).toBeVisible()
  await expect.poll(() => state.requests.at(-1)?.get("query")).toBe("An")
  await expect.poll(() => state.requests.at(-1)?.get("page")).toBe("0")
  await page.getByRole("button", { name: "Trang sau" }).click()
  await expect(page.getByText("Trang 2/2")).toBeVisible()
  await page.getByRole("button", { name: "Trang trước" }).click()
  await expect(page.getByText("Trang 1/2")).toBeVisible()
})

test("directory shows distinct empty results for a search and for an empty catalog", async ({ page }) => {
  const state = await setup(page)
  await page.goto("/chuyen-gia")
  await page.getByLabel("Tìm chuyên gia").fill("khong co")
  await expect(page.getByRole("heading", { name: "Chưa tìm thấy chuyên gia" })).toBeVisible()
  await expect(page.getByText("Thử tên khác hoặc xóa từ khóa tìm kiếm.")).toBeVisible()
  state.emptyCatalog = true
  await page.getByLabel("Tìm chuyên gia").fill("")
  await expect(page.getByRole("heading", { name: "Chưa tìm thấy chuyên gia" })).toBeVisible()
  await expect(page.getByText("Danh sách chuyên gia đang được cập nhật.")).toBeVisible()
})

test("directory load failures can be retried without losing the search", async ({ page }) => {
  const state = await setup(page)
  state.failLoad = true
  await page.goto("/chuyen-gia")
  await expect(page.getByRole("heading", { name: "Chưa tải được danh sách" })).toBeVisible()
  await page.getByLabel("Tìm chuyên gia").fill("An")
  await expect(page.getByRole("heading", { name: "Chưa tải được danh sách" })).toBeVisible()
  state.failLoad = false
  await page.getByRole("button", { name: "Thử lại" }).click()
  await expect(page.getByRole("link", { name: "Xem hồ sơ Chuyên gia An" })).toBeVisible()
  expect(state.requests.at(-1)?.get("query")).toBe("An")
})

test("Member can follow and unfollow an expert with the displayed count updated", async ({ page }) => {
  const state = await setup(page, "CUSTOMER")
  await page.goto("/chuyen-gia")
  await page.getByRole("button", { name: "Theo dõi", exact: true }).click()
  await expect(page.getByRole("button", { name: "Đang theo dõi", exact: true })).toBeVisible()
  await expect(page.getByText("1.251 người theo dõi")).toBeVisible()
  await page.getByRole("button", { name: "Đang theo dõi", exact: true }).click()
  await expect(page.getByRole("button", { name: "Theo dõi", exact: true })).toBeVisible()
  await expect(page.getByText("1.250 người theo dõi")).toBeVisible()
  expect(state.followWrites).toEqual(["PUT", "DELETE"])
})

test("failed follow keeps the source state and admin has no follow action", async ({ page }) => {
  const state = await setup(page, "CUSTOMER")
  state.failFollow = true
  await page.goto("/chuyen-gia")
  await page.getByRole("button", { name: "Theo dõi", exact: true }).click()
  await expect(page.getByRole("alert")).toContainText("Trạng thái vẫn theo dữ liệu máy chủ")
  await expect(page.getByRole("button", { name: "Theo dõi", exact: true })).toBeVisible()
  await seedDemoSession(page, "ADMIN")
  await page.goto("/chuyen-gia")
  await expect(page.getByRole("button", { name: "Theo dõi", exact: true })).toHaveCount(0)
  expect(state.followWrites).toEqual(["PUT"])
})
