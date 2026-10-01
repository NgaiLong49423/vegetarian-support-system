package tech.mamxanh.nutrition.dto.request;

import jakarta.validation.constraints.NotNull;

public record ConfirmNutritionEligibilityRequest(
        @NotNull Boolean pregnant,
        @NotNull Boolean breastfeeding,
        @NotNull Boolean therapeuticDietRequired) {
}
