package tech.mamxanh.shopping.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record CreateShoppingListFromRecipeRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull @Positive Long recipeId,
        @Schema(description = "Optional serving count; omit to use the published recipe quantities.", minimum = "1", maximum = "50")
        @Min(1) @Max(50) Integer servings) { }
