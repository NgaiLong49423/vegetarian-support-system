import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ArrowDownUp, Check, RotateCcw, Search, SlidersHorizontal, X } from 'lucide-react';
import { Button } from './ui';
import { recipesApi } from '../api/recipes';
import type { Choice, IngredientOption, RecipeBrowseResult, RecipeFormOptions, RecipeSearchParams } from '../api/recipes';

// FR-08 public recipe search and filters, shared by the Explore page and the FR-60 comparison picker.
// The keyword, diet, dish category, sort and selected ingredients (FR-62 links preset ingredientId/ingredientName)
// live in the URL; other query parameters are left untouched.

type SortMode = RecipeSearchParams['sort'];
type ViewPeriod = RecipeSearchParams['viewPeriod'];
type RecipeFilters = {
  vegetarianType: string;
  dishCategory: string;
  ingredientIds: IngredientOption[];
  maxTotalTimeMinutes: string;
  sort: SortMode;
  viewPeriod: ViewPeriod;
};

const initialFilters: RecipeFilters = {
  vegetarianType: '', dishCategory: '', ingredientIds: [], maxTotalTimeMinutes: '',
  sort: 'NEWEST', viewPeriod: 'ALL_TIME',
};

function ingredientsFrom(params: URLSearchParams): IngredientOption[] {
  const ids = params.getAll('ingredientId');
  const names = params.getAll('ingredientName');
  return ids.flatMap((rawId, index) => {
    const ingredientId = Number(rawId);
    return Number.isSafeInteger(ingredientId) && ingredientId > 0
      ? [{ ingredientId, name: names[index] || `Nguyên liệu ${ingredientId}` }]
      : [];
  });
}

const URL_KEYS = ['q', 'vegetarianType', 'dishCategory', 'sort', 'ingredientId', 'ingredientName'];

const sortOptions: Array<{ code: SortMode; label: string }> = [
  { code: 'NEWEST', label: 'Mới nhất' },
  { code: 'MOST_LIKED', label: 'Được yêu thích nhất' },
  { code: 'MOST_VIEWED', label: 'Xem nhiều nhất' },
  { code: 'MOST_COMMENTED', label: 'Nhiều bình luận nhất' },
  { code: 'MOST_ACTIVE', label: 'Hoạt động sôi nổi nhất' },
  { code: 'TRENDING', label: 'Thịnh hành' },
];

const viewPeriodOptions: Array<{ code: ViewPeriod; label: string }> = [
  { code: 'ALL_TIME', label: 'Tất cả thời gian' },
  { code: 'LAST_24_HOURS', label: '24 giờ qua' },
  { code: 'LAST_7_DAYS', label: '7 ngày qua' },
  { code: 'LAST_30_DAYS', label: '30 ngày qua' },
];

const controlClass = 'mt-1.5 w-full rounded-xl border border-brand-100 bg-paper px-3 py-2.5 text-sm text-ink shadow-sm outline-none transition focus-visible:border-brand-500 focus-visible:ring-4 focus-visible:ring-brand-100';

function sortFrom(params: URLSearchParams): SortMode {
  return sortOptions.some((item) => item.code === params.get('sort')) ? params.get('sort') as SortMode : 'NEWEST';
}

