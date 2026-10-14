package tech.mamxanh.mealplan.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record MealPlanNutritionResponse(LocalDate weekStartDate, String scope, LocalDate selectedDate,
        String referenceBasis, String source, String sourceUrl, String disclaimer,
        List<Target> targets, List<Day> days, Map<String, BigDecimal> weeklyAverage,
        Map<String, Integer> attainmentDays, List<MissingEntry> incompleteEntries) {
    public record Target(String key, String label, BigDecimal minimum, BigDecimal maximum, String unit,
            String referenceType, String source, String sourceUrl, String version) { }
    public record Day(LocalDate date, Map<String, BigDecimal> nutrients, List<Entry> entries) { }
    public record Entry(long entryId, LocalDate date, String mealType, long recipeId, String recipeTitle,
            BigDecimal plannedServings, boolean complete, List<String> ingredientsMissingData,
            Map<String, BigDecimal> nutrients) { }
    public record MissingEntry(long entryId, LocalDate date, String mealType, String recipeTitle,
            List<String> ingredientsMissingData) { }
}
