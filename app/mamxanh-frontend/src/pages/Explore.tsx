import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import {
  ArrowDownUp,
  Check,
  ChevronDown,
  Filter,
  RotateCcw,
  Search,
  SlidersHorizontal,
  X,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, EmptyState } from '../components/ui';
import { PublicRecipeCard, PublicRecipeSkeleton } from '../components/PublicRecipeCard';
import { recipesApi } from '../api/recipes';
import type { Choice, IngredientOption, RecipeFormOptions, RecipePost, RecipeSearchResult } from '../api/recipes';

type SortMode = 'NEWEST' | 'MOST_LIKED' | 'MOST_VIEWED' | 'MOST_COMMENTED' | 'MOST_ACTIVE' | 'TRENDING';
type ViewPeriod = 'ALL_TIME' | 'LAST_24_HOURS' | 'LAST_7_DAYS' | 'LAST_30_DAYS';
type ExploreFilters = {
  vegetarianType: string;
  dishCategory: string;
  ingredientIds: IngredientOption[];
  maxTotalTimeMinutes: string;
  sort: SortMode;
  viewPeriod: ViewPeriod;
};

const initialFilters: ExploreFilters = {
  vegetarianType: '', dishCategory: '', ingredientIds: [], maxTotalTimeMinutes: '',
  sort: 'NEWEST', viewPeriod: 'ALL_TIME',
};

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

