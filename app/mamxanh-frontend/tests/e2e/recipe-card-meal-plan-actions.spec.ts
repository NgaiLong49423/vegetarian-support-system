import { expect, test } from "./baseFixtures"
import { seedDemoSession } from "./demo-session"
import { localDateKey, mondayOf } from "../../src/components/WeekCalendar"

const formOptions = {
  vegetarianTypes: [
    { code: "VEGAN", label: "Thuần chay" },
    { code: "LACTO", label: "Có sữa" },
    { code: "OVO", label: "Có trứng" },
    { code: "LACTO_OVO", label: "Có trứng và sữa" },
  ],
  dishCategories: [
    { code: "SOUP", label: "Món canh" },
    { code: "BRAISED", label: "Món kho" },
  ],
  difficulties: [{ code: "EASY", label: "Dễ" }],
  units: [],
}

const makeRecipe = (overrides: Record<string, unknown> = {}) => ({
  id: 1701,
  authorId: 22,
  authorName: "Chuyên gia Minh An",
  authorAvatarUrl: "https://example.test/avatar-22.jpg",
  title: "Nấm đùi gà kho tiêu xanh",
  description: "Món kho đậm đà cho bữa cơm gia đình.",
  instructions: "Kho nấm đùi gà với tiêu xanh và nước tương trong 20 phút.",
  dishCategory: "BRAISED",
  vegetarianType: "VEGAN",
  difficulty: "EASY",
  servings: 3,
  prepTimeMin: 10,
  cookTimeMin: 20,
  youtubeUrl: null,
  status: "PUBLISHED",
  media: [
    {
      url: "https://example.test/cover-1701.jpg",
      mimeType: "image/jpeg",
      displayOrder: 0,
      cover: true,
    },
  ],
  ingredients: [{ ingredientId: 1, quantity: 200, unitId: 1 }],
  likes: 19,
  dislikes: 1,
  likePercentage: 95,
  viewCount: 128,
  ...overrides,
})

