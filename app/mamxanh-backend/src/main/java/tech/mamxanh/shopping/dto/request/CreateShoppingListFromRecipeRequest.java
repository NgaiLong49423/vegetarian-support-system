package tech.mamxanh.shopping.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CreateShoppingListFromRecipeRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull @Positive Long recipeId,
        @Schema(description = "Optional serving count; omit to use the published recipe quantities.", minimum = "1", maximum = "50")
        @Min(1) @Max(50) Integer servings,
        @Schema(description = "Ingredient IDs to include. Omit to include all recipe ingredients; an empty list is allowed only when manualItems are provided.")
        @Size(max = 200) List<@NotNull @Positive Long> selectedIngredientIds,
        @Schema(description = "Optional manual items to add atomically with the selected recipe ingredients.")
        @Valid @Size(max = 50) List<CreateShoppingItemRequest> manualItems) { }
