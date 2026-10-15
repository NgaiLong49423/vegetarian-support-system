import { useEffect, useMemo, useState } from "react"
import { Link } from "react-router-dom"
import { Button } from "../components/ui"
import { mealPlanApi, type MealPlanNutrition as NutritionData, type MealPlanNutritionRecommendations, type MealPlanWeek } from "../api/recipes"
import { asApiError, asApiErrorAsync, type ApiError } from "../lib/apiClient"

const disclaimer = "Thông tin dinh dưỡng trong báo cáo chỉ mang tính tham khảo, được ước tính từ thực đơn dự kiến; không phải kết quả khám, chẩn đoán hay điều trị y tế, và không thay thế tư vấn của bác sĩ hoặc chuyên gia dinh dưỡng. Kết quả không có giá trị pháp lý hoặc y tế."
const mealLabels = { BREAKFAST: "Sáng", LUNCH: "Trưa", DINNER: "Tối" }
function fmt(value: number | null | undefined, unit: string) {
  return value == null ? "Chưa đủ dữ liệu" : `${new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2 }).format(value)} ${unit}`
}

export function MealPlanNutrition({ plan, initialDay, initialScope, onEntryReplaced }: {
  plan: MealPlanWeek; initialDay: string; initialScope: "day" | "week";
  onEntryReplaced: (entry: MealPlanWeek["entries"][number]) => void
}) {
  const [scope, setScope] = useState(initialScope)
  const [day, setDay] = useState(initialDay)
  const [data, setData] = useState<NutritionData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<ApiError | null>(null)
  const [exportError, setExportError] = useState<ApiError | null>(null)
  const [exporting, setExporting] = useState(false)
  const [aiError, setAiError] = useState("")
  const [aiLoading, setAiLoading] = useState<number | null>(null)
  const [recommendations, setRecommendations] = useState<MealPlanNutritionRecommendations | null>(null)
  const [accepting, setAccepting] = useState<number | null>(null)
  const [retry, setRetry] = useState(0)
  const entries = scope === "week" ? plan.entries : plan.entries.filter((entry) => entry.mealDate === day)
  const days = useMemo(() => Array.from({ length: 7 }, (_, offset) => {
    const date = new Date(`${plan.weekStartDate}T12:00:00`)
    date.setDate(date.getDate() + offset)
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
  }), [plan.weekStartDate])

  useEffect(() => {
    let active = true
    setLoading(true); setError(null); setExportError(null); setAiError(""); setRecommendations(null)
    mealPlanApi.nutrition(plan.weekStartDate, scope === "day" ? day : undefined)
      .then((result) => { if (active) setData(result) })
      .catch((cause) => { if (active) setError(asApiError(cause)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [plan.weekStartDate, scope, day, retry])

  async function exportPdf() {
    setExporting(true); setExportError(null)
    try {
      const blob = await mealPlanApi.nutritionPdf(plan.weekStartDate, scope === "day" ? day : undefined)
      const url = URL.createObjectURL(blob)
      const link = document.createElement("a")
      link.href = url; link.download = `bao-cao-dinh-duong-${scope === "day" ? day : plan.weekStartDate}.pdf`
      document.body.appendChild(link); link.click(); link.remove()
      window.setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (cause) { setExportError(await asApiErrorAsync(cause)) }
    finally { setExporting(false) }
  }

  async function askAi(entryId: number) {
    setAiLoading(entryId); setAiError(""); setRecommendations(null)
    try { setRecommendations(await mealPlanApi.nutritionRecommendations(plan.weekStartDate, entryId)) }
    catch (cause) { setAiError(asApiError(cause).message) }
    finally { setAiLoading(null) }
  }

  async function acceptSuggestion(entryId: number, recipeId: number) {
    setAccepting(recipeId); setAiError("")
    try {
      const saved = await mealPlanApi.replaceEntry(plan.weekStartDate, entryId, recipeId)
      onEntryReplaced(saved)
      setRecommendations(null); setRetry((value) => value + 1)
    } catch (cause) { setAiError(asApiError(cause).message) }
    finally { setAccepting(null) }
  }

  const average = (key: string) => {
    if (!data || data.days.length !== 7) return null
    return data.weeklyAverage[key] ?? null
  }
  const targetMap = new Map((data?.targets ?? []).map((target) => [target.key, target]))
  const metrics = data?.targets ?? []
  const canExport = !loading && !error && entries.length > 0

  return <div className="space-y-4">
    <div className="flex flex-wrap items-center gap-2" aria-label="Phạm vi dinh dưỡng">
      <Button size="sm" variant={scope === "day" ? "secondary" : "ghost"} aria-pressed={scope === "day"} onClick={() => setScope("day")}>Theo ngày</Button>
      <Button size="sm" variant={scope === "week" ? "secondary" : "ghost"} aria-pressed={scope === "week"} onClick={() => setScope("week")}>So sánh &amp; Thống kê 7 ngày</Button>
      <Button size="sm" variant="ghost" disabled={!canExport || exporting} onClick={() => void exportPdf()}>{exporting ? "Đang tạo PDF…" : "Xuất báo cáo PDF"}</Button>
    </div>
    {scope === "day" && <label className="block text-sm text-ink-soft">Ngày xem<select value={day} onChange={(event) => setDay(event.target.value)} className="mt-2 block w-full rounded-lg border border-brand-200 bg-paper p-2 text-ink">{days.map((date) => <option key={date} value={date}>{new Date(`${date}T12:00:00`).toLocaleDateString("vi-VN", { weekday: "long", day: "numeric", month: "long" })}</option>)}</select></label>}
    <p className="rounded-xl bg-raised p-3 text-xs leading-5 text-ink-muted">{data?.referenceBasis ?? "Mức tham khảo sẽ hiện cùng bảng dinh dưỡng."} · Dữ liệu tính từ số khẩu phần dự kiến đã lưu. Không phải lượng thực tế đã ăn.</p>
    {loading && <p role="status" className="py-6 text-center text-sm text-ink-muted">Đang tính dinh dưỡng…</p>}
    {error && <div role="alert" className="space-y-2 text-sm text-ink-muted"><p>{error.message}</p>{error.code === "NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED" && <p>Hãy <Link to="/ho-so/dinh-duong" className="font-semibold underline">xác nhận phạm vi hỗ trợ dinh dưỡng</Link> trước khi sử dụng.</p>}<Button size="sm" variant="ghost" onClick={() => setRetry((value) => value + 1)}>Thử lại</Button></div>}
    {exportError && <div role="alert" className="space-y-2 text-sm text-ink-muted"><p>{exportError.message}</p>{exportError.code === "NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED" && <p>Hãy <Link to="/ho-so/dinh-duong" className="font-semibold underline">xác nhận phạm vi hỗ trợ dinh dưỡng</Link> trước khi xuất báo cáo.</p>}<Button size="sm" variant="ghost" onClick={() => void exportPdf()}>Thử xuất lại</Button></div>}
    {!loading && data && scope === "day" && entries.length === 0 && <p className="rounded-xl bg-raised p-3 text-sm text-ink-muted">Ngày này chưa có món để tính dinh dưỡng.</p>}
    {!loading && data && scope === "week" && entries.length === 0 && <p className="rounded-xl bg-raised p-3 text-sm text-ink-muted">Tuần này chưa có món để tính dinh dưỡng. Hãy thêm món vào lịch ăn trước khi xuất báo cáo.</p>}
    {!loading && data && <>
      {data.incompleteEntries.length > 0 && <div role="status" className="rounded-xl border border-brand-200 bg-raised p-3 text-sm text-ink-soft"><strong>Dữ liệu dinh dưỡng chưa đầy đủ.</strong><ul className="mt-2 list-disc pl-5">{data.incompleteEntries.map((entry) => <li key={entry.entryId}>{entry.recipeTitle}: {entry.ingredientsMissingData.join(", ") || "chưa có dữ liệu công thức"}</li>)}</ul></div>}
      <div className="overflow-x-auto rounded-xl border border-brand-100">
        <table className="min-w-[920px] w-full border-collapse text-xs"><thead><tr className="bg-raised text-left"><th className="sticky left-0 bg-raised p-2">Chỉ tiêu</th>{scope === "week" ? data.days.map((item) => <th key={item.date} className="p-2">{new Date(`${item.date}T12:00:00`).toLocaleDateString("vi-VN", { weekday: "short", day: "numeric" })}</th>) : <th className="p-2">Trong ngày</th>}<th className="p-2">{scope === "week" ? "Trung bình / 7 ngày" : "Mức tham khảo"}</th><th className="p-2">Tham khảo</th></tr></thead>
          <tbody>{metrics.map((metric) => { const values = scope === "week" ? data.days.map((item) => item.nutrients[metric.key]) : [data.days[0]?.nutrients[metric.key]]; const ref = targetMap.get(metric.key); const status = (value: number | null | undefined) => value == null || ref?.minimum == null ? "bg-paper" : ref.referenceType === "AMDR" ? (value >= ref.minimum && (ref.maximum == null || value <= ref.maximum) ? "bg-emerald-50 text-emerald-900" : "bg-amber-50 text-amber-900") : (value >= ref.minimum ? "bg-emerald-50 text-emerald-900" : "bg-amber-50 text-amber-900"); return <tr key={metric.key} className="border-t border-brand-100"><th scope="row" className="sticky left-0 bg-paper p-2 text-left font-medium">{metric.label}</th>{values.map((value, index) => <td key={index} className={`p-2 tabular-nums ${status(value)}`}>{fmt(value, metric.unit)}</td>)}<td className="p-2 tabular-nums">{scope === "week" ? fmt(average(metric.key), metric.unit) : fmt(ref?.minimum, metric.unit)}</td><td className="p-2">{ref ? `${ref.referenceType} · ${ref.source}` : "Chưa có mức tham khảo"}</td></tr>})}</tbody></table>
      </div>
      {scope === "week" && <section aria-label="Tỷ lệ ngày đạt tham khảo" className="space-y-2"><h3 className="text-sm font-semibold">Tỷ lệ ngày đạt theo từng chỉ tiêu</h3>{metrics.map((metric) => { const values = data.days.map((item) => item.nutrients[metric.key]); const known = values.filter((value): value is number => value != null); const passed = data.attainmentDays[metric.key] ?? 0; return <p key={metric.key} className="text-xs text-ink-muted">{metric.label}: {known.length === 7 ? `${passed}/7 ngày (${Math.round(passed / 7 * 100)}%)` : `Chưa đủ dữ liệu (${known.length}/7 ngày)`}</p>})}</section>}
      {scope === "day" && entries.length > 0 && <section className="space-y-2"><h3 className="text-sm font-semibold">Giải thích chênh lệch bằng AI</h3><p className="text-xs text-ink-muted">Dành cho gói Pro. AI chỉ chọn công thức có sẵn; số liệu lấy từ catalog dinh dưỡng.</p>{entries.map((entry) => <div key={entry.entryId} className="rounded-lg border border-brand-100 p-3"><p className="text-sm">{mealLabels[entry.mealType]} · {entry.recipeTitle ?? "Công thức không còn khả dụng"}</p><Button size="sm" variant="ghost" disabled={aiLoading != null || !entry.recipeTitle} onClick={() => void askAi(entry.entryId)}>{aiLoading === entry.entryId ? "Đang phân tích…" : "Phân tích và gợi ý món"}</Button></div>)}</section>}
      {recommendations && <section className="space-y-2 rounded-xl bg-raised p-3"><h3 className="text-sm font-semibold">Phân tích của AI</h3><p className="text-sm leading-6 text-ink-soft">{recommendations.analysis}</p><p className="text-xs text-ink-muted">Mức và số liệu chi tiết được lấy từ bảng dinh dưỡng nội bộ.</p><h4 className="pt-2 text-sm font-semibold">Món được gợi ý</h4>{recommendations.recommendations.length ? recommendations.recommendations.map((item) => <div key={item.recipeId} className="flex flex-wrap items-center justify-between gap-2 border-t border-brand-100 py-2"><span>{item.title}{!item.complete && <span className="block text-xs text-ink-muted">Thiếu dữ liệu dinh dưỡng</span>}</span><Button size="sm" disabled={accepting != null} onClick={() => void acceptSuggestion(recommendations.entryId, item.recipeId)}>{accepting === item.recipeId ? "Đang thay…" : "Thay món này"}</Button></div>) : <p className="text-sm text-ink-muted">Chưa có món phù hợp trong kho công thức.</p>}</section>}
      {aiError && <p role="alert" className="text-sm text-red-700">{aiError} Bảng dinh dưỡng vẫn có thể xem; bạn có thể thử lại sau.</p>}
    </>}
    <p className="text-xs leading-5 text-ink-muted">{disclaimer}</p>
  </div>
}
