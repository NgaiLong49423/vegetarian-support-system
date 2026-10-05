import { useEffect, useId, useState, type FormEvent, type KeyboardEvent, type ReactNode } from 'react';
import { LoaderCircle, Plus, X } from 'lucide-react';
import { Button } from './ui';
import { fieldMessages, toProblem } from '../lib/problem';
import {
  suggestIngredients,
  type CookingDifficulty,
  type DietaryPreferences,
  type IngredientSuggestion,
  type SaveDietaryPreferencesPayload,
  type VegetarianType,
} from '../services/dietaryPreferencesApi';

export const VEGETARIAN_TYPES: { value: VegetarianType; label: string; description: string }[] = [
  { value: 'VEGAN', label: 'Thuần chay (Vegan)', description: 'Không thịt, cá, trứng, sữa, mật ong' },
  { value: 'LACTO', label: 'Chay có sữa (Lacto)', description: 'Có dùng sữa và chế phẩm từ sữa' },
  { value: 'OVO', label: 'Chay có trứng (Ovo)', description: 'Có dùng trứng' },
  { value: 'LACTO_OVO', label: 'Chay có trứng và sữa (Lacto-Ovo)', description: 'Có dùng cả trứng và sữa' },
];

const DIFFICULTIES: { value: CookingDifficulty; label: string }[] = [
  { value: 'EASY', label: 'Dễ' },
  { value: 'MEDIUM', label: 'Trung bình' },
  { value: 'HARD', label: 'Nâng cao' },
];

const MAX_ITEMS = 30;
const AVOID_REQUIRED = 'Chọn ít nhất một nguyên liệu cần tránh hoặc xác nhận không có dị ứng/kiêng cử.';
const DISLIKE_REQUIRED = 'Chọn ít nhất một món hoặc nguyên liệu không thích hoặc xác nhận không có.';

type FormState = {
  vegetarianType: VegetarianType | '';
  avoidItems: string[];
  avoidNone: boolean;
  dislikeItems: string[];
  dislikeNone: boolean;
  cuisinePreference: string;
  maxCookingTimeMinutes: string;
  preferredDifficulty: CookingDifficulty | '';
};

type Errors = Partial<Record<'vegetarianType' | 'avoid' | 'dislike' | 'cuisinePreference' | 'maxCookingTimeMinutes' | 'form', string>>;

function fromProfile(profile: DietaryPreferences | null): FormState {
  return {
    vegetarianType: profile?.vegetarianType ?? '',
    avoidItems: profile?.avoid.items.map((item) => item.name) ?? [],
    avoidNone: profile?.avoid.noneConfirmed ?? false,
    dislikeItems: profile?.dislike.items.map((item) => item.name) ?? [],
    dislikeNone: profile?.dislike.noneConfirmed ?? false,
    cuisinePreference: profile?.cuisinePreference ?? '',
    maxCookingTimeMinutes: profile?.maxCookingTimeMinutes ? String(profile.maxCookingTimeMinutes) : '',
    preferredDifficulty: profile?.preferredDifficulty ?? '',
  };
}

const clean = (value: string) => value.normalize('NFC').replace(/[\s ]+/g, ' ').trim();
const sameName = (a: string, b: string) => clean(a).toLowerCase() === clean(b).toLowerCase();

/** Mirrors the Backend rules (BR-31) so most mistakes are shown before sending. */
function validate(form: FormState): Errors {
  const errors: Errors = {};
  if (!form.vegetarianType) errors.vegetarianType = 'Chọn một loại ăn chay.';
  if (!form.avoidNone && form.avoidItems.length === 0) errors.avoid = AVOID_REQUIRED;
  if (!form.dislikeNone && form.dislikeItems.length === 0) errors.dislike = DISLIKE_REQUIRED;
  const conflict = form.avoidItems.find((avoid) => form.dislikeItems.some((dislike) => sameName(avoid, dislike)));
  if (conflict) errors.dislike = `"${conflict}" không thể vừa là nguyên liệu cần tránh vừa là món không thích.`;
  if (form.cuisinePreference.trim().length > 200) errors.cuisinePreference = 'Khẩu vị tối đa 200 ký tự.';
  if (form.maxCookingTimeMinutes) {
    const minutes = Number(form.maxCookingTimeMinutes);
    if (!Number.isInteger(minutes) || minutes < 1 || minutes > 1440) errors.maxCookingTimeMinutes = 'Nhập số phút từ 1 đến 1440.';
  }
  return errors;
}

