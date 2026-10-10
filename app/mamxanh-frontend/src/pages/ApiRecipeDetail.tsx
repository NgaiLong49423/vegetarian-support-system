import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { ArrowLeft, Clock3, Users } from 'lucide-react';
import { PageContainer } from '../components/Layout';
import { Card } from '../components/ui';
import { MemberAvatar } from '../components/MemberAvatar';
import { asApiError } from '../lib/apiClient';
import { recipesApi, type RecipeDetail } from '../api/recipes';

/** `publishedAt` is UTC without an offset; show the date in Vietnam time. */
function publishedDate(value: string) {
  return new Date(`${value}Z`).toLocaleDateString('vi-VN', { timeZone: 'Asia/Ho_Chi_Minh' });
}

export function ApiRecipeDetail() {
  const { recipeId: rawId } = useParams();
  const recipeId = Number(rawId);
  const [recipe, setRecipe] = useState<RecipeDetail | null>(null);
  const [error, setError] = useState('');
  useEffect(() => {
    let alive = true;
    if (!Number.isSafeInteger(recipeId) || recipeId < 1) { setError('ID công thức không hợp lệ.'); return; }
    recipesApi.getPublished(recipeId).then((data) => { if (alive) setRecipe(data); })
      .catch((cause: unknown) => { if (alive) setError(asApiError(cause).status === 404 ? 'Công thức không còn công khai hoặc đã bị xóa.' : asApiError(cause).message); });
    return () => { alive = false; };
  }, [recipeId]);

  if (error) return <PageContainer className="py-12"><Card className="mx-auto max-w-xl p-6"><h1 className="text-xl font-extrabold text-ink">Không thể mở công thức</h1><p className="mt-2 text-sm text-ink-muted">{error}</p><Link to="/kham-pha" className="mt-5 inline-flex items-center gap-2 text-sm font-semibold text-brand-700"><ArrowLeft className="h-4 w-4" />Khám phá công thức</Link></Card></PageContainer>;
  if (!recipe) return <PageContainer className="py-16 text-center text-ink-muted">Đang tải công thức…</PageContainer>;

  const cover = recipe.media.find((media) => media.cover) ?? recipe.media[0];
  return <PageContainer className="py-8">
    <Link to="/kham-pha" className="mb-5 inline-flex items-center gap-2 text-sm font-semibold text-brand-700"><ArrowLeft className="h-4 w-4" />Khám phá</Link>
    <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
      <main><Card className="overflow-hidden"><div className="aspect-[16/8] bg-brand-50">{cover && <img src={cover.blobUrl} alt={recipe.title} className="h-full w-full object-cover" />}</div><div className="p-5 sm:p-7"><div className="flex flex-wrap gap-2 text-xs font-semibold text-brand-700"><span className="rounded-full bg-brand-50 px-3 py-1">{recipe.vegetarianTypeLabel}</span><span className="rounded-full bg-brand-50 px-3 py-1">{recipe.dishCategoryLabel}</span><span className="rounded-full bg-brand-50 px-3 py-1">{recipe.difficultyLabel}</span></div><h1 className="mt-4 text-3xl font-extrabold text-ink">{recipe.title}</h1>{recipe.author && <div className="mt-4 flex items-center gap-3"><MemberAvatar name={recipe.author.displayName} url={recipe.author.avatarUrl} /><div><Link to={`/thanh-vien/${recipe.author.userId}`} className="font-bold text-ink hover:text-brand-700">{recipe.author.displayName}</Link><p className="text-xs text-ink-muted">Đăng ngày {publishedDate(recipe.publishedAt)}</p></div></div>}{recipe.description && <p className="mt-3 leading-7 text-ink-muted">{recipe.description}</p>}<div className="mt-4 flex flex-wrap gap-4 text-sm text-ink-muted"><span className="inline-flex items-center gap-1.5"><Clock3 className="h-4 w-4" />{recipe.prepTimeMinutes + recipe.cookTimeMinutes} phút</span><span className="inline-flex items-center gap-1.5"><Users className="h-4 w-4" />{recipe.servings} khẩu phần</span></div></div></Card>
        <Card className="mt-5 p-5 sm:p-7"><h2 className="text-xl font-extrabold text-ink">Cách làm</h2><p className="mt-4 whitespace-pre-line leading-7 text-ink-soft">{recipe.instructions}</p></Card>
      </main>
      <aside className="space-y-4"><Card className="p-5"><h2 className="text-lg font-extrabold text-ink">Nguyên liệu</h2><ul className="mt-3 divide-y divide-brand-100">{recipe.ingredients.map((item, index) => <li key={`${item.ingredientId}-${index}`} className="flex justify-between gap-3 py-3 text-sm"><span className="text-ink-soft">{item.name}</span><strong className="shrink-0 text-ink">{item.quantity} {item.unitName}</strong></li>)}</ul><p className="mt-2 text-xs text-ink-muted">Cho {recipe.servings} khẩu phần</p></Card></aside>
    </div>
  </PageContainer>;
}
