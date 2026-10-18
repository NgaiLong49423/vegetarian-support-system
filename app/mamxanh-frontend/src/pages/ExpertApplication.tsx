import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useLocation } from 'react-router-dom';
import {
  AlertCircle,
  Award,
  CheckCircle2,
  Clock,
  Database,
  ExternalLink,
  FileText,
  Filter,
  Leaf,
  RefreshCw,
  ShieldCheck,
  UserCheck,
  XCircle,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, Card } from '../components/ui';
import { useAuth } from '../components/AuthContext';
import { asApiError } from '../lib/apiClient';
import { expertApplicationsApi, type ApplicationStatus, type ExpertApplication } from '../api/expertApplications';

const types = [
  ['VEGAN', 'Thuần chay'], ['LACTO', 'Lacto'], ['OVO', 'Ovo'], ['LACTO_OVO', 'Lacto-Ovo'], ['MACROBIOTIC', 'Thực dưỡng'],
];
const statuses: Array<'' | ApplicationStatus> = ['', 'PENDING', 'APPROVED', 'REJECTED'];
const errorText = (error: unknown) => {
  const api = asApiError(error);
  if (api.status === 409) return api.message || 'Đơn đã thay đổi. Tải lại dữ liệu trước khi tiếp tục.';
  return api.message;
};

