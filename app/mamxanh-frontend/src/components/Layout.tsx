import { type ReactNode } from "react"
import { useLocation } from "react-router-dom"
import { AppHeader } from "./AppHeader"
import { AppFooter } from "./AppFooter"
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
    <div className="food-app flex min-h-screen flex-col bg-cream">
      <AppHeader />
      <main className="flex-1">{children}</main>
      <AppFooter />
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