export function useRecipeSearch(pageSize: number) {
  const [params, setParams] = useSearchParams();
  const expectedSearchRef = useRef(params.toString());
  const query = params.get('q') ?? '';
  const presetIngredientsKey = JSON.stringify(params.getAll('ingredientId').map((rawId, index) => ({
    ingredientId: Number(rawId),
    name: params.getAll('ingredientName')[index] ?? '',
  })));
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<RecipeFilters>(() => ({ ...initialFilters, vegetarianType: params.get('vegetarianType') ?? '', dishCategory: params.get('dishCategory') ?? '', sort: sortFrom(params), ingredientIds: ingredientsFrom(params) }));
  const [options, setOptions] = useState<RecipeFormOptions | null>(null);
  const [optionsError, setOptionsError] = useState(false);
  const [ingredientQuery, setIngredientQuery] = useState('');
  const [ingredientOptions, setIngredientOptions] = useState<IngredientOption[]>([]);
  const [ingredientError, setIngredientError] = useState(false);
  const [result, setResult] = useState<RecipeBrowseResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [attempt, setAttempt] = useState(0);
  const [presetIngredientError, setPresetIngredientError] = useState(false);

  useEffect(() => {
    const requested = (JSON.parse(presetIngredientsKey) as Array<{ ingredientId: number; name: string }>)
      .filter((item) => Number.isSafeInteger(item.ingredientId) && item.ingredientId > 0 && item.name.trim());
    if (requested.length === 0) {
      setPresetIngredientError(false);
      return;
    }
    let active = true;
    Promise.all(requested.map(({ ingredientId, name }) => recipesApi.searchIngredients(name.trim())
      .then((items) => items.find((item) => item.ingredientId === ingredientId) ?? null)))
      .then((matches) => {
        if (!active) return;
        const resolved = matches.filter((item): item is IngredientOption => item !== null);
        setFilters((current) => {
          const byId = new Map(current.ingredientIds.map((item) => [item.ingredientId, item]));
          resolved.forEach((item) => byId.set(item.ingredientId, item));
          return { ...current, ingredientIds: [...byId.values()] };
        });
        setPresetIngredientError(resolved.length !== requested.length);
      })
      .catch(() => { if (active) setPresetIngredientError(true); });
    return () => { active = false; };
  }, [presetIngredientsKey]);

  useEffect(() => {
    const currentSearch = params.toString();
    if (currentSearch === expectedSearchRef.current) return;

    expectedSearchRef.current = currentSearch;
    const vegetarianType = params.get('vegetarianType') ?? '';
    const dishCategory = params.get('dishCategory') ?? '';
    const sort = sortFrom(params);
    const ingredientIds = ingredientsFrom(params);
    setFilters((current) => current.vegetarianType === vegetarianType
      && current.dishCategory === dishCategory
      && current.sort === sort
      && current.ingredientIds.length === ingredientIds.length
      && current.ingredientIds.every((item, index) => item.ingredientId === ingredientIds[index].ingredientId)
      ? current
      : { ...current, vegetarianType, dishCategory, sort, ingredientIds });
    setPage(0);
  }, [params]);

  useEffect(() => {
    let active = true;
    recipesApi.getFormOptions()
      .then((data) => { if (active) { setOptions(data); setOptionsError(false); } })
      .catch(() => { if (active) setOptionsError(true); });
    return () => { active = false; };
  }, [attempt]);

  useEffect(() => {
    let active = true;
    const term = ingredientQuery.trim();
    if (!term) {
      setIngredientOptions([]);
      setIngredientError(false);
      return () => { active = false; };
    }
    const timer = window.setTimeout(() => {
      recipesApi.searchIngredients(term)
        .then((items) => { if (active) { setIngredientOptions(items); setIngredientError(false); } })
        .catch(() => { if (active) { setIngredientOptions([]); setIngredientError(true); } });
    }, 200);
    return () => { active = false; window.clearTimeout(timer); };
  }, [ingredientQuery]);

  useEffect(() => {
    let active = true;
    const timer = window.setTimeout(() => {
      setLoading(true);
      setError(false);
      recipesApi.discoverPublic({
        keyword: query.trim(), page, size: pageSize, sort: filters.sort, viewPeriod: filters.viewPeriod,
        vegetarianType: filters.vegetarianType || undefined,
        dishCategory: filters.dishCategory || undefined,
        ingredientIds: filters.ingredientIds.map((ingredient) => ingredient.ingredientId),
        maxTotalTimeMinutes: filters.maxTotalTimeMinutes ? Number(filters.maxTotalTimeMinutes) : undefined,
      })
        .then((data) => { if (active) setResult(data); })
        .catch(() => { if (active) { setResult(null); setError(true); } })
        .finally(() => { if (active) setLoading(false); });
    }, query.trim() ? 250 : 0);
    return () => { active = false; window.clearTimeout(timer); };
  }, [query, page, pageSize, filters, attempt]);

  const updateQuery = (value: string) => {
    setPage(0);
    const next = new URLSearchParams(params);
    if (value) next.set('q', value); else next.delete('q');
    expectedSearchRef.current = next.toString();
    setParams(next, { replace: true });
  };

  const updateFilter = <K extends keyof RecipeFilters>(key: K, value: RecipeFilters[K]) => {
    setFilters((current) => ({ ...current, [key]: value }));
    setPage(0);
    if (key === 'vegetarianType' || key === 'dishCategory' || key === 'sort') {
      const next = new URLSearchParams(params);
      if (value) next.set(key, String(value)); else next.delete(key);
      expectedSearchRef.current = next.toString();
      setParams(next, { replace: true });
    }
  };

  const toggleIngredient = (ingredient: IngredientOption, selected: boolean) => {
    const next = selected
      ? [...filters.ingredientIds, ingredient]
      : filters.ingredientIds.filter((item) => item.ingredientId !== ingredient.ingredientId);
    updateFilter('ingredientIds', next);
    // Keep the selection in the URL; otherwise the next URL change re-reads removed ingredients from it.
    const nextParams = new URLSearchParams(params);
    nextParams.delete('ingredientId');
    nextParams.delete('ingredientName');
    next.forEach((item) => {
      nextParams.append('ingredientId', String(item.ingredientId));
      nextParams.append('ingredientName', item.name);
    });
    expectedSearchRef.current = nextParams.toString();
    setParams(nextParams, { replace: true });
  };

  const resetFilters = () => {
    setFilters(initialFilters);
    setPage(0);
    const next = new URLSearchParams(params);
    URL_KEYS.forEach((key) => next.delete(key));
    expectedSearchRef.current = next.toString();
    setParams(next, { replace: true });
    setIngredientQuery('');
    setIngredientOptions([]);
  };

  const retry = () => setAttempt((current) => current + 1);

  const activeFilterCount = Number(Boolean(filters.vegetarianType))
    + Number(Boolean(filters.dishCategory))
    + filters.ingredientIds.length
    + Number(Boolean(filters.maxTotalTimeMinutes));

  const filterPanelProps = {
    filters,
    options,
    optionsError,
    ingredientQuery,
    ingredientOptions,
    ingredientError,
    updateFilter,
    setIngredientQuery,
    toggleIngredient,
    retry,
    resetFilters,
  };

  return {
    query, updateQuery, page, setPage, filters, updateFilter, toggleIngredient, resetFilters, retry,
    activeFilterCount, options, result, loading, error, presetIngredientError, filterPanelProps,
  };
}

