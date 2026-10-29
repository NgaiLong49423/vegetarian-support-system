import { useState, useMemo } from 'react';
import { Link } from 'react-router-dom';
import {
  ArrowLeft,
  Search,
  Users,
  Lock,
  Unlock,
  UserCheck,
  Eye,
  CheckCircle2,
  XCircle,
  X,
  Download,
} from 'lucide-react';
import { Card, Badge, Button } from '../components/ui';

type PendingModuleType = 'users';

export function AdminPendingModule({ module }: { module: PendingModuleType }) {
  return <>
    <div role="status" className="mb-5 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
      Giao diện minh họa · Dữ liệu mẫu. Thao tác tại đây chỉ thay đổi bản xem thử, chưa cập nhật tài khoản hoặc giao dịch trên Backend.
    </div>
    <AdminUserManagement />
  </>;
}

/* ==========================================================================
 * 1. Module Quản lý tài khoản & Phân quyền người dùng
 * ========================================================================== */
interface MockUser {
  id: string;
  name: string;
  email: string;
  role: 'ADMIN' | 'EXPERT' | 'USER';
  status: 'ACTIVE' | 'LOCKED';
  joinedDate: string;
  recipesCount: number;
  lockReason?: string;
}

const INITIAL_USERS: MockUser[] = [
  {
    id: 'USR-101',
    name: 'Nguyễn Thị Lan',
    email: 'lan.nguyen@gmail.com',
    role: 'EXPERT',
    status: 'ACTIVE',
    joinedDate: '12/03/2026',
    recipesCount: 18,
  },
  {
    id: 'USR-102',
    name: 'Trần Minh Đức',
    email: 'duc.tran@gmail.com',
    role: 'USER',
    status: 'ACTIVE',
    joinedDate: '05/04/2026',
    recipesCount: 2,
  },
  {
    id: 'USR-103',
    name: 'Hoàng Gia Bảo',
    email: 'bao.chef@vegan.vn',
    role: 'EXPERT',
    status: 'ACTIVE',
    joinedDate: '20/01/2026',
    recipesCount: 45,
  },
  {
    id: 'USR-104',
    name: 'Phạm Văn Hải',
    email: 'hai.pham99@spam.net',
    role: 'USER',
    status: 'LOCKED',
    joinedDate: '15/09/2026',
    recipesCount: 0,
    lockReason: 'Đăng tải nội dung quảng cáo spam liên tục trong phần bình luận',
  },
  {
    id: 'USR-105',
    name: 'Lê Thu Hằng',
    email: 'hang.le@mamxanh.vn',
    role: 'ADMIN',
    status: 'ACTIVE',
    joinedDate: '01/01/2026',
    recipesCount: 12,
  },
  {
    id: 'USR-106',
    name: 'Đặng Quốc Huy',
    email: 'huy.dang@yahoo.com',
    role: 'USER',
    status: 'ACTIVE',
    joinedDate: '28/08/2026',
    recipesCount: 5,
  },
  {
    id: 'USR-107',
    name: 'Vũ Thị Mai',
    email: 'mai.vu@outlook.com',
    role: 'USER',
    status: 'ACTIVE',
    joinedDate: '02/09/2026',
    recipesCount: 1,
  },
  {
    id: 'USR-108',
    name: 'Bùi Quang Thắng',
    email: 'thang.bui@toxic.org',
    role: 'USER',
    status: 'LOCKED',
    joinedDate: '10/10/2026',
    recipesCount: 0,
    lockReason: 'Ngôn từ xúc phạm, đả kích các thành viên khác trong diễn đàn',
  },
];

