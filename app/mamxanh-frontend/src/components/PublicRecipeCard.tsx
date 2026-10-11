import { Link } from "react-router-dom"
import { Leaf, ThumbsUp } from "lucide-react"
import type { RecipeFormOptions, RecipePost } from "../api/recipes"
import { SaveRecipeButton } from "./SaveRecipeButton"

export function PublicRecipeCard({
  recipe,
  options,
  showLikes = false,
}: {
  recipe: RecipePost
  options?: RecipeFormOptions | null
  showLikes?: boolean
}) {
  const cover = recipe.media.find((media) => media.cover) ?? recipe.media[0]
  const category =
    options?.dishCategories.find((item) => item.code === recipe.dishCategory)
      ?.label ?? recipe.dishCategory
  const vegetarian =
    options?.vegetarianTypes.find((item) => item.code === recipe.vegetarianType)
      ?.label ?? recipe.vegetarianType
  return (
    <article className="relative min-w-0">
      <Link
        to={`/cong-thuc/id/${recipe.id}`}
        className="group block rounded-xl focus-visible:outline-offset-4"
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
            <div className="flex h-full flex-col items-center justify-center gap-2 px-3 text-center text-xs text-ink-muted">
              <Leaf className="h-7 w-7" />
              Chưa có ảnh công thức
            </div>
          )}
          {showLikes && (
            <span
              aria-label="Tỷ lệ yêu thích"
              className="absolute left-2 top-2 inline-flex items-center gap-1 rounded-full bg-black/65 px-2 py-1 text-[11px] font-semibold text-white"
            >
              <ThumbsUp className="h-3 w-3" />
              {recipe.likePercentage === null
                ? "Mới"
                : `${recipe.likePercentage}%`}
            </span>
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
        <h2 className="mt-2.5 line-clamp-2 text-sm font-semibold leading-5 text-ink group-hover:text-brand-600">
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
      </Link>
      <div className="absolute right-2 top-2">
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