type RecipeSearch = ReturnType<typeof useRecipeSearch>;

export function SortControls({ search }: { search: RecipeSearch }) {
  const { filters, updateFilter } = search;
  return <div className="flex flex-wrap items-center gap-2">
    <label className="sr-only" htmlFor="recipe-sort">Sắp xếp công thức</label>
    <span className="hidden items-center gap-1.5 text-xs font-medium text-ink-muted sm:inline-flex"><ArrowDownUp className="h-3.5 w-3.5" />Sắp xếp</span>
    <select id="recipe-sort" aria-label="Sắp xếp theo" value={filters.sort} onChange={(event) => updateFilter('sort', event.target.value as SortMode)} className="rounded-xl border border-brand-100 bg-paper px-3 py-2 text-sm text-ink outline-none focus-visible:ring-4 focus-visible:ring-brand-100">
      {sortOptions.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}
    </select>
    {filters.sort === 'MOST_VIEWED' && <select aria-label="Khung thời gian lượt xem" value={filters.viewPeriod} onChange={(event) => updateFilter('viewPeriod', event.target.value as ViewPeriod)} className="rounded-xl border border-brand-100 bg-paper px-3 py-2 text-sm text-ink outline-none focus-visible:ring-4 focus-visible:ring-brand-100">
      {viewPeriodOptions.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}
    </select>}
  </div>;
}

