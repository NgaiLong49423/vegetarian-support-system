import { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { Bell, Menu, Plus, Search, X, ChevronDown } from 'lucide-react';
import { Logo } from './Logo';
import { Button } from './ui';
import { currentUser } from '../data/mockData';

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

  return (
    <header className="sticky top-0 z-40 border-b border-brand-100 bg-cream/85 backdrop-blur-md">
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-4 px-4 sm:px-6 lg:px-8">
        <Logo />

        <nav className="ml-2 hidden items-center gap-1 lg:flex">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) =>
                `rounded-full px-3.5 py-2 text-sm font-semibold transition-colors ${
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

        <div className="ml-auto flex items-center gap-2 sm:gap-3">
          <button
            onClick={() => navigate('/kham-pha')}
            className="hidden h-10 w-10 items-center justify-center rounded-full text-ink-soft transition-colors hover:bg-brand-100 md:flex"
            aria-label="Tìm kiếm"
          >
            <Search className="h-5 w-5" />
          </button>
          <button
            className="relative hidden h-10 w-10 items-center justify-center rounded-full text-ink-soft transition-colors hover:bg-brand-100 md:flex"
            aria-label="Thông báo"
          >
            <Bell className="h-5 w-5" />
            <span className="absolute right-2 top-2 h-2 w-2 rounded-full bg-brand-600 ring-2 ring-cream" />
          </button>

          <Button size="md" className="hidden sm:inline-flex" onClick={() => navigate('/dang-cong-thuc')}>
            <Plus className="h-4 w-4" />
            Đăng công thức
          </Button>

          <div className="relative hidden md:block">
            <button
              onClick={() => setMenuOpen((v) => !v)}
              className="flex items-center gap-2 rounded-full py-1 pl-1 pr-2 transition-colors hover:bg-brand-100/70"
            >
              <img src={currentUser.avatar} alt={currentUser.name} className="h-9 w-9 rounded-full object-cover ring-2 ring-brand-200" />
              <span className="text-sm font-semibold text-ink">{currentUser.name}</span>
              <ChevronDown className="h-4 w-4 text-ink-muted" />
            </button>
            {menuOpen && (
              <div className="absolute right-0 mt-2 w-52 overflow-hidden rounded-xl border border-brand-100 bg-white py-1 shadow-xl shadow-brand-900/10">
                <Link to="/ho-so" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-brand-50">
                  Hồ sơ của tôi
                </Link>
                <Link to="/ho-so" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-brand-50">
                  Công thức đã lưu
                </Link>
                <Link to="/ke-hoach" onClick={() => setMenuOpen(false)} className="block px-4 py-2.5 text-sm font-medium text-ink-soft hover:bg-brand-50">
                  Kế hoạch bữa ăn
                </Link>
                <div className="my-1 border-t border-brand-50" />
                <button className="block w-full px-4 py-2.5 text-left text-sm font-medium text-ink-muted hover:bg-brand-50">
                  Đăng xuất
                </button>
              </div>
            )}
          </div>

          <button
            onClick={() => setOpen((v) => !v)}
            className="flex h-10 w-10 items-center justify-center rounded-full text-ink-soft hover:bg-brand-100 lg:hidden"
            aria-label="Menu"
          >
            {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {open && (
        <div className="border-t border-brand-100 bg-cream px-4 py-3 lg:hidden">
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
            <Button className="mt-2" onClick={() => { setOpen(false); navigate('/dang-cong-thuc'); }}>
              <Plus className="h-4 w-4" />
              Đăng công thức
            </Button>
          </nav>
        </div>
      )}
    </header>
  );
}
