import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Flame, TrendingUp } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { PostCard } from '../components/PostCard';
import { Badge } from '../components/ui';
import { posts } from '../data/mockData';

const topics = ['Tất cả', 'Dinh dưỡng', 'Đạm thực vật', 'Lối sống', 'Bí quyết', 'Thực đơn', 'Vi chất'];

export function Community() {
  const [topic, setTopic] = useState('Tất cả');
  const filtered = topic === 'Tất cả' ? posts : posts.filter((p) => p.tags.includes(topic));
  const feed = [...filtered, ...filtered].slice(0, 6);
  const trending = [...posts].sort((a, b) => b.likes - a.likes);

  return (
    <PageContainer className="py-8">
      <nav className="mb-4 text-sm text-ink-muted">
        <Link to="/" className="hover:text-brand-600">Trang chủ</Link> / <span className="font-medium text-brand-600">Cộng đồng</span>
      </nav>

      <div className="mb-6">
        <p className="mb-1 text-xs font-bold uppercase tracking-[0.15em] text-brand-600">Cộng đồng Mâm Xanh</p>
        <h1 className="text-3xl font-extrabold tracking-tight text-ink">Bài viết & Kiến thức ẩm thực chay</h1>
        <p className="mt-1 text-ink-muted">Chia sẻ kinh nghiệm, bí quyết và kiến thức dinh dưỡng từ cộng đồng ăn chay Việt.</p>
      </div>

      <div className="mb-6 flex flex-wrap gap-2">
        {topics.map((t) => (
          <button
            key={t}
            onClick={() => setTopic(t)}
            className={`rounded-full border px-3.5 py-1.5 text-sm font-medium transition-colors ${
              topic === t ? 'border-brand-600 bg-brand-600 text-white' : 'border-brand-200 bg-white text-ink-soft hover:border-brand-300 hover:text-brand-700'
            }`}
          >
            {t}
          </button>
        ))}
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-5">
          {feed.map((p, i) => (
            <PostCard key={`${p.id}-${i}`} post={p} horizontal />
          ))}
        </div>

        <aside className="space-y-4">
          <div className="rounded-2xl border border-brand-100 bg-white p-5">
            <h3 className="mb-4 flex items-center gap-2 font-bold text-ink">
              <TrendingUp className="h-[18px] w-[18px] text-brand-600" /> Bài viết nổi bật
            </h3>
            <div className="space-y-4">
              {trending.map((p, i) => (
                <Link key={p.id} to={`/bai-viet/${p.slug}`} className="group flex gap-3">
                  <span className="text-2xl font-extrabold text-brand-200">{i + 1}</span>
                  <div>
                    <p className="line-clamp-2 text-sm font-semibold text-ink group-hover:text-brand-700">{p.title}</p>
                    <p className="mt-1 flex items-center gap-1 text-xs text-ink-muted">
                      <Flame className="h-3 w-3 text-brand-500" /> {p.likes} lượt thích
                    </p>
                  </div>
                </Link>
              ))}
            </div>
          </div>

          <div className="rounded-2xl border border-brand-100 bg-gradient-to-br from-brand-50 to-white p-5">
            <h3 className="mb-2 font-bold text-ink">Chủ đề thịnh hành</h3>
            <div className="flex flex-wrap gap-2">
              {topics.slice(1).map((t) => (
                <Badge key={t} tone="brand">#{t}</Badge>
              ))}
            </div>
          </div>
        </aside>
      </div>
    </PageContainer>
  );
}
