import type { Page, Route } from '@playwright/test';
import { expect, test } from './baseFixtures';

// FR-31 (#36) Onboarding and dietary preferences. The API is replaced with page.route stubs, so these tests
// check the Frontend flow only; Backend and SQL Server behaviour is covered by DietaryPreferenceIntegrationTest.
const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'content-type, accept, authorization',
  'Access-Control-Allow-Methods': 'GET, POST, PUT, OPTIONS',
};

const PREFERENCES = '/nutrition/dietary-preferences';
const INVITATION = `${PREFERENCES}/onboarding/invitation`;

type Profile = Record<string, unknown>;

const emptyProfile = (onboardingStatus: string): Profile => ({
  vegetarianType: null,
  avoid: { noneConfirmed: false, items: [] },
  dislike: { noneConfirmed: false, items: [] },
  cuisinePreference: null,
  maxCookingTimeMinutes: null,
  preferredDifficulty: null,
  onboardingStatus,
  aiPersonalization: { eligible: false, missing: ['VEGETARIAN_TYPE', 'AVOID_INGREDIENTS', 'DISLIKED_INGREDIENTS'] },
});

const completedProfile: Profile = {
  vegetarianType: 'VEGAN',
  avoid: { noneConfirmed: false, items: [{ ingredientId: 7, name: 'Nấm' }] },
  dislike: { noneConfirmed: true, items: [] },
  cuisinePreference: 'Món chay Huế',
  maxCookingTimeMinutes: 45,
  preferredDifficulty: 'MEDIUM',
  onboardingStatus: 'COMPLETED',
  aiPersonalization: { eligible: true, missing: [] },
};

const member = { id: 201, displayName: 'Trần Bình', email: 'binh@example.com', avatarUrl: null, role: 'CUSTOMER', accountStatus: 'ACTIVE', emailVerified: true };

async function stubApi(page: Page, path: string, respond: (route: Route) => Promise<void>) {
  await page.route(`**/api/v1${path}`, async route => {
    if (route.request().method() === 'OPTIONS') return route.fulfill({ status: 204, headers: corsHeaders });
    return respond(route);
  });
}

const json = (body: unknown, status = 200) => ({ status, contentType: 'application/json', headers: corsHeaders, body: JSON.stringify(body) });

function problem(status: number, code: string, extra: Record<string, unknown> = {}) {
  return { status, contentType: 'application/problem+json', headers: corsHeaders, body: JSON.stringify({ title: code, status, code, ...extra }) };
}

/** Records every PUT body; GET returns the current profile. */
async function stubPreferences(page: Page, initial: Profile, onSave?: (body: Record<string, unknown>) => Profile | ReturnType<typeof problem>) {
  const saved: Record<string, unknown>[] = [];
  let current = initial;
  await stubApi(page, PREFERENCES, async route => {
    if (route.request().method() === 'PUT') {
      const body = route.request().postDataJSON() as Record<string, unknown>;
      saved.push(body);
      const result = onSave?.(body) ?? { ...current, ...body, onboardingStatus: 'COMPLETED', aiPersonalization: { eligible: true, missing: [] } };
      if ('status' in result && 'contentType' in result) return route.fulfill(result as ReturnType<typeof problem>);
      current = result as Profile;
      return route.fulfill(json(current));
    }
    return route.fulfill(json(current));
  });
  return saved;
}

/**
 * Server side of AC-31.10: only the first claim of an unanswered invitation shows the questionnaire.
 * Returns the Authorization header of every claim.
 */
async function stubInvitation(page: Page, unanswered = true) {
  const claims: string[] = [];
  let shown = !unanswered;
  await stubApi(page, INVITATION, async route => {
    claims.push(route.request().headers().authorization ?? '');
    const show = !shown;
    shown = true;
    return route.fulfill(json({ show }));
  });
  return claims;
}

async function signOut(page: Page) {
  await page.getByRole('button', { name: 'Tài khoản Trần Bình' }).click();
  await page.getByRole('button', { name: 'Đăng xuất' }).click();
}

async function signInAs(page: Page, account = member) {
  await stubApi(page, '/auth/login', async route => route.fulfill(json({ accessToken: 'header.payload.signature', tokenType: 'Bearer', expiresInSeconds: 3600, account })));
  await page.goto('/dang-nhap');
  await page.getByLabel('Email', { exact: true }).fill(account.email);
  await page.getByLabel('Mật khẩu', { exact: true }).fill('MatKhau123');
  await page.getByRole('button', { name: 'Đăng nhập', exact: true }).click();
}

