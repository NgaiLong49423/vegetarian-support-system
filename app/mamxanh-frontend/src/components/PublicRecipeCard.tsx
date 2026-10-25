import { Link } from "react-router-dom"
import { ArrowLeftRight } from "lucide-react"
import type { RecipeFormOptions, RecipePost } from "../api/recipes"
import { AddToMealPlanButton } from "./AddToMealPlanButton"
import { RecipeDefaultArtwork, type VegetarianCode } from "./RecipeDefaultArtwork"
import { SaveRecipeButton } from "./SaveRecipeButton"
import { RecipeViewCount } from "./RecipeViewCount"
import { RecipeReactionBadge } from "./RecipeReactionBadge"

export function PublicRecipeCard({
  recipe,
  options,
  showLikePercentage = true,
}: {
  recipe: RecipePost
  options?: RecipeFormOptions | null
  showLikePercentage?: boolean
}) {
  const cover = recipe.media.find((media) => media.cover) ?? recipe.media[0]
  const category =
    options?.dishCategories.find((item) => item.code === recipe.dishCategory)
      ?.label ?? recipe.dishCategory
  const vegetarian =
    options?.vegetarianTypes.find((item) => item.code === recipe.vegetarianType)
      ?.label ?? recipe.vegetarianType
  return (
    <article className="group relative flex h-full min-w-0 flex-col overflow-hidden rounded-2xl border border-brand-100/90 bg-paper shadow-xs transition-all duration-200 hover:-translate-y-0.5 hover:border-brand-200 hover:shadow-md dark:border-brand-800/70 dark:bg-surface">
      <Link
        to={`/cong-thuc/id/${recipe.id}`}
        className="group/link flex flex-1 flex-col focus-visible:outline-offset-4"
      >
        <div className="relative aspect-[4/3] w-full overflow-hidden bg-raised">
          {cover ? (
            <img
              src={cover.url}
              alt={recipe.title}
              loading="lazy"
              className="h-full w-full object-cover transition-transform duration-200 motion-safe:group-hover/link:scale-[1.03]"
            />
          ) : (
            <RecipeDefaultArtwork
              vegetarianType={recipe.vegetarianType as VegetarianCode}
              label={vegetarian}
              compact
            />
          )}
          {showLikePercentage && (
            <div className="absolute left-2 top-2">
              <RecipeReactionBadge percentage={recipe.likePercentage} />
            </div>
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
        <div className="flex flex-1 flex-col p-3.5 pb-2 sm:p-4 sm:pb-2">
          <h2 className="line-clamp-2 text-sm font-semibold leading-5 text-ink transition-colors duration-200 group-hover/link:text-brand-600">
            {recipe.title}
          </h2>
          <p className="mt-1 text-xs text-ink-muted">
            {recipe.ingredients.length} nguyên liệu ·{" "}
            {recipe.prepTimeMin + recipe.cookTimeMin} phút · {recipe.servings}{" "}
            phần
          </p>
          <p
            className="mt-1 truncate text-xs font-medium text-leaf-700"
            title={`${vegetarian} · ${category}`}
          >
            {vegetarian} · {category}
          </p>
          <div className="mt-1.5 text-[11px] text-ink-muted">
            <RecipeViewCount count={recipe.viewCount} />
          </div>
        </div>
      </Link>
      <div className="flex items-center justify-between px-3.5 pb-3 pt-0.5 sm:px-4 sm:pb-3.5">
        <Link
          to={`/so-sanh?a=${recipe.id}`}
          aria-label={`So sánh ${recipe.title}`}
          className="inline-flex items-center gap-1 rounded text-[11px] font-semibold text-brand-600 hover:text-brand-700"
        >
          <ArrowLeftRight className="h-3 w-3" aria-hidden="true" />
          So sánh
        </Link>
      </div>
      <div className="absolute right-2 top-2 z-10 flex items-center gap-1.5">
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
        <div
          key={index}
          aria-hidden="true"
          className="flex min-w-0 flex-col overflow-hidden rounded-2xl border border-brand-100/90 bg-paper shadow-xs dark:border-brand-800/70 dark:bg-surface"
        >
          <div className="aspect-[4/3] w-full bg-raised motion-safe:animate-pulse" />
          <div className="p-3.5 sm:p-4">
            <div className="h-4 w-4/5 rounded bg-raised motion-safe:animate-pulse" />
            <div className="mt-2.5 h-3 w-3/5 rounded bg-raised motion-safe:animate-pulse" />
            <div className="mt-2 h-3 w-2/5 rounded bg-raised motion-safe:animate-pulse" />
          </div>
        </div>
      ))}
    </>
  )
}
