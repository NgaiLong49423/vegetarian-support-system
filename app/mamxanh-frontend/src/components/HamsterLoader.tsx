import type { ReactNode } from "react"
import "./HamsterLoader.css"

export interface HamsterLoaderProps {
  /** Size variant: sm (9px base), md (13px base, default), lg (16px base) */
  size?: "sm" | "md" | "lg"
  /** Optional loading caption text rendered under the hamster */
  text?: ReactNode
  /** Custom wrapper class */
  className?: string
  /** Accessible label */
  ariaLabel?: string
}

export function HamsterLoader({
  size = "md",
  text,
  className = "",
  ariaLabel = "Đang tải dữ liệu Mâm Xanh...",
}: HamsterLoaderProps) {
  return (
    <div
      role="status"
      aria-label={ariaLabel}
      className={`hamster-loader-container ${className}`}
    >
      <div className={`wheel-and-hamster size-${size}`} aria-hidden="true">
        <div className="wheel" />
        <div className="hamster">
          <div className="hamster__body">
            <div className="hamster__head">
              <div className="hamster__ear" />
              <div className="hamster__eye" />
              <div className="hamster__nose" />
            </div>
            <div className="hamster__limb hamster__limb--fr" />
            <div className="hamster__limb hamster__limb--fl" />
            <div className="hamster__limb hamster__limb--br" />
            <div className="hamster__limb hamster__limb--bl" />
            <div className="hamster__tail" />
          </div>
        </div>
        <div className="spoke" />
      </div>

      {text && (
        <p className="mt-4 text-center text-xs font-semibold tracking-wide text-ink-muted animate-pulse sm:text-sm">
          {text}
        </p>
      )}
    </div>
  )
}

export default HamsterLoader
