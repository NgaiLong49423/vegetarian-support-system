import { useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { AppHeader } from './AppHeader';
import { AppFooter } from './AppFooter';

export function Layout({ children }: { children: ReactNode }) {
  const { pathname } = useLocation();
  if (['/dang-nhap', '/dang-ky', '/quen-mat-khau', '/xac-minh-email', '/dat-lai-mat-khau'].includes(pathname)) return <>{children}</>;
  return (
    <div className="flex min-h-screen flex-col">
      <AppHeader />
      <main className="flex-1">{children}</main>
      <AppFooter />
    </div>
  );
}

export function PageContainer({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 ${className}`}>{children}</div>;
}
