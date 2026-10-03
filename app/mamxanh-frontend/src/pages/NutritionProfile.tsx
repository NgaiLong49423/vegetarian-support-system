import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Activity, AlertCircle, CheckCircle2, Info, LoaderCircle, LockKeyhole, Save, Calculator } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Card } from '../components/ui';
import { ApiError, hasAccessToken } from '../lib/apiClient';
import { nutritionApi, type ActivityLevel, type BiologicalSex, type NutritionGoal, type NutritionProfileRequest, type NutritionProfileResponse } from '../api/nutrition';

type YesNo = '' | 'yes' | 'no';
type FormState = {
  dateOfBirth: string;
  biologicalSex: BiologicalSex | '';
  heightCm: string;
  weightKg: string;
  activityLevel: ActivityLevel | '';
  nutritionGoal: NutritionGoal | '';
  pregnant: YesNo;
  breastfeeding: YesNo;
  therapeuticDietRequired: YesNo;
  consentAccepted: boolean;
};

const emptyForm: FormState = {
  dateOfBirth: '', biologicalSex: '', heightCm: '', weightKg: '', activityLevel: '', nutritionGoal: '',
  pregnant: '', breastfeeding: '', therapeuticDietRequired: '', consentAccepted: false,
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
  return `Xấp xỉ ${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: fractionDigits, minimumFractionDigits: fractionDigits }).format(value)}`;
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
    nutritionGoal: response.profile.nutritionGoal,
  };
}

function sourceLink(source: string) {
  const separator = source.lastIndexOf(' — ');
  return separator < 0 ? { label: source, url: undefined } : { label: source.slice(0, separator), url: source.slice(separator + 3) };
}

