import { Buffer } from 'node:buffer';
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

test('expert application flow handles guest prompt, customer submission and admin review (FR-05)', async ({ page }) => {
  await page.goto('/dang-ky-chuyen-gia');
  await expect(page.getByText('Yêu cầu đăng nhập')).toBeVisible();
  await expect(page.getByRole('link', { name: 'Đăng nhập ngay' })).toBeVisible();

  await page.getByRole('link', { name: 'Đăng nhập ngay' }).click();
  await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
  await page.getByRole('button', { name: 'Đăng ký Chuyên gia' }).first().click();

  // Trigger validation error
  await page.getByRole('button', { name: 'Gửi đơn đăng ký' }).click();
  await expect(page.getByText('Kinh nghiệm ẩm thực chay cần tối thiểu 20 ký tự (theo FR-05).')).toBeVisible();

  await page.locator('#culinaryExperience').fill('Hơn 5 năm kinh nghiệm nấu ăn thuần chay thực dưỡng.');
  await page.locator('#sampleRecipe').fill('Đậu hũ sốt nấm hương tiêu xanh hấp dẫn thơm lừng.');
  await page.getByRole('button', { name: 'Gửi đơn đăng ký' }).click();

  await expect(page.getByText(/PENDING · Đang chờ phê duyệt/i)).toBeVisible();

  // Admin view & review
  await page.getByRole('button', { name: 'Admin Duyệt' }).click();
  await expect(page.getByText(/Bảng xét duyệt đơn đăng ký Chuyên gia/i)).toBeVisible();
  await page.getByRole('button', { name: 'Xem chi tiết & Thẩm định' }).first().click();

  // Attempt reject without reason error
  await page.getByRole('button', { name: 'Từ chối đơn' }).click();
  await expect(page.getByText('Bắt buộc nhập lý do khi từ chối đơn đăng ký (theo FR-05).')).toBeVisible();

  // Fill rejection reason and reject
  await page.locator('#reject-reason-input').fill('Cần bổ sung thêm kinh nghiệm cụ thể.');
  await page.getByRole('button', { name: 'Từ chối đơn' }).click();

  // Back to customer view to verify rejected state & resubmit
  await page.getByRole('button', { name: 'Customer' }).click();
  await expect(page.getByText(/REJECTED · Đơn bị từ chối/i)).toBeVisible();
  await expect(page.getByText('Cần bổ sung thêm kinh nghiệm cụ thể.')).toBeVisible();
  await page.getByRole('button', { name: 'Nộp lại đơn đăng ký mới' }).click();
  await expect(page.getByRole('heading', { name: /Biểu mẫu Đăng ký Chuyên gia/i })).toBeVisible();
  await page.getByRole('button', { name: 'Hủy' }).click();

  // Test Expert view
  await page.getByRole('button', { name: 'Chuyên gia', exact: true }).click();
  await expect(page.getByText('Bạn là Chuyên gia ẩm thực chay')).toBeVisible();
});

