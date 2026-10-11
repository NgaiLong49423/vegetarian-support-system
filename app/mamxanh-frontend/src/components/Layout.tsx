import { useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { AppHeader } from './AppHeader';
import { AppFooter } from './AppFooter';
import { useAuth } from './AuthContext';
import { Info } from 'lucide-react';

export function Layout({ children }: { children: ReactNode }) {
  const { pathname } = useLocation();
  const { account } = useAuth();
  const prototype = ['/di-cho', '/cong-dong', '/dinh-duong', '/goi-ai', '/giao-dich', '/so-sanh'].includes(pathname)
    || pathname.startsWith('/bai-viet/')
    || (pathname === '/ke-hoach' && !account);
  if (['/dang-nhap', '/dang-ky', '/quen-mat-khau', '/xac-minh-email', '/dat-lai-mat-khau'].includes(pathname)) return <>{children}</>;
  return (
    <div className="food-app flex min-h-screen flex-col bg-cream">
      <AppHeader />
      <main className="flex-1">{prototype && <PageContainer className="pt-4"><div role="note" className="flex items-center gap-2 rounded-lg border border-brand-100 bg-cream-soft px-3 py-2 text-xs text-ink-muted"><Info className="h-3.5 w-3.5 shrink-0 text-brand-600" />Bản xem thử giao diện · Thao tác ở màn hình này chưa lưu vào tài khoản.</div></PageContainer>}{children}</main>
      <AppFooter />
    </div>
  );
}

export function PageContainer({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`mx-auto w-full max-w-[1160px] px-4 sm:px-6 ${className}`}>{children}</div>;
}
