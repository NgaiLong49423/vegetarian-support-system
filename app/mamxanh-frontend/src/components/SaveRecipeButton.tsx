import { useState } from "react"
import { Link } from "react-router-dom"
import { Bookmark, LoaderCircle } from "lucide-react"
import { useAuth } from "./AuthContext"
import { useSavedRecipes } from "./SavedRecipesContext"
import { Modal } from "./Modal"
import { asApiError } from "../lib/apiClient"

export function SaveRecipeButton({
  recipeId,
  compact = false,
}: {
  recipeId: number
  compact?: boolean
}) {
  const { account } = useAuth()
  const { ids, loading, error, pending, reload, setSaved } = useSavedRecipes()
  const [loginOpen, setLoginOpen] = useState(false)
  const [notice, setNotice] = useState("")
  if (account?.role === "ADMIN") return null
  const saved = ids.has(recipeId)
  const busy = Boolean(account) && (loading || pending.has(recipeId))
  const action = async () => {
    if (!account) {
      setLoginOpen(true)
      return
    }
    setNotice("")
    if (error) {
      reload()
      setNotice("Đang thử tải lại danh sách đã lưu.")
      return
    }
    try {
      await setSaved(recipeId, !saved)
    } catch (cause) {
      setNotice(
        asApiError(cause).message ||
          "Chưa lưu được thay đổi. Vui lòng thử lại.",
      )
    }
  }
  return (
    <div className="relative">
      <button
        type="button"
        aria-label={saved ? "Bỏ lưu" : "Lưu bài viết"}
        aria-pressed={saved}
        aria-busy={busy}
        disabled={busy}
        onClick={action}
        title={
          error
            ? "Thử tải lại trạng thái đã lưu"
            : saved
              ? "Bỏ lưu"
              : "Lưu bài viết"
        }
        className={
          compact
            ? `flex h-8 w-8 items-center justify-center rounded-full transition-colors disabled:opacity-60 ${
                saved
                  ? "bg-brand-600 text-on-brand"
                  : "bg-black/65 text-white hover:bg-black/85"
              }`
            : `inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm font-medium disabled:opacity-60 ${
                saved
                  ? "border-brand-600 bg-brand-600 text-on-brand"
                  : "border-brand-200 text-ink hover:border-brand-600"
              }`
        }
      >
        {busy ? (
          <LoaderCircle className="h-4 w-4 animate-spin" />
        ) : (
          <Bookmark className={`h-4 w-4 ${saved ? "fill-current" : ""}`} />
        )}
        {!compact && (saved ? "Đã lưu" : "Lưu bài viết")}
      </button>
      {notice && (
        <p
          role="status"
          className={
            compact
              ? "absolute right-0 top-full z-20 mt-2 w-56 rounded-lg border border-brand-200 bg-paper p-3 text-xs text-ink"
              : "mt-2 max-w-xs text-xs text-ink-muted"
          }
        >
          {notice}
        </p>
      )}
      <Modal
        open={loginOpen}
        onClose={() => setLoginOpen(false)}
        title="Đăng nhập để lưu công thức"
      >
        <p className="text-sm leading-6 text-ink-muted">
          Đăng nhập để lưu công thức vào danh sách riêng của bạn và xem lại trên
          tài khoản.
        </p>
        <Link
          to="/dang-nhap"
          className="mt-5 inline-block rounded-full bg-brand-600 px-5 py-2.5 text-sm font-semibold text-on-brand"
        >
          Đăng nhập
        </Link>
      </Modal>
    </div>
  )
}
