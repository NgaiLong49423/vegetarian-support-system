import { useState } from 'react';
import { Link } from 'react-router-dom';
import { BadgeCheck, ChevronLeft, ChevronRight, Download, FileText } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Badge, Button, Card, ProgressBar } from '../components/ui';
import { nutritionTargets, weekPlan } from '../data/mockData';

const days = weekPlan.map((d) => ({ weekday: d.weekday, date: d.date, today: d.today }));

const macros = [
  { label: 'Tổng Năng lượng (Energy)', value: '1,290', unit: 'kcal', target: '1,850 kcal', pct: 70, tone: 'brand' as const, sub: 'Còn thiếu 560 kcal — khuyến nghị bổ sung bữa phụ' },
  { label: 'Đạm thực vật (Protein)', value: '56.4', unit: 'g/60 g', target: '', pct: 94, tone: 'leaf' as const, sub: 'Tuyệt vời, đủ đạm hoàn hảo' },
  { label: 'Carbohydrate phức hợp', value: '182', unit: 'g/220 g', target: '', pct: 83, tone: 'brand' as const, sub: 'Duy trì đường huyết ổn định' },
  { label: 'Chất béo tốt (Lipid)', value: '34', unit: 'g/45 g', target: '', pct: 76, tone: 'leaf' as const, sub: 'Bảo vệ sức khoẻ tim mạch' },
];

const meals = [
  { slot: 'Bữa sáng', time: '07:00 – 08:30', name: 'Bánh mì ngũ cốc & Sinh tố bơ chuối', kcal: 320, recipe: weekPlan[4].meals[0].recipe },
  { slot: 'Bữa trưa', time: '11:30 – 13:00', name: 'Miến xào nấm đậu hũ & Canh rong biển hạt sen', kcal: 470, recipe: weekPlan[4].meals[1].recipe },
  { slot: 'Bữa tối', time: '18:30 – 20:00', name: 'Cà ri đậu gà cốt dừa & Salad bơ chanh dây', kcal: 500, recipe: weekPlan[1].meals[2].recipe },
];

const statusStyle: Record<string, { label: string; cls: string }> = {
  ok: { label: 'Trong khoảng tham khảo', cls: 'bg-leaf-100 text-leaf-700' },
  low: { label: 'Thấp hơn mức tham khảo', cls: 'bg-amber-100 text-amber-700' },
  missing: { label: 'Chưa dữ liệu', cls: 'bg-brand-100 text-brand-700' },
};