export function NutritionProfile() {
  const authenticated = hasAccessToken();
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
    nutritionApi.getProfile()
      .then((response) => { if (active) { setSavedResponse(response); setForm(applyResponse(response)); } })
      .catch((error: unknown) => { if (active) setLoadError(loadErrorMessage(error)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [authenticated]);

  const age = useMemo(() => ageFromDate(form.dateOfBirth), [form.dateOfBirth]);
  const excluded = [form.pregnant, form.breastfeeding, form.therapeuticDietRequired].includes('yes');
  const answersComplete = form.pregnant !== '' && form.breastfeeding !== '' && form.therapeuticDietRequired !== '';
  const eligible = age !== null && age >= 18 && age <= 120 && answersComplete && !excluded;
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
  const showOutOfScope = age !== null && (age < 18 || age > 120) || excluded;

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

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitError('');
    if (!eligible) {
      setSubmitError(showOutOfScope
        ? 'Trường hợp này ngoài phạm vi hỗ trợ. Ứng dụng không lưu hồ sơ và không hiển thị BMI hoặc các chỉ tiêu.'
        : 'Vui lòng trả lời đầy đủ các câu hỏi về phạm vi hỗ trợ trước khi lưu.');
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
      nutritionGoal: form.nutritionGoal as NutritionGoal,
      pregnant: form.pregnant === 'yes',
      breastfeeding: form.breastfeeding === 'yes',
      therapeuticDietRequired: form.therapeuticDietRequired === 'yes',
      consentAccepted: true,
    };
    setSaving(true);
    try {
      const response = await nutritionApi.saveProfile(payload);
      setSavedResponse(response);
      setCalculationResponse(null);
      setForm((current) => ({ ...current, pregnant: 'no', breastfeeding: 'no', therapeuticDietRequired: 'no', consentAccepted: false }));
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

  const fieldClass = 'mt-2 w-full rounded-xl border border-leaf-200 bg-white px-3.5 py-3 text-sm text-ink outline-none transition focus:border-leaf-600 focus:ring-2 focus:ring-leaf-100 disabled:bg-stone-50';
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
      <div className="inline-flex items-center gap-2 rounded-full border border-leaf-100 bg-white px-3 py-2 text-xs font-semibold text-ink-soft"><LockKeyhole className="h-4 w-4 text-leaf-700" /> Hồ sơ riêng tư</div>
    </div>

    <aside role="note" className="mt-6 flex gap-3 rounded-2xl border border-teal-200 bg-teal-50/70 p-4 text-sm leading-6 text-ink-soft md:p-5">
      <Info aria-hidden="true" className="mt-0.5 h-5 w-5 shrink-0 text-teal-700" />
      <p><strong className="text-ink">Lưu ý:</strong> Đây là ứng dụng hỗ trợ tham khảo dinh dưỡng, không phải công cụ theo dõi sức khỏe lâm sàng. Thông tin do bạn tự khai báo và kết quả chỉ mang tính xấp xỉ tham khảo, dựa trên DRI/EER và nguồn NIH ODS. Lượng dinh dưỡng thực tế còn thay đổi theo loại thực phẩm, khẩu phần, bảo quản và cách chế biến. Các kết quả không phải chẩn đoán, điều trị hay kê đơn; không thay thế tư vấn của bác sĩ/chuyên gia dinh dưỡng; không có giá trị pháp lý hoặc chứng nhận y tế. Hệ thống không giám sát liên tục tình trạng sức khỏe.</p>
    </aside>

    {loadError && <div role="alert" className="mt-5 flex gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-950"><AlertCircle className="mt-0.5 h-5 w-5 shrink-0" /><p>{loadError}</p></div>}

    <div className="mt-6 grid items-start gap-6 xl:grid-cols-[minmax(0,1.05fr)_minmax(0,.95fr)]">
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
                {([{ value: 'FEMALE', label: 'Nữ' }, { value: 'MALE', label: 'Nam' }] as const).map((item) => <label key={item.value} className={`flex cursor-pointer items-center gap-2 rounded-xl border px-4 py-3 text-sm font-semibold transition focus-within:ring-2 focus-within:ring-leaf-200 ${form.biologicalSex === item.value ? 'border-leaf-600 bg-leaf-50 text-leaf-800' : 'border-leaf-100 bg-white text-ink-soft'}`}>
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
            <label className={`${miniLabelClass} sm:col-span-2`} htmlFor="nutrition-goal">Mục tiêu dinh dưỡng chung *
              <select id="nutrition-goal" className={fieldClass} required value={form.nutritionGoal} onChange={(event) => setField('nutritionGoal', event.target.value as NutritionGoal)}>
                <option value="" disabled>Chọn mục tiêu</option>
                <option value="MAINTAIN_WEIGHT">Duy trì cân nặng</option>
                <option value="IMPROVE_HEALTH">Cải thiện sức khỏe chung</option>
                <option value="SUPPORT_TRAINING">Hỗ trợ tập luyện</option>
              </select>
              <span className="mt-1 block text-xs font-normal text-ink-muted">Mục tiêu này không làm thay đổi các con số EER/DRI trong phiên bản hiện tại.</span>
            </label>
          </div>

          <fieldset className="rounded-2xl border border-leaf-100 bg-leaf-50/40 p-4">
            <legend className="px-1 text-sm font-bold text-ink">Xác nhận phạm vi hỗ trợ *</legend>
            <p className="mb-3 text-xs leading-5 text-ink-muted">Trả lời từng câu. Nếu có câu trả lời “Có”, ứng dụng sẽ chặn toàn bộ hồ sơ dinh dưỡng.</p>
            {([
              { key: 'pregnant', label: 'Bạn đang mang thai?' },
              { key: 'breastfeeding', label: 'Bạn đang cho con bú?' },
              { key: 'therapeuticDietRequired', label: 'Bạn cần chế độ ăn điều trị theo hướng dẫn y tế?' },
            ] as const).map((question) => <div key={question.key} className="flex flex-wrap items-center justify-between gap-3 border-t border-leaf-100 py-3 first:border-0">
              <span className="text-sm text-ink-soft">{question.label}</span>
              <div className="flex gap-4 text-sm">
                {([{ value: 'no', label: 'Không' }, { value: 'yes', label: 'Có' }] as const).map((option) => <label key={option.value} className="inline-flex cursor-pointer items-center gap-1.5"><input className="accent-leaf-700" type="radio" name={question.key} required checked={form[question.key] === option.value} onChange={() => setField(question.key, option.value)} />{option.label}</label>)}
              </div>
            </div>)}
            {age !== null && age < 18 && <p role="alert" className="mt-2 rounded-xl bg-amber-100 p-3 text-sm font-medium text-amber-950">Ứng dụng hiện không hỗ trợ hồ sơ dinh dưỡng cho người dưới 18 tuổi.</p>}
            {age !== null && age > 120 && <p role="alert" className="mt-2 rounded-xl bg-amber-100 p-3 text-sm font-medium text-amber-950">Ứng dụng hiện chỉ hỗ trợ độ tuổi từ 18 đến 120.</p>}
            {excluded && <p role="alert" className="mt-2 rounded-xl bg-amber-100 p-3 text-sm font-medium text-amber-950">Vui lòng tham khảo bác sĩ hoặc chuyên gia dinh dưỡng. Ứng dụng không lưu hồ sơ, tính BMI hoặc hiển thị 9 chỉ tiêu cho trường hợp này.</p>}
          </fieldset>

          <label className="flex cursor-pointer items-start gap-3 rounded-xl border border-leaf-100 bg-white p-4 text-sm leading-6 text-ink-soft">
            <input className="mt-1 h-4 w-4 shrink-0 accent-leaf-700" type="checkbox" required checked={form.consentAccepted} onChange={(event) => setField('consentAccepted', event.target.checked)} />
            <span>Tôi đồng ý lưu thông tin sức khỏe do mình tự khai báo để phục vụ các phép tính tham khảo trong hồ sơ dinh dưỡng. *</span>
          </label>

          {submitError && <p role="alert" className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-3 text-sm leading-5 text-red-800"><AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />{submitError}</p>}
          <button type="submit" disabled={saving || !eligible} className="inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-leaf-700 px-5 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-leaf-800 focus:outline-none focus:ring-2 focus:ring-leaf-300 focus:ring-offset-2 disabled:cursor-not-allowed disabled:bg-stone-300 sm:w-auto">
            {saving ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}{saving ? 'Đang lưu…' : 'Lưu hồ sơ'}
          </button>
          {savedResponse?.hasProfile && <button type="button" onClick={handleCalculate} disabled={calculating || saving || !eligible || hasUnsavedProfileChanges} className="ml-0 inline-flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-leaf-300 bg-white px-5 py-3 text-sm font-bold text-leaf-800 transition hover:bg-leaf-50 focus:outline-none focus:ring-2 focus:ring-leaf-200 disabled:cursor-not-allowed disabled:border-stone-200 disabled:text-stone-400 sm:ml-2 sm:w-auto">
            {calculating ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Calculator className="h-4 w-4" />}{calculating ? 'Đang tính…' : 'Tính chỉ số tham khảo'}
          </button>}
          {hasUnsavedProfileChanges && <p className="text-xs leading-5 text-amber-900">Lưu các thay đổi hồ sơ trước khi tính lại chỉ số tham khảo.</p>}
          </fieldset>
        </form>}
      </Card>

      <Card className="border-leaf-100 p-5 shadow-sm shadow-leaf-900/5 md:p-7">
        <div className="flex items-start justify-between gap-4"><div><h2 className="text-xl font-bold text-ink">Chỉ số tham khảo</h2><p className="mt-1 text-sm text-ink-muted">Dựa trên hồ sơ đã lưu của bạn.</p></div><span className="rounded-full bg-leaf-50 px-3 py-1 text-xs font-bold text-leaf-700">Xấp xỉ</span></div>
        {loading ? <div className="flex items-center gap-2 py-16 text-sm text-ink-muted"><LoaderCircle className="h-4 w-4 animate-spin" /> Đang tải kết quả…</div>
          : result ? <div className="mt-5">
            <div className="rounded-2xl bg-gradient-to-br from-leaf-50 to-white p-5 ring-1 ring-leaf-100">
              <p className="text-xs font-bold uppercase tracking-wide text-leaf-700">Chỉ số BMI tham khảo</p>
              <div className="mt-2 flex flex-wrap items-end justify-between gap-2"><p className="text-4xl font-extrabold tracking-tight text-leaf-800">{formatApprox(result.bmi)}</p><span className="mb-1 rounded-full bg-white px-3 py-1.5 text-xs font-bold text-leaf-800 ring-1 ring-leaf-200">{result.bmiCategory} · tham khảo</span></div>
              <p className="mt-2 text-xs leading-5 text-ink-muted">Chỉ số sàng lọc tham khảo từ chiều cao và cân nặng tự khai báo.</p>
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
          : showOutOfScope || savedResponse?.outOfScopeReasons.length ? <div className="mt-5 rounded-2xl border border-amber-200 bg-amber-50 p-5 text-sm leading-6 text-amber-950"><div className="flex gap-3"><AlertCircle className="mt-0.5 h-5 w-5 shrink-0" /><div><p className="font-bold">Chưa thể hiển thị hồ sơ dinh dưỡng</p><p className="mt-1">Các chỉ số BMI và 9 chỉ tiêu bị ẩn đối với trường hợp ngoài phạm vi hỗ trợ. Vui lòng tham khảo bác sĩ hoặc chuyên gia dinh dưỡng.</p></div></div></div>
            : <div className="mt-5 rounded-2xl border border-dashed border-leaf-200 bg-leaf-50/30 px-5 py-12 text-center"><div className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-white text-leaf-700 ring-1 ring-leaf-100"><Activity className="h-6 w-6" /></div><p className="mt-4 font-bold text-ink">{savedResponse?.hasProfile ? 'Xác nhận để xem chỉ số' : 'Chưa có kết quả'}</p><p className="mx-auto mt-1 max-w-sm text-sm leading-6 text-ink-muted">{savedResponse?.hasProfile ? 'Trả lời “Không” cho cả ba câu hỏi về phạm vi hỗ trợ, sau đó chọn “Tính chỉ số tham khảo”.' : 'Hoàn thành hồ sơ và đồng ý lưu dữ liệu để xem BMI cùng các chỉ tiêu dinh dưỡng tham khảo.'}</p></div>}
        {calculationResponse?.eligible && result && <p className="mt-4 flex items-start gap-2 text-xs leading-5 text-ink-muted"><CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-leaf-700" />Giá trị chưa làm tròn được dùng để phân loại; giao diện chỉ định dạng số để dễ đọc.</p>}
        {!savedResponse && !loading && !loadError && <p className="mt-4 text-xs leading-5 text-ink-muted">Đăng nhập để đọc và lưu hồ sơ riêng tư của bạn.</p>}
      </Card>
    </div>
  </PageContainer>;
}
