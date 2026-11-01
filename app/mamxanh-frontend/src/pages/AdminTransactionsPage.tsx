import { useEffect, useRef, useState, type FormEvent } from "react"

import { Download, RefreshCw, Search, X } from "lucide-react"

import { Card, Badge, Button } from "../components/ui"

import { useAuth } from "../components/AuthContext"

import {
  adminTransactionsApi,
  type AdminTransaction,
  type TransactionFilters,
  type TransactionReport,
  type TransactionState,
} from "../api/adminTransactions"

import { asApiError, asApiErrorAsync } from "../lib/apiClient"

const initial: TransactionFilters = {
  query: "",

  tier: "",

  status: "",

  from: "",

  to: "",
}

const statuses: Record<TransactionState, string> = {
  PAID: "Thành công",

  PENDING: "Chờ thanh toán",

  FAILED: "Thất bại",

  CANCELLED: "Đã hủy",
  NEEDS_RECONCILIATION: "Cần đối soát",
}

const currency = new Intl.NumberFormat("vi-VN", {
  style: "currency",

  currency: "VND",

  maximumFractionDigits: 0,
})

const date = new Intl.DateTimeFormat("vi-VN", {
  timeZone: "Asia/Ho_Chi_Minh",

  year: "numeric",

  month: "2-digit",

  day: "2-digit",

  hour: "2-digit",

  minute: "2-digit",
})

const time = (value: string | null) =>
  value ? date.format(new Date(value)) : "Chưa có"

const input =
  "min-h-11 w-full rounded-xl border border-brand-200 bg-paper px-3 text-sm text-ink focus-visible:outline-2 focus-visible:outline-brand-600"

function State({ value }: { value: TransactionState }) {
  return (
    <Badge
            tone={value === "PAID" ? "leaf" : value === "PENDING" ? "amber" : value === "NEEDS_RECONCILIATION" ? "brand" : "neutral"}
    >
      {statuses[value]}
    </Badge>
  )
}

