import type { ReactNode } from "react"

export interface AdminPageHeaderProps {
  title: string
  description?: string
  actions?: ReactNode
  children?: ReactNode
  className?: string
}

export function AdminPageHeader({
  title,
  description,
  actions,
  children,
  className = "",
}: AdminPageHeaderProps) {
  return (
    <div
      className={`relative mb-7 overflow-hidden rounded-3xl border border-brand-200/80 bg-gradient-to-br from-paper via-paper to-brand-50/40 p-6 shadow-sm sm:p-8 ${className}`}
    >
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -right-16 -top-16 h-56 w-56 rounded-full bg-brand-500/10 blur-3xl"
      />
      <div className="relative flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
        <div className="max-w-2xl">
          <h1 className="text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">
            {title}
          </h1>
          {description && (
            <p className="mt-1 text-sm text-ink-muted">{description}</p>
          )}
        </div>

        {actions && (
          <div className="flex flex-wrap items-center gap-2.5">{actions}</div>
        )}
      </div>

      {children && (
        <div className="relative mt-6 border-t border-brand-100/80 pt-5">
          {children}
        </div>
      )}
    </div>
  )
}
