import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { BadgeCheck, Bookmark, Check, ChevronRight, Clock, Heart, MessageCircle, Share2, Star } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { PostCard } from '../components/PostCard';
import { Badge, Button, SectionHeading } from '../components/ui';
import { posts } from '../data/mockData';

export function PostDetail() {
  const { slug } = useParams();
  const post = posts.find((p) => p.slug === slug) ?? posts[0];
  const [liked, setLiked] = useState(false);
  const [rating, setRating] = useState<number | null>(null);
  const [hoverRating, setHoverRating] = useState(0);
  const [ratedCount, setRatedCount] = useState(36);
  const [toast, setToast] = useState<string | null>(null);
  const related = posts.filter((p) => p.id !== post.id).slice(0, 3);

  return (
    <PageContainer className="py-8">
      <nav className="mb-5 flex items-center gap-1.5 text-sm text-ink-muted">
        <Link to="/" className="hover:text-brand-600">Trang chủ</Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <Link to="/cong-dong" className="hover:text-brand-600">Cộng đồng</Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <span className="line-clamp-1 font-medium text-ink-soft">{post.title}</span>
      </nav>

      <article className="mx-auto max-w-3xl">
        <div className="mb-3 flex flex-wrap gap-2">
          {post.tags.map((t) => <Badge key={t} tone="brand">{t}</Badge>)}
        </div>
        <h1 className="text-3xl font-extrabold leading-tight tracking-tight text-ink sm:text-4xl">{post.title}</h1>

        <div className="mt-5 flex items-center gap-3 border-b border-brand-100 pb-5">
          <img src={post.author.avatar} alt="" className="h-11 w-11 rounded-full object-cover ring-2 ring-brand-100" />
          <div className="flex-1">
            <p className="flex items-center gap-1 font-semibold text-ink">
              {post.author.name}
              {post.author.verified && <BadgeCheck className="h-4 w-4 text-brand-600" />}
            </p>
            <p className="flex items-center gap-2 text-xs text-ink-muted">
              {post.publishedAt} <span>·</span> <Clock className="h-3 w-3" /> {post.readTime} phút đọc
            </p>
          </div>
          <Button variant="outline" size="sm">Theo dõi</Button>
        </div>

        <img src={post.image} alt={post.title} className="my-6 aspect-[16/9] w-full rounded-2xl object-cover" />

        <div className="space-y-5 text-lg leading-relaxed text-ink-soft">
          <p className="text-xl font-medium text-ink">{post.excerpt}</p>
          {post.body.map((para, i) => <p key={i}>{para}</p>)}
        </div>

        <div className="mt-8 flex items-center gap-3 rounded-2xl border border-brand-100 bg-white p-3">
          <button
            onClick={() => setLiked((v) => !v)}
            className={`flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold transition-colors ${liked ? 'bg-brand-600 text-white' : 'bg-brand-100 text-brand-700 hover:bg-brand-200'}`}
          >
            <Heart className={`h-4 w-4 ${liked ? 'fill-current' : ''}`} /> {post.likes + (liked ? 1 : 0)}
          </button>
          <span className="flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold text-ink-soft">
            <MessageCircle className="h-4 w-4" /> {post.comments} bình luận
          </span>
          <div className="ml-auto flex gap-2">
            <Button variant="outline" size="sm"><Bookmark className="h-4 w-4" /> Lưu</Button>
            <Button variant="outline" size="sm"><Share2 className="h-4 w-4" /> Chia sẻ</Button>
          </div>
        </div>

        {/* Star Rating for Blog Post */}
        <div className="mt-6 rounded-2xl border border-brand-100 bg-brand-50/40 p-5 sm:p-6">
          <div className="flex flex-col items-start justify-between gap-3 sm:flex-row sm:items-center">
            <div>
              <h3 className="flex items-center gap-2 text-base font-bold text-ink">
                <Star className="h-5 w-5 fill-amber-400 text-amber-400" />
                Đánh giá bài viết ({ratedCount})
              </h3>
              <p className="mt-0.5 text-xs text-ink-muted">
                Bạn thấy nội dung này hữu ích chứ? Nhấp vào sao để gửi đánh giá của bạn.
              </p>
            </div>
            <div className="flex items-center gap-2">
              <div className="flex items-center gap-1">
                {[1, 2, 3, 4, 5].map((score) => {
                  const activeScore = hoverRating || rating || 0;
                  const isFilled = score <= activeScore;
                  return (
                    <button
                      key={score}
                      type="button"
                      onClick={() => {
                        setRating(score);
                        if (!rating) setRatedCount((c) => c + 1);
                        setToast(`Cảm ơn bạn đã đánh giá ${score} sao cho bài viết!`);
                        setTimeout(() => setToast(null), 3000);
                      }}
                      onMouseEnter={() => setHoverRating(score)}
                      onMouseLeave={() => setHoverRating(0)}
                      aria-label={`${score} sao`}
                      className="rounded-lg p-1 transition-transform hover:scale-110 focus:outline-none"
                    >
                      <Star
                        className={`h-6 w-6 transition-colors ${
                          isFilled
                            ? 'fill-amber-400 text-amber-400'
                            : 'fill-brand-100 text-brand-300 hover:text-amber-300'
                        }`}
                      />
                    </button>
                  );
                })}
              </div>
              {rating && (
                <span className="rounded-full bg-amber-100 px-2.5 py-0.5 text-xs font-bold text-amber-800">
                  {rating}/5 ★
                </span>
              )}
            </div>
          </div>
        </div>
      </article>

      {toast && (
        <div className="fixed bottom-6 left-1/2 z-50 flex -translate-x-1/2 items-center gap-2 rounded-full bg-ink px-5 py-3 text-sm font-semibold text-white shadow-xl">
          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-leaf-500">
            <Check className="h-3.5 w-3.5" />
          </span>
          {toast}
        </div>
      )}

      <div className="mt-14">
        <SectionHeading eyebrow="Đọc tiếp" title="Bài viết liên quan" />
        <div className="grid gap-5 md:grid-cols-3">
          {related.map((p) => <PostCard key={p.id} post={p} />)}
        </div>
      </div>
    </PageContainer>
  );
}