export function AdminTransactionsPage() {
  const { account } = useAuth()

  const [draft, setDraft] = useState<TransactionFilters>(initial)

  const [filters, setFilters] = useState<TransactionFilters>(initial)

  const [page, setPage] = useState(0)

  const [reload, setReload] = useState(0)

  const [report, setReport] = useState<TransactionReport | null>(null)

  const [error, setError] = useState("")

  const [formError, setFormError] = useState("")

  const [loading, setLoading] = useState(false)

  const [exporting, setExporting] = useState(false)

  const [exportError, setExportError] = useState("")

  const [selected, setSelected] = useState<number | null>(null)

  const [detail, setDetail] = useState<AdminTransaction | null>(null)

  const [detailError, setDetailError] = useState("")

  const [detailReload, setDetailReload] = useState(0)

  const dialog = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    const controller = new AbortController()

    setReport(null)

    setError("")

    setExportError("")

    if (account?.role !== "ADMIN") return

    setLoading(true)

    adminTransactionsApi

      .list(filters, page, controller.signal)

      .then((value) => {
        if (!controller.signal.aborted) setReport(value)
      })

      .catch((failure) => {
        if (!controller.signal.aborted) setError(asApiError(failure).message)
      })

      .finally(() => {
        if (!controller.signal.aborted) setLoading(false)
      })

    return () => controller.abort()
  }, [account?.id, account?.role, filters, page, reload])

  useEffect(() => {
    if (selected === null || account?.role !== "ADMIN") return

    const controller = new AbortController()

    setDetail(null)

    setDetailError("")

    if (!dialog.current?.open) dialog.current?.showModal()

    adminTransactionsApi

      .detail(selected, controller.signal)

      .then((value) => {
        if (!controller.signal.aborted) setDetail(value)
      })

      .catch((failure) => {
        if (!controller.signal.aborted)
          setDetailError(asApiError(failure).message)
      })

    return () => controller.abort()
  }, [selected, detailReload, account?.id, account?.role])

  if (account?.role !== "ADMIN") return null

  function submit(event: FormEvent) {
    event.preventDefault()

    if (draft.from && draft.to && draft.from > draft.to) {
      setFormError("Ngày bắt đầu không được sau ngày kết thúc.")

      return
    }

    setFormError("")

    setFilters({ ...draft, query: draft.query.trim() })

    setPage(0)
  }

  async function exportCsv() {
    setExporting(true)

    setExportError("")

    try {
      const blob = await adminTransactionsApi.export(filters)

      const url = URL.createObjectURL(blob)

      const anchor = document.createElement("a")

      anchor.href = url

      anchor.download = "MamXanh_giao_dich.csv"

      anchor.click()

      setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (failure) {
      setExportError((await asApiErrorAsync(failure)).message)
    } finally {
      setExporting(false)
    }
  }

  const summary = report?.summary

  const today = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Ho_Chi_Minh",

    year: "numeric",

    month: "2-digit",

    day: "2-digit",
  }).format(new Date())

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between xl:gap-6">
        <div className="min-w-0 flex-1">
          <h1 className="text-balance text-2xl leading-tight font-extrabold text-ink sm:text-3xl">
            Giám sát giao dịch gói AI
          </h1>
          <p className="mt-3 max-w-[72ch] text-pretty text-sm leading-6 text-ink-muted">
            Theo dõi thanh toán Plus và Pro qua payOS. Số liệu phản ánh giao
            dịch hệ thống đã ghi nhận. Thời gian hiển thị theo giờ Việt Nam.
          </p>
        </div>
        <div className="flex shrink-0 flex-wrap items-center gap-2 xl:pt-0.5">
          <Button
            type="button"
            variant="outline"
            className="min-h-11 whitespace-nowrap"
            disabled={loading}
            onClick={() => setReload((value) => value + 1)}
          >
            <RefreshCw className="h-4 w-4" aria-hidden="true" />
            Làm mới
          </Button>
          <Button
            type="button"
            variant="outline"
            className="min-h-11 whitespace-nowrap"
            disabled={loading || exporting || !summary?.total}
            onClick={exportCsv}
          >
            <Download className="h-4 w-4" aria-hidden="true" />
            {exporting ? "Đang xuất…" : "Xuất đối soát CSV"}
          </Button>
        </div>
      </div>
      <Card className="p-4 sm:p-5">
        <form
          onSubmit={submit}
          className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"
        >
          <label className="space-y-2 sm:col-span-2">
            <span className="text-sm font-semibold text-ink-soft">
              Tìm giao dịch
            </span>
            <div className="relative">
              <Search
                className="pointer-events-none absolute left-3 top-3 h-5 w-5 text-ink-muted"
                aria-hidden="true"
              />
              <input
                maxLength={200}
                className={`${input} pl-10`}
                value={draft.query}
                onChange={(e) => setDraft({ ...draft, query: e.target.value })}
                placeholder="Mã giao dịch, mã đơn payOS, tên hoặc email"
              />
            </div>
          </label>
          <label className="space-y-2 text-sm text-ink-soft">
            <span id="transaction-tier-label">Gói dịch vụ</span>
            <select
              aria-labelledby="transaction-tier-label"
              className={input}
              value={draft.tier}
              onChange={(e) =>
                setDraft({
                  ...draft,

                  tier: e.target.value as TransactionFilters["tier"],
                })
              }
            >
              <option value="">Tất cả gói</option>
              <option value="PLUS">Plus</option>
              <option value="PRO">Pro</option>
            </select>
          </label>
          <label className="space-y-2 text-sm text-ink-soft">
            <span id="transaction-status-label">Trạng thái</span>
            <select
              aria-labelledby="transaction-status-label"
              className={input}
              value={draft.status}
              onChange={(e) =>
                setDraft({
                  ...draft,

                  status: e.target.value as TransactionFilters["status"],
                })
              }
            >
              <option value="">Tất cả trạng thái</option>
              {Object.entries(statuses).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <label className="space-y-2 text-sm text-ink-soft">
            <span>Từ ngày tạo</span>
            <input
              className={input}
              type="date"
              min="1900-01-01"
              max={today}
              value={draft.from}
              onChange={(e) => setDraft({ ...draft, from: e.target.value })}
            />
          </label>
          <label className="space-y-2 text-sm text-ink-soft">
            <span>Đến ngày tạo</span>
            <input
              className={input}
              type="date"
              min="1900-01-01"
              max={today}
              value={draft.to}
              onChange={(e) => setDraft({ ...draft, to: e.target.value })}
            />
          </label>
          <div className="flex flex-wrap items-end gap-2 sm:col-span-2">
            <Button type="submit">Áp dụng bộ lọc</Button>
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                setDraft(initial)

                setFilters({ ...initial })

                setPage(0)

                setFormError("")
              }}
            >
              Xóa bộ lọc
            </Button>
          </div>
          {formError && (
            <p
              role="alert"
              className="text-sm text-red-700 sm:col-span-2 xl:col-span-4"
            >
              {formError}
            </p>
          )}
        </form>
      </Card>
      {loading && (
        <p role="status" className="text-sm text-ink-muted">
          Đang tải giao dịch…
        </p>
      )}
      {error && (
        <Card className="p-4">
          <p role="alert" className="text-red-700">
            {error}
          </p>
          <Button
            type="button"
            variant="outline"
            className="mt-3"
            onClick={() => setReload((value) => value + 1)}
          >
            Thử lại
          </Button>
        </Card>
      )}
      {exportError && (
        <p role="alert" className="text-sm text-red-700">
          {exportError}
        </p>
      )}
      {summary && report && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
            {[
              [
                "Doanh thu ghi nhận",

                currency.format(summary.revenueVnd),

                "Chỉ tính giao dịch thành công",
              ],

              [
                "Giao dịch thành công",

                summary.paid.toLocaleString("vi-VN"),

                `Tỷ lệ ${
                  summary.total
                    ? ((summary.paid / summary.total) * 100).toLocaleString(
                        "vi-VN",

                        { maximumFractionDigits: 1 },
                      )
                    : "0"
                }% trên tổng giao dịch`,
              ],

              [
                "Chờ thanh toán",

                summary.pending.toLocaleString("vi-VN"),

                "Theo trạng thái đã ghi nhận",
              ],

              [
                "Đã hủy",

                summary.cancelled.toLocaleString("vi-VN"),

                "Đã hủy trước khi thanh toán",
              ],

              [
                "Cần đối soát",

                summary.needsReconciliation.toLocaleString("vi-VN"),

                "Đã thanh toán sau hạn hoặc thiếu thời điểm xác minh",
              ],

              [
                "Thất bại",

                summary.failed.toLocaleString("vi-VN"),

                "Thanh toán không thành công",
              ],
            ].map(([label, value, hint]) => (
              <Card key={label} className="p-5">
                <p className="text-sm font-semibold text-ink-muted">{label}</p>
                <p className="mt-3 break-words text-2xl font-extrabold text-leaf-700">
                  {value}
                </p>
                <p className="mt-2 text-xs leading-5 text-ink-muted">{hint}</p>
              </Card>
            ))}
          </div>
          <p className="text-xs leading-5 text-ink-muted">
            Thống kê và CSV bao gồm mọi trang theo bộ lọc đã áp dụng. CSV tối đa
            10.000 giao dịch mỗi lần xuất, không phải hóa đơn hoặc xác nhận đối
            soát từ payOS.
          </p>
          <Card className="overflow-hidden">
            <div
              className="overflow-x-auto focus-visible:outline-2 focus-visible:outline-brand-600"
              tabIndex={0}
              role="region"
              aria-label="Danh sách giao dịch, cuộn ngang để xem đủ cột"
            >
              <table className="w-full min-w-[960px] text-left text-sm">
                <caption className="sr-only">
                  Giao dịch thanh toán gói tài khoản
                </caption>
                <thead className="bg-brand-50 text-ink-muted">
                  <tr>
                    {[
                      "Mã giao dịch / Đơn payOS",

                      "Khách hàng",

                      "Sản phẩm",

                      "Số tiền",

                      "Thời gian tạo",

                      "Trạng thái",

                      "Chi tiết",
                    ].map((label) => (
                      <th
                        key={label}
                        scope="col"
                        className="px-4 py-3 font-semibold"
                      >
                        {label}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {report.transactions.content.map((tx) => (
                    <tr key={tx.id} className="border-t border-brand-100">
                      <td className="px-4 py-4">
                        <p className="font-mono font-bold text-ink">
                          TX-{tx.id}
                        </p>
                        <p className="mt-1 font-mono text-xs text-ink-muted">
                          {tx.orderCode}
                        </p>
                      </td>
                      <td className="max-w-64 break-words px-4 py-4">
                        <p className="font-semibold text-ink">
                          {tx.customerName}
                        </p>
                        <p className="mt-1 text-xs text-ink-muted">
                          {tx.customerEmail}
                        </p>
                      </td>
                      <td className="px-4 py-4">
                        {tx.productType === "AI_CREDIT"
                          ? <div><Badge>Nạp AI Credit · {tx.creditAmount?.toLocaleString("vi-VN", { maximumFractionDigits: 9 })}</Badge><p className="mt-1 text-xs text-ink-muted">{tx.creditsPer1000VndSnapshot?.toLocaleString("vi-VN")} Credit / 1.000đ</p></div>
                          : tx.tier ? <Badge>{tx.tier}</Badge> : "Chưa xác định"}
                      </td>
                      <td className="whitespace-nowrap px-4 py-4 font-semibold text-ink">
                        {currency.format(tx.amountVnd)}
                      </td>
                      <td className="whitespace-nowrap px-4 py-4 text-ink-soft">
                        {time(tx.createdAt)}
                      </td>
                      <td className="px-4 py-4">
                        <State value={tx.status} />
                      </td>
                      <td className="px-4 py-4">
                        <Button
                          className="whitespace-nowrap"
                          type="button"
                          variant="outline"
                          size="sm"
                          aria-label={`Xem chi tiết TX-${tx.id}`}
                          onClick={() => setSelected(tx.id)}
                        >
                          Xem chi tiết
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {!report.transactions.content.length && (
              <p className="p-8 text-center text-sm text-ink-muted">
                Không có giao dịch phù hợp. Hãy đổi bộ lọc hoặc tạo thanh toán
                từ tài khoản Member.
              </p>
            )}
            <div className="flex flex-wrap items-center justify-between gap-3 border-t border-brand-100 p-4">
              <p className="text-sm text-ink-muted">
                {report.transactions.totalElements.toLocaleString("vi-VN")} giao
                dịch · Trang {page + 1}/
                {Math.max(1, report.transactions.totalPages)}
              </p>
              <div className="flex gap-2">
                <Button
                  type="button"
                  variant="outline"
                  disabled={page === 0}
                  onClick={() => setPage((value) => value - 1)}
                >
                  Trang trước
                </Button>
                <Button
                  type="button"
                  variant="outline"
                  disabled={page + 1 >= report.transactions.totalPages}
                  onClick={() => setPage((value) => value + 1)}
                >
                  Trang sau
                </Button>
              </div>
            </div>
          </Card>
        </>
      )}
      <dialog
        ref={dialog}
        onClose={() => {
          setSelected(null)

          setDetail(null)
        }}
        aria-labelledby="transaction-title"
        className="fixed inset-0 m-auto max-h-[85dvh] w-[calc(100%-2rem)] max-w-2xl overflow-y-auto rounded-2xl border border-brand-200 bg-paper p-5 text-ink shadow-xl backdrop:bg-black/50 sm:p-6"
      >
        <div className="flex items-center justify-between gap-3">
          <h2 id="transaction-title" className="text-lg font-extrabold">
            Chi tiết giao dịch {selected === null ? "" : `TX-${selected}`}
          </h2>
          <button
            type="button"
            aria-label="Đóng chi tiết giao dịch"
            className="flex min-h-11 min-w-11 items-center justify-center rounded-xl hover:bg-brand-100 focus-visible:outline-2"
            onClick={() => dialog.current?.close()}
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        {!detail && !detailError && (
          <p role="status" className="mt-4">
            Đang tải chi tiết…
          </p>
        )}
        {detailError && (
          <div className="mt-4">
            <p role="alert" className="text-red-700">
              {detailError}
            </p>
            <Button
              type="button"
              variant="outline"
              className="mt-3"
              onClick={() => setDetailReload((value) => value + 1)}
            >
              Tải lại chi tiết
            </Button>
          </div>
        )}
        {detail && (
          <>
            <dl className="mt-5 grid gap-4 sm:grid-cols-2">
              {[
                ["Mã đơn payOS", detail.orderCode],

                ["Khách hàng", detail.customerName],

                ["Email", detail.customerEmail],

                ["Sản phẩm", detail.productType === "AI_CREDIT" ? `Nạp AI Credit · ${detail.creditAmount?.toLocaleString("vi-VN", { maximumFractionDigits: 9 })} Credit` : detail.tier ?? "Chưa xác định"],

                ...(detail.productType === "AI_CREDIT" ? [["Tỷ lệ nạp", `${detail.creditsPer1000VndSnapshot?.toLocaleString("vi-VN")} Credit / 1.000đ`]] : []),

                ["Số tiền", currency.format(detail.amountVnd)],

                ["Trạng thái thanh toán", statuses[detail.status]],

                ["Tạo lúc", time(detail.createdAt)],

                ["Thanh toán lúc", time(detail.paidAt)],

                ["Hết hạn đơn", time(detail.expiresAt)],

                ["Mã đăng ký", detail.subscriptionId?.toString() ?? "Chưa có"],

                ["Bắt đầu chu kỳ", time(detail.subscriptionStartsAt)],

                ["Hết hạn chu kỳ", time(detail.subscriptionEndsAt)],
              ].map(([label, value]) => (
                <div key={label} className="min-w-0">
                  <dt className="text-xs text-ink-muted">{label}</dt>
                  <dd className="mt-1 break-words text-sm font-semibold">
                    {value}
                  </dd>
                </div>
              ))}
            </dl>
            <p className="mt-5 text-xs leading-5 text-ink-muted">
              Thông tin từ giao dịch và chu kỳ đã liên kết trong hệ thống. Thời
              hạn này thuộc lần mua tương ứng, không xác nhận gói hiện tại còn
              hiệu lực.
            </p>
          </>
        )}
        <div className="mt-6 flex justify-end">
          <Button
            type="button"
            variant="outline"
            onClick={() => dialog.current?.close()}
          >
            Đóng
          </Button>
        </div>
      </dialog>
    </div>
  )
}
