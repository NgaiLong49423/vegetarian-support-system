import { RecipeDefaultArtwork } from '../components/RecipeDefaultArtwork';
import { RecipeReactions } from '../components/RecipeReactions';
import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import {
  ArrowLeft,
  ArrowLeftRight,
  Check,
  ChefHat,
  ChevronDown,
  Clock3,
  Download,
  Eye,
  Leaf,
  LoaderCircle,
  MessageCircle,
  Minus,
  Plus,
  Printer,
  Share2,
  Sparkles,
  Users,
  Utensils,
  Video,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Badge, Button, Card } from '../components/ui';
import { asApiError } from '../lib/apiClient';
import { recipesApi, type RecipeDetail } from '../api/recipes';
import { SaveRecipeButton } from '../components/SaveRecipeButton';
import { RecipeViews } from '../components/RecipeViews';
import { MemberAvatar } from '../components/MemberAvatar';
import { RelatedRecipes } from '../components/RelatedRecipes';
import { RecipeComments } from '../components/RecipeComments';
import { MealPlanRecipeServings } from './MealPlanRecipeServings';
import { useAiChat } from '../components/AiChatContext';

const NUTRIENTS = [
  { key: 'ENERGY_KCAL', label: 'Năng lượng', unit: 'kcal' },
  { key: 'PROTEIN_G', label: 'Chất đạm', unit: 'g' },
  { key: 'TOTAL_FAT_G', label: 'Chất béo', unit: 'g' },
  { key: 'CARBOHYDRATE_G', label: 'Tinh bột', unit: 'g' },
  { key: 'FIBER_G', label: 'Chất xơ', unit: 'g' },
  { key: 'CALCIUM_MG', label: 'Canxi', unit: 'mg' },
  { key: 'IRON_MG', label: 'Sắt', unit: 'mg' },
  { key: 'VITAMIN_B12_MCG', label: 'Vitamin B12', unit: 'mcg' },
  { key: 'ZINC_MG', label: 'Kẽm', unit: 'mg' },
] as const;

/** `publishedAt` is UTC without an offset; show the date in Vietnam time (FR-23). */
function publishedDate(value: string) {
  return new Date(`${value}Z`).toLocaleDateString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' });
}

function formatNumber(value: number | null | undefined, maximumFractionDigits = 2) {
  if (value === null || value === undefined || !Number.isFinite(value)) return 'Chưa có dữ liệu';
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits }).format(value);
}

