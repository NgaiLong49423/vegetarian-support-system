import { useEffect, useRef, useState } from "react"
import { useNavigate } from "react-router-dom"
import { ShoppingBasket } from "lucide-react"
import { shoppingApi } from "../api/shopping"
import { asApiError } from "../lib/apiClient"
import { localDateKey } from "./WeekCalendar"
import { Button } from "./ui"
import { Modal } from "./Modal"

export function CreateShoppingListButton({
  week,
  disabled,
}: {
  week: string
  disabled: boolean
}) {
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)
  const [wholeWeek, setWholeWeek] = useState(true)
  const [dates, setDates] = useState<string[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")
  const pending = useRef(false)
  const alive = useRef(true)
  const trigger = useRef<HTMLButtonElement>(null)
  const firstChoice = useRef<HTMLInputElement>(null)
  useEffect(() => {
    alive.current = true
    return () => {
      alive.current = false
    }
  }, [])
  useEffect(() => {
    if (open) firstChoice.current?.focus()
  }, [open])
  function close() {
    if (pending.current) return
    setOpen(false)
    requestAnimationFrame(() => trigger.current?.focus())
  }
  async function create() {
    if (pending.current) return
    pending.current = true
    setBusy(true)
    setError("")
    try {
      const result = await shoppingApi.create(
        week,
        wholeWeek ? undefined : dates,
      )
      if (alive.current)
        navigate(`/di-cho?listId=${result.shoppingList.id}`, {
          state: { shoppingCreation: result },
        })
    } catch (cause) {
      if (alive.current) setError(asApiError(cause).message)
    } finally {
      pending.current = false
      if (alive.current) setBusy(false)
    }
  }
  return (
    <>
      <button
        ref={trigger}
        type="button"
        disabled={disabled}
        onClick={() => {
          setOpen(true)
          setError("")
          setWholeWeek(true)
          setDates([])
        }}
        className="inline-flex items-center gap-2 rounded-full border border-brand-200 px-3 py-1.5 text-sm font-semibold text-ink-soft hover:border-brand-400 focus-visible:outline-2 focus-visible:outline-brand-400 disabled:opacity-50"
      >
        <ShoppingBasket className="h-4 w-4" />
        Tạo danh sách mua sắm
      </button>
      <Modal trapFocus open={open} onClose={close} title="Tạo danh sách mua sắm">
        <form
          onSubmit={(event) => {
            event.preventDefault()
            void create()
          }}
          className="space-y-4"
          aria-busy={busy}
        >
          <p className="text-sm leading-6 text-ink-muted">
            Nguyên liệu được tính theo khẩu phần đã lưu. Mỗi lần xác nhận tạo
            một danh sách mới từ tuần bắt đầu{" "}
            {new Date(`${week}T12:00:00`).toLocaleDateString("vi-VN")}.
            {" "}Ngày được chọn là ngày của thực đơn cần chuẩn bị nguyên liệu,
            không phải ngày đi mua.
          </p>
          <fieldset disabled={busy} className="space-y-3 text-sm text-ink">
            <legend className="mb-2 font-semibold">
              Phạm vi lấy nguyên liệu
            </legend>
            <label className="flex items-center gap-2">
              <input
                ref={firstChoice}
                type="radio"
                name="shopping-scope"
                checked={wholeWeek}
                onChange={() => setWholeWeek(true)}
              />
              Cả tuần
            </label>
            <label className="flex items-center gap-2">
              <input
                type="radio"
                name="shopping-scope"
                checked={!wholeWeek}
                onChange={() => setWholeWeek(false)}
              />
              Chọn ngày
            </label>
            {!wholeWeek && (
              <div className="grid gap-3 sm:grid-cols-2">
                {Array.from({ length: 7 }, (_, offset) => {
                  const date = new Date(`${week}T12:00:00`)
                  date.setDate(date.getDate() + offset)
                  const key = localDateKey(date)
                  return (
                    <label key={key} className="flex items-center gap-2">
                      <input
                        type="checkbox"
                        checked={dates.includes(key)}
                        onChange={(event) =>
                          setDates((current) =>
                            event.target.checked
                              ? [...current, key]
                              : current.filter((value) => value !== key),
                          )
                        }
                      />
                      {date.toLocaleDateString("vi-VN", {
                        weekday: "long",
                        day: "numeric",
                        month: "numeric",
                      })}
                    </label>
                  )
                })}
              </div>
            )}
          </fieldset>
          {!wholeWeek && !dates.length && (
            <p className="text-xs text-ink-muted">
              Chọn ít nhất một ngày có món trong Lịch ăn.
            </p>
          )}
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
              onClick={close}
            >
              Hủy
            </Button>
            <Button
              type="submit"
              disabled={busy || (!wholeWeek && !dates.length)}
            >
              {busy ? "Đang tạo…" : "Tạo danh sách"}
            </Button>
          </div>
        </form>
      </Modal>
    </>
  )
}
