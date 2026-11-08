import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

const mockAiConfig = {
  plans: [
    { tier: 'FREE' as const, priceVnd: 0, tokenQuota: 10000, pendingTokenQuota: 12000, pendingEffectiveAt: '2026-12-01T00:00:00' },
    { tier: 'PLUS' as const, priceVnd: 49000, tokenQuota: 50000, pendingTokenQuota: null, pendingEffectiveAt: null },
    { tier: 'PRO' as const, priceVnd: 99000, tokenQuota: 120000, pendingTokenQuota: null, pendingEffectiveAt: null },
  ],
  models: [
    { modelId: 'gemini-2.0-flash', displayName: 'Gemini 2.0 Flash', enabled: true, freeAllowed: true, freeDefault: true, plusAllowed: true, plusDefault: true, proAllowed: true, proDefault: false },
    { modelId: 'gemini-1.5-pro', displayName: 'Gemini 1.5 Pro', enabled: true, freeAllowed: false, freeDefault: false, plusAllowed: true, plusDefault: false, proAllowed: true, proDefault: true },
    { modelId: 'gemini-1.5-flash', displayName: 'Gemini 1.5 Flash', enabled: false, freeAllowed: false, freeDefault: false, plusAllowed: false, plusDefault: false, proAllowed: false, proDefault: false },
  ],
  credit: {
    creditsPer1000Vnd: 10,
    creditsPer1000OverageTokens: 2,
    minimumTopUpVnd: 20000,
    maximumTopUpVnd: 2000000,
  },
};

test.beforeEach(async ({ page }) => {
  await seedDemoSession(page, 'ADMIN');
});