export function Explore() {
  const [params, setParams] = useSearchParams();
  const expectedSearchRef = useRef(params.toString());
  const query = params.get('q') ?? '';
  const presetIngredientsKey = JSON.stringify(params.getAll('ingredientId').map((rawId, index) => ({
    ingredientId: Number(rawId),
    name: params.getAll('ingredientName')[index] ?? '',
  })));
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<ExploreFilters>(() => ({ ...initialFilters, vegetarianType: params.get('vegetarianType') ?? '', dishCategory: params.get('dishCategory') ?? '', sort: sortOptions.some((item) => item.code === params.get('sort')) ? params.get('sort') as SortMode : 'NEWEST' }));
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [options, setOptions] = useState<RecipeFormOptions | null>(null);
  const [optionsError, setOptionsError] = useState(false);
  const [ingredientQuery, setIngredientQuery] = useState('');
  const [ingredientOptions, setIngredientOptions] = useState<IngredientOption[]>([]);
  const [ingredientError, setIngredientError] = useState(false);
  const [result, setResult] = useState<RecipeSearchResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [retry, setRetry] = useState(0);
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
    const sort = sortOptions.some((item) => item.code === params.get('sort')) ? params.get('sort') as SortMode : 'NEWEST';
    setFilters((current) => current.vegetarianType === vegetarianType && current.dishCategory === dishCategory && current.sort === sort ? current : { ...current, vegetarianType, dishCategory, sort });
    setPage(0);
  }, [params]);

  useEffect(() => {
    let active = true;
    recipesApi.getFormOptions()
      .then((data) => { if (active) { setOptions(data); setOptionsError(false); } })
      .catch(() => { if (active) setOptionsError(true); });
    return () => { active = false; };
  }, [retry]);

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
      recipesApi.searchPublic({
        keyword: query.trim(), page, size: 12, sort: filters.sort, viewPeriod: filters.viewPeriod,
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
  }, [query, page, filters, retry]);

  const updateQuery = (value: string) => {
    setPage(0);
    const next = new URLSearchParams(params);
    if (value) next.set('q', value); else next.delete('q');
    expectedSearchRef.current = next.toString();
    setParams(next, { replace: true });
  };

  const updateFilter = <K extends keyof ExploreFilters>(key: K, value: ExploreFilters[K]) => {
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
    expectedSearchRef.current = '';
    setParams({}, { replace: true });
    setIngredientQuery('');
    setIngredientOptions([]);
  };

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
    retry: () => setRetry((current) => current + 1),
    resetFilters,
  };

  return (
    <PageContainer className="py-6">
      <header className="mb-5">
        <p className="text-xs text-ink-muted">Mâm Xanh / Công thức</p>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight text-ink">Khám phá công thức chay</h1>
        <p className="mt-1 text-xs leading-5 text-ink-muted">Tìm món phù hợp với nguyên liệu và thời gian bạn có.</p>
        {presetIngredientError && <p role="status" className="mt-2 text-sm text-amber-800">Không tìm thấy nguyên liệu được chọn trong danh mục hiện tại.</p>}

        <div className="mt-4 flex max-w-3xl items-center gap-2 rounded-full border border-brand-200 bg-paper px-4 py-1 transition-all duration-200 focus-within:border-brand-600 focus-within:ring-2 focus-within:ring-brand-500/20">
          <Search aria-hidden="true" className="h-5 w-5 shrink-0 text-brand-600" />
          <input
            aria-label="Từ khóa"
            value={query}
            onChange={(event) => updateQuery(event.target.value)}
            placeholder="Tìm tên món hoặc nguyên liệu..."
            className="min-w-0 flex-1 bg-transparent py-2 text-sm text-ink outline-none placeholder:text-ink-muted"
          />
          {query && <button type="button" onClick={() => updateQuery('')} aria-label="Xóa từ khóa" className="rounded-full p-1.5 text-ink-muted transition-colors hover:bg-brand-100 hover:text-brand-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"><X className="h-4 w-4" /></button>}
        </div>
      </header>

      <div className="mb-4 flex items-center justify-between gap-3 lg:hidden">
        <button
          type="button"
          aria-expanded={filtersOpen}
          aria-controls="mobile-recipe-filters"
          onClick={() => setFiltersOpen((open) => !open)}
          className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-brand-100 bg-paper px-3.5 text-sm font-semibold text-ink shadow-sm hover:border-brand-300 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-100"
        >
          <SlidersHorizontal className="h-4 w-4 text-brand-600" />
          Bộ lọc{activeFilterCount > 0 && <span className="rounded-full bg-brand-600 px-2 py-0.5 text-xs text-on-brand">{activeFilterCount}</span>}
          <ChevronDown className={`h-4 w-4 transition-transform ${filtersOpen ? 'rotate-180' : ''}`} />
        </button>
        <span className="text-xs text-ink-muted">Điều kiện lọc kết hợp với nhau</span>
      </div>

      {filtersOpen && <section id="mobile-recipe-filters" className="mb-5 lg:hidden"><FilterPanel {...filterPanelProps} idSuffix="mobile" /></section>}

      <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_250px]">
        <section aria-label="Kết quả công thức" className="min-w-0">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-100 bg-paper px-4 py-3 shadow-sm">
            <div>
              <p className="text-sm font-bold text-ink">{loading ? 'Đang tìm công thức…' : `${result?.totalElements ?? 0} công thức`}</p>
              {activeFilterCount > 1 && <p className="mt-0.5 text-xs text-ink-muted">Công thức cần đáp ứng tất cả điều kiện đang chọn.</p>}
            </div>
            <div className="flex flex-wrap items-center gap-2">
              <label className="sr-only" htmlFor="recipe-sort">Sắp xếp công thức</label>
              <span className="hidden items-center gap-1.5 text-xs font-medium text-ink-muted sm:inline-flex"><ArrowDownUp className="h-3.5 w-3.5" />Sắp xếp</span>
              <select id="recipe-sort" aria-label="Sắp xếp theo" value={filters.sort} onChange={(event) => updateFilter('sort', event.target.value as SortMode)} className="rounded-xl border border-brand-100 bg-paper px-3 py-2 text-sm text-ink outline-none focus-visible:ring-4 focus-visible:ring-brand-100">
                {sortOptions.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}
              </select>
              {filters.sort === 'MOST_VIEWED' && <select aria-label="Khung thời gian lượt xem" value={filters.viewPeriod} onChange={(event) => updateFilter('viewPeriod', event.target.value as ViewPeriod)} className="rounded-xl border border-brand-100 bg-paper px-3 py-2 text-sm text-ink outline-none focus-visible:ring-4 focus-visible:ring-brand-100">
                {viewPeriodOptions.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}
              </select>}
            </div>
          </div>

          {activeFilterCount > 0 && <div className="mb-4 flex flex-wrap items-center gap-2" aria-label="Bộ lọc đang áp dụng">
            {filters.vegetarianType && <FilterChip label={options?.vegetarianTypes.find((item) => item.code === filters.vegetarianType)?.label ?? filters.vegetarianType} onRemove={() => updateFilter('vegetarianType', '')} />}
            {filters.dishCategory && <FilterChip label={options?.dishCategories.find((item) => item.code === filters.dishCategory)?.label ?? filters.dishCategory} onRemove={() => updateFilter('dishCategory', '')} />}
            {filters.maxTotalTimeMinutes && <FilterChip label={`Tối đa ${filters.maxTotalTimeMinutes} phút`} onRemove={() => updateFilter('maxTotalTimeMinutes', '')} />}
            {filters.ingredientIds.map((ingredient) => <FilterChip key={ingredient.ingredientId} label={ingredient.name} onRemove={() => toggleIngredient(ingredient, false)} />)}
            <button type="button" onClick={resetFilters} className="inline-flex items-center gap-1 rounded-lg px-2 py-1.5 text-xs font-semibold text-brand-700 hover:bg-brand-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"><RotateCcw className="h-3.5 w-3.5" />Xóa tất cả</button>
          </div>}

          {loading ? <div role="status" aria-label="Đang tải công thức" className="grid grid-cols-2 gap-4 sm:grid-cols-3 xl:grid-cols-4"><PublicRecipeSkeleton count={8} /></div>
            : error ? <EmptyState title="Không tải được công thức" description="Kiểm tra kết nối rồi thử tải lại danh sách công thức." action={<Button variant="secondary" onClick={() => setRetry((current) => current + 1)}>Thử lại</Button>} />
              : result && result.items.length > 0 ? <>
                <div className="grid grid-cols-2 gap-x-4 gap-y-6 sm:grid-cols-3 xl:grid-cols-4">
                  {result.items.map((recipe: RecipePost) => <PublicRecipeCard key={recipe.id} recipe={recipe} options={options} />)}
                </div>
                {result.totalPages > 1 && <nav aria-label="Phân trang công thức" className="mt-7 flex flex-wrap items-center justify-center gap-3">
                  <Button variant="outline" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>Trang trước</Button>
                  <span aria-live="polite" className="min-w-24 text-center text-sm text-ink-muted">Trang {page + 1} / {result.totalPages}</span>
                  <Button variant="outline" disabled={page + 1 >= result.totalPages} onClick={() => setPage((current) => current + 1)}>Trang sau</Button>
                </nav>}
              </>
                : <EmptyState title="Không tìm thấy công thức phù hợp" description="Thử đổi từ khóa hoặc điều kiện lọc để xem thêm công thức." action={<Button variant="secondary" onClick={resetFilters}>Đặt lại tìm kiếm</Button>} />}
        </section>

        <aside className="sticky top-20 hidden lg:block">
          <div className="mb-3 flex items-center gap-2 px-1 text-sm font-bold text-ink"><Filter className="h-4 w-4 text-brand-600" />Bộ lọc tìm kiếm</div>
          <FilterPanel {...filterPanelProps} idSuffix="desktop" />
        </aside>
      </div>
    </PageContainer>
  );
}

type FilterPanelProps = {
  filters: ExploreFilters;
  options: RecipeFormOptions | null;
  optionsError: boolean;
  ingredientQuery: string;
  ingredientOptions: IngredientOption[];
  ingredientError: boolean;
  updateFilter: <K extends keyof ExploreFilters>(key: K, value: ExploreFilters[K]) => void;
  setIngredientQuery: (value: string) => void;
  toggleIngredient: (ingredient: IngredientOption, selected: boolean) => void;
  retry: () => void;
  resetFilters: () => void;
  idSuffix: 'mobile' | 'desktop';
};

function FilterPanel({ filters, options, optionsError, ingredientQuery, ingredientOptions, ingredientError, updateFilter, setIngredientQuery, toggleIngredient, retry, resetFilters, idSuffix }: FilterPanelProps) {
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
