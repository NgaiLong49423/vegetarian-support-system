import { useEffect, useState } from "react"
import { Link } from "react-router-dom"
import {
  Activity,
  ArrowLeftRight,
  Bookmark,
  Leaf,
  LoaderCircle,
  Lock,
  Search,
  Settings,
  SlidersHorizontal,
  Sparkles,
  UserRound,
  X,
} from "lucide-react"
import { PageContainer } from "../components/Layout"
import { Button, Card, EmptyState } from "../components/ui"
import { useAuth } from "../components/AuthContext"
import { subscriptionApi, type MySubscriptionResponse } from "../api/subscription"
import { useSavedRecipes } from "../components/SavedRecipesContext"
import { AddToMealPlanButton } from "../components/AddToMealPlanButton"
import {
  savedRecipesApi,
  type SavedRecipePage,
} from "../services/savedRecipesApi"
import { asApiError } from "../lib/apiClient"
import { formatSubscriptionDate as formatDate } from "../utils/subscriptionDate"

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
        <Card className="mb-6 p-5 sm:p-7 shadow-xs">
          <header className="flex flex-wrap items-center gap-4 sm:gap-6 py-0">
            {account.avatarUrl ? (
              <img
                src={account.avatarUrl}
                alt=""
                className="h-20 w-20 rounded-full object-cover ring-2 ring-brand-100"
              />
            ) : (
              <span
                aria-hidden="true"
                className="flex h-20 w-20 items-center justify-center rounded-full bg-brand-50 text-2xl font-bold text-brand-600 ring-2 ring-brand-100"
              >
                {account.displayName.charAt(0).toUpperCase()}
              </span>
            )}
            <div className="min-w-0 flex-1">
              <h1 className="text-2xl font-extrabold text-ink sm:text-3xl">
                {account.displayName}
              </h1>
              <p className="mt-1 truncate text-xs text-ink-muted">
                {account.email}
              </p>
              <p className="mt-2 text-xs font-semibold text-leaf-700">
                {account.role === "EXPERT"
                  ? "Chuyên gia ẩm thực"
                  : account.role === "ADMIN"
                    ? "Quản trị viên"
                    : "Thành viên Mâm Xanh"}
              </p>
              <div className="mt-2.5 flex flex-wrap items-center gap-2.5 text-xs">
                <span className="inline-flex items-center gap-1.5 text-ink-muted">
                  <Sparkles className="h-3.5 w-3.5 text-brand-600" />
                  {isFree ? "Gói AI: Miễn phí" : `Gói AI: ${activeTier}`}
                  {!isFree && subscription?.endsAt
                    ? ` · Hết hạn ${formatDate(subscription.endsAt)}`
                    : ""}
                </span>
                <Link
                  to="/goi-ai"
                  className="inline-flex items-center rounded-full border border-brand-200/80 bg-brand-50 px-2.5 py-0.5 font-semibold text-brand-700 transition-colors hover:bg-brand-100 hover:border-brand-300 dark:border-brand-800 dark:bg-brand-900/30 dark:text-brand-400"
                >
                  {isFree ? "Nâng cấp gói" : "Đổi gói"}
                </Link>
                <Link
                  to="/giao-dich"
                  className="inline-flex items-center rounded-full border border-brand-200/60 bg-paper px-2.5 py-0.5 font-medium text-ink-soft transition-colors hover:border-brand-300 hover:text-brand-700 dark:border-brand-800 dark:bg-surface"
                >
                  Lịch sử giao dịch
                </Link>
              </div>
            </div>
            <div className="flex flex-wrap items-center gap-2">
              {account.role !== "ADMIN" && (
                <Link
                  to={`/thanh-vien/${account.id}`}
                  className="inline-flex items-center gap-2 rounded-full border border-brand-300/40 bg-paper px-3.5 py-1.5 text-sm font-semibold text-ink shadow-xs transition-all hover:border-brand-500 hover:text-brand-700 hover:shadow-sm dark:border-brand-800 dark:bg-surface"
                >
                  <UserRound className="h-4 w-4 text-brand-600 shrink-0" />
                  Trang cá nhân
                </Link>
              )}
              <Link
                to="/ho-so/so-thich-an-uong"
                className="inline-flex items-center gap-2 rounded-full border border-brand-300/40 bg-paper px-3.5 py-1.5 text-sm font-semibold text-ink shadow-xs transition-all hover:border-brand-500 hover:text-brand-700 hover:shadow-sm dark:border-brand-800 dark:bg-surface"
              >
                <Settings className="h-4 w-4 text-brand-600 shrink-0" />
                Sở thích & hồ sơ
              </Link>
            </div>
          </header>
        </Card>
        <nav
          aria-label="Nội dung hồ sơ"
          className="mb-6 mt-5 flex gap-6 border-b border-brand-100/80 dark:border-brand-900/40"
        >
          <button
            type="button"
            aria-pressed={tab === "saved"}
            onClick={() => setTab("saved")}
            className={`border-b-2 pb-3 text-sm font-semibold transition-colors duration-200 ${
              tab === "saved"
                ? "border-brand-600 text-brand-600"
                : "border-transparent text-ink-soft hover:text-brand-600"
            }`}
          >
            Công thức đã lưu
          </button>
          {account.role === "EXPERT" && (
            <Link
              to="/ho-so/chuyen-gia"
              className="pb-3 text-sm font-semibold text-ink-soft transition-colors duration-200 hover:text-brand-600"
            >
              Đã tạo
            </Link>
          )}
          <button
            type="button"
            aria-pressed={tab === "profile"}
            onClick={() => setTab("profile")}
            className={`border-b-2 pb-3 text-sm font-semibold transition-colors duration-200 ${
              tab === "profile"
                ? "border-brand-600 text-brand-600"
                : "border-transparent text-ink-soft hover:text-brand-600"
            }`}
          >
            Sức khỏe & sở thích
          </button>
        </nav>

        {tab === "profile" ? (
          <div className="grid gap-4 sm:grid-cols-2">
            <Card hover className="p-5">
              <Activity className="h-6 w-6 text-brand-600" />
              <h2 className="mt-3 font-semibold text-ink">Hồ sơ dinh dưỡng</h2>
              <p className="mt-2 text-xs leading-5 text-ink-muted">
                Xem và cập nhật thông tin để tính các chỉ số cá nhân.
              </p>
              <Link
                to="/ho-so/dinh-duong"
                className="mt-4 inline-block text-xs font-semibold text-brand-600 transition-colors hover:text-brand-500"
              >
                Mở hồ sơ dinh dưỡng →
              </Link>
            </Card>
            <Card hover className="p-5">
              <SlidersHorizontal className="h-6 w-6 text-leaf-700" />
              <h2 className="mt-3 font-semibold text-ink">Sở thích ăn uống</h2>
              <p className="mt-2 text-xs leading-5 text-ink-muted">
                Quản lý trường phái ăn chay, nguyên liệu cần tránh và khẩu vị.
              </p>
              <Link
                to="/ho-so/so-thich-an-uong"
                className="mt-4 inline-block text-xs font-semibold text-brand-600 transition-colors hover:text-brand-500"
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
            <div className="mb-5 flex items-center gap-2 rounded-full border border-brand-200 bg-paper px-4 transition-all duration-200 focus-within:border-brand-600 focus-within:ring-2 focus-within:ring-brand-500/20">
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
                  <article
                    key={item.recipeId}
                    className="group relative flex h-full min-w-0 flex-col overflow-hidden rounded-2xl border border-brand-100/90 bg-paper shadow-xs transition-all duration-200 hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-md dark:border-brand-800/70 dark:bg-surface"
                  >
                    {item.available ? (
                      <>
                        <Link
                          to={`/cong-thuc/id/${item.recipeId}`}
                          className="group/link flex flex-1 flex-col focus-visible:outline-offset-4"
                        >
                          <div className="relative aspect-[4/3] w-full overflow-hidden bg-raised">
                            {item.coverUrl ? (
                              <img
                                src={item.coverUrl}
                                alt={item.title ?? ""}
                                loading="lazy"
                                className="h-full w-full object-cover transition-transform duration-200 motion-safe:group-hover/link:scale-[1.03]"
                              />
                            ) : (
                              <div className="flex h-full items-center justify-center text-ink-muted">
                                <Leaf className="h-7 w-7" />
                              </div>
                            )}
                          </div>
                          <div className="flex flex-1 flex-col p-3.5 pb-2 sm:p-4 sm:pb-2">
                            <h3 className="line-clamp-2 min-h-10 text-sm font-semibold leading-5 text-ink transition-colors duration-200 group-hover/link:text-brand-600">
                              {item.title}
                            </h3>
                            <p className="mt-1 truncate text-xs text-ink-muted">
                              {item.authorName}
                            </p>
                          </div>
                        </Link>
                        <div className="flex items-center justify-between px-3.5 pb-3 pt-0.5 sm:px-4 sm:pb-3.5">
                          <Link
                            to={`/so-sanh?a=${item.recipeId}`}
                            aria-label={`So sánh ${item.title}`}
                            className="inline-flex items-center gap-1 rounded text-xs font-semibold text-brand-600 hover:text-brand-700"
                          >
                            <ArrowLeftRight className="h-3 w-3" aria-hidden="true" />
                            So sánh
                          </Link>
                        </div>
                      </>
                    ) : (
                      <div className="flex flex-1 flex-col">
                        <div className="flex aspect-[4/3] w-full items-center justify-center bg-raised">
                          <Lock className="h-7 w-7 text-ink-muted" />
                        </div>
                        <div className="flex flex-1 flex-col p-3.5 pb-3 sm:p-4 sm:pb-3.5">
                          <p className="min-h-10 text-sm font-medium leading-5 text-ink-muted">
                            {item.unavailableMessage ??
                              "Công thức không còn khả dụng"}
                          </p>
                        </div>
                      </div>
                    )}
                    {item.available && item.title && account.role !== "ADMIN" && (
                      <div className="absolute right-12 top-2 z-10">
                        <AddToMealPlanButton recipeId={item.recipeId} recipeTitle={item.title} />
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
                      className="absolute right-2 top-2 flex h-8 w-8 items-center justify-center rounded-full bg-brand-600 text-on-brand transition-transform duration-150 hover:scale-105 active:scale-95 disabled:opacity-50"
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
