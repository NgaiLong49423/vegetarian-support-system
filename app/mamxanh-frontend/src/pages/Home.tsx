import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  Activity,
  ArrowRight,
  Bookmark,
  CalendarDays,
  ChefHat,
  Leaf,
  Search,
  ShoppingBasket,
  SlidersHorizontal,
  Sparkles,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { AiButton, Badge, BorderGlow, Button, Card, Pattern, RippleDistortion } from '../components/ui';
import {
  PublicRecipeCard,
  PublicRecipeSkeleton,
} from '../components/PublicRecipeCard';
import { PostCard } from '../components/PostCard';
import { useAuth } from '../components/AuthContext';
import {
  recipesApi,
  type RecipeFormOptions,
  type RecipePost,
} from '../api/recipes';
import { posts } from '../data/mockData';

const shortcuts = [
  { to: '/kham-pha', label: 'Khám phá công thức', icon: ChefHat },
  { to: '/ho-so', label: 'Công thức đã lưu', icon: Bookmark },
  { to: '/ke-hoach', label: 'Lịch ăn trong tuần', icon: CalendarDays },
  { to: '/di-cho', label: 'Danh sách đi chợ', icon: ShoppingBasket },
];

export function Home() {
  const { account } = useAuth();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [newRecipes, setNewRecipes] = useState<RecipePost[]>([]);
  const [popularRecipes, setPopularRecipes] = useState<RecipePost[]>([]);
  const [options, setOptions] = useState<RecipeFormOptions | null>(null);
  const [loading, setLoading] = useState(true);
  const [newError, setNewError] = useState(false);
  const [popularError, setPopularError] = useState(false);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    const base = {
      keyword: '',
      page: 0,
      size: 8,
      viewPeriod: 'ALL_TIME' as const,
    };
    void Promise.allSettled([
      recipesApi.searchPublic({ ...base, sort: 'NEWEST' }),
      recipesApi.searchPublic({ ...base, sort: 'MOST_LIKED' }),
      recipesApi.getFormOptions(),
    ]).then(([latest, popular, reference]) => {
      if (!active) return;
      setNewError(latest.status === 'rejected');
      setPopularError(popular.status === 'rejected');
      setNewRecipes(latest.status === 'fulfilled' ? latest.value.items : []);
      setPopularRecipes(
        popular.status === 'fulfilled' ? popular.value.items : [],
      );
      setOptions(reference.status === 'fulfilled' ? reference.value : null);
      setLoading(false);
    });
    return () => {
      active = false;
    };
  }, [retry]);

  return (
    <>
      {/* Hero with interactive water ripple distortion */}
      <section className="relative overflow-hidden border-b-2 border-brand-300/85 bg-stone-900 shadow-sm">
        <div className="absolute inset-0 z-0">
          <RippleDistortion
            src="/hero-art.jpg"
            brushSize={110}
            strength={0.22}
            swirl={1}
            rings={4}
            grayscale={false}
            spread={3}
            fade={3}
            spacing={12}
            tint="#16a34a"
            tintAmount={0.04}
            trigger="both"
            quality="medium"
          />
        </div>

        <div className="pointer-events-none absolute inset-0 z-0 bg-gradient-to-b from-white/75 via-white/65 to-white/90" />

        <PageContainer className="relative z-10 py-12 text-center sm:py-16">
          <Badge tone="brand" className="mx-auto mb-4 bg-white/90 shadow-sm">
            <Leaf className="h-3.5 w-3.5" />
            Mâm Xanh · Ẩm thực thuần lành
          </Badge>
          <h1 className="mx-auto text-3xl font-extrabold leading-[1.18] tracking-tight text-ink sm:text-5xl sm:leading-[1.15]">
            <span className="block">Sống Xanh An Lành,</span>
            <span className="block bg-gradient-to-r from-brand-600 via-brand-500 to-amber-500 bg-clip-text pt-1 pb-2 text-transparent">
              Cân Bằng Vi Chất
            </span>
          </h1>
          <p className="mx-auto mt-3 max-w-2xl text-sm font-medium text-ink-soft sm:text-base">
            Khám phá công thức thuần thực vật và xây dựng thói quen ăn chay phù hợp với bạn.
          </p>
        </PageContainer>
      </section>

      {/* Main dashboard with Pattern aurora background */}
      <div className="relative overflow-hidden">
        <div className="pointer-events-none absolute inset-0 z-0">
          <Pattern />
        </div>
        <div className="pointer-events-none absolute inset-0 z-0 bg-white/45" />

        <PageContainer className="relative z-10 py-5 sm:py-7">
          <div className="grid items-start gap-4 lg:grid-cols-[210px_minmax(0,1fr)] xl:grid-cols-[210px_minmax(0,1fr)_235px]">
            <aside className="hidden space-y-4 lg:block">
              <Card className="p-5 text-center">
                {account?.avatarUrl ? (
                  <img
                    src={account.avatarUrl}
                    alt=""
                    className="mx-auto h-16 w-16 rounded-full object-cover"
                  />
                ) : (
                  <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-raised text-xl font-semibold text-brand-600">
                    {account?.displayName.trim().charAt(0).toUpperCase() ?? (
                      <Leaf className="h-7 w-7" />
                    )}
                  </div>
                )}
                <p className="mt-3 font-semibold text-ink">
                  {account?.displayName ?? 'Chào bạn đến Mâm Xanh'}
                </p>
                <p className="mt-1 text-xs leading-5 text-ink-muted">
                  {account
                    ? account.role === 'EXPERT'
                      ? 'Chuyên gia ẩm thực'
                      : account.role === 'ADMIN'
                        ? 'Quản trị viên'
                        : 'Góc bếp của bạn'
                    : 'Khám phá món chay cho bữa ăn mỗi ngày.'}
                </p>
                {!account && (
                  <Link
                    to="/dang-nhap"
                    className="mt-4 inline-block rounded-full bg-brand-600 px-4 py-2 text-xs font-semibold text-on-brand"
                  >
                    Đăng nhập
                  </Link>
                )}
              </Card>
              <Card className="p-3">
                <h2 className="px-2 py-2 text-sm font-semibold text-ink">
                  Góc bếp của tôi
                </h2>
                <nav aria-label="Lối tắt trang chủ" className="space-y-1">
                  {shortcuts.map((shortcut) => (
                    <Link
                      key={shortcut.to}
                      to={shortcut.to}
                      className="flex items-center gap-3 rounded-lg px-2 py-3 text-xs text-ink-soft hover:bg-raised hover:text-brand-600"
                    >
                      <shortcut.icon className="h-4 w-4 text-ink-muted" />
                      {shortcut.label}
                    </Link>
                  ))}
                </nav>
              </Card>
              <Card className="p-4">
                <h2 className="text-sm font-semibold text-ink">Khẩu vị của bạn</h2>
                <p className="mt-2 text-xs leading-5 text-ink-muted">
                  Lưu những nguyên liệu cần tránh và sở thích ăn uống trong hồ sơ.
                </p>
                <Link
                  to="/ho-so/so-thich-an-uong"
                  className="mt-3 inline-flex items-center gap-1 text-xs font-semibold text-brand-600"
                >
                  Mở sở thích ăn uống
                  <ArrowRight className="h-3.5 w-3.5" />
                </Link>
              </Card>
            </aside>

            <div className="min-w-0 space-y-4">
              <Card className="p-5">
                <h2 className="text-xl font-semibold tracking-tight text-ink">
                  Hôm nay bạn muốn nấu gì?
                </h2>
                <p className="mt-1 text-xs leading-5 text-ink-muted">
                  Một bữa chay ngon bắt đầu từ nguyên liệu bạn có.
                </p>
                <form
                  onSubmit={(event) => {
                    event.preventDefault();
                    navigate(
                      `/kham-pha${
                        query.trim() ? `?q=${encodeURIComponent(query.trim())}` : ''
                      }`,
                    );
                  }}
                  className="mt-4 flex items-center gap-2 rounded-full border border-brand-200 bg-cream-soft pl-3 pr-1.5"
                >
                  <Search className="h-4 w-4 shrink-0 text-ink-muted" />
                  <input
                    aria-label="Tìm công thức trang chủ"
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    placeholder="Tìm món hoặc nguyên liệu..."
                    className="min-w-0 flex-1 bg-transparent py-3 text-xs text-ink outline-none"
                  />
                  <AiButton type="submit" size="sm">
                    Tìm
                  </AiButton>
                </form>
                {options && (
                  <div className="mt-4 flex flex-wrap gap-2">
                    {options.vegetarianTypes.map((choice) => (
                      <Link
                        key={choice.code}
                        to={`/kham-pha?vegetarianType=${encodeURIComponent(choice.code)}`}
                        className="inline-flex items-center gap-1.5 rounded-full border border-brand-200 px-2.5 py-1.5 text-[11px] text-ink-soft hover:border-brand-600 hover:text-brand-600"
                      >
                        <Leaf className="h-3 w-3 text-leaf-700" />
                        {choice.label}
                      </Link>
                    ))}
                  </div>
                )}
              </Card>

              <BorderGlow
                data-testid="border-glow-card"
                borderRadius={20}
                backgroundColor="rgba(255, 255, 255, 0.85)"
                orbitSpeed={8}
                className="p-5 shadow-md shadow-brand-900/5"
              >
                <div className="relative flex flex-col items-start gap-4 sm:flex-row sm:items-center">
                  <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-brand-600 text-white shadow-lg shadow-brand-600/30">
                    <Sparkles className="h-6 w-6" />
                  </span>
                  <div className="flex-1">
                    <h3 className="text-base font-extrabold text-ink sm:text-lg">
                      Trợ lý dinh dưỡng và gợi ý thực đơn
                    </h3>
                    <p className="mt-0.5 text-xs text-ink-soft">
                      Phân bổ món ăn hài hòa cho cả 3 bữa, cân bằng năng lượng và dưỡng chất phù hợp với bạn.
                    </p>
                  </div>
                  <AiButton size="sm" onClick={() => navigate('/ke-hoach')}>
                    Gợi ý thực đơn của tôi
                  </AiButton>
                </div>
              </BorderGlow>

              <RecipeCollection
                title="Mới từ cộng đồng"
                recipes={newRecipes}
                options={options}
                loading={loading}
                error={newError}
                onRetry={() => setRetry((value) => value + 1)}
              />
              <RecipeCollection
                title="Được yêu thích"
                recipes={popularRecipes}
                options={options}
                loading={loading}
                error={popularError}
                onRetry={() => setRetry((value) => value + 1)}
                popular
              />
              <Card className="p-5">
                <div className="mb-4 flex items-center justify-between gap-3">
                  <h2 className="text-base font-semibold text-ink">Góc chia sẻ</h2>
                  <span className="rounded-full bg-raised px-2 py-1 text-[10px] text-ink-muted">
                    Mẫu giao diện
                  </span>
                </div>
                <p className="mb-4 text-xs leading-5 text-ink-muted">
                  Nội dung minh họa cho khu vực cộng đồng.
                </p>
                <div className="space-y-4">
                  {posts.slice(0, 2).map((post) => (
                    <PostCard key={post.id} post={post} />
                  ))}
                </div>
                <Link
                  to="/cong-dong"
                  className="mt-4 inline-flex items-center gap-1 text-xs font-medium text-brand-600"
                >
                  Xem cộng đồng
                  <ArrowRight className="h-3.5 w-3.5" />
                </Link>
              </Card>
            </div>

            <aside className="hidden space-y-4 xl:block">
              <Card className="p-5">
                <CalendarDays className="h-7 w-7 text-brand-600" />
                <h2 className="mt-3 text-base font-semibold text-ink">
                  Bữa ăn cho cả tuần
                </h2>
                <p className="mt-2 text-xs leading-5 text-ink-muted">
                  Xem lịch ăn và những công thức đã được lên kế hoạch.
                </p>
                <Link
                  to="/ke-hoach"
                  className="mt-4 inline-flex items-center gap-1 text-xs font-semibold text-brand-600"
                >
                  Mở lịch ăn
                  <ArrowRight className="h-3.5 w-3.5" />
                </Link>
              </Card>
              <Card className="p-5">
                <Activity className="h-6 w-6 text-leaf-700" />
                <h2 className="mt-3 text-sm font-semibold text-ink">
                  Hiểu nhu cầu dinh dưỡng
                </h2>
                <p className="mt-2 text-xs leading-5 text-ink-muted">
                  Khai báo hồ sơ để xem các chỉ số tham khảo cá nhân.
                </p>
                <Link
                  to="/ho-so/dinh-duong"
                  className="mt-4 block text-xs font-semibold text-brand-600"
                >
                  Hồ sơ dinh dưỡng →
                </Link>
              </Card>
              <Card className="p-5">
                <SlidersHorizontal className="h-6 w-6 text-ink-muted" />
                <h2 className="mt-3 text-sm font-semibold text-ink">
                  Nấu theo khẩu vị
                </h2>
                <p className="mt-2 text-xs leading-5 text-ink-muted">
                  Quản lý nguyên liệu cần tránh và món bạn không thích.
                </p>
                <Link
                  to="/ho-so/so-thich-an-uong"
                  className="mt-4 block text-xs font-semibold text-brand-600"
                >
                  Sở thích ăn uống →
                </Link>
              </Card>
            </aside>
          </div>
        </PageContainer>
      </div>
    </>
  );
}

