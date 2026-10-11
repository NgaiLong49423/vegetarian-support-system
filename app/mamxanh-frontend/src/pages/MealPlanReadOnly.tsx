import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { Calendar, Clock, LoaderCircle } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Card } from '../components/ui';
import { mealPlanApi, type MealPlanWeek } from '../api/recipes';
import { asApiError } from '../lib/apiClient';

const mealLabels: Record<string, string> = {
  BREAKFAST: 'Bữa sáng',
  LUNCH: 'Bữa trưa',
  DINNER: 'Bữa tối',
};

function currentWeekStart() {
  const monday = new Date();
  const daysSinceMonday = (monday.getDay() + 6) % 7;
  monday.setDate(monday.getDate() - daysSinceMonday);
  return `${monday.getFullYear()}-${String(monday.getMonth() + 1).padStart(2, '0')}-${String(monday.getDate()).padStart(2, '0')}`;
}

function friendlyError(error: unknown) {
  const apiError = asApiError(error);
  if (apiError.status === 401) return 'Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại để xem Lịch ăn.';
  if (apiError.status === 403) return 'Tài khoản này không có quyền xem Lịch ăn.';
  return apiError.message;
}

export function MealPlanReadOnly() {
  const [plan, setPlan] = useState<MealPlanWeek | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const weekStartDate = currentWeekStart();

  useEffect(() => {
    let active = true;
    setLoading(true);
    mealPlanApi.getWeek(weekStartDate)
      .then((value) => { if (active) setPlan(value); })
      .catch((cause: unknown) => { if (active) setError(friendlyError(cause)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [weekStartDate]);

  const weekEnd = new Date(`${weekStartDate}T00:00:00`);
  weekEnd.setDate(weekEnd.getDate() + 6);
  const dateRange = `${new Date(`${weekStartDate}T00:00:00`).toLocaleDateString('vi-VN')} – ${weekEnd.toLocaleDateString('vi-VN')}`;

  return <PageContainer className="py-8">
    <nav className="mb-4 text-sm text-ink-muted"><Link to="/" className="hover:text-brand-600">Trang chủ</Link> / <span className="font-medium text-brand-600">Lịch ăn</span></nav>
    <Card className="mb-6 p-5 sm:p-6">
      <p className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-brand-700"><Calendar className="h-4 w-4" />Lịch ăn của bạn</p>
      <h1 className="mt-2 text-2xl font-extrabold text-ink">Tuần {dateRange}</h1>
    </Card>
    {loading && <div role="status" className="py-10 text-center text-sm text-ink-muted"><LoaderCircle className="mx-auto mb-2 h-6 w-6 animate-spin" />Đang tải Lịch ăn…</div>}
    {error && <p role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">{error}</p>}
    {!loading && !error && plan?.entries.length === 0 && <Card className="p-6 text-center text-sm text-ink-muted">Tuần này chưa có công thức trong Lịch ăn.</Card>}
    {!loading && !error && plan && plan.entries.length > 0 && <div className="space-y-3">
      {Array.from({ length: 7 }, (_, offset) => {
        const date = new Date(`${weekStartDate}T00:00:00`);
        date.setDate(date.getDate() + offset);
        const dateKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
        const entries = plan.entries.filter((entry) => entry.mealDate === dateKey);
        return <Card key={dateKey} className="p-4 sm:p-5">
          <h2 className="mb-3 text-sm font-bold text-ink">{date.toLocaleDateString('vi-VN', { weekday: 'long', day: 'numeric', month: 'long' })}</h2>
          {entries.length === 0 ? <p className="text-sm text-ink-muted">Chưa có món.</p> : <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {entries.map((entry) => <div key={entry.entryId} className={`flex min-h-24 gap-3 rounded-xl border p-3 ${entry.recipeDeleted ? 'border-amber-300 bg-amber-50' : 'border-brand-100 bg-brand-50/40'}`}>
              {entry.recipeCoverUrl && !entry.recipeDeleted && <img src={entry.recipeCoverUrl} alt="" className="h-14 w-14 shrink-0 rounded-lg object-cover" />}
              <div className="min-w-0">
                <p className="text-xs font-bold text-brand-700">{mealLabels[entry.mealType] ?? entry.mealType}</p>
                {entry.recipeDeleted ? <p className="mt-2 text-sm font-semibold leading-5 text-amber-950">Công thức này đã bị xóa bởi tác giả</p>
                  : entry.unavailableMessage ? <p className="mt-2 text-sm text-ink-muted">{entry.unavailableMessage}</p>
                    : <Link to={`/cong-thuc/id/${entry.recipeId}`} className="mt-1 block truncate text-sm font-semibold text-ink hover:text-brand-700">{entry.recipeTitle}</Link>}
                {!entry.recipeDeleted && !entry.unavailableMessage && entry.totalTimeMinutes !== null && <p className="mt-1 flex items-center gap-1 text-xs text-ink-muted"><Clock className="h-3 w-3" />{entry.totalTimeMinutes} phút · {entry.plannedServings} phần</p>}
              </div>
            </div>)}
          </div>}
        </Card>;
      })}
    </div>}
  </PageContainer>;
}
