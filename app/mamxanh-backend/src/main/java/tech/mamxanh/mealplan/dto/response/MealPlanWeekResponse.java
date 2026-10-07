package tech.mamxanh.mealplan.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MealPlanWeekResponse(LocalDate weekStartDate, List<Entry> entries) {
    public record Entry(long entryId, LocalDate mealDate, String mealType, BigDecimal plannedServings,
            long recipeId, String recipeTitle, String recipeCoverUrl, String dishCategory,
            Integer totalTimeMinutes, boolean recipeDeleted, String unavailableMessage) { }
}