function getYoutubeEmbedUrl(rawUrl: string) {
  try {
    const url = new URL(rawUrl);
    const host = url.hostname.replace(/^www\./, '').toLowerCase();
    let videoId = '';
    if (host === 'youtu.be') videoId = url.pathname.slice(1).split('/')[0] ?? '';
    if (host === 'youtube.com' || host === 'm.youtube.com') {
      if (url.pathname === '/watch') videoId = url.searchParams.get('v') ?? '';
      else if (/^\/(embed|shorts)\//.test(url.pathname)) videoId = url.pathname.split('/')[2] ?? '';
    }
    return /^[A-Za-z0-9_-]{11}$/.test(videoId)
      ? `https://www.youtube-nocookie.com/embed/${videoId}`
      : null;
  } catch {
    return null;
  }
}

function downloadText(recipe: RecipeDetail) {
  const ingredients = (recipe.ingredients ?? [])
    .map((item) => `- ${item.name}: ${formatNumber(item.quantity)} ${item.unitName || item.unitCode}`)
    .join('\n');
  const sections = [
    recipe.title,
    `Tác giả: ${recipe.author?.displayName ?? 'Chuyên gia Mâm Xanh'}`,
    `Thể loại: ${recipe.dishCategoryLabel} · ${recipe.vegetarianTypeLabel}`,
    `Khẩu phần: ${recipe.servings}`,
    `Thời gian: chuẩn bị ${recipe.prepTimeMinutes} phút, nấu ${recipe.cookTimeMinutes} phút`,
    '',
    'NGUYÊN LIỆU',
    ingredients || 'Chưa có nguyên liệu.',
    '',
    'CÁCH LÀM',
    recipe.instructions,
  ];
  if (recipe.youtubeUrl) sections.push(`\nVideo: ${recipe.youtubeUrl}`);
  const text = sections.join('\n');
  const blob = new Blob([`\uFEFF${text}`], { type: 'text/plain;charset=utf-8' });
  const href = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = href;
  anchor.download = `${recipe.title.trim().replace(/[^\p{L}\p{N}]+/gu, '-').replace(/^-|-$/g, '') || 'cong-thuc'}.txt`;
  anchor.click();
  window.setTimeout(() => URL.revokeObjectURL(href), 1000);
}

export function ApiRecipeDetail() {
  const { recipeId: rawId, slug } = useParams();
  return <PublishedApiRecipeDetail key={rawId ?? slug} recipeId={Number(rawId ?? slug)} />;
}

function PublishedApiRecipeDetail({ recipeId }: { recipeId: number }) {
  const { openChat } = useAiChat();
  const [searchParams] = useSearchParams();
  const fromMealPlan = searchParams.has('mealPlanWeek') || searchParams.has('mealPlanEntryId');
  const planContextKey = `${recipeId}:${searchParams.get('mealPlanWeek')}:${searchParams.get('mealPlanEntryId')}`;
  const backTarget = fromMealPlan ? `/ke-hoach?weekStartDate=${encodeURIComponent(searchParams.get('mealPlanWeek') ?? '')}&mealDate=${encodeURIComponent(searchParams.get('mealDate') ?? '')}` : '/kham-pha';
  const [recipe, setRecipe] = useState<RecipeDetail | null>(null);
  const [error, setError] = useState('');
  const [selectedMedia, setSelectedMedia] = useState(0);
  const [servings, setServings] = useState(1);
  const [checkedIngredients, setCheckedIngredients] = useState<number[]>([]);
  const [exportOpen, setExportOpen] = useState(false);
  const [shareMessage, setShareMessage] = useState('');

  useEffect(() => {
    let alive = true;
    setError('');
    setRecipe(null);
    if (!Number.isSafeInteger(recipeId) || recipeId < 1) {
      setError('ID công thức không hợp lệ.');
      return () => { alive = false; };
    }
    recipesApi.getPublished(recipeId)
      .then((data) => {
        if (!alive) return;
        setRecipe(data);
        setServings(Math.max(1, data.servings || 1));
        setSelectedMedia(Math.max(0, (data.media ?? []).findIndex((media) => media.cover)));
      })
      .catch((cause: unknown) => {
        if (alive) {
          const apiError = asApiError(cause);
          setError(apiError.status === 404
            ? (apiError.message || 'Không tìm thấy công thức.')
            : apiError.message);
        }
      });
    return () => { alive = false; };
  }, [recipeId]);

  const media = useMemo(() => recipe?.media ?? [], [recipe]);
  const activeMedia = media[selectedMedia] ?? media.find((item) => item.cover) ?? media[0];
  const youtubeEmbedUrl = recipe?.youtubeUrl ? getYoutubeEmbedUrl(recipe.youtubeUrl) : null;

  async function shareRecipe() {
    if (!recipe) return;
    const url = new URL(`/cong-thuc/id/${recipe.recipeId}`, window.location.origin).href;
    try {
      if (navigator.share) await navigator.share({ title: recipe.title, url });
      else if (navigator.clipboard) {
        await navigator.clipboard.writeText(url);
        setShareMessage('Đã sao chép liên kết công thức.');
      } else {
        setShareMessage('Trình duyệt không hỗ trợ chia sẻ liên kết.');
      }
    } catch (cause) {
      if (cause instanceof Error && cause.name !== 'AbortError') {
        setShareMessage('Chưa thể chia sẻ liên kết. Vui lòng thử lại.');
      }
    }
  }

  if (error) {
    return (
      <PageContainer className="py-16">
        <Card className="mx-auto max-w-xl p-8 text-center">
          <h1 className="text-xl font-bold text-ink">Không thể mở công thức</h1>
          <p role="alert" className="mt-2 text-sm text-ink-muted">{error}</p>
          <Link to="/kham-pha" className="mt-5 inline-flex items-center gap-2 text-sm font-bold text-brand-700">
            <ArrowLeft className="h-4 w-4" />Quay lại khám phá
          </Link>
        </Card>
      </PageContainer>
    );
  }

  if (!recipe) {
    return (
      <PageContainer className="py-16">
        <p role="status" className="flex items-center justify-center gap-2 text-ink-muted">
          <LoaderCircle className="h-5 w-5 animate-spin" />Đang tải công thức…
        </p>
      </PageContainer>
    );
  }

  const author = recipe.author ?? {
    userId: 0,
    displayName: 'Tác giả Mâm Xanh',
    avatarUrl: null,
  };
  const statistics = recipe.statistics ?? {
    viewCount: 0,
    likes: 0,
    dislikes: 0,
    likePercentage: null,
    reactionCount: 0,
  };
  const nutritionComplete = recipe.nutrition?.complete ?? recipe.nutritionComplete;
  const missingNutrition = recipe.nutrition?.ingredientsMissingData ?? recipe.ingredientsWithoutNutrition ?? [];
  const printRecipe = () => window.print();

  return <PageContainer className="recipe-detail-page py-6 sm:py-8">
    <div className="recipe-detail-no-print mb-5 flex flex-wrap items-center justify-between gap-3">
      <Link to={backTarget} className="inline-flex items-center gap-2 text-sm font-semibold text-brand-700 hover:text-brand-900"><ArrowLeft className="h-4 w-4" />{fromMealPlan ? 'Về lịch ăn' : 'Khám phá'}</Link>
      <div className="flex flex-wrap items-center gap-2">
        <SaveRecipeButton recipeId={recipe.recipeId} />
        <Link to={`/so-sanh?a=${recipe.recipeId}`} aria-label="So sánh với công thức khác" className="rounded-full"><Button as="span" variant="outline" size="sm"><ArrowLeftRight className="h-4 w-4" />So sánh</Button></Link>
        <button type="button" onClick={() => openChat(recipe.recipeId)} className="inline-flex items-center gap-2 rounded-full border border-brand-200 px-3 py-2 text-xs font-semibold text-brand-700 hover:bg-brand-50"><MessageCircle className="h-4 w-4" aria-hidden="true" />Hỏi AI về công thức này</button>
        <Button variant="outline" size="sm" onClick={shareRecipe}><Share2 className="h-4 w-4" />Chia sẻ</Button>
        <div className="relative">
          <Button variant="primary" size="sm" aria-expanded={exportOpen} onClick={() => setExportOpen((open) => !open)}><Download className="h-4 w-4" />Xuất công thức<ChevronDown className="h-4 w-4" /></Button>
          {exportOpen && <div className="absolute right-0 z-20 mt-2 w-52 rounded-xl border border-brand-100 bg-paper p-1.5 shadow-lg">
            <button type="button" className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-ink hover:bg-brand-50" onClick={() => { downloadText(recipe); setExportOpen(false); }}><Download className="h-4 w-4 text-brand-600" />Tải tệp TXT</button>
            <button type="button" className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm text-ink hover:bg-brand-50" onClick={() => { setExportOpen(false); printRecipe(); }}><Printer className="h-4 w-4 text-brand-600" />In / lưu PDF A4</button>
          </div>}
        </div>
      </div>
    </div>
    {shareMessage && <p role="status" className="recipe-detail-no-print mb-4 text-right text-sm text-leaf-700">{shareMessage}</p>}

    {/* Header: Tiêu đề, Mô tả và Thông tin tác giả */}
    <div className="mb-6">
      <div className="mb-3 flex flex-wrap gap-2">
        <Badge tone="leaf">{recipe.vegetarianTypeLabel}</Badge>
        <Badge>{recipe.dishCategoryLabel}</Badge>
        <Badge tone="neutral">{recipe.difficultyLabel}</Badge>
      </div>
      <h1 className="max-w-4xl text-3xl font-extrabold tracking-tight text-ink sm:text-4xl">{recipe.title}</h1>
      {recipe.description && <p className="mt-3 max-w-3xl leading-7 text-ink-muted">{recipe.description}</p>}
      <div className="mt-5 flex flex-wrap items-center gap-x-5 gap-y-3 text-sm text-ink-muted">
        <div className="flex items-center gap-3">
          <MemberAvatar name={author.displayName} url={author.avatarUrl} />
          <div>
            <p className="text-xs">Tác giả</p>
            {author.userId > 0 ? (
              <Link to={`/thanh-vien/${author.userId}`} className="font-bold text-ink hover:text-brand-700">
                {author.displayName}
              </Link>
            ) : (
              <span className="font-bold text-ink">{author.displayName}</span>
            )}
          </div>
        </div>
        <span className="inline-flex items-center gap-1.5"><Clock3 className="h-4 w-4 text-brand-600" />{recipe.prepTimeMinutes + recipe.cookTimeMinutes} phút tổng cộng</span>
        <span className="inline-flex items-center gap-1.5"><Users className="h-4 w-4 text-brand-600" />{recipe.servings} khẩu phần gốc</span>
        {recipe.publishedAt && <time dateTime={recipe.publishedAt}>Đăng ngày {publishedDate(recipe.publishedAt)}</time>}
      </div>
    </div>

    {/* Hàng 1: Tổng quan (Ảnh món ăn & Thông tin nhanh) - Luôn cân bằng chiều cao 1:1 tuyệt đối */}
    <div className="mb-8 grid gap-6 lg:grid-cols-12 lg:items-stretch">
      {/* Cột trái (7 cột): Thư viện ảnh */}
      <section aria-label="Thư viện ảnh công thức" className="flex flex-col lg:col-span-7">
        <Card className="flex flex-1 flex-col overflow-hidden">
          <div className="relative flex-1 min-h-[300px] sm:min-h-[360px] w-full overflow-hidden bg-brand-50">
            {activeMedia ? (
              <img
                src={activeMedia.blobUrl}
                alt={recipe.title}
                className="absolute inset-0 h-full w-full object-cover object-center"
                fetchPriority="high"
              />
            ) : (
              <div className="flex h-full w-full items-center justify-center">
                <RecipeDefaultArtwork vegetarianType={recipe.vegetarianType} label={recipe.vegetarianTypeLabel} />
              </div>
            )}
          </div>
          {media.length > 1 && (
            <div className="recipe-detail-no-print grid grid-cols-4 gap-2 border-t border-brand-100 p-3 sm:gap-3 sm:p-4 shrink-0">
              {media.map((item, index) => (
                <button
                  key={`${item.displayOrder}-${item.blobUrl}`}
                  type="button"
                  onClick={() => setSelectedMedia(index)}
                  aria-label={`Xem ảnh ${index + 1}`}
                  aria-pressed={index === selectedMedia}
                  className={`h-14 sm:h-16 overflow-hidden rounded-xl border-2 bg-brand-50 transition-all ${index === selectedMedia ? 'border-brand-600 scale-[1.02]' : 'border-transparent opacity-75 hover:opacity-100'}`}
                >
                  <img src={item.blobUrl} alt="" loading="lazy" className="h-full w-full object-cover" />
                </button>
              ))}
            </div>
          )}
        </Card>
      </section>

      {/* Cột phải (5 cột): Thông tin nhanh - Cân bằng chiều cao ngang với ảnh */}
      <div className="flex flex-col lg:col-span-5">
        <Card className="flex flex-1 flex-col justify-between p-5 sm:p-6 space-y-5">
          <div>
            <div className="flex items-center justify-between gap-2">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.14em] text-brand-700">Tổng quan</p>
                <h2 className="mt-1 text-lg font-extrabold text-ink">Thông tin nhanh</h2>
              </div>
              <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-50 text-brand-700">
                <Clock3 className="h-4 w-4" />
              </span>
            </div>
            <dl className="mt-3 divide-y divide-brand-100">
              <QuickInfo label="Thời gian chuẩn bị" value={`${recipe.prepTimeMinutes} phút`} />
              <QuickInfo label="Thời gian nấu" value={`${recipe.cookTimeMinutes} phút`} />
              <QuickInfo label="Khẩu phần gốc" value={`${recipe.servings} người`} />
              <QuickInfo
                label="Độ khó"
                value={
                  <span>
                    {recipe.difficultyLabel}
                    <span className="block text-xs font-normal text-ink-muted">
                      ({(recipe.ingredients ?? []).length} nguyên liệu · {recipe.prepTimeMinutes + recipe.cookTimeMinutes} phút)
                    </span>
                  </span>
                }
              />
            </dl>
          </div>

          <div className="rounded-2xl border border-brand-100 bg-brand-50/60 p-4">
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-brand-100/80 pb-2.5">
              <div className="flex items-center gap-2">
                <span className="flex h-6 w-6 items-center justify-center rounded-lg bg-brand-100 text-brand-700">
                  <ChefHat className="h-3.5 w-3.5" />
                </span>
                <span className="text-xs font-bold uppercase tracking-wider text-brand-800">
                  Đặc trưng & Dinh dưỡng món
                </span>
              </div>
              <span className="text-[11px] font-semibold text-leaf-700">
                🌱 {recipe.vegetarianTypeLabel}
              </span>
            </div>
            <div className="mt-3 flex flex-wrap gap-1.5 text-xs">
              <span className="inline-flex items-center gap-1.5 rounded-lg border border-brand-100 bg-paper px-2.5 py-1 font-medium text-ink-soft shadow-2xs">
                <Leaf className="h-3.5 w-3.5 text-leaf-600" />
                {recipe.vegetarianTypeLabel}
              </span>
              <span className="inline-flex items-center gap-1.5 rounded-lg border border-brand-100 bg-paper px-2.5 py-1 font-medium text-ink-soft shadow-2xs">
                <Clock3 className="h-3.5 w-3.5 text-brand-600" />
                {recipe.prepTimeMinutes + recipe.cookTimeMinutes <= 30 ? 'Nấu nhanh tiện lợi' : 'Chuẩn vị nhà nấu'}
              </span>
              <span className="inline-flex items-center gap-1.5 rounded-lg border border-brand-100 bg-paper px-2.5 py-1 font-medium text-ink-soft shadow-2xs">
                <Sparkles className="h-3.5 w-3.5 text-amber-500" />
                {(recipe.ingredients ?? []).length} nguyên liệu tự nhiên
              </span>
              {recipe.nutrition?.perServing?.ENERGY_KCAL && (
                <span className="inline-flex items-center gap-1.5 rounded-lg border border-brand-100 bg-paper px-2.5 py-1 font-medium text-ink-soft shadow-2xs">
                  🔥 Năng lượng lành mạnh
                </span>
              )}
            </div>
            <div className="recipe-detail-no-print mt-3 flex flex-wrap items-center gap-3 border-t border-brand-100/60 pt-2.5 text-xs text-ink-muted">
              <a href="#nguyen-lieu" className="inline-flex items-center gap-1 font-semibold text-brand-700 hover:text-brand-900">
                Nguyên liệu ({(recipe.ingredients ?? []).length}) ↓
              </a>
              <span className="text-brand-200">·</span>
              <a href="#cach-lam" className="inline-flex items-center gap-1 font-semibold text-brand-700 hover:text-brand-900">
                Các bước làm ↓
              </a>
            </div>
          </div>
        </Card>
      </div>
    </div>

    {/* Hàng 2: Chi tiết thực hiện (Nguyên liệu, Cách làm) & Bảng dinh dưỡng - Xuất phát cùng một cao độ ngang hàng */}
    <div className="mb-8 grid gap-6 lg:grid-cols-12 lg:items-start">
      {/* Cột trái (7 cột): Nguyên liệu, Cách làm, Video */}
      <div className="flex flex-col gap-6 lg:col-span-7 min-w-0">
        <Card id="nguyen-lieu" className="p-5 sm:p-7 min-w-0 overflow-hidden">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div>
              <p className="text-xs font-bold uppercase tracking-[0.14em] text-brand-700">Chuẩn bị vào bếp</p>
              <h2 className="mt-1 text-xl font-extrabold text-ink">Nguyên liệu &amp; Định lượng</h2>
            </div>
            {fromMealPlan ? (
              <MealPlanRecipeServings key={planContextKey} recipeId={recipeId} servings={servings} onChange={setServings} />
            ) : (
              <div className="recipe-detail-no-print flex items-center gap-2 rounded-xl bg-brand-50 p-1">
                <button
                  type="button"
                  aria-label="Giảm khẩu phần"
                  disabled={servings <= 1}
                  onClick={() => setServings((value) => Math.max(1, value - 1))}
                  className="rounded-lg p-2 text-brand-700 hover:bg-paper disabled:opacity-40"
                >
                  <Minus className="h-4 w-4" />
                </button>
                <span className="min-w-20 text-center text-sm font-bold text-ink">{servings} phần</span>
                <button
                  type="button"
                  aria-label="Tăng khẩu phần"
                  disabled={servings >= 50}
                  onClick={() => setServings((value) => Math.min(50, value + 1))}
                  className="rounded-lg p-2 text-brand-700 hover:bg-paper disabled:opacity-40"
                >
                  <Plus className="h-4 w-4" />
                </button>
              </div>
            )}
          </div>

          <div className="mt-3 flex flex-wrap items-center justify-between gap-3 text-xs text-ink-muted">
            <p>Định lượng được tính theo <strong>{servings} phần ăn</strong>.</p>
            {(recipe.ingredients ?? []).length > 0 && (
              <div className="recipe-detail-no-print flex items-center gap-2">
                <span className="font-medium text-ink-soft">
                  Đã chuẩn bị: <strong className="text-emerald-700">{checkedIngredients.length}/{(recipe.ingredients ?? []).length}</strong>
                </span>
                <span className="text-brand-200">·</span>
                {checkedIngredients.length < (recipe.ingredients ?? []).length ? (
                  <button
                    type="button"
                    onClick={() => setCheckedIngredients((recipe.ingredients ?? []).map((_, i) => i))}
                    className="font-semibold text-brand-700 hover:text-brand-900 underline"
                  >
                    Đánh dấu tất cả đã xong
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => setCheckedIngredients([])}
                    className="font-semibold text-ink-muted hover:text-ink underline"
                  >
                    Bỏ đánh dấu tất cả
                  </button>
                )}
              </div>
            )}
          </div>

          {(recipe.ingredients ?? []).length > 0 ? (
            <ul className="mt-4 divide-y divide-brand-100/80 rounded-xl border border-brand-100 bg-paper">
              {(recipe.ingredients ?? []).map((item, index) => {
                const isChecked = checkedIngredients.includes(index);
                const scaled = recipe.servings > 0 ? (item.quantity * servings) / recipe.servings : item.quantity;
                const unitStr = item.unitCode || item.unitName || '';
                const toggle = () => {
                  setCheckedIngredients((current) =>
                    isChecked ? current.filter((checked) => checked !== index) : [...current, index],
                  );
                };
                return (
                  <li
                    key={`${item.ingredientId ?? item.name}-${index}`}
                    onClick={toggle}
                    className={`flex items-center gap-3 py-3 px-4 cursor-pointer transition-colors ${
                      isChecked ? 'bg-brand-50/25 text-ink-muted' : 'hover:bg-brand-50/40 text-ink'
                    }`}
                  >
                    <button
                      type="button"
                      aria-label={`${isChecked ? 'Bỏ chọn' : 'Đánh dấu'} ${item.name}`}
                      aria-pressed={isChecked}
                      onClick={(e) => {
                        e.stopPropagation();
                        toggle();
                      }}
                      className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-md border transition-all ${
                        isChecked
                          ? 'border-emerald-600 bg-emerald-600 text-white shadow-2xs'
                          : 'border-brand-300 bg-paper hover:border-brand-500'
                      }`}
                    >
                      {isChecked && <Check className="h-3.5 w-3.5 stroke-[2.5]" />}
                    </button>
                    <span
                      className={`min-w-0 flex-1 font-medium text-sm transition-all ${
                        isChecked ? 'line-through text-ink-muted opacity-60' : 'text-ink'
                      }`}
                    >
                      {item.name}
                      {item.nutritionSupported === false && (
                        <span className="ml-2 inline-flex rounded-full bg-amber-100 px-2 py-0.5 text-[11px] font-semibold text-amber-900">
                          Chưa hỗ trợ tính dinh dưỡng
                        </span>
                      )}
                    </span>
                    <strong
                      className={`shrink-0 text-right font-bold text-sm whitespace-nowrap transition-all ${
                        isChecked ? 'line-through text-ink-muted opacity-60' : 'text-ink'
                      }`}
                    >
                      {formatNumber(scaled)} {unitStr}
                    </strong>
                    <div className="shrink-0">
                      {isChecked ? (
                        <span className="inline-flex items-center gap-1 rounded-full bg-emerald-100 px-2.5 py-0.5 text-xs font-semibold text-emerald-800">
                          ✓ Đã chuẩn bị
                        </span>
                      ) : (
                        <span className="inline-flex items-center rounded-full bg-amber-50 border border-amber-200/60 px-2.5 py-0.5 text-xs font-medium text-amber-800">
                          Chờ chuẩn bị
                        </span>
                      )}
                    </div>
                  </li>
                );
              })}
            </ul>
          ) : (
            <p className="mt-4 text-sm text-ink-muted">Chưa có nguyên liệu cho công thức này.</p>
          )}
        </Card>

        <Card id="cach-lam" className="p-5 sm:p-7">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.14em] text-brand-700">Làm theo thứ tự</p>
            <h2 className="mt-1 text-xl font-extrabold text-ink">Cách làm</h2>
            <p className="mt-4 whitespace-pre-line break-words leading-7 text-ink-soft">{recipe.instructions}</p>
          </div>
          <div className="mt-6 flex items-center justify-between gap-3 rounded-xl border border-brand-100 bg-brand-50/50 px-4 py-3 text-xs text-ink-muted">
            <span className="inline-flex items-center gap-2 font-medium text-ink-soft">
              <Sparkles className="h-4 w-4 shrink-0 text-brand-600" />
              Mẹo nhỏ: Chuẩn bị sẵn nguyên liệu theo định lượng ở trên giúp quá trình chế biến nhanh và giữ trọn dưỡng chất.
            </span>
          </div>
        </Card>

        {recipe.youtubeUrl ? (
          <Card className="overflow-hidden p-5 sm:p-7" data-testid="recipe-youtube-section">
            <div className="flex items-center gap-2">
              <Video className="h-5 w-5 text-brand-600" />
              <h2 className="text-xl font-extrabold text-ink">Video hướng dẫn thực hiện (YouTube)</h2>
            </div>
            {youtubeEmbedUrl ? (
              <div className="mt-4 aspect-video overflow-hidden rounded-xl bg-raised">
                <iframe
                  src={youtubeEmbedUrl}
                  title={`Video hướng dẫn ${recipe.title}`}
                  className="h-full w-full"
                  loading="lazy"
                  referrerPolicy="strict-origin-when-cross-origin"
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                  allowFullScreen
                />
              </div>
            ) : null}
            <div className="mt-4">
              <a
                href={recipe.youtubeUrl}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-2 text-sm font-bold text-brand-700 hover:text-brand-900"
              >
                Xem video hướng dẫn
                <Share2 className="h-4 w-4" />
              </a>
            </div>
          </Card>
        ) : (
          <Card className="p-5 sm:p-6 border border-brand-100/90 bg-gradient-to-br from-brand-50/40 via-paper to-brand-50/20">
            <div className="flex items-center gap-2.5">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-100/80 text-brand-700 shadow-2xs">
                <ChefHat className="h-5 w-5" />
              </div>
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.14em] text-brand-700">Gợi ý từ Mâm Xanh</p>
                <h3 className="text-base font-extrabold text-ink">Mẹo nấu chay &amp; Giữ trọn vi chất</h3>
              </div>
            </div>
            <div className="mt-4 grid gap-3 sm:grid-cols-2 text-xs leading-relaxed text-ink-soft">
              <div className="rounded-xl border border-brand-100/70 bg-paper/90 p-3 shadow-2xs">
                <p className="font-bold text-ink flex items-center gap-1.5">
                  <Sparkles className="h-3.5 w-3.5 text-leaf-600" /> Giữ trọn vitamin
                </p>
                <p className="mt-1 text-ink-muted">
                  Hấp hoặc xào lửa lớn nhanh tay thay vì luộc kỹ giúp giữ lại tối đa các vitamin tan trong nước như vitamin C và nhóm B.
                </p>
              </div>
              <div className="rounded-xl border border-brand-100/70 bg-paper/90 p-3 shadow-2xs">
                <p className="font-bold text-ink flex items-center gap-1.5">
                  <Leaf className="h-3.5 w-3.5 text-brand-600" /> Tối ưu hấp thu khoáng
                </p>
                <p className="mt-1 text-ink-muted">
                  Kết hợp rau củ giàu sắt với nguồn vitamin C (chanh, cà chua, ớt chuông) để cơ thể hấp thu sắt thực vật hiệu quả hơn.
                </p>
              </div>
            </div>
          </Card>
        )}

        {/* Cảm nhận cộng đồng: Đánh giá & Mức độ yêu thích */}
        <Card className="recipe-detail-no-print p-5 sm:p-6">
          <p className="text-xs font-bold uppercase tracking-[0.14em] text-brand-700">Cảm nhận cộng đồng</p>
          <h2 className="mt-1 text-xl font-extrabold text-ink">Đánh giá &amp; Mức độ yêu thích</h2>
          <div className="mt-4">
            <RecipeViews key={recipeId} recipe={recipe} renderCount={(count) => <StatTile icon={<Eye className="h-4 w-4 text-brand-600" />} label="Lượt xem" value={<>{formatNumber(count, 0)}<span className="sr-only"> lượt xem</span></>} />} />
          </div>
          <RecipeReactions recipeId={recipe.recipeId} authorId={author.userId} statistics={statistics} />
        </Card>
      </div>

      {/* Cột phải (5 cột): Dinh dưỡng đi cùng cao độ với Nguyên liệu */}
      <aside className="flex flex-col gap-6 lg:col-span-5 lg:sticky lg:top-24">
        <Card className="p-5 sm:p-6">
          <div>
            <div className="flex items-start justify-between gap-3">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.14em] text-leaf-700">Tham khảo</p>
                <h2 className="mt-1 text-lg font-extrabold text-ink">Dinh dưỡng mỗi khẩu phần</h2>
              </div>
              <Utensils className="h-5 w-5 text-leaf-600" />
            </div>
            <dl className="mt-4 grid grid-cols-2 gap-2 sm:gap-2.5">
              {NUTRIENTS.map((nutrient, index) => (
                <div
                  key={nutrient.key}
                  className={`rounded-xl bg-brand-50/70 p-2.5 sm:p-3 transition-colors hover:bg-brand-100/60 ${index === 0 ? 'col-span-2 flex items-center justify-between border border-brand-100 bg-brand-50' : 'flex flex-col justify-center'}`}
                >
                  <dt className="text-xs font-medium text-ink-muted">{nutrient.label}</dt>
                  <dd className={`${index === 0 ? 'text-base font-extrabold text-brand-800' : 'mt-0.5 text-sm font-extrabold text-ink'}`}>
                    {recipe.nutrition?.perServing?.[nutrient.key] == null
                      ? 'Chưa có dữ liệu'
                      : `≈ ${formatNumber(recipe.nutrition.perServing[nutrient.key])} ${nutrient.unit}`}
                  </dd>
                </div>
              ))}
            </dl>
            {!nutritionComplete && (
              <section
                role="note"
                aria-live="polite"
                className="mt-4 rounded-xl border border-amber-200/90 bg-amber-50/95 p-4 text-amber-950 shadow-2xs"
                aria-labelledby="nutrition-status-heading"
              >
                <h2 id="nutrition-status-heading" className="text-sm font-bold text-amber-950 sm:text-base">
                  Ước tính chưa đầy đủ
                </h2>
                <p role="status" className="mt-1 text-xs leading-relaxed text-amber-900/90 sm:text-sm">
                  Số liệu có thể gồm ước tính chưa xác minh. Chỉ tiêu còn thiếu không được tính bằng 0.
                </p>
                <p className="mt-2.5 text-xs font-semibold text-amber-950 sm:text-sm">
                  Nguyên liệu còn thiếu ít nhất một chỉ tiêu:
                </p>
                {(missingNutrition ?? []).length > 0 && (
                  <ul className="mt-1.5 list-inside list-disc space-y-0.5 text-xs text-amber-900 sm:text-sm">
                    {(missingNutrition ?? []).map((name) => (
                      <li key={name}>{name}</li>
                    ))}
                  </ul>
                )}
              </section>
            )}
          </div>
          <p className="mt-4 border-t border-brand-100 pt-3 text-xs leading-5 text-ink-muted">
            Nguồn và số liệu do quản trị viên nhập; có thể là USDA/NIH, nguồn khác hoặc AI estimate chưa xác minh. Các giá trị chỉ mang tính tham khảo, không thay thế tư vấn y tế.
          </p>
        </Card>

        {(recipe.nutrition?.total || recipe.nutrition?.perServing) && (
          <Card className="p-5 sm:p-6">
            <details>
              <summary className="cursor-pointer list-none text-sm font-bold text-ink">
                <span>Xem tổng dinh dưỡng toàn công thức</span>
                {servings > 0 && (
                  <span className="ml-1.5 text-xs font-semibold text-brand-700">
                    ({servings} phần)
                  </span>
                )}
              </summary>
              <dl className="mt-3 grid grid-cols-2 gap-2">
                {NUTRIENTS.map((nutrient, index) => {
                  const perServingVal = recipe.nutrition?.perServing?.[nutrient.key];
                  const totalVal = recipe.nutrition?.total?.[nutrient.key];
                  const value =
                    perServingVal != null
                      ? perServingVal * servings
                      : totalVal != null && recipe.servings > 0
                        ? (totalVal * servings) / recipe.servings
                        : totalVal;

                  return (
                    <div
                      key={nutrient.key}
                      className={`rounded-lg bg-brand-50 px-3 py-2 ${index === 0 ? 'col-span-2 flex items-center justify-between border border-brand-100' : ''}`}
                    >
                      <dt className="text-xs text-ink-muted">{nutrient.label}</dt>
                      <dd className={`${index === 0 ? 'text-sm font-extrabold text-brand-800' : 'mt-1 text-sm font-semibold text-ink'}`}>
                        {value == null
                          ? 'Chưa có dữ liệu'
                          : `≈ ${formatNumber(value)} ${nutrient.unit}`}
                      </dd>
                    </div>
                  );
                })}
              </dl>
            </details>
          </Card>
        )}
      </aside>
    </div>

    {/* Phần bình luận dạt rộng toàn trang (full-width), to rõ và bề thế như một footer section */}
    <div className="recipe-detail-no-print my-8 sm:my-10 w-full">
      <RecipeComments key={recipe.recipeId} recipeId={recipe.recipeId} className="w-full" />
    </div>

    <RelatedRecipes key={recipeId} recipeId={recipeId} />
    <div className="recipe-detail-no-print mt-8"><Link to="/kham-pha" className="inline-flex items-center gap-2 text-sm font-semibold text-brand-700"><ArrowLeft className="h-4 w-4" />Trở lại khám phá</Link></div>
  </PageContainer>;
}

function StatTile({ icon, label, value }: { icon: ReactNode; label: string; value: ReactNode }) {
  return (
    <div className="min-w-0">
      <div className="flex items-center gap-1.5 text-xs text-ink-muted">{icon}{label}</div>
      <p className="mt-1 truncate text-xl font-extrabold text-ink">{value}</p>
    </div>
  );
}

function QuickInfo({ label, value }: { label: string; value: ReactNode }) {
  return <div className="flex items-center justify-between gap-4 py-2.5 text-sm"><dt className="text-ink-muted">{label}</dt><dd className="text-right font-bold text-ink">{value}</dd></div>;
}
