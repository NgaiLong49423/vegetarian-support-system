package tech.mamxanh.mealplan.dto.request;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ApplyAiMenuRequest(@NotNull LocalDate weekStartDate,
        @NotNull @Pattern(regexp = "PROFILE|THIS_WEEK") String mode,
        @Pattern(regexp = "|MAINTAIN_WEIGHT|LOSE_WEIGHT|GAIN_WEIGHT|BUILD_MUSCLE|IMPROVE_HEALTH|SUPPORT_TRAINING") String goal,
        @Size(max = 20) List<Long> avoidedIngredientIds,
        @NotEmpty @Size(max = 21) List<@Valid SelectedMeal> meals) {
    public record SelectedMeal(@NotNull LocalDate mealDate,
            @NotNull @Pattern(regexp = "BREAKFAST|LUNCH|DINNER") String mealType,
            @NotNull Long recipeId) { }
}
