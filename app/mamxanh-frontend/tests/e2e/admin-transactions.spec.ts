import { expect, test } from "./baseFixtures"

import { seedDemoSession } from "./demo-session"

import type {
  AdminTransaction,
  TransactionReport,
} from "../../src/api/adminTransactions"

const tx: AdminTransaction = {
  id: 101,
  orderCode: "12345678901234",
  userId: 8,
  customerName: "Khách Nguyễn",
  customerEmail: "nguyen@example.invalid",

  tier: "PRO",
  amountVnd: 99000,
  status: "PAID",
  createdAt: "2026-10-28T17:10:00Z",
  paidAt: "2026-10-28T17:11:00Z",

  subscriptionId: 7,
  subscriptionStartsAt: "2026-10-28T17:11:00Z",
  subscriptionEndsAt: "2026-11-27T17:11:00Z",
  subscriptionStatus: "ACTIVE",
}

const report = (
  rows: AdminTransaction[] = [tx],
  page = 0,
  total = rows.length,
): TransactionReport => ({
  transactions: {
    content: rows,
    page,
    size: 20,
    totalElements: total,
    totalPages: Math.ceil(total / 20),
  },

  summary: {
    total,
    paid: total,
    pending: 0,
    cancelled: 0,
    failed: 0,
    revenueVnd: total * 99000,
  },
})

test.beforeEach(async ({ page }) => {
  await seedDemoSession(page, "ADMIN")

  await page.route("**/api/v1/admin/**", (route) => route.fulfill({ json: {} }))
})

test("real-data page applies server filters, pages, resets, refreshes and exports the applied filters", async ({
  page,
}) => {
  const queries: URLSearchParams[] = []

  let exportQuery = ""

  await page.route("**/api/v1/admin/transactions**", (route) => {
    const url = new URL(route.request().url())

    if (url.pathname.endsWith("/export")) {
      exportQuery = url.search
      return route.fulfill({
        contentType: "text/csv",
        body: "\uFEFFMã giao dịch\r\nTX-101",
      })
    }

    const p = Number(url.searchParams.get("page") ?? 0)
    queries.push(url.searchParams)

    return route.fulfill({ json: report([{ ...tx, id: 101 + p }], p, 21) })
  })

  await page.goto("/admin/giao-dich")

  await expect(
    page.getByRole("heading", {
      name: "Giám sát giao dịch gói AI",
      exact: true,
    }),
  ).toBeVisible()

  await expect(
    page.getByText("Giao diện minh họa", { exact: false }),
  ).toHaveCount(0)

  await expect(
    page.getByRole("row").filter({ hasText: "TX-101" }),
  ).toContainText("Thành công")
  await expect(
    page.getByRole("row").filter({ hasText: "TX-101" }),
  ).toContainText("29/10/2026")
  await expect(
    page.getByRole("row").filter({ hasText: "TX-101" }),
  ).toContainText("00:10")

  await page.getByRole("button", { name: "Trang sau" }).click()

  await expect(
    page.getByRole("row").filter({ hasText: "TX-102" }),
  ).toBeVisible()

  await page.getByRole("textbox", { name: "Tìm giao dịch" }).fill("nguyen")

  await page.getByLabel("Gói dịch vụ", { exact: true }).selectOption("PRO")

  await page.getByLabel("Trạng thái", { exact: true }).selectOption("PAID")

  await page.getByLabel("Từ ngày tạo").fill("2026-09-01")

  await page.getByLabel("Đến ngày tạo").fill("2026-09-30")

  await page.getByRole("button", { name: "Áp dụng bộ lọc" }).click()

  await expect.poll(() => queries.at(-1)?.get("query")).toBe("nguyen")

  expect(queries.at(-1)?.get("page")).toBe("0")
  expect(queries.at(-1)?.get("tier")).toBe("PRO")

  expect(queries.at(-1)?.get("status")).toBe("PAID")
  expect(queries.at(-1)?.get("from")).toBe("2026-09-01")

  await expect(
    page.getByRole("row").filter({ hasText: "TX-101" }),
  ).toBeVisible()

  await page
    .getByRole("textbox", { name: "Tìm giao dịch" })
    .fill("chưa áp dụng")

  const download = page.waitForEvent("download")

  await page.getByRole("button", { name: "Xuất đối soát CSV" }).click()

  expect((await download).suggestedFilename()).toBe("MamXanh_giao_dich.csv")

  expect(new URLSearchParams(exportQuery).get("query")).toBe("nguyen")

  await page.getByRole("button", { name: "Xóa bộ lọc" }).click()

  await expect.poll(() => queries.at(-1)?.has("query")).toBe(false)

  const count = queries.length

  await page.getByRole("button", { name: "Làm mới", exact: true }).click()

  await expect.poll(() => queries.length).toBe(count + 1)
})

