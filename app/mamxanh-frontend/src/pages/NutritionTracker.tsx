import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle, AlertTriangle, Calendar, CheckCircle2, ChevronLeft, ChevronRight, Download,
  FileText, Info, PieChart, RefreshCw, Utensils,
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
const mealBadgeTones = { BREAKFAST: 'amber', LUNCH: 'leaf', DINNER: 'brand' } as const;
const mealColorClasses: Record<keyof typeof mealNames, string> = {
  BREAKFAST: 'bg-amber-500',
  LUNCH: 'bg-leaf-600',
  DINNER: 'bg-brand-600',
};
const numberFormat = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 });
const todayKey = () => {
  const today = new Date();
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;
};
const dateAtNoon = (key: string) => new Date(`${key}T12:00:00`);
const dateKey = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
const formatDate = (key: string, options: Intl.DateTimeFormatOptions) => dateAtNoon(key).toLocaleDateString('vi-VN', options);
const displayValue = (value: number | null | undefined, unit: string) => value == null ? 'Chưa đủ dữ liệu' : `≈ ${numberFormat.format(value)} ${unit}`;

export function NutritionTracker() {
  const { account, isAuthenticated } = useAuth();
  const [selectedDate, setSelectedDate] = useState(todayKey);
  const [scope, setScope] = useState<Scope>('day');
  const [data, setData] = useState<NutritionData | null>(null);
  const [recipeCovers, setRecipeCovers] = useState<Record<number, string | null>>({});
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
    setRecipeCovers({});
    mealPlanApi.getWeek(weekStart)
      .then((plan) => {
        if (active) setRecipeCovers(Object.fromEntries(plan.entries.map((entry) => [entry.entryId, entry.recipeCoverUrl])));
      })
      .catch(() => { if (active) setRecipeCovers({}); });
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
  const primaryKeys = ['ENERGY_KCAL', 'PROTEIN_G', 'CARBOHYDRATE_G', 'TOTAL_FAT_G'];
  const micronutrientKeys = ['FIBER_G', 'CALCIUM_MG', 'IRON_MG', 'VITAMIN_B12_MCG', 'ZINC_MG'];
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
      {/* 1. Breadcrumb Navigation */}
      <nav aria-label="Điều hướng đường dẫn" className="mb-4 flex items-center gap-2 text-xs sm:text-sm text-ink-muted">
        <Link to="/trang-chu" className="transition-colors hover:text-brand-600">Trang chủ</Link>
        <span className="text-ink-muted/50">/</span>
        <span className="font-semibold text-brand-600">Theo dõi dinh dưỡng</span>
      </nav>

      {/* 2. Page Header & Action Bar */}
      <div className="mb-6 flex flex-col items-start justify-between gap-4 border-b border-brand-100/60 pb-6 sm:flex-row sm:items-end">
        <div className="space-y-1.5 max-w-2xl">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold uppercase tracking-[0.16em] text-brand-600">Dinh dưỡng tham khảo</span>
            <Badge tone="leaf" soft={false} className="text-[11px] font-bold">Khoa học</Badge>
          </div>
          <h1 className="text-2xl font-black tracking-tight text-ink sm:text-3xl lg:text-4xl">
            Theo dõi Dinh dưỡng Thực đơn
          </h1>
          <p className="text-sm leading-relaxed text-ink-muted">
            Tổng hợp chỉ tiêu từ lịch ăn đã lưu. Đây là khẩu phần dự kiến, không ngầm hiểu món đã lên lịch là món đã ăn.
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2.5 shrink-0">
          <Button
            variant="outline"
            disabled={!canExport || exporting}
            onClick={() => void exportPdf()}
            className="shadow-2xs"
          >
            <Download className="h-4 w-4" /> {exporting ? 'Đang tạo PDF…' : 'Xuất báo cáo PDF'}
          </Button>
          <Button
            onClick={() => setAnalysisOpen(true)}
            disabled={!data || loading || !!error}
            className="shadow-sm shadow-brand-600/20"
          >
            <FileText className="h-4 w-4" /> Phân tích tổng thể
          </Button>
        </div>
      </div>

      {/* 3. Scope Selector & Date Context */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-100/80 bg-paper/60 p-2 sm:p-2.5 shadow-2xs backdrop-blur-xs">
        <div className="inline-flex rounded-xl bg-raised/90 p-1 border border-brand-100/60 shadow-inner" role="group" aria-label="Phạm vi báo cáo">
          <Button
            variant={scope === 'day' ? 'primary' : 'ghost'}
            size="sm"
            aria-pressed={scope === 'day'}
            onClick={() => setScope('day')}
            className={scope === 'day' ? 'shadow-sm shadow-brand-600/25' : 'text-ink-soft hover:text-ink'}
          >
            Theo ngày
          </Button>
          <Button
            variant={scope === 'week' ? 'primary' : 'ghost'}
            size="sm"
            aria-pressed={scope === 'week'}
            onClick={() => setScope('week')}
            className={scope === 'week' ? 'shadow-sm shadow-brand-600/25' : 'text-ink-soft hover:text-ink'}
          >
            So sánh &amp; thống kê 7 ngày
          </Button>
        </div>

        <div className="flex items-center gap-2 px-2 text-xs sm:text-sm font-semibold text-ink-muted">
          <Calendar className="h-4 w-4 text-brand-600 shrink-0" />
          <span>
            {scope === 'day'
              ? `Ngày ${formatDate(selectedDate, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}`
              : `Tuần ${formatDate(weekStart, { day: 'numeric', month: 'numeric' })} – ${formatDate(dateKey(new Date(dateAtNoon(weekStart).setDate(dateAtNoon(weekStart).getDate() + 6))), { day: 'numeric', month: 'numeric', year: 'numeric' })}`}
          </span>
        </div>
      </div>

      {/* Export Error Alert */}
      {exportError && (
        <div role="alert" className="mb-5 flex items-start gap-3 rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-900 shadow-2xs">
          <AlertCircle className="mt-0.5 h-5 w-5 shrink-0" />
          <div className="flex-1">
            <p className="font-semibold">{exportError.message}</p>
            {exportError.code === 'NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED' && (
              <p className="mt-1">
                Hãy <Link to="/ho-so/dinh-duong" className="font-bold underline">xác nhận phạm vi hỗ trợ dinh dưỡng</Link> trước khi xuất báo cáo.
              </p>
            )}
          </div>
          <Button size="sm" variant="ghost" onClick={() => void exportPdf()}>Thử lại</Button>
        </div>
      )}

      {/* 4. Nutritional Disclaimer Banner */}
      <div className="mb-6 flex items-start gap-3.5 rounded-2xl border border-leaf-200/80 bg-leaf-50/70 p-4 sm:p-4.5 text-xs sm:text-sm leading-relaxed text-ink-soft shadow-2xs">
        <div className="rounded-xl bg-leaf-100 p-2 text-leaf-700 shrink-0 mt-0.5">
          <Info className="h-4 w-4" />
        </div>
        <div className="min-w-0">
          <strong className="block font-bold text-leaf-900 mb-0.5">Lưu ý về dinh dưỡng:</strong>
          <p className="text-xs leading-normal text-ink-muted">
            {data?.disclaimer ?? 'Thông tin chỉ mang tính tham khảo, không thay thế tư vấn y tế.'}
          </p>
        </div>
      </div>

      {/* State Banners: Guest / Admin / Loading / Error */}
      {!isAuthenticated && (
        <div role="alert" className="mb-6 rounded-2xl border border-brand-200 bg-paper p-6 text-sm text-ink-soft shadow-2xs">
          Đăng nhập để xem dinh dưỡng từ lịch ăn của bạn.{' '}
          <Link to="/dang-nhap" className="font-bold text-brand-600 underline">Đăng nhập</Link>
        </div>
      )}
      {account?.role === 'ADMIN' && (
        <div role="alert" className="mb-6 rounded-2xl border border-brand-200 bg-paper p-6 text-sm text-ink-soft shadow-2xs">
          Chức năng này dành cho Member. Hãy đăng nhập bằng tài khoản Member để xem lịch ăn cá nhân.
        </div>
      )}
      {loading && isAuthenticated && (
        <p role="status" className="mb-6 rounded-2xl border border-brand-100 bg-paper p-10 text-center text-sm text-ink-muted shadow-2xs">
          <RefreshCw className="mx-auto mb-3 h-6 w-6 animate-spin text-brand-600" />
          Đang tải dinh dưỡng từ lịch ăn…
        </p>
      )}
      {error && isAuthenticated && account?.role !== 'ADMIN' && (
        <div role="alert" className="mb-6 rounded-2xl border border-rose-200 bg-rose-50 p-6 text-sm text-rose-900 shadow-2xs">
          <p className="font-bold">{error.message}</p>
          {error.code === 'NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED' && (
            <p className="mt-2">
              Để dùng tính năng, hãy <Link to="/ho-so/dinh-duong" className="font-bold underline">xác nhận phạm vi hỗ trợ trong Hồ sơ dinh dưỡng</Link>.
            </p>
          )}
          {error.code === 'NUTRITION_ELIGIBILITY_INELIGIBLE' && (
            <p className="mt-2">
              Tính năng không áp dụng theo phạm vi dinh dưỡng đã xác nhận. Có thể xem lại <Link to="/ho-so/dinh-duong" className="font-bold underline">Hồ sơ dinh dưỡng</Link>.
            </p>
          )}
          <Button size="sm" variant="outline" className="mt-3" onClick={() => setRetry((value) => value + 1)}>
            Thử tải lại
          </Button>
        </div>
      )}

      {/* Main Dashboard Content */}
      {data && !loading && !error && (
        <>
          {/* 5. Weekly Date Selector (Evenly distributed 7 cards across full width) */}
          <div className="mb-6 flex items-center gap-2 sm:gap-3 w-full" aria-label="Bộ chọn ngày trong tuần">
            <button
              type="button"
              aria-label="Tuần trước"
              onClick={() => shiftWeek(-1)}
              className="flex h-11 w-11 sm:h-12 sm:w-12 shrink-0 items-center justify-center rounded-xl border border-brand-200 bg-paper text-ink-soft transition-all duration-200 hover:border-brand-400 hover:bg-brand-50 hover:text-brand-700 active:scale-95 shadow-2xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 cursor-pointer"
            >
              <ChevronLeft className="h-5 w-5" />
            </button>
            
            <div className="grid flex-1 grid-cols-7 gap-2 sm:gap-2.5 md:gap-3 min-w-0 max-md:flex max-md:overflow-x-auto max-md:pb-1 max-md:scrollbar-thin">
              {data.days.map((day) => {
                const today = day.date === todayKey();
                const selected = day.date === selectedDate;
                return (
                  <button
                    key={day.date}
                    type="button"
                    aria-label={`Ngày ${formatDate(day.date, { weekday: 'long', day: 'numeric', month: 'numeric' })}`}
                    onClick={() => setSelectedDate(day.date)}
                    aria-pressed={selected}
                    className={`group flex flex-col items-center justify-center rounded-xl border py-3 px-2 sm:px-2.5 min-h-[86px] sm:min-h-[92px] w-full min-w-0 max-md:min-w-[108px] max-md:flex-1 text-center transition-all duration-200 motion-safe:hover:-translate-y-0.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 cursor-pointer ${
                      selected
                        ? 'border-brand-600 bg-brand-600 text-white shadow-md shadow-brand-600/30 ring-2 ring-brand-500/20'
                        : 'border-brand-100/90 bg-paper text-ink-soft hover:border-brand-300 hover:bg-brand-50/40 hover:text-ink shadow-2xs'
                    }`}
                  >
                    <span className={`flex items-center justify-center gap-1 text-[11px] sm:text-xs font-semibold truncate w-full ${selected ? 'text-white/95' : 'text-ink-muted group-hover:text-ink'}`}>
                      <span className="truncate">{formatDate(day.date, { weekday: 'long' })}</span>
                      {today && (
                        <span className={`inline-block h-1.5 w-1.5 shrink-0 rounded-full ${selected ? 'bg-white' : 'bg-brand-600'}`} title="Hôm nay" />
                      )}
                    </span>
                    <span className="mt-1 text-base sm:text-lg font-black tracking-tight">
                      {formatDate(day.date, { day: 'numeric', month: 'numeric' })}
                    </span>
                    <span className={`mt-0.5 text-[10px] sm:text-[11px] font-medium truncate w-full ${selected ? 'text-white/85' : 'text-ink-muted'}`}>
                      {displayValue(day.nutrients.ENERGY_KCAL, 'kcal')}
                    </span>
                  </button>
                );
              })}
            </div>

            <button
              type="button"
              aria-label="Tuần sau"
              onClick={() => shiftWeek(1)}
              className="flex h-11 w-11 sm:h-12 sm:w-12 shrink-0 items-center justify-center rounded-xl border border-brand-200 bg-paper text-ink-soft transition-all duration-200 hover:border-brand-400 hover:bg-brand-50 hover:text-brand-700 active:scale-95 shadow-2xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 cursor-pointer"
            >
              <ChevronRight className="h-5 w-5" />
            </button>
          </div>

          {/* Empty-Day Meal Notice */}
          {((scope === 'day' && !dayHasMeals) || (scope === 'week' && !weekHasMeals)) && (
            <div className="mb-6 rounded-2xl border border-brand-200/80 bg-paper/70 p-5 text-sm text-ink-soft shadow-2xs">
              <p>
                {scope === 'day' ? 'Ngày này chưa có món trong lịch ăn.' : 'Tuần này chưa có món trong lịch ăn.'}{' '}
                Thêm món vào <Link to={`/ke-hoach?weekStartDate=${weekStart}${scope === 'day' ? `&mealDate=${selectedDate}` : ''}`} className="font-bold text-brand-600 underline">Lịch ăn</Link> để xem số liệu và xuất PDF.
              </p>
            </div>
          )}

          {/* Incomplete Data Status Warning */}
          {data.incompleteEntries.length > 0 && (
            <div role="status" className="mb-6 flex items-start gap-3 rounded-2xl border border-amber-200 bg-amber-50/90 p-4 text-sm text-amber-900 shadow-2xs">
              <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" />
              <div>
                <strong>Một phần dữ liệu dinh dưỡng còn thiếu.</strong>
                <p className="mt-1 text-xs leading-relaxed text-amber-800">
                  Các số liệu tính được vẫn được hiển thị và đưa vào báo cáo, xem danh sách món thiếu dữ liệu trong phần phân tích.
                </p>
              </div>
            </div>
          )}

          {/* 6. Main nutrition metrics and compact micronutrients */}
          <section aria-label="Tổng quan dinh dưỡng" className="mb-6 space-y-4">
            <div>
              <h3 className="text-base font-extrabold text-ink">Chỉ tiêu chính</h3>
              <p className="mt-1 text-xs text-ink-muted">{scope === 'week' ? 'Giá trị trung bình mỗi ngày trong 7 ngày.' : 'Tổng từ khẩu phần dự kiến đã lên lịch trong ngày.'}</p>
            </div>
            <div className="grid gap-3.5 sm:grid-cols-2 lg:grid-cols-4">
            {primaryKeys.map((key, index) => {
              const metric = targets.find((target) => target.key === key);
              if (!metric) return null;
              const actual = values[key];
              const goal = metric.minimum;
              const pct = actual == null || goal == null || goal <= 0 ? 0 : Math.min(100, Math.round(actual / goal * 100));
              return (
                <Card
                  key={key}
                  hover
                  className="h-full min-h-48 p-5 border-brand-100/90 shadow-2xs transition-all duration-200"
                >
                  <div className="flex h-full flex-col">
                    <div className="mb-2.5 flex min-h-7 items-center justify-between gap-2">
                      <p className="text-xs sm:text-sm font-bold text-ink-soft uppercase tracking-wider">{metric.label}</p>
                      {goal != null && (
                        <Badge tone={toneFor(index)} soft={pct === 0} className="font-extrabold text-[11px]">
                          {pct}%
                        </Badge>
                      )}
                    </div>
                    <div className="flex min-h-10 items-end">
                      <span className="text-2xl leading-tight font-black tracking-tight text-ink sm:text-3xl">
                        {displayValue(actual, metric.unit)}
                      </span>
                    </div>
                    <p className="mt-1 min-h-5 text-xs font-semibold text-ink-muted">
                      {goal == null ? 'Chưa có mức tham khảo' : `/ ${numberFormat.format(goal)} ${metric.unit}`}
                    </p>
                    <div className="mt-3">
                      {goal != null ? <ProgressBar pct={pct} tone={toneFor(index)} /> : <div aria-hidden="true" className="h-2 rounded-full bg-brand-100" />}
                    </div>
                  </div>
                </Card>
              );
            })}
            </div>
            <Card className="p-4 sm:p-5 border-brand-100/90 shadow-2xs">
              <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                <div>
                  <h3 className="font-bold text-ink">Chất xơ &amp; vi chất</h3>
                  <p className="mt-0.5 text-xs text-ink-muted">Các chỉ tiêu bổ sung theo khẩu phần đã lên lịch.</p>
                </div>
                <span className="rounded-full bg-raised px-2.5 py-1 text-[11px] font-semibold text-ink-muted">{micronutrientKeys.length} chỉ tiêu</span>
              </div>
              <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-5">
                {micronutrientKeys.map((key) => {
                  const metric = targets.find((target) => target.key === key);
                  if (!metric) return null;
                  const actual = values[key];
                  const goal = metric.minimum;
                  const hasGoal = goal != null && goal > 0;
                  const pct = actual == null || !hasGoal ? null : Math.min(100, Math.round(actual / goal * 100));
                  return <div key={key} className="min-w-0 rounded-xl border border-brand-100 bg-raised/45 p-3">
                    <div className="flex items-start justify-between gap-2">
                      <p className="text-xs font-semibold text-ink-soft">{metric.label}</p>
                      {pct != null && <span className="shrink-0 text-[11px] font-bold text-brand-700">{pct}%</span>}
                    </div>
                    <p className="mt-2 truncate text-base font-extrabold text-ink" title={actual == null ? 'Chưa đủ dữ liệu' : `${numberFormat.format(actual)} ${metric.unit}`}>
                      {actual == null ? 'Chưa đủ dữ liệu' : `${numberFormat.format(actual)} ${metric.unit}`}
                    </p>
                    {hasGoal && <p className="mt-0.5 text-[11px] text-ink-muted">Mức tham khảo: {numberFormat.format(goal)} {metric.unit}</p>}
                    {pct != null && <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-brand-100"><div className="h-full rounded-full bg-leaf-600" style={{ width: `${pct}%` }} /></div>}
                  </div>;
                })}
              </div>
            </Card>
          </section>

          {/* 7. Energy Distribution Panel */}
          <Card className="mb-6 p-5 sm:p-6 border-brand-100/90 shadow-2xs">
            <div className="mb-3.5 flex flex-wrap items-center justify-between gap-2 border-b border-brand-100/60 pb-3">
              <div className="flex items-center gap-2">
                <PieChart className="h-4 w-4 text-brand-600" />
                <h3 className="font-bold text-ink text-sm sm:text-base">Phân bố năng lượng theo bữa ăn</h3>
              </div>
              <span className="text-xs sm:text-sm font-semibold text-ink-muted">{displayValue(knownMealEnergy || null, 'kcal')}</span>
            </div>
            {knownMealEnergy > 0 ? (
              <>
                <div className="flex h-4 overflow-hidden rounded-full shadow-inner bg-raised">
                  {Object.entries(energyByMeal).map(([mealType, amount]) => (
                    <div
                      key={mealType}
                      className={mealColorClasses[mealType as keyof typeof mealNames]}
                      style={{ width: `${amount / knownMealEnergy * 100}%` }}
                    />
                  ))}
                </div>
                <div className="mt-3.5 flex flex-wrap gap-x-6 gap-y-2 text-xs text-ink-muted">
                  {Object.entries(mealNames).map(([type, label]) => {
                    const amount = energyByMeal[type as keyof typeof mealNames] || 0;
                    const pct = Math.round(amount / knownMealEnergy * 100);
                    return (
                      <span key={type} className="inline-flex items-center gap-1.5 font-medium">
                        <span className={`inline-block h-2.5 w-2.5 rounded-full ${mealColorClasses[type as keyof typeof mealNames]}`} />
                        <span>{label} · {displayValue(amount || null, 'kcal')} ({pct}%)</span>
                      </span>
                    );
                  })}
                </div>
              </>
            ) : (
              <div className="rounded-xl border border-dashed border-brand-100 bg-raised/40 py-8 px-4 text-center">
                <div className="mx-auto mb-2.5 flex h-10 w-10 items-center justify-center rounded-xl bg-paper text-ink-muted shadow-2xs">
                  <Utensils className="h-5 w-5" />
                </div>
                <p className="text-sm font-bold text-ink">Chưa đủ dữ liệu năng lượng để chia theo bữa.</p>
                <p className="mt-1 text-xs text-ink-muted max-w-md mx-auto">
                  Khi bạn thêm món ăn vào lịch, tỷ lệ phân bổ calo giữa Bữa sáng, Bữa trưa và Bữa tối sẽ hiển thị tại đây.
                </p>
              </div>
            )}
          </Card>

          {/* 8. Daily Meal Plan Section */}
          <div className="mb-4 flex items-center justify-between">
            <h3 className="flex items-center gap-2 text-base sm:text-lg font-extrabold text-ink">
              <Utensils className="h-5 w-5 text-brand-600" />
              Thực đơn ngày {formatDate(selectedDate, { day: 'numeric', month: 'numeric' })}
            </h3>
            {meals.length > 0 && (
              <Badge tone="neutral" className="text-xs font-semibold">
                {meals.length} món đã lên lịch
              </Badge>
            )}
          </div>

          {meals.length ? (
            <div className="mb-6 grid gap-4 md:grid-cols-3">
              {meals.map((meal) => (
                <Card key={meal.entryId} hover className="overflow-hidden border-brand-100/90 shadow-2xs">
                  <div className="relative flex h-36 items-center justify-center bg-raised text-ink-muted">
                    <Utensils className="h-8 w-8" />
                    {recipeCovers[meal.entryId] && <img src={recipeCovers[meal.entryId]!} alt={meal.recipeTitle} loading="lazy" className="absolute inset-0 h-full w-full object-cover" onError={(event) => event.currentTarget.remove()} />}
                  </div>
                  <div className="p-4">
                    <div className="mb-2">
                      <Badge tone={mealBadgeTones[meal.mealType]} soft={false}>{mealNames[meal.mealType]}</Badge>
                    </div>
                    <p className="line-clamp-2 font-bold text-ink">{meal.recipeTitle}</p>
                    <p className="mt-2 text-sm text-ink-muted">{numberFormat.format(meal.plannedServings)} phần dự kiến</p>
                    <div className="mt-3 flex items-center justify-between gap-2 border-t border-brand-50 pt-3">
                      <span className="text-sm font-extrabold text-brand-600">{displayValue(meal.nutrients.ENERGY_KCAL, 'kcal')}</span>
                      <Link to={`/cong-thuc/id/${meal.recipeId}`} className="text-sm font-semibold text-brand-600 hover:underline">
                        Chi tiết công thức →
                      </Link>
                    </div>
                    {!meal.complete && (
                      <p className="mt-2 text-xs text-amber-800">Một số chỉ tiêu còn thiếu, số liệu hiện có vẫn được dùng.</p>
                    )}
                  </div>
                </Card>
              ))}
            </div>
          ) : (
            <div className="mb-6 flex flex-col items-center justify-center rounded-2xl border border-dashed border-brand-200/80 bg-paper/60 p-8 text-center shadow-2xs">
              <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-brand-50 text-brand-600">
                <Utensils className="h-6 w-6" />
              </div>
              <p className="font-bold text-ink">Ngày này chưa có món lên lịch.</p>
              <p className="mt-1 max-w-sm text-xs leading-relaxed text-ink-muted">
                Bạn có thể lên kế hoạch cho ngày này để hệ thống tính toán dinh dưỡng chính xác nhất.
              </p>
              <Link
                to={`/ke-hoach?weekStartDate=${weekStart}&mealDate=${selectedDate}`}
                className="mt-4 inline-flex items-center gap-1.5 rounded-full bg-brand-600 px-4 py-2 text-xs font-bold text-white shadow-sm shadow-brand-600/25 transition-all hover:bg-brand-700 active:scale-95"
              >
                Lên lịch món ăn ngay →
              </Link>
            </div>
          )}
        </>
      )}

      {/* 9. Overall Analysis Modal */}
      <Modal open={analysisOpen} onClose={() => setAnalysisOpen(false)} title="Phân tích dinh dưỡng tổng thể" size="lg">
        {!data ? (
          <p className="text-sm text-ink-muted">Chưa có dữ liệu để phân tích.</p>
        ) : (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div className="flex rounded-xl bg-brand-100/60 p-1">
                <button
                  type="button"
                  onClick={() => setScope('day')}
                  aria-pressed={scope === 'day'}
                  className={`rounded-lg px-3 py-1.5 text-xs font-bold transition-colors ${scope === 'day' ? 'bg-paper text-ink shadow-sm' : 'text-ink-soft hover:text-ink'}`}
                >
                  Theo ngày
                </button>
                <button
                  type="button"
                  onClick={() => setScope('week')}
                  aria-pressed={scope === 'week'}
                  className={`rounded-lg px-3 py-1.5 text-xs font-bold transition-colors ${scope === 'week' ? 'bg-paper text-ink shadow-sm' : 'text-ink-soft hover:text-ink'}`}
                >
                  7 ngày
                </button>
              </div>
              <p className="text-xs text-ink-muted font-medium">
                {scope === 'day' ? formatDate(selectedDate, { weekday: 'long', day: 'numeric', month: 'long' }) : `Tuần bắt đầu ${formatDate(weekStart, { day: 'numeric', month: 'long' })}`}
              </p>
            </div>
            <div className="overflow-x-auto rounded-xl border border-brand-100 shadow-2xs">
              <table className="w-full text-left text-xs sm:text-sm">
                <thead>
                  <tr className="border-b border-brand-100 bg-brand-50/50 text-[11px] font-bold uppercase text-ink-muted">
                    <th className="p-3">Chỉ tiêu</th>
                    <th className="p-3">Thực đơn</th>
                    <th className="p-3">Mức tham khảo</th>
                    <th className="p-3">Tình trạng</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-brand-50">
                  {targets.map((target) => {
                    const actual = (scope === 'day' ? selectedDay?.nutrients : data.weeklyAverage)?.[target.key];
                    const hasValue = actual != null;
                    const within = hasValue && target.minimum != null && actual >= target.minimum && (target.referenceType !== 'AMDR' || target.maximum == null || actual <= target.maximum);
                    const reference = target.minimum == null ? 'Chưa có mức tham khảo' : target.referenceType === 'AMDR' && target.maximum != null ? `${numberFormat.format(target.minimum)}–${numberFormat.format(target.maximum)} ${target.unit}` : `${numberFormat.format(target.minimum)} ${target.unit}`;
                    return (
                      <tr key={target.key} className="hover:bg-brand-50/30 transition-colors">
                        <td className="p-3 font-semibold text-ink">{target.label}</td>
                        <td className="p-3 font-bold text-ink">{displayValue(actual, target.unit)}</td>
                        <td className="p-3 text-ink-muted">{reference}</td>
                        <td className="p-3">
                          {!hasValue ? (
                            <Badge tone="neutral">Chưa đủ dữ liệu</Badge>
                          ) : within ? (
                            <Badge tone="leaf"><CheckCircle2 className="h-3 w-3" />Trong khoảng tham khảo</Badge>
                          ) : (
                            <Badge tone="brand">Ngoài mức tham khảo</Badge>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
            {data.incompleteEntries.length > 0 && (
              <div className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs text-amber-900">
                <strong>Món còn thiếu dữ liệu dinh dưỡng</strong>
                <ul className="mt-2 list-disc pl-5">
                  {data.incompleteEntries.filter((entry) => scope === 'week' || entry.date === selectedDate).map((entry) => (
                    <li key={entry.entryId}>{entry.recipeTitle}: {entry.ingredientsMissingData.join(', ') || 'chưa đủ dữ liệu công thức'}</li>
                  ))}
                </ul>
              </div>
            )}
            <p className="text-xs text-ink-muted leading-relaxed">{data.referenceBasis}. {data.disclaimer}</p>
          </div>
        )}
      </Modal>
    </PageContainer>
  );
}
