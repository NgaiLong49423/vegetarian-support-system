package tech.mamxanh.nutrition.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One FR-31 list (ingredients to avoid, or disliked dishes/ingredients): either names, or the
 * explicit "none" confirmation. An empty list is never read as "none" (BR-31).
 */
public record IngredientPreferenceListRequest(
        Boolean noneConfirmed,
        @Size(max = 30, message = "Mỗi danh sách có tối đa 30 mục.")
        List<@NotBlank(message = "Tên nguyên liệu không được để trống.")
             @Size(max = 200, message = "Tên nguyên liệu tối đa 200 ký tự.") String> items) {

    /** A missing confirmation is never read as "none" (BR-31). */
    public boolean confirmsNone() {
        return Boolean.TRUE.equals(noneConfirmed);
    }

    public List<String> itemsOrEmpty() {
        return items == null ? List.of() : items;
    }
}
