import { useEffect, useState } from "react"
import { Link } from "react-router-dom"
import {
  CreditCard,
  Loader2,
  AlertCircle,
  ArrowLeft,
  RefreshCw,
  CheckCircle,
  Clock,
  XCircle,
} from "lucide-react"
import { PageContainer } from "../components/Layout"
import { Badge, Button, Card } from "../components/ui"
import { useAuth } from "../components/AuthContext"
import {
  subscriptionApi,
  type TransactionHistoryResponse,
  type PaymentStatus,
} from "../api/subscription"
import { formatVnd } from "../utils/date"
import { formatSubscriptionDateTime as formatDateTime } from "../utils/subscriptionDate"

function renderStatusBadge(status: PaymentStatus) {
  switch (status) {
    case "PAID":
      return (
        <Badge tone="leaf">
          <CheckCircle className="mr-1 h-3 w-3" /> Đã thanh toán
        </Badge>
      )
    case "PENDING":
      return (
        <Badge tone="brand">
          <Clock className="mr-1 h-3 w-3" /> Đang chờ
        </Badge>
      )
    case "CANCELLED":
      return (
        <Badge tone="neutral">
          <XCircle className="mr-1 h-3 w-3" /> Đã hủy
        </Badge>
      )
    case "FAILED":
      return (
        <Badge tone="neutral">
          <AlertCircle className="mr-1 h-3 w-3" /> Thất bại
        </Badge>
      )
    default:
      return <Badge tone="neutral">{status}</Badge>
  }
}

