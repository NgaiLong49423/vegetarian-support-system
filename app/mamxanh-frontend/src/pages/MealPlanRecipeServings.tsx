import { useEffect, useRef, useState } from "react"
import { Link, useSearchParams } from "react-router-dom"
import { Minus, Plus } from "lucide-react"
import { Button } from "../components/ui"
import { mealPlanApi, type MealPlanWeek } from "../api/recipes"
import { asApiError } from "../lib/apiClient"

type Entry = MealPlanWeek["entries"][number]
const mealLabels = {
  BREAKFAST: "Bữa sáng",
  LUNCH: "Bữa trưa",
  DINNER: "Bữa tối",
}

export function MealPlanRecipeServings({
  recipeId,
  servings,
  onChange,
}: {
  recipeId: number
  servings: number
  onChange: (value: number) => void
}) {
  const [params] = useSearchParams()
  const week = params.get("mealPlanWeek") ?? ""
  const entryId = Number(params.get("mealPlanEntryId"))
  const [entry, setEntry] = useState<Entry | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState("")
  const [notice, setNotice] = useState("")
  const [retry, setRetry] = useState(0)
  const alive = useRef(true)
  const pending = useRef(false)

  useEffect(() => {
    alive.current = true
    return () => {
      alive.current = false
    }
  }, [])

  useEffect(() => {
    let active = true
    setEntry(null)
    setLoading(true)
    setError("")
    setNotice("")
    if (
      !/^\d{4}-\d{2}-\d{2}$/.test(week) ||
      !Number.isSafeInteger(entryId) ||
      entryId < 1
    ) {
      setError("Liên kết món trong lịch ăn không hợp lệ.")
      setLoading(false)
      return () => {
        active = false
      }
    }
    mealPlanApi
      .getWeek(week)
      .then((plan) => {
        if (!active) return
        const found = plan.entries.find(
          (item) => item.entryId === entryId && item.recipeId === recipeId,
        )
        if (!found || found.recipeDeleted || found.unavailableMessage) {
          setError(
            "Món này không còn thuộc lịch ăn đã chọn. Hãy mở lại món từ lịch ăn.",
          )
          return
        }
        setEntry(found)
        onChange(found.plannedServings)
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
  }, [week, entryId, recipeId, onChange, retry])

  async function save() {
    if (
      !entry ||
      pending.current ||
      !Number.isFinite(servings) ||
      servings < 0.5 ||
      servings > 10 ||
      !Number.isInteger(servings * 2)
    )
      return
    pending.current = true
    setSaving(true)
    setError("")
    setNotice("")
    try {
      const saved = await mealPlanApi.updateServings(
        week,
        entry.entryId,
        servings,
      )
      if (!alive.current) return
      setEntry(saved)
      onChange(saved.plannedServings)
      setNotice(`Đã lưu ${saved.plannedServings.toLocaleString("vi-VN")} phần cần nấu vào lịch ăn.`)
    } catch (cause) {
      if (alive.current) setError(asApiError(cause).message)
    } finally {
      pending.current = false
      if (alive.current) setSaving(false)
    }
  }

  const validServings = Number.isFinite(servings) && servings >= 0.5 && servings <= 10 && Number.isInteger(servings * 2)
  const decreaseServings = () => {
    const next = validServings ? servings - 0.5 : Math.floor(servings * 2) / 2
    onChange(Math.max(0.5, next))
    setNotice("")
  }
  const increaseServings = () => {
    const next = validServings ? servings + 0.5 : Math.ceil(servings * 2) / 2
    onChange(Math.min(10, next))
    setNotice("")
  }

  return (
    <div
      className="recipe-detail-no-print min-w-0 space-y-2"
      aria-label="Khẩu phần món trong lịch ăn"
      aria-busy={saving || loading}
    >
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex items-center gap-2 rounded-xl bg-brand-50 p-1">
          <button
            type="button"
            aria-label="Giảm khẩu phần"
            disabled={!entry || loading || saving || servings <= 0.5}
            onClick={decreaseServings}
            className="rounded-lg p-2 text-brand-700 hover:bg-paper disabled:opacity-40"
          >
            <Minus className="h-4 w-4" />
          </button>
          <span className="min-w-20 text-center text-sm font-bold text-ink">
            {entry ? `${servings.toLocaleString("vi-VN")} phần` : "—"}
          </span>
          <button
            type="button"
            aria-label="Tăng khẩu phần"
            disabled={!entry || loading || saving || servings >= 10}
            onClick={increaseServings}
            className="rounded-lg p-2 text-brand-700 hover:bg-paper disabled:opacity-40"
          >
            <Plus className="h-4 w-4" />
          </button>
        </div>
        <Button
          size="sm"
          disabled={
            !entry || loading || saving || !validServings || servings === entry.plannedServings
          }
          onClick={() => void save()}
        >
          {saving ? "Đang lưu…" : "Lưu khẩu phần vào lịch ăn"}
        </Button>
      </div>
      {loading && (
        <p role="status" className="text-xs text-ink-muted">
          Đang tải khẩu phần trong lịch ăn…
        </p>
      )}
      {entry && (
        <p className="text-xs leading-5 text-ink-muted">
          {mealLabels[entry.mealType]} ·{" "}
          {new Date(`${entry.mealDate}T12:00:00`).toLocaleDateString("vi-VN")} ·
          Đã lưu {entry.plannedServings.toLocaleString("vi-VN")} phần cần nấu.
          Thay đổi chỉ áp dụng cho món trong bữa này.
        </p>
      )}
      {notice && (
        <p role="status" className="text-sm text-leaf-700">
          {notice}
        </p>
      )}
      {error && (
        <div role="alert" className="text-sm text-ink-muted">
          <p>{error}</p>
          {!entry && (
            <Button
              size="sm"
              variant="ghost"
              onClick={() => setRetry((value) => value + 1)}
            >
              Thử lại
            </Button>
          )}
        </div>
      )}
      <Link
        to={`/ke-hoach?weekStartDate=${encodeURIComponent(week)}${
          entry ? `&mealDate=${entry.mealDate}` : ""
        }`}
        className="inline-block text-sm font-semibold text-brand-700"
      >
        Trở về lịch ăn
      </Link>
    </div>
  )
}
