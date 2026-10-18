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
      {/* Hero with interactive water ripple distortion & grey tone */}
      <section className="relative overflow-hidden border-b border-brand-100 bg-zinc-900 shadow-md">
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
            tint="#52525b"
            tintAmount={0.18}
            trigger="both"
            quality="medium"
          />
        </div>

        <div className="pointer-events-none absolute inset-0 z-0 bg-gradient-to-b from-zinc-800/75 via-zinc-700/65 to-zinc-900/90" />

        <PageContainer className="relative z-10 py-14 text-center sm:py-20">
          <Badge
            tone="brand"
            className="home-hero-badge mx-auto mb-4 shadow-sm backdrop-blur-sm"
          >
            <Leaf className="h-3.5 w-3.5" />
            Mâm Xanh · Ẩm thực thuần lành
          </Badge>
          <h1 className="mx-auto text-3xl font-extrabold leading-[1.18] tracking-tight text-white drop-shadow-sm sm:text-5xl sm:leading-[1.15]">
            <span className="block">Sống Xanh An Lành,</span>
            <span className="block bg-gradient-to-r from-brand-500 via-brand-400 to-amber-400 bg-clip-text pt-1 pb-2 text-transparent">
              Cân Bằng Vi Chất
            </span>
          </h1>
          <p className="mx-auto mt-3 max-w-2xl text-sm font-medium text-zinc-300 sm:text-base">
            Khám phá công thức thuần thực vật và xây dựng thói quen ăn chay phù hợp với bạn.
          </p>
        </PageContainer>
      </section>

      {/* Main dashboard with dark Pattern aurora background & spacious balanced layout */}
      <div className="relative overflow-hidden bg-cream">
        <div className="pointer-events-none absolute inset-0 z-0">
          <Pattern />
        </div>

        <PageContainer className="relative z-10 space-y-7 py-8 sm:py-10 pb-12 sm:pb-16">
          {/* Row 1: Search + BorderGlow AI Assistant (8 cols) & User Kitchen Shortcuts (4 cols) */}
          <div className="grid items-stretch gap-6 lg:grid-cols-12">
            <div className="flex flex-col justify-between gap-6 lg:col-span-8">
              <Card className="p-6 sm:p-7">
                <h2 className="text-xl font-bold tracking-tight text-ink sm:text-2xl">
                  Hôm nay bạn muốn nấu gì?
                </h2>
                <p className="mt-1.5 text-sm leading-6 text-ink-muted">
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
                  className="mt-4 flex items-center gap-2 rounded-full border border-brand-200 bg-cream-soft pl-4 pr-2"
                >
                  <Search className="h-4 w-4 shrink-0 text-ink-muted" />
                  <input
                    aria-label="Tìm công thức trang chủ"
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    placeholder="Tìm món hoặc nguyên liệu..."
                    className="min-w-0 flex-1 bg-transparent py-3.5 text-sm text-ink outline-none placeholder:text-ink-muted"
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
                        className="inline-flex items-center gap-1.5 rounded-full border border-brand-200 bg-cream-soft/60 px-3 py-1.5 text-xs font-medium text-ink-soft transition-colors hover:border-brand-600 hover:text-brand-600"
                      >
                        <Leaf className="h-3.5 w-3.5 text-leaf-700" />
                        {choice.label}
                      </Link>
                    ))}
                  </div>
                )}
              </Card>

              <BorderGlow
                data-testid="border-glow-card"
                borderRadius={20}
                backgroundColor="var(--color-paper)"
                orbitSpeed={8}
                className="p-6"
              >
                <div className="relative flex flex-col items-start gap-4 sm:flex-row sm:items-center">
                  <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-brand-600 text-on-brand shadow-lg shadow-brand-600/30">
                    <Sparkles className="h-6 w-6" />
                  </span>
                  <div className="flex-1">
                    <h3 className="text-base font-extrabold text-ink sm:text-lg">
                      Trợ lý dinh dưỡng và gợi ý thực đơn
                    </h3>
                    <p className="mt-1 text-xs leading-5 text-ink-soft sm:text-sm">
                      Phân bổ món ăn hài hòa cho cả 3 bữa, cân bằng năng lượng và dưỡng chất phù hợp với bạn.
                    </p>
                  </div>
                  <AiButton size="sm" onClick={() => navigate('/ke-hoach')}>
                    Gợi ý thực đơn của tôi
                  </AiButton>
                </div>
              </BorderGlow>
            </div>

            <Card className="flex flex-col justify-between p-6 lg:col-span-4">
              <div className="flex items-center gap-4 border-b border-brand-100 pb-5">
                {account?.avatarUrl ? (
                  <img
                    src={account.avatarUrl}
                    alt=""
                    className="h-14 w-14 shrink-0 rounded-full object-cover ring-2 ring-brand-200"
                  />
                ) : (
                  <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-full bg-raised text-xl font-semibold text-brand-600">
                    {account?.displayName.trim().charAt(0).toUpperCase() ?? (
                      <Leaf className="h-6 w-6" />
                    )}
                  </div>
                )}
                <div className="min-w-0 flex-1">
                  <p className="truncate text-base font-bold text-ink">
                    {account?.displayName ?? 'Chào bạn đến Mâm Xanh'}
                  </p>
                  <p className="mt-0.5 text-xs leading-5 text-ink-muted">
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
                      className="mt-2.5 inline-block rounded-full bg-brand-600 px-4 py-1.5 text-xs font-semibold text-on-brand hover:bg-brand-500"
                    >
                      Đăng nhập tài khoản
                    </Link>
                  )}
                </div>
              </div>

              <div className="mt-5">
                <h2 className="mb-3 text-sm font-semibold text-ink">
                  Góc bếp của tôi
                </h2>
                <nav
                  aria-label="Lối tắt trang chủ"
                  className="grid grid-cols-2 gap-2.5"
                >
                  {shortcuts.map((shortcut) => (
                    <Link
                      key={shortcut.to}
                      to={shortcut.to}
                      className="flex flex-col items-start gap-2 rounded-xl border border-brand-100 bg-cream-soft/70 p-3.5 text-xs font-medium text-ink-soft transition-colors hover:border-brand-300 hover:bg-raised hover:text-brand-600"
                    >
                      <shortcut.icon className="h-4 w-4 text-brand-600" />
                      <span>{shortcut.label}</span>
                    </Link>
                  ))}
                </nav>
              </div>
            </Card>
          </div>

          {/* Row 2: 3 Equal Feature Cards across full width */}
          <div className="grid gap-6 md:grid-cols-3">
            <Card hover className="flex flex-col justify-between p-6">
              <div>
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-100 text-brand-600">
                  <CalendarDays className="h-5 w-5" />
                </div>
                <h2 className="mt-4 text-base font-bold text-ink">
                  Bữa ăn cho cả tuần
                </h2>
                <p className="mt-1.5 text-xs leading-5 text-ink-muted sm:text-sm">
                  Xem lịch ăn và những công thức đã được lên kế hoạch cho cả gia đình.
                </p>
              </div>
              <Link
                to="/ke-hoach"
                className="mt-4 inline-flex items-center gap-1.5 text-xs font-semibold text-brand-600 hover:text-brand-500 sm:text-sm"
              >
                Mở lịch ăn
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </Card>

            <Card hover className="flex flex-col justify-between p-6">
              <div>
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-leaf-100 text-leaf-700">
                  <Activity className="h-5 w-5" />
                </div>
                <h2 className="mt-4 text-base font-bold text-ink">
                  Hiểu nhu cầu dinh dưỡng
                </h2>
                <p className="mt-1.5 text-xs leading-5 text-ink-muted sm:text-sm">
                  Khai báo hồ sơ để xem các chỉ số BMI, BMR, TDEE và nhu cầu tham khảo cá nhân.
                </p>
              </div>
              <Link
                to="/ho-so/dinh-duong"
                className="mt-4 inline-flex items-center gap-1.5 text-xs font-semibold text-brand-600 hover:text-brand-500 sm:text-sm"
              >
                Hồ sơ dinh dưỡng →
              </Link>
            </Card>

            <Card hover className="flex flex-col justify-between p-6">
              <div>
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-raised text-ink-soft">
                  <SlidersHorizontal className="h-5 w-5" />
                </div>
                <h2 className="mt-4 text-base font-bold text-ink">
                  Khẩu vị của bạn
                </h2>
                <p className="mt-1.5 text-xs leading-5 text-ink-muted sm:text-sm">
                  Lưu những nguyên liệu cần tránh và sở thích ăn uống trong hồ sơ.
                </p>
              </div>
              <Link
                to="/ho-so/so-thich-an-uong"
                className="mt-4 inline-flex items-center gap-1.5 text-xs font-semibold text-brand-600 hover:text-brand-500 sm:text-sm"
              >
                Mở sở thích ăn uống
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            </Card>
          </div>

          {/* Row 3 & Row 4: Full-width Recipe Collections */}
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

          {/* Row 5: Full-width 2-Column Community Posts */}
          <Card className="p-6 sm:p-7">
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-bold text-ink">Góc chia sẻ</h2>
                <p className="mt-1 text-xs leading-5 text-ink-muted sm:text-sm">
                  Nội dung minh họa cho khu vực cộng đồng.
                </p>
              </div>
              <div className="flex items-center gap-3">
                <span className="rounded-full bg-raised px-2.5 py-1 text-xs text-ink-muted">
                  Mẫu giao diện
                </span>
                <Link
                  to="/cong-dong"
                  className="inline-flex items-center gap-1 text-xs font-semibold text-brand-600 hover:text-brand-500 sm:text-sm"
                >
                  Xem cộng đồng
                  <ArrowRight className="h-3.5 w-3.5" />
                </Link>
              </div>
            </div>
            <div className="grid gap-6 md:grid-cols-2">
              {posts.slice(0, 2).map((post) => (
                <PostCard key={post.id} post={post} />
              ))}
            </div>
          </Card>
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
    <Card className="p-6 sm:p-7">
      <div className="mb-5 flex items-center justify-between gap-3">
        <h2 className="text-lg font-bold text-ink">{title}</h2>
        <Link
          to={`/kham-pha${popular ? '?sort=MOST_LIKED' : ''}`}
          className="shrink-0 text-xs font-semibold text-ink-muted hover:text-brand-600 sm:text-sm"
        >
          Xem tất cả
        </Link>
      </div>
      {error ? (
        <div role="alert" className="py-6 text-xs text-ink-muted sm:text-sm">
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
          className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4"
        >
          <PublicRecipeSkeleton count={4} />
        </div>
      ) : recipes.length ? (
        <div
          aria-label={title}
          className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4"
        >
          {recipes.slice(0, 4).map((recipe) => (
            <PublicRecipeCard
              key={recipe.id}
              recipe={recipe}
              options={options}
            />
          ))}
        </div>
      ) : (
        <p className="py-10 text-center text-xs text-ink-muted sm:text-sm">
          Chưa có công thức công khai trong bộ sưu tập này.
        </p>
      )}
    </Card>
  );
}
