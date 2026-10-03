import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  AlertCircle,
  Award,
  CheckCircle2,
  Clock,
  Lock,
  Plus,
  XCircle,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Badge, Button, Card } from '../components/ui';
import { useDemoAccount } from '../components/DemoAccount';
import { Modal } from '../components/Modal';
import type { DietTag, ExpertApplication } from '../types';

const dietaryStyles: DietTag[] = ['Thuần Chay', 'Lacto', 'Ovo', 'Lacto-Ovo'];

export function ExpertApplicationPage() {
  const navigate = useNavigate();
  const {
    active,
    role,
    setRole,
    applicationStatus,
    rejectionReason,
    pendingAppId,
    applications,
    submitApplication,
    reviewApplication,
  } = useDemoAccount();

  // Form states
  const [experience, setExperience] = useState('');
  const [dietaryStyle, setDietaryStyle] = useState<DietTag>('Thuần Chay');
  const [sampleRecipe, setSampleRecipe] = useState('');

  const [formState, setFormState] = useState<
      'initial' | 'validation_error' | 'submitting' | 'api_error' | 'success'
  >('initial');
  const [validationErrors, setValidationErrors] = useState<{
    experience?: string;
    sampleRecipe?: string;
  }>({});
  const [isResubmitting, setIsResubmitting] = useState(false);

  // Admin view states
  const [adminFilter, setAdminFilter] = useState<'ALL' | 'PENDING' | 'APPROVED' | 'REJECTED'>(
      'ALL',
  );
  const [selectedApp, setSelectedApp] = useState<ExpertApplication | null>(null);
  const [rejectReasonInput, setRejectReasonInput] = useState('');
  const [rejectError, setRejectError] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const errors: { experience?: string; sampleRecipe?: string } = {};

    if (experience.trim().length < 20) {
      errors.experience = 'Kinh nghiệm ẩm thực chay cần tối thiểu 20 ký tự (theo FR-05).';
    }
    if (sampleRecipe.trim().length < 30) {
      errors.sampleRecipe = 'Tóm tắt công thức mẫu cần tối thiểu 30 ký tự (theo FR-05).';
    }

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      setFormState('validation_error');
      return;
    }

    setValidationErrors({});
    setFormState('submitting');

    setTimeout(() => {
      const res = submitApplication({
        experience: experience.trim(),
        dietaryStyle,
        sampleRecipe: sampleRecipe.trim(),
      });

      if (res.ok) {
        setFormState('success');
        setIsResubmitting(false);
      } else {
        setFormState('api_error');
      }
    }, 400);
  };

  const handleSimulateApiError = () => {
    setFormState('submitting');
    setTimeout(() => {
      setFormState('api_error');
    }, 400);
  };

  const handleApprove = (id: string) => {
    reviewApplication(id, 'APPROVE');
    setSelectedApp(null);
  };

  const handleReject = (id: string) => {
    if (!rejectReasonInput.trim()) {
      setRejectError('Bắt buộc nhập lý do khi từ chối đơn đăng ký (theo FR-05).');
      return;
    }
    setRejectError('');
    reviewApplication(id, 'REJECT', rejectReasonInput.trim());
    setRejectReasonInput('');
    setSelectedApp(null);
  };

  const filteredApps = applications.filter((app) => {
    if (adminFilter === 'ALL') return true;
    return app.status === adminFilter;
  });

  return (
      <PageContainer className="py-8">
        {/* Top Banner */}
        <div className="mb-8">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <div className="flex items-center gap-2">
                <Badge tone="leaf">Cộng đồng Mâm Xanh</Badge>
                <Badge tone="brand">FR-04 · FR-05</Badge>
              </div>
              <h1 className="mt-2 text-3xl font-extrabold text-ink sm:text-4xl">
                Đăng ký & Xét duyệt Chuyên gia
              </h1>
              <p className="mt-1 text-sm text-ink-muted">
                Quy trình xét duyệt năng lực chia sẻ công thức chay dựa trên văn bản, không yêu cầu
                bằng cấp hay giấy tờ vật lý.
              </p>
            </div>

            {/* Role switcher tab */}
            <div className="flex items-center gap-1 rounded-2xl border border-brand-100 bg-white p-1.5 shadow-sm">
              <span className="px-2 text-xs font-bold text-ink-muted">Góc nhìn:</span>
              <button
                  type="button"
                  onClick={() => setRole('CUSTOMER')}
                  className={`rounded-xl px-3 py-1.5 text-xs font-bold transition ${
                      role === 'CUSTOMER'
                          ? 'bg-brand-600 text-white shadow-sm'
                          : 'text-ink-soft hover:bg-brand-50'
                  }`}
              >
                Customer
              </button>
              <button
                  type="button"
                  onClick={() => setRole('ADMIN')}
                  className={`rounded-xl px-3 py-1.5 text-xs font-bold transition ${
                      role === 'ADMIN'
                          ? 'bg-amber-600 text-white shadow-sm'
                          : 'text-ink-soft hover:bg-brand-50'
                  }`}
              >
                Admin Duyệt
              </button>
              <button
                  type="button"
                  onClick={() => setRole('EXPERT')}
                  className={`rounded-xl px-3 py-1.5 text-xs font-bold transition ${
                      role === 'EXPERT'
                          ? 'bg-leaf-600 text-white shadow-sm'
                          : 'text-ink-soft hover:bg-brand-50'
                  }`}
              >
                Chuyên gia
              </button>
            </div>
          </div>
        </div>

        {/* VIEW: GUEST */}
        {!active && (
            <Card className="mx-auto max-w-xl p-8 text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-amber-100 text-amber-600">
                <Lock className="h-8 w-8" />
              </div>
              <h2 className="mt-4 text-xl font-bold text-ink">Yêu cầu đăng nhập</h2>
              <p className="mt-2 text-sm leading-relaxed text-ink-muted">
                Khách vãng lai cần đăng nhập tài khoản Mâm Xanh trước khi nộp đơn đăng ký trở thành
                Chuyên gia ẩm thực.
              </p>
              <div className="mt-6 flex justify-center gap-3">
                <Link
                    to="/dang-nhap"
                    className="rounded-xl bg-brand-600 px-6 py-2.5 text-sm font-bold text-white hover:bg-brand-700"
                >
                  Đăng nhập ngay
                </Link>
                <Link
                    to="/"
                    className="rounded-xl border border-brand-200 bg-white px-5 py-2.5 text-sm font-semibold text-ink-soft hover:bg-brand-50"
                >
                  Quay lại Khám phá
                </Link>
              </div>
            </Card>
        )}

        {/* VIEW: EXPERT */}
        {active && role === 'EXPERT' && (
            <Card className="mx-auto max-w-2xl border-leaf-200 bg-leaf-50/40 p-8 text-center">
              <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-leaf-100 text-leaf-600">
                <Award className="h-8 w-8" />
              </div>
              <span className="mt-3 inline-block rounded-full bg-leaf-200 px-3 py-1 text-xs font-bold text-leaf-800">
            Đã được chứng thực
          </span>
              <h2 className="mt-3 text-2xl font-extrabold text-ink">Bạn là Chuyên gia ẩm thực chay</h2>
              <p className="mx-auto mt-2 max-w-md text-sm leading-relaxed text-ink-soft">
                Hồ sơ Chuyên gia của bạn đã được phê duyệt. Bạn có đầy đủ quyền chia sẻ bài viết và đăng
                tải công thức ẩm thực độc quyền lên cộng đồng.
              </p>
              <div className="mt-6 flex justify-center gap-3">
                <Button onClick={() => navigate('/dang-cong-thuc')}>
                  <Plus className="h-4 w-4" /> Đăng công thức mới
                </Button>
                <Button variant="outline" onClick={() => navigate('/ho-so')}>
                  Xem hồ sơ của tôi
                </Button>
              </div>
            </Card>
        )}

        {/* VIEW: ADMIN (XÉT DUYỆT ĐƠN) */}
        {active && role === 'ADMIN' && (
            <div className="space-y-6">
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-brand-100 pb-4">
                <div>
                  <h2 className="text-xl font-bold text-ink">
                    Bảng xét duyệt đơn đăng ký Chuyên gia ({filteredApps.length})
                  </h2>
                  <p className="text-xs text-ink-muted">
                    Dành cho Quản trị viên thẩm định kinh nghiệm và công thức mẫu của thành viên.
                  </p>
                </div>
                <div className="flex gap-1.5">
                  {(['ALL', 'PENDING', 'APPROVED', 'REJECTED'] as const).map((st) => (
                      <button
                          key={st}
                          type="button"
                          onClick={() => setAdminFilter(st)}
                          className={`rounded-lg px-3 py-1.5 text-xs font-bold transition ${
                              adminFilter === st
                                  ? 'bg-amber-700 text-white'
                                  : 'border border-brand-200 bg-white text-ink-soft hover:bg-brand-50'
                          }`}
                      >
                        {st === 'ALL' ? 'Tất cả' : st}
                      </button>
                  ))}
                </div>
              </div>

              <div className="grid gap-4">
                {filteredApps.map((app) => (
                    <Card key={app.id} className="p-5 transition hover:shadow-md">
                      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-brand-50 pb-3">
                        <div className="flex items-center gap-3">
                          <span className="font-mono text-sm font-extrabold text-brand-700">{app.id}</span>
                          <span className="text-sm font-bold text-ink">{app.applicantName}</span>
                          <span className="text-xs text-ink-muted">({app.applicantEmail})</span>
                        </div>
                        <div className="flex items-center gap-2">
                    <span
                        className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                            app.status === 'APPROVED'
                                ? 'bg-leaf-100 text-leaf-700'
                                : app.status === 'PENDING'
                                    ? 'bg-amber-100 text-amber-800'
                                    : 'bg-rose-100 text-rose-700'
                        }`}
                    >
                      {app.status}
                    </span>
                          <span className="text-xs text-ink-muted">{app.submittedAt}</span>
                        </div>
                      </div>

                      <div className="mt-3 grid gap-3 sm:grid-cols-2">
                        <div>
                    <span className="block text-xs font-bold uppercase tracking-wider text-ink-muted">
                      Trường phái:
                    </span>
                          <span className="text-sm font-semibold text-leaf-700">{app.dietaryStyle}</span>
                        </div>
                        <div>
                    <span className="block text-xs font-bold uppercase tracking-wider text-ink-muted">
                      Kinh nghiệm ẩm thực chay:
                    </span>
                          <p className="line-clamp-2 text-sm text-ink-soft">{app.experience}</p>
                        </div>
                      </div>

                      {app.rejectionReason && (
                          <div className="mt-3 rounded-lg bg-rose-50 p-2.5 text-xs text-rose-800">
                            <strong>Lý do từ chối:</strong> {app.rejectionReason}
                          </div>
                      )}

                      <div className="mt-4 flex justify-end gap-2">
                        <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              setSelectedApp(app);
                              setRejectReasonInput(app.rejectionReason || '');
                            }}
                        >
                          Xem chi tiết & Thẩm định
                        </Button>
                      </div>
                    </Card>
                ))}
              </div>

              {/* Modal Admin Review */}
              <Modal
                  open={!!selectedApp}
                  onClose={() => setSelectedApp(null)}
                  title={`Thẩm định đơn đăng ký ${selectedApp?.id}`}
              >
                {selectedApp && (
                    <div className="space-y-4">
                      <div>
                        <span className="block text-xs font-bold text-ink-muted">Ứng viên:</span>
                        <p className="text-base font-bold text-ink">
                          {selectedApp.applicantName} ({selectedApp.applicantEmail})
                        </p>
                        <p className="text-xs text-ink-muted">
                          Trường phái: <strong>{selectedApp.dietaryStyle}</strong>
                        </p>
                      </div>

                      <div className="rounded-xl border border-brand-100 bg-brand-50/40 p-4">
                        <span className="block text-xs font-bold text-ink">Kinh nghiệm ẩm thực chay:</span>
                        <p className="mt-1 whitespace-pre-wrap text-sm leading-relaxed text-ink-soft">
                          {selectedApp.experience}
                        </p>
                      </div>

                      <div className="rounded-xl border border-brand-100 bg-brand-50/40 p-4">
                        <span className="block text-xs font-bold text-ink">Tóm tắt công thức mẫu:</span>
                        <p className="mt-1 whitespace-pre-wrap text-sm leading-relaxed text-ink-soft">
                          {selectedApp.sampleRecipe}
                        </p>
                      </div>

                      {selectedApp.status === 'PENDING' ? (
                          <div className="space-y-3 border-t border-brand-100 pt-4">
                            <label className="block text-xs font-bold text-ink">
                              Lý do từ chối (bắt buộc khi bấm Từ chối):
                            </label>
                            <textarea
                                value={rejectReasonInput}
                                onChange={(e) => {
                                  setRejectReasonInput(e.target.value);
                                  setRejectError('');
                                }}
                                placeholder="Nêu rõ lý do từ chối để ứng viên có thể bổ sung..."
                                rows={2}
                                className="w-full rounded-xl border border-brand-200 p-2.5 text-sm text-ink outline-none focus:border-rose-400"
                            />
                            {rejectError && (
                                <p className="text-xs font-semibold text-rose-600">{rejectError}</p>
                            )}

                            <div className="flex justify-end gap-2 pt-2">
                              <button
                                  type="button"
                                  onClick={() => handleReject(selectedApp.id)}
                                  className="rounded-xl border border-rose-300 bg-rose-50 px-4 py-2 text-sm font-bold text-rose-700 hover:bg-rose-100"
                              >
                                Từ chối đơn
                              </button>
                              <button
                                  type="button"
                                  onClick={() => handleApprove(selectedApp.id)}
                                  className="rounded-xl bg-leaf-600 px-5 py-2 text-sm font-bold text-white hover:bg-leaf-700"
                              >
                                Phê duyệt Chuyên gia
                              </button>
                            </div>
                          </div>
                      ) : (
                          <div className="border-t border-brand-100 pt-3 text-right">
                            <Button variant="outline" onClick={() => setSelectedApp(null)}>
                              Đóng
                            </Button>
                          </div>
                      )}
                    </div>
                )}
              </Modal>
            </div>
        )}

        {/* VIEW: CUSTOMER (FORM & STATUS) */}
        {active && role === 'CUSTOMER' && (
            <div className="mx-auto max-w-3xl">
              {/* PENDING STATE */}
              {applicationStatus === 'PENDING' && !isResubmitting && (
                  <Card className="border-brand-200 bg-brand-50/30 p-8 text-center">
                    <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-brand-100 text-brand-600">
                      <Clock className="h-8 w-8" />
                    </div>
                    <span className="mt-3 inline-block rounded-full bg-brand-100 px-3 py-1 text-xs font-bold text-brand-800">
                PENDING · Đang chờ phê duyệt
              </span>
                    <h2 className="mt-3 text-2xl font-extrabold text-ink">
                      Đơn đăng ký của bạn đang được xử lý
                    </h2>
                    <p className="mx-auto mt-2 max-w-md text-sm leading-relaxed text-ink-muted">
                      Mã đơn:{' '}
                      <strong className="font-mono text-brand-700">
                        {pendingAppId || 'EX-24019'}
                      </strong>
                      . Quản trị viên đang xem xét thông tin kinh nghiệm và công thức mẫu của bạn.
                    </p>
                    <div className="mx-auto mt-4 max-w-md rounded-xl border border-brand-100 bg-white p-4 text-xs text-ink-soft">
                      🔒 <strong>Quy tắc bảo vệ (FR-05):</strong> Bạn đang có một đơn ở trạng thái PENDING.
                      Hệ thống chặn gửi đơn thứ hai cho đến khi có kết quả xét duyệt chính thức.
                    </div>
                    <div className="mt-6 flex justify-center gap-3">
                      <Button variant="outline" onClick={() => navigate('/ho-so')}>
                        Về hồ sơ của tôi
                      </Button>
                    </div>
                  </Card>
              )}

              {/* REJECTED STATE */}
              {applicationStatus === 'REJECTED' && !isResubmitting && (
                  <Card className="border-rose-200 bg-rose-50/30 p-8">
                    <div className="flex items-start gap-4">
                      <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-rose-100 text-rose-600">
                        <XCircle className="h-6 w-6" />
                      </div>
                      <div className="flex-1">
                  <span className="rounded-full bg-rose-100 px-2.5 py-0.5 text-xs font-bold text-rose-800">
                    REJECTED · Đơn bị từ chối
                  </span>
                        <h2 className="mt-2 text-xl font-bold text-ink">
                          Đơn đăng ký Chuyên gia chưa được phê duyệt
                        </h2>
                        <div className="mt-3 rounded-xl border border-rose-200 bg-white p-4">
                    <span className="block text-xs font-bold text-ink-muted">
                      Lý do từ chối từ Quản trị viên:
                    </span>
                          <p className="mt-1 text-sm font-semibold text-rose-800">
                            {rejectionReason ||
                                'Cần bổ sung chi tiết kinh nghiệm nấu ăn chay và tóm tắt công thức rõ ràng hơn.'}
                          </p>
                        </div>
                        <p className="mt-3 text-xs text-ink-muted">
                          Bạn có thể cập nhật lại thông tin để nộp lại đơn xét duyệt mới theo hướng dẫn trên.
                        </p>
                        <div className="mt-5">
                          <Button onClick={() => setIsResubmitting(true)}>Nộp lại đơn đăng ký mới</Button>
                        </div>
                      </div>
                    </div>
                  </Card>
              )}

              {/* FORM: NEW APPLICATION (or resubmitting) */}
              {(applicationStatus === 'DRAFT' || isResubmitting) && (
                  <Card className="p-6 sm:p-8">
                    <div className="border-b border-brand-100 pb-5">
                      <h2 className="text-xl font-extrabold text-ink sm:text-2xl">
                        Biểu mẫu Đăng ký Chuyên gia ẩm thực
                      </h2>
                      <p className="mt-1 text-xs text-ink-muted">
                        Vui lòng điền thông tin dạng văn bản rõ ràng. Không yêu cầu tải lên chứng chỉ hay
                        bằng cấp vật lý.
                      </p>
                    </div>

                    <form onSubmit={handleSubmit} className="mt-6 space-y-6">
                      {/* Field 1: Kinh nghiệm */}
                      <div>
                        <div className="flex items-center justify-between">
                          <label htmlFor="culinaryExperience" className="text-sm font-bold text-ink">
                            Kinh nghiệm ẩm thực chay <span className="text-rose-600">*</span>
                          </label>
                          <span
                              className={`text-xs ${
                                  experience.length < 20
                                      ? 'font-semibold text-rose-600'
                                      : 'text-ink-muted'
                              }`}
                          >
                      {experience.length}/20 ký tự tối thiểu
                    </span>
                        </div>
                        <textarea
                            id="culinaryExperience"
                            rows={4}
                            value={experience}
                            onChange={(e) => {
                              setExperience(e.target.value);
                              if (validationErrors.experience)
                                setValidationErrors((prev) => ({ ...prev, experience: undefined }));
                            }}
                            placeholder="Mô tả quá trình thực hành nấu chay, các phong cách ẩm thực bạn am hiểu hoặc thời gian theo đuổi ẩm thực thuần thực vật..."
                            className="mt-2 w-full rounded-xl border border-brand-200 p-3.5 text-sm text-ink outline-none transition focus:border-leaf-600 focus:ring-2 focus:ring-leaf-100"
                        />
                        {validationErrors.experience && (
                            <p className="mt-1.5 flex items-center gap-1 text-xs font-semibold text-rose-600">
                              <AlertCircle className="h-3.5 w-3.5" /> {validationErrors.experience}
                            </p>
                        )}
                      </div>

                      {/* Field 2: Trường phái chay */}
                      <div>
                        <label htmlFor="dietaryStyle" className="text-sm font-bold text-ink">
                          Trường phái ẩm thực chay chủ đạo <span className="text-rose-600">*</span>
                        </label>
                        <select
                            id="dietaryStyle"
                            value={dietaryStyle}
                            onChange={(e) => setDietaryStyle(e.target.value as DietTag)}
                            className="mt-2 w-full rounded-xl border border-brand-200 bg-white p-3 text-sm text-ink outline-none transition focus:border-leaf-600 focus:ring-2 focus:ring-leaf-100"
                        >
                          {dietaryStyles.map((style) => (
                              <option key={style} value={style}>
                                {style}
                              </option>
                          ))}
                        </select>
                      </div>

                      {/* Field 3: Tóm tắt công thức mẫu */}
                      <div>
                        <div className="flex items-center justify-between">
                          <label htmlFor="sampleRecipe" className="text-sm font-bold text-ink">
                            Tóm tắt công thức món chay tâm đắc <span className="text-rose-600">*</span>
                          </label>
                          <span
                              className={`text-xs ${
                                  sampleRecipe.length < 30
                                      ? 'font-semibold text-rose-600'
                                      : 'text-ink-muted'
                              }`}
                          >
                      {sampleRecipe.length}/30 ký tự tối thiểu
                    </span>
                        </div>
                        <textarea
                            id="sampleRecipe"
                            rows={4}
                            value={sampleRecipe}
                            onChange={(e) => {
                              setSampleRecipe(e.target.value);
                              if (validationErrors.sampleRecipe)
                                setValidationErrors((prev) => ({ ...prev, sampleRecipe: undefined }));
                            }}
                            placeholder="Tóm tắt một công thức bạn tự tin nhất: tên món, nguyên liệu điểm nhấn và bí quyết chế biến..."
                            className="mt-2 w-full rounded-xl border border-brand-200 p-3.5 text-sm text-ink outline-none transition focus:border-leaf-600 focus:ring-2 focus:ring-leaf-100"
                        />
                        {validationErrors.sampleRecipe && (
                            <p className="mt-1.5 flex items-center gap-1 text-xs font-semibold text-rose-600">
                              <AlertCircle className="h-3.5 w-3.5" /> {validationErrors.sampleRecipe}
                            </p>
                        )}
                      </div>

                      {/* Notice: Không yêu cầu bằng cấp/giấy tờ */}
                      <div className="rounded-xl border border-brand-100 bg-brand-50/60 p-3 text-xs leading-relaxed text-ink-muted">
                        <strong className="text-brand-700">Lưu ý quan trọng (FR-05):</strong> Quy trình này
                        xét duyệt năng lực chia sẻ công thức dựa trên văn bản tự khai báo. Hệ thống{' '}
                        <strong>không yêu cầu</strong> bằng cấp, chứng chỉ, giấy tờ tùy thân hay bất kỳ tệp
                        vật lý nào.
                      </div>

                      {/* Status messages */}
                      {formState === 'api_error' && (
                          <div
                              role="alert"
                              className="flex items-center gap-2 rounded-xl bg-rose-50 p-4 text-xs font-semibold text-rose-700"
                          >
                            <AlertCircle className="h-4 w-4 shrink-0" />
                            Không thể gửi đơn đăng ký lúc này. Vui lòng kiểm tra kết nối mạng và thử lại.
                          </div>
                      )}

                      {formState === 'success' && (
                          <div
                              role="status"
                              className="flex items-center gap-2 rounded-xl bg-leaf-50 p-4 text-xs font-semibold text-leaf-700"
                          >
                            <CheckCircle2 className="h-4 w-4 shrink-0" />
                            Đã gửi đơn đăng ký thành công! Đơn của bạn hiện ở trạng thái PENDING.
                          </div>
                      )}

                      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-brand-50 pt-5">
                        <button
                            type="button"
                            onClick={handleSimulateApiError}
                            className="text-xs text-ink-muted underline hover:text-rose-600"
                        >
                          Mô phỏng lỗi kết nối API
                        </button>

                        <div className="flex gap-2">
                          {isResubmitting && (
                              <Button
                                  variant="outline"
                                  type="button"
                                  onClick={() => setIsResubmitting(false)}
                              >
                                Hủy
                              </Button>
                          )}
                          <Button type="submit" disabled={formState === 'submitting'}>
                            {formState === 'submitting' ? 'Đang gửi hồ sơ...' : 'Gửi đơn đăng ký'}
                          </Button>
                        </div>
                      </div>
                    </form>
                  </Card>
              )}
            </div>
        )}
      </PageContainer>
  );
}