import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Clock, Search, Users, X } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, EmptyState } from '../components/ui';
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

export function Explore() {
  const [params, setParams] = useSearchParams();
  const [query, setQuery] = useState(params.get('q') ?? '');
  const [page, setPage] = useState(0);
  const [filters, setFilters] = useState<ExploreFilters>(initialFilters);
  const [options, setOptions] = useState<RecipeFormOptions | null>(null);
  const [optionsError, setOptionsError] = useState(false);
  const [ingredientQuery, setIngredientQuery] = useState('');
  const [ingredientOptions, setIngredientOptions] = useState<IngredientOption[]>([]);
  const [ingredientError, setIngredientError] = useState(false);
  const [result, setResult] = useState<RecipeSearchResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [retry, setRetry] = useState(0);

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
    setQuery(value);
    setPage(0);
    setParams(value ? { q: value } : {}, { replace: true });
  };

  const updateFilter = <K extends keyof ExploreFilters>(key: K, value: ExploreFilters[K]) => {
    setFilters((current) => ({ ...current, [key]: value }));
    setPage(0);
  };

  const toggleIngredient = (ingredient: IngredientOption, selected: boolean) => {
    const next = selected
      ? [...filters.ingredientIds, ingredient]
      : filters.ingredientIds.filter((item) => item.ingredientId !== ingredient.ingredientId);
    updateFilter('ingredientIds', next);
  };

  const resetFilters = () => {
    setFilters(initialFilters);
    setPage(0);
    updateQuery('');
    setIngredientQuery('');
    setIngredientOptions([]);
  };

  return <PageContainer className="py-8">
    <div className="mb-6">
      <p className="mb-1 text-xs font-bold uppercase tracking-[0.15em] text-brand-600">Khám phá</p>
      <h1 className="text-3xl font-extrabold tracking-tight text-ink">Khám phá công thức chay</h1>
      <p className="mt-1 text-ink-muted">Công thức đã xuất bản từ Backend Mâm Xanh.</p>
    </div>
    <div className="mb-5 flex items-center gap-2 rounded-xl border border-brand-200 bg-white px-3">
      <Search className="h-5 w-5 shrink-0 text-ink-muted" />
      <input aria-label="Từ khóa" value={query} onChange={(event) => updateQuery(event.target.value)} placeholder="Tìm món chay hoặc nguyên liệu..." className="w-full bg-transparent py-2.5 text-sm outline-none placeholder:text-ink-muted" />
      {query && <button onClick={() => updateQuery('')} aria-label="Xóa từ khóa" className="text-ink-muted hover:text-brand-600"><X className="h-4 w-4" /></button>}
    </div>

    <section aria-label="Bộ lọc công thức" className="mb-6 rounded-2xl border border-brand-100 bg-white p-4 sm:p-5">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <label className="grid gap-1.5 text-sm font-semibold text-ink">
          Trường phái ăn chay
          <select aria-label="Trường phái ăn chay" value={filters.vegetarianType} onChange={(event) => updateFilter('vegetarianType', event.target.value)} className="rounded-lg border border-brand-200 bg-white px-3 py-2 font-normal">
            <option value="">Tất cả</option>
            {options?.vegetarianTypes.map((item: Choice) => <option key={item.code} value={item.code}>{item.label}</option>)}
          </select>
        </label>
        <label className="grid gap-1.5 text-sm font-semibold text-ink">
          Thể loại món
          <select aria-label="Thể loại món" value={filters.dishCategory} onChange={(event) => updateFilter('dishCategory', event.target.value)} className="rounded-lg border border-brand-200 bg-white px-3 py-2 font-normal">
            <option value="">Tất cả</option>
            {options?.dishCategories.map((item: Choice) => <option key={item.code} value={item.code}>{item.label}</option>)}
          </select>
        </label>
        <label className="grid gap-1.5 text-sm font-semibold text-ink">
          Tổng thời gian tối đa
          <select aria-label="Tổng thời gian tối đa" value={filters.maxTotalTimeMinutes} onChange={(event) => updateFilter('maxTotalTimeMinutes', event.target.value)} className="rounded-lg border border-brand-200 bg-white px-3 py-2 font-normal">
            <option value="">Không giới hạn</option>
            <option value="15">≤ 15 phút</option>
            <option value="30">≤ 30 phút</option>
            <option value="60">≤ 60 phút</option>
          </select>
        </label>
        <label className="grid gap-1.5 text-sm font-semibold text-ink">
          Sắp xếp theo
          <select aria-label="Sắp xếp theo" value={filters.sort} onChange={(event) => updateFilter('sort', event.target.value as SortMode)} className="rounded-lg border border-brand-200 bg-white px-3 py-2 font-normal">
            {sortOptions.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}
          </select>
        </label>
      </div>

      {filters.sort === 'MOST_VIEWED' && <label className="mt-4 grid max-w-sm gap-1.5 text-sm font-semibold text-ink">
        Khung thời gian lượt xem
        <select aria-label="Khung thời gian lượt xem" value={filters.viewPeriod} onChange={(event) => updateFilter('viewPeriod', event.target.value as ViewPeriod)} className="rounded-lg border border-brand-200 bg-white px-3 py-2 font-normal">
          {viewPeriodOptions.map((item) => <option key={item.code} value={item.code}>{item.label}</option>)}
        </select>
      </label>}

      <div className="mt-4 grid gap-2 sm:max-w-xl">
        <label htmlFor="ingredient-search" className="text-sm font-semibold text-ink">Nguyên liệu (kết quả phải có đủ nguyên liệu đã chọn)</label>
        <input id="ingredient-search" value={ingredientQuery} onChange={(event) => setIngredientQuery(event.target.value)} placeholder="Nhập tên nguyên liệu để tìm trong danh mục..." className="rounded-lg border border-brand-200 px-3 py-2 text-sm" />
        {ingredientQuery.trim() && <div className="max-h-44 overflow-y-auto rounded-lg border border-brand-100 p-2" aria-label="Kết quả nguyên liệu">
          {ingredientError ? <p role="alert" className="p-2 text-sm text-red-700">Không tải được danh mục nguyên liệu.</p>
            : ingredientOptions.length ? ingredientOptions.map((ingredient) => {
              const selected = filters.ingredientIds.some((item) => item.ingredientId === ingredient.ingredientId);
              return <label key={ingredient.ingredientId} className="flex cursor-pointer items-center gap-2 rounded-md px-2 py-1.5 text-sm hover:bg-brand-50">
                <input type="checkbox" checked={selected} onChange={(event) => toggleIngredient(ingredient, event.target.checked)} />
                {ingredient.name}
              </label>;
            }) : !ingredientError && <p className="p-2 text-sm text-ink-muted">Không tìm thấy nguyên liệu phù hợp.</p>}
        </div>}
        {filters.ingredientIds.length > 0 && <ul aria-label="Nguyên liệu đã chọn" className="flex flex-wrap gap-2">
          {filters.ingredientIds.map((ingredient) => <li key={ingredient.ingredientId}>
            <button type="button" onClick={() => toggleIngredient(ingredient, false)} aria-label={`Bỏ ${ingredient.name}`} className="rounded-full bg-brand-50 px-3 py-1 text-xs font-semibold text-brand-800 hover:bg-brand-100">{ingredient.name} ×</button>
          </li>)}
        </ul>}
      </div>
      {optionsError && <p role="alert" className="mt-3 text-sm text-red-700">Không tải được tùy chọn bộ lọc. Hãy thử lại.</p>}
      <div className="mt-4 flex flex-wrap items-center gap-3">
        <Button variant="outline" onClick={() => setRetry((current) => current + 1)}>Tải lại dữ liệu</Button>
        <Button variant="secondary" onClick={resetFilters}>Đặt lại bộ lọc</Button>
      </div>
    </section>

    {loading ? <p role="status" className="py-12 text-center text-sm text-ink-muted">Đang tải công thức...</p>
      : error ? <EmptyState title="Không tải được công thức" description="Không thể kết nối Backend. Kiểm tra Backend rồi thử lại." action={<Button variant="secondary" onClick={() => setRetry((current) => current + 1)}>Thử lại</Button>} />
      : result && result.items.length > 0 ? <>
        <p className="mb-4 text-sm text-ink-muted"><strong className="text-ink">{result.totalElements}</strong> công thức</p>
        <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">{result.items.map((recipe: RecipePost) => {
          const image = recipe.media.find((item) => item.cover) ?? recipe.media[0];
          return <Link key={recipe.id} to={`/cong-thuc/id/${recipe.id}`} className="group overflow-hidden rounded-2xl border border-brand-100 bg-white transition-all hover:-translate-y-1 hover:shadow-xl">
            <div className="aspect-[4/3] bg-brand-50">{image ? <img src={image.url} alt={recipe.title} className="h-full w-full object-cover transition-transform group-hover:scale-105" /> : <div className="flex h-full items-center justify-center text-sm text-ink-muted">Ảnh công thức</div>}</div>
            <div className="p-4"><div className="flex flex-wrap gap-2 text-xs font-semibold text-brand-700"><span className="rounded-full bg-brand-50 px-2.5 py-1">{recipe.vegetarianType}</span><span className="rounded-full bg-brand-50 px-2.5 py-1">{recipe.dishCategory}</span>{filters.sort === 'MOST_LIKED' && <span aria-label="Tỷ lệ yêu thích" className="rounded-full bg-brand-50 px-2.5 py-1">{recipe.likePercentage === null ? 'Mới' : `👍 ${recipe.likePercentage}%`}</span>}</div><h2 className="mt-3 line-clamp-2 font-bold text-ink group-hover:text-brand-700">{recipe.title}</h2><p className="mt-2 flex items-center gap-4 text-xs text-ink-muted"><span className="inline-flex items-center gap-1"><Clock className="h-3.5 w-3.5" />{recipe.prepTimeMin + recipe.cookTimeMin} phút</span><span className="inline-flex items-center gap-1"><Users className="h-3.5 w-3.5" />{recipe.servings} khẩu phần</span></p><p className="mt-3 border-t border-brand-50 pt-3 text-xs text-ink-soft">Tác giả: {recipe.authorName}</p></div>
          </Link>;
        })}</div>
        {result.totalPages > 1 && <div className="mt-8 flex items-center justify-center gap-4"><Button variant="outline" disabled={page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>Trang trước</Button><span className="text-sm text-ink-muted">Trang {page + 1}/{result.totalPages}</span><Button variant="outline" disabled={page + 1 >= result.totalPages} onClick={() => setPage((current) => current + 1)}>Trang sau</Button></div>}
      </> : <EmptyState title="Không tìm thấy công thức phù hợp" description="Thử thay đổi từ khóa hoặc tiêu chí lọc, hoặc đặt lại bộ lọc để xem công thức khác." action={<Button variant="secondary" onClick={resetFilters}>Đặt lại bộ lọc</Button>} />}
  </PageContainer>;
}
