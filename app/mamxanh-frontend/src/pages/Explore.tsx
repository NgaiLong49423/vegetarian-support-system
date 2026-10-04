import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, SlidersHorizontal, X } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { RecipeCard } from '../components/RecipeCard';
import { Badge, Button, EmptyState } from '../components/ui';
import { recipes } from '../data/mockData';
import type { DietTag, Difficulty } from '../types';

const dietFilters: DietTag[] = ['Thuần Chay', 'Lacto', 'Ovo', 'Lacto-Ovo'];
const difficultyFilters: Difficulty[] = ['Dễ', 'Trung bình', 'Khó'];
const categoryFilters = ['Món mặn', 'Món nước', 'Món canh', 'Salad', 'Khai vị', 'Bữa sáng', 'Tráng miệng'];
const timeFilters = [
  { label: 'Dưới 20 phút', max: 20 },
  { label: '20 – 40 phút', max: 40 },
  { label: 'Trên 40 phút', max: Infinity },
];

/**
 * AC-01.1: Hỗ trợ 6 chế độ sắp xếp.
 */
const sortOptions = [
  { value: 'popular', label: 'Phổ biến nhất' },
  { value: 'rating', label: 'Được yêu thích nhất' },
  { value: 'time', label: 'Nấu nhanh nhất' },
  { value: 'calories', label: 'Ít calo nhất' },
  { value: 'newest', label: 'Mới nhất' },
  { value: 'views', label: 'Nhiều lượt xem nhất' },
];

function FilterGroup({ title, children }: { title: string; children: React.ReactNode }) {
  return (
      <div className="border-b border-brand-50 py-4 last:border-0">
        <h4 className="mb-3 text-xs font-bold uppercase tracking-wider text-ink-soft">{title}</h4>
        <div className="flex flex-wrap gap-2">{children}</div>
      </div>
  );
}

function Chip({ active, onClick, children }: { active: boolean; onClick: () => void; children: React.ReactNode }) {
  return (
      <button
          onClick={onClick}
          className={`rounded-full border px-3 py-1.5 text-sm font-medium transition-colors ${
              active
                  ? 'border-brand-600 bg-brand-600 text-white'
                  : 'border-brand-200 bg-white text-ink-soft hover:border-brand-300 hover:text-brand-700'
          }`}
      >
        {children}
      </button>
  );
}

