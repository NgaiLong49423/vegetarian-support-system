import { AdminPageHeader } from "../components/AdminPageHeader"
import { useCallback, useEffect, useState } from "react"
import { Link, useNavigate, useParams } from "react-router-dom"

import {
  BookOpen,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Coins,
  Eye,
  ExternalLink,
  Gift,
  Lock,
  Search,
  ShieldAlert,
  Unlock,
  UserCheck,
  Users,
  X,
} from "lucide-react"

import { ApiError } from "../lib/apiClient"

import {
  Badge,
  Button,
  Card,
  AnimatedNumber,
} from "../components/ui"

import {
  AdminAiTier,
  AdminAiPackageState,
  AdminExpertDetail,
  AdminUser,
  AdminUserAudit,
  AdminUserRole,
  AdminUserStatus,
  AdminUserSummary,
  getAdminAiTier,
  getAdminExpertDetail,
  getAdminUser,
  getAdminUserHistory,
  getAdminUserSummary,
  grantAdminAiCredit,
  giftAdminAiPackage,
  listAdminUsers,
  updateAdminUserRole,
  updateAdminUserStatus,
} from "../api/adminUsers"
import { membersApi } from "../api/members"
import { recipesApi, type RecipeFormOptions, type RecipeSearchResult } from "../api/recipes"

const PAGE_SIZE = 20
const EXPERT_RECIPE_PAGE_SIZE = 12
const recipeCategoryFallbacks: Record<string, string> = {
  NOODLE_SOUP: "Món nước",
  STIR_FRY: "Món xào",
  HOT_POT: "Món lẩu",
  BRAISED: "Món kho",
  SOUP: "Món canh",
  FRIED: "Món chiên",
  STEAMED: "Món hấp",
  SALAD: "Món gỏi / salad",
  ROLL: "Món cuốn",
  GRILLED: "Món nướng",
  DESSERT: "Món tráng miệng / chè",
}
const vegetarianTypeFallbacks: Record<string, string> = {
  VEGAN: "Thuần chay",
  LACTO: "Có sữa",
  OVO: "Có trứng",
  LACTO_OVO: "Có trứng và sữa",
}

type ExpertSummaryFallback = Pick<
  AdminUserSummary,
  "activeExperts" | "lockedExperts" | "expertPublishedRecipeCount"
>

const roleLabels: Record<AdminUserRole, string> = {
  CUSTOMER: "Thành viên",
  EXPERT: "Chuyên gia",
  ADMIN: "Quản trị viên",
}

function date(value: string) {
  return new Intl.DateTimeFormat("vi-VN", {
    dateStyle: "short",
    timeZone: "Asia/Ho_Chi_Minh",
  }).format(new Date(`${value.replace(" ", "T")}Z`))
}

function apiMessage(error: unknown) {
  if (
    error instanceof ApiError &&
    (error.status === 401 || error.status === 403)
  )
    return "Phiên đăng nhập không có quyền quản lý người dùng. Hãy đăng nhập bằng tài khoản Quản trị viên."

  return error instanceof ApiError
    ? error.message
    : "Không thể tải dữ liệu. Vui lòng thử lại."
}

function recipeCodeLabel(
  code: string,
  choices: RecipeFormOptions["dishCategories"] | undefined,
  fallbacks: Record<string, string>,
) {
  return choices?.find((choice) => choice.code === code)?.label ?? fallbacks[code] ?? code
}

export function AdminUserManagement() {
  return <AdminAccountManagementPage scope="users" />
}

export function AdminExpertManagement() {
  const { userId } = useParams()
  return (
    <AdminAccountManagementPage scope="experts" expertUserId={userId ?? null} />
  )
}

