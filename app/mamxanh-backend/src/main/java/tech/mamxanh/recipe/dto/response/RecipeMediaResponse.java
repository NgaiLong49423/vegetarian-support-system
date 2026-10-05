package tech.mamxanh.recipe.dto.response;

import tech.mamxanh.recipe.entity.RecipeMedia;

/**
 * Response representation of a recipe media entry (FR-14).
 */
public record RecipeMediaResponse(
        Long mediaId,
        Long recipeId,
        String blobUrl,
        String mimeType,
        Integer displayOrder,
        Boolean isCover
) {
    public static RecipeMediaResponse fromEntity(RecipeMedia entity) {
        return new RecipeMediaResponse(
                entity.getId(),
                entity.getRecipeId(),
                entity.getBlobUrl(),
                entity.getMimeType(),
                entity.getDisplayOrder(),
                entity.getIsCover()
        );
    }
}
