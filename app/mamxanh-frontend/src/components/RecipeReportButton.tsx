import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { isAxiosError } from 'axios';
import { Flag } from 'lucide-react';
import { Button } from './ui';
import { Modal } from './Modal';
import { useAuth } from './AuthContext';
import { asApiError } from '../lib/apiClient';
import { formatSubscriptionDateTime as formatDateTime } from '../utils/subscriptionDate';
import { reportReasons, reportsApi, type MyReport, type ReportReasonCode } from '../api/reports';

const TEXT_MIN = 10;
const TEXT_MAX = 500;
const statusLabels: Record<MyReport['status'], string> = {
  OPEN: 'Chờ xử lý',
  IN_REVIEW: 'Đang xem xét',
  RESOLVED: 'Đã giải quyết',
};
const reasonLabel = (code: string) => reportReasons.find((reason) => reason.code === code)?.label ?? code;
const outOfRange = (text: string) => text.length < TEXT_MIN || text.length > TEXT_MAX;

/** FR-26/27 report form with the FR-30 path to an already open report (AC-30.1, AC-30.2). */
export function RecipeReportButton({ recipeId, authorId }: { recipeId: number; authorId: number }) {
  const { account } = useAuth();
  const [open, setOpen] = useState(false);
  const [login, setLogin] = useState(false);
  if (account && (account.id === authorId || account.role === 'ADMIN')) return null;

  return (
    <div className="mt-4 flex flex-wrap items-center justify-end gap-3 border-t border-brand-100 pt-3">
      {login && (
        <Link to="/dang-nhap" className="text-sm font-semibold text-brand-700 underline">
          Đăng nhập để báo cáo
        </Link>
      )}
      <Button variant="ghost" size="sm" onClick={() => (account ? setOpen(true) : setLogin(true))}>
        <Flag className="h-4 w-4" aria-hidden="true" />
        Báo cáo công thức
      </Button>
      <Modal open={open} onClose={() => setOpen(false)} title="Báo cáo công thức" size="lg" trapFocus>
        {open && <ReportFlow recipeId={recipeId} onClose={() => setOpen(false)} />}
      </Modal>
    </div>
  );
}

function ReportFlow({ recipeId, onClose }: { recipeId: number; onClose: () => void }) {
  const [sent, setSent] = useState(false);
  const [existing, setExisting] = useState<MyReport | null>(null);
  const [notice, setNotice] = useState('');

  if (sent) {
    return (
      <p role="status" className="rounded-xl border border-leaf-200 bg-leaf-50 p-4 text-sm font-medium text-leaf-800">
        Đã gửi báo cáo. Quản trị viên sẽ xem xét nội dung này.
      </p>
    );
  }
  if (existing) return <ExistingReport report={existing} notice={notice} onChange={setExisting} />;
  return (
    <ReportForm
      recipeId={recipeId}
      onCancel={onClose}
      onSent={() => setSent(true)}
      onDuplicate={(report, message) => {
        setNotice(message);
        setExisting(report);
      }}
    />
  );
}

