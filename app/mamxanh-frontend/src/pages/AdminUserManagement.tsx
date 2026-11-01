import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, ChevronLeft, ChevronRight, Coins, Eye, Gift, Lock, Search, ShieldAlert, Unlock, UserCheck, Users, X } from 'lucide-react';
import { ApiError } from '../lib/apiClient';
import { Badge, Button, Card } from '../components/ui';
import {
  AdminAiTier, AdminAiPackageState, AdminUser, AdminUserAudit, AdminUserRole, AdminUserStatus, AdminUserSummary,
  getAdminAiTier, getAdminUser, getAdminUserHistory, getAdminUserSummary, grantAdminAiCredit, giftAdminAiPackage, listAdminUsers,
  updateAdminUserRole, updateAdminUserStatus,
} from '../api/adminUsers';

const PAGE_SIZE = 20;
const roleLabels: Record<AdminUserRole, string> = { CUSTOMER: 'Thành viên', EXPERT: 'Chuyên gia', ADMIN: 'Quản trị viên' };

function date(value: string) {
  return new Intl.DateTimeFormat('vi-VN', { dateStyle: 'short', timeZone: 'Asia/Ho_Chi_Minh' }).format(new Date(`${value.replace(' ', 'T')}Z`));
}

function apiMessage(error: unknown) {
  if (error instanceof ApiError && (error.status === 401 || error.status === 403)) return 'Phiên đăng nhập không có quyền quản lý người dùng. Hãy đăng nhập bằng tài khoản Quản trị viên.';
  return error instanceof ApiError ? error.message : 'Không thể tải dữ liệu. Vui lòng thử lại.';
}

