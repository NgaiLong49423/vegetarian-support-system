package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.json.JsonMapper;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.*;
import tech.mamxanh.integration.ai.*;
import tech.mamxanh.nutrition.dto.response.*;
import tech.mamxanh.nutrition.entity.MeasurementDimension;
import tech.mamxanh.nutrition.service.IngredientCatalogLookupService;
import tech.mamxanh.recipe.dto.request.RecipeAiDraftRequest;
import tech.mamxanh.recipe.dto.request.RecipeAiDraftRequest.*;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;

class RecipeAiDraftServiceTest {
    private final CurrentUserService users = mock(CurrentUserService.class);
    private final SubscriptionEntitlementService entitlements = mock(SubscriptionEntitlementService.class);
    private final IngredientCatalogLookupService catalog = mock(IngredientCatalogLookupService.class);
    private final AiClient ai = mock(AiClient.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final RecipeAiDraftService service = new RecipeAiDraftService(users, entitlements, catalog, ai, new JsonMapper(), events, Clock.systemUTC());
    private final List<IngredientResponse> ingredients = List.of(ingredient(11, "Đậu hũ", "100"), ingredient(12, "Nấm rơm", "20"));

    @BeforeEach void prepare() {
        when(users.requireActiveExpert()).thenReturn(new CurrentUserService.CurrentUser(7, Role.EXPERT));
        when(catalog.findActiveIngredients("")).thenReturn(ingredients);
        when(catalog.findActiveIngredientsByIds(anyList())).thenReturn(ingredients);
        when(catalog.findActiveUnits()).thenReturn(List.of(new UnitResponse(9, "g", "Gam", MeasurementDimension.MASS, BigDecimal.ONE, true)));
        when(ai.generateRecipeDraft(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult(valid(), 100, 50, 150));
    }
    @Test void checksProBeforeCatalogOrProvider() {
        doThrow(new AppException(ErrorCode.AI_SUBSCRIPTION_REQUIRED)).when(entitlements).requirePro(7);
        assertThatThrownBy(() -> service.generate(request("Đậu hũ, Nấm rơm"))).isInstanceOf(AppException.class);
        verifyNoInteractions(catalog, ai, events);
    }
    @Test void ignoresUntrustedProviderCalorieClaimsAndUsesTheActualCatalogWithoutSaving() {
        when(ai.generateRecipeDraft(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult(valid().replaceFirst("\\{", "{\"claimedCalories\":99999,"), 100, 50, 150));
        var result = service.generate(request("dau hu, nam rom"));
        assertThat(result.totalCalories()).isEqualByComparingTo("140.00");
        assertThat(result.caloriesPerServing()).isEqualByComparingTo("70.00");
        assertThat(result.targetCaloriesPerServing()).isEqualTo(350);
        assertThat(result.ingredients()).extracting(item -> item.unitId()).containsOnly(9);
        assertThat(result.ingredients()).extracting(item -> item.ingredientId()).containsExactly(11L, 12L);
        assertThat(result.toString()).doesNotContain("Đậu", "Nấm", "Món");
        verify(events).publishEvent(any(AiUsageEvent.class));
        var order = inOrder(users, entitlements, catalog, ai);
        order.verify(users).requireActiveExpert(); order.verify(entitlements).requirePro(7);
        order.verify(catalog).findActiveIngredients(""); order.verify(catalog).findActiveUnits();
        order.verify(ai).generateRecipeDraft(anyString(), eq(List.of(11L, 12L)));
    }
    @ParameterizedTest @ValueSource(strings = {"", " ; ", "Không có", "Đậu hũ, dau hu", "Đậu hũ và thịt bò"})
    void rejectsUnresolvedOrDuplicateIngredientsBeforeProvider(String raw) {
        assertThatThrownBy(() -> service.generate(request(raw))).isInstanceOfSatisfying(AppException.class, e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.RECIPE_DATA_INVALID));
        verifyNoInteractions(ai, events);
    }
    @Test void refusesAmbiguousNamesInsteadOfPickingTheFirstCatalogEntry() {
        when(catalog.findActiveIngredients("")).thenReturn(List.of(ingredient(11, "Đậu hũ trắng", "100"), ingredient(12, "Đậu hũ chiên", "200")));
        assertThatThrownBy(() -> service.generate(request("Đậu hũ"))).isInstanceOf(AppException.class);
        verifyNoInteractions(ai, events);
    }
    @Test void requiresRealEnergyAndAnActiveGramUnit() {
        var incomplete = new IngredientResponse(11L, "Đậu hũ", "Đậu", "Nguồn", null, LocalDate.now(), false, true);
        when(catalog.findActiveIngredients("")).thenReturn(List.of(incomplete));
        assertThatThrownBy(() -> service.generate(request("Đậu hũ"))).isInstanceOf(AppException.class);
        when(catalog.findActiveIngredients("")).thenReturn(ingredients);
        when(catalog.findActiveUnits()).thenReturn(List.of(new UnitResponse(2, "ml", "ml", MeasurementDimension.VOLUME, BigDecimal.ONE, true)));
        assertThatThrownBy(() -> service.generate(request("Đậu hũ,Nấm rơm"))).isInstanceOf(AppException.class);
        verifyNoInteractions(ai, events);
    }
    @Test void rejectsIngredientsRetiredWhileGeminiWasRunning() {
        when(catalog.findActiveIngredientsByIds(anyList())).thenReturn(List.of());
        assertThatThrownBy(() -> service.generate(request("Đậu hũ,Nấm rơm"))).isInstanceOf(AppException.class);
        verifyNoInteractions(events);
    }
    @ParameterizedTest @ValueSource(strings = {"negative", "zero", "extra", "missing", "duplicate", "precision", "string-number", "unknown-code", "bad-time", "bad-step", "invalid-json"})
    void rejectsUnsafeOrIncompleteProviderOutput(String type) {
        String raw = switch(type) {
            case "negative" -> valid().replace("100.0", "-100.0");
            case "zero" -> valid().replace("100.0", "0");
            case "extra" -> valid().replace("\"ingredientId\":11", "\"ingredientId\":999");
            case "missing" -> valid().replace(",{\"ingredientId\":12,\"quantityGrams\":200.0}", "");
            case "duplicate" -> valid().replace("\"ingredientId\":12", "\"ingredientId\":11");
            case "precision" -> valid().replace("100.0", "0.001");
            case "string-number" -> valid().replace("100.0", "\"100\"");
            case "unknown-code" -> valid().replace("STIR_FRY", "MADE_UP");
            case "bad-time" -> valid().replace("\"prepTimeMinutes\":5", "\"prepTimeMinutes\":-1");
            case "bad-step" -> valid().replace("\"ingredientIds\":[11,12]", "\"ingredientIds\":[999]");
            default -> "not JSON";
        };
        when(ai.generateRecipeDraft(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult(raw, 1, 1, 2));
        assertThatThrownBy(() -> service.generate(request("Đậu hũ,Nấm rơm"))).as(type)
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID));
        verifyNoInteractions(events);
    }
    @Test void providerFailuresNeverBecomeLocalFakeRecipes() {
        when(ai.generateRecipeDraft(anyString(), anyList())).thenThrow(new GeminiAiClient.AiProviderException("unavailable"));
        assertThatThrownBy(() -> service.generate(request("Đậu hũ,Nấm rơm"))).isInstanceOfSatisfying(AppException.class, e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.AI_PROVIDER_UNAVAILABLE));
        verifyNoInteractions(events);
    }
    private RecipeAiDraftRequest request(String raw) { return new RecipeAiDraftRequest("Đậu hũ xào nấm", raw, MealType.LUNCH, 350, 2, CookingMethod.STIR_FRY); }
    private IngredientResponse ingredient(long id, String name, String kcal) {
        return new IngredientResponse(id, name, "Đậu và nấm", "Danh mục thử nghiệm", null, LocalDate.now(), true, true,
            new IngredientNutritionResponse(new BigDecimal(kcal), BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE));
    }
    private String valid() {
        String step = "{\"text\":\"Sơ chế đậu hũ và nấm rơm, đảo chín đều.\",\"ingredientIds\":[11,12]}";
        return "{\"description\":\"Món đậu hũ nấm đã chọn.\",\"steps\":[" + String.join(",", Collections.nCopies(6, step))
                + "],\"dishCategory\":\"STIR_FRY\",\"vegetarianType\":\"VEGAN\",\"difficulty\":\"EASY\",\"prepTimeMinutes\":5,\"cookTimeMinutes\":10,"
                + "\"ingredients\":[{\"ingredientId\":11,\"quantityGrams\":100.0},{\"ingredientId\":12,\"quantityGrams\":200.0}]}";
    }
}
