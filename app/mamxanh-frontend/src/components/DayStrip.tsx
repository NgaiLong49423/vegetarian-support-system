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
            className={`group relative flex min-w-[56px] flex-1 flex-col items-center justify-center rounded-2xl py-2.5 px-1.5 transition-all duration-300 snap-center focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 ${
              item.isDayToday
                ? item.isSelected
                  ? "bg-brand-600 text-on-brand font-semibold scale-[1.04] day-chip-today-glow ring-2 ring-brand-300 dark:ring-brand-400"
                  : "bg-brand-50/90 dark:bg-brand-950/70 text-brand-700 dark:text-brand-300 font-semibold border-2 border-brand-500 day-chip-today-glow scale-[1.02]"
                : item.isSelected
                  ? "bg-brand-600 text-on-brand shadow-sm font-semibold scale-[1.02]"
                  : "bg-raised/40 hover:bg-raised dark:bg-raised/20 dark:hover:bg-raised/40 text-ink border border-brand-100/50 dark:border-brand-900/30"
            }`}
          >
            {item.isDayToday && (
              <span
                className={`text-[9px] font-extrabold uppercase tracking-wider mb-0.5 ${
                  item.isSelected ? "text-on-brand/95" : "text-brand-600 dark:text-brand-400"
                }`}
              >
                Hôm nay
              </span>
            )}
            <span
              className={`text-[11px] font-bold ${
                item.isSelected
                  ? "text-on-brand"
                  : item.isDayToday
                    ? "text-brand-600 dark:text-brand-400"
                    : "text-ink-muted group-hover:text-ink"
              }`}
            >
              {item.abbr}
            </span>
            <span className="text-base sm:text-lg font-black leading-tight tracking-tight mt-0.5">
              {item.dayNum}
            </span>
          </button>
        ))}
      </div>
    </div>
  )
}

export default DayStrip
