import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Activity, AlertCircle, CheckCircle2, Info, LoaderCircle, LockKeyhole, Save, Calculator } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Card } from '../components/ui';
import { ApiError, hasAccessToken } from '../lib/apiClient';
import { nutritionApi, type ActivityLevel, type BiologicalSex, type NutritionEligibilityStatus, type NutritionGoal, type NutritionProfileRequest, type NutritionProfileResponse } from '../api/nutrition';

type FormState = {
  dateOfBirth: string;
  biologicalSex: BiologicalSex | '';
  heightCm: string;
  weightKg: string;
  activityLevel: ActivityLevel | '';
  nutritionGoal: NutritionGoal | '';
  consentAccepted: boolean;
};

const emptyForm: FormState = {
  dateOfBirth: '', biologicalSex: '', heightCm: '', weightKg: '', activityLevel: '', nutritionGoal: '',
  consentAccepted: false,
};

const activities: { value: ActivityLevel; label: string; description: string }[] = [
  { value: 'SEDENTARY', label: 'Ít vận động', description: 'Chủ yếu sinh hoạt nhẹ hằng ngày' },
  { value: 'LIGHTLY_ACTIVE', label: 'Vận động nhẹ', description: 'Có thêm hoạt động nhẹ thường xuyên' },
  { value: 'MODERATELY_ACTIVE', label: 'Vận động vừa', description: 'Hoạt động thể chất mức vừa thường xuyên' },
  { value: 'VERY_ACTIVE', label: 'Vận động nhiều', description: 'Hoạt động thể chất cường độ cao thường xuyên' },
];

function ageFromDate(date: string): number | null {
  if (!date) return null;
  const dob = new Date(`${date}T00:00:00`);
  if (Number.isNaN(dob.getTime())) return null;
  const today = new Date();
  let age = today.getFullYear() - dob.getFullYear();
  if (today.getMonth() < dob.getMonth() || (today.getMonth() === dob.getMonth() && today.getDate() < dob.getDate())) age -= 1;
  return age;
}

function todayLocalString() {
  const today = new Date();
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
}

function formatApprox(value: number, fractionDigits = 1) {
  return `≈ ${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: fractionDigits, minimumFractionDigits: fractionDigits }).format(value)}`;
}

function getBmiTheme(category?: string, bmi?: number) {
  const isUnderweight = category?.includes('Thiếu') || (bmi !== undefined && bmi < 18.5);
  const isOverweight = category?.includes('Thừa') || (bmi !== undefined && bmi >= 25 && bmi < 30);
  const isObese = category?.includes('Béo phì') || (bmi !== undefined && bmi >= 30);

  if (isUnderweight) {
    // Thiếu cân -> Đỏ cảnh báo (Red)
    return {
      cardGradient: 'from-red-50 to-paper ring-1 ring-red-200 dark:from-red-950/40 dark:ring-red-900/60',
      labelColor: 'text-red-700 dark:text-red-400',
      numberColor: 'text-red-700 dark:text-red-400',
      badgeClass: 'bg-paper text-red-800 ring-1 ring-red-300 dark:bg-red-950/60 dark:text-red-300 dark:ring-red-800',
      dotColor: 'bg-red-500',
      statusText: 'Cân nặng dưới chuẩn (BMI < 18.5)',
      activeSegment: 0,
    };
  }

  if (isOverweight) {
    // Thừa cân -> Vàng cam cảnh báo (Amber)
    return {
      cardGradient: 'from-amber-50 to-paper ring-1 ring-amber-200 dark:from-amber-950/40 dark:ring-amber-900/60',
      labelColor: 'text-amber-700 dark:text-amber-400',
      numberColor: 'text-amber-800 dark:text-amber-400',
      badgeClass: 'bg-paper text-amber-900 ring-1 ring-amber-300 dark:bg-amber-950/60 dark:text-amber-300 dark:ring-amber-800',
      dotColor: 'bg-amber-500',
      statusText: 'Cân nặng trên chuẩn (25 ≤ BMI < 30)',
      activeSegment: 2,
    };
  }

  if (isObese) {
    // Béo phì -> Đỏ cam / Rose cảnh báo mức cao
    return {
      cardGradient: 'from-rose-50 to-paper ring-1 ring-rose-300 dark:from-rose-950/50 dark:ring-rose-900/70',
      labelColor: 'text-rose-700 dark:text-rose-400',
      numberColor: 'text-rose-800 dark:text-rose-400',
      badgeClass: 'bg-paper text-rose-900 ring-1 ring-rose-400 dark:bg-rose-950/60 dark:text-rose-300 dark:ring-rose-800',
      dotColor: 'bg-rose-600',
      statusText: 'Cảnh báo béo phì (BMI ≥ 30)',
      activeSegment: 3,
    };
  }

  // Bình thường (Đủ) -> Xanh lá an toàn (Leaf)
  return {
    cardGradient: 'from-leaf-50 to-paper ring-1 ring-leaf-100 dark:from-leaf-50 dark:ring-leaf-100',
    labelColor: 'text-leaf-700 dark:text-leaf-400',
    numberColor: 'text-leaf-800 dark:text-leaf-300',
    badgeClass: 'bg-paper text-leaf-800 ring-1 ring-leaf-200 dark:bg-paper dark:text-leaf-700 dark:ring-leaf-300',
    dotColor: 'bg-leaf-600',
    statusText: 'Cân nặng chuẩn theo khuyến nghị (18.5 ≤ BMI < 25)',
    activeSegment: 1,
  };
}

