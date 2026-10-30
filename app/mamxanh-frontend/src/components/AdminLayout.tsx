import { useState, type ReactNode } from 'react';
import { useLocation, Link, NavLink, useNavigate } from 'react-router-dom';
import {
  BadgeCheck,
  Activity,
  Boxes,
  CreditCard,
  ExternalLink,
  LayoutDashboard,
  Leaf,
  LogOut,
  Menu,
  Moon,
  ShieldAlert,
  ShieldCheck,
  Settings2,
  Sun,
  Users,
  X,
} from 'lucide-react';
import { useAuth } from './AuthContext';
import { useTheme } from './ThemeContext';

interface AdminNavItem {
  to: string;
  label: string;
  icon: typeof LayoutDashboard;
  badge?: string;
  end?: boolean;
}

const adminNavigationGroups: { title: string; items: AdminNavItem[] }[] = [
  {
    title: 'Tổng quan',
    items: [
      { to: '/admin', label: 'Bảng điều khiển', icon: LayoutDashboard, end: true },
    ],
  },
  {
    title: 'Nội dung & Dinh dưỡng',
    items: [
      { to: '/quan-tri/danh-muc', label: 'Danh mục nguyên liệu', icon: Boxes },
    ],
  },
  {
    title: 'Nhân sự & Chuyên gia',
    items: [
      { to: '/admin/xet-duyet-chuyen-gia', label: 'Xét duyệt Chuyên gia', icon: BadgeCheck },
    ],
  },
  {
    title: 'Quản trị hệ thống',
    items: [
      { to: '/admin/bao-cao', label: 'Hậu kiểm nội dung', icon: ShieldAlert },
      { to: '/admin/nguoi-dung', label: 'Quản lý người dùng', icon: Users },
      { to: '/admin/giao-dich', label: 'Giám sát giao dịch', icon: CreditCard },
      { to: '/admin/ai-telemetry', label: 'Mức sử dụng AI', icon: Activity },
      { to: '/admin/ai-config', label: 'Cấu hình AI', icon: Settings2 },
    ],
  },
];

