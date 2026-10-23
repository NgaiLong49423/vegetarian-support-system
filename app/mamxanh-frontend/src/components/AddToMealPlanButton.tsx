import { useEffect, useRef, useState } from "react"
import { Link } from "react-router-dom"
import { ChevronLeft, ChevronRight, Ellipsis, LoaderCircle } from "lucide-react"
import { useAuth } from "./AuthContext"
import { Modal } from "./Modal"
import { localDateKey, mondayOf } from "./WeekCalendar"
import { mealPlanApi, type MealPlanWeek } from "../api/recipes"
import { asApiError } from "../lib/apiClient"
import { AddRecipeShoppingDialog } from "./AddRecipeShoppingDialog"
import type { RecipePost } from "../api/recipes"

type MealType = MealPlanWeek["entries"][number]["mealType"]

const dayOptions = [
  { offset: 0, label: "Thứ Hai" },
  { offset: 1, label: "Thứ Ba" },
  { offset: 2, label: "Thứ Tư" },
  { offset: 3, label: "Thứ Năm" },
  { offset: 4, label: "Thứ Sáu" },
  { offset: 5, label: "Thứ Bảy" },
  { offset: 6, label: "Chủ Nhật" },
] as const

const mealOptions: Array<{ code: MealType; label: string; shortLabel: string }> = [
  { code: "BREAKFAST", label: "Bữa sáng", shortLabel: "Sáng" },
  { code: "LUNCH", label: "Bữa trưa", shortLabel: "Trưa" },
  { code: "DINNER", label: "Bữa tối", shortLabel: "Tối" },
]

type MealPlanSlot = { mealDate: string; mealType: MealType }

function slotKey(slot: MealPlanSlot) {
  return `${slot.mealDate}:${slot.mealType}`
}

function dateAtOffset(weekStartDate: string, offset: number) {
  const date = new Date(`${weekStartDate}T12:00:00`)
  date.setDate(date.getDate() + offset)
  return localDateKey(date)
}

