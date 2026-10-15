package tech.mamxanh.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.mealplan.dto.response.MealPlanNutritionResponse;
import tech.mamxanh.mealplan.entity.MealPlanEntity;
import tech.mamxanh.mealplan.entity.MealPlanEntryEntity;
import tech.mamxanh.mealplan.repository.MealPlanEntryRepository;
import tech.mamxanh.mealplan.repository.MealPlanRepository;
import tech.mamxanh.nutrition.service.NutritionProfileService;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;
import tech.mamxanh.recipe.service.RecipePostService;
import tech.mamxanh.recipe.service.RecipeService;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;
import tools.jackson.databind.json.JsonMapper;

class MealPlanNutritionAiServiceTest {
    private static final long OWNER = 17L;
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final NutritionProfileService profiles = mock(NutritionProfileService.class);
    private final SubscriptionEntitlementService entitlements = mock(SubscriptionEntitlementService.class);
    private final MealPlanRepository plans = mock(MealPlanRepository.class);
    private final MealPlanEntryRepository entries = mock(MealPlanEntryRepository.class);
    private final RecipeService nutrition = mock(RecipeService.class);
    private final MealPlanNutritionService analysis = mock(MealPlanNutritionService.class);
    private final RecipePostService recipePosts = mock(RecipePostService.class);
    private final AiClient ai = mock(AiClient.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final MealPlanNutritionAiService service = new MealPlanNutritionAiService(users, profiles, entitlements,
            plans, entries, nutrition, recipePosts, ai, new JsonMapper(), events, Clock.systemUTC(), analysis);

    @BeforeEach
    void setUp() {
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(OWNER, Role.CUSTOMER));
        when(plans.findByUserIdAndWeekStartDate(OWNER, MONDAY)).thenReturn(Optional.of(plan()));
        when(entries.findByIdAndMealPlanId(101L, 31L)).thenReturn(Optional.of(entry()));
    }

    @Test
    void returnsHelpfulEmptyResultWithoutCallingAiWhenNoPublicCandidatesExist() {
        when(entries.findPublishedCandidateIds(501L)).thenReturn(List.of());

        var result = service.recommend(MONDAY, 101L);

        assertThat(result.recommendations()).isEmpty();
        assertThat(result.analysis()).contains("chưa có công thức");
        verifyNoInteractions(nutrition, analysis, recipePosts, ai, events);
    }

    @Test
    void usesOnlyExistingCandidateIdsDeduplicatesSelectionAndRecordsUsage() {
        arrangeCandidate();
        when(ai.analyzeNutritionRecommendations(anyString(), anyList(), eq(3)))
                .thenReturn(new AiClient.RecommendationResult(
                        "{\"analysis\":\"Thực đơn có thể cân đối thêm nhóm đạm.\",\"recipeIds\":[601,601]}", 12, 5, 17));

        var result = service.recommend(MONDAY, 101L);

        assertThat(result.entryId()).isEqualTo(101L);
        assertThat(result.analysis()).isEqualTo("Thực đơn có thể cân đối thêm nhóm đạm.");
        assertThat(result.recommendations()).hasSize(1);
        assertThat(result.recommendations().getFirst().recipeId()).isEqualTo(601L);
        assertThat(result.recommendations().getFirst().title()).isEqualTo("Món gợi ý");
        verify(ai).analyzeNutritionRecommendations(contains("referenceTargets"), eq(List.of(601L)), eq(3));
        verify(events).publishEvent(any(AiUsageEvent.class));
    }

    @Test
    void mapsProviderAndNullResponsesToApplicationErrorsWithoutUsageEvents() {
        arrangeCandidate();
        when(ai.analyzeNutritionRecommendations(anyString(), anyList(), eq(3)))
                .thenThrow(new tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException("offline"));
        assertThatThrownBy(() -> service.recommend(MONDAY, 101L))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_PROVIDER_UNAVAILABLE);

        reset(ai);
        when(ai.analyzeNutritionRecommendations(anyString(), anyList(), eq(3))).thenReturn(null);
        assertThatThrownBy(() -> service.recommend(MONDAY, 101L))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_RESPONSE_INVALID);
        verifyNoInteractions(events);
    }

    @Test
    void rejectsUnsafeOrMalformedProviderExplanationsAndRecipeSelections() {
        arrangeCandidate();
        for (String response : List.of(
                "not-json", "{\"analysis\":\"\",\"recipeIds\":[601]}",
                "{\"analysis\":\"Protein is 12 grams\",\"recipeIds\":[601]}",
                "{\"analysis\":42,\"recipeIds\":[601]}",
                "{\"analysis\":\"Giải thích hợp lệ\",\"recipeIds\":[999]}",
                "{\"analysis\":\"Giải thích hợp lệ\",\"recipeIds\":[\"601\"]}",
                "{\"analysis\":\"Giải thích hợp lệ\",\"recipeIds\":[601,601,601,601]}")) {
            when(ai.analyzeNutritionRecommendations(anyString(), anyList(), eq(3)))
                    .thenReturn(new AiClient.RecommendationResult(response, 1, 1, 2));
            assertThatThrownBy(() -> service.recommend(MONDAY, 101L))
                    .as("provider response %s", response)
                    .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_RESPONSE_INVALID);
        }
        verifyNoInteractions(events);
    }

    private void arrangeCandidate() {
        when(entries.findPublishedCandidateIds(501L)).thenReturn(List.of(601L));
        when(nutrition.calculatePlannedNutrition(501L, BigDecimal.ONE))
                .thenReturn(planned(501L, "Món hiện tại", true, Map.of("PROTEIN_G", BigDecimal.TEN)));
        var day = new MealPlanNutritionResponse.Day(MONDAY, Map.of("PROTEIN_G", BigDecimal.TEN), List.of());
        when(analysis.analyze(MONDAY, MONDAY)).thenReturn(new MealPlanNutritionResponse(MONDAY, "DAY", MONDAY,
                "reference", "source", "https://example.test", "disclaimer", List.of(), List.of(day), Map.of(), Map.of(), List.of()));
        when(recipePosts.getPublishedCards(List.of(601L))).thenReturn(List.of(recipe()));
        when(nutrition.calculatePlannedNutrition(Map.of(601L, BigDecimal.ONE)))
                .thenReturn(Map.of(601L, planned(601L, "Món gợi ý", false, Map.of())));
    }

    private static RecipeService.PlannedNutrition planned(long id, String title, boolean complete,
            Map<String, BigDecimal> nutrients) {
        return new RecipeService.PlannedNutrition(id, title, complete, complete ? List.of() : List.of("Canxi"), nutrients);
    }

    private static MealPlanEntity plan() {
        var plan = new MealPlanEntity(); plan.setId(31L); plan.setUserId(OWNER); plan.setWeekStartDate(MONDAY);
        return plan;
    }

    private static MealPlanEntryEntity entry() {
        var entry = new MealPlanEntryEntity(); entry.setId(101L); entry.setMealPlanId(31L); entry.setRecipeId(501L);
        entry.setMealDate(MONDAY); entry.setMealType("LUNCH"); entry.setPlannedServings(BigDecimal.ONE);
        return entry;
    }

    private static RecipePostResponse recipe() {
        return new RecipePostResponse(601L, OWNER, "Author", null, "Món gợi ý", "description", "instructions",
                "SOUP", "VEGAN", "EASY", 2, 5, 10, null, "PUBLISHED", List.of(), List.of(), 0, 0, null, 0);
    }
}
