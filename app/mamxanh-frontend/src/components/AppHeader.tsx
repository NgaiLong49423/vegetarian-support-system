import { useEffect, useRef, useState } from "react"
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom"
import {
  Activity,
  Award,
  Bookmark,
  CalendarDays,
  ChevronDown,
  LayoutGrid,
  LogOut,
  Menu,
  MessageCircle,
  Plus,
  Search,
  ShoppingBasket,
  SlidersHorizontal,
  Sparkles,
  Sun,
  Moon,
  UserCog,
  Users,
  X,
} from "lucide-react"
import { useAuth } from "./AuthContext"
import { Logo } from "./Logo"
import { useTheme } from "./ThemeContext"
import { useAiChat } from "./AiChatContext"

const navigation = [
  { to: "/", label: "Trang chủ", icon: LayoutGrid },
  { to: "/kham-pha", label: "Khám phá", icon: Search },
  { to: "/ho-so", label: "Đã lưu", icon: Bookmark },
  { to: "/ke-hoach", label: "Lịch ăn", icon: CalendarDays },
  { to: "/di-cho", label: "Đi chợ", icon: ShoppingBasket },
  { to: "/cong-dong", label: "Cộng đồng", icon: Users },
]

export function AppHeader() {
  const { account, isAuthenticated, signOut } = useAuth()
  const { openChat } = useAiChat()
  const { theme, setTheme } = useTheme()
  const navigate = useNavigate()
  const location = useLocation()
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const [query, setQuery] = useState("")
  const drawer = useRef<HTMLDialogElement>(null)
  const accountMenu = useRef<HTMLDivElement>(null)
  const accountTrigger = useRef<HTMLButtonElement>(null)
  const role = account?.role
  const personalLinks = [
    { to: "/ho-so", label: "Hồ sơ của tôi", icon: Bookmark },
    ...(role !== "ADMIN"
      ? [{ to: "/ho-so/cai-dat", label: "Cài đặt hồ sơ", icon: UserCog }]
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
    ...(role === "CUSTOMER"
      ? [
          {
            to: "/dang-ky-chuyen-gia",
            label: "Đăng ký trở thành Chuyên gia",
            icon: Award,
          },
        ]
      : []),
    ...(role === "ADMIN"
      ? [
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
      : []),
  ]

  useEffect(() => {
    setDrawerOpen(false)
    setMenuOpen(false)
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
      <header className="sticky top-0 z-30 border-b border-brand-100 bg-cream-soft">
        <div className="mx-auto flex h-16 max-w-[1344px] items-center gap-3 px-4 sm:gap-5 sm:px-6">
          <button
            type="button"
            aria-label="Menu"
            aria-expanded={drawerOpen}
            onClick={() => setDrawerOpen(true)}
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg text-ink-muted hover:bg-paper hover:text-ink"
          >
            <Menu className="h-5 w-5" />
          </button>
          <Logo />
          <nav
            aria-label="Điều hướng chính"
            className="ml-3 hidden h-full items-center gap-6 xl:flex"
          >
            {navigation.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                aria-label={
                  item.to === "/kham-pha" ? "Khám phá món chay" : item.label
                }
                end={item.to === "/"}
                className={({ isActive }) =>
                  `relative flex h-full items-center whitespace-nowrap text-[13px] font-medium transition-colors hover:text-brand-600 ${
                    isActive
                      ? "text-brand-600 after:absolute after:inset-x-0 after:bottom-0 after:h-0.5 after:bg-brand-600"
                      : "text-ink-soft"
                  }`
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
          <div className="ml-auto flex items-center gap-2 sm:gap-3">
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
            {role === "EXPERT" && (
              <Link
                to="/dang-cong-thuc"
                className="hidden items-center gap-1.5 rounded-full bg-brand-600 px-3 py-2 text-xs font-semibold text-on-brand hover:bg-brand-500 sm:inline-flex"
              >
                <Plus className="h-3.5 w-3.5" />
                Đăng công thức
              </Link>
            )}
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
          className="absolute inset-0 cursor-default"
        />
        <div className="relative flex h-full w-80 max-w-[90vw] flex-col border-r border-brand-100 bg-cream-soft">
          <div className="flex h-16 items-center justify-between border-b border-brand-100 px-5">
            <Logo />
            <button
              type="button"
              aria-label="Đóng"
              onClick={() => setDrawerOpen(false)}
              className="rounded-lg p-2 text-ink-muted hover:bg-paper"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
          <nav aria-label="Menu chính" className="flex-1 overflow-y-auto p-3">
            {navigation.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                aria-label={
                  item.to === "/kham-pha" ? "Khám phá món chay" : item.label
                }
                end={item.to === "/"}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-xl px-4 py-3 text-sm ${
                    isActive
                      ? "bg-paper text-brand-600"
                      : "text-ink-soft hover:bg-paper"
                  }`
                }
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </NavLink>
            ))}
            <div className="my-3 border-t border-brand-100" />
            <p className="px-4 pb-2 text-xs font-semibold text-ink-muted">
              {account?.displayName ?? "Tài khoản & công cụ"}
            </p>
            {personalLinks.map((item) => (
              <Link
                key={item.to}
                to={item.to}
                className="flex items-center gap-3 rounded-xl px-4 py-3 text-xs text-ink-soft hover:bg-paper"
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </Link>
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
                className="flex items-center gap-2 text-sm text-ink-soft"
              >
                <LogOut className="h-4 w-4" />
                Đăng xuất
              </button>
            ) : (
              <div className="flex items-center gap-4">
                <Link to="/dang-nhap" className="text-sm text-ink-soft">
                  Đăng nhập
                </Link>
                <Link
                  to="/dang-ky"
                  className="rounded-full bg-brand-600 px-4 py-2 text-sm font-semibold text-on-brand"
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
