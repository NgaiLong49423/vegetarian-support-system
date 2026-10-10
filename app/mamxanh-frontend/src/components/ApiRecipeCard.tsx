import { Link } from 'react-router-dom';
import { Clock, Users } from 'lucide-react';
import type { RecipePost } from '../api/recipes';

/** Card for a published Recipe Post returned by the API (Explore, public member profile). */
export function ApiRecipeCard({ recipe, showLikePercentage = false, showAuthor = true }: {
  recipe: RecipePost;
  showLikePercentage?: boolean;
  showAuthor?: boolean;
}) {
  const image = recipe.media.find((item) => item.cover) ?? recipe.media[0];
  return <Link to={`/cong-thuc/id/${recipe.id}`} className="group overflow-hidden rounded-2xl border border-brand-100 bg-white transition-all hover:-translate-y-1 hover:shadow-xl">
    <div className="aspect-[4/3] bg-brand-50">{image ? <img src={image.url} alt={recipe.title} className="h-full w-full object-cover transition-transform group-hover:scale-105" /> : <div className="flex h-full items-center justify-center text-sm text-ink-muted">Ảnh công thức</div>}</div>
    <div className="p-4"><div className="flex flex-wrap gap-2 text-xs font-semibold text-brand-700"><span className="rounded-full bg-brand-50 px-2.5 py-1">{recipe.vegetarianType}</span><span className="rounded-full bg-brand-50 px-2.5 py-1">{recipe.dishCategory}</span>{showLikePercentage && <span aria-label="Tỷ lệ yêu thích" className="rounded-full bg-brand-50 px-2.5 py-1">{recipe.likePercentage === null ? 'Mới' : `👍 ${recipe.likePercentage}%`}</span>}</div><h2 className="mt-3 line-clamp-2 font-bold text-ink group-hover:text-brand-700">{recipe.title}</h2><p className="mt-2 flex items-center gap-4 text-xs text-ink-muted"><span className="inline-flex items-center gap-1"><Clock className="h-3.5 w-3.5" />{recipe.prepTimeMin + recipe.cookTimeMin} phút</span><span className="inline-flex items-center gap-1"><Users className="h-3.5 w-3.5" />{recipe.servings} khẩu phần</span></p>{showAuthor && <p className="mt-3 border-t border-brand-50 pt-3 text-xs text-ink-soft">Tác giả: {recipe.authorName}</p>}</div>
  </Link>;
}
