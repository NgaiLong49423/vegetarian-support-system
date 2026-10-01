import type { Page } from '@playwright/test';
import { expect, test } from './baseFixtures';

declare global {
  interface Window {
    __nutritionE2eAccessToken?: string;
  }
}

const exampleProfile = {
  dateOfBirth: '1990-01-01',
  biologicalSex: 'FEMALE',
  heightCm: 170,
  weightKg: 65,
  activityLevel: 'SEDENTARY',
  nutritionGoal: 'MAINTAIN_WEIGHT',
};

const emptyNutritionResponse = {
  hasProfile: false,
  eligible: false,
  outOfScopeReasons: [],
  profile: null,
  results: null,
};

const savedNutritionResponse = {
  hasProfile: true,
  eligible: false,
  outOfScopeReasons: [],
  profile: exampleProfile,
  results: null,
};

const calculatedNutritionResponse = {
  ...savedNutritionResponse,
  eligible: true,
  results: {
    bmi: 22.491349481,
    bmiCategory: 'Bình thường',
    energyKcal: 2200,
    energySource: 'NASEM EER — https://example.org/energy',
    nutrientSource: 'NASEM DRI',
    dailyTargets: [
      { key: 'protein', label: 'Chất đạm', minimum: 55, maximum: 165, unit: 'g', referenceType: 'AMDR', source: 'NASEM — https://example.org/macros' },
      { key: 'carbohydrate', label: 'Carbohydrate', minimum: 250, maximum: 350, unit: 'g', referenceType: 'AMDR', source: 'NASEM — https://example.org/macros' },
      { key: 'fat', label: 'Chất béo', minimum: 50, maximum: 85, unit: 'g', referenceType: 'AMDR', source: 'NASEM — https://example.org/macros' },
      { key: 'fiber', label: 'Chất xơ', minimum: 25, maximum: 25, unit: 'g', referenceType: 'AI', source: 'NASEM DRI' },
      { key: 'calcium', label: 'Canxi', minimum: 1000, maximum: 1000, unit: 'mg', referenceType: 'RDA', source: 'NIH ODS — https://example.org/calcium' },
      { key: 'iron', label: 'Sắt', minimum: 18, maximum: 18, unit: 'mg', referenceType: 'RDA', source: 'NIH ODS — https://example.org/iron' },
      { key: 'vitaminB12', label: 'Vitamin B12', minimum: 2.4, maximum: 2.4, unit: 'mcg', referenceType: 'RDA', source: 'NIH ODS — https://example.org/b12' },
      { key: 'zinc', label: 'Kẽm', minimum: 8, maximum: 8, unit: 'mg', referenceType: 'RDA', source: 'NIH ODS — https://example.org/zinc' },
    ],
  },
};

async function prepareMockAuthenticatedNutritionPage(page: Page) {
  await page.addInitScript(() => { window.__nutritionE2eAccessToken = 'test-only-token'; });
  await page.route('**/api/v1/nutrition/profile**', async (route) => {
    const path = new URL(route.request().url()).pathname;
    const method = route.request().method();
    if (path.endsWith('/calculate')) {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(calculatedNutritionResponse) });
    } else if (method === 'PUT') {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(savedNutritionResponse) });
    } else {
      await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(emptyNutritionResponse) });
    }
  });
  await page.goto('/ho-so/dinh-duong');
}

async function fillEligibleNutritionProfile(page: Page) {
  await page.getByLabel('Ngày sinh *').fill(exampleProfile.dateOfBirth);
  await page.getByRole('radio', { name: 'Nữ' }).check();
  await page.getByLabel('Chiều cao *').fill(String(exampleProfile.heightCm));
  await page.getByLabel('Cân nặng *').fill(String(exampleProfile.weightKg));
  await page.getByLabel('Mức độ vận động *').selectOption(exampleProfile.activityLevel);
  await page.getByLabel('Mục tiêu dinh dưỡng chung *').selectOption(exampleProfile.nutritionGoal);
  for (const name of ['pregnant', 'breastfeeding', 'therapeuticDietRequired']) {
    await page.locator(`input[name="${name}"]`).nth(0).check();
  }
  await page.getByRole('checkbox').check();
}

