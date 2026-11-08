package tech.mamxanh.recipe.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.mamxanh.auth.security.AccountStatusJwtAuthenticationConverter;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.config.ClockConfig;
import tech.mamxanh.common.config.SecurityConfig;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.common.exception.GlobalExceptionHandler;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.response.AiChatResponse;
import tech.mamxanh.recipe.service.*;
import tools.jackson.databind.json.JsonMapper;

/** Real DTO validation and chat orchestration through HTTP; provider responses are controlled. */
@WebMvcTest(value = AiChatController.class, properties = "logging.level.org.springframework.web=DEBUG")
@ExtendWith(OutputCaptureExtension.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, ClockConfig.class, GuestChatRateLimiter.class,
        GlobalExceptionHandler.class, AiChatService.class})
class AiChatFailureContractTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean AccountStatusJwtAuthenticationConverter converter;
    @MockitoBean CurrentUserService users;
    @MockitoBean RecipeService recipes;
    @MockitoBean RecipePostService posts;
    @MockitoBean DietaryPreferenceService preferences;
    @MockitoBean AiClient ai;
    @MockitoBean AdkAssistantEngine adkEngine;
    @MockitoBean tech.mamxanh.subscription.service.SubscriptionEntitlementService entitlements;
    @MockitoBean tech.mamxanh.subscription.service.AiTokenQuotaService aiQuota;
    @MockitoBean tech.mamxanh.subscription.service.AiChatSettingsService chatSettings;

    @Test
    void mvcDebugLoggingHidesPrivatePayloadsButHttpStillReturnsThem(CapturedOutput output) throws Exception {
        when(users.findAuthenticatedUserId()).thenReturn(Optional.empty());
        when(adkEngine.execute(any()))
                .thenReturn(new AiChatResponse(AiChatRequest.ContextType.GENERAL, null,
                        "FR10_SYNTHETIC_PRIVATE_ANSWER", List.of()));

        var json = new JsonMapper().writeValueAsString(Map.of("contextType", "GENERAL",
                "question", "FR10_SYNTHETIC_PRIVATE_QUESTION", "anonymousViewerId", UUID.randomUUID().toString(),
                "history", List.of(Map.of("speaker", "USER", "content", "FR10_SYNTHETIC_PRIVATE_HISTORY"))));
        mvc.perform(post("/api/v1/recipes/ai/chat").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("FR10_SYNTHETIC_PRIVATE_ANSWER"));
        assertThatSafeLog(output.getAll());
    }

    private static void assertThatSafeLog(String log) {
        org.assertj.core.api.Assertions.assertThat(log)
                .contains("AiChatRequest[redacted]", "AiChatResponse[redacted]")
                .doesNotContain("FR10_SYNTHETIC_PRIVATE_QUESTION", "FR10_SYNTHETIC_PRIVATE_ANSWER", "FR10_SYNTHETIC_PRIVATE_HISTORY");
    }

    @Test
    void incompleteProviderResponseIs502WhileTransportFailureIs503() throws Exception {
        when(users.findAuthenticatedUserId()).thenReturn(Optional.empty());
        when(adkEngine.execute(any()))
                .thenThrow(new AppException(ErrorCode.AI_RESPONSE_INVALID))
                .thenThrow(new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE))
                .thenThrow(new AppException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "Dịch vụ AI đang tạm giới hạn yêu cầu. Vui lòng thử lại sau."));

        mvc.perform(post("/api/v1/recipes/ai/chat").contentType(MediaType.APPLICATION_JSON).content(body(List.of())))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("AI_RESPONSE_INVALID"));
        mvc.perform(post("/api/v1/recipes/ai/chat").contentType(MediaType.APPLICATION_JSON).content(body(List.of())))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("AI_PROVIDER_UNAVAILABLE"));
        mvc.perform(post("/api/v1/recipes/ai/chat").contentType(MediaType.APPLICATION_JSON).content(body(List.of())))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("AI_PROVIDER_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value("Dịch vụ AI đang tạm giới hạn yêu cầu. Vui lòng thử lại sau."));
    }

    @Test
    void fullAssistantHistoryIsAcceptedButUserInputAndHistoryRemainBounded() throws Exception {
        when(users.findAuthenticatedUserId()).thenReturn(Optional.empty());
        when(adkEngine.execute(any())).thenReturn(
                new AiChatResponse(AiChatRequest.ContextType.GENERAL, null, "OK", List.of()));

        mvc.perform(post("/api/v1/recipes/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of(Map.of("speaker", "ASSISTANT", "content", "a".repeat(8000))))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.answer").value("OK"));
        clearInvocations(adkEngine);
        for (var turn : List.of(Map.of("speaker", "ASSISTANT", "content", "a".repeat(8001)),
                Map.of("speaker", "USER", "content", "a".repeat(1001)))) {
            mvc.perform(post("/api/v1/recipes/ai/chat").contentType(MediaType.APPLICATION_JSON)
                            .content(body(List.of(turn))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
        verifyNoInteractions(adkEngine);
    }

    private String body(List<Map<String, String>> history) {
        return new JsonMapper().writeValueAsString(Map.of("contextType", "GENERAL", "question", "Hello",
                "history", history, "anonymousViewerId", UUID.randomUUID().toString()));
    }

    @Test
    void advancedChatRequestUsesHttpUpgradeContractAndNeverInvokesProvider() throws Exception {
        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7, tech.mamxanh.auth.entity.Role.CUSTOMER));
        doThrow(new AppException(ErrorCode.AI_SUBSCRIPTION_REQUIRED,
                "Hãy nâng cấp lên Plus để lập thực đơn tuần.").withProperty("requiredTier", "PLUS").withProperty("upgradeUrl", "/goi-ai"))
                .when(entitlements).requirePlusOrPro(7L);
        when(adkEngine.execute(any())).thenThrow(
                new AppException(ErrorCode.AI_SUBSCRIPTION_REQUIRED,
                        "Hãy nâng cấp lên Plus để lập thực đơn tuần.").withProperty("requiredTier", "PLUS").withProperty("upgradeUrl", "/goi-ai"));

        mvc.perform(post("/api/v1/recipes/ai/chat")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("7").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(new JsonMapper().writeValueAsString(Map.of(
                                "contextType", "GENERAL", "question", "Lập thực đơn tuần 7 ngày", "history", List.of()))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.requiredTier").value("PLUS"))
                .andExpect(jsonPath("$.upgradeUrl").value("/goi-ai"));
        verifyNoInteractions(preferences, posts, recipes);
    }

    @Test
    void allowedWorkflowResponseHasALocalLinkAndNoSuccessfulProviderUsage() throws Exception {
        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7, tech.mamxanh.auth.entity.Role.CUSTOMER));
        when(adkEngine.execute(any())).thenReturn(
                new AiChatResponse(AiChatRequest.ContextType.GENERAL, null,
                        "Bạn có thể lập thực đơn tuần trong Lịch ăn.", List.of(), "/ke-hoach?view=for-you"));

        mvc.perform(post("/api/v1/recipes/ai/chat")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("7").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"contextType\":\"GENERAL\",\"question\":\"Lên menu cho tuần này\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.workflowUrl").value("/ke-hoach?view=for-you"))
                .andExpect(jsonPath("$.recommendations").isEmpty());
        verifyNoInteractions(preferences);
    }

    @Test
    void recipeCardHistoryIsOptionalBoundedAndCannotBeAttachedToUserTurns() throws Exception {
        for (String turn : List.of(
                "{\"speaker\":\"ASSISTANT\",\"content\":\"Món\",\"recommendedRecipeIds\":[0]}",
                "{\"speaker\":\"ASSISTANT\",\"content\":\"Món\",\"recommendedRecipeIds\":[null]}",
                "{\"speaker\":\"ASSISTANT\",\"content\":\"Món\",\"recommendedRecipeIds\":[1,2,3,4,5]}",
                "{\"speaker\":\"USER\",\"content\":\"Món\",\"recommendedRecipeIds\":[1]}")) {
            mvc.perform(post("/api/v1/recipes/ai/chat").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"contextType\":\"GENERAL\",\"question\":\"Hello\",\"history\":[" + turn + "]}"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(adkEngine);
    }
}
