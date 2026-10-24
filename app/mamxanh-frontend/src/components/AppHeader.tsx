import { useEffect, useRef, useState } from "react"
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom"
import {
  Activity,
  Award,
  Bookmark,
  CalendarDays,
  ChevronDown,
  LayoutDashboard,
  LayoutGrid,
  LogOut,
  MessageCircle,
  Plus,
  Search,
  ShoppingBasket,
  SlidersHorizontal,
  Sparkles,
  Sun,
  Moon,
  UserRound,
  X,
} from "lucide-react"
import { useAuth } from "./AuthContext"
import { NotificationBell } from "./NotificationBell"
import { Logo } from "./Logo"
import { useTheme } from "./ThemeContext"
import { useAiChat } from "./AiChatContext"

const navigation = [
  { to: "/trang-chu", label: "Trang chủ", icon: LayoutGrid },
  { to: "/kham-pha", label: "Khám phá", icon: Search },
  { to: "/ho-so", label: "Đã lưu", icon: Bookmark },
  { to: "/ke-hoach", label: "Lịch ăn", icon: CalendarDays },
  { to: "/di-cho", label: "Danh sách mua sắm", icon: ShoppingBasket },
]

const exploreLinks = [
  { to: "/kham-pha", label: "Công thức", icon: Search },
  { to: "/danh-muc-nguyen-lieu", label: "Danh mục nguyên liệu", icon: LayoutGrid },
  { to: "/chuyen-gia", label: "Chuyên gia", icon: Award },
]

