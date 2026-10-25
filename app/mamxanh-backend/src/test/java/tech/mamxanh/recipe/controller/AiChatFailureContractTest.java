package tech.mamxanh.recipe.controller;

import static org.mockito.ArgumentMatchers.anyString;
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
import tech.mamxanh.common.exception.GlobalExceptionHandler;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.recipe.service.*;
import tools.jackson.databind.json.JsonMapper;

/** Real DTO validation and chat orchestration through HTTP; provider responses are controlled. */
@WebMvcTest(AiChatController.class)
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

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void mvcDebugLoggingHidesPrivatePayloadsButHttpStillReturnsThem(CapturedOutput output) throws Exception {
        when(users.findAuthenticatedUserId()).thenReturn(Optional.empty());
        when(ai.answerChat(anyString(), anyString()))
                .thenReturn(new AiClient.ChatResult("FR10_SYNTHETIC_PRIVATE_ANSWER", 1, 1, 2));
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
        when(ai.answerChat(anyString(), anyString()))
                .thenThrow(AiProviderException.invalidResponse("incomplete"))
                .thenThrow(new AiProviderException("unavailable"))
                .thenThrow(new AiProviderException("provider throttled",
                        new com.google.genai.errors.ApiException(429, "RESOURCE_EXHAUSTED", "private-provider-body")));
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
        when(ai.answerChat(anyString(), anyString())).thenReturn(new AiClient.ChatResult("OK", 1, 1, 2));
        mvc.perform(post("/api/v1/recipes/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content(body(List.of(Map.of("speaker", "ASSISTANT", "content", "a".repeat(8000))))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.answer").value("OK"));
        clearInvocations(ai);
        for (var turn : List.of(Map.of("speaker", "ASSISTANT", "content", "a".repeat(8001)),
                Map.of("speaker", "USER", "content", "a".repeat(1001)))) {
            mvc.perform(post("/api/v1/recipes/ai/chat").contentType(MediaType.APPLICATION_JSON)
                            .content(body(List.of(turn))))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
        verifyNoInteractions(ai);
    }

    private String body(List<Map<String, String>> history) {
        return new JsonMapper().writeValueAsString(Map.of("contextType", "GENERAL", "question", "Hello",
                "history", history, "anonymousViewerId", UUID.randomUUID().toString()));
    }
}
