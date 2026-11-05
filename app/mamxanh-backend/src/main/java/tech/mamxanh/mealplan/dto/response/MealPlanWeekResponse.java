package tech.mamxanh.mealplan.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MealPlanWeekResponse(LocalDate weekStartDate, List<Entry> entries) {
    @Schema(name = "MealPlanWeekEntry")
    public record Entry(long entryId, LocalDate mealDate, String mealType, BigDecimal plannedServings,
            long recipeId, @Schema(nullable = true) String recipeTitle,
            @Schema(nullable = true) String recipeCoverUrl, @Schema(nullable = true) String dishCategory,
            @Schema(nullable = true) Integer totalTimeMinutes, boolean recipeDeleted,
            @Schema(nullable = true) String unavailableMessage) { }
}
