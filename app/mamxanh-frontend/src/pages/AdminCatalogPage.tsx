import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import {
  Activity,
  AlertCircle,
  Apple,
  Award,
  Check,
  CirclePlus,
  Database,
  ExternalLink,
  Filter,
  Layers,
  LoaderCircle,
  Nut,
  Pencil,
  RefreshCw,
  Salad,
  Search,
  ShieldAlert,
  ShieldCheck,
  Sparkles,
  Wheat,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, Card, EmptyState } from '../components/ui';
import { getIngredientImageUrl, getIngredientGroupVisual } from '../utils/ingredientImage';
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

const today = () => {
  const date = new Date();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return date.getFullYear() + '-' + month + '-' + day;
};
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
  onShowDetails: (item: CatalogIngredient) => void;
  onShowRecipes: (item: CatalogIngredient) => void;
  onEditNutrition: (item: CatalogIngredient) => void;
  onEditInfo: (item: CatalogIngredient) => void;
  onToggleActive: (item: CatalogIngredient) => void;
}

function IngredientCard({
  item,
  saving,
  onShowDetails,
  onShowRecipes,
  onEditNutrition,
  onEditInfo,
  onToggleActive,
}: IngredientCardProps) {
  const [imageError, setImageError] = useState(false);
  const rawImageUrl = getIngredientImageUrl(item.name);
  const groupVisual = getIngredientGroupVisual(item.ingredientGroup);
  const showImage = Boolean(rawImageUrl && !imageError);

  return (
    <Card
      key={item.id}
      className={`group relative flex flex-col overflow-hidden rounded-2xl border transition-all duration-200 motion-safe:hover:-translate-y-1 ${
        item.active
          ? 'border-brand-200/80 bg-paper shadow-2xs hover:border-brand-400 hover:shadow-md'
          : 'border-dashed border-brand-200 bg-brand-50/20 opacity-85 hover:opacity-100 hover:border-brand-300'
      }`}
    >
      {/* A. Image / Fallback Container */}
      <div className="relative aspect-[16/10] w-full overflow-hidden bg-gradient-to-br from-brand-50 to-brand-100/60 rounded-t-2xl border-b border-brand-100/70">
        {showImage ? (
          <img
            src={rawImageUrl!}
            alt={item.name}
            loading="lazy"
            onError={() => setImageError(true)}
            className="h-full w-full object-cover transition-transform duration-300 ease-out group-hover:scale-105"
          />
        ) : (
          <div className="flex h-full w-full flex-col items-center justify-center gap-1.5 p-4 text-center select-none bg-gradient-to-br from-brand-100/40 via-cream-soft to-brand-50/60">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl border border-brand-200/80 bg-paper/90 text-brand-600 shadow-2xs">
              {groupVisual.icon === 'apple' && <Apple className="h-6 w-6" />}
              {groupVisual.icon === 'wheat' && <Wheat className="h-6 w-6" />}
              {groupVisual.icon === 'nut' && <Nut className="h-6 w-6" />}
              {groupVisual.icon === 'layers' && <Layers className="h-6 w-6" />}
              {groupVisual.icon === 'sparkles' && <Sparkles className="h-6 w-6" />}
              {groupVisual.icon === 'salad' && <Salad className="h-6 w-6" />}
            </div>
            <span className="text-[11px] font-medium text-ink-muted/80">{groupVisual.label}</span>
          </div>
        )}

        {/* Top Floating Badges */}
        <div className="absolute top-2.5 inset-x-2.5 flex items-center justify-between gap-1.5 pointer-events-none">
          <span className={`rounded-lg border px-2.5 py-0.5 text-xs font-semibold backdrop-blur-xs shadow-2xs ${groupVisual.badgeClass}`}>
            {normalizeIngredientGroup(item.ingredientGroup)}
          </span>
          <span
            className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-semibold backdrop-blur-xs shadow-2xs ${
              item.active
                ? 'border-leaf-300/80 bg-leaf-50/95 text-leaf-800'
                : 'border-brand-200/80 bg-paper/95 text-ink-muted'
            }`}
          >
            {item.active && <span className="size-1.5 rounded-full bg-leaf-500" />}
            {item.active ? 'Đang dùng' : 'Ngừng dùng'}
          </span>
        </div>

        {/* Bottom Floating Badge: Nutrition Supported */}
        <div className="absolute bottom-2.5 left-2.5 pointer-events-none">
          <span
            className={`rounded-md border px-2 py-0.5 text-[11px] font-semibold backdrop-blur-xs shadow-2xs ${
              item.nutritionSupported
                ? 'border-leaf-300/80 bg-leaf-50/95 text-leaf-800'
                : 'border-brand-200/80 bg-paper/95 text-ink-muted'
            }`}
          >
            {item.nutritionSupported ? 'Có dữ liệu tính toán' : 'Chưa hỗ trợ tính'}
          </span>
        </div>
      </div>

      {/* B & C & D & E: Content Body */}
      <div className="flex flex-1 flex-col justify-between p-4 sm:p-5">
        <div>
          {/* Title */}
          <div className="flex items-start justify-between gap-2">
            <h2 className="text-base font-extrabold text-ink transition-colors group-hover:text-brand-600 sm:text-lg line-clamp-1" title={item.name}>
              {item.name}
            </h2>
          </div>

          {/* D. Source Metadata & E. Recipe Usage */}
          <div className="mt-2 space-y-1 text-xs text-ink-muted">
            <div className="flex items-center justify-between gap-2">
              <span className="truncate">Nguồn: <strong className="font-semibold text-ink-soft">{normalizeSourceName(item.sourceName)}</strong></span>
              {item.sourceUrl && (
                <a
                  href={item.sourceUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex shrink-0 items-center gap-1 font-semibold text-brand-600 hover:underline"
                  title="Mở liên kết nguồn"
                >
                  Nguồn <ExternalLink className="h-3 w-3" />
                </a>
              )}
            </div>
            <div className="flex items-center justify-between gap-2 text-ink-muted">
              <span>Ngày tham khảo: {item.referenceDate}</span>
              <span className={`font-medium ${item.recipeUsageCount ? 'font-semibold text-brand-600' : 'text-ink-soft'}`}>
                {item.recipeUsageCount ?? 0} công thức đang dùng
              </span>
            </div>
          </div>

          {/* C. Nutrition Summary: Micro-grid (Calo, Đạm, Carb, Béo) */}
          <div
            className="mt-3.5 grid grid-cols-4 gap-1.5 rounded-xl border border-brand-100/80 bg-cream-soft/60 p-2 text-center text-xs"
            aria-label={`Tóm tắt dinh dưỡng ${item.name}`}
          >
            <div className="rounded-lg bg-paper/90 py-1.5 px-1 shadow-2xs border border-brand-100/50">
              <div className="text-[10px] text-ink-muted">Calo</div>
              <div className="mt-0.5 font-bold text-ink truncate">
                {item.nutrition.energyKcalPer100g != null ? `${Number(item.nutrition.energyKcalPer100g.toFixed(0))}` : '—'}
              </div>
              <div className="text-[9px] text-ink-muted">kcal</div>
            </div>
            <div className="rounded-lg bg-paper/90 py-1.5 px-1 shadow-2xs border border-brand-100/50">
              <div className="text-[10px] text-ink-muted">Đạm</div>
              <div className="mt-0.5 font-semibold text-ink truncate">
                {item.nutrition.proteinGramsPer100g != null ? `${Number(item.nutrition.proteinGramsPer100g.toFixed(1))}` : '—'}
              </div>
              <div className="text-[9px] text-ink-muted">g</div>
            </div>
            <div className="rounded-lg bg-paper/90 py-1.5 px-1 shadow-2xs border border-brand-100/50">
              <div className="text-[10px] text-ink-muted">Carb</div>
              <div className="mt-0.5 font-semibold text-ink truncate">
                {item.nutrition.carbohydrateGramsPer100g != null ? `${Number(item.nutrition.carbohydrateGramsPer100g.toFixed(1))}` : '—'}
              </div>
              <div className="text-[9px] text-ink-muted">g</div>
            </div>
            <div className="rounded-lg bg-paper/90 py-1.5 px-1 shadow-2xs border border-brand-100/50">
              <div className="text-[10px] text-ink-muted">Béo</div>
              <div className="mt-0.5 font-semibold text-ink truncate">
                {item.nutrition.totalFatGramsPer100g != null ? `${Number(item.nutrition.totalFatGramsPer100g.toFixed(1))}` : '—'}
              </div>
              <div className="text-[9px] text-ink-muted">g</div>
            </div>
          </div>

          {/* Additional Micronutrients: Xơ, Canxi, Sắt (if available) */}
          {(item.nutrition.fiberGramsPer100g !== null || item.nutrition.calciumMgPer100g !== null || item.nutrition.ironMgPer100g !== null) && (
            <div className="mt-2.5 flex flex-wrap gap-1.5 text-[11px] text-ink-muted">
              {item.nutrition.fiberGramsPer100g !== null && item.nutrition.fiberGramsPer100g !== undefined && (
                <span className="rounded-md border border-brand-100/70 bg-cream-soft/50 px-2 py-0.5">
                  Xơ ≈ {item.nutrition.fiberGramsPer100g} g
                </span>
              )}
              {item.nutrition.calciumMgPer100g !== null && item.nutrition.calciumMgPer100g !== undefined && (
                <span className="rounded-md border border-brand-100/70 bg-cream-soft/50 px-2 py-0.5">
                  Canxi ≈ {item.nutrition.calciumMgPer100g} mg
                </span>
              )}
              {item.nutrition.ironMgPer100g !== null && item.nutrition.ironMgPer100g !== undefined && (
                <span className="rounded-md border border-brand-100/70 bg-cream-soft/50 px-2 py-0.5">
                  Sắt ≈ {item.nutrition.ironMgPer100g} mg
                </span>
              )}
            </div>
          )}
        </div>

        {/* F. Management Actions Toolbar */}
        <div className="mt-4 border-t border-brand-100/70 pt-3.5 space-y-2">
          <div className="grid grid-cols-2 gap-2">
            <Button
              size="sm"
              variant="ghost"
              onClick={() => onShowDetails(item)}
              aria-label={`Xem chi tiết ${item.name}`}
              className="min-h-8 py-1.5 px-2.5 text-xs font-semibold justify-center whitespace-nowrap rounded-xl border border-brand-100/60 bg-cream-soft/40 hover:bg-brand-50"
            >
              Chi tiết
            </Button>
            <Button
              size="sm"
              variant="ghost"
              onClick={() => onShowRecipes(item)}
              aria-label={`Xem công thức dùng ${item.name}`}
              className="min-h-8 py-1.5 px-2.5 text-xs font-semibold justify-center whitespace-nowrap rounded-xl border border-brand-100/60 bg-cream-soft/40 hover:bg-brand-50"
            >
              Xem công thức
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => onEditNutrition(item)}
              aria-label={`Sửa dinh dưỡng ${item.name}`}
              className="min-h-9 py-1.5 px-2.5 text-xs font-semibold justify-center whitespace-nowrap"
            >
              Sửa dinh dưỡng
            </Button>
            <Button
              size="sm"
              variant="outline"
              onClick={() => onEditInfo(item)}
              aria-label={`Sửa ${item.name}`}
              className="min-h-9 py-1.5 px-2.5 text-xs font-semibold justify-center whitespace-nowrap"
            >
              <Pencil className="mr-1 h-3 w-3 shrink-0" /> Sửa thông tin
            </Button>
          </div>
          <div className="pt-0.5">
            <Button
              size="sm"
              variant="ghost"
              onClick={() => onToggleActive(item)}
              disabled={saving}
              aria-label={`${item.active ? 'Ngừng sử dụng' : 'Bật lại'} ${item.name}`}
              className={`w-full min-h-8 py-1.5 text-xs font-semibold justify-center rounded-xl ${
                item.active
                  ? 'text-ink-muted hover:text-red-600 hover:bg-red-50/50'
                  : 'text-leaf-700 hover:text-leaf-800 hover:bg-leaf-50/60'
              }`}
            >
              {item.active ? 'Ngừng sử dụng' : 'Kích hoạt lại'}
            </Button>
          </div>
        </div>
      </div>
    </Card>
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
  const [detailTarget, setDetailTarget] = useState<CatalogIngredient | null>(null);
  const [catalogOptions, setCatalogOptions] = useState<{ groups: string[]; sources: string[] }>({
    groups: [...STANDARD_INGREDIENT_GROUPS],
    sources: [...STANDARD_DATA_SOURCES],
  });
  const [ingredientId, setIngredientId] = useState<number | null>(null);
  const [ingredientEditorOpen, setIngredientEditorOpen] = useState(false);
  const [nutritionTarget, setNutritionTarget] = useState<CatalogIngredient | null>(null);
  const [nutritionEnabled, setNutritionEnabled] = useState(false);
  const [nutritionForm, setNutritionForm] = useState<NutritionDraft>(emptyNutritionDraft);
  const [ingredientForm, setIngredientForm] = useState({
    name: '',
    ingredientGroup: '',
    sourceName: '',
    sourceUrl: '',
    referenceDate: today(),
  });
  const [loading, setLoading] = useState(true);
  const [accessDenied, setAccessDenied] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [guideOpen, setGuideOpen] = useState(true);
  const searchTimer = useRef<number | undefined>(undefined);

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

  const submitIngredient = (event: FormEvent) => {
    event.preventDefault();
    const body = {
      ...ingredientForm,
      name: ingredientForm.name.trim(),
      ingredientGroup: normalizeIngredientGroup(ingredientForm.ingredientGroup).trim(),
      sourceName: normalizeSourceName(ingredientForm.sourceName).trim(),
      sourceUrl: ingredientForm.sourceUrl.trim() || null,
    };
    void perform(
      () =>
        ingredientId === null
          ? adminCatalogApi.createIngredient(body)
          : adminCatalogApi.updateIngredient(ingredientId, body),
      ingredientId === null
        ? 'Đã thêm nguyên liệu.'
        : 'Đã cập nhật nguyên liệu.',
      () => {
        setIngredientEditorOpen(false);
        setIngredientId(null);
        setIngredientForm({
          name: '',
          ingredientGroup: '',
          sourceName: '',
          sourceUrl: '',
          referenceDate: today(),
        });
      },
    );
  };

  const editIngredient = (ingredient: CatalogIngredient) => {
    setDetailTarget(null);
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
    setError('');
    setNotice('');
  };

  const editNutrition = (ingredient: CatalogIngredient) => {
    setDetailTarget(null);
    setReferencesTarget(null);
    setIngredientEditorOpen(false);
    setIngredientId(null);
    setNutritionTarget(ingredient);
    setNutritionEnabled(ingredient.nutritionSupported);
    setNutritionForm(toNutritionDraft(ingredient.nutrition));
    setError('');
    setNotice('');
  };

  const showRecipeReferences = async (ingredient: CatalogIngredient) => {
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

  const showIngredientDetails = async (ingredient: CatalogIngredient) => {
    setReferencesTarget(null);
    setNutritionTarget(null);
    setIngredientEditorOpen(false);
    setError('');
    try {
      setDetailTarget(await adminCatalogApi.ingredient(ingredient.id));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Không tải được chi tiết nguyên liệu.');
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
      'Đã lưu chỉ tiêu dinh dưỡng.',
      () => setNutritionForm(toNutritionDraft(nutrition)),
    );
  };

  const toggleNutritionSupport = () => {
    if (!nutritionTarget) return;
    const enabled = !nutritionEnabled;
    void perform(
      () => adminCatalogApi.setNutritionSupported(nutritionTarget.id, enabled),
      enabled ? 'Đã bật hỗ trợ tính dinh dưỡng.' : 'Đã ngừng hỗ trợ tính dinh dưỡng.',
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
    setDetailTarget(null);
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
    setError('');
    setNotice('');
    setIngredientEditorOpen(true);
  };

  const activeCountOnPage = ingredients.filter((item) => item.active).length;
  const nutritionReadyCountOnPage = ingredients.filter((item) => item.nutritionSupported).length;
  const hasActivePanel = Boolean(detailTarget || referencesTarget || nutritionTarget || ingredientEditorOpen);
  const isPanelOpen = hasActivePanel || guideOpen;

  return (
    <PageContainer className="max-w-[1680px] 2xl:max-w-[1780px] py-6 sm:py-8 px-4 sm:px-6 lg:px-8">
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
                <p className="text-[11px] font-semibold uppercase tracking-wider text-ink-muted">Đang kích hoạt</p>
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
        </Card>
      )}

      {accessDenied ? null : loading ? (
        <Card className="flex items-center justify-center gap-3 p-12 text-sm text-ink-muted">
          <LoaderCircle className="h-5 w-5 animate-spin text-brand-600" /> Đang tải danh mục…
        </Card>
      ) : (
        <section
          className={`grid items-start gap-6 ${
            hasActivePanel
              ? 'xl:grid-cols-[minmax(0,1fr)_440px] 2xl:grid-cols-[minmax(0,1fr)_480px]'
              : guideOpen
                ? 'xl:grid-cols-[minmax(0,1fr)_340px] 2xl:grid-cols-[minmax(0,1fr)_380px]'
                : 'grid-cols-1'
          }`}
        >
          <div>
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="text-lg font-extrabold text-ink sm:text-xl">Danh sách nguyên liệu</h2>
                <p className="mt-0.5 text-sm text-ink-muted">Tìm, lọc và quản lý dữ liệu dinh dưỡng cùng trạng thái sử dụng.</p>
              </div>
              <div className="flex items-center gap-2">
                {!hasActivePanel && !guideOpen && (
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => setGuideOpen(true)}
                    className="flex items-center gap-1.5"
                    aria-label="Mở hướng dẫn quản lý"
                  >
                    <Salad className="h-4 w-4 text-brand-600" /> Hướng dẫn
                  </Button>
                )}
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
                  Nhóm nguyên liệu
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
                    <option value="true">Đang dùng</option>
                    <option value="false">Ngừng dùng</option>
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
                  “Đang dùng” quyết định nguyên liệu có thể được chọn cho công thức mới. “Hỗ trợ tính dinh dưỡng” là trạng thái riêng và chỉ bật khi đủ 9 chỉ tiêu cùng nguồn tham khảo.
                </span>
              </p>
            </Card>

            <div className="space-y-8">
              {ingredients.length === 0 ? (
                <EmptyState
                  icon="🥗"
                  title="Chưa có nguyên liệu phù hợp"
                  description="Thêm nguyên liệu chuẩn hoặc điều chỉnh từ khóa tìm kiếm."
                />
              ) : (
                <div
                  className={`grid grid-cols-1 sm:grid-cols-2 ${
                    isPanelOpen
                      ? 'xl:grid-cols-2 2xl:grid-cols-3'
                      : 'lg:grid-cols-3'
                  } gap-5 sm:gap-6 items-stretch`}
                >
                  {ingredients.map((item) => (
                    <IngredientCard
                      key={item.id}
                      item={item}
                      saving={saving}
                      onShowDetails={(target) => void showIngredientDetails(target)}
                      onShowRecipes={(target) => void showRecipeReferences(target)}
                      onEditNutrition={editNutrition}
                      onEditInfo={editIngredient}
                      onToggleActive={toggleIngredient}
                    />
                  ))}
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

          {/* Sticky Right-hand Inspector / Editor Panel */}
          {(hasActivePanel || guideOpen) && (
            <div className="xl:sticky xl:top-20 xl:max-h-[calc(100vh-6rem)] xl:overflow-y-auto pr-1">
            {detailTarget ? (
              <Card className="modal-panel-enter h-fit border-brand-200/80 bg-paper p-5 shadow-md sm:p-6">
                <div className="flex items-start justify-between gap-3 border-b border-brand-100 pb-4">
                  <div>
                    <p className="text-[11px] font-bold uppercase tracking-wider text-brand-600">Hồ sơ nguyên liệu</p>
                    <h2 className="mt-0.5 text-lg font-extrabold text-ink">Chi tiết · {detailTarget.name}</h2>
                    <p className="mt-1 text-xs text-ink-muted">Thông tin dinh dưỡng trên 100 g và nguồn tham khảo.</p>
                  </div>
                  <Button type="button" variant="ghost" size="sm" onClick={() => setDetailTarget(null)}>Đóng</Button>
                </div>
                <dl className="mt-4 grid grid-cols-2 gap-2.5 text-sm">
                  {nutritionFields.map(({ key, label, unit }) => (
                    <div key={key} className="rounded-xl border border-brand-100/80 bg-cream-soft/70 p-3">
                      <dt className="text-xs text-ink-muted">{label}</dt>
                      <dd className="mt-1 font-bold text-ink">
                        {detailTarget.nutrition[key] ?? 'Chưa có'}
                        {detailTarget.nutrition[key] === null ? '' : ` ${unit}`}
                      </dd>
                    </div>
                  ))}
                </dl>
                <div className="mt-4 space-y-2 rounded-2xl border border-brand-100/80 bg-brand-50/40 p-4 text-sm">
                  <p><span className="font-medium text-ink-muted">Nhóm:</span> <strong className="text-ink">{normalizeIngredientGroup(detailTarget.ingredientGroup)}</strong></p>
                  <p><span className="font-medium text-ink-muted">Nguồn:</span> <strong className="text-ink">{normalizeSourceName(detailTarget.sourceName)}</strong></p>
                  <p><span className="font-medium text-ink-muted">Ngày đối chiếu:</span> {detailTarget.referenceDate}</p>
                  <p><span className="font-medium text-ink-muted">Trạng thái:</span> {detailTarget.active ? 'ACTIVE' : 'INACTIVE'} · {detailTarget.nutritionSupported ? 'Có hỗ trợ tính' : 'Chưa hỗ trợ tính'}</p>
                  <p><span className="font-medium text-ink-muted">Công thức đang tham chiếu:</span> {detailTarget.recipeUsageCount ?? 0}</p>
                  {detailTarget.sourceUrl && (
                    <a href={detailTarget.sourceUrl} target="_blank" rel="noopener noreferrer" className="mt-1 inline-flex items-center gap-1 font-semibold text-brand-600 hover:underline">
                      Mở nguồn tham khảo <ExternalLink className="h-3.5 w-3.5" />
                    </a>
                  )}
                </div>
              </Card>
            ) : referencesTarget ? (
              <Card className="modal-panel-enter h-fit border-brand-200/80 bg-paper p-5 shadow-md sm:p-6">
                <div className="flex items-start justify-between gap-3 border-b border-brand-100 pb-4">
                  <div>
                    <p className="text-[11px] font-bold uppercase tracking-wider text-brand-600">Kiểm tra tham chiếu</p>
                    <h2 className="mt-0.5 text-lg font-extrabold text-ink">Công thức dùng {referencesTarget.name}</h2>
                    <p className="mt-1 text-xs text-ink-muted">Danh sách được tải từ Backend để kiểm tra các liên kết trước khi ngừng sử dụng nguyên liệu.</p>
                  </div>
                  <Button type="button" variant="ghost" size="sm" onClick={() => setReferencesTarget(null)}>Đóng</Button>
                </div>
                {referencesLoading ? (
                  <p className="mt-4 flex items-center gap-2 text-sm text-ink-muted">
                    <LoaderCircle className="h-4 w-4 animate-spin text-brand-600" /> Đang tải công thức…
                  </p>
                ) : error ? null : recipeReferences.length === 0 ? (
                  <p className="mt-4 rounded-xl border border-dashed border-brand-200 p-4 text-center text-sm text-ink-muted">
                    Chưa có công thức nào tham chiếu nguyên liệu này.
                  </p>
                ) : (
                  <ul className="mt-4 space-y-2.5">
                    {recipeReferences.map((recipe) => (
                      <li key={recipe.recipeId} className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-brand-100 bg-cream-soft/60 p-3.5 transition-colors hover:border-brand-200">
                        <span>
                          <span className="font-bold text-ink">{recipe.title}</span>
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
                  <div className="mt-4 flex items-center justify-between border-t border-brand-100 pt-3 text-xs text-ink-muted">
                    <span>{recipeReferenceTotal} công thức · Trang {recipeReferencePage + 1}/{recipeReferencePages}</span>
                    <div className="flex gap-2">
                      <Button size="sm" variant="outline" disabled={referencesLoading || recipeReferencePage === 0} onClick={() => void changeRecipeReferencePage(recipeReferencePage - 1)}>Trang trước</Button>
                      <Button size="sm" variant="outline" disabled={referencesLoading || recipeReferencePage + 1 >= recipeReferencePages} onClick={() => void changeRecipeReferencePage(recipeReferencePage + 1)}>Trang sau</Button>
                    </div>
                  </div>
                )}
              </Card>
            ) : nutritionTarget ? (
              <Card className="modal-panel-enter h-fit border-brand-200/80 bg-paper p-5 shadow-md sm:p-6">
                <div className="flex flex-wrap items-start justify-between gap-3 border-b border-brand-100 pb-4">
                  <div>
                    <p className="text-[11px] font-bold uppercase tracking-wider text-brand-600">Biểu mẫu 9 chỉ tiêu</p>
                    <h2 className="mt-0.5 text-lg font-extrabold text-ink">Dinh dưỡng · {nutritionTarget.name}</h2>
                    <p className="mt-1 text-xs text-ink-muted">Nhập số liệu trên 100 g. Để trống nếu chưa biết, không nhập 0 thay cho dữ liệu thiếu.</p>
                  </div>
                  <span className={`rounded-full border px-2.5 py-1 text-xs font-semibold ${
                    nutritionEnabled
                      ? 'border-leaf-200 bg-leaf-50 text-leaf-700'
                      : 'border-brand-200 bg-brand-50 text-ink-muted'
                  }`}>
                    {nutritionEnabled ? 'Đang hỗ trợ tính' : 'Chưa hỗ trợ tính'}
                  </span>
                </div>
                <form onSubmit={submitNutrition} className="mt-4 space-y-3.5">
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
                          onChange={(event) => setNutritionForm({ ...nutritionForm, [key]: event.target.value })}
                        />
                      </Field>
                    ))}
                  </div>
                  <div className="flex flex-wrap gap-2 border-t border-brand-100 pt-3.5">
                    <Button type="submit" disabled={saving}>
                      {saving ? 'Đang lưu…' : 'Lưu chỉ tiêu'}
                    </Button>
                    <Button type="button" variant="outline" disabled={saving} onClick={toggleNutritionSupport}>
                      {nutritionEnabled ? 'Ngừng hỗ trợ tính' : 'Bật hỗ trợ tính'}
                    </Button>
                    <Button type="button" variant="ghost" disabled={saving} onClick={() => setNutritionTarget(null)}>
                      Đóng
                    </Button>
                  </div>
                </form>
              </Card>
            ) : ingredientEditorOpen ? (
              <Card className="modal-panel-enter h-fit border-brand-200/80 bg-paper p-5 shadow-md sm:p-6">
                <div className="flex items-center justify-between border-b border-brand-100 pb-4">
                  <div>
                    <p className="text-[11px] font-bold uppercase tracking-wider text-brand-600">Thông tin chuẩn</p>
                    <h2 className="mt-0.5 text-lg font-extrabold text-ink">
                      {ingredientId === null ? 'Thêm nguyên liệu' : 'Sửa nguyên liệu'}
                    </h2>
                  </div>
                  <span className="rounded-full border border-brand-200/60 bg-brand-50 px-2.5 py-0.5 text-xs font-semibold text-brand-700">
                    {ingredientId === null ? 'Thêm mới' : 'Chỉnh sửa'}
                  </span>
                </div>
                <form onSubmit={submitIngredient} className="mt-4 space-y-4">
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
                  <Field label="Tên nguồn dữ liệu">
                    <select
                      className={fieldClass}
                      required
                      value={ingredientForm.sourceName}
                      onChange={(event) =>
                        setIngredientForm({
                          ...ingredientForm,
                          sourceName: event.target.value,
                        })
                      }
                    >
                      <option value="" disabled>Chọn nguồn dữ liệu</option>
                      {catalogOptions.sources.map((source) => (
                        <option key={source} value={source} className="font-sans">
                          {normalizeSourceName(source)}
                        </option>
                      ))}
                    </select>
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
                    Có thể để trống đường dẫn khi chưa bật hỗ trợ tính. Để bật tính dinh dưỡng chính thức, cần có đủ chỉ tiêu và đường dẫn nguồn hợp lệ.
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
                  <div className="flex flex-wrap gap-2 border-t border-brand-100 pt-3.5">
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
                      onClick={() => {
                        setIngredientEditorOpen(false);
                        setIngredientId(null);
                      }}
                    >
                      Hủy
                    </Button>
                  </div>
                </form>
              </Card>
            ) : guideOpen ? (
              <Card className="h-fit border-dashed border-brand-200 bg-gradient-to-br from-paper to-brand-50/30 p-5 sm:p-6 shadow-xs">
                <div className="flex items-start justify-between gap-3">
                  <div className="flex h-11 w-11 items-center justify-center rounded-2xl border border-brand-200/70 bg-brand-100/70 text-brand-600 shadow-2xs">
                    <Salad className="h-5 w-5" />
                  </div>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => setGuideOpen(false)}
                    className="text-xs text-ink-muted hover:text-ink"
                    aria-label="Ẩn hướng dẫn quản lý"
                  >
                    Ẩn bảng
                  </Button>
                </div>
                <h2 className="mt-3.5 text-base font-extrabold text-ink sm:text-lg">Quản lý một nguyên liệu</h2>
                <p className="mt-1.5 text-xs leading-5 text-ink-muted sm:text-sm">
                  Chọn <strong className="text-ink">Sửa thông tin</strong> hoặc <strong className="text-ink">Sửa dinh dưỡng</strong> trên một dòng để cập nhật. Trước khi ngừng sử dụng, xem các công thức đang tham chiếu để giữ nguyên liên kết cũ.
                </p>
                <div className="mt-4 space-y-2 rounded-2xl border border-brand-100/80 bg-cream-soft/70 p-3.5 text-xs text-ink-muted">
                  <p className="font-bold text-ink">Quy chuẩn thẩm định dữ liệu:</p>
                  <p>• Đầy đủ 9 chỉ tiêu dinh dưỡng trên 100 g.</p>
                  <p>• Có nguồn dữ liệu và ngày đối chiếu rõ ràng.</p>
                  <p>• Giữ nguyên liên kết với các công thức đã xuất bản.</p>
                </div>
              </Card>
            ) : null}
          </div>
        )}
        </section>
      )}
    </PageContainer>
  );
}
