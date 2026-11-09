import type { ReactNode } from "react"
import { useEffect, useRef } from "react"
import { createPortal } from "react-dom"
import { X } from "lucide-react"

export function Modal({
  open,
  onClose,
  title,
  children,
  size = "md",
  tone = "default",
  trapFocus = false,
  icon,
  subtitle,
  accent,
}: {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
  size?: "md" | "lg" | "xl"
  tone?: "default" | "dark"
  trapFocus?: boolean
  icon?: ReactNode
  subtitle?: ReactNode
  accent?: string
}) {
  const panel = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose()
      if (e.key !== "Tab" || !trapFocus || !panel.current) return
      const controls = Array.from(
        panel.current.querySelectorAll<HTMLElement>(
          'button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href], [tabindex="0"]',
        ),
      ).filter((element) => element.getClientRects().length > 0)
      const first = controls[0],
        last = controls[controls.length - 1]
      if (!first) {
        e.preventDefault()
        panel.current.focus()
        return
      }
      if (
        !panel.current.contains(document.activeElement) ||
        (e.shiftKey && document.activeElement === first)
      ) {
        e.preventDefault()
        ;(e.shiftKey ? last : first).focus()
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault()
        first.focus()
      }
    }
    window.addEventListener("keydown", onKey)
    document.body.style.overflow = "hidden"
    return () => {
      window.removeEventListener("keydown", onKey)
      document.body.style.overflow = ""
    }
  }, [open, onClose, trapFocus])

  if (!open) return null

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center p-0 sm:items-center sm:p-4">
      <button
        type="button"
        aria-label="Đóng nền"
        className="modal-backdrop-enter absolute inset-0 h-full w-full bg-black/65 backdrop-blur-md cursor-default"
        onClick={onClose}
      />
      <div
        ref={panel}
        tabIndex={-1}
        className={`modal-panel-enter group relative w-full ${
          size === "xl"
            ? "sm:max-w-3xl"
            : size === "lg"
              ? "sm:max-w-2xl"
              : "sm:max-w-md"
        } max-h-[92dvh] overflow-y-auto rounded-t-3xl border p-5 pb-[calc(1.25rem+env(safe-area-inset-bottom))] shadow-2xl sm:max-h-[90vh] sm:rounded-3xl sm:p-7 ${
          tone === "dark"
            ? "border-white/10 bg-[#191919] text-white shadow-black/60"
            : "border-brand-200/90 bg-paper text-ink shadow-brand-950/20 ring-1 ring-brand-900/5"
        }`}
        role="dialog"
        aria-modal="true"
        aria-label={title}
      >
        {/* Top Accent Gradient Line */}
        <div
          className={`absolute inset-x-0 top-0 h-1 bg-gradient-to-r ${
            accent ??
            (tone === "dark"
              ? "from-brand-500 via-amber-400 to-transparent"
              : "from-brand-500 via-leaf-400 to-amber-400")
          }`}
        />

        {/* Ambient Corner Glow */}
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -right-16 -top-16 h-40 w-40 rounded-full bg-brand-500/10 blur-3xl"
        />

        {/* Mobile drag indicator */}
        <div
          aria-hidden="true"
          className="modal-drag-pill mx-auto mb-3 h-1.5 w-11 rounded-full sm:hidden"
        />

        {/* Header */}
        <div className="mb-5 flex items-start justify-between gap-4 border-b border-brand-100/70 pb-4">
          <div className="flex items-center gap-3 min-w-0">
            {icon && (
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl bg-brand-50 text-brand-600 ring-1 ring-brand-200/70 shadow-2xs">
                {icon}
              </span>
            )}
            <div className="min-w-0">
              <h3
                className={`text-lg sm:text-xl font-extrabold tracking-tight ${
                  tone === "dark" ? "text-white" : "text-ink"
                }`}
              >
                {title}
              </h3>
              {subtitle && (
                <p
                  className={`mt-0.5 text-xs ${
                    tone === "dark" ? "text-white/70" : "text-ink-muted"
                  }`}
                >
                  {subtitle}
                </p>
              )}
            </div>
          </div>
          <button
            type="button"
            aria-label="Đóng"
            onClick={onClose}
            className={`flex h-9 w-9 shrink-0 items-center justify-center rounded-full border transition-all duration-200 hover:rotate-90 sm:h-8 sm:w-8 ${
              tone === "dark"
                ? "border-white/10 text-white/75 hover:bg-white/10 hover:text-white"
                : "border-brand-200/70 bg-cream-soft/60 text-ink-muted hover:border-brand-300 hover:bg-brand-50 hover:text-brand-700 shadow-2xs"
            }`}
          >
            <X className="h-4 w-4" />
          </button>
        </div>
        {children}
      </div>
    </div>,
    document.body,
  )
}
