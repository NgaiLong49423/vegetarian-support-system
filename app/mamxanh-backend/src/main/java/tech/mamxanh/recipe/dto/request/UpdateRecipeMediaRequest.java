package tech.mamxanh.recipe.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request payload to associate or update media items of a recipe (FR-14, BR-19, BR-20).
 */
public record UpdateRecipeMediaRequest(
        @Size(max = 5, message = "A recipe can have at most 5 images")
        List<@Valid RecipeMediaItemRequest> mediaItems
) {
    public UpdateRecipeMediaRequest {
        if (mediaItems == null) {
            mediaItems = List.of();
        }
    }
}
