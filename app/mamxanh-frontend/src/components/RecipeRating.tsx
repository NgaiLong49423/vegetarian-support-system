import { useState } from 'react';
import { Link } from 'react-router-dom';
import { AlertCircle, CheckCircle2, ShieldAlert, ThumbsDown, ThumbsUp, User } from 'lucide-react';
import { Button, Card, ProgressBar } from './ui';
import { useAuth } from './AuthContext';
import type { Recipe } from '../types';

export function RecipeRating({ recipe }: { recipe: Recipe }) {
  const { isAuthenticated: active, account } = useAuth();
  const isAuthor = !!account && recipe.author.name === account.displayName;

  // FR-57 Vote State
  const [userVote, setUserVote] = useState<'like' | 'dislike' | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [feedbackState, setFeedbackState] = useState<'idle' | 'success' | 'error'>('idle');

  // Calculate like & dislike totals
  const baseLikes = recipe.likes ?? 0;
  const baseDislikes = recipe.dislikes ?? 0;

  let currentLikes = baseLikes;
  let currentDislikes = baseDislikes;
  if (userVote === 'like') currentLikes += 1;
  if (userVote === 'dislike') currentDislikes += 1;

  const totalVotes = currentLikes + currentDislikes;
  const likePercentage = totalVotes > 0 ? Math.round((currentLikes / totalVotes) * 100) : undefined;

  const handleVote = (nextVote: 'like' | 'dislike' | null) => {
    if (!active) return;
    if (isAuthor) return;

    setSubmitting(true);
    setFeedbackState('idle');

    setTimeout(() => {
      setUserVote(nextVote);
      setSubmitting(false);
      setFeedbackState('success');
    }, 300);
  };

  const handleSimulateError = () => {
    setSubmitting(true);
    setFeedbackState('idle');
    setTimeout(() => {
      setSubmitting(false);
      setFeedbackState('error');
    }, 300);
  };

  return (
    <Card className="mb-10 p-6 sm:p-8">
      {/* Header */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3 border-b border-brand-100 pb-5">
        <div>
          <h2 className="flex items-center gap-2 text-xl font-extrabold text-ink sm:text-2xl">
            <ThumbsUp className="h-6 w-6 text-leaf-600" />
            Mức độ yêu thích từ cộng đồng (FR-57)
          </h2>
          <p className="mt-1 text-xs text-ink-muted">
            Bản minh họa UI; API bình chọn công thức chưa được kết nối.
          </p>
        </div>
      </div>

      {/* Overview Box */}
      <div className="mb-8 grid gap-6 rounded-2xl bg-brand-50/50 p-6 md:grid-cols-[240px_1fr]">
        <div className="flex flex-col items-center justify-center border-b border-brand-100 pb-5 md:border-b-0 md:border-r md:pb-0 md:pr-6">
          {likePercentage !== undefined ? (
            <>
              <span className="text-5xl font-black text-leaf-700">{likePercentage}%</span>
              <span className="mt-1 text-sm font-bold text-ink">Tỷ lệ hài lòng</span>
              <span className="text-xs font-semibold text-ink-muted">
                {totalVotes} lượt bình chọn ({currentLikes} Like · {currentDislikes} Dislike)
              </span>
            </>
          ) : (
            <>
              <span className="rounded-full bg-brand-100 px-4 py-1.5 text-2xl font-black text-brand-700">Mới</span>
              <span className="mt-2 text-xs font-semibold text-ink-muted">Chưa có lượt bình chọn nào</span>
            </>
          )}
        </div>

        <div className="flex flex-col justify-center space-y-3">
          {likePercentage !== undefined ? (
            <>
              <div className="flex items-center justify-between text-xs font-semibold text-ink-soft">
                <span className="flex items-center gap-1.5 text-leaf-700">
                  <ThumbsUp className="h-4 w-4" /> {currentLikes} người thích ({likePercentage}%)
                </span>
                <span className="flex items-center gap-1.5 text-rose-600">
                  <ThumbsDown className="h-4 w-4" /> {currentDislikes} người không thích ({100 - likePercentage}%)
                </span>
              </div>
              <ProgressBar pct={likePercentage} tone="leaf" />
              <p className="text-xs text-ink-muted">
                Dựa trên quy tắc bình chọn một tài khoản - một lượt đánh giá, tự động cập nhật theo thời gian thực.
              </p>
            </>
          ) : (
            <p className="text-sm text-ink-muted">
              Đây là công thức mới đăng tải. Hãy là người đầu tiên nấu thử và chia sẻ cảm nhận đến cộng đồng!
            </p>
          )}
        </div>
      </div>

      {/* User voting block */}
      <div className="mb-8 rounded-2xl border border-brand-200 bg-white p-6 shadow-sm">
        <h3 className="text-base font-bold text-ink">Bình chọn của bạn về công thức này</h3>

        {!active ? (
          <div className="mt-4 flex flex-col items-center justify-center rounded-xl border border-dashed border-brand-200 bg-brand-50/40 p-5 text-center">
            <p className="text-sm font-semibold text-ink">Bạn muốn chia sẻ cảm nhận về món ăn này?</p>
            <p className="mt-1 max-w-md text-xs text-ink-muted">
              Vui lòng đăng nhập để Like hoặc Dislike công thức.
            </p>
            <Link
              to="/dang-nhap"
              className="mt-3 inline-flex items-center gap-1.5 rounded-xl bg-brand-600 px-4 py-2 text-xs font-bold text-white shadow-sm hover:bg-brand-700"
            >
              Đăng nhập để bình chọn
            </Link>
          </div>
        ) : isAuthor ? (
          <div className="mt-4 flex items-center gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
            <ShieldAlert className="h-5 w-5 shrink-0 text-amber-600" />
            <p>
              Đây là công thức của bạn. Tác giả không thể tự Like hoặc Dislike bài viết của chính mình (FR-57 / BR-28).
            </p>
          </div>
        ) : (
          <div className="mt-4 space-y-4">
            <div className="flex flex-wrap items-center gap-3">
              <button
                type="button"
                id="btnLike"
                disabled={submitting}
                onClick={() => handleVote(userVote === 'like' ? null : 'like')}
                className={`inline-flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-bold transition ${
                  userVote === 'like'
                    ? 'bg-leaf-600 text-white shadow-md shadow-leaf-600/20 hover:bg-leaf-700'
                    : 'border border-brand-200 bg-white text-ink-soft hover:border-leaf-300 hover:bg-leaf-50 hover:text-leaf-700'
                } disabled:cursor-not-allowed disabled:opacity-50`}
              >
                <ThumbsUp className={`h-4 w-4 ${userVote === 'like' ? 'fill-current' : ''}`} />
                {submitting && userVote !== 'like' ? 'Đang gửi...' : 'Like'}
              </button>

              <button
                type="button"
                id="btnDislike"
                disabled={submitting}
                onClick={() => handleVote(userVote === 'dislike' ? null : 'dislike')}
                className={`inline-flex items-center gap-2 rounded-xl px-5 py-2.5 text-sm font-bold transition ${
                  userVote === 'dislike'
                    ? 'bg-rose-600 text-white shadow-md shadow-rose-600/20 hover:bg-rose-700'
                    : 'border border-brand-200 bg-white text-ink-soft hover:border-rose-300 hover:bg-rose-50 hover:text-rose-700'
                } disabled:cursor-not-allowed disabled:opacity-50`}
              >
                <ThumbsDown className={`h-4 w-4 ${userVote === 'dislike' ? 'fill-current' : ''}`} />
                {submitting && userVote !== 'dislike' ? 'Đang gửi...' : 'Dislike'}
              </button>

              {userVote !== null && (
                <button
                  type="button"
                  onClick={() => handleVote(null)}
                  disabled={submitting}
                  className="text-xs text-ink-muted underline hover:text-ink"
                >
                  Hủy bình chọn
                </button>
              )}

              <button
                type="button"
                id="btnSimulateError"
                onClick={handleSimulateError}
                className="ml-auto text-xs text-ink-muted hover:text-rose-600"
              >
                Mô phỏng lỗi gửi
              </button>
            </div>

            {feedbackState === 'success' && (
              <div role="status" className="flex items-center gap-2 text-xs font-semibold text-leaf-700">
                <CheckCircle2 className="h-4 w-4" />
                Đã cập nhật bình chọn thành công.
              </div>
            )}

            {feedbackState === 'error' && (
              <div role="alert" className="flex items-center gap-2 text-xs font-semibold text-rose-700">
                <AlertCircle className="h-4 w-4" />
                Không thể gửi bình chọn · Vui lòng thử lại.
              </div>
            )}
          </div>
        )}
      </div>
    </Card>
  );
}
