import { useEffect, useRef, useState } from "react"
import { Link, useLocation, useSearchParams } from "react-router-dom"
import {
  ListChecks,
  Pencil,
  Plus,
  ShoppingBasket,
  Trash2,
} from "lucide-react"
import { PageContainer } from "../components/Layout"
import { Button, Card } from "../components/ui"
import { Modal } from "../components/Modal"
import SpringCheck from "../components/SpringCheck"
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
  const grouped = items
    .filter(
      (item) => tab === "all" || (tab === "done" ? item.bought : !item.bought),
    )
    .reduce<Record<string, ShoppingItem[]>>((groups, item) => {
      ;(groups[item.ingredientGroup] ??= []).push(item)
      return groups
    }, {})
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
            Đi chợ
          </h1>
          <p className="text-xs leading-5 text-ink-muted">
            Danh sách được lưu riêng trong tài khoản của bạn.
          </p>
          <Link
            to="/ke-hoach"
            className="block text-sm font-semibold text-brand-600"
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
                className={`rounded-lg px-3 py-2 text-left text-sm focus-visible:outline-2 focus-visible:outline-brand-400 ${
                  summary.id === id
                    ? "bg-paper font-semibold text-brand-600"
                    : "text-ink-muted hover:bg-paper"
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
                <p role="status" className="mb-4 text-sm text-ink-muted">Đang lưu danh sách…</p>
              )}
              {error && !editor && !removal && (
                <p role="alert" className="mb-4 text-sm text-ink">
                  {error}
                </p>
              )}
              <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                <nav
                  aria-label="Lọc danh sách đi chợ"
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
                          ? "bg-raised font-semibold text-brand-600"
                          : "text-ink-muted"
                      }`}
                    >
                      {label} ({counts[value]})
                    </button>
                  ))}
                </nav>
                <Button
                  size="sm"
                  disabled={busy || units.length === 0}
                  onClick={() => openDialog(() => setEditor("new"))}
                >
                  <Plus className="h-4 w-4" />
                  Thêm mặt hàng
                </Button>
              </div>
              {Object.entries(grouped).length ? (
                <div className="space-y-6">
                  {Object.entries(grouped).map(([group, entries]) => (
                    <section key={group}>
                      <h3 className="mb-2 text-sm font-semibold text-ink">
                        {group}
                      </h3>
                      <div className="divide-y divide-brand-100">
                        {entries.map((item) => (
                          <div
                            key={item.id}
                            data-testid={`shopping-item-${item.id}`}
                            className={`grid grid-cols-[minmax(0,1fr)_auto_auto_auto] items-center gap-1 rounded-lg px-1 py-3 sm:gap-3 ${
                              item.bought ? "bg-cream-soft/50" : ""
                            }`}
                          >
                            <SpringCheck
                              checked={item.bought}
                              disabled={busy}
                              onChange={(bought) =>
                                void mutate(
                                  async (listId) => {
                                    const saved = await shoppingApi.bought(
                                      listId,
                                      item.id,
                                      bought,
                                    )
                                    return (current) => ({
                                      ...current,
                                      items: current.items.map((row) =>
                                        row.id === saved.id ? saved : row,
                                      ),
                                    })
                                  },
                                  bought
                                    ? "Đã đánh dấu đã mua."
                                    : "Đã đánh dấu cần mua.",
                                )
                              }
                              ariaLabel={`Đánh dấu ${item.name} ${item.unitCode}`}
                              label={
                                <span className="break-words text-sm text-ink">
                                  {item.name}
                                </span>
                              }
                              boxSize={22}
                              className="min-w-0"
                            />
                            <span
                              className={`text-sm ${
                                item.bought
                                  ? "text-ink-muted line-through"
                                  : "text-ink-soft"
                              }`}
                            >
                              {quantityText(item)}
                            </span>
                            <Button
                              variant="ghost"
                              size="sm"
                              aria-label={`Sửa ${item.name} ${item.unitCode}`}
                              disabled={busy}
                              onClick={() => openDialog(() => setEditor(item))}
                            >
                              <Pencil className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              aria-label={`Xoá ${item.name} ${item.unitCode}`}
                              disabled={busy}
                              onClick={() => openDialog(() => setRemoval(item))}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                            {separate(item) && (
                              <p className="col-span-4 pl-8 text-xs text-ink-muted">
                                Giữ dòng riêng theo đơn vị {item.unitCode};
                                không tự cộng với đơn vị khác.
                              </p>
                            )}
                            {!!item.sources?.length && (
                              <div
                                className="col-span-4 min-w-0 space-y-1 break-words pl-8 text-xs text-ink-muted"
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
                                    {source.originalQuantity.toLocaleString(
                                      "vi-VN",
                                    )}{" "}
                                    {source.originalUnitCode} lúc thêm
                                  </p>
                                ))}
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    </section>
                  ))}
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
                  const saved =
                    editor === "new"
                      ? await shoppingApi.add(listId, payload)
                      : await shoppingApi.update(listId, editor.id, {
                          quantity: payload.quantity,
                          unitId: payload.unitId,
                        })
                  return (current) => ({
                    ...current,
                    items:
                      editor === "new"
                        ? [...current.items, saved]
                        : current.items.map((row) =>
                            row.id === saved.id ? saved : row,
                          ),
                  })
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
