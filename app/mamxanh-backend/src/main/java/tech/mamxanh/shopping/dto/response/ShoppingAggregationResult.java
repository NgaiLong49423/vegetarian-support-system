package tech.mamxanh.shopping.dto.response;

import java.math.BigDecimal;
import java.util.List;

/** In-memory aggregation result, not a persisted list or an HTTP endpoint. */
public record ShoppingAggregationResult(List<Group> groups, List<Warning> warnings) {
    public ShoppingAggregationResult { groups = List.copyOf(groups); warnings = List.copyOf(warnings); }
    public record Group(String name, List<Item> items) {
        public Group { items = List.copyOf(items); }
    }
    public record Item(Long ingredientId, String name, BigDecimal quantity, int unitId,
            String unitCode, String unitName, boolean incompatibleUnits, List<Integer> sourceIndices) {
        public Item { sourceIndices = List.copyOf(sourceIndices); }
        public Item(Long ingredientId, String name, BigDecimal quantity, int unitId, String unitCode,
                String unitName, boolean incompatibleUnits) {
            this(ingredientId, name, quantity, unitId, unitCode, unitName, incompatibleUnits, List.of());
        }
    }
    public record Warning(int sourceIndex, String code, String message) { }
}
