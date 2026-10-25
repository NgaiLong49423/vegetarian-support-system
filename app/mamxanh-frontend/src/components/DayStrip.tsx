import { useMemo } from "react"
import { localDateKey } from "../utils/date"
import type { MealPlanWeek } from "../api/recipes"

type Entry = MealPlanWeek["entries"][number]

const weekdayAbbr = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"]

export function DayStrip({
  windowDays,
  selectedDay,
  entries: _entries,
  onSelectDay,
}: {
  windowDays: string[]
  selectedDay: string | null
  entries?: Entry[]
  onSelectDay: (day: string) => void
}) {
  const dayStats = useMemo(() => {
    const todayKey = localDateKey(new Date())
    return windowDays.map((day) => {
      const date = new Date(`${day}T12:00:00`)
      const dayIndex = date.getDay()
      const abbr = weekdayAbbr[dayIndex]
      const dayNum = date.getDate()
      const isDayToday = day === todayKey
      const isSelected = selectedDay === day

      return {
        day,
        abbr,
        dayNum,
        isDayToday,
        isSelected,
      }
    })
  }, [windowDays, selectedDay])

  const handleChipClick = (day: string) => {
    onSelectDay(day)
    requestAnimationFrame(() => {
      const el = document.getElementById(`day-section-${day}`)
      el?.scrollIntoView({ behavior: "smooth", block: "nearest" })
    })
  }

  return (
    <div
      aria-label="Chọn nhanh ngày trong 7 ngày"
      className="sticky top-0 z-10 -mx-2 mb-6 bg-paper/95 px-2 py-2.5 backdrop-blur-xs transition-colors border-b border-brand-100/60 dark:border-brand-900/40"
    >
      <div className="flex gap-2 overflow-x-auto no-scrollbar scroll-smooth snap-x sm:grid sm:grid-cols-7 sm:gap-2">
        {dayStats.map((item) => (
          <button
            key={item.day}
            type="button"
            onClick={() => handleChipClick(item.day)}
            aria-label={`${item.abbr}, ngày ${item.dayNum}${item.isDayToday ? " (Hôm nay)" : ""}`}
            aria-pressed={item.isSelected}
            className={`group relative flex min-w-[58px] flex-1 flex-col items-center justify-center rounded-2xl py-2 px-1.5 transition-all duration-200 snap-center focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 ${
              item.isDayToday
                ? item.isSelected
                  ? "z-10 bg-gradient-to-b from-brand-500 to-brand-600 dark:from-brand-600 dark:to-brand-700 text-white font-semibold ring-2 ring-brand-500/80 dark:ring-brand-400 ring-offset-2 ring-offset-paper day-chip-today-glow shadow-sm"
                  : "bg-paper hover:bg-brand-50/60 dark:bg-paper dark:hover:bg-brand-950/40 text-brand-700 dark:text-brand-300 font-semibold border-2 border-brand-500/80 dark:border-brand-500/90 shadow-2xs"
                : item.isSelected
                  ? "z-10 bg-brand-600 text-white shadow-sm font-semibold ring-2 ring-brand-600 ring-offset-2 ring-offset-paper"
                  : "bg-paper/80 hover:bg-paper dark:bg-raised/20 dark:hover:bg-raised/40 text-ink border border-brand-100/70 dark:border-brand-900/40 shadow-2xs hover:border-brand-200"
            }`}
          >
            {item.isDayToday ? (
              <span
                className={`inline-flex items-center gap-1 text-[9px] font-extrabold uppercase tracking-wider mb-0.5 ${
                  item.isSelected
                    ? "text-amber-100 dark:text-amber-200"
                    : "text-brand-600 dark:text-brand-400"
                }`}
              >
                <span
                  className={`h-1.5 w-1.5 rounded-full ${
                    item.isSelected
                      ? "bg-amber-300 shadow-[0_0_6px_rgba(253,224,71,0.9)] animate-pulse"
                      : "bg-brand-500 dark:bg-brand-400"
                  }`}
                  aria-hidden="true"
                />
                Hôm nay
              </span>
            ) : null}
            <span
              className={`text-[11px] font-bold ${
                item.isSelected
                  ? "text-white/95"
                  : item.isDayToday
                    ? "text-brand-600 dark:text-brand-400"
                    : "text-ink-muted group-hover:text-ink"
              }`}
            >
              {item.abbr}
            </span>
            <span
              className={`text-base sm:text-lg font-black leading-tight tracking-tight mt-0.5 ${
                item.isSelected
                  ? "text-white"
                  : item.isDayToday
                    ? "text-brand-700 dark:text-brand-300"
                    : "text-ink"
              }`}
            >
              {item.dayNum}
            </span>
          </button>
        ))}
      </div>
    </div>
  )
}

export default DayStrip
