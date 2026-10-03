import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  AlertCircle,
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Download,
  FileText,
  Info,
  RefreshCw,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Badge, Button, Card, ProgressBar } from '../components/ui';
import { Modal } from '../components/Modal';
import { nutrientsDay, nutrientsWeek, weekPlan } from '../data/mockData';
import type { NutrientComparisonItem } from '../types';

const days = weekPlan.map((d) => ({ weekday: d.weekday, date: d.date, today: d.today }));

const macros = [
  { label: 'Tổng Năng lượng (Energy)', value: '1,290', unit: 'kcal', target: '1,850 kcal', pct: 70, tone: 'brand' as const, sub: 'Còn thiếu 560 kcal so với mức dự kiến' },
  { label: 'Đạm thực vật (Protein)', value: '56.4', unit: 'g/60 g', target: '', pct: 94, tone: 'leaf' as const, sub: 'Đạt mức dự kiến cho thực đơn ngày' },
  { label: 'Carbohydrate phức hợp', value: '182', unit: 'g/220 g', target: '', pct: 83, tone: 'brand' as const, sub: 'Mức tiêu thụ carbohydrate cân đối' },
  { label: 'Chất béo tốt (Lipid)', value: '34', unit: 'g/45 g', target: '', pct: 76, tone: 'leaf' as const, sub: 'Chất béo không bão hòa từ hạt và dầu thực vật' },
];

const meals = [
  { slot: 'Bữa sáng', time: '07:00 – 08:30', name: 'Bánh mì ngũ cốc & Sinh tố bơ chuối', kcal: 320, recipe: weekPlan[4].meals[0].recipe },
  { slot: 'Bữa trưa', time: '11:30 – 13:00', name: 'Miến xào nấm đậu hũ & Canh rong biển hạt sen', kcal: 470, recipe: weekPlan[4].meals[1].recipe },
  { slot: 'Bữa tối', time: '18:30 – 20:00', name: 'Cà ri đậu gà cốt dừa & Salad bơ chanh dây', kcal: 500, recipe: weekPlan[1].meals[2].recipe },
];

