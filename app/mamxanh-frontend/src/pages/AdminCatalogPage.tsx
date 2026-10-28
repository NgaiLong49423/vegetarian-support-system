import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import {
  Activity,
  AlertCircle,
  Award,
  Check,
  CirclePlus,
  Database,
  ExternalLink,
  Filter,
  Layers,
  LoaderCircle,
  Pencil,
  RefreshCw,
  Search,
  ShieldAlert,
  ShieldCheck,
  Sparkles,
  X,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, Card, EmptyState } from '../components/ui';
import { IngredientImage } from '../components/IngredientImage';
import { getIngredientGroupVisual } from '../utils/ingredientImage';
import {
  normalizeIngredientGroup,
  normalizeSourceName,
  STANDARD_INGREDIENT_GROUPS,
  STANDARD_DATA_SOURCES,
} from '../utils/vietnameseText';
import {
  AdminCatalogApiError,
  adminCatalogApi,
  type CatalogIngredient,
  type IngredientRecipeReference,
  type IngredientSearch,
  type IngredientNutrition,
} from '../services/adminCatalogApi';

// Today in Vietnam time, the calendar the Backend checks referenceDate against, whatever the browser zone (BUG-059).
const today = () => new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Asia/Ho_Chi_Minh', year: 'numeric', month: '2-digit', day: '2-digit',
}).format(new Date());
const MAX_INGREDIENT_IMAGE_BYTES = 2 * 1024 * 1024;
const INGREDIENT_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);
const fieldClass =
  'mt-1.5 w-full rounded-xl border border-brand-200/80 bg-paper px-3.5 py-2.5 text-sm font-sans text-ink outline-none transition-all placeholder:text-ink-muted/50 focus:border-brand-500 focus:ring-2 focus:ring-brand-100';
const labelClass = 'block text-sm font-semibold text-ink';
const nutritionFields = [
  { key: 'energyKcalPer100g', label: 'Năng lượng', unit: 'kcal', step: '0.01' },
  { key: 'proteinGramsPer100g', label: 'Chất đạm', unit: 'g', step: '0.01' },
  { key: 'carbohydrateGramsPer100g', label: 'Carbohydrate', unit: 'g', step: '0.01' },
  { key: 'totalFatGramsPer100g', label: 'Chất béo', unit: 'g', step: '0.01' },
  { key: 'fiberGramsPer100g', label: 'Chất xơ', unit: 'g', step: '0.01' },
  { key: 'calciumMgPer100g', label: 'Canxi', unit: 'mg', step: '0.01' },
  { key: 'ironMgPer100g', label: 'Sắt', unit: 'mg', step: '0.01' },
  { key: 'vitaminB12MicrogramsPer100g', label: 'Vitamin B12', unit: 'µg', step: '0.0001' },
  { key: 'zincMgPer100g', label: 'Kẽm', unit: 'mg', step: '0.01' },
] as const satisfies ReadonlyArray<{ key: keyof IngredientNutrition; label: string; unit: string; step: string }>;
type NutritionDraft = Record<keyof IngredientNutrition, string>;

const emptyNutritionDraft = (): NutritionDraft => Object.fromEntries(
  nutritionFields.map(({ key }) => [key, '']),
) as NutritionDraft;

const toNutritionDraft = (nutrition: IngredientNutrition): NutritionDraft => Object.fromEntries(
  nutritionFields.map(({ key }) => [key, nutrition[key] === null ? '' : String(nutrition[key])]),
) as NutritionDraft;

const recipeStatusLabel = (status: string) => status === 'PUBLISHED'
  ? 'Đang công khai'
  : status === 'HIDDEN' ? 'Đã ẩn' : 'Đã xóa';

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className={labelClass}>
      {label}
      {children}
    </label>
  );
}

interface IngredientCardProps {
  item: CatalogIngredient;
  saving: boolean;
  onOpenNutritionDetails: (item: CatalogIngredient) => void;
  onShowRecipes: (item: CatalogIngredient) => void;
  onEditInfo: (item: CatalogIngredient) => void;
  onToggleActive: (item: CatalogIngredient) => void;
}

