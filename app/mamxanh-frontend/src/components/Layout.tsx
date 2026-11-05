import { type ReactNode } from "react"
import { useLocation } from "react-router-dom"
import { AppHeader } from "./AppHeader"
import { AppFooter } from "./AppFooter"
import { MobileBottomNav } from "./MobileBottomNav"
import { AdminLayout } from "./AdminLayout"

export function Layout({ children }: { children: ReactNode }) {
  const { pathname } = useLocation()
  if (
    [
      "/dang-nhap",
      "/dang-ky",
      "/quen-mat-khau",
      "/xac-minh-email",
      "/dat-lai-mat-khau",
    ].includes(pathname)
  ) {
    return <>{children}</>
  }

  // Pure Admin Workspace separation
  if (pathname.startsWith("/admin") || pathname.startsWith("/quan-tri")) {
    return <AdminLayout>{children}</AdminLayout>
  }

  return (
    <div className="food-app flex min-h-screen flex-col bg-cream pb-[calc(4.25rem+env(safe-area-inset-bottom))] md:pb-0">
      <AppHeader />
      <main className="min-w-0 flex-1">{children}</main>
      <AppFooter />
      <MobileBottomNav />
    </div>
  )
}

export function PageContainer({
  children,
  className = "",
}: {
  children: ReactNode
  className?: string
}) {
  const hasCustomMaxW = /\bmax-w-/.test(className)
  return (
    <div
      className={`mx-auto w-full ${
        hasCustomMaxW ? "" : "max-w-[1344px]"
      } px-4 sm:px-6 lg:px-8 ${className}`}
    >
      {children}
    </div>
  )
}
