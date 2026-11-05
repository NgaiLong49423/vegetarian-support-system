package tech.mamxanh.nutrition.dto.response;

import tech.mamxanh.nutrition.entity.NutritionEligibilityStatus;

import java.time.LocalDateTime;

public record NutritionEligibilityResponse(
        NutritionEligibilityStatus status,
        LocalDateTime confirmedAt) {
}
