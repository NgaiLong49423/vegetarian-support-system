import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Check, Info, Star, ThumbsUp, User } from 'lucide-react';
import { Button, Card, ProgressBar } from './ui';
import { useDemoAccount } from './DemoAccount';
import { currentUser } from '../data/mockData';
import type { Recipe } from '../types';

interface ReviewItem {
  id: string;
  author: string;
  avatar: string;
  rating: number;
  date: string;
  content: string;
  likes: number;
  liked?: boolean;
}

const initialReviews: Record<string, ReviewItem[]> = {
  default: [
    {
      id: 'rev-1',
      author: 'Thuỳ Trang',
      avatar: '/assets/1a85b.png',
      rating: 5,
      date: '2 ngày trước',
      content: 'Công thức rất chuẩn vị! Nước sốt nấm sánh nhẹ quyện vào đậu hũ non mềm mượt ăn cực kỳ đưa cơm. Mình có cho thêm chút nấm hương tươi nữa rất thơm.',
      likes: 14,
    },
    {
      id: 'rev-2',
      author: 'Minh Hoàng',
      avatar: '/assets/b8638.png',
      rating: 5,
      date: '5 ngày trước',
      content: 'Lần đầu nấu món chay đãi cả nhà vào ngày rằm mà ai cũng khen. Nguyên liệu dễ tìm, cách làm nhanh gọn và giải thích các bước rất kỹ.',
      likes: 8,
    },
    {
      id: 'rev-3',
      author: 'Ngọc Mai',
      avatar: '/assets/f2393.png',
      rating: 4,
      date: '1 tuần trước',
      content: 'Món ăn thanh đạm, giàu dinh dưỡng. Mình giảm bớt tiêu xanh một xíu cho các bé nhỏ dễ ăn hơn, hương vị vẫn rất ngon.',
      likes: 5,
    },
  ],
};

const ratingLabels: Record<number, string> = {
  1: '1 sao — Không hài lòng / Cần cải thiện nhiều',
  2: '2 sao — Tạm được / Chưa hợp khẩu vị',
  3: '3 sao — Bình thường / Ổn định',
  4: '4 sao — Rất ngon / Dễ làm và chuẩn vị',
  5: '5 sao — Tuyệt vời! Rất thích và khuyến nghị',
};

