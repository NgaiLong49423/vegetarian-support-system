package tech.mamxanh.recipe.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Representation of a single recipe media item (FR-14, AC-14.2, AC-14.3).
 */
public record RecipeMediaItemRequest(
        @NotBlank(message = "Blob URL must not be blank")
        String blobUrl,

        @NotBlank(message = "MIME type must not be blank")
        @Pattern(regexp = "^(image/jpeg|image/png|image/webp)$", message = "MIME type must be image/jpeg, image/png, or image/webp")
        String mimeType,

        @NotNull(message = "Display order is required")
        @Min(value = 1, message = "Display order must be between 1 and 5")
        @Max(value = 5, message = "Display order must be between 1 and 5")
        Integer displayOrder,

        @NotNull(message = "isCover flag is required")
        Boolean isCover
) {}
