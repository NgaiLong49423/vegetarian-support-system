import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Link, useLocation } from 'react-router-dom';
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
  const [filter, setFilter] = useState('PENDING');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [experience, setExperience] = useState('');
  const [vegetarianType, setVegetarianType] = useState('VEGAN');
  const [summary, setSummary] = useState('');
  const [portfolioUrl, setPortfolioUrl] = useState('');
  const [reason, setReason] = useState('');
  const [approvalNote, setApprovalNote] = useState('');
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
      setSelected(null); setReason(''); setNotice(action === 'approve' ? 'Đã phê duyệt đơn.' : 'Đã từ chối đơn.'); await load();
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
    <Link className="mt-5 inline-block rounded-xl bg-brand-600 px-5 py-2.5 font-bold text-white" to="/dang-nhap">Đăng nhập</Link>
  </Card></PageContainer>;

  if (adminView && account.role !== 'ADMIN') return <PageContainer className="py-10"><Card className="p-6">Bạn không có quyền xem hàng đợi xét duyệt.</Card></PageContainer>;
  if (!adminView && account.role !== 'CUSTOMER' && account.role !== 'EXPERT') return <PageContainer className="py-10"><Card className="p-6">Tài khoản này không thể nộp đơn đăng ký Chuyên gia.</Card></PageContainer>;

  return <PageContainer className="max-w-5xl py-8">
    <header className="mb-6"><p className="text-sm font-bold text-leaf-700">CỘNG ĐỒNG MÂM XANH</p>
      <h1 className="mt-1 text-3xl font-extrabold text-ink">{adminView ? 'Xét duyệt Chuyên gia' : 'Đăng ký Chuyên gia'}</h1>
      <p className="mt-2 max-w-3xl text-sm text-ink-muted">{adminView ? 'Thẩm định hồ sơ của thành viên, mỗi quyết định được ghi nhận một lần.' : 'Chia sẻ kinh nghiệm và công thức chay, kết nối cùng cộng đồng yêu ẩm thực thuần lành.'}</p>
    </header>
    {error && <div role="alert" className="mb-4 rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">{error} <button className="ml-2 font-bold underline" onClick={() => void load()}>Tải lại</button></div>}
    {notice && <div role="status" className="mb-4 rounded-xl bg-leaf-50 p-3 text-sm text-leaf-800">{notice}</div>}

    {adminView ? <section>
      <div className="mb-4 flex flex-wrap gap-2" aria-label="Lọc trạng thái">
        {statuses.map((status) => <button key={status || 'ALL'} onClick={() => { setPage(0); setFilter(status); }} className={`rounded-lg border px-3 py-2 text-sm ${filter === status ? 'border-brand-700 bg-brand-700 text-white' : 'border-brand-200 bg-white'}`}>{status || 'Tất cả'}</button>)}
        <Button variant="outline" onClick={() => void load()}>Làm mới</Button>
      </div>
      <div className="grid gap-3">{loading ? <Card className="p-8 text-center text-ink-muted">Đang tải danh sách hồ sơ…</Card> : applications.map((app) => <Card key={app.id} className="flex flex-col justify-between gap-4 p-4 sm:flex-row sm:items-center">
        <div><p className="font-bold text-ink">{app.displayName} <span className="font-normal text-ink-muted">· {app.email}</span></p><p className="mt-1 text-sm text-ink-muted">#{app.id} · {app.status} · {new Date(app.submittedAt).toLocaleString('vi-VN')}</p><p className="mt-2 line-clamp-2 text-sm">{app.experience}</p></div>
        <Button variant="outline" onClick={async () => { setBusy(true); try { setSelected(await expertApplicationsApi.detail(app.id)); setReason(''); } catch (e) { setError(errorText(e)); } finally { setBusy(false); } }}>Xem chi tiết</Button>
      </Card>)}{!loading && applications.length === 0 && <Card className="p-8 text-center text-ink-muted">Không có đơn phù hợp.</Card>}</div>
      {!loading && totalPages > 1 && <div className="mt-4 flex items-center justify-center gap-3"><Button variant="outline" disabled={page === 0} onClick={() => setPage((value) => Math.max(0, value - 1))}>Trang trước</Button><span className="text-sm">Trang {page + 1}/{totalPages}</span><Button variant="outline" disabled={page + 1 >= totalPages} onClick={() => setPage((value) => value + 1)}>Trang sau</Button></div>}
      {selected && <div role="dialog" aria-modal="true" aria-labelledby="application-title" className="fixed inset-0 z-50 overflow-y-auto bg-black/40 p-4" onClick={() => setSelected(null)}>
        <div className="mx-auto my-8 max-w-2xl" onClick={(event) => event.stopPropagation()}><Card className="space-y-4 p-5 sm:p-7">
          <h2 id="application-title" className="text-xl font-bold">Hồ sơ #{selected.id} · {selected.displayName}</h2><p className="text-sm text-ink-muted">{selected.email} · {selected.status}</p>
          <div><h3 className="font-semibold">Kinh nghiệm</h3><p className="whitespace-pre-wrap text-sm">{selected.experience}</p></div>
          <div><h3 className="font-semibold">Trường phái</h3><p>{types.find(([key]) => key === selected.vegetarianType)?.[1] ?? selected.vegetarianType}</p></div>
          <div><h3 className="font-semibold">Tóm tắt công thức</h3><p className="whitespace-pre-wrap text-sm">{selected.sampleRecipeSummary}</p></div>
          {selected.portfolioUrl && <p><a className="text-brand-700 underline" href={selected.portfolioUrl} target="_blank" rel="noreferrer">Portfolio tham khảo</a></p>}
          {selected.adminNote && <p className="rounded-lg bg-brand-50 p-3 text-sm"><strong>Ghi chú xử lý:</strong> {selected.adminNote}</p>}
          {selected.status === 'PENDING' && <><label className="block text-sm font-semibold" htmlFor="approval-note">Lời nhắn khi phê duyệt (không bắt buộc)</label><textarea id="approval-note" maxLength={1000} value={approvalNote} onChange={(e) => setApprovalNote(e.target.value)} className="w-full rounded-xl border border-brand-200 p-3" rows={2} />
            <label className="block text-sm font-semibold" htmlFor="reject-reason">Lý do từ chối (10–500 ký tự)</label><textarea id="reject-reason" minLength={10} maxLength={500} value={reason} onChange={(e) => setReason(e.target.value)} className="w-full rounded-xl border border-brand-200 p-3" rows={3} />
            <div className="flex flex-wrap justify-end gap-2"><Button variant="outline" onClick={() => setSelected(null)}>Đóng</Button><Button variant="outline" disabled={busy || reason.trim().length < 10} onClick={() => void review('reject')}>Từ chối</Button><Button disabled={busy} onClick={() => void review('approve')}>Phê duyệt</Button></div></>}
          {selected.status !== 'PENDING' && <Button variant="outline" onClick={() => setSelected(null)}>Đóng</Button>}
        </Card></div>
      </div>}
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
