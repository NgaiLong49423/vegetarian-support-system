package tech.mamxanh.shopping.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ShoppingListResponse(long id, String name, LocalDateTime createdAt, LocalDateTime updatedAt,
        List<Item> items, List<RecipeSource> recipeSources) {
    public ShoppingListResponse(long id, String name, LocalDateTime createdAt, LocalDateTime updatedAt, List<Item> items) {
        this(id, name, createdAt, updatedAt, items, List.of());
    }
    @Schema(name = "ShoppingListSummary")
    public record Summary(long id, String name, LocalDateTime createdAt, LocalDateTime updatedAt) { }
    @Schema(name = "ShoppingListItemResponse")
    public record Item(long id, @Schema(nullable = true) Long ingredientId, String name, String ingredientGroup,
            @Schema(nullable = true, description = "Ingredient catalog illustration URL; null when unavailable.") String imageUrl,
            BigDecimal quantity, int unitId, String unitCode, String unitName, boolean bought,
            @Schema(nullable = true, description = "Minimum amount required by recipe sources, expressed in the item's display unit.") BigDecimal requiredQuantity,
            @Schema(nullable = true) String requiredUnitCode,
            @Schema(nullable = true, description = "User-added amount, expressed in the item's display unit when safely convertible.") BigDecimal manualQuantity,
            @Schema(nullable = true) String manualUnitCode, List<Source> sources) {
        public Item(long id, Long ingredientId, String name, String ingredientGroup, BigDecimal quantity, int unitId,
                String unitCode, String unitName, boolean bought, List<Source> sources) {
            this(id, ingredientId, name, ingredientGroup, null, quantity, unitId, unitCode, unitName, bought,
                    null, null, null, null, sources);
        }
        public Item(long id, Long ingredientId, String name, String ingredientGroup, String imageUrl, BigDecimal quantity,
                int unitId, String unitCode, String unitName, boolean bought, List<Source> sources) {
            this(id, ingredientId, name, ingredientGroup, imageUrl, quantity, unitId, unitCode, unitName, bought,
                    null, null, null, null, sources);
        }
        public Item(long id, Long ingredientId, String name, String ingredientGroup, BigDecimal quantity, int unitId,
                String unitCode, String unitName, boolean bought) {
            this(id, ingredientId, name, ingredientGroup, null, quantity, unitId, unitCode, unitName, bought,
                    null, null, null, null, List.of());
        }
    }
    @Schema(name = "ShoppingRecipeSource")
    public record Source(String sourceKind, java.util.UUID sourceId, @Schema(nullable = true) Long recipeId,
            @Schema(nullable = true) Long ingredientId, @Schema(nullable = true) String title,
            boolean available, BigDecimal originalQuantity, int originalUnitId, String originalUnitCode,
            @Schema(nullable = true, description = "Servings assigned to this occurrence; null when legacy data has no serving count.") BigDecimal servings) { }
    @Schema(name = "ShoppingListRecipeSource")
    public record RecipeSource(java.util.UUID sourceId, long recipeId, @Schema(nullable = true) String title,
            boolean available, @Schema(nullable = true) String coverUrl, BigDecimal servings,
            int occurrenceCount, java.time.LocalDate mealDate, String mealType) { }
}
