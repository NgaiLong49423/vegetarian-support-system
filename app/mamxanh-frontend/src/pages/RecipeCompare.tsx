import { useEffect, useState, type ReactNode } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Info, Leaf, LoaderCircle, Search } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { asApiError } from '../lib/apiClient';
import { recipesApi, type RecipeDetail, type RecipePost } from '../api/recipes';

// FR-60 / BR-76: compare exactly two different PUBLISHED recipes per serving. The page only reads
// public data (FR-20 detail with FR-39 nutrition); the selection lives in the URL, nothing is stored.

type Slot = 'A' | 'B';
const OTHER: Record<Slot, Slot> = { A: 'B', B: 'A' };

// BR-76: the nine MVP nutrients; sodium/salt is never part of the comparison.
const NUTRIENTS = [
  { key: 'ENERGY_KCAL', label: 'Năng lượng', unit: 'kcal' },
  { key: 'PROTEIN_G', label: 'Chất đạm', unit: 'g' },
  { key: 'CARBOHYDRATE_G', label: 'Carbohydrate', unit: 'g' },
  { key: 'TOTAL_FAT_G', label: 'Chất béo', unit: 'g' },
  { key: 'FIBER_G', label: 'Chất xơ', unit: 'g' },
  { key: 'CALCIUM_MG', label: 'Canxi', unit: 'mg' },
  { key: 'IRON_MG', label: 'Sắt', unit: 'mg' },
  { key: 'VITAMIN_B12_MCG', label: 'Vitamin B12', unit: 'mcg' },
  { key: 'ZINC_MG', label: 'Kẽm', unit: 'mg' },
] as const;

const MISSING = 'Chưa đủ dữ liệu';
const ROW_3 = 'grid grid-cols-2 gap-x-3 gap-y-1 border-b border-brand-50 py-3 last:border-b-0 sm:grid-cols-[minmax(0,11rem)_minmax(0,1fr)_minmax(0,1fr)] sm:gap-x-4';
const ROW_4 = 'grid grid-cols-3 gap-x-3 gap-y-1 border-b border-brand-50 py-3 last:border-b-0 sm:grid-cols-[minmax(0,10rem)_repeat(3,minmax(0,1fr))] sm:gap-x-4';

type SlotState =
  | { status: 'empty' }
  | { status: 'loading' }
  | { status: 'ready'; recipe: RecipeDetail }
  | { status: 'unavailable' }
  | { status: 'error'; message: string };

type IngredientLine = RecipeDetail['ingredients'][number];

function parseId(value: string | null) {
  if (!value || !/^\d{1,15}$/.test(value)) return null;
  const id = Number(value);
  return id > 0 ? id : null;
}

function formatNumber(value: number) {
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2 }).format(value);
}

function formatDifference(value: number) {
  return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 2, signDisplay: 'exceptZero' }).format(value);
}