export function TransactionHistory() {
  const { isAuthenticated: isLoggedIn } = useAuth()
  const [transactions, setTransactions] = useState<TransactionHistoryResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  async function loadTransactions() {
    if (!isLoggedIn) {
      setLoading(false)
      return
    }
    setLoading(true)
    setErrorMsg(null)
    try {
      const data = await subscriptionApi.getTransactions()
      setTransactions(data)
    } catch (err: unknown) {
      const msg =
        err instanceof Error ? err.message : "Không thể tải lịch sử giao dịch."
      setErrorMsg(msg)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void loadTransactions()
  }, [isLoggedIn])

  return (
    <PageContainer className="py-6 sm:py-8">
      <nav className="mb-4 text-sm text-ink-muted sm:mb-5">
        <Link to="/ho-so" className="hover:text-brand-600">
          Hồ sơ
        </Link>{" "}
        / Lịch sử giao dịch
      </nav>

      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <h1 className="text-2xl font-extrabold text-ink sm:text-3xl">
            Lịch sử giao dịch
          </h1>
          <p className="mt-1 text-sm text-ink-muted">
            Theo dõi tất cả giao dịch thanh toán gói AI qua cổng payOS của tài khoản.
          </p>
        </div>
        <div className="flex gap-2">
          <Link to="/goi-ai">
            <Button variant="primary" size="sm">
              Xem các gói AI
            </Button>
          </Link>
          {isLoggedIn && (
            <Button
              variant="outline"
              size="sm"
              aria-label="Làm mới lịch sử giao dịch"
              onClick={loadTransactions}
              disabled={loading}
            >
              <RefreshCw
                className={`h-4 w-4 ${loading ? "animate-spin" : ""}`}
              />
            </Button>
          )}
        </div>
      </div>

      {!isLoggedIn ? (
        <Card className="mt-8 p-6 text-center sm:p-10">
          <CreditCard className="mx-auto h-10 w-10 text-brand-500" />
          <h2 className="mt-4 text-lg font-bold text-ink">
            Chưa có dữ liệu giao dịch
          </h2>
          <p className="mx-auto mt-2 max-w-lg text-sm text-ink-muted">
            Lịch sử sẽ hiển thị mã giao dịch, gói, số tiền, thời gian và trạng
            thái khi bạn thực hiện đăng ký gói.
          </p>
          <Link
            to="/goi-ai"
            className="mt-5 inline-block font-semibold text-brand-600 hover:text-brand-700"
          >
            Xem các gói AI
          </Link>
        </Card>
      ) : loading ? (
        <div className="flex justify-center py-20">
          <Loader2 className="h-8 w-8 animate-spin text-brand-600" />
        </div>
      ) : errorMsg ? (
        <Card className="mt-8 border-red-200 bg-red-50/50 p-6 text-center sm:p-8">
          <AlertCircle className="mx-auto h-10 w-10 text-red-500" />
          <h2 className="mt-3 text-base font-bold text-red-800">
            Không thể tải dữ liệu
          </h2>
          <p className="mt-1 text-sm text-red-600">{errorMsg}</p>
          <Button
            variant="outline"
            size="sm"
            className="mt-4"
            onClick={loadTransactions}
          >
            Thử lại
          </Button>
        </Card>
      ) : transactions.length === 0 ? (
        <Card className="mt-8 p-6 text-center sm:p-10">
          <CreditCard className="mx-auto h-10 w-10 text-brand-500" />
          <h2 className="mt-4 text-lg font-bold text-ink">
            Chưa có dữ liệu giao dịch
          </h2>
          <p className="mx-auto mt-2 max-w-lg text-sm text-ink-muted">
            Bạn chưa thực hiện giao dịch mua gói AI nào. Khi bạn thanh toán gói
            PLUS hoặc PRO, thông tin đơn hàng sẽ được lưu tại đây.
          </p>
          <Link
            to="/goi-ai"
            className="mt-5 inline-block font-semibold text-brand-600 hover:text-brand-700"
          >
            Xem các gói AI
          </Link>
        </Card>
      ) : (
        <div className="mt-6 sm:mt-8">
          <p className="mb-3 text-xs text-ink-muted">
            Thời gian hiển thị theo giờ Việt Nam (UTC+7).
          </p>
          <div className="overflow-hidden rounded-2xl border border-brand-100 bg-paper shadow-sm">
            <div className="overflow-x-auto">
              <table role="table" className="tx-responsive-table w-full text-left text-sm">
                <thead className="border-b border-brand-100 bg-brand-50/70 text-xs font-bold text-ink-muted uppercase">
                  <tr role="row">
                    <th role="columnheader" scope="col" className="px-6 py-4">Mã đơn hàng</th>
                    <th role="columnheader" scope="col" className="px-6 py-4">Gói dịch vụ</th>
                    <th role="columnheader" scope="col" className="px-6 py-4">Số tiền</th>
                    <th role="columnheader" scope="col" className="px-6 py-4">Trạng thái</th>
                    <th role="columnheader" scope="col" className="px-6 py-4">Thời gian tạo</th>
                    <th role="columnheader" scope="col" className="px-6 py-4">Thời gian thanh toán</th>
                    <th role="columnheader" scope="col" className="px-6 py-4">Thời gian hết hạn</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-brand-50">
                  {transactions.map((tx) => (
                    <tr
                      key={tx.orderCode}
                      role="row"
                      className="transition-colors hover:bg-brand-50/40"
                    >
                      <td role="cell" data-label="Mã đơn hàng" className="md:px-6 md:py-4 font-mono font-semibold text-brand-700">
                        #{tx.orderCode}
                      </td>
                      <td role="cell" data-label="Gói dịch vụ" className="md:px-6 md:py-4 font-bold text-ink">
                        {tx.tier ? `Gói ${tx.tier}` : "Không xác định"}
                      </td>
                      <td role="cell" data-label="Số tiền" className="md:px-6 md:py-4 font-semibold text-ink">
                        {formatVnd(tx.amountVnd)} đ
                      </td>
                      <td role="cell" data-label="Trạng thái" className="md:px-6 md:py-4">
                        {renderStatusBadge(tx.status)}
                      </td>
                      <td role="cell" data-label="Thời gian tạo" className="md:px-6 md:py-4 text-xs text-ink-muted">
                        {formatDateTime(tx.createdAt)}
                      </td>
                      <td role="cell" data-label="Thanh toán" className="md:px-6 md:py-4 text-xs text-ink-muted">
                        {tx.paidAt ? formatDateTime(tx.paidAt) : "—"}
                      </td>
                      <td role="cell" data-label="Hết hạn" className="md:px-6 md:py-4 text-xs text-ink-muted">
                        {tx.status === "PAID" && tx.endsAt
                          ? formatDateTime(tx.endsAt)
                          : "—"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      <div className="mt-6 flex flex-col gap-2 text-xs text-ink-muted sm:flex-row sm:items-center sm:justify-between">
        <Link
          to="/ho-so"
          className="inline-flex items-center gap-1 hover:text-brand-600"
        >
          <ArrowLeft className="h-3 w-3" /> Quay lại Hồ sơ cá nhân
        </Link>
        <span>Cổng thanh toán bảo mật bởi payOS</span>
      </div>
    </PageContainer>
  )
}
