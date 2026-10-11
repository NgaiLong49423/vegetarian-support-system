package tech.mamxanh.nutrition.dto.response;

import java.util.List;

import tech.mamxanh.nutrition.entity.CookingDifficulty;
import tech.mamxanh.nutrition.entity.OnboardingStatus;
import tech.mamxanh.nutrition.entity.VegetarianType;
import tech.mamxanh.nutrition.service.DietaryRequirement;

/** FR-31 private dietary-preference profile of the signed-in Member (never shown to others). */
public record DietaryPreferencesResponse(
        VegetarianType vegetarianType,
        IngredientPreferences avoid,
        IngredientPreferences dislike,
        String cuisinePreference,
        Integer maxCookingTimeMinutes,
        CookingDifficulty preferredDifficulty,
        OnboardingStatus onboardingStatus,
        AiPersonalization aiPersonalization) {

    public record IngredientPreferences(boolean noneConfirmed, List<PreferenceItem> items) {
    }

    /** {@code ingredientId} is set when the name matches an active standard ingredient. */
    public record PreferenceItem(Long ingredientId, String name) {
    }

    /** Whether personalized AI may be called, and which minimum groups are still missing. */
    public record AiPersonalization(boolean eligible, List<DietaryRequirement> missing) {
    }
}
