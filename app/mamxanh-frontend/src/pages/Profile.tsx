import { useEffect, useState } from "react"
import { Link } from "react-router-dom"
import {
  Activity,
  Bookmark,
  Leaf,
  LoaderCircle,
  Lock,
  Search,
  Settings,
  SlidersHorizontal,
  Sparkles,
  X,
} from "lucide-react"
import { PageContainer } from "../components/Layout"
import { Button, Card, EmptyState } from "../components/ui"
import { useAuth } from "../components/AuthContext"
import { subscriptionApi, type MySubscriptionResponse } from "../api/subscription"
import { useSavedRecipes } from "../components/SavedRecipesContext"
import {
  savedRecipesApi,
  type SavedRecipePage,
} from "../services/savedRecipesApi"
import { asApiError } from "../lib/apiClient"
import { formatDate } from "../utils/date"

export function Profile() {
  const { account } = useAuth()
  const { setSaved, pending, revision } = useSavedRecipes()
  const [subscription, setSubscription] = useState<MySubscriptionResponse | null>(null)
  const [tab, setTab] = useState<"saved" | "profile">("saved")
  const [keyword, setKeyword] = useState("")
  const [page, setPage] = useState(0)
  const [result, setResult] = useState<SavedRecipePage | null>(null)
  const [ownerId, setOwnerId] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    let active = true
    setSubscription(null)
    if (account) {
      subscriptionApi
        .getMySubscription()
        .then((value) => {
          if (active) setSubscription(value)
        })
        .catch(() => {})
    }
    return () => {
      active = false
    }
  }, [account?.id])

  const activeTier =
    subscription?.status === "ACTIVE" ? subscription.tier : null
  const isFree = !activeTier || activeTier === "FREE"

  useEffect(() => {
    let active = true
    setResult(null)
    setError("")
    setOwnerId(account?.id ?? null)
    if (!account || account.role === "ADMIN") {
      setLoading(false)
      return
    }
    setLoading(true)
    const timer = window.setTimeout(
      () => {
        savedRecipesApi
          .list(keyword.trim(), page, 20)
          .then((value) => {
            if (active) setResult(value)
          })
          .catch((cause) => {
            if (active) setError(asApiError(cause).message)
          })
          .finally(() => {
            if (active) setLoading(false)
          })
      },
      keyword.trim() ? 200 : 0,
    )
    return () => {
      active = false
      window.clearTimeout(timer)
    }
  }, [account?.id, account?.role, keyword, page, retry, revision])

  const remove = async (recipeId: number) => {
    setError("")
    try {
      await setSaved(recipeId, false)
      if (result?.content.length === 1 && page > 0)
        setPage((current) => current - 1)
    } catch (cause) {
      setError(asApiError(cause).message)
    }
  }

  if (!account)
    return (
      <PageContainer className="py-10">
        <EmptyState
          icon="🔖"
          title="Góc bếp riêng của bạn"
          description="Đăng nhập để xem hồ sơ và những công thức đã lưu trên tài khoản."
          action={
            <Link
              to="/dang-nhap"
              className="rounded-full bg-brand-600 px-5 py-2.5 text-sm font-semibold text-on-brand"
            >
              Đăng nhập
            </Link>
          }
        />
      </PageContainer>
    )
  const ownResult = ownerId === account.id ? result : null

  return (
    <PageContainer className="py-7">
      <div className="mx-auto max-w-4xl">
        <header className="flex flex-wrap items-center gap-4 py-3 sm:gap-5">
          {account.avatarUrl ? (
            <img
              src={account.avatarUrl}
              alt=""
              className="h-20 w-20 rounded-full object-cover"
            />
          ) : (
            <span
              aria-hidden="true"
              className="flex h-20 w-20 items-center justify-center rounded-full bg-paper text-2xl font-semibold text-brand-600"
            >
              {account.displayName.charAt(0).toUpperCase()}
            </span>
          )}
          <div className="min-w-0 flex-1">
            <h1 className="text-2xl font-semibold text-ink">
              {account.displayName}
            </h1>
            <p className="mt-1 truncate text-xs text-ink-muted">
              {account.email}
            </p>
            <p className="mt-2 text-xs text-leaf-700">
              {account.role === "EXPERT"
                ? "Chuyên gia ẩm thực"
                : account.role === "ADMIN"
                  ? "Quản trị viên"
                  : "Thành viên Mâm Xanh"}
            </p>
            <div className="mt-2 flex flex-wrap items-center gap-3 text-xs">
              <span className="inline-flex items-center gap-1.5 text-ink-muted">
                <Sparkles className="h-3.5 w-3.5" />
                {isFree ? "Gói AI: Miễn phí" : `Gói AI: ${activeTier}`}
                {!isFree && subscription?.endsAt
                  ? ` · Hết hạn ${formatDate(subscription.endsAt)}`
                  : ""}
              </span>
              <Link
                to="/goi-ai"
                className="font-semibold text-brand-600 underline hover:text-brand-700"
              >
                {isFree ? "Nâng cấp gói" : "Đổi gói"}
              </Link>
              <Link
                to="/giao-dich"
                className="text-ink-muted hover:text-ink-soft"
              >
                Lịch sử giao dịch
              </Link>
            </div>
          </div>
          <Link
            to="/ho-so/so-thich-an-uong"
            className="inline-flex items-center gap-2 rounded-full border border-brand-200 px-4 py-2 text-xs text-ink-soft hover:bg-paper"
          >
            <Settings className="h-3.5 w-3.5" />
            Sở thích & hồ sơ
          </Link>
        </header>
        <nav
          aria-label="Nội dung hồ sơ"
          className="mb-6 mt-5 flex gap-6 border-b border-brand-100"
        >
          <button
            type="button"
            aria-pressed={tab === "saved"}
            onClick={() => setTab("saved")}
            className={`border-b-2 pb-3 text-sm font-semibold ${
              tab === "saved"
                ? "border-brand-600 text-brand-600"
                : "border-transparent text-ink-muted"
            }`}
          >
            Công thức đã lưu
          </button>
          {account.role === "EXPERT" && (
            <Link
              to="/ho-so/chuyen-gia"
              className="pb-3 text-sm font-medium text-ink-muted hover:text-brand-600"
            >
              Đã tạo
            </Link>
          )}
          <button
            type="button"
            aria-pressed={tab === "profile"}
            onClick={() => setTab("profile")}
            className={`border-b-2 pb-3 text-sm font-semibold ${
              tab === "profile"
                ? "border-brand-600 text-brand-600"
                : "border-transparent text-ink-muted"
            }`}
          >
            Hồ sơ của tôi
          </button>
        </nav>

        {tab === "profile" ? (
          <div className="grid gap-4 sm:grid-cols-2">
            <Card className="p-5">
              <Activity className="h-6 w-6 text-brand-600" />
              <h2 className="mt-3 font-semibold text-ink">Hồ sơ dinh dưỡng</h2>
              <p className="mt-2 text-xs leading-5 text-ink-muted">
                Xem và cập nhật thông tin để tính các chỉ số cá nhân.
              </p>
              <Link
                to="/ho-so/dinh-duong"
                className="mt-4 inline-block text-xs font-semibold text-brand-600"
              >
                Mở hồ sơ dinh dưỡng →
              </Link>
            </Card>
            <Card className="p-5">
              <SlidersHorizontal className="h-6 w-6 text-leaf-700" />
              <h2 className="mt-3 font-semibold text-ink">Sở thích ăn uống</h2>
              <p className="mt-2 text-xs leading-5 text-ink-muted">
                Quản lý trường phái ăn chay, nguyên liệu cần tránh và khẩu vị.
              </p>
              <Link
                to="/ho-so/so-thich-an-uong"
                className="mt-4 inline-block text-xs font-semibold text-brand-600"
              >
                Mở sở thích ăn uống →
              </Link>
            </Card>
          </div>
        ) : account.role === "ADMIN" ? (
          <EmptyState
            icon="🔒"
            title="Danh sách riêng dành cho thành viên"
            description="Tài khoản quản trị viên không có quyền truy cập danh sách đã lưu của thành viên."
          />
        ) : (
          <>
            <div className="mb-5 flex items-center gap-2 rounded-full border border-brand-200 bg-paper px-4">
              <Search className="h-4 w-4 text-ink-muted" />
              <input
                aria-label="Tìm trong công thức đã lưu"
                maxLength={120}
                value={keyword}
                onChange={(event) => {
                  setKeyword(event.target.value)
                  setPage(0)
                }}
                placeholder="Tìm trong công thức đã lưu..."
                className="min-w-0 flex-1 bg-transparent py-3 text-sm text-ink outline-none"
              />
              {keyword && (
                <button
                  type="button"
                  aria-label="Xóa tìm kiếm đã lưu"
                  onClick={() => {
                    setKeyword("")
                    setPage(0)
                  }}
                  className="p-1 text-ink-muted"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>
            <div className="mb-5 flex items-center justify-between">
              <h2 className="text-base font-semibold text-ink">Công thức</h2>
              <span className="text-xs text-ink-muted">
                {ownResult
                  ? `${ownResult.totalElements} đã lưu · Mới lưu trước`
                  : "Danh sách riêng của bạn"}
              </span>
            </div>
            {error && (
              <div
                role="alert"
                className="mb-4 rounded-xl border border-brand-200 bg-paper p-4 text-sm text-ink-muted"
              >
                <p>{error}</p>
                <button
                  type="button"
                  onClick={() => setRetry((value) => value + 1)}
                  className="mt-2 font-semibold text-brand-600"
                >
                  Thử lại
                </button>
              </div>
            )}
            {loading ? (
              <div
                role="status"
                className="flex items-center justify-center gap-2 py-16 text-sm text-ink-muted"
              >
                <LoaderCircle className="h-5 w-5 animate-spin" />
                Đang tải danh sách đã lưu…
              </div>
            ) : ownResult?.content.length ? (
              <div className="grid grid-cols-2 gap-x-4 gap-y-6 sm:grid-cols-3 lg:grid-cols-4">
                {ownResult.content.map((item) => (
                  <article key={item.recipeId} className="relative min-w-0">
                    {item.available ? (
                      <Link
                        to={`/cong-thuc/id/${item.recipeId}`}
                        className="group block"
                      >
                        <div className="aspect-[4/5] overflow-hidden rounded-xl bg-paper">
                          {item.coverUrl ? (
                            <img
                              src={item.coverUrl}
                              alt={item.title ?? ""}
                              loading="lazy"
                              className="h-full w-full object-cover transition-transform motion-safe:group-hover:scale-[1.03]"
                            />
                          ) : (
                            <div className="flex h-full items-center justify-center text-ink-muted">
                              <Leaf className="h-7 w-7" />
                            </div>
                          )}
                        </div>
                        <h3 className="mt-2 line-clamp-2 text-sm font-semibold text-ink group-hover:text-brand-600">
                          {item.title}
                        </h3>
                        <p className="mt-1 truncate text-xs text-ink-muted">
                          {item.authorName}
                        </p>
                      </Link>
                    ) : (
                      <div>
                        <div className="flex aspect-[4/5] items-center justify-center rounded-xl border border-brand-100 bg-paper">
                          <Lock className="h-7 w-7 text-ink-muted" />
                        </div>
                        <p className="mt-2 text-sm font-medium text-ink-muted">
                          {item.unavailableMessage ??
                            "Công thức không còn khả dụng"}
                        </p>
                      </div>
                    )}
                    <button
                      type="button"
                      aria-label={`Bỏ lưu ${
                        item.available
                          ? item.title
                          : "công thức không còn khả dụng"
                      }`}
                      disabled={pending.has(item.recipeId)}
                      onClick={() => remove(item.recipeId)}
                      className="absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-full bg-brand-600 text-on-brand disabled:opacity-50"
                    >
                      {pending.has(item.recipeId) ? (
                        <LoaderCircle className="h-4 w-4 animate-spin" />
                      ) : (
                        <Bookmark className="h-4 w-4 fill-current" />
                      )}
                    </button>
                  </article>
                ))}
              </div>
            ) : (
              !error && (
                <EmptyState
                  icon="🔖"
                  title={
                    keyword
                      ? "Không tìm thấy công thức đã lưu"
                      : "Chưa có công thức đã lưu"
                  }
                  description={
                    keyword
                      ? "Thử một từ khóa khác trong danh sách của bạn."
                      : "Nhấn biểu tượng lưu trên thẻ món hoặc trang chi tiết để giữ công thức ở đây."
                  }
                  action={
                    <Link
                      to="/kham-pha"
                      className="text-sm font-semibold text-brand-600"
                    >
                      Khám phá công thức →
                    </Link>
                  }
                />
              )
            )}
            {ownResult && ownResult.totalPages > 1 && (
              <nav
                aria-label="Phân trang công thức đã lưu"
                className="mt-7 flex items-center justify-center gap-3"
              >
                <Button
                  variant="outline"
                  disabled={page === 0 || loading}
                  onClick={() => setPage((value) => value - 1)}
                >
                  Trang trước
                </Button>
                <span className="text-xs text-ink-muted">
                  {page + 1} / {ownResult.totalPages}
                </span>
                <Button
                  variant="outline"
                  disabled={page + 1 >= ownResult.totalPages || loading}
                  onClick={() => setPage((value) => value + 1)}
                >
                  Trang sau
                </Button>
              </nav>
            )}
          </>
        )}
      </div>
    </PageContainer>
  )
}
