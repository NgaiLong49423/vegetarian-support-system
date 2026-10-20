import { expect, test } from "./baseFixtures"
import { seedDemoSession } from "./demo-session"

test("public landing introduces the agreed product without fetching account or recipe data", async ({
  page,
}) => {
  const requests: string[] = []
  page.on("request", (request) => {
    if (request.url().includes("/api/v1/")) requests.push(request.url())
  })
  await page.goto("/")
  await expect(page.getByRole("heading", { level: 1 })).toHaveText(
    "Ăn chay theocách của bạn.",
  )
  await expect(page).toHaveTitle("Mâm Xanh — Ăn chay theo cách của bạn")
  await expect(page.locator("html")).toHaveAttribute("lang", "vi")
  await expect(page.locator('meta[name="description"]')).toHaveAttribute(
    "content",
    /lịch ăn.*đi chợ.*AI/,
  )
  for (const id of [
    "cong-thuc",
    "lich-an",
    "di-cho",
    "dinh-duong",
    "tro-ly-ai",
  ])
    await expect(page.locator(`#${id}`)).toBeVisible()
  await expect(page.getByText("Đang phát triển", { exact: true })).toHaveCount(
    0,
  )
  await expect(page.getByRole("group", { name: "Chủ đề màu" })).toHaveCount(0)
  expect(requests).toEqual([])
  await page
    .getByRole("link", { name: "Tạo bộ sưu tập của bạn", exact: true })
    .first()
    .click()
  await expect(page).toHaveURL(/\/dang-ky$/)
})

test("Guest can start from Home without signing in from every primary landing entry", async ({
  page,
}) => {
  await page.goto("/")
  await expect(
    page.getByRole("link", { name: "Bắt đầu khám phá", exact: true }),
  ).toHaveCount(2)
  for (const link of await page
    .getByRole("link", { name: "Bắt đầu khám phá", exact: true })
    .all()) {
    await expect(link).toHaveAttribute("href", "/trang-chu")
  }
  await page.getByRole("link", { name: "Bắt đầu", exact: true }).click()
  await expect(page).toHaveURL(/\/trang-chu$/)
  await expect(
    page.getByRole("heading", { name: "Hôm nay bạn muốn nấu gì?" }),
  ).toBeVisible()
  expect(
    await page.evaluate(() => sessionStorage.getItem("mamxanh.auth")),
  ).toBeNull()
  await expect(
    page.getByRole("link", { name: "Đăng nhập", exact: true }).first(),
  ).toBeVisible()
})

test("landing illustrations load local food assets without stale application screenshots or interactive controls", async ({
  page,
}) => {
  await page.goto("/")
  const previews = page.locator('.landing-preview')
  await expect(previews).toHaveCount(7)
  await expect(previews.locator('a, button, input, select, textarea')).toHaveCount(0)
  const images = previews.locator('img')
  expect(await images.count()).toBeGreaterThan(0)
  for (const image of await images.all()) {
    await image.scrollIntoViewIfNeeded()
    await expect(image).toHaveAttribute("src", /^\/assets\/[^/]+\.png$/)
    await expect(image).toHaveAttribute('alt', '')
    await expect
      .poll(() =>
        image.evaluate((element) => (element as HTMLImageElement).naturalWidth),
      )
      .toBeGreaterThan(0)
  }
  await expect(page.locator('.landing-app-preview')).toHaveCount(0)
  await expect(page.locator('img[src^="/landing/"]')).toHaveCount(0)
})

for (const role of ["CUSTOMER", "EXPERT", "ADMIN"] as const) {
  test(`landing remains public for ${role} and opens the existing application`, async ({
    page,
  }) => {
    await seedDemoSession(page, role)
    const requests: string[] = []
    page.on("request", (request) => {
      if (request.url().includes("/api/v1/")) requests.push(request.url())
    })
    await page.goto("/")
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible()
    await expect(
      page.getByRole("link", { name: "Tạo tài khoản", exact: true }),
    ).toHaveCount(0)
    expect(requests).toEqual([])
    await expect(
      page
        .getByRole("link", {
          name: role === "ADMIN" ? "Vào ứng dụng" : "Xem công thức đã lưu",
          exact: true,
        })
        .first(),
    ).toHaveAttribute("href", role === "ADMIN" ? "/trang-chu" : "/ho-so")
    await page
      .getByRole("link", { name: "Vào ứng dụng", exact: true })
      .first()
      .click()
    await expect(page).toHaveURL(/\/trang-chu$/)
    await expect(
      page.getByRole("heading", { name: "Hôm nay bạn muốn nấu gì?" }),
    ).toBeVisible()
    await expect(
      page.getByRole("link", { name: "Mâm Xanh · Trang chủ" }).first(),
    ).toHaveAttribute("href", "/trang-chu")
  })
}

