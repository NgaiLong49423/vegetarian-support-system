import { useEffect, useRef, useState } from "react"
import { Link, useNavigate } from "react-router-dom"
import { useAuth } from "./AuthContext"
import { Button } from "./ui"
import { shoppingApi } from "../api/shopping"
import { asApiError } from "../lib/apiClient"

/** The same selection action is used in Explore and saved recipes. IDs persist across pagination. */
export function useRecipeShoppingSelection() {
  const { account } = useAuth()
  const navigate = useNavigate()
  const [selected, setSelected] = useState<number[]>([])
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState("")
  const pending = useRef(false)
  const owner = useRef(account?.id)
  owner.current = account?.id
  const active = useRef(true)
  useEffect(() => {
    active.current = true
    return () => {
      active.current = false
    }
  }, [])
  useEffect(() => {
    setSelected([])
    setError("")
  }, [account?.id])
  const member = account && account.role !== "ADMIN"
  async function add() {
    if (!member || !selected.length || pending.current) return
    const currentOwner = account.id
    pending.current = true
    setBusy(true)
    setError("")
    try {
      const result = await shoppingApi.fromRecipes(selected)
      if (active.current && owner.current === currentOwner)
        navigate(`/di-cho?listId=${result.shoppingList.id}`, {
          state: { shoppingCreation: result },
        })
    } catch (cause) {
      if (active.current && owner.current === currentOwner)
        setError(asApiError(cause).message)
    } finally {
      pending.current = false
      if (active.current) setBusy(false)
    }
  }
  return {
    checkbox: (id: number, title: string, className = "") =>
      member ? (
        <label className={`mt-auto flex min-h-9 items-center justify-between gap-2 rounded-xl border border-brand-100 bg-brand-50/40 px-3 py-1.5 text-xs font-medium text-ink-soft select-none cursor-pointer hover:border-brand-300 hover:bg-brand-50 hover:text-ink transition-all ${className}`}>
          <span className="truncate">Chọn đi chợ</span>
          <input
            type="checkbox"
            aria-label={`Chọn ${title} để đi chợ`}
            checked={selected.includes(id)}
            disabled={busy || (!selected.includes(id) && selected.length >= 50)}
            className="h-4 w-4 rounded border-brand-300 text-brand-600 accent-brand-600 focus:ring-brand-500 cursor-pointer"
            onChange={(event) => {
              const checked = event.target.checked
              setSelected((ids) =>
                checked ? [...ids, id] : ids.filter((value) => value !== id),
              )
            }}
          />
        </label>
      ) : null,
    toolbar:
      account?.role === "ADMIN" ? null : (
        <section
          aria-label="Tạo danh sách từ công thức"
          className="sticky bottom-4 z-20 mt-6 rounded-2xl border border-brand-200 bg-paper/95 p-3.5 shadow-xl backdrop-blur-md transition-all sm:p-4"
        >
          {member ? (
            <>
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div className="min-w-0">
                  <p className="text-sm font-semibold text-ink">
                    {selected.length > 0
                      ? `Đã chọn ${selected.length} công thức để đi chợ`
                      : "Chưa chọn công thức nào"}
                  </p>
                  <p className="mt-0.5 text-xs text-ink-muted">
                    Chọn tối đa 50 công thức. Nguyên liệu sẽ được cộng vào danh sách cá nhân.
                  </p>
                </div>
                <div className="flex flex-wrap items-center gap-2 shrink-0">
                  <Button
                    variant="primary"
                    disabled={!selected.length || busy}
                    onClick={() => void add()}
                  >
                    {busy
                      ? "Đang thêm…"
                      : `Thêm ${selected.length} công thức vào danh sách đi chợ`}
                  </Button>
                  <Button
                    variant="outline"
                    disabled={!selected.length || busy}
                    onClick={() => setSelected([])}
                  >
                    Bỏ chọn tất cả
                  </Button>
                </div>
              </div>
              {error && (
                <p role="alert" className="mt-2 text-sm text-red-700">
                  {error}
                </p>
              )}
            </>
          ) : (
            <Link
              to="/dang-nhap"
              className="text-sm font-semibold text-brand-600 hover:underline"
            >
              Đăng nhập để tạo danh sách đi chợ từ công thức
            </Link>
          )}
        </section>
      ),
  }
}
