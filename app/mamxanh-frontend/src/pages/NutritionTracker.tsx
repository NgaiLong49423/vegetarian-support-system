import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle, AlertTriangle, CheckCircle2, ChevronLeft, ChevronRight, Download,
  FileText, Info, RefreshCw, Utensils,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Badge, Button, Card, ProgressBar } from '../components/ui';
import { Modal } from '../components/Modal';
import { mealPlanApi, type MealPlanNutrition as NutritionData } from '../api/recipes';
import { asApiError, asApiErrorAsync, type ApiError } from '../lib/apiClient';
import { mondayOf } from '../components/WeekCalendar';
import { useAuth } from '../components/AuthContext';

type Scope = 'day' | 'week';
const mealNames = { BREAKFAST: 'Bữa sáng', LUNCH: 'Bữa trưa', DINNER: 'Bữa tối' } as const;
const numberFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 });
const todayKey = () => {
  const today = new Date();
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
};
const dateAtNoon = (key: string) => new Date(`${key}T12:00:00`);
const dateKey = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
const formatDate = (key: string, options: Intl.DateTimeFormatOptions) => dateAtNoon(key).toLocaleDateString('vi-VN', options);
const displayValue = (value: number | null | undefined, unit: string) => value == null ? 'Chưa đủ dữ liệu' : `${numberFormat.format(value)} ${unit}`;

