import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Bell } from 'lucide-react';
import { notificationsApi, NOTIFICATIONS_CHANGED_EVENT } from '../api/notifications';

const REFRESH_MS = 60_000;

/** AC-49.1 bell with the unread count of the signed-in user; opens the notification center. */
export function NotificationBell() {
  const { pathname } = useLocation();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    let alive = true;
    const load = () => {
      notificationsApi.unreadCount()
        .then((count) => { if (alive) setUnread(count); })
        .catch(() => { /* keep the last known count; the bell must not break the header */ });
    };
    load();
    const timer = window.setInterval(load, REFRESH_MS);
    window.addEventListener('focus', load);
    window.addEventListener(NOTIFICATIONS_CHANGED_EVENT, load);
    return () => {
      alive = false;
      window.clearInterval(timer);
      window.removeEventListener('focus', load);
      window.removeEventListener(NOTIFICATIONS_CHANGED_EVENT, load);
    };
  }, [pathname]);

  const label = unread > 0 ? `Thông báo, ${unread} chưa đọc` : 'Thông báo';
  return (
    <Link
      to="/thong-bao"
      aria-label={label}
      title={label}
      className="relative flex h-8 w-8 items-center justify-center rounded-full text-ink-muted hover:bg-paper hover:text-brand-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
    >
      <Bell className="h-4 w-4" aria-hidden="true" />
      {unread > 0 && (
        <span aria-hidden="true" className="absolute -right-0.5 -top-0.5 min-w-[1.125rem] rounded-full bg-brand-600 px-1 text-center text-[10px] font-bold leading-[1.125rem] text-on-brand">
          {unread > 99 ? '99+' : unread}
        </span>
      )}
    </Link>
  );
}