test('overview renders all AI configuration cards and summary numbers', async ({ page }) => {
  let loadCount = 0;
  await page.route('**/api/v1/admin/ai-config', (route) => {
    loadCount++;
    return route.fulfill({ json: mockAiConfig });
  });

  await page.goto('/admin/ai-config');

  await expect(page.getByRole('heading', { level: 1, name: 'Cấu hình AI' })).toBeVisible();
  await expect(page.getByText('Gói và hạn mức token', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('Gemini model catalog', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('AI Credit và nạp tiền', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('Free sắp đổi thành 12.000 token')).toBeVisible();

  // Test reload button
  const reloadButton = page.getByRole('button', { name: 'Tải lại' });
  await reloadButton.click();
  await expect.poll(() => loadCount).toBeGreaterThanOrEqual(2);

  // Test overview card navigation
  await page.getByRole('link', { name: /Gói và hạn mức token/ }).first().click();
  await expect(page.getByRole('heading', { level: 1, name: 'Gói và hạn mức token' })).toBeVisible();
  await page.getByRole('link', { name: 'Tổng quan' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Cấu hình AI' })).toBeVisible();

  await page.getByRole('link', { name: /Gemini model catalog/ }).first().click();
  await expect(page.getByRole('heading', { level: 1, name: 'Gemini model catalog' })).toBeVisible();
  await page.getByRole('link', { name: 'Tổng quan' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Cấu hình AI' })).toBeVisible();

  await page.getByRole('link', { name: /AI Credit và nạp tiền/ }).first().click();
  await expect(page.getByRole('heading', { level: 1, name: 'AI Credit và nạp tiền' })).toBeVisible();
});

test('handles initial loading error and allows retry', async ({ page }) => {
  let attempts = 0;
  await page.route('**/api/v1/admin/ai-config', (route) => {
    attempts++;
    if (attempts === 1) {
      return route.fulfill({ status: 500, json: { status: 500, code: 'INTERNAL_ERROR', detail: 'Lỗi máy chủ khi tải cấu hình.' } });
    }
    return route.fulfill({ json: mockAiConfig });
  });

  await page.goto('/admin/ai-config');
  await expect(page.getByRole('alert')).toContainText('Lỗi máy chủ khi tải cấu hình.');

  await page.getByRole('button', { name: 'Thử lại' }).click();
  await expect(page.getByRole('heading', { level: 1, name: 'Cấu hình AI' })).toBeVisible();
});

test('edits plans section, validates changes, steps through confirmation dialog and updates', async ({ page }) => {
  let savedData: unknown = null;
  await page.route('**/api/v1/admin/ai-config', (route) => {
    if (route.request().method() === 'PUT') {
      savedData = route.request().postDataJSON();
      return route.fulfill({ json: { ...mockAiConfig, plans: [
        { ...mockAiConfig.plans[0], tokenQuota: 15000 },
        { ...mockAiConfig.plans[1], priceVnd: 59000 },
        mockAiConfig.plans[2],
      ] } });
    }
    return route.fulfill({ json: mockAiConfig });
  });

  await page.goto('/admin/ai-config/packages');
  await expect(page.getByRole('heading', { level: 1, name: 'Gói và hạn mức token' })).toBeVisible();

  // Navigation tab links
  await expect(page.getByRole('link', { name: 'Tổng quan' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Gemini model catalog' })).toBeVisible();

  // Click Sửa
  await page.getByRole('button', { name: 'Sửa' }).click();

  // Try saving without changes
  await page.getByRole('button', { name: 'Lưu phần này' }).click();
  await expect(page.getByRole('alert')).toContainText('Phần này chưa có thay đổi để lưu.');

  // Cancel editing reverts
  await page.getByRole('button', { name: 'Hủy' }).click();
  await expect(page.getByRole('button', { name: 'Sửa' })).toBeVisible();

  // Sửa again and modify values
  await page.getByRole('button', { name: 'Sửa' }).click();
  const plusPriceInput = page.locator('section').filter({ hasText: 'Plus' }).locator('input[type="number"]').first();
  await plusPriceInput.fill('59000');

  // Request save to open confirm dialog
  await page.getByRole('button', { name: 'Lưu phần này' }).click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  await expect(dialog.getByText('Bước 1 / 2')).toBeVisible();
  await expect(dialog.getByText('Kiểm tra thay đổi: Gói và hạn mức token')).toBeVisible();

  // Check email notification checkbox
  const emailCheckbox = dialog.locator('input[type="checkbox"]');
  await emailCheckbox.check();

  // Proceed to Step 2
  await dialog.getByRole('button', { name: 'Tiếp tục xác nhận' }).click();
  await expect(dialog.getByText('Bước 2 / 2')).toBeVisible();
  await expect(dialog.getByText('Xác nhận áp dụng cấu hình')).toBeVisible();

  // Back to Step 1
  await dialog.getByRole('button', { name: 'Xem lại thay đổi' }).click();
  await expect(dialog.getByText('Bước 1 / 2')).toBeVisible();

  // Proceed again and confirm save
  await dialog.getByRole('button', { name: 'Tiếp tục xác nhận' }).click();
  await dialog.getByRole('button', { name: 'Đồng ý, áp dụng và gửi email' }).click();

  await expect(dialog).toHaveCount(0);
  await expect(page.getByRole('status')).toContainText('Đã lưu phần “Gói và hạn mức token”.');
  expect(savedData).toBeTruthy();
});

test('edits models section, adds model, validates defaults, deletes model and saves', async ({ page }) => {
  let putRequests = 0;
  await page.route('**/api/v1/admin/ai-config', (route) => {
    if (route.request().method() === 'PUT') {
      putRequests++;
      return route.fulfill({ json: mockAiConfig });
    }
    return route.fulfill({ json: mockAiConfig });
  });

  await page.goto('/admin/ai-config/models');
  await expect(page.getByRole('heading', { level: 1, name: 'Gemini model catalog' })).toBeVisible();

  await page.getByRole('button', { name: 'Sửa' }).click();

  // Delete the disabled model using trash button
  const deleteButtons = page.getByRole('button', { name: 'Xóa model khỏi cấu hình' });
  await deleteButtons.nth(2).click();

  // Test validation when a tier has no default: uncheck enabled on model 0
  const enabledCheckboxes = page.locator('input[type="checkbox"]').filter({ hasText: '' });
  await enabledCheckboxes.first().uncheck();
  await page.getByRole('button', { name: 'Lưu phần này' }).click();
  await expect(page.getByRole('alert')).toContainText('cần có ít nhất một model đang bật');
  await enabledCheckboxes.first().check();

  // Add new model
  await page.getByRole('button', { name: 'Thêm model' }).click();
  const emptyModelInputs = page.locator('input[placeholder="gemini-model-id"]');
  await emptyModelInputs.last().fill('gemini-2.5-ultra');
  const emptyNameInputs = page.locator('input[placeholder="Gemini model"]');
  await emptyNameInputs.last().fill('Gemini 2.5 Ultra');

  // Save changes
  await page.getByRole('button', { name: 'Lưu phần này' }).click();

  // Dialog step 1 -> step 2 -> save
  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  await dialog.getByRole('button', { name: 'Tiếp tục xác nhận' }).click();
  await dialog.getByRole('button', { name: 'Đồng ý, áp dụng thay đổi' }).click();

  await expect(dialog).toHaveCount(0);
  await expect(page.getByRole('status')).toContainText('Đã lưu phần “Gemini model catalog”.');
  expect(putRequests).toBe(1);
});

test('edits credit settings, handles PUT error in dialog, and saves successfully', async ({ page }) => {
  let updateAttempts = 0;
  await page.route('**/api/v1/admin/ai-config', (route) => {
    if (route.request().method() === 'PUT') {
      updateAttempts++;
      if (updateAttempts === 1) {
        return route.fulfill({ status: 400, json: { status: 400, code: 'INVALID_ARGUMENT', detail: 'Tỷ lệ quy đổi AI Credit không hợp lệ.' } });
      }
      return route.fulfill({ json: { ...mockAiConfig, credit: { ...mockAiConfig.credit, creditsPer1000Vnd: 15 } } });
    }
    return route.fulfill({ json: mockAiConfig });
  });

  await page.goto('/admin/ai-config/credits');
  await expect(page.getByRole('heading', { level: 1, name: 'AI Credit và nạp tiền' })).toBeVisible();

  await page.getByRole('button', { name: 'Sửa' }).click();

  // Edit creditPer1000Vnd
  const creditInput = page.locator('input[type="number"]').first();
  await creditInput.fill('15');

  await page.getByRole('button', { name: 'Lưu phần này' }).click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  await dialog.getByRole('button', { name: 'Tiếp tục xác nhận' }).click();

  // First save fails with 400
  await dialog.getByRole('button', { name: 'Đồng ý, áp dụng thay đổi' }).click();
  await expect(dialog.getByRole('alert')).toContainText('Tỷ lệ quy đổi AI Credit không hợp lệ.');

  // Second save succeeds
  await dialog.getByRole('button', { name: 'Đồng ý, áp dụng thay đổi' }).click();
  await expect(dialog).toHaveCount(0);
  await expect(page.getByRole('status')).toContainText('Đã lưu phần “AI Credit và nạp tiền”.');
});
