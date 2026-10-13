import { useEffect, useState } from "react"

import { ChevronLeft, ChevronRight } from "lucide-react"

export function localDateKey(date: Date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`
}

export function mondayOf(date: Date) {
  const monday = new Date(date)

  monday.setDate(monday.getDate() - ((monday.getDay() + 6) % 7))

  return localDateKey(monday)
}

export function shiftWeek(weekStart: string, offset: number) {
  const date = new Date(`${weekStart}T12:00:00`)

  date.setDate(date.getDate() + offset * 7)

  return localDateKey(date)
}

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

  useEffect(() => {
    setMonth(new Date(`${weekStart}T12:00:00`))
  }, [weekStart])

  const first = new Date(month.getFullYear(), month.getMonth(), 1, 12)

  const gridStart = new Date(`${mondayOf(first)}T12:00:00`)

  const selectedStart = new Date(`${weekStart}T12:00:00`)

  const selectedEnd = new Date(selectedStart)

  selectedEnd.setDate(selectedEnd.getDate() + 7)

  const today = localDateKey(new Date())

  const moveMonth = (offset: number) =>
    setMonth(
      (current) =>
        new Date(current.getFullYear(), current.getMonth() + offset, 1, 12),
    )

  return (
    <section
      aria-label="Chọn tuần trong lịch"
      className="rounded-2xl border border-brand-100 bg-paper p-4"
    >
      <div className="mb-4 flex items-center justify-between">
        <button
          type="button"
          aria-label="Tháng trước"
          onClick={() => moveMonth(-1)}
          className="rounded-full p-1.5 text-ink-muted hover:bg-raised"
        >
          <ChevronLeft className="h-4 w-4" />
        </button>
        <h2 className="text-xs font-semibold capitalize text-ink">
          {month.toLocaleDateString("vi-VN", {
            month: "long",

            year: "numeric",
          })}
        </h2>
        <button
          type="button"
          aria-label="Tháng sau"
          onClick={() => moveMonth(1)}
          className="rounded-full p-1.5 text-ink-muted hover:bg-raised"
        >
          <ChevronRight className="h-4 w-4" />
        </button>
      </div>
      <div className="grid grid-cols-7 gap-y-1 text-center text-[11px]">
        {["T2", "T3", "T4", "T5", "T6", "T7", "CN"].map((day) => (
          <span key={day} className="pb-2 text-ink-muted">
            {day}
          </span>
        ))}
        {Array.from({ length: 42 }, (_, index) => {
          const date = new Date(gridStart)

          date.setDate(date.getDate() + index)

          const key = localDateKey(date)

          const selected = date >= selectedStart && date < selectedEnd

          return (
            <button
              key={key}
              type="button"
              aria-label={`${
                onSelectDay ? "Chọn ngày" : "Tuần chứa"
              } ${date.toLocaleDateString("vi-VN")}`}
              aria-pressed={onSelectDay ? selectedDay === key : selected}
              aria-current={key === today ? "date" : undefined}
              onClick={() => {
                onChange(mondayOf(date))

                onSelectDay?.(key)
              }}
              className={`h-7 rounded-md transition-colors ${
                selectedDay === key
                  ? "bg-brand-600 text-on-brand font-semibold"
                  : selected
                    ? "bg-raised"
                    : "hover:bg-raised"
              } ${
                selectedDay === key
                  ? ""
                  : key === today
                    ? "font-bold text-brand-600"
                    : date.getMonth() === month.getMonth()
                      ? "text-ink-soft"
                      : "text-ink-muted/60"
              }`}
            >
              {date.getDate()}
            </button>
          )
        })}
      </div>
      <button
        type="button"
        onClick={() => onChange(mondayOf(new Date()))}
        className="mt-3 w-full rounded-full border border-brand-200 py-2 text-xs font-medium text-ink-soft hover:border-brand-600"
      >
        Tuần này
      </button>
    </section>
  )
}