test('recipe discussion supports reply and keeps replies after parent deletion', async ({ page }) => {
  await page.goto('/cong-thuc/dau-hu-non-sot-nam-dong-co');
  await page.getByLabel('Viết bình luận').fill('Món này có thể dùng nấm khác không?');
  await page.getByRole('button', { name: 'Gửi bình luận' }).click();
  await expect(page.getByText('Món này có thể dùng nấm khác không?')).toBeVisible();
  await page.getByRole('button', { name: 'Trả lời' }).click();
  await page.getByLabel('Viết bình luận').fill('Tôi đã thử nấm đùi gà.');
  await page.getByRole('button', { name: 'Gửi trả lời' }).click();
  await page.getByRole('button', { name: 'Xóa' }).first().click();
  await expect(page.getByText('Bình luận này đã bị xóa bởi người dùng')).toBeVisible();
  await expect(page.getByText('Tôi đã thử nấm đùi gà.')).toBeVisible();
});

test('meal serving selector accepts half portions', async ({ page }) => {
  await page.goto('/ke-hoach');
  const selector = page.getByLabel(/Khẩu phần .* bữa /).first();
  await selector.selectOption('1.5');
  await expect(selector).toHaveValue('1.5');
});

test('recipe ingredients scale from the author serving count', async ({ page }) => {
  await page.goto('/cong-thuc/pho-chay-nam-huong-rung');
  await expect(page.getByRole('heading', { name: 'Dinh dưỡng cho 1 khẩu phần (8 chỉ tiêu minh họa)' })).toBeVisible();
  await expect(page.getByText('Số liệu đang là mẫu giao diện cho một khẩu phần')).toBeVisible();
  await expect(page.getByText('Công thức gốc: 3 phần')).toBeVisible();
  await page.getByRole('spinbutton', { name: 'Số khẩu phần' }).fill('6');
  await expect(page.getByText('160 g', { exact: true })).toBeVisible();
  await expect(page.getByText('800 g', { exact: true })).toBeVisible();
  await expect(page.getByText('2 gói', { exact: true })).toBeVisible();
});

test('avatar menu shows the demo plan and links to account features', async ({ page }) => {
  await page.goto('/dang-nhap');
  await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
  await page.getByRole('button', { name: 'Tài khoản Lan Anh, gói AI FREE demo' }).click();
  await page.getByRole('link', { name: 'Hồ sơ của tôi', exact: true }).click();
  await expect(page.getByRole('link', { name: 'Hồ sơ dinh dưỡng & BMI' })).toHaveCount(0);
  await page.getByRole('button', { name: 'Tài khoản Lan Anh, gói AI FREE demo' }).click();
  await expect(page.getByText('Gói AI hiện tại: FREE (dữ liệu demo)')).toBeVisible();
  await page.getByRole('link', { name: 'Nâng cấp gói AI' }).click();
  await expect(page).toHaveURL(/\/goi-ai$/);
  await expect(page.getByRole('heading', { name: 'Nâng cấp gói AI' })).toBeVisible();
  await expect(page.getByText('Gói hiện tại: FREE (demo)')).toBeVisible();
});

test('mobile account links remain reachable from the menu', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/dang-nhap');
  await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
  await page.getByRole('button', { name: 'Menu' }).click();
  await page.getByRole('link', { name: 'Lịch sử giao dịch' }).click();
  await expect(page).toHaveURL(/\/giao-dich$/);
});