export function ActiveFilters({ search }: { search: RecipeSearch }) {
  const { filters, options, updateFilter, toggleIngredient, resetFilters, activeFilterCount } = search;
  if (activeFilterCount === 0) return null;
  return <div className="mb-4 flex flex-wrap items-center gap-2" aria-label="Bộ lọc đang áp dụng">
    {filters.vegetarianType && <FilterChip label={options?.vegetarianTypes.find((item) => item.code === filters.vegetarianType)?.label ?? filters.vegetarianType} onRemove={() => updateFilter('vegetarianType', '')} />}
    {filters.dishCategory && <FilterChip label={options?.dishCategories.find((item) => item.code === filters.dishCategory)?.label ?? filters.dishCategory} onRemove={() => updateFilter('dishCategory', '')} />}
    {filters.maxTotalTimeMinutes && <FilterChip label={`Tối đa ${filters.maxTotalTimeMinutes} phút`} onRemove={() => updateFilter('maxTotalTimeMinutes', '')} />}
    {filters.ingredientIds.map((ingredient) => <FilterChip key={ingredient.ingredientId} label={ingredient.name} onRemove={() => toggleIngredient(ingredient, false)} />)}
    <button type="button" onClick={resetFilters} className="inline-flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs font-semibold text-brand-700 hover:bg-brand-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"><RotateCcw className="h-3.5 w-3.5" />Xóa tất cả</button>
  </div>;
}

export function ResultPagination({ search }: { search: RecipeSearch }) {
  const { page, setPage, result } = search;
  if (!result || (page === 0 && !result.hasNext)) return null;
  return <nav aria-label="Phân trang công thức" className="mt-7 flex flex-wrap items-center justify-center gap-3">
    <Button variant="outline" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>Trang trước</Button>
    <span aria-live="polite" className="min-w-24 text-center text-sm text-ink-muted">Trang {page + 1}</span>
    <Button variant="outline" disabled={!result.hasNext} onClick={() => setPage((current) => current + 1)}>Trang sau</Button>
  </nav>;
}

type FilterPanelProps = RecipeSearch['filterPanelProps'] & { idSuffix: 'mobile' | 'desktop' };