async function seedSession(page: Page, account = member) {
  await page.addInitScript(([key, value]) => {
    if (!sessionStorage.getItem('seeded')) {
      sessionStorage.setItem(key, value);
      sessionStorage.setItem('seeded', '1');
    }
  }, ['mamxanh.auth', JSON.stringify({ accessToken: 'header.payload.signature', expiresAt: Date.now() + 3_600_000, account })]);
}

test('a new member is invited after the first sign-in and completes onboarding with ingredients (AC-31.1, mock API)', async ({ page }) => {
  const saved = await stubPreferences(page, emptyProfile('NOT_STARTED'));
  const claims = await stubInvitation(page);
  await stubApi(page, `${PREFERENCES}/ingredient-suggestions*`, async route => {
    const query = new URL(route.request().url()).searchParams.get('query') ?? '';
    return route.fulfill(json(query.includes('phộng') ? [{ id: 7, name: 'Đậu phộng', ingredientGroup: 'Hạt' }] : []));
  });

  await signInAs(page);
  await expect(page).toHaveURL(/\/khoi-tao-so-thich$/);
  expect(claims).toEqual(['Bearer header.payload.signature']);
  await expect(page.getByRole('heading', { name: 'Cho Mâm Xanh biết khẩu vị của bạn' })).toBeVisible();

  await page.getByRole('button', { name: 'Hoàn tất' }).click();
  await expect(page.getByText('Chọn một loại ăn chay.')).toBeVisible();
  await expect(page.getByText('Chọn ít nhất một nguyên liệu cần tránh hoặc xác nhận không có dị ứng/kiêng cử.')).toBeVisible();
  await expect(page.getByText('Chọn ít nhất một món hoặc nguyên liệu không thích hoặc xác nhận không có.')).toBeVisible();
  expect(saved).toHaveLength(0);

  await page.getByRole('radio', { name: /Thuần chay \(Vegan\)/ }).check();
  await page.getByLabel('Thêm vào danh sách 2. nguyên liệu cần tránh (dị ứng/kiêng)', { exact: true }).fill('phộng');
  await page.getByRole('button', { name: '+ Đậu phộng' }).click();
  await page.getByLabel('Thêm vào danh sách 3. món hoặc nguyên liệu không thích', { exact: true }).fill('  Mướp   đắng ');
  await page.getByRole('button', { name: 'Thêm mục vào 3. món hoặc nguyên liệu không thích' }).click();
  await expect(page.getByRole('list', { name: '3. Món hoặc nguyên liệu không thích' })).toContainText('Mướp đắng');
  await page.getByLabel('Thời gian nấu tối đa (phút)').fill('30');
  await page.getByLabel('Độ khó mong muốn').selectOption('EASY');
  await page.getByRole('button', { name: 'Hoàn tất' }).click();

  await expect(page).toHaveURL(/\/kham-pha$/);
  expect(saved).toEqual([{
    vegetarianType: 'VEGAN',
    avoid: { noneConfirmed: false, items: ['Đậu phộng'] },
    dislike: { noneConfirmed: false, items: ['Mướp đắng'] },
    cuisinePreference: null,
    maxCookingTimeMinutes: 30,
    preferredDifficulty: 'EASY',
  }]);
});

test('confirming "none" for both lists completes onboarding (AC-31.2, mock API)', async ({ page }) => {
  await seedSession(page);
  const saved = await stubPreferences(page, emptyProfile('NOT_STARTED'));
  await page.goto('/khoi-tao-so-thich');

  await page.getByRole('radio', { name: /Lacto-Ovo/ }).check();
  await page.getByLabel('Thêm vào danh sách 2. nguyên liệu cần tránh (dị ứng/kiêng)', { exact: true }).fill('Gluten');
  await page.keyboard.press('Enter');
  await page.getByLabel('Tôi không có dị ứng/kiêng cử').check();
  await expect(page.getByRole('list', { name: '2. Nguyên liệu cần tránh (dị ứng/kiêng)' })).toHaveCount(0);
  await expect(page.getByLabel('Thêm vào danh sách 2. nguyên liệu cần tránh (dị ứng/kiêng)', { exact: true })).toBeDisabled();
  await page.getByLabel('Tôi không có món không thích').check();
  await page.getByRole('button', { name: 'Hoàn tất' }).click();

  await expect(page).toHaveURL(/\/kham-pha$/);
  expect(saved[0]).toMatchObject({ vegetarianType: 'LACTO_OVO', avoid: { noneConfirmed: true, items: [] }, dislike: { noneConfirmed: true, items: [] } });
});

