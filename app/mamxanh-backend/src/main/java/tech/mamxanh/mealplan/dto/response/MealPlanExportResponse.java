package tech.mamxanh.mealplan.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

/** Private export snapshot. Unavailable entries disclose only their slot and neutral label. */
public record MealPlanExportResponse(LocalDate weekStartDate, List<ExportEntry> entries) {
    public record ExportEntry(LocalDate mealDate, String mealType, String title,
            @Schema(nullable = true) BigDecimal plannedServings,
            @Schema(nullable = true, description = "Per-serving energy multiplied by saved planned servings; null when incomplete.")
            BigDecimal energyKcal, boolean available) { }
}