export function FilterPanel({ filters, options, optionsError, ingredientQuery, ingredientOptions, ingredientError, updateFilter, setIngredientQuery, toggleIngredient, retry, resetFilters, idSuffix }: FilterPanelProps) {
  return <section aria-label="Bộ lọc công thức" className="rounded-2xl border border-brand-100 bg-paper p-4 shadow-sm sm:p-5">
    <div className="mb-4 flex items-center justify-between gap-3">
      <div><h2 className="font-bold text-ink">Lọc công thức</h2><p className="mt-0.5 text-xs text-ink-muted">Các điều kiện được kết hợp cùng lúc.</p></div>
      <SlidersHorizontal className="h-4 w-4 text-brand-600" aria-hidden="true" />
    </div>
    <div className="space-y-4">
      <label className="block text-xs font-semibold text-ink">Trường phái ăn chay
        <select aria-label="Trường phái ăn chay" value={filters.vegetarianType} onChange={(event) => updateFilter('vegetarianType', event.target.value)} className={controlClass}>
          <option value="">Tất cả</option>
          {options?.vegetarianTypes.map((item: Choice) => <option key={item.code} value={item.code}>{item.label}</option>)}
        </select>
      </label>
      <label className="block text-xs font-semibold text-ink">Thể loại món
        <select aria-label="Thể loại món" value={filters.dishCategory} onChange={(event) => updateFilter('dishCategory', event.target.value)} className={controlClass}>
          <option value="">Tất cả</option>
          {options?.dishCategories.map((item: Choice) => <option key={item.code} value={item.code}>{item.label}</option>)}
        </select>
      </label>
      <label className="block text-xs font-semibold text-ink">Tổng thời gian tối đa
        <select aria-label="Tổng thời gian tối đa" value={filters.maxTotalTimeMinutes} onChange={(event) => updateFilter('maxTotalTimeMinutes', event.target.value)} className={controlClass}>
          <option value="">Không giới hạn</option>
          <option value="15">Tối đa 15 phút</option>
          <option value="30">Tối đa 30 phút</option>
          <option value="60">Tối đa 60 phút</option>
        </select>
      </label>
      <div>
        <label className="block text-xs font-semibold text-ink" htmlFor={`ingredient-search-${idSuffix}`}>Nguyên liệu <span className="font-normal text-ink-muted">(cần đủ tất cả)</span></label>
        <div className="relative mt-1.5">
          <Search aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-muted" />
          <input id={`ingredient-search-${idSuffix}`} aria-label="Nguyên liệu (kết quả phải có đủ nguyên liệu đã chọn)" value={ingredientQuery} onChange={(event) => setIngredientQuery(event.target.value)} placeholder="Tìm nguyên liệu..." className={`${controlClass} mt-0 pl-9`} />
        </div>
        {ingredientQuery.trim() && <div className="mt-2 max-h-44 overflow-y-auto rounded-xl border border-brand-100 p-1.5" aria-label="Kết quả nguyên liệu">
          {ingredientError ? <p role="alert" className="p-2 text-xs text-red-700">Không tải được danh mục nguyên liệu.</p>
            : ingredientOptions.length ? ingredientOptions.map((ingredient) => {
              const selected = filters.ingredientIds.some((item) => item.ingredientId === ingredient.ingredientId);
              return <label key={ingredient.ingredientId} className="flex cursor-pointer items-center gap-2 rounded-lg px-2 py-2 text-sm text-ink hover:bg-brand-50">
                <input type="checkbox" checked={selected} onChange={(event) => toggleIngredient(ingredient, event.target.checked)} className="h-4 w-4 rounded border-brand-300 text-brand-600 focus:ring-brand-500" />
                <span className="min-w-0 flex-1 truncate">{ingredient.name}</span>
                {selected && <Check aria-hidden="true" className="h-4 w-4 text-leaf-600" />}
              </label>;
            }) : !ingredientError && <p className="p-2 text-xs text-ink-muted">Không tìm thấy nguyên liệu phù hợp.</p>}
        </div>}
        {filters.ingredientIds.length > 0 && <ul aria-label="Nguyên liệu đã chọn" className="mt-2 flex flex-wrap gap-1.5">
          {filters.ingredientIds.map((ingredient) => <li key={ingredient.ingredientId}><button type="button" onClick={() => toggleIngredient(ingredient, false)} aria-label={`Bỏ lọc nguyên liệu ${ingredient.name}`} className="inline-flex items-center gap-1 rounded-full bg-leaf-50 px-2.5 py-1 text-xs font-medium text-leaf-700 hover:bg-leaf-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600">{ingredient.name}<X className="h-3 w-3" /></button></li>)}
        </ul>}
      </div>
      {optionsError && <div role="alert" className="rounded-xl border border-red-100 bg-red-50 p-3 text-xs text-red-800"><p>Không tải được tùy chọn bộ lọc.</p><button type="button" onClick={retry} className="mt-1 font-bold underline focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-600">Thử tải lại</button></div>}
      <p className="rounded-xl bg-brand-50/70 p-3 text-xs leading-5 text-ink-muted">Thời gian tối đa tính bằng thời gian chuẩn bị cộng thời gian nấu.</p>
      <div className="flex items-center gap-2 border-t border-brand-50 pt-3">
        <Button variant="secondary" size="sm" className="flex-1" onClick={resetFilters}><RotateCcw className="h-3.5 w-3.5" />Đặt lại bộ lọc</Button>
        <span className="sr-only">Kết quả cập nhật tự động khi thay đổi bộ lọc</span>
      </div>
    </div>
  </section>;
}

function FilterChip({ label, onRemove }: { label: string; onRemove: () => void }) {
  return <button type="button" onClick={onRemove} className="inline-flex max-w-full items-center gap-1.5 rounded-full border border-brand-100 bg-paper px-3 py-1.5 text-xs font-medium text-ink-soft transition-colors hover:border-brand-300 hover:bg-brand-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500">
    <span className="truncate">{label}</span><X className="h-3.5 w-3.5 shrink-0 text-ink-muted" />
  </button>;
}
