import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { CreditCard, Loader2, AlertCircle, ArrowLeft, RefreshCw, CheckCircle, Clock, XCircle } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Badge, Button, Card, EmptyState } from '../components/ui';
import { useDemoAccount } from '../components/DemoAccount';
import { subscriptionApi, type TransactionHistoryResponse, type PaymentStatus } from '../api/subscription';
import { hasAccessToken } from '../lib/apiClient';
import { formatDateTime, formatVnd } from '../utils/date';

function renderStatusBadge(status: PaymentStatus) {
  switch (status) {
    case 'PAID':
      return (
        <Badge tone="leaf">
          <CheckCircle className="mr-1 h-3 w-3" /> Đã thanh toán
        </Badge>
      );
    case 'PENDING':
      return (
        <Badge tone="brand">
          <Clock className="mr-1 h-3 w-3" /> Đang chờ
        </Badge>
      );
    case 'CANCELLED':
      return (
        <Badge tone="neutral">
          <XCircle className="mr-1 h-3 w-3" /> Đã hủy
        </Badge>
      );
    case 'FAILED':
      return (
        <Badge tone="neutral">
          <AlertCircle className="mr-1 h-3 w-3" /> Thất bại
        </Badge>
      );
    default:
      return <Badge tone="neutral">{status}</Badge>;
  }
}

export function TransactionHistory() {
  const { active: isDemoActive } = useDemoAccount();
  const isLoggedIn = hasAccessToken();
  const [transactions, setTransactions] = useState<TransactionHistoryResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  async function loadTransactions() {
    if (!isLoggedIn) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setErrorMsg(null);
    try {
      const data = await subscriptionApi.getTransactions();
      setTransactions(data);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Không thể tải lịch sử giao dịch.';
      setErrorMsg(msg);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadTransactions();
  }, [isLoggedIn]);

  return (
    <PageContainer className="py-8">
      <nav className="mb-5 text-sm text-ink-muted">
        <Link to="/ho-so" className="hover:text-brand-600">
          Hồ sơ
        </Link>{' '}
        / Lịch sử giao dịch
      </nav>

      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <h1 className="text-3xl font-extrabold text-ink">Lịch sử giao dịch</h1>
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
            <Button variant="outline" size="sm" onClick={loadTransactions} disabled={loading}>
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            </Button>
          )}
        </div>
      </div>

      {isDemoActive ? (
        <Card className="mt-8 p-10 text-center">
          <CreditCard className="mx-auto h-10 w-10 text-brand-500" />
          <h2 className="mt-4 text-lg font-bold text-ink">Chưa có dữ liệu giao dịch</h2>
          <p className="mx-auto mt-2 max-w-lg text-sm text-ink-muted">
            Lịch sử sẽ hiển thị mã giao dịch, gói, số tiền, thời gian và trạng thái khi kết nối dịch vụ thanh toán. Giao diện demo không tạo giao dịch giả.
          </p>
          <Link to="/goi-ai" className="mt-5 inline-block font-semibold text-brand-600 hover:text-brand-700">
            Xem các gói AI
          </Link>
        </Card>
      ) : !isLoggedIn ? (
        <Card className="mt-8 p-10 text-center">
          <CreditCard className="mx-auto h-12 w-12 text-brand-400" />
          <h2 className="mt-4 text-lg font-bold text-ink">Cần đăng nhập để xem lịch sử</h2>
          <p className="mx-auto mt-2 max-w-md text-sm text-ink-muted">
            Vui lòng đăng nhập vào tài khoản Mâm Xanh để kiểm tra các giao dịch gói AI của bạn.
          </p>
          <div className="mt-6">
            <Link to="/dang-nhap">
              <Button variant="primary">Đăng nhập ngay</Button>
            </Link>
          </div>
        </Card>
      ) : loading ? (
        <div className="flex justify-center py-20">
          <Loader2 className="h-8 w-8 animate-spin text-brand-600" />
        </div>
      ) : errorMsg ? (
        <Card className="mt-8 border-red-200 bg-red-50/50 p-8 text-center">
          <AlertCircle className="mx-auto h-10 w-10 text-red-500" />
          <h2 className="mt-3 text-base font-bold text-red-800">Không thể tải dữ liệu</h2>
          <p className="mt-1 text-sm text-red-600">{errorMsg}</p>
          <Button variant="outline" size="sm" className="mt-4" onClick={loadTransactions}>
            Thử lại
          </Button>
        </Card>
      ) : transactions.length === 0 ? (
        <Card className="mt-8 p-10 text-center">
          <CreditCard className="mx-auto h-10 w-10 text-brand-500" />
          <h2 className="mt-4 text-lg font-bold text-ink">Chưa có dữ liệu giao dịch</h2>
          <p className="mx-auto mt-2 max-w-lg text-sm text-ink-muted">
            Bạn chưa thực hiện giao dịch mua gói AI nào. Khi bạn thanh toán gói PLUS hoặc PRO, thông tin đơn hàng sẽ được lưu tại đây.
          </p>
          <Link to="/goi-ai" className="mt-5 inline-block font-semibold text-brand-600 hover:text-brand-700">
            Xem các gói AI
          </Link>
        </Card>
      ) : (
        <div className="mt-8 overflow-hidden rounded-2xl border border-brand-100 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="border-b border-brand-100 bg-brand-50/70 text-xs font-bold text-ink-muted uppercase">
                <tr>
                  <th scope="col" className="px-6 py-4">Mã đơn hàng</th>
                  <th scope="col" className="px-6 py-4">Gói dịch vụ</th>
                  <th scope="col" className="px-6 py-4">Số tiền</th>
                  <th scope="col" className="px-6 py-4">Trạng thái</th>
                  <th scope="col" className="px-6 py-4">Thời gian tạo</th>
                  <th scope="col" className="px-6 py-4">Thời gian thanh toán</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-brand-50">
                {transactions.map((tx) => (
                  <tr key={tx.orderCode} className="hover:bg-brand-50/40 transition-colors">
                    <td className="px-6 py-4 font-mono font-semibold text-brand-700">
                      #{tx.orderCode}
                    </td>
                    <td className="px-6 py-4 font-bold text-ink">
                      Gói {tx.tier}
                    </td>
                    <td className="px-6 py-4 font-semibold text-ink">
                      {formatVnd(tx.amountVnd)} đ
                    </td>
                    <td className="px-6 py-4">
                      {renderStatusBadge(tx.status)}
                    </td>
                    <td className="px-6 py-4 text-xs text-ink-muted">
                      {formatDateTime(tx.createdAt)}
                    </td>
                    <td className="px-6 py-4 text-xs text-ink-muted">
                      {tx.paidAt ? formatDateTime(tx.paidAt) : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <div className="mt-6 flex items-center justify-between text-xs text-ink-muted">
        <Link to="/ho-so" className="inline-flex items-center gap-1 hover:text-brand-600">
          <ArrowLeft className="h-3 w-3" /> Quay lại Hồ sơ cá nhân
        </Link>
        <span>Cổng thanh toán bảo mật bởi payOS</span>
      </div>
    </PageContainer>
  );
}
