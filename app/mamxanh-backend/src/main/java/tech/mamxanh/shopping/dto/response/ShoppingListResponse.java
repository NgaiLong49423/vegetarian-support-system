package tech.mamxanh.shopping.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

public record ShoppingListResponse(long id, String name, LocalDateTime createdAt, LocalDateTime updatedAt,
        List<Item> items) {
    @Schema(name = "ShoppingListSummary")
    public record Summary(long id, String name, LocalDateTime createdAt, LocalDateTime updatedAt) { }
    @Schema(name = "ShoppingListItemResponse")
    public record Item(long id, @Schema(nullable = true) Long ingredientId, String name, String ingredientGroup,
            @Schema(nullable = true, description = "Ingredient catalog illustration URL; null when unavailable.") String imageUrl,
            BigDecimal quantity, int unitId, String unitCode, String unitName, boolean bought, List<Source> sources) {
        public Item(long id, Long ingredientId, String name, String ingredientGroup, BigDecimal quantity, int unitId,
                String unitCode, String unitName, boolean bought, List<Source> sources) {
            this(id, ingredientId, name, ingredientGroup, null, quantity, unitId, unitCode, unitName, bought, sources);
        }
        public Item(long id, Long ingredientId, String name, String ingredientGroup, BigDecimal quantity, int unitId,
                String unitCode, String unitName, boolean bought) {
            this(id, ingredientId, name, ingredientGroup, null, quantity, unitId, unitCode, unitName, bought, List.of());
        }
    }
    @Schema(name = "ShoppingRecipeSource")
    public record Source(long recipeId, @Schema(nullable = true) String title, boolean available,
            BigDecimal originalQuantity, int originalUnitId, String originalUnitCode) { }
}
