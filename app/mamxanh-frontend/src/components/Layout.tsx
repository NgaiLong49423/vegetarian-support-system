import { useCallback, useMemo, useState, type ReactNode } from 'react';
import { useLocation } from 'react-router-dom';
import { AppHeader } from './AppHeader';
import { AppFooter } from './AppFooter';
import { useAuth } from './AuthContext';
import { Info } from 'lucide-react';
import { AiChatContext, type AiChatContextValue } from './AiChatContext';
import { AiChatPage } from '../pages/AiChatPage';

import { AdminLayout } from './AdminLayout';

export function Layout({ children }: { children: ReactNode }) {
  const { pathname } = useLocation();
  const { account } = useAuth();
  const [chatOpen, setChatOpen] = useState(false);
  const [chatRecipeId, setChatRecipeId] = useState<number | null>(null);
  const openChat = useCallback((recipeId?: number) => {
    setChatRecipeId(recipeId ?? null);
    setChatOpen(true);
  }, []);
  const closeChat = useCallback(() => setChatOpen(false), []);
  const chatContext = useMemo<AiChatContextValue>(() => ({
    isOpen: chatOpen,
    recipeId: chatRecipeId,
    openChat,
    closeChat,
  }), [chatOpen, chatRecipeId, openChat, closeChat]);

  if (['/dang-nhap', '/dang-ky', '/quen-mat-khau', '/xac-minh-email', '/dat-lai-mat-khau'].includes(pathname)) {
    return <>{children}</>;
  }

  // Pure Admin Workspace separation
  if (pathname.startsWith('/admin') || pathname.startsWith('/quan-tri')) {
    return <AdminLayout>{children}</AdminLayout>;
  }

  const isGuestBlockedRoute = !account && ['/di-cho', '/ke-hoach'].includes(pathname);
  const prototype = !isGuestBlockedRoute && (
    ['/cong-dong', '/dinh-duong', '/goi-ai', '/giao-dich', '/so-sanh'].includes(pathname)
    || pathname.startsWith('/bai-viet/')
  );

  return (
    <AiChatContext.Provider value={chatContext}>
      <div className="food-app flex min-h-screen flex-col bg-cream">
        <AppHeader />
        <main className="flex-1">
          {prototype && (
            <PageContainer className="pt-4">
              <div role="note" className="flex items-center gap-2 rounded-lg border border-brand-100 bg-cream-soft px-3 py-2 text-xs text-ink-muted">
                <Info className="h-3.5 w-3.5 shrink-0 text-brand-600" />
                Bản xem thử giao diện · Thao tác ở màn hình này chưa lưu vào tài khoản.
              </div>
            </PageContainer>
          )}
          {children}
        </main>
        <AppFooter />
        <AiChatPage />
      </div>
    </AiChatContext.Provider>
  );
}

export function PageContainer({ children, className = '' }: { children: ReactNode; className?: string }) {
  const hasCustomMaxW = /\bmax-w-/.test(className);
  return (
    <div className={`mx-auto w-full ${hasCustomMaxW ? '' : 'max-w-[1344px]'} px-4 sm:px-6 lg:px-8 ${className}`}>
      {children}
    </div>
  );
}