export function AppHeader() {
  const { account, isAuthenticated, signOut } = useAuth()
  const { openChat } = useAiChat()
  const { theme, setTheme } = useTheme()
  const navigate = useNavigate()
  const location = useLocation()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const [exploreMenuOpen, setExploreMenuOpen] = useState(false)
  const [exploreMobileOpen, setExploreMobileOpen] = useState(() =>
    ["/kham-pha", "/danh-muc-nguyen-lieu", "/chuyen-gia"].includes(location.pathname),
  )
  const [query, setQuery] = useState("")
  const drawer = useRef<HTMLDialogElement>(null)
  const accountMenu = useRef<HTMLDivElement>(null)
  const accountTrigger = useRef<HTMLButtonElement>(null)
  const exploreMenu = useRef<HTMLDivElement>(null)
  const exploreTrigger = useRef<HTMLButtonElement>(null)
  const navigationGroup = useRef<HTMLElement>(null)
  const role = account?.role
  const personalLinks = [
    ...(role === "ADMIN"
      ? [
          {
            to: "/admin",
            label: "Trung tâm quản trị (Dashboard)",
            icon: LayoutDashboard,
          },
          {
            to: "/admin/xet-duyet-chuyen-gia",
            label: "Xét duyệt Chuyên gia",
            icon: Award,
          },
          {
            to: "/quan-tri/danh-muc",
            label: "Danh mục nguyên liệu",
            icon: LayoutGrid,
          },
        ]
      : isAuthenticated && account
        ? [
            { to: "/ho-so", label: "Khu vực của tôi", icon: Bookmark },
            { to: `/thanh-vien/${account.id}`, label: "Trang cá nhân", icon: UserRound },
            ...(role === "EXPERT"
              ? [
                  {
                    to: "/ho-so/chuyen-gia",
                    label: "Bài công thức của tôi",
                    icon: LayoutGrid,
                  },
                  { to: "/dang-cong-thuc", label: "Đăng công thức mới", icon: Plus },
                ]
              : []),
            {
              to: "/ho-so/dinh-duong",
              label: "Hồ sơ dinh dưỡng & BMI",
              icon: Activity,
            },
            {
              to: "/ho-so/so-thich-an-uong",
              label: "Sở thích ăn uống",
              icon: SlidersHorizontal,
            },
            { to: "/dinh-duong", label: "Theo dõi dinh dưỡng", icon: Activity },
            { to: "/goi-ai", label: "Nâng cấp gói AI", icon: Sparkles },
            { to: "/giao-dich", label: "Lịch sử giao dịch", icon: Award },
            ...(role === "CUSTOMER"
              ? [
                  {
                    to: "/dang-ky-chuyen-gia",
                    label: "Đăng ký trở thành Chuyên gia",
                    icon: Award,
                  },
                ]
              : []),
          ]
        : []),
  ]

  useEffect(() => {
    setDrawerOpen(false)
    setMenuOpen(false)
    setExploreMenuOpen(false)
    setExploreMobileOpen(["/kham-pha", "/danh-muc-nguyen-lieu", "/chuyen-gia"].includes(location.pathname))
  }, [location.pathname, location.search])

  useEffect(() => {
    const dialog = drawer.current
    if (!dialog) return
    if (drawerOpen && !dialog.open) dialog.showModal()
    if (!drawerOpen && dialog.open) dialog.close()
    if (!drawerOpen) return
    const originalOverflow = document.body.style.overflow
    document.body.style.overflow = "hidden"
    return () => {
      document.body.style.overflow = originalOverflow
    }
  }, [drawerOpen])

  useEffect(() => {
    if (!menuOpen) return
    const dismiss = (event: PointerEvent) => {
      if (!accountMenu.current?.contains(event.target as Node))
        setMenuOpen(false)
    }
    const escape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setMenuOpen(false)
        accountTrigger.current?.focus()
      }
    }
    document.addEventListener("pointerdown", dismiss)
    document.addEventListener("keydown", escape)
    return () => {
      document.removeEventListener("pointerdown", dismiss)
      document.removeEventListener("keydown", escape)
    }
  }, [menuOpen])

  useEffect(() => {
    if (!exploreMenuOpen) return
    const dismiss = (event: PointerEvent) => {
      if (!exploreMenu.current?.contains(event.target as Node)) setExploreMenuOpen(false)
    }
    const escape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setExploreMenuOpen(false)
        exploreTrigger.current?.focus()
      }
    }
    document.addEventListener("pointerdown", dismiss)
    document.addEventListener("keydown", escape)
    return () => {
      document.removeEventListener("pointerdown", dismiss)
      document.removeEventListener("keydown", escape)
    }
  }, [exploreMenuOpen])

  const [hoveredNavIndex, setHoveredNavIndex] = useState<number | null>(null)
  const tabRefs = useRef<(HTMLElement | null)[]>([])
  const [gliderStyle, setGliderStyle] = useState<{
    left: number
    width: number
    opacity: number
  }>({
    left: 0,
    width: 0,
    opacity: 0,
  })

  const isExploreRoute = exploreLinks.some(({ to }) => location.pathname === to || location.pathname.startsWith(`${to}/`))
  const activeNavIndex = navigation.findIndex((item) => item.to === "/kham-pha"
    ? isExploreRoute
    : item.to === "/trang-chu"
      ? location.pathname === item.to
      : location.pathname === item.to || location.pathname.startsWith(`${item.to}/`))
  const targetNavIndex =
    hoveredNavIndex !== null ? hoveredNavIndex : activeNavIndex

  useEffect(() => {
    const updateGlider = () => {
      if (targetNavIndex >= 0 && tabRefs.current[targetNavIndex]) {
        const el = tabRefs.current[targetNavIndex]
        if (el) {
          const group = navigationGroup.current
          const itemBounds = el.getBoundingClientRect()
          const groupBounds = group?.getBoundingClientRect()
          setGliderStyle({
            left: groupBounds
              ? itemBounds.left - groupBounds.left - (group?.clientLeft ?? 0)
              : el.offsetLeft,
            width: itemBounds.width,
            opacity: 1,
          })
        }
      } else {
        setGliderStyle((prev) => ({ ...prev, opacity: 0 }))
      }
    }

    updateGlider()
    const raf = requestAnimationFrame(updateGlider)
    const timer = setTimeout(updateGlider, 60)
    window.addEventListener("resize", updateGlider)
    return () => {
      cancelAnimationFrame(raf)
      clearTimeout(timer)
      window.removeEventListener("resize", updateGlider)
    }
  }, [targetNavIndex, location.pathname])

  const leave = () => {
    signOut()
    setDrawerOpen(false)
    setMenuOpen(false)
    navigate("/dang-nhap")
  }
  const avatar = account?.avatarUrl ? (
    <img
      src={account.avatarUrl}
      alt=""
      className="h-8 w-8 rounded-full object-cover"
    />
  ) : (
    <span
      className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 text-xs font-bold text-brand-600"
      aria-hidden="true"
    >
      {account?.displayName.trim().charAt(0).toUpperCase()}
    </span>
  )

  return (
    <>
      <header className="sticky top-0 z-30 border-b border-brand-100 bg-cream-soft/95 backdrop-blur-md">
        <div className="mx-auto flex h-16 max-w-[1344px] items-center gap-3 px-4 sm:gap-5 sm:px-6">
          <button
            type="button"
            aria-label="Menu"
            aria-expanded={drawerOpen}
            onClick={() => setDrawerOpen(true)}
            className="menu-btn-animated group relative flex h-10 shrink-0 items-center justify-center gap-2 rounded-xl border border-brand-200/80 bg-paper px-2.5 text-ink-soft shadow-2xs transition-all hover:border-brand-500 hover:bg-raised hover:text-brand-600 sm:px-3"
          >
            <div
              className="flex h-4 w-5 flex-col items-start justify-center gap-1 overflow-hidden"
              aria-hidden="true"
            >
              <span className="menu-btn-line block h-0.5 w-4.5 rounded-full bg-current group-hover:w-5 group-hover:translate-x-0.5" />
              <span className="menu-btn-line block h-0.5 w-3.5 rounded-full bg-current group-hover:w-5 group-hover:translate-x-1" />
              <span className="menu-btn-line block h-0.5 w-4.5 rounded-full bg-current group-hover:w-5 group-hover:translate-x-0.5" />
            </div>
          </button>
          <Logo to="/trang-chu" />
          <div className="hidden flex-1 items-center justify-center px-2 xl:flex">
            <nav
              ref={navigationGroup}
              aria-label="Điều hướng chính"
              onMouseLeave={() => { setHoveredNavIndex(null); setExploreMenuOpen(false) }}
              className="glass-nav-group flex items-center"
            >
              <div
                className="glass-nav-glider"
                style={{
                  transform: `translateX(${gliderStyle.left}px)`,
                  width: `${gliderStyle.width}px`,
                  opacity: gliderStyle.opacity,
                  pointerEvents: "none",
                }}
              />
              {navigation.map((item, idx) => {
                const isCurrent = targetNavIndex === idx
                if (item.to === "/kham-pha") {
                  return <div key={item.to} ref={exploreMenu} className="relative" onMouseEnter={() => setExploreMenuOpen(true)}>
                    <button
                      ref={(el) => { tabRefs.current[idx] = el; exploreTrigger.current = el }}
                      type="button"
                      aria-expanded={exploreMenuOpen}
                      aria-controls="explore-navigation"
                      aria-current={isExploreRoute ? "page" : undefined}
                      onClick={() => setExploreMenuOpen((open) => !open)}
                      onMouseEnter={() => setHoveredNavIndex(idx)}
                      className={`glass-nav-link inline-flex items-center gap-1 border-0 bg-transparent ${isCurrent ? "is-active" : ""}`}
                    >{item.label}<ChevronDown className={`h-3.5 w-3.5 transition-transform ${exploreMenuOpen ? "rotate-180" : ""}`} /></button>
                    {exploreMenuOpen && <nav id="explore-navigation" aria-label="Khám phá" className="absolute left-0 top-full z-40 mt-1 w-60 rounded-xl border border-brand-100 bg-paper p-2 shadow-xl">
                      {exploreLinks.map((link) => <NavLink key={link.to} to={link.to} end className={({ isActive }) => `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium ${isActive ? "bg-brand-50 text-brand-800" : "text-ink-soft hover:bg-raised hover:text-brand-700"}`}>
                        <link.icon className="h-4 w-4 text-brand-700" />{link.label}
                      </NavLink>)}
                    </nav>}
                  </div>
                }
                return (
              <NavLink
                    key={item.to}
                    to={item.to}
                    ref={(el) => {
                      tabRefs.current[idx] = el
                    }}
                    onMouseEnter={() => setHoveredNavIndex(idx)}
                    aria-label={
                      item.to === "/kham-pha" ? "Khám phá món chay" : item.label
                    }
                    end={item.to === "/trang-chu"}
                    className={`glass-nav-link ${isCurrent ? "is-active" : ""}`}
                  >
                    {item.label}
              </NavLink>
                )
              })}
            </nav>
          </div>
          <div className="ml-auto flex shrink-0 items-center gap-2 sm:gap-3 xl:ml-0">
            <form
              onSubmit={(event) => {
                event.preventDefault()
                navigate(
                  `/kham-pha${
                    query.trim() ? `?q=${encodeURIComponent(query.trim())}` : ""
                  }`,
                )
              }}
              className="hidden w-40 items-center rounded-full border border-brand-100 bg-cream px-3 2xl:flex"
            >
              <button
                type="submit"
                aria-label="Tìm kiếm"
                className="py-2 text-ink-muted hover:text-brand-600"
              >
                <Search className="h-4 w-4" />
              </button>
              <input
                aria-label="Tìm kiếm nhanh"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="Tìm công thức"
                className="w-full min-w-0 bg-transparent py-2 pl-2 text-xs text-ink outline-none placeholder:text-ink-muted"
              />
            </form>
            <button
              type="button"
              aria-label="Tìm kiếm"
              onClick={() => navigate("/kham-pha")}
              className="flex h-8 w-8 items-center justify-center rounded-full text-ink-muted hover:bg-paper 2xl:hidden"
            >
              <Search className="h-4 w-4" />
            </button>
            <button
              type="button"
              aria-label={theme === "light" ? "Chuyển sang chế độ tối" : "Chuyển sang chế độ sáng"}
              title={theme === "light" ? "Chuyển sang chế độ tối" : "Chuyển sang chế độ sáng"}
              onClick={() => setTheme(theme === "light" ? "dark" : "light")}
              className="hidden h-8 w-8 items-center justify-center rounded-full text-ink-muted hover:bg-paper hover:text-brand-600 transition-colors sm:flex"
            >
              {theme === "light" ? (
                <Moon className="h-4 w-4" aria-hidden="true" />
              ) : (
                <Sun className="h-4 w-4" aria-hidden="true" />
              )}
            </button>
            {role === "EXPERT" && (
              <Link
                to="/dang-cong-thuc"
                className="hidden items-center gap-1.5 rounded-full bg-brand-600 px-3 py-2 text-xs font-semibold text-on-brand hover:bg-brand-500 sm:inline-flex"
              >
                <Plus className="h-3.5 w-3.5" />
                Đăng công thức
              </Link>
            )}
            {isAuthenticated && <NotificationBell />}
            {isAuthenticated ? (
              <div ref={accountMenu} className="relative">
                <button
                  ref={accountTrigger}
                  type="button"
                  aria-label={`Tài khoản ${account?.displayName}`}
                  aria-expanded={menuOpen}
                  aria-controls="account-navigation"
                  onClick={() => setMenuOpen((open) => !open)}
                  className="flex items-center gap-2 rounded-full p-1 hover:bg-paper"
                >
                  {avatar}
                  <span className="hidden max-w-24 truncate text-xs font-medium text-ink sm:block">
                    {account?.displayName}
                  </span>
                  <ChevronDown className="h-3.5 w-3.5 text-ink-muted" />
                </button>
                {menuOpen && (
                  <nav
                    id="account-navigation"
                    aria-label="Điều hướng tài khoản"
                    className="absolute right-0 top-full mt-3 w-64 rounded-xl border border-brand-100 bg-paper p-2 shadow-2xl shadow-black/40"
                  >
                    <div className="mb-1 border-b border-brand-100 px-3 py-2">
                      <p className="text-sm font-semibold text-ink">
                        {account?.displayName}
                      </p>
                      <p className="mt-0.5 truncate text-xs text-ink-muted">
                        {account?.email}
                      </p>
                    </div>
                    {personalLinks.map((item) => (
                      <Link
                        key={item.to}
                        to={item.to}
                        onClick={() => setMenuOpen(false)}
                        className="flex items-center gap-2.5 rounded-lg px-3 py-2.5 text-xs text-ink-soft hover:bg-raised"
                      >
                        <item.icon className="h-4 w-4 text-ink-muted" />
                        {item.label}
                      </Link>
                    ))}
                    <button
                      type="button"
                      onClick={leave}
                      className="mt-1 flex w-full items-center gap-2.5 border-t border-brand-100 px-3 py-3 text-xs text-ink-muted hover:text-brand-600"
                    >
                      <LogOut className="h-4 w-4" />
                      Đăng xuất
                    </button>
                  </nav>
                )}
              </div>
            ) : (
              <div className="flex items-center gap-3">
                <Link
                  to="/dang-nhap"
                  className="whitespace-nowrap text-xs font-medium text-ink-soft hover:text-brand-600"
                >
                  Đăng nhập
                </Link>
                <Link
                  to="/dang-ky"
                  className="hidden rounded-full bg-brand-600 px-4 py-2 text-xs font-semibold text-on-brand hover:bg-brand-500 sm:block"
                >
                  Đăng ký
                </Link>
              </div>
            )}
          </div>
        </div>
      </header>

      <dialog
        ref={drawer}
        onCancel={() => setDrawerOpen(false)}
        onClose={() => setDrawerOpen(false)}
        aria-label="Menu Mâm Xanh"
        className="fixed inset-0 m-0 h-dvh max-h-none w-screen max-w-none border-0 bg-transparent p-0 backdrop:bg-black/70"
      >
        <button
          type="button"
          onClick={() => setDrawerOpen(false)}
          aria-label="Đóng nền"
          className="drawer-backdrop absolute inset-0 cursor-default"
        />
        <div className="drawer-panel relative flex h-full w-80 max-w-[90vw] flex-col border-r border-brand-100 bg-cream-soft shadow-2xl">
          <div className="flex h-16 items-center justify-between border-b border-brand-100 px-5">
            <Logo to="/trang-chu" />
            <button
              type="button"
              aria-label="Đóng"
              onClick={() => setDrawerOpen(false)}
              className="rounded-lg p-2 text-ink-muted transition-colors hover:bg-paper hover:text-ink"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
          <nav
            aria-label="Menu chính"
            className="drawer-nav-menu flex-1 space-y-1 overflow-y-auto p-3"
          >
            {navigation.map((item) => item.to === "/kham-pha" ? (
              <div key={item.to}>
                <button type="button" aria-expanded={exploreMobileOpen} aria-controls="mobile-explore-navigation" onClick={() => setExploreMobileOpen((open) => !open)} className={`drawer-nav-item flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-all ${isExploreRoute ? "is-active bg-brand-600 font-bold text-on-brand shadow-md shadow-brand-600/20" : "text-ink-soft hover:bg-paper hover:text-brand-600"}`}>
                  <item.icon className="h-4 w-4 shrink-0" /><span className="flex-1 text-left">Khám phá</span><ChevronDown className={`h-4 w-4 transition-transform ${exploreMobileOpen ? "rotate-180" : ""}`} />
                </button>
                {exploreMobileOpen && <div id="mobile-explore-navigation" className="ml-5 mt-1 space-y-1 border-l border-brand-200 pl-3">
                  {exploreLinks.map((link) => <NavLink key={link.to} to={link.to} end onClick={() => setDrawerOpen(false)} className={({ isActive }) => `flex items-center gap-3 rounded-xl px-4 py-2.5 text-sm ${isActive ? "bg-brand-50 font-semibold text-brand-800" : "text-ink-soft hover:bg-paper hover:text-brand-600"}`}><link.icon className="h-4 w-4" />{link.label}</NavLink>)}
                </div>}
              </div>
            ) : <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/trang-chu"}
              className={({ isActive }) => `drawer-nav-item flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-all ${isActive ? "is-active bg-brand-600 font-bold text-on-brand shadow-md shadow-brand-600/20" : "text-ink-soft hover:bg-paper hover:text-brand-600"}`}
            >
              <item.icon className="h-4 w-4 shrink-0" /><span className="flex-1">{item.label}</span>
            </NavLink>)}
            <div className="my-3 border-t border-brand-100" />
            <p className="px-4 pb-2 text-xs font-semibold uppercase tracking-wider text-ink-muted">
              {account?.displayName ?? "Tài khoản & công cụ"}
            </p>
            {personalLinks.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end
                className={({ isActive }) => {
                  const activeItem = isActive && item.to !== "/ho-so"
                  return `drawer-nav-item flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-all ${
                    activeItem
                      ? "is-active bg-brand-600 font-bold text-on-brand shadow-md shadow-brand-600/20"
                      : "text-ink-soft hover:bg-paper hover:text-brand-600"
                  }`
                }}
              >
                <item.icon className="h-4 w-4 shrink-0" />
                <span className="flex-1">{item.label}</span>
              </NavLink>
            ))}
            <section
              aria-label="Cài đặt giao diện"
              className="mx-2 my-3 rounded-xl border border-brand-100 bg-cream p-3"
            >
              <h2 className="mb-2 px-1 text-xs font-semibold text-ink-muted">
                Cài đặt giao diện
              </h2>
              <div
                role="group"
                aria-label="Chủ đề màu"
                className="grid grid-cols-2 gap-2"
              >
                <button
                  type="button"
                  aria-pressed={theme === "light"}
                  onClick={() => setTheme("light")}
                  className={`flex items-center justify-center gap-2 rounded-lg border px-3 py-2 text-xs font-medium transition-colors ${
                    theme === "light"
                      ? "border-brand-600 bg-brand-600 text-on-brand"
                      : "border-brand-100 text-ink-soft hover:bg-paper"
                  }`}
                >
                  <Sun className="h-4 w-4" aria-hidden="true" />
                  Sáng
                </button>
                <button
                  type="button"
                  aria-pressed={theme === "dark"}
                  onClick={() => setTheme("dark")}
                  className={`flex items-center justify-center gap-2 rounded-lg border px-3 py-2 text-xs font-medium transition-colors ${
                    theme === "dark"
                      ? "border-brand-600 bg-brand-600 text-on-brand"
                      : "border-brand-100 text-ink-soft hover:bg-paper"
                  }`}
                >
                  <Moon className="h-4 w-4" aria-hidden="true" />
                  Tối
                </button>
              </div>
            </section>
          </nav>
          <div className="border-t border-brand-100 p-4">
            {isAuthenticated ? (
              <button
                type="button"
                onClick={leave}
                className="drawer-nav-item flex w-full items-center gap-2.5 rounded-xl px-4 py-2.5 text-sm font-medium text-ink-soft transition-all hover:bg-paper hover:text-brand-600"
              >
                <LogOut className="h-4 w-4" />
                Đăng xuất
              </button>
            ) : (
              <div className="flex items-center gap-3">
                <Link
                  to="/dang-nhap"
                  className="drawer-nav-item flex-1 rounded-xl border border-brand-200 px-4 py-2.5 text-center text-sm font-semibold text-ink-soft transition-all hover:bg-paper hover:text-brand-600"
                >
                  Đăng nhập
                </Link>
                <Link
                  to="/dang-ky"
                  className="drawer-nav-item flex-1 rounded-full bg-brand-600 px-4 py-2.5 text-center text-sm font-semibold text-on-brand shadow-md shadow-brand-600/20 transition-all hover:bg-brand-500"
                >
                  Đăng ký
                </Link>
              </div>
            )}
          </div>
        </div>
      </dialog>
      {isAuthenticated && role !== "ADMIN" && (
        <button
          type="button"
          onClick={() => openChat()}
          aria-label="Mở khung chat hỗ trợ AI"
          title="Bạn cần AI hỗ trợ gì?"
          className="fixed bottom-5 right-5 z-40 inline-flex items-center gap-2.5 rounded-full bg-brand-600 px-4 py-3 text-sm font-semibold text-on-brand shadow-xl shadow-brand-900/20 transition-transform hover:-translate-y-0.5 hover:bg-brand-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
        >
          <MessageCircle className="h-5 w-5" aria-hidden="true" />
          <span>Bạn cần AI hỗ trợ?</span>
        </button>
      )}
    </>
  )
}
