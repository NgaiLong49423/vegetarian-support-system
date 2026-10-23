import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Activity, RefreshCw } from 'lucide-react';
import { useAuth } from '../components/AuthContext';
import { Card } from '../components/ui';
import { getAiTelemetry, type TelemetryQuery, type TelemetryReport } from '../services/aiTelemetryApi';

const features: Record<string, string> = {
  'FR-21': 'Hỗ trợ viết bài', 'FR-34': 'Gợi ý công thức', 'FR-36': 'Thực đơn tuần',
  'FR-37': 'Phân tích dinh dưỡng', 'FR-47': 'Món biến tấu', 'FR-51': 'Chatbot',
};
const number = new Intl.NumberFormat('vi-VN');
const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'USD', minimumFractionDigits: 6, maximumFractionDigits: 6 });
function localDate(date: Date) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit' }).format(date);
}

export function AiTelemetryPage() {
  const { account } = useAuth();
  const today = localDate(new Date());
  const minimum = localDate(new Date(Date.now() - 90 * 86400000));
  const [draft, setDraft] = useState<TelemetryQuery>({ from: today, to: today, granularity: 'DAY', feature: '' });
  const [query, setQuery] = useState(draft);
  const [reload, setReload] = useState(0);
  const [report, setReport] = useState<TelemetryReport | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const latest = useRef(0);
  useEffect(() => {
    const id = ++latest.current;
    setReport(null); setError('');
    if (account?.role !== 'ADMIN') return;
    const controller = new AbortController();
    setLoading(true);
    getAiTelemetry(query, controller.signal).then(result => {
      if (id === latest.current && !controller.signal.aborted) setReport(result);
    }).catch((failure: Error) => {
      if (id === latest.current && !controller.signal.aborted) setError(failure.message);
    }).finally(() => {
      if (id === latest.current && !controller.signal.aborted) setLoading(false);
    });
    return () => controller.abort();
  }, [query, reload, account?.id, account?.role]);

  if (account?.role !== 'ADMIN') return null;
  function submit(event: FormEvent) {
    event.preventDefault();
    if (draft.from > draft.to) { setError('Ngày bắt đầu không được sau ngày kết thúc.'); return; }
    setQuery({ ...draft });
  }
  const input = 'min-h-11 w-full rounded-xl border border-brand-200 bg-cream px-3 text-sm text-ink focus-visible:outline-2 focus-visible:outline-brand-600';
  return (
    <div className="space-y-6 p-4 sm:p-6 lg:p-8">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div><p className="text-sm font-semibold text-brand-600">Vận hành AI</p><h1 className="mt-1 text-2xl font-extrabold text-ink">Mức sử dụng AI</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-ink-muted">Theo dõi token và chi phí quy đổi ước tính theo giá Standard trả phí. Số liệu không phải hóa đơn Google và không ảnh hưởng quyền sử dụng gói AI.</p></div>
        <button type="button" disabled={loading} onClick={() => setReload(value => value + 1)} className="flex min-h-11 shrink-0 items-center gap-2 rounded-xl border border-brand-200 px-4 text-sm text-ink focus-visible:outline-2 focus-visible:outline-brand-600 disabled:opacity-50"><RefreshCw className="h-4 w-4" aria-hidden="true" />Làm mới</button>
      </div>
      <Card className="p-4 sm:p-5"><form onSubmit={submit} className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <label className="space-y-2 text-sm text-ink-soft"><span>Từ ngày</span><input className={input} type="date" required min={minimum} max={today} value={draft.from} onChange={e => setDraft({ ...draft, from: e.target.value })} /></label>
        <label className="space-y-2 text-sm text-ink-soft"><span>Đến ngày</span><input className={input} type="date" required min={minimum} max={today} value={draft.to} onChange={e => setDraft({ ...draft, to: e.target.value })} /></label>
        <label className="space-y-2 text-sm text-ink-soft"><span>Chu kỳ</span><select className={input} value={draft.granularity} onChange={e => setDraft({ ...draft, granularity: e.target.value as TelemetryQuery['granularity'] })}><option value="DAY">Theo ngày</option><option value="WEEK">Theo tuần</option><option value="MONTH">Theo tháng</option></select></label>
        <label className="space-y-2 text-sm text-ink-soft"><span>Tính năng</span><select className={input} value={draft.feature} onChange={e => setDraft({ ...draft, feature: e.target.value })}><option value="">Tất cả tính năng</option>{Object.entries(features).map(([id, label]) => <option key={id} value={id}>{label}</option>)}</select></label>
        <button disabled={loading} className="min-h-11 self-end rounded-xl bg-brand-600 px-4 font-bold text-on-brand disabled:opacity-50">Xem báo cáo</button>
      </form></Card>
      {loading && <p role="status" className="text-ink-muted">Đang tải số liệu AI…</p>}
      {error && <div role="alert" className="rounded-xl border border-red-400/40 p-4 text-ink"><p>{error}</p><button type="button" className="mt-2 min-h-11 underline" onClick={() => setReload(value => value + 1)}>Thử lại</button></div>}
      {report && <>
        <div className="grid gap-4 sm:grid-cols-3">
          {[['Lượt gọi thành công', number.format(report.totals.calls)], ['Tổng token', number.format(report.totals.totalTokens)], ['Chi phí quy đổi (USD)', report.totals.estimatedCostUsd == null ? 'Chưa tính đủ' : money.format(report.totals.estimatedCostUsd)]].map(([label, value]) => <Card key={label} className="p-5"><p className="text-sm text-ink-muted">{label}</p><p className="mt-2 break-words text-2xl font-extrabold text-ink">{value}</p></Card>)}
        </div>
        <p className="text-sm leading-6 text-ink-muted">Báo cáo: {report.from} – {report.to} · Múi giờ Việt Nam · Tuần bắt đầu Thứ Hai. Chỉ giữ dữ liệu 90 ngày; mốc còn dữ liệu: {new Date(report.retainedFrom).toLocaleString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' })}.</p>
        {(report.totals.incompleteUsageCalls > 0 || report.totals.unpricedCalls > 0) && <div role="note" className="rounded-xl border border-amber-500/40 p-4 text-sm leading-6 text-ink-soft">Có {number.format(report.totals.incompleteUsageCalls)} lượt thiếu hoặc có metadata bất thường và {number.format(report.totals.unpricedCalls)} lượt chưa tính được chi phí. Phần đã tính: {money.format(report.totals.knownEstimatedCostUsd)}; tổng chi phí chưa đầy đủ.</div>}
        {report.totals.calls === 0 ? <Card className="p-10 text-center"><Activity className="mx-auto h-8 w-8 text-ink-muted" aria-hidden="true" /><h2 className="mt-3 font-bold text-ink">Chưa có dữ liệu sử dụng AI</h2><p className="mt-2 text-sm text-ink-muted">Không có lượt gọi thành công trong khoảng ngày và tính năng đã chọn.</p></Card> :
          <Card className="overflow-hidden"><div className="overflow-x-auto" tabIndex={0} role="region" aria-label="Bảng thống kê token AI"><table className="w-full min-w-[850px] text-left text-sm"><caption className="sr-only">Lượng token và chi phí theo kỳ, theo tính năng</caption><thead className="bg-cream-soft text-ink-muted"><tr>{['Đầu kỳ', 'Tính năng', 'Lượt gọi', 'Input', 'Response', 'Thinking', 'Cached', 'Tổng token', 'Ước tính USD'].map(label => <th scope="col" className="whitespace-nowrap p-4 font-semibold" key={label}>{label}</th>)}</tr></thead><tbody className="divide-y divide-brand-100 text-ink">{report.buckets.map(row => <tr key={`${row.periodStart}-${row.feature}`}><td className="whitespace-nowrap p-4">{row.periodStart}</td><td className="p-4">{features[row.feature] ?? row.feature}</td>{[row.totals.calls,row.totals.promptTokens,row.totals.responseTokens,row.totals.thinkingTokens,row.totals.cachedTokens,row.totals.totalTokens].map((value,index) => <td className="p-4 tabular-nums" key={index}>{number.format(value)}</td>)}<td className="whitespace-nowrap p-4">{row.totals.estimatedCostUsd == null ? 'Chưa tính đủ' : money.format(row.totals.estimatedCostUsd)}</td></tr>)}</tbody></table></div></Card>}
        <p className="text-xs leading-5 text-ink-muted">Báo cáo chỉ chứa số liệu tổng hợp, không chứa câu hỏi, câu trả lời hoặc thông tin tài khoản. Thinking đã nằm trong tổng token; cached là phần của input, không cộng lại vào tổng.</p>
      </>}
    </div>
  );
}
