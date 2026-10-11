import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Calendar,
  ChevronLeft,
  ChevronRight,
  Coffee,
  Flame,
  Info,
  Moon,
  Plus,
  RefreshCw,
  ShoppingCart,
  Sun,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { BinButton } from '../components/BinButton';
import { AiButton, Badge, Button } from '../components/ui';
import { Modal } from '../components/Modal';
import { weekPlan as initialPlan, recipes } from '../data/mockData';
import type { DayPlan, MealSlot } from '../types';
import { useAuth } from '../components/AuthContext';
import { MealPlanReadOnly } from './MealPlanReadOnly';
import { WeekCalendar, localDateKey, mondayOf, shiftWeek } from '../components/WeekCalendar';

const slotMeta: Record<MealSlot, { icon: typeof Sun; label: string; color: string }> = {
  Sáng: { icon: Coffee, label: 'Sáng', color: 'text-amber-500' },
  Trưa: { icon: Sun, label: 'Trưa', color: 'text-brand-500' },
  Tối: { icon: Moon, label: 'Tối', color: 'text-brand-700' },
};
const allSlots: MealSlot[] = ['Sáng', 'Trưa', 'Tối'];

export function MealPlanner() {
  const { account } = useAuth();
  const [week, setWeek] = useState<DayPlan[]>(() => initialPlan.map((d) => ({ ...d, meals: [...d.meals] })));
  const [weekStart, setWeekStart] = useState(() => mondayOf(new Date()));
  const [picker, setPicker] = useState<{ dayIdx: number; slot: MealSlot } | null>(null);
  const [aiModalOpen, setAiModalOpen] = useState(false);
  const [calorieGoal, setCalorieGoal] = useState(1800);
  const [dietStyle, setDietStyle] = useState('VEGAN');
  const [aiSuccessToast, setAiSuccessToast] = useState(false);

  const applyAiPlan = () => {
    setWeek((prev) =>
      prev.map((day, i) => {
        const offset = i * 2;
        const bRecipe = recipes[offset % recipes.length];
        const lRecipe = recipes[(offset + 1) % recipes.length];
        const dRecipe = recipes[(offset + 2) % recipes.length];
        return {
          ...day,
          meals: [
            { slot: 'Sáng', recipe: bRecipe, servings: 1 },
            { slot: 'Trưa', recipe: lRecipe, servings: 1 },
            { slot: 'Tối', recipe: dRecipe, servings: 1 },
          ],
        };
      }),
    );
    setAiModalOpen(false);
    setAiSuccessToast(true);
    setTimeout(() => setAiSuccessToast(false), 4000);
  };

  if (account && account.role !== 'ADMIN') return <MealPlanReadOnly />;
  if (account?.role === 'ADMIN') return <PageContainer className="py-12"><div role="alert" className="mx-auto max-w-xl rounded-xl border border-amber-200 bg-amber-50 p-5 text-sm text-amber-950">Tài khoản Quản trị viên không có quyền xem Lịch ăn của thành viên.</div></PageContainer>;

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
    <PageContainer className="py-6">
      <div className="grid items-start gap-5 md:grid-cols-[225px_minmax(0,1fr)]">
        <aside className="space-y-4"><WeekCalendar weekStart={weekStart} onChange={setWeekStart} /><div className="rounded-2xl border border-brand-100 bg-paper p-4"><p className="text-sm font-semibold text-ink">Kế hoạch mẫu</p><p className="mt-2 text-xs leading-5 text-ink-muted">Thử chọn món, thay đổi khẩu phần và chuẩn bị bữa chay trên giao diện demo.</p><Link to="/dang-nhap" className="mt-3 inline-block text-xs font-semibold text-brand-600">Đăng nhập để xem lịch riêng →</Link></div></aside>
        <div className="min-w-0">
          <header className="mb-4 rounded-2xl border border-brand-100 bg-paper p-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h1 className="flex items-center gap-2 text-xl font-semibold text-ink"><Calendar className="h-5 w-5 text-brand-600" />Kế hoạch bữa ăn</h1>
              <AiButton size="sm" onClick={() => setAiModalOpen(true)}>
                AI gợi ý thực đơn tuần
              </AiButton>
            </div>
            <div className="mt-4 flex items-center justify-between gap-3">
              <button type="button" aria-label="Tuần trước" onClick={() => setWeekStart((value) => shiftWeek(value, -1))} className="rounded-full border border-brand-200 p-1.5 text-ink-muted"><ChevronLeft className="h-4 w-4" /></button>
              <span className="text-xs font-medium text-ink-soft">Tuần bắt đầu {new Date(`${weekStart}T12:00:00`).toLocaleDateString('vi-VN')}</span>
              <button type="button" aria-label="Tuần sau" onClick={() => setWeekStart((value) => shiftWeek(value, 1))} className="rounded-full border border-brand-200 p-1.5 text-ink-muted"><ChevronRight className="h-4 w-4" /></button>
            </div>
          </header>
          {aiSuccessToast && (
            <div role="status" className="mb-4 rounded-xl border border-leaf-200 bg-leaf-50 p-3.5 text-sm font-semibold text-leaf-800">
              ✨ Đã tự động xếp thực đơn 7 ngày phù hợp với mục tiêu {calorieGoal.toLocaleString('vi-VN')} kcal/ngày của bạn!
            </div>
          )}
      <div className="space-y-3">
        {week.map((day, dayIdx) => { const calendarDate = new Date(`${weekStart}T12:00:00`); calendarDate.setDate(calendarDate.getDate() + dayIdx); const today = localDateKey(calendarDate) === localDateKey(new Date()); return (
          <div
            key={calendarDate.toLocaleDateString('vi-VN', { day: 'numeric', month: 'numeric' })}
            className={`rounded-2xl border bg-paper p-4 transition-colors ${today ? 'border-brand-500 ring-1 ring-brand-500' : 'border-brand-100'}`}
          >
            <div className="grid gap-3 lg:grid-cols-[140px_1fr] lg:items-center">
              <div className="flex items-center justify-between lg:flex-col lg:items-start">
                <div>
                  <p className="text-xs font-bold uppercase tracking-wider text-ink-muted">{calendarDate.toLocaleDateString('vi-VN', { weekday: 'long' })}</p>
                  <p className="text-2xl font-extrabold text-ink">{calendarDate.toLocaleDateString('vi-VN', { day: 'numeric', month: 'numeric' })}</p>
                </div>
                {today && <Badge tone="brand" soft={false} className="lg:mt-2">● Hôm nay</Badge>}
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
                        <button onClick={() => setPicker({ dayIdx, slot })} title="Đổi món" className="flex h-6 w-6 items-center justify-center rounded-md text-ink-muted hover:bg-paper hover:text-brand-600">
                          <RefreshCw className="h-3.5 w-3.5" />
                        </button>
                        <BinButton
                          size={24}
                          onClick={() => removeMeal(dayIdx, slot)}
                          title="Xoá món"
                          aria-label="Xoá món"
                        />
                        </div>
                      </div>
                      <label className="mt-2 flex items-center justify-between border-t border-brand-100 pt-2 text-xs text-ink-muted">
                        Khẩu phần dự định ăn
                        <select
                          aria-label={`Khẩu phần ${meal.recipe.name} ${calendarDate.toLocaleDateString('vi-VN', { weekday: 'long' })} bữa ${slot}`}
                          value={meal.servings ?? 1}
                          onChange={(event) => setServings(dayIdx, slot, Number(event.target.value))}
                          className="rounded-lg border border-brand-200 bg-paper px-2 py-1 font-semibold text-ink"
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
        );})}
      </div>

      <div className="mt-6 flex flex-col items-center justify-between gap-4 rounded-2xl border border-brand-100 bg-brand-50/50 p-5 sm:flex-row">
        <p className="text-sm text-ink-soft">Tiếp tục thử màn danh sách đi chợ mẫu để chuẩn bị nguyên liệu cho bữa ăn.</p>
        <Button as="span">
          <Link to="/di-cho" className="flex items-center gap-2">
            <ShoppingCart className="h-4 w-4" /> Tạo danh sách đi chợ
          </Link>
        </Button>
      </div>

        </div>
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

      <Modal open={aiModalOpen} onClose={() => setAiModalOpen(false)} title="Trợ lý gợi ý thực đơn tuần 7 ngày" size="lg">
        <div className="space-y-4">
          <div className="rounded-xl border border-brand-100 bg-brand-50/60 p-3.5 text-xs text-ink-soft leading-relaxed flex items-start gap-2.5">
            <Info className="h-4 w-4 shrink-0 text-brand-600 mt-0.5" />
            <p>
              Gợi ý từ trợ lý mang tính tham khảo dinh dưỡng dựa trên mục tiêu bạn chọn. Nhu cầu thực tế có thể thay đổi tùy thể trạng và lịch sinh hoạt, bạn luôn có thể linh hoạt đổi món hoặc điều chỉnh khẩu phần cho từng bữa.
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label htmlFor="ai-meal-calorie-goal" className="mb-1 block text-xs font-bold text-ink flex items-center gap-1.5">
                <Flame className="h-3.5 w-3.5 text-amber-500" /> Mục tiêu năng lượng mỗi ngày
              </label>
              <select
                id="ai-meal-calorie-goal"
                value={calorieGoal}
                onChange={(e) => setCalorieGoal(Number(e.target.value))}
                className="w-full rounded-xl border border-brand-200 bg-white p-2.5 text-sm font-semibold text-ink outline-none focus:border-brand-500"
              >
                <option value={1600}>1.600 kcal / ngày (Thanh lọc, nhẹ nhàng)</option>
                <option value={1800}>1.800 kcal / ngày (Cân bằng, duy trì sức khỏe)</option>
                <option value={2000}>2.000 kcal / ngày (Năng động, thể thao)</option>
              </select>
            </div>

            <div>
              <label htmlFor="ai-meal-diet-style" className="mb-1 block text-xs font-bold text-ink">
                Trường phái ăn chay mong muốn
              </label>
              <select
                id="ai-meal-diet-style"
                value={dietStyle}
                onChange={(e) => setDietStyle(e.target.value)}
                className="w-full rounded-xl border border-brand-200 bg-white p-2.5 text-sm font-semibold text-ink outline-none focus:border-brand-500"
              >
                <option value="VEGAN">Thuần chay (100% nguồn gốc thực vật)</option>
                <option value="LACTO_OVO">Chay có trứng và sữa (Lacto-Ovo)</option>
                <option value="FLEXIBLE">Chay linh hoạt (Ưu tiên đạm thực vật)</option>
              </select>
            </div>
          </div>

          <div className="rounded-xl border border-brand-100 bg-white p-4 space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-brand-700">
              Nguyên tắc phân bổ 3 bữa trong ngày
            </h4>
            <div className="grid gap-2.5 sm:grid-cols-3 text-xs">
              <div className="rounded-lg border border-amber-100 bg-amber-50/50 p-3">
                <p className="font-bold text-amber-800 flex items-center gap-1">
                  <Coffee className="h-3.5 w-3.5" /> Bữa sáng (~25%)
                </p>
                <p className="mt-1 font-semibold text-ink">
                  {Math.round(calorieGoal * 0.25)} kcal
                </p>
                <p className="mt-1 text-[11px] text-ink-muted">
                  Món nhẹ nhàng, ấm bụng, thanh lọc cơ thể để khởi đầu ngày mới.
                </p>
              </div>

              <div className="rounded-lg border border-brand-100 bg-brand-50/50 p-3">
                <p className="font-bold text-brand-800 flex items-center gap-1">
                  <Sun className="h-3.5 w-3.5" /> Bữa trưa (~45%)
                </p>
                <p className="mt-1 font-semibold text-ink">
                  {Math.round(calorieGoal * 0.45)} kcal
                </p>
                <p className="mt-1 text-[11px] text-ink-muted">
                  Bữa chính giàu năng lượng, kết hợp đậu hũ, các loại hạt và rau củ cung cấp đạm thực vật bền bỉ.
                </p>
              </div>

              <div className="rounded-lg border border-indigo-100 bg-indigo-50/50 p-3">
                <p className="font-bold text-indigo-800 flex items-center gap-1">
                  <Moon className="h-3.5 w-3.5" /> Bữa tối (~30%)
                </p>
                <p className="mt-1 font-semibold text-ink">
                  {Math.round(calorieGoal * 0.30)} kcal
                </p>
                <p className="mt-1 text-[11px] text-ink-muted">
                  Thanh đạm, giàu chất xơ và vitamin giúp nhẹ bụng và hỗ trợ giấc ngủ ngon.
                </p>
              </div>
            </div>
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button variant="outline" onClick={() => setAiModalOpen(false)}>Hủy</Button>
            <AiButton size="sm" onClick={applyAiPlan}>
              Áp dụng thực đơn gợi ý
            </AiButton>
          </div>
        </div>
      </Modal>

      {aiSuccessToast && (
        <div className="fixed bottom-6 right-6 z-50 flex items-center gap-2 rounded-2xl bg-brand-600 px-4 py-3 text-sm font-semibold text-white shadow-xl shadow-brand-900/20">
          <span>✨ Đã áp dụng thực đơn 7 ngày từ trợ lý AI!</span>
        </div>
      )}
    </PageContainer>
  );
}

