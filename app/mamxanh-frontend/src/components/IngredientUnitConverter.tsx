import { useCallback, useEffect, useState } from 'react';
import { ArrowLeftRight, LoaderCircle, RefreshCw, Scale } from 'lucide-react';
import { Button } from './ui';
import { discoveryApi, type PublicIngredientUnit } from '../api/discovery';

const UNIT_LABELS: Record<string, string> = {
  mg: 'mg', g: 'g', kg: 'kg', ml: 'ml', L: 'L',
  tsp: 'tsp · muỗng cà phê', tbsp: 'tbsp · muỗng canh', cup: 'cup · 240 ml',
  quả: 'quả', củ: 'củ', miếng: 'miếng',
};

function formatQuantity(value: number) {
  if (!Number.isFinite(value)) return '';
  return String(Number(value.toFixed(8)));
}

function convert(value: string, from: PublicIngredientUnit, to: PublicIngredientUnit) {
  if (!value.trim()) return '';
  const amount = Number(value.replace(',', '.'));
  if (!Number.isFinite(amount) || amount < 0 || from.gramsPerUnit <= 0 || to.gramsPerUnit <= 0) return '';
  return formatQuantity((amount * from.gramsPerUnit) / to.gramsPerUnit);
}

export function IngredientUnitConverter({ ingredientId, ingredientName }: { ingredientId: number; ingredientName: string }) {
  const [units, setUnits] = useState<PublicIngredientUnit[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [retry, setRetry] = useState(0);
  const [leftValue, setLeftValue] = useState('1');
  const [rightValue, setRightValue] = useState('1');
  const [leftUnitId, setLeftUnitId] = useState<number | null>(null);
  const [rightUnitId, setRightUnitId] = useState<number | null>(null);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setError(false);
    discoveryApi.getIngredientUnits(ingredientId)
      .then((options) => {
        if (!current) return;
        setUnits(options);
        const grams = options.find((unit) => unit.code === 'g') ?? options[0];
        const target = options.find((unit) => unit.dimension !== 'MASS')
          ?? options.find((unit) => unit.code === 'kg')
          ?? grams;
        setLeftUnitId(grams?.id ?? null);
        setRightUnitId(target?.id ?? grams?.id ?? null);
        const initialAmount = target?.gramsPerUnit ?? 1;
        const initialValue = formatQuantity(initialAmount);
        setLeftValue(initialValue);
        setRightValue(grams && target ? convert(initialValue, grams, target) : '');
      })
      .catch(() => { if (current) setError(true); })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [ingredientId, retry]);

  const leftUnit = units.find((unit) => unit.id === leftUnitId);
  const rightUnit = units.find((unit) => unit.id === rightUnitId);
  const convertFromLeft = useCallback((value: string, source = leftUnit, target = rightUnit) => {
    setLeftValue(value);
    if (source && target) setRightValue(convert(value, source, target));
  }, [leftUnit, rightUnit]);
  const convertFromRight = useCallback((value: string, source = rightUnit, target = leftUnit) => {
    setRightValue(value);
    if (source && target) setLeftValue(convert(value, source, target));
  }, [leftUnit, rightUnit]);

  const changeLeftUnit = (id: number) => {
    const next = units.find((unit) => unit.id === id);
    setLeftUnitId(id);
    if (next && rightUnit) setRightValue(convert(leftValue, next, rightUnit));
  };
  const changeRightUnit = (id: number) => {
    const next = units.find((unit) => unit.id === id);
    setRightUnitId(id);
    if (leftUnit && next) setRightValue(convert(leftValue, leftUnit, next));
  };

  return <section aria-labelledby="ingredient-unit-converter-title" className="mt-6 rounded-3xl border border-brand-100 bg-paper p-5 shadow-sm sm:p-8">
    <div className="flex items-start gap-3 border-b border-brand-100 pb-4">
      <span className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-brand-100 text-brand-700"><Scale aria-hidden="true" className="size-5" /></span>
      <div>
        <h2 id="ingredient-unit-converter-title" className="text-xl font-extrabold text-ink sm:text-2xl">Quy đổi đơn vị</h2>
        <p className="mt-1 text-sm leading-5 text-ink-muted">Quy đổi theo dữ liệu đang hỗ trợ cho {ingredientName}.</p>
      </div>
    </div>

    {loading ? <div role="status" className="flex items-center justify-center gap-2 py-8 text-sm text-ink-muted"><LoaderCircle aria-hidden="true" className="size-4 animate-spin" />Đang tải đơn vị hỗ trợ…</div>
      : error ? <div role="alert" className="flex flex-wrap items-center justify-between gap-3 py-5 text-sm text-red-700"><span>Không tải được đơn vị quy đổi.</span><Button type="button" variant="outline" onClick={() => setRetry((value) => value + 1)}><RefreshCw aria-hidden="true" className="mr-2 size-4" />Thử lại</Button></div>
        : units.length === 0 ? <p className="py-6 text-sm text-ink-muted">Nguyên liệu này chưa có đơn vị quy đổi khả dụng.</p>
          : <>
            <div className="mt-5 grid items-center gap-3 sm:grid-cols-[minmax(0,1fr)_auto_minmax(0,1fr)]">
              <div className="space-y-2">
                <label className="sr-only" htmlFor="ingredient-convert-left-value">Số lượng thứ nhất</label>
                <input id="ingredient-convert-left-value" type="number" min="0" step="any" inputMode="decimal" value={leftValue} onChange={(event) => convertFromLeft(event.target.value)}
                  className="w-full rounded-xl border border-brand-200 bg-paper px-3.5 py-3 text-lg font-semibold tabular-nums text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100" />
                <label className="sr-only" htmlFor="ingredient-convert-left-unit">Đơn vị thứ nhất</label>
                <select id="ingredient-convert-left-unit" value={leftUnitId ?? ''} onChange={(event) => changeLeftUnit(Number(event.target.value))}
                  className="w-full rounded-xl border border-brand-200 bg-raised px-3.5 py-2.5 text-sm font-semibold text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100">
                  {units.map((unit) => <option key={unit.id} value={unit.id}>{UNIT_LABELS[unit.code] ?? unit.name}</option>)}
                </select>
              </div>
              <ArrowLeftRight aria-label="bằng" className="mx-auto size-5 text-brand-600" />
              <div className="space-y-2">
                <label className="sr-only" htmlFor="ingredient-convert-right-value">Số lượng quy đổi</label>
                <input id="ingredient-convert-right-value" type="number" min="0" step="any" inputMode="decimal" value={rightValue} onChange={(event) => convertFromRight(event.target.value)}
                  className="w-full rounded-xl border border-brand-200 bg-paper px-3.5 py-3 text-lg font-semibold tabular-nums text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100" />
                <label className="sr-only" htmlFor="ingredient-convert-right-unit">Đơn vị quy đổi</label>
                <select id="ingredient-convert-right-unit" value={rightUnitId ?? ''} onChange={(event) => changeRightUnit(Number(event.target.value))}
                  className="w-full rounded-xl border border-brand-200 bg-raised px-3.5 py-2.5 text-sm font-semibold text-ink outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100">
                  {units.map((unit) => <option key={unit.id} value={unit.id}>{UNIT_LABELS[unit.code] ?? unit.name}</option>)}
                </select>
              </div>
            </div>
            {units.some((unit) => unit.approximate) && <p className="mt-3 text-xs leading-5 text-ink-muted">Một số quy đổi của nguyên liệu này là ước tính; kết quả mang tính tham khảo.</p>}
          </>}
  </section>;
}