export function NutritionTracker() {
  const [activeDay, setActiveDay] = useState(4);

  return (
    <PageContainer className="py-8">
      <nav className="mb-4 text-sm text-ink-muted">
        <Link to="/" className="hover:text-brand-600">Trang chủ</Link> / <span className="font-medium text-brand-600">Theo dõi dinh dưỡng</span>
      </nav>

      <div className="mb-6 flex flex-col items-start justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <p className="mb-1 text-xs font-bold uppercase tracking-[0.15em] text-brand-600">Dinh dưỡng khoa học</p>
          <h1 className="text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">Theo dõi & Kiểm tra Dinh dưỡng Ngày</h1>
          <p className="mt-1 max-w-2xl text-sm text-ink-muted">
            Phân tích khoa học 9 chỉ tiêu vi chất từ thực đơn thực vật của bạn, đối chiếu chuẩn Viện Dinh Dưỡng Quốc Gia (DRI Việt Nam) và USDA FoodData Central.
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline"><Download className="h-4 w-4" /> Xuất báo cáo PDF</Button>
          <Button><FileText className="h-4 w-4" /> Phân tích tổng thể</Button>
        </div>
      </div>

      {/* day selector */}
      <div className="mb-6 flex items-center gap-2">
        <button className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-brand-200 bg-white text-ink-soft hover:bg-brand-50"><ChevronLeft className="h-4 w-4" /></button>
        <div className="flex flex-1 gap-2 overflow-x-auto scrollbar-thin">
          {days.map((d, i) => (
            <button
              key={d.date}
              onClick={() => setActiveDay(i)}
              className={`flex min-w-[104px] flex-col items-center rounded-xl border px-3 py-2.5 transition-colors ${
                activeDay === i ? 'border-brand-600 bg-brand-600 text-white' : 'border-brand-100 bg-white text-ink-soft hover:border-brand-300'
              }`}
            >
              <span className="whitespace-nowrap text-xs font-semibold">{d.weekday}{d.today ? ' • Hôm nay' : ''}</span>
              <span className="text-lg font-extrabold">{d.date}</span>
              <span className={`text-[10px] ${activeDay === i ? 'text-white/80' : 'text-ink-muted'}`}>1,290 kcal</span>
            </button>
          ))}
        </div>
        <button className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-brand-200 bg-white text-ink-soft hover:bg-brand-50"><ChevronRight className="h-4 w-4" /></button>
      </div>

      {/* macro cards */}
      <div className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {macros.map((m) => (
          <Card key={m.label} className="p-5">
            <div className="mb-2 flex items-start justify-between gap-2">
              <p className="text-sm font-semibold text-ink-soft">{m.label}</p>
              <Badge tone={m.tone === 'leaf' ? 'leaf' : 'brand'}>{m.pct}%</Badge>
            </div>
            <p className="text-2xl font-extrabold text-ink">
              {m.value} <span className="text-sm font-semibold text-ink-muted">{m.unit}</span>
            </p>
            <div className="my-3"><ProgressBar pct={m.pct} tone={m.tone} /></div>
            <p className="text-xs text-ink-muted">{m.sub}</p>
          </Card>
        ))}
      </div>

      {/* calorie distribution */}
      <Card className="mb-6 p-5">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="font-bold text-ink">Phân bố Calorie theo 3 bữa ăn</h3>
          <span className="text-sm text-ink-muted">Tổng hợp: <strong className="text-ink">1,290 kcal</strong></span>
        </div>
        <div className="flex h-4 overflow-hidden rounded-full">
          <div className="bg-amber-400" style={{ width: '25%' }} />
          <div className="bg-brand-500" style={{ width: '35%' }} />
          <div className="bg-brand-700" style={{ width: '40%' }} />
        </div>
        <div className="mt-3 flex flex-wrap gap-x-6 gap-y-1 text-xs text-ink-muted">
          <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-full bg-amber-400" /> Bữa sáng · 320 kcal (25%)</span>
          <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-full bg-brand-500" /> Bữa trưa · 470 kcal (35%)</span>
          <span className="inline-flex items-center gap-1.5"><span className="h-2.5 w-2.5 rounded-full bg-brand-700" /> Bữa tối · 500 kcal (40%)</span>
        </div>
      </Card>

      {/* meals of the day */}
      <h3 className="mb-3 flex items-center gap-2 text-lg font-extrabold text-ink">🍽️ Thực đơn 3 Bữa Ngày {days[activeDay].date}</h3>
      <div className="mb-6 grid gap-4 md:grid-cols-3">
        {meals.map((meal) => (
          <Card key={meal.slot} hover className="overflow-hidden">
            <div className="relative">
              <img src={meal.recipe.image} alt="" className="h-36 w-full object-cover" />
              <div className="absolute left-3 top-3"><Badge tone="brand" soft={false}>{meal.slot}</Badge></div>
              <div className="absolute right-3 top-3"><Badge tone="leaf"><BadgeCheck className="h-3 w-3" /> Chuẩn khoa học</Badge></div>
            </div>
            <div className="p-4">
              <p className="text-xs text-ink-muted">{meal.time}</p>
              <p className="mt-1 line-clamp-2 font-bold text-ink">{meal.name}</p>
              <div className="mt-3 flex items-center justify-between border-t border-brand-50 pt-3">
                <span className="text-sm font-extrabold text-brand-600">{meal.kcal} kcal</span>
                <Link to={`/cong-thuc/${meal.recipe.slug}`} className="text-sm font-semibold text-brand-600 hover:underline">Chi tiết công thức →</Link>
              </div>
            </div>
          </Card>
        ))}
      </div>

      {/* nutrition table */}
      <Card className="overflow-hidden">
        <div className="border-b border-brand-50 p-5">
          <h3 className="font-extrabold text-ink">Bảng Đánh Giá 9 Chỉ Tiêu Dinh Dưỡng Cốt Lõi</h3>
          <p className="text-sm text-ink-muted">Đối chiếu chuẩn Viện Dinh Dưỡng Quốc Gia (DRI Việt Nam) & USDA FoodData Central cho người trưởng thành.</p>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-[640px] text-sm">
            <thead>
              <tr className="border-b border-brand-50 text-left text-xs uppercase tracking-wider text-ink-muted">
                <th className="p-4 font-bold">Chỉ tiêu dinh dưỡng</th>
                <th className="p-4 font-bold">Thực tế nạp</th>
                <th className="p-4 font-bold">Chuẩn DRI</th>
                <th className="p-4 font-bold">% Đạt</th>
                <th className="p-4 font-bold">Trạng thái đánh giá</th>
              </tr>
            </thead>
            <tbody>
              {nutritionTargets.map((n, i) => {
                const st = statusStyle[n.status] ?? statusStyle.ok;
                return (
                  <tr key={n.key} className="border-b border-brand-50 last:border-0 hover:bg-brand-50/40">
                    <td className="p-4 font-semibold text-ink">{i + 1}. {n.key}</td>
                    <td className="p-4 font-bold text-ink">{n.actual}</td>
                    <td className="p-4 text-ink-soft">{n.target}</td>
                    <td className="p-4">
                      <div className="flex items-center gap-2">
                        <div className="h-1.5 w-16 overflow-hidden rounded-full bg-brand-100">
                          <div className={`h-full rounded-full ${n.status === 'ok' ? 'bg-leaf-500' : 'bg-amber-400'}`} style={{ width: `${n.pct}%` }} />
                        </div>
                        <span className="font-bold text-ink">{n.pct}%</span>
                      </div>
                    </td>
                    <td className="p-4">
                      <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${st.cls}`}>{st.label}</span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
        <div className="border-t border-brand-50 bg-brand-50/40 p-4 text-xs leading-relaxed text-ink-muted">
          <strong className="text-brand-700">Lưu ý B12:</strong> Không phụ thuộc quá 2.0. Khuyến nghị bổ sung men dinh dưỡng hoặc viên uống thuần chay để đảm bảo vi chất ổn định lâu dài.
        </div>
      </Card>
    </PageContainer>
  );
}
