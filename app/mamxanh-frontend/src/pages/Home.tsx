import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  ArrowRight,
  Leaf,
  Search,
  Sparkles,
  Salad,
  Soup,
  UtensilsCrossed,
  ChefHat,
  CalendarCheck,
  ShoppingBasket,
  Activity,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { PostCard } from '../components/PostCard';
import { AiButton, Badge, BorderGlow, Button, Pattern, SectionHeading } from '../components/ui';
import { categories, posts } from '../data/mockData';
import { apiClient } from '../lib/apiClient';
import type { RecipePost } from '../api/recipes';

const categoryIcons = [Salad, Leaf, Soup, UtensilsCrossed];

const quickLinks = [
  { icon: ChefHat, label: 'Khám phá công thức', desc: 'Công thức đã xuất bản', to: '/kham-pha', tone: 'bg-brand-100 text-brand-600' },
  { icon: CalendarCheck, label: 'Kế hoạch bữa ăn', desc: 'Thực đơn 7 ngày', to: '/ke-hoach', tone: 'bg-leaf-100 text-leaf-600' },
  { icon: ShoppingBasket, label: 'Danh sách đi chợ', desc: 'Tự động tổng hợp', to: '/di-cho', tone: 'bg-amber-100 text-amber-600' },
  { icon: Activity, label: 'Theo dõi dinh dưỡng', desc: 'Cân bằng vi chất', to: '/dinh-duong', tone: 'bg-brand-100 text-brand-600' },
];

