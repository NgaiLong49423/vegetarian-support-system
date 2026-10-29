import { type ReactNode } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ArrowLeftRight, Info, Leaf, LoaderCircle, Plus } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Button } from '../components/ui';
import type { RecipeDetail } from '../api/recipes';
import {
  parseRecipeId, pickerHref, usePublishedRecipe, type ComparisonSide, type PublishedRecipeState,
} from '../lib/recipeComparison';

// FR-60 / BR-76: compare exactly two different PUBLISHED recipes per serving. The page only reads
// public data (FR-20 detail with FR-39 nutrition); the selection lives in the URL, nothing is stored.
// Recipes are chosen on the separate picker page (/so-sanh/chon-mon), never in a list on this page.

const SIDE_LABEL: Record<ComparisonSide, string> = { first: 'Công thức đầu tiên', second: 'Công thức thứ hai' };
const SIDE_NAME: Record<ComparisonSide, string> = { first: 'công thức đầu tiên', second: 'công thức thứ hai' };
const DIFFERENCE = 'Chênh lệch';

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

type IngredientLine = RecipeDetail['ingredients'][number];

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

export function RecipeComparePage() {
  const [params] = useSearchParams();
  const firstId = parseRecipeId(params.get('a'));
  const requestedSecond = parseRecipeId(params.get('b'));
  const sameRecipe = firstId !== null && firstId === requestedSecond;
  const secondId = sameRecipe ? null : requestedSecond;
  const first = usePublishedRecipe(firstId);
  const second = usePublishedRecipe(secondId);

  // An unavailable recipe is dropped from the picker link so the user chooses a replacement for it.
  const keptFirst = first.state.status === 'unavailable' ? null : firstId;
  const keptSecond = second.state.status === 'unavailable' ? null : secondId;
  const both = first.state.status === 'ready' && second.state.status === 'ready' ? [first.state.recipe, second.state.recipe] as const : null;
  const selectedCount = [first.state, second.state].filter((state) => state.status === 'ready').length;

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
          Hai công thức phải khác nhau. Hãy chọn một công thức thứ hai khác.
        </p>
      )}

      <div className="grid gap-4 md:grid-cols-2">
        <SlotPanel side="first" state={first.state} onRetry={first.retry} pickHref={pickerHref('first', keptFirst, keptSecond)}>
          <p className="mt-3 text-sm text-ink-muted">Chưa chọn công thức nào.</p>
          <PickLink href={pickerHref('first', keptFirst, keptSecond)} label="Chọn công thức đầu tiên" />
        </SlotPanel>
        <SlotPanel side="second" state={second.state} onRetry={second.retry} pickHref={pickerHref('second', keptFirst, keptSecond)}>
          {firstId === null ? (
            <p className="mt-3 text-sm text-ink-muted">Hãy chọn công thức đầu tiên trước, sau đó chọn món thứ hai để so sánh.</p>
          ) : (
            <>
              <p className="mt-3 text-sm text-ink-muted">Tìm và chọn món bạn muốn đặt cạnh công thức đầu tiên.</p>
              <PickLink href={pickerHref('second', keptFirst, keptSecond)} label="Chọn công thức thứ hai" />
            </>
          )}
        </SlotPanel>
      </div>

      {both ? (
        <Comparison first={both[0]} second={both[1]} />
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

function PickLink({ href, label }: { href: string; label: string }) {
  return (
    <Link to={href} className="mt-3 inline-block rounded-xl">
      <Button as="span" size="sm"><Plus className="h-4 w-4" aria-hidden="true" />{label}</Button>
    </Link>
  );
}

/** One side of the comparison; `children` is what an empty side shows. */
function SlotPanel({ side, state, onRetry, pickHref, children }: {
  side: ComparisonSide;
  state: PublishedRecipeState;
  onRetry: () => void;
  pickHref: string;
  children: ReactNode;
}) {
  const headingId = `compare-slot-${side}`;
  return (
    <section aria-labelledby={headingId} className="min-w-0 rounded-2xl border border-brand-100 bg-paper p-4 sm:p-5">
      <h2 id={headingId} className="text-xs font-bold uppercase tracking-wider text-ink-muted">{SIDE_LABEL[side]}</h2>
      {state.status === 'empty' && children}
      {state.status === 'loading' && (
        <p role="status" className="mt-3 flex items-center gap-2 text-sm text-ink-muted">
          <LoaderCircle className="h-4 w-4 animate-spin" aria-hidden="true" />Đang tải công thức…
        </p>
      )}
      {state.status === 'unavailable' && (
        <>
          <p role="alert" className="mt-3 rounded-xl border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
            {SIDE_LABEL[side]} không còn công khai. Hãy chọn công thức khác.
          </p>
          <PickLink href={pickHref} label="Chọn công thức khác" />
        </>
      )}
      {state.status === 'error' && (
        <div role="alert" className="mt-3 rounded-xl border border-brand-200 bg-cream p-3 text-sm text-ink-muted">
          <p>{state.message}</p>
          <button type="button" onClick={onRetry} className="mt-2 font-semibold text-brand-600 hover:text-brand-700">
            Thử tải lại<span className="sr-only"> {SIDE_NAME[side]}</span>
          </button>
        </div>
      )}
      {state.status === 'ready' && (
        <RecipeSummary recipe={state.recipe}>
          <Link to={pickHref} className="inline-flex items-center gap-1 text-xs font-semibold text-brand-600 hover:text-brand-700">
            <ArrowLeftRight className="h-3.5 w-3.5" aria-hidden="true" />Đổi công thức<span className="sr-only"> {side === 'first' ? 'đầu tiên' : 'thứ hai'}</span>
          </Link>
        </RecipeSummary>
      )}
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

function Comparison({ first, second }: { first: RecipeDetail; second: RecipeDetail }) {
  return (
    <div className="mt-8 space-y-6">
      <OverviewTable first={first} second={second} />
      <IngredientTable first={first} second={second} />
      <NutritionTable first={first} second={second} />
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

function HeaderRow({ className, label, first, second, extra }: { className: string; label: string; first: RecipeDetail; second: RecipeDetail; extra?: string }) {
  return (
    <div role="rowgroup" className="hidden sm:block">
      <div role="row" className={`${className} text-xs font-bold uppercase tracking-wide text-ink-muted`}>
        <span role="columnheader">{label}</span>
        <span role="columnheader" className="min-w-0 break-words">{first.title}</span>
        <span role="columnheader" className="min-w-0 break-words">{second.title}</span>
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

function OverviewTable({ first, second }: { first: RecipeDetail; second: RecipeDetail }) {
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
        <HeaderRow className={ROW_3} label="Thông tin" first={first} second={second} />
        <div role="rowgroup">
          {rows.map(([label, value]) => (
            <div role="row" key={label} className={ROW_3}>
              <span role="rowheader" className="col-span-2 text-sm font-semibold text-ink-soft sm:col-span-1">{label}</span>
              <Value tag={SIDE_LABEL.first}>{value(first)}</Value>
              <Value tag={SIDE_LABEL.second}>{value(second)}</Value>
            </div>
          ))}
        </div>
      </div>
    </SectionCard>
  );
}

/** Lines up ingredients only by ingredient_id (BR-76); names alone never merge rows. */
function ingredientRows(first: RecipeDetail, second: RecipeDetail) {
  const rows: Array<{ ingredientId: number; name: string; first: IngredientLine[]; second: IngredientLine[] }> = [];
  const byId = new Map<number, (typeof rows)[number]>();
  for (const [side, recipe] of [['first', first], ['second', second]] as const) {
    for (const line of recipe.ingredients) {
      let row = byId.get(line.ingredientId);
      if (!row) {
        row = { ingredientId: line.ingredientId, name: line.name, first: [], second: [] };
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

function IngredientTable({ first, second }: { first: RecipeDetail; second: RecipeDetail }) {
  return (
    <SectionCard id="compare-ingredients-heading" title="Đối chiếu nguyên liệu">
      <p className="mt-1 text-xs leading-5 text-ink-muted">
        Số lượng và đơn vị giữ nguyên theo từng công thức. Chỉ cùng một nguyên liệu trong danh mục mới được đặt cùng hàng;
        hệ thống không tự quy đổi đơn vị.
      </p>
      <div role="table" aria-label="Đối chiếu nguyên liệu" className="mt-3">
        <HeaderRow className={ROW_3} label="Nguyên liệu" first={first} second={second} />
        <div role="rowgroup">
          {ingredientRows(first, second).map((row) => {
            const differentUnits = row.first.length > 0 && row.second.length > 0
              && new Set([...row.first, ...row.second].map((line) => line.unitId)).size > 1;
            return (
              <div role="row" key={row.ingredientId} className={ROW_3}>
                <span role="rowheader" className="col-span-2 text-sm font-semibold text-ink-soft sm:col-span-1">
                  {row.name}
                  {differentUnits && <span className="block text-xs font-normal text-amber-800">Khác đơn vị, không so trực tiếp số lượng</span>}
                </span>
                <Value tag={SIDE_LABEL.first}><Quantities lines={row.first} /></Value>
                <Value tag={SIDE_LABEL.second}><Quantities lines={row.second} /></Value>
              </div>
            );
          })}
        </div>
      </div>
    </SectionCard>
  );
}

function NutritionTable({ first, second }: { first: RecipeDetail; second: RecipeDetail }) {
  const incomplete = [first, second].filter((recipe) => !(recipe.nutrition?.complete ?? recipe.nutritionComplete));
  return (
    <SectionCard id="compare-nutrition-heading" title="Dinh dưỡng trên một khẩu phần">
      <div className="mt-1 space-y-1 text-xs leading-5 text-ink-muted">
        <p>
          Mỗi cột tính trên một khẩu phần của công thức đó: {first.title} có {first.servings} khẩu phần gốc,
          {' '}{second.title} có {second.servings} khẩu phần gốc.
        </p>
        <p>
          Chênh lệch = công thức đầu tiên − công thức thứ hai, theo cùng đơn vị, khẩu phần của hai món có thể không có cùng
          khối lượng thành phẩm.
        </p>
      </div>
      <div role="table" aria-label="Dinh dưỡng trên một khẩu phần" className="mt-3">
        <HeaderRow className={ROW_4} label="Chỉ tiêu" first={first} second={second} extra={DIFFERENCE} />
        <div role="rowgroup">
          {NUTRIENTS.map((nutrient) => {
            const valueFirst = perServing(first, nutrient.key);
            const valueSecond = perServing(second, nutrient.key);
            const show = (value: number | null) => (value === null ? MISSING : `${formatNumber(value)} ${nutrient.unit}`);
            return (
              <div role="row" key={nutrient.key} className={ROW_4}>
                <span role="rowheader" className="col-span-3 text-sm font-semibold text-ink-soft sm:col-span-1">{nutrient.label}</span>
                <Value tag={SIDE_LABEL.first}>{show(valueFirst)}</Value>
                <Value tag={SIDE_LABEL.second}>{show(valueSecond)}</Value>
                <Value tag={DIFFERENCE}>
                  {valueFirst === null || valueSecond === null ? '—' : `${formatDifference(valueFirst - valueSecond)} ${nutrient.unit}`}
                </Value>
              </div>
            );
          })}
        </div>
      </div>
      {incomplete.map((recipe) => (
        <div key={recipe.recipeId} role="note" className="mt-4 rounded-xl border border-amber-300 bg-amber-50 p-3 text-sm text-amber-900">
          <p className="font-semibold">{recipe.title} chưa đủ dữ liệu dinh dưỡng</p>
          {(recipe.nutrition?.ingredientsMissingData ?? recipe.ingredientsWithoutNutrition).length > 0 && (
            <p className="mt-1 text-xs">
              Thiếu số liệu cho: {(recipe.nutrition?.ingredientsMissingData ?? recipe.ingredientsWithoutNutrition).join(', ')}.
            </p>
          )}
        </div>
      ))}
      <p className="mt-4 flex items-start gap-2 text-xs leading-5 text-ink-muted">
        <Info className="mt-0.5 h-4 w-4 shrink-0 text-brand-600" aria-hidden="true" />
        Số liệu là ước tính từ danh mục nguyên liệu, chỉ để đối chiếu, Mâm Xanh không chấm điểm hay xếp hạng công thức
        và không thay thế tư vấn của chuyên gia dinh dưỡng.
      </p>
    </SectionCard>
  );
}
