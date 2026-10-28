import { useEffect, useRef, useState } from "react"
import { Link, useLocation, useSearchParams } from "react-router-dom"
import {
  CalendarDays,
  ChevronRight,
  Info,
  MoreHorizontal,
  MoreVertical,
  Pencil,
  Plus,
  ShoppingBasket,
  Trash2,
  Utensils,
  Users,
} from "lucide-react"
import { PageContainer } from "../components/Layout"
import { Button, Card } from "../components/ui"
import { Modal } from "../components/Modal"
import SpringCheck from "../components/SpringCheck"
import { ShoppingListExport } from "../components/ShoppingListExport"
import { IngredientImage } from "../components/IngredientImage"
import { useAuth } from "../components/AuthContext"
import {
  shoppingApi,
  type ShoppingChecklist,
  type ShoppingCreation,
  type ShoppingItem,
  type ShoppingSummary,
  type ShoppingUnit,
} from "../api/shopping"
import { asApiError } from "../lib/apiClient"
import { discoveryApi, type PublicIngredient, type PublicIngredientUnit } from "../api/discovery"

type Tab = "all" | "todo" | "done"
const SHOPPING_LISTS_PER_PAGE = 20
const field =
  "min-w-0 rounded-lg border border-brand-200 bg-cream-soft px-3 py-2 text-sm text-ink focus-visible:outline-2 focus-visible:outline-brand-400"
const quantityText = (item: ShoppingItem) =>
  `${item.quantity.toLocaleString("vi-VN", { maximumFractionDigits: 2 })} ${item.unitCode}`
const unknownQuantityText = (item: ShoppingItem) => {
  const totals = new Map<string, number>()
  item.sources?.filter((source) => source.sourceKind === "UNKNOWN").forEach((source) => {
    const unit = source.originalUnitCode ?? item.unitCode
    totals.set(unit, (totals.get(unit) ?? 0) + source.originalQuantity)
  })
  return [...totals.entries()]
    .map(([unit, quantity]) => `${quantity.toLocaleString("vi-VN", { maximumFractionDigits: 6 })} ${unit}`)
    .join(" · ")
}
const normalizeItemName = (name: string) => name.replace(/\u00a0/g, " ").trim().replace(/\s+/g, " ").toLocaleLowerCase("vi")
const MASS_GRAMS_PER_UNIT: Record<string, number> = { mg: 0.001, g: 1, kg: 1000 }
const VOLUME_MILLILITERS_PER_UNIT: Record<string, number> = { ml: 1, L: 1000, tsp: 5, tbsp: 15, cup: 240 }

type RecipeAmountChange = { key: string; itemId: number; name: string; quantity: number; unitCode: string; removeItem: boolean }

function recipeServingChanges(list: ShoppingChecklist, recipeId: number, requested: number): RecipeAmountChange[] {
  const recipe = list.recipeSources?.find((source) => source.recipeId === recipeId)
  if (!recipe?.servings || requested <= 0) return []
  const occurrences = new Map<string, { servings: number; rows: Array<{ item: ShoppingItem; source: NonNullable<ShoppingItem["sources"]>[number] }> }>()
  for (const item of list.items) {
    item.sources?.filter((source) => source.sourceKind === "RECIPE" && source.recipeId === recipeId).forEach((source) => {
      const current = occurrences.get(source.sourceId) ?? { servings: source.servings ?? 0, rows: [] }
      current.rows.push({ item, source })
      occurrences.set(source.sourceId, current)
    })
  }
  const entries = [...occurrences.entries()]
  if (!entries.length || entries.some(([, occurrence]) => occurrence.servings <= 0)) return []
  const targetTenths = Math.round(requested * 10)
  const weights = entries.map(([, occurrence]) => Math.round(occurrence.servings * 10))
  const allocated = Array(entries.length).fill(0) as number[]
  const remainders = Array(entries.length).fill(0) as number[]
  let active = entries.map((_, index) => index)
  let remaining = targetTenths
  while (active.length) {
    const totalWeight = active.reduce((sum, index) => sum + weights[index], 0)
    const belowMinimum = active.find((index) => remaining * weights[index] < 5 * totalWeight)
    if (belowMinimum !== undefined) {
      allocated[belowMinimum] = 5
      remaining -= 5
      active = active.filter((index) => index !== belowMinimum)
      continue
    }
    let baseTotal = 0
    active.forEach((index) => {
      const numerator = remaining * weights[index]
      allocated[index] = Math.floor(numerator / totalWeight)
      remainders[index] = numerator % totalWeight
      baseTotal += allocated[index]
    })
    const leftover = remaining - baseTotal
    active.sort((left, right) => remainders[right] - remainders[left] || left - right)
    for (let index = 0; index < leftover; index += 1) allocated[active[index]] += 1
    break
  }
  const totals = new Map<string, RecipeAmountChange>()
  entries.forEach(([, occurrence], occurrenceIndex) => {
    const nextServings = allocated[occurrenceIndex] / 10
    occurrence.rows.forEach(({ item, source }) => {
      const unitCode = source.originalUnitCode ?? item.unitCode
      const key = `${item.id}:${unitCode}`
      const current = totals.get(key) ?? { key, itemId: item.id, name: item.name, quantity: 0, unitCode, removeItem: false }
      current.quantity += source.originalQuantity * nextServings / occurrence.servings - source.originalQuantity
      totals.set(key, current)
    })
  })
  return [...totals.values()].filter((change) => Math.abs(change.quantity) >= 0.0000005)
    .map((change) => ({ ...change, quantity: Math.abs(change.quantity) }))
}

function recipeRemovalChanges(list: ShoppingChecklist, recipeId: number): RecipeAmountChange[] {
  const totals = new Map<string, RecipeAmountChange>()
  for (const item of list.items) {
    const recipeSources = item.sources?.filter((source) => source.sourceKind === "RECIPE" && source.recipeId === recipeId) ?? []
    recipeSources.forEach((source) => {
      const unitCode = source.originalUnitCode ?? item.unitCode
      const key = `${item.id}:${unitCode}`
      const current = totals.get(key) ?? { key, itemId: item.id, name: item.name, quantity: 0, unitCode, removeItem: false }
      current.quantity += source.originalQuantity
      totals.set(key, current)
    })
    if (recipeSources.length && recipeSources.length === item.sources?.length) {
      const unitCode = recipeSources[0].originalUnitCode ?? item.unitCode
      const key = `${item.id}:${unitCode}`
      const current = totals.get(key)
      if (current) current.removeItem = true
    }
  }
  return [...totals.values()]
}

function quantityDeltaInUnit(
  currentQuantity: number,
  currentUnitId: number,
  currentUnitCode: string,
  nextQuantity: number,
  nextUnitId: number,
  nextUnitCode: string,
  ingredientUnits: PublicIngredientUnit[],
): number | null {
  if (currentUnitId === nextUnitId) return nextQuantity - currentQuantity
  const currentIngredientUnit = ingredientUnits.find((unit) => unit.id === currentUnitId)
  const nextIngredientUnit = ingredientUnits.find((unit) => unit.id === nextUnitId)
  if (currentIngredientUnit && nextIngredientUnit && nextIngredientUnit.gramsPerUnit !== 0) {
    return nextQuantity - currentQuantity * currentIngredientUnit.gramsPerUnit / nextIngredientUnit.gramsPerUnit
  }
  const currentMassFactor = MASS_GRAMS_PER_UNIT[currentUnitCode]
  const nextMassFactor = MASS_GRAMS_PER_UNIT[nextUnitCode]
  if (currentMassFactor != null && nextMassFactor != null)
    return nextQuantity - currentQuantity * currentMassFactor / nextMassFactor
  const currentVolumeFactor = VOLUME_MILLILITERS_PER_UNIT[currentUnitCode]
  const nextVolumeFactor = VOLUME_MILLILITERS_PER_UNIT[nextUnitCode]
  if (currentVolumeFactor != null && nextVolumeFactor != null)
    return nextQuantity - currentQuantity * currentVolumeFactor / nextVolumeFactor
  return null
}

function quantityAmountText(quantity: number, unitCode: string) {
  return `${Math.abs(quantity).toLocaleString("vi-VN", { maximumFractionDigits: 6 })} ${unitCode}`
}

function summarizeIngredientSources(item: ShoppingItem) {
  const grouped = new Map<number, {
    recipeId: number
    title: string | null
    available: boolean
    quantities: Map<string, { quantity: number; unitCode: string | null }>
    servingsBySource: Map<string, number | null>
  }>()
  item.sources?.filter((source) => source.sourceKind === "RECIPE" && source.recipeId != null).forEach((source) => {
    const recipeId = source.recipeId!
    let existing = grouped.get(recipeId)
    if (!existing) {
      existing = {
        recipeId,
        title: source.title,
        available: source.available,
        quantities: new Map(),
        servingsBySource: new Map(),
      }
      grouped.set(recipeId, existing)
    }
    const unitKey = source.originalUnitCode ?? ""
    const quantity = existing.quantities.get(unitKey)
    if (quantity) quantity.quantity += source.originalQuantity
    else existing.quantities.set(unitKey, { quantity: source.originalQuantity, unitCode: source.originalUnitCode })
    if (!existing.servingsBySource.has(source.sourceId))
      existing.servingsBySource.set(source.sourceId, source.servings)
  })
  return [...grouped.values()].map((source) => {
    const servings = [...source.servingsBySource.values()]
    return {
      ...source,
      servings: servings.length && servings.every((value) => value != null)
        ? servings.reduce<number>((total, value) => total + (value ?? 0), 0)
        : null,
      quantities: [...source.quantities.values()],
    }
  }).sort((a, b) => (a.title ?? "").localeCompare(b.title ?? "", "vi"))
}

