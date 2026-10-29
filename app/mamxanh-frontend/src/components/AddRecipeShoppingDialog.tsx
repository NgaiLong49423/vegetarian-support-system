import { useEffect, useState } from "react"
import { Link, useNavigate } from "react-router-dom"
import { LoaderCircle, Plus, ShoppingBasket, Trash2 } from "lucide-react"
import { shoppingApi, type ShoppingCreation, type ShoppingSummary } from "../api/shopping"
import { recipesApi, type IngredientOption, type IngredientRecipeUnit, type RecipeDetail, type RecipePost } from "../api/recipes"
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

function normalizeItemName(name: string) {
  return name.replace(/\u00a0/g, " ").trim().replace(/\s+/g, " ").toLocaleLowerCase("vi")
}

function ingredientSelection(ids: number[] | undefined, ingredients: Array<{ ingredientId: number }> | undefined) {
  return [...new Set(ids ?? ingredients?.map((item) => item.ingredientId) ?? [])]
}

function defaultShoppingListName() {
  return `Danh sách mua sắm ${new Intl.DateTimeFormat("en-GB", { timeZone: "Asia/Ho_Chi_Minh" }).format(new Date())}`
}

function nextDailyShoppingListName(baseName: string, summaries: ShoppingSummary[]) {
  const escapedBase = baseName.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
  const numberedName = new RegExp(`^${escapedBase} \\((\\d+)\\)$`)
  let hasBaseName = false
  const highestNumber = summaries.reduce((highest, summary) => {
    if (summary.name === baseName) {
      hasBaseName = true
      return highest
    }
    const match = summary.name.match(numberedName)
    return match ? Math.max(highest, Number(match[1])) : highest
  }, 0)
  if (!hasBaseName) return baseName
  return `${baseName} (${highestNumber + 1})`
}

type ManualItemDraft = {
  key: number
  query: string
  ingredient: IngredientOption | null
  quantity: string
  unitId: number | null
  units: IngredientRecipeUnit[]
  searchStatus: "idle" | "searching" | "results" | "empty" | "error"
  searchResults: IngredientOption[]
  unitStatus: "idle" | "loading" | "ready" | "error"
  searchAttempt: number
}