for (const savedTheme of ["dark", "light"]) {
  test(`landing appearance is fixed when saved application theme is ${savedTheme}`, async ({
    page,
  }) => {
    await page.addInitScript(
      (theme) => localStorage.setItem("mamxanh.theme", theme),
      savedTheme,
    )
    await page.goto("/")
    expect(
      await page
        .locator(".landing-page")
        .evaluate((element) => getComputedStyle(element).backgroundColor),
    ).toBe("rgb(246, 244, 239)")
    expect(
      await page.evaluate(() => localStorage.getItem("mamxanh.theme")),
    ).toBe(savedTheme)
    await page
      .getByRole("link", { name: "Khám phá công thức", exact: true })
      .first()
      .click()
    await expect(page).toHaveURL(/\/kham-pha$/)
    await expect(page.locator("html")).toHaveAttribute("data-theme", savedTheme)
    await expect(page).toHaveTitle("Mâm Xanh - Ẩm Thực Thuần Lành")
  })
}

for (const width of [360, 768, 1440]) {
  test(`landing fits ${width}px and its section navigation reaches the intended content`, async ({
    page,
  }, testInfo) => {
    await page.setViewportSize({ width, height: 900 })
    await page.goto("/")
    expect(
      await page.evaluate(() => document.documentElement.scrollWidth),
    ).toBeLessThanOrEqual(width)
    await page
      .getByRole('navigation', { name: 'Liên kết cuối trang giới thiệu' })
      .getByRole('link', { name: 'Lịch ăn', exact: true })
      .click()
    await expect(page).toHaveURL(/\/#lich-an$/)
    await expect(page.locator("#lich-an")).toBeInViewport()
    await page.emulateMedia({ reducedMotion: 'reduce' })
    await page.goto("/")
    for (const image of await page.locator(".landing-preview img").all()) {
      await image.scrollIntoViewIfNeeded()
      await expect
        .poll(() =>
          image.evaluate((element) => (element as HTMLImageElement).naturalWidth),
        )
        .toBeGreaterThan(0)
      await image.evaluate((element) => (element as HTMLImageElement).decode())
    }
    await page.evaluate(() => window.scrollTo({ top: 0, behavior: "instant" }))
    await page.screenshot({
      path: testInfo.outputPath(`landing-${width}.png`),
      fullPage: true,
    })
  })
}

test("keyboard users can skip navigation and public login and collection links have valid destinations", async ({
  page,
}) => {
  await page.goto("/")
  await page.keyboard.press("Tab")
  await expect(
    page.getByRole("link", { name: "Đi tới nội dung chính" }),
  ).toBeFocused()
  await page.keyboard.press("Enter")
  await expect(page.locator("#landing-main")).toBeFocused()
  await expect(
    page.getByRole("link", { name: "Tạo bộ sưu tập của bạn" }),
  ).toHaveAttribute("href", "/dang-ky")
  await page
    .getByRole("link", { name: "Đăng nhập", exact: true })
    .first()
    .click()
  await expect(page).toHaveURL(/\/dang-nhap$/)
})

test('landing scroll reveals sections and replays when returning to a section', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'no-preference' })
  await page.goto('/')
  const root = page.locator('.landing-page')
  await expect(root).toHaveClass(/has-scroll-motion/)
  const target = page.locator('#dinh-duong [data-reveal]').first()
  await expect(target).not.toHaveClass(/is-visible/)
  await target.scrollIntoViewIfNeeded()
  await expect(target).toHaveClass(/is-visible/)
  await expect(target).toHaveCSS('opacity', '1')
  await page.evaluate(() => window.scrollTo({ top: 0, behavior: 'instant' }))
  await expect(target).not.toHaveClass(/is-visible/)
  await target.scrollIntoViewIfNeeded()
  await expect(target).toHaveClass(/is-visible/)
  await expect(target).toHaveCSS('opacity', '1')
})

for (const mode of ['reduced-motion', 'no-observer'] as const) {
  test(`landing content remains readable with ${mode}`, async ({ page }) => {
    if (mode === 'reduced-motion') await page.emulateMedia({ reducedMotion: 'reduce' })
    else await page.addInitScript(() => { Object.defineProperty(window, 'IntersectionObserver', { value: undefined }) })
    await page.goto('/')
    await expect(page.locator('.landing-page')).not.toHaveClass(/has-scroll-motion/)
    for (const section of await page.locator('[data-reveal]').all()) {
      await expect(section).toHaveClass(/is-visible/)
      await expect(section).toHaveCSS('opacity', '1')
      await expect(section).toHaveCSS('transform', 'none')
    }
  })
}

test("unknown routes still display the original application Home", async ({
  page,
}) => {
  await page.goto("/unknown-existing-fallback")
  await expect(
    page.getByRole("heading", { name: "Hôm nay bạn muốn nấu gì?" }),
  ).toBeVisible()
})
