import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { AlertCircle, Check, Info, LoaderCircle, Plus, Sparkles, Trash2 } from 'lucide-react';
import { recipesApi, type Choice, type CreateRecipeRequest, type IngredientOption, type RecipeFormOptions } from '../api/recipes';
import { ApiError, asApiError } from '../lib/apiClient';
import { PageContainer } from '../components/Layout';
import { useAuth } from '../components/AuthContext';
import { YouTubeEmbed } from '../components/YouTubeEmbed';
import { validateYouTubeUrl } from '../utils/youtube';
import { Button, Card } from '../components/ui';
import type { UserRole } from '../types';
import { RecipeImageUploader, type ImageItem } from '../components/RecipeImageUploader';
import { RecipeAiAssistance } from '../components/RecipeAiAssistance';
import type { RecipeAiContentType } from '../api/recipes';
import { AiRecipeAssistantModal, type GeneratedRecipeData } from '../components/AiRecipeAssistantModal';

type IngredientRow = {
  key: string;
  ingredientId: number | null;
  ingredientName: string;
  search: string;
  quantity: string;
  unitId: number | null;
};

let ingredientRowSequence = 0;

const emptyIngredient = (): IngredientRow => ({
  key: `ingredient-${Date.now()}-${++ingredientRowSequence}`,
  ingredientId: null,
  ingredientName: '',
  search: '',
  quantity: '',
  unitId: null,
});

export function CreateRecipe() {
  const { isAuthenticated: active, account } = useAuth();
  const role = account?.role ?? 'CUSTOMER';
  const [initialAllowed] = useState(() => active && role === 'EXPERT');

  if (!initialAllowed) {
    // AC-04.5: a Guest goes straight to the login page.
    if (!active) return <Navigate to="/dang-nhap" replace />;
    return <RecipeCreationAccessGate role={role} />;
  }

  return <CreateRecipeForm />;
}

function RecipeCreationAccessGate({ role }: { role: UserRole }) {
  const isCustomer = role === 'CUSTOMER';

  return (
    <PageContainer className="py-12">
      <Card className="mx-auto max-w-xl p-8 text-center">
        <h1 className="text-2xl font-extrabold text-ink">Đăng công thức chỉ dành cho Chuyên gia</h1>
        <p className="mt-3 text-sm text-ink-muted">
          {isCustomer
            ? 'Bạn cần được phê duyệt đơn đăng ký Chuyên gia trước khi đăng công thức.'
            : 'Vai trò hiện tại không có quyền đăng công thức.'}
        </p>
        {isCustomer && (
          <Link to="/dang-ky-chuyen-gia" className="mt-5 inline-flex rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-bold text-on-brand hover:bg-brand-700">
            Đăng ký trở thành Chuyên gia
          </Link>
        )}
      </Card>
    </PageContainer>
  );
}

