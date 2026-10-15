import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import {
  ArrowLeft,
  Check,
  ChevronDown,
  Clock3,
  Download,
  Eye,
  Heart,
  MessageCircle,
  Minus,
  Plus,
  Printer,
  Share2,
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
  const ingredients = recipe.ingredients
    .map((item) => `- ${item.name}: ${formatNumber(item.quantity)} ${item.unitName}`)
    .join('\n');
  const sections = [
    recipe.title,
    `Tác giả: ${recipe.author.displayName}`,
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
  const { recipeId: rawId } = useParams();
  return <PublishedApiRecipeDetail key={rawId} recipeId={Number(rawId)} />;
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
        setSelectedMedia(Math.max(0, data.media.findIndex((media) => media.cover)));
      })
      .catch((cause: unknown) => {
        if (alive) {
          const apiError = asApiError(cause);
          setError(apiError.status === 404
            ? 'Công thức không còn công khai hoặc đã bị xóa.'
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

  if (error) return <PageContainer className="py-12"><Card className="mx-auto max-w-xl p-6"><h1 className="text-xl font-extrabold text-ink">Không thể mở công thức</h1><p className="mt-2 text-sm text-ink-muted">{error}</p><Link to="/kham-pha" className="mt-5 inline-flex items-center gap-2 text-sm font-semibold text-brand-700"><ArrowLeft className="h-4 w-4" />Khám phá công thức</Link></Card></PageContainer>;
  if (!recipe) return <PageContainer className="py-16 text-center text-ink-muted" aria-live="polite">Đang tải công thức…</PageContainer>;

  const nutritionComplete = recipe.nutrition?.complete ?? recipe.nutritionComplete;
  const missingNutrition = recipe.nutrition?.ingredientsMissingData ?? recipe.ingredientsWithoutNutrition;
  const printRecipe = () => window.print();

  return <PageContainer className="recipe-detail-page py-6 sm:py-8">
    <div className="recipe-detail-no-print mb-5 flex flex-wrap items-center justify-between gap-3">
      <Link to={backTarget} className="inline-flex items-center gap-2 text-sm font-semibold text-brand-700 hover:text-brand-900"><ArrowLeft className="h-4 w-4" />{fromMealPlan ? 'Về lịch ăn' : 'Khám phá'}</Link>
      <div className="flex flex-wrap items-center gap-2">
        <SaveRecipeButton recipeId={recipe.recipeId} />
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

    <div className="mb-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_300px]">
      <div className="min-w-0">
        <div className="mb-4 flex flex-wrap gap-2">
          <Badge tone="leaf">{recipe.vegetarianTypeLabel}</Badge>
          <Badge>{recipe.dishCategoryLabel}</Badge>
          <Badge tone="neutral">{recipe.difficultyLabel}</Badge>
        </div>
        <h1 className="max-w-4xl text-3xl font-extrabold tracking-tight text-ink sm:text-4xl">{recipe.title}</h1>
        {recipe.description && <p className="mt-3 max-w-3xl leading-7 text-ink-muted">{recipe.description}</p>}
        <div className="mt-5 flex flex-wrap items-center gap-x-5 gap-y-3 text-sm text-ink-muted">
          <div className="flex items-center gap-3">
            <MemberAvatar name={recipe.author.displayName} url={recipe.author.avatarUrl} />
            <div><p className="text-xs">Tác giả</p><Link to={`/thanh-vien/${recipe.author.userId}`} className="font-bold text-ink hover:text-brand-700">{recipe.author.displayName}</Link></div>
          </div>
          <span className="inline-flex items-center gap-1.5"><Clock3 className="h-4 w-4 text-brand-600" />{recipe.prepTimeMinutes + recipe.cookTimeMinutes} phút tổng cộng</span>
          <span className="inline-flex items-center gap-1.5"><Users className="h-4 w-4 text-brand-600" />{recipe.servings} khẩu phần gốc</span>
          {recipe.publishedAt && <time dateTime={recipe.publishedAt}>Đăng ngày {publishedDate(recipe.publishedAt)}</time>}
        </div>
      </div>
      <Card className="grid grid-cols-2 gap-3 p-4 sm:grid-cols-4 lg:grid-cols-2">
        <RecipeViews key={recipeId} recipe={recipe} renderCount={(count) => <StatTile icon={<Eye className="h-4 w-4" />} label="Lượt xem" value={<>{formatNumber(count, 0)}<span className="sr-only"> lượt xem</span></>} />} />
        <StatTile icon={<Heart className="h-4 w-4" />} label="Lượt thích" value={formatNumber(recipe.statistics.likes, 0)} />
        <StatTile icon={<Utensils className="h-4 w-4" />} label="Lượt không thích" value={formatNumber(recipe.statistics.dislikes, 0)} />
        <StatTile icon={<Heart className="h-4 w-4" />} label="Tỷ lệ thích" value={recipe.statistics.likePercentage === null ? 'Mới' : `${formatNumber(recipe.statistics.likePercentage)}%`} />
        <p className="col-span-2 text-xs text-ink-muted sm:col-span-4 lg:col-span-2">{formatNumber(recipe.statistics.reactionCount, 0)} lượt bình chọn</p>
      </Card>
    </div>

    <div className="mb-8 grid gap-6 lg:grid-cols-[minmax(0,1.7fr)_minmax(280px,0.8fr)]">
      <section aria-label="Thư viện ảnh công thức">
        <Card className="overflow-hidden">
          <div className="aspect-[16/9] bg-brand-50">
            {activeMedia ? <img src={activeMedia.blobUrl} alt={recipe.title} className="h-full w-full object-cover" fetchPriority="high" /> : <div className="flex h-full items-center justify-center text-sm text-ink-muted">Công thức chưa có ảnh.</div>}
          </div>
          {media.length > 1 && <div className="recipe-detail-no-print grid grid-cols-4 gap-2 p-3 sm:gap-3 sm:p-4">
            {media.map((item, index) => <button key={`${item.displayOrder}-${item.blobUrl}`} type="button" onClick={() => setSelectedMedia(index)} aria-label={`Xem ảnh ${index + 1}`} aria-pressed={index === selectedMedia} className={`overflow-hidden rounded-xl border-2 bg-brand-50 ${index === selectedMedia ? 'border-brand-600' : 'border-transparent'}`}><img src={item.blobUrl} alt="" loading="lazy" className="aspect-[4/3] h-full w-full object-cover" /></button>)}
          </div>}
        </Card>
      </section>
      <Card className="p-5 sm:p-6">
        <h2 className="text-lg font-extrabold text-ink">Thông tin nhanh</h2>
        <dl className="mt-4 divide-y divide-brand-100">
          <QuickInfo label="Thời gian chuẩn bị" value={`${recipe.prepTimeMinutes} phút`} />
          <QuickInfo label="Thời gian nấu" value={`${recipe.cookTimeMinutes} phút`} />
          <QuickInfo label="Khẩu phần gốc" value={`${recipe.servings} người`} />
          <QuickInfo label="Độ khó" value={recipe.difficultyLabel} />
        </dl>
      </Card>
    </div>

    <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1.2fr)_minmax(0,0.8fr)]">
      <div className="space-y-6">
        <Card className="p-5 sm:p-7">
          <div className="flex flex-wrap items-center justify-between gap-4">
            <div><p className="text-xs font-bold uppercase tracking-[0.14em] text-brand-700">Chuẩn bị vào bếp</p><h2 className="mt-1 text-xl font-extrabold text-ink">Nguyên liệu</h2></div>
            {fromMealPlan ? <MealPlanRecipeServings key={planContextKey} recipeId={recipeId} servings={servings} onChange={setServings} /> : <div className="recipe-detail-no-print flex items-center gap-2 rounded-xl bg-brand-50 p-1">
              <button type="button" aria-label="Giảm khẩu phần" disabled={servings <= 1} onClick={() => setServings((value) => Math.max(1, value - 1))} className="rounded-lg p-2 text-brand-700 hover:bg-paper disabled:opacity-40"><Minus className="h-4 w-4" /></button>
              <span className="min-w-20 text-center text-sm font-bold text-ink">{servings} phần</span>
              <button type="button" aria-label="Tăng khẩu phần" disabled={servings >= 50} onClick={() => setServings((value) => Math.min(50, value + 1))} className="rounded-lg p-2 text-brand-700 hover:bg-paper disabled:opacity-40"><Plus className="h-4 w-4" /></button>
            </div>}
          </div>
          <p className="mt-2 text-sm text-ink-muted">Định lượng được tính theo {servings} phần ăn.</p>
          <ul className="mt-4 divide-y divide-brand-100">
            {recipe.ingredients.map((item, index) => {
              const isChecked = checkedIngredients.includes(index);
              const scaled = recipe.servings > 0 ? item.quantity * servings / recipe.servings : item.quantity;
              return <li key={`${item.ingredientId ?? item.name}-${index}`} className="flex items-center gap-3 py-3">
                <button type="button" aria-label={`${isChecked ? 'Bỏ chọn' : 'Đánh dấu'} ${item.name}`} aria-pressed={isChecked} onClick={() => setCheckedIngredients((current) => isChecked ? current.filter((checked) => checked !== index) : [...current, index])} className={`flex h-5 w-5 shrink-0 items-center justify-center rounded border ${isChecked ? 'border-leaf-600 bg-leaf-600 text-white' : 'border-brand-300 bg-paper'}`}>{isChecked && <Check className="h-3.5 w-3.5" />}</button>
                <span className={`min-w-0 flex-1 text-sm ${isChecked ? 'text-ink-muted line-through' : 'text-ink-soft'}`}>{item.name}</span>
                <strong className="shrink-0 text-right text-sm text-ink">{formatNumber(scaled)} {item.unitName}</strong>
              </li>;
            })}
          </ul>
          {recipe.ingredients.length === 0 && <p className="mt-4 text-sm text-ink-muted">Chưa có nguyên liệu cho công thức này.</p>}
        </Card>

        <Card className="p-5 sm:p-7">
          <p className="text-xs font-bold uppercase tracking-[0.14em] text-brand-700">Làm theo thứ tự</p>
          <h2 className="mt-1 text-xl font-extrabold text-ink">Cách làm</h2>
          <p className="mt-4 whitespace-pre-line break-words leading-7 text-ink-soft">{recipe.instructions}</p>
        </Card>

        {recipe.youtubeUrl && <Card className="overflow-hidden">
          <div className="flex items-center gap-2 px-5 pt-5 sm:px-7"><Video className="h-5 w-5 text-brand-600" /><h2 className="text-lg font-extrabold text-ink">Video hướng dẫn</h2></div>
          {youtubeEmbedUrl ? <div className="mt-4 aspect-video bg-raised"><iframe src={youtubeEmbedUrl} title={`Video hướng dẫn ${recipe.title}`} className="h-full w-full" loading="lazy" referrerPolicy="strict-origin-when-cross-origin" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowFullScreen /></div> : <a href={recipe.youtubeUrl} target="_blank" rel="noreferrer" className="m-5 inline-flex items-center gap-2 text-sm font-semibold text-brand-700">Mở video trên YouTube<Share2 className="h-4 w-4" /></a>}
        </Card>}
      </div>

      <aside className="space-y-6">
        <Card className="p-5 sm:p-6">
          <div className="flex items-start justify-between gap-3"><div><p className="text-xs font-bold uppercase tracking-[0.14em] text-leaf-700">Tham khảo</p><h2 className="mt-1 text-lg font-extrabold text-ink">Dinh dưỡng mỗi khẩu phần</h2></div><Utensils className="h-5 w-5 text-leaf-600" /></div>
          <dl className="mt-4 grid grid-cols-1 gap-2 sm:grid-cols-2 lg:grid-cols-1 xl:grid-cols-2">
            {NUTRIENTS.map((nutrient) => <div key={nutrient.key} className="rounded-xl bg-brand-50 px-3 py-2.5"><dt className="text-xs text-ink-muted">{nutrient.label}</dt><dd className="mt-1 text-sm font-bold text-ink">{formatNumber(recipe.nutrition?.perServing?.[nutrient.key])} {recipe.nutrition?.perServing?.[nutrient.key] == null ? '' : nutrient.unit}</dd></div>)}
          </dl>
          {!nutritionComplete && <div role="note" aria-live="polite" className="mt-4 rounded-xl border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900"><p className="font-bold">Ước tính chưa đầy đủ</p><p className="mt-1">Các giá trị chỉ gồm phần tính được từ danh mục. Nguyên liệu thiếu dữ liệu không được tính bằng 0.</p>{missingNutrition.length > 0 && <><p className="mt-2 text-xs font-semibold">Nguyên liệu chưa hỗ trợ tính dinh dưỡng:</p><ul className="mt-1 list-inside list-disc text-xs">{missingNutrition.map((name) => <li key={name}>{name}</li>)}</ul></>}</div>}
          <p className="mt-3 text-xs leading-5 text-ink-muted">Nguồn tham khảo: danh mục dinh dưỡng nội bộ theo USDA/NIH. Số liệu chỉ mang tính ước tính, không thay thế tư vấn y tế.</p>
        </Card>
        {recipe.nutrition?.total && <Card className="p-5 sm:p-6"><details><summary className="cursor-pointer list-none text-sm font-bold text-ink">Xem tổng dinh dưỡng toàn công thức</summary><dl className="mt-3 grid grid-cols-2 gap-2">{NUTRIENTS.map((nutrient) => { const value = recipe.nutrition.total[nutrient.key]; return <div key={nutrient.key} className="rounded-lg bg-brand-50 px-3 py-2"><dt className="text-xs text-ink-muted">{nutrient.label}</dt><dd className="mt-1 text-sm font-semibold text-ink">{formatNumber(value)}{value == null ? '' : ` ${nutrient.unit}`}</dd></div>; })}</dl></details></Card>}
      </aside>
    </div>
    <RelatedRecipes key={recipeId} recipeId={recipeId} />
    <div className="recipe-detail-no-print mt-8"><Link to="/kham-pha" className="inline-flex items-center gap-2 text-sm font-semibold text-brand-700"><ArrowLeft className="h-4 w-4" />Trở lại khám phá</Link></div>
  </PageContainer>;
}

function StatTile({ icon, label, value }: { icon: ReactNode; label: string; value: ReactNode }) {
  return <div className="min-w-0"><div className="flex items-center gap-1.5 text-xs text-ink-muted">{icon}{label}</div><p className="mt-1 truncate text-lg font-extrabold text-ink">{value}</p></div>;
}

function QuickInfo({ label, value }: { label: string; value: string }) {
  return <div className="flex items-center justify-between gap-4 py-3 text-sm"><dt className="text-ink-muted">{label}</dt><dd className="text-right font-bold text-ink">{value}</dd></div>;
}
