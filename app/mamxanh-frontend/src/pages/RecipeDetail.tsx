import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  ArrowLeftRight,
  BadgeCheck,
  Bookmark,
  CalendarPlus,
  Check,
  ChevronRight,
  Clock,
  Eye,
  Flame,
  Flag,
  Heart,
  Leaf,
  LoaderCircle,
  Lock,
  Share2,
  ShoppingBasket,
  ThumbsUp,
  Users,
  UtensilsCrossed,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { RecipeCard } from '../components/RecipeCard';
import SpringCheck from '../components/SpringCheck';
import { Badge, Button, Card, SectionHeading } from '../components/ui';
import { HeartCheckbox } from '../components/HeartCheckbox';
import { Modal } from '../components/Modal';
import { RecipeComments } from '../components/RecipeComments';
import { RecipeRating } from '../components/RecipeRating';
import { useAuth } from '../components/AuthContext';
import { YouTubeEmbed } from '../components/YouTubeEmbed';
import { recipes } from '../data/mockData';
import { recipesApi, type RecipeDetail as RecipeDetailData } from '../api/recipes';
import { scaleQuantity } from '../utils/servings';
import { isSaved, toggleSaved, subscribeSaved } from '../lib/savedRecipes';

const reportReasons = [
  { code: 'NON_VEGAN', label: 'Không phải món chay' },
  { code: 'FOOD_SAFETY_HAZARD', label: 'Nguy cơ an toàn thực phẩm' },
  { code: 'INAPPROPRIATE_CONTENT', label: 'Nội dung phản cảm / Bạo lực' },
  { code: 'COPYRIGHT_VIOLATION', label: 'Vi phạm bản quyền / Sao chép' },
  { code: 'SPAM_ADVERTISING', label: 'Spam / Quảng cáo thương mại' },
  { code: 'OTHER', label: 'Khác' },
] as const;

const nutritionRows = [
  { label: 'Năng lượng', value: '≈ 210 kcal', rdi: '11% RDI' },
  { label: 'Đạm thực vật', value: '≈ 14.2 g', rdi: '28% RDI' },
  { label: 'Carbohydrate', value: '≈ 18.5 g', rdi: '6% RDI' },
  { label: 'Chất béo tốt', value: '≈ 6.8 g', rdi: '9% RDI' },
  { label: 'Chất xơ (Fiber)', value: '≈ 4.2 g', rdi: '16% RDI' },
  { label: 'Natri (Sodium)', value: '≈ 420 mg', rdi: '18% RDI' },
  { label: 'Sắt hữu cơ', value: '≈ 2.8 mg', rdi: '22% RDI' },
  { label: 'Canxi (Calcium)', value: '≈ 160 mg', rdi: '16% RDI' },
];

export function RecipeDetail() {
  const { slug } = useParams();
  if (slug && /^\d+$/.test(slug)) return <PublishedRecipeDetail recipeId={Number(slug)} />;
  return <MockRecipeDetail />;
}

