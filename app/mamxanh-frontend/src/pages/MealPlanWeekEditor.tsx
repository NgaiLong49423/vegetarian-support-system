import { useEffect, useMemo, useRef, useState, type ReactNode } from "react"

import { Link, useSearchParams } from "react-router-dom"

import {
  CalendarDays,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Leaf,
  LoaderCircle,
  Moon,
  Plus,
  RefreshCw,
  Sparkles,
  Sun,
  Sunrise,
  Trash2,
  Utensils,
  ArrowRightLeft,
  X,
} from "lucide-react"

import { PageContainer } from "../components/Layout"

import { Button, Card } from "../components/ui"

import { Modal } from "../components/Modal"

import {
  WeekCalendar,
  localDateKey,
  mondayOf,
  shiftDays,
  getWindowDays,
  isToday,
  isPastDate,
} from "../components/WeekCalendar"

import { mealPlanApi, recipesApi, type AiMenuGoal, type AiMenuMode, type AiMenuPreview, type MealPlanWeek } from "../api/recipes"

import { savedRecipesApi } from "../services/savedRecipesApi"

import { asApiError } from "../lib/apiClient"

import { MealPlanNutrition } from "./MealPlanNutrition"
import { AiRecipePlanner } from "./AiRecipePlanner"
import { CreateShoppingListButton } from "../components/CreateShoppingListButton"
import { DayStrip } from "../components/DayStrip"

type Entry = MealPlanWeek["entries"][number]

type MealType = Entry["mealType"]

type Target = {
  mealDate: string

  mealType: MealType

  entry?: Entry
}

type Choice = {
  id: number

  title: string

  image: string | null

  available: boolean
}

const meals: MealType[] = ["BREAKFAST", "LUNCH", "DINNER"]

const mealLabels: Record<MealType, string> = {
  BREAKFAST: "Bữa sáng",

  LUNCH: "Bữa trưa",

  DINNER: "Bữa tối",
}

export function MealPlanWeekEditor() {
  const [searchParams, setSearchParams] = useSearchParams()

  const [windowStart, setWindowStart] = useState(() => {
    const requested = searchParams.get("weekStartDate") ?? ""

    return /^\d{4}-\d{2}-\d{2}$/.test(requested)
      ? requested
      : mondayOf(new Date())
  })

  const [view, setView] = useState<"plan" | "for-you">(() =>
    searchParams.get("view") === "for-you" ? "for-you" : "plan",
  )
  const [aiMode, setAiMode] = useState<"ingredients" | "recipe-week" | "nutrition-week">("nutrition-week")

  const [calendarOpen, setCalendarOpen] = useState(false)

  const [selectedDay, setSelectedDay] = useState<string | null>(() => {
    const requested = searchParams.get("mealDate") ?? ""

    if (/^\d{4}-\d{2}-\d{2}$/.test(requested))
      return requested

    return windowStart === mondayOf(new Date()) ? localDateKey(new Date()) : windowStart
  })

  useEffect(() => {
    if (
      searchParams.get("weekStartDate") !== windowStart ||
      searchParams.get("mealDate") !== selectedDay ||
      searchParams.get("view") !== (view === "for-you" ? "for-you" : null)
    ) {
      setSearchParams(
        {
          weekStartDate: windowStart,
          ...(selectedDay ? { mealDate: selectedDay } : {}),
          ...(view === "for-you" ? { view: "for-you" } : {}),
        },
        { replace: true },
      )
    }
  }, [windowStart, selectedDay, view, searchParams, setSearchParams])

  function changeWindow(value: string) {
    setWindowStart(value)
    setSelectedDay(value === mondayOf(new Date()) ? localDateKey(new Date()) : value)
  }

  const windowEnd = shiftDays(windowStart, 6)
  const isAtToday = windowStart === mondayOf(new Date())

  return (
    <PageContainer className="py-6">
      <div className="grid items-start gap-6 lg:grid-cols-[250px_minmax(0,1fr)]">
        <aside className="space-y-4 lg:sticky lg:top-6">
          <Button
            variant="outline"
            className="w-full lg:hidden rounded-2xl shadow-2xs"
            aria-expanded={calendarOpen}
            aria-controls="planner-calendar"
            onClick={() => setCalendarOpen((value) => !value)}
          >
            <CalendarDays className="h-4 w-4" />
            Chọn ngày trong lịch
          </Button>
          <div
            id="planner-calendar"
            className={calendarOpen ? "block" : "hidden lg:block"}
          >
            <WeekCalendar
              weekStart={windowStart}
              onChange={changeWindow}
              selectedDay={selectedDay}
              onSelectDay={setSelectedDay}
            />
          </div>
          <Card className="hidden p-4 lg:block border-brand-100/80 dark:border-brand-800/60 shadow-2xs">
            <div className="flex items-center gap-2 text-sm font-bold text-ink">
              <Sparkles className="h-4 w-4 text-brand-600 dark:text-brand-400" />
              <span>Lên lịch từng ngày</span>
            </div>
            <p className="mt-2 text-xs leading-relaxed text-ink-muted">
              Chọn một ngày, thêm món từ công thức đã lưu và theo dõi dinh dưỡng trọn vẹn mỗi bữa ăn.
            </p>
            <Link
              to="/ho-so"
              className="mt-3 inline-flex items-center gap-1 text-xs font-bold text-brand-600 hover:text-brand-700 dark:text-brand-400 hover:underline group"
            >
              <span>Xem công thức đã lưu</span>
              <span className="transition-transform group-hover:translate-x-0.5">→</span>
            </Link>
          </Card>
        </aside>
        <Card className="min-w-0 p-4 sm:p-6 shadow-xs border-brand-100/90 dark:border-brand-800/60">
          <header className="mb-6 pb-4 border-b border-brand-100/60 dark:border-brand-900/40">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
              <div className="flex items-center gap-3">
                <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-brand-50 dark:bg-brand-950/60 border border-brand-100 dark:border-brand-800/60 text-brand-600 dark:text-brand-400 shrink-0 shadow-2xs">
                  <CalendarDays className="h-6 w-6" />
                </div>
                <div>
                  <h1
                    tabIndex={-1}
                    className="text-xl sm:text-2xl font-bold tracking-tight text-ink"
                  >
                    Lịch ăn của bạn
                  </h1>
                  <p className="text-xs sm:text-sm text-ink-muted mt-0.5">
                    Lên kế hoạch và theo dõi dinh dưỡng cho 7 ngày tới
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-2 self-start sm:self-auto flex-wrap">
                <div className="inline-flex items-center gap-1 rounded-full border border-brand-200/80 dark:border-brand-800/60 bg-raised/50 dark:bg-raised/30 p-1 shadow-2xs">
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label="Tuần trước"
                    onClick={() => changeWindow(shiftDays(windowStart, -7))}
                    className="h-7 w-7 rounded-full p-0 text-ink-muted hover:text-ink hover:bg-paper"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>
                  <span className="px-2 text-xs font-semibold text-ink whitespace-nowrap">
                    {new Date(`${windowStart}T12:00:00`).toLocaleDateString("vi-VN")} –{" "}
                    {new Date(`${windowEnd}T12:00:00`).toLocaleDateString("vi-VN")}
                  </span>
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label="Tuần sau"
                    onClick={() => changeWindow(shiftDays(windowStart, 7))}
                    className="h-7 w-7 rounded-full p-0 text-ink-muted hover:text-ink hover:bg-paper"
                  >
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>

                {!isAtToday && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => changeWindow(mondayOf(new Date()))}
                    className="h-8 rounded-full border border-brand-200 bg-brand-50/80 px-3 text-xs font-semibold text-brand-700 hover:bg-brand-100 dark:border-brand-800 dark:bg-brand-950/60 dark:text-brand-300 transition-all shadow-2xs"
                  >
                    Về hôm nay
                  </Button>
                )}
              </div>
            </div>

            <div className="mt-5 inline-flex p-1 rounded-2xl bg-raised/60 dark:bg-raised/40 border border-brand-100/60 dark:border-brand-900/40" role="tablist" aria-label="Chế độ Lịch ăn">
              <button
                type="button"
                role="tab"
                aria-selected={view === "plan"}
                onClick={() => setView("plan")}
                className={`rounded-xl px-4 py-2 text-sm font-semibold transition-all duration-200 ${
                  view === "plan"
                    ? "bg-paper dark:bg-surface text-brand-700 dark:text-brand-400 shadow-xs"
                    : "text-ink-muted hover:text-ink"
                }`}
              >
                Lịch của tôi
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={view === "for-you"}
                onClick={() => setView("for-you")}
                className={`rounded-xl px-4 py-2 text-sm font-semibold transition-all duration-200 ${
                  view === "for-you"
                    ? "bg-paper dark:bg-surface text-brand-700 dark:text-brand-400 shadow-xs"
                    : "text-ink-muted hover:text-ink"
                }`}
              >
                Dành cho bạn
              </button>
            </div>
          </header>
          {view === "plan" ? (
            <WeekContents
              key={windowStart}
              windowStart={windowStart}
              selectedDay={selectedDay}
              onSelectDay={setSelectedDay}
            />
          ) : (
            <div>
              <div className="mb-5 flex flex-wrap gap-2 border-b border-brand-100" role="tablist" aria-label="Cách chọn thực đơn">
                <button id="ai-mode-ingredients" type="button" role="tab" aria-controls="ai-mode-panel" aria-selected={aiMode === "ingredients"} onClick={() => setAiMode("ingredients")} className={`border-b-2 px-3 py-2 text-sm font-semibold ${aiMode === "ingredients" ? "border-brand-600 text-brand-800" : "border-transparent text-ink-muted hover:text-ink"}`}>
                  Món theo nguyên liệu
                </button>
                <button id="ai-mode-recipe-week" type="button" role="tab" aria-controls="ai-mode-panel" aria-selected={aiMode === "recipe-week"} onClick={() => setAiMode("recipe-week")} className={`border-b-2 px-3 py-2 text-sm font-semibold ${aiMode === "recipe-week" ? "border-brand-600 text-brand-800" : "border-transparent text-ink-muted hover:text-ink"}`}>
                  Thực đơn tuần
                </button>
                <button id="ai-mode-nutrition-week" type="button" role="tab" aria-controls="ai-mode-panel" aria-selected={aiMode === "nutrition-week"} onClick={() => setAiMode("nutrition-week")} className={`border-b-2 px-3 py-2 text-sm font-semibold ${aiMode === "nutrition-week" ? "border-brand-600 text-brand-800" : "border-transparent text-ink-muted hover:text-ink"}`}>
                  Theo nhu cầu dinh dưỡng
                </button>
              </div>
              <div id="ai-mode-panel" role="tabpanel" aria-labelledby={`ai-mode-${aiMode}`}>
                {aiMode === "ingredients" && <AiRecipePlanner key={`${windowStart}-ingredients`} weekStartDate={mondayOf(new Date(`${windowStart}T12:00:00`))} embedded plannerMode="ingredients" />}
                {aiMode === "recipe-week" && <AiRecipePlanner key={`${windowStart}-recipe-week`} weekStartDate={mondayOf(new Date(`${windowStart}T12:00:00`))} embedded plannerMode="weekly" />}
                {aiMode === "nutrition-week" && <AiWeeklyMenu key={`${windowStart}-nutrition-week`} week={mondayOf(new Date(`${windowStart}T12:00:00`))} />}
              </div>
            </div>
          )}
        </Card>
      </div>
    </PageContainer>
  )
}