export function Explore() {
  const [params] = useSearchParams();
  const [query, setQuery] = useState(params.get('q') ?? '');
  const [diet, setDiet] = useState<DietTag | null>(null);
  const [difficulty, setDifficulty] = useState<Difficulty | null>(null);
  const [category, setCategory] = useState<string | null>(null);
  const [timeMax, setTimeMax] = useState<number | null>(null);
  const [sort, setSort] = useState('popular');
  const [showFilters, setShowFilters] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 6;

  const filtered = useMemo(() => {
    let list = recipes.filter((r) => {
      // AC-01.4: Không hiển thị bài viết vi phạm hoặc bài không công khai cho Guest
      if (r.status === 'HIDDEN' || r.status === 'DELETED') return false;
      if (
          query &&
          !r.name.toLowerCase().includes(query.toLowerCase()) &&
          !r.tags.join(' ').toLowerCase().includes(query.toLowerCase())
      )
        return false;
      if (diet && r.diet !== diet) return false;
      if (difficulty && r.difficulty !== difficulty) return false;
      if (category && r.category !== category) return false;
      if (timeMax !== null && r.prepTime + r.cookTime > timeMax) return false;
      return true;
    });

    list = [...list].sort((a, b) => {
      if (sort === 'rating') {
        const aPct = a.likePercentage ?? -1;
        const bPct = b.likePercentage ?? -1;
        if (bPct !== aPct) return bPct - aPct;
        return b.likes + b.dislikes - (a.likes + a.dislikes);
      }
      if (sort === 'time') return a.prepTime + a.cookTime - (b.prepTime + b.cookTime);
      if (sort === 'calories') return a.calories - b.calories;
      if (sort === 'newest') {
        // Demo: sắp xếp theo id giảm dần (id cao = mới nhất)
        return b.id.localeCompare(a.id, undefined, { numeric: true });
      }
      if (sort === 'views') return b.viewCount - a.viewCount;
      // popular (default): tổng lượt bình chọn
      return b.likes + b.dislikes - (a.likes + a.dislikes);
    });

    return list;
  }, [query, diet, difficulty, category, timeMax, sort]);

  // Reset về trang 1 khi thay đổi tìm kiếm, bộ lọc hoặc chế độ sắp xếp
  useEffect(() => {
    setCurrentPage(1);
  }, [query, diet, difficulty, category, timeMax, sort]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
  const paginatedList = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return filtered.slice(start, start + pageSize);
  }, [filtered, currentPage, pageSize]);

  const activeCount = [diet, difficulty, category, timeMax].filter((x) => x !== null).length;
  const reset = () => {
    setDiet(null);
    setDifficulty(null);
    setCategory(null);
    setTimeMax(null);
  };

  const filterPanel = (
      <div className="rounded-2xl border border-brand-100 bg-white p-5">
        <div className="mb-2 flex items-center justify-between">
          <h3 className="flex items-center gap-2 font-bold text-ink">
            <SlidersHorizontal className="h-[18px] w-[18px] text-brand-600" /> Bộ lọc
          </h3>
          {activeCount > 0 && (
              <button onClick={reset} className="text-xs font-semibold text-brand-600 hover:underline">
                Xoá lọc ({activeCount})
              </button>
          )}
        </div>
        <FilterGroup title="Chế độ ăn chay">
          {dietFilters.map((d) => (
              <Chip key={d} active={diet === d} onClick={() => setDiet(diet === d ? null : d)}>
                {d}
              </Chip>
          ))}
        </FilterGroup>
        <FilterGroup title="Loại món">
          {categoryFilters.map((c) => (
              <Chip key={c} active={category === c} onClick={() => setCategory(category === c ? null : c)}>
                {c}
              </Chip>
          ))}
        </FilterGroup>
        <FilterGroup title="Thời gian nấu">
          {timeFilters.map((t) => (
              <Chip
                  key={t.label}
                  active={timeMax === t.max}
                  onClick={() => setTimeMax(timeMax === t.max ? null : t.max)}
              >
                {t.label}
              </Chip>
          ))}
        </FilterGroup>
        <FilterGroup title="Mức độ khó">
          {difficultyFilters.map((d) => (
              <Chip key={d} active={difficulty === d} onClick={() => setDifficulty(difficulty === d ? null : d)}>
                {d}
              </Chip>
          ))}
        </FilterGroup>
      </div>
  );

  return (
      <PageContainer className="py-8">
        <div className="mb-6">
          <p className="mb-1 text-xs font-bold uppercase tracking-[0.15em] text-brand-600">Khám phá</p>
          <h1 className="text-3xl font-extrabold tracking-tight text-ink">Khám phá công thức chay</h1>
          <p className="mt-1 text-ink-muted">
            Hơn 1.800 công thức thuần thực vật cân bằng vi chất, chọn lọc từ cộng đồng.
          </p>
        </div>

        {/* search + sort */}
        <div className="mb-6 flex flex-col gap-3 sm:flex-row">
          <div className="flex flex-1 items-center gap-2 rounded-xl border border-brand-200 bg-white px-3">
            <Search className="h-5 w-5 shrink-0 text-ink-muted" />
            <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Tìm món chay hoặc nguyên liệu..."
                className="w-full bg-transparent py-2.5 text-sm outline-none placeholder:text-ink-muted"
            />
            {query && (
                <button onClick={() => setQuery('')} className="text-ink-muted hover:text-brand-600">
                  <X className="h-4 w-4" />
                </button>
            )}
          </div>
          <div className="flex gap-2">
            <Button variant="outline" className="lg:hidden" onClick={() => setShowFilters((v) => !v)}>
              <SlidersHorizontal className="h-4 w-4" /> Lọc {activeCount > 0 && `(${activeCount})`}
            </Button>
            <select
                value={sort}
                onChange={(e) => setSort(e.target.value)}
                className="rounded-xl border border-brand-200 bg-white px-3 py-2.5 text-sm font-medium text-ink-soft outline-none focus:border-brand-400"
                aria-label="Chế độ sắp xếp"
            >
              {sortOptions.map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
              ))}
            </select>
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
          <aside className="hidden lg:block">
            <div className="sticky top-20">{filterPanel}</div>
          </aside>
          {showFilters && <div className="lg:hidden">{filterPanel}</div>}

          <div>
            <div className="mb-4 flex items-center gap-3 text-sm text-ink-muted">
            <span>
              <strong className="text-ink">{filtered.length}</strong> công thức
            </span>
              {activeCount > 0 && (
                  <div className="flex flex-wrap gap-1.5">
                    {diet && <Badge tone="leaf">{diet}</Badge>}
                    {category && <Badge>{category}</Badge>}
                    {difficulty && <Badge>{difficulty}</Badge>}
                  </div>
              )}
            </div>
            {filtered.length === 0 ? (
                <EmptyState
                    title="Không tìm thấy công thức phù hợp"
                    description="Thử điều chỉnh bộ lọc hoặc từ khoá tìm kiếm để khám phá nhiều món chay hơn nhé."
                    action={
                      <Button variant="secondary" onClick={reset}>
                        Xoá tất cả bộ lọc
                      </Button>
                    }
                />
            ) : (
                <>
                  <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">
                    {paginatedList.map((r) => (
                        <RecipeCard key={r.id} recipe={r} />
                    ))}
                  </div>

                  {/* Thanh điều khiển phân trang — AC-01.1 */}
                  {totalPages > 1 && (
                    <div
                      className="mt-8 flex flex-col items-center justify-between gap-4 border-t border-brand-100 pt-6 sm:flex-row"
                      data-testid="pagination-controls"
                    >
                      <p className="text-xs text-ink-muted">
                        Hiển thị <span className="font-semibold text-ink">{(currentPage - 1) * pageSize + 1}</span> -{' '}
                        <span className="font-semibold text-ink">
                          {Math.min(currentPage * pageSize, filtered.length)}
                        </span>{' '}
                        trên tổng số <span className="font-semibold text-ink">{filtered.length}</span> công thức
                      </p>
                      <div className="flex items-center gap-1.5">
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={currentPage === 1}
                          onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                          aria-label="Trang trước"
                        >
                          Trang trước
                        </Button>
                        {Array.from({ length: totalPages }, (_, i) => i + 1).map((p) => (
                          <button
                            key={p}
                            onClick={() => setCurrentPage(p)}
                            className={`flex h-9 w-9 items-center justify-center rounded-xl text-xs font-bold transition-colors ${
                              currentPage === p
                                ? 'bg-brand-600 text-white shadow-sm'
                                : 'border border-brand-200 bg-white text-ink-soft hover:bg-brand-50'
                            }`}
                            aria-label={`Trang ${p}`}
                            aria-current={currentPage === p ? 'page' : undefined}
                          >
                            {p}
                          </button>
                        ))}
                        <Button
                          variant="outline"
                          size="sm"
                          disabled={currentPage === totalPages}
                          onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                          aria-label="Trang sau"
                        >
                          Trang sau
                        </Button>
                      </div>
                    </div>
                  )}
                </>
            )}
          </div>
        </div>
      </PageContainer>
  );
}