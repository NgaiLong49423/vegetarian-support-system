package tech.mamxanh.mealplan.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ApplyAiWeeklyMenuRequest(@NotEmpty @Size(min = 21, max = 21) List<@NotNull @Valid Slot> slots) {
    public record Slot(@NotNull Integer dayOffset, @NotNull String mealType, @NotNull @Positive Long recipeId) { }
}
