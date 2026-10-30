import { useEffect, useState } from 'react';
import { ArrowLeft, Leaf, LoaderCircle, Search } from 'lucide-react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { PageContainer } from '../components/Layout';
import { Button, EmptyState } from '../components/ui';
import { IngredientImage } from '../components/IngredientImage';
import { IngredientRecipes } from '../components/IngredientRecipes';
import { IngredientUnitConverter } from '../components/IngredientUnitConverter';
import { discoveryApi, type PublicIngredientDetail, type PublicIngredientNutrition } from '../api/discovery';
import { asApiError } from '../lib/apiClient';

const NUTRIENTS: Array<{ key: keyof PublicIngredientNutrition; label: string; unit: string }> = [
  { key: 'energyKcalPer100g', label: 'Năng lượng', unit: 'kcal' },
  { key: 'proteinGramsPer100g', label: 'Chất đạm', unit: 'g' },
  { key: 'carbohydrateGramsPer100g', label: 'Carbohydrate', unit: 'g' },
  { key: 'totalFatGramsPer100g', label: 'Chất béo', unit: 'g' },
  { key: 'fiberGramsPer100g', label: 'Chất xơ', unit: 'g' },
  { key: 'calciumMgPer100g', label: 'Canxi', unit: 'mg' },
  { key: 'ironMgPer100g', label: 'Sắt', unit: 'mg' },
  { key: 'vitaminB12MicrogramsPer100g', label: 'Vitamin B12', unit: 'µg' },
  { key: 'zincMgPer100g', label: 'Kẽm', unit: 'mg' },
];

function formatValue(value: number | null, unit: string) {
  if (value === null || !Number.isFinite(value)) return 'Chưa có dữ liệu';
  return `${new Intl.NumberFormat('vi-VN', { maximumFractionDigits: unit === 'µg' ? 4 : 2 }).format(value)} ${unit}`;
}

