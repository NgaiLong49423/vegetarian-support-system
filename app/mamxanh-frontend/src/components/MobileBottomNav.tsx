import { Link, useLocation } from "react-router-dom"
import {
  CalendarDays,
  Compass,
  LayoutGrid,
  ShoppingBasket,
  UserRound,
} from "lucide-react"
import { useAuth } from "./AuthContext"

const EXPLORE_PREFIXES = [
  "/kham-pha",
  "/danh-muc-nguyen-lieu",
  "/nguyen-lieu/",
  "/chuyen-gia",
  "/cong-thuc/",
  "/so-sanh",
]

const PROFILE_PREFIXES = [
  "/ho-so",
  "/dinh-duong",
  "/giao-dich",
  "/goi-ai",
  "/dang-ky-chuyen-gia",
  "/dang-cong-thuc",
  "/thanh-vien/",
]

export function MobileBottomNav() {
  const { pathname } = useLocation()
  const { isAuthenticated } = useAuth()

  if (pathname === "/khoi-tao-so-thich") {
    return null
  }

  const tabs = [
    {
      to: "/trang-chu",
      label: "Trang chủ",
      ariaLabel: "Mục Trang chủ",
      icon: LayoutGrid,
      active: pathname === "/trang-chu",
    },
    {
      to: "/kham-pha",
      label: "Khám phá",
      ariaLabel: "Mục Khám phá",
      icon: Compass,
      active: EXPLORE_PREFIXES.some((prefix) => pathname.startsWith(prefix)),
    },
    {
      to: "/ke-hoach",
      label: "Lịch ăn",
      ariaLabel: "Mục Lịch ăn",
      icon: CalendarDays,
      active: pathname.startsWith("/ke-hoach"),
    },
    {
      to: "/di-cho",
      label: "Mua sắm",
      ariaLabel: "Mục Mua sắm",
      icon: ShoppingBasket,
      active: pathname.startsWith("/di-cho"),
    },
    {
      to: isAuthenticated ? "/ho-so" : "/dang-nhap",
      label: isAuthenticated ? "Cá nhân" : "Tài khoản",
      ariaLabel: isAuthenticated ? "Mục Cá nhân" : "Mục Tài khoản",
      icon: UserRound,
      active: PROFILE_PREFIXES.some((prefix) => pathname.startsWith(prefix)),
    },
  ]

  return (
    <nav
      aria-label="Điều hướng di động"
      className="mobile-bottom-nav fixed inset-x-0 bottom-0 z-30 border-t border-header-border bg-header/95 backdrop-blur-xl md:hidden"
    >
      <div className="mx-auto grid max-w-lg grid-cols-5 items-stretch px-1.5 pt-1 pb-1">
        {tabs.map((tab) => {
          const Icon = tab.icon
          return (
            <Link
              key={tab.ariaLabel}
              to={tab.to}
              aria-label={tab.ariaLabel}
              aria-current={tab.active ? "page" : undefined}
              className={`group relative flex min-h-14 flex-col items-center justify-center gap-0.5 rounded-xl px-1 py-1 text-[11px] transition-colors active:scale-95 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-brand-600 ${
                tab.active
                  ? "font-bold text-brand-600"
                  : "font-medium text-ink-muted hover:text-ink"
              }`}
            >
              <span
                aria-hidden="true"
                className={`absolute top-0 h-0.5 w-6 rounded-full transition-opacity ${
                  tab.active ? "bg-brand-600 opacity-100" : "opacity-0"
                }`}
              />
              <span
                className={`flex h-7 w-11 items-center justify-center rounded-2xl transition-colors ${
                  tab.active
                    ? "bg-brand-600/15 text-brand-600"
                    : "group-hover:bg-paper"
                }`}
              >
                <Icon
                  className="h-[18px] w-[18px]"
                  strokeWidth={tab.active ? 2.4 : 2}
                  aria-hidden="true"
                />
              </span>
              <span className="max-w-full truncate leading-tight">
                {tab.label}
              </span>
            </Link>
          )
        })}
      </div>
    </nav>
  )
}