function PublishedRecipeDetail({ recipeId }: { recipeId: number }) {
  const [recipe, setRecipe] = useState<RecipeDetailData | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  useEffect(() => {
    let active = true;
    recipesApi.getPublished(recipeId)
      .then((result) => { if (active) setRecipe(result); })
      .catch((error: unknown) => {
        if (active) setLoadError(error instanceof Error ? error.message : 'Không tải được công thức này.');
      })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [recipeId]);

  if (loading) return <PageContainer className="py-16"><p role="status" className="flex items-center justify-center gap-2 text-ink-muted"><LoaderCircle className="h-5 w-5 animate-spin" />Đang tải công thức…</p></PageContainer>;
  if (loadError || !recipe) return <PageContainer className="py-16"><Card className="mx-auto max-w-xl p-8 text-center"><h1 className="text-xl font-bold text-ink">Không tìm thấy công thức</h1><p role="alert" className="mt-2 text-sm text-ink-muted">{loadError || 'Công thức này không tồn tại hoặc chưa được công khai.'}</p><Link to="/kham-pha" className="mt-5 inline-flex text-sm font-bold text-brand-700">Quay lại khám phá</Link></Card></PageContainer>;

  const cover = recipe.media.find((item) => item.cover) ?? recipe.media[0];
  return (
    <PageContainer className="py-8">
      <Link to="/kham-pha" className="mb-5 inline-flex items-center gap-2 text-sm font-semibold text-brand-700"><ArrowLeftRight className="h-4 w-4" />Khám phá công thức</Link>
      <article className="mx-auto max-w-4xl overflow-hidden rounded-3xl border border-brand-100 bg-white shadow-sm">
        <div className="h-56 sm:h-80">
          {cover
            ? <img src={cover.blobUrl} alt={recipe.title} className="h-full w-full object-cover" />
            : <RecipeDefaultArtwork vegetarianType={recipe.vegetarianType} label={recipe.vegetarianTypeLabel} />}
        </div>
        <div className="p-5 sm:p-8">
          <div className="flex flex-wrap gap-2 text-xs font-semibold text-brand-800"><span className="rounded-full bg-brand-50 px-3 py-1">{recipe.dishCategoryLabel}</span><span className="rounded-full bg-leaf-50 px-3 py-1">{recipe.vegetarianTypeLabel}</span><span className="rounded-full bg-stone-100 px-3 py-1">Độ khó: {recipe.difficultyLabel}</span></div>
          <h1 className="mt-4 text-3xl font-extrabold text-ink">{recipe.title}</h1>
          {recipe.description && <p className="mt-3 text-base leading-7 text-ink-soft">{recipe.description}</p>}
          <div className="mt-5 flex flex-wrap gap-x-5 gap-y-2 text-sm text-ink-muted"><span>{recipe.servings} khẩu phần</span><span>Chuẩn bị {recipe.prepTimeMinutes} phút</span><span>Nấu {recipe.cookTimeMinutes} phút</span></div>
          <section className="mt-8"><h2 className="text-xl font-extrabold text-ink">Nguyên liệu</h2><ul className="mt-3 space-y-2">{recipe.ingredients.map((item, index) => <li key={`${item.ingredientId}-${item.unitId}-${index}`} className="flex justify-between gap-4 border-b border-brand-50 py-2 text-sm"><span>{item.name}</span><span className="shrink-0 font-semibold text-ink">{item.quantity} {item.unitCode}</span></li>)}</ul></section>
          {!recipe.nutritionComplete && (
            <section className="mt-6 rounded-xl border border-amber-200 bg-amber-50 p-4" aria-labelledby="nutrition-status-heading">
              <h2 id="nutrition-status-heading" className="font-bold text-amber-900">Chưa đủ dữ liệu dinh dưỡng</h2>
              <p role="status" className="mt-1 text-sm text-amber-800">Ước tính dinh dưỡng chưa đầy đủ. Chưa hỗ trợ tính dinh dưỡng cho:</p>
              <ul className="mt-2 list-inside list-disc text-sm text-amber-900">
                {recipe.ingredientsWithoutNutrition.map((name) => <li key={name}>{name}</li>)}
              </ul>
            </section>
          )}
          <section className="mt-8"><h2 className="text-xl font-extrabold text-ink">Hướng dẫn chế biến</h2><p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-ink-soft">{recipe.instructions}</p></section>
          {recipe.youtubeUrl && (
            <section className="mt-8" data-testid="recipe-youtube-section">
              <h2 className="text-xl font-extrabold text-ink">Video hướng dẫn thực hiện (YouTube)</h2>
              <div className="mt-4 max-w-2xl">
                <YouTubeEmbed urlOrId={recipe.youtubeUrl} title={`Video hướng dẫn nấu món ${recipe.title}`} />
              </div>
              <a href={recipe.youtubeUrl} target="_blank" rel="noreferrer" className="mt-4 inline-flex text-sm font-bold text-brand-700">Xem video hướng dẫn</a>
            </section>
          )}
        </div>
      </article>
    </PageContainer>
  );
}

function RecipeDefaultArtwork({ vegetarianType, label }: { vegetarianType: RecipeDetailData['vegetarianType']; label: string }) {
  const appearance = {
    VEGAN: 'from-leaf-100 to-emerald-50 text-leaf-800',
    LACTO: 'from-amber-100 to-orange-50 text-amber-900',
    OVO: 'from-yellow-100 to-lime-50 text-lime-900',
    LACTO_OVO: 'from-brand-100 to-yellow-50 text-brand-800',
  }[vegetarianType];
  return <div role="img" aria-label={`Ảnh mặc định cho món ${label}`} className={`flex h-full w-full flex-col items-center justify-center bg-gradient-to-br ${appearance}`}><Leaf className="h-14 w-14" /><span className="mt-3 text-sm font-bold">Ảnh mặc định · {label}</span></div>;
}

function MockRecipeDetail() {
  const { slug } = useParams();
  const { isAuthenticated } = useAuth();
  const matchedRecipe = recipes.find((r) => r.slug === slug);
  const isAvailable = matchedRecipe && matchedRecipe.status !== 'HIDDEN' && matchedRecipe.status !== 'DELETED';

  const [guestNoticeModal, setGuestNoticeModal] = useState<{ open: boolean; message: string }>({
    open: false,
    message: '',
  });

  const recipe = matchedRecipe ?? recipes[0];
  const [saved, setSaved] = useState(false);
  const [checked, setChecked] = useState<Record<string, boolean>>({});
  const [toast, setToast] = useState<string | null>(null);
  const [planOpen, setPlanOpen] = useState(false);
  const [desiredServings, setDesiredServings] = useState(recipe.servings);
  const [reportOpen, setReportOpen] = useState(false);
  const [reportReason, setReportReason] = useState('');
  const [reportDescription, setReportDescription] = useState('');
  const [reportError, setReportError] = useState('');
  const [demoReportSubmitted, setDemoReportSubmitted] = useState(false);

  // Sync trạng thái Lưu với localStorage
  useEffect(() => {
    if (!isAvailable) return;
    setSaved(isSaved(recipe.slug));
    const unsub = subscribeSaved(() => setSaved(isSaved(recipe.slug)));
    return unsub;
  }, [recipe.slug, isAvailable]);

  useEffect(() => {
    if (!isAvailable) return;
    setDesiredServings(recipe.servings);
    setReportOpen(false);
    setReportReason('');
    setReportDescription('');
    setReportError('');
    setDemoReportSubmitted(false);
  }, [recipe.id, recipe.servings, isAvailable]);

  if (!matchedRecipe || !isAvailable) {
    return (
      <PageContainer className="py-16">
        <Card className="mx-auto max-w-lg p-8 text-center" data-testid="recipe-not-available">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-amber-100 text-amber-600">
            <Lock className="h-7 w-7" />
          </div>
          <h1 className="text-2xl font-extrabold text-ink">Công thức không khả dụng hoặc đã bị ẩn</h1>
          <p className="mt-2 text-sm text-ink-muted">
            Bài viết bạn đang tìm không tồn tại hoặc đã bị Quản trị viên ẩn do vi phạm tiêu chuẩn cộng đồng (BR-05 / AC-01.4).
          </p>
          <div className="mt-6 flex justify-center gap-3">
            <Link
              to="/kham-pha"
              className="rounded-xl bg-brand-600 px-5 py-2.5 text-sm font-bold text-white shadow-sm hover:bg-brand-700"
            >
              Khám phá món chay khác
            </Link>
          </div>
        </Card>
      </PageContainer>
    );
  }

  const submitDemoReport = () => {
    const description = reportDescription.trim();
    if (!reportReasons.some((reason) => reason.code === reportReason)) {
      setReportError('Vui lòng chọn một lý do báo cáo.');
      return;
    }
    if (description.length > 500 || (reportReason === 'OTHER' && description.length < 10)) {
      setReportError(
        'Mô tả cần từ 10 đến 500 ký tự khi chọn lý do Khác; tối đa 500 ký tự với các lý do khác.',
      );
      return;
    }
    setReportError('');
    setDemoReportSubmitted(true);
    setReportOpen(false);
    showToast('Đã kiểm tra biểu mẫu. Chưa gửi báo cáo tới quản trị viên.');
  };

  const showToast = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 2500);
  };

  const handleToggleSave = () => {
    if (!isAuthenticated) {
      setGuestNoticeModal({
        open: true,
        message: 'Vui lòng đăng nhập để lưu công thức yêu thích (BR-05 / BR-32 / AC-01.5).',
      });
      return;
    }
    toggleSaved({
      slug: recipe.slug,
      name: recipe.name,
      image: recipe.image,
      diet: recipe.diet,
      calories: recipe.calories,
      prepTime: recipe.prepTime,
      cookTime: recipe.cookTime,
      author: recipe.author.name,
    });
    showToast(saved ? 'Đã bỏ lưu công thức' : 'Đã lưu công thức');
  };

  const handleOpenPlan = () => {
    if (!isAuthenticated) {
      setGuestNoticeModal({
        open: true,
        message: 'Vui lòng đăng nhập để thêm món vào kế hoạch tuần (BR-05 / BR-32 / AC-01.5).',
      });
      return;
    }
    setPlanOpen(true);
  };

  const related = recipes.filter((r) => r.id !== recipe.id).slice(0, 4);
  const groupedIngredients = recipe.ingredients.reduce<Record<string, typeof recipe.ingredients>>(
      (acc, ing) => {
        (acc[ing.group] ??= []).push(ing);
        return acc;
      },
      {},
  );

  return (
      <PageContainer className="py-8">
        <p className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-4 py-2 text-xs font-semibold text-amber-900">Nội dung công thức minh họa UI · chưa lấy từ Backend. Công thức thật được mở qua đường dẫn ID.</p>
        {/* breadcrumb */}
        <nav className="mb-5 flex items-center gap-1.5 text-sm text-ink-muted">
          <Link to="/" className="hover:text-brand-600">Trang chủ</Link>
          <ChevronRight className="h-3.5 w-3.5" />
          <Link to="/kham-pha" className="hover:text-brand-600">Khám phá</Link>
          <ChevronRight className="h-3.5 w-3.5" />
          <span className="font-medium text-ink-soft">{recipe.name}</span>
        </nav>

        {/* header */}
        <div className="mb-6 flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div className="max-w-3xl">
            <div className="mb-3 flex flex-wrap items-center gap-2">
              <Badge tone="leaf" soft={false}>
                <Leaf className="h-3.5 w-3.5" /> {recipe.diet}
              </Badge>
              <Badge tone="brand">{recipe.category}</Badge>
              {recipe.likePercentage !== undefined ? (
                  <span className="inline-flex items-center gap-1.5 rounded-full bg-leaf-50 px-2.5 py-1 text-xs font-bold text-leaf-700">
                <ThumbsUp className="h-3.5 w-3.5 text-leaf-600" /> {recipe.likePercentage}% hài lòng
                <span className="font-normal text-ink-muted">({recipe.likes} Like)</span>
              </span>
              ) : (
                  <span className="inline-flex items-center gap-1 rounded-full bg-brand-50 px-2.5 py-1 text-xs font-bold text-brand-700">
                Mới
              </span>
              )}
            </div>
            <h1 className="text-3xl font-extrabold leading-tight tracking-tight text-ink sm:text-4xl">
              {recipe.name}
            </h1>
            <div className="mt-4 flex items-center gap-3">
              <img
                  src={recipe.author.avatar}
                  alt=""
                  className="h-11 w-11 rounded-full object-cover ring-2 ring-brand-100"
              />
              <div>
                <p className="flex items-center gap-1 font-semibold text-ink">
                  {recipe.author.name}
                  {recipe.author.verified && <BadgeCheck className="h-4 w-4 text-brand-600" />}
                </p>
                <p className="text-xs text-ink-muted">
                  {recipe.author.bio ?? 'Thành viên cộng đồng Mâm Xanh'}
                </p>
              </div>
              <Button variant="outline" size="sm" className="ml-2">Theo dõi tác giả</Button>
            </div>
          </div>

          <div className="flex flex-wrap items-center gap-2">
            <Link
                to={`/so-sanh?left=${recipe.slug}`}
                className="flex h-11 items-center justify-center gap-2 rounded-xl border border-brand-200 bg-white px-3 text-sm font-semibold text-ink-soft transition-colors hover:border-brand-300"
                title="So sánh với công thức khác"
            >
              <ArrowLeftRight className="h-4 w-4 text-brand-600" />
              <span className="hidden sm:inline">So sánh món</span>
            </Link>
            <button
                onClick={() => setReportOpen(true)}
                aria-label="Báo cáo công thức"
                title="Báo cáo công thức"
                className="flex h-11 items-center justify-center gap-2 rounded-xl border border-brand-200 bg-white px-3 text-sm font-semibold text-ink-soft transition-colors hover:border-brand-300"
            >
              <Flag className="h-5 w-5" />
              <span className="hidden sm:inline">Báo cáo</span>
            </button>
            <button
                type="button"
                onClick={handleToggleSave}
                className={`flex h-11 w-11 items-center justify-center rounded-xl border transition-colors ${
                    saved
                        ? 'border-brand-600 bg-brand-600 text-white'
                        : 'border-brand-200 bg-white text-ink-soft hover:border-brand-300'
                }`}
                aria-label={saved ? 'Bỏ lưu' : 'Lưu'}
                title={saved ? 'Bỏ lưu' : 'Lưu'}
            >
              <Heart className={`h-5 w-5 ${saved ? 'fill-current text-white' : ''}`} />
            </button>
            <button className="flex h-11 w-11 items-center justify-center rounded-xl border border-brand-200 bg-white text-ink-soft transition-colors hover:border-brand-300">
              <Share2 className="h-5 w-5" />
            </button>
            <Button onClick={handleOpenPlan}>
              <CalendarPlus className="h-4 w-4" /> Thêm vào kế hoạch
            </Button>
          </div>
        </div>

        {/* stat row */}
        <div className="mb-8 grid grid-cols-2 gap-3 sm:grid-cols-5">
          {[
            { icon: Clock, label: 'Chuẩn bị', value: `≈ ${recipe.prepTime} phút` },
            { icon: Flame, label: 'Nấu ăn', value: `≈ ${recipe.cookTime} phút` },
            { icon: Users, label: 'Khẩu phần gốc', value: `≈ ${recipe.servings} người ăn` },
            { icon: UtensilsCrossed, label: 'Năng lượng', value: `≈ ${recipe.calories} kcal` },
            {
              icon: Eye,
              label: 'Lượt xem',
              value: `${recipe.viewCount.toLocaleString('vi-VN')}`,
            },
          ].map((s) => (
              <div
                  key={s.label}
                  className="flex items-center gap-3 rounded-2xl border border-brand-100 bg-white p-4"
              >
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-brand-100 text-brand-600">
              <s.icon className="h-5 w-5" />
            </span>
                <div>
                  <p className="text-xs text-ink-muted">{s.label}</p>
                  <p className="font-bold text-ink">{s.value}</p>
                </div>
              </div>
          ))}
        </div>

        {/* image + description */}
        <div className="mb-8 grid gap-6 lg:grid-cols-[1.2fr_1fr]">
          <div className="overflow-hidden rounded-2xl border border-brand-100">
            <img src={recipe.image} alt={recipe.name} className="h-full w-full object-cover" />
          </div>
          <Card className="flex flex-col p-6">
            <p className="mb-2 text-xs font-bold uppercase tracking-wider text-brand-600">
              Câu chuyện món ăn
            </p>
            <h2 className="mb-3 text-xl font-extrabold text-ink">
              Hương vị đất trời trong từng miếng đậu mềm mượt
            </h2>
            <p className="flex-1 leading-relaxed text-ink-soft">{recipe.description}</p>
            <div className="mt-5 flex flex-wrap gap-2">
              {recipe.tags.map((t) => (
                  <span key={t} className="text-sm font-medium text-brand-600">{t}</span>
              ))}
            </div>
            <div className="mt-5 flex gap-2 border-t border-brand-50 pt-4">
              <Button variant="secondary" className="flex-1" onClick={handleToggleSave}>
                <Bookmark className="h-4 w-4" /> {saved ? 'Đã lưu' : 'Lưu lại'}
              </Button>
              <Button variant="outline" className="flex-1">
                <Share2 className="h-4 w-4" /> Chia sẻ
              </Button>
            </div>
          </Card>
        </div>

        {/* ingredients */}
        <Card className="mb-8 p-6">
          <div className="mb-5 flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="flex items-center gap-2 text-xl font-extrabold text-ink">
                <span className="text-brand-600">🥬</span> Nguyên liệu
              </h2>
              <p className="text-sm text-ink-muted">
                Công thức gốc: {recipe.servings} phần · Chọn số phần để tính lại nguyên liệu.
              </p>
              <p className="text-xs text-ink-muted">
                Định lượng được nhân theo tỷ lệ, làm tròn tối đa 2 chữ số thập phân; đơn vị được giữ nguyên.
              </p>
            </div>
            <label className="flex items-center gap-2 text-sm font-semibold text-ink">
              Số khẩu phần
              <input
                  type="number"
                  min={1}
                  max={50}
                  step={1}
                  value={desiredServings}
                  onChange={(event) => {
                    const value = Number(event.target.value);
                    if (Number.isInteger(value) && value >= 1 && value <= 50) setDesiredServings(value);
                  }}
                  className="w-20 rounded-xl border border-brand-200 bg-white px-3 py-2 text-center outline-none focus:border-brand-500"
              />
            </label>
            <Button onClick={() => showToast('Đã thêm nguyên liệu vào Danh sách đi chợ!')}>
              <ShoppingBasket className="h-4 w-4" /> Thêm tất cả vào Danh sách đi chợ
            </Button>
          </div>
          <div className="grid gap-x-8 gap-y-2 sm:grid-cols-2">
            {Object.entries(groupedIngredients).map(([group, items]) => (
                <div key={group} className="sm:contents">
                  {items.map((ing) => (
                      <div
                          key={ing.id}
                          className="flex items-center justify-between gap-3 rounded-xl border border-brand-50 px-3 py-2 transition-colors hover:bg-brand-50/60"
                      >
                        <SpringCheck
                          checked={!!checked[ing.id]}
                          onChange={() => setChecked((c) => ({ ...c, [ing.id]: !c[ing.id] }))}
                          label={
                            <span className="text-sm font-medium">
                              {ing.name}
                              {ing.note && <span className="ml-1 text-xs font-normal text-ink-muted">· {ing.note}</span>}
                            </span>
                          }
                          strike="left"
                          boxSize={22}
                          boxRadius={6}
                          fontSize={14}
                          color="#ea580c"
                          fillColor="#ea580c"
                          checkColor="#ffffff"
                          textColor="#2d2b28"
                          className="flex-1 min-w-0"
                        />
                        <Badge tone="neutral">
                          {scaleQuantity(ing.quantity, recipe.servings, desiredServings)}
                        </Badge>
                      </div>
                  ))}
                </div>
            ))}
          </div>
        </Card>

        <Modal open={reportOpen} onClose={() => setReportOpen(false)} title="Báo cáo công thức">
          <p className="mb-3 text-sm text-ink-muted">
            Chọn một lý do để báo cáo “{recipe.name}”. Báo cáo chỉ là phản ánh để quản trị viên xem xét.
          </p>
          <div className="max-h-48 space-y-2 overflow-y-auto">
            {reportReasons.map((reason) => (
                <label
                    key={reason.code}
                    className="flex cursor-pointer items-center gap-2 text-sm text-ink-soft"
                >
                  <input
                      type="radio"
                      name="report-reason"
                      value={reason.code}
                      checked={reportReason === reason.code}
                      onChange={() => {
                        setReportReason(reason.code);
                        setReportError('');
                      }}
                  />
                  {reason.label}
                </label>
            ))}
          </div>
          <label htmlFor="report-description" className="mt-4 block text-sm font-semibold text-ink">
            Mô tả bổ sung {reportReason === 'OTHER' ? '(bắt buộc)' : '(tùy chọn)'}
          </label>
          <textarea
              id="report-description"
              maxLength={500}
              value={reportDescription}
              onChange={(event) => {
                setReportDescription(event.target.value);
                setReportError('');
              }}
              rows={3}
              className="mt-2 w-full rounded-xl border border-brand-200 p-3 text-sm outline-none focus:border-brand-500"
          />
          <p className="text-right text-xs text-ink-muted">{reportDescription.length}/500 ký tự</p>
          {reportError && <p role="alert" className="mt-2 text-sm text-red-600">{reportError}</p>}
          <p className="mt-3 rounded-lg bg-amber-50 p-3 text-xs text-ink-soft">
            Biểu mẫu đang ở chế độ xem trước, hệ thống chưa gửi báo cáo tới quản trị viên.
          </p>
          <div className="mt-4 flex justify-end gap-2">
            <Button variant="outline" onClick={() => setReportOpen(false)}>Hủy</Button>
            <Button onClick={submitDemoReport} disabled={demoReportSubmitted}>
              {demoReportSubmitted ? 'Đã kiểm tra' : 'Kiểm tra biểu mẫu'}
            </Button>
          </div>
        </Modal>

        {/* YouTube Video Section */}
        {recipe.youtubeUrl && (
          <Card className="mb-8 p-6" data-testid="recipe-youtube-section">
            <div className="mb-4 flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-xl font-extrabold text-ink">
                <span className="text-red-600">▶</span> Video hướng dẫn thực hiện (YouTube)
              </h2>
              <span className="text-xs text-ink-muted">Video hướng dẫn từ YouTube</span>
            </div>
            <div className="mx-auto max-w-3xl">
              <YouTubeEmbed
                urlOrId={recipe.youtubeUrl}
                title={`Video hướng dẫn nấu món ${recipe.name}`}
              />
            </div>
          </Card>
        )}

        {/* steps */}
        <Card className="mb-8 p-6">
          <h2 className="mb-5 flex items-center gap-2 text-xl font-extrabold text-ink">
            <span className="text-brand-600">👩‍🍳</span> Các bước thực hiện
          </h2>
          <ol className="space-y-4">
            {recipe.steps.map((step, i) => (
                <li key={i} className="flex gap-4">
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-600 text-sm font-bold text-white">
                {i + 1}
              </span>
                  <p className="pt-1 leading-relaxed text-ink-soft">{step}</p>
                </li>
            ))}
          </ol>
        </Card>

        {/* nutrition */}
        <Card className="mb-10 p-6">
          <div className="mb-5 flex flex-wrap items-center justify-between gap-2">
            <h2 className="flex items-center gap-2 text-xl font-extrabold text-ink">
              <span className="text-brand-600">📊</span> Dinh dưỡng cho 1 khẩu phần (8 chỉ tiêu minh họa)
            </h2>
            <Badge tone="neutral">Dữ liệu mẫu</Badge>
          </div>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            {nutritionRows.map((row) => (
                <div key={row.label} className="rounded-xl border border-brand-50 bg-brand-50/40 p-4">
                  <p className="text-xs text-ink-muted">{row.label}</p>
                  <p className="mt-0.5 text-lg font-extrabold text-ink">{row.value}</p>
                  <p className="text-xs font-semibold text-brand-600">{row.rdi}</p>
                </div>
            ))}
          </div>
          <p className="mt-4 text-xs leading-relaxed text-ink-muted">
            Số liệu đang là mẫu giao diện cho một khẩu phần, chưa được tính từ nguyên liệu của công thức này.
            Thay đổi số khẩu phần ở trên chỉ tính lại định lượng nguyên liệu; không nhân bảng dinh dưỡng mỗi khẩu phần.
          </p>
        </Card>

        <RecipeRating key={`rating-${recipe.id}`} recipe={recipe} />
        <RecipeComments key={recipe.id} />
        <SectionHeading eyebrow="Có thể bạn thích" title="Công thức tương tự" />
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {related.map((r) => (
              <RecipeCard key={r.id} recipe={r} />
          ))}
        </div>

        {/* add to plan modal */}
        <Modal open={planOpen} onClose={() => setPlanOpen(false)} title="Thêm vào kế hoạch bữa ăn">
          <p className="mb-4 text-sm text-ink-muted">
            Chọn ngày và buổi để thêm <strong className="text-ink">{recipe.name}</strong> vào thực đơn tuần của bạn.
          </p>
          <div className="mb-4 grid grid-cols-2 gap-3">
            <select className="rounded-xl border border-brand-200 bg-white px-3 py-2.5 text-sm outline-none focus:border-brand-400">
              {['Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy', 'Chủ Nhật'].map((d) => (
                  <option key={d}>{d}</option>
              ))}
            </select>
            <select className="rounded-xl border border-brand-200 bg-white px-3 py-2.5 text-sm outline-none focus:border-brand-400">
              {['Bữa sáng', 'Bữa trưa', 'Bữa tối'].map((d) => (
                  <option key={d}>{d}</option>
              ))}
            </select>
          </div>
          <div className="flex justify-end gap-2">
            <Button variant="outline" onClick={() => setPlanOpen(false)}>Huỷ</Button>
            <Button
                onClick={() => {
                  setPlanOpen(false);
                  showToast('Đã thêm món vào kế hoạch tuần!');
                }}
            >
              Thêm vào kế hoạch
            </Button>
          </div>
        </Modal>

        {toast && (
            <div className="fixed bottom-6 left-1/2 z-50 flex -translate-x-1/2 items-center gap-2 rounded-full bg-ink px-5 py-3 text-sm font-semibold text-white shadow-xl">
          <span className="flex h-5 w-5 items-center justify-center rounded-full bg-leaf-500">
            <Check className="h-3.5 w-3.5" />
          </span>
              {toast}
            </div>
        )}

        {/* guest notice modal - AC-01.5 / BR-05 / BR-32 */}
        <Modal
          open={guestNoticeModal.open}
          onClose={() => setGuestNoticeModal({ open: false, message: '' })}
          title="Yêu cầu đăng nhập"
        >
          <div className="space-y-4">
            <div className="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
              <Lock className="mt-0.5 h-5 w-5 shrink-0 text-amber-600" />
              <p>{guestNoticeModal.message}</p>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <Button variant="outline" onClick={() => setGuestNoticeModal({ open: false, message: '' })}>
                Để sau
              </Button>
              <Link
                to="/dang-nhap"
                className="inline-flex items-center justify-center rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-bold text-white shadow-sm hover:bg-brand-700"
              >
                Đăng nhập ngay
              </Link>
            </div>
          </div>
        </Modal>
      </PageContainer>
  );
}