function WeekContents({
  windowStart,
  selectedDay,
  onSelectDay,
}: {
  windowStart: string
  selectedDay: string | null
  onSelectDay: (day: string | null) => void
}) {
  const windowDays = useMemo(() => getWindowDays(windowStart, 7), [windowStart])
  const startMonday = mondayOf(new Date(`${windowStart}T12:00:00`))
  const endMonday = mondayOf(new Date(`${windowDays[6]}T12:00:00`))

  const [plan, setPlan] = useState<MealPlanWeek | null>(null)
  const windowEntries = useMemo(() => {
    return plan ? plan.entries.filter((entry) => windowDays.includes(entry.mealDate)) : []
  }, [plan, windowDays])
  const scheduledSlotsCount = useMemo(() => {
    let count = 0
    if (!plan) return 0
    for (const day of windowDays) {
      const dayEntries = plan.entries.filter((e) => e.mealDate === day)
      if (dayEntries.some((e) => e.mealType === "BREAKFAST")) count++
      if (dayEntries.some((e) => e.mealType === "LUNCH")) count++
      if (dayEntries.some((e) => e.mealType === "DINNER")) count++
    }
    return count
  }, [plan, windowDays])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState("")
  const [actionError, setActionError] = useState("")
  const [notice, setNotice] = useState("")
  const [retry, setRetry] = useState(0)
  const [busy, setBusy] = useState(false)
  const [picker, setPicker] = useState<Target | null>(null)
  const [removal, setRemoval] = useState<Entry | null>(null)
  const [moving, setMoving] = useState<Entry | null>(null)
  const [nutrition, setNutrition] = useState<{
    day: string
    scope: "day" | "week"
  } | null>(null)

  const alive = useRef(true)
  const pending = useRef(false)
  const returnFocus = useRef<HTMLElement | null>(null)

  useEffect(() => {
    alive.current = true
    return () => {
      alive.current = false
    }
  }, [])

  useEffect(() => {
    let active = true
    setLoading(true)
    setLoadError("")

    const fetchPromise = startMonday === endMonday
      ? mealPlanApi.getWeek(startMonday).then((res) => [res])
      : Promise.all([
          mealPlanApi.getWeek(startMonday),
          mealPlanApi.getWeek(endMonday),
        ])

    fetchPromise
      .then((weeks) => {
        if (!active) return
        const entryMap = new Map<number, Entry>()
        weeks.forEach((w) => {
          w.entries.forEach((e) => entryMap.set(e.entryId, e))
        })
        setPlan({
          weekStartDate: startMonday,
          entries: Array.from(entryMap.values()),
        })
      })
      .catch((cause) => {
        if (active) setLoadError(asApiError(cause).message)
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [windowStart, startMonday, endMonday, retry])

  function restoreDialogFocus() {
    requestAnimationFrame(() => {
      if (!alive.current) return

      if (returnFocus.current?.isConnected) returnFocus.current.focus()
      else document.querySelector<HTMLElement>("h1")?.focus()
    })
  }

  function closeDialog() {
    setPicker(null)

    setRemoval(null)

    setNutrition(null)

    setMoving(null)
    setActionError("")

    restoreDialogFocus()
  }

  function openPicker(target: Target) {
    returnFocus.current = (document.activeElement as HTMLElement)

    setActionError("")

    setPicker(target)
  }

  async function mutate(action: () => Promise<void>, message: string) {
    if (pending.current) return

    pending.current = true

    setBusy(true)

    setActionError("")

    setNotice("")

    try {
      await action()

      if (!alive.current) return

      setNotice(message)

      setPicker(null)

      setRemoval(null)
      setMoving(null)

      restoreDialogFocus()
    } catch (cause) {
      if (alive.current) setActionError(asApiError(cause).message)
    } finally {
      pending.current = false

      if (alive.current) setBusy(false)
    }
  }

  function choose(recipeId: number) {
    if (!picker) return

    const target = picker
    const targetWeek = mondayOf(new Date(`${target.mealDate}T12:00:00`))

    void mutate(
      async () => {
        const entry = target.entry
          ? await mealPlanApi.replaceEntry(targetWeek, target.entry.entryId, recipeId)
          : await mealPlanApi.addEntry(targetWeek, {
              mealDate: target.mealDate,

              mealType: target.mealType,

              recipeId,
            })

        if (alive.current)
          setPlan(
            (current) =>
              current && {
                ...current,

                entries: [
                  ...current.entries.filter(
                    (item) => item.entryId !== entry.entryId,
                  ),

                  entry,
                ],
              },
          )
      },

      target.entry
        ? "Đã thay công thức trong lịch ăn."
        : "Đã thêm món vào lịch ăn.",
    )
  }

  function move(mealDate: string, mealType: MealType) {
    if (!moving) return
    const id = moving.entryId
    const sourceWeek = mondayOf(new Date(`${moving.mealDate}T12:00:00`))
    void mutate(async () => {
      const entry = await mealPlanApi.moveEntry(sourceWeek, id, {
        mealDate,
        mealType,
      })
      if (alive.current) {
        setPlan(
          (current) =>
            current && {
              ...current,
              entries: current.entries.map((item) =>
                item.entryId === id ? entry : item,
              ),
            },
        )
        onSelectDay(mealDate)
      }
    }, "Đã chuyển món sang bữa được chọn.")
  }
  if (loading)
    return (
      <p
        role="status"
        className="flex items-center justify-center gap-2 py-16 text-sm text-ink-muted"
      >
        <LoaderCircle className="h-5 w-5 animate-spin" />
        Đang tải Lịch ăn…
      </p>
    )

  if (loadError)
    return (
      <div
        role="alert"
        className="rounded-xl bg-raised p-4 text-sm text-ink-muted"
      >
        <p>{loadError}</p>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => setRetry((value) => value + 1)}
        >
          Thử lại
        </Button>
      </div>
    )

  if (!plan) return null

  return (
    <div className="space-y-6" aria-busy={busy}>
      {notice && (
        <p
          role="status"
          className="rounded-xl bg-brand-50 p-3 text-sm text-brand-700"
        >
          {notice}
        </p>
      )}
      {busy && !picker && !removal && !moving && (
        <p role="status" className="text-sm text-ink-muted">
          Đang lưu lịch ăn…
        </p>
      )}
      {actionError && !picker && !removal && !moving && (
        <p
          role="alert"
          className="rounded-xl bg-raised p-3 text-sm text-ink-muted"
        >
          {actionError}
        </p>
      )}
      {/* 1. Day Strip (Sticky 7-day pill strip) */}
      <DayStrip
        windowDays={windowDays}
        selectedDay={selectedDay}
        entries={plan.entries}
        onSelectDay={onSelectDay}
      />

      {/* 2. Banner trạng thái (Empty state thân thiện hoặc Progress Summary) */}
      {windowEntries.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-brand-200/90 dark:border-brand-800/80 bg-gradient-to-b from-brand-50/50 to-raised/20 dark:from-paper/50 dark:to-raised/30 p-6 sm:p-8 text-center transition-all">
          <div className="mx-auto mb-3.5 flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-100/80 text-brand-600 dark:bg-brand-950/60 dark:text-brand-400 shadow-2xs">
            <Sparkles className="h-7 w-7" />
          </div>
          <h3 className="text-base font-bold text-ink">
            7 ngày này chưa có công thức trong Lịch ăn
          </h3>
          <p className="mx-auto mt-1 max-w-md text-xs sm:text-sm text-ink-muted leading-relaxed">
            Lên lịch từng bữa để cân đối dinh dưỡng và tạo danh sách đi chợ nhanh chóng.
          </p>
          <div className="mt-4 flex flex-wrap items-center justify-center gap-3">
            <Button
              variant="outline"
              size="sm"
              disabled={busy}
              onClick={() =>
                void mutate(async () => {
                  if (startMonday === endMonday) {
                    const result = await mealPlanApi.openWeek(startMonday)
                    if (alive.current) setPlan(result)
                  } else {
                    const [res1, res2] = await Promise.all([
                      mealPlanApi.openWeek(startMonday),
                      mealPlanApi.openWeek(endMonday),
                    ])
                    if (alive.current) {
                      const entryMap = new Map<number, Entry>()
                      res1.entries.forEach((e) => entryMap.set(e.entryId, e))
                      res2.entries.forEach((e) => entryMap.set(e.entryId, e))
                      setPlan({
                        weekStartDate: startMonday,
                        entries: Array.from(entryMap.values()),
                      })
                    }
                  }
                }, "Đã lưu lịch tuần. Bạn có thể thêm món cho từng bữa.")
              }
              className="rounded-full shadow-2xs"
            >
              Lưu lịch tuần trống
            </Button>
            <Link
              to="/kham-pha"
              className="inline-flex items-center justify-center rounded-full bg-brand-600 px-4 py-2 text-xs sm:text-sm font-semibold text-on-brand shadow-xs hover:bg-brand-700 transition-colors"
            >
              Khám phá món ngon →
            </Link>
          </div>
        </div>
      ) : (
        <div className="rounded-2xl border border-brand-100/80 dark:border-brand-800/60 bg-paper dark:bg-surface p-4 shadow-2xs">
          <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
            <div className="flex items-center gap-2">
              <span className="flex h-6 w-6 items-center justify-center rounded-lg bg-leaf-100 dark:bg-leaf-900/40 text-leaf-700 dark:text-leaf-300">
                <Leaf className="h-3.5 w-3.5" />
              </span>
              <span className="font-semibold text-ink">Tiến độ thực đơn tuần</span>
            </div>
            <span className="font-medium text-brand-600 dark:text-brand-400">
              {scheduledSlotsCount} / 21 bữa đã lên lịch ({Math.round((scheduledSlotsCount / 21) * 100)}%)
            </span>
          </div>
          <div className="mt-2.5 h-1.5 w-full overflow-hidden rounded-full bg-brand-50 dark:bg-brand-950/40">
            <div
              className="h-full rounded-full bg-brand-600 transition-all duration-500 ease-out"
              style={{ width: `${Math.min(100, Math.round((scheduledSlotsCount / 21) * 100))}%` }}
            />
          </div>
        </div>
      )}

      {/* 3. Thanh hành động */}
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl bg-raised/40 dark:bg-raised/20 p-2.5 sm:p-3 border border-brand-100/60 dark:border-brand-900/40">
        <div className="flex items-center gap-2">
          <CreateShoppingListButton week={startMonday} disabled={busy || windowEntries.length === 0} />
          {windowEntries.length === 0 && (
            <span className="hidden sm:inline-block text-[11px] text-ink-muted">
              (Thêm món để tạo danh sách)
            </span>
          )}
        </div>
        <Button
          variant="outline"
          size="sm"
          disabled={windowEntries.length === 0}
          title={windowEntries.length === 0 ? "Thêm món để xem dinh dưỡng" : "Xem dinh dưỡng cả tuần"}
          onClick={() => {
            returnFocus.current = (document.activeElement as HTMLElement)
            setNutrition({ day: selectedDay ?? windowStart, scope: "week" })
          }}
          className="rounded-full shadow-2xs hover:border-brand-400"
        >
          <Utensils className="h-4 w-4" />
          Dinh dưỡng cả tuần
        </Button>
      </div>
      <div className="space-y-4">
        {windowDays.map((day, displayIndex) => {
          const date = new Date(`${day}T12:00:00`)
          const label = date.toLocaleDateString("vi-VN", {
            weekday: "long",
            day: "numeric",
            month: "long",
          })
          const entries = plan.entries.filter((entry) => entry.mealDate === day)
          const expanded = selectedDay === day
          const today = isToday(day)
          const dayOfWeek = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"][date.getDay()]
          const dayNum = date.getDate()
          const isTomorrow = localDateKey(new Date(Date.now() + 86400000)) === day
          const isWeekend = date.getDay() === 0 || date.getDay() === 6

          const breakfastCount = entries.filter((e) => e.mealType === "BREAKFAST").length
          const lunchCount = entries.filter((e) => e.mealType === "LUNCH").length
          const dinnerCount = entries.filter((e) => e.mealType === "DINNER").length

          return (
            <div key={day} id={`day-section-${day}`}>
              {displayIndex === 1 && (
                <div className="py-2 px-2 text-xs font-bold uppercase tracking-wider text-ink-muted flex items-center gap-3">
                  <span className="h-px flex-1 bg-brand-100 dark:bg-brand-900/60" />
                  <span className="flex items-center gap-1.5">
                    <Sparkles className="h-3.5 w-3.5 text-brand-500" />
                    Các ngày tiếp theo
                  </span>
                  <span className="h-px flex-1 bg-brand-100 dark:bg-brand-900/60" />
                </div>
              )}
              <div
                className={`rounded-2xl border transition-all duration-200 overflow-hidden ${
                  expanded
                    ? "border-brand-200 dark:border-brand-800/80 bg-paper dark:bg-surface shadow-xs"
                    : "border-brand-100/70 dark:border-brand-900/50 bg-paper/60 hover:bg-paper dark:bg-surface/50 dark:hover:bg-surface hover:border-brand-200 hover:shadow-2xs"
                } ${today ? "border-l-4 border-l-brand-600 dark:border-l-brand-500 day-card-today-glow border-brand-300 dark:border-brand-700/80" : ""}`}
              >
                <section
                  aria-label={date.toLocaleDateString("vi-VN")}
                  className="p-3 sm:p-4"
                >
                  <div className="flex items-center justify-between gap-3">
                    <h2 className="min-w-0 flex-1">
                      <button
                        type="button"
                        aria-label={`Xem ngày ${date.toLocaleDateString("vi-VN")}`}
                        aria-expanded={expanded}
                        aria-controls={`meals-${day}`}
                        onClick={() => onSelectDay(expanded ? null : day)}
                        className="flex w-full items-center gap-3 rounded-xl p-1 text-left hover:bg-raised/50 dark:hover:bg-raised/30 focus-visible:outline-2 focus-visible:outline-brand-600 transition-colors"
                      >
                        {/* Mini date block */}
                        <div
                          className={`flex h-11 w-11 shrink-0 flex-col items-center justify-center rounded-xl font-bold transition-colors ${
                            today
                              ? "bg-brand-600 text-on-brand shadow-xs day-badge-today-glow ring-2 ring-brand-300/80 dark:ring-brand-400/80"
                              : "bg-raised/70 dark:bg-raised/40 text-ink border border-brand-100/50 dark:border-brand-900/40"
                          }`}
                        >
                          <span
                            className={`text-[9px] uppercase tracking-wider leading-none ${
                              today ? "text-on-brand/90" : "text-ink-muted"
                            }`}
                          >
                            {dayOfWeek}
                          </span>
                          <span className="text-sm font-extrabold leading-none mt-1">
                            {dayNum}
                          </span>
                        </div>

                        <div className="min-w-0 flex-1">
                          <div className="flex flex-wrap items-center gap-1.5">
                            <span className="text-sm font-bold capitalize text-ink">
                              {today ? `Hôm nay · ${label}` : label}
                            </span>
                            {today && (
                              <span className="rounded-full bg-brand-600 px-2 py-0.5 text-[10px] font-bold text-on-brand">
                                Hôm nay
                              </span>
                            )}
                            {isTomorrow && !today && (
                              <span className="rounded-full bg-brand-100 px-2 py-0.5 text-[10px] font-semibold text-brand-700 dark:bg-brand-950/60 dark:text-brand-300">
                                Ngày mai
                              </span>
                            )}
                            {isWeekend && !today && !isTomorrow && (
                              <span className="rounded-full bg-raised px-2 py-0.5 text-[10px] font-medium text-ink-muted">
                                Cuối tuần
                              </span>
                            )}
                          </div>

                          {/* Collapsed meal preview */}
                          {!expanded && (
                            <div className="mt-1 flex flex-wrap items-center gap-2">
                              {entries.length > 0 ? (
                                <>
                                  <span className="rounded-full bg-brand-50 dark:bg-brand-950/40 px-2 py-0.5 text-xs text-brand-700 dark:text-brand-300 font-semibold border border-brand-100 dark:border-brand-900/40">
                                    {entries.length} món
                                  </span>
                                  <span className="flex items-center gap-2.5 text-xs text-ink-muted font-medium">
                                    <span
                                      className={`inline-flex items-center gap-1 ${
                                        breakfastCount > 0
                                          ? "text-amber-700 dark:text-amber-300 font-semibold"
                                          : "text-ink-muted/60"
                                      }`}
                                      title="Bữa sáng"
                                    >
                                      <Sunrise className="h-3.5 w-3.5 text-amber-500" />
                                      <span>{breakfastCount}</span>
                                    </span>
                                    <span className="text-brand-200 dark:text-brand-800">·</span>
                                    <span
                                      className={`inline-flex items-center gap-1 ${
                                        lunchCount > 0
                                          ? "text-brand-700 dark:text-brand-300 font-semibold"
                                          : "text-ink-muted/60"
                                      }`}
                                      title="Bữa trưa"
                                    >
                                      <Sun className="h-3.5 w-3.5 text-brand-500" />
                                      <span>{lunchCount}</span>
                                    </span>
                                    <span className="text-brand-200 dark:text-brand-800">·</span>
                                    <span
                                      className={`inline-flex items-center gap-1 ${
                                        dinnerCount > 0
                                          ? "text-indigo-700 dark:text-indigo-300 font-semibold"
                                          : "text-ink-muted/60"
                                      }`}
                                      title="Bữa tối"
                                    >
                                      <Moon className="h-3.5 w-3.5 text-indigo-500" />
                                      <span>{dinnerCount}</span>
                                    </span>
                                  </span>
                                </>
                              ) : (
                                <span className="text-xs text-ink-muted font-normal">
                                  {isPastDate(day)
                                    ? "Không có món nào được lên lịch"
                                    : "Chưa lên lịch · thêm món khi bạn sẵn sàng"}
                                </span>
                              )}
                            </div>
                          )}

                          {expanded && (
                            <span className="mt-0.5 block text-xs text-ink-muted">
                              {entries.length
                                ? `${entries.length} món đã lên lịch`
                                : isPastDate(day)
                                  ? "Không có món nào được lên lịch"
                                  : "Chưa lên lịch · thêm món khi bạn sẵn sàng"}
                            </span>
                          )}
                        </div>

                        <div className="flex items-center gap-1 shrink-0">
                          <div
                            className={`flex h-7 w-7 items-center justify-center rounded-full text-ink-muted transition-transform duration-200 ${
                              expanded
                                ? "rotate-180 text-brand-600 bg-brand-50 dark:bg-brand-950/40"
                                : ""
                            }`}
                          >
                            <ChevronDown className="h-4 w-4" />
                          </div>
                        </div>
                      </button>
                    </h2>

                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={busy}
                      aria-label={`Lên lịch ngày ${date.toLocaleDateString("vi-VN")}`}
                      onClick={() => onSelectDay(day)}
                      className="h-8 w-8 rounded-full p-0 text-ink-muted hover:text-brand-600 hover:bg-brand-50 dark:hover:bg-brand-950/40 shrink-0"
                    >
                      <Plus className="h-4 w-4" />
                    </Button>
                  </div>

                  {/* Expanded Content */}
                  {expanded && (
                    <div
                      id={`meals-${day}`}
                      className="mt-4 pt-3 border-t border-brand-100/60 dark:border-brand-900/40 space-y-4 animate-in fade-in-50 duration-200"
                    >
                      <div className="flex justify-end">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => {
                            returnFocus.current = (document.activeElement as HTMLElement)
                            setNutrition({ day, scope: "day" })
                          }}
                          className="rounded-full text-xs font-semibold text-brand-700 dark:text-brand-300 hover:bg-brand-50 dark:hover:bg-brand-950/40"
                        >
                          <Utensils className="h-3.5 w-3.5" />
                          Xem dinh dưỡng ngày
                        </Button>
                      </div>

                      {/* 3 Meal Columns */}
                      <div className="grid grid-cols-1 md:grid-cols-3 gap-3.5 items-stretch">
                        {meals.map((meal) => {
                          const mealEntries = entries
                            .filter((entry) => entry.mealType === meal)
                            .reverse()
                          const isBreakfast = meal === "BREAKFAST"
                          const isLunch = meal === "LUNCH"
                          const MealIcon = isBreakfast ? Sunrise : isLunch ? Sun : Moon

                          return (
                            <section
                              key={meal}
                              aria-label={`${mealLabels[meal]} ${label}`}
                              className={`min-w-0 flex flex-col rounded-2xl border p-3.5 transition-all ${
                                isBreakfast
                                  ? "border-amber-200/80 bg-amber-50/20 dark:border-amber-900/40 dark:bg-amber-950/10"
                                  : isLunch
                                    ? "border-brand-200/80 bg-brand-50/20 dark:border-brand-900/40 dark:bg-brand-950/10"
                                    : "border-indigo-200/80 bg-indigo-50/20 dark:border-indigo-900/40 dark:bg-indigo-950/10"
                              }`}
                            >
                              <div className="mb-2.5 flex items-center justify-between gap-2 border-b border-brand-100/60 dark:border-brand-900/40 pb-2">
                                <div className="flex items-center gap-2 min-w-0">
                                  <div
                                    className={`flex h-7 w-7 items-center justify-center rounded-xl shrink-0 ${
                                      isBreakfast
                                        ? "bg-amber-100 text-amber-700 dark:bg-amber-950/60 dark:text-amber-300"
                                        : isLunch
                                          ? "bg-brand-100 text-brand-700 dark:bg-brand-950/60 dark:text-brand-300"
                                          : "bg-indigo-100 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300"
                                    }`}
                                  >
                                    <MealIcon className="h-4 w-4" />
                                  </div>
                                  <h3 className="text-xs sm:text-sm font-bold text-ink truncate">
                                    {mealLabels[meal]}
                                  </h3>
                                  <span className="rounded-full bg-paper dark:bg-surface px-2 py-0.5 text-[10px] font-semibold text-ink-muted border border-brand-100/60 dark:border-brand-900/40 shrink-0">
                                    {mealEntries.length} món
                                  </span>
                                </div>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  disabled={busy}
                                  aria-label={`Thêm món ${mealLabels[meal]} ${label}`}
                                  onClick={() => openPicker({ mealDate: day, mealType: meal })}
                                  className="h-7 rounded-full px-2 text-xs font-semibold text-brand-700 dark:text-brand-300 hover:bg-brand-100/60 dark:hover:bg-brand-900/40 shrink-0"
                                >
                                  <Plus className="h-3.5 w-3.5" />
                                  Thêm món
                                </Button>
                              </div>

                              <div className="flex-1 space-y-2">
                                {mealEntries.map((entry) => {
                                  const unavailable =
                                    entry.recipeDeleted ||
                                    Boolean(entry.unavailableMessage) ||
                                    !entry.recipeTitle

                                  const name = unavailable
                                    ? "Công thức không còn khả dụng"
                                    : entry.recipeTitle!

                                  return (
                                    <article
                                      key={entry.entryId}
                                      aria-label={name}
                                      className="flex min-w-0 flex-col gap-2 rounded-xl bg-paper dark:bg-surface p-2.5 shadow-2xs border border-brand-100/80 dark:border-brand-800/60 hover:border-brand-300 dark:hover:border-brand-700 transition-colors"
                                    >
                                      <div className="flex items-center gap-2.5 min-w-0">
                                        {!unavailable && entry.recipeCoverUrl ? (
                                          <img
                                            src={entry.recipeCoverUrl}
                                            alt=""
                                            loading="lazy"
                                            className="h-12 w-12 shrink-0 rounded-lg object-cover"
                                          />
                                        ) : (
                                          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-lg bg-raised text-ink-muted">
                                            <Utensils className="h-4 w-4" />
                                          </div>
                                        )}
                                        <div className="min-w-0 flex-1">
                                          {unavailable ? (
                                            <p className="text-xs text-ink-muted line-clamp-2">
                                              {name}
                                            </p>
                                          ) : (
                                            <Link
                                              to={`/cong-thuc/id/${entry.recipeId}?mealPlanWeek=${mondayOf(new Date(`${day}T12:00:00`))}&mealPlanEntryId=${entry.entryId}&mealDate=${day}`}
                                              className="block text-xs font-bold text-ink hover:text-brand-600 dark:hover:text-brand-400 line-clamp-2"
                                              title={name}
                                            >
                                              {name}
                                            </Link>
                                          )}
                                          <p className="mt-0.5 flex items-center gap-1 text-[11px] text-ink-muted">
                                            <Clock3 className="h-3 w-3 shrink-0" />
                                            {entry.totalTimeMinutes === null
                                              ? ""
                                              : `${entry.totalTimeMinutes} phút · `}
                                            {entry.plannedServings.toLocaleString("vi-VN")} phần
                                          </p>
                                        </div>
                                      </div>

                                      <div className="flex items-center justify-end gap-1 border-t border-brand-100/50 dark:border-brand-900/30 pt-1.5">
                                        <Button
                                          variant="ghost"
                                          size="sm"
                                          disabled={busy}
                                          aria-label={`Chuyển món ${name}`}
                                          onClick={() => {
                                            returnFocus.current =
                                              (document.activeElement as HTMLElement)
                                            setActionError("")
                                            setMoving(entry)
                                          }}
                                          className="h-6 rounded-md px-1.5 text-[11px] font-medium"
                                        >
                                          <ArrowRightLeft className="h-3 w-3 mr-0.5" />
                                          Chuyển
                                        </Button>
                                        <Button
                                          variant="ghost"
                                          size="sm"
                                          disabled={busy}
                                          aria-label={`Thay món ${name}`}
                                          onClick={() =>
                                            openPicker({
                                              mealDate: day,
                                              mealType: meal,
                                              entry,
                                            })
                                          }
                                          className="h-6 rounded-md px-1.5 text-[11px] font-medium"
                                        >
                                          <RefreshCw className="h-3 w-3 mr-0.5" />
                                          Thay
                                        </Button>
                                        <Button
                                          variant="ghost"
                                          size="sm"
                                          disabled={busy}
                                          aria-label={`Gỡ món ${name}`}
                                          onClick={() => {
                                            returnFocus.current =
                                              (document.activeElement as HTMLElement)
                                            setActionError("")
                                            setRemoval(entry)
                                          }}
                                          className="h-6 rounded-md px-1.5 text-[11px] font-medium text-ink-muted hover:text-red-600 dark:hover:text-red-400"
                                        >
                                          <Trash2 className="h-3 w-3" />
                                        </Button>
                                      </div>
                                    </article>
                                  )
                                })}

                                {mealEntries.length === 0 && (
                                  <button
                                    type="button"
                                    disabled={busy}
                                    onClick={() =>
                                      openPicker({ mealDate: day, mealType: meal })
                                    }
                                    className="w-full flex-1 min-h-[96px] rounded-xl border-2 border-dashed border-brand-200/70 hover:border-brand-500 hover:bg-brand-50/40 dark:border-brand-800/70 dark:hover:bg-brand-950/20 p-3 flex flex-col items-center justify-center gap-1 text-center group cursor-pointer transition-all"
                                  >
                                    <div className="flex h-7 w-7 items-center justify-center rounded-full bg-brand-100/70 text-brand-600 group-hover:scale-110 dark:bg-brand-950/50 dark:text-brand-400 transition-transform">
                                      <Plus className="h-3.5 w-3.5" />
                                    </div>
                                    <span className="text-xs font-medium text-ink-muted group-hover:text-ink transition-colors">
                                      Chưa chọn món cho bữa này
                                    </span>
                                    <span className="text-[10px] text-brand-600/80 dark:text-brand-400/80">
                                      Bấm để thêm món
                                    </span>
                                  </button>
                                )}
                              </div>
                            </section>
                          )
                        })}
                      </div>
                    </div>
                  )}
                </section>
              </div>
            </div>
          )
        })}
      </div>
      <Modal
        open={Boolean(nutrition)}
        onClose={closeDialog}
        title="Dinh dưỡng theo lịch ăn"
        size="lg"
      >
        {nutrition && (
          <DialogFocus>
            <MealPlanNutrition
              plan={{
                ...plan,
                weekStartDate: mondayOf(new Date(`${(nutrition.day || windowStart)}T12:00:00`)),
              }}
              initialDay={nutrition.day}
              initialScope={nutrition.scope}
              onEntryReplaced={(entry) => setPlan((current) => current && ({
                ...current,
                entries: [...current.entries.filter((item) => item.entryId !== entry.entryId), entry],
              }))}
            />
          </DialogFocus>
        )}
      </Modal>
      <Modal
        open={Boolean(moving)}
        onClose={closeDialog}
        title="Chuyển món trong tuần"
      >
        {moving && (
          <MoveDialog
            key={moving.entryId}
            windowDays={windowDays}
            entry={moving}
            entries={plan.entries}
            busy={busy}
            error={actionError}
            onMove={move}
            onClose={closeDialog}
          />
        )}
      </Modal>
      <Modal
        open={Boolean(picker)}
        onClose={closeDialog}
        title={`${picker?.entry ? "Thay món" : "Thêm món"} · ${
          picker ? mealLabels[picker.mealType] : ""
        }`}
        size="lg"
      >
        {picker && (
          <RecipePicker
            key={`${picker.mealDate}:${picker.mealType}:${picker.entry?.entryId ?? "new"}`}
            target={picker}
            existing={plan.entries}
            busy={busy}
            error={actionError}
            onChoose={choose}
          />
        )}
      </Modal>
      <Modal
        open={Boolean(removal)}
        onClose={closeDialog}
        title="Gỡ món khỏi lịch ăn"
      >
        {removal && (
          <DialogFocus>
            <p className="text-sm text-ink-soft">
              Gỡ “{removal.recipeTitle ?? "Công thức không còn khả dụng"}” khỏi{" "}
              {mealLabels[removal.mealType].toLowerCase()}? Công thức nguồn và
              danh sách đã lưu vẫn được giữ.
            </p>
            {actionError && (
              <p role="alert" className="mt-3 text-sm text-ink-muted">
                {actionError}
              </p>
            )}
            <div className="mt-5 flex flex-wrap gap-2">
              <Button
                disabled={busy}
                onClick={() =>
                  void mutate(async () => {
                    await mealPlanApi.removeEntry(
                      mondayOf(new Date(`${removal.mealDate}T12:00:00`)),
                      removal.entryId,
                    )

                    if (alive.current)
                      setPlan(
                        (current) =>
                          current && {
                            ...current,

                            entries: current.entries.filter(
                              (entry) => entry.entryId !== removal.entryId,
                            ),
                          },
                      )
                  }, "Đã gỡ món khỏi lịch ăn.")
                }
              >
                {busy ? "Đang gỡ…" : "Xác nhận gỡ"}
              </Button>
              <Button variant="ghost" disabled={busy} onClick={closeDialog}>
                Hủy
              </Button>
            </div>
          </DialogFocus>
        )}
      </Modal>
    </div>
  )
}
function MoveDialog({
  windowDays,
  entry,
  entries,
  busy,
  error,
  onMove,
  onClose,
}: {
  windowDays: string[]
  entry: Entry
  entries: Entry[]
  busy: boolean
  error: string
  onMove: (date: string, meal: MealType) => void
  onClose: () => void
}) {
  const [date, setDate] = useState(entry.mealDate)
  const [meal, setMeal] = useState(entry.mealType)
  const duplicate = entries.some(
    (item) =>
      item.entryId !== entry.entryId &&
      item.recipeId === entry.recipeId &&
      item.mealDate === date &&
      item.mealType === meal,
  )
  const unchanged = date === entry.mealDate && meal === entry.mealType
  const dates = windowDays.map((dayStr) => {
    const day = new Date(`${dayStr}T12:00:00`)
    return {
      key: dayStr,
      label: day.toLocaleDateString("vi-VN", {
        weekday: "long",
        day: "2-digit",
        month: "2-digit",
      }),
    }
  })
  return (
    <DialogFocus>
      <p className="text-sm text-ink-soft">
        {entry.recipeTitle ?? "Công thức không còn khả dụng"} · Giữ nguyên{" "}
        {entry.plannedServings.toLocaleString("vi-VN")} phần.
      </p>
      <form
        onSubmit={(event) => {
          event.preventDefault()
          if (!busy && !duplicate && !unchanged) onMove(date, meal)
        }}
      >
        <label className="mt-4 block text-sm text-ink">
          Ngày đích
          <select
            value={date}
            disabled={busy}
            onChange={(event) => setDate(event.target.value)}
            className="mt-1 w-full rounded-lg border border-brand-200 bg-paper p-2 focus-visible:outline-2 focus-visible:outline-brand-600"
          >
            {dates.map((day) => (
              <option key={day.key} value={day.key}>
                {day.label}
              </option>
            ))}
          </select>
        </label>
        <label className="mt-3 block text-sm text-ink">
          Bữa đích
          <select
            value={meal}
            disabled={busy}
            onChange={(event) => setMeal(event.target.value as MealType)}
            className="mt-1 w-full rounded-lg border border-brand-200 bg-paper p-2 focus-visible:outline-2 focus-visible:outline-brand-600"
          >
            {meals.map((value) => (
              <option key={value} value={value}>
                {mealLabels[value]}
              </option>
            ))}
          </select>
        </label>
        {duplicate && (
          <p role="alert" className="mt-3 text-sm text-ink-muted">
            Món này đã có trong bữa đích. Chọn ngày hoặc bữa khác.
          </p>
        )}
        {error && (
          <p role="alert" className="mt-3 text-sm text-ink-muted">
            {error}
          </p>
        )}
        <div className="mt-5 flex gap-2">
          <Button type="submit" disabled={busy || duplicate || unchanged}>
            {busy ? "Đang chuyển…" : "Xác nhận chuyển"}
          </Button>
          <Button
            type="button"
            variant="ghost"
            disabled={busy}
            onClick={onClose}
          >
            Hủy
          </Button>
        </div>
      </form>
    </DialogFocus>
  )
}

