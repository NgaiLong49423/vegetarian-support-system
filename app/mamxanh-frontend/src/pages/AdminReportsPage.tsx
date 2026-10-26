import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { AlertCircle, CheckCircle2, Clock, ExternalLink, RefreshCw, Search, ShieldAlert } from 'lucide-react';
import { Button, Card } from '../components/ui';
import { Modal } from '../components/Modal';
import { useAuth } from '../components/AuthContext';
import { asApiError } from '../lib/apiClient';
import { formatSubscriptionDateTime as formatDateTime } from '../utils/subscriptionDate';
import {
  adminReportsApi,
  type AdminReport,
  type AdminReportGroup,
  type ReportDecision,
  type ReportGroupSort,
  type ReportStatus,
  type ReportStatusCounts,
} from '../api/adminReports';

const REASON_MIN = 10;
const REASON_MAX = 500;
const QUERY_MAX = 200;

const fieldClass =
  'w-full rounded-xl border border-brand-200/80 bg-paper px-3.5 py-2.5 text-sm text-ink outline-none transition-all placeholder:text-ink-muted/60 focus:border-brand-500 focus:ring-2 focus:ring-brand-100';

const reasonLabels: Record<string, string> = {
  RECIPE_INFO_OR_DIET_LABEL: 'Sai thông tin công thức hoặc nhãn ăn chay',
  FOOD_SAFETY_HAZARD: 'Nguy cơ an toàn thực phẩm',
  SPAM_ADVERTISING: 'Spam hoặc quảng cáo không phù hợp',
  INAPPROPRIATE_CONTENT: 'Nội dung xúc phạm hoặc không phù hợp',
  COPYRIGHT_VIOLATION: 'Sao chép hoặc nghi vi phạm bản quyền',
  OTHER: 'Lý do khác',
};

const statusLabels: Record<ReportStatus, string> = {
  OPEN: 'Chờ xử lý',
  IN_REVIEW: 'Đang xem xét',
  RESOLVED: 'Đã giải quyết',
};

const statusStyles: Record<ReportStatus, { badge: string; border: string }> = {
  OPEN: { badge: 'border-amber-200 bg-amber-50 text-amber-800', border: 'border-l-amber-500' },
  IN_REVIEW: { badge: 'border-brand-200 bg-brand-50 text-brand-700', border: 'border-l-brand-500' },
  RESOLVED: { badge: 'border-leaf-200 bg-leaf-50 text-leaf-700', border: 'border-l-leaf-500' },
};

const decisions: Array<{ value: ReportDecision; label: string; hint: string }> = [
  { value: 'NO_VIOLATION', label: 'Không vi phạm', hint: 'Bài vẫn hiển thị bình thường.' },
  { value: 'WARN_AUTHOR', label: 'Cảnh cáo tác giả', hint: 'Bài vẫn hiển thị. Tác giả nhận thông báo kèm lý do.' },
  { value: 'HIDE_RECIPE', label: 'Ẩn bài viết', hint: 'Bài không còn xuất hiện ở trang công khai và kết quả tìm kiếm.' },
  { value: 'LOCK_AUTHOR', label: 'Khóa tài khoản tác giả', hint: 'Tác giả bị đăng xuất và không đăng nhập lại được.' },
];

const filters: Array<{ value: ReportStatus | ''; label: string; count: (counts: ReportStatusCounts) => number }> = [
  { value: 'OPEN', label: 'Chờ xử lý', count: (counts) => counts.open },
  { value: 'IN_REVIEW', label: 'Đang xem xét', count: (counts) => counts.inReview },
  { value: 'RESOLVED', label: 'Đã giải quyết', count: (counts) => counts.resolved },
  { value: '', label: 'Tất cả', count: (counts) => counts.open + counts.inReview + counts.resolved },
];

// Group cards print "Lý do: …", so OTHER reads "Khác" there instead of "Lý do khác".
const reasonLabel = (code: string) => (code === 'OTHER' ? 'Khác' : reasonLabels[code] ?? code);

const decisionLabel = (decision: ReportDecision | null) =>
  decisions.find((item) => item.value === decision)?.label ?? 'Chưa có kết luận';

