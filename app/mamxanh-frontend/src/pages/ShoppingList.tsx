import { useEffect, useRef, useState } from "react"
import { Link, useLocation, useSearchParams } from "react-router-dom"
import {
  CalendarDays,
  MoreHorizontal,
  MoreVertical,
  Pencil,
  Plus,
  ShoppingBasket,
  Trash2,
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

type Tab = "all" | "todo" | "done"
const SHOPPING_LISTS_PER_PAGE = 20
const field =
  "min-w-0 rounded-lg border border-brand-200 bg-cream-soft px-3 py-2 text-sm text-ink focus-visible:outline-2 focus-visible:outline-brand-400"
const quantityText = (item: ShoppingItem) =>
  `${item.quantity.toLocaleString("vi-VN", { maximumFractionDigits: 2 })} ${item.unitCode}`
const normalizeItemName = (name: string) => name.replace(/\u00a0/g, " ").trim().replace(/\s+/g, " ").toLocaleLowerCase("vi")

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
  const [units, setUnits] = useState<ShoppingUnit[]>([])
  const [loading, setLoading] = useState(false)
  const [loadError, setLoadError] = useState("")
  const [retry, setRetry] = useState(0)
  const [tab, setTab] = useState<Tab>("all")
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")
  const [notice, setNotice] = useState("")
  const [editor, setEditor] = useState<ShoppingItem | "new" | null>(null)
  const [removal, setRemoval] = useState<ShoppingItem | "bought" | null>(null)
  const [nameEditor, setNameEditor] = useState(false)
  const [emptyListEditor, setEmptyListEditor] = useState(false)
  const [nameDraft, setNameDraft] = useState("")
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
    setLoadError("")
    setError("")
    setNotice("")
    setEditor(null)
    setRemoval(null)
    setNameEditor(false)
    setEmptyListEditor(false)
    setListRemoval(false)
    setTab("all")
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
  const groupedEntries = Object.entries(grouped) as [string, ShoppingItem[]][]
  const groupColumns: [Array<[string, ShoppingItem[]]>, Array<[string, ShoppingItem[]]>] = [[], []]
  const groupColumnWeights = [0, 0]
  groupedEntries.forEach((entry) => {
    const columnIndex = groupColumnWeights[0] <= groupColumnWeights[1] ? 0 : 1
    const estimatedHeight = 0.4 + entry[1].reduce((height, item) => height + 1 + (item.sources?.length ?? 0) * 0.35, 0)
    groupColumns[columnIndex].push(entry)
    groupColumnWeights[columnIndex] += estimatedHeight
  })
  const creation = (location.state as {
    shoppingCreation?: ShoppingCreation
  } | null)?.shoppingCreation
  const diagnostics = creation?.shoppingList.id === list?.id ? creation : null
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
  ) {
    if (!id || pending.current) return
    const target = id
    pending.current = true
    setBusy(true)
    setError("")
    setNotice("")
    try {
      const update = await action(target)
      if (!alive.current || selectedId.current !== target) return
      setList((current) => (current?.id === target ? update(current) : current))
      setNotice(message)
      pending.current = false
      closeDialog()
    } catch (cause) {
      if (alive.current && selectedId.current === target)
        setError(asApiError(cause).message)
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
      <h3 className="mb-2 text-sm font-semibold text-ink">{group}</h3>
      <div className="divide-y divide-brand-100">
        {entries.map((item) => (
          <div
            key={item.id}
            data-testid={`shopping-item-${item.id}`}
            className={`grid grid-cols-[minmax(0,1fr)_auto_auto_auto] items-center gap-1 rounded-lg px-1 py-3 sm:gap-3 ${item.bought ? "bg-cream-soft/50" : ""}`}
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
                <span className="flex min-w-0 items-center gap-3">
                  <IngredientImage
                    imageUrl={item.imageUrl ?? null}
                    name={item.name}
                    containerClassName="h-12 w-12 shrink-0 overflow-hidden rounded-lg border border-brand-100 bg-cream-soft"
                  />
                  <span className="break-words text-sm text-ink">{item.name}</span>
                </span>
              }
              boxSize={22}
              color="#f97316"
              fillColor="#f97316"
              className="min-w-0 w-full"
            />
            <span className={`text-sm ${item.bought ? "text-ink-muted line-through" : "text-ink-soft"}`}>
              {quantityText(item)}
            </span>
            <Button
              variant="outline"
              size="sm"
              className="!h-9 !w-9 !p-0"
              aria-label={`Sửa ${item.name} ${item.unitCode}`}
              title={`Sửa ${item.name}`}
              disabled={busy}
              onClick={() => openDialog(() => setEditor(item))}
            >
              <Pencil className="h-4 w-4" />
            </Button>
            <Button
              variant="outline"
              size="sm"
              className="!h-9 !w-9 !p-0"
              aria-label={`Xoá ${item.name} ${item.unitCode}`}
              title={`Xóa ${item.name}`}
              disabled={busy}
              onClick={() => openDialog(() => setRemoval(item))}
            >
              <Trash2 className="h-4 w-4" />
            </Button>
            {separate(item) && (
              <p className="col-span-4 pl-8 text-xs text-ink-muted">
                Giữ dòng riêng theo đơn vị {item.unitCode}; không tự cộng với đơn vị khác.
              </p>
            )}
            {!!item.sources?.length && (
              <div className="col-span-4 min-w-0 space-y-1 break-words pl-8 text-xs text-ink-muted" aria-label={`Nguồn của ${item.name}`}>
                {item.sources.map((source, index) => (
                  <p key={`${source.recipeId}-${index}`}>
                    {source.available ? (
                      <Link className="text-orange-600 underline" to={`/cong-thuc/id/${source.recipeId}`}>
                        {source.title}
                      </Link>
                    ) : <span>Công thức không còn khả dụng</span>}
                    {" · "}{source.originalQuantity.toLocaleString("vi-VN")} {source.originalUnitCode} lúc thêm
                  </p>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </section>
  )
  return (
    <PageContainer className="py-6 lg:[zoom:0.88]">
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
                    ? "border-transparent bg-brand-50 text-ink"
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
                  <span className={`block break-words leading-5 ${summary.id === id ? "font-semibold" : "font-medium"}`}>{summary.name}</span>
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
              <header className="mb-5 flex flex-wrap items-center justify-between gap-3">
                <div className="min-w-0">
                  <h2 className="break-words text-lg font-semibold text-ink">
                    {list.name}
                  </h2>
                </div>
              </header>
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
                    ["all", "Tất cả"],
                    ["todo", "Cần mua"],
                    ["done", "Đã mua"],
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
                <div>
                  <div className="space-y-5 lg:hidden">
                    {groupedEntries.map(renderGroup)}
                  </div>
                  <div className="hidden gap-x-6 lg:grid lg:grid-cols-2">
                    {groupColumns.map((column, index) => (
                      <div key={index} className="space-y-5">{column.map(renderGroup)}</div>
                    ))}
                  </div>
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
        title={editor === "new" ? "Thêm mặt hàng" : "Sửa số lượng và đơn vị"}
      >
        {editor && (
          <ItemForm
            key={editor === "new" ? "new" : editor.id}
            item={editor === "new" ? null : editor}
            units={units}
            busy={busy}
            error={error}
            onCancel={closeDialog}
            onSave={(payload) => {
              if (editor === "new") {
                const duplicate = items.find((row) => normalizeItemName(row.name) === normalizeItemName(payload.name))
                if (duplicate) {
                  setError(`${duplicate.name} đã có trong danh sách (${quantityText(duplicate)}).`)
                  return
                }
                void mutate(async (listId) => {
                  const saved = await shoppingApi.add(listId, payload)
                  return (current) => ({ ...current, items: [...current.items, saved] })
                }, "Đã thêm mặt hàng.")
                return
              }
              void mutate(async (listId) => {
                const saved = await shoppingApi.update(listId, editor.id, { quantity: payload.quantity, unitId: payload.unitId })
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
            : `Xoá ${removal?.name ?? ""} khỏi danh sách?`}
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
                return (current) => ({
                  ...current,
                  items: current.items.filter((item) =>
                    target === "bought" ? !item.bought : item.id !== target.id,
                  ),
                })
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
  units,
  busy,
  error,
  onSave,
  onCancel,
}: {
  item: ShoppingItem | null
  units: ShoppingUnit[]
  busy: boolean
  error: string
  onSave: (value: { name: string; quantity: number; unitId: number }) => void
  onCancel: () => void
}) {
  const [name, setName] = useState(item?.name ?? "")
  const [quantity, setQuantity] = useState(String(item?.quantity ?? 1))
  const [unit, setUnit] = useState(String(item?.unitId ?? units[0]?.id ?? ""))
  const nameInput = useRef<HTMLInputElement>(null)
  const quantityInput = useRef<HTMLInputElement>(null)
  useEffect(() => {
    const input = item === null ? nameInput.current : quantityInput.current
    input?.focus()
  }, [item])
  return (
    <form
      onSubmit={(event) => {
        event.preventDefault()
        onSave({
          name: name.trim(),
          quantity: Number(quantity),
          unitId: Number(unit),
        })
      }}
      className="space-y-4"
      aria-busy={busy}
    >
      <fieldset disabled={busy} className="space-y-4">
        <label className="block text-sm text-ink">
          Tên mặt hàng
          <input
            ref={nameInput}
            className={`${field} mt-2 w-full`}
            required
            maxLength={200}
            disabled={item !== null}
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
        </label>
        <div className="grid grid-cols-2 gap-3">
          <label className="text-sm text-ink">
            Số lượng
            <input
              ref={quantityInput}
              className={`${field} mt-2 w-full`}
              type="number"
              required
              min="0.01"
              max="99999999.99"
              step="0.01"
              value={quantity}
              onChange={(event) => setQuantity(event.target.value)}
            />
          </label>
          <label className="text-sm text-ink">
            Đơn vị
            <select
              aria-label="Đơn vị"
              className={`${field} mt-2 w-full`}
              required
              value={unit}
              onChange={(event) => setUnit(event.target.value)}
            >
              {item && !units.some((option) => option.id === item.unitId) && (
                <option value={item.unitId}>
                  {item.unitCode} (đã ngừng dùng)
                </option>
              )}
              {units.map((option) => (
                <option key={option.id} value={option.id}>
                  {option.code} — {option.name}
                </option>
              ))}
            </select>
          </label>
        </div>
      </fieldset>
      <p className="text-xs text-ink-muted">Chọn đơn vị rồi nhập số lượng tương ứng.</p>
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
        <Button type="submit" disabled={busy || !name.trim() || !unit}>
          {busy ? "Đang lưu…" : "Lưu mặt hàng"}
        </Button>
      </div>
    </form>
  )
}
