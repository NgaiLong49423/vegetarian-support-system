package tech.mamxanh.recipe.dto.request;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRecipePostRequest(
        @NotBlank @Size(min = 3, max = 120) String title,
        @Size(max = 2000) String description,
        @NotBlank @Size(min = 10, max = 5000) String instructions,
        @NotBlank @Pattern(regexp = "NOODLE_SOUP|STIR_FRY|HOT_POT|BRAISED|SOUP|FRIED|STEAMED|SALAD|ROLL|GRILLED|DESSERT") String dishCategory,
        @NotBlank @Pattern(regexp = "VEGAN|LACTO|OVO|LACTO_OVO") String vegetarianType,
        @NotBlank @Pattern(regexp = "EASY|MEDIUM|HARD") String difficulty,
        @NotNull @Min(1) @Max(50) Integer servings,
        @NotNull @Min(0) @Max(1440) Integer prepTimeMin,
        @NotNull @Min(0) @Max(1440) Integer cookTimeMin,
        @Pattern(regexp = "^$|https?://(www\\.)?(youtube\\.com|youtu\\.be)/.+") @Size(max = 2048) String youtubeUrl,
        @NotNull @Size(min = 1, max = 50) List<@NotNull @Valid Ingredient> ingredients,
        @NotNull @Size(max = 5) List<@NotNull @Valid Media> media) {

    public record Ingredient(
            Long ingredientId,
            @Size(max = 200) String customName,
            @NotNull @Min(1) Integer unitId,
            @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal quantity) { }

    public record Media(
            @NotBlank @Pattern(regexp = "https?://.+") @Size(max = 2048) String url,
            @NotBlank @Pattern(regexp = "image/jpeg|image/png|image/webp") String mimeType,
            @NotNull @Min(1) @Max(5) Integer displayOrder,
            @NotNull Boolean cover) { }
}