function AdminUserManagement() {
  const [users, setUsers] = useState<MockUser[]>(INITIAL_USERS);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');

  // Modals state
  const [selectedUser, setSelectedUser] = useState<MockUser | null>(null);
  const [modalMode, setModalMode] = useState<'VIEW' | 'LOCK' | 'ROLE' | null>(null);
  const [lockReasonInput, setLockReasonInput] = useState('');
  const [newRoleInput, setNewRoleInput] = useState<'ADMIN' | 'EXPERT' | 'USER'>('USER');
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToastMessage(msg);
    setTimeout(() => setToastMessage(null), 3000);
  };

  const filteredUsers = useMemo(() => {
    return users.filter((u) => {
      const matchSearch =
        u.name.toLowerCase().includes(search.toLowerCase()) ||
        u.email.toLowerCase().includes(search.toLowerCase()) ||
        u.id.toLowerCase().includes(search.toLowerCase());
      const matchRole = roleFilter === 'ALL' || u.role === roleFilter;
      const matchStatus = statusFilter === 'ALL' || u.status === statusFilter;
      return matchSearch && matchRole && matchStatus;
    });
  }, [users, search, roleFilter, statusFilter]);

  const activeCount = users.filter((u) => u.status === 'ACTIVE').length;
  const lockedCount = users.filter((u) => u.status === 'LOCKED').length;
  const expertCount = users.filter((u) => u.role === 'EXPERT').length;

  const handleToggleLock = () => {
    if (!selectedUser) return;
    if (selectedUser.status === 'ACTIVE') {
      setUsers((prev) =>
        prev.map((u) =>
          u.id === selectedUser.id
            ? { ...u, status: 'LOCKED', lockReason: lockReasonInput || 'Vi phạm tiêu chuẩn cộng đồng Mâm Xanh' }
            : u
        )
      );
      showToast(`Đã khóa tài khoản của ${selectedUser.name}`);
    } else {
      setUsers((prev) =>
        prev.map((u) =>
          u.id === selectedUser.id
            ? { ...u, status: 'ACTIVE', lockReason: undefined }
            : u
        )
      );
      showToast(`Đã mở khóa tài khoản cho ${selectedUser.name}`);
    }
    setModalMode(null);
    setSelectedUser(null);
    setLockReasonInput('');
  };

  const handleSaveRole = () => {
    if (!selectedUser) return;
    setUsers((prev) =>
      prev.map((u) => (u.id === selectedUser.id ? { ...u, role: newRoleInput } : u))
    );
    showToast(`Đã cập nhật vai trò ${newRoleInput} cho ${selectedUser.name}`);
    setModalMode(null);
    setSelectedUser(null);
  };

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
            <span className="text-xs font-semibold text-brand-700">Quản lý người dùng</span>
          </div>
          <h1 className="mt-1 text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">
            Quản lý tài khoản & Phân quyền
          </h1>
          <p className="mt-1 text-xs text-ink-muted sm:text-sm">
            Tra cứu thông tin thành viên, giám sát hoạt động và quản lý vai trò trong cộng đồng Mâm Xanh.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => showToast('Đang tải danh sách người dùng về định dạng Excel/CSV')}
          >
            <Download className="h-3.5 w-3.5" /> Xuất danh sách
          </Button>
        </div>
      </div>

      {toastMessage && (
        <div className="flex items-center gap-2 rounded-xl border border-leaf-200 bg-leaf-50 p-3 text-xs font-semibold text-leaf-800">
          <CheckCircle2 className="h-4 w-4 text-leaf-600 shrink-0" />
          <span>{toastMessage}</span>
        </div>
      )}

      {/* Summary Stats */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Card className="p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-ink-muted">Tổng thành viên</span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-brand-100 text-brand-700">
              <Users className="h-4 w-4" />
            </span>
          </div>
          <p className="mt-2 text-2xl font-extrabold text-ink">{users.length}</p>
          <span className="text-[11px] text-leaf-700 font-medium">Toàn hệ thống</span>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-ink-muted">Đang hoạt động</span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-leaf-100 text-leaf-700">
              <CheckCircle2 className="h-4 w-4" />
            </span>
          </div>
          <p className="mt-2 text-2xl font-extrabold text-leaf-700">{activeCount}</p>
          <span className="text-[11px] text-ink-muted">Tài khoản bình thường</span>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-ink-muted">Chuyên gia ẩm thực</span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-brand-100 text-brand-700">
              <UserCheck className="h-4 w-4" />
            </span>
          </div>
          <p className="mt-2 text-2xl font-extrabold text-brand-600">{expertCount}</p>
          <span className="text-[11px] text-ink-muted">Đã xác thực danh tính</span>
        </Card>

        <Card className="p-4">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-ink-muted">Đang bị khóa</span>
            <span className="flex h-7 w-7 items-center justify-center rounded-lg bg-red-100 text-red-700">
              <Lock className="h-4 w-4" />
            </span>
          </div>
          <p className="mt-2 text-2xl font-extrabold text-red-600">{lockedCount}</p>
          <span className="text-[11px] text-ink-muted">Chế tài vi phạm</span>
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
              placeholder="Tìm theo họ tên, email hoặc mã thành viên..."
              className="w-full rounded-xl border border-brand-200 bg-paper py-2 pl-9 pr-3 text-xs text-ink placeholder:text-ink-muted focus:border-brand-500 focus:outline-none"
            />
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <select
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
              className="rounded-xl border border-brand-200 bg-paper px-3 py-2 text-xs font-medium text-ink focus:border-brand-500 focus:outline-none"
            >
              <option value="ALL">Tất cả vai trò</option>
              <option value="USER">Thành viên (USER)</option>
              <option value="EXPERT">Chuyên gia (EXPERT)</option>
              <option value="ADMIN">Quản trị viên (ADMIN)</option>
            </select>

            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="rounded-xl border border-brand-200 bg-paper px-3 py-2 text-xs font-medium text-ink focus:border-brand-500 focus:outline-none"
            >
              <option value="ALL">Tất cả trạng thái</option>
              <option value="ACTIVE">Đang hoạt động</option>
              <option value="LOCKED">Đang bị khóa</option>
            </select>
          </div>
        </div>
      </Card>

      {/* Users Table */}
      <Card className="overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-brand-100 bg-brand-50/50 text-ink-muted">
              <tr>
                <th className="px-4 py-3 font-bold">Thành viên</th>
                <th className="px-4 py-3 font-bold">Vai trò</th>
                <th className="px-4 py-3 font-bold">Ngày tham gia</th>
                <th className="px-4 py-3 font-bold">Công thức</th>
                <th className="px-4 py-3 font-bold">Trạng thái</th>
                <th className="px-4 py-3 text-right font-bold">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-brand-100/60 text-ink">
              {filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-8 text-center text-ink-muted">
                    Không tìm thấy thành viên nào phù hợp với bộ lọc tìm kiếm.
                  </td>
                </tr>
              ) : (
                filteredUsers.map((user) => (
                  <tr key={user.id} className="transition-colors hover:bg-brand-50/30">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-100 font-bold text-brand-700">
                          {user.name.charAt(0)}
                        </div>
                        <div>
                          <p className="font-bold text-ink">{user.name}</p>
                          <p className="text-[11px] text-ink-muted">{user.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      {user.role === 'ADMIN' ? (
                        <Badge tone="brand">Quản trị viên</Badge>
                      ) : user.role === 'EXPERT' ? (
                        <Badge tone="leaf">Chuyên gia</Badge>
                      ) : (
                        <Badge tone="neutral">Thành viên</Badge>
                      )}
                    </td>
                    <td className="px-4 py-3 text-ink-muted">{user.joinedDate}</td>
                    <td className="px-4 py-3 font-semibold">{user.recipesCount} món</td>
                    <td className="px-4 py-3">
                      {user.status === 'ACTIVE' ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-leaf-100 px-2 py-0.5 text-[11px] font-bold text-leaf-800">
                          <span className="h-1.5 w-1.5 rounded-full bg-leaf-600" />
                          Hoạt động
                        </span>
                      ) : (
                        <span className="inline-flex items-center gap-1 rounded-full bg-red-100 px-2 py-0.5 text-[11px] font-bold text-red-800" title={user.lockReason}>
                          <Lock className="h-3 w-3 text-red-600" />
                          Đã khóa
                        </span>
                      )}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          type="button"
                          title="Xem chi tiết hồ sơ"
                          onClick={() => {
                            setSelectedUser(user);
                            setModalMode('VIEW');
                          }}
                          className="rounded-lg p-1.5 text-ink-muted transition-colors hover:bg-brand-100 hover:text-ink"
                        >
                          <Eye className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          title="Phân quyền vai trò"
                          onClick={() => {
                            setSelectedUser(user);
                            setNewRoleInput(user.role);
                            setModalMode('ROLE');
                          }}
                          className="rounded-lg p-1.5 text-ink-muted transition-colors hover:bg-brand-100 hover:text-brand-600"
                        >
                          <UserCheck className="h-4 w-4" />
                        </button>
                        <button
                          type="button"
                          title={user.status === 'ACTIVE' ? 'Khóa tài khoản' : 'Mở khóa tài khoản'}
                          onClick={() => {
                            setSelectedUser(user);
                            setModalMode('LOCK');
                          }}
                          className={`rounded-lg p-1.5 transition-colors ${
                            user.status === 'ACTIVE'
                              ? 'text-ink-muted hover:bg-red-50 hover:text-red-600'
                              : 'text-leaf-600 hover:bg-leaf-50 hover:text-leaf-700'
                          }`}
                        >
                          {user.status === 'ACTIVE' ? <Lock className="h-4 w-4" /> : <Unlock className="h-4 w-4" />}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>

      {/* MODAL: Xem chi tiết người dùng */}
      {modalMode === 'VIEW' && selectedUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
          <Card className="w-full max-w-lg p-6 shadow-xl animate-in fade-in zoom-in-95">
            <div className="flex items-center justify-between border-b border-brand-100 pb-3">
              <h3 className="text-base font-bold text-ink">Hồ sơ chi tiết thành viên</h3>
              <button
                type="button"
                onClick={() => setModalMode(null)}
                className="rounded-lg p-1 text-ink-muted hover:bg-brand-100"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="mt-4 space-y-4 text-xs">
              <div className="flex items-center gap-4">
                <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-100 text-xl font-bold text-brand-700">
                  {selectedUser.name.charAt(0)}
                </div>
                <div>
                  <h4 className="text-base font-bold text-ink">{selectedUser.name}</h4>
                  <p className="text-ink-muted">{selectedUser.email}</p>
                  <p className="mt-1 font-mono text-[11px] text-ink-muted">Mã ID: {selectedUser.id}</p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 rounded-xl border border-brand-100 bg-brand-50/40 p-3">
                <div>
                  <span className="text-ink-muted">Vai trò hệ thống:</span>
                  <p className="font-bold text-ink">{selectedUser.role}</p>
                </div>
                <div>
                  <span className="text-ink-muted">Trạng thái:</span>
                  <p className="font-bold text-ink">
                    {selectedUser.status === 'ACTIVE' ? 'Đang hoạt động' : 'Tạm thời bị khóa'}
                  </p>
                </div>
                <div>
                  <span className="text-ink-muted">Ngày gia nhập:</span>
                  <p className="font-bold text-ink">{selectedUser.joinedDate}</p>
                </div>
                <div>
                  <span className="text-ink-muted">Số công thức đóng góp:</span>
                  <p className="font-bold text-ink">{selectedUser.recipesCount} công thức</p>
                </div>
              </div>

              {selectedUser.lockReason && (
                <div className="rounded-xl border border-red-200 bg-red-50/80 p-3 text-red-800">
                  <p className="font-bold">Lý do khóa tài khoản:</p>
                  <p className="mt-0.5">{selectedUser.lockReason}</p>
                </div>
              )}
            </div>

            <div className="mt-6 flex justify-end gap-2 border-t border-brand-100 pt-4">
              <Button variant="outline" size="sm" onClick={() => setModalMode(null)}>
                Đóng
              </Button>
            </div>
          </Card>
        </div>
      )}

      {/* MODAL: Khóa / Mở khóa tài khoản */}
      {modalMode === 'LOCK' && selectedUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
          <Card className="w-full max-w-md p-6 shadow-xl animate-in fade-in zoom-in-95">
            <div className="flex items-center gap-3">
              <span className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${
                selectedUser.status === 'ACTIVE' ? 'bg-red-100 text-red-700' : 'bg-leaf-100 text-leaf-700'
              }`}>
                {selectedUser.status === 'ACTIVE' ? <Lock className="h-5 w-5" /> : <Unlock className="h-5 w-5" />}
              </span>
              <div>
                <h3 className="text-base font-bold text-ink">
                  {selectedUser.status === 'ACTIVE' ? 'Khóa tài khoản thành viên' : 'Mở khóa tài khoản'}
                </h3>
                <p className="text-xs text-ink-muted">{selectedUser.name} ({selectedUser.email})</p>
              </div>
            </div>

            {selectedUser.status === 'ACTIVE' ? (
              <div className="mt-4 space-y-3 text-xs">
                <p className="text-ink-soft">
                  Khi bị khóa, người dùng sẽ không thể đăng nhập, không thể chia sẻ công thức hoặc bình luận trên cộng đồng.
                </p>
                <div>
                  <label className="block font-bold text-ink mb-1">Lý do khóa tài khoản *</label>
                  <textarea
                    rows={3}
                    value={lockReasonInput}
                    onChange={(e) => setLockReasonInput(e.target.value)}
                    placeholder="Nhập chi tiết vi phạm tiêu chuẩn cộng đồng, spam quảng cáo..."
                    className="w-full rounded-xl border border-brand-200 bg-paper p-2.5 text-xs text-ink focus:border-brand-500 focus:outline-none"
                  />
                </div>
              </div>
            ) : (
              <p className="mt-4 text-xs text-ink-soft">
                Bạn có chắc chắn muốn mở khóa cho tài khoản này? Người dùng sẽ được phép đăng nhập và hoạt động bình thường trở lại.
              </p>
            )}

            <div className="mt-6 flex justify-end gap-2 border-t border-brand-100 pt-4">
              <Button variant="outline" size="sm" onClick={() => setModalMode(null)}>
                Hủy bỏ
              </Button>
              <Button
                variant={selectedUser.status === 'ACTIVE' ? 'primary' : 'secondary'}
                size="sm"
                onClick={handleToggleLock}
              >
                {selectedUser.status === 'ACTIVE' ? 'Xác nhận khóa' : 'Xác nhận mở khóa'}
              </Button>
            </div>
          </Card>
        </div>
      )}

      {/* MODAL: Phân quyền vai trò */}
      {modalMode === 'ROLE' && selectedUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-xs">
          <Card className="w-full max-w-md p-6 shadow-xl animate-in fade-in zoom-in-95">
            <div className="flex items-center gap-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-100 text-brand-700">
                <UserCheck className="h-5 w-5" />
              </span>
              <div>
                <h3 className="text-base font-bold text-ink">Phân quyền vai trò</h3>
                <p className="text-xs text-ink-muted">{selectedUser.name}</p>
              </div>
            </div>

            <div className="mt-4 space-y-3 text-xs">
              <p className="text-ink-soft">Lựa chọn vai trò mới cho tài khoản:</p>
              <div className="space-y-2">
                {[
                  { id: 'USER', label: 'Thành viên (USER)', desc: 'Người dùng thông thường, xem và đăng bài' },
                  { id: 'EXPERT', label: 'Chuyên gia (EXPERT)', desc: 'Có tích xanh xác thực, đăng công thức chuẩn' },
                  { id: 'ADMIN', label: 'Quản trị viên (ADMIN)', desc: 'Toàn quyền quản trị nội dung và hệ thống' },
                ].map((r) => (
                  <label
                    key={r.id}
                    className={`flex items-start gap-2.5 rounded-xl border p-3 cursor-pointer transition-colors ${
                      newRoleInput === r.id ? 'border-brand-500 bg-brand-50/60' : 'border-brand-100 hover:bg-brand-50/30'
                    }`}
                  >
                    <input
                      type="radio"
                      name="userRole"
                      checked={newRoleInput === r.id}
                      onChange={() => setNewRoleInput(r.id as any)}
                      className="mt-0.5 text-brand-600 focus:ring-brand-500"
                    />
                    <div>
                      <p className="font-bold text-ink">{r.label}</p>
                      <p className="text-[11px] text-ink-muted">{r.desc}</p>
                    </div>
                  </label>
                ))}
              </div>
            </div>

            <div className="mt-6 flex justify-end gap-2 border-t border-brand-100 pt-4">
              <Button variant="outline" size="sm" onClick={() => setModalMode(null)}>
                Hủy bỏ
              </Button>
              <Button size="sm" onClick={handleSaveRole}>
                Lưu phân quyền
              </Button>
            </div>
          </Card>
        </div>
      )}
    </div>
  );
}
