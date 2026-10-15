import { Link } from 'react-router-dom';
import { ArrowLeft, Clock, FileText, ShieldAlert, Users, CreditCard } from 'lucide-react';
import { Card } from '../components/ui';

type PendingModuleType = 'reports' | 'users' | 'transactions';

interface ModuleConfig {
  title: string;
  srsRef: string;
  icon: typeof ShieldAlert;
  description: string;
  requiredEndpoints: string[];
  plannedFeatures: string[];
}

const CONFIGS: Record<PendingModuleType, ModuleConfig> = {
  reports: {
    title: 'Hậu kiểm nội dung & Xử lý báo cáo vi phạm',
    srsRef: 'FR-06 / BR-23 / BR-26 / UC-06.1–UC-06.2',
    icon: ShieldAlert,
    description:
      'Module tiếp nhận và xử lý các báo cáo vi phạm đối với bài viết công thức, bình luận và tài khoản thành viên từ cộng đồng Mầm Xanh.',
    requiredEndpoints: [
      'GET /api/v1/admin/reports (Danh sách hàng đợi báo cáo)',
      'GET /api/v1/admin/reports/{id} (Chi tiết nội dung bị báo cáo)',
      'POST /api/v1/admin/reports/{id}/resolve (Quyết định hậu kiểm: ẩn bài, cảnh cáo, bác bỏ)',
    ],
    plannedFeatures: [
      'Xem hàng đợi báo cáo vi phạm được lọc theo độ ưu tiên và loại nội dung',
      'Thẩm định nội dung bị báo cáo và bảo mật danh tính người gửi (BR-28)',
      'Ghi nhận kết luận xử lý hậu kiểm và lưu vết kiểm toán (Audit Trail)',
    ],
  },
  users: {
    title: 'Quản lý tài khoản & Phân quyền người dùng',
    srsRef: 'FR-01 / FR-02 / BR-05 / BR-26',
    icon: Users,
    description:
      'Module quản lý danh sách tài khoản thành viên, trạng thái hoạt động (ACTIVE, LOCKED) và vai trò trong hệ thống.',
    requiredEndpoints: [
      'GET /api/v1/admin/users?query=&status=&role=&page=&size= (Danh sách người dùng)',
      'GET /api/v1/admin/users/{id} (Chi tiết hồ sơ và lịch sử hoạt động)',
      'PATCH /api/v1/admin/users/{id}/status (Khóa/Mở khóa tài khoản vi phạm)',
    ],
    plannedFeatures: [
      'Tìm kiếm và tra cứu thành viên theo email, tên hiển thị và vai trò',
      'Giám sát trạng thái kích hoạt tài khoản và lịch sử cảnh cáo',
      'Thực hiện chế tài khóa tài khoản khi có vi phạm nghiêm trọng',
    ],
  },
  transactions: {
    title: 'Giám sát giao dịch thanh toán gói AI hệ thống',
    srsRef: 'FR-13 / M08 / Issue #19',
    icon: CreditCard,
    description:
      'Module theo dõi các giao dịch thanh toán đăng ký gói dịch vụ AI (PLUS, PRO) trên toàn hệ thống.',
    requiredEndpoints: [
      'GET /api/v1/admin/subscriptions/transactions (Lịch sử giao dịch toàn hệ thống)',
      'GET /api/v1/admin/subscriptions/summary (Thống kê doanh thu và lượt đăng ký)',
    ],
    plannedFeatures: [
      'Xem nhật ký giao dịch qua cổng thanh toán VNPay / PayOS',
      'Đối soát trạng thái giao dịch (SUCCESS, PENDING, FAILED)',
      'Thống kê số lượng gói dịch vụ đang hoạt động',
    ],
  },
};

export function AdminPendingModule({ module }: { module: PendingModuleType }) {
  const config = CONFIGS[module];
  const Icon = config.icon;

  return (
    <div className="mx-auto max-w-4xl space-y-6 py-4">
      <div className="flex items-center gap-3">
        <Link
          to="/admin"
          className="inline-flex items-center gap-1.5 rounded-xl border border-brand-200/80 bg-paper px-3 py-1.5 text-xs font-semibold text-ink-soft transition-colors hover:border-brand-500 hover:text-brand-600"
        >
          <ArrowLeft className="h-3.5 w-3.5" /> Quay lại Dashboard
        </Link>
        <span className="inline-flex items-center gap-1.5 rounded-full border border-amber-200 bg-amber-50 px-3 py-1 text-xs font-bold text-amber-900">
          <Clock className="h-3.5 w-3.5 text-amber-600" /> Chờ kết nối API Backend
        </span>
      </div>

      <Card className="border-amber-200/80 bg-paper p-6 shadow-sm sm:p-8">
        <div className="flex flex-col gap-5 sm:flex-row sm:items-start">
          <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-amber-100 text-amber-700">
            <Icon className="h-7 w-7" />
          </span>
          <div className="min-w-0 flex-1">
            <p className="text-xs font-bold uppercase tracking-wider text-amber-800">
              Căn cứ yêu cầu: {config.srsRef}
            </p>
            <h1 className="mt-1 text-2xl font-black text-ink sm:text-3xl">{config.title}</h1>
            <p className="mt-3 text-sm leading-relaxed text-ink-muted">{config.description}</p>
          </div>
        </div>

        <div className="mt-8 border-t border-brand-100/80 pt-6">
          <h2 className="text-sm font-bold uppercase tracking-wider text-ink">
            Nghiệp vụ quản trị dự kiến (SRS)
          </h2>
          <ul className="mt-3 space-y-2">
            {config.plannedFeatures.map((feat) => (
              <li key={feat} className="flex items-start gap-2.5 text-sm text-ink-soft">
                <span className="mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full bg-brand-500" />
                <span>{feat}</span>
              </li>
            ))}
          </ul>
        </div>

        <div className="mt-8 rounded-2xl border border-brand-100 bg-cream-soft/60 p-5">
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-ink-muted">
            <FileText className="h-4 w-4 text-brand-600" />
            Hợp đồng API Backend cần hoàn thiện
          </div>
          <div className="mt-3 space-y-2 font-mono text-xs">
            {config.requiredEndpoints.map((ep) => (
              <div
                key={ep}
                className="rounded-lg border border-brand-200/60 bg-paper px-3 py-2 text-ink shadow-2xs"
              >
                {ep}
              </div>
            ))}
          </div>
          <p className="mt-3 text-xs text-ink-muted leading-relaxed">
            Theo nguyên tắc phát triển an toàn của dự án, giao diện quản trị không giả lập dữ liệu hoặc hiển thị nút bấm thiếu API thực tế. Màn hình quản trị đầy đủ sẽ tự động được kích hoạt khi Backend triển khai các endpoint tương ứng.
          </p>
        </div>
      </Card>
    </div>
  );
}
