package tech.mamxanh.shopping.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateMealPlanShoppingListRequest(
        @NotNull LocalDate weekStartDate,
        @Schema(nullable = true, description = "Omit or use null for the entire week; otherwise select 1–7 distinct dates within this week.")
        @Size(min = 1, max = 7) List<@NotNull LocalDate> selectedDates) { }
