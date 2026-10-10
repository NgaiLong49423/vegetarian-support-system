package tech.mamxanh.nutrition.dto.request;

import tech.mamxanh.nutrition.entity.NutritionEligibilityStatus;

/** States that a Member may explicitly confirm through the FR-38 API. */
public enum NutritionEligibilityConfirmationStatus {
    ELIGIBLE,
    INELIGIBLE;

    public NutritionEligibilityStatus toEligibilityStatus() {
        return NutritionEligibilityStatus.valueOf(name());
    }
}
