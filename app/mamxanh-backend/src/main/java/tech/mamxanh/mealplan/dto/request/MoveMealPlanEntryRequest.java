package tech.mamxanh.mealplan.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record MoveMealPlanEntryRequest(
        @NotNull LocalDate mealDate,
        @NotNull @Pattern(regexp = "BREAKFAST|LUNCH|DINNER") String mealType) { }