test.describe("Issue #23 [FR-17] — Hiển thị thẻ món và liên kết lịch ăn", () => {
  test("AC-17.1 & AC-17.5: Thẻ món hiển thị đầy đủ thông tin chuẩn và điều hướng tới trang chi tiết", async ({
    page,
  }) => {
    const item = makeRecipe()
    await page.route("**/api/v1/recipes/form-options", (route) =>
      route.fulfill({ json: formOptions }),
    )
    await page.route("**/api/v1/recipes/discovery?**", (route) =>
      route.fulfill({
        json: {
          items: [item],
          page: 0,
          size: 12,
          hasNext: false,
        },
      }),
    )
    await page.route("**/api/v1/recipes/1701", (route) =>
      route.fulfill({
        json: {
          recipeId: 1701,
          title: item.title,
          description: item.description,
          instructions: item.instructions,
          dishCategory: "BRAISED",
          dishCategoryLabel: "Món kho",
          vegetarianType: "VEGAN",
          vegetarianTypeLabel: "Thuần chay",
          difficulty: "EASY",
          difficultyLabel: "Dễ",
          servings: 3,
          prepTimeMinutes: 10,
          cookTimeMinutes: 20,
          youtubeUrl: null,
          publishedAt: "2026-10-15T08:00:00",
          nutritionComplete: true,
          ingredientsWithoutNutrition: [],
          author: {
            userId: 22,
            displayName: item.authorName,
            avatarUrl: item.authorAvatarUrl,
          },
          statistics: {
            likes: 19,
            dislikes: 1,
            reactionCount: 20,
            likePercentage: 95,
            viewCount: 128,
          },
          nutrition: {
            complete: true,
            ingredientsMissingData: [],
            total: {},
            perServing: {},
          },
          ingredients: [
            {
              ingredientId: 1,
              name: "Nấm đùi gà",
              quantity: 200,
              unitId: 1,
              unitCode: "g",
              unitName: "gram",
            },
          ],
          media: [
            {
              blobUrl: "https://example.test/cover-1701.jpg",
              mimeType: "image/jpeg",
              displayOrder: 0,
              cover: true,
            },
          ],
        },
      }),
    )
    await page.route("**/api/v1/recipes/1701/views", (route) =>
      route.fulfill({ json: { recipeId: 1701, counted: true, viewCount: 129 } }),
    )
    await page.route("**/api/v1/recipes/1701/related", (route) =>
      route.fulfill({ json: { items: [] } }),
    )
    await page.route("**/api/v1/recipes/1701/comments**", (route) =>
      route.fulfill({
        json: {
          content: [],
          page: 0,
          size: 10,
          totalElements: 0,
          totalPages: 0,
          commentCount: 0,
        },
      }),
    )

    await page.goto("/kham-pha")
    const card = page.locator("article").first()
    await expect(card.locator('img[alt="Nấm đùi gà kho tiêu xanh"]')).toHaveAttribute(
      "src",
      "https://example.test/cover-1701.jpg",
    )
    await expect(card.getByRole("heading", { name: item.title })).toBeVisible()
    await expect(card.getByText("Chuyên gia Minh An")).toBeVisible()
    await expect(card.getByText("Thuần chay · Món kho")).toBeVisible()
    await expect(card.getByText("1 nguyên liệu · 30 phút · 3 phần")).toBeVisible()
    await expect(card.getByText("95%")).toBeVisible()
    await expect(card.getByText("128 lượt xem")).toBeVisible()

    // AC-17.5: Click vào thân thẻ món điều hướng chính xác tới trang chi tiết
    await card.getByRole("heading", { name: item.title }).click()
    await expect(page).toHaveURL(/\/cong-thuc\/id\/1701$/)
    await expect(
      page.getByRole("heading", { level: 1, name: item.title }),
    ).toBeVisible()
  })

  test("AC-17.2: Tự động gán hình ảnh minh họa mặc định tương ứng với từng loại ăn chay khi bài không có ảnh", async ({
    page,
  }) => {
    const items = [
      makeRecipe({
        id: 1711,
        title: "Món Thuần Chay Không Ảnh",
        vegetarianType: "VEGAN",
        media: [],
        likePercentage: null,
      }),
      makeRecipe({
        id: 1712,
        title: "Món Chay Có Sữa Không Ảnh",
        vegetarianType: "LACTO",
        media: [],
      }),
      makeRecipe({
        id: 1713,
        title: "Món Chay Có Trứng Không Ảnh",
        vegetarianType: "OVO",
        media: [],
      }),
      makeRecipe({
        id: 1714,
        title: "Món Chay Trứng Sữa Không Ảnh",
        vegetarianType: "LACTO_OVO",
        media: [],
      }),
    ]
    await page.route("**/api/v1/recipes/form-options", (route) =>
      route.fulfill({ json: formOptions }),
    )
    await page.route("**/api/v1/recipes/discovery?**", (route) =>
      route.fulfill({
        json: {
          items,
          page: 0,
          size: 12,
          hasNext: false,
        },
      }),
    )

    await page.goto("/kham-pha")
    await expect(
      page.getByRole("img", { name: "Ảnh mặc định cho món Thuần chay", exact: true }),
    ).toBeVisible()
    await expect(
      page.getByRole("img", { name: "Ảnh mặc định cho món Có sữa", exact: true }),
    ).toBeVisible()
    await expect(
      page.getByRole("img", { name: "Ảnh mặc định cho món Có trứng", exact: true }),
    ).toBeVisible()
    await expect(
      page.getByRole("img", { name: "Ảnh mặc định cho món Có trứng và sữa", exact: true }),
    ).toBeVisible()
    await expect(page.getByTestId("recipe-default-artwork-VEGAN")).toBeVisible()
    await expect(page.getByTestId("recipe-default-artwork-LACTO")).toBeVisible()
    await expect(page.getByTestId("recipe-default-artwork-OVO")).toBeVisible()
    await expect(
      page.getByTestId("recipe-default-artwork-LACTO_OVO"),
    ).toBeVisible()
  })

  test("AC-17.3 & AC-17.4: Member lưu nhanh và thêm nhiều ngày/bữa từ thẻ món; xử lý lỗi và Guest/Admin", async ({
    page,
  }) => {
    const item = makeRecipe()
    await page.route("**/api/v1/recipes/form-options", (route) =>
      route.fulfill({ json: formOptions }),
    )
    await page.route("**/api/v1/recipes/discovery?**", (route) =>
      route.fulfill({
        json: {
          items: [item],
          page: 0,
          size: 12,
          hasNext: false,
        },
      }),
    )

    // 1. Guest mở menu hành động rồi chọn Thêm vào lịch ăn -> yêu cầu đăng nhập (BR-05)
    let mealPlanRequests = 0
    await page.route("**/api/v1/meal-plans/**", (route) => {
      mealPlanRequests += 1
      return route.fulfill({ status: 201, json: {} })
    })

    await page.goto("/kham-pha")
    await page.getByRole("button", { name: `Tùy chọn cho công thức ${item.title}` }).click()
    await page.getByRole("menuitem", { name: "Thêm vào lịch ăn" }).click()
    const loginDialog = page.getByRole("dialog", {
      name: "Đăng nhập để thêm vào lịch ăn",
    })
    await expect(loginDialog).toBeVisible()
    await expect(
      loginDialog.getByRole("link", { name: "Đăng nhập" }),
    ).toHaveAttribute("href", "/dang-nhap")
    await loginDialog.getByRole("button", { name: "Đóng" }).click()
    await expect(loginDialog).toHaveCount(0)
    expect(mealPlanRequests).toBe(0)

    // 2. Member đăng nhập thực hiện Lưu nhanh (AC-17.3) và Thêm vào Bữa tối Thứ Tư (AC-17.4)
    await seedDemoSession(page, "CUSTOMER")
    let savedState = false
    await page.route(
      (url) => url.pathname.startsWith("/api/v1/saved-recipes"),
      (route) => {
        if (route.request().method() === "PUT") {
          savedState = true
          return route.fulfill({
            json: {
              recipeId: item.id,
              title: item.title,
              description: item.description,
              coverUrl: "https://example.test/cover-1701.jpg",
              authorName: item.authorName,
              available: true,
              unavailableMessage: null,
              savedAt: "2026-10-16T10:00:00",
            },
          })
        }
        return route.fulfill({
          json: {
            content: savedState
              ? [
                  {
                    recipeId: item.id,
                    title: item.title,
                    description: item.description,
                    coverUrl: "https://example.test/cover-1701.jpg",
                    authorName: item.authorName,
                    available: true,
                    unavailableMessage: null,
                    savedAt: "2026-10-16T10:00:00",
                  },
                ]
              : [],
            page: 0,
            size: 50,
            totalElements: savedState ? 1 : 0,
            totalPages: savedState ? 1 : 0,
          },
        })
      },
    )

    let failOnce = true
    const capturedEntries: Array<{
      weekStartDate: string
      mealDate: string
      mealType: string
      recipeId: number
    }> = []

    await page.unroute("**/api/v1/meal-plans/**")
    await page.route("**/api/v1/meal-plans/*/entries", async (route) => {
      const url = new URL(route.request().url())
      const weekStartDate = url.pathname.split("/").at(-2) ?? ""
      const body = route.request().postDataJSON() as {
        mealDate: string
        mealType: string
        recipeId: number
      }
      if (failOnce) {
        failOnce = false
        return route.fulfill({
          status: 409,
          contentType: "application/problem+json",
          body: JSON.stringify({
            status: 409,
            code: "MEAL_PLAN_RECIPE_DUPLICATE",
            detail: "Món này đã có trong bữa được chọn.",
          }),
        })
      }
      capturedEntries.push({ weekStartDate, ...body })
      return route.fulfill({
        status: 201,
        json: {
          entryId: 501,
          mealDate: body.mealDate,
          mealType: body.mealType,
          plannedServings: 1,
          recipeId: body.recipeId,
          recipeTitle: item.title,
          recipeCoverUrl: "https://example.test/cover-1701.jpg",
          dishCategory: "BRAISED",
          totalTimeMinutes: 30,
          recipeDeleted: false,
          unavailableMessage: null,
        },
      })
    })

    await page.goto("/kham-pha")

    // AC-17.3: Lưu nhanh không reload trang
    await page.getByRole("button", { name: "Lưu bài viết" }).click()
    await expect(
      page.getByRole("button", { name: "Bỏ lưu" }),
    ).toHaveAttribute("aria-pressed", "true")

    // AC-17.4: Mở giao diện chọn ngày, kiểm tra nút đóng trước
    await page.getByRole("button", { name: `Tùy chọn cho công thức ${item.title}` }).click()
    await page.getByRole("menuitem", { name: "Thêm vào lịch ăn" }).click()
    const planDialog = page.getByRole("dialog", {
      name: "Chọn ngày",
    })
    const clickMealSlot = async (name: RegExp) => {
      const checkbox = planDialog.getByRole("checkbox", { name })
      await checkbox.locator("xpath=..").click()
      await expect(checkbox).toBeChecked()
    }
    await expect(planDialog).toBeVisible()
    await planDialog.getByRole("button", { name: "Đóng" }).click()
    await expect(planDialog).toHaveCount(0)

    // Mở lại, chọn nhiều bữa trong tuần này và tuần kế tiếp
    await page.getByRole("button", { name: `Tùy chọn cho công thức ${item.title}` }).click()
    await page.getByRole("menuitem", { name: "Thêm vào lịch ăn" }).click()
    await expect(planDialog).toBeVisible()
    await clickMealSlot(/Bữa tối, Thứ Tư/)
    await clickMealSlot(/Bữa trưa, Thứ Năm/)
    await planDialog.getByRole("button", { name: "Tuần sau" }).click()
    await clickMealSlot(/Bữa sáng, Thứ Ba/)
    await expect(planDialog.getByRole("button", { name: "Hoàn tất · 3" })).toBeEnabled()
    await planDialog.getByRole("button", { name: "Tuần trước" }).click()
    await expect(planDialog.getByRole("checkbox", { name: /Bữa tối, Thứ Tư/ })).toBeChecked()
    await expect(planDialog.getByRole("checkbox", { name: /Bữa trưa, Thứ Năm/ })).toBeChecked()
    await planDialog.getByRole("button", { name: "Tuần sau" }).click()
    await expect(planDialog.getByRole("checkbox", { name: /Bữa sáng, Thứ Ba/ })).toBeChecked()
    await planDialog.getByRole("button", { name: "Tuần trước" }).click()

    // Lần 1: giả lập lỗi 409 để kiểm chứng hiển thị lỗi
    await planDialog.getByRole("button", { name: "Hoàn tất" }).click()
    await expect(planDialog.getByRole("alert")).toContainText("Đã thêm 2 bữa ăn.")
    await expect(planDialog.getByRole("alert")).toContainText("Món này đã có trong bữa được chọn.")
    await expect(planDialog.getByRole("checkbox", { name: /Bữa tối, Thứ Tư/ })).toBeChecked()
    await expect(planDialog.getByRole("checkbox", { name: /Bữa trưa, Thứ Năm/ })).not.toBeChecked()

    // Lần 2: xác nhận thành công
    await planDialog.getByRole("button", { name: "Hoàn tất" }).click()
    await expect(planDialog).toHaveCount(0)
    await expect(page.getByRole("status")).toContainText("Đã thêm 3 bữa ăn vào Lịch ăn.")

    const expectedMonday = mondayOf(new Date())
    const expectedWednesdayDate = new Date(`${expectedMonday}T12:00:00`)
    expectedWednesdayDate.setDate(expectedWednesdayDate.getDate() + 2)
    const expectedWednesday = localDateKey(expectedWednesdayDate)
    const expectedThursdayDate = new Date(`${expectedMonday}T12:00:00`)
    expectedThursdayDate.setDate(expectedThursdayDate.getDate() + 3)
    const expectedThursday = localDateKey(expectedThursdayDate)
    const expectedNextMondayDate = new Date(`${expectedMonday}T12:00:00`)
    expectedNextMondayDate.setDate(expectedNextMondayDate.getDate() + 7)
    const expectedNextMonday = localDateKey(expectedNextMondayDate)
    const expectedNextTuesdayDate = new Date(`${expectedNextMonday}T12:00:00`)
    expectedNextTuesdayDate.setDate(expectedNextTuesdayDate.getDate() + 1)
    const expectedNextTuesday = localDateKey(expectedNextTuesdayDate)

    expect(capturedEntries).toEqual([
      {
        weekStartDate: expectedMonday,
        mealDate: expectedThursday,
        mealType: "LUNCH",
        recipeId: 1701,
      },
      {
        weekStartDate: expectedNextMonday,
        mealDate: expectedNextTuesday,
        mealType: "BREAKFAST",
        recipeId: 1701,
      },
      {
        weekStartDate: expectedMonday,
        mealDate: expectedWednesday,
        mealType: "DINNER",
        recipeId: 1701,
      },
    ])

    // 3. Admin không hiển thị nút thao tác cá nhân trên thẻ món
    await seedDemoSession(page, "ADMIN")
    await page.goto("/kham-pha")
    await expect(page.getByRole("heading", { name: item.title })).toBeVisible()
    await expect(
      page.getByRole("button", { name: `Tùy chọn cho công thức ${item.title}` }),
    ).toHaveCount(0)
    await expect(
      page.getByRole("button", { name: "Lưu bài viết" }),
    ).toHaveCount(0)
  })
})