test('recipe report form validates the six reason groups without claiming a server submission', async ({ page }) => {
  await page.goto('/cong-thuc/pho-chay-nam-huong-rung');
  await page.getByRole('button', { name: 'Báo cáo công thức' }).click();
  await expect(page.getByRole('radio')).toHaveCount(6);
  await page.getByRole('button', { name: 'Kiểm tra biểu mẫu' }).click();
  await expect(page.getByRole('alert')).toContainText('chọn một lý do');
  await page.getByRole('radio', { name: 'Khác' }).check();
  await page.getByLabel('Mô tả bổ sung').fill('Ngắn');
  await page.getByRole('button', { name: 'Kiểm tra biểu mẫu' }).click();
  await expect(page.getByRole('alert')).toContainText('10 đến 500');
  await page.getByLabel('Mô tả bổ sung').fill('Nội dung cần được kiểm tra thêm.');
  await page.getByRole('button', { name: 'Kiểm tra biểu mẫu' }).click();
  await expect(page.getByText('Đã kiểm tra biểu mẫu. Chưa gửi báo cáo tới quản trị viên.')).toBeVisible();
});

test('nutrition profile requires real authentication and plan/history pages do not simulate payment', async ({ page }) => {
  await page.goto('/ho-so/dinh-duong');
  await expect(page.getByText('Đăng nhập tài khoản thật để khai báo và lưu hồ sơ.')).toBeVisible();
  await expect(page.getByRole('main').getByRole('link', { name: 'Đăng nhập' })).toHaveAttribute('href', '/dang-nhap');
  await expect(page.getByRole('alert')).toContainText('cần phiên đăng nhập đã được xác thực');
  await expect(page.getByLabel('Ngày sinh *')).toBeDisabled();
  await expect(page.getByLabel('Chiều cao *')).toBeDisabled();
  await expect(page.getByLabel('Cân nặng *')).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Lưu hồ sơ' })).toBeDisabled();
  await expect(page.getByText('Chỉ số BMI tham khảo')).toHaveCount(0);

  await page.goto('/dang-nhap');
  await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
  await page.getByRole('button', { name: 'Tài khoản Lan Anh, gói AI FREE demo' }).click();
  await page.getByRole('link', { name: 'Hồ sơ dinh dưỡng & BMI' }).click();
  await expect(page.getByText('Đăng nhập tài khoản thật để khai báo và lưu hồ sơ.')).toBeVisible();
  await expect(page.getByLabel('Ngày sinh *')).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Lưu hồ sơ' })).toBeDisabled();
  await expect(page.getByText('Chỉ số BMI tham khảo')).toHaveCount(0);

  await page.goto('/goi-ai');
  await expect(page.getByText('49.000')).toBeVisible();
  await expect(page.getByText('99.000')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Thanh toán chưa khả dụng' }).first()).toBeDisabled();
  await page.getByRole('link', { name: 'Xem lịch sử giao dịch' }).click();
  await expect(page.getByText('Chưa có dữ liệu giao dịch')).toBeVisible();
});

// The coverage build's test-only token seam is paired with route mocks; this is not real authentication or FE→BE→DB evidence.
test('mock-authenticated nutrition profile saves consent and shows approximate results with intercepted API', async ({ page }) => {
  const requestHeaders: string[] = [];
  const savedBodies: string[] = [];
  await page.addInitScript(() => { window.__nutritionE2eAccessToken = 'test-only-token'; });
  await page.route('**/api/v1/nutrition/profile**', async (route) => {
    requestHeaders.push(route.request().headers().authorization ?? '');
    const path = new URL(route.request().url()).pathname;
    const method = route.request().method();
    if (method === 'PUT') savedBodies.push(route.request().postData() ?? '');
    const response = path.endsWith('/calculate') ? calculatedNutritionResponse
      : method === 'PUT' ? savedNutritionResponse : emptyNutritionResponse;
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(response) });
  });

  await page.goto('/ho-so/dinh-duong');
  await fillEligibleNutritionProfile(page);
  await page.getByRole('button', { name: 'Lưu hồ sơ' }).click();
  await expect(page.getByText('Đã lưu', { exact: true })).toBeVisible();
  expect(requestHeaders).toContain('Bearer test-only-token');
  expect(JSON.parse(savedBodies[0])).toMatchObject({
    ...exampleProfile,
    pregnant: false,
    breastfeeding: false,
    therapeuticDietRequired: false,
    consentAccepted: true,
  });

  await page.getByRole('button', { name: 'Tính chỉ số tham khảo' }).click();
  await expect(page.getByText('Xấp xỉ 22,5')).toBeVisible();
  await expect(page.getByText('Xấp xỉ 2.200')).toBeVisible();
  await expect(page.getByText('9 chỉ tiêu tham khảo mỗi ngày')).toBeVisible();
  await expect(page.getByText('Chất xơ').locator('..').locator('..').getByText('Xấp xỉ 25,0')).toBeVisible();
  await expect(page.getByRole('link', { name: /Nguồn tham khảo: NASEM EER/ })).toHaveAttribute('href', 'https://example.org/energy');

  await page.getByLabel('Chiều cao *').fill('171');
  await expect(page.getByText('Lưu các thay đổi hồ sơ trước khi tính lại chỉ số tham khảo.')).toBeVisible();
  await expect(page.getByText('Xấp xỉ 22,5')).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Tính chỉ số tham khảo' })).toBeDisabled();
});