export function NutritionTracker() {
  const { account, isAuthenticated } = useAuth();
  const [selectedDate, setSelectedDate] = useState(todayKey);
  const [scope, setScope] = useState<Scope>('day');
  const [data, setData] = useState<NutritionData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ApiError | null>(null);
  const [retry, setRetry] = useState(0);
  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState<ApiError | null>(null);
  const [analysisOpen, setAnalysisOpen] = useState(false);
  const weekStart = useMemo(() => mondayOf(dateAtNoon(selectedDate)), [selectedDate]);

  useEffect(() => {
    let active = true;
    if (!isAuthenticated || account?.role === 'ADMIN') {
      setLoading(false);
      setError(null);
      setData(null);
      return () => { active = false; };
    }
    setLoading(true);
    setError(null);
    setData(null);
    mealPlanApi.nutrition(weekStart)
      .then((result) => { if (active) setData(result); })
      .catch((cause) => { if (active) setError(asApiError(cause)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [account?.role, isAuthenticated, weekStart, retry]);

  const selectedDay = data?.days.find((day) => day.date === selectedDate) ?? null;
  const weekHasMeals = Boolean(data?.days.some((day) => day.entries.length > 0));
  const dayHasMeals = Boolean(selectedDay?.entries.length);
  const canExport = !loading && !error && (scope === 'day' ? dayHasMeals : weekHasMeals);

  async function exportPdf() {
    if (!canExport) return;
    setExporting(true);
    setExportError(null);
    try {
      const blob = await mealPlanApi.nutritionPdf(weekStart, scope === 'day' ? selectedDate : undefined);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `bao-cao-dinh-duong-${scope === 'day' ? selectedDate : weekStart}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch (cause) {
      setExportError(await asApiErrorAsync(cause));
    } finally {
      setExporting(false);
    }
  }

  function shiftWeek(offset: number) {
    const date = dateAtNoon(selectedDate);
    date.setDate(date.getDate() + offset * 7);
    setSelectedDate(dateKey(date));
  }

  const targets = data?.targets ?? [];
  const dailyNutrients = selectedDay?.nutrients ?? {};
  const values = scope === 'day' ? dailyNutrients : data?.weeklyAverage ?? {};
  const macroKeys = ['ENERGY_KCAL', 'PROTEIN_G', 'CARBOHYDRATE_G', 'TOTAL_FAT_G'];
  const toneFor = (index: number) => index % 2 === 0 ? 'brand' as const : 'leaf' as const;
  const meals = selectedDay?.entries ?? [];
  const energyByMeal = Object.fromEntries(Object.keys(mealNames).map((mealType) => [
    mealType,
    meals.filter((entry) => entry.mealType === mealType)
      .map((entry) => entry.nutrients.ENERGY_KCAL)
      .filter((value): value is number => value != null)
      .reduce((sum, value) => sum + value, 0),
  ])) as Record<keyof typeof mealNames, number>;
  const knownMealEnergy = Object.values(energyByMeal).reduce((sum, value) => sum + value, 0);

  return (
    <PageContainer className="py-8">
      <nav className="mb-4 text-sm text-ink-muted">
        <Link to="/trang-chu" className="hover:text-brand-600">Trang chủ</Link> / <span className="font-medium text-brand-600">Theo dõi dinh dưỡng</span>
      </nav>

      <div className="mb-6 flex flex-col items-start justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold uppercase tracking-[0.15em] text-brand-600">Dinh dưỡng tham khảo</span>
            <Badge tone="leaf">Khoa học</Badge>
          </div>
          <h1 className="mt-1 text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">Theo dõi Dinh dưỡng Thực đơn</h1>
          <p className="mt-1 max-w-2xl text-sm text-ink-muted">Tổng hợp chỉ tiêu từ lịch ăn đã lưu. Đây là khẩu phần dự kiến, không ngầm hiểu món đã lên lịch là món đã ăn.</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" disabled={!canExport || exporting} onClick={() => void exportPdf()}>
            <Download className="h-4 w-4" /> {exporting ? 'Đang tạo PDF…' : 'Xuất báo cáo PDF'}
          </Button>
          <Button onClick={() => setAnalysisOpen(true)} disabled={!data || loading || !!error}>
            <FileText className="h-4 w-4" /> Phân tích tổng thể
          </Button>
        </div>
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-2" role="group" aria-label="Phạm vi báo cáo">
        <Button variant={scope === 'day' ? 'secondary' : 'ghost'} aria-pressed={scope === 'day'} onClick={() => setScope('day')}>Theo ngày</Button>
        <Button variant={scope === 'week' ? 'secondary' : 'ghost'} aria-pressed={scope === 'week'} onClick={() => setScope('week')}>So sánh &amp; thống kê 7 ngày</Button>
        <span className="text-sm text-ink-muted">{scope === 'day' ? `Ngày ${formatDate(selectedDate, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}` : `Tuần ${formatDate(weekStart, { day: 'numeric', month: 'numeric' })} – ${formatDate(dateKey(new Date(dateAtNoon(weekStart).setDate(dateAtNoon(weekStart).getDate() + 6))), { day: 'numeric', month: 'numeric', year: 'numeric' })}`}</span>
      </div>

      {exportError && <div role="alert" className="mb-5 flex items-start gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-900">
        <AlertCircle className="mt-0.5 h-5 w-5 shrink-0" />
        <div className="flex-1"><p>{exportError.message}</p>{exportError.code === 'NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED' && <p className="mt-1">Hãy <Link to="/ho-so/dinh-duong" className="font-semibold underline">xác nhận phạm vi hỗ trợ dinh dưỡng</Link> trước khi xuất báo cáo.</p>}</div>
        <Button size="sm" variant="ghost" onClick={() => void exportPdf()}>Thử lại</Button>
      </div>}

      <div className="mb-6 flex items-start gap-3 rounded-2xl border border-leaf-200 bg-leaf-50/60 p-4 text-xs leading-relaxed text-ink-soft">
        <Info className="mt-0.5 h-4 w-4 shrink-0 text-leaf-600" />
        <div><strong className="text-leaf-800">Lưu ý về dinh dưỡng:</strong><p className="mt-0.5 text-ink-muted">{data?.disclaimer ?? 'Thông tin chỉ mang tính tham khảo, không thay thế tư vấn y tế.'}</p></div>
      </div>

      {!isAuthenticated && <div role="alert" className="mb-6 rounded-xl border border-brand-200 bg-paper p-5 text-sm text-ink-soft">Đăng nhập để xem dinh dưỡng từ lịch ăn của bạn. <Link to="/dang-nhap" className="font-semibold text-brand-700 underline">Đăng nhập</Link></div>}
      {account?.role === 'ADMIN' && <div role="alert" className="mb-6 rounded-xl border border-brand-200 bg-paper p-5 text-sm text-ink-soft">Chức năng này dành cho Member. Hãy đăng nhập bằng tài khoản Member để xem lịch ăn cá nhân.</div>}
      {loading && isAuthenticated && <p role="status" className="mb-6 rounded-xl bg-paper p-8 text-center text-sm text-ink-muted"><RefreshCw className="mx-auto mb-2 h-5 w-5 animate-spin" />Đang tải dinh dưỡng từ lịch ăn…</p>}
      {error && isAuthenticated && account?.role !== 'ADMIN' && <div role="alert" className="mb-6 rounded-xl border border-rose-200 bg-rose-50 p-5 text-sm text-rose-900">
        <p>{error.message}</p>
        {error.code === 'NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED' && <p className="mt-2">Để dùng tính năng, hãy <Link to="/ho-so/dinh-duong" className="font-semibold underline">xác nhận phạm vi hỗ trợ trong Hồ sơ dinh dưỡng</Link>.</p>}
        {error.code === 'NUTRITION_ELIGIBILITY_INELIGIBLE' && <p className="mt-2">Tính năng không áp dụng theo phạm vi dinh dưỡng đã xác nhận. Có thể xem lại <Link to="/ho-so/dinh-duong" className="font-semibold underline">Hồ sơ dinh dưỡng</Link>.</p>}
        <Button size="sm" variant="outline" className="mt-3" onClick={() => setRetry((value) => value + 1)}>Thử tải lại</Button>
      </div>}

      {data && !loading && !error && <>
        <div className="mb-6 flex items-center gap-2">
          <button type="button" aria-label="Tuần trước" onClick={() => shiftWeek(-1)} className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-brand-200 bg-paper text-ink-soft transition-colors hover:bg-brand-50"><ChevronLeft className="h-4 w-4" /></button>
          <div className="flex flex-1 gap-2 overflow-x-auto scrollbar-thin">
            {data.days.map((day) => {
              const today = day.date === todayKey();
              const selected = day.date === selectedDate;
              return <button key={day.date} type="button" aria-label={`Ngày ${formatDate(day.date, { weekday: 'long', day: 'numeric', month: 'numeric' })}`} onClick={() => setSelectedDate(day.date)} aria-pressed={selected} className={`flex min-w-[104px] flex-col items-center rounded-xl border px-3 py-2.5 transition-all duration-200 motion-safe:hover:-translate-y-0.5 ${selected ? 'border-brand-600 bg-brand-600 text-on-brand shadow-sm shadow-brand-600/20' : 'border-brand-100 bg-paper text-ink-soft hover:border-brand-300'}`}>
                <span className="whitespace-nowrap text-xs font-semibold">{formatDate(day.date, { weekday: 'long' })}{today ? ' • Hôm nay' : ''}</span>
                <span className="text-lg font-extrabold">{formatDate(day.date, { day: 'numeric', month: 'numeric' })}</span>
                <span className={`text-[10px] ${selected ? 'text-white/80' : 'text-ink-muted'}`}>{displayValue(day.nutrients.ENERGY_KCAL, 'kcal')}</span>
              </button>;
            })}
          </div>
          <button type="button" aria-label="Tuần sau" onClick={() => shiftWeek(1)} className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-brand-200 bg-paper text-ink-soft transition-colors hover:bg-brand-50"><ChevronRight className="h-4 w-4" /></button>
        </div>

        {((scope === 'day' && !dayHasMeals) || (scope === 'week' && !weekHasMeals)) && <div className="mb-6 rounded-xl border border-brand-200 bg-paper p-5 text-sm text-ink-soft">
          <p>{scope === 'day' ? 'Ngày này chưa có món trong lịch ăn.' : 'Tuần này chưa có món trong lịch ăn.'} Thêm món vào <Link to={`/ke-hoach?weekStartDate=${weekStart}${scope === 'day' ? `&mealDate=${selectedDate}` : ''}`} className="font-semibold text-brand-700 underline">Lịch ăn</Link> để xem số liệu và xuất PDF.</p>
        </div>}

        {data.incompleteEntries.length > 0 && <div role="status" className="mb-6 flex items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" /><div><strong>Một phần dữ liệu dinh dưỡng còn thiếu.</strong><p className="mt-1">Các số liệu tính được vẫn được hiển thị và đưa vào báo cáo; xem danh sách món thiếu dữ liệu trong phần phân tích.</p></div>
        </div>}

        <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {macroKeys.map((key, index) => {
            const metric = targets.find((target) => target.key === key);
            if (!metric) return null;
            const actual = values[key];
            const goal = metric.minimum;
            const pct = actual == null || goal == null || goal <= 0 ? 0 : Math.min(100, Math.round(actual / goal * 100));
            return <Card key={key} hover className="p-5"><div className="mb-2 flex items-start justify-between gap-2"><p className="text-sm font-semibold text-ink-soft">{metric.label}</p>{goal != null && <Badge tone={toneFor(index)}>{pct}%</Badge>}</div>
              <p className="text-2xl font-extrabold text-ink">{displayValue(actual, metric.unit)}{goal != null && <span className="text-sm font-semibold text-ink-muted"> / {numberFormat.format(goal)} {metric.unit}</span>}</p>
              {goal != null && <div className="my-3"><ProgressBar pct={pct} tone={toneFor(index)} /></div>}
              <p className="text-xs text-ink-muted">{actual == null ? 'Chưa đủ dữ liệu để tổng hợp chỉ tiêu này.' : scope === 'week' ? 'Trung bình mỗi ngày trong 7 ngày.' : 'Tổng từ khẩu phần đã lên lịch trong ngày.'}</p>
            </Card>;
          })}
        </div>

        <Card className="mb-6 p-5">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2"><h3 className="font-bold text-ink">Phân bố năng lượng theo bữa ăn</h3><span className="text-sm text-ink-muted">{displayValue(knownMealEnergy || null, 'kcal')}</span></div>
          {knownMealEnergy > 0 ? <><div className="flex h-4 overflow-hidden rounded-full">{Object.entries(energyByMeal).map(([mealType, amount], index) => <div key={mealType} className={index === 0 ? 'bg-amber-400' : index === 1 ? 'bg-brand-500' : 'bg-brand-700'} style={{ width: `${amount / knownMealEnergy * 100}%` }} />)}</div>
            <div className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-xs text-ink-muted">{Object.entries(mealNames).map(([type, label]) => <span key={type}>{label} · {displayValue(energyByMeal[type as keyof typeof mealNames] || null, 'kcal')} ({Math.round(energyByMeal[type as keyof typeof mealNames] / knownMealEnergy * 100)}%)</span>)}</div></> : <p className="text-sm text-ink-muted">Chưa đủ dữ liệu năng lượng để chia theo bữa.</p>}
        </Card>

        <h3 className="mb-3 flex items-center gap-2 text-lg font-extrabold text-ink"><Utensils className="h-5 w-5" />Thực đơn ngày {formatDate(selectedDate, { day: 'numeric', month: 'numeric' })}</h3>
        {meals.length ? <div className="mb-6 grid gap-4 md:grid-cols-3">{meals.map((meal) => <Card key={meal.entryId} hover className="overflow-hidden"><div className="flex h-36 items-center justify-center bg-raised text-ink-muted"><Utensils className="h-8 w-8" /></div><div className="p-4"><div className="mb-2"><Badge tone="brand" soft={false}>{mealNames[meal.mealType]}</Badge></div><p className="line-clamp-2 font-bold text-ink">{meal.recipeTitle}</p><p className="mt-2 text-sm text-ink-muted">{numberFormat.format(meal.plannedServings)} phần dự kiến</p><div className="mt-3 flex items-center justify-between gap-2 border-t border-brand-50 pt-3"><span className="text-sm font-extrabold text-brand-600">{displayValue(meal.nutrients.ENERGY_KCAL, 'kcal')}</span><Link to={`/cong-thuc/id/${meal.recipeId}`} className="text-sm font-semibold text-brand-600 hover:underline">Chi tiết công thức →</Link></div>{!meal.complete && <p className="mt-2 text-xs text-amber-800">Một số chỉ tiêu còn thiếu; số liệu hiện có vẫn được dùng.</p>}</div></Card>)}</div> : <div className="mb-6 rounded-xl bg-paper p-6 text-center text-sm text-ink-muted">Ngày này chưa có món lên lịch.</div>}
      </>}

      <Modal open={analysisOpen} onClose={() => setAnalysisOpen(false)} title="Phân tích dinh dưỡng tổng thể" size="lg">
        {!data ? <p className="text-sm text-ink-muted">Chưa có dữ liệu để phân tích.</p> : <div className="space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-2"><div className="flex rounded-xl bg-brand-100/60 p-1"><button type="button" onClick={() => setScope('day')} aria-pressed={scope === 'day'} className={`rounded-lg px-3 py-1.5 text-xs font-bold ${scope === 'day' ? 'bg-paper text-ink shadow-sm' : 'text-ink-soft'}`}>Theo ngày</button><button type="button" onClick={() => setScope('week')} aria-pressed={scope === 'week'} className={`rounded-lg px-3 py-1.5 text-xs font-bold ${scope === 'week' ? 'bg-paper text-ink shadow-sm' : 'text-ink-soft'}`}>7 ngày</button></div><p className="text-xs text-ink-muted">{scope === 'day' ? formatDate(selectedDate, { weekday: 'long', day: 'numeric', month: 'long' }) : `Tuần bắt đầu ${formatDate(weekStart, { day: 'numeric', month: 'long' })}`}</p></div>
          <div className="overflow-x-auto rounded-xl border border-brand-100"><table className="w-full text-left text-xs sm:text-sm"><thead><tr className="border-b border-brand-100 bg-brand-50/50 text-[11px] font-bold uppercase text-ink-muted"><th className="p-3">Chỉ tiêu</th><th className="p-3">Thực đơn</th><th className="p-3">Mức tham khảo</th><th className="p-3">Tình trạng</th></tr></thead><tbody className="divide-y divide-brand-50">{targets.map((target) => { const actual = (scope === 'day' ? selectedDay?.nutrients : data.weeklyAverage)?.[target.key]; const hasValue = actual != null; const within = hasValue && target.minimum != null && actual >= target.minimum && (target.referenceType !== 'AMDR' || target.maximum == null || actual <= target.maximum); const reference = target.minimum == null ? 'Chưa có mức tham khảo' : target.referenceType === 'AMDR' && target.maximum != null ? `${numberFormat.format(target.minimum)}–${numberFormat.format(target.maximum)} ${target.unit}` : `${numberFormat.format(target.minimum)} ${target.unit}`; return <tr key={target.key}><td className="p-3 font-semibold text-ink">{target.label}</td><td className="p-3 font-bold text-ink">{displayValue(actual, target.unit)}</td><td className="p-3 text-ink-muted">{reference}</td><td className="p-3">{!hasValue ? <Badge tone="neutral">Chưa đủ dữ liệu</Badge> : within ? <Badge tone="leaf"><CheckCircle2 className="h-3 w-3" />Trong khoảng tham khảo</Badge> : <Badge tone="brand">Ngoài mức tham khảo</Badge>}</td></tr>; })}</tbody></table></div>
          {data.incompleteEntries.length > 0 && <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs text-amber-900"><strong>Món còn thiếu dữ liệu dinh dưỡng</strong><ul className="mt-2 list-disc pl-5">{data.incompleteEntries.filter((entry) => scope === 'week' || entry.date === selectedDate).map((entry) => <li key={entry.entryId}>{entry.recipeTitle}: {entry.ingredientsMissingData.join(', ') || 'chưa đủ dữ liệu công thức'}</li>)}</ul></div>}
          <p className="text-xs text-ink-muted">{data.referenceBasis}. {data.disclaimer}</p>
        </div>}
      </Modal>
    </PageContainer>
  );
}
