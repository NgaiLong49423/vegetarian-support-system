import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react';
import {
  AlertCircle,
  Check,
  CirclePlus,
  ExternalLink,
  LoaderCircle,
  Pencil,
  RefreshCw,
  Salad,
  Search,
  ShieldAlert,
  ShieldCheck,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, Card, EmptyState } from '../components/ui';
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
  'mt-1.5 w-full rounded-xl border border-brand-200/80 bg-paper px-3.5 py-2.5 text-sm text-ink outline-none transition-all placeholder:text-ink-muted/50 focus:border-brand-500 focus:ring-2 focus:ring-brand-100';
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

export function AdminCatalogPage() {
  const [ingredients, setIngredients] = useState<CatalogIngredient[]>([]);
  const [query, setQuery] = useState('');
  const [groupFilter, setGroupFilter] = useState('');
  const [activeFilter, setActiveFilter] = useState('');
  const [nutritionFilter, setNutritionFilter] = useState('');
  const [ingredientFilters, setIngredientFilters] = useState<IngredientSearch>({
    query: '', ingredientGroup: '', active: null, nutritionSupported: null, page: 0, size: 20,
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
  const [catalogOptions, setCatalogOptions] = useState({ groups: [] as string[], sources: [] as string[] });
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
      setCatalogOptions(options);
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
      ingredientGroup: ingredientForm.ingredientGroup.trim(),
      sourceName: ingredientForm.sourceName.trim(),
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
      ingredientGroup: ingredient.ingredientGroup,
      sourceName: ingredient.sourceName,
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
        ? 'Đã ngừng sử dụng nguyên liệu; liên kết cũ được giữ nguyên.'
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

  return (
    <PageContainer className="py-8 sm:py-10">
      {/* Breadcrumb Navigation */}
      <nav aria-label="Đường dẫn trang" className="mb-3 flex items-center gap-2 text-xs font-medium text-ink-muted">
        <span>Trang chủ</span>
        <span className="text-brand-300">/</span>
        <span>Quản trị hệ thống</span>
        <span className="text-brand-300">/</span>
        <span className="font-bold text-brand-700">Danh mục nguyên liệu dinh dưỡng</span>
      </nav>

      {/* Admin Header */}
      <div className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="mb-2 flex items-center gap-2 text-xs font-bold uppercase tracking-[0.14em] text-leaf-700">
            <ShieldCheck className="h-4 w-4" /> Khu vực Administrator
          </p>
          <h1 className="text-3xl font-extrabold tracking-tight text-ink sm:text-4xl">
            Danh mục nguyên liệu dinh dưỡng
          </h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-ink-muted">
            Quản lý tên chuẩn, nguồn tham khảo và dữ liệu dinh dưỡng. Nguyên liệu đã được dùng chỉ có thể ngừng sử dụng; công thức cũ vẫn giữ liên kết.
          </p>
        </div>
        <div className="flex items-center gap-3">
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

      {/* Notifications */}
      {error && (
        <div
          role="alert"
          className="mb-5 flex gap-3 rounded-2xl border border-red-200 bg-red-50/90 p-4 text-sm text-red-800 shadow-2xs"
        >
          <AlertCircle className="h-5 w-5 shrink-0 text-red-600" />
          <div className="flex-1 font-medium">{error}</div>
        </div>
      )}
      {notice && (
        <div
          role="status"
          className="mb-5 flex gap-3 rounded-2xl border border-leaf-200 bg-leaf-50/90 p-4 text-sm text-leaf-800 shadow-2xs"
        >
          <Check className="h-5 w-5 shrink-0 text-leaf-600" />
          <div className="flex-1 font-medium">{notice}</div>
        </div>
      )}

      {/* 403 Forbidden State */}
      {accessDenied && (
        <Card className="flex flex-col items-center justify-center p-10 sm:p-14 text-center border-red-200 bg-paper shadow-xs">
          <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-red-100 text-red-600 shadow-2xs">
            <ShieldAlert className="h-7 w-7" />
          </div>
          <h2 className="text-xl font-bold text-ink">Quyền truy cập bị từ chối (403 Forbidden)</h2>
          <p className="mt-2 max-w-md text-sm text-ink-muted leading-relaxed">
            Khu vực này chỉ dành riêng cho Quản trị viên hệ thống Mâm Xanh. Vui lòng đăng nhập bằng tài khoản có thẩm quyền để quản lý danh mục.
          </p>
        </Card>
      )}

      {accessDenied ? null : loading ? (
        <Card className="flex items-center justify-center gap-3 p-12 text-sm text-ink-muted">
          <LoaderCircle className="h-5 w-5 animate-spin text-brand-600" /> Đang tải danh mục…
        </Card>
      ) : (
        <>
          <section className="grid items-start gap-5 xl:grid-cols-[minmax(0,1.35fr)_minmax(340px,.85fr)]">
              <div>
                <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
                  <div>
                    <h2 className="text-lg font-bold text-ink">Danh sách nguyên liệu</h2>
                    <p className="mt-1 text-sm text-ink-muted">Tìm, lọc và quản lý dữ liệu dinh dưỡng cùng trạng thái sử dụng.</p>
                  </div>
                  <Button type="button" onClick={openNewIngredient} aria-label="Mở biểu mẫu thêm nguyên liệu">
                    <CirclePlus className="h-4 w-4" /> Thêm mới
                  </Button>
                </div>
                <Card className="mb-4 p-4 sm:p-5">
                <form onSubmit={searchIngredients} className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                  <label className="relative min-w-0">
                    <span className="sr-only">Tìm tên nguyên liệu</span>
                    <Search className="absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" />
                    <input
                      className={`${fieldClass} mt-0 pl-10 pr-4`}
                      value={query}
                      onChange={(event) => {
                        setQuery(event.target.value);
                        scheduleIngredientSearch(event.target.value);
                      }}
                      placeholder="Tìm tên nguyên liệu"
                    />
                  </label>
                  <label className="text-xs font-semibold text-ink-soft">
                    Nhóm nguyên liệu
                    <select className={fieldClass} value={groupFilter} onChange={(event) => setGroupFilter(event.target.value)}>
                      <option value="">Tất cả nhóm</option>
                      {catalogOptions.groups.map((group) => <option key={group} value={group}>{group}</option>)}
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
                  <Button type="submit" variant="outline" className="self-end">
                    Tìm và lọc
                  </Button>
                </form>
                <p className="mt-3 border-t border-brand-100 pt-3 text-xs leading-5 text-ink-muted">
                  “Đang dùng” quyết định nguyên liệu có thể được chọn cho công thức mới. “Hỗ trợ tính dinh dưỡng” là trạng thái riêng và chỉ bật khi đủ 9 chỉ tiêu cùng nguồn tham khảo.
                </p>
                </Card>
                <div className="space-y-3">
                  {ingredients.length === 0 ? (
                    <EmptyState
                      icon="🥗"
                      title="Chưa có nguyên liệu phù hợp"
                      description="Thêm nguyên liệu chuẩn hoặc điều chỉnh từ khóa tìm kiếm."
                    />
                  ) : (
                    ingredients.map((item) => (
                      <Card
                        key={item.id}
                        className={`flex flex-col gap-3 p-4 sm:flex-row sm:items-center sm:justify-between transition-all ${
                          item.active
                            ? 'border-brand-100 bg-paper hover:border-brand-200 shadow-2xs'
                            : 'border-dashed border-brand-200 bg-brand-50/30 opacity-80'
                        }`}
                      >
                        <div className="min-w-0">
                          <div className="flex flex-wrap items-center gap-2">
                            <h2 className="font-bold text-ink">{item.name}</h2>
                            <span className="rounded-md bg-brand-100/70 px-2 py-0.5 text-xs font-semibold text-brand-900 border border-brand-200/60">
                              {item.ingredientGroup}
                            </span>
                            <span
                              className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold border ${
                                item.active
                                  ? 'bg-leaf-50 text-leaf-700 border-leaf-200/60'
                                  : 'bg-brand-100 text-ink-muted border-brand-200/60'
                              }`}
                            >
                              {item.active && (
                                <span className="size-1.5 rounded-full bg-leaf-500"></span>
                              )}
                              {item.active ? 'Đang dùng' : 'Ngừng dùng'}
                            </span>
                            <span className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold ${
                              item.nutritionSupported
                                ? 'border-leaf-200 bg-leaf-50 text-leaf-700'
                                : 'border-brand-200 bg-brand-50 text-ink-muted'
                            }`}>
                              {item.nutritionSupported ? 'Có dữ liệu tính toán' : 'Chưa hỗ trợ tính'}
                            </span>
                          </div>
                          <p className="mt-1 flex flex-wrap items-center gap-x-2 text-xs text-ink-muted">
                            <span>Nguồn: <strong className="font-medium text-ink-soft">{item.sourceName}</strong></span>
                            <span>·</span>
                            <span>Ngày tham khảo: {item.referenceDate}</span>
                            <span>·</span>
                            <span>{item.recipeUsageCount ?? 0} công thức đang dùng</span>
                            {item.sourceUrl && (
                              <>
                                <span>·</span>
                                <a
                                  href={item.sourceUrl}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="inline-flex items-center gap-0.5 font-medium text-brand-600 hover:underline"
                                >
                                  Liên kết nguồn <ExternalLink className="h-3 w-3" />
                                </a>
                              </>
                            )}
                          </p>
                          <p className="mt-2 flex flex-wrap gap-x-3 gap-y-1 text-xs text-ink-soft" aria-label={`Tóm tắt dinh dưỡng ${item.name}`}>
                            <span>{item.nutrition.energyKcalPer100g ?? '—'} kcal</span>
                            <span>Đạm {item.nutrition.proteinGramsPer100g ?? '—'} g</span>
                            <span>Carb {item.nutrition.carbohydrateGramsPer100g ?? '—'} g</span>
                            <span>Béo {item.nutrition.totalFatGramsPer100g ?? '—'} g</span>
                          </p>
                        </div>
                        <div className="flex flex-wrap gap-2">
                          <Button size="sm" variant="ghost" onClick={() => void showIngredientDetails(item)} aria-label={`Xem chi tiết ${item.name}`}>
                            Chi tiết
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => void showRecipeReferences(item)}
                            aria-label={`Xem công thức dùng ${item.name}`}
                          >
                            Xem công thức
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => editNutrition(item)}
                            aria-label={`Sửa dinh dưỡng ${item.name}`}
                          >
                            Sửa dinh dưỡng
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => editIngredient(item)}
                            aria-label={`Sửa ${item.name}`}
                          >
                            <Pencil className="h-3.5 w-3.5" /> Sửa thông tin
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            onClick={() => toggleIngredient(item)}
                            disabled={saving}
                            aria-label={`${item.active ? 'Ngừng sử dụng' : 'Bật lại'} ${item.name}`}
                          >
                            {item.active ? 'Ngừng sử dụng' : 'Kích hoạt lại'}
                          </Button>
                        </div>
                      </Card>
                    ))
                  )}
                  {totalElements > 0 && (
                    <div className="flex flex-wrap items-center justify-between gap-3 pt-2 text-sm text-ink-muted">
                      <span>{totalElements} nguyên liệu · Trang {ingredientFilters.page + 1}/{totalPages}</span>
                      <div className="flex gap-2">
                        <Button type="button" variant="outline" size="sm" disabled={loading || ingredientFilters.page === 0} onClick={() => changeIngredientPage(ingredientFilters.page - 1)}>Trang trước</Button>
                        <Button type="button" variant="outline" size="sm" disabled={loading || ingredientFilters.page + 1 >= totalPages} onClick={() => changeIngredientPage(ingredientFilters.page + 1)}>Trang sau</Button>
                      </div>
                    </div>
                  )}
                </div>
              </div>
              {detailTarget ? (
                <Card className="h-fit p-5 sm:p-6 border-brand-200/80 bg-paper shadow-xs">
                  <div className="flex items-start justify-between gap-3 border-b border-brand-100 pb-3">
                    <div>
                      <h2 className="text-lg font-extrabold text-ink">Chi tiết · {detailTarget.name}</h2>
                      <p className="mt-1 text-xs text-ink-muted">Thông tin dinh dưỡng trên 100 g và nguồn tham khảo.</p>
                    </div>
                    <Button type="button" variant="ghost" size="sm" onClick={() => setDetailTarget(null)}>Đóng</Button>
                  </div>
                  <dl className="mt-4 grid grid-cols-2 gap-3 text-sm">
                    {nutritionFields.map(({ key, label, unit }) => (
                      <div key={key} className="rounded-xl bg-brand-50/70 p-3">
                        <dt className="text-xs text-ink-muted">{label}</dt>
                        <dd className="mt-1 font-semibold text-ink">{detailTarget.nutrition[key] ?? 'Chưa có'}{detailTarget.nutrition[key] === null ? '' : ` ${unit}`}</dd>
                      </div>
                    ))}
                  </dl>
                  <div className="mt-4 space-y-2 border-t border-brand-100 pt-4 text-sm">
                    <p><span className="text-ink-muted">Nhóm:</span> {detailTarget.ingredientGroup}</p>
                    <p><span className="text-ink-muted">Nguồn:</span> {detailTarget.sourceName}</p>
                    <p><span className="text-ink-muted">Ngày đối chiếu:</span> {detailTarget.referenceDate}</p>
                    <p><span className="text-ink-muted">Trạng thái:</span> {detailTarget.active ? 'ACTIVE' : 'INACTIVE'} · {detailTarget.nutritionSupported ? 'Có hỗ trợ tính' : 'Chưa hỗ trợ tính'}</p>
                    <p><span className="text-ink-muted">Công thức đang tham chiếu:</span> {detailTarget.recipeUsageCount ?? 0}</p>
                    {detailTarget.sourceUrl && <a href={detailTarget.sourceUrl} target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 text-brand-700 hover:underline">Mở nguồn tham khảo <ExternalLink className="h-3 w-3" /></a>}
                  </div>
                </Card>
              ) : referencesTarget ? (
                <Card className="h-fit p-5 sm:p-6 border-brand-200/80 bg-paper shadow-xs">
                  <div className="flex items-start justify-between gap-3 border-b border-brand-100 pb-3">
                    <div>
                      <h2 className="text-lg font-extrabold text-ink">Công thức dùng {referencesTarget.name}</h2>
                      <p className="mt-1 text-xs text-ink-muted">Danh sách được tải từ Backend để kiểm tra các liên kết trước khi ngừng sử dụng nguyên liệu.</p>
                    </div>
                    <Button type="button" variant="ghost" size="sm" onClick={() => setReferencesTarget(null)}>Đóng</Button>
                  </div>
                  {referencesLoading ? (
                    <p className="mt-4 text-sm text-ink-muted">Đang tải công thức…</p>
                  ) : error ? null : recipeReferences.length === 0 ? (
                    <p className="mt-4 text-sm text-ink-muted">Chưa có công thức nào tham chiếu nguyên liệu này.</p>
                  ) : (
                    <ul className="mt-4 space-y-2">
                      {recipeReferences.map((recipe) => (
                        <li key={recipe.recipeId} className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-brand-100 p-3">
                          <span><span className="font-semibold text-ink">{recipe.title}</span><span className="mt-1 block text-xs text-ink-muted">{recipe.publishedAt ? new Date(recipe.publishedAt).toLocaleDateString('vi-VN') : 'Chưa công khai'}</span></span>
                          <span className="rounded-full bg-brand-50 px-2.5 py-1 text-xs font-medium text-brand-800">{recipeStatusLabel(recipe.status)}</span>
                        </li>
                      ))}
                    </ul>
                  )}
                  {recipeReferenceTotal > 0 && <div className="mt-4 flex items-center justify-between border-t border-brand-100 pt-3 text-xs text-ink-muted"><span>{recipeReferenceTotal} công thức · Trang {recipeReferencePage + 1}/{recipeReferencePages}</span><div className="flex gap-2"><Button size="sm" variant="outline" disabled={referencesLoading || recipeReferencePage === 0} onClick={() => void changeRecipeReferencePage(recipeReferencePage - 1)}>Trang trước</Button><Button size="sm" variant="outline" disabled={referencesLoading || recipeReferencePage + 1 >= recipeReferencePages} onClick={() => void changeRecipeReferencePage(recipeReferencePage + 1)}>Trang sau</Button></div></div>}
                </Card>
              ) : nutritionTarget ? (
                <Card className="h-fit p-5 sm:p-6 border-brand-200/80 bg-paper shadow-xs">
                  <div className="flex flex-wrap items-start justify-between gap-3 border-b border-brand-100 pb-3">
                    <div>
                      <h2 className="text-lg font-extrabold text-ink">Dinh dưỡng · {nutritionTarget.name}</h2>
                      <p className="mt-1 text-xs text-ink-muted">Nhập số liệu trên 100 g. Để trống nếu chưa biết; không nhập 0 thay cho dữ liệu thiếu.</p>
                    </div>
                    <span className={`rounded-full border px-2.5 py-0.5 text-xs font-semibold ${
                      nutritionEnabled
                        ? 'border-leaf-200 bg-leaf-50 text-leaf-700'
                        : 'border-brand-200 bg-brand-50 text-ink-muted'
                    }`}>
                      {nutritionEnabled ? 'Đang hỗ trợ tính' : 'Chưa hỗ trợ tính'}
                    </span>
                  </div>
                  <form onSubmit={submitNutrition} className="mt-4 space-y-3">
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
                    <div className="flex flex-wrap gap-2 pt-2">
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
              ) : ingredientEditorOpen ? <Card className="h-fit p-5 sm:p-6 border-brand-200/80 bg-paper shadow-xs">
                <div className="flex items-center justify-between pb-3 border-b border-brand-100">
                  <h2 className="text-lg font-extrabold text-ink">
                    {ingredientId === null ? 'Thêm nguyên liệu' : 'Sửa nguyên liệu'}
                  </h2>
                  <span className="text-xs font-semibold text-brand-700 bg-brand-50 px-2.5 py-0.5 rounded-full border border-brand-200/60">
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
                      {catalogOptions.groups.map((group) => <option key={group} value={group}>{group}</option>)}
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
                      {catalogOptions.sources.map((source) => <option key={source} value={source}>{source}</option>)}
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
                  <div className="flex flex-wrap gap-2 pt-2">
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
              </Card> : <Card className="h-fit border-dashed border-brand-200 bg-brand-50/30 p-6 sm:p-7">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-paper text-brand-700 shadow-2xs">
                  <Salad className="h-5 w-5" />
                </div>
                <h2 className="mt-4 text-lg font-bold text-ink">Quản lý một nguyên liệu</h2>
                <p className="mt-2 text-sm leading-6 text-ink-muted">
                  Chọn <strong className="text-ink">Sửa thông tin</strong> hoặc <strong className="text-ink">Dinh dưỡng</strong> trên một dòng để cập nhật. Trước khi ngừng sử dụng, xem các công thức đang tham chiếu để giữ nguyên liên kết cũ.
                </p>
              </Card>}
          </section>

        </>
      )}
    </PageContainer>
  );
}
