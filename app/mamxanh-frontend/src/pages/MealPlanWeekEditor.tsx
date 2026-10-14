import { useEffect, useRef, useState, type ReactNode } from "react"

import { Link, useSearchParams } from "react-router-dom"

import {
  CalendarDays,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Clock3,
  LoaderCircle,
  Plus,
  RefreshCw,
  Sparkles,
  Trash2,
  Utensils,
} from "lucide-react"

import { PageContainer } from "../components/Layout"

import { Button, Card } from "../components/ui"

import { Modal } from "../components/Modal"

import {
  WeekCalendar,
  localDateKey,
  mondayOf,
  shiftWeek,
} from "../components/WeekCalendar"

import { mealPlanApi, recipesApi, type AiMenuGoal, type AiMenuMode, type AiMenuPreview, type MealPlanWeek } from "../api/recipes"

import { savedRecipesApi } from "../services/savedRecipesApi"

import { asApiError } from "../lib/apiClient"

import { MealPlanNutrition } from "./MealPlanNutrition"

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

  const [week, setWeek] = useState(() => {
    const requested = searchParams.get("weekStartDate") ?? ""

    return /^\d{4}-\d{2}-\d{2}$/.test(requested) &&
      mondayOf(new Date(`${requested}T12:00:00`)) === requested
      ? requested
      : mondayOf(new Date())
  })

  const [calendarOpen, setCalendarOpen] = useState(false)

  const [selectedDay, setSelectedDay] = useState<string | null>(() => {
    const requested = searchParams.get("mealDate") ?? ""

    if (
      /^\d{4}-\d{2}-\d{2}$/.test(requested) &&
      mondayOf(new Date(`${requested}T12:00:00`)) === week
    )
      return requested

    return week === mondayOf(new Date()) ? localDateKey(new Date()) : week
  })

  useEffect(() => {
    if (
      searchParams.get("weekStartDate") !== week ||
      searchParams.get("mealDate") !== selectedDay
    ) {
      setSearchParams(
        {
          weekStartDate: week,
          ...(selectedDay ? { mealDate: selectedDay } : {}),
        },
        { replace: true },
      )
    }
  }, [week, selectedDay, searchParams, setSearchParams])

  function changeWeek(value: string) {
    setWeek(value)

    setSelectedDay(
      value === mondayOf(new Date()) ? localDateKey(new Date()) : value,
    )
  }

  const end = new Date(`${week}T12:00:00`)

  end.setDate(end.getDate() + 6)

  return (
    <PageContainer className="py-6">
      <div className="grid items-start gap-5 md:grid-cols-[225px_minmax(0,1fr)]">
        <aside className="space-y-4">
          <Button
            variant="outline"
            className="w-full md:hidden"
            aria-expanded={calendarOpen}
            aria-controls="planner-calendar"
            onClick={() => setCalendarOpen((value) => !value)}
          >
            <CalendarDays className="h-4 w-4" />
            Chọn ngày trong lịch
          </Button>
          <div
            id="planner-calendar"
            className={calendarOpen ? "block" : "hidden md:block"}
          >
            <WeekCalendar
              weekStart={week}
              onChange={changeWeek}
              selectedDay={selectedDay}
              onSelectDay={setSelectedDay}
            />
          </div>
          <Card className="hidden p-4 md:block">
            <p className="text-sm font-semibold text-ink">Lên lịch từng ngày</p>
            <p className="mt-2 text-xs leading-5 text-ink-muted">
              Chọn một ngày, rồi thêm món từ công thức bạn đã lưu. Bạn có thể
              lên lịch từng bữa và bổ sung các ngày còn lại sau.
            </p>
            <Link
              to="/ho-so"
              className="mt-3 inline-block text-xs font-semibold text-brand-600"
            >
              Xem công thức đã lưu →
            </Link>
          </Card>
        </aside>
        <Card className="min-w-0 p-4 sm:p-6">
          <header className="mb-6 border-b border-brand-100 pb-4">
            <h1
              tabIndex={-1}
              className="flex items-center gap-2 text-xl font-semibold text-ink"
            >
              <CalendarDays className="h-5 w-5 text-brand-600" />
              Lịch ăn của bạn
            </h1>
            <div className="mt-4 flex items-center justify-between gap-2">
              <Button
                variant="ghost"
                size="sm"
                aria-label="Tuần trước"
                onClick={() => changeWeek(shiftWeek(week, -1))}
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <p className="text-xs font-medium text-ink-soft">
                {new Date(`${week}T12:00:00`).toLocaleDateString("vi-VN")} –{" "}
                {end.toLocaleDateString("vi-VN")}
              </p>
              <Button
                variant="ghost"
                size="sm"
                aria-label="Tuần sau"
                onClick={() => changeWeek(shiftWeek(week, 1))}
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => changeWeek(mondayOf(new Date()))}
            >
              Về tuần hiện tại
            </Button>
          </header>
          <WeekContents
            key={week}
            week={week}
            selectedDay={selectedDay}
            onSelectDay={setSelectedDay}
          />
        </Card>
      </div>
    </PageContainer>
  )
}

