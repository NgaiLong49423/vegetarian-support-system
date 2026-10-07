import type { Page } from '@playwright/test';
import { expect, test } from './baseFixtures';
import { seedDemoSession } from './demo-session';

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

test('meal planner adds, changes serving count and removes a recipe from a meal slot', async ({ page }) => {
  await page.goto('/ke-hoach', { waitUntil: 'domcontentloaded' });

  const addSlot = page.getByRole('button', { name: /Thêm món/ }).first();
  const slot = (await addSlot.innerText()).replace('Thêm món', '').trim();
  const dayCard = addSlot.locator('xpath=../../..');
  await addSlot.click();
  const picker = page.getByRole('dialog');
  await expect(picker.getByRole('heading', { name: `Chọn món ${slot}` })).toBeVisible();
  await expect(picker).toBeVisible();
  const recipeButton = picker.locator('button:has(p)').first();
  const recipeName = (await recipeButton.locator('p').first().textContent())?.trim();
  await recipeButton.click();

  expect(recipeName).toBeTruthy();
  const servings = dayCard.getByLabel(/Khẩu phần/).last();
  await expect(servings).toHaveValue('1');
  await servings.selectOption('1.5');
  await expect(servings).toHaveValue('1.5');

  await dayCard.getByRole('button', { name: 'Xoá món' }).last().click();
  await expect(addSlot).toBeVisible();
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

test('avatar menu displays the authenticated Backend identity and UI-only plan remains labeled demo', async ({ page }) => {
  await seedDemoSession(page, 'CUSTOMER');
  await page.goto('/');
  await page.getByRole('button', { name: 'Tài khoản Demo CUSTOMER' }).click();
  await expect(page.getByText('demo-customer@mamxanh.local')).toBeVisible();
  await expect(page.getByRole('button', { name: /Chuyển vai trò demo/ })).toHaveCount(0);
  await page.getByRole('link', { name: 'Nâng cấp gói AI' }).click();
  await expect(page.getByRole('heading', { name: 'Nâng cấp gói AI' })).toBeVisible();
  await expect(page.getByText('Gói hiện tại: FREE (demo)')).toBeVisible();
});

test('mobile account links remain reachable from the menu', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await seedDemoSession(page, 'CUSTOMER');
  await page.goto('/');
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

test('recipe report form accepts an empty optional description for each standard reason', async ({ page }) => {
  const optionalReasons = [
    'Không phải món chay',
    'Nguy cơ an toàn thực phẩm',
    'Nội dung phản cảm / Bạo lực',
    'Vi phạm bản quyền / Sao chép',
    'Spam / Quảng cáo thương mại',
  ];

  for (const reason of optionalReasons) {
    await page.goto('/cong-thuc/pho-chay-nam-huong-rung');
    await page.getByRole('button', { name: 'Báo cáo công thức' }).click();
    await page.getByRole('radio', { name: reason }).check();
    await expect(page.getByRole('radio', { checked: true })).toHaveCount(1);
    await expect(page.getByLabel('Mô tả bổ sung')).toHaveValue('');
    await page.getByRole('button', { name: 'Kiểm tra biểu mẫu' }).click();
    await expect(page.getByText('Đã kiểm tra biểu mẫu. Chưa gửi báo cáo tới quản trị viên.')).toBeVisible();
  }
});

test('recipe report form enforces trimmed OTHER boundaries and the 500 character limit', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/cong-thuc/pho-chay-nam-huong-rung');
  await page.getByRole('button', { name: 'Báo cáo công thức' }).click();
  await page.getByRole('radio', { name: 'Khác' }).check();
  await page.getByLabel('Mô tả bổ sung').fill('         ');
  await page.getByRole('button', { name: 'Kiểm tra biểu mẫu' }).click();
  await expect(page.getByRole('alert')).toContainText('10 đến 500');
  await page.getByLabel('Mô tả bổ sung').fill('123456789');
  await page.getByRole('button', { name: 'Kiểm tra biểu mẫu' }).click();
  await expect(page.getByRole('alert')).toContainText('10 đến 500');
  await page.getByLabel('Mô tả bổ sung').fill('1234567890');
  await page.getByRole('button', { name: 'Kiểm tra biểu mẫu' }).click();
  await expect(page.getByText('Đã kiểm tra biểu mẫu. Chưa gửi báo cáo tới quản trị viên.')).toBeVisible();

  await page.reload();
  await page.getByRole('button', { name: 'Báo cáo công thức' }).click();
  await page.getByRole('radio', { name: 'Khác' }).check();
  const longDescription = 'x'.repeat(501);
  await page.getByLabel('Mô tả bổ sung').fill(longDescription);
  await expect(page.getByLabel('Mô tả bổ sung')).toHaveValue('x'.repeat(500));
  await expect(page.getByText('500/500 ký tự')).toBeVisible();
  await page.getByRole('button', { name: 'Kiểm tra biểu mẫu' }).click();
  await expect(page.getByText('Đã kiểm tra biểu mẫu. Chưa gửi báo cáo tới quản trị viên.')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
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

test('recipe comparison shows two recipes side-by-side without health score (FR-60)', async ({ page }) => {
  await page.goto('/so-sanh');
  await expect(page.getByRole('heading', { name: /So sánh hai công thức/i })).toBeVisible();
  await expect(page.locator('#compare-recipe-left')).toBeVisible();
  await expect(page.locator('#compare-recipe-right')).toBeVisible();
  await expect(page.getByText('Năng lượng').first()).toBeVisible();
  await expect(page.getByText('Chưa đủ dữ liệu').first()).toBeVisible();
  await expect(page.getByText(/không tính toán điểm Health Score/i)).toBeVisible();

  // Change right dropdown to exercise select handler
  await page.locator('#compare-recipe-right').selectOption({ index: 2 });
  await expect(page.locator('#compare-recipe-right')).toBeVisible();
});

test('nutrition tracker opens overall 9-indicator analysis modal (FR-37)', async ({ page }) => {
  await page.goto('/dinh-duong');

  // Exercise simulated PDF alert
  await page.getByRole('button', { name: 'Xuất báo cáo PDF' }).click();
  await expect(page.getByText(/Tính năng xuất PDF đang ở chế độ mô phỏng/i)).toBeVisible();
  await page.getByRole('button', { name: 'Đóng' }).first().click();

  await page.getByRole('button', { name: 'Phân tích tổng thể' }).click();
  const modal = page.getByRole('dialog');
  await expect(modal.getByRole('heading', { name: /Phân tích dinh dưỡng tổng thể/i })).toBeVisible();
  await modal.getByRole('button', { name: /Tuần qua/i }).click();
  await expect(modal.getByRole('cell', { name: 'Tổng năng lượng' })).toBeVisible();
  await expect(modal.getByText('12,194 kcal')).toBeVisible();

  // Test state simulation selector
  await modal.getByRole('combobox').selectOption('error');
  await expect(modal.getByText('Không thể tải dữ liệu dinh dưỡng')).toBeVisible();
  await modal.getByRole('button', { name: 'Thử lại' }).click();

  await modal.getByRole('combobox').selectOption('empty');
  await expect(modal.getByText('Chưa có thực đơn để phân tích')).toBeVisible();

  await modal.getByRole('button', { name: /Hôm nay/i }).click();
  await page.keyboard.press('Escape');
  await expect(modal).toBeHidden();
});

test('expert application customer and admin workflows use the API and recover from stale decisions (FR-05)', async ({ page }) => {
  await page.goto('/dang-ky-chuyen-gia');
  await expect(page.getByRole('heading', { name: 'Đăng nhập để tiếp tục' })).toBeVisible();
  await page.evaluate(() => sessionStorage.setItem('mamxanh.auth', JSON.stringify({
    accessToken: 'test-only-token', expiresAt: Date.now() + 3600000,
    account: { id: 41, displayName: 'Nguyễn An', email: 'an@example.org', role: 'CUSTOMER', accountStatus: 'ACTIVE', emailVerified: true },
  })));

  let status: 'PENDING' | 'REJECTED' | 'APPROVED' | null = null;
  let adminNote = '';
  let conflictOnce = true;
  let posted: Record<string, unknown> | null = null;
  await page.route('**/api/v1/**expert-applications**', async (route) => {
    const request = route.request();
    const path = new URL(request.url()).pathname;
    const application = { id: 501, userId: 41, displayName: 'Nguyễn An', email: 'an@example.org', experience: 'Hơn 5 năm kinh nghiệm nấu ăn thuần chay thực dưỡng.', vegetarianType: 'VEGAN', sampleRecipeSummary: 'Đậu hũ sốt nấm hương tiêu xanh hấp dẫn và thơm lừng.', portfolioUrl: null, status, adminNote, submittedAt: new Date().toISOString(), reviewedAt: status && status !== 'PENDING' ? new Date().toISOString() : null };
    if (path.endsWith('/admin/expert-applications') && request.method() === 'GET') {
      return route.fulfill({ json: { content: status ? [application] : [], page: 0, size: 20, totalElements: Number(!!status), totalPages: Number(!!status) } });
    }
    if (path.endsWith('/admin/expert-applications/501') && request.method() === 'GET') {
      return route.fulfill({ json: application });
    }
    if (path.endsWith('/reject') && request.method() === 'POST') {
      adminNote = request.postDataJSON().reason;
      status = 'REJECTED';
      if (conflictOnce) {
        conflictOnce = false;
        return route.fulfill({ status: 409, contentType: 'application/problem+json', body: JSON.stringify({ status: 409, detail: 'Đơn đã được xử lý bởi một yêu cầu khác.', code: 'EXPERT_APPLICATION_STALE' }) });
      }
    }
    if (path.endsWith('/approve') && request.method() === 'POST') {
      status = 'APPROVED';
      return route.fulfill({ json: { ...application, status, adminNote: null } });
    }
    if (request.method() === 'POST' && path.endsWith('/expert-applications')) {
      posted = request.postDataJSON();
      status = 'PENDING';
      adminNote = '';
      return route.fulfill({ status: 201, json: { ...application, status, adminNote: null } });
    }
    if (path.endsWith('/me') && request.method() === 'GET') {
      return route.fulfill({ json: { content: status ? [application] : [], page: 0, size: 20, totalElements: Number(!!status), totalPages: Number(!!status) } });
    }
    return route.fulfill({ json: application });
  });
  await page.reload();
  await page.getByLabel(/Kinh nghiệm ẩm thực chay/).fill('Hơn 5 năm kinh nghiệm nấu ăn thuần chay thực dưỡng.');
  await page.getByLabel(/Tóm tắt công thức sở trường/).fill('Đậu hũ sốt nấm hương tiêu xanh hấp dẫn và thơm lừng.');
  await page.getByLabel(/Tôi cam kết/).check();
  await page.getByRole('button', { name: 'Gửi đơn đăng ký' }).click();
  await expect(page.getByRole('status')).toContainText('Đơn đã được gửi');
  expect(posted).toMatchObject({ vegetarianType: 'VEGAN' });

  await page.evaluate(() => {
    const session = JSON.parse(sessionStorage.getItem('mamxanh.auth')!); session.account.role = 'ADMIN'; sessionStorage.setItem('mamxanh.auth', JSON.stringify(session));
    window.location.assign('/admin/xet-duyet-chuyen-gia');
  });
  await expect(page.getByRole('heading', { name: 'Xét duyệt Chuyên gia' })).toBeVisible();
  await page.getByRole('button', { name: 'Xem chi tiết' }).click();
  await page.getByLabel(/Lý do từ chối/).fill('Cần bổ sung thêm kinh nghiệm cụ thể.');
  await page.getByRole('button', { name: 'Từ chối', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Đơn đã được xử lý');
  await expect(page.getByText('REJECTED', { exact: true })).toBeVisible();

  await page.evaluate(() => {
    const session = JSON.parse(sessionStorage.getItem('mamxanh.auth')!); session.account.role = 'CUSTOMER'; sessionStorage.setItem('mamxanh.auth', JSON.stringify(session));
    window.location.assign('/dang-ky-chuyen-gia');
  });
  await expect(page.getByText('Cần bổ sung thêm kinh nghiệm cụ thể.')).toBeVisible();
  await page.getByLabel(/Kinh nghiệm ẩm thực chay/).fill('Tôi đã bổ sung kinh nghiệm chế biến món chay trong nhiều năm.');
  await page.getByLabel(/Tóm tắt công thức sở trường/).fill('Công thức đậu hũ và nấm được hướng dẫn đầy đủ, dễ thực hiện.');
  await page.getByLabel(/Tôi cam kết/).check();
  await expect(page.getByRole('button', { name: 'Gửi đơn đăng ký' })).toBeEnabled();
  await page.getByRole('button', { name: 'Gửi đơn đăng ký' }).click();
  await expect(page.getByText(/#501 · PENDING/)).toBeVisible();

  await page.evaluate(() => {
    const session = JSON.parse(sessionStorage.getItem('mamxanh.auth')!); session.account.role = 'ADMIN'; sessionStorage.setItem('mamxanh.auth', JSON.stringify(session));
    window.location.assign('/admin/xet-duyet-chuyen-gia');
  });
  await page.getByRole('button', { name: 'Xem chi tiết' }).click();
  await page.getByRole('button', { name: 'Phê duyệt' }).click();
  await expect(page.getByRole('status')).toContainText('Đã phê duyệt đơn');

  await page.evaluate(() => {
    const session = JSON.parse(sessionStorage.getItem('mamxanh.auth')!); session.account.role = 'CUSTOMER'; sessionStorage.setItem('mamxanh.auth', JSON.stringify(session));
    window.location.assign('/dang-ky-chuyen-gia');
  });
  await expect(page.getByText('Tài khoản đã được phê duyệt Chuyên gia.')).toBeVisible();
});

test('recipe like and dislike reactions update state without rating stars (FR-57)', async ({ page }) => {
  await seedDemoSession(page, 'CUSTOMER');
  await page.goto('/cong-thuc/pho-chay-nam-huong-rung');
  await expect(page.getByRole('heading', { name: /Mức độ yêu thích từ cộng đồng/i })).toBeVisible();

  // Vote Like
  await page.locator('#btnLike').click();
  await expect(page.getByText(/Đã cập nhật bình chọn thành công/i)).toBeVisible();

  // Cancel vote
  await page.getByRole('button', { name: 'Hủy bình chọn' }).click();

  // Vote Dislike
  await page.locator('#btnDislike').click();
  await expect(page.getByText(/Đã cập nhật bình chọn thành công/i)).toBeVisible();

  // Simulate error
  await page.locator('#btnSimulateError').click();
  await expect(page.getByText(/Không thể gửi bình chọn/i)).toBeVisible();
});

test('recipe exploration does not expose local-only card actions as Backend features', async ({ page }) => {
  await page.route('**/api/v1/recipes?**', (route) => route.fulfill({ json: {
    items: [], page: 0, size: 12, totalElements: 0, totalPages: 0,
  } }));
  await page.goto('/kham-pha');
  await expect(page.getByRole('heading', { name: 'Chưa có công thức phù hợp' })).toBeVisible();
  await expect(page.getByLabel('Lưu công thức')).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Thêm vào lịch ăn' })).toHaveCount(0);
});

test('recipe creation route uses authenticated account role (FR-05)', async ({ page }) => {
  await page.goto('/dang-cong-thuc');
  await expect(page.getByRole('heading', { name: 'Đăng công thức chỉ dành cho Chuyên gia' })).toBeVisible();
  await expect(page.getByRole('main').getByRole('link', { name: 'Đăng nhập' })).toHaveAttribute('href', '/dang-nhap');

  await seedDemoSession(page, 'CUSTOMER');
  await page.goto('/dang-cong-thuc');
  await expect(page.getByText('Bạn cần được phê duyệt đơn đăng ký Chuyên gia trước khi đăng công thức.')).toBeVisible();
  await expect(page.getByRole('main').getByRole('link', { name: 'Đăng ký trở thành Chuyên gia' })).toHaveAttribute('href', '/dang-ky-chuyen-gia');

  await seedDemoSession(page, 'EXPERT');
  await page.goto('/dang-cong-thuc');
  await expect(page.getByRole('heading', { name: 'Đăng công thức món chay' })).toBeVisible();

  await seedDemoSession(page, 'ADMIN');
  await page.goto('/dang-cong-thuc');
  await expect(page.getByText('Vai trò hiện tại không có quyền đăng công thức.')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Đăng công thức món chay mới' })).toHaveCount(0);
});

test('profile labels locally saved recipe prototype as not connected to Backend', async ({ page }) => {
  await page.goto('/ho-so');
  await expect(page.getByText(/API lưu công thức chưa được kết nối/)).toBeVisible();
});
