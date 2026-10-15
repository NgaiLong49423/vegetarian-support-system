package tech.mamxanh.recipe.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;

public record RecipeAiAssistanceRequest(
        @NotNull ContentType contentType,
        @NotBlank @Size(min = 3, max = 120) String title,
        @NotBlank @Pattern(regexp = "NOODLE_SOUP|STIR_FRY|HOT_POT|BRAISED|SOUP|FRIED|STEAMED|SALAD|ROLL|GRILLED|DESSERT") String dishCategory,
        @NotBlank @Pattern(regexp = "VEGAN|LACTO|OVO|LACTO_OVO") String vegetarianType,
        @NotNull @Min(1) @Max(50) Integer servings,
        @NotEmpty @Size(max = 50) List<@Valid Ingredient> ingredients) {

    public enum ContentType { DESCRIPTION, INSTRUCTIONS }

    public record Ingredient(@NotNull @Min(1) Long ingredientId,
                             @NotNull @Min(1) Integer unitId,
                             @NotNull @DecimalMin(value = "0", inclusive = false)
                             @Digits(integer = 8, fraction = 2) BigDecimal quantity) { }
}
