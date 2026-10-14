package tech.mamxanh.mealplan.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.mealplan.dto.response.MealPlanExportResponse;
import tech.mamxanh.mealplan.dto.response.MealPlanWeekResponse;
import tech.mamxanh.recipe.dto.response.RecipeDetailResponse;
import tech.mamxanh.recipe.service.RecipeService;

@Service
public class MealPlanExportService {
    private final MealPlanReadService reads;
    private final RecipeService recipes;

    public MealPlanExportService(MealPlanReadService reads, RecipeService recipes) {
        this.reads = reads;
        this.recipes = recipes;
    }

    @Transactional(readOnly = true)
    public MealPlanExportResponse getExport(LocalDate weekStartDate) {
        // Authentication and owner filtering happen in the read service, before recipe lookup.
        var week = reads.getWeek(weekStartDate);
        Map<Long, RecipeDetailResponse> details = new HashMap<>();
        var entries = week.entries().stream().map(entry -> exportEntry(entry, details)).toList();
        return new MealPlanExportResponse(week.weekStartDate(), entries);
    }

    private MealPlanExportResponse.ExportEntry exportEntry(MealPlanWeekResponse.Entry entry,
            Map<Long, RecipeDetailResponse> details) {
        if (entry.recipeTitle() == null || entry.recipeDeleted() || entry.unavailableMessage() != null) {
            return unavailable(entry);
        }
        if (!details.containsKey(entry.recipeId())) {
            try {
                details.put(entry.recipeId(), recipes.getPublishedRecipe(entry.recipeId()));
            } catch (AppException exception) {
                if (exception.errorCode() != ErrorCode.NOT_FOUND && exception.errorCode() != ErrorCode.RECIPE_NOT_FOUND) {
                    throw exception;
                }
                // A source can be hidden between the week read and the detail lookup.
                details.put(entry.recipeId(), null);
            }
        }
        var recipe = details.get(entry.recipeId());
        if (recipe == null) return unavailable(entry);
        var nutrition = recipe.nutrition();
        BigDecimal energy = nutrition != null && nutrition.complete()
                ? nutrition.perServing().get("ENERGY_KCAL") : null;
        return new MealPlanExportResponse.ExportEntry(entry.mealDate(), entry.mealType(), recipe.title(),
                entry.plannedServings(), energy == null ? null : energy.multiply(entry.plannedServings()), true);
    }

    private MealPlanExportResponse.ExportEntry unavailable(MealPlanWeekResponse.Entry entry) {
        return new MealPlanExportResponse.ExportEntry(entry.mealDate(), entry.mealType(),
                "Công thức không còn khả dụng", null, null, false);
    }
}
