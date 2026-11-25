import type { Page } from '@playwright/test'
import { expect, test } from './baseFixtures'
import { seedDemoSession } from './demo-session'

const firstExpert = {
  id: 42,
  displayName: 'Nguyễn An',
  email: 'an@example.test',
  role: 'EXPERT',
  accountStatus: 'ACTIVE',
  joinedAt: '2026-10-20T08:00:00',
  expertSince: '2026-10-21T08:00:00',
  publishedRecipeCount: 13,
}

const secondExpert = {
  ...firstExpert,
  id: 43,
  displayName: 'Trần Bình',
  email: 'binh@example.test',
  accountStatus: 'LOCKED',
  expertSince: null,
  publishedRecipeCount: 0,
}

const accountDetail = {
  account: firstExpert,
  avatarUrl: null,
  bio: null,
  approvedApplication: null,
}

const recipe = (id: number, title: string) => ({
  id,
  authorId: firstExpert.id,
  authorName: firstExpert.displayName,
  authorAvatarUrl: null,
  title,
  description: null,
  instructions: 'Sơ chế nguyên liệu rồi nấu chín.',
  dishCategory: 'SIDE_DISH',
  vegetarianType: 'VEGAN',
  difficulty: 'EASY',
  servings: 2,
  prepTimeMin: 10,
  cookTimeMin: 15,
  youtubeUrl: null,
  status: 'PUBLISHED',
  media: [],
  ingredients: [],
  likes: 0,
  dislikes: 0,
  likePercentage: null,
  viewCount: id * 3,
})

async function mockAdminExpertApi(page: Page, options: {
  detailStatus?: number
  detail?: typeof accountDetail
  recipeFailureOnce?: boolean
  recipePages?: Array<ReturnType<typeof recipe>[]>
} = {}) {
  const listRequests: URL[] = []
  const recipeRequests: URL[] = []
  let failNextList = false
  let recipesFailed = false

  await page.route('**/api/v1/admin/users**', async (route) => {
    const url = new URL(route.request().url())
    const path = url.pathname.replace(/^.*\/api\/v1\/admin\/users/, '')
    if (route.request().method() !== 'GET') {
      return route.fulfill({ status: 404, json: { status: 404, code: 'NOT_FOUND' } })
    }
    if (path === '/summary') {
      return route.fulfill({ json: {
        totalUsers: 20,
        activeUsers: 17,
        expertUsers: 2,
        lockedUsers: 1,
        activeExperts: 1,
        lockedExperts: 1,
        expertPublishedRecipeCount: 13,
      } })
    }
    if (path === '') {
      listRequests.push(url)
      if (failNextList) {
        failNextList = false
        return route.fulfill({ status: 503, json: {
          status: 503,
          code: 'SERVICE_UNAVAILABLE',
          detail: 'Dịch vụ quản lý Chuyên gia đang tạm thời gián đoạn.',
        } })
      }
      const requestedPage = Number(url.searchParams.get('page') ?? 0)
      const content = requestedPage === 0 ? [firstExpert] : [secondExpert]
      return route.fulfill({ json: {
        content,
        page: requestedPage,
        size: 20,
        totalElements: 21,
        totalPages: 2,
      } })
    }
    return route.fulfill({ status: 404, json: { status: 404, code: 'NOT_FOUND' } })
  })

  await page.route('**/api/v1/admin/experts/*', async (route) => {
    if (options.detailStatus) {
      return route.fulfill({ status: options.detailStatus, json: {
        status: options.detailStatus,
        code: 'EXPERT_NOT_FOUND',
        detail: 'Không tìm thấy tài khoản Chuyên gia.',
      } })
    }
    return route.fulfill({ json: options.detail ?? accountDetail })
  })

  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({ json: {
    dishCategories: [{ code: 'SIDE_DISH', label: 'Món phụ' }],
    vegetarianTypes: [{ code: 'VEGAN', label: 'Thuần chay' }],
    difficulties: [],
    units: [],
  } }))

  await page.route('**/api/v1/members/*/recipes**', async (route) => {
    const url = new URL(route.request().url())
    recipeRequests.push(url)
    if (options.recipeFailureOnce && !recipesFailed) {
      recipesFailed = true
      return route.fulfill({ status: 503, json: {
        status: 503,
        code: 'SERVICE_UNAVAILABLE',
        detail: 'Chưa tải được công thức công khai.',
      } })
    }
    const requestedPage = Number(url.searchParams.get('page') ?? 0)
    const pages = options.recipePages ?? [[recipe(301, 'Đậu hũ sốt cà chua')]]
    return route.fulfill({ json: {
      items: pages[requestedPage] ?? [],
      page: requestedPage,
      size: 12,
      totalElements: pages.reduce((total, items) => total + items.length, 0),
      totalPages: pages.length,
    } })
  })

  return {
    listRequests,
    recipeRequests,
    failNextListRequest: () => { failNextList = true },
  }
}

