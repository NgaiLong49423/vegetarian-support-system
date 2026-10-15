import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { ThumbsDown, ThumbsUp } from 'lucide-react';
import { recipeReactionApi, type RecipeReactionType, type RecipeStatistics } from '../api/recipes';
import { asApiError } from '../lib/apiClient';
import { useAuth } from './AuthContext';
import { Modal } from './Modal';
import { Button } from './ui';
import { RecipeReactionBadge } from './RecipeReactionBadge';

export function RecipeReactions(props: { recipeId: number; authorId: number; statistics: RecipeStatistics }) {
  const { account } = useAuth();
  return <ReactionControls key={`${props.recipeId}:${account?.id ?? 'guest'}:${account?.role ?? ''}`} {...props} />;
}

function ReactionControls({ recipeId, authorId, statistics }: { recipeId: number; authorId: number; statistics: RecipeStatistics }) {
  const { account } = useAuth();
  const author = account?.id === authorId;
  const member = account?.role === 'CUSTOMER' || account?.role === 'EXPERT';
  const eligible = member && !author;
  const [counts, setCounts] = useState(statistics);
  const [selected, setSelected] = useState<RecipeReactionType | null>(null);
  const [ready, setReady] = useState(!eligible);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [blocked, setBlocked] = useState(false);
  const [retry, setRetry] = useState(0);
  const [login, setLogin] = useState(false);
  const pending = useRef(false);
  const alive = useRef(true);
  useEffect(() => { alive.current = true; return () => { alive.current = false; }; }, []);
  useEffect(() => {
    if (!eligible) return;
    let active = true;
    setReady(false);
    setError('');
    recipeReactionApi.get(recipeId).then((data) => {
      if (!active) return;
      setCounts((previous) => ({ ...previous, ...data }));
      setSelected(data.currentReaction);
      setReady(true);
    }).catch((cause: unknown) => { if (active) setError(asApiError(cause).message); });
    return () => { active = false; };
  }, [eligible, recipeId, retry]);

  async function vote(type: RecipeReactionType) {
    if (!account) { setLogin(true); return; }
    if (!eligible || !ready || blocked || pending.current) return;
    pending.current = true;
    setBusy(true);
    setError('');
    try {
      const data = selected === type ? await recipeReactionApi.remove(recipeId) : await recipeReactionApi.set(recipeId, type);
      if (!alive.current) return;
      setCounts((previous) => ({ ...previous, ...data }));
      setSelected(data.currentReaction);
    } catch (cause) {
      if (!alive.current) return;
      const failure = asApiError(cause);
      setError(failure.status === 404 ? 'Công thức không còn công khai hoặc đã bị xóa.' : failure.message);
      if (failure.status === 403 || failure.status === 404) setBlocked(true);
    } finally {
      pending.current = false;
      if (alive.current) setBusy(false);
    }
  }

  const disabled = Boolean(account) && (!eligible || !ready || busy || blocked);
  return <section aria-label="Bình chọn công thức" className="mt-6 border-t border-brand-100 pt-5">
    <div className="flex flex-wrap items-center justify-between gap-3">
      <h2 className="font-bold text-ink">Bạn thấy công thức này thế nào?</h2>
      <RecipeReactionBadge percentage={counts.likePercentage} />
    </div>
    {counts.likePercentage != null && <div role="progressbar" aria-label="Tỷ lệ Like" aria-valuemin={0} aria-valuemax={100} aria-valuenow={counts.likePercentage} className="mt-3 h-2 overflow-hidden rounded-full bg-brand-100">
      <div className="h-full bg-brand-600" style={{ width: `${counts.likePercentage}%` }} />
    </div>}
    <p aria-live="polite" className="mt-3 text-sm text-ink-muted">{counts.likes} lượt thích · {counts.dislikes} lượt không thích · {counts.reactionCount} bình chọn</p>
    <div className="mt-4 flex flex-wrap gap-3" aria-busy={busy}>
      {(['LIKE', 'DISLIKE'] as const).map((type) => {
        const Icon = type === 'LIKE' ? ThumbsUp : ThumbsDown;
        return <Button key={type} className="focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600" variant={selected === type ? 'primary' : 'outline'} disabled={disabled} aria-pressed={selected === type} onClick={() => void vote(type)}>
          <Icon aria-hidden="true" className="h-4 w-4" />{type === 'LIKE' ? 'Thích' : 'Không thích'}
        </Button>;
      })}
    </div>
    {author ? <p className="mt-3 text-sm text-ink-muted">Tác giả không được tự bình chọn bài viết của chính mình.</p>
      : account && !member ? <p className="mt-3 text-sm text-ink-muted">Chỉ thành viên và chuyên gia được bình chọn công thức.</p>
      : <p role="status" className="mt-3 text-xs text-ink-muted">{busy ? 'Đang lưu bình chọn…' : eligible && !ready && !error ? 'Đang tải bình chọn của bạn…' : selected ? 'Nhấn lại nút đã chọn để hủy bình chọn.' : 'Chọn thích hoặc không thích để chia sẻ ý kiến.'}</p>}
    {error && <div role="alert" className="mt-3 text-sm text-red-700">{error}{!ready && !blocked && <Button className="mt-2" variant="outline" onClick={() => setRetry((value) => value + 1)}>Tải lại bình chọn</Button>}</div>}
    <Modal open={login} onClose={() => setLogin(false)} title="Yêu cầu đăng nhập">
      <p className="text-sm text-ink-muted">Vui lòng đăng nhập để bình chọn công thức.</p>
      <div className="mt-5 flex flex-wrap items-center gap-3"><Link to="/dang-nhap" className="rounded-xl bg-brand-600 px-4 py-2 font-semibold text-white focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600">Đăng nhập</Link><Button variant="outline" onClick={() => setLogin(false)}>Để sau</Button></div>
    </Modal>
  </section>;
}