function toPayload(form: FormState): SaveDietaryPreferencesPayload {
  return {
    vegetarianType: form.vegetarianType as VegetarianType,
    avoid: { noneConfirmed: form.avoidNone && form.avoidItems.length === 0, items: form.avoidItems },
    dislike: { noneConfirmed: form.dislikeNone && form.dislikeItems.length === 0, items: form.dislikeItems },
    cuisinePreference: form.cuisinePreference.trim() || null,
    maxCookingTimeMinutes: form.maxCookingTimeMinutes ? Number(form.maxCookingTimeMinutes) : null,
    preferredDifficulty: form.preferredDifficulty || null,
  };
}

/** Maps a ProblemDetail from the Backend onto the form fields. */
function serverErrors(error: unknown): Errors {
  const problem = toProblem(error);
  if (!problem) return { form: 'Không thể kết nối dịch vụ. Vui lòng thử lại.' };
  if (problem.code === 'INGREDIENT_PREFERENCE_CONFLICT') return { dislike: problem.detail };
  if (problem.code === 'VALIDATION_FAILED' && problem.errors?.length) {
    const errors: Errors = {};
    for (const [field, message] of Object.entries(fieldMessages(problem))) {
      const key = field.split(/[.[]/)[0] as keyof Errors;
      errors[key in { vegetarianType: 1, avoid: 1, dislike: 1, cuisinePreference: 1, maxCookingTimeMinutes: 1 } ? key : 'form'] = message;
    }
    return errors;
  }
  return { form: problem.detail ?? 'Chưa lưu được sở thích. Vui lòng thử lại.' };
}

const fieldClass = 'mt-2 w-full rounded-xl border border-leaf-200 bg-white px-3.5 py-3 text-sm text-ink outline-none transition focus:border-leaf-600 focus:ring-2 focus:ring-leaf-100 disabled:bg-stone-50';
const labelClass = 'block text-sm font-semibold text-ink';
const errorClass = 'mt-1 block text-xs font-medium text-red-700';

type IngredientListProps = {
  id: string;
  legend: string;
  hint: string;
  noneLabel: string;
  examples: string[];
  items: string[];
  none: boolean;
  error?: string;
  onItemsChange: (items: string[]) => void;
  onNoneChange: (none: boolean) => void;
};

/** One required list: names typed or picked from the standard catalog, or the explicit "none" box. */
function IngredientList({ id, legend, hint, noneLabel, examples, items, none, error, onItemsChange, onNoneChange }: IngredientListProps) {
  const [text, setText] = useState('');
  const [suggestions, setSuggestions] = useState<IngredientSuggestion[]>([]);
  const errorId = `${id}-error`;

  useEffect(() => {
    const query = clean(text);
    if (!query) {
      setSuggestions([]);
      return;
    }
    let active = true;
    const timer = window.setTimeout(() => {
      suggestIngredients(query)
        .then((found) => { if (active) setSuggestions(found); })
        .catch(() => { if (active) setSuggestions([]); });
    }, 250);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [text]);

  const add = (raw: string) => {
    const name = clean(raw);
    if (!name || name.length > 200 || items.length >= MAX_ITEMS) return;
    if (!items.some((item) => sameName(item, name))) onItemsChange([...items, name]);
    onNoneChange(false);
    setText('');
    setSuggestions([]);
  };

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      add(text);
    }
  };

  const quickPicks = examples.filter((example) => !items.some((item) => sameName(item, example)));

  return (
    <fieldset aria-describedby={error ? errorId : undefined} className={`rounded-2xl border p-4 ${error ? 'border-red-300 bg-red-50/40' : 'border-leaf-100 bg-leaf-50/40'}`}>
      <legend className="px-1 text-sm font-bold text-ink">{legend} *</legend>
      <p className="text-xs leading-5 text-ink-muted">{hint}</p>

      {items.length > 0 && (
        <ul aria-label={legend} className="mt-3 flex flex-wrap gap-2">
          {items.map((item) => (
            <li key={item} className="inline-flex items-center gap-1 rounded-full bg-white px-3 py-1.5 text-sm font-semibold text-ink ring-1 ring-leaf-200">
              {item}
              <button type="button" aria-label={`Bỏ ${item}`} onClick={() => onItemsChange(items.filter((current) => current !== item))} className="rounded-full p-0.5 text-ink-muted hover:bg-leaf-100 hover:text-ink focus-visible:outline-2 focus-visible:outline-leaf-600">
                <X aria-hidden="true" className="h-3.5 w-3.5" />
              </button>
            </li>
          ))}
        </ul>
      )}

      <div className="mt-3 flex gap-2">
        <label htmlFor={`${id}-input`} className="sr-only">Thêm vào danh sách {legend.toLowerCase()}</label>
        <input id={`${id}-input`} value={text} disabled={none} maxLength={200} onChange={(event) => setText(event.target.value)} onKeyDown={onKeyDown} placeholder="Nhập tên rồi nhấn Thêm" autoComplete="off" className={`${fieldClass} mt-0`} />
        <Button type="button" variant="outline" disabled={none || !clean(text)} onClick={() => add(text)} aria-label={`Thêm mục vào ${legend.toLowerCase()}`}>
          <Plus aria-hidden="true" className="h-4 w-4" /> <span className="hidden sm:inline">Thêm</span>
        </Button>
      </div>

      {!none && (suggestions.length > 0 || quickPicks.length > 0) && (
        <div className="mt-3 flex flex-wrap items-center gap-2 text-xs">
          <span className="text-ink-muted">{suggestions.length > 0 ? 'Trong danh mục:' : 'Gợi ý:'}</span>
          {(suggestions.length > 0 ? suggestions.map((suggestion) => suggestion.name) : quickPicks).map((name) => (
            <button key={name} type="button" onClick={() => add(name)} className="rounded-full border border-leaf-200 bg-white px-3 py-1 font-semibold text-leaf-700 hover:border-leaf-400 focus-visible:outline-2 focus-visible:outline-leaf-600">
              + {name}
            </button>
          ))}
        </div>
      )}

      <label className="mt-4 flex cursor-pointer items-center gap-3 text-sm text-ink-soft">
        <input type="checkbox" className="h-4 w-4 accent-leaf-700" checked={none} onChange={(event) => { onNoneChange(event.target.checked); if (event.target.checked) onItemsChange([]); }} />
        {noneLabel}
      </label>
      {error && <span id={errorId} role="alert" className={errorClass}>{error}</span>}
    </fieldset>
  );
}

