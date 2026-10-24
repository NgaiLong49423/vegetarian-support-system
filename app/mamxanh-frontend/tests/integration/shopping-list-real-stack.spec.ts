import { randomUUID } from 'node:crypto'
import { execFileSync } from 'node:child_process'
import { existsSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { expect, test } from '@playwright/test'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../../..')
const backend = process.env.MAMXANX_E2E_BACKEND_URL ?? 'http://localhost:8080/api/v1'
const project = process.env.MAMXANX_E2E_COMPOSE_PROJECT ?? 'mamxanh-dev'
const marker = randomUUID()
const email = `fr53-${marker}@example.invalid`, otherEmail = `fr53-other-${marker}@example.invalid`
const password = `Aa1!${randomUUID()}z`
const literal = (value: string) => `N'${value.replaceAll("'", "''")}'`
const week = '2026-10-12'
function sql(query: string): string {
  const envFile = path.join(root, 'app/mamxanh-backend/.env')
  return execFileSync('docker', ['compose', '-p', project, '-f', path.join(root, 'docker-compose.yml'), ...(existsSync(envFile) ? ['--env-file', envFile] : []), 'exec', '-T', 'sqlserver', '/opt/mssql-tools18/bin/sqlcmd', '-S', 'sqlserver', '-U', 'sa', '-C', '-d', 'MamXanhDB', '-h', '-1', '-W', '-b', '-Q', `SET QUOTED_IDENTIFIER ON; SET NOCOUNT ON; ${query}`], { cwd: root, encoding: 'utf8', timeout: 30000 }).trim()
}
// Real credentials and JWT remain in memory; do not retain network traces.
test.use({ trace: 'off' })
test('FR-53 browser creates selected-day and weekly checklists; CRUD and bought state persist in SQL', async ({ page, request }) => {
  test.setTimeout(360000)
  const members = `SELECT user_id FROM [USER] WHERE email IN (${literal(email)}, ${literal(otherEmail)})`
  try {
    for (const address of [email, otherEmail]) {
      const register = await request.post(`${backend}/auth/register`, { data: { displayName: 'Kiểm thử đi chợ', email: address, password, confirmPassword: password } })
      expect(register.status()).toBe(201)
    }
    sql(`UPDATE [USER] SET email_verified=1, onboarding_invited_at=SYSUTCDATETIME() WHERE user_id IN (${members});`)
    const owner = Number(sql(`SELECT user_id FROM [USER] WHERE email=${literal(email)}`))
    const ingredient = Number(sql(`SELECT TOP 1 ingredient_id FROM [INGREDIENT] i WHERE status='ACTIVE' AND NOT EXISTS(SELECT 1 FROM [INGREDIENT_UNIT_CONVERSION] c WHERE c.ingredient_id=i.ingredient_id AND c.is_active=1) ORDER BY ingredient_id;`))
    expect(ingredient).toBeGreaterThan(0)
    const ingredientName = sql(`SELECT name FROM [INGREDIENT] WHERE ingredient_id=${ingredient}`)
    const g = Number(sql("SELECT unit_id FROM [UNIT] WHERE code=N'g'"))
    const ml = Number(sql("SELECT unit_id FROM [UNIT] WHERE code=N'ml'"))
    const roll = Number(sql("SELECT unit_id FROM [UNIT] WHERE code=N'cuộn'"))
    const recipes: number[] = []
    for (let index = 0; index < 3; index++) {
      const title = `FR53 ${marker} ${index}`
      sql(`INSERT INTO [RECIPE_POST](author_id,title,instructions,dish_category,vegetarian_type,difficulty,servings,prep_time_min,cook_time_min,status,published_at) VALUES(${owner},${literal(title)},N'Nấu món kiểm thử.','SOUP','VEGAN','EASY',2,10,15,'PUBLISHED',SYSUTCDATETIME());`)
      recipes.push(Number(sql(`SELECT recipe_id FROM [RECIPE_POST] WHERE title=${literal(title)}`)))
    }
    sql(`INSERT INTO [RECIPE_INGREDIENT](recipe_id,ingredient_id,unit_id,quantity) VALUES(${recipes[0]},${ingredient},${g},600),(${recipes[1]},${ingredient},${g},800),(${recipes[1]},${ingredient},${ml},200);
      INSERT INTO [MEAL_PLAN](user_id,week_start_date) VALUES(${owner},'${week}');
      DECLARE @plan INT=(SELECT meal_plan_id FROM [MEAL_PLAN] WHERE user_id=${owner} AND week_start_date='${week}');
      INSERT INTO [MEAL_PLAN_ENTRY](meal_plan_id,recipe_id,meal_date,meal_type,planned_servings) VALUES(@plan,${recipes[0]},'${week}','LUNCH',2),(@plan,${recipes[1]},'2026-10-13','DINNER',2),(@plan,${recipes[2]},'2026-10-14','LUNCH',2);
      UPDATE [RECIPE_POST] SET status='HIDDEN' WHERE recipe_id=${recipes[2]};`)
    await page.goto('/dang-nhap')
    await page.getByLabel('Email').fill(email)
    await page.getByLabel('Mật khẩu', { exact: true }).fill(password)
    const login = page.waitForResponse((response) => response.url().endsWith('/auth/login') && response.request().method() === 'POST', { timeout: 30000 })
    await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click()
    expect((await login).status()).toBe(200)
    await expect(page).toHaveURL(/trang-chu/, { timeout: 15000 })
    await page.goto(`/ke-hoach?weekStartDate=${week}`)
    await page.getByRole('button', { name: 'Tạo danh sách mua sắm', exact: true }).click()
    await page.getByLabel('Chọn ngày', { exact: true }).check()
    await page.getByRole('checkbox', { name: /Thứ Hai/i }).check()
    const selectedCreationPromise = page.waitForResponse((response) => response.url().endsWith('/shopping-lists/from-meal-plan') && response.request().method() === 'POST')
    await page.getByRole('button', { name: 'Tạo danh sách', exact: true }).click()
    const selectedCreation = await selectedCreationPromise
    expect(selectedCreation.status()).toBe(201)
    const selectedResult = await selectedCreation.json()
    const selectedName = selectedResult.shoppingList.name as string
    expect(selectedName).toMatch(/^Danh sách mua sắm \d{2}\/\d{2}\/\d{4}$/)
    await expect(page).toHaveURL(/di-cho\?listId=\d+/)
    const selectedList = Number(new URL(page.url()).searchParams.get('listId'))
    await expect(page.getByRole('heading', { name: selectedName, exact: true })).toBeVisible()
    expect(sql(`SELECT name FROM [SHOPPING_LIST] WHERE shopping_list_id=${selectedList}`)).toBe(selectedName)
    await expect(page.getByText('600 g', { exact: true })).toBeVisible()
    expect(Number(sql(`SELECT COUNT(*) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=${selectedList}`))).toBe(1)
    await page.goto(`/ke-hoach?weekStartDate=${week}`)
    await page.getByRole('button', { name: 'Tạo danh sách mua sắm', exact: true }).click()
    const creationPromise = page.waitForResponse((response) => response.url().endsWith('/shopping-lists/from-meal-plan') && response.request().method() === 'POST')
    await page.getByRole('button', { name: 'Tạo danh sách', exact: true }).click()
    const creation = await creationPromise
    expect(creation.status()).toBe(201)
    const result = await creation.json(), listId = result.shoppingList.id as number
    expect(result.shoppingList.name).toMatch(/^Danh sách mua sắm \d{2}\/\d{2}\/\d{4}$/)
    await expect(page).toHaveURL(new RegExp(`listId=${listId}$`))
    await expect(page.getByRole('region', { name: 'Lưu ý khi tạo danh sách' })).toBeVisible()
    await expect(page.getByText('1.400 g', { exact: true })).toBeVisible()
    await expect(page.getByText('200 ml', { exact: true })).toBeVisible()
    await expect(page.getByText(/Giữ dòng riêng theo đơn vị/)).toHaveCount(2)
    await page.screenshot({ path: path.join(root, '.agents/outputs/fr53-ui/real-created.png'), fullPage: true })
    expect(sql(`SELECT CAST(quantity AS VARCHAR(20)) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=${listId} AND unit_id=${g}`)).toBe('1400.00')
    await page.getByRole('button', { name: 'Thêm mặt hàng', exact: true }).click()
    await page.getByLabel('Tên mặt hàng').fill('Khăn giấy lau bếp')
    await page.getByLabel('Số lượng', { exact: true }).fill('2')
    await page.getByLabel('Đơn vị', { exact: true }).selectOption(String(roll))
    await page.getByRole('button', { name: 'Lưu mặt hàng' }).click()
    await expect(page.getByText('2 cuộn', { exact: true })).toBeVisible()
    const check = page.getByRole('button', { name: 'Đánh dấu Khăn giấy lau bếp cuộn', exact: true })
    await check.click(); await expect(check).toHaveAttribute('aria-pressed', 'true')
    expect(Number(sql(`SELECT CAST(is_bought AS INT) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=${listId} AND custom_ingredient_name=N'Khăn giấy lau bếp'`))).toBe(1)
    await page.reload(); await expect(check).toHaveAttribute('aria-pressed', 'true')
    await page.getByRole('button', { name: 'Sửa Khăn giấy lau bếp cuộn', exact: true }).click()
    await page.getByLabel('Số lượng', { exact: true }).fill('3')
    await page.getByRole('button', { name: 'Lưu mặt hàng' }).click()
    await expect(page.getByText('3 cuộn', { exact: true })).toBeVisible()
    expect(sql(`SELECT CAST(quantity AS VARCHAR(20)) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=${listId} AND custom_ingredient_name=N'Khăn giấy lau bếp'`)).toBe('3.00')
    await check.click(); await expect(check).toHaveAttribute('aria-pressed', 'false')
    await page.getByRole('button', { name: `Đánh dấu ${ingredientName} g`, exact: true }).click()
    await page.getByRole('button', { name: 'Xoá 1 mục đã mua', exact: true }).click()
    await page.getByRole('button', { name: 'Xoá', exact: true }).click()
    await expect(page.getByText('1.400 g', { exact: true })).toHaveCount(0)
    expect(Number(sql(`SELECT COUNT(*) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=${listId}`))).toBe(2)
    await page.getByRole('button', { name: 'Xoá Khăn giấy lau bếp cuộn', exact: true }).click()
    await page.getByRole('button', { name: 'Xoá', exact: true }).click()
    await expect(check).toHaveCount(0)
    sql(`UPDATE [RECIPE_INGREDIENT] SET quantity=900 WHERE recipe_id=${recipes[1]} AND unit_id=${ml};`)
    await page.reload(); await expect(page.getByText('200 ml', { exact: true })).toBeVisible()
    const otherLogin = await request.post(`${backend}/auth/login`, { data: { email: otherEmail, password } })
    expect(otherLogin.status()).toBe(200)
    const foreign = await request.get(`${backend}/shopping-lists/${listId}`, { headers: { Authorization: `Bearer ${(await otherLogin.json()).accessToken}` } })
    expect(foreign.status()).toBe(404)
    // AC-53.6: add recipes one at a time to a selected/new owned checklist.
    await page.goto(`/kham-pha?q=${encodeURIComponent(marker)}`)
    const recipeZeroTitle = `FR53 ${marker} 0`
    await page.getByRole('button', { name: `Tùy chọn cho công thức ${recipeZeroTitle}` }).click()
    await page.getByRole('menuitem', { name: 'Thêm vào danh sách mua sắm' }).click()
    await page.getByRole('button', { name: 'Tạo danh sách mới' }).click()
    await page.getByLabel('Tên danh sách').fill(`FR53 danh sách ${marker}`)
    const personalResponse = page.waitForResponse(response => response.url().endsWith('/shopping-lists/from-recipe') && response.request().method() === 'POST')
    await page.getByRole('button', { name: 'Tạo danh sách và thêm' }).click()
    const personal = await personalResponse
    expect(personal.status()).toBe(201)
    const personalId = (await personal.json()).shoppingList.id as number
    expect(personalId).not.toBe(listId)
    await expect(page).toHaveURL(new RegExp(`listId=${personalId}$`))
    await expect(page.getByLabel(`Nguồn của ${ingredientName}`).first()).toContainText('600 g lúc thêm')
    expect(sql(`SELECT name FROM [SHOPPING_LIST] WHERE shopping_list_id=${personalId}`)).toBe(`FR53 danh sách ${marker}`)
    await page.goto(`/kham-pha?q=${encodeURIComponent(marker)}`)
    const recipeOneTitle = `FR53 ${marker} 1`
    await page.getByRole('button', { name: `Tùy chọn cho công thức ${recipeOneTitle}` }).click()
    await page.getByRole('menuitem', { name: 'Thêm vào danh sách mua sắm' }).click()
    await page.getByRole('radio', { name: `FR53 danh sách ${marker}` }).check()
    const addSecondResponse = page.waitForResponse(response => response.url().endsWith(`/shopping-lists/${personalId}/recipes`) && response.request().method() === 'POST')
    await page.getByRole('button', { name: 'Thêm vào danh sách', exact: true }).click()
    expect((await addSecondResponse).status()).toBe(200)
    expect(Number(sql(`SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=${personalId}`))).toBe(3)
    expect(sql(`SELECT CAST(quantity AS VARCHAR(20)) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=${personalId} AND unit_id=${g}`)).toBe('1400.00')
    await page.getByRole('button', { name: `Đánh dấu ${ingredientName} g`, exact: true }).click()
    sql(`UPDATE [RECIPE_INGREDIENT] SET quantity=42 WHERE recipe_id=${recipes[0]}; UPDATE [RECIPE_POST] SET status='HIDDEN' WHERE recipe_id=${recipes[0]};`)
    await page.reload()
    await expect(page.getByText('1.400 g', { exact: true })).toBeVisible()
    await expect(page.getByText('Công thức không còn khả dụng', { exact: true })).toBeVisible()
    await expect(page.getByRole('button', { name: `Đánh dấu ${ingredientName} g`, exact: true })).toHaveAttribute('aria-pressed', 'true')
    await page.goto(`/kham-pha?q=${encodeURIComponent(marker)}`)
    await page.getByRole('button', { name: `Tùy chọn cho công thức ${recipeOneTitle}` }).click()
    await page.getByRole('menuitem', { name: 'Thêm vào danh sách mua sắm' }).click()
    await page.getByRole('radio', { name: `FR53 danh sách ${marker}` }).check()
    const duplicateResponse = page.waitForResponse(response => response.url().endsWith(`/shopping-lists/${personalId}/recipes`) && response.request().method() === 'POST')
    await page.getByRole('button', { name: 'Thêm vào danh sách', exact: true }).click()
    expect((await duplicateResponse).status()).toBe(200)
    await expect(page).toHaveURL(new RegExp(`listId=${personalId}$`))
    await expect(page.getByText('1.400 g', { exact: true })).toBeVisible()
    expect(Number(sql(`SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=${personalId}`))).toBe(3)
    expect(sql(`SELECT CAST(quantity AS VARCHAR(20)) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=${personalId} AND recipe_id=${recipes[0]}`)).toBe('600.00')
    const foreignPersonal = await request.get(`${backend}/shopping-lists/${personalId}`, { headers: { Authorization: `Bearer ${(await otherLogin.json()).accessToken}` } })
    expect(foreignPersonal.status()).toBe(404)
    await page.setViewportSize({ width: 375, height: 812 })
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true)
    await page.screenshot({ path: path.join(root, '.agents/outputs/fr53-ui/real-mobile.png'), fullPage: true })
    await page.setViewportSize({ width: 1440, height: 1000 })
    await page.screenshot({ path: path.join(root, '.agents/outputs/fr53-ui/real-desktop.png'), fullPage: true })
  } finally {
    sql(`DELETE FROM [SHOPPING_LIST] WHERE user_id IN (${members}); DELETE FROM [MEAL_PLAN] WHERE user_id IN (${members}); DELETE FROM [RECIPE_POST] WHERE author_id IN (${members}); DELETE FROM [USER] WHERE user_id IN (${members});`)
  }
})