function WeekContents({
  week,

  selectedDay,

  onSelectDay,
}: {
  week: string

  selectedDay: string | null

  onSelectDay: (day: string | null) => void
}) {
  const [plan, setPlan] = useState<MealPlanWeek | null>(null)

  const [loading, setLoading] = useState(true)

  const [loadError, setLoadError] = useState("")

  const [actionError, setActionError] = useState("")

  const [notice, setNotice] = useState("")

  const [retry, setRetry] = useState(0)

  const [busy, setBusy] = useState(false)

  const [picker, setPicker] = useState<Target | null>(null)

  const [removal, setRemoval] = useState<Entry | null>(null)

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

    mealPlanApi

      .getWeek(week)

      .then((value) => {
        if (active) setPlan(value)
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
  }, [week, retry])

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

    void mutate(
      async () => {
        const entry = target.entry
          ? await mealPlanApi.replaceEntry(week, target.entry.entryId, recipeId)
          : await mealPlanApi.addEntry(week, {
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
      {busy && !picker && !removal && (
        <p role="status" className="text-sm text-ink-muted">
          Đang lưu lịch ăn…
        </p>
      )}
      {actionError && !picker && !removal && (
        <p
          role="alert"
          className="rounded-xl bg-raised p-3 text-sm text-ink-muted"
        >
          {actionError}
        </p>
      )}
      {plan.entries.length === 0 && (
        <div className="rounded-xl bg-raised p-4 text-center">
          <p className="text-sm text-ink-muted">
            Tuần này chưa có công thức trong Lịch ăn.
          </p>
          <Button
            variant="ghost"
            size="sm"
            disabled={busy}
            onClick={() =>
              void mutate(async () => {
                const result = await mealPlanApi.openWeek(week)

                if (alive.current) setPlan(result)
              }, "Đã lưu lịch tuần. Bạn có thể thêm món cho từng bữa.")
            }
          >
            Lưu lịch tuần trống
          </Button>
        </div>
      )}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-ink-muted">Chọn ngày để lên lịch từng bữa</p>
        <Button
          variant="outline"
          size="sm"
          onClick={() => {
            returnFocus.current = (document.activeElement as HTMLElement)

            setNutrition({ day: selectedDay ?? week, scope: "week" })
          }}
        >
          <Utensils className="h-4 w-4" />
          Dinh dưỡng cả tuần
        </Button>
      </div>
      <AiWeeklyMenu week={week} onApplied={(value) => { setPlan(value); setNotice("Đã thay toàn bộ lịch ăn của tuần này."); }} />
      <div className="divide-y divide-brand-100">
        {Array.from({ length: 7 }, (_, offset) => {
          const date = new Date(`${week}T12:00:00`)

          date.setDate(date.getDate() + offset)

          const day = localDateKey(date)

          const label = date.toLocaleDateString("vi-VN", {
            weekday: "long",

            day: "numeric",

            month: "long",
          })

          const entries = plan.entries.filter((entry) => entry.mealDate === day)

          const expanded = selectedDay === day

          const today = day === localDateKey(new Date())

          return (
            <section
              key={day}
              aria-label={date.toLocaleDateString("vi-VN")}
              className="py-3"
            >
              <div className="flex items-center gap-2">
                <h2 className="min-w-0 flex-1">
                  <button
                    type="button"
                    aria-label={`Xem ngày ${date.toLocaleDateString("vi-VN")}`}
                    aria-expanded={expanded}
                    aria-controls={`meals-${day}`}
                    onClick={() => onSelectDay(expanded ? null : day)}
                    className="flex w-full items-center gap-3 rounded-lg p-2 text-left hover:bg-raised focus-visible:outline-2 focus-visible:outline-brand-600"
                  >
                    {expanded ? (
                      <ChevronDown className="h-4 w-4 shrink-0" />
                    ) : (
                      <ChevronRight className="h-4 w-4 shrink-0" />
                    )}
                    <span className="min-w-0">
                      <span
                        className={`block text-sm font-semibold capitalize ${
                          today ? "text-brand-600" : "text-ink"
                        }`}
                      >
                        {today ? `Hôm nay · ${label}` : label}
                      </span>
                      <span className="mt-1 block text-xs font-normal text-ink-muted">
                        {entries.length
                          ? `${entries.length} món đã lên lịch`
                          : "Chưa lên lịch · thêm món khi bạn sẵn sàng"}
                      </span>
                    </span>
                  </button>
                </h2>
                <Button
                  variant="ghost"
                  size="sm"
                  disabled={busy}
                  aria-label={`Lên lịch ngày ${date.toLocaleDateString("vi-VN")}`}
                  onClick={() => onSelectDay(day)}
                >
                  <Plus className="h-4 w-4" />
                </Button>
              </div>
              {expanded && (
                <div
                  id={`meals-${day}`}
                  className="mt-3 space-y-5 pl-2 sm:pl-9"
                >
                  <div className="flex justify-end">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => {
                        returnFocus.current =
                          (document.activeElement as HTMLElement)

                        setNutrition({ day, scope: "day" })
                      }}
                    >
                      <Utensils className="h-4 w-4" />
                      Xem dinh dưỡng ngày
                    </Button>
                  </div>
                  {meals.map((meal) => (
                    <section
                      key={meal}
                      aria-label={`${mealLabels[meal]} ${label}`}
                      className="min-w-0"
                    >
                      <div className="mb-2 flex items-center justify-between gap-2 border-b border-brand-100 pb-2">
                        <h3 className="text-sm font-semibold text-ink">
                          {mealLabels[meal]}
                        </h3>
                        <Button
                          variant="ghost"
                          size="sm"
                          disabled={busy}
                          aria-label={`Thêm món ${mealLabels[meal]} ${label}`}
                          onClick={() =>
                            openPicker({ mealDate: day, mealType: meal })
                          }
                        >
                          <Plus className="h-4 w-4" />
                          Thêm món
                        </Button>
                      </div>
                      <div className="space-y-2">
                        {entries

                          .filter((entry) => entry.mealType === meal)

                          .map((entry) => {
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
                                className="flex min-w-0 flex-wrap items-center gap-3 rounded-xl p-2 hover:bg-raised sm:flex-nowrap"
                              >
                                {!unavailable && entry.recipeCoverUrl ? (
                                  <img
                                    src={entry.recipeCoverUrl}
                                    alt=""
                                    loading="lazy"
                                    className="h-16 w-16 shrink-0 rounded-lg object-cover"
                                  />
                                ) : (
                                  <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-lg bg-raised text-ink-muted">
                                    <Utensils className="h-5 w-5" />
                                  </div>
                                )}
                                <div className="min-w-0 flex-1">
                                  {unavailable ? (
                                    <p className="text-sm text-ink-muted">
                                      {name}
                                    </p>
                                  ) : (
                                    <Link
                                      to={`/cong-thuc/id/${entry.recipeId}?mealPlanWeek=${week}&mealPlanEntryId=${entry.entryId}&mealDate=${day}`}
                                      className="block break-words text-sm font-semibold text-ink hover:text-brand-600"
                                    >
                                      {name}
                                    </Link>
                                  )}
                                  <p className="mt-1 flex items-center gap-1 text-xs text-ink-muted">
                                    <Clock3 className="h-3 w-3" />
                                    {entry.totalTimeMinutes === null
                                      ? ""
                                      : `${entry.totalTimeMinutes} phút · `}
                                    {entry.plannedServings} phần
                                  </p>
                                </div>
                                <div className="flex w-full gap-1 pl-[76px] sm:ml-auto sm:w-auto sm:pl-0">
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
                                  >
                                    <RefreshCw className="h-3 w-3" />
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
                                  >
                                    <Trash2 className="h-3 w-3" />
                                    Gỡ
                                  </Button>
                                </div>
                              </article>
                            )
                          })}
                        {!entries.some((entry) => entry.mealType === meal) && (
                          <p className="py-3 text-xs text-ink-muted">
                            Chưa chọn món cho bữa này.
                          </p>
                        )}
                      </div>
                    </section>
                  ))}
                </div>
              )}
            </section>
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
              plan={plan}
              initialDay={nutrition.day}
              initialScope={nutrition.scope}
            />
          </DialogFocus>
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
                    await mealPlanApi.removeEntry(week, removal.entryId)

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
  const [text, setText] = useState("")

  const [keyword, setKeyword] = useState("")

  const [page, setPage] = useState(0)

  const [choices, setChoices] = useState<Choice[]>([])

  const [totalPages, setTotalPages] = useState(0)

  const [loading, setLoading] = useState(true)

  const [loadError, setLoadError] = useState("")

  const [retry, setRetry] = useState(0)

  useEffect(() => {
    let active = true

    setLoading(true)

    setLoadError("")

    setChoices([])

    const request = savedRecipesApi.list(keyword, page, 12).then((result) => ({
      choices: result.content.map((recipe) => ({
        id: recipe.recipeId,

        title: recipe.title ?? "Công thức không còn khả dụng",

        image: recipe.coverUrl,

        available: recipe.available,
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
  }, [keyword, page, retry])

  return (
    <DialogFocus>
      <p className="mb-3 text-xs text-ink-muted">
        {mealLabels[target.mealType]} ·{" "}
        {new Date(`${target.mealDate}T12:00:00`).toLocaleDateString("vi-VN")}
      </p>
      <p className="mb-4 text-sm font-semibold text-ink">Công thức đã lưu</p>
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
          placeholder="Tìm trong công thức đã lưu…"
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
                {keyword
                  ? "Không có công thức đã lưu phù hợp với tìm kiếm."
                  : "Bạn chưa lưu công thức nào."}
              </p>
              <Link
                to="/kham-pha"
                className="mt-3 inline-block font-semibold text-brand-600"
              >
                Khám phá và lưu công thức →
              </Link>
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
  { value: "IMPROVE_HEALTH", label: "Ăn uống cân bằng hơn" },
  { value: "SUPPORT_TRAINING", label: "Hỗ trợ tập luyện" },
]
const nutritionLabels: Record<string, string> = {
  ENERGY_KCAL: "Năng lượng (kcal)", PROTEIN_G: "Chất đạm (g)",
  CARBOHYDRATE_G: "Carbohydrate (g)", TOTAL_FAT_G: "Chất béo (g)",
  FIBER_G: "Chất xơ (g)", CALCIUM_MG: "Canxi (mg)", IRON_MG: "Sắt (mg)",
  VITAMIN_B12_MCG: "Vitamin B12 (mcg)", ZINC_MG: "Kẽm (mg)",
}

function AiWeeklyMenu({ week, onApplied }: { week: string; onApplied: (plan: MealPlanWeek) => void }) {
  const [mode, setMode] = useState<AiMenuMode>("PROFILE")
  const [goal, setGoal] = useState<AiMenuGoal | "">("")
  const [query, setQuery] = useState("")
  const [ingredientOptions, setIngredientOptions] = useState<Array<{ ingredientId: number; name: string }>>([])
  const [preferred, setPreferred] = useState<number[]>([])
  const [avoided, setAvoided] = useState<number[]>([])
  const [preview, setPreview] = useState<AiMenuPreview | null>(null)
  const [previewCriteria, setPreviewCriteria] = useState<{ mode: AiMenuMode; goal: AiMenuGoal | ""; preferred: number[]; avoided: number[] } | null>(null)
  const [selected, setSelected] = useState<number[]>([])
  const [loading, setLoading] = useState(false)
  const [applying, setApplying] = useState(false)
  const [confirmArmed, setConfirmArmed] = useState(false)
  const [error, setError] = useState("")

  useEffect(() => {
    let active = true
    const timer = window.setTimeout(() => {
      recipesApi.searchIngredients(query).then((items) => {
        if (active) setIngredientOptions(items)
      }).catch((cause) => {
        if (active) setError(asApiError(cause).message)
      })
    }, 250)
    return () => { active = false; window.clearTimeout(timer) }
  }, [query])

  function clearPreview() { setPreview(null); setPreviewCriteria(null); setSelected([]); setConfirmArmed(false) }
  function toggle(id: number, list: number[], setList: (ids: number[]) => void, other: number[], setOther: (ids: number[]) => void) {
    setList(list.includes(id) ? list.filter((value) => value !== id) : [...list, id])
    setOther(other.filter((value) => value !== id))
    clearPreview()
  }
  async function generate() {
    setLoading(true); setError(""); setConfirmArmed(false)
    const criteria = { mode, goal, preferred: [...preferred], avoided: [...avoided] }
    try {
      const result = await mealPlanApi.createAiMenuPreview({
        weekStartDate: week, mode: criteria.mode, goal: criteria.mode === "THIS_WEEK" ? criteria.goal || null : undefined,
        preferredIngredientIds: criteria.mode === "THIS_WEEK" ? criteria.preferred : [],
        avoidedIngredientIds: criteria.mode === "THIS_WEEK" ? criteria.avoided : [],
      })
      setPreview(result); setPreviewCriteria(criteria); setSelected(result.meals.map((_, index) => index))
      if (criteria.mode === "THIS_WEEK") { setGoal(""); setPreferred([]); setAvoided([]) }
    } catch (cause) { setError(asApiError(cause).message) }
    finally { setLoading(false) }
  }
  async function apply() {
    if (!preview || !selected.length) return
    if (!confirmArmed) { setConfirmArmed(true); return }
    setApplying(true); setError("")
    const criteria = previewCriteria ?? { mode, goal, preferred, avoided }
    try {
      const result = await mealPlanApi.applyAiMenu({
        weekStartDate: week, mode: criteria.mode, goal: criteria.mode === "THIS_WEEK" ? criteria.goal || null : undefined,
        avoidedIngredientIds: criteria.mode === "THIS_WEEK" ? criteria.avoided : [],
        meals: selected.map((index) => preview.meals[index]).filter(Boolean),
      })
      onApplied(result); setPreview(null); setPreviewCriteria(null); setSelected([]); setConfirmArmed(false)
    } catch (cause) { setError(asApiError(cause).message); setConfirmArmed(false) }
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
        <label className="block text-sm font-medium text-ink">Mục tiêu tuần này <span className="font-normal text-ink-muted">(không bắt buộc)</span>
          <select value={goal} onChange={(event) => { setGoal(event.target.value as AiMenuGoal | ""); clearPreview() }} className="mt-1 w-full rounded-lg border border-brand-200 bg-surface px-3 py-2 text-sm">
            <option value="">Để AI chọn theo hoàn cảnh</option>{goalOptions.map((option) => <option key={option.value} value={option.value}>{option.label}</option>)}
          </select>
        </label>
        <label className="block text-sm font-medium text-ink">Tìm nguyên liệu muốn có hoặc cần tránh
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm trong danh mục nguyên liệu" className="mt-1 w-full rounded-lg border border-brand-200 bg-surface px-3 py-2 text-sm" />
        </label>
        <div className="md:col-span-2 flex flex-wrap gap-2" aria-label="Nguyên liệu tuần này">
          {ingredientOptions.map((item) => <span key={item.ingredientId} className="inline-flex items-center gap-1 rounded-full border border-brand-100 px-2 py-1 text-xs text-ink">
            <span>{item.name}</span>
            <button type="button" aria-label={`Ưu tiên ${item.name}`} aria-pressed={preferred.includes(item.ingredientId)} onClick={() => toggle(item.ingredientId, preferred, setPreferred, avoided, setAvoided)} className={preferred.includes(item.ingredientId) ? "font-bold text-brand-700" : "text-ink-muted"}>Muốn có</button>
            <button type="button" aria-label={`Tránh ${item.name}`} aria-pressed={avoided.includes(item.ingredientId)} onClick={() => toggle(item.ingredientId, avoided, setAvoided, preferred, setPreferred)} className={avoided.includes(item.ingredientId) ? "font-bold text-red-700" : "text-ink-muted"}>Tránh</button>
          </span>)}
        </div>
        <p className="text-xs text-ink-muted md:col-span-2">Thông tin ở đây chỉ dùng cho lần tạo này. Mục tiêu chỉ giúp chọn món, không làm thay đổi chỉ số dinh dưỡng tham khảo trong hồ sơ.</p>
      </div>}
      {mode === "PROFILE" && <p className="mt-3 text-sm text-ink-muted">Dùng mục tiêu và ràng buộc đã lưu. Nếu chưa đặt mục tiêu, thực đơn được tạo theo hướng cân bằng; hồ sơ sở thích chưa khai báo không bắt buộc phải bổ sung.</p>}
      <div className="mt-4 flex flex-wrap gap-2">
        <Button disabled={loading || applying} onClick={() => void generate()}>{loading ? <LoaderCircle className="h-4 w-4 animate-spin" /> : <Sparkles className="h-4 w-4" />}{preview ? "Tạo thực đơn khác" : "Tạo thực đơn tuần"}</Button>
      </div>
      {error && <p role="alert" className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-800">{error}</p>}
      {preview && <div className="mt-5 space-y-4 border-t border-brand-100 pt-4">
        <div className="flex flex-wrap items-center justify-between gap-2"><p className="text-sm font-semibold text-ink">Bản xem trước · {selected.length} món được chọn</p><Button variant="ghost" size="sm" onClick={() => { setSelected(selected.length === preview.meals.length ? [] : preview.meals.map((_, i) => i)); setConfirmArmed(false) }}>{selected.length === preview.meals.length ? "Bỏ chọn tất cả" : "Chọn tất cả"}</Button></div>
        <div className="rounded-xl bg-raised p-3"><p className="mb-2 text-xs font-semibold text-ink">Tổng dinh dưỡng dự kiến theo các món đã chọn trong tuần</p><div className="grid grid-cols-2 gap-2 text-xs text-ink-muted sm:grid-cols-3 lg:grid-cols-5">{Object.entries(nutritionLabels).map(([key, label]) => <span key={key}>{label}: {Number(totals?.[key] ?? 0).toFixed(1)}</span>)}</div></div>
        <div className="space-y-3">{Object.entries(grouped ?? {}).map(([day, dayMeals]) => <section key={day} className="rounded-xl bg-raised p-3"><h3 className="text-sm font-semibold capitalize text-ink">{new Date(`${day}T12:00:00`).toLocaleDateString("vi-VN", { weekday: "long", day: "numeric", month: "long" })}</h3><div className="mt-2 grid gap-2 sm:grid-cols-3">{dayMeals.map((meal) => {
          const index = preview.meals.findIndex((item) => item.mealDate === meal.mealDate && item.mealType === meal.mealType)
          const label = meal.mealType === "BREAKFAST" ? "Sáng" : meal.mealType === "LUNCH" ? "Trưa" : "Tối"
          return <label key={`${meal.mealDate}-${meal.mealType}`} className="flex min-w-0 gap-2 rounded-lg border border-brand-100 bg-surface p-2 text-sm"><input type="checkbox" checked={selected.includes(index)} onChange={() => { setSelected(selected.includes(index) ? selected.filter((value) => value !== index) : [...selected, index]); setConfirmArmed(false) }} /><span className="min-w-0"><span className="block text-xs text-ink-muted">{label}</span><Link to={`/cong-thuc/id/${meal.recipeId}`} className="font-semibold text-ink hover:text-brand-600">{meal.recipeTitle}</Link><span className="mt-1 block text-xs text-ink-muted">{meal.nutritionPerServing.ENERGY_KCAL ?? "—"} kcal · {meal.nutritionPerServing.PROTEIN_G ?? "—"} g đạm</span></span></label>
        })}</div></section>)}</div>
        <p className="text-xs text-ink-muted">{preview.nutritionDisclaimer} Món có thể được lặp nếu nguồn phù hợp chưa đủ đa dạng; cùng món không xuất hiện ở hai bữa liền nhau.</p>
        <div className="rounded-lg border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">Khi xác nhận, toàn bộ món đã có trong lịch của tuần này sẽ bị thay bằng các món đang được chọn.</div>
        <div className="flex flex-wrap gap-2"><Button disabled={applying || selected.length === 0} onClick={() => void apply()}>{applying ? <LoaderCircle className="h-4 w-4 animate-spin" /> : null}{confirmArmed ? "Xác nhận thay toàn bộ lịch" : "Áp dụng vào Lịch ăn"}</Button>{confirmArmed && <Button variant="outline" onClick={() => setConfirmArmed(false)}>Quay lại</Button>}</div>
      </div>}
    </section>
  )
}

function DialogFocus({ children }: { children: ReactNode }) {
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
    ;(dialog.querySelector<HTMLElement>("input") ?? focusable()[0])?.focus()

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
  }, [])

  return <div ref={root}>{children}</div>
}