function RecipeCollection({
  title,
  recipes,
  options,
  loading,
  error,
  onRetry,
  popular = false,
}: {
  title: string;
  recipes: RecipePost[];
  options: RecipeFormOptions | null;
  loading: boolean;
  error: boolean;
  onRetry: () => void;
  popular?: boolean;
}) {
  return (
    <Card className="p-5">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-base font-semibold text-ink">{title}</h2>
        <Link
          to={`/kham-pha${popular ? '?sort=MOST_LIKED' : ''}`}
          className="shrink-0 text-xs text-ink-muted hover:text-brand-600"
        >
          Xem tất cả
        </Link>
      </div>
      {error ? (
        <div role="alert" className="py-4 text-xs text-ink-muted">
          <p>Không tải được công thức. Vui lòng thử lại.</p>
          <button
            type="button"
            onClick={onRetry}
            className="mt-2 font-semibold text-brand-600"
          >
            Thử lại
          </button>
        </div>
      ) : loading ? (
        <div
          role="status"
          aria-label="Đang tải công thức"
          className="grid grid-cols-3 gap-3"
        >
          <PublicRecipeSkeleton count={3} />
        </div>
      ) : recipes.length ? (
        <div
          aria-label={title}
          className="scrollbar-thin grid auto-cols-[160px] grid-flow-col gap-3 overflow-x-auto pb-2 sm:auto-cols-[170px]"
        >
          {recipes.map((recipe) => (
            <PublicRecipeCard
              key={recipe.id}
              recipe={recipe}
              options={options}
              showLikes={popular}
            />
          ))}
        </div>
      ) : (
        <p className="py-8 text-center text-xs text-ink-muted">
          Chưa có công thức công khai trong bộ sưu tập này.
        </p>
      )}
    </Card>
  );
}
