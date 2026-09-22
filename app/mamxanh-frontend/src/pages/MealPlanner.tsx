import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Calendar,
  ChevronLeft,
  ChevronRight,
  Coffee,
  Moon,
  Plus,
  RefreshCw,
  Settings2,
  ShoppingCart,
  Sun,
  Trash2,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Badge, Button } from '../components/ui';
import { Modal } from '../components/Modal';
import { weekPlan as initialPlan, recipes } from '../data/mockData';
import type { DayPlan, MealSlot } from '../types';

const slotMeta: Record<MealSlot, { icon: typeof Sun; label: string; color: string }> = {
  Sáng: { icon: Coffee, label: 'Sáng', color: 'text-amber-500' },
  Trưa: { icon: Sun, label: 'Trưa', color: 'text-brand-500' },
  Tối: { icon: Moon, label: 'Tối', color: 'text-brand-700' },
};
const allSlots: MealSlot[] = ['Sáng', 'Trưa', 'Tối'];

export function MealPlanner() {
  const [week, setWeek] = useState<DayPlan[]>(() => initialPlan.map((d) => ({ ...d, meals: [...d.meals] })));
  const [weekNum, setWeekNum] = useState(42);
  const [picker, setPicker] = useState<{ dayIdx: number; slot: MealSlot } | null>(null);

  const setMeal = (dayIdx: number, slot: MealSlot, recipeId: string) => {
    setWeek((prev) =>
      prev.map((day, i) => {
        if (i !== dayIdx) return day;
        const recipe = recipes.find((r) => r.id === recipeId)!;
        const others = day.meals.filter((m) => m.slot !== slot);
        return { ...day, meals: [...others, { slot, recipe, servings: 1 }].sort((a, b) => allSlots.indexOf(a.slot) - allSlots.indexOf(b.slot)) };
      }),
    );
  };

  const removeMeal = (dayIdx: number, slot: MealSlot) => {
    setWeek((prev) => prev.map((day, i) => (i === dayIdx ? { ...day, meals: day.meals.filter((m) => m.slot !== slot) } : day)));
  };

  const setServings = (dayIdx: number, slot: MealSlot, servings: number) => {
    setWeek((prev) => prev.map((day, i) => i === dayIdx
      ? { ...day, meals: day.meals.map((meal) => meal.slot === slot ? { ...meal, servings } : meal) }
      : day));
  };

  return (
    <PageContainer className="py-8">
      <nav className="mb-4 text-sm text-ink-muted">
        <Link to="/" className="hover:text-brand-600">Trang chủ</Link> / <span className="font-medium text-brand-600">Kế hoạch</span>
      </nav>

      <div className="mb-6 rounded-2xl border border-brand-100 bg-white p-6">
        <div className="flex flex-col items-start justify-between gap-4 lg:flex-row lg:items-center">
          <div>
            <h1 className="text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">Kế hoạch thực đơn tuần 7 ngày</h1>
            <p className="mt-1 max-w-xl text-sm text-ink-muted">
              Danh sách món ăn thuần thực vật theo ngày, cân bằng dinh dưỡng, dễ dàng theo dõi và chuẩn bị bữa cơm gia đình.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1 rounded-xl border border-brand-200 bg-white px-1.5 py-1.5">
              <button onClick={() => setWeekNum((n) => n - 1)} className="flex h-8 w-8 items-center justify-center rounded-lg text-ink-soft hover:bg-brand-100">
                <ChevronLeft className="h-4 w-4" />
              </button>
              <span className="flex items-center gap-2 px-2 text-sm font-semibold text-ink">
                <Calendar className="h-4 w-4 text-brand-600" /> Tuần {weekNum}: 14/10 – 20/10/2024
              </span>
              <button onClick={() => setWeekNum((n) => n + 1)} className="flex h-8 w-8 items-center justify-center rounded-lg text-ink-soft hover:bg-brand-100">
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
            <Button variant="outline"><Settings2 className="h-4 w-4" /> Tùy chỉnh</Button>
          </div>
        </div>
      </div>

      <div className="space-y-3">
        {week.map((day, dayIdx) => (
          <div
            key={day.date}
            className={`rounded-2xl border bg-white p-4 transition-colors ${day.today ? 'border-brand-500 ring-1 ring-brand-500' : 'border-brand-100'}`}
          >
            <div className="grid gap-3 lg:grid-cols-[140px_1fr] lg:items-center">
              <div className="flex items-center justify-between lg:flex-col lg:items-start">
                <div>
                  <p className="text-xs font-bold uppercase tracking-wider text-ink-muted">{day.weekday}</p>
                  <p className="text-2xl font-extrabold text-ink">{day.date}</p>
                </div>
                {day.today && <Badge tone="brand" soft={false} className="lg:mt-2">● Hôm nay</Badge>}
              </div>

              <div className="grid gap-3 sm:grid-cols-3">
                {allSlots.map((slot) => {
                  const meal = day.meals.find((m) => m.slot === slot);
                  const Meta = slotMeta[slot];
                  if (!meal) {
                    return (
                      <button
                        key={slot}
                        onClick={() => setPicker({ dayIdx, slot })}
                        className="flex min-h-[68px] items-center justify-center gap-2 rounded-xl border border-dashed border-brand-200 text-sm font-semibold text-brand-600 transition-colors hover:border-brand-400 hover:bg-brand-50"
                      >
                        <Plus className="h-4 w-4" /> Thêm món {slot}
                      </button>
                    );
                  }
                  return (
                    <div key={slot} className="group rounded-xl border border-brand-100 bg-brand-50/40 p-2">
                      <div className="flex items-center gap-2">
                        <img src={meal.recipe.image} alt="" className="h-12 w-12 shrink-0 rounded-lg object-cover" />
                        <div className="min-w-0 flex-1">
                        <p className={`flex items-center gap-1 text-xs font-bold ${Meta.color}`}>
                          <Meta.icon className="h-3 w-3" /> {Meta.label}
                        </p>
                        <Link to={`/cong-thuc/${meal.recipe.slug}`} className="block truncate text-sm font-semibold text-ink hover:text-brand-700">
                          {meal.recipe.name}
                        </Link>
                        </div>
                        <div className="flex shrink-0 flex-col gap-0.5">
                        <button onClick={() => setPicker({ dayIdx, slot })} title="Đổi món" className="flex h-6 w-6 items-center justify-center rounded-md text-ink-muted hover:bg-white hover:text-brand-600">
                          <RefreshCw className="h-3.5 w-3.5" />
                        </button>
                        <button onClick={() => removeMeal(dayIdx, slot)} title="Xoá món" className="flex h-6 w-6 items-center justify-center rounded-md text-ink-muted hover:bg-white hover:text-red-500">
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                        </div>
                      </div>
                      <label className="mt-2 flex items-center justify-between border-t border-brand-100 pt-2 text-xs text-ink-muted">
                        Khẩu phần dự định ăn
                        <select
                          aria-label={`Khẩu phần ${meal.recipe.name} ${day.weekday} bữa ${slot}`}
                          value={meal.servings ?? 1}
                          onChange={(event) => setServings(dayIdx, slot, Number(event.target.value))}
                          className="rounded-lg border border-brand-200 bg-white px-2 py-1 font-semibold text-ink"
                        >
                          {Array.from({ length: 20 }, (_, i) => (i + 1) / 2).map((value) => (
                            <option key={value} value={value}>{value} phần</option>
                          ))}
                        </select>
                      </label>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="mt-6 flex flex-col items-center justify-between gap-4 rounded-2xl border border-brand-100 bg-brand-50/50 p-5 sm:flex-row">
        <p className="text-sm text-ink-soft">Đã lên kế hoạch xong? Tạo danh sách đi chợ tổng hợp cho cả tuần chỉ với một chạm.</p>
        <Button as="span">
          <Link to="/di-cho" className="flex items-center gap-2">
            <ShoppingCart className="h-4 w-4" /> Tạo danh sách đi chợ
          </Link>
        </Button>
      </div>

      <Modal open={!!picker} onClose={() => setPicker(null)} title={`Chọn món ${picker?.slot ?? ''}`} size="lg">
        <div className="grid max-h-[60vh] gap-2 overflow-y-auto sm:grid-cols-2">
          {recipes.map((r) => (
            <button
              key={r.id}
              onClick={() => {
                if (picker) setMeal(picker.dayIdx, picker.slot, r.id);
                setPicker(null);
              }}
              className="flex items-center gap-3 rounded-xl border border-brand-100 p-2 text-left transition-colors hover:border-brand-300 hover:bg-brand-50"
            >
              <img src={r.image} alt="" className="h-12 w-12 shrink-0 rounded-lg object-cover" />
              <div className="min-w-0">
                <p className="truncate text-sm font-semibold text-ink">{r.name}</p>
                <p className="text-xs text-ink-muted">{r.calories} kcal · {r.prepTime + r.cookTime} phút</p>
              </div>
            </button>
          ))}
        </div>
      </Modal>
    </PageContainer>
  );
}
