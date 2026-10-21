package tech.mamxanh.recipe.dto.response;

/** Public engagement totals across all published recipes belonging to one author. */
public record RecipeAuthorStatisticsResponse(long recipeCount, long totalLikes, long totalViews) { }
