import { useAuth } from './AuthContext';
import { useEffect, useState, useRef } from 'react';
import { Link, NavLink, useNavigate, useLocation } from 'react-router-dom';
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
  UserCog,
  Award,
  Sparkles,
} from 'lucide-react';
import { Logo } from './Logo';
import { Button } from './ui';
import { countSaved, subscribeSaved } from '../lib/savedRecipes';

const navItems = [
  { to: '/kham-pha', label: 'Khám phá món chay' },
  { to: '/ke-hoach', label: 'Kế hoạch' },
  { to: '/di-cho', label: 'Danh sách đi chợ' },
  { to: '/dinh-duong', label: 'Theo dõi dinh dưỡng' },
];

const drawerNavItems = [
  { section: 'Khám phá', items: [
      { to: '/', label: 'Trang chủ', icon: Home, end: true },
      { to: '/kham-pha', label: 'Khám phá món chay', icon: Search, end: true },
    ]},
  { section: 'Kế hoạch & Dinh dưỡng', items: [
      { to: '/ke-hoach', label: 'Kế hoạch tuần', icon: Calendar, end: true },
      { to: '/di-cho', label: 'Danh sách đi chợ', icon: ShoppingCart, end: true },
      { to: '/dinh-duong', label: 'Theo dõi dinh dưỡng', icon: Activity, end: true },
    ]},
];