export function RecipeRating({ recipe }: { recipe: Recipe }) {
  const { active } = useDemoAccount();
  const isAuthor = active && recipe.author.name === currentUser.name;

  const [reviews, setReviews] = useState<ReviewItem[]>(() => initialReviews[recipe.id] ?? initialReviews.default);
  const [userRating, setUserRating] = useState<number | null>(null);
  const [hoverRating, setHoverRating] = useState<number>(0);
  const [reviewContent, setReviewContent] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');
  const [toast, setToast] = useState<string | null>(null);

  // Local calculation of ratings
  const baseCount = recipe.reviews || 1;
  const totalCount = submitted && userRating !== null ? baseCount + 1 : baseCount;
  const currentAvg = submitted && userRating !== null
    ? ((recipe.rating * baseCount + userRating) / totalCount).toFixed(1)
    : recipe.rating.toFixed(1);

  const starBreakdown = [
    { stars: 5, pct: 75 },
    { stars: 4, pct: 18 },
    { stars: 3, pct: 5 },
    { stars: 2, pct: 1 },
    { stars: 1, pct: 1 },
  ];

  const handleStarClick = (score: number) => {
    if (!active) {
      setError('Vui lòng đăng nhập tài khoản để gửi đánh giá sao (BR-69).');
      return;
    }
    if (isAuthor) {
      setError('Tác giả không được tự đánh giá bài công thức của chính mình (BR-69).');
      return;
    }
    setError('');
    setUserRating(score);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!active) {
      setError('Vui lòng đăng nhập để gửi đánh giá.');
      return;
    }
    if (isAuthor) {
      setError('Tác giả không được tự đánh giá bài công thức của chính mình.');
      return;
    }
    if (!userRating) {
      setError('Vui lòng chọn số sao đánh giá (từ 1 đến 5 sao).');
      return;
    }

    const newReview: ReviewItem = {
      id: `rev-user-${Date.now()}`,
      author: currentUser.name,
      avatar: currentUser.avatar,
      rating: userRating,
      date: 'Vừa xong',
      content: reviewContent.trim() || 'Đã đánh giá chất lượng công thức.',
      likes: 0,
    };

    setReviews((prev) => [newReview, ...prev.filter((r) => r.author !== currentUser.name)]);
    setSubmitted(true);
    setToast(submitted ? 'Đã cập nhật đánh giá của bạn!' : 'Đã gửi đánh giá sao thành công!');
    setTimeout(() => setToast(null), 3000);
  };

  const toggleLikeReview = (id: string) => {
    setReviews((prev) =>
      prev.map((r) => {
        if (r.id === id) {
          const liked = !r.liked;
          return { ...r, liked, likes: r.likes + (liked ? 1 : -1) };
        }
        return r;
      })
    );
  };

  return (
    <Card className="mb-10 p-6 sm:p-8">
      {/* Header */}
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3 border-b border-brand-100 pb-5">
        <div>
          <h2 className="flex items-center gap-2 text-xl font-extrabold text-ink sm:text-2xl">
            <Star className="h-6 w-6 fill-amber-400 text-amber-400" />
            Đánh giá từ cộng đồng ({totalCount})
          </h2>
          <p className="mt-1 text-xs text-ink-muted">
            Xếp hạng chất lượng công thức từ 1 đến 5 sao theo chuẩn cộng đồng Mâm Xanh.
          </p>
        </div>
      </div>

      {/* Overview & Breakdown */}
      <div className="mb-8 grid gap-6 rounded-2xl bg-brand-50/50 p-6 md:grid-cols-[220px_1fr]">
        <div className="flex flex-col items-center justify-center border-b border-brand-100 pb-5 md:border-b-0 md:border-r md:pb-0 md:pr-6">
          <span className="text-5xl font-black text-ink">{currentAvg}</span>
          <div className="my-2 flex items-center gap-1">
            {[1, 2, 3, 4, 5].map((s) => (
              <Star
                key={s}
                className={`h-5 w-5 ${
                  s <= Math.round(Number(currentAvg))
                    ? 'fill-amber-400 text-amber-400'
                    : 'fill-brand-200 text-brand-200'
                }`}
              />
            ))}
          </div>
          <span className="text-xs font-semibold text-ink-muted">{totalCount} lượt đánh giá</span>
        </div>

        <div className="flex flex-col justify-center space-y-2">
          {starBreakdown.map((item) => (
            <div key={item.stars} className="flex items-center gap-3 text-xs font-medium text-ink-soft">
              <span className="flex w-12 items-center gap-1">
                {item.stars} <Star className="h-3.5 w-3.5 fill-amber-400 text-amber-400" />
              </span>
              <div className="flex-1">
                <ProgressBar pct={item.pct} tone="brand" />
              </div>
              <span className="w-10 text-right text-ink-muted">{item.pct}%</span>
            </div>
          ))}
        </div>
      </div>

      {/* User rating box */}
      <div className="mb-8 rounded-2xl border border-brand-200 bg-white p-6 shadow-sm">
        <h3 className="text-base font-bold text-ink">
          {submitted ? 'Đánh giá của bạn về công thức này' : 'Chấm điểm & Viết đánh giá'}
        </h3>

        {!active ? (
          <div className="mt-4 rounded-xl border border-dashed border-brand-200 bg-brand-50/40 p-5 text-center">
            <p className="text-sm font-semibold text-ink">Bạn đã nấu thử món này?</p>
            <p className="mt-1 text-xs text-ink-muted">
              Đăng nhập để đánh giá từ 1 đến 5 sao và chia sẻ trải nghiệm cùng cộng đồng.
            </p>
            <Link to="/dang-nhap" className="mt-3 inline-block">
              <Button size="sm">Đăng nhập để đánh giá</Button>
            </Link>
          </div>
        ) : isAuthor ? (
          <div className="mt-4 flex items-center gap-3 rounded-xl border border-amber-200 bg-amber-50/70 p-4 text-xs text-amber-800">
            <Info className="h-4 w-4 shrink-0 text-amber-600" />
            <span>
              Bạn là tác giả bài công thức này. Theo quy chế cộng đồng (BR-69), tác giả không được tự đánh giá món ăn của chính mình.
            </span>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="mt-4">
            <div className="flex flex-col items-start gap-2 sm:flex-row sm:items-center sm:gap-4">
              <div className="flex items-center gap-1.5">
                {[1, 2, 3, 4, 5].map((score) => {
                  const activeScore = hoverRating || userRating || 0;
                  const isFilled = score <= activeScore;
                  return (
                    <button
                      key={score}
                      type="button"
                      onClick={() => handleStarClick(score)}
                      onMouseEnter={() => setHoverRating(score)}
                      onMouseLeave={() => setHoverRating(0)}
                      aria-label={`${score} sao`}
                      className="rounded-lg p-1 transition-transform hover:scale-110 focus:outline-none focus:ring-2 focus:ring-amber-400"
                    >
                      <Star
                        className={`h-8 w-8 transition-colors ${
                          isFilled
                            ? 'fill-amber-400 text-amber-400 drop-shadow-sm'
                            : 'fill-brand-100 text-brand-300 hover:text-amber-300'
                        }`}
                      />
                    </button>
                  );
                })}
              </div>
              <span className="text-xs font-semibold text-brand-700">
                {(hoverRating || userRating) ? ratingLabels[hoverRating || userRating || 0] : 'Nhấp vào sao để chấm điểm'}
              </span>
            </div>

            <div className="mt-4">
              <label htmlFor="recipe-review" className="block text-xs font-semibold text-ink-muted">
                Chia sẻ nhận xét hoặc mẹo nấu ăn của bạn (tùy chọn)
              </label>
              <textarea
                id="recipe-review"
                value={reviewContent}
                onChange={(e) => setReviewContent(e.target.value)}
                maxLength={500}
                rows={3}
                placeholder="Món ăn có hợp khẩu vị không? Bạn có điều chỉnh gì về gia vị hay thời gian nấu không?..."
                className="mt-1.5 w-full rounded-xl border border-brand-200 bg-white p-3 text-sm outline-none focus:border-brand-500 focus:ring-1 focus:ring-brand-500"
              />
              <div className="mt-1.5 flex items-center justify-between text-xs text-ink-muted">
                <span>{reviewContent.length}/500 ký tự</span>
                {userRating && (
                  <span className="font-medium text-brand-600">
                    Đã chọn: {userRating} sao
                  </span>
                )}
              </div>
            </div>

            {error && (
              <p role="alert" className="mt-2 text-xs font-semibold text-red-600">
                {error}
              </p>
            )}

            <div className="mt-4 flex items-center justify-end gap-3">
              <Button type="submit" size="sm">
                {submitted ? 'Cập nhật đánh giá' : 'Gửi đánh giá'}
              </Button>
            </div>
          </form>
        )}
      </div>

      {/* Reviews list */}
      <div className="space-y-4">
        <h3 className="text-sm font-bold uppercase tracking-wider text-ink-muted">
          Nhận xét gần đây ({reviews.length})
        </h3>
        <div className="divide-y divide-brand-100">
          {reviews.map((rev) => (
            <div key={rev.id} className="py-4 first:pt-0 last:pb-0">
              <div className="flex items-start justify-between gap-3">
                <div className="flex items-center gap-3">
                  <img
                    src={rev.avatar}
                    alt={rev.author}
                    className="h-9 w-9 rounded-full object-cover ring-1 ring-brand-100"
                  />
                  <div>
                    <div className="flex items-center gap-2">
                      <p className="text-sm font-bold text-ink">{rev.author}</p>
                      {rev.author === currentUser.name && (
                        <span className="rounded-full bg-brand-100 px-2 py-0.5 text-[10px] font-bold text-brand-700">
                          Bạn
                        </span>
                      )}
                    </div>
                    <div className="mt-0.5 flex items-center gap-2">
                      <div className="flex items-center">
                        {[1, 2, 3, 4, 5].map((s) => (
                          <Star
                            key={s}
                            className={`h-3 w-3 ${
                              s <= rev.rating
                                ? 'fill-amber-400 text-amber-400'
                                : 'fill-brand-200 text-brand-200'
                            }`}
                          />
                        ))}
                      </div>
                      <span className="text-[11px] text-ink-muted">{rev.date}</span>
                    </div>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => toggleLikeReview(rev.id)}
                  className={`flex items-center gap-1 rounded-lg px-2 py-1 text-xs font-medium transition-colors ${
                    rev.liked
                      ? 'bg-brand-50 text-brand-700'
                      : 'text-ink-muted hover:bg-brand-50 hover:text-ink'
                  }`}
                  aria-label="Hữu ích"
                >
                  <ThumbsUp className={`h-3.5 w-3.5 ${rev.liked ? 'fill-current text-brand-600' : ''}`} />
                  <span>{rev.likes}</span>
                </button>
              </div>

              <p className="mt-2.5 text-sm leading-relaxed text-ink-soft">
                {rev.content}
              </p>
            </div>
          ))}
        </div>
      </div>

      {toast && (
        <div className="fixed bottom-6 left-1/2 z-50 flex -translate-x-1/2 items-center gap-2 rounded-full bg-ink px-5 py-3 text-sm font-semibold text-white shadow-xl">
          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-leaf-500">
            <Check className="h-3.5 w-3.5" />
          </span>
          {toast}
        </div>
      )}
    </Card>
  );
}