function RecipePicker({
  target,

  existing,

  busy,

  error,

  onChoose,
}: {
  target: Target

  existing: Entry[]

  busy: boolean

  error: string

  onChoose: (id: number) => void
}) {
  const [source, setSource] = useState<"saved" | "public">("saved")
  const [text, setText] = useState("")

  const [keyword, setKeyword] = useState("")

  const [page, setPage] = useState(0)

  const [choices, setChoices] = useState<Choice[]>([])

  const [totalPages, setTotalPages] = useState(0)

  const [loading, setLoading] = useState(true)

  const [loadError, setLoadError] = useState("")

  const [retry, setRetry] = useState(0)

  function chooseSource(value: "saved" | "public") {
    if (value === source) return
    setSource(value)
    setText("")
    setKeyword("")
    setPage(0)
    setLoading(true)
  }

  useEffect(() => {
    let active = true

    setLoading(true)

    setLoadError("")

    setChoices([])

    const request = source === "saved"
      ? savedRecipesApi.list(keyword, page, 12).then((result) => ({
          choices: result.content.map((recipe) => ({
            id: recipe.recipeId,
            title: recipe.title ?? "Công thức không còn khả dụng",
            image: recipe.coverUrl,
            available: recipe.available,
          })),
          totalPages: result.totalPages,
        }))
      : recipesApi.searchPublic({ keyword, page, size: 12, sort: "NEWEST", viewPeriod: "ALL_TIME" })
          .then((result) => ({
            choices: result.items.map((recipe) => ({
              id: recipe.id,
              title: recipe.title,
              image: recipe.media.find((media) => media.cover)?.url ?? recipe.media[0]?.url ?? null,
              available: recipe.status === "PUBLISHED",
            })),
            totalPages: result.totalPages,
          }))

    request

      .then((result) => {
        if (active) {
          setChoices(result.choices)

          setTotalPages(result.totalPages)
        }
      })

      .catch((cause) => {
        if (active) setLoadError(asApiError(cause).message)
      })

      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [source, keyword, page, retry])

  return (
    <DialogFocus initialFocus="input">
      <p className="mb-3 text-xs text-ink-muted">
        {mealLabels[target.mealType]} ·{" "}
        {new Date(`${target.mealDate}T12:00:00`).toLocaleDateString("vi-VN")}
      </p>
      <nav aria-label="Nguồn công thức" className="mb-4 flex flex-wrap gap-2">
        {([
          ["saved", "Đã lưu"],
          ["public", "Tìm công thức"],
        ] as const).map(([value, label]) => (
          <button
            key={value}
            type="button"
            aria-pressed={source === value}
            disabled={busy}
            onClick={() => chooseSource(value)}
            className={`rounded-lg px-3 py-2 text-sm focus-visible:outline-2 focus-visible:outline-brand-600 disabled:opacity-60 ${source === value ? "bg-raised font-semibold text-brand-600" : "text-ink-muted"}`}
          >
            {label}
          </button>
        ))}
      </nav>
      <form
        className="mb-4 flex gap-2"
        onSubmit={(event) => {
          event.preventDefault()

          setKeyword(text.trim())

          setPage(0)
        }}
      >
        <input
          aria-label="Tìm món cho lịch ăn"
          maxLength={120}
          value={text}
          disabled={busy}
          onChange={(event) => setText(event.target.value)}
          placeholder={source === "saved" ? "Tìm trong công thức đã lưu…" : "Tìm công thức công khai…"}
          className="min-w-0 flex-1 rounded-lg border border-brand-200 bg-paper px-3 py-2 text-sm text-ink focus-visible:outline-2 focus-visible:outline-brand-600"
        />
        <Button size="sm" type="submit" disabled={busy}>
          Tìm
        </Button>
      </form>
      {error && (
        <p
          role="alert"
          className="mb-3 rounded-lg bg-raised p-3 text-sm text-ink-muted"
        >
          {error}
        </p>
      )}
      {busy && (
        <p role="status" className="mb-3 text-sm text-ink-muted">
          Đang lưu món…
        </p>
      )}
      {loading ? (
        <p role="status" className="py-8 text-center text-sm text-ink-muted">
          Đang tìm công thức…
        </p>
      ) : loadError ? (
        <div role="alert">
          <p className="text-sm text-ink-muted">{loadError}</p>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setRetry((value) => value + 1)}
          >
            Thử lại
          </Button>
        </div>
      ) : (
        <>
          {!choices.length && (
            <div className="py-8 text-center text-sm text-ink-muted">
              <p>
                {source === "public"
                  ? "Không tìm thấy công thức công khai phù hợp."
                  : keyword
                  ? "Không có công thức đã lưu phù hợp với tìm kiếm."
                  : "Bạn chưa lưu công thức nào."}
              </p>
              {source === "saved" && (
                <Button variant="ghost" size="sm" disabled={busy} onClick={() => chooseSource("public")}>
                  Tìm công thức công khai
                </Button>
              )}
            </div>
          )}
          <div className="grid gap-2 sm:grid-cols-2">
            {choices.map((choice) => {
              const duplicate = existing.some(
                (entry) =>
                  entry.mealDate === target.mealDate &&
                  entry.mealType === target.mealType &&
                  entry.recipeId === choice.id &&
                  entry.entryId !== target.entry?.entryId,
              )

              return (
                <button
                  key={choice.id}
                  type="button"
                  disabled={busy || !choice.available || duplicate}
                  onClick={() => onChoose(choice.id)}
                  className="flex min-w-0 items-center gap-3 rounded-xl border border-brand-100 p-3 text-left hover:border-brand-300 hover:bg-raised disabled:cursor-not-allowed disabled:opacity-60"
                >
                  {choice.image && (
                    <img
                      src={choice.image}
                      alt=""
                      loading="lazy"
                      className="h-12 w-12 shrink-0 rounded-lg object-cover"
                    />
                  )}
                  <span className="min-w-0 break-words text-sm font-semibold text-ink">
                    {choice.title}
                    {(!choice.available || duplicate) && (
                      <span className="mt-1 block text-xs font-normal text-ink-muted">
                        {duplicate
                          ? "Đã có trong bữa này"
                          : "Không thể chọn công thức không còn khả dụng"}
                      </span>
                    )}
                  </span>
                </button>
              )
            })}
          </div>
          {totalPages > 1 && (
            <div className="mt-4 flex items-center justify-between gap-2">
              <Button
                variant="ghost"
                size="sm"
                disabled={busy || page === 0}
                onClick={() => setPage((value) => value - 1)}
              >
                Trang trước
              </Button>
              <span className="text-xs text-ink-muted">
                {page + 1} / {totalPages}
              </span>
              <Button
                variant="ghost"
                size="sm"
                disabled={busy || page + 1 >= totalPages}
                onClick={() => setPage((value) => value + 1)}
              >
                Trang sau
              </Button>
            </div>
          )}
        </>
      )}
    </DialogFocus>
  )
}

