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
        <label className={`mt-auto flex min-h-10 items-center gap-2 text-xs text-ink-soft select-none cursor-pointer hover:text-ink ${className}`}>
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
          <span>Chọn đi chợ</span>
        </label>
      ) : null,
    toolbar:
      account?.role === "ADMIN" ? null : (
        <section
          aria-label="Tạo danh sách từ công thức"
          className="mb-4 rounded-xl border border-brand-100 bg-paper p-3"
        >
          {member ? (
            <>
              <div className="flex flex-wrap items-center gap-3">
                <Button
                  variant="secondary"
                  disabled={!selected.length || busy}
                  onClick={() => void add()}
                >
                  {busy
                    ? "Đang thêm…"
                    : `Thêm ${selected.length} công thức vào danh sách đi chợ`}
                </Button>
                <Button
                  variant="ghost"
                  disabled={!selected.length || busy}
                  onClick={() => setSelected([])}
                >
                  Bỏ chọn tất cả
                </Button>
              </div>
              <p className="mt-2 text-xs text-ink-muted">
                Chọn tối đa 50 công thức, kể cả ở các trang khác. Nguyên liệu
                theo khẩu phần gốc được thêm vào danh sách cá nhân; công thức đã
                thêm không bị cộng trùng.
              </p>
              {error && (
                <p role="alert" className="mt-2 text-sm text-red-700">
                  {error}
                </p>
              )}
            </>
          ) : (
            <Link
              to="/dang-nhap"
              className="text-sm font-semibold text-brand-600"
            >
              Đăng nhập để tạo danh sách đi chợ từ công thức
            </Link>
          )}
        </section>
      ),
  }
}
