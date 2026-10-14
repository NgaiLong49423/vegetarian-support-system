import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, CalendarDays, Check, ChefHat, ChevronRight, LoaderCircle, Search, Sparkles } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button } from '../components/ui';
import { ApiError } from '../lib/apiClient';
import { getDietaryPreferences, type DietaryPreferences } from '../services/dietaryPreferencesApi';
import { mealPlanApi, recipesApi, type AiRecipeSuggestion, type AiWeeklyMenuPreview, type IngredientOption } from '../api/recipes';
import { useAuth } from '../components/AuthContext';

const mealLabels = { BREAKFAST: 'Sáng', LUNCH: 'Trưa', DINNER: 'Tối' } as const;
const allMeals = ['BREAKFAST', 'LUNCH', 'DINNER'] as const;

function mondayOf(date: Date): string {
  const result = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  result.setDate(result.getDate() - ((result.getDay() + 6) % 7));
  return `${result.getFullYear()}-${String(result.getMonth() + 1).padStart(2, '0')}-${String(result.getDate()).padStart(2, '0')}`;
}

function addDays(value: string, days: number): Date {
  const date = new Date(`${value}T12:00:00`);
  date.setDate(date.getDate() + days);
  return date;
}

function failureMessage(error: unknown, action: 'recommend' | 'weekly' | 'apply') {
  if (error instanceof ApiError) {
    if (error.code === 'DIETARY_PROFILE_INCOMPLETE') return 'Hoàn tất hồ sơ sở thích ăn uống để nhận gợi ý phù hợp.';
    if (error.code === 'AI_SUBSCRIPTION_REQUIRED') return 'Tạo thực đơn tuần cần gói Pro đang hoạt động.';
    if (error.code === 'AI_MENU_UNAVAILABLE') return 'Chưa có đủ công thức phù hợp để tạo thực đơn. Hãy chọn thêm nguyên liệu khác.';
    if (error.code === 'MEAL_PLAN_RECIPE_DUPLICATE') return 'Một món trong preview đã có ở đúng ngày và bữa của tuần này. Chọn tuần khác hoặc tạo preview mới.';
    if (error.code === 'RECIPE_NOT_FOUND' && action === 'apply') return 'Có công thức vừa không còn công khai. Hãy tạo preview mới rồi thử lại.';
    if (error.code === 'AI_PROVIDER_UNAVAILABLE' || error.code === 'AI_RESPONSE_INVALID') return 'Chưa thể tạo đề xuất lúc này. Bạn có thể thử lại sau.';
    return error.message;
  }
  return 'Không kết nối được máy chủ. Vui lòng thử lại.';
}

