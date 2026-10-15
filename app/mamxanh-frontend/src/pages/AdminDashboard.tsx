import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Activity,
  AlertCircle,
  ArrowRight,
  Award,
  Boxes,
  CheckCircle2,
  Clock,
  Database,
  ExternalLink,
  Layers,
  LoaderCircle,
  RefreshCw,
  ShieldAlert,
  ShieldCheck,
  UserCheck,
  Users,
} from 'lucide-react';
import { Card, Button, Badge } from '../components/ui';
import { useAuth } from '../components/AuthContext';
import { expertApplicationsApi, type ExpertApplication } from '../api/expertApplications';
import {
  adminCatalogApi,
  type CatalogIngredient,
  type IngredientCatalogOptions,
} from '../services/adminCatalogApi';

export function AdminDashboard() {
  const { account } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Real data state
  const [pendingApplications, setPendingApplications] = useState<ExpertApplication[]>([]);
  const [pendingCount, setPendingCount] = useState(0);
  const [ingredientTotal, setIngredientTotal] = useState(0);
  const [recentIngredients, setRecentIngredients] = useState<CatalogIngredient[]>([]);
  const [catalogOptions, setCatalogOptions] = useState<IngredientCatalogOptions>({
    groups: [],
    sources: [],
  });

  const loadData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [appRes, ingRes, optRes] = await Promise.allSettled([
        expertApplicationsApi.list('PENDING', 0),
        adminCatalogApi.searchIngredients({
          query: '',
          ingredientGroup: '',
          active: null,
          nutritionSupported: null,
          page: 0,
          size: 5,
        }),
        adminCatalogApi.ingredientOptions(),
      ]);

      if (appRes.status === 'fulfilled') {
        setPendingApplications(appRes.value.content || []);
        setPendingCount(appRes.value.totalElements || 0);
      }
      if (ingRes.status === 'fulfilled') {
        setIngredientTotal(ingRes.value.totalElements || 0);
        setRecentIngredients(ingRes.value.content || []);
      }
      if (optRes.status === 'fulfilled') {
        setCatalogOptions(optRes.value);
      }

      // Check if unauthorized or forbidden
      if (appRes.status === 'rejected' && ingRes.status === 'rejected') {
        setError('Không tải được một số dữ liệu quản trị. Kiểm tra quyền truy cập của tài khoản.');
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Lỗi tải dữ liệu bảng điều khiển.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadData();
  }, [loadData]);

  return (
    <div className="space-y-7 py-2">
      {/* 1. Command Center Hero Header */}
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <div className="mb-2 inline-flex items-center gap-2 rounded-full border border-leaf-200 bg-leaf-50/80 px-3 py-1 text-xs font-bold text-leaf-800">
            <ShieldCheck className="h-3.5 w-3.5 text-leaf-700" /> Trung tâm quản trị Mầm Xanh
          </div>
          <h1 className="text-2xl font-black tracking-tight text-ink sm:text-3xl lg:text-4xl">
            Bảng điều phối hệ thống
          </h1>
          <p className="mt-1.5 text-xs text-ink-muted sm:text-sm">
            Giám sát danh mục nguyên liệu, xét duyệt hồ sơ chuyên gia và theo dõi trạng thái tích hợp dịch vụ.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            onClick={() => void loadData()}
            disabled={loading}
            className="rounded-xl border-brand-200"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin text-brand-600' : ''}`} />
            Tải lại số liệu
          </Button>
          <Link
            to="/trang-chu"
            className="inline-flex items-center gap-1.5 rounded-xl border border-brand-200/80 bg-paper px-3.5 py-2 text-xs font-semibold text-ink-soft transition-colors hover:border-brand-500 hover:text-brand-600"
          >
            <ExternalLink className="h-3.5 w-3.5" /> Xem giao diện người dùng
          </Link>
        </div>
      </div>

      {error && (
        <div role="alert" className="flex items-center gap-3 rounded-2xl border border-amber-200 bg-amber-50 p-4 text-xs font-medium text-amber-950 sm:text-sm">
          <AlertCircle className="h-5 w-5 shrink-0 text-amber-600" />
          <span>{error}</span>
        </div>
      )}

      {/* 2. Live KPI Cards (Backed by real API values) */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* KPI 1: Pending Expert Applications */}
        <Card className="flex flex-col justify-between p-5 transition-shadow hover:shadow-md">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-[11px] font-bold uppercase tracking-wider text-ink-muted">
                Đơn Chuyên gia chờ duyệt
              </p>
              <p className="mt-2 text-3xl font-black text-ink">
                {loading ? <LoaderCircle className="h-8 w-8 animate-spin text-brand-600" /> : pendingCount}
              </p>
            </div>
            <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-brand-100 text-brand-600">
              <Award className="h-6 w-6" />
            </span>
          </div>
          <div className="mt-4 flex items-center justify-between border-t border-brand-100/70 pt-3">
            <span className={`inline-flex items-center gap-1 text-xs font-semibold ${pendingCount > 0 ? 'text-amber-700' : 'text-leaf-700'}`}>
              <Clock className="h-3.5 w-3.5" /> {pendingCount > 0 ? `${pendingCount} đơn cần xử lý` : 'Đã xử lý xong'}
            </span>
            <Link
              to="/admin/xet-duyet-chuyen-gia"
              className="inline-flex items-center gap-1 text-xs font-bold text-brand-600 hover:text-brand-700"
            >
              Xem hàng đợi <ArrowRight className="h-3 w-3" />
            </Link>
          </div>
        </Card>

        {/* KPI 2: Total Ingredients */}
        <Card className="flex flex-col justify-between p-5 transition-shadow hover:shadow-md">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-[11px] font-bold uppercase tracking-wider text-ink-muted">
                Nguyên liệu chuẩn hóa
              </p>
              <p className="mt-2 text-3xl font-black text-ink">
                {loading ? <LoaderCircle className="h-8 w-8 animate-spin text-brand-600" /> : ingredientTotal}
              </p>
            </div>
            <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-leaf-100 text-leaf-700">
              <Boxes className="h-6 w-6" />
            </span>
          </div>
          <div className="mt-4 flex items-center justify-between border-t border-brand-100/70 pt-3">
            <span className="inline-flex items-center gap-1 text-xs font-semibold text-leaf-700">
              <CheckCircle2 className="h-3.5 w-3.5" /> Chuẩn 9 chỉ tiêu vi chất
            </span>
            <Link
              to="/quan-tri/danh-muc"
              className="inline-flex items-center gap-1 text-xs font-bold text-brand-600 hover:text-brand-700"
            >
              Quản lý kho <ArrowRight className="h-3 w-3" />
            </Link>
          </div>
        </Card>

        {/* KPI 3: Food Groups */}
        <Card className="flex flex-col justify-between p-5 transition-shadow hover:shadow-md">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-[11px] font-bold uppercase tracking-wider text-ink-muted">
                Nhóm thực phẩm
              </p>
              <p className="mt-2 text-3xl font-black text-ink">
                {loading ? <LoaderCircle className="h-8 w-8 animate-spin text-brand-600" /> : catalogOptions.groups.length}
              </p>
            </div>
            <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-raised text-ink-soft">
              <Layers className="h-6 w-6" />
            </span>
          </div>
          <div className="mt-4 flex items-center justify-between border-t border-brand-100/70 pt-3">
            <span className="text-xs text-ink-muted">
              {catalogOptions.sources.length} nguồn kiểm định
            </span>
            <span className="text-xs font-semibold text-ink-soft">Danh mục chuẩn</span>
          </div>
        </Card>

        {/* KPI 4: Security & Role status */}
        <Card className="flex flex-col justify-between p-5 transition-shadow hover:shadow-md">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-[11px] font-bold uppercase tracking-wider text-ink-muted">
                Phiên quản trị viên
              </p>
              <p className="mt-2 truncate text-base font-extrabold text-ink">
                {account?.displayName || 'Administrator'}
              </p>
              <p className="truncate text-xs text-ink-muted">{account?.email || 'admin@mamxanh.local'}</p>
            </div>
            <span className="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl bg-leaf-100 text-leaf-800">
              <ShieldCheck className="h-6 w-6" />
            </span>
          </div>
          <div className="mt-4 flex items-center justify-between border-t border-brand-100/70 pt-3">
            <Badge tone="leaf">Quyền: ADMIN</Badge>
            <span className="text-xs font-medium text-leaf-700">Xác thực hợp lệ</span>
          </div>
        </Card>
      </div>

      {/* 3. Main Operational Sections */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* Left Column: Pending Expert Applications Queue */}
        <Card className="flex flex-col p-6 shadow-sm">
          <div className="flex items-center justify-between border-b border-brand-100/80 pb-4">
            <div className="flex items-center gap-2.5">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-100 text-brand-600">
                <Award className="h-4 w-4" />
              </span>
              <div>
                <h2 className="text-base font-bold text-ink">Đơn đăng ký Chuyên gia mới nhất</h2>
                <p className="text-xs text-ink-muted">Hồ sơ thành viên gửi yêu cầu nâng cấp vai trò</p>
              </div>
            </div>
            <Link
              to="/admin/xet-duyet-chuyen-gia"
              className="text-xs font-bold text-brand-600 hover:text-brand-700"
            >
              Xem tất cả ({pendingCount}) →
            </Link>
          </div>

          <div className="mt-4 flex-1 space-y-3">
            {loading ? (
              <div className="flex items-center justify-center py-12 text-xs text-ink-muted">
                <LoaderCircle className="mr-2 h-4 w-4 animate-spin text-brand-600" /> Đang tải danh sách...
              </div>
            ) : pendingApplications.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-center">
                <CheckCircle2 className="h-10 w-10 text-leaf-600" />
                <p className="mt-3 text-sm font-bold text-ink">Hàng đợi trống</p>
                <p className="mt-1 text-xs text-ink-muted">Tất cả đơn đăng ký Chuyên gia đã được xử lý.</p>
              </div>
            ) : (
              pendingApplications.slice(0, 4).map((app) => (
                <div
                  key={app.id}
                  className="flex items-start justify-between gap-3 rounded-xl border border-brand-100/80 bg-cream-soft/40 p-3.5 transition-colors hover:border-brand-300"
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="truncate text-xs font-bold text-ink">{app.displayName}</span>
                      <span className="rounded-md bg-brand-100 px-1.5 py-0.5 text-[10px] font-bold text-brand-700">
                        {app.vegetarianType}
                      </span>
                    </div>
                    <p className="mt-1 truncate text-xs text-ink-muted">{app.email}</p>
                    <p className="mt-1.5 line-clamp-1 text-xs text-ink-soft">
                      {app.experience || app.sampleRecipeSummary}
                    </p>
                  </div>
                  <Link
                    to="/admin/xet-duyet-chuyen-gia"
                    className="shrink-0 rounded-lg bg-brand-600 px-2.5 py-1.5 text-[11px] font-bold text-on-brand shadow-xs hover:bg-brand-500"
                  >
                    Xem & Duyệt
                  </Link>
                </div>
              ))
            )}
          </div>
        </Card>

        {/* Right Column: Platform Governance & Integration Status */}
        <Card className="flex flex-col p-6 shadow-sm">
          <div className="flex items-center justify-between border-b border-brand-100/80 pb-4">
            <div className="flex items-center gap-2.5">
              <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-leaf-100 text-leaf-700">
                <Database className="h-4 w-4" />
              </span>
              <div>
                <h2 className="text-base font-bold text-ink">Ma trận năng lực quản trị (RBAC)</h2>
                <p className="text-xs text-ink-muted">Theo đặc tả SRS và tiến độ hoàn thiện API Backend</p>
              </div>
            </div>
            <span className="rounded-full bg-leaf-50 px-2.5 py-1 text-[11px] font-bold text-leaf-800 border border-leaf-200">
              Hệ thống v1.0
            </span>
          </div>

          <div className="mt-4 divide-y divide-brand-100/60 text-xs">
            {/* Capability 1 */}
            <div className="flex items-center justify-between py-3">
              <div>
                <p className="font-bold text-ink">Danh mục nguyên liệu & dinh dưỡng</p>
                <p className="text-ink-muted">FR-42 · Chuẩn hóa 9 chỉ tiêu vi chất và quy đổi đơn vị</p>
              </div>
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-leaf-100 px-2.5 py-0.5 font-bold text-leaf-800">Sẵn sàng</span>
                <Link to="/quan-tri/danh-muc" className="font-bold text-brand-600 hover:underline">
                  Mở →
                </Link>
              </div>
            </div>

            {/* Capability 2 */}
            <div className="flex items-center justify-between py-3">
              <div>
                <p className="font-bold text-ink">Xét duyệt Chuyên gia ẩm thực</p>
                <p className="text-ink-muted">FR-27 · Thẩm định hồ sơ, ghi nhận quyết định và lý do</p>
              </div>
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-leaf-100 px-2.5 py-0.5 font-bold text-leaf-800">Sẵn sàng</span>
                <Link to="/admin/xet-duyet-chuyen-gia" className="font-bold text-brand-600 hover:underline">
                  Mở →
                </Link>
              </div>
            </div>

            {/* Capability 3 */}
            <div className="flex items-center justify-between py-3">
              <div>
                <p className="font-bold text-ink">Hậu kiểm nội dung & Báo cáo vi phạm</p>
                <p className="text-ink-muted">FR-06 · Hàng đợi kiểm duyệt bài viết và bình luận</p>
              </div>
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-amber-100 px-2.5 py-0.5 font-bold text-amber-900">Chờ API</span>
                <Link to="/admin/bao-cao" className="font-medium text-ink-muted hover:text-ink">
                  Chi tiết →
                </Link>
              </div>
            </div>

            {/* Capability 4 */}
            <div className="flex items-center justify-between py-3">
              <div>
                <p className="font-bold text-ink">Quản lý tài khoản & Chế tài người dùng</p>
                <p className="text-ink-muted">FR-01/02 · Tra cứu thành viên và khóa tài khoản vi phạm</p>
              </div>
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-amber-100 px-2.5 py-0.5 font-bold text-amber-900">Chờ API</span>
                <Link to="/admin/nguoi-dung" className="font-medium text-ink-muted hover:text-ink">
                  Chi tiết →
                </Link>
              </div>
            </div>

            {/* Capability 5 */}
            <div className="flex items-center justify-between py-3">
              <div>
                <p className="font-bold text-ink">Giám sát giao dịch gói AI hệ thống</p>
                <p className="text-ink-muted">FR-13 · Nhật ký thanh toán PayOS/VNPay trên toàn hệ thống</p>
              </div>
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-amber-100 px-2.5 py-0.5 font-bold text-amber-900">Chờ API</span>
                <Link to="/admin/giao-dich" className="font-medium text-ink-muted hover:text-ink">
                  Chi tiết →
                </Link>
              </div>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