function IngredientTableRow({
  item,
  saving,
  onOpenNutritionDetails,
  onShowRecipes,
  onEditInfo,
  onToggleActive,
}: IngredientCardProps) {
  const groupVisual = getIngredientGroupVisual(item.ingredientGroup);

  return (
    <tr
      key={item.id}
      className={`transition-colors hover:bg-brand-50/40 ${
        item.active ? 'bg-paper' : 'bg-brand-50/20 text-ink-muted'
      }`}
    >
      {/* 1. Nguyên liệu (Ảnh thumbnail + Tên + Nguồn tham khảo) */}
      <td className="py-3 px-4 align-middle">
        <div className="flex items-center gap-3">
          <IngredientImage
            imageUrl={item.imageUrl}
            name={item.name}
            containerClassName="h-10 w-10 shrink-0 overflow-hidden rounded-xl border border-brand-200/80 bg-brand-50 shadow-2xs"
          />
          <div className="min-w-0">
            <p className="font-bold text-ink leading-tight line-clamp-1" title={item.name}>
              {item.name}
            </p>
            <div className="flex items-center gap-1 text-[11px] text-ink-muted">
              <span>{normalizeSourceName(item.sourceName)}</span>
              {item.sourceUrl && (
                <a
                  href={item.sourceUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-brand-600 hover:underline"
                  title="Mở liên kết nguồn"
                >
                  <ExternalLink className="h-3 w-3 inline" />
                </a>
              )}
            </div>
          </div>
        </div>
      </td>

      {/* 2. Nhóm nguyên liệu */}
      <td className="py-3 px-2 align-middle whitespace-nowrap">
        <span className={`inline-block rounded-lg border px-2.5 py-0.5 text-xs font-semibold ${groupVisual.badgeClass}`}>
          {normalizeIngredientGroup(item.ingredientGroup)}
        </span>
      </td>

      {/* 3. Trạng thái (Đang hoạt động / Ngừng hoạt động) */}
      <td className="py-3 px-3 text-center align-middle whitespace-nowrap">
        <span
          className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-0.5 text-xs font-semibold ${
            item.active
              ? 'border-leaf-300/80 bg-leaf-50 text-leaf-800'
              : 'border-brand-200/80 bg-paper text-ink-muted'
          }`}
        >
          <span className={`size-1.5 rounded-full ${item.active ? 'bg-leaf-500' : 'bg-ink-muted/50'}`} />
          {item.active ? 'Đang hoạt động' : 'Ngừng hoạt động'}
        </span>
      </td>

      {/* 4. Trạng thái hỗ trợ tính dinh dưỡng */}
      <td className="py-3 px-3 text-center align-middle whitespace-nowrap">
        <span
          className={`inline-flex items-center rounded-lg border px-2.5 py-0.5 text-xs font-semibold ${
            item.nutritionSupported
              ? 'border-leaf-300/80 bg-leaf-50/95 text-leaf-800'
              : 'border-brand-200/80 bg-paper/95 text-ink-muted'
          }`}
        >
          {item.nutritionSupported ? 'Hỗ trợ dinh dưỡng' : 'Chưa hỗ trợ'}
        </span>
      </td>

      {/* 5. Số công thức có sử dụng nguyên liệu */}
      <td className="py-3 px-2 text-center align-middle whitespace-nowrap">
        <button
          type="button"
          onClick={() => onShowRecipes(item)}
          aria-label={`Xem công thức có dùng ${item.name}`}
          className={`inline-flex items-center justify-center min-w-8 h-7 px-2.5 rounded-full text-xs font-bold transition-all shadow-2xs hover:scale-105 ${
            item.recipeUsageCount
              ? 'bg-brand-100 text-brand-800 border border-brand-300 hover:bg-brand-200'
              : 'bg-cream-soft text-ink-muted border border-brand-200/60'
          }`}
          title={`${item.recipeUsageCount ?? 0} công thức có dùng ${item.name}`}
        >
          <span>{item.recipeUsageCount ?? 0}</span>
          <span className="sr-only"> công thức có dùng nguyên liệu này</span>
        </button>
      </td>

      {/* 6. Thao tác quản trị */}
      <td className="py-3 px-4 text-center align-middle whitespace-nowrap">
        <div className="flex items-center justify-center gap-1.5">
          <Button
            size="sm"
            variant="outline"
            onClick={() => onEditInfo(item)}
            aria-label={`Sửa ${item.name}`}
            className="h-8 py-1 px-2.5 text-xs font-semibold"
          >
            <Pencil className="h-3 w-3 mr-1 shrink-0" /> Sửa
          </Button>
          <Button
            size="sm"
            variant="outline"
            onClick={() => onOpenNutritionDetails(item)}
            aria-label={`Xem chi tiết ${item.name} · Sửa dinh dưỡng ${item.name}`}
            className="h-8 py-1 px-2.5 text-xs font-semibold"
          >
            <Activity className="h-3 w-3 mr-1 shrink-0 text-brand-600" /> Dinh dưỡng
          </Button>
          <Button
            size="sm"
            variant="outline"
            onClick={() => onToggleActive(item)}
            disabled={saving}
            aria-label={`${item.active ? 'Ngừng sử dụng' : 'Bật lại'} ${item.name}`}
            className={`h-8 py-1 px-2.5 text-xs font-semibold ${
              item.active
                ? 'text-red-700 hover:border-red-300 hover:bg-red-50'
                : 'text-leaf-700 hover:border-leaf-300 hover:bg-leaf-50'
            }`}
          >
            {item.active ? 'Ngừng sử dụng' : 'Bật lại'}
          </Button>
        </div>
      </td>
    </tr>
  );
}

export function AdminCatalogPage() {
  const [ingredients, setIngredients] = useState<CatalogIngredient[]>([]);
  const [query, setQuery] = useState('');
  const [groupFilter, setGroupFilter] = useState('');
  const [activeFilter, setActiveFilter] = useState('');
  const [nutritionFilter, setNutritionFilter] = useState('');
  const [ingredientFilters, setIngredientFilters] = useState<IngredientSearch>({
    query: '', ingredientGroup: '', active: null, nutritionSupported: null, page: 0, size: 24,
  });
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [referencesTarget, setReferencesTarget] = useState<CatalogIngredient | null>(null);
  const [recipeReferences, setRecipeReferences] = useState<IngredientRecipeReference[]>([]);
  const [recipeReferencePage, setRecipeReferencePage] = useState(0);
  const [recipeReferencePages, setRecipeReferencePages] = useState(0);
  const [recipeReferenceTotal, setRecipeReferenceTotal] = useState(0);
  const [referencesLoading, setReferencesLoading] = useState(false);
  const [catalogOptions, setCatalogOptions] = useState<{ groups: string[]; sources: string[] }>({
    groups: [...STANDARD_INGREDIENT_GROUPS],
    sources: [...STANDARD_DATA_SOURCES],
  });
  const [ingredientId, setIngredientId] = useState<number | null>(null);
  const [ingredientEditorOpen, setIngredientEditorOpen] = useState(false);
  const [nutritionTarget, setNutritionTarget] = useState<CatalogIngredient | null>(null);
  const [nutritionEnabled, setNutritionEnabled] = useState(false);
  const [nutritionDetailsLoading, setNutritionDetailsLoading] = useState(false);
  const [nutritionForm, setNutritionForm] = useState<NutritionDraft>(emptyNutritionDraft);
  const [ingredientForm, setIngredientForm] = useState({
    name: '',
    ingredientGroup: '',
    sourceName: '',
    sourceUrl: '',
    referenceDate: today(),
  });
  const [ingredientImageUrl, setIngredientImageUrl] = useState<string | null>(null);
  const [ingredientImageFile, setIngredientImageFile] = useState<File | null>(null);
  const [ingredientImagePreviewUrl, setIngredientImagePreviewUrl] = useState<string | null>(null);
  const [ingredientImageError, setIngredientImageError] = useState('');
  const ingredientImageInputRef = useRef<HTMLInputElement>(null);
  const [loading, setLoading] = useState(true);
  const [accessDenied, setAccessDenied] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const searchTimer = useRef<number | undefined>(undefined);
  const nutritionLoadId = useRef(0);
  const cancelNutritionDetailsLoad = () => {
    nutritionLoadId.current += 1;
    setNutritionDetailsLoading(false);
  };

  const refresh = async (filters = ingredientFilters) => {
    setLoading(true);
    setError('');
    setAccessDenied(false);
    try {
      const [ingredientPage, options] = await Promise.all([
        adminCatalogApi.searchIngredients(filters),
        adminCatalogApi.ingredientOptions(),
      ]);
      setIngredients(ingredientPage.content);
      setTotalElements(ingredientPage.totalElements);
      setTotalPages(ingredientPage.totalPages);

      const rawGroups = options?.groups && options.groups.length > 0
        ? options.groups
        : [...STANDARD_INGREDIENT_GROUPS];
      const rawSources = options?.sources && options.sources.length > 0
        ? options.sources
        : [...STANDARD_DATA_SOURCES];

      const cleanGroups = Array.from(new Set(rawGroups.map((g) => normalizeIngredientGroup(g))));
      const cleanSources = Array.from(new Set(rawSources.map((s) => normalizeSourceName(s))));

      setCatalogOptions({ groups: cleanGroups, sources: cleanSources });
    } catch (cause) {
      setAccessDenied(
        cause instanceof AdminCatalogApiError &&
          (cause.status === 401 || cause.status === 403),
      );
      setError(
        cause instanceof Error ? cause.message : 'Không tải được danh mục.',
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void refresh();
  }, []);

  useEffect(() => {
    if (!ingredientImagePreviewUrl) return;
    return () => URL.revokeObjectURL(ingredientImagePreviewUrl);
  }, [ingredientImagePreviewUrl]);

  const closeActiveModal = () => {
    if (saving) return;
    cancelNutritionDetailsLoad();
    setNutritionTarget(null);
    setReferencesTarget(null);
    setIngredientEditorOpen(false);
    setIngredientId(null);
    setIngredientImageUrl(null);
    setIngredientImageFile(null);
    setIngredientImagePreviewUrl(null);
    setIngredientImageError('');
  };

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !saving) {
        closeActiveModal();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [saving]);

  const perform = async (
    action: () => Promise<unknown>,
    successMessage: string,
    onSuccess?: () => void,
  ) => {
    setSaving(true);
    setError('');
    setNotice('');
    try {
      await action();
      onSuccess?.();
      setNotice(successMessage);
      await refresh();
    } catch (cause) {
      setError(
        cause instanceof Error ? cause.message : 'Không lưu được thay đổi.',
      );
    } finally {
      setSaving(false);
    }
  };

  const submitIngredient = async (event: FormEvent) => {
    event.preventDefault();
    if (ingredientImageError) return;
    const isCreatingIngredient = ingredientId === null;
    const refreshFilters = isCreatingIngredient
      ? { ...ingredientFilters, page: 0 }
      : ingredientFilters;
    const body = {
      ...ingredientForm,
      name: ingredientForm.name.trim(),
      ingredientGroup: normalizeIngredientGroup(ingredientForm.ingredientGroup).trim(),
      sourceName: normalizeSourceName(ingredientForm.sourceName).trim(),
      sourceUrl: ingredientForm.sourceUrl.trim() || null,
    };
    setSaving(true);
    setError('');
    setNotice('');
    let savedIngredient: CatalogIngredient | null = null;
    try {
      savedIngredient = ingredientId === null
        ? await adminCatalogApi.createIngredient(body)
        : await adminCatalogApi.updateIngredient(ingredientId, body);

      if (ingredientImageFile) {
        const uploaded = await adminCatalogApi.uploadIngredientImage(savedIngredient.id, ingredientImageFile);
        savedIngredient = { ...savedIngredient, imageUrl: uploaded.imageUrl };
      }

      setIngredientEditorOpen(false);
      setIngredientId(null);
      setIngredientForm({
        name: '',
        ingredientGroup: '',
        sourceName: '',
        sourceUrl: '',
        referenceDate: today(),
      });
      setIngredientImageUrl(null);
      setIngredientImageFile(null);
      setIngredientImagePreviewUrl(null);
      setIngredientImageError('');
      if (isCreatingIngredient) setIngredientFilters(refreshFilters);
      setNotice(ingredientImageFile
        ? 'Đã lưu thành công.'
        : ingredientId === null ? 'Đã thêm nguyên liệu.' : 'Đã cập nhật nguyên liệu.');
      await refresh(refreshFilters);
    } catch (cause) {
      const message = cause instanceof Error ? cause.message : 'Không lưu được thay đổi.';
      if (savedIngredient && ingredientImageFile) {
        if (isCreatingIngredient) setIngredientFilters(refreshFilters);
        await refresh(refreshFilters);
        setIngredientId(savedIngredient.id);
        setIngredientImageUrl(savedIngredient.imageUrl);
        setNotice('Thông tin nguyên liệu đã lưu, nhưng ảnh chưa tải lên thành công. Bạn có thể thử lưu lại để tải ảnh.');
        setError(`Không tải được ảnh minh họa: ${message}`);
      } else {
        setError(message);
      }
    } finally {
      setSaving(false);
    }
  };

  const editIngredient = (ingredient: CatalogIngredient) => {
    cancelNutritionDetailsLoad();
    setNutritionTarget(null);
    setReferencesTarget(null);
    setIngredientEditorOpen(true);
    setIngredientId(ingredient.id);
    setIngredientForm({
      name: ingredient.name,
      ingredientGroup: normalizeIngredientGroup(ingredient.ingredientGroup),
      sourceName: normalizeSourceName(ingredient.sourceName),
      sourceUrl: ingredient.sourceUrl ?? '',
      referenceDate: ingredient.referenceDate,
    });
    setIngredientImageUrl(ingredient.imageUrl);
    setIngredientImageFile(null);
    setIngredientImagePreviewUrl(null);
    setIngredientImageError('');
    setError('');
    setNotice('');
  };

  const openNutritionDetails = async (ingredient: CatalogIngredient) => {
    const loadId = ++nutritionLoadId.current;
    setReferencesTarget(null);
    setIngredientEditorOpen(false);
    setIngredientId(null);
    setNutritionTarget(ingredient);
    setNutritionEnabled(ingredient.nutritionSupported);
    setNutritionForm(toNutritionDraft(ingredient.nutrition));
    setNutritionDetailsLoading(true);
    setError('');
    setNotice('');
    try {
      const full = await adminCatalogApi.ingredient(ingredient.id);
      if (loadId !== nutritionLoadId.current) return;
      setNutritionTarget(full);
      setNutritionEnabled(full.nutritionSupported);
      setNutritionForm(toNutritionDraft(full.nutrition));
    } catch {
      // Keep loaded row data if remote detail fetch is skipped
    } finally {
      if (loadId === nutritionLoadId.current) setNutritionDetailsLoading(false);
    }
  };

  const showRecipeReferences = async (ingredient: CatalogIngredient) => {
    cancelNutritionDetailsLoad();
    setNutritionTarget(null);
    setIngredientEditorOpen(false);
    setReferencesTarget(ingredient);
    setRecipeReferences([]);
    setRecipeReferencePage(0);
    setRecipeReferencePages(0);
    setRecipeReferenceTotal(0);
    setReferencesLoading(true);
    setError('');
    setNotice('');
    try {
      const result = await adminCatalogApi.searchIngredientRecipes(ingredient.id, 0, 10);
      setRecipeReferences(result.content);
      setRecipeReferencePages(result.totalPages);
      setRecipeReferenceTotal(result.totalElements);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không tải được công thức đang dùng nguyên liệu này.');
    } finally {
      setReferencesLoading(false);
    }
  };

  const changeRecipeReferencePage = async (page: number) => {
    if (!referencesTarget) return;
    setReferencesLoading(true);
    setError('');
    try {
      const result = await adminCatalogApi.searchIngredientRecipes(referencesTarget.id, page, 10);
      setRecipeReferences(result.content);
      setRecipeReferencePage(result.page);
      setRecipeReferencePages(result.totalPages);
      setRecipeReferenceTotal(result.totalElements);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không tải được công thức đang dùng nguyên liệu này.');
    } finally {
      setReferencesLoading(false);
    }
  };

  const submitNutrition = (event: FormEvent) => {
    event.preventDefault();
    if (!nutritionTarget) return;
    const nutrition = Object.fromEntries(nutritionFields.map(({ key }) => {
      const value = nutritionForm[key].trim();
      return [key, value === '' ? null : Number(value)];
    })) as IngredientNutrition;
    void perform(
      () => adminCatalogApi.updateNutrition(nutritionTarget.id, nutrition),
      'Đã lưu số liệu dinh dưỡng. Các chỉ tiêu để trống sẽ không được tính.',
      () => setNutritionForm(toNutritionDraft(nutrition)),
    );
  };

  const toggleNutritionSupport = () => {
    if (!nutritionTarget) return;
    const enabled = !nutritionEnabled;
    void perform(
      () => adminCatalogApi.setNutritionSupported(nutritionTarget.id, enabled),
      enabled ? 'Đã bật tính các chỉ tiêu có dữ liệu.' : 'Đã ngừng hỗ trợ tính dinh dưỡng.',
      () => setNutritionEnabled(enabled),
    );
  };

  const toggleIngredient = (item: CatalogIngredient) =>
    void perform(
      () => adminCatalogApi.setIngredientActive(item.id, !item.active),
      item.active
        ? 'Đã ngừng sử dụng nguyên liệu, liên kết cũ được giữ nguyên.'
        : 'Đã bật lại nguyên liệu.',
    );

  const searchIngredients = (event: FormEvent) => {
    event.preventDefault();
    window.clearTimeout(searchTimer.current);
    const filters: IngredientSearch = {
      query: query.trim(),
      ingredientGroup: groupFilter.trim(),
      active: activeFilter === '' ? null : activeFilter === 'true',
      nutritionSupported: nutritionFilter === '' ? null : nutritionFilter === 'true',
      page: 0,
      size: ingredientFilters.size,
    };
    setIngredientFilters(filters);
    void refresh(filters);
  };

  const scheduleIngredientSearch = (value: string) => {
    window.clearTimeout(searchTimer.current);
    searchTimer.current = window.setTimeout(() => {
      const filters = { ...ingredientFilters, query: value.trim(), page: 0 };
      setIngredientFilters(filters);
      void refresh(filters);
    }, 300);
  };

  const changeIngredientPage = (page: number) => {
    const filters = { ...ingredientFilters, page };
    setIngredientFilters(filters);
    void refresh(filters);
  };

  const openNewIngredient = () => {
    cancelNutritionDetailsLoad();
    setReferencesTarget(null);
    setNutritionTarget(null);
    setIngredientId(null);
    setIngredientForm({
      name: '',
      ingredientGroup: '',
      sourceName: '',
      sourceUrl: '',
      referenceDate: today(),
    });
    setIngredientImageUrl(null);
    setIngredientImageFile(null);
    setIngredientImagePreviewUrl(null);
    setIngredientImageError('');
    setError('');
    setNotice('');
    setIngredientEditorOpen(true);
  };

  const selectIngredientImage = (file: File | undefined, input: HTMLInputElement) => {
    setIngredientImageError('');
    if (!file) return;
    if (!INGREDIENT_IMAGE_TYPES.has(file.type)) {
      setIngredientImageFile(null);
      setIngredientImagePreviewUrl(null);
      setIngredientImageError('Ảnh phải có định dạng JPEG, PNG hoặc WebP.');
      input.value = '';
      return;
    }
    if (file.size === 0 || file.size > MAX_INGREDIENT_IMAGE_BYTES) {
      setIngredientImageFile(null);
      setIngredientImagePreviewUrl(null);
      setIngredientImageError('Ảnh phải có dung lượng lớn hơn 0 và tối đa 2 MB.');
      input.value = '';
      return;
    }
    setIngredientImageFile(file);
    setIngredientImagePreviewUrl(URL.createObjectURL(file));
  };

  const activeCountOnPage = ingredients.filter((item) => item.active).length;
  const nutritionReadyCountOnPage = ingredients.filter((item) => item.nutritionSupported).length;

  return (
    <PageContainer className="w-full max-w-none px-0 py-2 sm:py-4">
      {/* Breadcrumb Navigation */}
      <nav aria-label="Đường dẫn trang" className="mb-4 flex flex-wrap items-center gap-2 text-xs font-medium text-ink-muted">
        <Link to="/trang-chu" className="transition-colors hover:text-brand-600">Trang chủ</Link>
        <span className="text-brand-300">/</span>
        <span>Quản trị hệ thống</span>
        <span className="text-brand-300">/</span>
        <span className="font-bold text-brand-600">Danh mục nguyên liệu dinh dưỡng</span>
      </nav>

      {/* Admin Command Hero Banner */}
      <div className="relative mb-7 overflow-hidden rounded-3xl border border-brand-200/80 bg-gradient-to-br from-paper via-paper to-brand-50/40 p-6 shadow-sm sm:p-8">
        <div
          aria-hidden="true"
          className="pointer-events-none absolute -right-16 -top-16 h-56 w-56 rounded-full bg-brand-500/10 blur-3xl"
        />
        <div className="relative flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div className="max-w-2xl">
            <div className="mb-3 inline-flex items-center gap-2 rounded-full border border-leaf-200/80 bg-leaf-50/90 px-3 py-1 text-xs font-bold uppercase tracking-[0.12em] text-leaf-700">
              <ShieldCheck className="h-3.5 w-3.5" /> Khu vực Administrator
            </div>
            <h1 className="text-3xl font-extrabold tracking-tight text-ink sm:text-4xl">
              Danh mục nguyên liệu dinh dưỡng
            </h1>
            <p className="mt-2.5 text-sm leading-6 text-ink-muted">
              Quản lý tên chuẩn, nguồn tham khảo và dữ liệu dinh dưỡng. Nguyên liệu đã được dùng chỉ có thể ngừng sử dụng, công thức cũ vẫn giữ liên kết.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            <div className="inline-flex items-center rounded-full border border-brand-200/80 bg-cream-soft/80 p-1">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-brand-600 px-3.5 py-1.5 text-xs font-bold text-on-brand shadow-xs">
                <Database className="h-3.5 w-3.5" /> Danh mục nguyên liệu
              </span>
              <Link
                to="/admin/xet-duyet-chuyen-gia"
                className="inline-flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-semibold text-ink-soft transition-colors hover:text-brand-600"
              >
                <Award className="h-3.5 w-3.5" /> Xét duyệt Chuyên gia
              </Link>
            </div>
            <Button
              variant="outline"
              onClick={() => void refresh()}
              disabled={loading}
              aria-label="Tải lại danh mục"
            >
              {loading ? (
                <LoaderCircle className="h-4 w-4 animate-spin text-brand-600" />
              ) : (
                <RefreshCw className="h-4 w-4" />
              )}{' '}
              Tải lại
            </Button>
          </div>
        </div>

        {!accessDenied && (
          <div className="relative mt-6 grid grid-cols-2 gap-3 border-t border-brand-100/80 pt-5 sm:grid-cols-4">
            <div className="flex items-center gap-3 rounded-2xl border border-brand-100/80 bg-cream-soft/60 px-4 py-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-100 text-brand-600">
                <Database className="h-5 w-5" />
              </span>
              <div className="min-w-0">
                <p className="text-[11px] font-semibold uppercase tracking-wider text-ink-muted">Tổng danh mục</p>
                <p className="truncate text-lg font-extrabold text-ink">{totalElements}</p>
              </div>
            </div>
            <div className="flex items-center gap-3 rounded-2xl border border-brand-100/80 bg-cream-soft/60 px-4 py-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-leaf-100 text-leaf-700">
                <Check className="h-5 w-5" />
              </span>
              <div className="min-w-0">
                <p className="text-[11px] font-semibold uppercase tracking-wider text-ink-muted">Đang hoạt động</p>
                <p className="truncate text-lg font-extrabold text-ink">{activeCountOnPage}/{ingredients.length || 0}</p>
              </div>
            </div>
            <div className="flex items-center gap-3 rounded-2xl border border-brand-100/80 bg-cream-soft/60 px-4 py-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-brand-100 text-brand-600">
                <Activity className="h-5 w-5" />
              </span>
              <div className="min-w-0">
                <p className="text-[11px] font-semibold uppercase tracking-wider text-ink-muted">Chuẩn 9 chỉ tiêu</p>
                <p className="truncate text-lg font-extrabold text-ink">{nutritionReadyCountOnPage}</p>
              </div>
            </div>
            <div className="flex items-center gap-3 rounded-2xl border border-brand-100/80 bg-cream-soft/60 px-4 py-3">
              <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-raised text-ink-soft">
                <Layers className="h-5 w-5" />
              </span>
              <div className="min-w-0">
                <p className="text-[11px] font-semibold uppercase tracking-wider text-ink-muted">Nhóm & Nguồn</p>
                <p className="truncate text-lg font-extrabold text-ink">
                  {catalogOptions.groups.length} · {catalogOptions.sources.length}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* 403 Forbidden State */}
      {accessDenied && (
        <Card className="flex flex-col items-center justify-center border-red-200 bg-paper p-10 text-center shadow-xs sm:p-14">
          <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-red-100 text-red-600 shadow-2xs">
            <ShieldAlert className="h-7 w-7" />
          </div>
          <h2 className="text-xl font-bold text-ink">Quyền truy cập bị từ chối (403 Forbidden)</h2>
          <p className="mt-2 max-w-md text-sm leading-relaxed text-ink-muted">
            Khu vực này chỉ dành riêng cho Quản trị viên hệ thống Mâm Xanh. Vui lòng đăng nhập bằng tài khoản có thẩm quyền để quản lý danh mục.
          </p>
          {error && (
            <div role="alert" className="mt-5 flex w-full max-w-md items-center gap-3 rounded-xl border border-red-200 bg-red-50 p-4 text-left text-sm text-red-800">
              <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />
              <span className="font-medium">{error}</span>
            </div>
          )}
        </Card>
      )}

      {accessDenied ? null : (
        <section className="w-full">
          <div>
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-extrabold text-ink sm:text-xl">Danh sách nguyên liệu</h2>
                <p className="mt-0.5 text-sm text-ink-muted">Tìm, lọc và quản lý dữ liệu dinh dưỡng cùng trạng thái sử dụng.</p>
              </div>
              <div className="flex items-center gap-2">
                <Button type="button" onClick={openNewIngredient} aria-label="Mở biểu mẫu thêm nguyên liệu">
                  <CirclePlus className="h-4 w-4" /> Thêm mới
                </Button>
              </div>
            </div>

            <Card className="mb-5 border-brand-200/70 p-4 shadow-xs sm:p-5">
              <div className="mb-3 flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-brand-600">
                <Filter className="h-3.5 w-3.5" /> Bộ lọc tra cứu danh mục
              </div>
              <form onSubmit={searchIngredients} className="grid items-end gap-3 sm:grid-cols-2 lg:grid-cols-[1.3fr_1fr_1fr_1fr_auto]">
                <label className="text-xs font-semibold text-ink-soft">
                  Từ khóa nguyên liệu
                  <span className="sr-only">Tìm tên nguyên liệu</span>
                  <div className="relative mt-1.5">
                    <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" />
                    <input
                      className={`${fieldClass} mt-0 pl-10 pr-4`}
                      value={query}
                      onChange={(event) => {
                        setQuery(event.target.value);
                        scheduleIngredientSearch(event.target.value);
                      }}
                      placeholder="Tìm tên nguyên liệu"
                    />
                  </div>
                </label>
                <label className="text-xs font-semibold text-ink-soft">
                  Phân loại
                  <select className={fieldClass} value={groupFilter} onChange={(event) => setGroupFilter(event.target.value)}>
                    <option value="">Tất cả nhóm</option>
                    {catalogOptions.groups.map((group) => (
                      <option key={group} value={group} className="font-sans">
                        {normalizeIngredientGroup(group)}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="text-xs font-semibold text-ink-soft">
                  Trạng thái danh mục
                  <select className={fieldClass} value={activeFilter} onChange={(event) => setActiveFilter(event.target.value)}>
                    <option value="">Tất cả</option>
                    <option value="true">Đang hoạt động</option>
                    <option value="false">Ngừng hoạt động</option>
                  </select>
                </label>
                <label className="text-xs font-semibold text-ink-soft">
                  Hỗ trợ tính dinh dưỡng
                  <select className={fieldClass} value={nutritionFilter} onChange={(event) => setNutritionFilter(event.target.value)}>
                    <option value="">Tất cả</option>
                    <option value="true">Có hỗ trợ</option>
                    <option value="false">Chưa hỗ trợ</option>
                  </select>
                </label>
                <Button type="submit" variant="outline" className="h-[42px] px-4">
                  Tìm và lọc
                </Button>
              </form>
              <p className="mt-3.5 flex items-start gap-2 rounded-xl bg-brand-50/50 px-3.5 py-2.5 text-xs leading-5 text-ink-muted">
                <Sparkles className="mt-0.5 h-3.5 w-3.5 shrink-0 text-brand-600" />
                <span>
                  “Đang hoạt động” quyết định nguyên liệu có thể được chọn cho công thức mới. “Hỗ trợ tính dinh dưỡng” là trạng thái riêng. Chỉ cần có ít nhất một chỉ tiêu, phần còn thiếu sẽ được cảnh báo.
                </span>
              </p>
            </Card>

            {/* Notifications */}
            {error && (
              <div
                role="alert"
                className="mb-5 flex items-center gap-3 rounded-2xl border border-red-200 bg-red-50/90 p-4 text-sm text-red-800 shadow-2xs"
              >
                <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />
                <div className="flex-1 font-medium">{error}</div>
              </div>
            )}
            {notice && (
              <div
                role="status"
                className="mb-5 flex items-center gap-3 rounded-2xl border border-leaf-200 bg-leaf-50/90 p-4 text-sm text-leaf-800 shadow-2xs"
              >
                <Check className="h-5 w-5 shrink-0 text-leaf-600" />
                <div className="flex-1 font-medium">{notice}</div>
              </div>
            )}

            <div className="space-y-8">
              {loading ? (
                <Card role="status" className="flex items-center justify-center gap-3 p-12 text-sm text-ink-muted">
                  <LoaderCircle className="h-5 w-5 animate-spin text-brand-600" /> Đang tải danh mục…
                </Card>
              ) : ingredients.length === 0 ? (
                <EmptyState
                  icon="🥗"
                  title="Chưa có nguyên liệu phù hợp"
                  description="Thêm nguyên liệu chuẩn hoặc điều chỉnh từ khóa tìm kiếm."
                />
              ) : (
                <div className="w-full overflow-hidden rounded-2xl border border-brand-200/80 bg-paper shadow-sm">
                  <div className="max-w-full overflow-x-auto">
                    <table className="w-full text-left text-sm border-collapse">
                      <thead className="bg-gradient-to-r from-brand-50/90 via-cream-soft to-brand-50/70 text-xs font-bold uppercase tracking-wider text-brand-900 border-b border-brand-200/80">
                        <tr>
                          <th scope="col" className="py-3.5 px-4 min-w-[200px] text-center">Nguyên liệu</th>
                          <th scope="col" className="py-3.5 px-2 min-w-[105px] whitespace-nowrap">Phân loại</th>
                          <th scope="col" className="py-3.5 px-3 text-center min-w-[130px]">Trạng thái</th>
                          <th scope="col" className="py-3.5 px-3 text-center min-w-[150px]">Dinh dưỡng</th>
                          <th scope="col" className="py-3.5 px-2 text-center min-w-[110px] whitespace-nowrap">Số công thức</th>
                          <th scope="col" className="py-3.5 px-4 text-center min-w-[240px]">Thao tác quản trị</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-brand-100/80">
                        {ingredients.map((item) => (
                          <IngredientTableRow
                            key={item.id}
                            item={item}
                            saving={saving}
                            onOpenNutritionDetails={(target) => void openNutritionDetails(target)}
                            onShowRecipes={(target) => void showRecipeReferences(target)}
                            onEditInfo={editIngredient}
                            onToggleActive={toggleIngredient}
                          />
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
              {totalElements > 0 && (
                <div className="mt-8 sm:mt-10 flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-brand-200/80 bg-paper px-5 py-4 text-sm text-ink-muted shadow-xs">
                  <span className="font-medium">{totalElements} nguyên liệu · Trang {ingredientFilters.page + 1}/{totalPages}</span>
                  <div className="flex gap-2.5">
                    <Button type="button" variant="outline" size="sm" disabled={loading || ingredientFilters.page === 0} onClick={() => changeIngredientPage(ingredientFilters.page - 1)}>Trang trước</Button>
                    <Button type="button" variant="outline" size="sm" disabled={loading || ingredientFilters.page + 1 >= totalPages} onClick={() => changeIngredientPage(ingredientFilters.page + 1)}>Trang sau</Button>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Floating Modal (Bảng nổi) - Chi tiết & Sửa số liệu dinh dưỡng */}
          {nutritionTarget && (
            <div className="fixed inset-2 z-50 flex items-start justify-center overflow-y-auto overscroll-contain px-2 py-4 sm:inset-3 sm:items-center sm:px-3 sm:py-5">
              <div
                className="fixed inset-0 bg-black/60 backdrop-blur-xs transition-opacity cursor-pointer"
                onClick={closeActiveModal}
                aria-hidden="true"
              />
              <div
                className="relative my-auto w-full max-w-2xl shrink-0 rounded-3xl border border-brand-200/80 bg-paper p-5 sm:p-7 shadow-2xl z-10 modal-panel-enter"
                role="dialog"
                aria-modal="true"
              >
                <div className="flex items-start justify-between gap-3 border-b border-brand-100 pb-4">
                  <div>
                    <p className="text-[11px] font-bold uppercase tracking-wider text-brand-600">Hồ sơ & Dinh dưỡng nguyên liệu</p>
                    <h2 className="mt-0.5 text-lg sm:text-xl font-extrabold text-ink">Chi tiết · {nutritionTarget.name}</h2>
                    <p className="mt-1 text-xs text-ink-muted">Thông tin dinh dưỡng trên 100 g và nguồn tham khảo.</p>
                  </div>
                  <button
                    type="button"
                    onClick={closeActiveModal}
                    className="rounded-full p-1.5 text-ink-muted hover:bg-brand-100 hover:text-ink transition-colors"
                    aria-label="Tắt"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>

                {/* Metadata section with balanced 2 rows/cards for Status */}
                <div className="mt-4 rounded-2xl border border-brand-100/80 bg-brand-50/40 p-4 text-xs sm:text-sm space-y-3">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-2.5 gap-y-2.5">
                    <div>
                      <span className="font-medium text-ink-muted">Nhóm nguyên liệu:</span>{' '}
                      <strong className="text-ink">{normalizeIngredientGroup(nutritionTarget.ingredientGroup)}</strong>
                    </div>
                    <div className="min-w-0">
                      <span className="font-medium text-ink-muted">Nguồn:</span>{' '}
                      <strong className="text-ink">{normalizeSourceName(nutritionTarget.sourceName)}</strong>
                      {nutritionTarget.sourceUrl && <>
                        {' '}
                        <a
                          href={nutritionTarget.sourceUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          aria-label="Mở liên kết nguồn tham khảo"
                          className="inline-flex translate-y-[-1px] align-middle text-brand-600 hover:text-brand-800"
                        >
                          <ExternalLink className="h-3.5 w-3.5" />
                        </a>
                      </>}
                    </div>
                    <div>
                      <span className="font-medium text-ink-muted">Ngày đối chiếu:</span>{' '}
                      <span className="font-semibold text-ink">{nutritionTarget.referenceDate}</span>
                    </div>
                    <div>
                      <span className="font-medium text-ink-muted">Công thức đang dùng:</span>{' '}
                      <span className="font-semibold text-brand-700">{nutritionTarget.recipeUsageCount ?? 0} công thức</span>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-2 border-t border-brand-100/70">
                    <div>
                      <span className="font-medium text-ink-muted">Trạng thái danh mục:</span>{' '}
                      <span className={`font-semibold ${nutritionTarget.active ? 'text-leaf-700' : 'text-ink-muted'}`}>
                        {nutritionTarget.active ? 'Đang hoạt động' : 'Không hoạt động'}
                      </span>
                    </div>
                    <div>
                      <span className="font-medium text-ink-muted">Dữ liệu tính toán:</span>{' '}
                      <span className={`font-semibold ${nutritionEnabled ? 'text-leaf-700' : 'text-ink-muted'}`}>
                        {nutritionEnabled ? 'Hỗ trợ dinh dưỡng' : 'Chưa hỗ trợ'}
                      </span>
                    </div>
                  </div>

                </div>

                {/* Nutrition editing form with 9 inputs */}
                <form onSubmit={submitNutrition} className="mt-5 space-y-4">
                  {nutritionDetailsLoading && (
                    <p role="status" className="text-sm text-ink-muted">Đang tải số liệu dinh dưỡng…</p>
                  )}
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <h3 className="text-xs font-bold uppercase tracking-wider text-brand-700">Số liệu dinh dưỡng (trên 100 g)</h3>
                      <span className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold ${
                        nutritionEnabled
                          ? 'border-leaf-200 bg-leaf-50 text-leaf-700'
                          : 'border-brand-200 bg-brand-50 text-ink-muted'
                      }`}>
                        {nutritionEnabled ? 'Đang tính dữ liệu có sẵn' : 'Chưa đưa vào tính'}
                      </span>
                    </div>
                    <p className="text-xs text-ink-muted mb-3">Nhập số liệu trên 100 g. Nguồn có thể là USDA/NIH, nguồn khác hoặc AI estimate. Chỉ tiêu để trống sẽ không được tính và không bị xem là 0.</p>
                    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                      {nutritionFields.map(({ key, label, unit, step }) => (
                        <Field key={key} label={`${label} (${unit}/100 g)`}>
                          <input
                            className={fieldClass}
                            type="number"
                            min="0"
                            step={step}
                            inputMode="decimal"
                            aria-label={`${label} trên 100 g`}
                            value={nutritionForm[key]}
                            disabled={nutritionDetailsLoading || saving}
                            onChange={(event) => setNutritionForm({ ...nutritionForm, [key]: event.target.value })}
                          />
                        </Field>
                      ))}
                    </div>
                  </div>

                  <div className="flex flex-wrap items-center justify-between gap-2 border-t border-brand-100 pt-4">
                    <div className="flex flex-wrap gap-2">
                      <Button type="submit" disabled={saving || nutritionDetailsLoading}>
                        {saving ? 'Đang lưu…' : 'Lưu chỉ tiêu'}
                      </Button>
                      <Button type="button" variant="outline" disabled={saving || nutritionDetailsLoading} onClick={toggleNutritionSupport}>
                        {nutritionEnabled ? 'Ngừng hỗ trợ tính' : 'Bật hỗ trợ tính'}
                      </Button>
                    </div>
                    <Button type="button" variant="ghost" disabled={saving} onClick={closeActiveModal}>
                      Đóng
                    </Button>
                  </div>
                </form>
              </div>
            </div>
          )}

          {/* Floating Modal (Bảng nổi) - Công thức tham chiếu */}
          {referencesTarget && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto">
              <div
                className="fixed inset-0 bg-black/60 backdrop-blur-xs transition-opacity cursor-pointer"
                onClick={closeActiveModal}
                aria-hidden="true"
              />
              <div
                className="relative w-full max-w-xl max-h-[90vh] overflow-y-auto rounded-3xl border border-brand-200/80 bg-paper p-5 sm:p-7 shadow-2xl z-10 modal-panel-enter"
                role="dialog"
                aria-modal="true"
              >
                <div className="flex items-start justify-between gap-3 border-b border-brand-100 pb-4">
                  <div>
                    <p className="text-[11px] font-bold uppercase tracking-wider text-brand-600">Kiểm tra tham chiếu</p>
                    <h2 className="mt-0.5 text-lg sm:text-xl font-extrabold text-ink">Công thức dùng {referencesTarget.name}</h2>
                    <p className="mt-1 text-xs text-ink-muted">Danh sách công thức đang sử dụng nguyên liệu này trong hệ thống.</p>
                  </div>
                  <button
                    type="button"
                    onClick={closeActiveModal}
                    className="rounded-full p-1.5 text-ink-muted hover:bg-brand-100 hover:text-ink transition-colors"
                    aria-label="Tắt"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>

                {referencesLoading ? (
                  <div className="flex items-center justify-center gap-2 p-10 text-sm text-ink-muted">
                    <LoaderCircle className="h-5 w-5 animate-spin text-brand-600" /> Đang tải công thức…
                  </div>
                ) : error ? null : recipeReferences.length === 0 ? (
                  <p className="mt-6 rounded-2xl border border-dashed border-brand-200 bg-cream-soft/40 p-6 text-center text-sm text-ink-muted">
                    Chưa có công thức nào tham chiếu nguyên liệu này.
                  </p>
                ) : (
                  <ul className="mt-4 space-y-2.5">
                    {recipeReferences.map((recipe) => (
                      <li key={recipe.recipeId} className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-brand-100 bg-cream-soft/60 p-3.5 transition-colors hover:border-brand-200">
                        <span>
                          <Link
                            to={`/cong-thuc/id/${recipe.recipeId}`}
                            className="font-bold text-ink hover:text-brand-700 hover:underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"
                          >
                            {recipe.title}
                          </Link>
                          <span className="mt-1 block text-xs text-ink-muted">
                            {recipe.publishedAt ? new Date(recipe.publishedAt).toLocaleDateString('vi-VN') : 'Chưa công khai'}
                          </span>
                        </span>
                        <span className="rounded-full border border-brand-200/60 bg-brand-50 px-2.5 py-1 text-xs font-semibold text-brand-800">
                          {recipeStatusLabel(recipe.status)}
                        </span>
                      </li>
                    ))}
                  </ul>
                )}

                {recipeReferenceTotal > 0 && (
                  <div className="mt-5 flex items-center justify-between border-t border-brand-100 pt-3 text-xs text-ink-muted">
                    <span>{recipeReferenceTotal} công thức · Trang {recipeReferencePage + 1}/{recipeReferencePages}</span>
                    <div className="flex gap-2">
                      <Button size="sm" variant="outline" disabled={referencesLoading || recipeReferencePage === 0} onClick={() => void changeRecipeReferencePage(recipeReferencePage - 1)}>Trang trước</Button>
                      <Button size="sm" variant="outline" disabled={referencesLoading || recipeReferencePage + 1 >= recipeReferencePages} onClick={() => void changeRecipeReferencePage(recipeReferencePage + 1)}>Trang sau</Button>
                    </div>
                  </div>
                )}

                <div className="mt-6 flex justify-end border-t border-brand-100 pt-3">
                  <Button type="button" variant="ghost" onClick={closeActiveModal}>
                    Đóng
                  </Button>
                </div>
              </div>
            </div>
          )}

          {/* Floating Modal (Bảng nổi) - Thêm / Sửa nguyên liệu */}
          {ingredientEditorOpen && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto">
              <div
                className="fixed inset-0 bg-black/60 backdrop-blur-xs transition-opacity cursor-pointer"
                onClick={closeActiveModal}
                aria-hidden="true"
              />
              <div
                className="relative w-full max-w-xl max-h-[90vh] overflow-y-auto rounded-3xl border border-brand-200/80 bg-paper p-5 sm:p-7 shadow-2xl z-10 modal-panel-enter"
                role="dialog"
                aria-modal="true"
              >
                <div className="flex items-center justify-between border-b border-brand-100 pb-4">
                  <div>
                    <p className="text-[11px] font-bold uppercase tracking-wider text-brand-600">Thông tin chuẩn</p>
                    <h2 className="mt-0.5 text-lg sm:text-xl font-extrabold text-ink">
                      {ingredientId === null ? 'Thêm nguyên liệu' : 'Sửa nguyên liệu'}
                    </h2>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="rounded-full border border-brand-200/60 bg-brand-50 px-2.5 py-0.5 text-xs font-semibold text-brand-700">
                      {ingredientId === null ? 'Thêm mới' : 'Chỉnh sửa'}
                    </span>
                    <button
                      type="button"
                      disabled={saving}
                      onClick={closeActiveModal}
                      className="rounded-full p-1.5 text-ink-muted hover:bg-brand-100 hover:text-ink transition-colors"
                      aria-label="Tắt"
                    >
                      <X className="h-5 w-5" />
                    </button>
                  </div>
                </div>

                <form onSubmit={submitIngredient} className="mt-5 space-y-4">
                  <Field label="Tên chuẩn tiếng Việt">
                    <input
                      className={fieldClass}
                      required
                      maxLength={200}
                      value={ingredientForm.name}
                      onChange={(event) =>
                        setIngredientForm({
                          ...ingredientForm,
                          name: event.target.value,
                        })
                      }
                      placeholder="Ví dụ: Đậu hũ non, Nấm đông cô"
                    />
                  </Field>
                  <Field label="Nhóm nguyên liệu">
                    <select
                      className={fieldClass}
                      required
                      value={ingredientForm.ingredientGroup}
                      onChange={(event) =>
                        setIngredientForm({
                          ...ingredientForm,
                          ingredientGroup: event.target.value,
                        })
                      }
                    >
                      <option value="" disabled>Chọn nhóm nguyên liệu</option>
                      {catalogOptions.groups.map((group) => (
                        <option key={group} value={group} className="font-sans">
                          {normalizeIngredientGroup(group)}
                        </option>
                      ))}
                    </select>
                  </Field>
                  <div className="grid gap-3 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-end">
                    <Field label="Ảnh minh họa">
                      <input
                        ref={ingredientImageInputRef}
                        className={fieldClass}
                        type="file"
                        accept="image/jpeg,image/png,image/webp"
                        onChange={(event) => selectIngredientImage(event.currentTarget.files?.[0], event.currentTarget)}
                        aria-describedby="ingredient-image-help"
                      />
                    </Field>
                    <IngredientImage
                      imageUrl={ingredientImagePreviewUrl ?? ingredientImageUrl}
                      name={ingredientForm.name || 'nguyên liệu'}
                      containerClassName="h-20 w-20 overflow-hidden rounded-xl border border-brand-200/80 bg-brand-50 shadow-2xs"
                      placeholderSize="large"
                    />
                  </div>
                  <p id="ingredient-image-help" className="-mt-2 text-xs leading-5 text-ink-muted">
                    Chọn ảnh JPEG, PNG hoặc WebP, tối đa 2 MB. Ảnh sẽ được tải lên và lưu khi bạn bấm lưu nguyên liệu.
                    {ingredientImageUrl && ' Ảnh mới sẽ thay thế ảnh hiện tại.'}
                  </p>
                  {ingredientImageFile && (
                    <div className="-mt-2 flex flex-wrap items-center gap-2 text-xs text-ink-soft">
                      <span className="max-w-full truncate" title={ingredientImageFile.name}>{ingredientImageFile.name}</span>
                      <button
                        type="button"
                        onClick={() => {
                          setIngredientImageFile(null);
                          setIngredientImagePreviewUrl(null);
                          setIngredientImageError('');
                          if (ingredientImageInputRef.current) ingredientImageInputRef.current.value = '';
                        }}
                        className="font-semibold text-brand-700 underline hover:text-brand-900"
                      >
                        Bỏ ảnh mới chọn
                      </button>
                    </div>
                  )}
                  {ingredientImageError && (
                    <p role="alert" className="-mt-2 text-xs font-medium text-red-700">{ingredientImageError}</p>
                  )}
                  {error && (
                    <p role="alert" className="rounded-xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800">{error}</p>
                  )}
                  {notice && (
                    <p role="status" className="rounded-xl border border-leaf-200 bg-leaf-50 px-3 py-2 text-sm text-leaf-800">{notice}</p>
                  )}
                  <Field label="Nguồn dữ liệu hoặc cách ước tính">
                    <input
                      className={fieldClass}
                      type="text"
                      list="ingredient-source-suggestions"
                      required
                      maxLength={200}
                      value={ingredientForm.sourceName}
                      onChange={(event) =>
                        setIngredientForm({
                          ...ingredientForm,
                          sourceName: event.target.value,
                        })
                      }
                    />
                    <datalist id="ingredient-source-suggestions">
                      {catalogOptions.sources.map((source) => (
                        <option key={source} value={normalizeSourceName(source)} />
                      ))}
                    </datalist>
                  </Field>
                  <Field label="Đường dẫn nguồn (không bắt buộc)">
                    <input
                      className={fieldClass}
                      type="url"
                      maxLength={2048}
                      value={ingredientForm.sourceUrl}
                      onChange={(event) =>
                        setIngredientForm({
                          ...ingredientForm,
                          sourceUrl: event.target.value,
                        })
                      }
                      placeholder="https://..."
                    />
                  </Field>
                  <p className="-mt-2 text-xs leading-5 text-ink-muted">
                    USDA/NIH và đường dẫn tham khảo không bắt buộc. Ghi rõ nguồn do người nhập hoặc AI tạo. Có thể bật tính khi đã có ít nhất một chỉ tiêu.
                  </p>
                  <Field label="Ngày tham khảo">
                    <input
                      className={fieldClass}
                      type="date"
                      required
                      max={today()}
                      value={ingredientForm.referenceDate}
                      onChange={(event) =>
                        setIngredientForm({
                          ...ingredientForm,
                          referenceDate: event.target.value,
                        })
                      }
                    />
                  </Field>
                  <div className="flex flex-wrap gap-2 border-t border-brand-100 pt-4">
                    <Button type="submit" disabled={saving}>
                      {saving ? (
                        'Đang lưu…'
                      ) : ingredientId === null ? (
                        <>
                          <CirclePlus className="h-4 w-4" /> Thêm nguyên liệu
                        </>
                      ) : (
                        'Lưu thay đổi'
                      )}
                    </Button>
                    <Button
                      type="button"
                      variant="ghost"
                      disabled={saving}
                      onClick={closeActiveModal}
                    >
                      Hủy
                    </Button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </section>
      )}
    </PageContainer>
  );
}
