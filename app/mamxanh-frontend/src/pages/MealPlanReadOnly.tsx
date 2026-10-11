import { useEffect, useState } from "react"
import { Link } from "react-router-dom"
import {
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Clock3,
  LoaderCircle,
} from "lucide-react"
import { PageContainer } from "../components/Layout"
import { Card } from "../components/ui"
import {
  WeekCalendar,
  localDateKey,
  mondayOf,
  shiftWeek,
} from "../components/WeekCalendar"
import { mealPlanApi, type MealPlanWeek } from "../api/recipes"
import { asApiError } from "../lib/apiClient"

const mealLabels: Record<string, string> = {
  BREAKFAST: "Bữa sáng",
  LUNCH: "Bữa trưa",
  DINNER: "Bữa tối",
}

export function MealPlanReadOnly() {
  const [plan, setPlan] = useState<MealPlanWeek | null>(null)
  const [error, setError] = useState("")
  const [loading, setLoading] = useState(true)
  const [weekStartDate, setWeekStartDate] = useState(() => mondayOf(new Date()))
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    let active = true
    setLoading(true)
    setError("")
    setPlan(null)
    mealPlanApi
      .getWeek(weekStartDate)
      .then((value) => {
        if (active) setPlan(value)
      })
      .catch((cause) => {
        if (active) setError(asApiError(cause).message)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [weekStartDate, retry])

  const weekEnd = new Date(`${weekStartDate}T12:00:00`)
  weekEnd.setDate(weekEnd.getDate() + 6)
  const range = `${new Date(`${weekStartDate}T12:00:00`).toLocaleDateString("vi-VN")} – ${weekEnd.toLocaleDateString("vi-VN")}`

  return (
    <PageContainer className="py-6">
      <div className="grid items-start gap-5 md:grid-cols-[225px_minmax(0,1fr)]">
        <aside className="space-y-4">
          <WeekCalendar weekStart={weekStartDate} onChange={setWeekStartDate} />
          <Card className="p-4">
            <p className="text-sm font-semibold text-ink">
              Chuẩn bị cho tuần mới
            </p>
            <p className="mt-2 text-xs leading-5 text-ink-muted">
              Mở một công thức trong lịch để xem nguyên liệu và cách làm.
            </p>
            <Link
              to="/kham-pha"
              className="mt-3 inline-block text-xs font-semibold text-brand-600"
            >
              Khám phá công thức →
            </Link>
          </Card>
        </aside>
        <Card className="min-w-0 p-4 sm:p-6">
          <header className="mb-6 border-b border-brand-100 pb-4">
            <h1 className="flex items-center gap-2 text-xl font-semibold text-ink">
              <CalendarDays className="h-5 w-5 text-brand-600" />
              Lịch ăn của bạn
            </h1>
            <div className="mt-4 flex items-center justify-between gap-2">
              <button
                type="button"
                aria-label="Tuần trước"
                onClick={() =>
                  setWeekStartDate((value) => shiftWeek(value, -1))
                }
                className="rounded-full border border-brand-200 p-1.5 text-ink-muted hover:text-brand-600"
              >
                <ChevronLeft className="h-4 w-4" />
              </button>
              <p className="text-xs font-medium text-ink-soft">{range}</p>
              <button
                type="button"
                aria-label="Tuần sau"
                onClick={() => setWeekStartDate((value) => shiftWeek(value, 1))}
                className="rounded-full border border-brand-200 p-1.5 text-ink-muted hover:text-brand-600"
              >
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          </header>
          {loading && (
            <div
              role="status"
              className="flex items-center justify-center gap-2 py-16 text-sm text-ink-muted"
            >
              <LoaderCircle className="h-5 w-5 animate-spin" />
              Đang tải Lịch ăn…
            </div>
          )}
          {error && (
            <div
              role="alert"
              className="rounded-xl bg-raised p-4 text-sm text-ink-muted"
            >
              <p>{error}</p>
              <button
                type="button"
                onClick={() => setRetry((value) => value + 1)}
                className="mt-3 text-xs font-semibold text-brand-600"
              >
                Thử lại
              </button>
            </div>
          )}
          {!loading && !error && plan && (
            <div className="space-y-6">
              {plan.entries.length === 0 && (
                <p className="rounded-xl bg-raised px-4 py-5 text-center text-sm text-ink-muted">
                  Tuần này chưa có công thức trong Lịch ăn.
                </p>
              )}
              {Array.from({ length: 7 }, (_, offset) => {
                const date = new Date(`${weekStartDate}T12:00:00`)
                date.setDate(date.getDate() + offset)
                const key = localDateKey(date)
                const entries = plan.entries.filter(
                  (entry) => entry.mealDate === key,
                )
                return (
                  <section
                    key={key}
                    aria-label={date.toLocaleDateString("vi-VN")}
                  >
                    <h2
                      className={`mb-3 text-sm font-semibold capitalize ${
                        key === localDateKey(new Date())
                          ? "text-brand-600"
                          : "text-ink"
                      }`}
                    >
                      {date.toLocaleDateString("vi-VN", {
                        weekday: "long",
                        day: "numeric",
                        month: "long",
                      })}
                    </h2>
                    {entries.length === 0 ? (
                      <p className="border-l-2 border-brand-200 pl-3 text-xs text-ink-muted">
                        Chưa có món trong ngày này.
                      </p>
                    ) : (
                      <div className="grid gap-3 lg:grid-cols-3">
                        {entries.map((entry) => {
                          const unavailable =
                            entry.recipeDeleted ||
                            Boolean(entry.unavailableMessage) ||
                            !entry.recipeTitle
                          return (
                            <div
                              key={entry.entryId}
                              className="flex min-w-0 gap-3 rounded-xl bg-raised p-3"
                            >
                              {!unavailable && entry.recipeCoverUrl && (
                                <img
                                  src={entry.recipeCoverUrl}
                                  alt=""
                                  loading="lazy"
                                  className="h-16 w-16 shrink-0 rounded-lg object-cover"
                                />
                              )}
                              <div className="min-w-0">
                                <p className="text-[10px] font-semibold text-brand-600">
                                  {mealLabels[entry.mealType] ?? entry.mealType}
                                </p>
                                {unavailable ? (
                                  <p className="mt-2 text-xs leading-5 text-ink-muted">
                                    Công thức không còn khả dụng
                                  </p>
                                ) : (
                                  <>
                                    <Link
                                      to={`/cong-thuc/id/${entry.recipeId}`}
                                      className="mt-1 block line-clamp-2 text-xs font-semibold leading-5 text-ink hover:text-brand-600"
                                    >
                                      {entry.recipeTitle}
                                    </Link>
                                    <p className="mt-1 inline-flex items-center gap-1 text-[10px] text-ink-muted">
                                      <Clock3 className="h-3 w-3" />
                                      {entry.totalTimeMinutes === null
                                        ? ""
                                        : `${entry.totalTimeMinutes} phút · `}
                                      {entry.plannedServings} phần
                                    </p>
                                  </>
                                )}
                              </div>
                            </div>
                          )
                        })}
                      </div>
                    )}
                  </section>
                )
              })}
            </div>
          )}
        </Card>
      </div>
    </PageContainer>
  )
}