export function AdminLayout({ children }: { children: ReactNode }) {
  const { account, signOut } = useAuth();
  const { theme, setTheme } = useTheme();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [mobileDrawerOpen, setMobileDrawerOpen] = useState(false);

  const getPageTitle = () => {
    if (pathname === '/admin' || pathname === '/admin/tong-quan') return 'Bảng điều phối hệ thống';
    if (pathname.startsWith('/admin/ai-telemetry')) return 'Mức sử dụng AI';
    if (pathname.startsWith('/admin/ai-config')) return 'Cấu hình AI';
    if (pathname.startsWith('/quan-tri/danh-muc')) return 'Danh mục nguyên liệu dinh dưỡng';
    if (pathname.startsWith('/admin/xet-duyet-chuyen-gia')) return 'Xét duyệt Chuyên gia';
    if (pathname.startsWith('/admin/bao-cao')) return 'Hậu kiểm nội dung';
    if (pathname.startsWith('/admin/nguoi-dung')) return 'Quản lý người dùng';
    if (pathname.startsWith('/admin/giao-dich')) return 'Giám sát giao dịch hệ thống';
    return 'Khu vực quản trị';
  };

  const leave = () => {
    signOut();
    navigate('/dang-nhap');
  };

  if (!account && !pathname.startsWith('/quan-tri')) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-cream px-4 py-10">
        <section className="w-full max-w-md rounded-3xl border border-brand-200/80 bg-paper p-8 text-center shadow-sm">
          <ShieldAlert className="mx-auto h-10 w-10 text-brand-600" aria-hidden="true" />
          <h1 className="mt-4 text-2xl font-extrabold text-ink">Đăng nhập để tiếp tục</h1>
          <p className="mt-2 text-sm leading-6 text-ink-muted">Bạn cần đăng nhập bằng tài khoản Quản trị viên để tiếp tục.</p>
          <Link to="/dang-nhap" className="mt-6 inline-flex min-h-11 items-center justify-center rounded-xl bg-brand-600 px-5 font-semibold text-white transition hover:bg-brand-500 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600">
            Đăng nhập
          </Link>
        </section>
      </main>
    );
  }

  if (account && account.role !== 'ADMIN' && !pathname.startsWith('/quan-tri')) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-cream px-4 py-10">
        <section role="alert" className="w-full max-w-md rounded-3xl border border-amber-500/30 bg-paper p-8 text-center shadow-sm">
          <ShieldAlert className="mx-auto h-10 w-10 text-amber-500" aria-hidden="true" />
          <h1 className="mt-4 text-2xl font-extrabold text-ink">Không có quyền truy cập</h1>
          <p className="mt-2 text-sm leading-6 text-ink-muted">Khu vực quản trị chỉ dành cho tài khoản Administrator.</p>
          <Link to="/" className="mt-6 inline-flex min-h-11 items-center justify-center rounded-xl border border-brand-200 px-5 font-semibold text-ink-soft hover:bg-brand-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600">
            Về trang chủ
          </Link>
        </section>
      </main>
    );
  }

  return (
    <div className="min-h-screen bg-cream text-ink lg:grid lg:grid-cols-[280px_minmax(0,1fr)] items-start">
      {/* =========================================================================
          Desktop & Mobile Sidebar
      ========================================================================= */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-72 flex-col bg-[#112d24] text-white transition-transform duration-200 lg:sticky lg:top-0 lg:h-screen lg:w-full lg:self-start lg:translate-x-0 lg:overflow-hidden ${
          mobileDrawerOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        {/* Brand Header */}
        <div className="flex h-16 items-center justify-between border-b border-white/10 px-5">
          <Link
            to="/admin"
            className="flex items-center gap-3 focus-visible:outline-2 focus-visible:outline-white"
          >
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-white text-leaf-700 shadow-sm">
              <Leaf className="h-5 w-5" aria-hidden="true" />
            </span>
            <div className="min-w-0">
              <span className="block text-base font-black tracking-tight text-white">Mâm Xanh</span>
              <span className="block text-[10px] font-bold uppercase tracking-[0.16em] text-emerald-200/80">
                Trung tâm quản trị
              </span>
            </div>
          </Link>
          <button
            type="button"
            onClick={() => setMobileDrawerOpen(false)}
            aria-label="Đóng menu"
            className="rounded-lg p-1.5 text-white/70 hover:bg-white/10 hover:text-white lg:hidden"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Navigation Groups */}
        <nav aria-label="Điều hướng quản trị" className="flex-1 space-y-6 overflow-y-auto px-4 py-5 scrollbar-thin">
          {adminNavigationGroups.map((group) => (
            <div key={group.title}>
              <p className="px-3 pb-2 text-[10px] font-extrabold uppercase tracking-[0.18em] text-emerald-200/50">
                {group.title}
              </p>
              <div className="space-y-1">
                {group.items.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.end}
                    onClick={() => setMobileDrawerOpen(false)}
                    className={({ isActive }) =>
                      `flex items-center justify-between gap-3 rounded-xl px-3.5 py-2.5 text-xs font-semibold transition-all ${
                        isActive
                          ? 'bg-brand-600 font-bold text-white shadow-sm shadow-brand-600/30'
                          : 'text-emerald-100/75 hover:bg-white/10 hover:text-white'
                      }`
                    }
                  >
                    <div className="flex items-center gap-3">
                      <item.icon className="h-4 w-4 shrink-0" aria-hidden="true" />
                      <span>{item.label}</span>
                    </div>
                    {item.badge && (
                      <span className="rounded-full bg-white/15 px-2 py-0.5 text-[9px] font-bold uppercase tracking-wider text-emerald-200">
                        {item.badge}
                      </span>
                    )}
                  </NavLink>
                ))}
              </div>
            </div>
          ))}

          {/* Quick link to public user website */}
          <div className="border-t border-white/10 pt-4">
            <Link
              to="/trang-chu"
              onClick={() => setMobileDrawerOpen(false)}
              className="flex items-center gap-3 rounded-xl px-3.5 py-2.5 text-xs font-medium text-emerald-200/70 transition-colors hover:bg-white/10 hover:text-white"
            >
              <ExternalLink className="h-4 w-4 shrink-0" />
              <span>Xem giao diện người dùng</span>
            </Link>
          </div>
        </nav>

        {/* Admin Account & Sign out card */}
        <div className="border-t border-white/10 p-4">
          <div className="flex items-center gap-3 rounded-xl bg-white/[0.08] p-3">
            <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-emerald-100 font-bold text-[#112d24]">
              {account?.displayName ? account.displayName.trim().charAt(0).toUpperCase() : 'A'}
            </span>
            <div className="min-w-0 flex-1">
              <p className="truncate text-xs font-bold text-white">
                {account?.displayName || 'Administrator'}
              </p>
              <p className="truncate text-[11px] text-emerald-200/70">
                {account?.email || 'admin@mamxanh.local'}
              </p>
            </div>
          </div>
          {account && (
            <button
              type="button"
              onClick={leave}
              className="mt-2 flex w-full items-center justify-center gap-2 rounded-xl py-2 text-xs font-medium text-emerald-100/75 hover:bg-white/10 hover:text-white"
            >
              <LogOut className="h-3.5 w-3.5" /> Đăng xuất
            </button>
          )}
        </div>
      </aside>

      {/* Backdrop for mobile sidebar */}
      {mobileDrawerOpen && (
        <div
          role="presentation"
          onClick={() => setMobileDrawerOpen(false)}
          className="fixed inset-0 z-30 bg-black/60 backdrop-blur-xs lg:hidden"
        />
      )}

      {/* =========================================================================
          Main Content Frame
      ========================================================================= */}
      <div className="flex min-h-screen min-w-0 flex-col">
        {/* Top Navbar */}
        <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-brand-100 bg-paper/95 px-4 backdrop-blur-md sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setMobileDrawerOpen(true)}
              aria-label="Mở menu quản trị"
              className="flex h-9 w-9 items-center justify-center rounded-xl border border-brand-200 bg-paper text-ink-soft hover:bg-raised lg:hidden"
            >
              <Menu className="h-5 w-5" />
            </button>
            <div className="min-w-0">
              <nav aria-label="Breadcrumb" className="hidden text-xs text-ink-muted sm:flex sm:items-center sm:gap-1.5">
                <span>Khu vực quản trị</span>
                <span className="text-leaf-600">/</span>
                <span className="font-semibold text-ink">{getPageTitle()}</span>
              </nav>
              <p className="truncate text-sm font-black text-ink sm:hidden">{getPageTitle()}</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {/* Theme Toggle Button */}
            <button
              type="button"
              aria-label={theme === 'light' ? 'Chuyển sang chế độ tối' : 'Chuyển sang chế độ sáng'}
              title={theme === 'light' ? 'Chuyển sang chế độ tối' : 'Chuyển sang chế độ sáng'}
              onClick={() => setTheme(theme === 'light' ? 'dark' : 'light')}
              className="flex h-9 w-9 items-center justify-center rounded-full border border-brand-200/80 bg-paper text-ink-muted hover:bg-raised hover:text-brand-600 transition-colors"
            >
              {theme === 'light' ? <Moon className="h-4 w-4" /> : <Sun className="h-4 w-4" />}
            </button>

            {/* Administrator Status Badge */}
            <div className="flex items-center gap-1.5 rounded-full border border-leaf-200 bg-leaf-50 px-3 py-1.5 text-xs font-bold text-leaf-800">
              <ShieldCheck className="h-4 w-4 text-leaf-700" />
              <span className="hidden sm:inline">Administrator</span>
            </div>
          </div>
        </header>

        {/* Content Area */}
        <main className="min-w-0 flex-1 px-4 py-6 sm:px-6 sm:py-7 lg:px-8">
          {children}
        </main>

        {/* Admin Footer */}
        <footer className="border-t border-brand-100 bg-paper/60 px-4 py-4 text-center text-xs text-ink-muted sm:px-6 lg:px-8 lg:text-left">
          Mầm Xanh · Trung tâm điều phối và quản trị hệ thống chay
        </footer>
      </div>
    </div>
  );
}
