import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowLeft,
  Search,
  Users,
  ShieldAlert,
  CreditCard,
  Lock,
  Unlock,
  UserCheck,
  Eye,
  CheckCircle2,
  XCircle,
  X,
  Download,
  Calendar,
  AlertCircle,
  Clock,
  Check,
} from 'lucide-react';
import { Card, Badge, Button } from '../components/ui';
import { AdminUserManagement } from './AdminUserManagement';

type PendingModuleType = 'users' | 'transactions';

export function AdminPendingModule({ module }: { module: PendingModuleType }) {
  if (module === 'users') return <AdminUserManagement />;
  return <>
    <div role="status" className="mb-5 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
      Giao diện minh họa · Dữ liệu mẫu. Thao tác tại đây chỉ thay đổi bản xem thử, chưa cập nhật giao dịch trên Backend.
    </div>
    <AdminTransactionAudit />
  </>;
}

/* ==========================================================================
 * 3. Module Giám sát giao dịch thanh toán gói AI hệ thống
 * ========================================================================== */
interface MockTransaction {
  id: string;
  payOsCode: string;
  customerName: string;
  customerEmail: string;
  tier: 'PLUS' | 'PRO';
  amountVnd: number;
  paymentGateway: string;
  createdAt: string;
  status: 'SUCCESS' | 'PENDING' | 'FAILED';
}

type TransactionPreviewRow = [
  id: string,
  payOsCode: string,
  customerName: string,
  customerEmail: string,
  tier: MockTransaction['tier'],
  createdAt: string,
  status: MockTransaction['status'],
];

const TRANSACTION_PREVIEW_ROWS: TransactionPreviewRow[] = [
  ['TX-883921', 'POS-774921', 'Nguyễn Thị Lan', 'lan.nguyen@gmail.com', 'PRO', '16/10/2026 15:30', 'SUCCESS'],
  ['TX-883915', 'POS-774912', 'Trần Minh Đức', 'duc.tran@gmail.com', 'PLUS', '16/10/2026 12:15', 'SUCCESS'],
  ['TX-883902', 'POS-774899', 'Đặng Quốc Huy', 'huy.dang@yahoo.com', 'PRO', '16/10/2026 10:45', 'PENDING'],
  ['TX-883890', 'POS-774872', 'Vũ Thị Mai', 'mai.vu@outlook.com', 'PLUS', '15/10/2026 21:10', 'FAILED'],
  ['TX-883854', 'POS-774840', 'Hoàng Gia Bảo', 'bao.chef@vegan.vn', 'PRO', '15/10/2026 14:02', 'SUCCESS'],
  ['TX-883812', 'POS-774811', 'Phạm Thanh Sơn', 'son.pham@gmail.com', 'PLUS', '14/10/2026 09:20', 'SUCCESS'],
];

const INITIAL_TRANSACTIONS: MockTransaction[] = TRANSACTION_PREVIEW_ROWS.map(
  ([id, payOsCode, customerName, customerEmail, tier, createdAt, status]) => ({
    id,
    payOsCode,
    customerName,
    customerEmail,
    tier,
    amountVnd: tier === 'PRO' ? 99000 : 49000,
    paymentGateway: 'VietQR (payOS)',
    createdAt,
    status,
  }),
);

