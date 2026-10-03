import { useDemoAccount } from './DemoAccount';
import { useEffect, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import {
  Bell,
  Menu,
  Plus,
  Search,
  X,
  ChevronDown,
  Home,
  Bookmark,
  Calendar,
  ShoppingCart,
  Activity,
  Sliders,
  Award,
} from 'lucide-react';
import { Logo } from './Logo';
import { Button } from './ui';
import { currentUser, demoAiPlan } from '../data/mockData';
import { countSaved, subscribeSaved } from '../lib/savedRecipes';

const navItems = [
  { to: '/kham-pha', label: 'Khám phá món chay' },
  { to: '/ke-hoach', label: 'Kế hoạch' },
  { to: '/di-cho', label: 'Danh sách đi chợ' },
  { to: '/dinh-duong', label: 'Theo dõi dinh dưỡng' },
];

const drawerNavItems = [
  { section: 'Khám phá', items: [
      { to: '/', label: 'Trang chủ', icon: Home },
      { to: '/kham-pha', label: 'Khám phá món chay', icon: Search },
      { to: '/ho-so', label: 'Đã lưu', icon: Bookmark, hasBadge: true },
    ]},
  { section: 'Kế hoạch & Dinh dưỡng', items: [
      { to: '/ke-hoach', label: 'Kế hoạch tuần', icon: Calendar },
      { to: '/di-cho', label: 'Danh sách đi chợ', icon: ShoppingCart },
      { to: '/dinh-duong', label: 'Theo dõi dinh dưỡng', icon: Activity },
    ]},
  { section: 'Cá nhân', items: [
      { to: '/ho-so', label: 'Sở thích ăn uống', icon: Sliders },
      { to: '/dang-ky-chuyen-gia', label: 'Đăng ký Chuyên gia', icon: Award },
    ]},
];

export function AppHeader() {
  const [open, setOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [savedCount, setSavedCount] = useState(0);
  const navigate = useNavigate();
  const { active, setActive, role, setRole } = useDemoAccount();

  // Cập nhật badge số lượng đã lưu
  useEffect(() => {
    setSavedCount(countSaved());
    const unsub = subscribeSaved(() => setSavedCount(countSaved()));
    return unsub;
  }, []);

  // Đóng drawer khi nhấn ESC
  useEffect(() => {
    if (!drawerOpen) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setDrawerOpen(false);
    };
    document.addEventListener('keydown', onKey);
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', onKey);
      document.body.style.overflow = '';
    };
  }, [drawerOpen]);

  return (
      <>
        {/* ============ DRAWER BACKDROP ============ */}
        <div
            className={`fixed inset-0 z-40 bg-ink/40 backdrop-blur-sm transition-opacity ${
                drawerOpen ? 'opacity-100' : 'pointer-events-none opacity-0'
            }`}
            onClick={() => setDrawerOpen(false)}
        />

        {/* ============ DRAWER ============ */}
        <aside
            className={`fixed inset-y-0 left-0 z-50 flex w-72 max-w-[85vw] flex-col bg-white shadow-2xl transition-transform duration-300 ${
                drawerOpen ? 'translate-x-0' : '-translate-x-full'
            }`}
        >
          <div className="flex items-center justify-between border-b border-brand-100 p-4">
            <Logo />
            <button
                onClick={() => setDrawerOpen(false)}
                className="flex h-9 w-9 items-center justify-center rounded-full text-ink-muted transition-colors hover:bg-brand-100 hover:text-brand-700"
                aria-label="Đóng menu"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <nav className="flex-1 overflow-y-auto p-3">
            {drawerNavItems.map((group) => (
                <div key={group.section} className="mb-2">
                  <p className="px-3 py-2 text-[10px] font-bold uppercase tracking-widest text-ink-muted">
                    {group.section}
                  </p>
                  {group.items.map((item) => {
                    const Icon = item.icon;
                    return (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            onClick={() => setDrawerOpen(false)}
                            className={({ isActive }) =>
                                `flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors ${
                                    isActive
                                        ? 'bg-brand-600 text-white'
                                        : 'text-ink-soft hover:bg-brand-50 hover:text-brand-700'
                                }`
                            }
                        >
                          <Icon className="h-4 w-4 shrink-0" />
                          <span className="flex-1">{item.label}</span>
                          {item.hasBadge && savedCount > 0 && (
                              <span className="inline-flex h-5 min-w-[20px] items-center justify-center rounded-full bg-brand-600 px-1.5 text-[10px] font-bold text-white">
                        {savedCount}
                      </span>
                          )}
                        </NavLink>
                    );
                  })}
                </div>
            ))}
          </nav>

          <div className="border-t border-brand-100 p-4">
          <span className="inline-flex rounded-full bg-brand-100 px-2.5 py-1 text-[10px] font-bold text-brand-700">
            Phiên bản demo
          </span>
            <p className="mt-2 text-[11px] leading-relaxed text-ink-muted">
              © 2025 Mâm Xanh · Nuôi dưỡng lối sống xanh lành
            </p>
          </div>
        </aside>

        {/* ============ HEADER ============ */}
        <header className="sticky top-0 z-30 border-b border-brand-100 bg-cream/85 backdrop-blur-md">
          <div className="mx-auto flex h-16 max-w-screen-2xl items-center gap-3 px-4 sm:px-6 lg:px-8">
            {/* Hamburger — luôn hiện ở mọi kích thước màn hình */}
            <button
                onClick={() => setDrawerOpen(true)}
                className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-ink-soft transition-colors hover:bg-brand-100"
                aria-label="Mở menu"
            >
              <Menu className="h-5 w-5" />
            </button>

            <Logo />

            <nav className="ml-1 hidden min-w-0 items-center gap-0.5 min-[1200px]:flex">
              {navItems.map((item) => (
                  <NavLink
                      key={item.to}
                      to={item.to}
                      className={({ isActive }) =>
                          `whitespace-nowrap rounded-full px-2.5 py-2 text-sm font-semibold transition-colors ${
                              isActive
                                  ? 'bg-brand-600 text-white'
                                  : 'text-ink-soft hover:bg-brand-100/70 hover:text-brand-700'
                          }`
                      }
                  >
                    {item.label}
                  </NavLink>
              ))}
            </nav>

            <div className="ml-auto flex shrink-0 items-center gap-2">
              <button
                  onClick={() => navigate('/kham-pha')}
                  className="hidden h-10 w-10 items-center justify-center rounded-full text-ink-soft transition-colors hover:bg-brand-100 md:flex"
                  aria-label="Tìm kiếm"
              >
                <Search className="h-5 w-5" />
              </button>
              {active && (
                  <button
                      className="relative hidden h-10 w-10 items-center justify-center rounded-full text-ink-soft transition-colors hover:bg-brand-100 md:flex"
                      aria-label="Thông báo"
                  >
                    <Bell className="h-5 w-5" />
                    <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-brand-600 ring-2 ring-cream" />
                  </button>
              )}

              {active && role === 'EXPERT' && (
                  <div className="hidden sm:block">
                    <Button size="md" className="whitespace-nowrap" onClick={() => navigate('/dang-cong-thuc')}>
                      <Plus className="h-4 w-4" />
                      Đăng công thức
                    </Button>
                  </div>
              )}

              {active && role === 'CUSTOMER' && (
                  <div className="hidden sm:block">
                    <Button
                        variant="outline"
                        size="md"
                        className="whitespace-nowrap border-leaf-400 text-leaf-700 hover:bg-leaf-50"
                        onClick={() => navigate('/dang-ky-chuyen-gia')}
                    >
                      Đăng ký Chuyên gia
                    </Button>
                  </div>
              )}

              {active && role === 'ADMIN' && (
                  <div className="hidden sm:block">
                    <Button
                        variant="outline"
                        size="md"
                        className="whitespace-nowrap border-amber-400 text-amber-800 hover:bg-amber-50"
                        onClick={() => navigate('/admin/xet-duyet-chuyen-gia')}
                    >
                      Xét duyệt Chuyên gia
                    </Button>
                  </div>
              )}

              {!active && (
                  <div className="flex items-center gap-2 whitespace-nowrap">
                    <Link
                        to="/dang-nhap"
                        className="rounded-xl px-2 py-2 text-sm font-semibold text-ink-soft hover:bg-brand-50 sm:px-4"
                    >
                      Đăng nhập
                    </Link>
                    <Link
                        to="/dang-ky"
                        className="hidden rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-700 sm:block"
                    >
                      Đăng ký
                    </Link>
                  </div>
              )}

              {active && (
                  <div className="relative hidden md:block">
                    <button
                        onClick={() => setMenuOpen((v) => !v)}
                        aria-label={`Tài khoản ${currentUser.name}, gói AI ${demoAiPlan} demo`}
                        aria-expanded={menuOpen}
                        aria-haspopup="menu"
                        className="flex items-center gap-2 whitespace-nowrap rounded-full py-1 pl-1 pr-2 transition-colors hover:bg-brand-100/70"
                    >
                      <img
                          src={currentUser.avatar}
                          alt={currentUser.name}
                          className="h-9 w-9 rounded-full object-cover ring-2 ring-brand-200"
                      />
                      <span className="text-left leading-tight">
                    <span className="block text-sm font-semibold text-ink">{currentUser.name}</span>
                    <span className="block text-xs text-ink-muted">
                      {role === 'EXPERT' ? 'Chuyên gia' : role === 'ADMIN' ? 'Quản trị viên' : 'Thành viên'} · demo
                    </span>
                  </span>
                      <ChevronDown className="h-4 w-4 text-ink-muted" />
                    </button>

                    {menuOpen && (
                        <div className="absolute right-0 mt-2 w-64 overflow-hidden rounded-xl border border-brand-100 bg-white py-1 shadow-xl shadow-brand-900/10">
                          <div className="border-b border-brand-50 px-4 py-3">
                            <p className="font-bold text-ink">{currentUser.name}</p>
                            <p className="text-xs font-semibold text-leaf-700">
                              Vai trò:{' '}
                              {role === 'EXPERT'
                                  ? 'Chuyên gia (EXPERT)'
                                  : role === 'ADMIN'
                                      ? 'Quản trị viên (ADMIN)'
                                      : 'Khách hàng (CUSTOMER)'}
                            </p>
                            <p className="text-xs text-ink-muted">Gói AI hiện tại: {demoAiPlan} (dữ liệu demo)</p>
                          </div>

                          <div className="border-b border-brand-50 bg-brand-50/40 px-4 py-2 text-xs">
                            <span className="font-bold text-ink-soft">Chuyển vai trò demo:</span>
                            <div className="mt-1.5 flex gap-1">
                              <button
                                  type="button"
                                  onClick={() => setRole('CUSTOMER')}
                                  className={`rounded px-2 py-0.5 font-semibold ${
                                      role === 'CUSTOMER'
                                          ? 'bg-brand-600 text-white'
                                          : 'border border-brand-200 bg-white text-ink-soft'
                                  }`}
                              >
                                Customer
                              </button>
                              <button
                                  type="button"
                                  onClick={() => setRole('EXPERT')}
                                  className={`rounded px-2 py-0.5 font-semibold ${
                                      role === 'EXPERT'
                                          ? 'bg-brand-600 text-white'
                                          : 'border border-brand-200 bg-white text-ink-soft'
                                  }`}
                              >
                                Expert
                              </button>
                              <button
                                  type="button"
                                  onClick={() => setRole('ADMIN')}
                                  className={`rounded px-2 py-0.5 font-semibold ${
                                      role === 'ADMIN'
                                          ? 'bg-brand-600 text-white'
                                          : 'border border-brand-200 bg-white text-ink-soft'
                                  }`}
                              >
                                Admin
                              </button>
                            </div>
                          </div>

                          <Link
                              to="/ho-so"
                              onClick={() => setMenuOpen(false)}
                              className="block px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-brand-50"
                          >
                            Hồ sơ của tôi
                          </Link>

                          {role === 'CUSTOMER' && (
                              <Link
                                  to="/dang-ky-chuyen-gia"
                                  onClick={() => setMenuOpen(false)}
                                  className="block px-4 py-2.5 text-sm font-semibold text-leaf-700 hover:bg-leaf-50"
                              >
                                Đăng ký trở thành Chuyên gia
                              </Link>
                          )}

                          {role === 'EXPERT' && (
                              <Link
                                  to="/dang-cong-thuc"
                                  onClick={() => setMenuOpen(false)}
                                  className="block px-4 py-2.5 text-sm font-semibold text-brand-700 hover:bg-brand-50"
                              >
                                Đăng công thức mới
                              </Link>
                          )}

                          {role === 'ADMIN' && (
                              <Link
                                  to="/admin/xet-duyet-chuyen-gia"
                                  onClick={() => setMenuOpen(false)}
                                  className="block px-4 py-2.5 text-sm font-semibold text-amber-800 hover:bg-amber-50"
                              >
                                Xét duyệt Chuyên gia
                              </Link>
                          )}

                          <Link
                              to="/ho-so/dinh-duong"
                              onClick={() => setMenuOpen(false)}
                              className="block px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-brand-50"
                          >
                            Hồ sơ dinh dưỡng & BMI
                          </Link>
                          <Link
                              to="/goi-ai"
                              onClick={() => setMenuOpen(false)}
                              className="block px-4 py-2.5 text-sm font-semibold text-brand-700 hover:bg-brand-50"
                          >
                            Nâng cấp gói AI
                          </Link>
                          <Link
                              to="/giao-dich"
                              onClick={() => setMenuOpen(false)}
                              className="block px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-brand-50"
                          >
                            Lịch sử giao dịch
                          </Link>
                          <div className="my-1 border-t border-brand-50" />
                          <button
                              onClick={() => {
                                setActive(false);
                                setMenuOpen(false);
                                navigate('/dang-nhap');
                              }}
                              className="block w-full px-4 py-2.5 text-left text-sm font-medium text-ink-muted hover:bg-brand-50"
                          >
                            Thoát tài khoản demo
                          </button>
                        </div>
                    )}
                  </div>
              )}
            </div>
          </div>
        </header>
      </>
  );
}