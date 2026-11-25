import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { Edit3, LoaderCircle, MessageSquareWarning, RotateCw, Send } from 'lucide-react';
import { Button } from './ui';
import { asApiError } from '../lib/apiClient';
import { APPEAL_MESSAGE_MAX, APPEAL_MESSAGE_MIN, recipeAppealsApi, type RecipeAppeal } from '../api/appeals';

/**
 * Shown on a hidden post in the Expert's own list (FR-28, BR-27): the hide reason, the appeal form, and the
 * steps that follow an Administrator's answer. The post stays hidden until an Administrator restores it.
 */
export function RecipeAppealPanel({ recipeId }: { recipeId: number }) {
  const [appeal, setAppeal] = useState<RecipeAppeal | null>(null);
  const [loading, setLoading] = useState(true);
  const [formOpen, setFormOpen] = useState(false);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      setAppeal(await recipeAppealsApi.get(recipeId));
    } catch (cause) {
      setError(asApiError(cause).message);
    } finally {
      setLoading(false);
    }
  }, [recipeId]);

  useEffect(() => { void load(); }, [load]);

  const run = async (action: () => Promise<RecipeAppeal>) => {
    setBusy(true);
    setError('');
    try {
      setAppeal(await action());
      setFormOpen(false);
    } catch (cause) {
      const problem = asApiError(cause);
      setError(problem.message);
      // 409: the appeal moved on elsewhere (another tab, an Administrator). Show where it is now.
      if (problem.status === 409) {
        try {
          setAppeal(await recipeAppealsApi.get(recipeId));
          setFormOpen(false);
        } catch {
          // Keep the refused step's message; the author can reload the page.
        }
      }
    } finally {
      setBusy(false);
    }
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    void run(() => recipeAppealsApi.submit(recipeId, message.trim()));
  };

  const box = 'mt-2.5 space-y-2 rounded-lg border border-amber-200/60 bg-amber-50 p-3 text-xs leading-relaxed text-amber-950';
  if (loading) {
    return <div className={box}><LoaderCircle className="inline h-3.5 w-3.5 animate-spin" /> Đang tải thông tin ẩn bài…</div>;
  }
  if (!appeal) {
    return <div className={box}>
      <p role="alert">Admin đã ẩn bài. {error}</p>
      <Button size="sm" variant="outline" onClick={() => void load()}>
        <RotateCw className="h-3.5 w-3.5" />Thử lại
      </Button>
    </div>;
  }

  const trimmed = message.trim().length;
  const status = appeal.appealStatus;
  return <div className={box}>
    <p><strong>Admin đã ẩn bài.</strong> Lý do: {appeal.hideReason}</p>

    {status === null && !formOpen && <>
      <p>Bạn có thể xem nội dung nhưng chưa sửa được. Nếu bài bị ẩn nhầm, hãy gửi khiếu nại để Admin xem xét lại.</p>
      <Button size="sm" variant="outline" onClick={() => setFormOpen(true)}>
        <MessageSquareWarning className="h-3.5 w-3.5" />Khiếu nại
      </Button>
    </>}

    {status === null && formOpen && <form onSubmit={submit} className="space-y-2">
      <label className="block font-semibold" htmlFor={`appeal-message-${recipeId}`}>Lời khiếu nại</label>
      <textarea
        id={`appeal-message-${recipeId}`}
        className="w-full rounded-lg border border-amber-300 bg-paper p-2 text-xs text-ink outline-none focus:border-brand-500"
        rows={4}
        maxLength={APPEAL_MESSAGE_MAX}
        value={message}
        onChange={(event) => setMessage(event.target.value)}
        placeholder="Giải thích vì sao bài không vi phạm hoặc bạn sẽ sửa gì."
      />
      <div className="flex items-center justify-between gap-2">
        <span className={trimmed < APPEAL_MESSAGE_MIN ? 'text-amber-800' : 'text-ink-muted'}>
          {trimmed}/{APPEAL_MESSAGE_MAX}{trimmed < APPEAL_MESSAGE_MIN ? ` · cần ít nhất ${APPEAL_MESSAGE_MIN} ký tự` : ''}
        </span>
        <div className="flex gap-2">
          <Button type="button" size="sm" variant="ghost" disabled={busy} onClick={() => setFormOpen(false)}>Hủy</Button>
          <Button type="submit" size="sm" disabled={busy || trimmed < APPEAL_MESSAGE_MIN}>
            {busy ? <LoaderCircle className="h-3.5 w-3.5 animate-spin" /> : <Send className="h-3.5 w-3.5" />}Gửi khiếu nại
          </Button>
        </div>
      </div>
    </form>}

    {status === 'PENDING' && <p role="status">Đã gửi khiếu nại, đang chờ Admin xem xét.</p>}

    {status === 'EDIT_ALLOWED' && <>
      <p role="status">Admin cho phép bạn sửa bài. Lý do: {appeal.adminReason}</p>
      <p>Bài vẫn ẩn trong lúc sửa. Sửa xong, hãy gửi duyệt lại để Admin xem bản mới.</p>
      <div className="flex flex-wrap gap-2">
        <Link to={`/cong-thuc/${recipeId}/chinh-sua`} className="inline-flex items-center gap-1 rounded-full bg-brand-600 px-3 py-1.5 font-semibold text-white hover:bg-brand-700">
          <Edit3 className="h-3.5 w-3.5" />Sửa bài đang ẩn
        </Link>
        <Button size="sm" variant="outline" disabled={busy} onClick={() => void run(() => recipeAppealsApi.resubmit(recipeId))}>
          {busy ? <LoaderCircle className="h-3.5 w-3.5 animate-spin" /> : <Send className="h-3.5 w-3.5" />}Gửi duyệt lại
        </Button>
      </div>
    </>}

    {status === 'RESUBMITTED' && <p role="status">Đã gửi duyệt lại, đang chờ Admin xem xét bản đã sửa.</p>}

    {status === 'KEPT_HIDDEN' && <>
      <p role="status">Admin giữ ẩn bài sau khi xem khiếu nại.</p>
      <p>Lý do: {appeal.adminReason}</p>
    </>}

    {error && <p role="alert" className="text-red-700">{error}</p>}
  </div>;
}
