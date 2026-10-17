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
            String unitCode, String unitName, boolean incompatibleUnits) { }
    public record Warning(int sourceIndex, String code, String message) { }
}
