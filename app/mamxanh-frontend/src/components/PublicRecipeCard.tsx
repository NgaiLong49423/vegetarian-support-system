import { Link } from "react-router-dom"
import type { RecipeFormOptions, RecipePost } from "../api/recipes"
import { AddToMealPlanButton } from "./AddToMealPlanButton"
import { RecipeDefaultArtwork, type VegetarianCode } from "./RecipeDefaultArtwork"
import { SaveRecipeButton } from "./SaveRecipeButton"
import { RecipeViewCount } from "./RecipeViewCount"

export function PublicRecipeCard({
  recipe,
  options,
}: {
  recipe: RecipePost
  options?: RecipeFormOptions | null
}) {
  const cover = recipe.media.find((media) => media.cover) ?? recipe.media[0]
  const category =
    options?.dishCategories.find((item) => item.code === recipe.dishCategory)
      ?.label ?? recipe.dishCategory
  const vegetarian =
    options?.vegetarianTypes.find((item) => item.code === recipe.vegetarianType)
      ?.label ?? recipe.vegetarianType
  return (
    <article className="relative flex h-full min-w-0 flex-col transition-transform duration-200 motion-safe:hover:-translate-y-0.5">
      <Link
        to={`/cong-thuc/id/${recipe.id}`}
        className="group flex flex-1 flex-col rounded-xl focus-visible:outline-offset-4"
      >
        <div className="relative aspect-[4/3] overflow-hidden rounded-xl bg-raised">
          {cover ? (
            <img
              src={cover.url}
              alt={recipe.title}
              loading="lazy"
              className="h-full w-full object-cover transition-transform duration-200 motion-safe:group-hover:scale-[1.03]"
            />
          ) : (
            <RecipeDefaultArtwork
              vegetarianType={recipe.vegetarianType as VegetarianCode}
              label={vegetarian}
              compact
            />
          )}
          <div className="absolute inset-x-0 bottom-0 flex items-center gap-1.5 bg-black/60 px-2.5 py-2 text-[11px] text-white">
            {recipe.authorAvatarUrl ? (
              <img
                src={recipe.authorAvatarUrl}
                alt=""
                className="h-5 w-5 shrink-0 rounded-full object-cover"
              />
            ) : (
              <span
                aria-hidden="true"
                className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-white/20 text-[10px]"
              >
                {recipe.authorName.trim().charAt(0).toUpperCase()}
              </span>
            )}
            <span className="truncate">{recipe.authorName}</span>
          </div>
        </div>
        <h2 className="mt-2.5 line-clamp-2 text-sm font-semibold leading-5 text-ink transition-colors duration-200 group-hover:text-brand-600">
          {recipe.title}
        </h2>
        <p className="mt-1 text-xs text-ink-muted">
          {recipe.ingredients.length} nguyên liệu ·{" "}
          {recipe.prepTimeMin + recipe.cookTimeMin} phút · {recipe.servings}{" "}
          phần
        </p>
        <p
          className="mt-1 truncate text-xs text-leaf-700"
          title={`${vegetarian} · ${category}`}
        >
          {vegetarian} · {category}
        </p>
        <div className="mt-1 text-[11px] text-ink-muted"><RecipeViewCount count={recipe.viewCount} /></div>
      </Link>
      <div className="absolute right-2 top-2 flex items-center gap-1.5">
        <AddToMealPlanButton recipeId={recipe.id} recipeTitle={recipe.title} preview={recipe} />
        <SaveRecipeButton recipeId={recipe.id} compact />
      </div>
    </article>
  )
}

export function PublicRecipeSkeleton({ count = 4 }: { count?: number }) {
  return (
    <>
      {Array.from({ length: count }, (_, index) => (
        <div key={index} aria-hidden="true" className="min-w-0">
          <div className="aspect-[4/3] rounded-xl bg-raised motion-safe:animate-pulse" />
          <div className="mt-3 h-3 w-4/5 rounded bg-raised motion-safe:animate-pulse" />
          <div className="mt-2 h-3 w-3/5 rounded bg-raised motion-safe:animate-pulse" />
        </div>
      ))}
    </>
  )
}