export function IngredientDetailPage() {
  const { ingredientId: rawId } = useParams();
  const navigate = useNavigate();
  const ingredientId = Number(rawId);
  const [ingredient, setIngredient] = useState<PublicIngredientDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<'not-found' | 'unavailable' | null>(null);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    let active = true;
    if (!Number.isSafeInteger(ingredientId) || ingredientId < 1) {
      setLoading(false);
      setError('not-found');
      return () => { active = false; };
    }
    setLoading(true);
    setError(null);
    discoveryApi.getIngredient(ingredientId)
      .then((result) => { if (active) setIngredient(result); })
      .catch((reason) => { if (active) setError(asApiError(reason).status === 404 ? 'not-found' : 'unavailable'); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [ingredientId, retry]);

  const findRecipes = () => {
    if (!ingredient) return;
    const params = new URLSearchParams({ ingredientId: String(ingredient.ingredientId), ingredientName: ingredient.name });
    navigate(`/kham-pha?${params.toString()}`);
  };

  return <PageContainer className="py-8 sm:py-10">
    <Link to="/danh-muc-nguyen-lieu" className="inline-flex items-center gap-2 text-sm font-semibold text-ink-muted transition-colors hover:text-brand-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-leaf-600">
      <ArrowLeft aria-hidden="true" className="h-4 w-4" /> Danh mục nguyên liệu
    </Link>

    {loading && <div role="status" className="mx-auto flex min-h-64 max-w-4xl items-center justify-center gap-3 text-sm text-ink-muted"><LoaderCircle aria-hidden="true" className="h-5 w-5 animate-spin" />Đang tải thông tin nguyên liệu…</div>}
    {!loading && error && <div className="mx-auto mt-6 max-w-2xl"><EmptyState title={error === 'not-found' ? 'Không tìm thấy nguyên liệu' : 'Không tải được nguyên liệu'} description={error === 'not-found' ? 'Nguyên liệu không tồn tại hoặc đã ngừng hoạt động.' : 'Kết nối đang gặp sự cố. Vui lòng thử tải lại.'} action={error === 'not-found' ? <Button variant="secondary" onClick={() => navigate('/danh-muc-nguyen-lieu')}>Về danh mục</Button> : <Button variant="secondary" onClick={() => setRetry((current) => current + 1)}>Thử lại</Button>} /></div>}
    {!loading && !error && ingredient && <article className="mx-auto mt-6 max-w-5xl">
      <section className="grid overflow-hidden rounded-3xl border border-brand-100 bg-paper shadow-sm md:grid-cols-[minmax(0,0.9fr)_minmax(0,1.1fr)]">
        <div className="flex min-h-64 items-center justify-center bg-raised p-6 sm:min-h-80 sm:p-10">
          <IngredientImage imageUrl={ingredient.imageUrl} name={ingredient.name} loading="eager" placeholderSize="large" containerClassName="flex min-h-52 w-full max-w-md items-center justify-center overflow-hidden rounded-2xl border border-brand-100 bg-paper/70" imageClassName="max-h-80 w-full object-contain" />
        </div>
        <div className="flex flex-col justify-center p-6 sm:p-9">
          <p className="text-xs font-bold uppercase tracking-[0.16em] text-brand-700">Nguyên liệu Mâm Xanh</p>
          <h1 className="mt-3 text-3xl font-extrabold tracking-tight text-ink sm:text-4xl">{ingredient.name}</h1>
          <span className="mt-4 inline-flex w-fit items-center gap-2 rounded-full bg-leaf-100 px-3 py-1.5 text-sm font-semibold text-leaf-800"><Leaf aria-hidden="true" className="h-4 w-4" />{ingredient.ingredientGroup}</span>
          <p className="mt-5 max-w-xl text-sm leading-6 text-ink-muted">Thông tin dinh dưỡng cho mỗi 100 g nguyên liệu. Giá trị chưa được cập nhật sẽ được ghi rõ là chưa có dữ liệu.</p>
          <div className="mt-7 flex flex-wrap gap-3">
            <Button variant="primary" onClick={findRecipes}><Search aria-hidden="true" className="mr-2 inline h-4 w-4" />Tìm công thức</Button>
            <Button variant="secondary" onClick={() => navigate('/danh-muc-nguyen-lieu')}>Quay lại danh mục</Button>
          </div>
        </div>
      </section>

      <section aria-labelledby="ingredient-nutrition-title" className="mt-6 rounded-3xl border border-brand-100 bg-paper p-5 shadow-sm sm:p-8">
        <div className="flex flex-wrap items-end justify-between gap-3 border-b border-brand-100 pb-5">
          <div>
            <p className="text-xs font-bold uppercase tracking-[0.16em] text-brand-700">Thành phần dinh dưỡng</p>
            <h2 id="ingredient-nutrition-title" className="mt-1 text-xl font-extrabold text-ink sm:text-2xl">Giá trị trên 100 g</h2>
          </div>
          <span className="rounded-full bg-raised px-3 py-1.5 text-xs font-semibold text-ink-muted">9 chỉ tiêu</span>
        </div>
        {NUTRIENTS.every(({ key }) => ingredient.nutrition[key] === null) ? <div className="py-10 text-center"><p className="font-semibold text-ink">Chưa có dữ liệu dinh dưỡng</p><p className="mt-1 text-sm text-ink-muted">Các chỉ tiêu sẽ hiển thị tại đây khi được cập nhật.</p></div> : <dl className="mt-4 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
          {NUTRIENTS.map(({ key, label, unit }) => {
            const value = ingredient.nutrition[key];
            const hasValue = value !== null;
            return <div key={key} className={`flex min-h-16 items-center justify-between gap-3 rounded-2xl border px-4 py-3 ${hasValue ? 'border-brand-100 bg-raised/50' : 'border-dashed border-brand-100 bg-paper'}`}>
              <dt className="text-sm font-medium text-ink-soft">{label}</dt>
              <dd className={`text-right text-sm font-bold tabular-nums ${hasValue ? 'text-ink' : 'text-ink-muted'}`}>{formatValue(value, unit)}</dd>
            </div>;
          })}
        </dl>}
        <p className="mt-5 border-t border-brand-100 pt-4 text-xs leading-5 text-ink-muted">Giá trị có thể chưa đầy đủ và thay đổi theo loại nguyên liệu, giống và cách chế biến. Đây là thông tin tham khảo, không thay thế tư vấn dinh dưỡng cá nhân.</p>
      </section>
      <IngredientUnitConverter key={`unit-converter-${ingredient.ingredientId}`} ingredientId={ingredient.ingredientId} ingredientName={ingredient.name} />
      <IngredientRecipes key={ingredient.ingredientId} ingredientId={ingredient.ingredientId} name={ingredient.name} />
    </article>}
  </PageContainer>;
}