test.beforeEach(async ({ page }) => {
  await seedDemoSession(page, 'ADMIN')
})

test('loads the expert directory and changes pages', async ({ page }) => {
  const { listRequests } = await mockAdminExpertApi(page)
  await page.goto('/admin/chuyen-gia')

  await expect(page.getByRole('heading', { name: 'Danh sách Chuyên gia' })).toBeVisible({ timeout: 15_000 })
  await expect(page.getByText('Nguyễn An', { exact: true })).toBeVisible()
  await expect(page.getByText('13 món', { exact: true })).toBeVisible()
  await expect(page.getByText('21 chuyên gia')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Trước' })).toBeDisabled()

  await page.getByRole('button', { name: 'Sau' }).click()
  await expect(page.getByText('Trần Bình', { exact: true })).toBeVisible()
  await expect.poll(() => listRequests.some((url) => url.searchParams.get('page') === '1')).toBe(true)
  await page.getByRole('button', { name: 'Trước' }).click()
  await expect(page.getByText('Nguyễn An', { exact: true })).toBeVisible()
})

test('shows list error and retries the request', async ({ page }) => {
  const { listRequests, failNextListRequest } = await mockAdminExpertApi(page)
  await page.goto('/admin/chuyen-gia')
  await expect(page.getByText('Nguyễn An', { exact: true })).toBeVisible()
  failNextListRequest()
  await page.getByRole('combobox', { name: 'Lọc trạng thái' }).selectOption('ACTIVE')

  await expect(page.getByRole('alert')).toContainText('Dịch vụ quản lý Chuyên gia đang tạm thời gián đoạn.')
  await page.getByRole('button', { name: 'Thử lại' }).click()
  await expect(page.getByText('Nguyễn An', { exact: true })).toBeVisible()
  expect(listRequests).toHaveLength(3)
})

test('opens the profile, shows no approved application, and pages through public recipes', async ({ page }) => {
  const { recipeRequests } = await mockAdminExpertApi(page, {
    recipePages: [
      [recipe(301, 'Đậu hũ sốt cà chua')],
      [recipe(302, 'Canh rau củ')],
    ],
  })
  await page.goto('/admin/chuyen-gia')
  await page.getByRole('button', { name: 'Xem hồ sơ Chuyên gia' }).click()

  await expect(page).toHaveURL(/\/admin\/chuyen-gia\/42$/)
  await expect(page.getByRole('heading', { name: 'Hồ sơ Chuyên gia' })).toBeVisible()
  await expect(page.getByText('Không có đơn đã duyệt gắn với tài khoản này.', { exact: false })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Mở công thức Đậu hũ sốt cà chua trong tab mới' })).toBeVisible()
  await expect(page.getByText('Trang 1/2')).toBeVisible()

  await page.getByRole('button', { name: 'Sau' }).last().click()
  await expect(page.getByRole('link', { name: 'Mở công thức Canh rau củ trong tab mới' })).toBeVisible()
  await expect.poll(() => recipeRequests.some((url) => url.searchParams.get('page') === '1')).toBe(true)
})

test('shows profile 404 and returns to the expert directory', async ({ page }) => {
  await mockAdminExpertApi(page, { detailStatus: 404 })
  await page.goto('/admin/chuyen-gia/999')

  await expect(page.getByRole('alert')).toContainText('Không tìm thấy tài khoản Chuyên gia.')
  await page.getByRole('button', { name: 'Quay lại danh sách' }).click()
  await expect(page).toHaveURL(/\/admin\/chuyen-gia$/)
  await expect(page.getByRole('heading', { name: 'Danh sách Chuyên gia' })).toBeVisible()
})