export function ExpertApplicationPage() {
  const { account, updateAccountRole } = useAuth();
  const location = useLocation();
  const adminView = location.pathname.startsWith('/admin/');
  const [history, setHistory] = useState<ExpertApplication[]>([]);
  const [applications, setApplications] = useState<ExpertApplication[]>([]);
  const [selected, setSelected] = useState<ExpertApplication | null>(null);
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [filter, setFilter] = useState('PENDING');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [experience, setExperience] = useState('');
  const [vegetarianType, setVegetarianType] = useState('VEGAN');
  const [summary, setSummary] = useState('');
  const [portfolioUrl, setPortfolioUrl] = useState('');
  const [reason, setReason] = useState('');
  const [approvalNote, setApprovalNote] = useState('');
  const [reviewDecision, setReviewDecision] = useState<'approve' | 'reject'>('approve');
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [commitment, setCommitment] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      if (adminView) {
        const result = await expertApplicationsApi.list(filter, page);
        setApplications(result.content); setTotalPages(result.totalPages);
      }
      else {
        const result = await expertApplicationsApi.history(page);
        const own = result.content;
        setHistory(own); setTotalPages(result.totalPages);
        if (own.some((application) => application.status === 'APPROVED')) updateAccountRole('EXPERT');
      }
    } catch (cause) { setError(errorText(cause)); }
    finally { setLoading(false); }
  }, [adminView, filter, page, updateAccountRole]);

  useEffect(() => { if (account) void load(); }, [account, load]);
  useEffect(() => { if (selected && !dialogRef.current?.open) dialogRef.current?.showModal(); }, [selected]);

  async function submit(event: FormEvent) {
    event.preventDefault(); setBusy(true); setError(''); setNotice('');
    try {
      await expertApplicationsApi.submit({ experience: experience.trim(), vegetarianType, sampleRecipeSummary: summary.trim(), portfolioUrl: portfolioUrl.trim() || undefined });
      setNotice('Đơn đã được gửi. Bạn có thể theo dõi trạng thái tại đây.'); setExperience(''); setSummary(''); setPortfolioUrl(''); await load();
    } catch (cause) { setError(errorText(cause)); }
    finally { setBusy(false); }
  }

  async function review(action: 'approve' | 'reject') {
    if (!selected) return;
    setBusy(true); setError('');
    try {
      const updated = action === 'approve' ? await expertApplicationsApi.approve(selected.id, approvalNote.trim() || undefined) : await expertApplicationsApi.reject(selected.id, reason.trim());
      setApplications((items) => items.map((item) => item.id === updated.id ? updated : item));
      setSelected(null); setReason(''); setApprovalNote(''); setReviewDecision('approve'); setNotice(action === 'approve' ? 'Đã phê duyệt đơn.' : 'Đã từ chối đơn.'); await load();
    } catch (cause) {
      const message = errorText(cause);
      await load();
      if (asApiError(cause).status === 409 && selected) {
        try { setSelected(await expertApplicationsApi.detail(selected.id)); } catch { setSelected(null); }
      }
      setError(message);
    }
    finally { setBusy(false); }
  }

  if (!account) return <PageContainer className="py-10"><Card className="mx-auto max-w-xl p-8 text-center">
    <h1 className="text-2xl font-bold">Đăng nhập để tiếp tục</h1><p className="mt-2 text-ink-muted">Đơn đăng ký Chuyên gia gắn với tài khoản của bạn.</p>
    <Link className="mt-5 inline-block rounded-xl bg-brand-600 px-5 py-2.5 font-bold text-on-brand" to="/dang-nhap">Đăng nhập</Link>
  </Card></PageContainer>;

  if (adminView && account.role !== 'ADMIN') return <PageContainer className="py-10"><Card className="p-6">Bạn không có quyền xem hàng đợi xét duyệt.</Card></PageContainer>;
  if (!adminView && account.role !== 'CUSTOMER' && account.role !== 'EXPERT') return <PageContainer className="py-10"><Card className="p-6">Tài khoản này không thể nộp đơn đăng ký Chuyên gia.</Card></PageContainer>;

  return <PageContainer className="max-w-5xl py-8">
    {adminView ? (
      <header className="relative mb-7 overflow-hidden rounded-3xl border border-brand-200/80 bg-gradient-to-br from-paper via-paper to-brand-50/40 p-6 shadow-sm sm:p-8">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -right-16 -top-16 h-56 w-56 rounded-full bg-brand-500/10 blur-3xl"
        />
        <div className="relative flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div className="max-w-2xl">
            <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-leaf-200/80 bg-leaf-50/90 px-3 py-1 text-xs font-bold uppercase tracking-[0.12em] text-leaf-700">
              <ShieldCheck className="h-3.5 w-3.5" /> Khu vực Administrator
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight text-ink sm:text-4xl">Xét duyệt Chuyên gia</h1>
            <p className="mt-2.5 text-sm leading-6 text-ink-muted">
              Thẩm định hồ sơ của thành viên, mỗi quyết định được ghi nhận một lần.
            </p>
          </div>
          <div className="inline-flex items-center self-start rounded-full border border-brand-200/80 bg-cream-soft/80 p-1">
            <Link
              to="/quan-tri/danh-muc"
              className="inline-flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-semibold text-ink-soft transition-colors hover:text-brand-600"
            >
              <Database className="h-3.5 w-3.5" /> Danh mục nguyên liệu
            </Link>
            <span className="inline-flex items-center gap-1.5 rounded-full bg-brand-600 px-3.5 py-1.5 text-xs font-bold text-on-brand shadow-xs">
              <Award className="h-3.5 w-3.5" /> Xét duyệt Chuyên gia
            </span>
          </div>
        </div>

        <div className="relative mt-6 grid grid-cols-1 gap-3 border-t border-brand-100/80 pt-5 sm:grid-cols-3">
          <div className="flex items-center gap-3 rounded-2xl border border-brand-100/80 bg-cream-soft/60 px-4 py-3">
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-100 text-brand-600">
              <FileText className="h-5 w-5" />
            </span>
            <div className="min-w-0">
              <p className="text-[11px] font-semibold uppercase tracking-wider text-ink-muted">Hồ sơ trên trang</p>
              <p className="truncate text-lg font-extrabold text-ink">{applications.length}</p>
            </div>
          </div>
          <div className="flex items-center gap-3 rounded-2xl border border-brand-100/80 bg-cream-soft/60 px-4 py-3">
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-leaf-100 text-leaf-700">
              <Filter className="h-5 w-5" />
            </span>
            <div className="min-w-0">
              <p className="text-[11px] font-semibold uppercase tracking-wider text-ink-muted">Bộ lọc hiện tại</p>
              <p className="truncate text-lg font-extrabold text-ink">
                {filter === 'PENDING' ? 'Chờ duyệt' : filter === 'APPROVED' ? 'Đã duyệt' : filter === 'REJECTED' ? 'Đã từ chối' : 'Tất cả'}
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3 rounded-2xl border border-brand-100/80 bg-cream-soft/60 px-4 py-3">
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-raised text-ink-soft">
              <UserCheck className="h-5 w-5" />
            </span>
            <div className="min-w-0">
              <p className="text-[11px] font-semibold uppercase tracking-wider text-ink-muted">Trang hiển thị</p>
              <p className="truncate text-lg font-extrabold text-ink">{page + 1}/{Math.max(1, totalPages)}</p>
            </div>
          </div>
        </div>
      </header>
    ) : (
      <header className="mb-6">
        <p className="text-sm font-bold text-leaf-700">CỘNG ĐỒNG MÂM XANH</p>
        <h1 className="mt-1 text-3xl font-extrabold text-ink">Đăng ký Chuyên gia</h1>
        <p className="mt-2 max-w-3xl text-sm text-ink-muted">
          Chia sẻ kinh nghiệm và công thức chay, kết nối cùng cộng đồng yêu ẩm thực thuần lành.
        </p>
      </header>
    )}

    {error && (
      <div role="alert" className="mb-5 flex items-center gap-3 rounded-2xl border border-rose-200 bg-rose-50/90 p-4 text-sm text-rose-800 shadow-2xs">
        <AlertCircle className="h-5 w-5 shrink-0 text-rose-600" />
        <div className="flex-1 font-medium">
          {error} <button className="ml-2 font-bold underline" onClick={() => void load()}>Tải lại</button>
        </div>
      </div>
    )}
    {notice && (
      <div role="status" className="mb-5 flex items-center gap-3 rounded-2xl border border-leaf-200 bg-leaf-50/90 p-4 text-sm text-leaf-800 shadow-2xs">
        <CheckCircle2 className="h-5 w-5 shrink-0 text-leaf-600" />
        <div className="flex-1 font-medium">{notice}</div>
      </div>
    )}

    {adminView ? <section>
      <Card className="mb-5 flex flex-wrap items-center justify-between gap-3 border-brand-200/70 p-3.5 shadow-2xs sm:px-5">
        <div className="flex flex-wrap items-center gap-2" aria-label="Lọc trạng thái">
          {statuses.map((status) => (
            <button
              key={status || 'ALL'}
              onClick={() => { setPage(0); setFilter(status); }}
              className={`rounded-xl border px-3.5 py-2 text-xs font-bold transition-all duration-200 sm:text-sm ${
                filter === status
                  ? 'border-brand-600 bg-brand-600 text-on-brand shadow-sm shadow-brand-600/20'
                  : 'border-brand-200 bg-paper text-ink-soft hover:border-brand-300 hover:text-brand-600'
              }`}
            >
              {status || 'Tất cả'}
            </button>
          ))}
        </div>
        <Button variant="outline" size="sm" onClick={() => void load()}>
          <RefreshCw className="h-3.5 w-3.5" /> Làm mới
        </Button>
      </Card>

      <div className="grid gap-3.5">
        {loading ? (
          <Card className="p-10 text-center text-sm text-ink-muted">Đang tải danh sách hồ sơ…</Card>
        ) : (
          applications.map((app) => {
            const schoolLabel = types.find(([key]) => key === app.vegetarianType)?.[1] ?? app.vegetarianType;
            const statusBorder =
              app.status === 'APPROVED'
                ? 'border-l-leaf-500'
                : app.status === 'REJECTED'
                  ? 'border-l-rose-500'
                  : 'border-l-brand-500';
            return (
              <Card
                key={app.id}
                className={`group flex flex-col justify-between gap-4 border-l-4 ${statusBorder} p-5 shadow-2xs transition-all duration-200 motion-safe:hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-md sm:flex-row sm:items-center`}
              >
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2.5">
                    <span
                      aria-hidden="true"
                      className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-100 text-xs font-extrabold text-brand-700"
                    >
                      {app.displayName.trim().charAt(0).toUpperCase()}
                    </span>
                    <div>
                      <p className="font-bold text-ink">
                        {app.displayName} <span className="font-normal text-ink-muted">· {app.email}</span>
                      </p>
                      <p className="mt-0.5 flex flex-wrap items-center gap-1.5 text-xs text-ink-muted">
                        <Clock className="h-3.5 w-3.5 text-brand-600" />
                        <span>#{app.id} · {app.status} · {new Date(app.submittedAt).toLocaleString('vi-VN')}</span>
                      </p>
                    </div>
                    <span className="ml-auto inline-flex items-center gap-1 rounded-full border border-leaf-200/70 bg-leaf-50 px-2.5 py-0.5 text-xs font-semibold text-leaf-700 sm:ml-2">
                      <Leaf className="h-3 w-3" /> {schoolLabel}
                    </span>
                  </div>
                  <p className="mt-3 line-clamp-2 rounded-xl border border-brand-100/70 bg-cream-soft/60 px-3.5 py-2.5 text-sm text-ink-soft">
                    {app.experience}
                  </p>
                </div>
                <Button
                  variant="outline"
                  className="shrink-0"
                  onClick={async () => {
                    setBusy(true);
                    try {
                      setSelected(await expertApplicationsApi.detail(app.id));
                      setReason('');
                      setApprovalNote('');
                      setReviewDecision('approve');
                    } catch (e) {
                      setError(errorText(e));
                    } finally {
                      setBusy(false);
                    }
                  }}
                >
                  Xem chi tiết
                </Button>
              </Card>
            );
          })
        )}
        {!loading && applications.length === 0 && (
          <Card className="border-dashed border-brand-200 p-10 text-center text-sm text-ink-muted">
            Không có đơn phù hợp.
          </Card>
        )}
      </div>

      {!loading && totalPages > 1 && (
        <div className="mt-5 flex items-center justify-center gap-3 rounded-2xl border border-brand-100 bg-paper px-4 py-3">
          <Button variant="outline" size="sm" disabled={page === 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>Trang trước</Button>
          <span className="text-sm font-medium text-ink-muted">Trang {page + 1}/{totalPages}</span>
          <Button variant="outline" size="sm" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>Trang sau</Button>
        </div>
      )}

      {selected && (
        <dialog
          ref={dialogRef}
          aria-labelledby="application-title"
          className="fixed inset-0 m-0 h-dvh max-h-none w-screen max-w-none overflow-y-auto border-0 bg-transparent p-4 backdrop:bg-black/65 backdrop:backdrop-blur-xs"
          onClose={() => setSelected(null)}
          onClick={(event) => { if (event.target === event.currentTarget) event.currentTarget.close(); }}
        >
          <div className="mx-auto my-8 max-w-2xl">
            <Card className="modal-panel-enter space-y-5 border-brand-200 bg-paper p-6 shadow-2xl sm:p-8">
              <div className="flex items-start justify-between gap-4 border-b border-brand-100 pb-4">
                <div className="flex items-center gap-3">
                  <span
                    aria-hidden="true"
                    className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-brand-100 text-base font-extrabold text-brand-700"
                  >
                    {selected.displayName.trim().charAt(0).toUpperCase()}
                  </span>
                  <div>
                    <h2 id="application-title" className="text-xl font-extrabold text-ink">
                      Hồ sơ #{selected.id} · {selected.displayName}
                    </h2>
                    <p className="mt-0.5 text-sm text-ink-muted">{selected.email} · {selected.status}</p>
                  </div>
                </div>
              </div>

              <div className="grid gap-3.5 sm:grid-cols-2">
                <div className="rounded-2xl border border-brand-100/80 bg-cream-soft/70 p-4 sm:col-span-2">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-brand-600">Kinh nghiệm</h3>
                  <p className="mt-1.5 whitespace-pre-wrap text-sm leading-6 text-ink">{selected.experience}</p>
                </div>
                <div className="rounded-2xl border border-brand-100/80 bg-cream-soft/70 p-4">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-brand-600">Trường phái</h3>
                  <p className="mt-1.5 font-semibold text-ink">
                    {types.find(([key]) => key === selected.vegetarianType)?.[1] ?? selected.vegetarianType}
                  </p>
                </div>
                {selected.portfolioUrl && (
                  <div className="rounded-2xl border border-brand-100/80 bg-cream-soft/70 p-4">
                    <h3 className="text-xs font-bold uppercase tracking-wider text-brand-600">Liên kết hồ sơ</h3>
                    <p className="mt-1.5">
                      <a className="inline-flex items-center gap-1 text-sm font-semibold text-brand-600 underline hover:text-brand-500" href={selected.portfolioUrl} target="_blank" rel="noreferrer">
                        Portfolio tham khảo <ExternalLink className="h-3.5 w-3.5" />
                      </a>
                    </p>
                  </div>
                )}
                <div className="rounded-2xl border border-brand-100/80 bg-cream-soft/70 p-4 sm:col-span-2">
                  <h3 className="text-xs font-bold uppercase tracking-wider text-brand-600">Tóm tắt công thức</h3>
                  <p className="mt-1.5 whitespace-pre-wrap text-sm leading-6 text-ink">{selected.sampleRecipeSummary}</p>
                </div>
              </div>

              {selected.adminNote && (
                <p className="rounded-xl border border-brand-200/70 bg-brand-50/70 p-3.5 text-sm text-ink">
                  <strong>Ghi chú xử lý:</strong> {selected.adminNote}
                </p>
              )}

              {selected.status === 'PENDING' && (
                <div className="space-y-4 border-t border-brand-100 pt-4">
                  <div>
                    <p className="text-xs font-bold uppercase tracking-wider text-ink-muted mb-2">
                      Quyết định thẩm định hồ sơ
                    </p>
                    <div className="grid grid-cols-2 gap-3">
                      <button
                        type="button"
                        onClick={() => setReviewDecision('approve')}
                        className={`flex items-center justify-center gap-2 rounded-xl border p-3 text-sm font-semibold transition-all ${
                          reviewDecision === 'approve'
                            ? 'border-leaf-600 bg-leaf-50 text-leaf-800 ring-2 ring-leaf-500/20 font-bold shadow-xs'
                            : 'border-brand-200 bg-paper text-ink-muted hover:border-brand-300 hover:text-ink'
                        }`}
                      >
                        <CheckCircle2 className={`h-4 w-4 ${reviewDecision === 'approve' ? 'text-leaf-600' : 'text-ink-muted'}`} />
                        Đồng ý phê duyệt
                      </button>
                      <button
                        type="button"
                        onClick={() => setReviewDecision('reject')}
                        className={`flex items-center justify-center gap-2 rounded-xl border p-3 text-sm font-semibold transition-all ${
                          reviewDecision === 'reject'
                            ? 'border-rose-600 bg-rose-50 text-rose-800 ring-2 ring-rose-500/20 font-bold shadow-xs'
                            : 'border-brand-200 bg-paper text-ink-muted hover:border-brand-300 hover:text-ink'
                        }`}
                      >
                        <XCircle className={`h-4 w-4 ${reviewDecision === 'reject' ? 'text-rose-600' : 'text-ink-muted'}`} />
                        Từ chối hồ sơ
                      </button>
                    </div>
                  </div>

                  {reviewDecision === 'approve' ? (
                    <div className="rounded-2xl border border-leaf-200/80 bg-leaf-50/50 p-4">
                      <label className="block text-sm font-semibold text-leaf-900" htmlFor="approval-note">
                        Lời nhắn khi phê duyệt (không bắt buộc)
                      </label>
                      <p className="mt-0.5 text-xs text-leaf-700/80">
                        Lời nhắn chúc mừng hoặc ghi chú chào mừng chuyên gia mới gia nhập.
                      </p>
                      <textarea
                        id="approval-note"
                        maxLength={1000}
                        value={approvalNote}
                        onChange={(e) => setApprovalNote(e.target.value)}
                        placeholder="Nhập lời nhắn gửi đến thành viên (tùy chọn)..."
                        className="mt-2 w-full rounded-xl border border-leaf-200 bg-paper p-3 text-sm text-ink outline-none transition-all focus:border-leaf-500 focus:ring-2 focus:ring-leaf-500/20"
                        rows={2}
                      />
                    </div>
                  ) : (
                    <div className="rounded-2xl border border-rose-200/80 bg-rose-50/50 p-4">
                      <div className="flex items-center justify-between">
                        <label className="block text-sm font-semibold text-rose-900" htmlFor="reject-reason">
                          Lý do từ chối (10–500 ký tự) <span className="text-rose-600">*</span>
                        </label>
                        <span className={`text-xs font-semibold ${reason.trim().length >= 10 ? 'text-leaf-700' : 'text-rose-600'}`}>
                          {reason.trim().length}/500 ký tự (tối thiểu 10)
                        </span>
                      </div>
                      <p className="mt-0.5 text-xs text-rose-700/80">
                        Nêu rõ lý do để ứng viên biết điểm cần hoàn thiện trước khi nộp lại.
                      </p>
                      <textarea
                        id="reject-reason"
                        minLength={10}
                        maxLength={500}
                        value={reason}
                        onChange={(e) => setReason(e.target.value)}
                        placeholder="Nêu cụ thể lý do từ chối (tối thiểu 10 ký tự)..."
                        className="mt-2 w-full rounded-xl border border-rose-200 bg-paper p-3 text-sm text-ink outline-none transition-all focus:border-rose-500 focus:ring-2 focus:ring-rose-500/20"
                        rows={3}
                      />
                    </div>
                  )}

                  <div className="flex flex-wrap items-center justify-between gap-2.5 pt-2">
                    <Button variant="outline" onClick={() => setSelected(null)}>Đóng</Button>
                    <div className="flex items-center gap-2">
                      {reviewDecision === 'approve' ? (
                        <Button
                          disabled={busy}
                          onClick={() => void review('approve')}
                          className="font-bold"
                        >
                          {busy ? 'Đang xử lý...' : 'Phê duyệt'}
                        </Button>
                      ) : (
                        <Button
                          variant="outline"
                          disabled={busy || reason.trim().length < 10}
                          onClick={() => void review('reject')}
                          className="border-rose-300 bg-rose-600 text-white hover:bg-rose-700 font-bold disabled:opacity-50"
                        >
                          {busy ? 'Đang xử lý...' : 'Từ chối'}
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              )}
              {selected.status !== 'PENDING' && (
                <div className="flex justify-end border-t border-brand-100 pt-4">
                  <Button variant="outline" onClick={() => setSelected(null)}>Đóng</Button>
                </div>
              )}
            </Card>
          </div>
        </dialog>
      )}
    </section> : <section className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
      <Card className="p-5 sm:p-7"><h2 className="text-xl font-bold">Hồ sơ của bạn</h2>
        {account.role === 'EXPERT' && <p className="mt-3 rounded-xl bg-leaf-50 p-3 text-sm text-leaf-800">Tài khoản đã được phê duyệt Chuyên gia.</p>}
        <div className="mt-4 space-y-3">{loading ? <p className="text-sm text-ink-muted">Đang tải lịch sử…</p> : history.map((app) => <article key={app.id} className="rounded-xl border border-brand-100 p-3">
          <p className="font-semibold">#{app.id} · {app.status}</p><p className="text-xs text-ink-muted">Gửi lúc {new Date(app.submittedAt).toLocaleString('vi-VN')}</p>
          {app.status === 'REJECTED' && app.adminNote && <p className="mt-2 rounded-lg bg-rose-50 p-2 text-sm text-rose-800"><strong>Lý do:</strong> {app.adminNote}</p>}
        </article>)}{!loading && history.length === 0 && <p className="text-sm text-ink-muted">Bạn chưa có hồ sơ nào.</p>}</div>
        {!loading && totalPages > 1 && <div className="mt-4 flex items-center justify-center gap-3"><Button variant="outline" disabled={page === 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>Trang trước</Button><span className="text-sm">Trang {page + 1}/{totalPages}</span><Button variant="outline" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>Trang sau</Button></div>}
      </Card>
      {account.role === 'CUSTOMER' && (!history.some((app) => app.status === 'PENDING') || history[0]?.status === 'REJECTED') && <Card className="p-5 sm:p-7"><h2 className="text-xl font-bold">{history.length ? 'Nộp lại hồ sơ' : 'Gửi hồ sơ'}</h2>
        <form className="mt-4 space-y-4" onSubmit={submit}>
          <label className="block text-sm font-semibold">Kinh nghiệm ẩm thực chay (20–2.000 ký tự)<textarea required minLength={20} maxLength={2000} value={experience} onChange={(e) => setExperience(e.target.value)} rows={4} className="mt-1 w-full rounded-xl border border-brand-200 p-3" /></label>
          <label className="block text-sm font-semibold">Trường phái<select value={vegetarianType} onChange={(e) => setVegetarianType(e.target.value)} className="mt-1 w-full rounded-xl border border-brand-200 p-3">{types.map(([key, label]) => <option key={key} value={key}>{label}</option>)}</select></label>
          <label className="block text-sm font-semibold">Tóm tắt công thức sở trường (30–2.000 ký tự)<textarea required minLength={30} maxLength={2000} value={summary} onChange={(e) => setSummary(e.target.value)} rows={4} className="mt-1 w-full rounded-xl border border-brand-200 p-3" /></label>
          <label className="block text-sm font-semibold">Portfolio URL (không bắt buộc, HTTP/HTTPS, tối đa 500 ký tự)<input type="url" maxLength={500} value={portfolioUrl} onChange={(e) => setPortfolioUrl(e.target.value)} className="mt-1 w-full rounded-xl border border-brand-200 p-3" /></label>
          <label className="flex items-start gap-2 text-sm"><input type="checkbox" required checked={commitment} onChange={(e) => setCommitment(e.target.checked)} /><span>Tôi cam kết thông tin và nội dung công thức gửi lên là trung thực.</span></label>
          <Button type="submit" disabled={busy || !commitment || experience.trim().length < 20 || summary.trim().length < 30}>{busy ? 'Đang gửi…' : 'Gửi đơn đăng ký'}</Button>
        </form>
      </Card>}
      {history.some((app) => app.status === 'PENDING') && <Card className="p-5 sm:p-7"><h2 className="text-xl font-bold">Đơn đang chờ xét duyệt</h2><p className="mt-2 text-sm text-ink-muted">Bạn đã gửi một đơn PENDING. Có thể theo dõi lịch sử ở đây.</p></Card>}
    </section>}
  </PageContainer>;
}