const groupTotal = (counts: ReportStatusCounts) => counts.open + counts.inReview + counts.resolved;

/** The most urgent status of a group sets its accent: open reports first, then reports in review. */
const groupStatus = (counts: ReportStatusCounts): ReportStatus =>
  counts.open > 0 ? 'OPEN' : counts.inReview > 0 ? 'IN_REVIEW' : 'RESOLVED';

const groupBreakdown = (counts: ReportStatusCounts) => [
  counts.open > 0 && `${counts.open} chờ xử lý`,
  counts.inReview > 0 && `${counts.inReview} đang xem xét`,
  counts.resolved > 0 && `${counts.resolved} đã giải quyết`,
].filter(Boolean).join(' · ');

function StatusBadge({ status }: { status: ReportStatus }) {
  return (
    <span className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-bold ${statusStyles[status].badge}`}>
      {statusLabels[status]}
    </span>
  );
}

export function AdminReportsPage() {
  const { account } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();
  // AC-30.4: the board opens grouped by recipe; ?xem=bao-cao keeps the FR-28 queue of single reports.
  const view = searchParams.get('xem') === 'bao-cao' ? 'REPORTS' : 'GROUPS';
  const [recipe, setRecipe] = useState<{ id: number; title: string } | null>(null);
  const [filter, setFilter] = useState<ReportStatus | ''>('OPEN');
  // UC-06.1: the grouped view can be searched by post title or author and ranked by report count.
  const [draft, setDraft] = useState('');
  const [query, setQuery] = useState('');
  const [sort, setSort] = useState<ReportGroupSort>('NEWEST');
  const [page, setPage] = useState(0);
  const [groups, setGroups] = useState<AdminReportGroup[]>([]);
  const [reports, setReports] = useState<AdminReport[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [counts, setCounts] = useState<ReportStatusCounts | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);
  const [selected, setSelected] = useState<AdminReport | null>(null);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const recipeId = view === 'REPORTS' ? recipe?.id : undefined;
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const listing = view === 'GROUPS'
        ? adminReportsApi.groups(filter, page, query, sort).then((data) => { setGroups(data.content); return data.totalPages; })
        : adminReportsApi.list(filter, page, recipeId).then((data) => { setReports(data.content); return data.totalPages; });
      const [pages, countData] = await Promise.all([listing, adminReportsApi.counts()]);
      setTotalPages(pages);
      setCounts(countData);
      setLoadFailed(false);
    } catch (cause) {
      setError(asApiError(cause).message);
      setLoadFailed(true);
    } finally {
      setLoading(false);
    }
  }, [view, filter, page, recipeId, query, sort]);

  useEffect(() => {
    void load();
  }, [load]);

  const heldByMe = (report: AdminReport) => report.status === 'IN_REVIEW' && report.handledBy?.id === account?.id;

  async function openDetail(report: AdminReport) {
    setBusyId(report.id);
    setError('');
    try {
      setSelected(await adminReportsApi.detail(report.id));
    } catch (cause) {
      setError(asApiError(cause).message);
    } finally {
      setBusyId(null);
    }
  }

  async function claim(report: AdminReport) {
    setBusyId(report.id);
    setError('');
    setNotice('');
    try {
      const updated = await adminReportsApi.claim(report.id);
      setSelected(updated);
      setNotice(`Bạn đã tiếp nhận báo cáo #${updated.id}. Hãy xem nội dung bài rồi ghi kết luận.`);
      await load();
    } catch (cause) {
      const api = asApiError(cause);
      setError(api.message);
      if (api.status === 409 || api.status === 404) {
        setSelected(null);
        await load();
      }
    } finally {
      setBusyId(null);
    }
  }

  function showView(next: 'GROUPS' | 'REPORTS', group: { id: number; title: string } | null = null) {
    setError('');
    setPage(0);
    setRecipe(group);
    if (group) setFilter('');
    setSearchParams(next === 'REPORTS' ? { xem: 'bao-cao' } : {});
  }

  function search(event: FormEvent) {
    event.preventDefault();
    setError('');
    setPage(0);
    setQuery(draft.trim());
  }

  function resolved(updated: AdminReport) {
    setSelected(null);
    setError('');
    setNotice(`Đã giải quyết báo cáo #${updated.id} với kết luận "${decisionLabel(updated.decision)}".`);
    void load();
  }

  return (
    <div className="space-y-5 py-2">
      <header className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">Hậu kiểm nội dung</h1>
          <p className="mt-1 max-w-2xl text-sm text-ink-muted">
            Xem các bài công thức bị báo cáo, tiếp nhận từng báo cáo rồi ghi kết luận kèm lý do. Mỗi báo cáo chỉ do một quản trị viên xử lý.
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => { setError(''); void load(); }} disabled={loading}>
          <RefreshCw className="h-3.5 w-3.5" aria-hidden="true" /> Làm mới
        </Button>
      </header>

      {error && (
        <div role="alert" className="flex items-center gap-3 rounded-2xl border border-rose-200 bg-rose-50/90 p-4 text-sm text-rose-800">
          <AlertCircle className="h-5 w-5 shrink-0 text-rose-600" aria-hidden="true" />
          <p className="flex-1 font-medium">{error}</p>
        </div>
      )}
      {notice && (
        <div role="status" className="flex items-center gap-3 rounded-2xl border border-leaf-200 bg-leaf-50/90 p-4 text-sm text-leaf-800">
          <CheckCircle2 className="h-5 w-5 shrink-0 text-leaf-600" aria-hidden="true" />
          <p className="flex-1 font-medium">{notice}</p>
        </div>
      )}

      <Card className="flex flex-col gap-3 p-3.5 sm:px-5 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Lọc theo trạng thái">
          {filters.map((item) => (
            <button
              key={item.value || 'ALL'}
              type="button"
              aria-pressed={filter === item.value}
              onClick={() => {
                setError('');
                setPage(0);
                setFilter(item.value);
              }}
              className={`rounded-xl border px-3.5 py-2 text-sm font-bold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400/70 ${
                filter === item.value
                  ? 'border-brand-600 bg-brand-600 text-on-brand'
                  : 'border-brand-200 bg-paper text-ink-soft hover:border-brand-300 hover:text-brand-600'
              }`}
            >
              {item.label}
              {counts && <>{' '}<span className="tabular-nums opacity-80">({item.count(counts)})</span></>}
            </button>
          ))}
        </div>
        <div className="flex flex-wrap items-center gap-2" role="group" aria-label="Cách xem báo cáo">
          {([['GROUPS', 'Theo bài viết'], ['REPORTS', 'Từng báo cáo']] as const).map(([value, label]) => (
            <button
              key={value}
              type="button"
              aria-pressed={view === value}
              onClick={() => showView(value)}
              className={`rounded-xl border px-3 py-1.5 text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400/70 ${
                view === value
                  ? 'border-brand-500 bg-brand-50 text-brand-700'
                  : 'border-brand-200 bg-paper text-ink-soft hover:border-brand-300 hover:text-brand-600'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </Card>

      {view === 'GROUPS' && (
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <form role="search" aria-label="Tìm bài bị báo cáo" onSubmit={search} className="flex min-w-0 flex-1 items-end gap-2">
            <label className="min-w-0 flex-1 text-xs font-semibold text-ink-soft">
              Tìm theo tên bài hoặc tác giả
              <span className="relative mt-1.5 block">
                <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" aria-hidden="true" />
                <input
                  type="search"
                  value={draft}
                  maxLength={QUERY_MAX}
                  onChange={(event) => setDraft(event.target.value)}
                  placeholder="Tên bài, tên hoặc email tác giả"
                  className={`${fieldClass} pl-10`}
                />
              </span>
            </label>
            <Button type="submit" variant="outline" className="h-[42px] shrink-0 px-4">Tìm</Button>
          </form>
          <label className="text-xs font-semibold text-ink-soft sm:w-52">
            Sắp xếp
            <select
              value={sort}
              onChange={(event) => {
                setError('');
                setPage(0);
                setSort(event.target.value as ReportGroupSort);
              }}
              className={`${fieldClass} mt-1.5`}
            >
              <option value="NEWEST">Báo cáo mới nhất</option>
              <option value="MOST_REPORTED">Nhiều báo cáo nhất</option>
            </select>
          </label>
        </div>
      )}

      {view === 'REPORTS' && recipe && (
        <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-100 bg-brand-50/40 px-4 py-3">
          <p className="min-w-0 break-words text-sm font-semibold text-ink">Báo cáo của bài “{recipe.title}”</p>
          <Button variant="outline" size="sm" onClick={() => showView('GROUPS')}>Xem tất cả bài viết</Button>
        </div>
      )}

      <div className="grid gap-3.5" aria-busy={loading}>
        {view === 'GROUPS' ? (
          loading && groups.length === 0 ? (
            <Card className="p-10 text-center text-sm text-ink-muted">Đang tải danh sách báo cáo…</Card>
          ) : loadFailed && groups.length === 0 ? (
            <Card className="border-dashed border-rose-200 p-10 text-center text-sm text-ink-muted">
              Chưa tải được danh sách báo cáo. Bấm Làm mới để thử lại.
            </Card>
          ) : groups.length === 0 ? (
            <Card className="border-dashed border-brand-200 p-10 text-center text-sm text-ink-muted">
              {query ? `Không tìm thấy bài viết nào khớp “${query}”.` : 'Không có bài viết nào có báo cáo ở trạng thái này.'}
            </Card>
          ) : (
            groups.map((group) => (
              <ReportGroupCard
                key={group.recipe.id}
                group={group}
                onOpen={() => showView('REPORTS', { id: group.recipe.id, title: group.recipe.title })}
              />
            ))
          )
        ) : loading && reports.length === 0 ? (
          <Card className="p-10 text-center text-sm text-ink-muted">Đang tải danh sách báo cáo…</Card>
        ) : loadFailed && reports.length === 0 ? (
          <Card className="border-dashed border-rose-200 p-10 text-center text-sm text-ink-muted">
            Chưa tải được danh sách báo cáo. Bấm Làm mới để thử lại.
          </Card>
        ) : reports.length === 0 ? (
          <Card className="border-dashed border-brand-200 p-10 text-center text-sm text-ink-muted">
            Không có báo cáo nào ở trạng thái này.
          </Card>
        ) : (
          reports.map((report) => (
            <Card key={report.id} className={`flex flex-col gap-4 border-l-4 ${statusStyles[report.status].border} p-5 sm:flex-row sm:items-start`}>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2 text-xs text-ink-muted">
                  <StatusBadge status={report.status} />
                  <span className="inline-flex items-center gap-1">
                    <Clock className="h-3.5 w-3.5 text-brand-600" aria-hidden="true" />
                    #{report.id} · Gửi lúc {formatDateTime(report.createdAt)}
                  </span>
                  {report.supplements.length > 0 && <span>{report.supplements.length} lần bổ sung</span>}
                </div>
                <h2 className="mt-2 break-words text-base font-bold text-ink">{report.recipe.title}</h2>
                <p className="mt-1 text-sm font-semibold text-ink-soft">{reasonLabels[report.reasonCode] ?? report.reasonCode}</p>
                {report.description && (
                  <div className="mt-2 rounded-xl border border-brand-100/70 bg-cream-soft/60 px-3.5 py-2">
                    <p className="line-clamp-2 text-sm text-ink-soft">{report.description}</p>
                  </div>
                )}
                <dl className="mt-3 grid gap-1 text-xs text-ink-muted sm:grid-cols-2">
                  <div>
                    <dt className="inline font-semibold">Người báo cáo: </dt>
                    <dd className="inline">{report.reporter.displayName}</dd>
                  </div>
                  <div>
                    <dt className="inline font-semibold">Tác giả: </dt>
                    <dd className="inline">{report.author.displayName}</dd>
                  </div>
                  {report.handledBy && (
                    <div>
                      <dt className="inline font-semibold">Người xử lý: </dt>
                      <dd className="inline">{report.handledBy.displayName}</dd>
                    </div>
                  )}
                  {report.decision && (
                    <div>
                      <dt className="inline font-semibold">Kết luận: </dt>
                      <dd className="inline">{decisionLabel(report.decision)}</dd>
                    </div>
                  )}
                </dl>
              </div>
              <div className="flex shrink-0 flex-wrap gap-2 sm:w-44 sm:flex-col">
                {report.status === 'OPEN' && (
                  <Button size="sm" disabled={busyId === report.id} onClick={() => void claim(report)}>
                    Tiếp nhận xem xét
                  </Button>
                )}
                {heldByMe(report) ? (
                  <Button size="sm" disabled={busyId === report.id} onClick={() => void openDetail(report)}>
                    Ghi kết luận
                  </Button>
                ) : (
                  <Button variant="outline" size="sm" disabled={busyId === report.id} onClick={() => void openDetail(report)}>
                    Xem chi tiết
                  </Button>
                )}
              </div>
            </Card>
          ))
        )}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 rounded-2xl border border-brand-100 bg-paper px-4 py-3">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>
            Trang trước
          </Button>
          <span className="text-sm font-medium text-ink-muted">Trang {page + 1}/{totalPages}</span>
          <Button variant="outline" size="sm" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>
            Trang sau
          </Button>
        </div>
      )}

      <Modal open={selected !== null} onClose={() => setSelected(null)} title={selected ? `Báo cáo #${selected.id}` : 'Báo cáo'} size="lg" trapFocus>
        {selected && (
          <ReportDetail
            report={selected}
            heldByMe={heldByMe(selected)}
            busy={busyId === selected.id}
            onClaim={() => void claim(selected)}
            onResolved={resolved}
            onClose={() => setSelected(null)}
          />
        )}
      </Modal>
    </div>
  );
}

function ReportGroupCard({ group, onOpen }: { group: AdminReportGroup; onOpen: () => void }) {
  const total = groupTotal(group.statusCounts);
  return (
    <article>
      <Card className={`flex flex-col gap-4 border-l-4 ${statusStyles[groupStatus(group.statusCounts)].border} p-5 sm:flex-row sm:items-start`}>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2 text-xs text-ink-muted">
            <span className="inline-flex items-center rounded-full border border-brand-200 bg-brand-50 px-2.5 py-0.5 font-bold text-brand-700">
              {total} báo cáo
            </span>
            {group.latestReportAt && (
              <span className="inline-flex items-center gap-1">
                <Clock className="h-3.5 w-3.5 text-brand-600" aria-hidden="true" />
                Báo cáo mới nhất lúc {formatDateTime(group.latestReportAt)}
              </span>
            )}
          </div>
          <h2 className="mt-2 break-words text-base font-bold text-ink">{group.recipe.title}</h2>
          <p className="mt-1 text-sm font-semibold text-ink-soft">
            {group.reportCount > 1
              ? `Lý do nhiều nhất: ${reasonLabel(group.topReasonCode)} (${group.topReasonCount}/${group.reportCount} báo cáo)`
              : `Lý do: ${reasonLabel(group.topReasonCode)}`}
          </p>
          {group.recipe.status !== 'PUBLISHED' && (
            <p className="mt-1 text-sm font-semibold text-ink-muted">
              {group.recipe.status === 'HIDDEN' ? 'Bài đang bị ẩn khỏi trang công khai.' : 'Tác giả đã xóa bài này.'}
            </p>
          )}
          <p className="mt-1 text-sm text-ink-soft">{groupBreakdown(group.statusCounts)}</p>
          <dl className="mt-3 text-xs text-ink-muted">
            <dt className="inline font-semibold">Tác giả: </dt>
            <dd className="inline">{group.author.displayName}</dd>
          </dl>
        </div>
        <div className="flex shrink-0 flex-wrap gap-2 sm:w-44 sm:flex-col">
          <Button variant="outline" size="sm" onClick={onOpen}>Xem {total} báo cáo</Button>
        </div>
      </Card>
    </article>
  );
}

function ReportDetail({
  report,
  heldByMe,
  busy,
  onClaim,
  onResolved,
  onClose,
}: {
  report: AdminReport;
  heldByMe: boolean;
  busy: boolean;
  onClaim: () => void;
  onResolved: (report: AdminReport) => void;
  onClose: () => void;
}) {
  return (
    <div className="space-y-4 text-sm">
      <section className="rounded-xl border border-brand-100 bg-brand-50/40 p-4">
        <h3 className="text-xs font-bold uppercase tracking-wider text-ink-muted">Bài bị báo cáo</h3>
        <p className="mt-1 break-words text-base font-bold text-ink">{report.recipe.title}</p>
        {report.recipe.status === 'PUBLISHED' ? (
          <Link
            to={`/cong-thuc/id/${report.recipe.id}`}
            target="_blank"
            rel="noreferrer"
            className="mt-1 inline-flex items-center gap-1 text-sm font-semibold text-brand-700 hover:underline"
          >
            Mở bài trong thẻ mới <ExternalLink className="h-3.5 w-3.5" aria-hidden="true" />
          </Link>
        ) : (
          <p className="mt-1 text-sm font-semibold text-ink-muted">
            {report.recipe.status === 'HIDDEN' ? 'Bài đang bị ẩn khỏi trang công khai.' : 'Tác giả đã xóa bài này.'}
          </p>
        )}
      </section>

      <section>
        <h3 className="text-xs font-bold uppercase tracking-wider text-ink-muted">Nội dung báo cáo</h3>
        <p className="mt-1 font-semibold text-ink">{reasonLabels[report.reasonCode] ?? report.reasonCode}</p>
        <p className="mt-1 whitespace-pre-line text-ink-soft">{report.description || 'Người báo cáo không ghi thêm mô tả.'}</p>
        <p className="mt-1 text-xs text-ink-muted">Gửi lúc {formatDateTime(report.createdAt)}</p>
      </section>

      {report.supplements.length > 0 && (
        <section>
          <h3 id="report-supplements-heading" className="text-xs font-bold uppercase tracking-wider text-ink-muted">Thông tin bổ sung</h3>
          <ul aria-labelledby="report-supplements-heading" className="mt-1 space-y-2">
            {report.supplements.map((item) => (
              <li key={item.supplementId} className="rounded-lg bg-cream-soft/60 px-3 py-2">
                <p className="whitespace-pre-line text-ink-soft">{item.content}</p>
                <p className="mt-0.5 text-xs text-ink-muted">{formatDateTime(item.createdAt)}</p>
              </li>
            ))}
          </ul>
        </section>
      )}

      <div className="grid gap-3 sm:grid-cols-2">
        <section className="rounded-xl border border-brand-100 p-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-ink-muted">Người báo cáo</h3>
          <p className="mt-1 font-semibold text-ink">{report.reporter.displayName}</p>
          <p className="break-all text-xs text-ink-muted">{report.reporter.email}</p>
        </section>
        <section className="rounded-xl border border-brand-100 p-3">
          <h3 className="text-xs font-bold uppercase tracking-wider text-ink-muted">Tác giả</h3>
          <p className="mt-1 font-semibold text-ink">{report.author.displayName}</p>
          <p className="break-all text-xs text-ink-muted">{report.author.email}</p>
          {report.author.accountStatus === 'LOCKED' && <p className="mt-1 text-xs font-semibold text-rose-700">Tài khoản đang bị khóa</p>}
        </section>
      </div>

      <section className="flex flex-wrap items-center gap-2">
        <StatusBadge status={report.status} />
        {report.handledBy && <span className="text-xs text-ink-muted">Người xử lý: {report.handledBy.displayName}</span>}
      </section>

      {report.status === 'RESOLVED' && (
        <section className="rounded-xl border border-leaf-200 bg-leaf-50/70 p-4 text-leaf-900">
          <h3 className="font-bold">Kết luận: {decisionLabel(report.decision)}</h3>
          <p className="mt-1 whitespace-pre-line">{report.decisionReason}</p>
          {report.handledAt && <p className="mt-1 text-xs">Ghi lúc {formatDateTime(report.handledAt)}</p>}
        </section>
      )}

      {report.status === 'IN_REVIEW' && !heldByMe && (
        <p className="flex items-start gap-2 rounded-xl border border-amber-200 bg-amber-50 p-3 text-amber-900">
          <ShieldAlert className="mt-0.5 h-4 w-4 shrink-0" aria-hidden="true" />
          {report.handledBy
            ? `${report.handledBy.displayName} đang xem xét báo cáo này. Chỉ người tiếp nhận mới ghi được kết luận.`
            : 'Báo cáo chưa có người xử lý. Hãy tiếp nhận để ghi kết luận.'}
        </p>
      )}

      {heldByMe ? (
        <ResolveForm report={report} onResolved={onResolved} onCancel={onClose} />
      ) : (report.status === 'OPEN' || (report.status === 'IN_REVIEW' && !report.handledBy)) && (
        <div className="flex justify-end border-t border-brand-100 pt-4">
          <Button size="sm" disabled={busy} onClick={onClaim}>Tiếp nhận xem xét</Button>
        </div>
      )}
    </div>
  );
}

function ResolveForm({
  report,
  onResolved,
  onCancel,
}: {
  report: AdminReport;
  onResolved: (report: AdminReport) => void;
  onCancel: () => void;
}) {
  const [decision, setDecision] = useState<ReportDecision | ''>('');
  const [reason, setReason] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const trimmed = reason.trim();
  const decisionError = submitted && !decision ? 'Chọn một kết luận xử lý.' : '';
  const reasonError = submitted && (trimmed.length < REASON_MIN || trimmed.length > REASON_MAX)
    ? `Nhập lý do kết luận từ ${REASON_MIN} đến ${REASON_MAX} ký tự.`
    : '';

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSubmitted(true);
    setError('');
    if (!decision || trimmed.length < REASON_MIN || trimmed.length > REASON_MAX) return;
    setBusy(true);
    try {
      onResolved(await adminReportsApi.resolve(report.id, decision, trimmed));
    } catch (cause) {
      const api = asApiError(cause);
      setError(api.errors[0]?.message ?? api.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-4 border-t border-brand-100 pt-4">
      <fieldset aria-describedby={decisionError ? 'report-decision-error' : undefined}>
        <legend className="font-bold text-ink">Kết luận xử lý</legend>
        <div className="mt-2 grid gap-2 sm:grid-cols-2">
          {decisions.map((item) => (
            <label
              key={item.value}
              className={`flex cursor-pointer gap-3 rounded-xl border p-3 transition-colors ${
                decision === item.value ? 'border-brand-500 bg-brand-50/80 ring-1 ring-brand-500' : 'border-brand-100 hover:bg-brand-50/40'
              }`}
            >
              <input
                type="radio"
                name="report-decision"
                value={item.value}
                checked={decision === item.value}
                onChange={() => setDecision(item.value)}
                className="mt-0.5 h-4 w-4 accent-brand-600"
              />
              <span>
                <span className="block font-bold text-ink">{item.label}</span>
                <span className="mt-0.5 block text-xs text-ink-muted">{item.hint}</span>
              </span>
            </label>
          ))}
        </div>
        {decisionError && <p id="report-decision-error" className="mt-1 text-xs font-semibold text-rose-700">{decisionError}</p>}
      </fieldset>

      <div>
        <label htmlFor="report-decision-reason" className="font-bold text-ink">Lý do kết luận</label>
        <p className="text-xs text-ink-muted">Tác giả sẽ đọc lý do này khi bài bị cảnh cáo, bị ẩn hoặc tài khoản bị khóa.</p>
        <textarea
          id="report-decision-reason"
          rows={4}
          maxLength={REASON_MAX}
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          aria-invalid={reasonError ? true : undefined}
          aria-describedby={reasonError ? 'report-reason-error' : undefined}
          className="mt-1 w-full rounded-xl border border-brand-200 bg-paper p-3 text-sm text-ink focus:border-brand-500 focus:outline-none"
        />
        <div className="flex justify-between gap-2 text-xs">
          <span id="report-reason-error" className="font-semibold text-rose-700">{reasonError}</span>
          <span className="tabular-nums text-ink-muted">{trimmed.length}/{REASON_MAX}</span>
        </div>
      </div>

      {decision === 'LOCK_AUTHOR' && (
        <p className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-rose-800">
          Khóa tài khoản sẽ đăng xuất {report.author.displayName} khỏi mọi thiết bị. Chỉ chọn khi vi phạm nghiêm trọng hoặc lặp lại nhiều lần.
        </p>
      )}
      {error && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 font-medium text-rose-800">{error}</p>}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" size="sm" onClick={onCancel}>Hủy</Button>
        <Button type="submit" size="sm" disabled={busy}>{busy ? 'Đang lưu…' : 'Hoàn tất xử lý'}</Button>
      </div>
    </form>
  );
}
