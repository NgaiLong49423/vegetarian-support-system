package tech.mamxanh.recipe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecipeMediaInput(
        @NotBlank @Size(max = 2048) String blobUrl,
        @NotBlank @Size(max = 20) String mimeType,
        boolean cover) {
}
