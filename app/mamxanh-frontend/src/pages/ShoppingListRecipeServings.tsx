import { useEffect, useRef, useState } from "react"
import { useNavigate } from "react-router-dom"
import { Minus, Plus } from "lucide-react"
import { Button } from "../components/ui"
import { shoppingApi, type ShoppingRecipeSource } from "../api/shopping"
import { asApiError } from "../lib/apiClient"

export function ShoppingListRecipeServings({
  listId,
  recipeId,
  servings,
  onChange,
}: {
  listId: number
  recipeId: number
  servings: number
  onChange: (value: number) => void
}) {
  const navigate = useNavigate()
  const [source, setSource] = useState<ShoppingRecipeSource | null>(null)
  const [savedServings, setSavedServings] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState("")
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
    setSource(null)
    setSavedServings(null)
    setLoading(true)
    setError("")
    if (!Number.isSafeInteger(listId) || listId < 1 || !Number.isSafeInteger(recipeId) || recipeId < 1) {
      setError("Liên kết danh sách mua sắm không hợp lệ.")
      setLoading(false)
      return () => { active = false }
    }
    shoppingApi.get(listId)
      .then((list) => {
        if (!active) return
        const found = list.recipeSources?.find((item) => item.recipeId === recipeId)
        if (!found || found.servings == null || found.occurrenceCount < 1) {
          setError("Danh sách này không có đủ dữ liệu khẩu phần để chỉnh món. Hãy mở lại món từ danh sách mua sắm.")
          return
        }
        setSource(found)
        setSavedServings(found.servings)
        onChange(found.servings)
      })
      .catch((cause) => {
        if (active) setError(asApiError(cause).message)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [listId, recipeId, onChange, retry])

  async function save() {
    const minimum = (source?.occurrenceCount ?? 1) * 0.5
    if (!source || pending.current || !Number.isFinite(servings) || servings < minimum || servings > 500
        || !Number.isInteger(servings * 10) || servings === savedServings) return
    pending.current = true
    setSaving(true)
    setError("")
    try {
      await shoppingApi.updateRecipeServings(listId, recipeId, servings)
      if (!alive.current) return
      navigate(`/di-cho?listId=${listId}`, { replace: true })
    } catch (cause) {
      if (alive.current) setError(asApiError(cause).message)
    } finally {
      pending.current = false
      if (alive.current) setSaving(false)
    }
  }

  const minimum = (source?.occurrenceCount ?? 1) * 0.5
  const validServings = Number.isFinite(servings) && servings >= minimum && servings <= 500
      && Number.isInteger(servings * 10)

  return (
    <div className="recipe-detail-no-print min-w-0 space-y-2" aria-label="Khẩu phần trong danh sách mua sắm" aria-busy={loading || saving}>
      <div className="flex flex-wrap items-center gap-2">
        <div className="flex items-center gap-2 rounded-xl bg-brand-50 p-1">
          <button type="button" aria-label="Giảm khẩu phần" disabled={!source || loading || saving || servings <= minimum}
            onClick={() => onChange(Math.max(minimum, Math.round((servings - 0.5) * 10) / 10))}
            className="rounded-lg p-2 text-brand-700 hover:bg-paper disabled:opacity-40">
            <Minus className="h-4 w-4" />
          </button>
          <span aria-live="polite" className="min-w-20 text-center text-sm font-bold text-ink">
            {source ? `${servings.toLocaleString("vi-VN")} phần` : "—"}
          </span>
          <button type="button" aria-label="Tăng khẩu phần" disabled={!source || loading || saving || servings >= 500}
            onClick={() => onChange(Math.min(500, Math.round((servings + 0.5) * 10) / 10))}
            className="rounded-lg p-2 text-brand-700 hover:bg-paper disabled:opacity-40">
            <Plus className="h-4 w-4" />
          </button>
        </div>
        <Button size="sm" disabled={!source || loading || saving || !validServings || servings === savedServings} onClick={() => void save()}>
          {saving ? "Đang lưu…" : "Lưu khẩu phần vào danh sách"}
        </Button>
      </div>
      {loading && <p role="status" className="text-xs text-ink-muted">Đang tải khẩu phần trong danh sách…</p>}
      {source && <p className="text-xs leading-5 text-ink-muted">Tổng khẩu phần của {source.occurrenceCount} lần xuất hiện. Lưu xong sẽ quay lại danh sách mua sắm.</p>}
      {error && (
        <div role="alert" className="text-sm text-ink-muted">
          <p>{error}</p>
          <Button size="sm" variant="ghost" disabled={loading || saving} onClick={() => setRetry((value) => value + 1)}>Thử lại</Button>
        </div>
      )}
    </div>
  )
}