export function ShoppingList() {
  const { account } = useAuth()
  if (!account)
    return (
      <PageContainer className="py-12">
        <Card className="mx-auto max-w-xl p-6">
          <h1 className="text-xl font-semibold text-ink">
            Danh sách mua sắm của bạn
          </h1>
          <p className="my-4 text-sm text-ink-muted">
            Vui lòng đăng nhập để tạo và lưu danh sách mua sắm theo tài khoản.
          </p>
          <Link to="/dang-nhap" className="font-semibold text-brand-600">
            Đăng nhập
          </Link>
        </Card>
      </PageContainer>
    )
  if (account.role === "ADMIN")
    return (
      <PageContainer className="py-12">
        <p role="alert" className="text-sm text-ink">
          Tài khoản Quản trị viên không có quyền quản lý danh sách mua sắm của
          thành viên.
        </p>
      </PageContainer>
    )
  return <ShoppingWorkspace key={account.id} />
}

function ShoppingWorkspace() {
  const [params, setParams] = useSearchParams()
  const location = useLocation()
  const requested = params.get("listId")
  const id =
    requested &&
    /^[1-9]\d*$/.test(requested) &&
    Number.isSafeInteger(Number(requested))
      ? Number(requested)
      : null
  const [lists, setLists] = useState<ShoppingSummary[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [listError, setListError] = useState("")
  const [listLoading, setListLoading] = useState(true)
  const [listRetry, setListRetry] = useState(0)
  const [list, setList] = useState<ShoppingChecklist | null>(null)
  const [lastChangedItemId, setLastChangedItemId] = useState<number | null>(null)
  const [lastChangedGroup, setLastChangedGroup] = useState<string | null>(null)
  const [units, setUnits] = useState<ShoppingUnit[]>([])
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState("")
  const [retry, setRetry] = useState(0)
  const [tab, setTab] = useState<Tab>("todo")
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")
  const [notice, setNotice] = useState("")
  const [editor, setEditor] = useState<ShoppingItem | "new" | null>(null)
  const [removal, setRemoval] = useState<ShoppingItem | "bought" | null>(null)
  const [sourceDetails, setSourceDetails] = useState<ShoppingItem | null>(null)
  const [recipeMenuId, setRecipeMenuId] = useState<number | null>(null)
  const [recipeAction, setRecipeAction] = useState<{ source: NonNullable<ShoppingChecklist["recipeSources"]>[number]; mode: "servings" | "remove" } | null>(null)
  const [recipeServingsDraft, setRecipeServingsDraft] = useState("")
  const [recipeActionConfirmed, setRecipeActionConfirmed] = useState(false)
  const [manualSourceEditor, setManualSourceEditor] = useState<{ item: ShoppingItem; source: NonNullable<ShoppingItem["sources"]>[number] } | null>(null)
  const [manualSourcePendingQuantity, setManualSourcePendingQuantity] = useState<number | null>(null)
  const [manualSourceDraft, setManualSourceDraft] = useState("")
  const [nameEditor, setNameEditor] = useState(false)
  const [emptyListEditor, setEmptyListEditor] = useState(false)
  const [nameDraft, setNameDraft] = useState("")
  const [failedSourceCovers, setFailedSourceCovers] = useState<Record<string, boolean>>({})
  const recipeCarousel = useRef<HTMLUListElement>(null)
  const [listRemoval, setListRemoval] = useState(false)
  const [otherActionsOpen, setOtherActionsOpen] = useState(false)
  const [listActionMenuId, setListActionMenuId] = useState<number | null>(null)
  const pending = useRef(false)
  const alive = useRef(true)
  const selectedId = useRef(id)
  selectedId.current = id
  const dialogTrigger = useRef<HTMLElement | null>(null)
  const removalActions = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (removal) removalActions.current?.querySelector("button")?.focus()
  }, [removal])
  useEffect(() => {
    alive.current = true
    return () => {
      alive.current = false
    }
  }, [])
  useEffect(() => {
    let active = true
    setListLoading(true)
    setListError("")
    async function loadLists() {
      try {
        const firstPage = await shoppingApi.list(0)
        const summaries = [...firstPage.content]
        for (let currentPage = 1; currentPage < firstPage.totalPages; currentPage += 1) {
          const nextPage = await shoppingApi.list(currentPage)
          summaries.push(...nextPage.content)
        }
        if (!active) return
        summaries.sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt) || b.id - a.id)
        setLists(summaries)
        setTotalPages(firstPage.totalPages)
        setTotalElements(firstPage.totalElements)
      } catch (cause) {
        if (active) setListError(asApiError(cause).message)
      } finally {
        if (active) setListLoading(false)
      }
    }
    void loadLists()
    return () => {
      active = false
    }
  }, [listRetry])
  useEffect(() => {
    if (!lists.length) return
    if (!requested) {
      setParams({ listId: String(lists[0].id) }, { replace: true })
      return
    }
    const selectedIndex = lists.findIndex((summary) => summary.id === Number(requested))
    if (selectedIndex >= 0)
      setPage(Math.floor(selectedIndex / SHOPPING_LISTS_PER_PAGE))
  }, [lists, requested, setParams])
  useEffect(() => {
    let active = true
    setList(null)
    setLastChangedItemId(null)
    setLastChangedGroup(null)
    setLoadError("")
    setError("")
    setNotice("")
    setEditor(null)
    setRemoval(null)
    setSourceDetails(null)
    setRecipeMenuId(null)
    setRecipeAction(null)
    setRecipeServingsDraft("")
    setRecipeActionConfirmed(false)
    setManualSourceEditor(null)
    setManualSourcePendingQuantity(null)
    setManualSourceDraft("")
    setNameEditor(false)
    setEmptyListEditor(false)
    setListRemoval(false)
    setTab("todo")
    if (!id) {
      setLoading(false)
      return
    }
    setLoading(true)
    Promise.all([shoppingApi.get(id), shoppingApi.options()])
      .then(([checklist, options]) => {
        if (active) {
          setList(checklist)
          setUnits(options)
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
  }, [id, retry])
  const items = list?.items ?? []
  const visibleLists = lists.slice(page * SHOPPING_LISTS_PER_PAGE, (page + 1) * SHOPPING_LISTS_PER_PAGE)
  const counts = {
    all: items.length,
    todo: items.filter((item) => !item.bought).length,
    done: items.filter((item) => item.bought).length,
  }
  const grouped = items
    .filter(
      (item) => tab === "all" || (tab === "done" ? item.bought : !item.bought),
    )
    .reduce<Record<string, ShoppingItem[]>>((groups, item) => {
      ;(groups[item.ingredientGroup] ??= []).push(item)
      return groups
    }, {})
  Object.values(grouped).forEach((entries) => entries.sort((a, b) => Number(a.bought) - Number(b.bought) || a.name.localeCompare(b.name, "vi")))
  const groupedEntries = (Object.entries(grouped) as [string, ShoppingItem[]][])
    .map((entry, index) => ({ entry, index }))
    .sort((a, b) => {
      const aIsChanged = a.entry[0] === lastChangedGroup
      const bIsChanged = b.entry[0] === lastChangedGroup
      if (aIsChanged !== bIsChanged) return Number(bIsChanged) - Number(aIsChanged)
      const aHasTodo = a.entry[1].some((item) => !item.bought)
      const bHasTodo = b.entry[1].some((item) => !item.bought)
      return Number(bHasTodo) - Number(aHasTodo) || a.index - b.index
    })
    .map(({ entry }) => entry)
  const groupedColumns: [string, ShoppingItem[]][][] = [[], []]
  const groupedColumnLoads = [0, 0]
  groupedEntries.forEach((entry) => {
    const column = groupedColumnLoads[0] <= groupedColumnLoads[1] ? 0 : 1
    groupedColumns[column].push(entry)
    groupedColumnLoads[column] += entry[1].length
  })
  const creation = (location.state as {
    shoppingCreation?: ShoppingCreation
  } | null)?.shoppingCreation
  const diagnostics = creation?.shoppingList.id === list?.id ? creation : null
  const recipeActionChanges = !recipeAction || !list ? [] : recipeAction.mode === "remove"
    ? recipeRemovalChanges(list, recipeAction.source.recipeId)
    : recipeServingChanges(list, recipeAction.source.recipeId, Number(recipeServingsDraft))
  const changedRecipeItemCount = new Set(recipeActionChanges.map((change) => change.itemId)).size
  const requestedRecipeServings = Number(recipeServingsDraft)
  const minimumRecipeServings = (recipeAction?.source.occurrenceCount ?? 1) * 0.5
  const validRecipeServings = Number.isFinite(requestedRecipeServings)
    && requestedRecipeServings >= minimumRecipeServings
    && requestedRecipeServings <= 500
    && Math.abs(requestedRecipeServings * 10 - Math.round(requestedRecipeServings * 10)) < 0.000001
  // Saved rows stay separate on reload; this hint never performs a conversion.
  const separate = (item: ShoppingItem) =>
    item.ingredientId !== null &&
    items.some(
      (other) =>
        other.id !== item.id &&
        other.ingredientId === item.ingredientId &&
        other.unitId !== item.unitId,
    )
  function closeDialog() {
    if (pending.current) return
    setEditor(null)
    setRemoval(null)
    setSourceDetails(null)
    setRecipeMenuId(null)
    setRecipeAction(null)
    setRecipeServingsDraft("")
    setRecipeActionConfirmed(false)
    setManualSourceEditor(null)
    setManualSourcePendingQuantity(null)
    setManualSourceDraft("")
    setNameEditor(false)
    setEmptyListEditor(false)
    setListRemoval(false)
    setError("")
    requestAnimationFrame(() => {
      if (dialogTrigger.current?.isConnected) dialogTrigger.current.focus()
      else document.querySelector<HTMLElement>("#shopping-title")?.focus()
    })
  }
  function openDialog(action: () => void) {
    dialogTrigger.current = (document.activeElement as HTMLElement)
    setError("")
    action()
  }
  async function mutate(
    action: (
      listId: number,
    ) => Promise<(current: ShoppingChecklist) => ShoppingChecklist>,
    message: string,
  ): Promise<boolean> {
    if (!id || pending.current) return false
    const target = id
    pending.current = true
    setBusy(true)
    setError("")
    setNotice("")
    try {
      const update = await action(target)
      if (!alive.current || selectedId.current !== target) return false
      setList((current) => (current?.id === target ? update(current) : current))
      setNotice(message)
      pending.current = false
      closeDialog()
      return true
    } catch (cause) {
      if (alive.current && selectedId.current === target)
        setError(asApiError(cause).message)
      return false
    } finally {
      pending.current = false
      if (alive.current) setBusy(false)
    }
  }
  async function removeCurrentList() {
    if (!id || pending.current) return
    const target = id
    pending.current = true
    setBusy(true)
    setError("")
    try {
      await shoppingApi.removeList(target)
      if (!alive.current || selectedId.current !== target) return
      const nextCount = Math.max(0, totalElements - 1)
      const remaining = lists.filter((summary) => summary.id !== target)
      setLists(remaining)
      setTotalElements(nextCount)
      setTotalPages(Math.ceil(nextCount / SHOPPING_LISTS_PER_PAGE))
      setPage(0)
      setList(null)
      setNotice("")
      setListRemoval(false)
      setError("")
      pending.current = false
      if (remaining.length) {
        setParams({ listId: String(remaining[0].id) }, { replace: true })
      } else {
        setParams({}, { replace: true })
      }
      requestAnimationFrame(() => {
        if (dialogTrigger.current?.isConnected) dialogTrigger.current.focus()
        else document.querySelector<HTMLElement>("#shopping-title")?.focus()
      })
    } catch (cause) {
      if (alive.current && selectedId.current === target)
        setError(asApiError(cause).message)
    } finally {
      pending.current = false
      if (alive.current) setBusy(false)
    }
  }
  async function createEmptyList(name: string) {
    if (pending.current) return
    pending.current = true
    setBusy(true)
    setError("")
    setNotice("")
    try {
      const created = await shoppingApi.createEmpty(name)
      if (!alive.current) return
      pending.current = false
      setEmptyListEditor(false)
      setListRetry((current) => current + 1)
      setPage(0)
      setParams({ listId: String(created.id) })
      requestAnimationFrame(() => {
        if (dialogTrigger.current?.isConnected) dialogTrigger.current.focus()
        else document.querySelector<HTMLElement>("#shopping-title")?.focus()
      })
    } catch (cause) {
      if (alive.current) setError(asApiError(cause).message)
    } finally {
      pending.current = false
      if (alive.current) setBusy(false)
    }
  }
  const renderGroup = ([group, entries]: [string, ShoppingItem[]]) => (
    <section key={group}>
      <h3 className="mb-3 text-base font-bold text-ink">{group}</h3>
      <div className="divide-y divide-brand-100">
        {[...entries].sort((a, b) => Number(b.id === lastChangedItemId) - Number(a.id === lastChangedItemId) || b.id - a.id).map((item) => (
          <div
            key={item.id}
            data-testid={`shopping-item-${item.id}`}
            className={`grid grid-cols-[minmax(0,1fr)_auto_auto_auto_auto] items-center gap-1 rounded-lg px-1 py-5 sm:gap-3 ${item.bought ? "bg-cream-soft/50" : ""}`}
          >
            <SpringCheck
              checked={item.bought}
              disabled={busy}
              onChange={(bought) =>
                void mutate(
                  async (listId) => {
                    const saved = await shoppingApi.bought(listId, item.id, bought)
                    return (current) => ({
                      ...current,
                      items: current.items.map((row) => row.id === saved.id ? saved : row),
                    })
                  },
                  bought ? "Đã đánh dấu đã mua." : "Đã đánh dấu cần mua.",
                )
              }
              ariaLabel={`Đánh dấu ${item.name} ${item.unitCode}`}
              label={
                <span className="flex min-w-0 items-center gap-3.5">
                  <IngredientImage
                    imageUrl={item.imageUrl ?? null}
                    name={item.name}
                    containerClassName="h-16 w-16 shrink-0 overflow-hidden rounded-lg border border-brand-100 bg-cream-soft md:h-20 md:w-20"
                  />
                  <span className="break-words text-lg font-medium leading-6 text-ink">{item.name}</span>
                </span>
              }
              boxSize={28}
              color="#f97316"
              fillColor="#f97316"
              className="min-w-0 w-full"
            />
            <span className={`text-lg font-medium ${item.bought ? "text-ink-muted line-through" : "text-ink-soft"}`}>
              {quantityText(item)}
            </span>
            {item.sources?.length ? (
              <Button
                variant="outline"
                size="sm"
                className="!h-9 !w-9 !p-0 md:!h-10 md:!w-10"
                aria-label={`Xem món đã dùng ${item.name}`}
                title={`Xem món đã dùng ${item.name}`}
                disabled={busy}
                onClick={() => openDialog(() => setSourceDetails(item))}
              >
                <Info className="h-4 w-4 md:h-5 md:w-5" />
              </Button>
            ) : <span aria-hidden="true" className="h-9 w-9 md:h-10 md:w-10" />}
            <Button
              variant="outline"
              size="sm"
              className="!h-9 !w-9 !p-0 md:!h-10 md:!w-10"
              aria-label={`Sửa ${item.name} ${item.unitCode}`}
              title={`Sửa số lượng ${item.name}`}
              disabled={busy}
              onClick={() => openDialog(() => setEditor(item))}
            >
              <Pencil className="h-4 w-4 md:h-5 md:w-5" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="!h-9 !w-9 !p-0 md:!h-10 md:!w-10"
              aria-label={`Xoá ${item.name} ${item.unitCode}`}
              title={`Xóa ${item.name}`}
              disabled={busy}
              onClick={() => openDialog(() => setRemoval(item))}
            >
              <Trash2 className="h-4 w-4 md:h-5 md:w-5" />
            </Button>
            {separate(item) && (
              <p className="col-span-5 pl-8 text-xs text-ink-muted">
                Giữ dòng riêng theo đơn vị {item.unitCode}; không tự cộng với đơn vị khác.
              </p>
            )}
          </div>
        ))}
      </div>
    </section>
  )
  return (
    <PageContainer className="py-6">
      <div className="grid items-start gap-5 md:grid-cols-[225px_minmax(0,1fr)]">
        <aside className="space-y-4 md:sticky md:top-20">
          <h1
            id="shopping-title"
            tabIndex={-1}
            className="flex items-center gap-2 text-lg font-semibold text-ink"
          >
            <ShoppingBasket className="h-5 w-5 text-orange-500" />
            Danh sách mua sắm
          </h1>
          <p className="text-xs leading-5 text-ink-muted">
            Danh sách được lưu riêng trong tài khoản của bạn.
          </p>
          <Link
            to="/ke-hoach"
            className="flex h-10 w-full items-center justify-center gap-2 rounded-full bg-orange-500 px-3 text-sm font-semibold text-white shadow-sm transition hover:bg-orange-400 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-orange-500"
          >
            <CalendarDays className="h-4 w-4" />
            Tạo từ Lịch ăn
          </Link>
          <Button
            size="md"
            className="h-10 w-full bg-orange-500 text-white shadow-sm hover:bg-orange-400"
            disabled={busy}
            onClick={() => openDialog(() => {
              setNameDraft("")
              setEmptyListEditor(true)
            })}
          >
            <Plus className="h-4 w-4" />
            Tạo danh sách trống
          </Button>
          {listLoading && (
            <p role="status" className="text-xs text-ink-muted">
              Đang tải các danh sách…
            </p>
          )}
          {listError && (
            <div role="alert" className="text-sm text-ink">
              <p>{listError}</p>
              <Button
                size="sm"
                variant="ghost"
                onClick={() => setListRetry((value) => value + 1)}
              >
                Thử tải danh sách
              </Button>
            </div>
          )}
          <nav aria-label="Danh sách đã lưu" className="flex flex-col gap-2">
            {visibleLists.map((summary) => (
              <div
                key={summary.id}
                className={`flex min-w-0 items-start gap-1 rounded-xl border px-2 py-1.5 transition-colors ${
                  summary.id === id
                    ? "border-brand-200 bg-brand-50 text-ink shadow-sm ring-1 ring-brand-100"
                    : "border-transparent bg-paper/70 text-ink-muted hover:bg-paper"
                }`}
              >
                <button
                  type="button"
                  disabled={busy}
                  aria-current={summary.id === id ? "page" : undefined}
                  onClick={() => { setListActionMenuId(null); setParams({ listId: String(summary.id) }) }}
                  className="min-w-0 flex-1 rounded-lg px-1.5 py-1 text-left text-sm focus-visible:outline-2 focus-visible:outline-brand-400"
                >
                  <span className={`block break-words leading-5 ${summary.id === id ? "font-bold text-brand-900" : "font-medium"}`}>{summary.name}</span>
                </button>
                <div className="relative mt-0.5 shrink-0">
                  <button
                    type="button"
                    aria-label={`Thao tác với ${summary.name}`}
                    aria-expanded={listActionMenuId === summary.id}
                    disabled={busy}
                    onClick={() => setListActionMenuId((current) => current === summary.id ? null : summary.id)}
                    className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-ink-muted transition-colors hover:bg-white/80 hover:text-brand-700 focus-visible:outline-2 focus-visible:outline-brand-400 disabled:opacity-50"
                  >
                    <MoreVertical className="h-4 w-4" />
                  </button>
                  {listActionMenuId === summary.id && (
                    <div className="absolute right-0 top-full z-30 mt-1 flex min-w-40 flex-col rounded-xl border border-brand-100 bg-paper p-1.5 shadow-lg">
                      {summary.id !== id ? (
                        <button type="button" onClick={() => { setParams({ listId: String(summary.id) }); setListActionMenuId(null) }} className="rounded-lg px-3 py-2 text-left text-sm text-ink hover:bg-brand-50">Mở danh sách</button>
                      ) : (
                        <>
                          <button type="button" onClick={() => { setNameDraft(summary.name); setListActionMenuId(null); openDialog(() => setNameEditor(true)) }} className="rounded-lg px-3 py-2 text-left text-sm text-ink hover:bg-brand-50">Đổi tên</button>
                          <button type="button" onClick={() => { setListActionMenuId(null); openDialog(() => setListRemoval(true)) }} className="rounded-lg px-3 py-2 text-left text-sm text-red-700 hover:bg-red-50">Xóa danh sách</button>
                        </>
                      )}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </nav>
          {totalPages > 1 && (
            <div className="flex items-center gap-2">
              <Button
                size="sm"
                variant="ghost"
                disabled={busy || listLoading || page === 0}
                onClick={() => setPage((value) => value - 1)}
              >
                Trước
              </Button>
              <span className="text-xs text-ink-muted">
                {page + 1}/{totalPages}
              </span>
              <Button
                size="sm"
                variant="ghost"
                disabled={busy || listLoading || page + 1 >= totalPages}
                onClick={() => setPage((value) => value + 1)}
              >
                Sau
              </Button>
            </div>
          )}
        </aside>
        <Card className="min-w-0 p-4 sm:p-6">
          {requested && !id ? (
            <p role="alert" className="text-sm text-ink">
              Mã danh sách không hợp lệ. Hãy chọn một danh sách đã lưu.
            </p>
          ) : loading ? (
            <p
              role="status"
              className="py-12 text-center text-sm text-ink-muted"
            >
              Đang tải nguyên liệu…
            </p>
          ) : loadError ? (
            <div role="alert" className="text-sm text-ink">
              <p>{loadError}</p>
              <Button
                variant="ghost"
                onClick={() => setRetry((value) => value + 1)}
              >
                Thử lại
              </Button>
            </div>
          ) : !list ? (
            !listLoading &&
            !listError && (
              <div className="py-12 text-center">
                <h2 className="text-lg font-semibold text-ink">
                  Chưa có danh sách mua sắm
                </h2>
                <p className="mt-2 text-sm text-ink-muted">
                  Tạo danh sách riêng để tự thêm món cần mua, không cần Lịch ăn
                  hay công thức.
                </p>
                <Button
                  className="mt-4 bg-orange-500 text-white shadow-sm hover:bg-orange-400"
                  onClick={() => openDialog(() => {
                    setNameDraft("")
                    setEmptyListEditor(true)
                  })}
                >
                  <Plus className="h-4 w-4" />
                  Tạo danh sách trống
                </Button>
                <p className="mt-3 text-sm">
                  Hoặc <Link to="/ke-hoach" className="font-semibold text-orange-600">tạo từ Lịch ăn</Link>.
                </p>
              </div>
            )
          ) : (
            <div aria-busy={busy}>
              <header className="mb-5 rounded-2xl border border-brand-200 bg-gradient-to-r from-orange-50 via-brand-50/70 to-paper px-4 py-4 shadow-sm sm:px-5 sm:py-5">
                <div className="flex min-w-0 items-center gap-3 sm:gap-4">
                  <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-orange-100 text-orange-600 sm:h-12 sm:w-12">
                    <ShoppingBasket className="h-5 w-5 sm:h-6 sm:w-6" />
                  </span>
                  <div className="min-w-0">
                    <p className="text-[10px] font-extrabold uppercase tracking-[0.16em] text-brand-700 sm:text-xs">Danh sách đang mở</p>
                    <h2 className="mt-0.5 break-words text-xl font-extrabold leading-tight tracking-tight text-ink sm:text-2xl">
                      {list.name}
                    </h2>
                  </div>
                </div>
              </header>
              {!!list.recipeSources?.length && (
                <section aria-label="Công thức trong danh sách" className="mb-5 rounded-2xl border border-brand-200 bg-cream-soft p-4 sm:p-5">
                  <div className="flex items-end justify-between gap-3">
                    <div>
                      <p className="text-[10px] font-extrabold uppercase tracking-[0.14em] text-brand-700">Từ công thức</p>
                      <h3 className="mt-1 text-lg font-extrabold text-ink">Món ăn trong danh sách này</h3>
                    </div>
                    <span className="text-xs text-ink-muted">{list.recipeSources.length} món</span>
                  </div>
                  <div className="relative mt-4">
                    <ul ref={recipeCarousel} aria-label="Công thức trong danh sách mua sắm" className="flex snap-x snap-mandatory gap-3 overflow-x-auto scroll-smooth pb-2 pr-12">
                      {list.recipeSources.map((source) => (
                        <li key={source.recipeId} className="relative w-40 shrink-0 snap-start sm:w-48">
                          <button type="button" disabled={busy} aria-label={`Thao tác với ${source.title ?? "công thức"}`} title="Thao tác với công thức"
                            onClick={() => setRecipeMenuId((current) => current === source.recipeId ? null : source.recipeId)}
                            className="absolute right-2 top-2 z-10 flex h-9 w-9 items-center justify-center rounded-full border border-white/15 bg-black/55 text-white shadow-sm backdrop-blur-sm transition-colors hover:bg-black/75 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-white">
                            <MoreHorizontal className="h-4 w-4" aria-hidden="true" />
                          </button>
                          {recipeMenuId === source.recipeId && (
                            <div className="absolute right-0 top-12 z-20 flex min-w-48 flex-col rounded-xl border border-brand-100 bg-paper p-1.5 shadow-lg">
                              <button type="button" disabled={busy || source.servings == null} title={source.servings == null ? "Không thể chỉnh vì danh sách này chưa lưu khẩu phần gốc." : undefined} onClick={() => {
                                setRecipeMenuId(null)
                                setRecipeServingsDraft(String(source.servings ?? ""))
                                setRecipeActionConfirmed(false)
                                openDialog(() => setRecipeAction({ source, mode: "servings" }))
                              }} className="rounded-lg px-3 py-2 text-left text-sm font-medium text-ink hover:bg-brand-50 disabled:opacity-50">Chỉnh khẩu phần</button>
                              <button type="button" disabled={busy} onClick={() => {
                                setRecipeMenuId(null)
                                setRecipeServingsDraft("")
                                setRecipeActionConfirmed(true)
                                openDialog(() => setRecipeAction({ source, mode: "remove" }))
                              }} className="rounded-lg px-3 py-2 text-left text-sm font-medium text-red-700 hover:bg-red-50 disabled:opacity-50">Gỡ khỏi danh sách</button>
                            </div>
                          )}
                          <Link to={`/cong-thuc/id/${source.recipeId}?shoppingListId=${id}`} className="group block w-full rounded-xl text-left focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500">
                            <span className="relative block aspect-square overflow-hidden rounded-xl bg-brand-50 shadow-sm ring-1 ring-brand-100 transition group-hover:shadow-md group-hover:ring-brand-300">
                              {source.coverUrl && !failedSourceCovers[String(source.recipeId)] ? <img src={source.coverUrl} alt={source.title ? `Ảnh món ${source.title}` : "Ảnh món ăn"} loading="lazy" onError={() => setFailedSourceCovers((current) => ({ ...current, [String(source.recipeId)]: true }))} className="h-full w-full object-cover transition duration-200 group-hover:scale-[1.03]" /> : <span className="flex h-full flex-col items-center justify-center gap-2 text-brand-700"><Utensils className="h-8 w-8" aria-hidden="true" /><span className="text-xs font-semibold">Ảnh món ăn</span></span>}
                            </span>
                            <span className="mt-2 block line-clamp-2 text-sm font-bold leading-5 text-ink group-hover:text-brand-700">{source.title ?? "Công thức không còn công khai"}</span>
                            {source.servings != null ? (
                              <span className="mt-1.5 inline-flex items-center gap-1 text-xs leading-4">
                                <Users className="mr-0.5 h-3.5 w-3.5 text-brand-600" aria-hidden="true" />
                                <span className="font-bold text-ink">{source.servings.toLocaleString("vi-VN")}</span>
                                <span className="text-ink-muted">khẩu phần</span>
                              </span>
                            ) : (
                              <span className="mt-1.5 block text-xs leading-4 text-ink-muted">Chưa rõ khẩu phần</span>
                            )}
                          </Link>
                        </li>
                      ))}
                    </ul>
                    <button type="button" aria-label="Xem thêm món ăn" onClick={() => { const carousel = recipeCarousel.current; if (carousel) carousel.scrollBy({ left: carousel.clientWidth * 0.8, behavior: "smooth" }) }} className="absolute right-0 top-1/2 flex h-10 w-10 -translate-y-1/2 items-center justify-center rounded-full border border-brand-200 bg-paper text-brand-800 shadow-md hover:bg-brand-50 focus-visible:outline-2 focus-visible:outline-brand-400"><ChevronRight className="h-5 w-5" aria-hidden="true" /></button>
                  </div>
                </section>
              )}
              {diagnostics && diagnostics.warnings.length > 0 && (
                <section
                  aria-label="Lưu ý khi tạo danh sách"
                  className="mb-5 rounded-xl border border-brand-200 bg-cream-soft p-4 text-sm text-ink-soft"
                >
                  <h3 className="font-semibold">Lưu ý khi tạo danh sách</h3>
                  <ul className="mt-2 list-disc space-y-1 pl-5">
                    {diagnostics.warnings.map((warning, index) => (
                      <li key={index}>{warning.message}</li>
                    ))}
                  </ul>
                </section>
              )}
              {notice && (
                <p role="status" className="mb-4 text-sm text-orange-500">
                  {notice}
                </p>
              )}
              {busy && !editor && !removal && (
                <p role="status" className="mb-4 text-sm text-ink-muted">Đang lưu danh sách…</p>
              )}
              {error && !editor && !removal && (
                <p role="alert" className="mb-4 text-sm text-ink">
                  {error}
                </p>
              )}
              <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                <nav
                  aria-label="Lọc danh sách mua sắm"
                  className="flex flex-wrap gap-2"
                >
                  {([
                    ["todo", "Cần mua"],
                    ["done", "Đã mua"],
                    ["all", "Tất cả"],
                  ] as const).map(([value, label]) => (
                    <button
                      key={value}
                      type="button"
                      aria-pressed={tab === value}
                      onClick={() => setTab(value)}
                      className={`rounded-lg px-3 py-2 text-sm focus-visible:outline-2 focus-visible:outline-brand-400 ${
                        tab === value
                          ? "bg-raised font-semibold text-black"
                          : "text-black"
                      }`}
                    >
                      {label} ({counts[value]})
                    </button>
                  ))}
                </nav>
                <div className="flex flex-wrap items-center gap-2">
                  <Button
                    size="sm"
                    className="bg-orange-500 text-white hover:bg-orange-400"
                    disabled={busy || units.length === 0}
                    onClick={() => openDialog(() => setEditor("new"))}
                  >
                    <Plus className="h-4 w-4" />
                    Thêm mặt hàng
                  </Button>
                  <details className="relative">
                    <summary aria-label="Thao tác khác" className="flex h-9 cursor-pointer list-none items-center gap-2 rounded-full border border-brand-200 px-3 text-sm font-semibold text-ink-soft hover:border-brand-400 focus-visible:outline-2 focus-visible:outline-brand-400">
                      <MoreHorizontal className="h-4 w-4" /> Thao tác khác
                    </summary>
                    <div className="absolute right-0 z-20 mt-2 flex min-w-52 flex-col gap-1 rounded-xl border border-brand-100 bg-paper p-2 shadow-lg">
                      <Button variant="ghost" size="sm" className="justify-start" disabled={busy} onClick={() => openDialog(() => { setNameDraft(list.name); setNameEditor(true) })}><Pencil className="h-4 w-4" />Đổi tên</Button>
                      <Button variant="ghost" size="sm" className="justify-start text-red-700 hover:bg-red-50" disabled={busy} onClick={() => openDialog(() => setListRemoval(true))}><Trash2 className="h-4 w-4" />Xóa danh sách</Button>
                      {counts.done > 0 && <Button variant="ghost" size="sm" className="justify-start" disabled={busy} onClick={() => openDialog(() => setRemoval("bought"))}><Trash2 className="h-4 w-4" />Xóa {counts.done} mục đã mua</Button>}
                      <ShoppingListExport key={list.id} items={list.items} listName={list.name} disabled={busy} onNotice={setNotice} compact />
                    </div>
                  </details>
                </div>
              </div>
              {Object.entries(grouped).length ? (
                <div className="grid grid-cols-1 gap-y-5 lg:grid-cols-2">
                  {groupedColumns.map((column, index) => (
                    <div key={index} className={`min-w-0 space-y-5 ${index === 0 ? "lg:border-r lg:border-brand-200 lg:pr-7" : "lg:pl-7"}`}>
                      {column.map(renderGroup)}
                    </div>
                  ))}
                </div>
              ) : (
                <p className="py-12 text-center text-sm text-ink-muted">
                  Không có nguyên liệu trong mục này.
                </p>
              )}
            </div>
          )}
        </Card>
      </div>
      <Modal
        trapFocus
        open={recipeAction !== null}
        onClose={closeDialog}
        title={recipeAction?.mode === "remove" ? "Gỡ món khỏi danh sách mua sắm" : "Chỉnh nhanh khẩu phần"}
        size="lg"
      >
        {recipeAction && list && (
          <form className="space-y-4" onSubmit={(event) => {
            event.preventDefault()
            if (recipeAction.mode === "servings" && !recipeActionConfirmed) {
              if (!validRecipeServings) {
                setError(`Khẩu phần phải từ ${minimumRecipeServings.toLocaleString("vi-VN")} đến 500, theo bước 0,1.`)
                return
              }
              setError("")
              setRecipeActionConfirmed(true)
              return
            }
            void mutate(async (listId) => {
              const saved = recipeAction.mode === "servings"
                ? await shoppingApi.updateRecipeServings(listId, recipeAction.source.recipeId, requestedRecipeServings)
                : await shoppingApi.removeRecipe(listId, recipeAction.source.recipeId)
              return () => saved
            }, recipeAction.mode === "servings" ? "Đã cập nhật khẩu phần và nguyên liệu trong danh sách." : "Đã gỡ món và cập nhật nguyên liệu trong danh sách.")
          }}>
            <div className="rounded-xl border border-brand-100 bg-cream-soft p-4">
              <p className="text-xs font-extrabold uppercase tracking-wide text-brand-700">Món trong danh sách này</p>
              <h3 className="mt-1 text-base font-bold text-ink">{recipeAction.source.title ?? "Công thức"}</h3>
              {recipeAction.mode === "servings" && <p className="mt-1 text-sm text-ink-muted">{recipeAction.source.servings?.toLocaleString("vi-VN") ?? "?"} khẩu phần hiện tại</p>}
            </div>
            {recipeAction.mode === "servings" && !recipeActionConfirmed && (
              <label className="block text-sm font-semibold text-ink" htmlFor="shopping-recipe-servings">Tổng khẩu phần mới
                <input id="shopping-recipe-servings" type="number" min={minimumRecipeServings} max="500" step="0.1" required
                  value={recipeServingsDraft} onChange={(event) => { setRecipeServingsDraft(event.target.value); setError("") }}
                  className={`${field} mt-2 w-full`} />
                <span className="mt-1 block text-xs font-normal text-ink-muted">Ít nhất {minimumRecipeServings.toLocaleString("vi-VN")} khẩu phần để mỗi lần xuất hiện còn tối thiểu 0,5 khẩu phần.</span>
              </label>
            )}
            {(recipeActionConfirmed || recipeAction.mode === "remove") && (
              <section className="space-y-2 rounded-xl border border-brand-100 p-4" aria-live="polite">
                <h4 className="font-semibold text-ink">{recipeAction.mode === "remove" ? `Nguyên liệu bị ảnh hưởng · ${changedRecipeItemCount} mặt hàng` : `Xác nhận thay đổi · ${changedRecipeItemCount} mặt hàng`}</h4>
                {recipeAction.mode === "servings" && (
                  <p className="text-sm text-ink-muted">Khẩu phần: {recipeAction.source.servings?.toLocaleString("vi-VN")} → <strong className="text-ink">{requestedRecipeServings.toLocaleString("vi-VN")}</strong>. Chỉ các nguyên liệu từ món này thay đổi; phần người dùng thêm và phần mất xác định được giữ nguyên.</p>
                )}
                {recipeActionChanges.length ? (
                  <ul className="max-h-64 space-y-2 overflow-y-auto">
                    {recipeActionChanges.map((change) => (
                      <li key={change.key} className="flex items-center justify-between gap-3 rounded-lg bg-cream-soft px-3 py-2 text-sm">
                        <span className="font-medium text-ink">{change.name}</span>
                        <span className="shrink-0 text-right text-ink-soft">
                          {recipeAction.mode === "remove"
                            ? `${change.quantity.toLocaleString("vi-VN", { maximumFractionDigits: 3 })} ${change.unitCode} · ${change.removeItem ? "sẽ xóa mặt hàng" : "sẽ giảm"}`
                            : `${requestedRecipeServings > (recipeAction.source.servings ?? 0) ? "Tăng" : "Giảm"} ${change.quantity.toLocaleString("vi-VN", { maximumFractionDigits: 3 })} ${change.unitCode}`}
                        </span>
                      </li>
                    ))}
                  </ul>
                ) : <p className="rounded-lg bg-cream-soft px-3 py-3 text-sm text-ink-muted">Không có định lượng nguyên liệu nào thay đổi.</p>}
                <p className="text-xs text-ink-muted">Thay đổi chỉ áp dụng cho danh sách mua sắm này; bài công thức và Lịch ăn không bị sửa.</p>
              </section>
            )}
            {error && <p role="alert" className="text-sm text-red-700">{error}</p>}
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" disabled={busy} onClick={() => {
                if (recipeAction.mode === "servings" && recipeActionConfirmed) {
                  setRecipeActionConfirmed(false)
                  setError("")
                } else closeDialog()
              }}>{recipeAction.mode === "servings" && recipeActionConfirmed ? "Quay lại" : "Hủy"}</Button>
              {recipeAction.mode === "servings" && !recipeActionConfirmed ? (
                <Button type="submit" disabled={busy || !validRecipeServings}>Xem thay đổi</Button>
              ) : (
                <Button type="submit" disabled={busy || recipeActionChanges.length === 0} className={recipeAction.mode === "remove" ? "bg-red-700 text-white hover:bg-red-600" : undefined}>
                  {busy ? "Đang lưu…" : recipeAction.mode === "remove" ? "Xác nhận gỡ món" : "Xác nhận thay đổi"}
                </Button>
              )}
            </div>
          </form>
        )}
      </Modal>
      <Modal
        trapFocus
        open={sourceDetails !== null}
        onClose={closeDialog}
        title={`Chi tiết ${sourceDetails?.name ?? "nguyên liệu"}`}
        size="lg"
      >
        {sourceDetails && (
          <div className="space-y-4">
            <div className="flex items-center justify-between gap-3 rounded-xl bg-cream-soft px-4 py-3">
              <span className="text-sm font-semibold text-ink">Tổng trong danh sách</span>
              <span className="text-base font-extrabold text-brand-800">{quantityText(sourceDetails)}</span>
            </div>
            {sourceDetails.sources?.some((source) => source.sourceKind === "RECIPE") && <p className="text-sm text-ink-muted">Định lượng bắt buộc từ các công thức:</p>}
            {summarizeIngredientSources(sourceDetails).length ? (
              <ul className="max-h-[55vh] space-y-2 overflow-y-auto">
                {summarizeIngredientSources(sourceDetails).map((source) => (
                  <li key={source.recipeId} className="flex flex-col gap-1 rounded-xl border border-brand-100 bg-paper px-4 py-3 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
                    <div className="min-w-0">
                      {source.available ? (
                        <Link className="font-semibold text-brand-800 underline decoration-brand-300 underline-offset-2 hover:text-brand-600" to={`/cong-thuc/id/${source.recipeId}`} onClick={closeDialog}>
                          {source.title ?? "Công thức"}
                        </Link>
                      ) : <span className="font-semibold text-ink-muted">{source.title ?? "Công thức không còn khả dụng"}</span>}
                      <p className="mt-1 text-xs font-medium text-ink-muted">
                        {source.servings != null ? `${source.servings.toLocaleString("vi-VN")} khẩu phần` : "Khẩu phần chưa xác định"}
                      </p>
                    </div>
                    <span className="shrink-0 text-sm font-medium text-ink-soft">
                      {source.quantities.map((quantity) => `${quantity.quantity.toLocaleString("vi-VN", { maximumFractionDigits: 3 })} ${quantity.unitCode ?? sourceDetails.unitCode}`).join(" · ")} lúc thêm
                    </span>
                  </li>
                ))}
              </ul>
            ) : (
              !sourceDetails.sources?.length && <p className="rounded-xl border border-brand-100 px-4 py-6 text-center text-sm text-ink-muted">Mặt hàng này được thêm thủ công.</p>
            )}
            {!!sourceDetails.sources?.some((source) => source.sourceKind === "UNKNOWN") && (
              <section className="space-y-2">
                <p className="text-sm font-semibold text-ink">Phần không còn gắn với công thức đầy đủ</p>
                <p className="text-xs text-ink-muted">Lượng này vẫn nằm trong danh sách và có thể giảm khi bạn chỉnh tổng số lượng.</p>
                <div className="flex items-center justify-between gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3">
                  <span className="text-sm font-semibold text-ink">Tổng lượng mất xác định</span>
                  <span className="shrink-0 text-sm font-semibold text-ink-soft">{unknownQuantityText(sourceDetails)}</span>
                </div>
              </section>
            )}
            {!!sourceDetails.sources?.filter((source) => source.sourceKind === "MANUAL").length && (
              <section className="space-y-2">
                <p className="text-sm font-semibold text-ink">Phần bạn đã thêm</p>
                {sourceDetails.sources.filter((source) => source.sourceKind === "MANUAL").map((source) => (
                  <div key={source.sourceId} className="flex items-center justify-between gap-3 rounded-xl border border-brand-100 bg-paper px-4 py-3">
                    <span className="text-sm text-ink">{source.originalQuantity.toLocaleString("vi-VN")} {source.originalUnitCode ?? sourceDetails.unitCode} · Thêm thủ công</span>
                    <div className="flex gap-2">
                      <Button size="sm" variant="outline" disabled={busy} onClick={() => { setSourceDetails(null); setManualSourceDraft(String(source.originalQuantity)); setManualSourceEditor({ item: sourceDetails, source }) }}>Sửa</Button>
                      <Button size="sm" variant="outline" disabled={busy} onClick={() => void mutate(async (listId) => {
                        await shoppingApi.removeManualSource(listId, source.sourceId)
                        const fresh = await shoppingApi.get(listId)
                        return () => fresh
                      }, "Đã xóa phần thêm thủ công.")}>Xóa phần thêm</Button>
                    </div>
                  </div>
                ))}
              </section>
            )}
            <div className="flex justify-end">
              <Button variant="outline" onClick={closeDialog}>Đóng</Button>
            </div>
          </div>
        )}
      </Modal>
      <Modal trapFocus open={manualSourceEditor !== null} onClose={closeDialog} title="Sửa phần đã thêm thủ công">
        {manualSourceEditor && (
          <form className="space-y-4" onSubmit={(event) => {
            event.preventDefault()
            const quantity = Number(manualSourceDraft)
            if (!Number.isFinite(quantity) || quantity <= 0) return
            setManualSourcePendingQuantity(quantity)
          }}>
            {manualSourcePendingQuantity == null ? (
              <label className="block text-sm font-semibold text-ink">Số lượng {manualSourceEditor.source.originalUnitCode}
                <input name="quantity" type="number" min="0.000001" step="0.000001" required value={manualSourceDraft}
                  onChange={(event) => setManualSourceDraft(event.target.value)}
                  className={`${field} mt-2 w-full`} />
              </label>
            ) : (
              <div className="space-y-2 rounded-xl border border-brand-100 bg-cream-soft p-4 text-sm text-ink" aria-live="polite">
                <h3 className="font-semibold">Xác nhận thay đổi số lượng</h3>
                {Math.abs(manualSourcePendingQuantity - manualSourceEditor.source.originalQuantity) >= 0.0000005 ? (
                  <p>
                    Bạn có muốn {manualSourcePendingQuantity > manualSourceEditor.source.originalQuantity ? "thêm" : "giảm"}{" "}
                    {quantityAmountText(manualSourcePendingQuantity - manualSourceEditor.source.originalQuantity, manualSourceEditor.source.originalUnitCode ?? manualSourceEditor.item.unitCode)}{" "}
                    <strong>{manualSourceEditor.item.name}</strong>{manualSourcePendingQuantity > manualSourceEditor.source.originalQuantity ? " vào" : " trong"} danh sách không?
                  </p>
                ) : (
                  <p>Số lượng <strong>{manualSourceEditor.item.name}</strong> không thay đổi. Bạn vẫn muốn xác nhận lưu chứ?</p>
                )}
                <p className="text-ink-muted">
                  {manualSourceEditor.source.originalQuantity.toLocaleString("vi-VN")} {manualSourceEditor.source.originalUnitCode}
                  <span className="px-2" aria-hidden="true">→</span>
                  <strong className="text-ink">{manualSourcePendingQuantity.toLocaleString("vi-VN")} {manualSourceEditor.source.originalUnitCode}</strong>
                </p>
              </div>
            )}
            {error && <p role="alert" className="text-sm text-ink">{error}</p>}
            <div className="flex justify-end gap-2">
              {manualSourcePendingQuantity == null ? (
                <>
                  <Button variant="outline" type="button" onClick={closeDialog}>Hủy</Button>
                  <Button type="submit" disabled={busy}>Tiếp tục</Button>
                </>
              ) : (
                <>
                  <Button variant="outline" type="button" disabled={busy} onClick={() => setManualSourcePendingQuantity(null)}>Quay lại</Button>
                  <Button type="button" disabled={busy} onClick={() => void mutate(async (listId) => {
                    const saved = await shoppingApi.updateManualSource(listId, manualSourceEditor.source.sourceId,
                      manualSourcePendingQuantity, manualSourceEditor.source.originalUnitId)
                    return (current) => ({ ...current, items: current.items.map((row) => row.id === saved.id ? saved : row) })
                  }, "Đã cập nhật phần thêm thủ công.")}>{busy ? "Đang lưu…" : "Xác nhận thay đổi"}</Button>
                </>
              )}
            </div>
          </form>
        )}
      </Modal>
      <Modal
        trapFocus
        open={nameEditor}
        onClose={closeDialog}
        title="Đổi tên danh sách"
      >
        <form
          onSubmit={(event) => {
            event.preventDefault()
            const name = nameDraft.replace(/\u00a0/g, " ").trim()
            if (!name) {
              setError("Tên danh sách không được để trống.")
              return
            }
            if (name.length > 200) {
              setError("Tên danh sách tối đa 200 ký tự.")
              return
            }
            void mutate(async (listId) => {
              const saved = await shoppingApi.renameList(listId, name)
              setLists((current) => current.map((summary) =>
                summary.id === saved.id ? saved : summary,
              ))
              return (current) => ({
                ...current,
                name: saved.name,
                updatedAt: saved.updatedAt,
              })
            }, "Đã đổi tên danh sách.")
          }}
          className="space-y-4"
        >
          <div>
            <label htmlFor="shopping-list-name" className="mb-1 block text-sm font-semibold text-ink">
              Tên danh sách
            </label>
            <input
              id="shopping-list-name"
              autoFocus
              maxLength={200}
              required
              value={nameDraft}
              disabled={busy}
              onChange={(event) => setNameDraft(event.target.value)}
              className={field + " w-full"}
            />
            <p className="mt-1 text-xs text-ink-muted">Tối đa 200 ký tự.</p>
          </div>
          {error && <p role="alert" className="text-sm text-ink">{error}</p>}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" disabled={busy} onClick={closeDialog}>Hủy</Button>
            <Button type="submit" disabled={busy}>
              {busy ? "Đang lưu…" : "Lưu tên"}
            </Button>
          </div>
        </form>
      </Modal>
      <Modal
        trapFocus
        open={emptyListEditor}
        onClose={closeDialog}
        title="Tạo danh sách mua sắm"
      >
        <form
          onSubmit={(event) => {
            event.preventDefault()
            const name = nameDraft.replace(/\u00a0/g, " ").trim()
            if (!name) {
              setError("Hãy nhập tên danh sách.")
              return
            }
            if (name.length > 200) {
              setError("Tên danh sách tối đa 200 ký tự.")
              return
            }
            void createEmptyList(name)
          }}
          className="space-y-4"
        >
          <div>
            <label htmlFor="new-shopping-list-name" className="mb-1 block text-sm font-semibold text-ink">
              Tên danh sách
            </label>
            <input
              id="new-shopping-list-name"
              autoFocus
              maxLength={200}
              required
              value={nameDraft}
              disabled={busy}
              onChange={(event) => setNameDraft(event.target.value)}
              placeholder="Ví dụ: Mua sắm cuối tuần"
              className={field + " w-full"}
            />
            <p className="mt-1 text-xs text-ink-muted">
              Danh sách được tạo rỗng, bạn có thể thêm mặt hàng bất cứ lúc nào.
            </p>
          </div>
          {error && <p role="alert" className="text-sm text-ink">{error}</p>}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" disabled={busy} onClick={closeDialog}>Hủy</Button>
            <Button type="submit" disabled={busy}>
              {busy ? "Đang tạo…" : "Tạo danh sách"}
            </Button>
          </div>
        </form>
      </Modal>
      <Modal
        trapFocus
        open={listRemoval}
        onClose={closeDialog}
        title="Xóa danh sách mua sắm?"
      >
        <p className="text-sm leading-6 text-ink-soft">
          Danh sách “{list?.name}” và {counts.all} mặt hàng trong checklist sẽ bị xóa.
          Lịch ăn và công thức nguồn vẫn được giữ nguyên.
        </p>
        {error && <p role="alert" className="mt-3 text-sm text-ink">{error}</p>}
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="outline" disabled={busy} onClick={closeDialog}>Hủy</Button>
          <Button
            disabled={busy}
            className="bg-red-700 hover:bg-red-800"
            onClick={() => void removeCurrentList()}
          >
            {busy ? "Đang xóa…" : "Xóa danh sách"}
          </Button>
        </div>
      </Modal>
      <Modal
        trapFocus
        open={editor !== null}
        onClose={closeDialog}
        title={editor === "new" ? "Thêm mặt hàng" : "Sửa số lượng nguyên liệu"}
      >
        {editor && (
          <ItemForm
            key={editor === "new" ? "new" : editor.id}
            item={editor === "new" ? null : editor}
            mode={editor === "new" ? "add" : "edit"}
            units={units}
            busy={busy}
            error={error}
            onCancel={closeDialog}
            onSave={(payload) => {
              if (editor === "new") {
                void mutate(async (listId) => {
                  const saved = await shoppingApi.add(listId, payload)
                  setLastChangedItemId(saved.id)
                  setLastChangedGroup(saved.ingredientGroup)
                  return (current) => ({ ...current, items: current.items.some((row) => row.id === saved.id)
                    ? current.items.map((row) => row.id === saved.id ? saved : row)
                    : [...current.items, saved] })
                }, "Đã thêm mặt hàng.")
                return
              }
              void mutate(async (listId) => {
                const saved = await shoppingApi.update(listId, editor.id, { quantity: payload.quantity, unitId: payload.unitId })
                setLastChangedItemId(saved.id)
                setLastChangedGroup(saved.ingredientGroup)
                return (current) => ({ ...current, items: current.items.map((row) => row.id === saved.id ? saved : row) })
              }, "Đã lưu số lượng và đơn vị.")
            }}
          />
        )}
      </Modal>
      <Modal open={removal !== null} onClose={closeDialog} title="Xác nhận xoá">
        <p className="text-sm text-ink-soft">
          {removal === "bought"
            ? `Xoá ${counts.done} mục đã mua? Các mục cần mua được giữ lại.`
            : `Xoá toàn bộ ${removal?.name ?? ""} khỏi danh sách, gồm lượng từ công thức và phần bạn đã thêm? Món công thức thiếu nguyên liệu này sẽ bị ẩn trong danh sách.`}
        </p>
        {error && (
          <p role="alert" className="mt-3 text-sm text-ink">
            {error}
          </p>
        )}
        <div ref={removalActions} className="mt-5 flex justify-end gap-2">
          <Button
            variant="outline"
            disabled={busy}
            onClick={closeDialog}
          >
            Hủy
          </Button>
          <Button
            disabled={busy}
            onClick={() => {
              const target = removal
              if (!target) return
              void mutate(async (listId) => {
                if (target === "bought") await shoppingApi.clearBought(listId)
                else await shoppingApi.remove(listId, target.id)
                const refreshed = await shoppingApi.get(listId)
                return () => refreshed
              }, "Đã xoá mục khỏi danh sách.")
            }}
          >
            {busy ? "Đang xoá…" : "Xoá"}
          </Button>
        </div>
      </Modal>
    </PageContainer>
  )
}

function ItemForm({
  item,
  mode,
  units,
  busy,
  error,
  onSave,
  onCancel,
}: {
  item: ShoppingItem | null
  mode: "add" | "edit"
  units: ShoppingUnit[]
  busy: boolean
  error: string
  onSave: (value: { name: string; quantity: number; unitId: number; ingredientId?: number }) => void
  onCancel: () => void
}) {
  type ItemFormValue = { name: string; quantity: number; unitId: number; ingredientId?: number }
  const [name, setName] = useState(item?.name ?? "")
  const [ingredientId, setIngredientId] = useState<number | null>(item?.ingredientId ?? null)
  const [matches, setMatches] = useState<PublicIngredient[]>([])
  const [searching, setSearching] = useState(false)
  const [searchError, setSearchError] = useState("")
  const [searchRetry, setSearchRetry] = useState(0)
  const [ingredientUnits, setIngredientUnits] = useState<PublicIngredientUnit[]>([])
  const [unitLoading, setUnitLoading] = useState(false)
  const [unitError, setUnitError] = useState("")
  const [unitRetry, setUnitRetry] = useState(0)
  const [quantity, setQuantity] = useState(String(mode === "edit" ? item?.quantity ?? 1 : 1))
  const [unit, setUnit] = useState(String(item?.unitId ?? units[0]?.id ?? ""))
  const [pendingSave, setPendingSave] = useState<ItemFormValue | null>(null)
  const nameInput = useRef<HTMLInputElement>(null)
  const quantityInput = useRef<HTMLInputElement>(null)
  const hasRequiredAmount = Boolean(item?.sources?.some((source) => source.sourceKind === "RECIPE"))
  const enteredQuantity = quantity.trim() === "" ? Number.NaN : Number(quantity)
  const belowRequiredMinimum = hasRequiredAmount
    && item?.requiredQuantity != null
    && Number.isFinite(enteredQuantity)
    && enteredQuantity < item.requiredQuantity
  const requiredMinimumText = item?.requiredQuantity?.toLocaleString("vi-VN", { maximumFractionDigits: 6 })
  useEffect(() => {
    const input = item === null ? nameInput.current : quantityInput.current
    input?.focus()
  }, [item])
  useEffect(() => {
    if (item || ingredientId || !name.trim()) {
      setMatches([])
      setSearching(false)
      setSearchError("")
      return
    }
    let active = true
    const timer = window.setTimeout(() => {
      setSearching(true)
      setSearchError("")
      void discoveryApi.searchShoppingIngredients(name.trim()).then((page) => {
        if (active) setMatches(page.content)
      }).catch((cause) => {
        if (active) setSearchError(asApiError(cause).message)
      }).finally(() => {
        if (active) setSearching(false)
      })
    }, 250)
    return () => { active = false; window.clearTimeout(timer) }
  }, [name, ingredientId, item, searchRetry])
  useEffect(() => {
    if (!ingredientId) {
      setIngredientUnits([])
      setUnitError("")
      return
    }
    let active = true
    setUnitLoading(true)
    setUnitError("")
    void discoveryApi.getIngredientUnits(ingredientId).then((result) => {
      if (active) setIngredientUnits(result)
    }).catch((cause) => {
      if (active) setUnitError(asApiError(cause).message)
    }).finally(() => {
      if (active) setUnitLoading(false)
    })
    return () => { active = false }
  }, [ingredientId, unitRetry])
  useEffect(() => {
    if (mode !== "add" || unitLoading || (ingredientId && ingredientUnits.length === 0)) return
    const supportedUnits = ingredientId ? ingredientUnits : units
    if (supportedUnits.length > 0 && !supportedUnits.some((option) => String(option.id) === unit)) {
      setUnit(String(supportedUnits[0].id))
    }
  }, [ingredientId, ingredientUnits, mode, unit, unitLoading, units])
  const availableUnits: ShoppingUnit[] = ingredientId
    ? ingredientUnits.map((option) => ({ id: option.id, code: option.code, name: option.name, active: true }))
    : units
  const pendingUnitCode = pendingSave
    ? availableUnits.find((option) => option.id === pendingSave.unitId)?.code ?? item?.unitCode ?? ""
    : ""
  const pendingQuantityDelta = item && pendingSave
    ? quantityDeltaInUnit(item.quantity, item.unitId, item.unitCode, pendingSave.quantity, pendingSave.unitId, pendingUnitCode, ingredientUnits)
    : null
  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        if (belowRequiredMinimum) return
        const nextValue = {
          name: name.trim(),
          quantity: Number(quantity),
          unitId: Number(unit),
          ...(ingredientId ? { ingredientId } : {}),
        }
        if (mode === "edit" && item && nextValue.quantity === item.quantity && nextValue.unitId === item.unitId) {
          onSave(nextValue)
          return
        }
        setPendingSave(nextValue)
      }}
      className="space-y-4"
      aria-busy={busy}
    >
      {pendingSave ? (
        <div className="space-y-4">
          <div className="space-y-2 rounded-xl border border-brand-100 bg-cream-soft p-4 text-sm text-ink" aria-live="polite">
            <h3 className="font-semibold">Xác nhận thay đổi danh sách</h3>
            {mode === "add" ? (
              <p>Bạn có muốn thêm {quantityAmountText(pendingSave.quantity, pendingUnitCode)} <strong>{pendingSave.name}</strong> vào danh sách không?</p>
            ) : item && pendingQuantityDelta != null && Math.abs(pendingQuantityDelta) >= 0.0000005 ? (
              <p>
                Bạn có muốn {pendingQuantityDelta > 0 ? "thêm" : "giảm"} {quantityAmountText(pendingQuantityDelta, pendingUnitCode)} <strong>{item.name}</strong>
                {pendingQuantityDelta > 0 ? " vào" : " trong"} danh sách không?
              </p>
            ) : item && pendingQuantityDelta != null ? (
              <p>Số lượng <strong>{item.name}</strong> không thay đổi. Bạn vẫn muốn xác nhận lưu chứ?</p>
            ) : item && pendingSave ? (
              <p>
                Bạn có muốn đổi lượng <strong>{item.name}</strong> từ {quantityText(item)} sang {quantityAmountText(pendingSave.quantity, pendingUnitCode)} không?
                <span className="mt-1 block text-ink-muted">Hai đơn vị này không có quy đổi để xác định chính xác phần tăng hoặc giảm.</span>
              </p>
            ) : null}
            {mode === "edit" && item && (
              <p className="text-ink-muted">
                {quantityText(item)} <span className="px-2" aria-hidden="true">→</span>
                <strong className="text-ink">{quantityAmountText(pendingSave.quantity, pendingUnitCode)}</strong>
              </p>
            )}
          </div>
          {error && <p role="alert" className="text-sm text-ink">{error}</p>}
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" disabled={busy} onClick={() => setPendingSave(null)}>Quay lại</Button>
            <Button type="button" disabled={busy} onClick={() => onSave(pendingSave)}>
              {busy ? "Đang lưu…" : mode === "add" ? "Xác nhận thêm" : "Xác nhận thay đổi"}
            </Button>
          </div>
        </div>
      ) : <>
      <fieldset disabled={busy} className="space-y-4">
        <label className="relative block text-sm text-ink">
          {mode === "add" ? "Tên mặt hàng" : "Nguyên liệu"}
          <input
            ref={nameInput}
            className={`${field} mt-2 w-full`}
            required
            maxLength={200}
            disabled={item !== null}
            value={name}
            onChange={(event) => { setName(event.target.value); setIngredientId(null) }}
          />
          {!item && !ingredientId && !!name.trim() && (
            <div className="absolute inset-x-0 top-full z-30 mt-1 max-h-48 overflow-y-auto rounded-lg border border-brand-200 bg-white shadow-lg">
              {searching ? <p className="px-3 py-2 text-xs text-ink-muted">Đang tìm trong danh mục…</p>
                : searchError ? <div className="px-3 py-2 text-xs text-red-700"><p>{searchError}</p><button type="button" className="mt-1 font-semibold underline" onClick={() => setSearchRetry((value) => value + 1)}>Thử lại</button></div>
                  : matches.length ? matches.map((match) => (
                    <button type="button" key={match.ingredientId} className="flex w-full items-center gap-2 px-3 py-2 text-left text-sm hover:bg-cream-soft focus-visible:bg-cream-soft focus-visible:outline-none"
                      onClick={() => { setName(match.name); setIngredientId(match.ingredientId); setUnit(""); setMatches([]) }}>
                      {match.imageUrl && <img src={match.imageUrl} alt="" className="h-8 w-8 rounded-md object-cover" />}
                      <span>{match.name}</span>
                    </button>
                  )) : <p className="px-3 py-2 text-xs text-ink-muted">Không có kết quả phù hợp trong danh mục.</p>}
            </div>
          )}
        </label>
        <div className="grid grid-cols-2 gap-3">
          <label className="text-sm text-ink">
            {mode === "add" ? "Số lượng" : "Tổng số lượng mới"}
            <input
              ref={quantityInput}
              className={`${field} mt-2 w-full`}
              type="number"
              required
              min={hasRequiredAmount && item?.requiredQuantity != null ? item.requiredQuantity : "0.01"}
              max="99999999.99"
              step="0.000001"
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
            />
            {belowRequiredMinimum && (
              <span role="alert" className="mt-2 block text-sm font-medium text-red-700 dark:text-red-300">
                Không thể thấp hơn {requiredMinimumText} {item?.requiredUnitCode ?? item?.unitCode}. Đây là lượng tối thiểu để hoàn thành công thức.
              </span>
            )}
          </label>
          <label className="text-sm text-ink">
            Đơn vị
            {hasRequiredAmount && item ? (
              <span className={`${field} mt-2 block w-full`}>{item.unitCode} — giữ nguyên đơn vị công thức</span>
            ) : (
              <select
                aria-label="Đơn vị"
                className={`${field} mt-2 w-full`}
                required
                disabled={busy || unitLoading || Boolean(unitError)}
                value={unit}
                onChange={(event) => setUnit(event.target.value)}
              >
                {item && !availableUnits.some((option) => option.id === item.unitId) && (
                  <option value={item.unitId}>
                    {item.unitCode} (đã ngừng dùng)
                  </option>
                )}
                {availableUnits.map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.code} — {option.name}
                  </option>
                ))}
              </select>
            )}
          </label>
        </div>
      </fieldset>
      {unitLoading && <p role="status" className="text-xs text-ink-muted">Đang tải đơn vị hỗ trợ…</p>}
      {unitError && <div className="flex items-center justify-between gap-3 text-xs text-red-700"><span>{unitError}</span><Button size="sm" variant="outline" type="button" onClick={() => setUnitRetry((value) => value + 1)}>Thử lại</Button></div>}
      {error && (
        <p role="alert" className="text-sm text-ink">
          {error}
        </p>
      )}
      <div className="flex justify-end gap-2">
        <Button
          type="button"
          variant="outline"
          disabled={busy}
          onClick={onCancel}
        >
          Hủy
        </Button>
        <Button type="submit" disabled={busy || !name.trim() || !unit || unitLoading || Boolean(unitError) || belowRequiredMinimum || (hasRequiredAmount && item?.requiredQuantity == null)}>
          Tiếp tục
        </Button>
      </div>
      </>}
    </form>
  )
}