export function AdminUserManagement() {
  const [rows, setRows] = useState<AdminUser[]>([]);
  const [summary, setSummary] = useState<AdminUserSummary | null>(null);
  const [search, setSearch] = useState('');
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [revision, setRevision] = useState(0);
  const [selected, setSelected] = useState<AdminUser | null>(null);
  const [detail, setDetail] = useState<AdminUser | null>(null);
  const [history, setHistory] = useState<AdminUserAudit[]>([]);
  const [historyPage, setHistoryPage] = useState(0);
  const [historyTotalPages, setHistoryTotalPages] = useState(0);
  const [historyError, setHistoryError] = useState<string | null>(null);
  const [mode, setMode] = useState<'VIEW' | 'STATUS' | 'ROLE' | 'AI_CHOICE' | 'AI_PACKAGE' | 'AI_CREDIT' | null>(null);
  const [reason, setReason] = useState('');
  const [packageReason, setPackageReason] = useState('');
  const [creditReason, setCreditReason] = useState('');
  const [nextRole, setNextRole] = useState<Exclude<AdminUserRole, 'ADMIN'>>('EXPERT');
  const [saving, setSaving] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [aiTier, setAiTier] = useState<AdminAiPackageState | null>(null);
  const [nextAiTier, setNextAiTier] = useState<AdminAiTier>('FREE');
  const [creditAmount, setCreditAmount] = useState('');
  const [aiTierLoading, setAiTierLoading] = useState(false);
  const [aiError, setAiError] = useState<string | null>(null);
  const [aiNotice, setAiNotice] = useState<string | null>(null);

  const load = useCallback(async (signal?: AbortSignal) => {
    setLoading(true);
    setError(null);
    try {
      const params = { query: search.trim() || undefined, role: role || undefined, status: status || undefined, page, size: PAGE_SIZE };
      const [result, counts] = await Promise.all([listAdminUsers(params), getAdminUserSummary()]);
      if (signal?.aborted) return;
      setRows(result.content);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
      setSummary(counts);
    } catch (cause) {
      if (!signal?.aborted) setError(apiMessage(cause));
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }, [search, role, status, page, revision]);

  useEffect(() => {
    const controller = new AbortController();
    const timer = window.setTimeout(() => void load(controller.signal), 250);
    return () => { window.clearTimeout(timer); controller.abort(); };
  }, [load]);

  const reload = () => setRevision((value) => value + 1);

  const openDetail = async (user: AdminUser, requestedHistoryPage = 0) => {
    setSelected(user);
    setDetail(null);
    setHistory([]);
    setHistoryPage(requestedHistoryPage);
    setHistoryError(null);
    setMode('VIEW');
    try {
      const [account, events, tier] = await Promise.all([getAdminUser(user.id), getAdminUserHistory(user.id, requestedHistoryPage), getAdminAiTier(user.id)]);
      setDetail(account);
      setHistory(events.content);
      setHistoryTotalPages(events.totalPages);
      setAiTier(tier);
    } catch (cause) { setHistoryError(apiMessage(cause)); }
  };

  const openAiManagement = async (user: AdminUser) => {
    setSelected(user); setMode('AI_CHOICE'); setPackageReason(''); setCreditReason(''); setCreditAmount(''); setAiError(null); setAiNotice(null);
  };

  const openPackageGift = async () => {
    if (!selected || selected.role === 'ADMIN') return;
    setMode('AI_PACKAGE'); setPackageReason(''); setAiError(null); setAiNotice(null);
    setAiTier(null); setAiTierLoading(true);
    try {
      const tier = await getAdminAiTier(selected.id);
      setAiTier(tier); setNextAiTier(tier.newTier);
    } catch (cause) { setAiError(apiMessage(cause)); }
    finally { setAiTierLoading(false); }
  };

  const openCreditGrant = () => {
    setMode('AI_CREDIT'); setCreditReason(''); setCreditAmount(''); setAiError(null); setAiNotice(null);
  };

  const saveAiTier = async () => {
    if (!selected || !aiTier || packageReason.trim().length < 10 || packageReason.trim().length > 500 || nextAiTier === aiTier.newTier) return;
    setSaving(true); setAiError(null);
    try {
      const updated = await giftAdminAiPackage(selected.id, nextAiTier, packageReason.trim());
      setAiTier(updated); setPackageReason(''); setAiNotice(`Đã tặng gói AI ${updated.newTier} cho ${selected.displayName}. Người nhận đã được thông báo.`);
      const events = await getAdminUserHistory(selected.id);
      setHistory(events.content); setHistoryPage(0); setHistoryTotalPages(events.totalPages);
      reload();
    } catch (cause) { setAiError(apiMessage(cause)); }
    finally { setSaving(false); }
  };

  const grantCredit = async () => {
    const amount = Number(creditAmount);
    if (!selected || !Number.isFinite(amount) || amount <= 0 || creditReason.trim().length < 10 || creditReason.trim().length > 500) return;
    setSaving(true); setAiError(null);
    try {
      const result = await grantAdminAiCredit(selected.id, amount, creditReason.trim());
      setCreditAmount(''); setCreditReason('');
      setAiNotice(`Đã cộng ${result.credits.toLocaleString('vi-VN')} AI Credit. Số dư mới: ${result.balance.toLocaleString('vi-VN')}. Giao dịch đã được lưu vào lịch sử tài khoản.`);
      const events = await getAdminUserHistory(selected.id);
      setHistory(events.content); setHistoryPage(0); setHistoryTotalPages(events.totalPages);
    } catch (cause) { setAiError(apiMessage(cause)); }
    finally { setSaving(false); }
  };

  const saveStatusOrRole = async () => {
    if (!selected || reason.trim().length < 10 || reason.trim().length > 500) return;
    setSaving(true);
    try {
      const updated = mode === 'STATUS'
        ? await updateAdminUserStatus(selected.id, selected.accountStatus === 'ACTIVE' ? 'LOCKED' : 'ACTIVE', reason.trim())
        : await updateAdminUserRole(selected.id, nextRole, reason.trim());
      setSelected(updated);
      setMode(null);
      setReason('');
      setNotice(`Đã cập nhật tài khoản ${updated.displayName}.`);
      reload();
      window.setTimeout(() => setNotice(null), 3500);
    } catch (cause) { setNotice(apiMessage(cause)); }
    finally { setSaving(false); }
  };

  return <div className="space-y-6 py-2">
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <div className="flex items-center gap-2"><Link to="/admin" className="inline-flex items-center gap-1 text-xs font-semibold text-ink-muted hover:text-brand-600"><ArrowLeft className="h-3.5 w-3.5"/>Dashboard</Link><span className="text-xs text-ink-muted">/</span><span className="text-xs font-semibold text-brand-700">Quản lý người dùng</span></div>
        <h1 className="mt-1 text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">Quản lý tài khoản &amp; phân quyền</h1>
        <p className="mt-1 text-xs text-ink-muted sm:text-sm">Tra cứu tài khoản và quản lý vai trò, trạng thái từ dữ liệu hệ thống.</p>
        <p className="mt-1 text-xs text-ink-muted sm:text-sm">Tặng gói AI cho Thành viên hoặc Chuyên gia bằng biểu tượng quà; AI Credit có thể cộng cho mọi tài khoản.</p>
      </div>
    </div>

    {notice && <div role="status" className="rounded-xl border border-leaf-200 bg-leaf-50 p-3 text-sm text-leaf-800">{notice}</div>}

    <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
      <SummaryCard label="Tổng tài khoản" value={summary?.totalUsers} icon={<Users className="h-4 w-4"/>}/>
      <SummaryCard label="Đang hoạt động" value={summary?.activeUsers} icon={<CheckCircle2 className="h-4 w-4"/>}/>
      <SummaryCard label="Chuyên gia" value={summary?.expertUsers} icon={<UserCheck className="h-4 w-4"/>}/>
      <SummaryCard label="Đang bị khóa" value={summary?.lockedUsers} icon={<Lock className="h-4 w-4"/>}/>
    </div>

    <Card className="p-4"><div className="flex flex-col gap-3 sm:flex-row sm:items-center">
      <div className="relative flex-1"><Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted"/><input aria-label="Tìm người dùng" value={search} onChange={(event) => { setSearch(event.target.value); setPage(0); }} placeholder="Tìm theo họ tên, email hoặc mã tài khoản..." className="w-full rounded-xl border border-brand-200 bg-paper py-2 pl-9 pr-3 text-xs text-ink focus:border-brand-500 focus:outline-none"/></div>
      <div className="flex gap-2"><select aria-label="Lọc vai trò" value={role} onChange={(event) => { setRole(event.target.value); setPage(0); }} className="rounded-xl border border-brand-200 bg-paper px-3 py-2 text-xs text-ink"><option value="">Tất cả vai trò</option><option value="CUSTOMER">Thành viên</option><option value="EXPERT">Chuyên gia</option><option value="ADMIN">Quản trị viên</option></select><select aria-label="Lọc trạng thái" value={status} onChange={(event) => { setStatus(event.target.value); setPage(0); }} className="rounded-xl border border-brand-200 bg-paper px-3 py-2 text-xs text-ink"><option value="">Tất cả trạng thái</option><option value="ACTIVE">Đang hoạt động</option><option value="LOCKED">Đang bị khóa</option></select></div>
    </div></Card>

    {error ? <Card className="p-8 text-center"><ShieldAlert className="mx-auto h-8 w-8 text-red-600"/><p role="alert" className="mt-3 text-sm text-ink">{error}</p><Button className="mt-4" size="sm" onClick={reload}>Thử lại</Button></Card> : <Card className="overflow-hidden">
      {loading && <div role="status" className="border-b border-brand-100 px-4 py-2 text-xs text-ink-muted">Đang tải dữ liệu tài khoản...</div>}
      <div className="overflow-x-auto"><table className="w-full text-left text-xs"><thead className="border-b border-brand-100 bg-brand-50/50 text-ink-muted"><tr><th className="px-4 py-3">Thành viên</th><th className="px-4 py-3">Vai trò</th><th className="px-4 py-3">Ngày tham gia</th><th className="px-4 py-3">Công thức</th><th className="px-4 py-3">Trạng thái</th><th className="px-4 py-3 text-right">Thao tác</th></tr></thead>
        <tbody className="divide-y divide-brand-100/60 text-ink">{!loading && rows.length === 0 ? <tr><td colSpan={6} className="px-4 py-8 text-center text-ink-muted">Không tìm thấy tài khoản phù hợp.</td></tr> : rows.map((user) => <tr key={user.id} className="hover:bg-brand-50/30"><td className="px-4 py-3"><div className="flex items-center gap-3"><div className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 font-bold text-brand-700">{user.displayName.charAt(0)}</div><div><p className="font-bold">{user.displayName}</p><p className="text-[11px] text-ink-muted">{user.email} · #{user.id}</p></div></div></td>
          <td className="px-4 py-3">{user.role === 'EXPERT' ? <Badge tone="leaf">{roleLabels[user.role]}</Badge> : user.role === 'ADMIN' ? <Badge tone="brand">{roleLabels[user.role]}</Badge> : <Badge tone="neutral">{roleLabels[user.role]}</Badge>}</td><td className="px-4 py-3 text-ink-muted">{date(user.joinedAt)}</td><td className="px-4 py-3 font-semibold">{user.publishedRecipeCount} món</td><td className="px-4 py-3">{user.accountStatus === 'ACTIVE' ? <span className="rounded-full bg-leaf-100 px-2 py-1 text-[11px] font-bold text-leaf-800">● Hoạt động</span> : <span className="rounded-full bg-red-100 px-2 py-1 text-[11px] font-bold text-red-800">● Đã khóa</span>}</td>
          <td className="px-4 py-3 text-right"><div className="flex justify-end gap-1"><IconButton title="Xem chi tiết và lịch sử" onClick={() => void openDetail(user)}><Eye className="h-4 w-4"/></IconButton><IconButton title={user.role === 'ADMIN' ? 'Cộng AI Credit' : 'Tặng gói AI / Cộng AI Credit'} onClick={() => void openAiManagement(user)}>{user.role === 'ADMIN' ? <Coins className="h-4 w-4"/> : <Gift className="h-4 w-4"/>}</IconButton>{user.role !== 'ADMIN' && <><IconButton title="Đổi vai trò" onClick={() => { setSelected(user); setNextRole(user.role === 'CUSTOMER' ? 'EXPERT' : 'CUSTOMER'); setReason(''); setMode('ROLE'); }}><UserCheck className="h-4 w-4"/></IconButton><IconButton title={user.accountStatus === 'ACTIVE' ? 'Khóa tài khoản' : 'Mở khóa tài khoản'} onClick={() => { setSelected(user); setReason(''); setMode('STATUS'); }}>{user.accountStatus === 'ACTIVE' ? <Lock className="h-4 w-4"/> : <Unlock className="h-4 w-4"/>}</IconButton></>}</div></td></tr>)}</tbody>
      </table></div>
      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-brand-100 px-4 py-3 text-xs"><span className="text-ink-muted">{totalElements} tài khoản · Trang {totalPages === 0 ? 0 : page + 1}/{totalPages}</span><div className="flex gap-2"><Button size="sm" variant="outline" disabled={page === 0 || loading} onClick={() => setPage((value) => Math.max(0, value - 1))}><ChevronLeft className="h-4 w-4"/>Trước</Button><Button size="sm" variant="outline" disabled={page + 1 >= totalPages || loading} onClick={() => setPage((value) => value + 1)}>Sau<ChevronRight className="h-4 w-4"/></Button></div></div>
    </Card>}

    {mode && selected && <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget && !saving) setMode(null); }}>
      <Card role="dialog" aria-modal="true" aria-labelledby="admin-user-modal-title" className="max-h-[90vh] w-full max-w-xl overflow-y-auto p-5 shadow-xl">
        <div className="flex items-start justify-between border-b border-brand-100 pb-3"><div><h2 id="admin-user-modal-title" className="font-bold text-ink">{mode === 'VIEW' ? 'Chi tiết tài khoản' : mode === 'ROLE' ? 'Đổi vai trò tài khoản' : mode === 'AI_CHOICE' ? 'Chọn thao tác AI' : mode === 'AI_PACKAGE' ? 'Tặng gói AI' : mode === 'AI_CREDIT' ? 'Tặng AI Credit' : selected.accountStatus === 'ACTIVE' ? 'Khóa tài khoản' : 'Mở khóa tài khoản'}</h2><p className="mt-1 text-xs text-ink-muted">{selected.displayName} · {selected.email}</p></div><button aria-label="Đóng" onClick={() => setMode(null)} className="rounded p-1 text-ink-muted hover:bg-brand-100"><X className="h-4 w-4"/></button></div>
        {mode === 'VIEW' ? <div className="space-y-4 pt-4 text-xs">{!detail && !historyError ? <p role="status">Đang tải hồ sơ và lịch sử...</p> : detail && <div className="grid grid-cols-2 gap-3 rounded-xl bg-brand-50/50 p-3"><div><span className="text-ink-muted">Mã tài khoản</span><p className="font-bold">#{detail.id}</p></div><div><span className="text-ink-muted">Vai trò</span><p className="font-bold">{roleLabels[detail.role]}</p></div><div><span className="text-ink-muted">Trạng thái</span><p className="font-bold">{detail.accountStatus === 'ACTIVE' ? 'Đang hoạt động' : 'Đã khóa'}</p></div><div><span className="text-ink-muted">Công thức công khai</span><p className="font-bold">{detail.publishedRecipeCount}</p></div><div><span className="text-ink-muted">Gói AI</span><p className="font-bold">{aiTier?.newTier ?? '—'}</p></div><div><span className="text-ink-muted">Hết hạn</span><p className="font-bold">{aiTier?.endsAt ? date(aiTier.endsAt) : 'Theo tháng dương lịch / không áp dụng'}</p></div></div>}
          <h3 className="font-bold">Lịch sử tài khoản</h3>{historyError ? <div><p role="alert">{historyError}</p><Button size="sm" variant="outline" onClick={() => void openDetail(selected, historyPage)}>Thử lại</Button></div> : history.length === 0 ? <p className="text-ink-muted">Chưa có lịch sử thay đổi.</p> : <><ol className="space-y-2">{history.map((event) => <li key={event.eventId} className="rounded-lg border border-brand-100 p-3"><div className="flex justify-between gap-2"><strong>{event.action === 'ROLE_CHANGED' ? 'Đổi vai trò' : event.action === 'AI_PACKAGE_GIFTED' ? 'Tặng gói AI' : event.action === 'AI_CREDIT_GRANTED' ? 'Tặng AI Credit' : 'Đổi trạng thái'}</strong><time className="text-ink-muted">{date(event.occurredAt)}</time></div><p className="mt-1">{event.action === 'AI_CREDIT_GRANTED' ? `+${event.oldValue} AI Credit · Số dư sau giao dịch: ${event.newValue}` : `${event.oldValue} → ${event.newValue}`}</p>{event.effectiveUntil && <p className="mt-1">Có hiệu lực đến {date(event.effectiveUntil)}</p>}<p className="mt-1 text-ink-muted">Admin: {event.administratorName ?? `#${event.administratorId ?? '?'}`} · {event.reason}</p></li>)}</ol><div className="flex items-center justify-between"><span className="text-ink-muted">Trang lịch sử {historyPage + 1}/{historyTotalPages}</span><div className="flex gap-2"><Button size="sm" variant="outline" disabled={historyPage === 0} onClick={() => void openDetail(selected, historyPage - 1)}>Trước</Button><Button size="sm" variant="outline" disabled={historyPage + 1 >= historyTotalPages} onClick={() => void openDetail(selected, historyPage + 1)}>Sau</Button></div></div></>}
          <Button size="sm" variant="outline" className="w-full" onClick={() => setMode(null)}>Đóng</Button>
        </div> : mode === 'AI_CHOICE' ? <div className="space-y-4 pt-4 text-xs">
          <p className="text-ink-soft">Chọn một thao tác. Mỗi loại có biểu mẫu và lý do riêng; hoàn tất một thao tác không yêu cầu thực hiện thao tác còn lại.</p>
          <div className="grid gap-3 sm:grid-cols-2">
            {selected.role !== 'ADMIN' && <button type="button" onClick={() => void openPackageGift()} className="flex min-h-32 flex-col items-start gap-2 rounded-xl border border-brand-200 bg-paper p-4 text-left transition-colors hover:border-brand-500 hover:bg-brand-50 focus:outline-none focus:ring-2 focus:ring-brand-500"><Gift className="h-5 w-5 text-brand-700"/><strong>Tặng gói AI</strong><span className="text-ink-muted">Chọn Free, Plus hoặc Pro. Gói áp dụng ngay và được lưu vào lịch sử tài khoản.</span></button>}
            <button type="button" onClick={openCreditGrant} className="flex min-h-32 flex-col items-start gap-2 rounded-xl border border-brand-200 bg-paper p-4 text-left transition-colors hover:border-brand-500 hover:bg-brand-50 focus:outline-none focus:ring-2 focus:ring-brand-500"><Coins className="h-5 w-5 text-brand-700"/><strong>Tặng AI Credit</strong><span className="text-ink-muted">Cộng Credit độc lập; giao dịch được lưu vào Credit ledger và lịch sử tài khoản.</span></button>
          </div>
          <div className="flex justify-end border-t border-brand-100 pt-3"><Button size="sm" variant="outline" onClick={() => setMode(null)}>Đóng</Button></div>
        </div> : mode === 'AI_PACKAGE' ? <div className="space-y-4 pt-4 text-xs">
          {aiError && <p role="alert" className="rounded-lg bg-red-50 p-3 text-red-800">{aiError}</p>}
          {aiNotice && <p role="status" className="rounded-lg bg-leaf-50 p-3 text-leaf-800">{aiNotice}</p>}
          {aiTierLoading ? <p role="status">Đang tải gói AI hiện tại...</p> : aiTier && <><div className="rounded-xl bg-brand-50/50 p-3"><span className="text-ink-muted">Gói hiện tại</span><p className="font-bold">{aiTier.newTier} · {aiTier.tokenQuota.toLocaleString('vi-VN')} token</p><p className="text-ink-muted">{aiTier.endsAt ? `Hết hạn ${date(aiTier.endsAt)}` : 'Free theo tháng dương lịch'}</p></div>
          <label className="block font-bold">Gói muốn tặng<select aria-label="Gói AI muốn tặng" value={nextAiTier} onChange={(event) => setNextAiTier(event.target.value as AdminAiTier)} className="mt-1 w-full rounded-lg border border-brand-200 bg-paper p-2 font-normal"><option value="FREE">Free</option><option value="PLUS">Plus</option><option value="PRO">Pro</option></select></label><p className="text-ink-muted">Plus/Pro bắt đầu chu kỳ 30 ngày mới. Không tạo payment order hoặc hoàn tiền; người nhận được thông báo in-app.</p>
          <label className="block font-bold">Lý do tặng gói (10–500 ký tự)<textarea aria-label="Lý do tặng gói AI" rows={3} maxLength={500} value={packageReason} onChange={(event) => setPackageReason(event.target.value)} className="mt-1 w-full rounded-lg border border-brand-200 bg-paper p-2 font-normal" placeholder="Ghi rõ lý do tặng gói AI..."/></label><p className="text-ink-muted">{packageReason.trim().length}/500 ký tự</p>
          <div className="flex flex-wrap justify-between gap-2 border-t border-brand-100 pt-3"><Button size="sm" variant="outline" disabled={saving} onClick={() => setMode('AI_CHOICE')}>Quay lại chọn thao tác</Button><Button size="sm" disabled={saving || aiTier.newTier === nextAiTier || packageReason.trim().length < 10 || packageReason.trim().length > 500} onClick={() => void saveAiTier()}>{saving ? 'Đang xử lý...' : 'Xác nhận tặng gói'}</Button></div></>}
        </div> : mode === 'AI_CREDIT' ? <div className="space-y-4 pt-4 text-xs">
          {aiError && <p role="alert" className="rounded-lg bg-red-50 p-3 text-red-800">{aiError}</p>}
          {aiNotice && <p role="status" className="rounded-lg bg-leaf-50 p-3 text-leaf-800">{aiNotice}</p>}
          <label className="block font-bold">Số AI Credit<input aria-label="Số AI Credit muốn tặng" type="number" min="0.000000001" step="any" value={creditAmount} onChange={(event) => setCreditAmount(event.target.value)} className="mt-1 w-full rounded-lg border border-brand-200 bg-paper p-2 font-normal" placeholder="Ví dụ: 1000"/></label>
          <label className="block font-bold">Lý do tặng AI Credit (10–500 ký tự)<textarea aria-label="Lý do tặng AI Credit" rows={3} maxLength={500} value={creditReason} onChange={(event) => setCreditReason(event.target.value)} className="mt-1 w-full rounded-lg border border-brand-200 bg-paper p-2 font-normal" placeholder="Ghi rõ lý do tặng AI Credit..."/></label><p className="text-ink-muted">{creditReason.trim().length}/500 ký tự</p>
          <div className="flex flex-wrap justify-between gap-2 border-t border-brand-100 pt-3"><Button size="sm" variant="outline" disabled={saving} onClick={() => setMode('AI_CHOICE')}>Quay lại chọn thao tác</Button><Button size="sm" disabled={saving || !Number.isFinite(Number(creditAmount)) || Number(creditAmount) <= 0 || creditReason.trim().length < 10 || creditReason.trim().length > 500} onClick={() => void grantCredit()}>{saving ? 'Đang xử lý...' : 'Xác nhận tặng AI Credit'}</Button></div>
        </div> : <div className="space-y-4 pt-4 text-xs"><p className="text-ink-soft">{mode === 'ROLE' ? 'Chỉ có thể chuyển giữa Thành viên và Chuyên gia. Nếu cấp Chuyên gia khi đang có đơn chờ, đơn sẽ được kết thúc và người dùng nhận thông báo.' : selected.accountStatus === 'ACTIVE' ? 'Nếu khóa tài khoản này, người dùng sẽ không thể đăng nhập hoặc tiếp tục sử dụng tài khoản cho đến khi quản trị viên mở khóa. Bạn có chắc muốn tiếp tục không?' : 'Mở khóa để người dùng có thể đăng nhập và sử dụng lại tài khoản.'}</p>
          {mode === 'ROLE' && <label className="block font-bold">Vai trò mới<select aria-label="Vai trò mới" value={nextRole} onChange={(event) => setNextRole(event.target.value as Exclude<AdminUserRole, 'ADMIN'>)} className="mt-1 w-full rounded-lg border border-brand-200 bg-paper p-2 font-normal"><option value="CUSTOMER">Thành viên (CUSTOMER)</option><option value="EXPERT">Chuyên gia (EXPERT)</option></select></label>}
          <label className="block font-bold">Lý do bắt buộc (10–500 ký tự)<textarea aria-label="Lý do thay đổi" rows={4} maxLength={500} value={reason} onChange={(event) => setReason(event.target.value)} className="mt-1 w-full rounded-lg border border-brand-200 bg-paper p-2 font-normal" placeholder="Ghi rõ lý do thay đổi tài khoản..."/></label><p className="text-ink-muted">{reason.trim().length}/500 ký tự</p>
          <div className="flex justify-end gap-2 border-t border-brand-100 pt-3"><Button size="sm" variant="outline" disabled={saving} onClick={() => setMode(null)}>Hủy</Button><Button size="sm" disabled={saving || reason.trim().length < 10 || reason.trim().length > 500 || (mode === 'ROLE' && selected.role === nextRole)} onClick={() => void saveStatusOrRole()}>{saving ? 'Đang lưu...' : 'Xác nhận'}</Button></div>
        </div>}
      </Card>
    </div>}
  </div>;
}

function SummaryCard({ label, value, icon }: { label: string; value?: number; icon: React.ReactNode }) {
  return <Card className="p-4"><div className="flex items-center justify-between"><span className="text-xs font-semibold text-ink-muted">{label}</span><span className="flex h-7 w-7 items-center justify-center rounded-lg bg-brand-100 text-brand-700">{icon}</span></div><p className="mt-2 text-2xl font-extrabold text-ink">{value ?? '—'}</p><span className="text-[11px] text-ink-muted">Toàn hệ thống</span></Card>;
}

function IconButton({ title, onClick, children }: { title: string; onClick: () => void; children: React.ReactNode }) {
  return <button type="button" title={title} aria-label={title} onClick={onClick} className="rounded-lg p-1.5 text-ink-muted transition-colors hover:bg-brand-100 hover:text-brand-700">{children}</button>;
}