test('skip closes the invitation and the next sign-in goes straight home (AC-31.3, AC-31.10, mock API)', async ({ page }) => {
  let status = 'NOT_STARTED';
  const skips: string[] = [];
  await stubApi(page, PREFERENCES, async route => route.fulfill(json(emptyProfile(status))));
  const claims = await stubInvitation(page);
  await stubApi(page, `${PREFERENCES}/onboarding/skip`, async route => {
    skips.push(route.request().headers().authorization ?? '');
    status = 'SKIPPED';
    return route.fulfill({ status: 204, headers: corsHeaders });
  });

  await signInAs(page);
  await expect(page).toHaveURL(/\/khoi-tao-so-thich$/);
  await page.getByRole('button', { name: 'Bỏ qua' }).click();
  await expect(page).toHaveURL(/\/kham-pha$/);
  expect(skips).toEqual(['Bearer header.payload.signature']);

  await signOut(page);
  await signInAs(page);
  await expect(page).toHaveURL(/\/$/);
  expect(claims).toHaveLength(2);
  expect(skips).toHaveLength(1);
});

test('leaving the questionnaire unanswered is not invited again but stays open from settings (AC-31.10, mock API)', async ({ page }) => {
  await stubPreferences(page, emptyProfile('NOT_STARTED'));
  const claims = await stubInvitation(page);

  await signInAs(page);
  await expect(page).toHaveURL(/\/khoi-tao-so-thich$/);
  await page.getByRole('link', { name: 'Khám phá món chay' }).first().click();
  await expect(page).toHaveURL(/\/kham-pha$/);

  await signOut(page);
  await signInAs(page);
  await expect(page).toHaveURL(/\/$/);
  expect(claims).toHaveLength(2);

  await page.getByRole('button', { name: 'Menu' }).click();
  await page.getByRole('link', { name: 'Sở thích ăn uống' }).click();
  await expect(page).toHaveURL(/\/ho-so\/so-thich-an-uong$/);
  await expect(page.getByRole('form', { name: 'Sở thích ăn uống' })).toBeVisible();
  await expect(page.getByRole('note', { name: 'Thông tin còn thiếu cho AI cá nhân hóa' })).toBeVisible();

  await page.goto('/khoi-tao-so-thich');
  await expect(page.getByRole('heading', { name: 'Cho Mâm Xanh biết khẩu vị của bạn' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Hoàn tất' })).toBeEnabled();
  expect(claims).toHaveLength(2);
});

test('a failed invitation check never blocks the sign-in (mock API)', async ({ page }) => {
  await stubApi(page, INVITATION, async route => route.fulfill(problem(500, 'INTERNAL_ERROR', { detail: 'Hệ thống gặp lỗi. Vui lòng thử lại sau.' })));

  await signInAs(page);

  await expect(page).toHaveURL(/\/$/);
  await expect(page.getByRole('button', { name: 'Tài khoản Trần Bình' })).toBeVisible();
});

test('an administrator signs in without being asked about dietary preferences (mock API)', async ({ page }) => {
  const calls: string[] = [];
  await stubApi(page, PREFERENCES, async route => {
    calls.push(route.request().method());
    return route.fulfill(json(emptyProfile('NOT_STARTED')));
  });
  const claims = await stubInvitation(page);

  await signInAs(page, { ...member, id: 1, displayName: 'Quản trị', email: 'admin@example.com', role: 'ADMIN' });

  await expect(page).toHaveURL(/\/$/);
  expect(calls).toEqual([]);
  expect(claims).toEqual([]);
});

test('settings refuse an empty allergy list until "none" is confirmed (AC-31.7, mock API)', async ({ page }) => {
  await seedSession(page);
  const saved = await stubPreferences(page, completedProfile);
  await page.goto('/ho-so/so-thich-an-uong');

  await expect(page.getByRole('status')).toContainText('Đã đủ thông tin cho AI cá nhân hóa.');
  await expect(page.getByLabel('Khẩu vị ẩm thực')).toHaveValue('Món chay Huế');
  await expect(page.getByLabel('Tôi không có món không thích')).toBeChecked();
  await page.getByRole('button', { name: 'Bỏ Nấm' }).click();
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByRole('alert')).toHaveText('Chọn ít nhất một nguyên liệu cần tránh hoặc xác nhận không có dị ứng/kiêng cử.');
  expect(saved).toHaveLength(0);

  await page.getByLabel('Tôi không có dị ứng/kiêng cử').check();
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByText('Đã lưu sở thích ăn uống.')).toBeVisible();
  expect(saved[0]).toMatchObject({ vegetarianType: 'VEGAN', avoid: { noneConfirmed: true, items: [] }, maxCookingTimeMinutes: 45, preferredDifficulty: 'MEDIUM' });
});

test('settings explain which minimum information personalized AI still needs (AC-31.4–AC-31.6, mock API)', async ({ page }) => {
  await seedSession(page);
  await stubPreferences(page, { ...emptyProfile('SKIPPED'), vegetarianType: 'LACTO', aiPersonalization: { eligible: false, missing: ['AVOID_INGREDIENTS', 'DISLIKED_INGREDIENTS'] } });
  await page.goto('/');
  await page.getByRole('button', { name: 'Menu' }).click();
  await page.getByRole('link', { name: 'Sở thích ăn uống' }).click();

  await expect(page).toHaveURL(/\/ho-so\/so-thich-an-uong$/);
  const note = page.getByRole('note', { name: 'Thông tin còn thiếu cho AI cá nhân hóa' });
  await expect(note).toContainText('Bạn cần hoàn tất 3 thông tin cơ bản về chế độ ăn chay');
  await expect(note).toContainText('Còn thiếu: Nguyên liệu dị ứng/kiêng, Món không thích.');
  await expect(page.getByRole('radio', { name: /Chay có sữa \(Lacto\)/ })).toBeChecked();
});

test('server validation and conflicts are shown on the matching list (mock API)', async ({ page }) => {
  await seedSession(page);
  let attempt = 0;
  await stubPreferences(page, completedProfile, () => (++attempt === 1
    ? problem(400, 'INGREDIENT_PREFERENCE_CONFLICT', { detail: '"Nấm" không thể vừa là nguyên liệu cần tránh vừa là món không thích.' })
    : problem(400, 'VALIDATION_FAILED', { errors: [{ field: 'avoid.items[0]', message: 'Tên nguyên liệu tối đa 200 ký tự.' }] })));
  await page.goto('/ho-so/so-thich-an-uong');

  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByRole('alert')).toHaveText('"Nấm" không thể vừa là nguyên liệu cần tránh vừa là món không thích.');
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();
  await expect(page.getByRole('alert')).toHaveText('Tên nguyên liệu tối đa 200 ký tự.');
});