test('nutrition profile hides all metrics for underage and excluded declarations', async ({ page }) => {
  await prepareMockAuthenticatedNutritionPage(page);
  await page.getByLabel('Ngày sinh *').fill('2011-01-01');
  await page.getByRole('radio', { name: 'Nữ' }).check();
  await page.getByLabel('Chiều cao *').fill('160');
  await page.getByLabel('Cân nặng *').fill('55');
  await page.getByLabel('Mức độ vận động *').selectOption('SEDENTARY');
  await page.getByLabel('Mục tiêu dinh dưỡng chung *').selectOption('MAINTAIN_WEIGHT');
  await page.locator('input[name="pregnant"]').nth(1).check();
  await page.locator('input[name="breastfeeding"]').nth(0).check();
  await page.locator('input[name="therapeuticDietRequired"]').nth(0).check();
  await page.getByRole('checkbox').check();

  await expect(page.getByText('Ứng dụng hiện không hỗ trợ hồ sơ dinh dưỡng cho người dưới 18 tuổi.')).toBeVisible();
  await expect(page.getByText('Vui lòng tham khảo bác sĩ hoặc chuyên gia dinh dưỡng. Ứng dụng không lưu hồ sơ, tính BMI hoặc hiển thị 9 chỉ tiêu cho trường hợp này.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Lưu hồ sơ' })).toBeDisabled();
  await expect(page.getByText('Chỉ số BMI tham khảo')).toHaveCount(0);
});

test('nutrition profile maps backend validation errors to their fields', async ({ page }) => {
  await page.addInitScript(() => { window.__nutritionE2eAccessToken = 'test-only-token'; });
  await page.route('**/api/v1/nutrition/profile**', async (route) => {
    if (route.request().method() === 'PUT') {
      await route.fulfill({ status: 400, contentType: 'application/json', body: JSON.stringify({
        code: 'VALIDATION_FAILED',
        errors: [{ field: 'heightCm', message: 'Chiều cao phải từ 100 đến 250 cm.' }],
      }) });
      return;
    }
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(emptyNutritionResponse) });
  });
  await page.goto('/ho-so/dinh-duong');
  await fillEligibleNutritionProfile(page);
  await page.getByRole('button', { name: 'Lưu hồ sơ' }).click();
  await expect(page.getByText('Chiều cao phải từ 100 đến 250 cm.')).toBeVisible();
  await expect(page.getByLabel('Chiều cao *')).toHaveAttribute('aria-invalid', 'true');
});
