import { expect, test } from '@playwright/test'

test('theme can be changed from the menu and persists after reload', async ({ page }) => {
  await page.addInitScript(() => {
    if (sessionStorage.getItem('theme-test-initialized') !== 'true') {
      localStorage.removeItem('mamxanh.theme')
      sessionStorage.setItem('theme-test-initialized', 'true')
    }
  })
  await page.goto('/')

  const root = page.locator('html')
  await expect(root).toHaveAttribute('data-theme', 'dark')
  await page.getByRole('button', { name: 'Menu' }).click()

  const menu = page.getByRole('dialog', { name: 'Menu Mâm Xanh' })
  const themeOptions = menu.getByRole('group', { name: 'Chủ đề màu' })
  await themeOptions.getByRole('button', { name: 'Sáng' }).click()
  await expect(root).toHaveAttribute('data-theme', 'light')
  await expect(themeOptions.getByRole('button', { name: 'Sáng' })).toHaveAttribute('aria-pressed', 'true')
  expect(await root.evaluate((element) => getComputedStyle(element).getPropertyValue('--color-cream').trim())).toBe('#f7f5f0')

  await page.reload()
  await expect(root).toHaveAttribute('data-theme', 'light')
  await page.getByRole('button', { name: 'Menu' }).click()
  const reloadedOptions = page.getByRole('dialog', { name: 'Menu Mâm Xanh' }).getByRole('group', { name: 'Chủ đề màu' })
  await reloadedOptions.getByRole('button', { name: 'Tối' }).click()
  await expect(root).toHaveAttribute('data-theme', 'dark')
  await expect(reloadedOptions.getByRole('button', { name: 'Tối' })).toHaveAttribute('aria-pressed', 'true')
})
