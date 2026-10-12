import type { ReactNode } from 'react';
import { useLocation, Link, NavLink, useNavigate } from 'react-router-dom';
import { BadgeCheck, Boxes, Leaf, LogOut, ShieldCheck } from 'lucide-react';
import { useAuth } from './AuthContext';

const adminNavigation = [
  { to: '/admin/xet-duyet-chuyen-gia', label: 'Xét duyệt Chuyên gia', icon: BadgeCheck },
  { to: '/quan-tri/danh-muc', label: 'Danh mục nguyên liệu', icon: Boxes },
];

export function AdminLayout({ children }: { children: ReactNode }) {
  const { account, signOut } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const pageTitle = pathname.startsWith('/quan-tri/') ? 'Danh mục nguyên liệu' : 'Xét duyệt Chuyên gia';

  if (!account) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#f5f8f4] px-4 py-10">
        <section className="w-full max-w-md rounded-3xl border border-leaf-100 bg-white p-8 text-center shadow-sm">
          <ShieldCheck className="mx-auto h-10 w-10 text-leaf-700" aria-hidden="true" />
          <h1 className="mt-4 text-2xl font-extrabold text-ink">Đăng nhập quản trị</h1>
          <p className="mt-2 text-sm leading-6 text-ink-muted">Bạn cần đăng nhập bằng tài khoản Quản trị viên để tiếp tục.</p>
          <Link to="/dang-nhap" className="mt-6 inline-flex min-h-11 items-center justify-center rounded-xl bg-leaf-700 px-5 font-semibold text-white transition hover:bg-leaf-700/90 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-leaf-700">
            Đăng nhập
          </Link>
        </section>
      </main>
    );
  }

  if (account.role !== 'ADMIN') {
    return (
      <main className="flex min-h-screen items-center justify-center bg-[#f5f8f4] px-4 py-10">
        <section role="alert" className="w-full max-w-md rounded-3xl border border-amber-200 bg-white p-8 text-center shadow-sm">
          <ShieldCheck className="mx-auto h-10 w-10 text-amber-700" aria-hidden="true" />
          <h1 className="mt-4 text-2xl font-extrabold text-ink">Không có quyền truy cập</h1>
          <p className="mt-2 text-sm leading-6 text-ink-muted">Khu vực quản trị chỉ dành cho tài khoản Administrator.</p>
          <Link to="/trang-chu" className="mt-6 inline-flex min-h-11 items-center justify-center rounded-xl border border-brand-200 px-5 font-semibold text-ink-soft hover:bg-brand-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600">
            Về trang chủ
          </Link>
        </section>
      </main>
    );
  }

  const leave = () => {
    signOut();
    navigate('/dang-nhap');
  };

  return (
    <div className="min-h-screen bg-[#f5f8f4] text-ink lg:grid lg:grid-cols-[264px_minmax(0,1fr)]">
      <aside className="z-20 flex flex-col bg-[#173d31] text-white lg:sticky lg:top-0 lg:h-screen">
        <Link to="/admin/xet-duyet-chuyen-gia" className="flex h-[76px] items-center gap-3 border-b border-white/10 px-5 focus-visible:outline-2 focus-visible:outline-inset focus-visible:outline-white">
          <span className="flex h-10 w-10 items-center justify-center rounded-2xl bg-white text-leaf-700"><Leaf className="h-5 w-5" aria-hidden="true" /></span>
          <span className="min-w-0">
            <span className="block text-lg font-extrabold tracking-tight">Mâm Xanh</span>
            <span className="block text-[10px] font-semibold uppercase tracking-[0.18em] text-emerald-100/75">Trung tâm quản trị</span>
          </span>
        </Link>

        <div className="hidden px-5 pb-3 pt-7 text-[11px] font-bold uppercase tracking-[0.18em] text-emerald-100/55 lg:block">Quản lý hệ thống</div>
        <nav aria-label="Điều hướng quản trị" className="flex gap-2 overflow-x-auto px-3 py-3 lg:flex-1 lg:flex-col lg:overflow-visible lg:px-4 lg:py-0">
          {adminNavigation.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} className={({ isActive }) => `flex min-h-11 shrink-0 items-center gap-3 rounded-xl px-3.5 text-sm font-semibold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white ${isActive ? 'bg-white text-[#173d31] shadow-sm' : 'text-emerald-50/80 hover:bg-white/10 hover:text-white'}`}>
              <Icon className="h-[18px] w-[18px] shrink-0" aria-hidden="true" />
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="hidden border-t border-white/10 p-4 lg:block">
          <div className="flex items-center gap-3 rounded-xl bg-white/[0.07] p-3">
            <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-emerald-100 font-bold text-[#173d31]" aria-hidden="true">{account.displayName.trim().charAt(0).toUpperCase()}</span>
            <span className="min-w-0 flex-1">
              <span className="block truncate text-sm font-semibold">{account.displayName}</span>
              <span className="block text-xs text-emerald-100/65">Quản trị viên</span>
            </span>
          </div>
          <button onClick={leave} className="mt-2 flex min-h-10 w-full items-center gap-3 rounded-xl px-3 text-sm font-medium text-emerald-50/80 hover:bg-white/10 hover:text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white">
            <LogOut className="h-4 w-4" aria-hidden="true" /> Đăng xuất
          </button>
        </div>
      </aside>

      <div className="flex min-h-screen min-w-0 flex-col">
        <header className="sticky top-0 z-10 flex h-[76px] items-center justify-between border-b border-[#e5ebe4] bg-white/95 px-4 backdrop-blur sm:px-7 lg:px-10">
          <div className="min-w-0">
            <p className="hidden text-xs font-semibold text-ink-muted sm:block">Khu vực quản trị <span className="px-1.5 text-leaf-600">/</span> {pageTitle}</p>
            <p className="truncate text-sm font-bold text-ink sm:mt-1">Xin chào, {account.displayName}</p>
          </div>
          <div className="flex items-center gap-2 rounded-full border border-leaf-100 bg-leaf-50 px-3 py-1.5 text-xs font-bold text-leaf-700">
            <ShieldCheck className="h-4 w-4" aria-hidden="true" /> Administrator
          </div>
          <button onClick={leave} aria-label="Đăng xuất" title="Đăng xuất" className="ml-2 inline-flex h-10 w-10 items-center justify-center rounded-xl text-ink-muted hover:bg-red-50 hover:text-red-700 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-leaf-700 lg:hidden">
            <LogOut className="h-4 w-4" aria-hidden="true" />
          </button>
        </header>

        <main className="min-w-0 flex-1 px-4 py-6 sm:px-7 sm:py-8 lg:px-10 lg:py-9">{children}</main>
        <footer className="border-t border-[#e5ebe4] px-4 py-4 text-center text-xs text-ink-muted sm:px-7 lg:px-10 lg:text-left">Mâm Xanh · Quản trị danh mục và hồ sơ Chuyên gia</footer>
      </div>
    </div>
  );
}