function AdminAccountManagementPage({
  scope,
  expertUserId = null,
}: {
  scope: "users" | "experts"
  expertUserId?: string | null
}) {
  const navigate = useNavigate()
  const [rows, setRows] = useState<AdminUser[]>([])

  const [summary, setSummary] = useState<AdminUserSummary | null>(null)

  const [expertSummaryFallback, setExpertSummaryFallback] =
    useState<ExpertSummaryFallback | null>(null)

  const [search, setSearch] = useState("")

  const [role, setRole] = useState("")

  const [status, setStatus] = useState("")

  const [page, setPage] = useState(0)

  const [totalPages, setTotalPages] = useState(0)

  const [totalElements, setTotalElements] = useState(0)

  const [loading, setLoading] = useState(true)

  const [error, setError] = useState<string | null>(null)

  const [revision, setRevision] = useState(0)

  const [selected, setSelected] = useState<AdminUser | null>(null)

  const [detail, setDetail] = useState<AdminUser | null>(null)

  const [history, setHistory] = useState<AdminUserAudit[]>([])

  const [historyPage, setHistoryPage] = useState(0)

  const [historyTotalPages, setHistoryTotalPages] = useState(0)

  const [historyError, setHistoryError] = useState<string | null>(null)

  const [mode, setMode] =
    useState<"VIEW" | "STATUS" | "ROLE" | "AI_CHOICE" | "AI_PACKAGE" | "AI_CREDIT" | null>(
      null,
    )

  const [reason, setReason] = useState("")

  const [packageReason, setPackageReason] = useState("")

  const [creditReason, setCreditReason] = useState("")

  const [nextRole, setNextRole] =
    useState<Exclude<AdminUserRole, "ADMIN">>("EXPERT")

  const [saving, setSaving] = useState(false)

  const [notice, setNotice] = useState<string | null>(null)

  const [aiTier, setAiTier] = useState<AdminAiPackageState | null>(null)

  const [nextAiTier, setNextAiTier] = useState<AdminAiTier>("FREE")

  const [creditAmount, setCreditAmount] = useState("")

  const [aiTierLoading, setAiTierLoading] = useState(false)

  const [aiError, setAiError] = useState<string | null>(null)

  const [aiNotice, setAiNotice] = useState<string | null>(null)

  const [expertDetail, setExpertDetail] = useState<AdminExpertDetail | null>(null)

  const [expertDetailLoading, setExpertDetailLoading] = useState(false)

  const [expertDetailError, setExpertDetailError] = useState<string | null>(null)

  const load = useCallback(
    async (signal?: AbortSignal) => {
      setLoading(true)

      setError(null)

      if (scope === "experts" && expertUserId) {
        setLoading(false)
        return
      }

      try {
        const params = {
          query: search.trim() || undefined,
          role: scope === "experts" ? "EXPERT" : role || undefined,
          status: status || undefined,
          page,
          size: PAGE_SIZE,
        }

        const [result, counts] = await Promise.all([
          listAdminUsers(params),
          getAdminUserSummary(),
        ])

        if (signal?.aborted) return

        setRows(result.content)

        setTotalPages(result.totalPages)

        setTotalElements(result.totalElements)

        setSummary(counts)

        if (
          scope === "experts" &&
          (counts.activeExperts == null ||
            counts.lockedExperts == null ||
            counts.expertPublishedRecipeCount == null)
        ) {
          try {
            const firstPage = await listAdminUsers({
              role: "EXPERT",
              page: 0,
              size: 100,
            })
            const remainingPages = await Promise.all(
              Array.from(
                { length: Math.max(0, firstPage.totalPages - 1) },
                (_, index) =>
                  listAdminUsers({
                    role: "EXPERT",
                    page: index + 1,
                    size: 100,
                  }),
              ),
            )
            if (signal?.aborted) return

            const allExperts = [firstPage, ...remainingPages].flatMap(
              (expertPage) => expertPage.content,
            )
            setExpertSummaryFallback({
              activeExperts: allExperts.filter(
                (user) => user.accountStatus === "ACTIVE",
              ).length,
              lockedExperts: allExperts.filter(
                (user) => user.accountStatus === "LOCKED",
              ).length,
              expertPublishedRecipeCount: allExperts.reduce(
                (total, user) => total + user.publishedRecipeCount,
                0,
              ),
            })
          } catch {
            if (!signal?.aborted) setExpertSummaryFallback(null)
          }
        } else if (scope === "experts") {
          setExpertSummaryFallback(null)
        }
      } catch (cause) {
        if (!signal?.aborted) setError(apiMessage(cause))
      } finally {
        if (!signal?.aborted) setLoading(false)
      }
    },
    [scope, expertUserId, search, role, status, page, revision],
  )

  useEffect(() => {
    const controller = new AbortController()

    const timer = window.setTimeout(() => void load(controller.signal), 250)

    return () => {
      window.clearTimeout(timer)
      controller.abort()
    }
  }, [load])

  useEffect(() => {
    if (scope !== "experts" || !expertUserId) {
      setExpertDetail(null)
      setExpertDetailError(null)
      setExpertDetailLoading(false)
      return
    }

    const id = Number(expertUserId)
    setExpertDetail(null)
    setExpertDetailError(null)
    setExpertDetailLoading(true)
    setMode(null)
    setSelected(null)

    if (!Number.isSafeInteger(id) || id < 1) {
      setExpertDetailError("Mã tài khoản Chuyên gia không hợp lệ.")
      setExpertDetailLoading(false)
      return
    }

    let cancelled = false
    void getAdminExpertDetail(id)
      .then((result) => {
        if (!cancelled) setExpertDetail(result)
      })
      .catch((cause) => {
        if (!cancelled) setExpertDetailError(apiMessage(cause))
      })
      .finally(() => {
        if (!cancelled) setExpertDetailLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [scope, expertUserId, revision])

  const reload = () => setRevision((value) => value + 1)

  const openDetail = async (user: AdminUser, requestedHistoryPage = 0) => {
    setSelected(user)

    setDetail(null)

    setHistory([])

    setHistoryPage(requestedHistoryPage)

    setHistoryError(null)

    setMode("VIEW")

    try {
      const [accountRes, eventsRes, tierRes] = await Promise.allSettled([
        getAdminUser(user.id),
        getAdminUserHistory(user.id, requestedHistoryPage),
        getAdminAiTier(user.id),
      ])

      if (accountRes.status === "fulfilled") setDetail(accountRes.value)

      if (eventsRes.status === "fulfilled") {
        setHistory(eventsRes.value.content)
        setHistoryTotalPages(eventsRes.value.totalPages)
      } else {
        setHistoryError(apiMessage(eventsRes.reason))
      }

      if (tierRes.status === "fulfilled") setAiTier(tierRes.value)
    } catch (cause) {
      setHistoryError(apiMessage(cause))
    }
  }

  const openAiManagement = async (user: AdminUser) => {
    setSelected(user)
    setMode("AI_CHOICE")
    setPackageReason("")
    setCreditReason("")
    setCreditAmount("")
    setAiError(null)
    setAiNotice(null)
  }

  const openPackageGift = async () => {
    if (!selected || selected.role === "ADMIN") return

    setMode("AI_PACKAGE")
    setPackageReason("")
    setAiError(null)
    setAiNotice(null)

    setAiTier(null)
    setAiTierLoading(true)

    try {
      const tier = await getAdminAiTier(selected.id)

      setAiTier(tier)
      setNextAiTier(tier.newTier)
    } catch (cause) {
      setAiError(apiMessage(cause))
    } finally {
      setAiTierLoading(false)
    }
  }

  const openCreditGrant = () => {
    setMode("AI_CREDIT")
    setCreditReason("")
    setCreditAmount("")
    setAiError(null)
    setAiNotice(null)
  }

  const saveAiTier = async () => {
    if (
      !selected ||
      !aiTier ||
      packageReason.trim().length < 10 ||
      packageReason.trim().length > 500 ||
      nextAiTier === aiTier.newTier
    )
      return

    setSaving(true)
    setAiError(null)

    try {
      const updated = await giftAdminAiPackage(
        selected.id,
        nextAiTier,
        packageReason.trim(),
      )

      setAiTier(updated)
      setPackageReason("")
      setAiNotice(
        `Đã tặng gói AI ${updated.newTier} cho ${selected.displayName}. Người nhận đã được thông báo.`,
      )

      const events = await getAdminUserHistory(selected.id)

      setHistory(events.content)
      setHistoryPage(0)
      setHistoryTotalPages(events.totalPages)

      reload()
    } catch (cause) {
      setAiError(apiMessage(cause))
    } finally {
      setSaving(false)
    }
  }

  const grantCredit = async () => {
    const amount = Number(creditAmount)

    if (
      !selected ||
      !Number.isFinite(amount) ||
      amount <= 0 ||
      creditReason.trim().length < 10 ||
      creditReason.trim().length > 500
    )
      return

    setSaving(true)
    setAiError(null)

    try {
      const result = await grantAdminAiCredit(
        selected.id,
        amount,
        creditReason.trim(),
      )

      setCreditAmount("")
      setCreditReason("")

      setAiNotice(
        `Đã cộng ${result.credits.toLocaleString("vi-VN")} AI Credit. Số dư mới: ${result.balance.toLocaleString("vi-VN")}. Giao dịch đã được lưu vào lịch sử tài khoản.`,
      )

      const events = await getAdminUserHistory(selected.id)

      setHistory(events.content)
      setHistoryPage(0)
      setHistoryTotalPages(events.totalPages)
    } catch (cause) {
      setAiError(apiMessage(cause))
    } finally {
      setSaving(false)
    }
  }

  const saveStatusOrRole = async () => {
    if (!selected || reason.trim().length < 10 || reason.trim().length > 500)
      return

    setSaving(true)

    try {
      const updated =
        mode === "STATUS"
          ? await updateAdminUserStatus(
              selected.id,
              selected.accountStatus === "ACTIVE" ? "LOCKED" : "ACTIVE",
              reason.trim(),
            )
          : await updateAdminUserRole(selected.id, nextRole, reason.trim())

      setSelected(updated)

      setMode(null)

      setReason("")

      if (scope === "experts" && updated.role !== "EXPERT") {
        navigate("/admin/nguoi-dung")
        return
      }

      if (scope === "experts") {
        setExpertDetail((current) =>
          current ? { ...current, account: updated } : current,
        )
      }

      setNotice(`Đã cập nhật tài khoản ${updated.displayName}.`)

      reload()

      window.setTimeout(() => setNotice(null), 3500)
    } catch (cause) {
      setNotice(apiMessage(cause))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="space-y-6 py-2">
      {expertUserId ? (
        <AdminExpertDetailPanel
          detail={expertDetail}
          loading={expertDetailLoading}
          error={expertDetailError}
          onRetry={reload}
          onBack={() => navigate("/admin/chuyen-gia")}
          onHistory={() => {
            if (expertDetail) void openDetail(expertDetail.account)
          }}
          onAiManagement={() => {
            if (expertDetail) void openAiManagement(expertDetail.account)
          }}
          onRoleChange={() => {
            if (!expertDetail) return
            setSelected(expertDetail.account)
            setNextRole("CUSTOMER")
            setReason("")
            setMode("ROLE")
          }}
          onStatusChange={() => {
            if (!expertDetail) return
            setSelected(expertDetail.account)
            setReason("")
            setMode("STATUS")
          }}
        />
      ) : (
        <>
      <AdminPageHeader
        title={scope === "experts" ? "Danh sách Chuyên gia" : "Quản lý tài khoản & phân quyền"}
        description={scope === "experts"
          ? "Tra cứu hồ sơ Expert, xếp theo ngày lên vai trò mới nhất. Tài khoản bị khóa vẫn được liệt kê."
          : "Quản lý tài khoản Customer, Expert và tra cứu tài khoản Admin ở chế độ chỉ xem."}
      >
        {scope === "experts" ? (
          <div className="grid w-full grid-cols-2 gap-4 lg:grid-cols-4">
            <SummaryCard
              label="Tổng Chuyên gia"
              value={summary?.expertUsers}
              icon={<UserCheck className="h-4 w-4" />}
            />
            <SummaryCard
              label="Đang hoạt động"
              value={summary?.activeExperts ?? expertSummaryFallback?.activeExperts}
              icon={<CheckCircle2 className="h-4 w-4" />}
            />
            <SummaryCard
              label="Đang bị khóa"
              value={summary?.lockedExperts ?? expertSummaryFallback?.lockedExperts}
              icon={<Lock className="h-4 w-4" />}
            />
            <SummaryCard
              label="Tổng công thức"
              value={summary?.expertPublishedRecipeCount ?? expertSummaryFallback?.expertPublishedRecipeCount}
              icon={<BookOpen className="h-4 w-4" />}
              caption="Đang công khai của Expert"
            />
          </div>
        ) : (
          <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
            <SummaryCard label="Tổng tài khoản" value={summary?.totalUsers} icon={<Users className="h-4 w-4" />} />
            <SummaryCard label="Đang hoạt động" value={summary?.activeUsers} icon={<CheckCircle2 className="h-4 w-4" />} />
            <SummaryCard label="Chuyên gia" value={summary?.expertUsers} icon={<UserCheck className="h-4 w-4" />} />
            <SummaryCard label="Đang bị khóa" value={summary?.lockedUsers} icon={<Lock className="h-4 w-4" />} />
          </div>
        )}
      </AdminPageHeader>

      {notice && (
        <div
          role="status"
          className="rounded-xl border border-leaf-200 bg-leaf-50 p-3 text-sm text-leaf-800"
        >
          {notice}
        </div>
      )}

      <Card className="p-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" />
            <input
              aria-label={scope === "experts" ? "Tìm chuyên gia" : "Tìm người dùng"}
              value={search}
              onChange={(event) => {
                setSearch(event.target.value)
                setPage(0)
              }}
              placeholder={scope === "experts"
                ? "Tìm theo tên, email hoặc mã chuyên gia..."
                : "Tìm theo họ tên, email hoặc mã tài khoản..."}
              className="w-full rounded-xl border border-brand-200 bg-paper py-2 pl-9 pr-3 text-xs text-ink focus:border-brand-500 focus:outline-none"
            />
          </div>
          <div className="flex gap-2">
            {scope === "users" && (
              <select
                aria-label="Lọc vai trò"
                value={role}
                onChange={(event) => {
                  setRole(event.target.value)
                  setPage(0)
                }}
                className="rounded-xl border border-brand-200 bg-paper px-3 py-2 text-xs text-ink focus:border-brand-500 focus:outline-none"
              >
                <option value="">Tất cả vai trò</option>
                <option value="CUSTOMER">Thành viên</option>
                <option value="EXPERT">Chuyên gia</option>
                <option value="ADMIN">Quản trị viên</option>
              </select>
            )}
            <select
              aria-label="Lọc trạng thái"
              value={status}
              onChange={(event) => {
                setStatus(event.target.value)
                setPage(0)
              }}
              className="rounded-xl border border-brand-200 bg-paper px-3 py-2 text-xs text-ink focus:border-brand-500 focus:outline-none"
            >
              <option value="">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="LOCKED">Đang bị khóa</option>
            </select>
          </div>
        </div>
      </Card>

      {error ? (
        <Card className="p-8 text-center">
          <ShieldAlert className="mx-auto h-8 w-8 text-red-600" />
          <p role="alert" className="mt-3 text-sm text-ink">
            {error}
          </p>
          <Button className="mt-4" size="sm" onClick={reload}>
            Thử lại
          </Button>
        </Card>
      ) : (
        <Card className="overflow-hidden">
          {loading && (
            <div
              role="status"
              className="border-b border-brand-100 px-4 py-2 text-xs text-ink-muted"
            >
              Đang tải dữ liệu tài khoản...
            </div>
          )}
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="border-b border-brand-100 bg-brand-50/50 text-ink-muted">
                <tr>
                  <th className="px-4 py-3">{scope === "experts" ? "Chuyên gia" : "Tài khoản"}</th>
                  <th className="px-4 py-3">Vai trò</th>
                  <th className="px-4 py-3">{scope === "experts" ? "Ngày lên Expert" : "Ngày tham gia"}</th>
                  <th className="px-4 py-3">Công thức</th>
                  <th className="px-4 py-3">Trạng thái</th>
                  <th className="px-4 py-3 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-100/60 text-ink">
                {!loading && rows.length === 0 ? (
                  <tr>
                    <td
                      colSpan={6}
                      className="px-4 py-8 text-center text-ink-muted"
                    >
                      {scope === "experts"
                        ? "Không tìm thấy Chuyên gia phù hợp."
                        : "Không tìm thấy tài khoản phù hợp."}
                    </td>
                  </tr>
                ) : (
                  rows.map((user) => (
                    <tr key={user.id} className="hover:bg-brand-50/30">
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-100 font-bold text-brand-700">
                            {user.displayName.charAt(0)}
                          </div>
                          <div>
                            <p className="font-bold">{user.displayName}</p>
                            <p className="text-[11px] text-ink-muted">
                              {user.email} · #{user.id}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="px-4 py-3">
                        {user.role === "EXPERT" ? (
                          <Badge tone="leaf">{roleLabels[user.role]}</Badge>
                        ) : user.role === "ADMIN" ? (
                          <Badge tone="brand">{roleLabels[user.role]}</Badge>
                        ) : (
                          <Badge tone="neutral">{roleLabels[user.role]}</Badge>
                        )}
                      </td>
                      <td className="px-4 py-3 text-ink-muted">
                        {scope === "experts"
                          ? user.expertSince
                            ? date(user.expertSince)
                            : null
                          : date(user.joinedAt)}
                      </td>
                      <td className="px-4 py-3 font-semibold">
                        {user.publishedRecipeCount} món
                      </td>
                      <td className="px-4 py-3">
                        {user.accountStatus === "ACTIVE" ? (
                          <span className="rounded-full bg-leaf-100 px-2 py-1 text-[11px] font-bold text-leaf-800">
                            ● Hoạt động
                          </span>
                        ) : (
                          <span className="rounded-full bg-red-100 px-2 py-1 text-[11px] font-bold text-red-800">
                            ● Đã khóa
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex justify-end gap-1">
                          {scope === "experts" ? (
                            <IconButton
                              title="Xem hồ sơ Chuyên gia"
                              onClick={() => navigate(`/admin/chuyen-gia/${user.id}`)}
                            >
                              <Eye className="h-4 w-4" />
                            </IconButton>
                          ) : (
                            <>
                              <IconButton
                                title="Xem chi tiết và lịch sử"
                                onClick={() => void openDetail(user)}
                              >
                                <Eye className="h-4 w-4" />
                              </IconButton>
                              {user.role !== "ADMIN" && (
                                <>
                                  <IconButton
                                    title="Tặng gói AI / Cộng AI Credit"
                                    onClick={() => void openAiManagement(user)}
                                  >
                                    <Gift className="h-4 w-4" />
                                  </IconButton>
                                  <IconButton
                                    title="Đổi vai trò"
                                    onClick={() => {
                                      setSelected(user)
                                      setNextRole(
                                        user.role === "CUSTOMER"
                                          ? "EXPERT"
                                          : "CUSTOMER",
                                      )
                                      setReason("")
                                      setMode("ROLE")
                                    }}
                                  >
                                    <UserCheck className="h-4 w-4" />
                                  </IconButton>
                                  <IconButton
                                    title={
                                      user.accountStatus === "ACTIVE"
                                        ? "Khóa tài khoản"
                                        : "Mở khóa tài khoản"
                                    }
                                    onClick={() => {
                                      setSelected(user)
                                      setReason("")
                                      setMode("STATUS")
                                    }}
                                  >
                                    {user.accountStatus === "ACTIVE" ? (
                                      <Lock className="h-4 w-4" />
                                    ) : (
                                      <Unlock className="h-4 w-4" />
                                    )}
                                  </IconButton>
                                </>
                              )}
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-brand-100 px-4 py-3 text-xs">
            <span className="text-ink-muted">
              {totalElements} {scope === "experts" ? "chuyên gia" : "tài khoản"} · Trang{" "}
              {totalPages === 0 ? 0 : page + 1}/{totalPages}
            </span>
            <div className="flex gap-2">
              <Button
                size="sm"
                variant="outline"
                disabled={page === 0 || loading}
                onClick={() => setPage((value) => Math.max(0, value - 1))}
              >
                <ChevronLeft className="h-4 w-4" />
                Trước
              </Button>
              <Button
                size="sm"
                variant="outline"
                disabled={page + 1 >= totalPages || loading}
                onClick={() => setPage((value) => value + 1)}
              >
                Sau
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        </Card>
      )}
        </>
      )}

      {mode && selected && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !saving) setMode(null)
          }}
        >
          <Card
            role="dialog"
            aria-modal="true"
            aria-labelledby="admin-user-modal-title"
            className="max-h-[90vh] w-full max-w-xl overflow-y-auto p-5 shadow-xl"
          >
            <div className="flex items-start justify-between border-b border-brand-100 pb-3">
              <div>
                <h2 id="admin-user-modal-title" className="font-bold text-ink">
                  {mode === "VIEW"
                    ? "Chi tiết tài khoản"
                    : mode === "ROLE"
                      ? "Đổi vai trò tài khoản"
                      : mode === "AI_CHOICE"
                        ? "Chọn thao tác AI"
                        : mode === "AI_PACKAGE"
                          ? "Tặng gói AI"
                          : mode === "AI_CREDIT"
                            ? "Tặng AI Credit"
                            : selected.accountStatus === "ACTIVE"
                              ? "Khóa tài khoản"
                              : "Mở khóa tài khoản"}
                </h2>
                <p className="mt-1 text-xs text-ink-muted">
                  {selected.displayName} · {selected.email}
                </p>
              </div>
              <button
                aria-label="Đóng"
                onClick={() => setMode(null)}
                className="rounded p-1 text-ink-muted hover:bg-brand-100"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            {mode === "VIEW" ? (
              <div className="space-y-4 pt-4 text-xs">
                {!detail && !historyError ? (
                  <p role="status">Đang tải hồ sơ và lịch sử...</p>
                ) : (
                  detail && (
                    <div className="grid grid-cols-2 gap-3 rounded-xl bg-brand-50/50 p-3">
                      <div>
                        <span className="text-ink-muted">Mã tài khoản</span>
                        <p className="font-bold">#{detail.id}</p>
                      </div>
                      <div>
                        <span className="text-ink-muted">Vai trò</span>
                        <p className="font-bold">{roleLabels[detail.role]}</p>
                      </div>
                      <div>
                        <span className="text-ink-muted">Trạng thái</span>
                        <p className="font-bold">
                          {detail.accountStatus === "ACTIVE"
                            ? "Đang hoạt động"
                            : "Đã khóa"}
                        </p>
                      </div>
                      <div>
                        <span className="text-ink-muted">
                          Công thức công khai
                        </span>
                        <p className="font-bold">
                          {detail.publishedRecipeCount}
                        </p>
                      </div>
                      <div>
                        <span className="text-ink-muted">Gói AI</span>
                        <p className="font-bold">{aiTier?.newTier ?? "—"}</p>
                      </div>
                      <div>
                        <span className="text-ink-muted">Hết hạn</span>
                        <p className="font-bold">
                          {aiTier?.endsAt
                            ? date(aiTier.endsAt)
                            : "Theo tháng dương lịch / không áp dụng"}
                        </p>
                      </div>
                    </div>
                  )
                )}
                <h3 className="font-bold">Lịch sử tài khoản</h3>
                {historyError ? (
                  <div>
                    <p role="alert">{historyError}</p>
                    <Button
                      size="sm"
                      variant="outline"
                      onClick={() => void openDetail(selected, historyPage)}
                    >
                      Thử lại
                    </Button>
                  </div>
                ) : history.length === 0 ? (
                  <p className="text-ink-muted">Chưa có lịch sử thay đổi.</p>
                ) : (
                  <>
                    <ol className="space-y-2">
                      {history.map((event) => (
                        <li
                          key={event.eventId}
                          className="rounded-lg border border-brand-100 p-3"
                        >
                          <div className="flex justify-between gap-2">
                            <strong>
                              {event.action === "ROLE_CHANGED"
                                ? "Đổi vai trò"
                                : event.action === "AI_PACKAGE_GIFTED"
                                  ? "Tặng gói AI"
                                  : event.action === "AI_CREDIT_GRANTED"
                                    ? "Tặng AI Credit"
                                    : "Đổi trạng thái"}
                            </strong>
                            <time className="text-ink-muted">
                              {date(event.occurredAt)}
                            </time>
                          </div>
                          <p className="mt-1">
                            {event.action === "AI_CREDIT_GRANTED"
                              ? `+${event.oldValue} AI Credit · Số dư sau giao dịch: ${event.newValue}`
                              : `${event.oldValue} → ${event.newValue}`}
                          </p>
                          {event.effectiveUntil && (
                            <p className="mt-1">
                              Có hiệu lực đến {date(event.effectiveUntil)}
                            </p>
                          )}
                          <p className="mt-1 text-ink-muted">
                            Admin:{" "}
                            {event.administratorName ??
                              `#${event.administratorId ?? "?"}`}{" "}
                            · {event.reason}
                          </p>
                        </li>
                      ))}
                    </ol>
                    <div className="flex items-center justify-between">
                      <span className="text-ink-muted">
                        Trang lịch sử {historyPage + 1}/{historyTotalPages}
                      </span>
                      <div className="flex gap-2">
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={historyPage === 0}
                          onClick={() =>
                            void openDetail(selected, historyPage - 1)
                          }
                        >
                          Trước
                        </Button>
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={historyPage + 1 >= historyTotalPages}
                          onClick={() =>
                            void openDetail(selected, historyPage + 1)
                          }
                        >
                          Sau
                        </Button>
                      </div>
                    </div>
                  </>
                )}
                <Button
                  size="sm"
                  variant="outline"
                  className="w-full"
                  onClick={() => setMode(null)}
                >
                  Đóng
                </Button>
              </div>
            ) : mode === "AI_CHOICE" ? (
              <div className="space-y-4 pt-4 text-xs">
                <p className="text-ink-soft">
                  Chọn một thao tác. Mỗi loại có biểu mẫu và lý do riêng. Hoàn
                  tất một thao tác không yêu cầu thực hiện thao tác còn lại.
                </p>
                <div className="grid gap-3 sm:grid-cols-2">
                  {selected.role !== "ADMIN" && (
                    <button
                      type="button"
                      onClick={() => void openPackageGift()}
                      className="flex min-h-32 flex-col items-start gap-2 rounded-xl border border-brand-200 bg-paper p-4 text-left transition-colors hover:border-brand-500 hover:bg-brand-50 focus:outline-none focus:ring-2 focus:ring-brand-500"
                    >
                      <Gift className="h-5 w-5 text-brand-700" />
                      <strong>Tặng gói AI</strong>
                      <span className="text-ink-muted">
                        Chọn Free, Plus hoặc Pro. Gói áp dụng ngay và được lưu
                        vào lịch sử tài khoản.
                      </span>
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={openCreditGrant}
                    className="flex min-h-32 flex-col items-start gap-2 rounded-xl border border-brand-200 bg-paper p-4 text-left transition-colors hover:border-brand-500 hover:bg-brand-50 focus:outline-none focus:ring-2 focus:ring-brand-500"
                  >
                    <Coins className="h-5 w-5 text-brand-700" />
                    <strong>Tặng AI Credit</strong>
                    <span className="text-ink-muted">
                      Cộng Credit độc lập. Giao dịch được lưu vào Credit ledger
                      và lịch sử tài khoản.
                    </span>
                  </button>
                </div>
                <div className="flex justify-end border-t border-brand-100 pt-3">
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setMode(null)}
                  >
                    Đóng
                  </Button>
                </div>
              </div>
            ) : mode === "AI_PACKAGE" ? (
              <div className="space-y-4 pt-4 text-xs">
                {aiError && (
                  <p
                    role="alert"
                    className="rounded-lg bg-red-50 p-3 text-red-800"
                  >
                    {aiError}
                  </p>
                )}
                {aiNotice && (
                  <p
                    role="status"
                    className="rounded-lg bg-leaf-50 p-3 text-leaf-800"
                  >
                    {aiNotice}
                  </p>
                )}
                {aiTierLoading ? (
                  <p role="status">Đang tải gói AI hiện tại...</p>
                ) : (
                  aiTier && (
                    <>
                      <div className="rounded-xl bg-brand-50/50 p-3">
                        <span className="text-ink-muted">Gói hiện tại</span>
                        <p className="font-bold">
                          {aiTier.newTier} ·{" "}
                          {aiTier.tokenQuota.toLocaleString("vi-VN")} token
                        </p>
                        <p className="text-ink-muted">
                          {aiTier.endsAt
                            ? `Hết hạn ${date(aiTier.endsAt)}`
                            : "Free theo tháng dương lịch"}
                        </p>
                      </div>
                      <label className="block font-bold">
                        Gói muốn tặng
                        <select
                          aria-label="Gói AI muốn tặng"
                          value={nextAiTier}
                          onChange={(event) =>
                            setNextAiTier(event.target.value as AdminAiTier)
                          }
                          className="mt-1 w-full rounded-xl border border-brand-200 bg-paper p-2.5 text-xs font-normal text-ink focus:border-brand-500 focus:outline-none"
                        >
                          <option value="FREE">Free</option>
                          <option value="PLUS">Plus</option>
                          <option value="PRO">Pro</option>
                        </select>
                      </label>
                      <p className="text-ink-muted">
                        Plus/Pro bắt đầu chu kỳ 30 ngày mới. Không tạo payment
                        order hoặc hoàn tiền. Người nhận được thông báo in-app.
                      </p>
                      <label className="block font-bold">
                        Lý do tặng gói (10–500 ký tự)
                        <textarea
                          aria-label="Lý do tặng gói AI"
                          rows={3}
                          maxLength={500}
                          value={packageReason}
                          onChange={(event) =>
                            setPackageReason(event.target.value)
                          }
                          className="mt-1 w-full rounded-lg border border-brand-200 bg-paper p-2 font-normal"
                          placeholder="Ghi rõ lý do tặng gói AI..."
                        />
                      </label>
                      <p className="text-ink-muted">
                        {packageReason.trim().length}/500 ký tự
                      </p>
                      <div className="flex flex-wrap justify-between gap-2 border-t border-brand-100 pt-3">
                        <Button
                          size="sm"
                          variant="outline"
                          disabled={saving}
                          onClick={() => setMode("AI_CHOICE")}
                        >
                          Quay lại chọn thao tác
                        </Button>
                        <Button
                          size="sm"
                          disabled={
                            saving ||
                            aiTier.newTier === nextAiTier ||
                            packageReason.trim().length < 10 ||
                            packageReason.trim().length > 500
                          }
                          onClick={() => void saveAiTier()}
                        >
                          {saving ? "Đang xử lý..." : "Xác nhận tặng gói"}
                        </Button>
                      </div>
                    </>
                  )
                )}
              </div>
            ) : mode === "AI_CREDIT" ? (
              <div className="space-y-4 pt-4 text-xs">
                {aiError && (
                  <p
                    role="alert"
                    className="rounded-lg bg-red-50 p-3 text-red-800"
                  >
                    {aiError}
                  </p>
                )}
                {aiNotice && (
                  <p
                    role="status"
                    className="rounded-lg bg-leaf-50 p-3 text-leaf-800"
                  >
                    {aiNotice}
                  </p>
                )}
                <label className="block font-bold">
                  Số AI Credit
                  <input
                    aria-label="Số AI Credit muốn tặng"
                    type="number"
                    min="0.000000001"
                    step="any"
                    value={creditAmount}
                    onChange={(event) => setCreditAmount(event.target.value)}
                    className="mt-1 w-full rounded-lg border border-brand-200 bg-paper p-2 font-normal"
                    placeholder="Ví dụ: 1000"
                  />
                </label>
                <label className="block font-bold">
                  Lý do tặng AI Credit (10–500 ký tự)
                  <textarea
                    aria-label="Lý do tặng AI Credit"
                    rows={3}
                    maxLength={500}
                    value={creditReason}
                    onChange={(event) => setCreditReason(event.target.value)}
                    className="mt-1 w-full rounded-lg border border-brand-200 bg-paper p-2 font-normal"
                    placeholder="Ghi rõ lý do tặng AI Credit..."
                  />
                </label>
                <p className="text-ink-muted">
                  {creditReason.trim().length}/500 ký tự
                </p>
                <div className="flex flex-wrap justify-between gap-2 border-t border-brand-100 pt-3">
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={saving}
                    onClick={() => setMode("AI_CHOICE")}
                  >
                    Quay lại chọn thao tác
                  </Button>
                  <Button
                    size="sm"
                    disabled={
                      saving ||
                      !Number.isFinite(Number(creditAmount)) ||
                      Number(creditAmount) <= 0 ||
                      creditReason.trim().length < 10 ||
                      creditReason.trim().length > 500
                    }
                    onClick={() => void grantCredit()}
                  >
                    {saving ? "Đang xử lý..." : "Xác nhận tặng AI Credit"}
                  </Button>
                </div>
              </div>
            ) : (
              <div className="space-y-4 pt-4 text-xs">
                <p className="text-ink-soft">
                  {mode === "ROLE"
                    ? "Chỉ có thể chuyển giữa Thành viên và Chuyên gia. Nếu cấp Chuyên gia khi đang có đơn chờ, đơn sẽ được kết thúc và người dùng nhận thông báo."
                    : selected.accountStatus === "ACTIVE"
                      ? "Nếu khóa tài khoản này, người dùng sẽ không thể đăng nhập hoặc tiếp tục sử dụng tài khoản cho đến khi quản trị viên mở khóa. Bạn có chắc muốn tiếp tục không?"
                      : "Mở khóa để người dùng có thể đăng nhập và sử dụng lại tài khoản."}
                </p>
                {mode === "ROLE" && (
                  <label className="block font-bold">
                    Vai trò mới
                    <select
                      aria-label="Vai trò mới"
                      value={nextRole}
                      onChange={(event) =>
                        setNextRole(
                          event.target.value as Exclude<AdminUserRole, "ADMIN">,
                        )
                      }
                      className="mt-1 w-full rounded-xl border border-brand-200 bg-paper p-2.5 text-xs font-normal text-ink focus:border-brand-500 focus:outline-none"
                    >
                      <option value="CUSTOMER">Thành viên (CUSTOMER)</option>
                      <option value="EXPERT">Chuyên gia (EXPERT)</option>
                    </select>
                  </label>
                )}
                <label className="block font-bold">
                  Lý do bắt buộc (10–500 ký tự)
                  <textarea
                    aria-label="Lý do thay đổi"
                    rows={4}
                    maxLength={500}
                    value={reason}
                    onChange={(event) => setReason(event.target.value)}
                    className="mt-1 w-full rounded-lg border border-brand-200 bg-paper p-2 font-normal"
                    placeholder="Ghi rõ lý do thay đổi tài khoản..."
                  />
                </label>
                <p className="text-ink-muted">
                  {reason.trim().length}/500 ký tự
                </p>
                <div className="flex justify-end gap-2 border-t border-brand-100 pt-3">
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={saving}
                    onClick={() => setMode(null)}
                  >
                    Hủy
                  </Button>
                  <Button
                    size="sm"
                    disabled={
                      saving ||
                      reason.trim().length < 10 ||
                      reason.trim().length > 500 ||
                      (mode === "ROLE" && selected.role === nextRole)
                    }
                    onClick={() => void saveStatusOrRole()}
                  >
                    {saving ? "Đang lưu..." : "Xác nhận"}
                  </Button>
                </div>
              </div>
            )}
          </Card>
        </div>
      )}
    </div>
  )
}

function SummaryCard({
  label,
  value,
  icon,
  caption = "Toàn hệ thống",
  accent = "from-brand-500 via-brand-400 to-transparent",
  iconClass = "bg-brand-50 text-brand-700 ring-1 ring-brand-200/70",
}: {
  label: string
  value?: number
  icon: React.ReactNode
  caption?: string
  accent?: string
  iconClass?: string
}) {
  return (
    <Card className="group relative flex flex-col justify-between overflow-hidden border-brand-200/80 bg-paper p-4.5 transition-all duration-300 hover:-translate-y-1 hover:border-brand-300 hover:shadow-lg hover:shadow-brand-900/5">
      <div
        className={"absolute inset-x-0 top-0 h-1 bg-gradient-to-r " + accent}
      />
      <div>
        <div className="flex items-start justify-between gap-2">
          <span className="text-[11px] font-bold uppercase tracking-wider text-ink-muted">
            {label}
          </span>
          <span
            className={
              "flex h-8 w-8 shrink-0 items-center justify-center rounded-xl shadow-2xs transition-transform duration-200 group-hover:scale-110 " +
              iconClass
            }
          >
            {icon}
          </span>
        </div>
        <p className="mt-2.5 text-2xl font-black tracking-tight text-ink sm:text-3xl">
          {value != null ? <AnimatedNumber value={value} /> : "—"}
        </p>
      </div>
      <div className="mt-3 flex items-center justify-between border-t border-brand-100/60 pt-2 text-[11px] text-ink-muted">
        <span>{caption}</span>
        <span className="h-1.5 w-1.5 rounded-full bg-brand-400/80" />
      </div>
    </Card>
  )
}

function AdminExpertDetailPanel({
  detail,
  loading,
  error,
  onRetry,
  onBack,
  onHistory,
  onAiManagement,
  onRoleChange,
  onStatusChange,
}: {
  detail: AdminExpertDetail | null
  loading: boolean
  error: string | null
  onRetry: () => void
  onBack: () => void
  onHistory: () => void
  onAiManagement: () => void
  onRoleChange: () => void
  onStatusChange: () => void
}) {
  if (loading) {
    return (
      <Card className="p-8 text-center text-sm text-ink-muted" role="status">
        Đang tải hồ sơ Chuyên gia...
      </Card>
    )
  }

  if (error || !detail) {
    return (
      <Card className="p-8 text-center">
        <ShieldAlert className="mx-auto h-8 w-8 text-red-600" />
        <p role="alert" className="mt-3 text-sm text-ink">
          {error ?? "Không tìm thấy tài khoản Chuyên gia."}
        </p>
        <div className="mt-4 flex justify-center gap-2">
          <Button size="sm" variant="outline" onClick={onBack}>
            Quay lại danh sách
          </Button>
          <Button size="sm" onClick={onRetry}>
            Thử lại
          </Button>
        </div>
      </Card>
    )
  }

  const { account, approvedApplication } = detail

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <Button size="sm" variant="outline" onClick={onBack}>
          <ChevronLeft className="h-4 w-4" />
          Danh sách Chuyên gia
        </Button>
        <div className="flex flex-wrap gap-2">
          <Button size="sm" variant="outline" onClick={onHistory}>
            <Eye className="h-4 w-4" />
            Lịch sử tài khoản
          </Button>
          <Button size="sm" variant="outline" onClick={onAiManagement}>
            <Coins className="h-4 w-4" />
            Quản lý AI
          </Button>
          <Button size="sm" variant="outline" onClick={onRoleChange}>
            <UserCheck className="h-4 w-4" />
            Chuyển về Customer
          </Button>
          <Button size="sm" onClick={onStatusChange}>
            {account.accountStatus === "ACTIVE" ? (
              <Lock className="h-4 w-4" />
            ) : (
              <Unlock className="h-4 w-4" />
            )}
            {account.accountStatus === "ACTIVE" ? "Khóa tài khoản" : "Mở khóa"}
          </Button>
        </div>
      </div>

      <AdminPageHeader
        title="Hồ sơ Chuyên gia"
        description="Thông tin tài khoản, hồ sơ công khai hiện tại và dữ liệu từ đơn đăng ký đã được duyệt gần nhất."
      />

      <Card className="p-5 sm:p-6">
        <div className="flex flex-wrap items-start gap-4">
          {detail.avatarUrl ? (
            <img
              src={detail.avatarUrl}
              alt={`Ảnh đại diện ${account.displayName}`}
              className="h-16 w-16 rounded-full object-cover ring-2 ring-brand-100"
            />
          ) : (
            <div
              aria-hidden="true"
              className="flex h-16 w-16 items-center justify-center rounded-full bg-brand-100 text-xl font-bold text-brand-700"
            >
              {account.displayName.charAt(0)}
            </div>
          )}
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-xl font-bold text-ink">{account.displayName}</h2>
              <Badge tone={account.accountStatus === "ACTIVE" ? "leaf" : "neutral"}>
                {account.accountStatus === "ACTIVE" ? "Đang hoạt động" : "Đã khóa"}
              </Badge>
              <Badge tone="brand">Chuyên gia</Badge>
            </div>
            <p className="mt-1 break-all text-sm text-ink-muted">
              {account.email} · Mã tài khoản #{account.id}
            </p>
            {detail.bio ? (
              <p className="mt-3 whitespace-pre-wrap text-sm text-ink-soft">{detail.bio}</p>
            ) : (
              <p className="mt-3 text-sm italic text-ink-muted">
                Chuyên gia chưa thêm giới thiệu vào hồ sơ công khai.
              </p>
            )}
          </div>
        </div>
        <dl
          className={`mt-5 grid gap-3 border-t border-brand-100 pt-4 sm:grid-cols-2 ${
            account.expertSince ? "lg:grid-cols-4" : "lg:grid-cols-3"
          }`}
        >
          <div>
            <dt className="text-xs text-ink-muted">Ngày tham gia</dt>
            <dd className="mt-1 text-sm font-semibold text-ink">{date(account.joinedAt)}</dd>
          </div>
          {account.expertSince && (
            <div>
              <dt className="text-xs text-ink-muted">Ngày lên Expert</dt>
              <dd className="mt-1 text-sm font-semibold text-ink">{date(account.expertSince)}</dd>
            </div>
          )}
          <div>
            <dt className="text-xs text-ink-muted">Công thức đã công khai</dt>
            <dd className="mt-1 text-sm font-semibold text-ink">{account.publishedRecipeCount} món</dd>
          </div>
          <div>
            <dt className="text-xs text-ink-muted">Trạng thái tài khoản</dt>
            <dd className="mt-1 text-sm font-semibold text-ink">
              {account.accountStatus === "ACTIVE" ? "Đang hoạt động" : "Đã khóa"}
            </dd>
          </div>
        </dl>
      </Card>

      <Card className="p-5 sm:p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-lg font-bold text-ink">Đơn đăng ký Chuyên gia đã duyệt gần nhất</h2>
            <p className="mt-1 text-xs text-ink-muted">
              Nội dung này giúp xem căn cứ đăng ký. Tệp riêng tư vẫn được mở trong màn hình xét duyệt.
            </p>
          </div>
          {approvedApplication && (
            <Badge tone="leaf">Đã duyệt · #{approvedApplication.id}</Badge>
          )}
        </div>

        {!approvedApplication ? (
          <div className="mt-4 rounded-xl border border-brand-100 bg-brand-50/40 p-4 text-sm text-ink-soft">
            Không có đơn đã duyệt gắn với tài khoản này. Tài khoản có thể đã được cấp vai trò Chuyên gia trực tiếp.
          </div>
        ) : (
          <>
            <dl className="mt-4 grid gap-4 sm:grid-cols-2">
              <div>
                <dt className="text-xs text-ink-muted">Kinh nghiệm</dt>
                <dd className="mt-1 whitespace-pre-wrap text-sm text-ink">{approvedApplication.experience}</dd>
              </div>
              <div>
                <dt className="text-xs text-ink-muted">Trường phái ăn chay</dt>
                <dd className="mt-1 whitespace-pre-wrap text-sm text-ink">{approvedApplication.vegetarianType}</dd>
              </div>
              <div className="sm:col-span-2">
                <dt className="text-xs text-ink-muted">Món ăn mẫu / nội dung thực hành</dt>
                <dd className="mt-1 whitespace-pre-wrap text-sm text-ink">{approvedApplication.sampleRecipeSummary}</dd>
              </div>
              <div>
                <dt className="text-xs text-ink-muted">Ngày gửi đơn</dt>
                <dd className="mt-1 text-sm text-ink">{date(approvedApplication.submittedAt)}</dd>
              </div>
              <div>
                <dt className="text-xs text-ink-muted">Ngày duyệt</dt>
                <dd className="mt-1 text-sm text-ink">
                  {approvedApplication.reviewedAt ? date(approvedApplication.reviewedAt) : "—"}
                </dd>
              </div>
              {approvedApplication.adminNote && (
                <div className="sm:col-span-2">
                  <dt className="text-xs text-ink-muted">Ghi chú xét duyệt</dt>
                  <dd className="mt-1 whitespace-pre-wrap text-sm text-ink">
                    {approvedApplication.adminNote}
                  </dd>
                </div>
              )}
              {approvedApplication.portfolioUrl && (
                <div className="sm:col-span-2">
                  <dt className="text-xs text-ink-muted">Portfolio</dt>
                  <dd className="mt-1">
                    <a
                      href={approvedApplication.portfolioUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex items-center gap-1 break-all text-sm font-semibold text-brand-700 underline"
                    >
                      {approvedApplication.portfolioUrl}
                      <ExternalLink className="h-3.5 w-3.5 shrink-0" />
                    </a>
                  </dd>
                </div>
              )}
            </dl>
            <div className="mt-5 flex flex-wrap items-center justify-between gap-3 border-t border-brand-100 pt-4">
              <p className="text-xs text-ink-muted">
                {approvedApplication.certificateAvailable || approvedApplication.applicationAvatarAvailable
                  ? "Đơn có tệp đính kèm riêng tư."
                  : "Đơn không có tệp đính kèm."} Tệp chỉ xem trong quy trình xét duyệt.
              </p>
              <Link
                to={`/admin/xet-duyet-chuyen-gia?applicationId=${approvedApplication.id}`}
                className="inline-flex items-center gap-2 rounded-xl border border-brand-200 px-3 py-2 text-xs font-semibold text-brand-800 transition-colors hover:bg-brand-50"
              >
                Mở đơn trong xét duyệt
                <ExternalLink className="h-3.5 w-3.5" />
              </Link>
            </div>
          </>
        )}
      </Card>

      <AdminExpertPublishedRecipes userId={account.id} />
    </div>
  )
}

function AdminExpertPublishedRecipes({ userId }: { userId: number }) {
  const [result, setResult] = useState<RecipeSearchResult | null>(null)
  const [formOptions, setFormOptions] = useState<RecipeFormOptions | null>(null)
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [revision, setRevision] = useState(0)

  useEffect(() => {
    let cancelled = false
    void recipesApi
      .getFormOptions()
      .then((options) => {
        if (!cancelled) setFormOptions(options)
      })
      .catch(() => {
        if (!cancelled) setFormOptions(null)
      })

    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    void membersApi
      .getRecipes(userId, page, EXPERT_RECIPE_PAGE_SIZE)
      .then((recipes) => {
        if (cancelled) return
        if (recipes.totalPages > 0 && page >= recipes.totalPages) {
          setPage(recipes.totalPages - 1)
          return
        }
        if (recipes.totalPages === 0 && page !== 0) {
          setPage(0)
          return
        }
        setResult(recipes)
      })
      .catch((cause) => {
        if (!cancelled) setError(apiMessage(cause))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [userId, page, revision])

  return (
    <Card className="overflow-hidden">
      <div className="flex flex-wrap items-start justify-between gap-3 p-5 sm:p-6">
        <div>
          <h2 className="text-lg font-bold text-ink">Công thức đang công khai</h2>
          <p className="mt-1 text-xs text-ink-muted">
            Sắp xếp theo ngày đăng gần nhất đến xa nhất. Chọn tiêu đề để mở bài viết trong tab mới.
          </p>
        </div>
        {result && (
          <span className="rounded-full bg-brand-50 px-3 py-1 text-xs font-semibold text-brand-800">
            {result.totalElements.toLocaleString("vi-VN")} công thức
          </span>
        )}
      </div>

      {loading ? (
        <div className="border-t border-brand-100 px-5 py-6 text-center text-sm text-ink-muted" role="status">
          Đang tải công thức...
        </div>
      ) : error ? (
        <div className="border-t border-brand-100 p-5 text-center">
          <p className="text-sm text-red-700" role="alert">{error}</p>
          <Button
            className="mt-3"
            size="sm"
            variant="outline"
            onClick={() => setRevision((value) => value + 1)}
          >
            Thử tải lại
          </Button>
        </div>
      ) : !result || result.items.length === 0 ? (
        <p className="border-t border-brand-100 px-5 py-6 text-center text-sm text-ink-muted">
          Chuyên gia chưa có công thức công khai.
        </p>
      ) : (
        <>
          <div className="overflow-x-auto border-t border-brand-100">
            <table className="w-full min-w-[38rem] table-fixed text-left text-xs">
              <colgroup>
                <col className="w-[50%]" />
                <col className="w-[20%]" />
                <col className="w-[20%]" />
                <col className="w-[10%]" />
              </colgroup>
              <thead className="bg-brand-50/50 text-ink-muted">
                <tr>
                  <th scope="col" className="px-5 py-3 pl-7">Công thức</th>
                  <th scope="col" className="px-5 py-3">Thể loại</th>
                  <th scope="col" className="px-5 py-3">Loại ăn chay</th>
                  <th scope="col" className="px-5 py-3 pl-7">Lượt xem</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-100/60 text-ink">
                {result.items.map((recipe) => (
                  <tr key={recipe.id} className="hover:bg-brand-50/30">
                    <td className="px-5 py-3 pl-7">
                      <Link
                        to={`/cong-thuc/id/${recipe.id}`}
                        target="_blank"
                        rel="noreferrer"
                        aria-label={`Mở công thức ${recipe.title} trong tab mới`}
                        className="flex items-center justify-start gap-1.5 font-semibold text-brand-700 hover:underline"
                      >
                        <span className="line-clamp-2 min-w-0">{recipe.title}</span>
                        <ExternalLink className="h-3.5 w-3.5 shrink-0" aria-hidden="true" />
                      </Link>
                    </td>
                    <td className="px-5 py-3 text-ink-muted">
                      {recipeCodeLabel(recipe.dishCategory, formOptions?.dishCategories, recipeCategoryFallbacks)}
                    </td>
                    <td className="px-5 py-3 text-ink-muted">
                      {recipeCodeLabel(recipe.vegetarianType, formOptions?.vegetarianTypes, vegetarianTypeFallbacks)}
                    </td>
                    <td className="px-5 py-3 pl-7 tabular-nums text-ink-muted">
                      {recipe.viewCount.toLocaleString("vi-VN")}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {result.totalPages > 1 && (
            <div className="flex items-center justify-between gap-3 border-t border-brand-100 px-5 py-3 text-xs">
              <span className="text-ink-muted">
                Trang {page + 1}/{result.totalPages}
              </span>
              <div className="flex gap-2">
                <Button
                  size="sm"
                  variant="outline"
                  disabled={page === 0 || loading}
                  onClick={() => setPage((value) => Math.max(0, value - 1))}
                >
                  <ChevronLeft className="h-4 w-4" />
                  Trước
                </Button>
                <Button
                  size="sm"
                  variant="outline"
                  disabled={page + 1 >= result.totalPages || loading}
                  onClick={() => setPage((value) => value + 1)}
                >
                  Sau
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </>
      )}
    </Card>
  )
}

function IconButton({
  title,
  onClick,
  children,
}: {
  title: string
  onClick: () => void
  children: React.ReactNode
}) {
  return (
    <button
      type="button"
      title={title}
      aria-label={title}
      onClick={onClick}
      className="rounded-lg p-1.5 text-ink-muted transition-colors hover:bg-brand-100 hover:text-brand-700"
    >
      {children}
    </button>
  )
}