export function NutritionTracker() {
  const [activeDay, setActiveDay] = useState(4);
  const [pdfAlert, setPdfAlert] = useState(false);

  // Analysis modal & state
  const [analysisOpen, setAnalysisOpen] = useState(false);
  const [period, setPeriod] = useState<'day' | 'week'>('day');
  const [analysisState, setAnalysisState] = useState<'success' | 'empty' | 'error' | 'incomplete'>('success');
  const [loading, setLoading] = useState(false);

  const triggerAnalyze = () => {
    setLoading(true);
    setAnalysisOpen(true);
    setTimeout(() => {
      setLoading(false);
    }, 350);
  };

  const currentNutrients: NutrientComparisonItem[] = period === 'day' ? nutrientsDay : nutrientsWeek;

  const renderStatusBadge = (status: NutrientComparisonItem['status']) => {
    switch (status) {
      case 'good':
        return <span className="inline-flex rounded-full bg-leaf-100 px-2.5 py-0.5 text-xs font-semibold text-leaf-800">Trong khoảng tham khảo</span>;
      case 'low':
        return <span className="inline-flex rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-semibold text-amber-800">Thấp hơn tham khảo</span>;
      case 'high':
        return <span className="inline-flex rounded-full bg-rose-100 px-2.5 py-0.5 text-xs font-semibold text-rose-800">Cao hơn tham khảo</span>;
      case 'missing':
        return <span className="inline-flex rounded-full bg-brand-100 px-2.5 py-0.5 text-xs font-semibold text-brand-800">Chưa đủ dữ liệu</span>;
    }
  };

  return (
    <PageContainer className="py-8">
      <nav className="mb-4 text-sm text-ink-muted">
        <Link to="/" className="hover:text-brand-600">Trang chủ</Link> / <span className="font-medium text-brand-600">Theo dõi dinh dưỡng</span>
      </nav>

      <div className="mb-6 flex flex-col items-start justify-between gap-3 sm:flex-row sm:items-end">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold uppercase tracking-[0.15em] text-brand-600">Dinh dưỡng tham khảo</span>
            <Badge tone="leaf">FR-37</Badge>
          </div>
          <h1 className="mt-1 text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">Theo dõi Dinh dưỡng Thực đơn</h1>
          <p className="mt-1 max-w-2xl text-sm text-ink-muted">
            Tổng hợp 9 chỉ tiêu tham khảo từ thực đơn dự kiến của bạn. Số liệu tính theo khẩu phần dự kiến, không ngầm hiểu món đã lên lịch là món đã ăn.
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" onClick={() => setPdfAlert(true)}>
            <Download className="h-4 w-4" /> Xuất báo cáo PDF
          </Button>
          <Button onClick={triggerAnalyze}>
            <FileText className="h-4 w-4" /> Phân tích tổng thể
          </Button>
        </div>
      </div>

      {/* PDF simulated alert */}
      {pdfAlert && (
        <div className="mb-6 flex items-center justify-between rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-5 w-5 shrink-0 text-amber-600" />
            <span>Tính năng xuất PDF đang ở chế độ mô phỏng giao diện và chưa kết nối dịch vụ tạo tệp PDF thật.</span>
          </div>
          <button type="button" onClick={() => setPdfAlert(false)} className="text-xs font-bold text-amber-900 underline hover:no-underline">
            Đóng
          </button>
        </div>
      )}

      {/* Uniform Nutrition Disclaimer Banner */}
      <div className="mb-6 flex items-start gap-3 rounded-2xl border border-leaf-200 bg-leaf-50/60 p-4 text-xs leading-relaxed text-ink-soft">
        <Info className="h-4 w-4 shrink-0 text-leaf-600 mt-0.5" />
        <div>
          <strong className="text-leaf-800">Tuyên bố miễn trừ y tế (BR-08 / FR-37):</strong>
          <p className="mt-0.5 text-ink-muted">
            Thông tin chỉ mang tính tham khảo, không phải theo dõi sức khỏe lâm sàng, không có giá trị pháp lý/y tế và không thay thế chuyên gia y tế. Mâm Xanh không cung cấp Health Score, không đưa ra chẩn đoán hay phác đồ điều trị bệnh.
          </p>
        </div>
      </div>

      {/* Day Selector */}
      <div className="mb-6 flex items-center gap-2">
        <button
          onClick={() => setActiveDay((d) => Math.max(0, d - 1))}
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-brand-200 bg-white text-ink-soft hover:bg-brand-50"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
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
        <button
          onClick={() => setActiveDay((d) => Math.min(days.length - 1, d + 1))}
          className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-brand-200 bg-white text-ink-soft hover:bg-brand-50"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>

      {/* Macro Cards */}
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

      {/* Calorie distribution */}
      <Card className="mb-6 p-5">
        <div className="mb-3 flex items-center justify-between">
          <h3 className="font-bold text-ink">Phân bố Calorie theo 3 bữa ăn</h3>
          <span className="text-sm text-ink-muted">Tổng hợp thực đơn: <strong className="text-ink">1,290 kcal</strong></span>
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

      {/* Meals of the day */}
      <h3 className="mb-3 flex items-center gap-2 text-lg font-extrabold text-ink">
        🍽️ Thực đơn 3 Bữa Ngày {days[activeDay].date}
      </h3>
      <div className="mb-6 grid gap-4 md:grid-cols-3">
        {meals.map((meal) => (
          <Card key={meal.slot} hover className="overflow-hidden">
            <div className="relative">
              <img src={meal.recipe.image} alt="" className="h-36 w-full object-cover" />
              <div className="absolute left-3 top-3"><Badge tone="brand" soft={false}>{meal.slot}</Badge></div>
              <div className="absolute right-3 top-3"><Badge tone="neutral">Thực đơn mẫu</Badge></div>
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

      {/* MODAL PHÂN TÍCH TỔNG THỂ (FR-37) */}
      <Modal open={analysisOpen} onClose={() => setAnalysisOpen(false)} title="Phân tích dinh dưỡng tổng thể (FR-37)">
        <div className="space-y-5">
          {/* Header row in modal */}
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-brand-100 pb-3">
            {/* Period tabs */}
            <div className="flex rounded-xl bg-brand-100/60 p-1">
              <button
                type="button"
                onClick={() => setPeriod('day')}
                className={`rounded-lg px-3 py-1.5 text-xs font-bold transition ${
                  period === 'day' ? 'bg-white text-ink shadow-sm' : 'text-ink-soft hover:text-ink'
                }`}
              >
                Hôm nay (1 ngày)
              </button>
              <button
                type="button"
                onClick={() => setPeriod('week')}
                className={`rounded-lg px-3 py-1.5 text-xs font-bold transition ${
                  period === 'week' ? 'bg-white text-ink shadow-sm' : 'text-ink-soft hover:text-ink'
                }`}
              >
                Tuần qua (7 ngày)
              </button>
            </div>

            {/* Test Simulation selector */}
            <div className="flex items-center gap-1.5 text-xs text-ink-muted">
              <span>Mô phỏng state:</span>
              <select
                value={analysisState}
                onChange={(e) => setAnalysisState(e.target.value as typeof analysisState)}
                className="rounded-lg border border-brand-200 bg-white p-1 text-xs text-ink outline-none"
              >
                <option value="success">Success</option>
                <option value="incomplete">Incomplete data</option>
                <option value="empty">Empty</option>
                <option value="error">Error</option>
              </select>
            </div>
          </div>

          {loading ? (
            <div className="py-12 text-center">
              <RefreshCw className="mx-auto h-8 w-8 animate-spin text-brand-600" />
              <p className="mt-3 text-sm font-bold text-ink">Đang tổng hợp 9 chỉ tiêu dinh dưỡng...</p>
              <p className="text-xs text-ink-muted">Dữ liệu được tính theo khẩu phần dự kiến của thực đơn.</p>
            </div>
          ) : analysisState === 'empty' ? (
            <div className="py-10 text-center">
              <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-brand-100 text-brand-600">
                <FileText className="h-6 w-6" />
              </div>
              <h4 className="mt-3 font-bold text-ink">Chưa có thực đơn để phân tích</h4>
              <p className="mt-1 text-xs text-ink-muted">Hãy thêm món vào kế hoạch ngày trước khi thực hiện phân tích tổng thể.</p>
            </div>
          ) : analysisState === 'error' ? (
            <div className="rounded-xl border border-rose-200 bg-rose-50 p-6 text-center">
              <AlertCircle className="mx-auto h-8 w-8 text-rose-600" />
              <h4 className="mt-2 font-bold text-rose-900">Không thể tải dữ liệu dinh dưỡng</h4>
              <p className="mt-1 text-xs text-rose-700">Dữ liệu bạn đã chọn vẫn được giữ nguyên. Vui lòng thử lại.</p>
              <Button size="sm" variant="outline" className="mt-4" onClick={() => setAnalysisState('success')}>
                Thử lại
              </Button>
            </div>
          ) : (
            <>
              {/* Incomplete warning if state is incomplete */}
              {analysisState === 'incomplete' && (
                <div className="flex items-center gap-2 rounded-xl border border-amber-200 bg-amber-50 p-3 text-xs text-amber-800">
                  <AlertTriangle className="h-4 w-4 shrink-0 text-amber-600" />
                  <span>Cảnh báo: Một số công thức hoặc nguyên liệu trong thực đơn chưa có đầy đủ số liệu dinh dưỡng chuẩn hóa.</span>
                </div>
              )}

              {/* 9 Nutrients Table */}
              <div className="overflow-x-auto rounded-xl border border-brand-100">
                <table className="w-full text-left text-xs sm:text-sm">
                  <thead>
                    <tr className="border-b border-brand-100 bg-brand-50/50 text-[11px] font-bold uppercase text-ink-muted">
                      <th className="p-3">Chỉ tiêu</th>
                      <th className="p-3">Thực đơn</th>
                      <th className="p-3">Khuyến nghị</th>
                      <th className="p-3">% Đạt</th>
                      <th className="p-3">Trạng thái</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-brand-50">
                    {currentNutrients.map((item, idx) => (
                      <tr key={idx} className="hover:bg-brand-50/30">
                        <td className="p-3 font-semibold text-ink">{item.name}</td>
                        <td className="p-3 font-bold text-ink">{item.actual}</td>
                        <td className="p-3 text-ink-muted">{item.target}</td>
                        <td className="p-3 font-semibold text-ink">{item.percentage}</td>
                        <td className="p-3">{renderStatusBadge(item.status)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* PDF note and disclaimer */}
              <div className="rounded-xl border border-brand-100 bg-brand-50/40 p-3 text-xs text-ink-muted leading-relaxed">
                <p>
                  <strong>Lưu ý:</strong> Dữ liệu mỗi món được tính theo khẩu phần dự định. Không tạo Health Score, nhãn "lành mạnh/không lành mạnh" hoặc lời khuyên điều trị theo quy định của FR-37.
                </p>
              </div>

              <div className="flex justify-between items-center pt-2">
                <span className="text-xs text-ink-muted">Mâm Xanh · Báo cáo dinh dưỡng tham khảo</span>
                <Button variant="outline" onClick={() => setAnalysisOpen(false)}>
                  Đóng
                </Button>
              </div>
            </>
          )}
        </div>
      </Modal>
    </PageContainer>
  );
}
