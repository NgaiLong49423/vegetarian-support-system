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

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.auth.service.CurrentUserService.CurrentUser;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.service.IngredientCatalogLookupService;
import tech.mamxanh.recipe.dto.request.RecipeAiAssistanceRequest;
import tech.mamxanh.recipe.dto.request.RecipeAiAssistanceRequest.ContentType;
import tech.mamxanh.subscription.service.SubscriptionEntitlementService;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RecipeAiAssistanceServiceTest {
    private static final IngredientResponse TOFU = ingredient(1L, "Đậu phụ");
    private static final IngredientResponse TOMATO = ingredient(2L, "Cà chua");

    @Mock private CurrentUserService currentUserService;
    @Mock private SubscriptionEntitlementService entitlementService;
    @Mock private IngredientCatalogLookupService ingredientCatalog;
    @Mock private AiClient aiClient;
    @Mock private org.springframework.context.ApplicationEventPublisher events;

    private RecipeAiAssistanceService service;

    @BeforeEach
    void setUp() {
        service = new RecipeAiAssistanceService(currentUserService, entitlementService, ingredientCatalog,
                aiClient, new JsonMapper(), events, java.time.Clock.systemUTC());
        when(currentUserService.requireActiveExpert()).thenReturn(new CurrentUser(7L, Role.EXPERT));
        lenient().when(ingredientCatalog.findActiveIngredientsByIds(List.of(1L, 2L))).thenReturn(List.of(TOFU, TOMATO));
    }

    @Test
    void generatedStepsUseOnlyAllowedIdsAndResolveNamesFromCatalog() {
        when(aiClient.generateRecipeContent(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult(generatedInstructions(
                "Đun nóng chảo rồi đảo đều đến khi mềm.", List.of(1L, 2L)), 150, 300, 450));

        var result = service.generate(request(ContentType.INSTRUCTIONS));

        assertThat(result.content()).startsWith("1. Đun nóng chảo rồi đảo đều đến khi mềm. (Đậu phụ, Cà chua)\n");
        assertThat(result.content().lines()).hasSize(6);
        verify(entitlementService).requirePlusOrPro(7L);
        org.mockito.ArgumentCaptor<tech.mamxanh.integration.ai.AiUsageEvent> telemetry =
                org.mockito.ArgumentCaptor.forClass(tech.mamxanh.integration.ai.AiUsageEvent.class);
        verify(events).publishEvent(telemetry.capture());
        assertThat(telemetry.getValue().feature()).isEqualTo("FR-21");
        assertThat(telemetry.getValue().promptTokens()).isEqualTo(150);
    }

    @Test
    void selectedSoySauceInstructionsCanMentionWater() {
        var soySauce = ingredient(3L, "Nước tương");
        when(ingredientCatalog.findActiveIngredientsByIds(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(TOFU, TOMATO, soySauce));
        when(aiClient.generateRecipeContent(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult(generatedInstructions(
                "Cho nước tương vào nồi, kho đến khi nước sốt sánh lại.", List.of(1L, 2L, 3L)), 150, 300, 450));
        var withSoySauce = new RecipeAiAssistanceRequest(ContentType.INSTRUCTIONS, "Đậu phụ kho cà chua",
                "BRAISED", "VEGAN", 2, List.of(
                        new RecipeAiAssistanceRequest.Ingredient(1L, 1, new BigDecimal("300")),
                        new RecipeAiAssistanceRequest.Ingredient(2L, 1, new BigDecimal("200")),
                        new RecipeAiAssistanceRequest.Ingredient(3L, 1, new BigDecimal("15"))));

        var result = service.generate(withSoySauce);

        assertThat(result.content()).contains("nước sốt sánh lại.", "Nước tương");
        assertThat(result.content().lines()).hasSize(6);
    }

    @Test
    void unknownGeneratedIngredientIdsDoNotRejectInstructionText() {
        when(aiClient.generateRecipeContent(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult(
                generatedInstructions("Nấu thật ngon món ăn này.", List.of(99L)), 150, 300, 450));

        var result = service.generate(request(ContentType.INSTRUCTIONS));

        assertThat(result.content()).contains("Nấu thật ngon món ăn này.");
        assertThat(result.content()).doesNotContain("(Đậu phụ", "(Cà chua");
        assertThat(result.content().lines()).hasSize(6);
    }

    @Test
    void allowsAdditionalIngredientMentionedInGeneratedText() {
        when(aiClient.generateRecipeContent(anyString(), anyList())).thenReturn(new AiClient.RecommendationResult(generatedInstructions(
                "Thêm nấm và đảo đều.", List.of(1L)), 150, 300, 450));

        var result = service.generate(request(ContentType.INSTRUCTIONS));

        assertThat(result.content()).contains("Thêm nấm và đảo đều.");
        assertThat(result.content().lines()).hasSize(6);
    }

    @Test
    void blocksProviderWhenEntitlementIsDenied() {
        org.mockito.Mockito.doThrow(new AppException(ErrorCode.AI_SUBSCRIPTION_REQUIRED))
                .when(entitlementService).requirePlusOrPro(anyLong());

        assertThatThrownBy(() -> service.generate(request(ContentType.INSTRUCTIONS)))
                .isInstanceOf(AppException.class);
        verify(aiClient, never()).generateRecipeContent(anyString(), anyList());
    }

    @Test
    void rejectsDescriptionLongerThanAcceptanceLimit() {
        when(aiClient.generateRecipeContent(anyString(), anyList()))
                .thenReturn(new tech.mamxanh.integration.ai.AiClient.RecommendationResult("{\"description\":\"" + "x".repeat(2001) + "\",\"steps\":[]}", 150, 300, 450));

        assertThatThrownBy(() -> service.generate(request(ContentType.DESCRIPTION)))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).errorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
    }

    @Test
    void rejectsDuplicateOrInactiveIngredientsBeforeCallingProvider() {
        var duplicate = new RecipeAiAssistanceRequest(ContentType.INSTRUCTIONS, "Đậu phụ sốt", "BRAISED", "VEGAN", 2,
                List.of(new RecipeAiAssistanceRequest.Ingredient(1L, 1, BigDecimal.ONE),
                        new RecipeAiAssistanceRequest.Ingredient(1L, 1, BigDecimal.ONE)));

        assertThatThrownBy(() -> service.generate(duplicate)).isInstanceOf(AppException.class);
        verify(aiClient, never()).generateRecipeContent(anyString(), anyList());
    }

    @Test
    void translatesProviderFailuresWithoutExposingProviderDetails() {
        when(aiClient.generateRecipeContent(anyString(), anyList())).thenThrow(new AiProviderException("provider detail"));

        assertThatThrownBy(() -> service.generate(request(ContentType.DESCRIPTION)))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).errorCode()).isEqualTo(ErrorCode.AI_PROVIDER_UNAVAILABLE);
    }

    @Test
    void translatesUnexpectedProviderRuntimeFailures() {
        when(aiClient.generateRecipeContent(anyString(), anyList())).thenThrow(new IllegalStateException("internal detail"));

        assertThatThrownBy(() -> service.generate(request(ContentType.DESCRIPTION)))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).errorCode()).isEqualTo(ErrorCode.AI_PROVIDER_UNAVAILABLE);
    }

    @Test
    void incompleteProviderOutputRemainsAnInvalidResponseInsteadOfAnOutage() {
        when(aiClient.generateRecipeContent(anyString(), anyList()))
                .thenThrow(AiProviderException.invalidResponse("private incomplete output"));
        assertThatThrownBy(() -> service.generate(request(ContentType.DESCRIPTION)))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).errorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
    }

    @Test
    void rejectsMalformedProviderJson() {
        when(aiClient.generateRecipeContent(anyString(), anyList())).thenReturn(new tech.mamxanh.integration.ai.AiClient.RecommendationResult("not-json", 150, 300, 450));

        assertThatThrownBy(() -> service.generate(request(ContentType.DESCRIPTION)))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).errorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
    }

    @Test
    void rejectsEmptyOrOversizedCookingInstructions() {
        when(aiClient.generateRecipeContent(anyString(), anyList())).thenReturn(new tech.mamxanh.integration.ai.AiClient.RecommendationResult("{\"description\":\"Ok\",\"steps\":[]}", 150, 300, 450));
        assertThatThrownBy(() -> service.generate(request(ContentType.INSTRUCTIONS)))
                .isInstanceOf(AppException.class);

        when(aiClient.generateRecipeContent(anyString(), anyList()))
                .thenReturn(new AiClient.RecommendationResult(generatedInstructions("x".repeat(5000), List.of()), 150, 300, 450));
        assertThatThrownBy(() -> service.generate(request(ContentType.INSTRUCTIONS)))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).errorCode()).isEqualTo(ErrorCode.AI_RESPONSE_INVALID);
    }

    @Test
    void rejectsUnavailableIngredientsBeforeCallingProvider() {
        when(ingredientCatalog.findActiveIngredientsByIds(List.of(1L, 2L))).thenReturn(List.of(TOFU));

        assertThatThrownBy(() -> service.generate(request(ContentType.DESCRIPTION)))
                .isInstanceOf(AppException.class)
                .extracting(error -> ((AppException) error).errorCode()).isEqualTo(ErrorCode.RECIPE_DATA_INVALID);
        verify(aiClient, never()).generateRecipeContent(anyString(), anyList());
    }

    private static String generatedInstructions(String firstStep, List<Long> ids) {
        List<String> stages = List.of(firstStep, "Rửa sạch rồi cắt thành miếng vừa ăn.",
                "Làm nóng nồi trên bếp.", "Cho vào nồi và đảo nhẹ tay.",
                "Nấu đến khi mềm và phần sốt sánh lại.", "Tắt bếp rồi bày ra đĩa.");
        return new JsonMapper().writeValueAsString(java.util.Map.of(
                "description", "Món đậu phụ cà chua thanh vị.",
                "steps", stages.stream().map(text -> java.util.Map.of("text", text, "ingredientIds", ids)).toList()));
    }

    private static RecipeAiAssistanceRequest request(ContentType type) {
        return new RecipeAiAssistanceRequest(type, "Đậu phụ sốt cà", "BRAISED", "VEGAN", 2,
                List.of(new RecipeAiAssistanceRequest.Ingredient(1L, 1, new BigDecimal("200")),
                        new RecipeAiAssistanceRequest.Ingredient(2L, 1, new BigDecimal("100"))));
    }

    private static IngredientResponse ingredient(Long id, String name) {
        return new IngredientResponse(id, name, "Test", "Test", null, java.time.LocalDate.now(), false, true);
    }
}