export function AiRecipePlanner({ weekStartDate, embedded = false, plannerMode = 'ingredients' }: {
  weekStartDate?: string;
  embedded?: boolean;
  plannerMode?: 'ingredients' | 'weekly';
}) {
  const { account, isAuthenticated } = useAuth();
  const [profile, setProfile] = useState<DietaryPreferences | null>(null);
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileError, setProfileError] = useState('');
  const [query, setQuery] = useState('');
  const [options, setOptions] = useState<IngredientOption[]>([]);
  const [selected, setSelected] = useState<IngredientOption[]>([]);
  const [searching, setSearching] = useState(false);
  const [suggestions, setSuggestions] = useState<AiRecipeSuggestion | null>(null);
  const [preview, setPreview] = useState<AiWeeklyMenuPreview | null>(null);
  const weekStart = weekStartDate ?? mondayOf(new Date());
  const [busy, setBusy] = useState<'recommend' | 'weekly' | 'apply' | null>(null);
  const [error, setError] = useState<{ message: string; code?: string } | null>(null);
  const [applied, setApplied] = useState(false);

  useEffect(() => {
    if (!isAuthenticated || account?.role === 'ADMIN') {
      setProfileLoading(false);
      return;
    }
    getDietaryPreferences().then(setProfile).catch(() => setProfileError('Không tải được hồ sơ sở thích ăn uống.'))
      .finally(() => setProfileLoading(false));
  }, [isAuthenticated, account?.role]);

  useEffect(() => {
    if (query.trim().length < 2) {
      setOptions([]);
      setSearching(false);
      return undefined;
    }
    const timer = window.setTimeout(() => {
      setSearching(true);
      recipesApi.searchIngredients(query.trim()).then((items) => setOptions(items))
        .catch(() => setOptions([])).finally(() => setSearching(false));
    }, 250);
    return () => window.clearTimeout(timer);
  }, [query]);

  const slotsByDay = useMemo(() => Array.from({ length: 7 }, (_, dayOffset) => ({
    dayOffset,
    date: addDays(weekStart, dayOffset),
    slots: allMeals.map((mealType) => preview?.slots.find((slot) => slot.dayOffset === dayOffset && slot.mealType === mealType) ?? null),
  })), [weekStart, preview]);

  const chooseIngredient = (option: IngredientOption) => {
    setSelected((current) => current.some((item) => item.ingredientId === option.ingredientId) ? current : [...current, option]);
    setSuggestions(null);
    setPreview(null);
    setApplied(false);
    setQuery('');
    setOptions([]);
    setError(null);
  };

  const runRecommendation = async (action: 'recommend' | 'weekly') => {
    if (!selected.length) return;
    setBusy(action);
    setError(null);
    setApplied(false);
    try {
      if (action === 'recommend') {
        setPreview(null);
        setSuggestions(await recipesApi.recommendByIngredients(selected.map((item) => item.ingredientId)));
      } else {
        setPreview(await recipesApi.createWeeklyMenuPreview(selected.map((item) => item.ingredientId)));
        setSuggestions(null);
      }
    } catch (cause) {
      const message = failureMessage(cause, action);
      setError({ message, code: cause instanceof ApiError ? cause.code : undefined });
    } finally {
      setBusy(null);
    }
  };

  const applyPreview = async () => {
    if (!preview || preview.slots.length !== 21) return;
    setBusy('apply');
    setError(null);
    setApplied(false);
    try {
      await mealPlanApi.applyAiWeeklyMenu(weekStart, preview.slots.map(({ dayOffset, mealType, recipeId }) => ({ dayOffset, mealType, recipeId })));
      setApplied(true);
    } catch (cause) {
      const message = failureMessage(cause, 'apply');
      setError({ message, code: cause instanceof ApiError ? cause.code : undefined });
    } finally {
      setBusy(null);
    }
  };

  if (!isAuthenticated || account?.role === 'ADMIN') {
    return <PageContainer className="py-12"><section className="mx-auto max-w-2xl rounded-3xl border border-brand-100 bg-paper p-8 text-center shadow-sm">
      <Sparkles className="mx-auto h-9 w-9 text-brand-600" /><h1 className="mt-4 text-2xl font-bold text-ink">Dành cho bạn</h1>
      <p className="mt-3 text-ink-muted">{account?.role === 'ADMIN' ? 'Tài khoản Quản trị viên không dùng được gợi ý dành cho thành viên.' : 'Đăng nhập bằng tài khoản thành viên để nhận gợi ý từ các công thức đã đăng.'}</p>
      {account?.role !== 'ADMIN' && <Button className="mt-5"><Link to="/dang-nhap">Đăng nhập</Link></Button>}
    </section></PageContainer>;
  }

  const profileReady = profile?.aiPersonalization.eligible === true;
  const content = <>
    {!embedded && <header className="mb-6 rounded-3xl border border-brand-100 bg-paper p-6 shadow-sm sm:p-8">
      <p className="flex items-center gap-2 text-sm font-semibold text-brand-700"><Sparkles className="h-4 w-4" />Dành cho bạn</p>
      <h1 className="mt-2 text-2xl font-bold text-ink">Món phù hợp với bạn</h1>
      <p className="mt-2 max-w-3xl text-sm leading-6 text-ink-muted">Chọn nguyên liệu đang có để tìm món phù hợp và xem trước thực đơn theo ngày.</p>
    </header>}

    {!profileLoading && !profileReady && <div role="alert" className="mb-5 flex flex-col gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950 sm:flex-row sm:items-center sm:justify-between">
      <span>{profileError || 'Hoàn tất hồ sơ sở thích ăn uống để xem đề xuất phù hợp.'}</span>
      <Link className="inline-flex shrink-0 items-center gap-1 font-semibold text-amber-900 underline" to="/ho-so/so-thich-an-uong">Mở hồ sơ <ChevronRight className="h-4 w-4" /></Link>
    </div>}
    {profileLoading && <p role="status" className="mb-5 flex items-center gap-2 text-sm text-ink-muted"><LoaderCircle className="h-4 w-4 animate-spin" />Đang kiểm tra hồ sơ…</p>}

    <section className="rounded-3xl border border-brand-100 bg-paper p-5 shadow-sm sm:p-7">
      <h2 className="flex items-center gap-2 text-lg font-semibold text-ink"><Search className="h-5 w-5 text-brand-600" />Nguyên liệu bạn đang có</h2>
      <label htmlFor="ai-ingredient-search" className="sr-only">Tìm nguyên liệu</label>
      <div className="relative mt-4 max-w-2xl">
        <input id="ai-ingredient-search" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Nhập tên nguyên liệu…" autoComplete="off" className="w-full rounded-xl border border-brand-200 bg-paper px-4 py-3 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100" />
        {searching && <LoaderCircle className="absolute right-3 top-3.5 h-4 w-4 animate-spin text-ink-muted" />}
        {options.length > 0 && <ul role="listbox" className="absolute z-20 mt-1 max-h-56 w-full overflow-y-auto rounded-xl border border-brand-100 bg-paper p-1 shadow-lg">
          {options.filter((option) => !selected.some((item) => item.ingredientId === option.ingredientId)).map((option) => <li key={option.ingredientId}>
            <button type="button" role="option" aria-selected="false" onClick={() => chooseIngredient(option)} className="w-full rounded-lg px-3 py-2 text-left text-sm text-ink hover:bg-brand-50">{option.name}</button>
          </li>)}
        </ul>}
      </div>
      <div className="mt-3 flex min-h-8 flex-wrap gap-2">
        {selected.map((ingredient) => <button key={ingredient.ingredientId} type="button" onClick={() => { setSelected((items) => items.filter((item) => item.ingredientId !== ingredient.ingredientId)); setSuggestions(null); setPreview(null); setApplied(false); }} className="rounded-full bg-brand-50 px-3 py-1.5 text-sm font-medium text-brand-800 hover:bg-brand-100" aria-label={`Bỏ ${ingredient.name}`}>{ingredient.name} <span aria-hidden="true">×</span></button>)}
        {!selected.length && <p className="text-sm text-ink-muted">Chọn ít nhất một nguyên liệu.</p>}
      </div>
      <div className="mt-5 flex flex-wrap gap-3">
        {plannerMode === 'ingredients' && <Button type="button" disabled={!profileReady || selected.length === 0 || busy !== null} onClick={() => runRecommendation('recommend')}>
          {busy === 'recommend' ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Search className="h-4 w-4" />} Tìm món phù hợp
        </Button>}
        {plannerMode === 'weekly' && <Button type="button" variant="secondary" disabled={!profileReady || selected.length === 0 || busy !== null} onClick={() => runRecommendation('weekly')}>
          {busy === 'weekly' ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <CalendarDays className="h-4 w-4" />} Tạo thực đơn tuần
        </Button>}
      </div>
    </section>

    {error && <div role="alert" className="mt-5 flex items-start gap-2 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-900"><AlertCircle className="mt-0.5 h-4 w-4 shrink-0" /><div>{error.message}{error.code === 'AI_SUBSCRIPTION_REQUIRED' && <Link to="/goi-ai" className="ml-1 font-semibold underline">Xem các gói</Link>}{error.code === 'DIETARY_PROFILE_INCOMPLETE' && <Link to="/ho-so/so-thich-an-uong" className="ml-1 font-semibold underline">Cập nhật hồ sơ</Link>}</div></div>}

    {plannerMode === 'ingredients' && suggestions && <section className="mt-7" aria-labelledby="suggestions-title">
      <h2 id="suggestions-title" className="mb-4 text-xl font-bold text-ink">Công thức phù hợp</h2>
      {suggestions.items.length === 0 ? <p className="rounded-2xl border border-brand-100 bg-paper p-5 text-sm text-ink-muted">Chưa tìm thấy công thức phù hợp với nguyên liệu và hồ sơ hiện tại.</p> : <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {suggestions.items.map(({ recipe, reason }) => {
          const coverUrl = recipe.media.find((media) => media.cover)?.url ?? recipe.media[0]?.url;
          return <article key={recipe.id} className="overflow-hidden rounded-2xl border border-brand-100 bg-paper shadow-sm">
            <Link to={`/cong-thuc/id/${recipe.id}`} className="block">
              {coverUrl ? <img src={coverUrl} alt={recipe.title} className="h-44 w-full object-cover" />
                : <div aria-hidden="true" className="flex h-44 items-center justify-center bg-brand-50 text-brand-600"><ChefHat className="h-10 w-10" /></div>}
              <div className="p-4"><h3 className="font-semibold text-ink">{recipe.title}</h3><p className="mt-2 text-sm text-ink-muted">{reason}</p></div>
            </Link>
          </article>;
        })}
      </div>}
    </section>}

    {plannerMode === 'weekly' && preview && <section className="mt-8" aria-labelledby="preview-title">
      <div className="mb-4 flex flex-col gap-3 rounded-2xl border border-brand-100 bg-paper p-4 sm:flex-row sm:items-center sm:justify-between">
        <div><h2 id="preview-title" className="text-xl font-bold text-ink">Thực đơn tuần dành cho bạn</h2><p className="mt-1 text-sm text-ink-muted">Xem lại thực đơn theo từng ngày. Thực đơn chỉ được thêm vào Lịch ăn sau khi bạn xác nhận.</p></div>
        <p className="text-sm font-medium text-ink">Tuần bắt đầu {new Date(`${weekStart}T12:00:00`).toLocaleDateString('vi-VN')}</p>
        <button type="button" onClick={() => { setPreview(null); setApplied(false); setError(null); }} className="text-sm font-semibold text-ink-muted underline hover:text-ink">Bỏ preview</button>
      </div>
      <div className="space-y-2">
        {slotsByDay.map(({ dayOffset, date, slots }) => <article key={dayOffset} className="rounded-xl border border-brand-100 bg-paper p-4">
          <h3 className="mb-3 font-semibold capitalize text-ink">{date.toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'long' })}</h3>
          <div className="divide-y divide-brand-100">{slots.map((slot, index) => <div key={allMeals[index]} className="flex min-h-16 items-center gap-3 py-2">
            <p className="w-20 shrink-0 text-sm font-medium text-ink-muted">{mealLabels[allMeals[index]]}</p>
            {slot ? <Link to={`/cong-thuc/id/${slot.recipeId}`} className="flex min-w-0 items-center gap-3 text-sm font-semibold text-ink hover:text-brand-700">{slot.coverUrl && <img src={slot.coverUrl} alt="" className="h-12 w-12 shrink-0 rounded-lg object-cover" />}<span>{slot.recipeTitle}</span></Link> : <p className="text-sm text-ink-muted">Chưa có món</p>}
          </div>)}</div>
        </article>)}
      </div>
      <div className="mt-5 flex flex-col items-start gap-3 sm:flex-row sm:items-center">
        <Button type="button" disabled={busy !== null || preview.slots.length !== 21 || !profileReady || applied} onClick={applyPreview}>
          {busy === 'apply' ? <LoaderCircle className="h-4 w-4 animate-spin" /> : applied ? <Check className="h-4 w-4" /> : <ChefHat className="h-4 w-4" />}{applied ? 'Đã thêm vào Lịch ăn' : 'Xác nhận thêm vào Lịch ăn'}
        </Button>
        {applied && <p role="status" className="text-sm text-green-700">Đã thêm đủ 21 bữa cho tuần đã chọn.</p>}
      </div>
    </section>}
  </>;
  return embedded ? content : <PageContainer className="py-8">{content}</PageContainer>;
}