export function AddRecipeShoppingDialog({
  open,
  onClose,
  recipeId,
  recipeTitle,
  preview,
  selectedIngredientIds: initialSelectedIngredientIds,
  hideIngredientPicker = false,
  onAdded,
}: {
  open: boolean
  onClose: () => void
  recipeId: number
  recipeTitle: string
  preview?: Pick<RecipePost, "id" | "title" | "servings" | "ingredients">
  selectedIngredientIds?: number[]
  hideIngredientPicker?: boolean
  onAdded?: (result: ShoppingCreation) => void
}) {
  const { account } = useAuth()
  const navigate = useNavigate()
  const [lists, setLists] = useState<ShoppingSummary[]>([])
  const [page, setPage] = useState(0)
  const [hasMore, setHasMore] = useState(false)
  const [selectedListId, setSelectedListId] = useState<number | null>(null)
  const [createMode, setCreateMode] = useState(false)
  const [name, setName] = useState(defaultShoppingListName())
  const [servings, setServings] = useState(Math.min(50, Math.max(1, preview?.servings || 1)))
  const [loading, setLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")
  const [recipe, setRecipe] = useState<ShoppingRecipePreview | null>(preview ?? null)
  const [recipeLoading, setRecipeLoading] = useState(false)
  const [selectedIngredientIds, setSelectedIngredientIds] = useState<number[]>(() => ingredientSelection(initialSelectedIngredientIds, preview?.ingredients))
  const [manualItems, setManualItems] = useState<ManualItemDraft[]>([])
  const [nextManualItemKey, setNextManualItemKey] = useState(1)
  const manualSearchKey = JSON.stringify(manualItems.map(({ key, query, searchAttempt, ingredient }) => ({ key, query, searchAttempt, ingredientId: ingredient?.ingredientId ?? null })))

  useEffect(() => {
    if (!open) return
    const timers: number[] = []
    for (const item of manualItems) {
      const searchedName = item.query.trim()
      if (!searchedName || item.ingredient) continue
      const timer = window.setTimeout(() => {
        setManualItems((current) => current.map((row) => row.key === item.key && row.query === item.query
          ? { ...row, searchStatus: "searching" }
          : row))
        void recipesApi.searchIngredients(searchedName).then((results) => {
          setManualItems((current) => current.map((row) => row.key === item.key && row.query === item.query
            ? { ...row, searchStatus: results.length ? "results" : "empty", searchResults: results }
            : row))
        }).catch(() => {
          setManualItems((current) => current.map((row) => row.key === item.key && row.query === item.query
            ? { ...row, searchStatus: "error", searchResults: [] }
            : row))
        })
      }, 250)
      timers.push(timer)
    }
    return () => timers.forEach(window.clearTimeout)
  }, [open, manualSearchKey])

  useEffect(() => {
    if (!open || !account || account.role === "ADMIN") return
    let active = true
    setRecipe(preview ?? null)
    setSelectedIngredientIds(ingredientSelection(initialSelectedIngredientIds, preview?.ingredients))
    setManualItems([])
    setNextManualItemKey(1)
    setLists([])
    setPage(0)
    setHasMore(false)
    setSelectedListId(null)
    setCreateMode(false)
    setName(defaultShoppingListName())
    setServings(Math.min(50, Math.max(1, preview?.servings || 1)))
    setError("")
    setLoading(true)
    if (!preview) {
      setRecipeLoading(true)
      recipesApi.getPublished(recipeId).then((detail: RecipeDetail) => {
        if (active) {
          setRecipe({ id: detail.recipeId, title: detail.title, servings: detail.servings, ingredients: detail.ingredients })
          setSelectedIngredientIds(ingredientSelection(initialSelectedIngredientIds, detail.ingredients))
          setServings(Math.min(50, Math.max(1, detail.servings || 1)))
        }
      }).catch((cause) => {
        if (active) setError(asApiError(cause).message)
      }).finally(() => {
        if (active) setRecipeLoading(false)
      })
    }
    shoppingApi.list(0).then(async (result) => {
      if (!active) return
      setLists(result.content)
      setHasMore(result.page + 1 < result.totalPages)
      const allSummaries = [...result.content]
      for (let currentPage = 1; currentPage < result.totalPages; currentPage += 1) {
        const nextPage = await shoppingApi.list(currentPage)
        allSummaries.push(...nextPage.content)
      }
      if (active) setName(nextDailyShoppingListName(defaultShoppingListName(), allSummaries))
    }).catch((cause) => {
      if (active) setError(asApiError(cause).message)
    }).finally(() => {
      if (active) setLoading(false)
    })
    return () => { active = false }
  }, [open, account, recipeId, recipeTitle, preview, initialSelectedIngredientIds])

  async function selectManualIngredient(key: number, ingredient: IngredientOption) {
    setManualItems((current) => current.map((row) => row.key === key
      ? { ...row, query: ingredient.name, ingredient, units: [], unitId: null, unitStatus: "loading", searchResults: [], searchStatus: "idle" }
      : row))
    try {
      const units = await recipesApi.getIngredientUnits(ingredient.ingredientId)
      if (units.length === 0) throw new Error("Nguyên liệu chưa có đơn vị được hỗ trợ.")
      const defaultUnit = units.find((unit) => unit.code === "g") ?? units[0]
      setManualItems((current) => current.map((row) => row.key === key && row.ingredient?.ingredientId === ingredient.ingredientId
        ? { ...row, units, unitId: defaultUnit.id, unitStatus: "ready" }
        : row))
    } catch {
      setManualItems((current) => current.map((row) => row.key === key && row.ingredient?.ingredientId === ingredient.ingredientId
        ? { ...row, unitStatus: "error" }
        : row))
    }
  }

  function updateManualQuery(key: number, query: string) {
    setManualItems((current) => current.map((row) => row.key === key
      ? { ...row, query, ingredient: null, units: [], unitId: null, unitStatus: "idle", searchResults: [], searchStatus: query.trim() ? "searching" : "idle", searchAttempt: row.searchAttempt + 1 }
      : row))
  }

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
    if (!account || account.role === "ADMIN" || busy || !recipe) return
    if (createMode && (!name.trim() || name.trim().length > 200)) {
      setError("Tên danh sách phải có từ 1 đến 200 ký tự.")
      return
    }
    if (!createMode && selectedListId === null) {
      setError("Hãy chọn danh sách mua sắm muốn thêm công thức.")
      return
    }
    if (manualItems.some((item) => item.searchStatus === "searching" || item.searchStatus === "error" || item.unitStatus === "loading" || item.unitStatus === "error")) {
      setError("Hãy chờ tìm nguyên liệu và tải đơn vị hoàn tất hoặc thử lại trước khi thêm.")
      return
    }
    if (manualItems.some((item) => !item.ingredient || item.unitStatus !== "ready" || !item.units.some((unit) => unit.id === item.unitId))) {
      setError("Hãy chọn nguyên liệu trong danh mục và đơn vị được hỗ trợ.")
      return
    }
    const normalizedManualItems = manualItems.map((item) => ({ name: item.ingredient!.name, quantity: Number(item.quantity), unitId: item.unitId! }))
    if (normalizedManualItems.some((item) => !Number.isFinite(item.quantity) || item.quantity <= 0 || !item.unitId)) {
      setError("Kiểm tra số lượng và đơn vị của nguyên liệu đã chọn.")
      return
    }
    if (selectedIngredientIds.length === 0 && normalizedManualItems.length === 0) {
      setError("Hãy chọn ít nhất một nguyên liệu hoặc thêm nguyên liệu từ danh mục.")
      return
    }
    if (normalizedManualItems.length) {
      const duplicateDraft = normalizedManualItems.find((item, index) =>
        normalizedManualItems.some((other, otherIndex) => otherIndex !== index && normalizeItemName(other.name) === normalizeItemName(item.name)),
      )
      if (duplicateDraft) {
        setError(`${duplicateDraft.name} bị trùng tên trong các mặt hàng đang thêm.`)
        return
      }
    }
    if (normalizedManualItems.length && !createMode && selectedListId !== null) {
      try {
        const currentList = await shoppingApi.get(selectedListId)
        const duplicate = normalizedManualItems.find((manual) =>
          currentList.items.some((item) => normalizeItemName(item.name) === normalizeItemName(manual.name)),
        )
        if (duplicate) {
          const existing = currentList.items.find((item) => normalizeItemName(item.name) === normalizeItemName(duplicate.name))
          setError(`${duplicate.name} đã có trong danh sách${existing ? ` (${number(existing.quantity)} ${existing.unitCode})` : ""}.`)
          return
        }
      } catch (cause) {
        setError(asApiError(cause).message)
        return
      }
    }
    setBusy(true)
    setError("")
    try {
      const result = createMode
        ? await shoppingApi.createFromRecipe(name.trim(), recipeId, servings, { selectedIngredientIds, manualItems: normalizedManualItems })
        : await shoppingApi.addRecipe(selectedListId!, recipeId, servings, { selectedIngredientIds, manualItems: normalizedManualItems })
      onClose()
      if (onAdded) onAdded(result)
      else navigate(`/di-cho?listId=${result.shoppingList.id}`, {
          state: { shoppingCreation: result },
        })
    } catch (cause) {
      setError(asApiError(cause).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal open={open} onClose={busy ? () => {} : onClose} title="Thêm vào danh sách mua sắm" size="lg" trapFocus>
      {!account ? (
        <div className="space-y-4">
          <p className="text-sm leading-6 text-ink-muted">Đăng nhập để chọn danh sách mua sắm và lưu nguyên liệu của công thức.</p>
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

          {!hideIngredientPicker && <div>
            <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
              <h4 className="font-semibold text-ink">Nguyên liệu sẽ thêm</h4>
              <div className="flex items-center gap-3 text-xs text-ink-muted">
                <span>{selectedIngredientIds.length}/{new Set(recipe?.ingredients.map((item) => item.ingredientId) ?? []).size} nguyên liệu · gốc {recipe?.servings ?? 1} phần</span>
                {recipe?.ingredients.length && !initialSelectedIngredientIds ? <button type="button" className="font-semibold text-brand-700 underline" onClick={() => setSelectedIngredientIds(selectedIngredientIds.length === new Set(recipe.ingredients.map((item) => item.ingredientId)).size ? [] : [...new Set(recipe.ingredients.map((item) => item.ingredientId))])}>{selectedIngredientIds.length === new Set(recipe.ingredients.map((item) => item.ingredientId)).size ? "Bỏ chọn tất cả" : "Chọn tất cả"}</button> : null}
              </div>
            </div>
            {recipeLoading ? <p role="status" className="flex items-center gap-2 py-5 text-sm text-ink-muted"><LoaderCircle className="h-4 w-4 animate-spin" />Đang tải công thức…</p> : !recipe?.ingredients.length ? <p className="rounded-xl bg-raised/50 p-3 text-sm text-ink-muted">Công thức chưa có nguyên liệu.</p> : <ul className="max-h-52 divide-y divide-brand-100 overflow-y-auto rounded-xl border border-brand-100 px-3">
              {recipe.ingredients.map((item, index) => (
                <li key={`${item.ingredientId}-${index}`} className="flex items-center gap-3 py-2.5 text-sm">
                  <input type="checkbox" aria-label={`Thêm ${item.name}`} checked={selectedIngredientIds.includes(item.ingredientId)} disabled={busy} onChange={(event) => setSelectedIngredientIds((current) => event.target.checked ? [...new Set([...current, item.ingredientId])] : current.filter((id) => id !== item.ingredientId))} className="h-4 w-4 shrink-0 accent-brand-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600" />
                  <span className="min-w-0 flex-1 break-words font-medium text-ink">{item.name}</span>
                  <strong className="shrink-0 tabular-nums text-ink">{number(recipe.servings > 0 ? item.quantity * servings / recipe.servings : item.quantity)} {item.unitCode || item.unitName}</strong>
                </li>
              ))}
            </ul>}
          </div>}

          {!hideIngredientPicker && <section aria-label="Thêm nguyên liệu từ danh mục">
            <div className="mb-2 flex items-center justify-between gap-2">
              <h4 className="font-semibold text-ink">Thêm nguyên liệu</h4>
              <Button type="button" variant="outline" size="sm" disabled={busy || manualItems.length >= 50} onClick={() => {
                const key = nextManualItemKey
                setNextManualItemKey((value) => value + 1)
                setManualItems((current) => [...current, { key, query: "", ingredient: null, quantity: "1", unitId: null, units: [], searchStatus: "idle", searchResults: [], unitStatus: "idle", searchAttempt: 0 }])
              }}><Plus className="h-4 w-4" />Thêm nguyên liệu</Button>
            </div>
            {manualItems.length === 0 ? <p className="text-xs text-ink-muted">Tìm và chọn nguyên liệu có trong danh mục của ứng dụng.</p> : <div className="space-y-2">
              {manualItems.map((item) => {
                return <div key={item.key} className="grid grid-cols-1 items-end gap-2 rounded-xl border border-brand-100 p-3 sm:grid-cols-[minmax(0,1fr)_6rem_8rem_auto]">
                <div className="relative min-w-0 text-xs font-medium text-ink-muted">
                  <label className="block">Tìm nguyên liệu trong danh mục<input value={item.query} maxLength={200} placeholder="Nhập tên nguyên liệu để tìm" onChange={(event) => updateManualQuery(item.key, event.target.value)} className="mt-1 block w-full rounded-lg border border-brand-100 bg-paper px-3 py-2 text-sm text-ink" /></label>
                  {item.searchStatus === "searching" && <span role="status" className="mt-1 block text-[11px]">{item.ingredient ? "Đang tải đơn vị được hỗ trợ…" : "Đang tìm nguyên liệu…"}</span>}
                  {!item.ingredient && item.searchStatus === "results" && <ul role="listbox" aria-label="Kết quả nguyên liệu" className="absolute left-0 right-0 top-full z-30 mt-1 max-h-36 overflow-y-auto rounded-lg border border-brand-100 bg-paper py-1 shadow-lg">{item.searchResults.map((result) => <li key={result.ingredientId}><button type="button" role="option" aria-selected="false" onClick={() => void selectManualIngredient(item.key, result)} className="w-full px-3 py-2 text-left text-sm text-ink hover:bg-raised">{result.name}</button></li>)}</ul>}
                  {!item.ingredient && item.searchStatus === "empty" && <div role="status" className="absolute left-0 right-0 top-full z-30 mt-1 rounded-lg border border-green-200 bg-green-50 px-3 py-2 text-[11px] font-medium text-green-800 shadow-sm">Không có nguyên liệu phù hợp trong danh mục.</div>}
                  {!item.ingredient && item.searchStatus === "error" && <span className="mt-1 block text-[11px] text-red-700">Không tìm được nguyên liệu. <button type="button" className="font-semibold underline" onClick={() => setManualItems((current) => current.map((row) => row.key === item.key ? { ...row, searchStatus: "searching", searchAttempt: row.searchAttempt + 1 } : row))}>Thử lại</button></span>}
                  {item.ingredient && <span className="mt-1 block text-[11px] text-green-800">Đã chọn: {item.ingredient.name} <button type="button" className="font-semibold underline" onClick={() => updateManualQuery(item.key, "")}>Đổi nguyên liệu</button></span>}
                  {item.ingredient && item.unitStatus === "error" && <span className="mt-1 block text-[11px] text-red-700">Không tải được đơn vị. <button type="button" className="font-semibold underline" onClick={() => void selectManualIngredient(item.key, item.ingredient!)}>Thử lại</button></span>}
                </div>
                <label className="text-xs font-medium text-ink-muted">Số lượng<input type="number" min="0.01" step="0.01" value={item.quantity} onChange={(event) => setManualItems((current) => current.map((row) => row.key === item.key ? { ...row, quantity: event.target.value } : row))} className="mt-1 block w-full rounded-lg border border-brand-100 bg-paper px-2 py-2 text-sm text-ink" /></label>
                <label className="text-xs font-medium text-ink-muted">Đơn vị<select value={item.unitId ?? ""} disabled={!item.ingredient || item.unitStatus !== "ready"} onChange={(event) => setManualItems((current) => current.map((row) => row.key === item.key ? { ...row, unitId: Number(event.target.value) } : row))} className="mt-1 block w-full rounded-lg border border-brand-100 bg-paper px-2 py-2 text-sm text-ink disabled:cursor-wait disabled:opacity-60"><option value="">{item.ingredient ? "Đang tải…" : "Chọn nguyên liệu"}</option>{item.units.map((unit) => <option key={unit.id} value={unit.id}>{unit.name} ({unit.code})</option>)}</select></label>
                <Button type="button" variant="ghost" size="sm" aria-label={`Xóa ${item.ingredient?.name || "nguyên liệu"}`} disabled={busy} onClick={() => setManualItems((current) => current.filter((row) => row.key !== item.key))}><Trash2 className="h-4 w-4" /></Button>
              </div>
              })}
            </div>}
          </section>}

          {hideIngredientPicker && (
            <section aria-label="Nguyên liệu đã chọn" className="space-y-2">
              <div className="flex items-center justify-between gap-3">
                <h4 className="font-semibold text-ink">Nguyên liệu đã chọn ({selectedIngredientIds.length})</h4>
                {selectedIngredientIds.length > 0 && (
                  <button type="button" disabled={busy} onClick={() => setSelectedIngredientIds([])} className="text-xs font-semibold text-brand-700 underline underline-offset-2 disabled:opacity-50">
                    Bỏ chọn tất cả
                  </button>
                )}
              </div>
              {recipeLoading ? (
                <p role="status" className="py-3 text-sm text-ink-muted">Đang tải nguyên liệu…</p>
              ) : selectedIngredientIds.length > 0 ? (
                <ul className="max-h-44 divide-y divide-brand-100 overflow-y-auto rounded-xl border border-brand-100 px-3">
                  {recipe?.ingredients.filter((item) => selectedIngredientIds.includes(item.ingredientId)).map((item, index) => (
                    <li key={`${item.ingredientId}-${index}`} className="flex items-center gap-3 py-2.5 text-sm">
                      <input
                        type="checkbox"
                        aria-label={`Thêm ${item.name}`}
                        checked
                        disabled={busy}
                        onChange={() => setSelectedIngredientIds((current) => current.filter((id) => id !== item.ingredientId))}
                        className="h-4 w-4 shrink-0 accent-brand-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600"
                      />
                      <span className="min-w-0 flex-1 break-words font-medium text-ink">{item.name}</span>
                      <strong className="shrink-0 tabular-nums text-ink">
                        {number(recipe && recipe.servings > 0 ? item.quantity * servings / recipe.servings : item.quantity)} {item.unitCode || item.unitName}
                      </strong>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="rounded-lg bg-raised/50 px-3 py-2 text-sm text-ink-muted">Chưa chọn nguyên liệu nào. Bạn có thể chọn lại ở bài công thức hoặc thêm nguyên liệu từ danh mục bên trên.</p>
              )}
            </section>
          )}
          <section aria-label="Chọn danh sách mua sắm">
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
              <Button type="submit" disabled={busy || loading || recipeLoading || !recipe || (!createMode && selectedListId === null)}>
              {busy ? <><LoaderCircle className="h-4 w-4 animate-spin" />Đang thêm…</> : <><ShoppingBasket className="h-4 w-4" />{createMode ? "Tạo danh sách và thêm" : "Thêm vào danh sách"}</>}
            </Button>
          </div>
        </form>
      )}
    </Modal>
  )
}
