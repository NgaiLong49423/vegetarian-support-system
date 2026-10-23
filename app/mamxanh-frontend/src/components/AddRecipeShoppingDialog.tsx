import { useEffect, useState } from "react"
import { Link, useNavigate } from "react-router-dom"
import { LoaderCircle, Plus, ShoppingBasket } from "lucide-react"
import { shoppingApi, type ShoppingSummary } from "../api/shopping"
import { recipesApi, type RecipeDetail, type RecipePost } from "../api/recipes"
import { asApiError } from "../lib/apiClient"
import { useAuth } from "./AuthContext"
import { Button } from "./ui"
import { Modal } from "./Modal"

type ShoppingRecipePreview = {
  id: number
  title: string
  servings: number
  ingredients: Array<{ ingredientId: number; name: string; quantity: number; unitCode?: string; unitName?: string }>
}

function number(value: number) {
  return new Intl.NumberFormat("vi-VN", { maximumFractionDigits: 2 }).format(value)
}

export function AddRecipeShoppingDialog({
  open,
  onClose,
  recipeId,
  recipeTitle,
  preview,
}: {
  open: boolean
  onClose: () => void
  recipeId: number
  recipeTitle: string
  preview?: Pick<RecipePost, "id" | "title" | "servings" | "ingredients">
}) {
  const { account } = useAuth()
  const navigate = useNavigate()
  const [lists, setLists] = useState<ShoppingSummary[]>([])
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(false)
  const [selectedListId, setSelectedListId] = useState<number | null>(null)
  const [createMode, setCreateMode] = useState(false)
  const [name, setName] = useState(recipeTitle)
  const [servings, setServings] = useState(Math.min(50, Math.max(1, preview?.servings || 1)))
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")
  const [recipe, setRecipe] = useState<ShoppingRecipePreview | null>(preview ?? null)
  const [recipeLoading, setRecipeLoading] = useState(false)

  useEffect(() => {
    if (!open || !account || account.role === "ADMIN") return
    let active = true
    setRecipe(preview ?? null)
    setLists([])
    setPage(0)
    setHasMore(false)
    setSelectedListId(null)
    setCreateMode(false)
    setName(recipeTitle)
    setServings(Math.min(50, Math.max(1, preview?.servings || 1)))
    setError("")
    setLoading(true)
    if (!preview) {
      setRecipeLoading(true)
      recipesApi.getPublished(recipeId).then((detail: RecipeDetail) => {
        if (active) {
          setRecipe({ id: detail.recipeId, title: detail.title, servings: detail.servings, ingredients: detail.ingredients })
          setServings(Math.min(50, Math.max(1, detail.servings || 1)))
        }
      }).catch((cause) => {
        if (active) setError(asApiError(cause).message)
      }).finally(() => {
        if (active) setRecipeLoading(false)
      })
    }
    shoppingApi.list(0).then((result) => {
      if (!active) return
      setLists(result.content)
      setHasMore(result.page + 1 < result.totalPages)
    }).catch((cause) => {
      if (active) setError(asApiError(cause).message)
    }).finally(() => {
      if (active) setLoading(false)
    })
    return () => { active = false }
  }, [open, account, recipeId, recipeTitle, preview])

  async function loadMore() {
    if (loading || !hasMore) return
    const nextPage = page + 1
    setLoading(true)
    setError("")
    try {
      const result = await shoppingApi.list(nextPage)
      setLists((current) => [...current, ...result.content])
      setPage(nextPage)
      setHasMore(result.page + 1 < result.totalPages)
    } catch (cause) {
      setError(asApiError(cause).message)
    } finally {
      setLoading(false)
    }
  }

  async function submit() {
    if (!account || account.role === "ADMIN" || busy || !recipe?.ingredients.length) return
    if (createMode && (!name.trim() || name.trim().length > 200)) {
      setError("Tên danh sách phải có từ 1 đến 200 ký tự.")
      return
    }
    if (!createMode && selectedListId === null) {
      setError("Hãy chọn danh sách đi chợ muốn thêm công thức.")
      return
    }
    setBusy(true)
    setError("")
    try {
      const result = createMode
        ? await shoppingApi.createFromRecipe(name.trim(), recipeId, servings)
        : await shoppingApi.addRecipe(selectedListId!, recipeId, servings)
      onClose()
      navigate(`/di-cho?listId=${result.shoppingList.id}`, {
        state: { shoppingCreation: result },
      })
    } catch (cause) {
      setError(asApiError(cause).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal open={open} onClose={busy ? () => {} : onClose} title="Thêm vào danh sách đi chợ" size="lg" trapFocus>
      {!account ? (
        <div className="space-y-4">
          <p className="text-sm leading-6 text-ink-muted">Đăng nhập để chọn danh sách đi chợ và lưu nguyên liệu của công thức.</p>
          <Link to="/dang-nhap" className="inline-flex min-h-10 items-center rounded-full bg-brand-600 px-5 py-2 text-sm font-semibold text-on-brand">Đăng nhập</Link>
        </div>
      ) : account.role === "ADMIN" ? (
        <p className="text-sm text-ink-muted">Tính năng này dành cho thành viên.</p>
      ) : (
        <form onSubmit={(event) => { event.preventDefault(); void submit() }} className="space-y-5" aria-busy={busy}>
          <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-brand-100 bg-raised/50 p-3">
            <p className="min-w-0 truncate font-semibold text-ink" title={recipeTitle}>{recipeTitle}</p>
            <div aria-label="Khẩu phần cần mua" className="inline-flex items-center gap-2 rounded-full border border-brand-100 bg-paper px-2 py-1">
              <button type="button" aria-label="Giảm khẩu phần cần mua" disabled={servings <= 1 || busy} onClick={() => setServings((value) => Math.max(1, value - 1))} className="flex h-9 w-9 items-center justify-center rounded-full text-brand-600 hover:bg-raised disabled:opacity-40">−</button>
              <span className="min-w-16 text-center text-sm font-bold tabular-nums text-ink">{servings} phần</span>
              <button type="button" aria-label="Tăng khẩu phần cần mua" disabled={servings >= 50 || busy} onClick={() => setServings((value) => Math.min(50, value + 1))} className="flex h-9 w-9 items-center justify-center rounded-full text-brand-600 hover:bg-raised disabled:opacity-40"><Plus className="h-4 w-4" /></button>
            </div>
          </div>

          <div>
            <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
              <h4 className="font-semibold text-ink">Nguyên liệu sẽ thêm</h4>
              <span className="text-xs text-ink-muted">Tính từ khẩu phần gốc {recipe?.servings ?? 1} phần</span>
            </div>
            {recipeLoading ? <p role="status" className="flex items-center gap-2 py-5 text-sm text-ink-muted"><LoaderCircle className="h-4 w-4 animate-spin" />Đang tải công thức…</p> : !recipe?.ingredients.length ? <p className="rounded-xl bg-raised/50 p-3 text-sm text-ink-muted">{error ? "Không tải được nguyên liệu công thức." : "Công thức chưa có nguyên liệu hợp lệ để thêm."}</p> : <ul className="max-h-40 divide-y divide-brand-100 overflow-y-auto rounded-xl border border-brand-100 px-3">
              {recipe.ingredients.map((item, index) => (
                <li key={`${item.ingredientId}-${index}`} className="flex items-center justify-between gap-3 py-2.5 text-sm">
                  <span className="min-w-0 break-words font-medium text-ink">{item.name}</span>
                  <strong className="shrink-0 tabular-nums text-ink">{number(recipe.servings > 0 ? item.quantity * servings / recipe.servings : item.quantity)} {item.unitCode || item.unitName}</strong>
                </li>
              ))}
            </ul>}
          </div>

          <section aria-label="Chọn danh sách đi chợ">
            <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
              <h4 className="font-semibold text-ink">{createMode ? "Danh sách mới" : "Chọn danh sách"}</h4>
              <Button type="button" variant="ghost" size="sm" disabled={busy} onClick={() => { setCreateMode((value) => !value); setError("") }}>
                {createMode ? "Chọn danh sách có sẵn" : "Tạo danh sách mới"}
              </Button>
            </div>
            {createMode ? (
              <label className="block text-sm font-medium text-ink">
                Tên danh sách
                <input autoFocus maxLength={200} value={name} onChange={(event) => setName(event.target.value)} className="mt-1.5 w-full rounded-xl border border-brand-100 bg-paper px-3 py-2.5 text-ink outline-none focus-visible:border-brand-500 focus-visible:ring-4 focus-visible:ring-brand-100" />
              </label>
            ) : loading && lists.length === 0 ? (
              <p role="status" className="flex items-center gap-2 py-5 text-sm text-ink-muted"><LoaderCircle className="h-4 w-4 animate-spin" />Đang tải danh sách…</p>
            ) : lists.length === 0 ? (
              <p className="rounded-xl bg-raised/50 p-3 text-sm text-ink-muted">Bạn chưa có danh sách nào. Tạo danh sách mới để thêm công thức này.</p>
            ) : (
              <div className="max-h-48 space-y-2 overflow-y-auto">
                {lists.map((list) => (
                  <label key={list.id} className={`flex cursor-pointer items-center gap-3 rounded-xl border px-3 py-3 text-sm transition-colors ${selectedListId === list.id ? "border-brand-500 bg-brand-50" : "border-brand-100 hover:bg-raised/60"}`}>
                    <input type="radio" name="shopping-list" value={list.id} checked={selectedListId === list.id} onChange={() => setSelectedListId(list.id)} disabled={busy} />
                    <span className="min-w-0 flex-1 truncate font-medium text-ink">{list.name}</span>
                  </label>
                ))}
                {hasMore && <Button type="button" variant="ghost" disabled={loading || busy} onClick={() => void loadMore()}>{loading ? "Đang tải…" : "Tải thêm danh sách"}</Button>}
              </div>
            )}
          </section>

          {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" disabled={busy} onClick={onClose}>Hủy</Button>
            <Button type="submit" disabled={busy || loading || recipeLoading || !recipe?.ingredients.length || (!createMode && selectedListId === null)}>
              {busy ? <><LoaderCircle className="h-4 w-4 animate-spin" />Đang thêm…</> : <><ShoppingBasket className="h-4 w-4" />{createMode ? "Tạo danh sách và thêm" : "Thêm vào danh sách"}</>}
            </Button>
          </div>
        </form>
      )}
    </Modal>
  )
}