export function AddToMealPlanButton({
  recipeId,
  recipeTitle,
  preview,
}: {
  recipeId: number
  recipeTitle: string
  preview?: RecipePost
}) {
  const { account } = useAuth()
  const [loginOpen, setLoginOpen] = useState(false)
  const [planOpen, setPlanOpen] = useState(false)
  const [shoppingOpen, setShoppingOpen] = useState(false)
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)
  const triggerRef = useRef<HTMLButtonElement>(null)
  const menuItemRef = useRef<HTMLButtonElement>(null)
  const [weekStartDate, setWeekStartDate] = useState(() => mondayOf(new Date()))
  const [selectedSlots, setSelectedSlots] = useState<Set<string>>(() => new Set())
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")
  const [notice, setNotice] = useState("")
  const [completedSlots, setCompletedSlots] = useState(0)

  const openAction = () => {
    setMenuOpen(false)
    setNotice("")
    if (!account) {
      setLoginOpen(true)
      return
    }
    setError("")
    setWeekStartDate(mondayOf(new Date()))
    setSelectedSlots(new Set())
    setCompletedSlots(0)
    setPlanOpen(true)
  }

  useEffect(() => {
    if (!menuOpen) return
    menuItemRef.current?.focus()
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!menuRef.current?.contains(event.target as Node)) setMenuOpen(false)
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setMenuOpen(false)
        triggerRef.current?.focus()
      }
    }
    document.addEventListener("mousedown", closeOnOutsideClick)
    document.addEventListener("keydown", closeOnEscape)
    return () => {
      document.removeEventListener("mousedown", closeOnOutsideClick)
      document.removeEventListener("keydown", closeOnEscape)
    }
  }, [menuOpen])

  if (account?.role === "ADMIN") return null

  const submit = async () => {
    if (selectedSlots.size === 0) return
    setBusy(true)
    setError("")
    const slots = Array.from(selectedSlots, (key) => {
      const [mealDate, mealType] = key.split(":")
      return { mealDate, mealType: mealType as MealType }
    }).sort((left, right) => left.mealDate.localeCompare(right.mealDate) ||
      mealOptions.findIndex((meal) => meal.code === left.mealType) -
      mealOptions.findIndex((meal) => meal.code === right.mealType))
    const added: MealPlanSlot[] = []
    const failed: Array<{ slot: MealPlanSlot; message: string }> = []

    for (const slot of slots) {
      try {
        const slotWeekStartDate = mondayOf(new Date(`${slot.mealDate}T12:00:00`))
        await mealPlanApi.addEntry(slotWeekStartDate, {
          mealDate: slot.mealDate,
          mealType: slot.mealType,
          recipeId,
        })
        added.push(slot)
      } catch (cause) {
        failed.push({
          slot,
          message: asApiError(cause).message || "Vui lòng thử lại.",
        })
      }
    }

    if (added.length > 0) {
      setCompletedSlots((count) => count + added.length)
      setSelectedSlots(new Set(failed.map(({ slot }) => slotKey(slot))))
    }

    if (failed.length === 0) {
      const totalAdded = completedSlots + added.length
      setPlanOpen(false)
      setNotice(`Đã thêm ${totalAdded} ${totalAdded === 1 ? "bữa" : "bữa ăn"} vào Lịch ăn.`)
    } else {
      const partialNotice = added.length > 0
        ? `Đã thêm ${added.length} bữa ăn. `
        : ""
      setError(`${partialNotice}${failed[0].message} Còn ${failed.length} lựa chọn chưa thêm; bạn có thể thử lại.`)
    }
    setBusy(false)
  }

  const isCurrentWeek = weekStartDate === mondayOf(new Date())
  const formatDate = new Intl.DateTimeFormat("vi-VN", {
    day: "numeric",
    month: "short",
  })
  const weekEndDate = new Date(`${dateAtOffset(weekStartDate, 6)}T12:00:00`)
  const weekLabel = isCurrentWeek
    ? "Tuần này"
    : `${formatDate.format(new Date(`${weekStartDate}T12:00:00`))} – ${formatDate.format(weekEndDate)}`
  const updateWeek = (offset: number) => {
    setWeekStartDate((current) => dateAtOffset(current, offset))
    setError("")
  }

  const toggleSlot = (slot: MealPlanSlot) => {
    setSelectedSlots((current) => {
      const next = new Set(current)
      const key = slotKey(slot)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
    setError("")
  }

  return (
    <div className="relative" ref={menuRef}>
      <button
        ref={triggerRef}
        type="button"
        aria-label={`Tùy chọn cho công thức ${recipeTitle}`}
        aria-haspopup="menu"
        aria-expanded={menuOpen}
        disabled={busy}
        onClick={() => setMenuOpen((open) => !open)}
        className="flex h-8 w-8 items-center justify-center rounded-full bg-black/65 text-white transition-colors hover:bg-black/85 disabled:opacity-60"
      >
        {busy ? (
          <LoaderCircle className="h-4 w-4 animate-spin" />
        ) : (
          <Ellipsis className="h-5 w-5" />
        )}
      </button>
      {menuOpen && (
        <div
          role="menu"
          aria-label={`Hành động cho ${recipeTitle}`}
          className="absolute right-0 top-full z-30 mt-1 min-w-52 overflow-hidden rounded-lg border border-brand-100 bg-paper py-1 text-ink shadow-lg"
        >
          <button
            ref={menuItemRef}
            type="button"
            role="menuitem"
            onClick={openAction}
            className="w-full px-4 py-2.5 text-left text-sm transition-colors hover:bg-raised focus-visible:bg-raised focus-visible:outline-none"
          >
            Thêm vào lịch ăn
          </button>
          <button
            type="button"
            role="menuitem"
            onClick={() => { setMenuOpen(false); setShoppingOpen(true) }}
            className="w-full px-4 py-2.5 text-left text-sm transition-colors hover:bg-raised focus-visible:bg-raised focus-visible:outline-none"
          >
            Thêm vào danh sách đi chợ
          </button>
        </div>
      )}
      {notice && (
        <p
          role="status"
          className="absolute right-0 top-full z-20 mt-2 w-60 rounded-lg border border-brand-200 bg-paper p-3 text-xs text-ink shadow-md"
        >
          {notice}
        </p>
      )}
      <Modal
        open={loginOpen}
        onClose={() => setLoginOpen(false)}
        title="Đăng nhập để thêm vào lịch ăn"
      >
        <p className="text-sm leading-6 text-ink-muted">
          Đăng nhập để thêm công thức vào lịch ăn tuần 3 bữa của bạn.
        </p>
        <Link
          to="/dang-nhap"
          className="mt-5 inline-block rounded-full bg-brand-600 px-5 py-2.5 text-sm font-semibold text-on-brand"
        >
          Đăng nhập
        </Link>
      </Modal>
      <Modal
        open={planOpen}
        onClose={() => setPlanOpen(false)}
        title="Chọn ngày"
        size="lg"
        tone="dark"
      >
        <p className="mb-4 truncate text-sm text-white/60" title={recipeTitle}>
          {recipeTitle}
        </p>
        <div className="mx-auto mb-2 flex max-w-sm items-center justify-between">
          <button
            type="button"
            aria-label="Tuần trước"
            onClick={() => updateWeek(-7)}
            disabled={busy}
            className="flex h-10 w-10 items-center justify-center rounded-full text-white transition-colors hover:bg-white/10 focus-visible:outline focus-visible:outline-2 focus-visible:outline-white disabled:opacity-40"
          >
            <ChevronLeft className="h-5 w-5" />
          </button>
          <span className="font-semibold text-white">{weekLabel}</span>
          <button
            type="button"
            aria-label="Tuần sau"
            onClick={() => updateWeek(7)}
            disabled={busy}
            className="flex h-10 w-10 items-center justify-center rounded-full text-white transition-colors hover:bg-white/10 focus-visible:outline focus-visible:outline-2 focus-visible:outline-white disabled:opacity-40"
          >
            <ChevronRight className="h-5 w-5" />
          </button>
        </div>
        <div className="max-h-[min(48vh,430px)] overflow-y-auto border-t border-white/10">
          <div className="sticky top-0 z-10 grid grid-cols-[minmax(0,1fr)_repeat(3,40px)] items-center border-b border-white/10 bg-[#191919] px-4 py-2 text-center text-xs font-semibold text-white/60">
            <span className="text-left">Ngày</span>
            {mealOptions.map((meal) => <span key={meal.code}>{meal.shortLabel}</span>)}
          </div>
          {dayOptions.map((day) => {
            const date = new Date(`${dateAtOffset(weekStartDate, day.offset)}T12:00:00`)
            const dateLabel = new Intl.DateTimeFormat("vi-VN", {
              day: "numeric",
              month: "short",
            }).format(date)
            return (
              <div
                key={day.offset}
                className="grid min-h-[56px] grid-cols-[minmax(0,1fr)_repeat(3,40px)] items-center border-b border-white/10 px-4 text-[13px] text-white sm:text-sm"
              >
                <span className="pr-1 leading-5">{day.label} · {dateLabel}</span>
                {mealOptions.map((meal) => {
                  const slot = {
                    mealDate: dateAtOffset(weekStartDate, day.offset),
                    mealType: meal.code,
                  }
                  const selected = selectedSlots.has(slotKey(slot))
                  return (
                    <label key={meal.code} className="flex h-10 cursor-pointer items-center justify-center rounded-md hover:bg-white/5 has-[:focus-visible]:outline has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-white">
                      <input
                        type="checkbox"
                        aria-label={`${meal.label}, ${day.label} ${dateLabel}`}
                        checked={selected}
                        onChange={() => toggleSlot(slot)}
                        disabled={busy}
                        className="peer sr-only"
                      />
                      <span
                        aria-hidden="true"
                        className="flex h-5 w-5 items-center justify-center rounded-md border-2 border-white/55 transition-colors after:h-2 after:w-2 after:scale-0 after:rounded-sm after:bg-white after:content-[''] after:transition-transform peer-checked:border-brand-400 peer-checked:bg-brand-500 peer-checked:after:scale-100 peer-focus-visible:outline peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-white"
                      />
                    </label>
                  )
                })}
              </div>
            )
          })}
        </div>
        {error && (
          <p role="alert" className="mt-3 text-sm font-medium text-red-300">
            {error}
          </p>
        )}
        <button
          type="button"
          onClick={submit}
          disabled={busy || selectedSlots.size === 0}
          className="mt-6 flex h-11 w-full items-center justify-center rounded-full bg-brand-500 px-5 font-semibold text-white transition-colors hover:bg-brand-400 disabled:cursor-not-allowed disabled:bg-white/25 disabled:text-white/60"
        >
          {busy ? <LoaderCircle className="h-5 w-5 animate-spin" /> : `Hoàn tất${selectedSlots.size > 0 ? ` · ${selectedSlots.size}` : ""}`}
        </button>
      </Modal>
      <AddRecipeShoppingDialog
        open={shoppingOpen}
        onClose={() => setShoppingOpen(false)}
        recipeId={recipeId}
        recipeTitle={recipeTitle}
        preview={preview}
      />
    </div>
  )
}
