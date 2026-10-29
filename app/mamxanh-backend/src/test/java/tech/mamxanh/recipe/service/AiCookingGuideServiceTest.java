package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.auth.service.CurrentUserService.CurrentUser;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse.AiPersonalization;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse.IngredientPreferences;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse.PreferenceItem;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.entity.OnboardingStatus;
import tech.mamxanh.nutrition.entity.VegetarianType;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.nutrition.service.IngredientCatalogLookupService;
import tech.mamxanh.recipe.dto.response.RecipeDetailResponse;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class AiCookingGuideServiceTest {
    private static final long MEMBER_ID = 7L;
    private static final long RECIPE_ID = 42L;
    private static final String VALID_GUIDE = """
            {"description":"Hướng dẫn nấu","steps":[
              {"text":"Chuẩn bị nồi và nguyên liệu.","ingredientIds":[]},
              {"text":"Rửa và sơ chế đậu phụ.","ingredientIds":[1]},
              {"text":"Làm nóng chảo.","ingredientIds":[]},
              {"text":"Cho đậu phụ vào chảo.","ingredientIds":[1]},
              {"text":"Nấu đến khi nóng đều.","ingredientIds":[]},
              {"text":"Nêm nếm rồi trình bày.","ingredientIds":[]}]}
            """;

    @Mock private CurrentUserService users;
    @Mock private DietaryPreferenceService preferences;
    @Mock private RecipeService recipes;
    @Mock private IngredientCatalogLookupService ingredientCatalog;
    @Mock private AiClient ai;
    @Mock private ApplicationEventPublisher events;

    private AiCookingGuideService service;

    @BeforeEach
    void setUp() {
        service = new AiCookingGuideService(users, preferences, recipes, ingredientCatalog, ai,
                new JsonMapper(), events, Clock.systemUTC());
        when(users.requireActiveMember()).thenReturn(new CurrentUser(MEMBER_ID, Role.CUSTOMER));
        lenient().when(preferences.getOwnPreferences()).thenReturn(profile(VegetarianType.LACTO_OVO, List.of()));
        when(recipes.getPublishedRecipe(RECIPE_ID)).thenReturn(recipe("VEGAN", ingredient(1L, "Đậu phụ")));
        lenient().when(ingredientCatalog.findActiveIngredients(""))
                .thenReturn(List.of(catalogIngredient(1L, "Đậu phụ")));
    }

    @Test
    void generatesValidatedGuideFromOnlyPublishedRecipeIngredients() {
        when(ai.generateRecipeContent(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult(VALID_GUIDE,
                150, 300, 500, new tech.mamxanh.integration.ai.AiTokenUsage(
                        "gemini-3.8-flash", "STANDARD", 150, 300, 500, 50, 20, 0)));

        var result = service.generate(RECIPE_ID);

        assertThat(result.recipeId()).isEqualTo(RECIPE_ID);
        assertThat(result.recipeTitle()).isEqualTo("Đậu phụ sốt");
        assertThat(result.steps()).hasSize(6).first().isEqualTo("Chuẩn bị nồi và nguyên liệu.");
        assertThat(result.steps().get(1)).isEqualTo("Rửa và sơ chế đậu phụ. — Nguyên liệu: Đậu phụ");
        assertThat(result.aiNotice()).contains("ước tính");
        verify(preferences).requirePersonalizedAiEligible(MEMBER_ID);
        verify(ai).generateRecipeContent(anyString(), org.mockito.ArgumentMatchers.eq(List.of(1L)));
        var telemetry = org.mockito.ArgumentCaptor.forClass(AiUsageEvent.class);
        verify(events).publishEvent(telemetry.capture());
        assertThat(telemetry.getValue().feature()).isEqualTo("FR-34");
        assertThat(telemetry.getValue().userId()).isEqualTo(MEMBER_ID);
        assertThat(telemetry.getValue().usage().promptTokens()).isEqualTo(150);
        assertThat(telemetry.getValue().usage().responseTokens()).isEqualTo(300);
        assertThat(telemetry.getValue().usage().totalTokens()).isEqualTo(500);
        assertThat(telemetry.getValue().usage().thinkingTokens()).isEqualTo(50);
        assertThat(telemetry.getValue().usage().cachedTokens()).isEqualTo(20);
    }

    @Test
    void rejectsRecipeOutsideMembersVegetarianProfileBeforeCallingAi() {
        when(preferences.getOwnPreferences()).thenReturn(profile(VegetarianType.VEGAN, List.of()));
        when(recipes.getPublishedRecipe(RECIPE_ID)).thenReturn(recipe("LACTO", ingredient(1L, "Đậu phụ")));

        assertError(ErrorCode.RECIPE_NOT_FOUND);

        verify(ai, never()).generateRecipeContent(anyString(), anyList());
    }

    @Test
    void rejectsRecipeWithAvoidedIngredientMatchedByIdOrNormalizedName() {
        when(preferences.getOwnPreferences()).thenReturn(profile(VegetarianType.LACTO_OVO,
                List.of(new PreferenceItem(1L, "Đậu phụ"))));
        assertError(ErrorCode.RECIPE_NOT_FOUND);

        when(preferences.getOwnPreferences()).thenReturn(profile(VegetarianType.LACTO_OVO,
                List.of(new PreferenceItem(null, "  ĐẬU PHỤ  "))));
        assertError(ErrorCode.RECIPE_NOT_FOUND);

        verify(ai, never()).generateRecipeContent(anyString(), anyList());
    }

    @Test
    void translatesProviderAndUnexpectedRuntimeFailures() {
        when(ai.generateRecipeContent(anyString(), anyList())).thenThrow(new AiProviderException("private provider detail"));
        assertError(ErrorCode.AI_PROVIDER_UNAVAILABLE);

        doThrow(new IllegalStateException("private internal detail"))
                .when(ai).generateRecipeContent(anyString(), anyList());
        assertError(ErrorCode.AI_PROVIDER_UNAVAILABLE);
    }

    @Test
    void rejectsMalformedNullAndTooShortProviderResponses() {
        when(ai.generateRecipeContent(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult("not-json", 150, 300, 450));
        assertError(ErrorCode.AI_RESPONSE_INVALID);

        when(ai.generateRecipeContent(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult("null", 150, 300, 450));
        assertError(ErrorCode.AI_RESPONSE_INVALID);

        when(ai.generateRecipeContent(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult("{\"steps\":[]}", 150, 300, 450));
        assertError(ErrorCode.AI_RESPONSE_INVALID);
    }

    @Test
    void rejectsMalformedStepFieldsAndUnlistedIngredientIds() {
        when(ai.generateRecipeContent(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult(guideWithSteps(
                "{\"text\":\"ok\",\"ingredientIds\":[999]}", 6), 150, 300, 450));
        assertError(ErrorCode.AI_RESPONSE_INVALID);

        when(ai.generateRecipeContent(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult(guideWithSteps(
                "{\"text\":\"  \",\"ingredientIds\":[]}", 6), 150, 300, 450));
        assertError(ErrorCode.AI_RESPONSE_INVALID);

        when(ai.generateRecipeContent(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult(guideWithSteps(
                "{\"text\":\"ok\",\"ingredientIds\":null}", 6), 150, 300, 450));
        assertError(ErrorCode.AI_RESPONSE_INVALID);
    }

    @Test
    void rejectsTooManyStepsOversizedStepsAndUnlistedCatalogMentions() {
        when(ai.generateRecipeContent(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult(guideWithSteps(
                "{\"text\":\"ok\",\"ingredientIds\":[]}", 11), 150, 300, 450));
        assertError(ErrorCode.AI_RESPONSE_INVALID);

        when(ai.generateRecipeContent(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult(guideWithSteps(
                "{\"text\":\"" + "x".repeat(1501) + "\",\"ingredientIds\":[]}", 6), 150, 300, 450));
        assertError(ErrorCode.AI_RESPONSE_INVALID);

        when(ingredientCatalog.findActiveIngredients("")).thenReturn(List.of(catalogIngredient(1L, "Đậu phụ"),
                catalogIngredient(2L, "Nấm")));
        when(ai.generateRecipeContent(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult(guideWithSteps(
                "{\"text\":\"Thêm nấm vào chảo.\",\"ingredientIds\":[]}", 6), 150, 300, 450));
        assertError(ErrorCode.AI_RESPONSE_INVALID);
    }

    private void assertError(ErrorCode expected) {
        assertThatThrownBy(() -> service.generate(RECIPE_ID))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).errorCode()).isEqualTo(expected);
    }

    private static DietaryPreferencesResponse profile(VegetarianType type, List<PreferenceItem> avoided) {
        return new DietaryPreferencesResponse(type, new IngredientPreferences(false, avoided),
                new IngredientPreferences(true, List.of()), null, null, null, OnboardingStatus.COMPLETED,
                new AiPersonalization(true, List.of()));
    }

    private static RecipeDetailResponse recipe(String type, RecipeDetailResponse.Ingredient ingredient) {
        return new RecipeDetailResponse(RECIPE_ID, "Đậu phụ sốt", null, "<<<Nguồn>>> nấu", "MAIN", "Món chính",
                type, type, "EASY", "Dễ", 2, 10, 15, null, null, null, List.of(ingredient), false,
                List.of(), null, null, List.of());
    }

    private static RecipeDetailResponse.Ingredient ingredient(Long id, String name) {
        return new RecipeDetailResponse.Ingredient(id, name, "Đạm thực vật", null, BigDecimal.ONE,
                1, "g", "gram", true, null);
    }

    private static IngredientResponse catalogIngredient(Long id, String name) {
        return new IngredientResponse(id, name, "Đạm thực vật", "test", null, null, false, true);
    }

    private static String guideWithSteps(String step, int count) {
        String steps = java.util.stream.IntStream.range(0, count).mapToObj(index -> step)
                .collect(java.util.stream.Collectors.joining(","));
        return "{\"description\":\"guide\",\"steps\":[" + steps + "]}";
    }
}