const goalOptions: Array<{ value: AiMenuGoal; label: string }> = [
  { value: "MAINTAIN_WEIGHT", label: "Duy trì cân nặng" },
  { value: "LOSE_WEIGHT", label: "Giảm cân" },
  { value: "GAIN_WEIGHT", label: "Tăng cân" },
  { value: "BUILD_MUSCLE", label: "Tăng cơ" },
]
const nutritionLabels: Record<string, string> = {
  ENERGY_KCAL: "Năng lượng (kcal)", PROTEIN_G: "Chất đạm (g)",
  CARBOHYDRATE_G: "Carbohydrate (g)", TOTAL_FAT_G: "Chất béo (g)",
  FIBER_G: "Chất xơ (g)", CALCIUM_MG: "Canxi (mg)", IRON_MG: "Sắt (mg)",
  VITAMIN_B12_MCG: "Vitamin B12 (mcg)", ZINC_MG: "Kẽm (mg)",
}

function AiWeeklyMenu({ week }: { week: string }) {
  const [mode, setMode] = useState<AiMenuMode>("PROFILE")
  const [goal, setGoal] = useState<AiMenuGoal | "">("")
  const [preferredQuery, setPreferredQuery] = useState("")
  const [avoidedQuery, setAvoidedQuery] = useState("")
  const [preferredOptions, setPreferredOptions] = useState<Array<{ ingredientId: number; name: string }>>([])
  const [avoidedOptions, setAvoidedOptions] = useState<Array<{ ingredientId: number; name: string }>>([])
  const [ingredientNames, setIngredientNames] = useState<Record<number, string>>({})
  const [preferred, setPreferred] = useState<number[]>([])
  const [avoided, setAvoided] = useState<number[]>([])
  const [preview, setPreview] = useState<AiMenuPreview | null>(null)
  const [previewCriteria, setPreviewCriteria] = useState<{ mode: AiMenuMode; goal: AiMenuGoal | ""; preferred: number[]; avoided: number[] } | null>(null)
  const [selected, setSelected] = useState<number[]>([])
  const [loading, setLoading] = useState(false)
  const [applying, setApplying] = useState(false)
  const [confirmArmed, setConfirmArmed] = useState(false)
  const [error, setError] = useState<{ message: string; code?: string } | null>(null)
  const [successMessage, setSuccessMessage] = useState("")

  useEffect(() => {
    let active = true
    if (!preferredQuery.trim()) { setPreferredOptions([]); return () => { active = false } }
    const timer = window.setTimeout(() => {
      recipesApi.searchIngredients(preferredQuery).then((items) => {
        if (active) {
          setPreferredOptions(items)
          setIngredientNames((current) => ({ ...current, ...Object.fromEntries(items.map((item) => [item.ingredientId, item.name])) }))
        }
      }).catch((cause) => {
        if (active) {
          const apiError = asApiError(cause)
          setError({ message: apiError.message, code: apiError.code })
        }
      })
    }, 250)
    return () => { active = false; window.clearTimeout(timer) }
  }, [preferredQuery])

  useEffect(() => {
    let active = true
    if (!avoidedQuery.trim()) { setAvoidedOptions([]); return () => { active = false } }
    const timer = window.setTimeout(() => {
      recipesApi.searchIngredients(avoidedQuery).then((items) => {
        if (active) {
          setAvoidedOptions(items)
          setIngredientNames((current) => ({ ...current, ...Object.fromEntries(items.map((item) => [item.ingredientId, item.name])) }))
        }
      }).catch((cause) => {
        if (active) {
          const apiError = asApiError(cause)
          setError({ message: apiError.message, code: apiError.code })
        }
      })
    }, 250)
    return () => { active = false; window.clearTimeout(timer) }
  }, [avoidedQuery])

  function clearPreview() { setPreview(null); setPreviewCriteria(null); setSelected([]); setConfirmArmed(false) }
  function addIngredient(item: { ingredientId: number; name: string }, setList: (update: (ids: number[]) => number[]) => void, clearSearch: () => void, clearOptions: () => void) {
    setList((ids) => ids.includes(item.ingredientId) ? ids : [...ids, item.ingredientId])
    setIngredientNames((current) => ({ ...current, [item.ingredientId]: item.name }))
    clearSearch()
    clearOptions()
    clearPreview()
  }
  function removeIngredient(id: number, setList: (update: (ids: number[]) => number[]) => void) {
    setList((ids) => ids.filter((value) => value !== id))
    clearPreview()
  }
  async function generate() {
    setLoading(true); setError(null); setSuccessMessage(""); setConfirmArmed(false)
    const criteria = { mode, goal, preferred: [...preferred], avoided: [...avoided] }
    try {
      const result = await mealPlanApi.createAiMenuPreview({
        weekStartDate: week, mode: criteria.mode, goal: criteria.mode === "THIS_WEEK" ? criteria.goal || null : undefined,
        preferredIngredientIds: criteria.mode === "THIS_WEEK" ? criteria.preferred : [],
        avoidedIngredientIds: criteria.mode === "THIS_WEEK" ? criteria.avoided : [],
      })
      setPreview(result); setPreviewCriteria(criteria);
      setSelected(result.meals.map((_, index) => index))
      if (criteria.mode === "THIS_WEEK") { setGoal(""); setPreferred([]); setAvoided([]) }
    } catch (cause) {
      const apiError = asApiError(cause)
      setError({ message: apiError.message, code: apiError.code })
    }
    finally { setLoading(false) }
  }
  async function apply() {
    if (!preview || !selected.length) return
    if (!confirmArmed) { setConfirmArmed(true); return }
    setApplying(true); setError(null)
    const criteria = previewCriteria ?? { mode, goal, preferred, avoided }
    try {
      const result = await mealPlanApi.applyAiMenu({
        weekStartDate: week, mode: criteria.mode, goal: criteria.mode === "THIS_WEEK" ? criteria.goal || null : undefined,
        avoidedIngredientIds: criteria.mode === "THIS_WEEK" ? criteria.avoided : [],
        meals: selected.map((index) => preview.meals[index]).filter(Boolean),
      })
      setSuccessMessage("Đã thay toàn bộ lịch ăn của tuần này bằng các món đã chọn.")
      setPreview(null); setPreviewCriteria(null); setSelected([]); setConfirmArmed(false)
    } catch (cause) {
      const apiError = asApiError(cause)
      setError({ message: apiError.message, code: apiError.code })
      setConfirmArmed(false)
    }
    finally { setApplying(false) }
  }
  const grouped = preview?.meals.reduce<Record<string, AiMenuPreview["meals"]>>((acc, meal) => {
    (acc[meal.mealDate] ??= []).push(meal)
    return acc
  }, {})
  const totals = preview?.meals.reduce<Record<string, number>>((acc, meal, index) => {
    if (selected.includes(index)) Object.entries(meal.nutritionPerServing).forEach(([key, value]) => { acc[key] = (acc[key] ?? 0) + value })
    return acc
  }, {})

  function ingredientPicker(title: string, queryValue: string, setQueryValue: (value: string) => void,
    options: Array<{ ingredientId: number; name: string }>, selectedIds: number[],
    setIds: (update: (ids: number[]) => number[]) => void, clearOptions: () => void) {
    return <div className="space-y-2">
      <label className="block text-sm font-medium text-ink">{title}
        <input value={queryValue} onChange={(event) => setQueryValue(event.target.value)} placeholder="Tìm trong danh mục nguyên liệu" className="mt-1 w-full rounded-lg border border-brand-200 bg-surface px-3 py-2 text-sm" />
      </label>
      {options.filter((item) => !selectedIds.includes(item.ingredientId)).length > 0 && <ul className="max-h-36 space-y-1 overflow-y-auto rounded-lg border border-brand-100 bg-surface p-2">
        {options.filter((item) => !selectedIds.includes(item.ingredientId)).map((item) => <li key={item.ingredientId}>
          <button type="button" onClick={() => addIngredient(item, setIds, () => setQueryValue(""), clearOptions)} className="w-full rounded-md px-2 py-1.5 text-left text-sm hover:bg-raised focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500">+ {item.name}</button>
        </li>)}
      </ul>}
      {selectedIds.length > 0 && <ul className="flex flex-wrap gap-2" aria-label={`${title} đã chọn`}>
        {selectedIds.map((id) => <li key={id} className="inline-flex items-center gap-1 rounded-full border border-brand-100 bg-surface px-3 py-1 text-xs text-ink">
          {ingredientNames[id] ?? `Nguyên liệu #${id}`}
          <button type="button" aria-label={`Xóa ${ingredientNames[id] ?? "nguyên liệu"}`} onClick={() => removeIngredient(id, setIds)} className="rounded-full p-0.5 text-ink-muted hover:text-red-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"><X className="h-3.5 w-3.5" /></button>
        </li>)}
      </ul>}
    </div>
  }

  return (
    <section aria-labelledby="ai-menu-title" className="rounded-2xl border border-brand-200 bg-surface p-4 shadow-sm sm:p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 id="ai-menu-title" className="flex items-center gap-2 text-base font-semibold text-ink"><Sparkles className="h-4 w-4 text-brand-600" /> Dành cho bạn</h2>
          <p className="mt-1 text-sm text-ink-muted">Tạo thực đơn cho tuần đang xem rồi chọn món trước khi thay lịch.</p>
        </div>
        <div className="flex gap-2" role="group" aria-label="Cách tạo thực đơn">
          <Button variant={mode === "PROFILE" ? "primary" : "outline"} size="sm" onClick={() => { setMode("PROFILE"); clearPreview() }}>Dựa trên hồ sơ</Button>
          <Button variant={mode === "THIS_WEEK" ? "primary" : "outline"} size="sm" onClick={() => { setMode("THIS_WEEK"); clearPreview() }}>Theo mong muốn tuần này</Button>
        </div>
      </div>
      {mode === "THIS_WEEK" && <div className="mt-4 grid gap-4 md:grid-cols-2">
        <label className="block text-sm font-medium text-ink">Mục tiêu kế hoạch ăn của tuần này <span className="font-normal text-ink-muted">(không bắt buộc)</span>
          <select value={goal} onChange={(event) => { setGoal(event.target.value as AiMenuGoal | ""); clearPreview() }} className="mt-1 w-full rounded-lg border border-brand-200 bg-surface px-3 py-2 text-sm">
            <option value="">Không chọn mục tiêu</option>{goalOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        </label>
        {ingredientPicker("Bạn muốn lịch ăn có những nguyên liệu nào", preferredQuery, setPreferredQuery, preferredOptions, preferred, setPreferred, () => setPreferredOptions([]))}
        {ingredientPicker("Bạn muốn lịch ăn không có nguyên liệu nào", avoidedQuery, setAvoidedQuery, avoidedOptions, avoided, setAvoided, () => setAvoidedOptions([]))}
        <p className="text-xs text-ink-muted md:col-span-2">Nguyên liệu cần tránh là điều kiện bắt buộc: món có chứa bất kỳ nguyên liệu nào trong danh sách này sẽ bị loại, kể cả khi có nguyên liệu bạn muốn ưu tiên. Thông tin chỉ dùng cho lần tạo lịch này.</p>
      </div>}
      {mode === "PROFILE" && <p className="mt-3 text-sm text-ink-muted">Dùng mục tiêu và ràng buộc đã lưu. Nếu chưa đặt mục tiêu, thực đơn được tạo theo hướng cân bằng. Hồ sơ sở thích chưa khai báo không bắt buộc phải bổ sung.</p>}
      <div className="mt-4 flex flex-wrap gap-2">
        <Button disabled={loading || applying} onClick={() => void generate()}>{loading ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}{preview ? "Tạo thực đơn khác" : "Tạo thực đơn tuần"}</Button>
      </div>
      {error && <div role="alert" className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-800"><p>{error.message}</p>{error.code === "NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED" && <p className="mt-1">Để tiếp tục, hãy <Link to="/ho-so/dinh-duong" className="font-semibold underline underline-offset-2">xác nhận phạm vi hỗ trợ trong Hồ sơ dinh dưỡng</Link>.</p>}</div>}
      {successMessage && <p role="status" className="mt-3 rounded-lg bg-green-50 p-3 text-sm text-green-800">{successMessage}</p>}
      {preview && <div className="mt-5 space-y-4 border-t border-brand-100 pt-4">
        <div className="flex flex-wrap items-center justify-between gap-2"><p className="text-sm font-semibold text-ink">Bản xem trước · {selected.length} món được chọn</p><Button variant="ghost" size="sm" onClick={() => { setSelected(selected.length === preview.meals.length ? [] : preview.meals.map((_, i) => i)); setConfirmArmed(false) }}>{selected.length === preview.meals.length ? "Bỏ chọn tất cả" : "Chọn tất cả"}</Button></div>
        <div className="rounded-xl bg-raised p-3"><p className="mb-2 text-xs font-semibold text-ink">Tổng dinh dưỡng dự kiến theo các món đã chọn trong tuần</p><div className="grid grid-cols-2 gap-2 text-xs text-ink-muted sm:grid-cols-3 lg:grid-cols-5">{Object.entries(nutritionLabels).map(([key, label]) => <span key={key}>{label}: ≈ {Number(totals?.[key] ?? 0).toFixed(1)}</span>)}</div></div>
        <div className="space-y-3">{Object.entries(grouped ?? {}).map(([day, dayMeals]) => <section key={day} className="rounded-xl bg-raised p-3"><h3 className="text-sm font-semibold capitalize text-ink">{new Date(`${day}T12:00:00`).toLocaleDateString("vi-VN", { weekday: "long", day: "numeric", month: "long" })}</h3><div className="mt-2 grid gap-2 sm:grid-cols-3">{dayMeals.map((meal) => {
          const index = preview.meals.findIndex((item) => item.mealDate === meal.mealDate && item.mealType === meal.mealType)
          const label = meal.mealType === "BREAKFAST" ? "Sáng" : meal.mealType === "LUNCH" ? "Trưa" : "Tối"
          const isSelected = selected.includes(index)
          return <article key={`${meal.mealDate}-${meal.mealType}`} className={`min-w-0 overflow-hidden rounded-lg border bg-surface text-sm ${isSelected ? "border-brand-200" : "border-brand-100 opacity-70"}`}>
            <div className="flex items-center justify-between gap-2 border-b border-brand-100 px-3 py-2">
              <span className="text-xs font-medium text-ink-muted">Bữa {label.toLowerCase()}</span>
              <label className="flex cursor-pointer items-center gap-2 text-xs text-ink-muted">
                <input type="checkbox" checked={isSelected} aria-label={`Đưa ${meal.recipeTitle} vào lịch`} onChange={() => { setSelected(isSelected ? selected.filter((value) => value !== index) : [...selected, index]); setConfirmArmed(false) }} />
                {isSelected ? "Đưa vào lịch" : "Bỏ qua món này"}
              </label>
            </div>
            <Link to={`/cong-thuc/id/${meal.recipeId}`} aria-label={`Xem công thức ${meal.recipeTitle}`} className="group flex min-w-0 items-center gap-3 p-3 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-600">
              {meal.coverUrl ? <img src={meal.coverUrl} alt="" loading="lazy" className="h-16 w-16 shrink-0 rounded-lg object-cover" /> : <span className="flex h-16 w-16 shrink-0 items-center justify-center rounded-lg bg-raised text-ink-muted"><Utensils className="h-5 w-5" /></span>}
              <span className="min-w-0">
                <span className="block break-words font-semibold text-ink group-hover:text-brand-600">{meal.recipeTitle}</span>
                <span className="mt-1 block text-xs text-ink-muted">{meal.nutritionPerServing.ENERGY_KCAL != null ? `≈ ${meal.nutritionPerServing.ENERGY_KCAL}` : "—"} kcal · {meal.nutritionPerServing.PROTEIN_G != null ? `≈ ${meal.nutritionPerServing.PROTEIN_G}` : "—"} g đạm</span>
                <span className="mt-1 block text-xs font-medium text-brand-700">Xem công thức</span>
              </span>
            </Link>
          </article>
        })}</div></section>)}</div>
        <p className="text-xs text-ink-muted">{preview.nutritionDisclaimer} Món có thể được lặp nếu nguồn phù hợp chưa đủ đa dạng, cùng món không xuất hiện ở hai bữa liền nhau.</p>
        <div className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">Khi xác nhận, toàn bộ món đã có trong lịch của tuần này sẽ bị thay bằng các món đang được chọn.</div>
        <div className="flex flex-wrap gap-2"><Button disabled={applying || selected.length === 0} onClick={() => void apply()}>{applying ? <LoaderCircle className="h-4 w-4 animate-spin" /> : null}{confirmArmed ? "Xác nhận thay toàn bộ lịch" : "Áp dụng vào Lịch ăn"}</Button>{confirmArmed && <Button variant="outline" onClick={() => setConfirmArmed(false)}>Quay lại</Button>}</div>
      </div>}
    </section>
  )
}

function DialogFocus({ children, initialFocus }: { children: ReactNode; initialFocus?: string }) {
  const root = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const dialog = root.current?.closest<HTMLElement>('[role="dialog"]')

    if (!dialog) return

    const focusable = () =>
      Array.from(
        dialog.querySelectorAll<HTMLElement>(
          "button:not(:disabled), input:not(:disabled), select:not(:disabled), a[href]",
        ),
      ).filter((element) => element.offsetParent !== null)
    ;(
      root.current?.querySelector<HTMLElement>(
        initialFocus ?? "input, select, button:not(:disabled)",
      ) ?? focusable()[0]
    )?.focus()

    const trap = (event: KeyboardEvent) => {
      if (event.key !== "Tab") return

      const items = focusable()

      const first = items[0]

      const last = items[items.length - 1]

      if (!dialog.contains(document.activeElement)) {
        event.preventDefault()
        ;(event.shiftKey ? last : first)?.focus()
      } else if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()

        last?.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()

        first?.focus()
      }
    }

    document.addEventListener("keydown", trap, true)

    return () => document.removeEventListener("keydown", trap, true)
  }, [initialFocus])

  return <div ref={root}>{children}</div>
}
