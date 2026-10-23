package tech.mamxanh.shopping.dto.response;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/** Creation diagnostics; the checklist itself is persisted independently of its current recipe sources. */
public record ShoppingListCreationResponse(ShoppingListResponse shoppingList,
        List<Warning> warnings,
        @Schema(description = "IDs of saved items that the aggregation kept in separate unit buckets.")
        List<Long> incompatibleItemIds) {
    public ShoppingListCreationResponse {
        warnings = List.copyOf(warnings);
        incompatibleItemIds = List.copyOf(incompatibleItemIds);
    }
    @Schema(name = "ShoppingListCreationWarning")
    public record Warning(String code, String message, @Schema(nullable = true) Long mealPlanEntryId,
            @Schema(nullable = true) Long recipeId) { }
}
