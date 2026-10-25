import { useEffect, useMemo, useState } from 'react';
import { ArrowUpRight, Check, Leaf, Search, X } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { PageContainer } from '../components/Layout';
import { Button, EmptyState } from '../components/ui';
import { IngredientImage } from '../components/IngredientImage';
import { discoveryApi, type PublicIngredient } from '../api/discovery';

export function IngredientDirectoryPage() {
  const navigate = useNavigate();
  const [ingredients, setIngredients] = useState<PublicIngredient[]>([]);
  const [selected, setSelected] = useState<PublicIngredient[]>([]);
  const [query, setQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError(false);
    discoveryApi.getAllIngredients()
      .then((items) => { if (active) setIngredients(items); })
      .catch(() => { if (active) setError(true); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [retry]);

  const visibleIngredients = useMemo(() => {
    const term = query.trim().toLocaleLowerCase('vi');
    return term
      ? ingredients.filter((ingredient) => ingredient.name.toLocaleLowerCase('vi').includes(term))
      : ingredients;
  }, [ingredients, query]);

  const toggleIngredient = (ingredient: PublicIngredient) => {
    setSelected((current) => current.some((item) => item.ingredientId === ingredient.ingredientId)
      ? current.filter((item) => item.ingredientId !== ingredient.ingredientId)
      : [...current, ingredient]);
  };

  const findRecipes = () => {
    const params = new URLSearchParams();
    selected.forEach((ingredient) => {
      params.append('ingredientId', String(ingredient.ingredientId));
      params.append('ingredientName', ingredient.name);
    });
    navigate(`/kham-pha?${params.toString()}`);
  };

  return <PageContainer className="py-8 sm:py-10">
    <div className="mx-auto w-full max-w-5xl">
    <header className="mb-7">
      <p className="text-xs font-semibold uppercase tracking-[0.16em] text-brand-700">Mâm Xanh / Khám phá</p>
      <div className="mt-2 flex items-center gap-3">
        <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-leaf-100 text-leaf-800"><Leaf className="h-5 w-5" /></span>
        <h1 className="text-2xl font-extrabold tracking-tight text-ink sm:text-3xl">Danh mục nguyên liệu</h1>
      </div>
      <p className="mt-3 max-w-2xl text-sm leading-6 text-ink-muted">Chọn một hoặc nhiều nguyên liệu để tìm các công thức có đủ những nguyên liệu bạn cần.</p>
    </header>

    <section aria-labelledby="ingredient-list-title">
      <div className="mb-4 flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div>
          <h2 id="ingredient-list-title" className="text-lg font-bold text-ink">Tất cả nguyên liệu</h2>
          <p className="mt-1 text-sm text-ink-muted">{loading ? 'Đang tải danh mục…' : `${ingredients.length.toLocaleString('vi-VN')} nguyên liệu`}</p>
        </div>
        <label className="flex w-full items-center gap-2 rounded-full border border-brand-200 bg-paper px-3.5 py-2.5 sm:max-w-sm">
          <Search aria-hidden="true" className="h-4 w-4 shrink-0 text-brand-700" />
          <input aria-label="Tìm nguyên liệu trong danh mục" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm nguyên liệu" className="min-w-0 flex-1 bg-transparent text-sm text-ink outline-none placeholder:text-ink-muted" />
        </label>
      </div>

      {selected.length > 0 && <div className="mb-4 flex flex-wrap items-center gap-2">
        <span className="text-sm font-medium text-ink-muted">Đã chọn {selected.length}:</span>
        {selected.map((ingredient) => <button key={ingredient.ingredientId} type="button" onClick={() => toggleIngredient(ingredient)} aria-label={`Bỏ chọn ${ingredient.name}`} className="inline-flex items-center gap-1 rounded-full bg-leaf-100 px-3 py-1.5 text-xs font-medium text-leaf-800 hover:bg-leaf-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600">{ingredient.name}<X aria-hidden="true" className="h-3.5 w-3.5" /></button>)}
        <Button variant="ghost" size="sm" onClick={() => setSelected([])}>Bỏ chọn tất cả</Button>
      </div>}

      {error && <EmptyState title="Không tải được danh mục" description="Hãy thử tải lại danh mục nguyên liệu." action={<Button variant="secondary" onClick={() => setRetry((current) => current + 1)}>Thử lại</Button>} />}
      {!error && loading && <p role="status" className="py-10 text-center text-sm text-ink-muted">Đang tải tất cả nguyên liệu…</p>}
      {!error && !loading && ingredients.length === 0 && <EmptyState title="Chưa có nguyên liệu" description="Danh mục hiện chưa có nguyên liệu đang hoạt động." />}
      {!error && !loading && ingredients.length > 0 && visibleIngredients.length === 0 && <EmptyState title="Không tìm thấy nguyên liệu" description="Thử một từ khóa khác." />}
      {!error && !loading && visibleIngredients.length > 0 && <ul aria-label="Danh sách nguyên liệu" className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {visibleIngredients.map((ingredient) => {
          const isSelected = selected.some((item) => item.ingredientId === ingredient.ingredientId);
          return <li key={ingredient.ingredientId}>
            <article className={`flex h-full min-h-28 items-center gap-3 rounded-2xl border p-3 transition-colors ${isSelected ? 'border-leaf-300 bg-leaf-50/70' : 'border-brand-100 bg-paper hover:border-brand-200'}`}>
              <Link to={`/nguyen-lieu/${ingredient.ingredientId}`} aria-label={`Xem nguyên liệu ${ingredient.name}`} className="shrink-0 rounded-xl focus-visible:outline-2 focus-visible:outline-brand-600"><IngredientImage imageUrl={ingredient.imageUrl} name={ingredient.name} containerClassName="h-16 w-16 overflow-hidden rounded-xl border border-brand-100 bg-raised" imageClassName="h-full w-full object-cover" /></Link>
              <div className="min-w-0 flex-1">
                <h3 className="truncate text-sm font-bold text-ink"><Link to={`/nguyen-lieu/${ingredient.ingredientId}`} className="hover:text-brand-700 focus-visible:outline-2 focus-visible:outline-brand-600">{ingredient.name}</Link></h3>
                <p className="mt-1 truncate text-xs text-ink-muted">{ingredient.ingredientGroup}</p>
                <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1">
                  <Link to={`/nguyen-lieu/${ingredient.ingredientId}`} className="inline-flex items-center gap-1 text-xs font-semibold text-brand-700 hover:text-brand-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600">Xem chi tiết <ArrowUpRight aria-hidden="true" className="h-3.5 w-3.5" /></Link>
                  <button type="button" aria-label={`Chọn ${ingredient.name}`} aria-pressed={isSelected} onClick={() => toggleIngredient(ingredient)} className="inline-flex items-center gap-1 text-xs font-semibold text-ink-muted hover:text-leaf-800 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600">
                    {isSelected && <Check aria-hidden="true" className="h-3.5 w-3.5" />}{isSelected ? 'Đã chọn' : 'Chọn'}
                  </button>
                </div>
              </div>
            </article>
          </li>;
        })}
      </ul>}

      {!error && !loading && ingredients.length > 0 && <div className="mt-7 flex flex-wrap items-center gap-3 border-t border-brand-100 pt-5">
        <Button variant="primary" disabled={selected.length === 0} onClick={findRecipes}>Tìm công thức{selected.length > 0 ? ` (${selected.length})` : ''}</Button>
        <span className="text-sm text-ink-muted">Công thức sẽ cần có đủ các nguyên liệu đã chọn.</span>
      </div>}
    </section>
    </div>
  </PageContainer>;
}
