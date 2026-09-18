import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Clock, Flame, Heart, Star, Users } from 'lucide-react';
import type { Recipe } from '../types';
import { Badge } from './ui';

export function RecipeCard({ recipe }: { recipe: Recipe }) {
  const [saved, setSaved] = useState(false);

  return (
    <Link
      to={`/cong-thuc/${recipe.slug}`}
      className="group flex flex-col overflow-hidden rounded-2xl border border-brand-100 bg-white transition-all hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl hover:shadow-brand-900/5"
    >
      <div className="relative aspect-[4/3] overflow-hidden">
        <img
          src={recipe.image}
          alt={recipe.name}
          className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
        />
        <div className="absolute left-3 top-3">
          <Badge tone="leaf" soft={false}>
            {recipe.diet}
          </Badge>
        </div>
        <button
          onClick={(e) => {
            e.preventDefault();
            setSaved((v) => !v);
          }}
          className="absolute right-3 top-3 flex h-9 w-9 items-center justify-center rounded-full bg-white/90 text-ink-soft shadow-sm backdrop-blur transition-colors hover:text-brand-600"
          aria-label="Lưu công thức"
        >
          <Heart className={`h-[18px] w-[18px] ${saved ? 'fill-brand-600 text-brand-600' : ''}`} />
        </button>
      </div>

      <div className="flex flex-1 flex-col p-4">
        <div className="mb-2 flex items-center gap-1.5 text-sm">
          <Star className="h-4 w-4 fill-amber-400 text-amber-400" />
          <span className="font-bold text-ink">{recipe.rating}</span>
          <span className="text-ink-muted">({recipe.reviews})</span>
          <span className="ml-auto text-xs font-semibold text-brand-600">{recipe.category}</span>
        </div>
        <h3 className="mb-3 line-clamp-2 font-bold leading-snug text-ink transition-colors group-hover:text-brand-700">
          {recipe.name}
        </h3>

        <div className="mt-auto flex flex-wrap items-center gap-x-4 gap-y-1.5 text-xs text-ink-muted">
          <span className="inline-flex items-center gap-1">
            <Clock className="h-3.5 w-3.5" /> {recipe.prepTime + recipe.cookTime} phút
          </span>
          <span className="inline-flex items-center gap-1">
            <Flame className="h-3.5 w-3.5" /> {recipe.calories} kcal
          </span>
          <span className="inline-flex items-center gap-1">
            <Users className="h-3.5 w-3.5" /> {recipe.servings} người
          </span>
        </div>

        <div className="mt-3 flex items-center gap-2 border-t border-brand-50 pt-3">
          <img src={recipe.author.avatar} alt="" className="h-6 w-6 rounded-full object-cover" />
          <span className="truncate text-xs font-medium text-ink-soft">{recipe.author.name}</span>
        </div>
      </div>
    </Link>
  );
}
