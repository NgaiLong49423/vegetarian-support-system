import { useState } from 'react';
import {
  ChevronDown,
  Filter,
  Search,
  SlidersHorizontal,
  X,
} from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button, EmptyState } from '../components/ui';
import { PublicRecipeCard, PublicRecipeSkeleton } from '../components/PublicRecipeCard';
import { ActiveFilters, FilterPanel, ResultPagination, SortControls, useRecipeSearch } from '../components/RecipeSearch';
import type { RecipePost } from '../api/recipes';

export function Explore() {
  const search = useRecipeSearch(12);
  const { query, updateQuery, options, result, loading, error, retry, resetFilters, activeFilterCount, presetIngredientError, filterPanelProps } = search;
  const [filtersOpen, setFiltersOpen] = useState(false);

  return (
    <PageContainer className="py-5 sm:py-6">
      <header className="mb-4 sm:mb-5">
        <p className="text-xs text-ink-muted">Mâm Xanh / Công thức</p>
        <h1 className="mt-2 text-2xl font-semibold tracking-tight text-ink">Khám phá công thức chay</h1>
        <p className="mt-1 text-xs leading-5 text-ink-muted">Tìm món phù hợp với nguyên liệu và thời gian bạn có.</p>
        {presetIngredientError && <p role="status" className="mt-2 text-sm text-amber-800">Không tìm thấy nguyên liệu được chọn trong danh mục hiện tại.</p>}

        <div className="mt-4 flex max-w-3xl items-center gap-2 rounded-full border border-brand-200 bg-paper px-4 py-1.5 transition-all duration-200 focus-within:border-brand-600 focus-within:ring-2 focus-within:ring-brand-500/20">
          <Search aria-hidden="true" className="h-5 w-5 shrink-0 text-brand-600" />
          <input
            aria-label="Từ khóa"
            value={query}
            onChange={(event) => updateQuery(event.target.value)}
            placeholder="Tìm tên món hoặc nguyên liệu..."
            className="min-w-0 flex-1 bg-transparent py-2 text-base sm:text-sm text-ink outline-none placeholder:text-ink-muted"
          />
          {query && <button type="button" onClick={() => updateQuery('')} aria-label="Xóa từ khóa" className="rounded-full p-1.5 text-ink-muted transition-colors hover:bg-brand-100 hover:text-brand-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500"><X className="h-4 w-4" /></button>}
        </div>
      </header>

      <div className="mb-4 flex items-center justify-between gap-2 lg:hidden">
        <button
          type="button"
          aria-expanded={filtersOpen}
          aria-controls="mobile-recipe-filters"
          onClick={() => setFiltersOpen((open) => !open)}
          className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-brand-100 bg-paper px-3.5 text-sm font-semibold text-ink shadow-sm hover:border-brand-300 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-100"
        >
          <SlidersHorizontal className="h-4 w-4 text-brand-600" />
          Bộ lọc{activeFilterCount > 0 && <span className="rounded-full bg-brand-600 px-2 py-0.5 text-xs text-on-brand">{activeFilterCount}</span>}
          <ChevronDown className={`h-4 w-4 transition-transform ${filtersOpen ? 'rotate-180' : ''}`} />
        </button>
        <span className="truncate text-xs text-ink-muted">Điều kiện lọc kết hợp với nhau</span>
      </div>

      {filtersOpen && (
        <div className="fixed inset-0 z-40 flex items-end justify-center lg:hidden">
          <button
            type="button"
            aria-label="Đóng nền bộ lọc"
            onClick={() => setFiltersOpen(false)}
            className="modal-backdrop-enter absolute inset-0 h-full w-full bg-black/65 backdrop-blur-xs"
          />
          <section
            id="mobile-recipe-filters"
            role="dialog"
            aria-modal="true"
            aria-label="Bộ lọc tìm kiếm di động"
            className="mobile-sheet-enter relative flex max-h-[86dvh] w-full flex-col overflow-hidden rounded-t-3xl border-t border-brand-100 bg-cream shadow-2xl"
          >
            <div className="flex items-center justify-between border-b border-brand-100 bg-paper px-4 pt-3 pb-2.5">
              <span aria-hidden="true" className="modal-drag-pill absolute left-1/2 top-1.5 h-1 w-10 -translate-x-1/2 rounded-full" />
              <p className="pt-1 text-sm font-extrabold text-ink">Bộ lọc tìm kiếm</p>
              <button
                type="button"
                aria-label="Đóng bộ lọc"
                onClick={() => setFiltersOpen(false)}
                className="flex h-8 w-8 items-center justify-center rounded-lg text-ink-muted hover:bg-brand-100 hover:text-ink"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <div className="flex-1 overflow-y-auto p-3">
              <FilterPanel {...filterPanelProps} idSuffix="mobile" />
            </div>
            <div className="border-t border-brand-100 bg-paper p-3 pb-[calc(0.75rem+env(safe-area-inset-bottom))]">
              <Button className="w-full justify-center" onClick={() => setFiltersOpen(false)}>
                Xem kết quả{result ? ` (${result.items.length})` : ''}
              </Button>
            </div>
          </section>
        </div>
      )}

      <div className="grid items-start gap-5 lg:grid-cols-[minmax(0,1fr)_250px]">
        <section aria-label="Kết quả công thức" className="min-w-0">
          <div className="mb-4 flex flex-col gap-2.5 rounded-2xl border border-brand-100 bg-paper px-4 py-3 shadow-sm sm:flex-row sm:flex-wrap sm:items-center sm:justify-between sm:gap-3">
            <div>
              <p className="text-sm font-bold text-ink">{loading ? 'Đang tìm công thức…' : 'Kết quả công thức'}</p>
              {activeFilterCount > 1 && <p className="mt-0.5 text-xs text-ink-muted">Công thức cần đáp ứng tất cả điều kiện đang chọn.</p>}
            </div>
            <SortControls search={search} />
          </div>

          <ActiveFilters search={search} />

          {loading ? <div role="status" aria-label="Đang tải công thức" className="grid grid-cols-2 gap-3.5 sm:grid-cols-3 sm:gap-4 xl:grid-cols-4"><PublicRecipeSkeleton count={8} /></div>
            : error ? <EmptyState title="Không tải được công thức" description="Kiểm tra kết nối rồi thử tải lại danh sách công thức." action={<Button variant="secondary" onClick={retry}>Thử lại</Button>} />
              : result && result.items.length > 0 ? <>
                <div className="grid grid-cols-2 gap-x-3.5 gap-y-5 sm:grid-cols-3 sm:gap-x-4 sm:gap-y-6 xl:grid-cols-4">
                  {result.items.map((recipe: RecipePost) => <div key={recipe.id} className="min-w-0"><PublicRecipeCard recipe={recipe} options={options} /></div>)}
                </div>
                <ResultPagination search={search} />
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