export function Home() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [featured, setFeatured] = useState<RecipePost[]>([]);
  useEffect(() => {
    apiClient.get<{ items: RecipePost[] }>('/recipes', { params: { page: 0, size: 4 } })
      .then(({ data }) => setFeatured(data.items))
      .catch(() => setFeatured([]));
  }, []);

  return (
    <>
      {/* Hero with soft atmospheric background */}
      <section className="relative overflow-hidden border-b-2 border-brand-300/85 bg-stone-900 shadow-sm">
        {/* Full-width Hero Art Background */}
        <div
          className="absolute inset-0 z-0 bg-cover bg-center opacity-40"
          style={{ backgroundImage: "url('/hero-art.jpg')" }}
        />

        {/* Soft atmospheric overlay for crystal clear text readability */}
        <div className="pointer-events-none absolute inset-0 z-0 bg-gradient-to-b from-white/75 via-white/65 to-white/90" />

        {/* Ambient decorative blurs */}
        <div className="pointer-events-none absolute -left-20 -top-32 h-96 w-96 rounded-full bg-brand-200/25 blur-3xl" />
        <div className="pointer-events-none absolute -right-24 top-40 h-96 w-96 rounded-full bg-amber-200/20 blur-3xl" />

        <PageContainer className="relative z-10 py-16 text-center sm:py-24">
          <Badge tone="brand" className="mx-auto mb-6 bg-white/90 shadow-sm">
            <Leaf className="h-3.5 w-3.5" />
            Mâm Xanh · Ẩm thực thuần lành
          </Badge>
          <h1 className="mx-auto text-4xl font-extrabold leading-[1.18] tracking-tight text-ink sm:text-6xl sm:leading-[1.15]">
            <span className="block">Sống Xanh An Lành,</span>
            <span className="block bg-gradient-to-r from-brand-600 via-brand-500 to-amber-500 bg-clip-text pt-1 pb-2.5 text-transparent">
              Cân Bằng Vi Chất
            </span>
          </h1>
          <p className="mx-auto mt-5 max-w-2xl text-base font-medium text-ink-soft sm:text-lg">
            Khám phá công thức thuần thực vật và xây dựng thói quen ăn chay phù hợp với bạn.
          </p>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              navigate(`/kham-pha${query ? `?q=${encodeURIComponent(query)}` : ''}`);
            }}
            className="mx-auto mt-8 flex max-w-2xl items-center gap-2 rounded-2xl border-2 border-brand-200/90 bg-white/95 p-2 shadow-xl shadow-brand-900/10"
          >
            <Search className="ml-3 h-5 w-5 shrink-0 text-ink-muted" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Nhập nguyên liệu sẵn có hoặc món chay bạn đang tìm kiếm..."
              className="min-w-0 flex-1 bg-transparent px-1 py-2 text-sm text-ink outline-none placeholder:text-ink-muted"
            />
            <AiButton type="submit" size="sm" className="shrink-0">
              <span className="hidden sm:inline">Tìm công thức</span>
              <span className="sm:hidden">Tìm</span>
            </AiButton>
          </form>

          <div className="mt-5 flex flex-wrap items-center justify-center gap-2 text-xs text-ink-muted">
            <span className="font-semibold text-ink">Gợi ý:</span>
            {['Đậu hũ sốt nấm', 'Phở chay', 'Salad quinoa', 'Cà ri chay'].map((s) => (
              <button
                key={s}
                onClick={() => navigate(`/kham-pha?q=${encodeURIComponent(s)}`)}
                className="rounded-full border border-brand-100/90 bg-white/90 px-3 py-1 font-medium text-ink shadow-sm transition-colors hover:border-brand-300 hover:text-brand-600"
              >
                {s}
              </button>
            ))}
          </div>
        </PageContainer>
      </section>

      {/* Lower content with Pattern background extending down towards footer */}
      <div className="relative overflow-hidden">
        {/* Rotating Aurora Gradient Background */}
        <div className="pointer-events-none absolute inset-0 z-0">
          <Pattern />
        </div>

        {/* Soft atmospheric overlay for clear text and card contrast */}
        <div className="pointer-events-none absolute inset-0 z-0 bg-white/45" />

        {/* Smooth fade-out near footer */}
        <div className="pointer-events-none absolute inset-x-0 bottom-0 z-0 h-48 bg-gradient-to-t from-[#fffaf5] via-[#fffaf5]/80 to-transparent" />

        <div className="relative z-10">
          {/* Categories */}
          <PageContainer className="py-14">
            <SectionHeading
              eyebrow="Chế độ ăn phù hợp"
              title="Chế độ chay phù hợp với bạn"
              action={
                <Link to="/kham-pha" className="hidden items-center gap-1 text-sm font-semibold text-brand-600 hover:text-brand-700 sm:inline-flex">
                  Xem tất cả <ArrowRight className="h-4 w-4" />
                </Link>
              }
            />
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {categories.map((cat, i) => {
                const Icon = categoryIcons[i];
                return (
                  <Link
                    key={cat.id}
                    to="/kham-pha"
                    className="group rounded-2xl border border-white/80 bg-white/90 p-5 shadow-sm transition-all hover:-translate-y-1 hover:border-brand-200 hover:bg-white hover:shadow-lg hover:shadow-brand-900/5"
                  >
                    <div className="mb-4 flex items-center justify-between">
                      <span className={`flex h-11 w-11 items-center justify-center rounded-xl ${cat.tone === 'leaf' ? 'bg-leaf-100 text-leaf-600' : 'bg-brand-100 text-brand-600'}`}>
                        <Icon className="h-[22px] w-[22px]" />
                      </span>
                      <Badge tone={cat.tone === 'leaf' ? 'leaf' : 'brand'}>Gợi ý</Badge>
                    </div>
                    <h3 className="mb-1 font-bold text-ink group-hover:text-brand-700">{cat.name}</h3>
                    <p className="text-sm text-ink-muted">{cat.desc}</p>
                  </Link>
                );
              })}
            </div>
          </PageContainer>

          {/* AI banner */}
          <PageContainer>
            <BorderGlow
              borderRadius={24}
              backgroundColor="rgba(255, 255, 255, 0.75)"
              orbitSpeed={8}
              className="p-8 sm:p-10 shadow-lg shadow-brand-900/5"
            >
              <div className="relative flex flex-col items-start gap-6 lg:flex-row lg:items-center">
                <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-brand-600 text-white shadow-lg shadow-brand-600/30">
                  <Sparkles className="h-7 w-7" />
                </span>
                <div className="flex-1">
                  <h3 className="text-xl font-extrabold text-ink sm:text-2xl">
                    Trợ lý dinh dưỡng và gợi ý thực đơn
                  </h3>
                  <p className="mt-1 text-sm text-ink-soft">
                    Phân bổ món ăn hài hòa cho cả 3 bữa, cân bằng năng lượng và dưỡng chất phù hợp với bạn.
                  </p>
                </div>
                <div className="flex flex-wrap gap-3">
                  <Button variant="outline" onClick={() => navigate('/kham-pha')}>Khám phá ngay</Button>
                  <AiButton size="md" onClick={() => navigate('/ke-hoach')}>
                    Gợi ý thực đơn của tôi
                  </AiButton>
                </div>
              </div>
            </BorderGlow>
          </PageContainer>

          {/* Quick links */}
          <PageContainer className="py-14">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              {quickLinks.map((q) => (
                <Link
                  key={q.label}
                  to={q.to}
                  className="group flex items-center gap-4 rounded-2xl border border-white/80 bg-white/90 p-4 shadow-sm transition-all hover:-translate-y-0.5 hover:border-brand-200 hover:bg-white hover:shadow-md"
                >
                  <span className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-xl ${q.tone}`}>
                    <q.icon className="h-6 w-6" />
                  </span>
                  <div>
                    <p className="font-bold text-ink group-hover:text-brand-700">{q.label}</p>
                    <p className="text-xs text-ink-muted">{q.desc}</p>
                  </div>
                  <ArrowRight className="ml-auto h-4 w-4 text-ink-muted transition-transform group-hover:translate-x-1 group-hover:text-brand-600" />
                </Link>
              ))}
            </div>
          </PageContainer>

          {/* Featured recipes */}
          <PageContainer>
            <SectionHeading
              eyebrow="Công thức chọn lọc"
              title="Công thức được cộng đồng yêu thích"
              action={
                <Link to="/kham-pha" className="hidden items-center gap-1 text-sm font-semibold text-brand-600 hover:text-brand-700 sm:inline-flex">
                  Xem tất cả <ArrowRight className="h-4 w-4" />
                </Link>
              }
            />
            <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
              {featured.map((recipe) => {
                const image = recipe.media.find((item) => item.cover) ?? recipe.media[0];
                return <Link key={recipe.id} to={`/cong-thuc/id/${recipe.id}`} className="overflow-hidden rounded-2xl border border-white/80 bg-white/90 hover:bg-white hover:shadow-lg transition-all">
                  <div className="aspect-[4/3] bg-brand-50">{image ? <img src={image.url} alt={recipe.title} className="h-full w-full object-cover" /> : <div className="flex h-full items-center justify-center text-sm text-ink-muted">Ảnh công thức</div>}</div>
                  <div className="p-4"><p className="text-xs font-semibold text-brand-700">{recipe.vegetarianType} · {recipe.dishCategory}</p><h3 className="mt-2 font-bold text-ink">{recipe.title}</h3><p className="mt-2 text-xs text-ink-muted">Tác giả: {recipe.authorName}</p></div>
                </Link>;
              })}
            </div>
            {featured.length === 0 && <p className="text-sm text-ink-muted">Backend chưa có công thức công khai hoặc chưa thể kết nối.</p>}
            <div className="mt-8 flex justify-center">
              <Button variant="outline" size="lg" onClick={() => navigate('/kham-pha')}>
                Khám phá công thức
              </Button>
            </div>
          </PageContainer>

          {/* Blog */}
          <PageContainer className="py-14">
            <SectionHeading
              eyebrow="Góc chia sẻ"
              title={<>Bài viết cộng đồng <span className="text-brand-600">nổi bật</span></>}
              action={
                <Link to="/cong-dong" className="hidden items-center gap-1 text-sm font-semibold text-brand-600 hover:text-brand-700 sm:inline-flex">
                  Xem tất cả bài viết <ArrowRight className="h-4 w-4" />
                </Link>
              }
            />
            <div className="grid gap-5 md:grid-cols-3">
              {posts.map((p) => (
                <PostCard key={p.id} post={p} />
              ))}
            </div>
          </PageContainer>
        </div>
      </div>
    </>
  );
}