function loadErrorMessage(error: unknown) {
  if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
    return 'Hồ sơ dinh dưỡng cần phiên đăng nhập đã được xác thực. Đăng nhập thật sẽ được nối khi phần xác thực sẵn sàng.';
  }
  return error instanceof Error ? error.message : 'Chưa tải được hồ sơ. Vui lòng thử lại.';
}

function applyResponse(response: NutritionProfileResponse): FormState {
  if (!response.profile) return emptyForm;
  return {
    ...emptyForm,
    dateOfBirth: response.profile.dateOfBirth,
    biologicalSex: response.profile.biologicalSex,
    heightCm: String(response.profile.heightCm),
    weightKg: String(response.profile.weightKg),
    activityLevel: response.profile.activityLevel,
    nutritionGoal: response.profile.nutritionGoal ?? '',
  };
}

function sourceLink(source: string) {
  const separator = source.lastIndexOf(' — ');
  return separator < 0 ? { label: source, url: undefined } : { label: source.slice(0, separator), url: source.slice(separator + 3) };
}

export function NutritionProfile() {
  const authenticated = hasAccessToken();
  const [eligibility, setEligibility] = useState<NutritionEligibilityStatus | null>(null);
  const [eligibilityChoice, setEligibilityChoice] = useState<Exclude<NutritionEligibilityStatus, 'NOT_CONFIRMED'>>('ELIGIBLE');
  const [eligibilityCommitment, setEligibilityCommitment] = useState(false);
  const [editingEligibility, setEditingEligibility] = useState(false);
  const [confirmingEligibility, setConfirmingEligibility] = useState(false);
  const [eligibilityError, setEligibilityError] = useState('');
  const [form, setForm] = useState<FormState>(emptyForm);
  const [savedResponse, setSavedResponse] = useState<NutritionProfileResponse | null>(null);
  const [calculationResponse, setCalculationResponse] = useState<NutritionProfileResponse | null>(null);
  const [loadError, setLoadError] = useState('');
  const [submitError, setSubmitError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<'heightCm' | 'weightKg', string>>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [calculating, setCalculating] = useState(false);

  useEffect(() => {
    if (!authenticated) {
      setLoading(false);
      setLoadError('Hồ sơ dinh dưỡng cần phiên đăng nhập đã được xác thực. Đăng nhập thật sẽ được nối khi phần xác thực sẵn sàng.');
      return;
    }

    let active = true;
    nutritionApi.getEligibility()
      .then(async (response) => {
        if (!active) return;
        setEligibility(response.status);
        if (response.status === 'ELIGIBLE') {
          const profile = await nutritionApi.getProfile();
          if (active) { setSavedResponse(profile); setForm(applyResponse(profile)); }
        }
      })
      .catch((error: unknown) => { if (active) setLoadError(loadErrorMessage(error)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [authenticated]);

  const age = useMemo(() => ageFromDate(form.dateOfBirth), [form.dateOfBirth]);
  const eligible = eligibility === 'ELIGIBLE' && age !== null && age >= 18 && age <= 120;
  const result = calculationResponse?.eligible && eligible ? calculationResponse.results : null;
  const savedProfile = savedResponse?.profile;
  const hasUnsavedProfileChanges = Boolean(savedProfile && (
    form.dateOfBirth !== savedProfile.dateOfBirth
    || form.biologicalSex !== savedProfile.biologicalSex
    || Number(form.heightCm) !== savedProfile.heightCm
    || Number(form.weightKg) !== savedProfile.weightKg
    || form.activityLevel !== savedProfile.activityLevel
    || form.nutritionGoal !== savedProfile.nutritionGoal
  ));
  const showOutOfScope = age !== null && (age < 18 || age > 120);

  const setField = <K extends keyof FormState>(key: K, value: FormState[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
    if (key === 'heightCm' || key === 'weightKg') {
      setFieldErrors((current) => ({ ...current, [key]: undefined }));
    }
    setCalculationResponse(null);
    setSubmitError('');
  };

  const showSaveError = (error: unknown) => {
    const errors = error instanceof ApiError ? error.errors : [];
    const nextFieldErrors: Partial<Record<'heightCm' | 'weightKg', string>> = {};
    for (const fieldError of errors) {
      if (fieldError.field === 'heightCm' || fieldError.field === 'weightKg') {
        nextFieldErrors[fieldError.field] = fieldError.message;
      }
    }
    setFieldErrors(nextFieldErrors);
    setSubmitError(errors.length > Object.keys(nextFieldErrors).length || errors.length === 0 ? loadErrorMessage(error) : '');
  };

  const handleCalculate = async () => {
    setSubmitError('');
    if (!eligible || !savedResponse?.hasProfile) return;
    setCalculating(true);
    try {
      setCalculationResponse(await nutritionApi.calculateProfile({ pregnant: false, breastfeeding: false, therapeuticDietRequired: false }));
    } catch (error) {
      setCalculationResponse(null);
      setSubmitError(loadErrorMessage(error));
    } finally {
      setCalculating(false);
    }
  };

  const handleEligibilitySubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setEligibilityError('');
    setConfirmingEligibility(true);
    try {
      const response = await nutritionApi.updateEligibility(eligibilityChoice);
      setEligibility(response.status);
      setEditingEligibility(false);
      setSavedResponse(null);
      setCalculationResponse(null);
      if (response.status === 'ELIGIBLE') {
        const profile = await nutritionApi.getProfile();
        setSavedResponse(profile);
        setForm(applyResponse(profile));
      } else {
        setForm(emptyForm);
      }
    } catch (error) {
      setEligibilityError(loadErrorMessage(error));
    } finally {
      setConfirmingEligibility(false);
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitError('');
    if (!eligible) {
      setSubmitError(showOutOfScope
        ? 'Trường hợp này ngoài phạm vi hỗ trợ. Ứng dụng không lưu hồ sơ và không hiển thị BMI hoặc các chỉ tiêu.'
        : 'Hãy xác nhận phạm vi hỗ trợ trước khi lưu hồ sơ dinh dưỡng.');
      return;
    }
    if (!form.consentAccepted) {
      setSubmitError('Bạn cần đồng ý lưu dữ liệu tự khai báo trước khi lưu hồ sơ.');
      return;
    }
    const payload: NutritionProfileRequest = {
      dateOfBirth: form.dateOfBirth,
      biologicalSex: form.biologicalSex as BiologicalSex,
      heightCm: Number(form.heightCm),
      weightKg: Number(form.weightKg),
      activityLevel: form.activityLevel as ActivityLevel,
      nutritionGoal: form.nutritionGoal || null,
      pregnant: false,
      breastfeeding: false,
      therapeuticDietRequired: false,
      consentAccepted: true,
    };
    setSaving(true);
    try {
      const response = await nutritionApi.saveProfile(payload);
      setSavedResponse(response);
      setCalculationResponse(null);
      setForm((current) => ({ ...current, consentAccepted: false }));
      setCalculating(true);
      await handleCalculateAfterSave();
      setCalculating(false);
    } catch (error) {
      showSaveError(error);
    } finally {
      setSaving(false);
    }
  };

  const handleCalculateAfterSave = async () => {
    try {
      setCalculationResponse(await nutritionApi.calculateProfile({ pregnant: false, breastfeeding: false, therapeuticDietRequired: false }));
    } catch (error) {
      setCalculationResponse(null);
      setSubmitError(loadErrorMessage(error));
    }
  };

  const fieldClass = 'mt-2 w-full rounded-xl border border-leaf-200 bg-paper px-3.5 py-3 text-sm text-ink outline-none transition focus:border-leaf-600 focus:ring-2 focus:ring-leaf-100 disabled:bg-raised';
  const miniLabelClass = 'block text-sm font-semibold text-ink';

  return <PageContainer className="py-8 md:py-10">
    <nav aria-label="Điều hướng trang" className="mb-5 text-sm text-ink-muted">
      <Link to="/ho-so" className="hover:text-leaf-700">Hồ sơ</Link><span className="mx-2">/</span><span aria-current="page">Hồ sơ dinh dưỡng</span>
    </nav>

    <div className="flex flex-wrap items-start justify-between gap-4">
      <div>
        <p className="mb-2 inline-flex items-center gap-2 rounded-full bg-leaf-50 px-3 py-1 text-xs font-bold text-leaf-700"><Activity className="h-4 w-4" /> THAM KHẢO DINH DƯỠNG</p>
        <h1 className="text-3xl font-extrabold tracking-tight text-ink md:text-4xl">Hồ sơ dinh dưỡng cá nhân</h1>
        <p className="mt-2 max-w-2xl text-sm leading-6 text-ink-muted">Khai báo thông tin để xem các chỉ số dinh dưỡng tham khảo theo dữ liệu của bạn.</p>
      </div>
      <div className="inline-flex items-center gap-2 rounded-full border border-leaf-100 bg-paper px-3 py-2 text-xs font-semibold text-ink-soft"><LockKeyhole className="h-4 w-4 text-leaf-700" /> Hồ sơ riêng tư</div>
    </div>

    <aside role="note" className="mt-6 flex gap-3 rounded-2xl border border-leaf-100 bg-leaf-50 p-4 text-sm leading-6 text-ink-soft md:p-5">
      <Info aria-hidden="true" className="mt-0.5 h-5 w-5 shrink-0 text-leaf-700" />
      <p><strong className="text-ink">Lưu ý:</strong> Đây là ứng dụng hỗ trợ tham khảo dinh dưỡng, không phải công cụ theo dõi sức khỏe lâm sàng. Thông tin do bạn tự khai báo và kết quả chỉ mang tính xấp xỉ tham khảo, dựa trên DRI/EER và nguồn NIH ODS. Lượng dinh dưỡng thực tế còn thay đổi theo loại thực phẩm, khẩu phần, bảo quản và cách chế biến. Các kết quả không phải chẩn đoán, điều trị hay kê đơn, không thay thế tư vấn của bác sĩ hoặc chuyên gia dinh dưỡng và không có giá trị pháp lý hay chứng nhận y tế. Hệ thống không giám sát liên tục tình trạng sức khỏe.</p>
    </aside>

    {loadError && <div role="alert" className="mt-5 flex gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-950"><AlertCircle className="mt-0.5 h-5 w-5 shrink-0" /><p>{loadError}</p></div>}

    {(!authenticated || !eligibility || eligibility !== 'ELIGIBLE' || editingEligibility) ? <Card className="mt-6 max-w-3xl border-leaf-100 p-5 shadow-sm shadow-leaf-900/5 md:p-7">
      <h2 className="text-xl font-bold text-ink">Xác nhận phạm vi hỗ trợ dinh dưỡng</h2>
      <p className="mt-2 text-sm leading-6 text-ink-muted">Các tính năng dinh dưỡng chỉ hỗ trợ người từ đủ 18 tuổi, không mang thai hoặc cho con bú, và không cần chế độ ăn điều trị theo hướng dẫn y tế.</p>
      <div className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-950">
        Thông tin và kết quả trong ứng dụng chỉ mang tính tham khảo để lập kế hoạch ăn uống. Đây không phải chẩn đoán, điều trị hay chỉ định y tế, và không thay thế tư vấn của bác sĩ hoặc chuyên gia dinh dưỡng.
      </div>
      {loading ? <div className="flex items-center gap-2 py-8 text-sm text-ink-muted"><LoaderCircle className="h-4 w-4 animate-spin" /> Đang kiểm tra trạng thái…</div>
        : !authenticated ? <p className="mt-4 text-sm">Đăng nhập để xác nhận và sử dụng tính năng dinh dưỡng. <Link to="/dang-nhap" className="font-bold text-leaf-700 underline">Đăng nhập</Link></p>
          : <form onSubmit={handleEligibilitySubmit} className="mt-5 space-y-4">
            <fieldset className="space-y-3" disabled={confirmingEligibility}>
              <legend className="mb-2 text-sm font-bold text-ink">Chọn xác nhận hiện tại của bạn</legend>
              <label className={`flex cursor-pointer items-start gap-3 rounded-xl border p-4 text-sm leading-6 ${eligibilityChoice === 'ELIGIBLE' ? 'border-leaf-500 bg-leaf-50' : 'border-leaf-100'}`}>
                <input className="mt-1 accent-leaf-700" type="radio" name="nutrition-eligibility" value="ELIGIBLE" checked={eligibilityChoice === 'ELIGIBLE'} onChange={() => { setEligibilityChoice('ELIGIBLE'); setEligibilityCommitment(false); }} />
                <span>Tôi xác nhận mình từ đủ 18 tuổi, không mang thai/cho con bú và không cần chế độ ăn điều trị theo hướng dẫn y tế.</span>
              </label>
              <label className={`flex cursor-pointer items-start gap-3 rounded-xl border p-4 text-sm leading-6 ${eligibilityChoice === 'INELIGIBLE' ? 'border-amber-400 bg-amber-50' : 'border-leaf-100'}`}>
                <input className="mt-1 accent-amber-700" type="radio" name="nutrition-eligibility" value="INELIGIBLE" checked={eligibilityChoice === 'INELIGIBLE'} onChange={() => { setEligibilityChoice('INELIGIBLE'); setEligibilityCommitment(false); }} />
                <span>Tôi hiện không thuộc phạm vi hỗ trợ dinh dưỡng này.</span>
              </label>
            </fieldset>
            {eligibilityChoice === 'ELIGIBLE' && <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-leaf-100 p-4 text-sm leading-6 text-ink-soft">
              <input className="mt-1 h-4 w-4 accent-leaf-700" type="checkbox" required checked={eligibilityCommitment} onChange={(event) => setEligibilityCommitment(event.target.checked)} />
              <span>Tôi đã đọc và xác nhận đáp ứng đủ cả ba điều kiện hỗ trợ.</span>
            </label>}
            {eligibilityChoice === 'INELIGIBLE' && <p className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-950">Hồ sơ, chỉ số và gợi ý dinh dưỡng cá nhân sẽ bị khóa để bảo vệ an toàn sức khỏe. Bạn vẫn sử dụng bình thường các tính năng khác và có thể cập nhật xác nhận nếu tình trạng thay đổi.</p>}
            {eligibilityError && <p role="alert" className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-800">{eligibilityError}</p>}
            {eligibility === 'INELIGIBLE' && <p className="text-sm leading-6 text-amber-900">Hồ sơ và gợi ý dinh dưỡng sẽ tiếp tục bị khóa. Bạn vẫn có thể sử dụng các tính năng khác và xác nhận lại nếu tình trạng thay đổi.</p>}
            <div className="flex flex-wrap gap-3">
              <button type="submit" disabled={confirmingEligibility || (eligibilityChoice === 'ELIGIBLE' && !eligibilityCommitment)} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-leaf-600 px-5 py-3 text-sm font-bold text-white disabled:opacity-60">
                {confirmingEligibility && <LoaderCircle className="h-4 w-4 animate-spin" />}{eligibilityChoice === 'ELIGIBLE' ? 'Xác nhận và tiếp tục' : 'Xác nhận'}
              </button>
              {eligibility === 'ELIGIBLE' && <button type="button" onClick={() => { setEditingEligibility(false); setEligibilityChoice('ELIGIBLE'); }} className="min-h-11 rounded-xl border border-leaf-200 px-5 py-3 text-sm font-semibold text-ink-soft">Hủy</button>}
            </div>
          </form>}
    </Card> : <>
    <div className="mb-4 flex justify-end"><button type="button" onClick={() => { setEligibilityChoice('ELIGIBLE'); setEligibilityCommitment(false); setEligibilityError(''); setEditingEligibility(true); }} className="text-sm font-semibold text-leaf-700 underline underline-offset-2">Cập nhật xác nhận phạm vi</button></div>
    <div className="grid items-start gap-6 xl:grid-cols-[minmax(0,1.05fr)_minmax(0,.95fr)]">
      <Card className="border-leaf-100 p-5 shadow-sm shadow-leaf-900/5 md:p-7">
        <div className="flex items-start justify-between gap-3">
          <div><h2 className="text-xl font-bold text-ink">Thông tin hồ sơ</h2><p className="mt-1 text-sm text-ink-muted">Các trường có dấu * là bắt buộc.</p></div>
          {savedResponse?.hasProfile && <span className="rounded-full bg-leaf-50 px-3 py-1 text-xs font-bold text-leaf-700">Đã lưu</span>}
        </div>

        {loading ? <div className="flex items-center gap-2 py-16 text-sm text-ink-muted"><LoaderCircle className="h-4 w-4 animate-spin" /> Đang tải hồ sơ…</div> : <form onSubmit={handleSubmit} className="mt-6 space-y-5">
          {!authenticated && <p className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm leading-5 text-amber-950">Đăng nhập tài khoản thật để khai báo và lưu hồ sơ. <Link to="/dang-nhap" className="font-bold underline underline-offset-2">Đăng nhập</Link>. Chế độ tài khoản demo không được dùng để gửi dữ liệu dinh dưỡng.</p>}
          <fieldset disabled={!authenticated} className="m-0 min-w-0 space-y-5 border-0 p-0 disabled:opacity-60">
          <div className="grid gap-4 sm:grid-cols-2">
            <label className={`${miniLabelClass} sm:col-span-2`} htmlFor="nutrition-dob">Ngày sinh *
              <input id="nutrition-dob" className={fieldClass} type="date" min="1900-01-01" max={todayLocalString()} required value={form.dateOfBirth} onChange={(event) => setField('dateOfBirth', event.target.value)} />
            </label>
            <fieldset className="sm:col-span-2">
              <legend className={miniLabelClass}>Giới tính sinh học *</legend>
              <div className="mt-2 grid grid-cols-2 gap-3">
                {([{ value: 'FEMALE', label: 'Nữ' }, { value: 'MALE', label: 'Nam' }] as const).map((item) => <label key={item.value} className={`flex cursor-pointer items-center gap-2 rounded-xl border px-4 py-3 text-sm font-semibold transition focus-within:ring-2 focus-within:ring-leaf-200 ${form.biologicalSex === item.value ? 'border-leaf-600 bg-leaf-50 text-leaf-800' : 'border-leaf-100 bg-paper text-ink-soft'}`}>
                  <input className="accent-leaf-700" type="radio" name="biologicalSex" required checked={form.biologicalSex === item.value} onChange={() => setField('biologicalSex', item.value)} />{item.label}
                </label>)}
              </div>
            </fieldset>
            <label className={miniLabelClass} htmlFor="nutrition-height">Chiều cao *
              <span className="relative block"><input id="nutrition-height" aria-invalid={Boolean(fieldErrors.heightCm)} aria-describedby={fieldErrors.heightCm ? 'nutrition-height-error' : undefined} className={`${fieldClass} pr-12 ${fieldErrors.heightCm ? 'border-red-500' : ''}`} type="number" min="100" max="250" step="0.1" required value={form.heightCm} onChange={(event) => setField('heightCm', event.target.value)} /><span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-xs font-medium text-ink-muted">cm</span></span>
              {fieldErrors.heightCm && <span id="nutrition-height-error" role="alert" className="mt-1 block text-xs font-medium text-red-700">{fieldErrors.heightCm}</span>}
            </label>
            <label className={miniLabelClass} htmlFor="nutrition-weight">Cân nặng *
              <span className="relative block"><input id="nutrition-weight" aria-invalid={Boolean(fieldErrors.weightKg)} aria-describedby={fieldErrors.weightKg ? 'nutrition-weight-error' : undefined} className={`${fieldClass} pr-12 ${fieldErrors.weightKg ? 'border-red-500' : ''}`} type="number" min="30" max="300" step="0.1" required value={form.weightKg} onChange={(event) => setField('weightKg', event.target.value)} /><span className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-xs font-medium text-ink-muted">kg</span></span>
              {fieldErrors.weightKg && <span id="nutrition-weight-error" role="alert" className="mt-1 block text-xs font-medium text-red-700">{fieldErrors.weightKg}</span>}
            </label>
            <label className={`${miniLabelClass} sm:col-span-2`} htmlFor="nutrition-activity">Mức độ vận động *
              <select id="nutrition-activity" className={fieldClass} required value={form.activityLevel} onChange={(event) => setField('activityLevel', event.target.value as ActivityLevel)}>
                <option value="" disabled>Chọn mức vận động</option>
                {activities.map((item) => <option key={item.value} value={item.value}>{item.label} — {item.description}</option>)}
              </select>
            </label>
            <label className={`${miniLabelClass} sm:col-span-2`} htmlFor="nutrition-goal">Mục tiêu dinh dưỡng chung (không bắt buộc)
              <select id="nutrition-goal" className={fieldClass} value={form.nutritionGoal} onChange={(event) => setField('nutritionGoal', event.target.value as NutritionGoal | '')}>
                <option value="">Chưa chọn mục tiêu</option>
                <option value="MAINTAIN_WEIGHT">Duy trì cân nặng</option>
                <option value="LOSE_WEIGHT">Giảm cân</option>
                <option value="GAIN_WEIGHT">Tăng cân</option>
                <option value="BUILD_MUSCLE">Tăng cơ</option>
                <option value="IMPROVE_HEALTH">Cải thiện sức khỏe chung</option>
                <option value="SUPPORT_TRAINING">Hỗ trợ tập luyện</option>
              </select>
              <span className="mt-1 block text-xs font-normal text-ink-muted">Mục tiêu này dùng cho gợi ý thực đơn theo hồ sơ. Bạn có thể để trống và chọn mục tiêu riêng khi lập lịch cho một tuần.</span>
            </label>
          </div>

          <div className="rounded-xl border border-leaf-100 bg-leaf-50/40 p-4 text-sm leading-6 text-ink-muted">
            Phạm vi hỗ trợ dinh dưỡng đã được bạn xác nhận riêng. Nếu tình trạng thay đổi, hãy cập nhật xác nhận trước khi tiếp tục sử dụng các tính năng này.
            {age !== null && age < 18 && <p role="alert" className="mt-2 rounded-xl bg-amber-100 p-3 text-sm font-medium text-amber-950">Ứng dụng hiện không hỗ trợ hồ sơ dinh dưỡng cho người dưới 18 tuổi.</p>}
            {age !== null && age > 120 && <p role="alert" className="mt-2 rounded-xl bg-amber-100 p-3 text-sm font-medium text-amber-950">Ứng dụng hiện chỉ hỗ trợ độ tuổi từ 18 đến 120.</p>}
          </div>

          <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-leaf-100 bg-paper p-4 text-sm leading-6 text-ink-soft">
            <input className="mt-1 h-4 w-4 shrink-0 accent-leaf-700" type="checkbox" required checked={form.consentAccepted} onChange={(event) => setField('consentAccepted', event.target.checked)} />
            <span>Tôi đồng ý lưu thông tin sức khỏe do mình tự khai báo để phục vụ các phép tính tham khảo trong hồ sơ dinh dưỡng. *</span>
          </label>

          {submitError && <p role="alert" className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-sm leading-5 text-red-800"><AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />{submitError}</p>}
          <button type="submit" disabled={saving || !eligible} className="inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-leaf-500 px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-leaf-600 focus:outline-none focus:ring-2 focus:ring-leaf-300 focus:ring-offset-2 disabled:cursor-not-allowed disabled:bg-raised sm:w-auto">
            {saving ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}{saving ? 'Đang lưu…' : 'Lưu hồ sơ'}
          </button>
          {savedResponse?.hasProfile && <button type="button" onClick={handleCalculate} disabled={calculating || saving || !eligible || hasUnsavedProfileChanges} className="ml-0 inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-leaf-300 bg-paper px-5 py-3 text-sm font-bold text-leaf-800 transition hover:bg-leaf-50 focus:outline-none focus:ring-2 focus:ring-leaf-200 disabled:cursor-not-allowed disabled:border-stone-200 disabled:text-stone-400 sm:ml-2 sm:w-auto">
            {calculating ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Calculator className="h-4 w-4" />}{calculating ? 'Đang tính…' : 'Tính chỉ số tham khảo'}
          </button>}
          {hasUnsavedProfileChanges && <p className="text-xs leading-5 text-amber-900">Lưu các thay đổi hồ sơ trước khi tính lại chỉ số tham khảo.</p>}
          </fieldset>
        </form>}
      </Card>

      <Card className="border-leaf-100 p-5 shadow-sm shadow-leaf-900/5 md:p-7">
        <div className="flex items-start justify-between gap-4"><div><h2 className="text-xl font-bold text-ink">Chỉ số tham khảo</h2><p className="mt-1 text-sm text-ink-muted">Dựa trên hồ sơ đã lưu của bạn.</p></div><span className="rounded-full bg-leaf-50 px-3 py-1 text-xs font-bold text-leaf-700">≈ Tham khảo</span></div>
        {loading ? <div className="flex items-center gap-2 py-16 text-sm text-ink-muted"><LoaderCircle className="h-4 w-4 animate-spin" /> Đang tải kết quả…</div>
          : result ? (() => {
            const bmiTheme = getBmiTheme(result.bmiCategory, result.bmi);
            return (
              <div className="mt-5">
                <div className={`rounded-2xl bg-gradient-to-br ${bmiTheme.cardGradient} p-5 transition-colors duration-200`}>
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className={`text-xs font-bold uppercase tracking-wide ${bmiTheme.labelColor}`}>Chỉ số BMI tham khảo</p>
                    <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1.5 text-xs font-bold shadow-2xs ${bmiTheme.badgeClass}`}>
                      <span className={`h-2 w-2 rounded-full ${bmiTheme.dotColor}`} aria-hidden="true" />
                      {result.bmiCategory} · tham khảo
                    </span>
                  </div>
                  <div className="mt-2 flex flex-wrap items-baseline gap-3">
                    <p className={`text-4xl font-extrabold tracking-tight ${bmiTheme.numberColor}`}>{formatApprox(result.bmi)}</p>
                    <span className="text-xs font-medium text-ink-muted">{bmiTheme.statusText}</span>
                  </div>
                  <p className="mt-2 text-xs leading-5 text-ink-muted">Chỉ số sàng lọc tham khảo từ chiều cao và cân nặng tự khai báo.</p>

                  {/* Thước đo phân vùng BMI trực quan */}
                  <div className="mt-4 border-t border-black/5 pt-3 dark:border-white/10">
                    <div className="mb-1.5 flex items-center justify-between text-[10.5px] font-semibold text-ink-muted">
                      <span className="flex items-center gap-1"><span className="inline-block h-1.5 w-1.5 rounded-full bg-red-500" />Thiếu &lt;18.5</span>
                      <span className="flex items-center gap-1"><span className="inline-block h-1.5 w-1.5 rounded-full bg-leaf-500" />Chuẩn 18.5–24.9</span>
                      <span className="flex items-center gap-1"><span className="inline-block h-1.5 w-1.5 rounded-full bg-amber-500" />Thừa 25–29.9</span>
                      <span className="flex items-center gap-1"><span className="inline-block h-1.5 w-1.5 rounded-full bg-rose-600" />Béo phì ≥30</span>
                    </div>
                    <div className="flex h-2 w-full gap-0.5 overflow-hidden rounded-full bg-raised">
                      <div className={`h-full flex-1 rounded-l-full bg-red-400 transition-opacity ${bmiTheme.activeSegment === 0 ? 'opacity-100 ring-2 ring-red-500' : 'opacity-30'}`} title="Thiếu cân (<18.5)" />
                      <div className={`h-full flex-[1.4] bg-leaf-500 transition-opacity ${bmiTheme.activeSegment === 1 ? 'opacity-100 ring-2 ring-leaf-600' : 'opacity-30'}`} title="Bình thường (18.5 - 24.9)" />
                      <div className={`h-full flex-1 bg-amber-400 transition-opacity ${bmiTheme.activeSegment === 2 ? 'opacity-100 ring-2 ring-amber-500' : 'opacity-30'}`} title="Thừa cân (25 - 29.9)" />
                      <div className={`h-full flex-1 rounded-r-full bg-rose-600 transition-opacity ${bmiTheme.activeSegment === 3 ? 'opacity-100 ring-2 ring-rose-700' : 'opacity-30'}`} title="Béo phì (≥30)" />
                    </div>
                  </div>
                </div>
                <div className="mt-4 rounded-2xl border border-leaf-100 p-4">
                  <div className="flex items-center justify-between gap-3"><div><p className="text-sm font-bold text-ink">Năng lượng mỗi ngày</p><p className="mt-1 text-xs text-ink-muted">Theo mức vận động và nhóm tuổi</p></div><p className="text-right text-lg font-extrabold text-leaf-800">{formatApprox(result.energyKcal, 0)} <span className="text-xs font-semibold">kcal</span></p></div>
                </div>
                <div className="mt-5 overflow-hidden rounded-2xl border border-leaf-100">
                  <div className="flex items-center justify-between bg-leaf-50 px-4 py-3"><h3 className="text-sm font-bold text-ink">9 chỉ tiêu tham khảo mỗi ngày</h3><span className="text-xs text-ink-muted">Định lượng ước tính</span></div>
                  <dl className="divide-y divide-leaf-50 px-4">
                    {result.dailyTargets.map((target) => <div key={target.key} className="flex items-start justify-between gap-4 py-3"><dt><span className="block text-sm font-semibold text-ink">{target.label}</span><span className="mt-0.5 block text-xs text-ink-muted">{target.referenceType}</span></dt><dd className="text-right text-sm font-bold tabular-nums text-leaf-800">{target.minimum === target.maximum ? formatApprox(target.minimum) : `${formatApprox(target.minimum)} – ${formatApprox(target.maximum)}`} <span className="text-xs font-semibold text-ink-muted">{target.unit}</span></dd></div>)}
                  </dl>
                </div>
                <div className="mt-4 space-y-2 rounded-xl bg-stone-50 p-4 text-xs leading-5 text-ink-muted">
                  {[sourceLink(result.energySource), ...result.dailyTargets.slice(0, 1).map((target) => sourceLink(target.source)), ...result.dailyTargets.slice(4).map((target) => sourceLink(target.source))]
                    .filter((source, index, sources) => sources.findIndex((item) => item.url === source.url) === index)
                    .map((source) => source.url ? <a key={source.url} href={source.url} target="_blank" rel="noreferrer" className="block break-words font-medium underline decoration-leaf-300 underline-offset-2 hover:text-leaf-800">Nguồn tham khảo: {source.label}</a> : <p key={source.label}>{source.label}</p>)}
                </div>
              </div>
            );
          })()
          : showOutOfScope || savedResponse?.outOfScopeReasons.length ? <div className="mt-5 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm leading-6 text-amber-950"><div className="flex gap-3"><AlertCircle className="mt-0.5 h-5 w-5 shrink-0" /><div><p className="font-bold">Chưa thể hiển thị hồ sơ dinh dưỡng</p><p className="mt-1">Các chỉ số BMI và 9 chỉ tiêu bị ẩn đối với trường hợp ngoài phạm vi hỗ trợ. Vui lòng tham khảo bác sĩ hoặc chuyên gia dinh dưỡng.</p></div></div></div>
            : <div className="mt-5 rounded-2xl border border-dashed border-leaf-200 bg-leaf-50/30 px-5 py-12 text-center"><div className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-paper text-leaf-700 ring-1 ring-leaf-100"><Activity className="h-6 w-6" /></div><p className="mt-4 font-bold text-ink">{savedResponse?.hasProfile ? 'Xác nhận để xem chỉ số' : 'Chưa có kết quả'}</p><p className="mx-auto mt-1 max-w-sm text-sm leading-6 text-ink-muted">{savedResponse?.hasProfile ? 'Trả lời “Không” cho cả ba câu hỏi về phạm vi hỗ trợ, sau đó chọn “Tính chỉ số tham khảo”.' : 'Hoàn thành hồ sơ và đồng ý lưu dữ liệu để xem BMI cùng các chỉ tiêu dinh dưỡng tham khảo.'}</p></div>}
        {calculationResponse?.eligible && result && <p className="mt-4 flex items-start gap-2 text-xs leading-5 text-ink-muted"><CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-leaf-700" />Giá trị chưa làm tròn được dùng để phân loại, hệ thống định dạng số để bạn dễ theo dõi.</p>}
        {!savedResponse && !loading && !loadError && <p className="mt-4 text-xs leading-5 text-ink-muted">Đăng nhập để đọc và lưu hồ sơ riêng tư của bạn.</p>}
      </Card>
    </div>
    </>}
  </PageContainer>;
}