test('recipe like and dislike reactions update state without rating stars (FR-57)', async ({ page }) => {
  await page.goto('/dang-nhap');
  await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
  await page.getByRole('link', { name: 'Khám phá món chay' }).first().click();
  await page.getByRole('link', { name: /Phở Chay/i }).first().click();
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

test('recipe card save and add-to-plan actions handle guest alert and authenticated updates (AC-01.5)', async ({ page }) => {
  // 1. As guest on explore page:
  await page.goto('/kham-pha');
  await page.getByLabel('Lưu công thức').first().click();
  await expect(page.getByText('Vui lòng đăng nhập để lưu công thức.')).toBeVisible();

  await page.getByRole('button', { name: 'Thêm vào lịch ăn' }).first().click();
  await expect(page.getByText('Vui lòng đăng nhập để thêm vào lịch ăn.')).toBeVisible();

  // 2. Log in with demo account:
  await page.goto('/dang-nhap');
  await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
  await page.getByRole('link', { name: 'Khám phá món chay' }).first().click();

  // 3. Save recipe and add to plan on card:
  await page.getByLabel('Lưu công thức').first().click();
  await expect(page.getByLabel('Bỏ lưu công thức').first()).toBeVisible();

  await page.getByRole('button', { name: 'Thêm vào lịch ăn' }).first().click();
  await expect(page.getByText('Đã thêm vào lịch ăn').first()).toBeVisible();

  // 4. Open recipe detail and un-save:
  await page.getByRole('link', { name: /Đậu hũ non/i }).first().click();
  await page.getByRole('button', { name: 'Đã lưu' }).click();
  await expect(page.getByRole('button', { name: 'Lưu lại' })).toBeVisible();
});

test('recipe creation route is restricted to approved Expert demo role (FR-05)', async ({ page }) => {
  await page.goto('/dang-cong-thuc');
  await expect(page.getByRole('heading', { name: 'Đăng công thức chỉ dành cho Chuyên gia' })).toBeVisible();
  await expect(page.getByRole('main').getByRole('link', { name: 'Đăng nhập' })).toHaveAttribute('href', '/dang-nhap');

  await page.goto('/dang-nhap');
  await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
  const accountMenu = page.getByRole('button', { name: /Tài khoản Lan Anh/ });

  await accountMenu.click();
  await page.getByRole('button', { name: 'Customer', exact: true }).click();
  await page.evaluate(() => {
    window.history.pushState({}, '', '/dang-cong-thuc');
    window.dispatchEvent(new PopStateEvent('popstate'));
  });
  await expect(page.getByText('Bạn cần được phê duyệt đơn đăng ký Chuyên gia trước khi đăng công thức.')).toBeVisible();
  await expect(page.getByRole('main').getByRole('link', { name: 'Đăng ký trở thành Chuyên gia' })).toHaveAttribute('href', '/dang-ky-chuyen-gia');

  await page.getByRole('button', { name: 'Expert', exact: true }).click();
  await page.evaluate(() => {
    window.history.pushState({}, '', '/dang-cong-thuc');
    window.dispatchEvent(new PopStateEvent('popstate'));
  });
  await expect(page.getByRole('heading', { name: 'Đăng công thức món chay mới' })).toBeVisible();

  await page.getByRole('button', { name: 'Admin', exact: true }).click();
  await page.evaluate(() => {
    window.history.pushState({}, '', '/dang-cong-thuc');
    window.dispatchEvent(new PopStateEvent('popstate'));
  });
  await expect(page.getByText('Vai trò hiện tại không có quyền đăng công thức.')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Đăng công thức món chay mới' })).toHaveCount(0);
});

test('saved recipe appears in profile and disappears when unsaved', async ({ page }) => {
  await page.goto('/ho-so');
  await expect(page.getByRole('heading', { name: 'Chưa có công thức đã lưu' })).toBeVisible();

  await page.goto('/dang-nhap');
  await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
  await page.getByRole('link', { name: 'Khám phá món chay' }).first().click();
  await page.getByLabel('Lưu công thức').first().click();
  await expect(page.getByLabel('Bỏ lưu công thức').first()).toBeVisible();

  await page.evaluate(() => {
    window.history.pushState({}, '', '/ho-so');
    window.dispatchEvent(new PopStateEvent('popstate'));
  });
  const savedCard = page.getByRole('link', { name: /Đậu hũ non sốt nấm đông cô tiêu xanh/i });
  await expect(savedCard).toBeVisible();
  await savedCard.getByRole('button', { name: 'Bỏ lưu công thức' }).click();
  await expect(page.getByRole('heading', { name: 'Chưa có công thức đã lưu' })).toBeVisible();
});

test('FR-25: Direct Recipe Publishing with validation error preservation and successful publish flow', async ({ page }) => {
  await page.route('**/api/v1/recipes/form-options', (route) => route.fulfill({
    json: {
      dishCategories: [{ code: 'BRAISED', label: 'Món kho' }],
      vegetarianTypes: [{ code: 'VEGAN', label: 'Thuần chay' }],
      difficulties: [{ code: 'EASY', label: 'Dễ' }],
      units: [{ unitId: 1, code: 'g', name: 'gram', dimension: 'MASS' }],
    },
  }));
  await page.route('**/api/v1/recipes/ingredient-options**', (route) => route.fulfill({
    json: [{ ingredientId: 1, name: 'Đậu hũ' }],
  }));

  // Control recipes publish route: first fail validation, then succeed
  let publishAttempts = 0;
  let receivedPayload: any = null;
  await page.route('**/api/v1/recipes', async (route) => {
    if (route.request().method() !== 'POST') {
      return route.continue();
    }
    publishAttempts++;
    receivedPayload = route.request().postDataJSON();

    if (publishAttempts === 1) {
      // AC-25.3: Server validation failure
      return route.fulfill({
        status: 400,
        contentType: 'application/problem+json',
        json: {
          status: 400,
          code: 'VALIDATION_FAILED',
          title: 'Validation Failed',
          detail: 'Dữ liệu không hợp lệ',
          errors: [
            { field: 'title', message: 'Tên món ăn đã tồn tại hoặc không hợp lệ' },
          ],
        },
      });
    }

    // AC-25.1: Success response
    return route.fulfill({
      status: 201,
      json: {
        recipeId: 999,
        title: receivedPayload.title,
        status: 'PUBLISHED',
      },
    });
  });

  // Mock recipe detail page route after redirect
  await page.route('**/api/v1/recipes/999', (route) => route.fulfill({
    json: {
      id: 999,
      title: 'Nấm đùi gà kho tiêu xanh',
      description: 'Món kho thơm lừng đậm đà.',
      instructions: 'Cắt nấm thành từng lát vừa ăn, ướp gia vị chay và kho liu riu cho thấm.',
      dishCategory: 'BRAISED',
      vegetarianType: 'VEGAN',
      difficulty: 'EASY',
      servings: 4,
      prepTimeMinutes: 15,
      cookTimeMinutes: 20,
      status: 'PUBLISHED',
      authorId: 42,
      authorName: 'Trần Thị Chuyên Gia',
      media: [],
      ingredients: [
        {
          ingredientId: 1,
          ingredientName: 'Đậu hũ',
          quantity: 200,
          unitId: 1,
          unitCode: 'g',
          unitName: 'gram',
        },
      ],
    },
  }));

  // 1. Log in as Expert
  await page.goto('/dang-nhap');
  await page.getByRole('button', { name: 'Khám phá tài khoản demo' }).click();
  const accountMenu = page.getByRole('button', { name: /Tài khoản Lan Anh/ });
  await accountMenu.click();
  await page.getByRole('button', { name: 'Expert', exact: true }).click();
  await page.evaluate(() => {
    window.history.pushState({}, '', '/dang-cong-thuc');
    window.dispatchEvent(new PopStateEvent('popstate'));
  });

  await expect(page.getByRole('heading', { name: 'Đăng công thức món chay mới' })).toBeVisible();

  // 2. Validate image cover selection (AC-25.6)
  const imageInput = page.getByLabel('Chọn tối đa 5 ảnh');
  await imageInput.setInputFiles([
    { name: 'cover-candidate.png', mimeType: 'image/png', buffer: Buffer.from('cover') },
  ]);
  await page.getByRole('button', { name: 'Xuất bản công thức' }).click();
  await expect(page.getByRole('alert')).toContainText('hãy chọn đúng 1 ảnh bìa');

  // Select cover image, verify notice that FR-14 storage is disconnected
  await page.getByLabel('Chọn cover-candidate.png làm ảnh cover').check();
  await page.getByRole('button', { name: 'Xuất bản công thức' }).click();
  await expect(page.getByRole('alert')).toContainText('Upload ảnh thuộc FR-14');

  // Clear images for direct publish
  await imageInput.setInputFiles([]);

  // 3. Fill form fields
  await page.getByLabel('Tên món *').fill('Nấm đùi gà kho tiêu xanh');
  await page.getByLabel('Mô tả').fill('Món kho thơm lừng đậm đà.');
  await page.getByLabel('Thể loại món').selectOption('BRAISED');
  await page.getByLabel('Loại ăn chay').selectOption('VEGAN');
  await page.getByLabel('Độ khó').selectOption('EASY');
  await page.getByLabel('Khẩu phần').fill('4');
  await page.getByLabel('Thời gian chuẩn bị').fill('15');
  await page.getByLabel('Thời gian nấu').fill('20');

  // Fill ingredient
  await page.getByLabel('Chọn nguyên liệu 1').fill('Đậu');
  await page.getByRole('button', { name: 'Đậu hũ', exact: true }).click();
  await page.getByLabel('Số lượng nguyên liệu 1').fill('200');
  await page.getByLabel('Đơn vị nguyên liệu 1').selectOption('1');

  // Fill instructions
  await page.getByLabel('Hướng dẫn * (10–5.000 ký tự)').fill('Cắt nấm thành từng lát vừa ăn, ướp gia vị chay và kho liu riu cho thấm.');

  // 4. Submit once: trigger validation failure from server (AC-25.3)
  await page.getByRole('button', { name: 'Xuất bản công thức' }).click();

  // Field error from API response should be rendered
  await expect(page.getByText('Tên món ăn đã tồn tại hoặc không hợp lệ')).toBeVisible();

  // Form input preservation (AC-25.3)
  expect(await page.getByLabel('Tên món *').inputValue()).toBe('Nấm đùi gà kho tiêu xanh');
  expect(await page.getByLabel('Mô tả').inputValue()).toBe('Món kho thơm lừng đậm đà.');
  expect(await page.getByLabel('Khẩu phần').inputValue()).toBe('4');
  expect(await page.getByLabel('Thời gian chuẩn bị').inputValue()).toBe('15');
  expect(await page.getByLabel('Thời gian nấu').inputValue()).toBe('20');
  expect(await page.getByLabel('Số lượng nguyên liệu 1').inputValue()).toBe('200');
  expect(await page.getByLabel('Hướng dẫn * (10–5.000 ký tự)').inputValue()).toContain('Cắt nấm thành từng lát');

  // 5. Submit again: validation passes, direct publish succeeds (AC-25.1)
  await page.getByRole('button', { name: 'Xuất bản công thức' }).click();

  // Verify server payload
  expect(receivedPayload).toBeTruthy();
  expect(receivedPayload.title).toBe('Nấm đùi gà kho tiêu xanh');

  // Navigates directly to published recipe detail
  await page.waitForURL('**/cong-thuc/999');
});

