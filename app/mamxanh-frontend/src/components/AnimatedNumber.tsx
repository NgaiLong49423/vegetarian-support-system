import { useEffect, useRef, useState } from "react"

export interface AnimatedNumberProps {
  /** Target number to animate to */
  value: number
  /** Initial number to count up from (default: 0) */
  startValue?: number
  /** Duration in milliseconds (default: 900) */
  duration?: number
  /** Decimal places to display (default: 0) */
  decimals?: number
  /** Prefix to prepend (e.g. "$", "+") */
  prefix?: string
  /** Suffix to append (e.g. " đ", "%", " token") */
  suffix?: string
  /** Optional custom formatting function */
  formatter?: (val: number) => string
  /** Locale for Intl.NumberFormat (default: 'vi-VN') */
  locale?: string
  /** Additional CSS class names */
  className?: string
}

/**
 * Custom hook to smoothly animate a numeric value from start to target.
 * Uses requestAnimationFrame with ease-out quartic easing and respects prefers-reduced-motion.
 */
export function useCountUp(
  targetValue: number,
  options?: {
    startValue?: number
    duration?: number
    decimals?: number
  },
) {
  const { startValue = 0, duration = 900, decimals = 0 } = options || {}

  const isReducedMotion = () =>
    typeof window !== "undefined" &&
    Boolean(window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches)

  const [displayValue, setDisplayValue] = useState<number>(() => {
    if (isReducedMotion() || !Number.isFinite(targetValue)) {
      return targetValue
    }
    return startValue
  })

  const currentRef = useRef<number>(displayValue)
  const frameRef = useRef<number | null>(null)

  useEffect(() => {
    if (!Number.isFinite(targetValue) || isReducedMotion()) {
      setDisplayValue(targetValue)
      currentRef.current = targetValue
      return
    }

    const start = currentRef.current
    const diff = targetValue - start

    if (diff === 0) {
      setDisplayValue(targetValue)
      return
    }

    const startTime = performance.now()

    const tick = (now: number) => {
      const elapsed = now - startTime
      const progress = Math.min(1, elapsed / duration)

      // easeOutQuart: quick ramp-up with gentle settling
      const ease = 1 - Math.pow(1 - progress, 4)
      const current = start + diff * ease

      const factor = Math.pow(10, decimals)
      const rounded = Math.round(current * factor) / factor

      setDisplayValue(rounded)
      currentRef.current = current

      if (progress < 1) {
        frameRef.current = requestAnimationFrame(tick)
      } else {
        setDisplayValue(targetValue)
        currentRef.current = targetValue
      }
    }

    frameRef.current = requestAnimationFrame(tick)

    return () => {
      if (frameRef.current !== null) {
        cancelAnimationFrame(frameRef.current)
      }
    }
  }, [targetValue, duration, decimals])

  return displayValue
}

/**
 * AnimatedNumber component
 * Displays a number counting up from 0 (or startValue) to the target value.
 */
export function AnimatedNumber({
  value,
  startValue = 0,
  duration = 900,
  decimals = 0,
  prefix = "",
  suffix = "",
  formatter,
  locale = "vi-VN",
  className = "",
}: AnimatedNumberProps) {
  const animatedValue = useCountUp(value, { startValue, duration, decimals })

  if (!Number.isFinite(value)) {
    return <span className={className}>—</span>
  }

  let formatted: string
  if (formatter) {
    formatted = formatter(animatedValue)
  } else {
    formatted = new Intl.NumberFormat(locale, {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals,
    }).format(animatedValue)
  }

  return (
    <span
      className={`inline-block tabular-nums transition-opacity duration-300 ${className}`}
    >
      {prefix}
      {formatted}
      {suffix}
    </span>
  )
}
