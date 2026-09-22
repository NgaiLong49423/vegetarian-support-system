import { Link } from 'react-router-dom';
import { CreditCard } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Card } from '../components/ui';

export function TransactionHistory() {
  return <PageContainer className="py-8">
    <nav className="mb-5 text-sm text-ink-muted"><Link to="/ho-so" className="hover:text-brand-600">Hồ sơ</Link> / Lịch sử giao dịch</nav>
    <h1 className="text-3xl font-extrabold text-ink">Lịch sử giao dịch</h1>
    <p className="mt-2 text-sm text-ink-muted">Theo dõi thanh toán gói AI của tài khoản.</p>
    <Card className="mt-6 p-10 text-center">
      <CreditCard className="mx-auto h-10 w-10 text-brand-500" />
      <h2 className="mt-4 text-lg font-bold text-ink">Chưa có dữ liệu giao dịch</h2>
      <p className="mx-auto mt-2 max-w-lg text-sm text-ink-muted">Lịch sử sẽ hiển thị mã giao dịch, gói, số tiền, thời gian và trạng thái khi kết nối dịch vụ thanh toán. Giao diện demo không tạo giao dịch giả.</p>
      <Link to="/goi-ai" className="mt-5 inline-block font-semibold text-brand-600 hover:text-brand-700">Xem các gói AI</Link>
    </Card>
  </PageContainer>;
}
