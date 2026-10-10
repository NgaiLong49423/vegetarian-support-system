import { useEffect, useState, type FormEvent, type ReactNode } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { AlertCircle, ImagePlus, Info, LoaderCircle, Plus, Trash2, UploadCloud } from 'lucide-react';
import { recipesApi, type Choice, type CreateRecipeRequest, type IngredientOption, type RecipeFormOptions } from '../api/recipes';
import { ApiError } from '../lib/apiClient';
import { PageContainer } from '../components/Layout';
import { useAuth } from '../components/AuthContext';
import { YouTubeEmbed } from '../components/YouTubeEmbed';
import { validateYouTubeUrl } from '../utils/youtube';
import { Button, Card } from '../components/ui';
import type { UserRole } from '../types';
import { RecipeImageUploader, type ImageItem } from '../components/RecipeImageUploader';

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
          <Link to="/dang-ky-chuyen-gia" className="mt-5 inline-flex rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-bold text-white hover:bg-brand-700">
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
  const [files, setFiles] = useState<File[]>([]);
  const [coverIndex, setCoverIndex] = useState<number | null>(null);
  const [mediaList, setMediaList] = useState<ImageItem[]>([]);
  const [mediaError, setMediaError] = useState('');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitError, setSubmitError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const navigate = useNavigate();

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

  const handleFiles = (selected: FileList | null) => {
    setFiles(selected ? Array.from(selected) : []);
    setCoverIndex(null);
    setMediaError('');
  };

  const validateMediaSelection = () => {
    if (files.length > 5) {
      setMediaError('Mỗi công thức được chọn tối đa 5 ảnh.');
      return false;
    }
    if (files.length > 0 && (coverIndex === null || coverIndex >= files.length)) {
      setMediaError('Nếu chọn ảnh, hãy chọn đúng 1 ảnh bìa.');
      return false;
    }
    if (files.length > 0) {
      setMediaError('Tính năng tải lên ảnh đang được hoàn thiện. Hiện hãy đăng bài không kèm ảnh.');
      return false;
    }
    if (mediaList.length > 0) {
      const coverCount = mediaList.filter((m) => m.isCover).length;
      if (coverCount !== 1) {
        setMediaError('Vui lòng chỉ định chính xác 1 ảnh đại diện (ảnh bìa) cho bài viết (FR-14, BR-19).');
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

      <div className="mb-6">
        <h1 className="text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">Đăng công thức món chay mới</h1>
        <p className="mt-1 text-sm text-ink-muted">Chia sẻ bí quyết nấu ăn thuần lành và định lượng chính xác để mọi người cùng thực hiện.</p>
      </div>

      <div className="mb-6 flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">
        <Info className="mt-0.5 h-5 w-5 shrink-0" />
        <p><strong>Dành cho Chuyên gia ẩm thực:</strong> Vui lòng kiểm tra kỹ định lượng nguyên liệu và các bước hướng dẫn trước khi xuất bản công thức.</p>
      </div>

      {loadingOptions && <div role="status" className="mb-5 flex items-center gap-2 text-sm text-ink-muted"><LoaderCircle className="h-4 w-4 animate-spin" /> Đang tải danh mục…</div>}
      {optionsError && <div role="alert" className="mb-5 flex items-start gap-2 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"><AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />{optionsError}</div>}

      <form onSubmit={handlePublish} className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_300px]">
        <div className="space-y-5">
          <Card className="space-y-4 p-5 sm:p-6">
            <SectionHead number="1" title="Thông tin món ăn" />
            <Field label="Tên món *" error={errorFor('title')}>
              <input aria-label="Tên món *" value={title} onChange={(event) => setTitle(event.target.value)} maxLength={120} placeholder="VD: Đậu hũ kho cà chua" className={inputClass} />
            </Field>
            <Field label="Mô tả (không bắt buộc, tối đa 2.000 ký tự)" error={errorFor('description')}>
              <textarea aria-label="Mô tả" value={description} onChange={(event) => setDescription(event.target.value)} maxLength={2000} rows={3} placeholder="Giới thiệu ngắn về món ăn" className={`${inputClass} resize-y`} />
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
            </Field>
          </Card>

          <Card className="space-y-4 p-5 sm:p-6">
            <SectionHead number="4" title="Hình ảnh bài công thức (Tối đa 5 ảnh, đúng 1 ảnh bìa - FR-14)" />
            <RecipeImageUploader
              images={mediaList}
              onChange={setMediaList}
              maxImages={5}
            />

            <div className="rounded-xl border border-dashed border-brand-200 bg-brand-50/50 p-4">
              <div className="flex items-start gap-3">
                <UploadCloud className="mt-0.5 h-5 w-5 shrink-0 text-brand-600" />
                <div className="min-w-0 flex-1">
                  <p className="font-semibold text-ink">Ảnh công thức (0–5 ảnh)</p>
                  <p className="mt-1 text-xs text-ink-muted">Tối đa 5 ảnh minh họa cho món ăn, hãy chọn 1 ảnh làm ảnh đại diện bìa.</p>
                  <input type="file" accept="image/*" multiple onChange={(event) => handleFiles(event.target.files)} className="mt-3 block w-full text-sm file:mr-3 file:rounded-lg file:border-0 file:bg-brand-100 file:px-3 file:py-2 file:font-semibold file:text-brand-700" aria-label="Chọn tối đa 5 ảnh" />
                </div>
                {files.length === 0 && <ImagePlus className="h-6 w-6 text-brand-400" />}
              </div>
              {files.length > 0 && <ul className="mt-3 space-y-2 border-t border-brand-100 pt-3">{files.map((file, index) => (
                <li key={`${file.name}-${index}`} className="flex items-center gap-2 text-sm">
                  <input type="radio" name="cover-image" checked={coverIndex === index} onChange={() => { setCoverIndex(index); setMediaError(''); }} aria-label={`Chọn ${file.name} làm ảnh cover`} />
                  <span className="min-w-0 flex-1 truncate">{file.name}</span>
                  <span className="text-xs text-ink-muted">{coverIndex === index ? 'Cover' : ''}</span>
                </li>
              ))}</ul>}
              {mediaError && <p role="alert" className={errorClass}>{mediaError}</p>}
              {errorFor('media') && <p role="alert" className={errorClass}>{errorFor('media')}</p>}
            </div>
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
      {open && <div className="absolute z-20 mt-1 max-h-56 w-full overflow-auto rounded-xl border border-brand-200 bg-white p-1 shadow-lg">
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
  return <div className="flex items-center gap-3"><span className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-600 text-sm font-bold text-white">{number}</span><h2 className="text-lg font-extrabold text-ink">{title}</h2></div>;
}

const inputClass = 'w-full rounded-xl border border-brand-200 bg-white px-3 py-2.5 text-sm text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100';
const errorClass = 'mt-1 text-xs font-medium text-red-700';

function quantityError(rawQuantity: string): string | null {
  const quantity = rawQuantity.trim();
  if (!/^\d+(?:\.\d{1,2})?$/.test(quantity) || Number(quantity) <= 0) {
    return 'Định lượng phải là số dương, tối đa hai chữ số thập phân.';
  }
  return null;
}
