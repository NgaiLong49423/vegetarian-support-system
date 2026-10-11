package tech.mamxanh.nutrition.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
/** Explicitly confirms the Member's current FR-38 eligibility state. */
public record UpdateNutritionEligibilityRequest(
        @NotNull @Schema(implementation = NutritionEligibilityConfirmationStatus.class) NutritionEligibilityConfirmationStatus status) {
}
