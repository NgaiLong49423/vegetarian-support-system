package tech.mamxanh.nutrition.dto.response;

import java.time.LocalDateTime;
import tech.mamxanh.nutrition.entity.NutritionEligibilityStatus;

public record NutritionEligibilityResponse(
        NutritionEligibilityStatus status,
        LocalDateTime confirmedAt) {
}
