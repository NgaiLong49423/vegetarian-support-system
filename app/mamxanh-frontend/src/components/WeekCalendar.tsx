/* Lucian UX/UI Prompt #030 · MIT License (c) 2026 Seokwoooo · https://lucianlabs.dev/uxui-prompt/#030 */

import { useEffect, useRef, useState } from "react"
import { ChevronLeft, ChevronRight } from "lucide-react"
import { localDateKey, mondayOf } from "../utils/date"

export { localDateKey, mondayOf, shiftWeek, shiftDays, getWindowDays, isToday, isPastDate } from "../utils/date"

import "./WeekCalendar.css"

export function WeekCalendar({
  weekStart,
  onChange,
  selectedDay,
  onSelectDay,
}: {
  weekStart: string
  onChange: (date: string) => void
  selectedDay?: string | null
  onSelectDay?: (date: string) => void
}) {
  const [month, setMonth] = useState(() => new Date(`${weekStart}T12:00:00`))
  const [anim, setAnim] = useState<{
    prevMonth: Date
    dir: 1 | -1
  } | null>(null)

  const viewRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const target = new Date(`${weekStart}T12:00:00`)
    if (
      target.getFullYear() !== month.getFullYear() ||
      target.getMonth() !== month.getMonth()
    ) {
      setMonth(target)
      setAnim(null)
    }
  }, [weekStart])

  useEffect(() => {
    if (!anim) return
    const timer = setTimeout(() => {
      setAnim(null)
    }, 450)
    return () => clearTimeout(timer)
  }, [anim])

  const moveMonth = (offset: 1 | -1) => {
    const nextMonth = new Date(
      month.getFullYear(),
      month.getMonth() + offset,
      1,
      12,
    )
    setAnim({ prevMonth: month, dir: offset })
    setMonth(nextMonth)
  }

  const handleThisWeek = () => {
    const now = new Date()
    const thisMonday = mondayOf(now)
    onChange(thisMonday)
    onSelectDay?.(localDateKey(now))

    const targetMonth = new Date(now.getFullYear(), now.getMonth(), 1, 12)
    if (
      targetMonth.getFullYear() !== month.getFullYear() ||
      targetMonth.getMonth() !== month.getMonth()
    ) {
      const dir: 1 | -1 = targetMonth > month ? 1 : -1
      setAnim({ prevMonth: month, dir })
      setMonth(targetMonth)
    }
  }

  const today = localDateKey(new Date())

  const selectedStart = new Date(`${weekStart}T12:00:00`)
  const selectedEnd = new Date(selectedStart)
  selectedEnd.setDate(selectedEnd.getDate() + 7)

  const endWindow = new Date(selectedStart)
  endWindow.setDate(endWindow.getDate() + 6)
  const endWindowKey = localDateKey(endWindow)

  const handleKeyDown = (
    e: React.KeyboardEvent<HTMLButtonElement>,
    currentDate: Date,
  ) => {
    const steps: Record<string, number> = {
      ArrowLeft: -1,
      ArrowRight: 1,
      ArrowUp: -7,
      ArrowDown: 7,
    }
    const step = steps[e.key]
    if (step === undefined) return
    e.preventDefault()

    const targetDate = new Date(currentDate)
    targetDate.setDate(targetDate.getDate() + step)
    const targetKey = localDateKey(targetDate)

    if (
      targetDate.getMonth() !== month.getMonth() ||
      targetDate.getFullYear() !== month.getFullYear()
    ) {
      const dir: 1 | -1 = targetDate > currentDate ? 1 : -1
      moveMonth(dir)
    }

    onChange(mondayOf(targetDate))
    onSelectDay?.(targetKey)

    requestAnimationFrame(() => {
      const btn = viewRef.current?.querySelector<HTMLButtonElement>(
        `.u030-grid:not([aria-hidden="true"]) button[data-date="${targetKey}"]`,
      )
      btn?.focus()
    })
  }

  const renderGrid = (
    gridMonth: Date,
    animationClass: string,
    isAriaHidden?: boolean,
  ) => {
    const first = new Date(
      gridMonth.getFullYear(),
      gridMonth.getMonth(),
      1,
      12,
    )
    const gridStart = new Date(`${mondayOf(first)}T12:00:00`)

    // Identify which cell receives tabIndex 0
    let focusKey: string | null = null
    const days: { date: Date; key: string }[] = []
    for (let i = 0; i < 42; i++) {
      const d = new Date(gridStart)
      d.setDate(d.getDate() + i)
      const k = localDateKey(d)
      days.push({ date: d, key: k })
      if (!focusKey) {
        if (selectedDay && k === selectedDay) focusKey = k
      }
    }
    if (!focusKey) {
      const todayCell = days.find((d) => d.key === today)
      if (todayCell) focusKey = todayCell.key
      else {
        const firstOfMonth = days.find(
          (d) =>
            d.date.getDate() === 1 &&
            d.date.getMonth() === gridMonth.getMonth(),
        )
        focusKey = firstOfMonth?.key ?? days[0].key
      }
    }

    return (
      <div
        className={`u030-grid ${animationClass}`}
        aria-hidden={isAriaHidden ? true : undefined}
      >
        {days.map(({ date, key }) => {
          const selected = date >= selectedStart && date < selectedEnd
          const isStart = key === weekStart
          const isEnd = key === endWindowKey
          const isSelectedDay = selectedDay === key
          const isToday = key === today
          const isCurrentMonth = date.getMonth() === gridMonth.getMonth()
          const isFocusable = !isAriaHidden && key === focusKey

          return (
            <div
              key={key}
              className={`u030-cell ${selected ? "in" : ""} ${isStart ? "s" : ""} ${isEnd ? "e" : ""}`}
            >
              <button
                type="button"
                tabIndex={isFocusable ? 0 : -1}
                data-date={key}
                aria-label={`${
                  onSelectDay ? "Chọn ngày" : "Tuần chứa"
                } ${date.toLocaleDateString("vi-VN")}`}
                aria-pressed={onSelectDay ? isSelectedDay : selected}
                aria-current={isToday ? "date" : undefined}
                onClick={() => {
                  onChange(mondayOf(date))
                  onSelectDay?.(key)
                }}
                onKeyDown={(e) => handleKeyDown(e, date)}
                className={`u030-d ${
                  isSelectedDay
                    ? "is-selected-day font-semibold"
                    : selected
                      ? "text-ink"
                      : "hover:bg-raised"
                } ${
                  isSelectedDay
                    ? ""
                    : isToday
                      ? "font-bold text-brand-600"
                      : isCurrentMonth
                        ? "text-ink-soft"
                        : "text-ink-muted/60"
                } ${isToday ? "is-today" : ""}`}
              >
                {date.getDate()}
              </button>
            </div>
          )
        })}
      </div>
    )
  }

  return (
    <section
      aria-label="Chọn tuần trong lịch"
      className="u030-wrap rounded-2xl border border-brand-100 bg-paper p-4"
    >
      <div className="mb-4 flex items-center justify-between">
        <button
          type="button"
          aria-label="Tháng trước"
          onClick={() => moveMonth(-1)}
          className="u030-nav-btn"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>

        <div className="u030-month-container" aria-live="polite">
          {anim && (
            <span
              aria-hidden="true"
              className={`u030-month-title ${
                anim.dir === 1 ? "u030-title-out-up" : "u030-title-out-down"
              }`}
            >
              {anim.prevMonth.toLocaleDateString("vi-VN", {
                month: "long",
                year: "numeric",
              })}
            </span>
          )}
          <h2
            className={`u030-month-title ${
              anim
                ? anim.dir === 1
                  ? "u030-title-in-up"
                  : "u030-title-in-down"
                : "is-static"
            }`}
          >
            {month.toLocaleDateString("vi-VN", {
              month: "long",
              year: "numeric",
            })}
          </h2>
        </div>

        <button
          type="button"
          aria-label="Tháng sau"
          onClick={() => moveMonth(1)}
          className="u030-nav-btn"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>

      <div className="u030-wk" aria-hidden="true">
        {["T2", "T3", "T4", "T5", "T6", "T7", "CN"].map((day) => (
          <span key={day}>{day}</span>
        ))}
      </div>

      <div ref={viewRef} className="u030-view">
        {anim &&
          renderGrid(
            anim.prevMonth,
            anim.dir === 1 ? "u030-slide-out-left" : "u030-slide-out-right",
            true,
          )}
        {renderGrid(
          month,
          anim
            ? anim.dir === 1
              ? "u030-slide-in-right"
              : "u030-slide-in-left"
            : "is-static",
          false,
        )}
      </div>

      <button
        type="button"
        onClick={handleThisWeek}
        className="u030-today-btn mt-3 w-full rounded-full border border-brand-200 bg-brand-50/60 text-brand-700 hover:bg-brand-100 hover:border-brand-400 dark:border-brand-800 dark:bg-brand-950/40 dark:text-brand-300 py-2 text-xs font-semibold cursor-pointer shadow-2xs"
      >
        Tuần này
      </button>
    </section>
  )
}