test('the same name cannot be added to both lists in the browser (mock API)', async ({ page }) => {
  await seedSession(page);
  const saved = await stubPreferences(page, completedProfile);
  await page.goto('/ho-so/so-thich-an-uong');

  await page.getByLabel('Tôi không có món không thích').uncheck();
  await page.getByLabel('Thêm vào danh sách 3. món hoặc nguyên liệu không thích', { exact: true }).fill('nấm');
  await page.keyboard.press('Enter');
  await page.getByRole('button', { name: 'Lưu thay đổi' }).click();

  await expect(page.getByRole('alert')).toHaveText('"Nấm" không thể vừa là nguyên liệu cần tránh vừa là món không thích.');
  expect(saved).toHaveLength(0);
});

test('guests are sent to sign in and see no private preferences (mock API)', async ({ page }) => {
  const calls: string[] = [];
  await stubApi(page, PREFERENCES, async route => {
    calls.push(route.request().method());
    return route.fulfill(json(completedProfile));
  });

  await page.goto('/khoi-tao-so-thich');
  await expect(page).toHaveURL(/\/dang-nhap$/);
  await page.goto('/ho-so/so-thich-an-uong');
  await expect(page.getByRole('alert')).toContainText('Đăng nhập tài khoản thật để xem và cập nhật sở thích ăn uống.');
  await expect(page.getByRole('form', { name: 'Sở thích ăn uống' })).toHaveCount(0);
  expect(calls).toEqual([]);
});

test('a load failure keeps onboarding skippable (mock API)', async ({ page }) => {
  await seedSession(page);
  await stubApi(page, PREFERENCES, async route => route.fulfill(problem(500, 'INTERNAL_ERROR', { detail: 'Hệ thống gặp lỗi. Vui lòng thử lại sau.' })));
  await stubApi(page, `${PREFERENCES}/onboarding/skip`, async route => route.fulfill(problem(500, 'INTERNAL_ERROR', { detail: 'Hệ thống gặp lỗi. Vui lòng thử lại sau.' })));
  await page.goto('/khoi-tao-so-thich');

  await expect(page.getByRole('alert')).toHaveText('Hệ thống gặp lỗi. Vui lòng thử lại sau.');
  await page.getByRole('button', { name: 'Bỏ qua' }).click();
  await expect(page).toHaveURL(/\/khoi-tao-so-thich$/);
  await expect(page.getByRole('alert')).toHaveText('Hệ thống gặp lỗi. Vui lòng thử lại sau.');
});
