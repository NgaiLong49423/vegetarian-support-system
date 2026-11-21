package tech.mamxanh.nutrition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.entity.OnboardingStatus;
import tech.mamxanh.nutrition.entity.VegetarianType;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;
import tech.mamxanh.recipe.service.RecipePostService;

class DietarySafetyAssessmentServiceTest {
    private final DietaryPreferenceService preferences = mock(DietaryPreferenceService.class);
    private final IngredientCatalogLookupService catalog = mock(IngredientCatalogLookupService.class);
    private final RecipePostService recipes = mock(RecipePostService.class);
    private final DietarySafetyAssessmentService service = new DietarySafetyAssessmentService(preferences, catalog,
            recipes, new DietaryConstraintEvaluator());

    @Test
    void requiresFreshExplicitAcknowledgementForCurrentRecipeAndProfile() {
        var profile = profile();
        var recipe = recipe();
        when(preferences.getOwnPreferences()).thenReturn(profile);
        when(preferences.currentProfileVersion()).thenReturn(LocalDateTime.of(2026, 11, 20, 10, 0));
        when(recipes.getPublishedCards(List.of(56L))).thenReturn(List.of(recipe));
        when(catalog.findIngredientReferences(List.of(7L))).thenReturn(
                List.of(new IngredientResponse(7L, "Đậu phộng", "Hạt", null, null, null, false, true)));

        var assessment = service.assessCurrentRecipe(56L);
        assertThat(assessment.hasAllergenOrAvoidConflict()).isTrue();
        assertThat(assessment.requiresRiskAcknowledgement()).isTrue();
        assertThat(assessment.assessmentToken()).isNotBlank();

        assertThatThrownBy(() -> service.requireRiskAcknowledged(56L, false, assessment.assessmentToken()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DIETARY_RISK_ACKNOWLEDGEMENT_REQUIRED);
        assertThatThrownBy(() -> service.requireRiskAcknowledged(56L, true, "stale-token"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DIETARY_RISK_ACKNOWLEDGEMENT_REQUIRED);
        service.requireRiskAcknowledged(56L, true, assessment.assessmentToken());
    }

    private static DietaryPreferencesResponse profile() {
        return new DietaryPreferencesResponse(VegetarianType.VEGAN,
                new DietaryPreferencesResponse.IngredientPreferences(false,
                        List.of(new DietaryPreferencesResponse.PreferenceItem(7L, "Đậu phộng"))),
                new DietaryPreferencesResponse.IngredientPreferences(true, List.of()), null, null, null,
                OnboardingStatus.COMPLETED, new DietaryPreferencesResponse.AiPersonalization(true, List.of()));
    }

    private static RecipePostResponse recipe() {
        return new RecipePostResponse(56, 3, "Author", null, "Món thử", null, "Steps", "SOUP", "VEGAN", "EASY",
                2, 10, 10, null, "PUBLISHED", List.of(),
                List.of(new RecipePostResponse.Ingredient(7L, "Đậu phộng", null, 1, "g", "gram", BigDecimal.ONE)),
                0, 0, null, 0);
    }
}