export function AppHeader() {
  const [open, setOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerRendered, setDrawerRendered] = useState(false);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [savedCount, setSavedCount] = useState(0);
  const navigate = useNavigate();
  const location = useLocation();
  const [hoveredNavIndex, setHoveredNavIndex] = useState<number | null>(null);

  // Hiệu ứng mượt mà khi mở và đóng Drawer
  useEffect(() => {
    if (drawerOpen) {
      setDrawerRendered(true);
      const raf = requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          setDrawerVisible(true);
        });
      });
      return () => cancelAnimationFrame(raf);
    } else {
      setDrawerVisible(false);
      const timer = setTimeout(() => {
        setDrawerRendered(false);
      }, 340);
      return () => clearTimeout(timer);
    }
  }, [drawerOpen]);

  const activeNavIndex = navItems.findIndex((item) =>
    location.pathname === item.to || location.pathname.startsWith(`${item.to}/`)
  );
  const targetNavIndex = hoveredNavIndex !== null ? hoveredNavIndex : activeNavIndex;

  const tabRefs = useRef<(HTMLAnchorElement | null)[]>([]);
  const [gliderStyle, setGliderStyle] = useState<{ left: number; width: number; opacity: number }>({
    left: 0,
    width: 0,
    opacity: 0,
  });

  useEffect(() => {
    const updateGlider = () => {
      if (targetNavIndex >= 0 && tabRefs.current[targetNavIndex]) {
        const el = tabRefs.current[targetNavIndex];
        if (el) {
          setGliderStyle({
            left: el.offsetLeft,
            width: el.offsetWidth,
            opacity: 1,
          });
        }
      } else {
        setGliderStyle((prev) => ({ ...prev, opacity: 0 }));
      }
    };

    updateGlider();
    const raf = requestAnimationFrame(updateGlider);
    const timer = setTimeout(updateGlider, 60);
    window.addEventListener('resize', updateGlider);
    return () => {
      cancelAnimationFrame(raf);
      clearTimeout(timer);
      window.removeEventListener('resize', updateGlider);
    };
  }, [targetNavIndex, location.pathname]);
  const { account, isAuthenticated: active, signOut } = useAuth();
  const role = account?.role;
  const displayName = account?.displayName ?? '';
  const accountLabel = account ? `Tài khoản ${account.displayName}` : 'Tài khoản';
  const leaveLabel = 'Đăng xuất';
  const avatar = account?.avatarUrl
    ? <img src={account.avatarUrl} alt="" className="h-9 w-9 rounded-full object-cover ring-2 ring-brand-200" />
    : <span aria-hidden="true" className="flex h-9 w-9 items-center justify-center rounded-full bg-brand-600 text-sm font-bold text-white ring-2 ring-brand-200">{displayName.trim().charAt(0).toUpperCase()}</span>;
  const leave = () => {
    signOut();
    setMenuOpen(false);
    setOpen(false);
    setDrawerOpen(false);
    navigate('/dang-nhap');
  };

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
        {/* ============ DRAWER BACKDROP & DRAWER ============ */}
        {drawerRendered && (
          <>
            <button
                type="button"
                aria-label="Đóng nền"
                className={`drawer-backdrop fixed inset-0 z-40 bg-ink/40 backdrop-blur-xs cursor-default ${
                  drawerVisible ? 'opacity-100' : 'opacity-0 pointer-events-none'
                }`}
                onClick={() => setDrawerOpen(false)}
            />

            <aside
                className={`drawer-panel fixed inset-y-0 left-0 z-50 flex w-72 max-w-[85vw] flex-col bg-white shadow-2xl ${
                  drawerVisible ? 'translate-x-0' : '-translate-x-full'
                }`}
            >
              <div className="flex items-center justify-between border-b border-brand-100 p-4">
                <Logo />
                <button
                    onClick={() => setDrawerOpen(false)}
                    className="flex h-9 w-9 items-center justify-center rounded-full text-ink-muted transition-all duration-200 hover:bg-brand-100 hover:text-brand-700 hover:rotate-90 active:scale-90"
                    aria-label="Đóng"
                >
                  <X className="h-5 w-5 transition-transform duration-200" />
                </button>
              </div>

              <nav className="drawer-nav-menu flex-1 overflow-y-auto p-3">
                {drawerNavItems.map((group) => (
                    <div key={group.section} className="mb-2">
                      <p className="px-3 py-2 text-[10px] font-bold uppercase tracking-widest text-ink-muted">
                        {group.section}
                      </p>
                      <div className="flex flex-col gap-0.5">
                        {group.items.map((item) => {
                          const Icon = item.icon;
                          return (
                              <NavLink
                                  key={item.to}
                                  to={item.to}
                                  end={item.end ?? true}
                                  onClick={() => setDrawerOpen(false)}
                                  className={({ isActive }) =>
                                      `drawer-nav-item flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all ${
                                          isActive
                                              ? 'is-active bg-brand-600 text-white shadow-sm'
                                              : 'text-ink-soft hover:bg-brand-50 hover:text-brand-700'
                                      }`
                                  }
                              >
                                <Icon className="h-4 w-4 shrink-0 transition-transform duration-300" />
                                <span className="flex-1">{item.label}</span>
                              </NavLink>
                          );
                        })}
                      </div>
                    </div>
                ))}

                {active ? (
                  <div className="mt-2 border-t border-brand-100 pt-2">
                    <p className="px-3 py-2 text-[10px] font-bold uppercase tracking-widest text-ink-muted">
                      Tài khoản ({displayName})
                    </p>
                    <div className="flex flex-col gap-0.5">
                      <NavLink
                        to="/ho-so"
                        end
                        onClick={() => setDrawerOpen(false)}
                        className={({ isActive }) =>
                          `drawer-nav-item flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all ${
                            isActive ? 'is-active bg-brand-600 text-white' : 'text-ink-soft hover:bg-brand-50 hover:text-brand-700'
                          }`
                        }
                      >
                        <Bookmark className="h-4 w-4 shrink-0" />
                        <span className="flex-1">Hồ sơ & Đã lưu</span>
                        {savedCount > 0 && (
                          <span className="inline-flex h-5 min-w-[20px] items-center justify-center rounded-full bg-brand-600 px-1.5 text-[10px] font-bold text-white">
                            {savedCount}
                          </span>
                        )}
                      </NavLink>
                      {role !== 'ADMIN' && (
                        <NavLink
                          to="/ho-so/cai-dat"
                          end
                          onClick={() => setDrawerOpen(false)}
                          className={({ isActive }) =>
                            `drawer-nav-item flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all ${
                              isActive ? 'is-active bg-brand-600 text-white' : 'text-ink-soft hover:bg-brand-50 hover:text-brand-700'
                            }`
                          }
                        >
                          <UserCog className="h-4 w-4 shrink-0" />
                          <span className="flex-1">Cài đặt hồ sơ</span>
                        </NavLink>
                      )}
                      <NavLink
                        to="/ho-so/dinh-duong"
                        end
                        onClick={() => setDrawerOpen(false)}
                        className={({ isActive }) =>
                          `drawer-nav-item flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all ${
                            isActive ? 'is-active bg-brand-600 text-white' : 'text-ink-soft hover:bg-brand-50 hover:text-brand-700'
                          }`
                        }
                      >
                        <Activity className="h-4 w-4 shrink-0" />
                        <span className="flex-1">Hồ sơ dinh dưỡng & BMI</span>
                      </NavLink>
                      <NavLink
                        to="/ho-so/so-thich-an-uong"
                        end
                        onClick={() => setDrawerOpen(false)}
                        className={({ isActive }) =>
                          `drawer-nav-item flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all ${
                            isActive ? 'is-active bg-brand-600 text-white' : 'text-ink-soft hover:bg-brand-50 hover:text-brand-700'
                          }`
                        }
                      >
                        <Sliders className="h-4 w-4 shrink-0" />
                        <span className="flex-1">Sở thích ăn uống</span>
                      </NavLink>
                      <NavLink
                        to="/goi-ai"
                        end
                        onClick={() => setDrawerOpen(false)}
                        className={({ isActive }) =>
                          `drawer-nav-item flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition-all ${
                            isActive ? 'is-active bg-brand-600 text-white' : 'text-brand-700 hover:bg-brand-50'
                          }`
                        }
                      >
                        <Sparkles className="h-4 w-4 shrink-0" />
                        <span className="flex-1">Nâng cấp gói AI</span>
                      </NavLink>
                      <NavLink
                        to="/giao-dich"
                        end
                        onClick={() => setDrawerOpen(false)}
                        className={({ isActive }) =>
                          `drawer-nav-item flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all ${
                            isActive ? 'is-active bg-brand-600 text-white' : 'text-ink-soft hover:bg-brand-50 hover:text-brand-700'
                          }`
                        }
                      >
                        <Award className="h-4 w-4 shrink-0" />
                        <span className="flex-1">Lịch sử giao dịch</span>
                      </NavLink>
                      {role === 'EXPERT' && (
                        <NavLink
                          to="/dang-cong-thuc"
                          end
                          onClick={() => setDrawerOpen(false)}
                          className={({ isActive }) =>
                            `drawer-nav-item flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition-all ${
                              isActive ? 'is-active bg-brand-600 text-white' : 'text-brand-700 hover:bg-brand-50'
                            }`
                          }
                        >
                          <Plus className="h-4 w-4 shrink-0" />
                          <span className="flex-1">Đăng công thức</span>
                        </NavLink>
                      )}
                      {role === 'CUSTOMER' && (
                        <NavLink
                          to="/dang-ky-chuyen-gia"
                          end
                          onClick={() => setDrawerOpen(false)}
                          className={({ isActive }) =>
                            `drawer-nav-item flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition-all ${
                              isActive ? 'is-active bg-leaf-600 text-white' : 'text-leaf-700 hover:bg-leaf-50'
                            }`
                          }
                        >
                          <Award className="h-4 w-4 shrink-0" />
                          <span className="flex-1">Đăng ký Chuyên gia</span>
                        </NavLink>
                      )}
                      {role === 'ADMIN' && (
                        <NavLink
                          to="/admin/xet-duyet-chuyen-gia"
                          end
                          onClick={() => setDrawerOpen(false)}
                          className={({ isActive }) =>
                            `drawer-nav-item flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition-all ${
                              isActive ? 'is-active bg-amber-700 text-white' : 'text-amber-800 hover:bg-amber-50'
                            }`
                          }
                        >
                          <Award className="h-4 w-4 shrink-0" />
                          <span className="flex-1">Xét duyệt Chuyên gia</span>
                        </NavLink>
                      )}
                      <button
                        onClick={() => {
                          leave();
                        }}
                        className="drawer-nav-item mt-1 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium text-ink-muted hover:bg-brand-50 transition-all"
                      >
                        {leaveLabel}
                      </button>
                    </div>
                  </div>
                ) : (
                  <div className="mt-2 border-t border-brand-100 pt-2">
                    <div className="flex flex-col gap-0.5">
                      <NavLink
                        to="/ho-so"
                        end
                        onClick={() => setDrawerOpen(false)}
                        className={({ isActive }) =>
                          `drawer-nav-item flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all ${
                            isActive ? 'is-active bg-brand-600 text-white' : 'text-ink-soft hover:bg-brand-50 hover:text-brand-700'
                          }`
                        }
                      >
                        <Bookmark className="h-4 w-4 shrink-0" />
                        <span className="flex-1">Công thức đã lưu</span>
                        {savedCount > 0 && (
                          <span className="inline-flex h-5 min-w-[20px] items-center justify-center rounded-full bg-brand-600 px-1.5 text-[10px] font-bold text-white">
                            {savedCount}
                          </span>
                        )}
                      </NavLink>
                    </div>
                    <div className="mt-2 flex flex-col gap-2 pt-2">
                      <Link
                        to="/dang-nhap"
                        onClick={() => setDrawerOpen(false)}
                        className="drawer-nav-item rounded-xl px-3 py-2 text-center text-sm font-semibold text-ink-soft hover:bg-brand-50 transition-all"
                      >
                        Đăng nhập
                      </Link>
                      <Link
                        to="/dang-ky"
                        onClick={() => setDrawerOpen(false)}
                        className="drawer-nav-item rounded-xl bg-brand-600 px-3 py-2 text-center text-sm font-semibold text-white hover:bg-brand-700 transition-all"
                      >
                        Đăng ký
                      </Link>
                    </div>
                  </div>
                )}
              </nav>

              <div className="border-t border-brand-100 p-4">
                <span className="inline-flex rounded-full bg-brand-100 px-2.5 py-1 text-[10px] font-bold text-brand-700">
                  Phiên bản 1.0
                </span>
                <p className="mt-2 text-[11px] leading-relaxed text-ink-muted">
                  © 2025 Mâm Xanh · Nuôi dưỡng lối sống xanh lành
                </p>
              </div>
            </aside>
          </>
        )}

        {/* ============ HEADER ============ */}
        <header className="sticky top-0 z-30 border-b-2 border-brand-300/85 bg-cream/90 backdrop-blur-md shadow-2xs">
          <div className="mx-auto flex h-16 max-w-screen-2xl items-center gap-3 px-4 sm:px-6 lg:px-8">
            {/* Hamburger & Logo */}
            <div className="flex shrink-0 items-center gap-2 sm:gap-3">
              <button
                onClick={() => setDrawerOpen(true)}
                className="menu-btn-animated group relative flex h-10 sm:h-11 items-center justify-center sm:justify-start gap-2 sm:gap-2.5 rounded-xl border border-brand-200/80 bg-white/85 px-2.5 sm:px-3.5 text-ink-soft shadow-2xs backdrop-blur-xs transition-all hover:border-brand-300 hover:bg-brand-50/90 hover:text-brand-700 hover:shadow-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
                aria-label="Menu"
              >
                <div className="flex h-4.5 w-5 flex-col items-start justify-center gap-1 overflow-hidden" aria-hidden="true">
                  <span className="menu-btn-line block h-0.5 w-4.5 rounded-full bg-current group-hover:w-5 group-hover:translate-x-0.5" />
                  <span className="menu-btn-line block h-0.5 w-3.5 rounded-full bg-current group-hover:w-5 group-hover:translate-x-1" />
                  <span className="menu-btn-line block h-0.5 w-4.5 rounded-full bg-current group-hover:w-5 group-hover:translate-x-0.5" />
                </div>
                <span className="hidden sm:inline text-xs font-bold tracking-wide uppercase text-ink-soft group-hover:text-brand-700 transition-colors">
                  Menu
                </span>
              </button>
              <Logo />
            </div>

            {/* Desktop Navigation: Căn giữa hoàn hảo với hiệu ứng Glass Pill Glider thích ứng */}
            <div className="hidden min-[1100px]:flex flex-1 items-center justify-center px-2">
              <nav
                className="glass-nav-group relative flex items-center"
                onMouseLeave={() => setHoveredNavIndex(null)}
              >
                <div
                  className="glass-nav-glider"
                  style={{
                    transform: `translateX(${gliderStyle.left}px)`,
                    width: `${gliderStyle.width}px`,
                    opacity: gliderStyle.opacity,
                    pointerEvents: 'none',
                  }}
                />
                {navItems.map((item, idx) => {
                  const isCurrent = targetNavIndex === idx;
                  return (
                    <NavLink
                      key={item.to}
                      to={item.to}
                      ref={(el) => {
                        tabRefs.current[idx] = el;
                      }}
                      onMouseEnter={() => setHoveredNavIndex(idx)}
                      className={`glass-nav-link ${isCurrent ? 'is-active' : ''}`}
                    >
                      {item.label}
                    </NavLink>
                  );
                })}
              </nav>
            </div>

            <div className="ml-auto flex shrink-0 items-center gap-2 min-[1100px]:ml-0">
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
                        aria-label={accountLabel}
                        aria-expanded={menuOpen}
                        aria-haspopup="menu"
                        className="flex items-center gap-2 whitespace-nowrap rounded-full py-1 pl-1 pr-2 transition-colors hover:bg-brand-100/70"
                    >
                      {avatar}
                      <span className="text-left leading-tight">
                    <span className="block text-sm font-semibold text-ink">{displayName}</span>
                    <span className="block text-xs text-ink-muted">
                      {role === 'EXPERT' ? 'Chuyên gia' : role === 'ADMIN' ? 'Quản trị viên' : 'Thành viên'}
                    </span>
                  </span>
                      <ChevronDown className="h-4 w-4 text-ink-muted" />
                    </button>

                    {menuOpen && (
                        <div className="absolute right-0 mt-2 w-64 overflow-hidden rounded-xl border border-brand-100 bg-white py-1 shadow-xl shadow-brand-900/10">
                          <div className="border-b border-brand-50 px-4 py-3">
                            <p className="font-bold text-ink">{displayName}</p>
                            <p className="text-xs font-semibold text-leaf-700">
                              Vai trò:{' '}
                              {role === 'EXPERT'
                                  ? 'Chuyên gia (EXPERT)'
                                  : role === 'ADMIN'
                                      ? 'Quản trị viên (ADMIN)'
                                      : 'Khách hàng (CUSTOMER)'}
                            </p>
                            <p className="text-xs text-ink-muted">{account?.email}</p>
                          </div>

                          <Link
                              to="/ho-so"
                              onClick={() => setMenuOpen(false)}
                              className="block px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-brand-50"
                          >
                            Hồ sơ của tôi
                          </Link>
                          {role !== 'ADMIN' && (
                              <Link
                                  to="/ho-so/cai-dat"
                                  onClick={() => setMenuOpen(false)}
                                  className="block px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-brand-50"
                              >
                                Cài đặt hồ sơ
                              </Link>
                          )}

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
                              to="/ho-so/so-thich-an-uong"
                              onClick={() => setMenuOpen(false)}
                              className="block px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-brand-50"
                          >
                            Sở thích ăn uống
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
                                leave();
                              }}
                              className="block w-full px-4 py-2.5 text-left text-sm font-medium text-ink-muted hover:bg-brand-50"
                          >
                            {leaveLabel}
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