function ReportForm({
  recipeId,
  onCancel,
  onSent,
  onDuplicate,
}: {
  recipeId: number;
  onCancel: () => void;
  onSent: () => void;
  onDuplicate: (report: MyReport, message: string) => void;
}) {
  const [reason, setReason] = useState<ReportReasonCode | ''>('');
  const [description, setDescription] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [busy, setBusy] = useState(false);
  const [serverDescriptionError, setServerDescriptionError] = useState('');
  const [error, setError] = useState('');
  const trimmed = description.trim();
  const otherInvalid = reason === 'OTHER' && outOfRange(trimmed);
  const reasonError = submitted && !reason ? 'Chọn một lý do báo cáo.' : '';
  const descriptionError = (submitted && otherInvalid ? 'Mô tả cho lý do Khác cần từ 10 đến 500 ký tự.' : '') || serverDescriptionError;

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSubmitted(true);
    setError('');
    setServerDescriptionError('');
    if (!reason || otherInvalid) return;
    setBusy(true);
    try {
      await reportsApi.submit(recipeId, reason, trimmed);
      onSent();
    } catch (cause) {
      const api = asApiError(cause);
      const body = isAxiosError(cause) ? (cause.response?.data as { reportId?: unknown; field?: unknown } | undefined) : undefined;
      if (api.code === 'REPORT_ALREADY_OPEN' && typeof body?.reportId === 'number') {
        try {
          onDuplicate(await reportsApi.mine(body.reportId), api.message);
        } catch (inner) {
          setError(asApiError(inner).message);
        }
      } else if (api.code === 'VALIDATION_FAILED' && (body?.field === 'description' || api.errors[0]?.field === 'description')) {
        setServerDescriptionError(api.errors[0]?.message ?? api.message);
      } else if (api.status === 404) {
        setError('Công thức không còn công khai nên không thể báo cáo.');
      } else {
        setError(api.message);
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-4 text-sm">
      <fieldset aria-describedby={reasonError ? 'report-reason-error' : undefined}>
        <legend className="font-bold text-ink">Lý do báo cáo</legend>
        <div className="mt-2 grid gap-2 sm:grid-cols-2">
          {reportReasons.map((item) => (
            <label
              key={item.code}
              className={`flex cursor-pointer items-start gap-3 rounded-xl border p-3 transition-colors ${
                reason === item.code ? 'border-brand-500 bg-brand-50/80 ring-1 ring-brand-500' : 'border-brand-100 hover:bg-brand-50/40'
              }`}
            >
              <input
                type="radio"
                name="report-reason"
                value={item.code}
                checked={reason === item.code}
                onChange={() => setReason(item.code)}
                className="mt-0.5 h-4 w-4 accent-brand-600"
              />
              <span className="font-semibold text-ink">{item.label}</span>
            </label>
          ))}
        </div>
        {reasonError && <p id="report-reason-error" className="mt-1 text-xs font-semibold text-rose-700">{reasonError}</p>}
      </fieldset>

      <div>
        <label htmlFor="report-description" className="font-bold text-ink">Mô tả thêm</label>
        <p className="text-xs text-ink-muted">
          {reason === 'OTHER' ? 'Bắt buộc với lý do Khác, từ 10 đến 500 ký tự.' : 'Không bắt buộc, tối đa 500 ký tự.'}
        </p>
        <textarea
          id="report-description"
          rows={4}
          maxLength={TEXT_MAX}
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          aria-invalid={descriptionError ? true : undefined}
          aria-describedby={descriptionError ? 'report-description-error' : undefined}
          className="mt-1 w-full rounded-xl border border-brand-200 bg-paper p-3 text-sm text-ink focus:border-brand-500 focus:outline-none"
        />
        <div className="flex justify-between gap-2 text-xs">
          <span id="report-description-error" className="font-semibold text-rose-700">{descriptionError}</span>
          <span className="tabular-nums text-ink-muted">{trimmed.length}/{TEXT_MAX}</span>
        </div>
      </div>

      <p className="text-xs text-ink-muted">
        Tác giả không thấy tên của bạn. Bài vẫn hiển thị cho tới khi quản trị viên xem xét và có kết luận.
      </p>
      {error && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 font-medium text-rose-800">{error}</p>}

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" size="sm" onClick={onCancel}>Hủy</Button>
        <Button type="submit" size="sm" disabled={busy}>{busy ? 'Đang gửi…' : 'Gửi báo cáo'}</Button>
      </div>
    </form>
  );
}

function ExistingReport({ report, notice, onChange }: { report: MyReport; notice: string; onChange: (report: MyReport) => void }) {
  const [content, setContent] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [saved, setSaved] = useState('');
  const trimmed = content.trim();
  const remaining = Math.max(0, report.supplementLimit - report.supplements.length);
  const stillOpen = report.status !== 'RESOLVED';
  const contentError = submitted && outOfRange(trimmed) ? 'Nhập nội dung bổ sung từ 10 đến 500 ký tự.' : '';

  async function submit(event: FormEvent) {
    event.preventDefault();
    setSubmitted(true);
    setError('');
    setSaved('');
    if (outOfRange(trimmed)) return;
    setBusy(true);
    try {
      const created = await reportsApi.supplement(report.reportId, trimmed);
      onChange({ ...report, supplements: [...report.supplements, created] });
      setContent('');
      setSubmitted(false);
      setSaved('Đã lưu phần bổ sung.');
    } catch (cause) {
      const api = asApiError(cause);
      setError(api.errors[0]?.message ?? api.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-4 text-sm">
      <p className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-amber-900">{notice}</p>
      <section className="rounded-xl border border-brand-100 p-4">
        <div className="flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center rounded-full border border-brand-200 bg-brand-50 px-2.5 py-0.5 text-xs font-bold text-brand-700">
            {statusLabels[report.status]}
          </span>
          <span className="text-xs text-ink-muted">Gửi lúc {formatDateTime(report.createdAt)}</span>
        </div>
        <p className="mt-2 font-semibold text-ink">{reasonLabel(report.reasonCode)}</p>
        <p className="mt-1 whitespace-pre-line text-ink-soft">{report.description || 'Bạn chưa ghi thêm mô tả.'}</p>
        {report.supplements.length > 0 && (
          <div className="mt-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-ink-muted">Đã bổ sung</h3>
            <ul className="mt-1 space-y-2">
              {report.supplements.map((item) => (
                <li key={item.supplementId} className="rounded-lg bg-cream-soft/60 px-3 py-2">
                  <p className="whitespace-pre-line text-ink-soft">{item.content}</p>
                  <p className="mt-0.5 text-xs text-ink-muted">{formatDateTime(item.createdAt)}</p>
                </li>
              ))}
            </ul>
          </div>
        )}
      </section>

      {stillOpen && remaining > 0 ? (
        <form onSubmit={submit} noValidate className="space-y-2">
          <label htmlFor="report-supplement" className="font-bold text-ink">Thông tin bổ sung</label>
          <p className="text-xs text-ink-muted">Còn {remaining} lần bổ sung. Mỗi lần từ 10 đến 500 ký tự.</p>
          <textarea
            id="report-supplement"
            rows={3}
            maxLength={TEXT_MAX}
            value={content}
            onChange={(event) => setContent(event.target.value)}
            aria-invalid={contentError ? true : undefined}
            aria-describedby={contentError ? 'report-supplement-error' : undefined}
            className="w-full rounded-xl border border-brand-200 bg-paper p-3 text-sm text-ink focus:border-brand-500 focus:outline-none"
          />
          <div className="flex justify-between gap-2 text-xs">
            <span id="report-supplement-error" className="font-semibold text-rose-700">{contentError}</span>
            <span className="tabular-nums text-ink-muted">{trimmed.length}/{TEXT_MAX}</span>
          </div>
          {saved && <p role="status" className="font-medium text-leaf-700">{saved}</p>}
          {error && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 font-medium text-rose-800">{error}</p>}
          <div className="flex justify-end">
            <Button type="submit" size="sm" disabled={busy}>{busy ? 'Đang lưu…' : 'Lưu bổ sung'}</Button>
          </div>
        </form>
      ) : (
        <p className="text-ink-muted">{stillOpen ? 'Báo cáo đã có đủ số lần bổ sung.' : 'Báo cáo đã được giải quyết.'}</p>
      )}
    </div>
  );
}
