import { useEffect, useState } from 'react';
import { ChefHat } from 'lucide-react';
import { recipesApi, type RecipeFormOptions, type RecipePost, type RecipeSearchParams } from '../api/recipes';
import { PublicRecipeCard, PublicRecipeSkeleton } from './PublicRecipeCard';
import { Button } from './ui';

export function IngredientRecipes({ ingredientId, name }: { ingredientId: number; name: string }) {
  const [query, setQuery] = useState<{ page: number; vegetarianType: string; sort: RecipeSearchParams['sort']; retry: number }>({
    page: 0, vegetarianType: '', sort: 'MOST_LIKED', retry: 0,
  });
  const [items, setItems] = useState<RecipePost[]>([]);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [options, setOptions] = useState<RecipeFormOptions | null>(null);
  const [optionsError, setOptionsError] = useState(false);
  const [optionsRetry, setOptionsRetry] = useState(0);

  useEffect(() => {
    let active = true;
    setOptionsError(false);
    recipesApi.getFormOptions().then((data) => { if (active) setOptions(data); })
      .catch(() => { if (active) setOptionsError(true); });
    return () => { active = false; };
  }, [optionsRetry]);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(false);
    if (query.page === 0) { setItems([]); setHasNext(false); }
    recipesApi.discoverPublic({ keyword: '', page: query.page, size: 9, sort: query.sort,
      viewPeriod: 'ALL_TIME', ingredientIds: [ingredientId], vegetarianType: query.vegetarianType || undefined })
      .then((result) => {
        if (!active) return;
        setItems((current) => query.page === 0 ? result.items
          : [...new Map([...current, ...result.items].map((item) => [item.id, item])).values()]);
        setHasNext(result.hasNext);
      })
      .catch(() => { if (active) setError(true); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [ingredientId, query]);

  const chip = (selected: boolean) => `rounded-full border px-3.5 py-2 text-xs font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-600 ${selected ? 'border-leaf-300 bg-leaf-100 text-leaf-800' : 'border-brand-100 bg-raised text-ink-soft hover:border-brand-300'}`;

  return <section aria-labelledby="ingredient-recipes-title" className="mt-6 rounded-3xl border border-brand-100 bg-paper p-5 shadow-sm sm:p-8">
    <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
      <div className="min-w-0 flex-1">
        <p className="flex items-center gap-2 text-xs font-bold uppercase tracking-[0.16em] text-brand-700"><ChefHat aria-hidden="true" className="h-4 w-4" />Cảm hứng vào bếp</p>
        <h2 id="ingredient-recipes-title" className="mt-2 break-words text-xl font-extrabold text-ink sm:text-2xl">Nấu gì với {name}?</h2>
        <p className="mt-2 text-sm leading-6 text-ink-muted">Khám phá các bài công thức có sử dụng nguyên liệu này.</p>
      </div>
      <label className="flex w-full items-center justify-between gap-2 text-xs font-medium text-ink-muted sm:w-auto sm:justify-start">Sắp xếp
        <select aria-label="Sắp xếp công thức theo nguyên liệu" value={query.sort} onChange={(event) => setQuery((current) => ({ ...current, page: 0, sort: event.target.value as RecipeSearchParams['sort'] }))} className="rounded-xl border border-brand-100 bg-raised px-3 py-2.5 text-sm font-semibold text-ink focus-visible:outline-brand-600">
          <option value="MOST_LIKED">Được yêu thích</option>
          <option value="NEWEST">Mới nhất</option>
        </select>
      </label>
    </div>
    {options && <div className="mt-5 border-y border-brand-100 py-4">
      <p className="mb-3 text-xs font-medium text-ink-muted">Lọc theo loại ăn chay của công thức</p>
      <div aria-label="Loại ăn chay của công thức" className="flex flex-wrap gap-2">
        <button type="button" aria-pressed={!query.vegetarianType} onClick={() => setQuery((current) => ({ ...current, page: 0, vegetarianType: '' }))} className={chip(!query.vegetarianType)}>Tất cả</button>
        {options.vegetarianTypes.map((type) => <button key={type.code} type="button" aria-pressed={query.vegetarianType === type.code} onClick={() => setQuery((current) => ({ ...current, page: 0, vegetarianType: type.code }))} className={chip(query.vegetarianType === type.code)}>{type.label}</button>)}
      </div>
    </div>}
    {optionsError && <p className="mt-4 text-sm text-ink-muted">Chưa tải được nhãn loại món. <button type="button" onClick={() => setOptionsRetry((value) => value + 1)} className="font-semibold text-brand-700 underline">Thử tải nhãn</button></p>}
    <div aria-busy={loading} className="mt-6">
      <div className="grid gap-x-5 gap-y-7 sm:grid-cols-2 lg:grid-cols-3">
        {items.map((recipe) => <PublicRecipeCard key={recipe.id} recipe={recipe} options={options} />)}
        {loading && items.length === 0 && <PublicRecipeSkeleton count={3} />}
      </div>
      {loading && <p role="status" className="mt-4 text-center text-sm text-ink-muted">Đang tải bài công thức…</p>}
      {error && <div role="alert" className="mt-5 rounded-xl border border-brand-100 bg-raised p-4 text-center text-sm text-ink-muted">Không tải được bài công thức. <button type="button" onClick={() => setQuery((current) => ({ ...current, retry: current.retry + 1 }))} className="font-semibold text-brand-700 underline">Thử tải công thức</button></div>}
      {!loading && !error && items.length === 0 && <p className="rounded-2xl border border-dashed border-brand-200 px-4 py-10 text-center text-sm text-ink-muted">{query.vegetarianType ? 'Chưa có bài công thức thuộc loại ăn chay này với nguyên liệu đang xem.' : 'Chưa có bài công thức công khai sử dụng nguyên liệu này.'}</p>}
      {hasNext && !error && <div className="mt-7 flex justify-center"><Button variant="primary" disabled={loading} onClick={() => setQuery((current) => ({ ...current, page: current.page + 1 }))}>Xem thêm công thức</Button></div>}
    </div>
  </section>;
}
