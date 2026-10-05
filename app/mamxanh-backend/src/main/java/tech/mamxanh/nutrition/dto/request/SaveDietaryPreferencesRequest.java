package tech.mamxanh.nutrition.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tech.mamxanh.nutrition.entity.CookingDifficulty;
import tech.mamxanh.nutrition.entity.VegetarianType;

/**
 * UC-31.1 "Hoàn tất" and UC-31.3 "Lưu thay đổi": the full dietary-preference profile. The three
 * minimum groups are required; cuisine, cooking time and difficulty are optional.
 */
public record SaveDietaryPreferencesRequest(
        @NotNull(message = "Chọn một loại ăn chay.")
        VegetarianType vegetarianType,

        @NotNull(message = "Chọn ít nhất một nguyên liệu cần tránh hoặc xác nhận không có dị ứng/kiêng cử.")
        @DeclaredOrNoneConfirmed(message = "Chọn ít nhất một nguyên liệu cần tránh hoặc xác nhận không có dị ứng/kiêng cử.")
        @Valid
        IngredientPreferenceListRequest avoid,

        @NotNull(message = "Chọn ít nhất một món hoặc nguyên liệu không thích hoặc xác nhận không có.")
        @DeclaredOrNoneConfirmed(message = "Chọn ít nhất một món hoặc nguyên liệu không thích hoặc xác nhận không có.")
        @Valid
        IngredientPreferenceListRequest dislike,

        @Size(max = 200, message = "Khẩu vị tối đa 200 ký tự.")
        String cuisinePreference,

        @Min(value = 1, message = "Thời gian nấu tối đa phải là số phút dương.")
        @Max(value = 1440, message = "Thời gian nấu tối đa không quá 1440 phút.")
        Integer maxCookingTimeMinutes,

        CookingDifficulty preferredDifficulty) {
}