function CreateRecipeForm() {
  const [options, setOptions] = useState<RecipeFormOptions | null>(null);
  const [optionsError, setOptionsError] = useState('');
  const [loadingOptions, setLoadingOptions] = useState(true);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [instructions, setInstructions] = useState('');
  const [dishCategory, setDishCategory] = useState('');
  const [vegetarianType, setVegetarianType] = useState('');
  const [difficulty, setDifficulty] = useState('');
  const [servings, setServings] = useState('');
  const [prepTimeMinutes, setPrepTimeMinutes] = useState('');
  const [cookTimeMinutes, setCookTimeMinutes] = useState('');
  const [youtubeUrl, setYoutubeUrl] = useState('');
  const [youtubeError, setYoutubeError] = useState('');
  const [youtubeVideoId, setYoutubeVideoId] = useState<string | null>(null);

  const handleYoutubeChange = (val: string) => {
    setYoutubeUrl(val);
    const result = validateYouTubeUrl(val);
    if (!result.valid) {
      setYoutubeError(result.error ?? 'Đường dẫn YouTube không hợp lệ.');
      setYoutubeVideoId(null);
    } else {
      setYoutubeError('');
      setYoutubeVideoId(result.videoId);
    }
  };
  const [ingredients, setIngredients] = useState<IngredientRow[]>(() => [emptyIngredient()]);
  const [mediaList, setMediaList] = useState<ImageItem[]>([]);
  const [mediaError, setMediaError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitError, setSubmitError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [aiLoading, setAiLoading] = useState<RecipeAiContentType | null>(null);
  const [aiError, setAiError] = useState<{ contentType: RecipeAiContentType; message: string } | null>(null);
  const [aiModalOpen, setAiModalOpen] = useState(false);
  const [aiNotice, setAiNotice] = useState<string | null>(null);
  const navigate = useNavigate();

  const handleApplyAiRecipe = async (aiRecipe: GeneratedRecipeData) => {
    setTitle(aiRecipe.title);
    setDescription(aiRecipe.description);
    setInstructions(aiRecipe.instructions);
    if (aiRecipe.dishCategory) setDishCategory(aiRecipe.dishCategory);
    if (aiRecipe.vegetarianType) setVegetarianType(aiRecipe.vegetarianType);
    if (aiRecipe.difficulty) setDifficulty(aiRecipe.difficulty);
    setServings(String(aiRecipe.servings));
    setPrepTimeMinutes(String(aiRecipe.prepTimeMinutes));
    setCookTimeMinutes(String(aiRecipe.cookTimeMinutes));

    if (aiRecipe.ingredients && aiRecipe.ingredients.length > 0) {
      const mappedRows: IngredientRow[] = await Promise.all(
        aiRecipe.ingredients.map(async (item, idx) => {
          const matchedUnit = options?.units.find((u) => {
            const uCode = u.code.toLowerCase();
            const uName = u.name.toLowerCase();
            const itemCode = item.unitCode.toLowerCase();
            const itemName = item.unitName.toLowerCase();
            return (
              uCode === itemCode ||
              uName === itemName ||
              uCode.includes(itemCode) ||
              itemCode.includes(uCode) ||
              (itemCode === 'qua' && (uCode === 'quả' || uCode === 'qua' || uCode === 'trái')) ||
              (itemCode === 'g' && (uCode === 'g' || uCode === 'gram')) ||
              (itemCode === 'ml' && (uCode === 'ml' || uCode === 'mililit'))
            );
          });

          let foundIngredientId: number | null = null;
          let foundName = item.name;
          try {
            const searchResults = await recipesApi.searchIngredients(item.name);
            if (searchResults && searchResults.length > 0) {
              const exact = searchResults.find(
                (r) => r.name.toLowerCase() === item.name.toLowerCase(),
              );
              if (exact) {
                foundIngredientId = exact.ingredientId;
                foundName = exact.name;
              } else {
                foundIngredientId = searchResults[0].ingredientId;
                foundName = searchResults[0].name;
              }
            }
          } catch {
            // bỏ qua lỗi nếu search catalog tạm thời không khả dụng
          }

          return {
            key: `ingredient-${Date.now()}-${idx}`,
            ingredientId: foundIngredientId,
            ingredientName: foundName,
            search: foundName,
            quantity: String(item.quantity),
            unitId: matchedUnit?.unitId ?? (options?.units[0]?.unitId ?? null),
          };
        }),
      );
      setIngredients(mappedRows);
    }

    setAiNotice(
      `Đã nạp thành công thông tin món "${aiRecipe.title}" kèm định lượng nguyên liệu vào biểu mẫu!`,
    );
  };

  useEffect(() => {
    let active = true;
    recipesApi.getFormOptions()
      .then((result) => { if (active) setOptions(result); })
      .catch((error: unknown) => {
        if (active) setOptionsError(error instanceof Error ? error.message : 'Không tải được danh mục công thức.');
      })
      .finally(() => { if (active) setLoadingOptions(false); });
    return () => { active = false; };
  }, []);

  const updateIngredient = (key: string, patch: Partial<IngredientRow>) => {
    setIngredients((current) => current.map((row) => row.key === key ? { ...row, ...patch } : row));
  };

  const generateWithAi = async (contentType: RecipeAiContentType) => {
    setAiError(null);
    if (title.trim().length < 3 || !dishCategory || !vegetarianType || Number(servings) < 1
        || ingredients.some((row) => row.ingredientId === null || row.unitId === null
          || !row.quantity || Number(row.quantity) <= 0)) {
      setAiError({ contentType, message: 'Hãy nhập tên món, loại chay, danh mục, khẩu phần và hoàn tất nguyên liệu trước khi yêu cầu AI.' });
      return;
    }
    setAiLoading(contentType);
    try {
      const result = await recipesApi.suggestText({
        contentType,
        title,
        dishCategory,
        vegetarianType,
        servings: Number(servings),
        ingredients: ingredients.map((row) => ({
          ingredientId: row.ingredientId!,
          unitId: row.unitId!,
          quantity: Number(row.quantity),
        })),
      });
      if (contentType === 'DESCRIPTION') setDescription(result.content);
      else setInstructions(result.content);
    } catch (cause) {
      const error = asApiError(cause);
      setAiError({ contentType, message: error.code === 'AI_SUBSCRIPTION_REQUIRED'
        ? 'Tính năng AI dành cho gói Plus/Pro. Bạn vẫn có thể tự viết và đăng công thức.'
        : error.code === 'AI_PROVIDER_UNAVAILABLE' || error.code === 'AI_RESPONSE_INVALID'
          ? 'AI hiện không tạo được nội dung hợp lệ. Bạn vẫn có thể tự viết và đăng công thức.'
          : error.message });
    } finally {
      setAiLoading(null);
    }
  };

  const validateMediaSelection = () => {
    if (mediaList.length > 0) {
      if (mediaList.some((image) => image.url.startsWith('blob:'))) {
        setMediaError('Một số ảnh chưa tải lên thành công. Vui lòng xóa ảnh xem trước và tải lại trước khi xuất bản.');
        return false;
      }
      const coverCount = mediaList.filter((m) => m.isCover).length;
      if (coverCount !== 1) {
        setMediaError('Vui lòng chọn đúng 1 ảnh làm ảnh bìa.');
        return false;
      }
    }
    setMediaError('');
    return true;
  };

  const handlePublish = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitError('');
    setFieldErrors({});
    if (!validateMediaSelection()) return;
    if (!options) {
      setSubmitError('Danh mục công thức chưa tải được. Hãy thử tải lại trang.');
      return;
    }

    const quantityErrors: Record<string, string> = {};
    ingredients.forEach((row, index) => {
      const error = quantityError(row.quantity);
      if (error) quantityErrors[`ingredients[${index}].quantity`] = error;
    });
    if (Object.keys(quantityErrors).length > 0) {
      setFieldErrors(quantityErrors);
      return;
    }

    if (youtubeUrl.trim()) {
      const youtubeCheck = validateYouTubeUrl(youtubeUrl);
      if (!youtubeCheck.valid) {
        setYoutubeError(youtubeCheck.error ?? 'Đường dẫn YouTube không hợp lệ.');
      }
    }

    const payload: CreateRecipeRequest = {
      title,
      description,
      instructions,
      dishCategory,
      vegetarianType,
      difficulty,
      servings: Number(servings),
      prepTimeMinutes: Number(prepTimeMinutes),
      cookTimeMinutes: Number(cookTimeMinutes),
      youtubeUrl,
      ingredients: ingredients.map((row) => ({
        ingredientId: row.ingredientId ?? 0,
        unitId: row.unitId ?? 0,
        quantity: Number(row.quantity),
      })),
      media: mediaList.map((m) => ({
        blobUrl: m.url,
        mimeType: m.mimeType,
        cover: m.isCover,
      })),
    };

    setSubmitting(true);
    try {
      const created = await recipesApi.publish(payload);
      navigate(`/cong-thuc/${created.recipeId}`);
    } catch (error) {
      if (error instanceof ApiError && error.errors.length > 0) {
        setFieldErrors(Object.fromEntries(error.errors.map((item) => [item.field, item.message])));
      }
      if (error instanceof ApiError && error.status === 401) {
        setSubmitError('Phiên đăng nhập không còn hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại bằng tài khoản Chuyên gia.');
      } else if (error instanceof ApiError && error.status === 403) {
        setSubmitError('Chỉ Chuyên gia đang hoạt động mới được đăng công thức.');
      } else {
        setSubmitError(error instanceof Error ? error.message : 'Không đăng được công thức. Hãy thử lại.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const errorFor = (field: string) => fieldErrors[field];

  return (
    <PageContainer className="py-8">
      <nav className="mb-4 text-sm text-ink-muted" aria-label="Breadcrumb">
        <span>Trang chủ</span> / <span className="font-medium text-brand-600">Đăng công thức</span>
      </nav>

      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">Đăng công thức món chay mới</h1>
          <p className="mt-1 text-sm text-ink-muted">Chia sẻ bí quyết nấu ăn thuần lành và định lượng chính xác để mọi người cùng thực hiện.</p>
        </div>
        <Button
          type="button"
          onClick={() => setAiModalOpen(true)}
          className="inline-flex items-center gap-2 bg-gradient-to-r from-amber-500 to-orange-600 hover:from-amber-600 hover:to-orange-700 text-white shadow-md font-bold px-4 py-2.5 rounded-xl shrink-0"
        >
          <Sparkles className="h-4 w-4" /> Soạn với Trợ lý AI
        </Button>
      </div>

      {aiNotice && (
        <div role="status" className="mb-5 flex items-start gap-3 rounded-xl border border-green-200 bg-green-50 p-4 text-sm text-green-900 shadow-sm animate-fade-in">
          <Check className="mt-0.5 h-5 w-5 shrink-0 text-green-600" />
          <div className="flex-1">
            <p className="font-semibold">{aiNotice}</p>
          </div>
          <button type="button" onClick={() => setAiNotice(null)} className="text-green-700 hover:text-green-900 text-xs font-bold">
            Đóng
          </button>
        </div>
      )}

      <div className="mb-6 flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
        <Info className="mt-0.5 h-5 w-5 shrink-0" />
        <p><strong>Dành cho Chuyên gia ẩm thực:</strong> Vui lòng kiểm tra kỹ định lượng nguyên liệu và các bước hướng dẫn trước khi xuất bản công thức.</p>
      </div>

      {loadingOptions && <div role="status" className="mb-5 flex items-center gap-2 text-sm text-ink-muted"><LoaderCircle className="h-4 w-4 animate-spin" /> Đang tải danh mục…</div>}
      {optionsError && <div role="alert" className="mb-5 flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"><AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />{optionsError}</div>}

      <form noValidate onSubmit={handlePublish} className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_300px]">
        <div className="space-y-5">
          <Card className="space-y-4 p-5 sm:p-6">
            <SectionHead number="1" title="Thông tin món ăn" />
            <Field label="Tên món *" error={errorFor('title')}>
              <input aria-label="Tên món *" value={title} onChange={(event) => setTitle(event.target.value)} maxLength={120} placeholder="VD: Đậu hũ kho cà chua" className={inputClass} />
            </Field>
            <Field label="Mô tả (không bắt buộc, tối đa 2.000 ký tự)" error={errorFor('description')}>
              <textarea aria-label="Mô tả" value={description} onChange={(event) => setDescription(event.target.value)} maxLength={2000} rows={3} placeholder="Giới thiệu ngắn về món ăn" className={`${inputClass} resize-y`} />
              <RecipeAiAssistance contentType="DESCRIPTION" loading={aiLoading} error={aiError?.contentType === 'DESCRIPTION' ? aiError.message : ''} onGenerate={generateWithAi} />
            </Field>
            <div className="grid gap-4 sm:grid-cols-2">
              <Field label="Thể loại món *" error={errorFor('dishCategory')}>
                <ChoiceSelect value={dishCategory} options={options?.dishCategories ?? []} onChange={setDishCategory} placeholder="Chọn thể loại" label="Thể loại món" />
              </Field>
              <Field label="Loại ăn chay *" error={errorFor('vegetarianType')}>
                <ChoiceSelect value={vegetarianType} options={options?.vegetarianTypes ?? []} onChange={setVegetarianType} placeholder="Chọn loại ăn chay" label="Loại ăn chay" />
              </Field>
              <Field label="Độ khó *" error={errorFor('difficulty')}>
                <ChoiceSelect value={difficulty} options={options?.difficulties ?? []} onChange={setDifficulty} placeholder="Chọn độ khó" label="Độ khó" />
              </Field>
              <Field label="Khẩu phần *" error={errorFor('servings')}>
                <NumberInput value={servings} onChange={setServings} min={1} max={50} suffix="người" label="Khẩu phần" />
              </Field>
              <Field label="Thời gian chuẩn bị *" error={errorFor('prepTimeMinutes')}>
                <NumberInput value={prepTimeMinutes} onChange={setPrepTimeMinutes} min={0} max={1440} suffix="phút" label="Thời gian chuẩn bị" />
              </Field>
              <Field label="Thời gian nấu *" error={errorFor('cookTimeMinutes')}>
                <NumberInput value={cookTimeMinutes} onChange={setCookTimeMinutes} min={0} max={1440} suffix="phút" label="Thời gian nấu" />
              </Field>
            </div>
          </Card>

          <Card className="space-y-4 p-5 sm:p-6">
            <SectionHead number="2" title="Nguyên liệu và định lượng" />
            <p className="text-sm text-ink-muted">Chọn nguyên liệu trong danh mục và nhập số lượng lớn hơn 0. Đơn vị khác g/kg cần có tỷ lệ quy đổi sẵn.</p>
            {ingredients.map((row, index) => (
              <div key={row.key} className="rounded-xl border border-brand-100 p-3">
                <div className="mb-2 flex items-center justify-between">
                  <span className="text-xs font-bold uppercase tracking-wide text-brand-700">Nguyên liệu {index + 1}</span>
                  {ingredients.length > 1 && <button type="button" onClick={() => setIngredients((current) => current.filter((item) => item.key !== row.key))} className="rounded-lg p-2 text-ink-muted hover:bg-red-50 hover:text-red-700" aria-label={`Xóa nguyên liệu ${index + 1}`}><Trash2 className="h-4 w-4" /></button>}
                </div>
                <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_120px_180px]">
                  <IngredientPicker
                    value={row.search || row.ingredientName}
                    selectedId={row.ingredientId}
                    onTextChange={(search) => updateIngredient(row.key, { search, ingredientId: null, ingredientName: '' })}
                    onSelect={(ingredient) => updateIngredient(row.key, { ingredientId: ingredient.ingredientId, ingredientName: ingredient.name, search: ingredient.name })}
                    error={errorFor(`ingredients[${index}].ingredientId`)}
                    rowNumber={index + 1}
                  />
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-ink-soft" htmlFor={`quantity-${row.key}`}>Số lượng *</label>
                    <input id={`quantity-${row.key}`} type="number" min="0.01" step={['g', 'kg'].includes(options?.units.find((unit) => unit.unitId === row.unitId)?.code ?? '') ? 'any' : '0.01'} value={row.quantity} onChange={(event) => updateIngredient(row.key, { quantity: event.target.value })} placeholder={options?.units.find((unit) => unit.unitId === row.unitId)?.code === 'g' ? '100' : '0'} className={inputClass} aria-label={`Số lượng nguyên liệu ${index + 1}`} />
                    {errorFor(`ingredients[${index}].quantity`) && <p className={errorClass}>{errorFor(`ingredients[${index}].quantity`)}</p>}
                  </div>
                  <div>
                    <label className="mb-1 block text-xs font-semibold text-ink-soft" htmlFor={`unit-${row.key}`}>Đơn vị *</label>
                    <select id={`unit-${row.key}`} value={row.unitId ?? ''} onChange={(event) => updateIngredient(row.key, { unitId: event.target.value ? Number(event.target.value) : null })} className={inputClass} aria-label={`Đơn vị nguyên liệu ${index + 1}`}>
                      <option value="">Chọn đơn vị</option>
                      {(options?.units ?? []).map((unit) => <option key={unit.unitId} value={unit.unitId}>{unit.name} ({unit.code})</option>)}
                    </select>
                    {errorFor(`ingredients[${index}].unitId`) && <p className={errorClass}>{errorFor(`ingredients[${index}].unitId`)}</p>}
                  </div>
                </div>
              </div>
            ))}
            {errorFor('ingredients') && <p className={errorClass}>{errorFor('ingredients')}</p>}
            <Button type="button" variant="outline" size="sm" onClick={() => setIngredients((current) => [...current, emptyIngredient()])} disabled={ingredients.length >= 50}>
              <Plus className="h-4 w-4" /> Thêm nguyên liệu
            </Button>
          </Card>

          <Card className="space-y-4 p-5 sm:p-6">
            <SectionHead number="3" title="Hướng dẫn chế biến" />
            <Field label="Hướng dẫn * (10–5.000 ký tự)" error={errorFor('instructions')}>
              <textarea aria-label="Hướng dẫn * (10–5.000 ký tự)" value={instructions} onChange={(event) => setInstructions(event.target.value)} rows={8} placeholder="Viết hướng dẫn theo cách của bạn, có thể chia theo từng bước hoặc cách nấu chi tiết." className={`${inputClass} resize-y`} />
              <p className="mt-1 text-right text-xs text-ink-muted">{instructions.trim().length}/5.000 ký tự</p>
              <RecipeAiAssistance contentType="INSTRUCTIONS" loading={aiLoading} error={aiError?.contentType === 'INSTRUCTIONS' ? aiError.message : ''} onGenerate={generateWithAi} />
            </Field>
          </Card>

          <Card className="space-y-4 p-5 sm:p-6">
            <SectionHead number="4" title="Hình ảnh bài công thức" />
            <RecipeImageUploader
              images={mediaList}
              onChange={setMediaList}
              maxImages={5}
            />
            {mediaError && <p role="alert" className={errorClass}>{mediaError}</p>}
            {errorFor('media') && <p role="alert" className={errorClass}>{errorFor('media')}</p>}
            <Field label="Link YouTube / Liên kết video YouTube (không bắt buộc)">
              <input
                type="url"
                aria-label="Link YouTube / Liên kết video YouTube"
                value={youtubeUrl}
                onChange={(event) => handleYoutubeChange(event.target.value)}
                maxLength={2048}
                placeholder="https://www.youtube.com/watch?v=... hoặc https://youtu.be/..."
                className={inputClass}
              />
              <p className="mt-1 text-xs text-ink-muted">Hỗ trợ video từ YouTube, tối đa 1 video cho mỗi bài công thức.</p>
              {errorFor('youtubeUrl') ? (
                <p role="alert" className={errorClass}>
                  {errorFor('youtubeUrl')}
                </p>
              ) : youtubeError ? (
                <p role="alert" className={errorClass}>
                  {youtubeError}
                </p>
              ) : null}
              {youtubeVideoId && !youtubeError && (
                <div className="mt-3 space-y-2 rounded-xl border border-brand-100 bg-brand-50/50 p-3">
                  <p className="text-xs font-semibold text-brand-700">
                    Đã trích xuất YouTube Video ID: <span className="font-mono">{youtubeVideoId}</span>
                  </p>
                  <div className="max-w-md">
                    <YouTubeEmbed urlOrId={youtubeVideoId} title="Xem trước video YouTube" />
                  </div>
                </div>
              )}
            </Field>
          </Card>

          {submitError && <div role="alert" className="flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"><AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />{submitError}</div>}
          <div className="flex justify-end">
            <Button type="submit" size="lg" disabled={submitting || loadingOptions || Boolean(optionsError) || !options}>
              {submitting ? <><LoaderCircle className="h-4 w-4 animate-spin" /> Đang gửi…</> : 'Xuất bản công thức'}
            </Button>
          </div>
        </div>

        <aside className="space-y-4">
          <Card className="p-5">
            <h2 className="font-bold text-ink">Điều kiện đăng</h2>
            <ul className="mt-3 space-y-2 text-sm text-ink-soft">
              <li>• Tên món: 3–120 ký tự</li>
              <li>• 1–50 nguyên liệu, số lượng lớn hơn 0</li>
              <li>• Mọi đơn vị cần quy đổi phải có dữ liệu sẵn</li>
              <li>• Hướng dẫn: 10–5.000 ký tự</li>
              <li>• Chọn độ khó, khẩu phần và thời gian hợp lệ</li>
            </ul>
          </Card>
          <Card className="flex items-start gap-3 p-5 text-sm text-ink-soft">
            <Info className="mt-0.5 h-4 w-4 shrink-0 text-brand-600" />
            <p>Số lượng cần lớn hơn 0, tối đa hai chữ số thập phân. Các đơn vị thông dụng như quả, củ, gam, ml sẽ giúp người nấu dễ dàng thực hiện theo.</p>
          </Card>
        </aside>
      </form>

      <AiRecipeAssistantModal
        open={aiModalOpen}
        onClose={() => setAiModalOpen(false)}
        options={options}
        onApplyRecipe={handleApplyAiRecipe}
      />
    </PageContainer>
  );
}

function IngredientPicker({ value, selectedId, onTextChange, onSelect, error, rowNumber }: {
  value: string;
  selectedId: number | null;
  onTextChange: (value: string) => void;
  onSelect: (item: IngredientOption) => void;
  error?: string;
  rowNumber: number;
}) {
  const [suggestions, setSuggestions] = useState<IngredientOption[]>([]);
  const [open, setOpen] = useState(false);
  const [searchError, setSearchError] = useState('');

  useEffect(() => {
    if (!open) return;
    let active = true;
    const timer = window.setTimeout(() => {
      recipesApi.searchIngredients(value).then((items) => {
        if (active) { setSuggestions(items); setSearchError(''); }
      }).catch((reason: unknown) => {
        if (active) setSearchError(reason instanceof Error ? reason.message : 'Không tải được nguyên liệu.');
      });
    }, 180);
    return () => { active = false; window.clearTimeout(timer); };
  }, [open, value]);

  return (
    <div className="relative">
      <label className="mb-1 block text-xs font-semibold text-ink-soft" htmlFor={`ingredient-${rowNumber}`}>Nguyên liệu *</label>
      <input id={`ingredient-${rowNumber}`} value={value} onFocus={() => setOpen(true)} onChange={(event) => onTextChange(event.target.value)} onBlur={() => window.setTimeout(() => setOpen(false), 120)} placeholder="Tìm nguyên liệu có sẵn" autoComplete="off" className={inputClass} aria-label={`Chọn nguyên liệu ${rowNumber}`} aria-expanded={open} />
      {open && <div className="absolute z-20 mt-1 max-h-56 w-full overflow-auto rounded-xl border border-brand-200 bg-paper p-1 shadow-lg">
        {searchError && <p className="px-3 py-2 text-xs text-red-700">{searchError}</p>}
        {!searchError && suggestions.length === 0 && <p className="px-3 py-2 text-xs text-ink-muted">Nguyên liệu này hiện chưa được hỗ trợ.</p>}
        {suggestions.map((item) => <button key={item.ingredientId} type="button" onMouseDown={(event) => event.preventDefault()} onClick={() => { onSelect(item); setOpen(false); }} className={`block w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-brand-50 ${selectedId === item.ingredientId ? 'bg-brand-50 font-semibold' : ''}`}>{item.name}</button>)}
      </div>}
      {error && <p className={errorClass}>{error}</p>}
    </div>
  );
}

function ChoiceSelect({ value, options, onChange, placeholder, label }: { value: string; options: Choice[]; onChange: (value: string) => void; placeholder: string; label: string }) {
  return <select value={value} onChange={(event) => onChange(event.target.value)} className={inputClass} aria-label={label}>
    <option value="">{placeholder}</option>
    {options.map((option) => <option key={option.code} value={option.code}>{option.label}</option>)}
  </select>;
}

function NumberInput({ value, onChange, min, max, suffix, label }: { value: string; onChange: (value: string) => void; min: number; max: number; suffix: string; label: string }) {
  return <div className="flex items-center gap-2">
    <input type="number" value={value} onChange={(event) => onChange(event.target.value)} min={min} max={max} step={1} className={inputClass} aria-label={label} />
    <span className="shrink-0 text-xs text-ink-muted">{suffix}</span>
  </div>;
}

function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return <div><p className="mb-1.5 text-sm font-semibold text-ink-soft">{label}</p>{children}{error && <p className={errorClass}>{error}</p>}</div>;
}

function SectionHead({ number, title }: { number: string; title: string }) {
  return <div className="flex items-center gap-3"><span className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-600 text-sm font-bold text-on-brand">{number}</span><h2 className="text-lg font-extrabold text-ink">{title}</h2></div>;
}

const inputClass = 'w-full rounded-xl border border-brand-200 bg-paper px-3 py-2.5 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100';
const errorClass = 'mt-1 text-xs font-medium text-red-700';

function quantityError(rawQuantity: string): string | null {
  const quantity = rawQuantity.trim();
  if (!/^\d+(?:\.\d{1,2})?$/.test(quantity) || Number(quantity) <= 0) {
    return 'Định lượng phải là số dương, tối đa hai chữ số thập phân.';
  }
  return null;
}
