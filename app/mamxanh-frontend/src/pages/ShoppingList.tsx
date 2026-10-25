import { useEffect, useRef, useState } from "react"
import { Link, useLocation, useSearchParams } from "react-router-dom"
import {
  ListChecks,
  Minus,
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
const field =
  "min-w-0 rounded-lg border border-brand-200 bg-cream-soft px-3 py-2 text-sm text-ink focus-visible:outline-2 focus-visible:outline-brand-400"
const quantityText = (item: ShoppingItem) =>
  `${item.quantity.toLocaleString("vi-VN", { maximumFractionDigits: 2 })} ${item.unitCode}`

function isSpiceItem(item: ShoppingItem): boolean {
  const group = (item.ingredientGroup || "").toLowerCase()
  if (group.includes("gia vị")) return true
  const name = (item.name || "").toLowerCase()
  const spiceKeywords = [
    "muối",
    "đường",
    "tiêu",
    "hạt nêm",
    "nước mắm",
    "nước tương",
    "dầu ăn",
    "dầu mè",
    "dầu hào",
    "tương",
    "giấm",
    "ớt bột",
    "ngũ vị hương",
    "bột cà ri",
    "bột nghệ",
    "bột năng",
    "bột bắp",
    "bột quế",
    "thảo quả",
    "hoa hồi",
    "sa tế",
    "chao",
    "mắm ruốc chay",
  ]
  return spiceKeywords.some((keyword) => name.includes(keyword))
}

export function ShoppingList() {
  const { account } = useAuth()
  if (!account)
    return (
      <PageContainer className="py-12">
        <Card className="mx-auto max-w-xl p-6">
          <h1 className="text-xl font-semibold text-ink">
            Danh sách đi chợ của bạn
          </h1>
          <p className="my-4 text-sm text-ink-muted">
            Vui lòng đăng nhập để tạo và lưu danh sách đi chợ theo tài khoản.
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
          Tài khoản Quản trị viên không có quyền quản lý danh sách đi chợ của
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
    shoppingApi
      .list(page)
      .then((value) => {
        if (!active) return
        setLists(value.content)
        setTotalPages(value.totalPages)
        if (!requested && value.content.length)
          setParams({ listId: String(value.content[0].id) }, { replace: true })
      })
      .catch((cause) => {
        if (active) setListError(asApiError(cause).message)
      })
      .finally(() => {
        if (active) setListLoading(false)
      })
    return () => {
      active = false
    }
  }, [page, listRetry, requested, setParams])

  useEffect(() => {
    let active = true
    setList(null)
    setLoadError("")
    setError("")
    setNotice("")
    setEditor(null)
    setRemoval(null)
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
  const counts = {
    all: items.length,
    todo: items.filter((item) => !item.bought).length,
    done: items.filter((item) => item.bought).length,
  }

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
    setError("")
    requestAnimationFrame(() => {
      if (dialogTrigger.current?.isConnected) dialogTrigger.current.focus()
      else document.querySelector<HTMLElement>("#shopping-title")?.focus()
    })
  }

  function openDialog(action: () => void) {
    dialogTrigger.current = document.activeElement as HTMLElement
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

  async function handleStepQuantity(item: ShoppingItem, delta: number) {
    const currentQuantity = Number(item.quantity)
    const nextQuantity = Math.max(0.1, Number((currentQuantity + delta).toFixed(2)))
    if (nextQuantity === currentQuantity) return
    await mutate(
      async (listId) => {
        const saved = await shoppingApi.update(listId, item.id, {
          quantity: nextQuantity,
          unitId: item.unitId,
        })
        return (current) => ({
          ...current,
          items: current.items.map((row) => (row.id === saved.id ? saved : row)),
        })
      },
      delta > 0
        ? `Đã tăng số lượng ${item.name} lên ${nextQuantity} ${item.unitCode}.`
        : `Đã giảm số lượng ${item.name} xuống ${nextQuantity} ${item.unitCode}.`,
    )
  }

  // Filter and sort items: unbought items come first, bought items move to bottom!
  const filteredItems = items.filter(
    (item) => tab === "all" || (tab === "done" ? item.bought : !item.bought),
  )
  const sortItems = (arr: ShoppingItem[]) =>
    [...arr].sort((a, b) => Number(a.bought) - Number(b.bought))

  const mainIngredients = sortItems(filteredItems.filter((item) => !isSpiceItem(item)))
  const spiceIngredients = sortItems(filteredItems.filter((item) => isSpiceItem(item)))

  const renderItemRow = (item: ShoppingItem) => (
    <div
      key={item.id}
      data-testid={`shopping-item-${item.id}`}
      className={`grid grid-cols-[minmax(0,1fr)_auto_auto] items-center gap-2 rounded-xl p-2.5 transition-all sm:gap-3 ${
        item.bought
          ? "bg-cream-soft/40 opacity-75"
          : "bg-paper hover:bg-cream-soft/30"
      }`}
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
                items: current.items.map((row) =>
                  row.id === saved.id ? saved : row,
                ),
              })
            },
            bought ? "Đã đánh dấu đã mua." : "Đã đánh dấu cần mua.",
          )
        }
        ariaLabel={`Đánh dấu ${item.name} ${item.unitCode}`}
        label={
          <span
            className={`break-words text-sm font-medium ${
              item.bought ? "text-ink-muted line-through" : "text-ink"
            }`}
          >
            {item.name}
          </span>
        }
        boxSize={22}
        className="min-w-0"
      />

      {/* Inline Stepper Controls */}
      <div className="flex items-center gap-1">
        <button
          type="button"
          aria-label={`Giảm số lượng ${item.name}`}
          disabled={busy || item.bought || item.quantity <= 1}
          onClick={() => void handleStepQuantity(item, -1)}
          className="inline-flex h-7 w-7 items-center justify-center rounded-lg border border-brand-200 bg-paper text-ink-muted transition hover:border-brand-400 hover:text-ink hover:bg-brand-50 disabled:opacity-30 disabled:pointer-events-none"
        >
          <Minus className="h-3 w-3" />
        </button>
        <span
          className={`min-w-14 px-1 text-center text-xs font-semibold sm:text-sm ${
            item.bought ? "text-ink-muted line-through" : "text-ink-soft"
          }`}
        >
          {quantityText(item)}
        </span>
        <button
          type="button"
          aria-label={`Tăng số lượng ${item.name}`}
          disabled={busy || item.bought}
          onClick={() => void handleStepQuantity(item, 1)}
          className="inline-flex h-7 w-7 items-center justify-center rounded-lg border border-brand-200 bg-paper text-ink-muted transition hover:border-brand-400 hover:text-ink hover:bg-brand-50 disabled:opacity-30 disabled:pointer-events-none"
        >
          <Plus className="h-3 w-3" />
        </button>
      </div>

      {/* Row Actions: Sửa & Xoá */}
      <div className="flex items-center gap-1">
        <Button
          variant="ghost"
          size="sm"
          aria-label={`Sửa ${item.name} ${item.unitCode}`}
          disabled={busy}
          onClick={() => openDialog(() => setEditor(item))}
          className="h-8 w-8 p-0 text-ink-muted hover:text-brand-700 hover:bg-brand-50"
        >
          <Pencil className="h-3.5 w-3.5" />
        </Button>
        <Button
          variant="ghost"
          size="sm"
          aria-label={`Xoá ${item.name} ${item.unitCode}`}
          disabled={busy}
          onClick={() => openDialog(() => setRemoval(item))}
          className="h-8 w-8 p-0 text-ink-muted hover:text-red-700 hover:bg-red-50"
        >
          <Trash2 className="h-3.5 w-3.5" />
        </Button>
      </div>

      {separate(item) && (
        <p className="col-span-3 pl-8 text-xs text-ink-muted">
          Giữ dòng riêng theo đơn vị {item.unitCode}; không tự cộng với đơn vị khác.
        </p>
      )}
      {!!item.sources?.length && (
        <div
          className="col-span-3 min-w-0 space-y-1 break-words pl-8 text-xs text-ink-muted"
          aria-label={`Nguồn của ${item.name}`}
        >
          {item.sources.map((source, index) => (
            <p key={`${source.recipeId}-${index}`}>
              {source.available ? (
                <Link
                  className="text-brand-700 underline"
                  to={`/cong-thuc/id/${source.recipeId}`}
                >
                  {source.title}
                </Link>
              ) : (
                <span>Công thức không còn khả dụng</span>
              )}
              {" · "}
              {source.originalQuantity.toLocaleString("vi-VN")}{" "}
              {source.originalUnitCode} lúc thêm
            </p>
          ))}
        </div>
      )}
    </div>
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
            <ShoppingBasket className="h-5 w-5 text-brand-600" />
            Danh sách mua sắm
          </h1>
          <p className="text-xs leading-5 text-ink-muted">
            Mua sắm & Chuẩn bị nguyên liệu riêng theo tài khoản của bạn.
          </p>
          <Link
            to="/ke-hoach"
            className="block text-sm font-semibold text-brand-600 hover:underline"
          >
            Tạo từ Lịch ăn →
          </Link>
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
            {lists.map((summary) => (
              <button
                key={summary.id}
                type="button"
                disabled={busy}
                aria-current={summary.id === id ? "page" : undefined}
                onClick={() => setParams({ listId: String(summary.id) })}
                className={`rounded-xl px-3 py-2 text-left text-sm transition-all focus-visible:outline-2 focus-visible:outline-brand-400 ${
                  summary.id === id
                    ? "bg-paper font-semibold text-brand-700 shadow-xs border border-brand-200"
                    : "text-ink-muted hover:bg-paper hover:text-ink"
                }`}
              >
                <span className="block break-words">{summary.name}</span>
                <span className="mt-1 block text-xs text-ink-muted">
                  Mã {summary.id}
                </span>
              </button>
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
          <p className="hidden text-xs leading-5 text-ink-muted md:block">
            <ListChecks className="mb-2 h-5 w-5 text-brand-600" />
            Chạm để đánh dấu món đã mua. Lượng nguyên liệu đã lưu giữ nguyên khi
            công thức thay đổi.
          </p>
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
                  Chưa có danh sách đi chợ
                </h2>
                <p className="mt-2 text-sm text-ink-muted">
                  Thêm món vào Lịch ăn, rồi chọn cả tuần hoặc các ngày để tạo
                  danh sách.
                </p>
                <Link
                  to="/ke-hoach"
                  className="mt-4 inline-block font-semibold text-brand-600"
                >
                  Mở Lịch ăn
                </Link>
              </div>
            )
          ) : (
            <div aria-busy={busy}>
              <header className="mb-5 flex flex-wrap items-center justify-between gap-3">
                <div className="min-w-0">
                  <h2 className="break-words text-lg font-semibold text-ink">
                    {list.name}
                  </h2>
                  <p className="mt-1 text-xs text-ink-muted">
                    {counts.todo} cần mua · {counts.done} đã mua
                  </p>
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
                <p role="status" className="mb-4 text-sm text-brand-700">
                  {notice}
                </p>
              )}

              {busy && !editor && !removal && (
                <p role="status" className="mb-4 text-sm text-ink-muted">
                  Đang lưu danh sách…
                </p>
              )}

              {error && !editor && !removal && (
                <p role="alert" className="mb-4 text-sm text-ink">
                  {error}
                </p>
              )}

              <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
                <nav
                  aria-label="Lọc danh sách đi chợ"
                  className="flex flex-wrap gap-1.5 rounded-xl bg-raised/70 p-1"
                >
                  {(
                    [
                      ["all", "Tất cả"],
                      ["todo", "Cần mua"],
                      ["done", "Đã mua"],
                    ] as const
                  ).map(([value, label]) => (
                    <button
                      key={value}
                      type="button"
                      aria-pressed={tab === value}
                      onClick={() => setTab(value)}
                      className={`rounded-lg px-3 py-1.5 text-xs font-medium transition-all focus-visible:outline-2 focus-visible:outline-brand-400 sm:text-sm ${
                        tab === value
                          ? "bg-paper font-bold text-brand-700 shadow-xs"
                          : "text-ink-muted hover:text-ink"
                      }`}
                    >
                      {label} ({counts[value]})
                    </button>
                  ))}
                </nav>

                <div className="flex flex-wrap items-center gap-2">
                  <ShoppingListExport
                    key={list.id}
                    items={list.items}
                    listName={list.name}
                    disabled={busy}
                    onNotice={setNotice}
                  />
                  <Button
                    variant="primary"
                    size="sm"
                    disabled={busy || units.length === 0}
                    onClick={() => openDialog(() => setEditor("new"))}
                  >
                    <Plus className="h-4 w-4" />
                    Thêm mặt hàng
                  </Button>
                </div>
              </div>

              {filteredItems.length > 0 ? (
                <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
                  {/* Cột 1: Nguyên liệu chính */}
                  <section className="rounded-2xl border border-brand-100 bg-paper/80 p-4 shadow-xs">
                    <div className="mb-3.5 flex items-center justify-between border-b border-brand-100 pb-2.5">
                      <div className="flex items-center gap-2">
                        <span
                          className="flex h-2.5 w-2.5 rounded-full bg-brand-500"
                          aria-hidden="true"
                        />
                        <h3 className="text-sm font-bold text-ink">
                          Nguyên liệu chính
                        </h3>
                      </div>
                      <span className="rounded-full bg-brand-50 px-2.5 py-0.5 text-xs font-semibold text-brand-700">
                        {mainIngredients.length} món
                      </span>
                    </div>
                    {mainIngredients.length > 0 ? (
                      <div className="space-y-2">
                        {mainIngredients.map((item) => renderItemRow(item))}
                      </div>
                    ) : (
                      <p className="py-8 text-center text-xs text-ink-muted">
                        Không có nguyên liệu chính trong mục này.
                      </p>
                    )}
                  </section>

                  {/* Cột 2: Gia vị & Phụ liệu */}
                  <section className="rounded-2xl border border-amber-100 bg-paper/80 p-4 shadow-xs">
                    <div className="mb-3.5 flex items-center justify-between border-b border-amber-100 pb-2.5">
                      <div className="flex items-center gap-2">
                        <span
                          className="flex h-2.5 w-2.5 rounded-full bg-amber-500"
                          aria-hidden="true"
                        />
                        <h3 className="text-sm font-bold text-ink">
                          Gia vị & Phụ liệu
                        </h3>
                      </div>
                      <span className="rounded-full bg-amber-50 px-2.5 py-0.5 text-xs font-semibold text-amber-800">
                        {spiceIngredients.length} món
                      </span>
                    </div>
                    {spiceIngredients.length > 0 ? (
                      <div className="space-y-2">
                        {spiceIngredients.map((item) => renderItemRow(item))}
                      </div>
                    ) : (
                      <p className="py-8 text-center text-xs text-ink-muted">
                        Không có gia vị nào trong mục này.
                      </p>
                    )}
                  </section>
                </div>
              ) : (
                <p className="py-12 text-center text-sm text-ink-muted">
                  Không có nguyên liệu trong mục này.
                </p>
              )}

              {counts.done > 0 && (
                <div className="mt-5 flex justify-end border-t border-brand-100 pt-4">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={busy}
                    onClick={() => openDialog(() => setRemoval("bought"))}
                    className="border-red-200 text-red-700 hover:bg-red-50 hover:border-red-300"
                  >
                    <Trash2 className="h-4 w-4" />
                    Xoá {counts.done} mục đã mua
                  </Button>
                </div>
              )}
            </div>
          )}
        </Card>
      </div>

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
            onSave={(payload) =>
              void mutate(
                async (listId) => {
                  if (editor === "new") {
                    const normalizedName = payload.name.trim().toLowerCase()
                    const existing = list?.items.find(
                      (it) =>
                        it.name.trim().toLowerCase() === normalizedName &&
                        it.unitId === payload.unitId,
                    )
                    if (existing) {
                      const combinedQuantity = Number(
                        (existing.quantity + payload.quantity).toFixed(2),
                      )
                      const saved = await shoppingApi.update(
                        listId,
                        existing.id,
                        {
                          quantity: combinedQuantity,
                          unitId: existing.unitId,
                        },
                      )
                      return (current) => ({
                        ...current,
                        items: current.items.map((row) =>
                          row.id === saved.id ? saved : row,
                        ),
                      })
                    }
                    const saved = await shoppingApi.add(listId, payload)
                    return (current) => ({
                      ...current,
                      items: [...current.items, saved],
                    })
                  } else {
                    const saved = await shoppingApi.update(listId, editor.id, {
                      quantity: payload.quantity,
                      unitId: payload.unitId,
                    })
                    return (current) => ({
                      ...current,
                      items: current.items.map((row) =>
                        row.id === saved.id ? saved : row,
                      ),
                    })
                  }
                },
                editor === "new"
                  ? "Đã thêm mặt hàng."
                  : "Đã lưu số lượng và đơn vị.",
              )
            }
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
      <p className="text-xs text-ink-muted">
        {item === null
          ? "Nếu trùng tên và đơn vị với mặt hàng đã có, số lượng sẽ được tự động cộng dồn."
          : "Chọn đơn vị rồi nhập số lượng tương ứng."}
      </p>
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
