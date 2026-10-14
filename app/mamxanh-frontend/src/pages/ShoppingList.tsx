import { useMemo, useState } from "react"
import {
  ClipboardCopy,
  Download,
  ListChecks,
  Plus,
  ShoppingBasket,
  X,
} from "lucide-react"
import { PageContainer } from "../components/Layout"
import { BinButton } from "../components/BinButton"
import SpringCheck from "../components/SpringCheck"
import { Button, Card, ProgressBar } from "../components/ui"
import { shoppingList as initialList, nutritionTargets } from "../data/mockData"
import type { ShoppingListItem } from "../types"

type Tab = "all" | "todo" | "done"

export function ShoppingList() {
  const [items, setItems] = useState<ShoppingListItem[]>(initialList)
  const [tab, setTab] = useState<Tab>("all")
  const [adding, setAdding] = useState("")
  const [toast, setToast] = useState("")
  const counts = {
    all: items.length,
    todo: items.filter((item) => !item.checked).length,
    done: items.filter((item) => item.checked).length,
  }
  const visible = items.filter((item) =>
    tab === "todo" ? !item.checked : tab === "done" ? item.checked : true,
  )
  const grouped = useMemo(
    () =>
      visible.reduce<Record<string, ShoppingListItem[]>>((groups, item) => {
        ;(groups[item.group] ??= []).push(item)
        return groups
      }, {}),
    [visible],
  )
  const flash = (message: string) => {
    setToast(message)
  }
  const toggle = (id: string) =>
    setItems((current) =>
      current.map((item) =>
        item.id === id ? { ...item, checked: !item.checked } : item,
      ),
    )
  const remove = (id: string) =>
    setItems((current) => current.filter((item) => item.id !== id))
  const addItem = () => {
    if (!adding.trim()) return
    setItems((current) => [
      ...current,
      {
        id: `n${Date.now()}`,
        group: "Nhóm khác",
        name: adding.trim(),
        quantity: "vừa đủ",
        checked: false,
      },
    ])
    setAdding("")
  }
  const exportTxt = () => {
    const text = items
      .map(
        (item) =>
          `${item.checked ? "[x]" : "[ ]"} ${item.name} — ${item.quantity}`,
      )
      .join("\n")
    const href = URL.createObjectURL(
      new Blob([`\uFEFFDANH SÁCH ĐI CHỢ — MÂM XANH\n\n${text}`], {
        type: "text/plain;charset=utf-8",
      }),
    )
    const anchor = document.createElement("a")
    anchor.href = href
    anchor.download = "danh-sach-di-cho.txt"
    anchor.click()
    window.setTimeout(() => URL.revokeObjectURL(href), 1000)
    flash("Đã xuất danh sách giao diện ra TXT.")
  }
  const copy = async () => {
    try {
      if (!navigator.clipboard) throw new Error("Clipboard unavailable.")
      await navigator.clipboard.writeText(
        items.map((item) => `- ${item.name}: ${item.quantity}`).join("\n"),
      )
      flash("Đã sao chép danh sách.")
    } catch {
      flash("Không thể sao chép. Bạn có thể tải tệp TXT.")
    }
  }

  return (
    <PageContainer className="py-6">
      <div className="grid items-start gap-5 md:grid-cols-[190px_minmax(0,1fr)] xl:grid-cols-[190px_minmax(0,1fr)_240px]">
        <aside className="md:sticky md:top-20">
          <h1 className="flex items-center gap-2 text-lg font-semibold text-ink">
            <ShoppingBasket className="h-5 w-5 text-brand-600" />
            Đi chợ
          </h1>
          <p className="mt-1 text-xs text-ink-muted">
            Danh sách của phiên xem thử
          </p>
          <nav
            aria-label="Lọc danh sách đi chợ"
            className="mt-5 flex flex-wrap gap-2 md:flex-col"
          >
            {([
              ["all", "Tất cả"],
              ["todo", "Cần mua"],
              ["done", "Đã mua"],
            ] as const).map(([value, label]) => (
              <button
                type="button"
                key={value}
                aria-pressed={tab === value}
                onClick={() => setTab(value)}
                className={`flex items-center justify-between gap-3 rounded-lg px-3 py-2.5 text-left text-xs ${
                  tab === value
                    ? "bg-paper font-semibold text-brand-600"
                    : "text-ink-muted hover:bg-paper"
                }`}
              >
                {label}
                <span>{counts[value]}</span>
              </button>
            ))}
          </nav>
          <div className="mt-5 hidden rounded-xl border border-brand-100 p-3 text-xs leading-5 text-ink-muted md:block">
            <ListChecks className="mb-2 h-5 w-5 text-brand-600" />
            Đánh dấu những nguyên liệu đã mua để theo dõi phần còn lại.
          </div>
        </aside>

        <Card className="min-w-0 p-4 sm:p-5">
          <header className="mb-5 flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-ink">
                Danh sách đi chợ
              </h2>
              <p className="mt-1 text-xs text-ink-muted">
                {counts.todo} cần mua · {counts.done} đã mua
              </p>
            </div>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={copy}>
                <ClipboardCopy className="h-3.5 w-3.5" />
                Sao chép
              </Button>
              <Button variant="outline" size="sm" onClick={exportTxt}>
                <Download className="h-3.5 w-3.5" />
                TXT
              </Button>
            </div>
          </header>
          <form
            onSubmit={(event) => {
              event.preventDefault()
              addItem()
            }}
            className="mb-6 flex items-center gap-2 rounded-full border border-brand-200 bg-cream-soft pl-3 pr-1"
          >
            <Plus className="h-4 w-4 text-brand-600" />
            <input
              aria-label="Thêm nguyên liệu vào danh sách"
              value={adding}
              onChange={(event) => setAdding(event.target.value)}
              placeholder="Thêm nguyên liệu..."
              className="min-w-0 flex-1 bg-transparent py-3 text-xs text-ink outline-none"
            />
            <Button type="submit" size="sm" disabled={!adding.trim()}>
              Thêm
            </Button>
          </form>
          {Object.entries(grouped).length ? (
            <div className="space-y-6">
              {Object.entries(grouped).map(([group, list]) => (
                <section key={group}>
                  <div className="mb-2 flex items-center justify-between">
                    <h3 className="text-xs font-semibold text-ink">{group}</h3>
                    <span className="text-[11px] text-ink-muted">
                      {list.filter((item) => item.checked).length} /{" "}
                      {list.length}
                    </span>
                  </div>
                  <div className="divide-y divide-brand-100">
                    {list.map((item) => (
                      <div
                        key={item.id}
                        className={`flex items-center gap-3 rounded-lg px-2 py-2.5 ${
                          item.checked
                            ? "bg-cream-soft/50"
                            : "hover:bg-raised/50"
                        }`}
                      >
                        <SpringCheck
                          checked={item.checked}
                          onChange={() => toggle(item.id)}
                          ariaLabel={`Đánh dấu ${item.name}`}
                          label={
                            <span className="min-w-0 flex-1">
                              <span className="text-xs font-medium text-ink">
                                {item.name}
                              </span>
                              {item.note && (
                                <span className="ml-1.5 text-[11px] font-normal text-ink-muted">
                                  · {item.note}
                                </span>
                              )}
                            </span>
                          }
                          strike="left"
                          boxSize={20}
                          boxRadius={6}
                          fontSize={13}
                          color="#ea580c"
                          fillColor="#ea580c"
                          checkColor="#ffffff"
                          textColor="var(--color-ink)"
                          className="min-w-0 flex-1"
                        />
                        <span
                          className={`text-xs ${
                            item.checked
                              ? "text-ink-muted line-through"
                              : "text-ink-soft"
                          }`}
                        >
                          {item.quantity}
                        </span>
                        <BinButton
                          size={28}
                          onClick={() => remove(item.id)}
                          title={`Xoá ${item.name}`}
                          aria-label={`Xoá ${item.name}`}
                        />
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
              <BinButton
                size={38}
                label={`Xoá ${counts.done} mục đã mua`}
                onClick={() =>
                  setItems((current) => current.filter((item) => !item.checked))
                }
                title="Xoá tất cả mục đã mua"
              />
            </div>
          )}
        </Card>

        <aside className="hidden xl:block">
          <Card className="p-4">
            <h2 className="text-sm font-semibold text-ink">
              Dinh dưỡng minh họa
            </h2>
            <p className="mt-2 text-xs leading-5 text-ink-muted">
              Số liệu mẫu để xem trước bảng kiểm tra vi chất, chưa tính từ danh
              sách bên cạnh.
            </p>
            <div className="mt-5 space-y-4">
              {nutritionTargets.map((nutrient) => (
                <div key={nutrient.key}>
                  <div className="mb-1.5 flex justify-between text-[11px]">
                    <span className="text-ink-muted">{nutrient.key}</span>
                    <span className="text-ink">{nutrient.actual}</span>
                  </div>
                  <ProgressBar
                    pct={nutrient.pct}
                    tone={
                      nutrient.status === "missing" || nutrient.status === "low"
                        ? "amber"
                        : "leaf"
                    }
                  />
                </div>
              ))}
            </div>
          </Card>
        </aside>
      </div>
      {toast && (
        <div
          role="status"
          className="fixed bottom-5 left-1/2 z-40 flex max-w-[calc(100vw-2rem)] -translate-x-1/2 items-center gap-3 rounded-xl border border-brand-200 bg-raised px-4 py-3 text-xs text-ink shadow-xl"
        >
          <span>{toast}</span>
          <button
            type="button"
            aria-label="Đóng thông báo"
            onClick={() => setToast("")}
            className="rounded p-1 text-ink-muted"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      )}
    </PageContainer>
  )
}
