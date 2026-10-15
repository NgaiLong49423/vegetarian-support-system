import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { ArrowLeft, Filter, Leaf, LoaderCircle, Search, SlidersHorizontal } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, EmptyState } from '../components/ui';
import { ActiveFilters, FilterPanel, ResultPagination, SortControls, useRecipeSearch } from '../components/RecipeSearch';
import type { Choice, RecipeFormOptions, RecipePost } from '../api/recipes';
import { comparisonHref, parseRecipeId, pickerSide, usePublishedRecipe } from '../lib/recipeComparison';

// FR-60 picker (PR #132 review): a separate page that reuses the FR-08 search and filters to choose one
// recipe for the comparison. The recipe already on the other side is kept and left out of the choices
// (AC-60.2); choosing returns to /so-sanh with both IDs. The page only sends GET requests (AC-60.7).

export function RecipeComparePickerPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const firstId = parseRecipeId(params.get('a'));
  const secondId = parseRecipeId(params.get('b'));
  const side = pickerSide(params, firstId);
  const keptId = side === 'first' ? secondId : firstId;
  const kept = usePublishedRecipe(keptId);
  const search = useRecipeSearch(12);
  const { query, updateQuery, result, loading, error, retry, resetFilters, activeFilterCount, options, filterPanelProps } = search;

  const choices = result?.items.filter((recipe) => recipe.id !== keptId) ?? [];
  const narrowed = query.trim() !== '' || activeFilterCount > 0;
  const pick = (recipeId: number) => navigate(side === 'first'
    ? comparisonHref(recipeId, secondId)
    : comparisonHref(firstId, recipeId));

  return (
    <PageContainer className="py-6">
      <header className="mb-5">
        <Link to={comparisonHref(firstId, secondId)} className="inline-flex items-center gap-1.5 rounded text-sm font-semibold text-brand-700 hover:text-brand-800">
          <ArrowLeft className="h-4 w-4" aria-hidden="true" />Quay lại so sánh
        </Link>
        <h1 className="mt-3 text-2xl font-semibold tracking-tight text-ink">
          {side === 'first' ? 'Chọn công thức đầu tiên' : 'Chọn công thức thứ hai'}
        </h1>
        <p className="mt-1 text-sm leading-6 text-ink-muted">
          {kept.state.status === 'ready'
            ? <>Món bạn chọn sẽ được so sánh với <strong className="font-semibold text-ink">{kept.state.recipe.title}</strong>.</>
            : 'Tìm và chọn một công thức công khai để bắt đầu so sánh.'}
        </p>
        <label className="mt-4 flex max-w-3xl items-center gap-2 rounded-full border border-brand-200 bg-paper px-4 focus-within:border-brand-600">
          <Search aria-hidden="true" className="h-5 w-5 shrink-0 text-brand-600" />
          <span className="sr-only">Tìm công thức</span>
          <input
            type="search"
            value={query}
            maxLength={120}
            onChange={(event) => updateQuery(event.target.value)}
            placeholder="Tìm tên món hoặc nguyên liệu..."
            className="min-w-0 flex-1 bg-transparent py-3 text-sm text-ink outline-none placeholder:text-ink-muted"
          />
        </label>
      </header>

      <details className="group mb-5 lg:hidden">
        <summary className="inline-flex min-h-10 cursor-pointer list-none items-center gap-2 rounded-xl border border-brand-100 bg-paper px-3.5 text-sm font-semibold text-ink shadow-sm">
          <SlidersHorizontal className="h-4 w-4 text-brand-600" aria-hidden="true" />Bộ lọc
        </summary>
        <div className="mt-3"><FilterPanel {...filterPanelProps} idSuffix="mobile" /></div>
      </details>

      <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_250px]">
        <section aria-label="Kết quả tìm công thức" className="min-w-0">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-brand-100 bg-paper px-4 py-3 shadow-sm">
            <p className="text-sm font-bold text-ink">Công thức công khai</p>
            <SortControls search={search} />
          </div>
          <ActiveFilters search={search} />

          {loading ? (
            <p role="status" className="flex items-center gap-2 py-8 text-sm text-ink-muted">
              <LoaderCircle className="h-4 w-4 animate-spin" aria-hidden="true" />Đang tìm công thức…
            </p>
          ) : error ? (
            <EmptyState title="Không tải được công thức" description="Kiểm tra kết nối rồi thử tải lại danh sách công thức." action={<Button variant="secondary" onClick={retry}>Thử lại</Button>} />
          ) : choices.length > 0 ? (
            <ul aria-label="Công thức có thể chọn" className="grid gap-3 sm:grid-cols-2">
              {choices.map((recipe) => (
                <li key={recipe.id}><ChoiceCard recipe={recipe} options={options} onPick={() => pick(recipe.id)} /></li>
              ))}
            </ul>
          ) : narrowed ? (
            <EmptyState title="Không tìm thấy công thức phù hợp" description="Thử đổi từ khóa hoặc điều kiện lọc để xem thêm công thức." action={<Button variant="secondary" onClick={resetFilters}>Đặt lại tìm kiếm</Button>} />
          ) : (
            <EmptyState title="Không còn công thức công khai khác để so sánh." action={<Link to="/kham-pha" className="font-semibold text-brand-700 hover:text-brand-800">Quay lại khám phá</Link>} />
          )}
          {!loading && !error && <ResultPagination search={search} />}
        </section>

        <aside className="sticky top-20 hidden lg:block">
          <div className="mb-3 flex items-center gap-2 px-1 text-sm font-bold text-ink"><Filter className="h-4 w-4 text-brand-600" aria-hidden="true" />Bộ lọc tìm kiếm</div>
          <FilterPanel {...filterPanelProps} idSuffix="desktop" />
        </aside>
      </div>
    </PageContainer>
  );
}

function labelOf(choices: Choice[] | undefined, code: string) {
  return choices?.find((item) => item.code === code)?.label ?? code;
}

function ChoiceCard({ recipe, options, onPick }: { recipe: RecipePost; options: RecipeFormOptions | null; onPick: () => void }) {
  const cover = recipe.media.find((media) => media.cover) ?? recipe.media[0];
  return (
    <article className="flex h-full gap-3 rounded-2xl border border-brand-100 bg-paper p-3 shadow-sm">
      <div className="h-20 w-20 shrink-0 overflow-hidden rounded-xl bg-raised">
        {cover
          ? <img src={cover.url} alt="" loading="lazy" className="h-full w-full object-cover" />
          : <div className="flex h-full items-center justify-center text-ink-muted"><Leaf className="h-6 w-6" aria-hidden="true" /></div>}
      </div>
      <div className="flex min-w-0 flex-1 flex-col">
        <h2 className="line-clamp-2 text-sm font-semibold text-ink">{recipe.title}</h2>
        <p className="mt-0.5 truncate text-xs text-ink-muted">{recipe.authorName}</p>
        <p className="mt-0.5 truncate text-xs text-leaf-700">
          {labelOf(options?.vegetarianTypes, recipe.vegetarianType)} · {labelOf(options?.dishCategories, recipe.dishCategory)} · {recipe.prepTimeMin + recipe.cookTimeMin} phút
        </p>
        <div className="mt-auto pt-2">
          <Button size="sm" onClick={onPick}>Chọn<span className="sr-only"> {recipe.title}</span></Button>
        </div>
      </div>
    </article>
  );
}
