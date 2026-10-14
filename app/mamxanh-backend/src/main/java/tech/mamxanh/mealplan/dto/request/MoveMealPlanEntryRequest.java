package tech.mamxanh.mealplan.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MoveMealPlanEntryRequest(
        @NotNull LocalDate mealDate,
        @NotNull @Pattern(regexp = "BREAKFAST|LUNCH|DINNER") String mealType) { }