function AdminTransactionAudit() {
  const [transactions] = useState<MockTransaction[]>(INITIAL_TRANSACTIONS);
  const [search, setSearch] = useState('');
  const [tierFilter, setTierFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [selectedTx, setSelectedTx] = useState<MockTransaction | null>(null);

  const filteredTx = useMemo(() => {
    return transactions.filter((tx) => {
      const matchSearch =
        tx.id.toLowerCase().includes(search.toLowerCase()) ||
        tx.payOsCode.toLowerCase().includes(search.toLowerCase()) ||
        tx.customerName.toLowerCase().includes(search.toLowerCase()) ||
        tx.customerEmail.toLowerCase().includes(search.toLowerCase());
      const matchTier = tierFilter === 'ALL' || tx.tier === tierFilter;
      const matchStatus = statusFilter === 'ALL' || tx.status === statusFilter;
      return matchSearch && matchTier && matchStatus;
    });
  }, [transactions, search, tierFilter, statusFilter]);

  const totalSuccessRevenue = transactions
    .filter((tx) => tx.status === 'SUCCESS')
    .reduce((sum, tx) => sum + tx.amountVnd, 0);

  const successCount = transactions.filter((tx) => tx.status === 'SUCCESS').length;

  return (
    <div className="space-y-6 py-2">
      {/* Top Header */}
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <div className="flex items-center gap-2">
            <Link
              to="/admin"
              className="inline-flex items-center gap-1 text-xs font-semibold text-ink-muted hover:text-brand-600"
            >
              <ArrowLeft className="h-3.5 w-3.5" /> Dashboard
            </Link>
            <span className="text-xs text-ink-muted">/</span>
            <span className="text-xs font-semibold text-brand-700">Giao dịch hệ thống</span>
          </div>
          <h1 className="mt-1 text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">
            Giám sát giao dịch gói AI hệ thống
          </h1>
          <p className="mt-1 text-xs text-ink-muted sm:text-sm">
            Theo dõi nhật ký thanh toán các gói PLUS và PRO qua cổng VietQR PayOS trên toàn hệ thống.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Button variant="outline" size="sm">
            <Download className="h-3.5 w-3.5" /> Xuất đối soát
          </Button>
        </div>
      </div>

      {/* Summary Stats */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-ink-muted">Doanh thu ghi nhận</span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-leaf-100 text-leaf-700">
              <CreditCard className="h-4 w-4" />
            </span>
          </div>
          <p className="mt-2 text-2xl font-extrabold text-leaf-700">
            {totalSuccessRevenue.toLocaleString('vi-VN')} đ
          </p>
          <span className="text-[11px] text-ink-muted">Từ giao dịch hoàn tất</span>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-ink-muted">Giao dịch thành công</span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-brand-100 text-brand-700">
              <CheckCircle2 className="h-4 w-4" />
            </span>
          </div>
          <p className="mt-2 text-2xl font-extrabold text-ink">{successCount}</p>
          <span className="text-[11px] text-leaf-700 font-medium">Tỷ lệ thành công 85%</span>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-ink-muted">Đang chờ quét mã</span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-amber-100 text-amber-800">
              <Clock className="h-4 w-4" />
            </span>
          </div>
          <p className="mt-2 text-2xl font-extrabold text-amber-700">
            {transactions.filter((tx) => tx.status === 'PENDING').length}
          </p>
          <span className="text-[11px] text-ink-muted">Phiên thanh toán mở</span>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-ink-muted">Cổng thanh toán</span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-brand-100 text-brand-700">
              <CreditCard className="h-4 w-4" />
            </span>
          </div>
          <p className="mt-2 text-lg font-extrabold text-brand-600">VietQR PayOS</p>
          <span className="text-[11px] text-ink-muted">Hoạt động bình thường</span>
        </Card>
      </div>

      {/* Filter and Search Bar */}
      <Card className="p-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm theo mã giao dịch, email khách hàng hoặc mã đối soát..."
              className="w-full rounded-xl border border-brand-200 bg-paper py-2 pl-9 pr-3 text-xs text-ink placeholder:text-ink-muted focus:border-brand-500 focus:outline-none"
            />
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <select
              value={tierFilter}
              onChange={(e) => setTierFilter(e.target.value)}
              className="rounded-xl border border-brand-200 bg-paper px-3 py-2 text-xs font-medium text-ink focus:border-brand-500 focus:outline-none"
            >
              <option value="ALL">Tất cả gói</option>
              <option value="PLUS">Gói PLUS (49.000 đ)</option>
              <option value="PRO">Gói PRO (99.000 đ)</option>
            </select>

            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="rounded-xl border border-brand-200 bg-paper px-3 py-2 text-xs font-medium text-ink focus:border-brand-500 focus:outline-none"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="SUCCESS">Thành công</option>
              <option value="PENDING">Đang chờ quét</option>
              <option value="FAILED">Hết hạn / Lỗi</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Transactions Table */}
      <Card className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-brand-100 bg-brand-50/50 text-ink-muted">
              <tr>
                <th className="px-4 py-3 font-bold">Mã GD / PayOS</th>
                <th className="px-4 py-3 font-bold">Khách hàng</th>
                <th className="px-4 py-3 font-bold">Gói dịch vụ</th>
                <th className="px-4 py-3 font-bold">Số tiền</th>
                <th className="px-4 py-3 font-bold">Thời gian</th>
                <th className="px-4 py-3 font-bold">Trạng thái</th>
                <th className="px-4 py-3 text-right font-bold">Biên nhận</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-brand-100/60 text-ink">
              {filteredTx.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-8 text-center text-ink-muted">
                    Không tìm thấy giao dịch nào phù hợp với bộ lọc.
                  </td>
                </tr>
              ) : (
                filteredTx.map((tx) => (
                  <tr key={tx.id} className="transition-colors hover:bg-brand-50/30">
                    <td className="px-4 py-3">
                      <p className="font-mono font-bold text-ink">{tx.id}</p>
                      <p className="font-mono text-[11px] text-ink-muted">{tx.payOsCode}</p>
                    </td>
                    <td className="px-4 py-3">
                      <p className="font-bold text-ink">{tx.customerName}</p>
                      <p className="text-[11px] text-ink-muted">{tx.customerEmail}</p>
                    </td>
                    <td className="px-4 py-3">
                      <Badge tone={tx.tier === 'PRO' ? 'brand' : 'neutral'}>
                        Gói {tx.tier}
                      </Badge>
                    </td>
                    <td className="px-4 py-3 font-bold text-ink">
                      {tx.amountVnd.toLocaleString('vi-VN')} đ
                    </td>
                    <td className="px-4 py-3 text-ink-muted">{tx.createdAt}</td>
                    <td className="px-4 py-3">
                      {tx.status === 'SUCCESS' ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-leaf-100 px-2.5 py-0.5 text-[11px] font-bold text-leaf-800">
                          Thành công
                        </span>
                      ) : tx.status === 'PENDING' ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2.5 py-0.5 text-[11px] font-bold text-amber-800">
                          Chờ thanh toán
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2.5 py-0.5 text-[11px] font-bold text-red-800">
                          Hết hạn
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <Button size="sm" variant="outline" onClick={() => setSelectedTx(tx)}>
                        Xem hóa đơn
                      </Button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* MODAL: Xem hóa đơn chi tiết */}
      {selectedTx && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
          <Card className="w-full max-w-md p-6 shadow-xl animate-in fade-in zoom-in-95">
            <div className="flex items-center justify-between border-b border-brand-100 pb-3">
              <div>
                <span className="font-mono text-xs font-bold text-brand-600">Hóa đơn điện tử</span>
                <h3 className="text-base font-bold text-ink">Chi tiết giao dịch {selectedTx.id}</h3>
              </div>
              <button
                type="button"
                onClick={() => setSelectedTx(null)}
                className="rounded-lg p-1 text-ink-muted hover:bg-brand-100"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="mt-4 space-y-3 text-xs">
              <div className="flex justify-between py-1.5 border-b border-brand-100/60">
                <span className="text-ink-muted">Khách hàng:</span>
                <span className="font-bold text-ink">{selectedTx.customerName}</span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-brand-100/60">
                <span className="text-ink-muted">Email:</span>
                <span className="font-mono text-ink">{selectedTx.customerEmail}</span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-brand-100/60">
                <span className="text-ink-muted">Gói dịch vụ AI:</span>
                <span className="font-bold text-brand-600">Gói {selectedTx.tier}</span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-brand-100/60">
                <span className="text-ink-muted">Số tiền thanh toán:</span>
                <span className="text-sm font-extrabold text-ink">
                  {selectedTx.amountVnd.toLocaleString('vi-VN')} đ
                </span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-brand-100/60">
                <span className="text-ink-muted">Cổng thanh toán:</span>
                <span className="font-medium text-ink">{selectedTx.paymentGateway}</span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-brand-100/60">
                <span className="text-ink-muted">Mã chuẩn chi PayOS:</span>
                <span className="font-mono font-bold text-ink">{selectedTx.payOsCode}</span>
              </div>
              <div className="flex justify-between py-1.5 border-b border-brand-100/60">
                <span className="text-ink-muted">Thời gian:</span>
                <span className="text-ink-muted">{selectedTx.createdAt}</span>
              </div>
              <div className="flex justify-between py-1.5">
                <span className="text-ink-muted">Trạng thái:</span>
                <span className="font-bold text-leaf-700">
                  {selectedTx.status === 'SUCCESS' ? 'Hoàn tất giao dịch' : selectedTx.status === 'PENDING' ? 'Chờ thanh toán' : 'Giao dịch hết hạn'}
                </span>
              </div>
            </div>

            <div className="mt-6 flex justify-end gap-2 border-t border-brand-100 pt-4">
              <Button size="sm" onClick={() => setSelectedTx(null)}>
                Đóng
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