type Props = {
  profile: DietaryPreferences | null;
  submitLabel: string;
  disabled?: boolean;
  onSubmit: (payload: SaveDietaryPreferencesPayload) => Promise<void>;
  secondaryAction?: ReactNode;
};

/** FR-31 dietary-preference questionnaire, shared by Onboarding (UC-31.1) and Settings (UC-31.3). */
export function DietaryPreferencesForm({ profile, submitLabel, disabled = false, onSubmit, secondaryAction }: Props) {
  const [form, setForm] = useState<FormState>(() => fromProfile(profile));
  const [errors, setErrors] = useState<Errors>({});
  const [submitting, setSubmitting] = useState(false);
  const formId = useId();

  useEffect(() => setForm(fromProfile(profile)), [profile]);

  const set = <K extends keyof FormState>(key: K, value: FormState[K]) => {
    setForm((current) => ({ ...current, [key]: value }));
    setErrors((current) => ({ ...current, [key.startsWith('avoid') ? 'avoid' : key.startsWith('dislike') ? 'dislike' : key]: undefined, form: undefined }));
  };

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;
    setSubmitting(true);
    try {
      await onSubmit(toPayload(form));
    } catch (error) {
      setErrors(serverErrors(error));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form noValidate onSubmit={submit} aria-label="Sở thích ăn uống" className="space-y-5">
      <fieldset disabled={disabled || submitting} className="m-0 min-w-0 space-y-5 border-0 p-0">
        <fieldset aria-describedby={errors.vegetarianType ? `${formId}-type-error` : undefined}>
          <legend className={labelClass}>1. Bạn ăn chay theo kiểu nào? *</legend>
          <div className="mt-2 grid gap-3 sm:grid-cols-2">
            {VEGETARIAN_TYPES.map((type) => (
              <label key={type.value} className={`grid cursor-pointer grid-cols-[auto_1fr] items-start gap-x-3 rounded-xl border px-4 py-3 text-sm transition focus-within:ring-2 focus-within:ring-leaf-100 ${form.vegetarianType === type.value ? 'border-leaf-600 bg-leaf-50' : 'border-leaf-100 bg-white hover:border-leaf-300'}`}>
                <input className="row-span-2 mt-1 accent-leaf-700" type="radio" name={`${formId}-type`} checked={form.vegetarianType === type.value} onChange={() => set('vegetarianType', type.value)} />
                <span className="font-semibold text-ink">{type.label}</span>
                <span className="text-xs text-ink-muted">{type.description}</span>
              </label>
            ))}
          </div>
          {errors.vegetarianType && <span id={`${formId}-type-error`} role="alert" className={errorClass}>{errors.vegetarianType}</span>}
        </fieldset>

        <IngredientList id={`${formId}-avoid`} legend="2. Nguyên liệu cần tránh (dị ứng/kiêng)" hint="Ví dụ: đậu phộng, gluten, nấm. Để trống không có nghĩa là bạn không dị ứng." noneLabel="Tôi không có dị ứng/kiêng cử" examples={['Đậu phộng', 'Gluten', 'Nấm', 'Đậu nành']} items={form.avoidItems} none={form.avoidNone} error={errors.avoid} onItemsChange={(items) => set('avoidItems', items)} onNoneChange={(none) => set('avoidNone', none)} />

        <IngredientList id={`${formId}-dislike`} legend="3. Món hoặc nguyên liệu không thích" hint="Ví dụ: mướp đắng, rau mùi. Một nguyên liệu chỉ nằm trong một danh sách." noneLabel="Tôi không có món không thích" examples={['Mướp đắng', 'Rau mùi', 'Sầu riêng']} items={form.dislikeItems} none={form.dislikeNone} error={errors.dislike} onItemsChange={(items) => set('dislikeItems', items)} onNoneChange={(none) => set('dislikeNone', none)} />

        <fieldset className="rounded-2xl border border-leaf-100 p-4">
          <legend className="px-1 text-sm font-bold text-ink">4. Sở thích khác (tùy chọn)</legend>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className={`${labelClass} sm:col-span-2`} htmlFor={`${formId}-cuisine`}>Khẩu vị ẩm thực
              <input id={`${formId}-cuisine`} className={fieldClass} maxLength={200} value={form.cuisinePreference} onChange={(event) => set('cuisinePreference', event.target.value)} placeholder="Ví dụ: món chay truyền thống Việt Nam" aria-invalid={Boolean(errors.cuisinePreference)} />
              {errors.cuisinePreference && <span role="alert" className={errorClass}>{errors.cuisinePreference}</span>}
            </label>
            <label className={labelClass} htmlFor={`${formId}-time`}>Thời gian nấu tối đa (phút)
              <input id={`${formId}-time`} className={fieldClass} type="number" inputMode="numeric" min={1} max={1440} value={form.maxCookingTimeMinutes} onChange={(event) => set('maxCookingTimeMinutes', event.target.value)} placeholder="Ví dụ: 30" aria-invalid={Boolean(errors.maxCookingTimeMinutes)} />
              {errors.maxCookingTimeMinutes && <span role="alert" className={errorClass}>{errors.maxCookingTimeMinutes}</span>}
            </label>
            <label className={labelClass} htmlFor={`${formId}-difficulty`}>Độ khó mong muốn
              <select id={`${formId}-difficulty`} className={fieldClass} value={form.preferredDifficulty} onChange={(event) => set('preferredDifficulty', event.target.value as CookingDifficulty | '')}>
                <option value="">Không chọn</option>
                {DIFFICULTIES.map((difficulty) => <option key={difficulty.value} value={difficulty.value}>{difficulty.label}</option>)}
              </select>
            </label>
          </div>
        </fieldset>
      </fieldset>

      {errors.form && <p role="alert" className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-800">{errors.form}</p>}

      <div className="flex flex-wrap items-center gap-3">
        <Button type="submit" disabled={disabled || submitting}>
          {submitting && <LoaderCircle aria-hidden="true" className="h-4 w-4 animate-spin" />} {submitLabel}
        </Button>
        {secondaryAction}
      </div>
    </form>
  );
}