test("detail uses its API, handles failed loading, supports keyboard close and restores focus", async ({
  page,
}) => {
  let fails = true

  await page.route("**/api/v1/admin/transactions**", (route) => {
    if (new URL(route.request().url()).pathname.endsWith("/101"))
      return fails
        ? route.fulfill({
            status: 503,
            json: {
              code: "INTERNAL_ERROR",
              detail: "Không tải được chi tiết.",
              status: 503,
            },
          })
        : route.fulfill({ json: tx })

    return route.fulfill({ json: report() })
  })

  await page.goto("/admin/giao-dich")

  const open = page.getByRole("button", { name: "Xem chi tiết TX-101" })

  await open.click()

  await expect(page.getByRole("dialog")).toBeVisible()

  await expect(page.getByRole("alert")).toContainText(
    "Không tải được chi tiết.",
  )

  fails = false

  await page.getByRole("button", { name: "Tải lại chi tiết" }).click()

  await expect(page.getByRole("dialog")).toContainText(tx.orderCode)

  await expect(page.getByRole("dialog")).toContainText(
    "không xác nhận gói hiện tại còn hiệu lực",
  )

  await page.keyboard.press("Escape")

  await expect(page.getByRole("dialog")).not.toBeVisible()

  await expect(open).toBeFocused()

  await open.click()
  await page.getByRole("button", { name: "Đóng", exact: true }).click()

  await expect(page.getByRole("dialog")).not.toBeVisible()
})

test("errors retry without sample fallback, empty data and invalid dates remain clear", async ({
  page,
}) => {
  let fails = true

  await page.route("**/api/v1/admin/transactions**", (route) =>
    fails
      ? route.fulfill({
          status: 503,
          json: {
            code: "INTERNAL_ERROR",
            detail: "Dịch vụ giao dịch tạm lỗi.",
            status: 503,
          },
        })
      : route.fulfill({ json: report([]) }),
  )

  await page.goto("/admin/giao-dich")

  await expect(page.getByRole("alert")).toContainText(
    "Dịch vụ giao dịch tạm lỗi.",
  )

  await expect(
    page.getByRole("button", { name: "Xuất đối soát CSV" }),
  ).toBeDisabled()

  fails = false
  await page.getByRole("button", { name: "Thử lại", exact: true }).click()

  await expect(
    page.getByText("Không có giao dịch phù hợp.", { exact: false }),
  ).toBeVisible()

  await page.getByLabel("Từ ngày tạo").fill("2026-09-30")
  await page.getByLabel("Đến ngày tạo").fill("2026-09-01")

  await page.getByRole("button", { name: "Áp dụng bộ lọc" }).click()

  await expect(page.getByRole("alert")).toContainText(
    "Ngày bắt đầu không được sau ngày kết thúc.",
  )
})

test("mobile table stays inside its scrolling region and unknown tier or absent period is honest", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 })

  await page.route("**/api/v1/admin/transactions**", (route) =>
    route.fulfill({
      json: new URL(route.request().url()).pathname.endsWith("/101")
        ? {
            ...tx,
            tier: null,
            paidAt: null,
            subscriptionId: null,
            subscriptionStartsAt: null,
            subscriptionEndsAt: null,
            status: "PENDING",
          }
        : report([{ ...tx, tier: null, status: "CANCELLED" }]),
    }),
  )

  await page.goto("/admin/giao-dich")

  await expect(
    page.getByRole("row").filter({ hasText: "TX-101" }),
  ).toContainText("Chưa xác định")

  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth + 1,
    ),
  ).toBe(true)

  await page.getByRole("button", { name: "Xem chi tiết TX-101" }).click()

  await expect(page.getByRole("dialog")).toContainText("Chờ thanh toán")

  await expect(page.getByRole("dialog")).toContainText("Chưa có")

  await page.getByRole("button", { name: "Đóng chi tiết giao dịch" }).click()
})

test("export errors retain the report and show the server reason", async ({
  page,
}) => {
  await page.route("**/api/v1/admin/transactions**", (route) =>
    new URL(route.request().url()).pathname.endsWith("/export")
      ? route.fulfill({
          status: 400,
          contentType: "application/problem+json",
          json: {
            code: "VALIDATION_FAILED",
            status: 400,
            detail: "Tối đa 10.000 giao dịch mỗi lần xuất.",
          },
        })
      : route.fulfill({ json: report() }),
  )

  await page.goto("/admin/giao-dich")
  await page.getByRole("button", { name: "Xuất đối soát CSV" }).click()

  await expect(page.getByRole("alert")).toContainText("Tối đa 10.000")

  await expect(
    page.getByRole("row").filter({ hasText: "TX-101" }),
  ).toBeVisible()
})
