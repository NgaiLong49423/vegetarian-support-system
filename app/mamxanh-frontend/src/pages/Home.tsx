import { useState } from 'react';
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
import { RecipeCard } from '../components/RecipeCard';
import { PostCard } from '../components/PostCard';
import { Badge, Button, SectionHeading } from '../components/ui';
import { categories, posts, recipes } from '../data/mockData';

const categoryIcons = [Salad, Leaf, Soup, UtensilsCrossed];

const quickLinks = [
  { icon: ChefHat, label: 'Khám phá công thức', desc: 'Hơn 1.800 món chay', to: '/kham-pha', tone: 'bg-brand-100 text-brand-600' },
  { icon: CalendarCheck, label: 'Kế hoạch bữa ăn', desc: 'Thực đơn 7 ngày', to: '/ke-hoach', tone: 'bg-leaf-100 text-leaf-600' },
  { icon: ShoppingBasket, label: 'Danh sách đi chợ', desc: 'Tự động tổng hợp', to: '/di-cho', tone: 'bg-amber-100 text-amber-600' },
  { icon: Activity, label: 'Theo dõi dinh dưỡng', desc: 'Cân bằng vi chất', to: '/dinh-duong', tone: 'bg-brand-100 text-brand-600' },
];

export function Home() {
  const navigate = useNavigate();
  const [query, setQuery] = useState('');

  return (
    <>
      {/* Hero */}
      <section className="relative overflow-hidden border-b border-brand-100">
        <div className="pointer-events-none absolute -left-20 -top-32 h-96 w-96 rounded-full bg-brand-200/30 blur-3xl" />
        <div className="pointer-events-none absolute -right-24 top-40 h-96 w-96 rounded-full bg-amber-200/25 blur-3xl" />
        <PageContainer className="relative py-16 text-center sm:py-24">
          <Badge tone="brand" className="mx-auto mb-6">
            <Leaf className="h-3.5 w-3.5" />
            Hệ chuẩn dinh dưỡng thuần thực vật Việt Nam
          </Badge>
          <h1 className="mx-auto max-w-3xl text-4xl font-extrabold leading-[1.05] tracking-tight text-ink sm:text-6xl">
            Sống Xanh An Lành,{' '}
            <span className="bg-gradient-to-r from-brand-600 via-brand-500 to-amber-500 bg-clip-text text-transparent">
              Cân Bằng Vi Chất
            </span>
          </h1>
          <p className="mx-auto mt-5 max-w-2xl text-base text-ink-soft sm:text-lg">
            Hệ sinh thái hơn 1.800+ công thức thuần thực vật được chứng nhận vi chất bởi chuyên gia.
            Nấu ngon, đủ chất, nuôi dưỡng thân tâm và trọn vẹn an yên mỗi ngày.
          </p>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              navigate(`/kham-pha${query ? `?q=${encodeURIComponent(query)}` : ''}`);
            }}
            className="mx-auto mt-8 flex max-w-2xl items-center gap-2 rounded-2xl border border-brand-200 bg-white p-2 shadow-lg shadow-brand-900/5"
          >
            <Search className="ml-3 h-5 w-5 shrink-0 text-ink-muted" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Nhập nguyên liệu sẵn có hoặc món chay bạn đang tìm kiếm..."
              className="min-w-0 flex-1 bg-transparent px-1 py-2 text-sm text-ink outline-none placeholder:text-ink-muted"
            />
            <Button type="submit" size="md" className="shrink-0">
              <Sparkles className="h-4 w-4" />
              <span className="hidden sm:inline">Tìm với Gemini AI</span>
              <span className="sm:hidden">Tìm</span>
            </Button>
          </form>

          <div className="mt-5 flex flex-wrap items-center justify-center gap-2 text-xs text-ink-muted">
            <span className="font-semibold">Gợi ý:</span>
            {['Đậu hũ sốt nấm', 'Phở chay', 'Salad quinoa', 'Cà ri chay'].map((s) => (
              <button
                key={s}
                onClick={() => navigate(`/kham-pha?q=${encodeURIComponent(s)}`)}
                className="rounded-full border border-brand-100 bg-white px-3 py-1 font-medium transition-colors hover:border-brand-300 hover:text-brand-600"
              >
                {s}
              </button>
            ))}
          </div>
        </PageContainer>
      </section>

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
                className="group rounded-2xl border border-brand-100 bg-white p-5 transition-all hover:-translate-y-1 hover:border-brand-200 hover:shadow-lg hover:shadow-brand-900/5"
              >
                <div className="mb-4 flex items-center justify-between">
                  <span className={`flex h-11 w-11 items-center justify-center rounded-xl ${cat.tone === 'leaf' ? 'bg-leaf-100 text-leaf-600' : 'bg-brand-100 text-brand-600'}`}>
                    <Icon className="h-[22px] w-[22px]" />
                  </span>
                  <Badge tone={cat.tone === 'leaf' ? 'leaf' : 'brand'}>{cat.count} món</Badge>
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
        <div className="relative overflow-hidden rounded-3xl border border-brand-200 bg-gradient-to-r from-brand-50 via-white to-amber-50 p-8 sm:p-10">
          <div className="pointer-events-none absolute -right-10 -top-10 h-48 w-48 rounded-full bg-brand-200/30 blur-2xl" />
          <div className="relative flex flex-col items-start gap-6 lg:flex-row lg:items-center">
            <span className="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-brand-600 text-white shadow-lg shadow-brand-600/30">
              <Sparkles className="h-7 w-7" />
            </span>
            <div className="flex-1">
              <h3 className="text-xl font-extrabold text-ink sm:text-2xl">
                Không biết hôm nay ăn gì? Trợ lý Gemini AI hỗ trợ tận tay!
              </h3>
              <p className="mt-1 text-sm text-ink-soft">
                Gợi ý thực đơn cân bằng vi chất dựa trên nguyên liệu sẵn có và sở thích của bạn.
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <Button variant="outline" onClick={() => navigate('/kham-pha')}>Khám phá ngay</Button>
              <Button onClick={() => navigate('/ke-hoach')}>
                <Sparkles className="h-4 w-4" /> Gợi ý thực đơn của tôi
              </Button>
            </div>
          </div>
        </div>
      </PageContainer>

      {/* Quick links */}
      <PageContainer className="py-14">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {quickLinks.map((q) => (
            <Link
              key={q.label}
              to={q.to}
              className="group flex items-center gap-4 rounded-2xl border border-brand-100 bg-white p-4 transition-all hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-md"
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
          eyebrow="Thực đơn hay tuyển"
          title="Thực đơn thay tinh tuyển"
          action={
            <Link to="/kham-pha" className="hidden items-center gap-1 text-sm font-semibold text-brand-600 hover:text-brand-700 sm:inline-flex">
              Xem tất cả <ArrowRight className="h-4 w-4" />
            </Link>
          }
        />
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {recipes.map((r) => (
            <RecipeCard key={r.id} recipe={r} />
          ))}
        </div>
        <div className="mt-8 flex justify-center">
          <Button variant="outline" size="lg" onClick={() => navigate('/kham-pha')}>
            Xem thêm 34 công thức khác
          </Button>
        </div>
      </PageContainer>

      {/* Blog */}
      <PageContainer className="py-14">
        <SectionHeading
          eyebrow="Kiến thức cộng đồng"
          title={<>Bài viết blog chay <span className="text-brand-600">hot nhất</span></>}
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
    </>
  );
}