/** Per-serving value, or null when the recipe's nutrition is incomplete (BR-76: never shown as 0). */
function perServing(recipe: RecipeDetail, key: string) {
  const complete = recipe.nutrition?.complete ?? recipe.nutritionComplete;
  const value = recipe.nutrition?.perServing?.[key];
  return complete && typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function usePublishedRecipe(recipeId: number | null) {
  const [state, setState] = useState<SlotState>({ status: recipeId === null ? 'empty' : 'loading' });
  const [attempt, setAttempt] = useState(0);
  useEffect(() => {
    if (recipeId === null) {
      setState({ status: 'empty' });
      return;
    }
    let active = true;
    setState({ status: 'loading' });
    recipesApi.getPublished(recipeId)
      .then((recipe) => { if (active) setState({ status: 'ready', recipe }); })
      .catch((cause: unknown) => {
        if (!active) return;
        const error = asApiError(cause);
        setState(error.status === 404 ? { status: 'unavailable' } : { status: 'error', message: error.message });
      });
    return () => { active = false; };
  }, [recipeId, attempt]);
  return { state, retry: () => setAttempt((value) => value + 1) };
}

export function RecipeComparePage() {
  const [params, setParams] = useSearchParams();
  const aId = parseId(params.get('a'));
  const requestedB = parseId(params.get('b'));
  const sameRecipe = aId !== null && aId === requestedB;
  const bId = sameRecipe ? null : requestedB;
  const a = usePublishedRecipe(aId);
  const b = usePublishedRecipe(bId);
  const [picking, setPicking] = useState<Slot | null>(null);

  const select = (slot: Slot, recipeId: number) => {
    const next = new URLSearchParams();
    const nextA = slot === 'A' ? recipeId : aId;
    const nextB = slot === 'B' ? recipeId : bId;
    if (nextA !== null) next.set('a', String(nextA));
    if (nextB !== null) next.set('b', String(nextB));
    setPicking(null);
    setParams(next);
  };

  const ids: Record<Slot, number | null> = { A: aId, B: bId };
  const states: Record<Slot, ReturnType<typeof usePublishedRecipe>> = { A: a, B: b };
  const both = a.state.status === 'ready' && b.state.status === 'ready' ? [a.state.recipe, b.state.recipe] as const : null;
  const selectedCount = [a.state, b.state].filter((state) => state.status === 'ready').length;

  return (
    <PageContainer className="py-6 sm:py-8">
      <header className="mb-6">
        <h1 className="text-3xl font-extrabold tracking-tight text-ink sm:text-4xl">So sánh hai công thức</h1>
        <p className="mt-2 max-w-3xl text-sm leading-6 text-ink-muted">
          Đối chiếu thông tin, nguyên liệu và chín chỉ tiêu dinh dưỡng trên một khẩu phần của hai công thức công khai.
          Kết quả không được lưu lại.
        </p>
      </header>

      {sameRecipe && (
        <p role="alert" className="mb-4 rounded-xl border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
          Hai vị trí phải là hai công thức khác nhau. Hãy chọn một công thức khác cho vị trí B.
        </p>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        {(['A', 'B'] as const).map((slot) => (
          <SlotPanel
            key={slot}
            slot={slot}
            state={states[slot].state}
            otherId={ids[OTHER[slot]]}
            picking={picking === slot}
            onPick={(recipeId) => select(slot, recipeId)}
            onReplace={() => setPicking(slot)}
            onKeep={() => setPicking(null)}
            onRetry={states[slot].retry}
          />
        ))}
      </div>

      {both ? (
        <Comparison a={both[0]} b={both[1]} />
      ) : (
        <p className="mt-6 text-center text-sm text-ink-muted">
          {selectedCount === 0
            ? 'Chọn hai công thức công khai khác nhau để xem bảng so sánh.'
            : 'Chọn thêm một công thức để bắt đầu so sánh.'}
        </p>
      )}
    </PageContainer>
  );
}

function SlotPanel({ slot, state, otherId, picking, onPick, onReplace, onKeep, onRetry }: {
  slot: Slot;
  state: SlotState;
  otherId: number | null;
  picking: boolean;
  onPick: (recipeId: number) => void;
  onReplace: () => void;
  onKeep: () => void;
  onRetry: () => void;
}) {
  const headingId = `compare-slot-${slot}`;
  const showPicker = picking || state.status === 'empty' || state.status === 'unavailable';
  return (
    <section aria-labelledby={headingId} className="min-w-0 rounded-2xl border border-brand-100 bg-paper p-4 sm:p-5">
      <h2 id={headingId} className="text-xs font-bold uppercase tracking-wider text-ink-muted">Công thức {slot}</h2>
      {state.status === 'loading' && (
        <p role="status" className="mt-3 flex items-center gap-2 text-sm text-ink-muted">
          <LoaderCircle className="h-4 w-4 animate-spin" aria-hidden="true" />Đang tải công thức…
        </p>
      )}
      {state.status === 'unavailable' && (
        <p role="alert" className="mt-3 rounded-xl border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
          Công thức ở vị trí {slot} không còn công khai. Hãy chọn công thức khác.
        </p>
      )}
      {state.status === 'error' && (
        <div role="alert" className="mt-3 rounded-xl border border-brand-200 bg-cream p-3 text-sm text-ink-muted">
          <p>{state.message}</p>
          <button type="button" onClick={onRetry} aria-label={`Thử tải lại công thức ${slot}`} className="mt-2 font-semibold text-brand-600 hover:text-brand-700">
            Thử lại
          </button>
        </div>
      )}
      {state.status === 'ready' && (
        <RecipeSummary recipe={state.recipe}>
          {picking ? (
            <button type="button" onClick={onKeep} className="text-xs font-semibold text-ink-muted hover:text-ink">
              Giữ công thức hiện tại
            </button>
          ) : (
            <button type="button" onClick={onReplace} className="text-xs font-semibold text-brand-600 hover:text-brand-700">
              Thay công thức {slot}
            </button>
          )}
        </RecipeSummary>
      )}
      {showPicker && <RecipePicker slot={slot} otherId={otherId} onPick={onPick} />}
    </section>
  );
}

function RecipeSummary({ recipe, children }: { recipe: RecipeDetail; children: ReactNode }) {
  const cover = recipe.media.find((item) => item.cover) ?? recipe.media[0];
  return (
    <div className="mt-3 flex gap-3">
      <div className="h-20 w-24 shrink-0 overflow-hidden rounded-xl bg-raised">
        {cover
          ? <img src={cover.blobUrl} alt={recipe.title} className="h-full w-full object-cover" />
          : <div className="flex h-full items-center justify-center text-ink-muted"><Leaf className="h-6 w-6" aria-hidden="true" /></div>}
      </div>
      <div className="min-w-0">
        <Link to={`/cong-thuc/id/${recipe.recipeId}`} className="line-clamp-2 font-semibold text-ink hover:text-brand-600">
          {recipe.title}
        </Link>
        <p className="mt-1 truncate text-xs text-ink-muted">{recipe.author.displayName}</p>
        <div className="mt-2">{children}</div>
      </div>
    </div>
  );
}

type SearchState = { status: 'loading' } | { status: 'ready'; items: RecipePost[] } | { status: 'error'; message: string };

function RecipePicker({ slot, otherId, onPick }: { slot: Slot; otherId: number | null; onPick: (recipeId: number) => void }) {
  const [keyword, setKeyword] = useState('');
  const [search, setSearch] = useState<SearchState>({ status: 'loading' });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    let active = true;
    setSearch({ status: 'loading' });
    const query = keyword.trim();
    const timer = window.setTimeout(() => {
      recipesApi.searchPublic({ keyword: query, page: 0, size: 8, sort: 'NEWEST', viewPeriod: 'ALL_TIME' })
        .then((result) => { if (active) setSearch({ status: 'ready', items: result.items }); })
        .catch((cause: unknown) => { if (active) setSearch({ status: 'error', message: asApiError(cause).message }); });
    }, query ? 250 : 0);
    return () => {
      active = false;
      window.clearTimeout(timer);
    };
  }, [keyword, attempt]);

  const items = search.status === 'ready' ? search.items : [];
  const hasChoice = items.some((item) => item.id !== otherId);
  return (
    <div className="mt-3">
      <div className="flex items-center gap-2 rounded-full border border-brand-200 bg-cream px-3 focus-within:border-brand-500">
        <Search className="h-4 w-4 shrink-0 text-ink-muted" aria-hidden="true" />
        <input
          type="search"
          aria-label={`Tìm công thức cho vị trí ${slot}`}
          placeholder="Tìm theo tên món…"
          maxLength={120}
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          className="min-w-0 flex-1 bg-transparent py-2.5 text-sm text-ink outline-none"
        />
      </div>
      {search.status === 'loading' && (
        <p role="status" className="mt-3 flex items-center gap-2 text-sm text-ink-muted">
          <LoaderCircle className="h-4 w-4 animate-spin" aria-hidden="true" />Đang tìm công thức…
        </p>
      )}
      {search.status === 'error' && (
        <div role="alert" className="mt-3 text-sm text-ink-muted">
          <p>{search.message}</p>
          <button type="button" onClick={() => setAttempt((value) => value + 1)} className="mt-1 font-semibold text-brand-600 hover:text-brand-700">
            Thử tìm lại
          </button>
        </div>
      )}
      {items.length > 0 && (
        <ul aria-label={`Kết quả tìm cho vị trí ${slot}`} className="mt-3 divide-y divide-brand-50">
          {items.map((item) => {
            const taken = item.id === otherId;
            return (
              <li key={item.id} className="flex items-center justify-between gap-3 py-2">
                <div className="min-w-0">
                  <p className="truncate text-sm font-semibold text-ink">{item.title}</p>
                  <p className="truncate text-xs text-ink-muted">{item.authorName}</p>
                </div>
                <button
                  type="button"
                  disabled={taken}
                  aria-label={`Chọn ${item.title} cho vị trí ${slot}`}
                  onClick={() => onPick(item.id)}
                  className="shrink-0 rounded-full border border-brand-200 px-3 py-1 text-xs font-semibold text-brand-700 hover:border-brand-400 disabled:border-transparent disabled:text-ink-muted"
                >
                  {taken ? `Đang ở vị trí ${OTHER[slot]}` : 'Chọn'}
                </button>
              </li>
            );
          })}
        </ul>
      )}
      {search.status === 'ready' && !hasChoice && (
        <div className="mt-3 text-sm text-ink-muted">
          <p>Không còn công thức công khai khác phù hợp.</p>
          <Link to="/kham-pha" className="mt-1 inline-block font-semibold text-brand-600 hover:text-brand-700">Quay lại khám phá</Link>
        </div>
      )}
    </div>
  );
}

function Comparison({ a, b }: { a: RecipeDetail; b: RecipeDetail }) {
  return (
    <div className="mt-8 space-y-6">
      <OverviewTable a={a} b={b} />
      <IngredientTable a={a} b={b} />
      <NutritionTable a={a} b={b} />
    </div>
  );
}

function SectionCard({ id, title, children }: { id: string; title: string; children: ReactNode }) {
  return (
    <section aria-labelledby={id} className="rounded-2xl border border-brand-100 bg-paper p-4 sm:p-6">
      <h2 id={id} className="text-lg font-extrabold text-ink">{title}</h2>
      {children}
    </section>
  );
}

function HeaderRow({ className, first, a, b, extra }: { className: string; first: string; a: RecipeDetail; b: RecipeDetail; extra?: string }) {
  return (
    <div role="rowgroup" className="hidden sm:block">
      <div role="row" className={`${className} text-xs font-bold uppercase tracking-wide text-ink-muted`}>
        <span role="columnheader">{first}</span>
        <span role="columnheader" className="min-w-0 break-words">A · {a.title}</span>
        <span role="columnheader" className="min-w-0 break-words">B · {b.title}</span>
        {extra && <span role="columnheader">{extra}</span>}
      </div>
    </div>
  );
}

/** A value cell; on phones the column headers are hidden, so a short side tag precedes the value. */
function Value({ tag, children }: { tag: string; children: ReactNode }) {
  return (
    <div className="min-w-0">
      <span className="block text-[11px] font-semibold text-ink-muted sm:hidden">{tag}</span>
      <span role="cell" className="block break-words text-sm text-ink">{children}</span>
    </div>
  );
}

function OverviewTable({ a, b }: { a: RecipeDetail; b: RecipeDetail }) {
  const likes = (recipe: RecipeDetail) => recipe.statistics.likePercentage === null
    ? 'Chưa có đánh giá'
    : `${formatNumber(recipe.statistics.likePercentage)}%`;
  const rows: Array<[string, (recipe: RecipeDetail) => string]> = [
    ['Tác giả', (recipe) => recipe.author.displayName],
    ['Loại ăn chay', (recipe) => recipe.vegetarianTypeLabel],
    ['Loại món', (recipe) => recipe.dishCategoryLabel],
    ['Khẩu phần gốc', (recipe) => `${recipe.servings} khẩu phần`],
    ['Thời gian', (recipe) => `Chuẩn bị ${recipe.prepTimeMinutes} phút · Nấu ${recipe.cookTimeMinutes} phút`],
    ['Tỷ lệ thích', likes],
  ];
  return (
    <SectionCard id="compare-overview-heading" title="Thông tin tổng quan">
      <div role="table" aria-label="Thông tin tổng quan" className="mt-3">
        <HeaderRow className={ROW_3} first="Thông tin" a={a} b={b} />
        <div role="rowgroup">
          {rows.map(([label, value]) => (
            <div role="row" key={label} className={ROW_3}>
              <span role="rowheader" className="col-span-2 text-sm font-semibold text-ink-soft sm:col-span-1">{label}</span>
              <Value tag="A">{value(a)}</Value>
              <Value tag="B">{value(b)}</Value>
            </div>
          ))}
        </div>
      </div>
    </SectionCard>
  );
}

/** Lines up ingredients only by ingredient_id (BR-76); names alone never merge rows. */
function ingredientRows(a: RecipeDetail, b: RecipeDetail) {
  const rows: Array<{ ingredientId: number; name: string; a: IngredientLine[]; b: IngredientLine[] }> = [];
  const byId = new Map<number, (typeof rows)[number]>();
  for (const [side, recipe] of [['a', a], ['b', b]] as const) {
    for (const line of recipe.ingredients) {
      let row = byId.get(line.ingredientId);
      if (!row) {
        row = { ingredientId: line.ingredientId, name: line.name, a: [], b: [] };
        byId.set(line.ingredientId, row);
        rows.push(row);
      }
      row[side].push(line);
    }
  }
  return rows;
}

function Quantities({ lines }: { lines: IngredientLine[] }) {
  if (lines.length === 0) return <>—</>;
  return <>{lines.map((line, index) => <span key={index} className="block">{formatNumber(line.quantity)} {line.unitName}</span>)}</>;
}

function IngredientTable({ a, b }: { a: RecipeDetail; b: RecipeDetail }) {
  return (
    <SectionCard id="compare-ingredients-heading" title="Đối chiếu nguyên liệu">
      <p className="mt-1 text-xs leading-5 text-ink-muted">
        Số lượng và đơn vị giữ nguyên theo từng công thức. Chỉ cùng một nguyên liệu trong danh mục mới được đặt cùng hàng;
        hệ thống không tự quy đổi đơn vị.
      </p>
      <div role="table" aria-label="Đối chiếu nguyên liệu" className="mt-3">
        <HeaderRow className={ROW_3} first="Nguyên liệu" a={a} b={b} />
        <div role="rowgroup">
          {ingredientRows(a, b).map((row) => {
            const differentUnits = row.a.length > 0 && row.b.length > 0
              && new Set([...row.a, ...row.b].map((line) => line.unitId)).size > 1;
            return (
              <div role="row" key={row.ingredientId} className={ROW_3}>
                <span role="rowheader" className="col-span-2 text-sm font-semibold text-ink-soft sm:col-span-1">
                  {row.name}
                  {differentUnits && <span className="block text-xs font-normal text-amber-800">Khác đơn vị, không so trực tiếp số lượng</span>}
                </span>
                <Value tag="A"><Quantities lines={row.a} /></Value>
                <Value tag="B"><Quantities lines={row.b} /></Value>
              </div>
            );
          })}
        </div>
      </div>
    </SectionCard>
  );
}

function NutritionTable({ a, b }: { a: RecipeDetail; b: RecipeDetail }) {
  const incomplete = ([['A', a], ['B', b]] as const).filter(([, recipe]) => !(recipe.nutrition?.complete ?? recipe.nutritionComplete));
  return (
    <SectionCard id="compare-nutrition-heading" title="Dinh dưỡng trên một khẩu phần">
      <div className="mt-1 space-y-1 text-xs leading-5 text-ink-muted">
        <p>Mỗi cột tính trên một khẩu phần của công thức đó: A có {a.servings} khẩu phần gốc, B có {b.servings} khẩu phần gốc.</p>
        <p>Chênh lệch là A − B theo cùng đơn vị; khẩu phần của hai món có thể không có cùng khối lượng thành phẩm.</p>
      </div>
      <div role="table" aria-label="Dinh dưỡng trên một khẩu phần" className="mt-3">
        <HeaderRow className={ROW_4} first="Chỉ tiêu" a={a} b={b} extra="A − B" />
        <div role="rowgroup">
          {NUTRIENTS.map((nutrient) => {
            const valueA = perServing(a, nutrient.key);
            const valueB = perServing(b, nutrient.key);
            const show = (value: number | null) => (value === null ? MISSING : `${formatNumber(value)} ${nutrient.unit}`);
            return (
              <div role="row" key={nutrient.key} className={ROW_4}>
                <span role="rowheader" className="col-span-3 text-sm font-semibold text-ink-soft sm:col-span-1">{nutrient.label}</span>
                <Value tag="A">{show(valueA)}</Value>
                <Value tag="B">{show(valueB)}</Value>
                <Value tag="A − B">
                  {valueA === null || valueB === null ? '—' : `${formatDifference(valueA - valueB)} ${nutrient.unit}`}
                </Value>
              </div>
            );
          })}
        </div>
      </div>
      {incomplete.map(([slot, recipe]) => (
        <div key={slot} role="note" className="mt-4 rounded-xl border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
          <p className="font-semibold">Công thức {slot} chưa đủ dữ liệu dinh dưỡng</p>
          {(recipe.nutrition?.ingredientsMissingData ?? recipe.ingredientsWithoutNutrition).length > 0 && (
            <p className="mt-1 text-xs">
              Thiếu số liệu cho: {(recipe.nutrition?.ingredientsMissingData ?? recipe.ingredientsWithoutNutrition).join(', ')}.
            </p>
          )}
        </div>
      ))}
      <p className="mt-4 flex items-start gap-2 text-xs leading-5 text-ink-muted">
        <Info className="mt-0.5 h-4 w-4 shrink-0 text-brand-600" aria-hidden="true" />
        Số liệu là ước tính từ danh mục nguyên liệu, chỉ để đối chiếu; Mâm Xanh không chấm điểm hay xếp hạng công thức
        và không thay thế tư vấn của chuyên gia dinh dưỡng.
      </p>
    </SectionCard>
  );
}
