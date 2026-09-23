import { useDemoAccount } from './DemoAccount';
import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { Bell, Menu, Plus, Search, X, ChevronDown } from 'lucide-react';
import { Logo } from './Logo';
import { Button } from './ui';
import { currentUser, demoAiPlan } from '../data/mockData';

const navItems = [
  { to: '/kham-pha', label: 'Khám phá món chay' },
  { to: '/ke-hoach', label: 'Kế hoạch' },
  { to: '/di-cho', label: 'Danh sách đi chợ' },
  { to: '/dinh-duong', label: 'Theo dõi dinh dưỡng' },
];

export function AppHeader() {
  const [open, setOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const navigate = useNavigate();
  const { active, setActive } = useDemoAccount();

  return (
    <header className="sticky top-0 z-40 border-b border-brand-100 bg-cream/85 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-screen-2xl items-center gap-3 px-4 sm:px-6 lg:px-8">
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
          {active && <button
            className="relative hidden h-10 w-10 items-center justify-center rounded-full text-ink-soft transition-colors hover:bg-brand-100 md:flex"
            aria-label="Thông báo"
          >
            <Bell className="h-5 w-5" />
            <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-brand-600 ring-2 ring-cream" />
          </button>}

          {active && <div className="hidden sm:block">
            <Button size="md" className="whitespace-nowrap" onClick={() => navigate('/dang-cong-thuc')}>
              <Plus className="h-4 w-4" />
              Đăng công thức
            </Button>
          </div>}

          {!active && <div className="flex items-center gap-2 whitespace-nowrap">
            <Link to="/dang-nhap" className="rounded-xl px-2 py-2 text-sm font-semibold text-ink-soft hover:bg-brand-50 sm:px-4">Đăng nhập</Link>
            <Link to="/dang-ky" className="hidden rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-brand-700 sm:block">Đăng ký</Link>
          </div>}
          {active && <div className="relative hidden md:block">
            <button
              onClick={() => setMenuOpen((v) => !v)}
              aria-label={`Tài khoản ${currentUser.name}, gói AI ${demoAiPlan} demo`}
              aria-expanded={menuOpen}
              aria-haspopup="menu"
              className="flex items-center gap-2 whitespace-nowrap rounded-full py-1 pl-1 pr-2 transition-colors hover:bg-brand-100/70"
            >
              <img src={currentUser.avatar} alt={currentUser.name} className="h-9 w-9 rounded-full object-cover ring-2 ring-brand-200" />
              <span className="text-left leading-tight">
                <span className="block text-sm font-semibold text-ink">{currentUser.name}</span>
                <span className="block text-xs text-ink-muted">{demoAiPlan} · demo</span>
              </span>
              <ChevronDown className="h-4 w-4 text-ink-muted" />
            </button>
            {menuOpen && (
              <div className="absolute right-0 mt-2 w-64 overflow-hidden rounded-xl border border-brand-100 bg-white py-1 shadow-xl shadow-brand-900/10">
                <div className="border-b border-brand-50 px-4 py-3">
                  <p className="font-bold text-ink">{currentUser.name}</p>
                  <p className="text-xs text-ink-muted">Gói AI hiện tại: {demoAiPlan} (dữ liệu demo)</p>
                </div>
                <Link to="/ho-so" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-brand-50">
                  Hồ sơ của tôi
                </Link>
                <Link to="/ho-so/dinh-duong" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-brand-50">
                  Hồ sơ dinh dưỡng & BMI
                </Link>
                <Link to="/goi-ai" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm font-semibold text-brand-700 hover:bg-brand-50">
                  Nâng cấp gói AI
                </Link>
                <Link to="/giao-dich" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-brand-50">
                  Lịch sử giao dịch
                </Link>
                <Link to="/ho-so" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-brand-50">
                  Công thức đã lưu
                </Link>
                <Link to="/ke-hoach" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-brand-50">
                  Kế hoạch bữa ăn
                </Link>
                <div className="my-1 border-t border-brand-50" />
                <button onClick={() => { setActive(false); setMenuOpen(false); navigate('/dang-nhap'); }} className="block w-full px-4 py-2.5 text-left text-sm font-medium text-ink-muted hover:bg-brand-50">
                  Thoát tài khoản demo
                </button>
              </div>
            )}
          </div>}

          <button
            onClick={() => setOpen((v) => !v)}
            className="flex h-10 w-10 items-center justify-center rounded-full text-ink-soft hover:bg-brand-100 min-[1200px]:hidden"
            aria-label="Menu"
          >
            {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {open && (
        <div className="border-t border-brand-100 bg-cream px-4 py-3 min-[1200px]:hidden">
          <nav className="flex flex-col gap-1">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={() => setOpen(false)}
                className={({ isActive }) =>
                  `rounded-lg px-3 py-2.5 text-sm font-semibold ${
                    isActive ? 'bg-brand-600 text-white' : 'text-ink-soft hover:bg-brand-100'
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
            {active ? <><div className="my-1 border-t border-brand-100 px-3 py-2 text-xs text-ink-muted">
              {currentUser.name} · Gói AI {demoAiPlan} (demo)
            </div>
            <Link to="/ho-so" onClick={() => setOpen(false)} className="rounded-lg px-3 py-2.5 text-sm font-semibold text-ink-soft hover:bg-brand-100">Hồ sơ của tôi</Link>
            <Link to="/ho-so/dinh-duong" onClick={() => setOpen(false)} className="rounded-lg px-3 py-2.5 text-sm font-semibold text-ink-soft hover:bg-brand-100">Hồ sơ dinh dưỡng & BMI</Link>
            <Link to="/goi-ai" onClick={() => setOpen(false)} className="rounded-lg px-3 py-2.5 text-sm font-semibold text-brand-700 hover:bg-brand-100">Nâng cấp gói AI</Link>
            <Link to="/giao-dich" onClick={() => setOpen(false)} className="rounded-lg px-3 py-2.5 text-sm font-semibold text-ink-soft hover:bg-brand-100">Lịch sử giao dịch</Link>
            <Button className="mt-2" onClick={() => { setOpen(false); navigate('/dang-cong-thuc'); }}>
              <Plus className="h-4 w-4" />
              Đăng công thức
            </Button>
            <button onClick={() => { setActive(false); setOpen(false); navigate('/dang-nhap'); }} className="rounded-lg px-3 py-2.5 text-left text-sm text-ink-muted">Thoát tài khoản demo</button>
            </> : <div className="mt-2 flex gap-3 border-t border-brand-100 pt-3">
              <Link to="/dang-nhap" onClick={() => setOpen(false)} className="rounded-xl px-4 py-2 text-sm font-semibold text-ink">Đăng nhập</Link>
              <Link to="/dang-ky" onClick={() => setOpen(false)} className="rounded-xl bg-brand-600 px-4 py-2 text-sm font-semibold text-white">Đăng ký</Link>
            </div>}
          </nav>
        </div>
      )}
    </header>
  );
}
