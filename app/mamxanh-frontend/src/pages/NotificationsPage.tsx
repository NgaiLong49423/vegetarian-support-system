import { useEffect, useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { Bell, CheckCheck, LoaderCircle } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, EmptyState } from '../components/ui';
import { useAuth } from '../components/AuthContext';
import {
  announceNotificationsChanged,
  notificationsApi,
  type AppNotification,
  type NotificationPage,
} from '../api/notifications';
import { asApiError } from '../lib/apiClient';

function formatTime(createdAt: string) {
  return new Date(`${createdAt}Z`).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' });
}

/** UC-49.1 notification center and UC-49.2 read state (`/thong-bao`); only the owner's notifications. */
export function NotificationsPage() {
  const { isAuthenticated } = useAuth();
  if (!isAuthenticated) return <Navigate to="/dang-nhap" replace />;
  return <NotificationCenter />;
}

function NotificationCenter() {
  const navigate = useNavigate();
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<NotificationPage | null>(null);
  const [loadError, setLoadError] = useState('');
  const [actionError, setActionError] = useState('');
  const [attempt, setAttempt] = useState(0);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let alive = true;
    setLoadError('');
    notificationsApi.list(page, unreadOnly)
      .then((data) => { if (alive) setResult(data); })
      .catch((cause: unknown) => { if (alive) { setResult(null); setLoadError(asApiError(cause).message); } });
    return () => { alive = false; };
  }, [page, unreadOnly, attempt]);

  const showFilter = (next: boolean) => {
    setUnreadOnly(next);
    setPage(0);
  };

  const open = async (item: AppNotification) => {
    setActionError('');
    if (!item.read) {
      try {
        await notificationsApi.markRead(item.notificationId);
        announceNotificationsChanged();
      } catch (cause) {
        setActionError(asApiError(cause).message);
        return;
      }
    }
    if (item.targetPath) navigate(item.targetPath);
    else setAttempt((current) => current + 1);
  };

  const readAll = async () => {
    setActionError('');
    setBusy(true);
    try {
      await notificationsApi.markAllRead();
      announceNotificationsChanged();
      setAttempt((current) => current + 1);
    } catch (cause) {
      setActionError(asApiError(cause).message);
    } finally {
      setBusy(false);
    }
  };

  const filterClass = (active: boolean) => `rounded-full px-3.5 py-1.5 text-sm font-semibold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 ${
    active ? 'bg-brand-600 text-on-brand' : 'text-ink-soft hover:bg-brand-50'}`;
  const hasUnread = result?.content.some((item) => !item.read) ?? false;

  return <PageContainer className="py-8">
    <div className="mx-auto max-w-2xl">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <h1 className="text-2xl font-extrabold text-ink">Thông báo</h1>
        <Link to="/ho-so/cai-dat#email-thong-bao" className="text-sm font-semibold text-brand-700 hover:underline">Cài đặt email thông báo</Link>
      </div>

      <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
        <div role="group" aria-label="Lọc thông báo" className="flex gap-1 rounded-full border border-brand-100 bg-paper p-1">
          <button type="button" aria-pressed={!unreadOnly} onClick={() => showFilter(false)} className={filterClass(!unreadOnly)}>Tất cả</button>
          <button type="button" aria-pressed={unreadOnly} onClick={() => showFilter(true)} className={filterClass(unreadOnly)}>Chưa đọc</button>
        </div>
        <Button variant="outline" size="sm" disabled={busy || !hasUnread} onClick={() => void readAll()}>
          {busy ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <CheckCheck className="h-4 w-4" />}Đánh dấu tất cả đã đọc
        </Button>
      </div>
      {actionError && <p role="alert" className="mt-3 rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-800">{actionError}</p>}

      <div className="mt-5">
        {loadError ? <EmptyState title="Không tải được thông báo" description={loadError}
          action={<Button variant="secondary" onClick={() => setAttempt((current) => current + 1)}>Thử lại</Button>} />
          : !result ? <p role="status" className="py-10 text-center text-sm text-ink-muted">Đang tải thông báo...</p>
            : result.content.length === 0 ? <EmptyState title={unreadOnly ? 'Không có thông báo chưa đọc' : 'Chưa có thông báo'}
              description="Khi có người trả lời bình luận của bạn, thông báo sẽ hiện ở đây." />
              : <ul aria-label="Danh sách thông báo" className="divide-y divide-brand-100 overflow-hidden rounded-2xl border border-brand-100 bg-paper">
                {result.content.map((item) => <li key={item.notificationId}>
                  <button type="button" onClick={() => void open(item)}
                    className={`flex w-full gap-3 px-4 py-3.5 text-left hover:bg-brand-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-500 ${item.read ? '' : 'bg-brand-50/60'}`}>
                    <Bell aria-hidden="true" className={`mt-0.5 h-4 w-4 shrink-0 ${item.read ? 'text-ink-muted' : 'text-brand-600'}`} />
                    <span className="min-w-0 flex-1">
                      <span className="flex flex-wrap items-center gap-2">
                        <span className={`text-sm ${item.read ? 'font-medium text-ink-soft' : 'font-bold text-ink'}`}>{item.title}</span>
                        {!item.read && <span className="rounded-full bg-brand-600 px-2 py-0.5 text-[10px] font-bold text-on-brand">Chưa đọc</span>}
                      </span>
                      <span className="mt-1 block break-words text-sm text-ink-soft">{item.message}</span>
                      <span className="mt-1 block text-xs text-ink-muted">{formatTime(item.createdAt)}</span>
                    </span>
                  </button>
                </li>)}
              </ul>}
      </div>

      {result && result.totalPages > 1 && <nav aria-label="Phân trang thông báo" className="mt-5 flex items-center justify-center gap-3">
        <Button variant="outline" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>Trang trước</Button>
        <span aria-live="polite" className="text-sm text-ink-muted">Trang {page + 1}/{result.totalPages}</span>
        <Button variant="outline" disabled={page + 1 >= result.totalPages} onClick={() => setPage((current) => current + 1)}>Trang sau</Button>
      </nav>}
    </div>
  </PageContainer>;
}
