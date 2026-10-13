import { useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { LoaderCircle, Sparkles } from 'lucide-react';
import { recipesApi, type RecipeFormOptions, type RecipePost } from '../api/recipes';
import { asApiError } from '../lib/apiClient';
import { useAuth } from './AuthContext';
import { PublicRecipeCard } from './PublicRecipeCard';

export function RelatedRecipes({ recipeId }: { recipeId: number }) {
  const { isAuthenticated, account } = useAuth();
  const [items, setItems] = useState<RecipePost[]>([]);
  const [options, setOptions] = useState<RecipeFormOptions | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  const [aiItems, setAiItems] = useState<RecipePost[] | null>(null);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState('');
  const [upgradeRequired, setUpgradeRequired] = useState(false);
  const alive = useRef(false);
  const member = isAuthenticated && (account?.role === 'CUSTOMER' || account?.role === 'EXPERT');

  useEffect(() => {
    alive.current = true;
    return () => { alive.current = false; };
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError('');
    recipesApi.related(recipeId).then((data) => {
      if (!Array.isArray(data.items)) throw new Error('Chưa thể tải công thức liên quan.');
      if (active) setItems(data.items);
    }).catch((cause: unknown) => {
      if (active) setError(asApiError(cause).message);
    }).finally(() => {
      if (active) setLoading(false);
    });
    recipesApi.getFormOptions().then((data) => {
      if (active) setOptions(data);
    }).catch(() => { /* Recipe data remains available if reference labels cannot be loaded. */ });
    return () => { active = false; };
  }, [recipeId, retry]);

  async function recommend() {
    if (!member || aiLoading) return;
    setAiLoading(true);
    setAiError('');
    setUpgradeRequired(false);
    try {
      const result = await recipesApi.recommendRelated(recipeId);
      if (!Array.isArray(result.items)) throw new Error('Invalid related recipe response');
      if (alive.current) setAiItems(result.items);
    } catch (cause) {
      if (!alive.current) return;
      const upgrade = asApiError(cause).code === 'AI_SUBSCRIPTION_REQUIRED';
      setUpgradeRequired(upgrade);
      setAiError(upgrade ? 'Gợi ý AI dành cho gói Plus và Pro.'
        : 'Chưa thể lấy gợi ý từ AI. Bạn có thể thử lại hoặc xem các công thức liên quan bên dưới.');
    } finally {
      if (alive.current) setAiLoading(false);
    }
  }

  return <section aria-label="Công thức liên quan" className="recipe-detail-no-print mt-10 border-t border-brand-200 pt-8">
    <div className="flex flex-wrap items-center justify-between gap-4">
      <h2 className="text-xl font-extrabold text-ink">Công thức liên quan</h2>
      {member && <button type="button" onClick={recommend} disabled={aiLoading}
        className="inline-flex max-w-full items-center gap-2 rounded-lg border border-brand-500 px-4 py-2.5 text-left text-sm font-semibold text-brand-700 transition hover:bg-brand-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500 disabled:cursor-wait disabled:opacity-60">
        {aiLoading ? <LoaderCircle aria-hidden="true" className="h-4 w-4 animate-spin" /> : <Sparkles aria-hidden="true" className="h-4 w-4" />}
        {aiLoading ? 'Đang tìm món phù hợp…' : 'AI gợi ý món ăn kèm hoặc biến tấu'}
      </button>}
      {!isAuthenticated && <Link to="/dang-nhap" className="text-sm font-semibold text-brand-700">Đăng nhập để dùng gợi ý AI</Link>}
    </div>
    {aiLoading && <p role="status" className="mt-3 text-sm text-ink-muted">AI đang lựa chọn từ các công thức có sẵn.</p>}
    {aiError && <div role="alert" className="mt-4 text-sm text-ink-muted">{aiError}
      {upgradeRequired && <Link to="/goi-ai" className="ml-2 font-semibold text-brand-700">Xem các gói</Link>}
    </div>}
    {aiItems !== null && <div className="mt-6">
      <h3 className="mb-4 text-base font-bold text-ink">Gợi ý từ AI</h3>
      {aiItems.length > 0 && <p className="mb-4 text-sm text-ink-muted">Các món cùng loại chay, được AI chọn để ăn kèm hoặc đổi món.</p>}
      {aiItems.length === 0 ? <p className="text-sm text-ink-muted">AI chưa tìm được món phù hợp trong các công thức hiện có.</p>
        : <RecipeGrid items={aiItems} options={options} />}
    </div>}
    <div className="mt-5"><RegularRecommendations loading={loading} error={error} items={items}
      options={options} onRetry={() => setRetry((value) => value + 1)} /></div>
  </section>;
}

function RecipeGrid({ items, options }: { items: RecipePost[]; options: RecipeFormOptions | null }) {
  return <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">{items.map((item) =>
    <PublicRecipeCard key={item.id} recipe={item} options={options} />)}</div>;
}

function RegularRecommendations({ loading, error, items, options, onRetry }: {
  loading: boolean; error: string; items: RecipePost[]; options: RecipeFormOptions | null; onRetry: () => void;
}) {
  if (loading) return <p role="status" className="text-sm text-ink-muted">Đang tải công thức liên quan…</p>;
  if (error) return <div role="alert" className="text-sm text-ink-muted">{error}
    <button type="button" onClick={onRetry} className="ml-3 font-semibold text-brand-700">Thử lại</button></div>;
  if (items.length === 0) return <p className="text-sm text-ink-muted">Chưa có công thức liên quan.</p>;
  return <RecipeGrid items={items} options={options} />;
}
