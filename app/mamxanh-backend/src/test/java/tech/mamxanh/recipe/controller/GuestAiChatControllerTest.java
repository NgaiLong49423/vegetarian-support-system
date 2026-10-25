package tech.mamxanh.recipe.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.mamxanh.MutableClock;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.security.AccountStatusJwtAuthenticationConverter;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.config.*;
import tech.mamxanh.common.exception.*;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException;
import tech.mamxanh.nutrition.service.DietaryPreferenceService;
import tech.mamxanh.recipe.dto.response.RecipeDetailResponse;
import tech.mamxanh.recipe.service.*;

/** Real MVC, Security, CORS, chat orchestration and limiter; provider/catalog are controlled doubles. */
@WebMvcTest(value = AiChatController.class, properties = "mamxanh.cors.allowed-origins=http://localhost:5173")
@ActiveProfiles("test")
@EnableConfigurationProperties(SpringDocConfigProperties.class)
@Import({SecurityConfig.class, CorsConfig.class, GlobalExceptionHandler.class, OpenApiConfig.class,
        AiChatService.class, GuestChatRateLimiter.class, GuestAiChatControllerTest.TimeConfig.class,
        SpringDocConfiguration.class, SpringDocSpecPropertiesConfiguration.class, SpringDocWebMvcConfiguration.class})
class GuestAiChatControllerTest {
    private static final String URL = "/api/v1/recipes/ai/chat";
    private static final String BODY = "{\"contextType\":\"GENERAL\",\"question\":\"Cách nấu đậu?\"}";
    @Autowired MockMvc mvc;
    @Autowired Clock clock;
    @MockitoBean CurrentUserService users;
    @MockitoBean RecipeService recipes;
    @MockitoBean RecipePostService posts;
    @MockitoBean DietaryPreferenceService preferences;
    @MockitoBean AiClient ai;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean AccountStatusJwtAuthenticationConverter jwtAuthenticationConverter;

    @TestConfiguration
    static class TimeConfig { @Bean Clock clock() { return new MutableClock(); } }

    @BeforeEach
    void setup() {
        ((MutableClock) clock).advance(Duration.ofMinutes(2));
        when(ai.answerChat(anyString(), anyString())).thenReturn(new AiClient.ChatResult("Nấu nhỏ lửa.", 1, 2, 3));
    }

    @Test
    void anonymousGeneralAndRecipeRequestsUseTheSameChatbotWithoutProfileAccess() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.answer").value("Nấu nhỏ lửa."));
        when(recipes.getPublishedRecipe(19L)).thenReturn(new RecipeDetailResponse(
                19L, "Canh rau", "Mô tả", "Nấu trong 10 phút", "SOUP", "Món canh", "VEGAN", "Thuần chay",
                "EASY", "Dễ", 2, 5, 10, null, null, null, List.of(), true, List.of(), null, null, List.of()));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contextType\":\"RECIPE\",\"recipeId\":19,\"question\":\"Nấu bao lâu?\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.recipeId").value(19));
        verify(ai).answerChat(anyString(), contains("Canh rau"));
        verifyNoInteractions(preferences);
    }

    @Test
    void eleventhRequestIs429WithRetryAfterAndDoesNotReachGemini() throws Exception {
        String id = UUID.randomUUID().toString();
        for (int i = 0; i < 10; i++) mvc.perform(post(URL).header("X-Guest-Chat-Id", id)
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isOk());
        mvc.perform(post(URL).header("X-Guest-Chat-Id", id).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.code").value("GUEST_CHAT_RATE_LIMITED"));
        verify(ai, times(10)).answerChat(anyString(), anyString());
        ((MutableClock) clock).advance(Duration.ofMinutes(1));
        mvc.perform(post(URL).header("X-Guest-Chat-Id", id).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    void rotatingCookiesAndSpoofedForwardingHeadersStillHitTheCoarsePeerLimit() throws Exception {
        when(recipes.getPublishedRecipe(anyLong())).thenThrow(new AppException(ErrorCode.RECIPE_NOT_FOUND));
        String body = "{\"contextType\":\"RECIPE\",\"recipeId\":999,\"question\":\"Cách nấu?\"}";
        for (int i = 0; i < 60; i++) mvc.perform(post(URL)
                        .header("X-Guest-Chat-Id", UUID.randomUUID().toString()).header("X-Forwarded-For", "192.0.2." + i)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        mvc.perform(post(URL).header("X-Guest-Chat-Id", UUID.randomUUID().toString())
                        .header("X-Forwarded-For", "198.51.100.1").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests());
        verifyNoInteractions(ai);
    }

    @Test
    void providerFailureAndInvalidOutputHaveFriendlyProblemResponses() throws Exception {
        when(ai.answerChat(anyString(), anyString())).thenThrow(new AiProviderException("timeout"));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("AI_PROVIDER_UNAVAILABLE"));
        doReturn(null).when(ai).answerChat(anyString(), anyString());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("AI_RESPONSE_INVALID"));
    }

    @Test
    void invalidRequestsAndUnavailableRecipesNeverReachTheProvider() throws Exception {
        for (String invalid : List.of("{}", "{\"contextType\":\"GENERAL\",\"question\":\" \"}",
                BODY.replace("Cách nấu đậu?", "a".repeat(1001)),
                BODY.replace("GENERAL", "RECIPE"), BODY.replace("?\"}", "?\",\"recipeId\":1}"),
                BODY.replace("?\"}", "?\",\"history\":[null]}"))) {
            mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post(URL).header("X-Guest-Chat-Id", "malformed").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
        when(recipes.getPublishedRecipe(999L)).thenThrow(new AppException(ErrorCode.RECIPE_NOT_FOUND));
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contextType\":\"RECIPE\",\"recipeId\":999,\"question\":\"Cách nấu?\"}"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(ai);
    }

    @Test
    void bearerFailuresCannotBecomeGuestAndOtherAiEndpointsStayProtected() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("invalid"));
        mvc.perform(post(URL).header("Authorization", "Bearer invalid").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/recipes/ai/weekly-menu").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        when(users.findAuthenticatedUserId()).thenReturn(java.util.Optional.of(7L));
        when(users.requireActiveMember()).thenThrow(new AppException(ErrorCode.ACCESS_DENIED));
        mvc.perform(post(URL).with(user("7").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ai);
    }

    @Test
    void membersKeepTheirProfileAndDoNotConsumeGuestWindows() throws Exception {
        when(users.findAuthenticatedUserId()).thenReturn(java.util.Optional.of(7L));
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7, Role.CUSTOMER));
        when(preferences.menuConstraints(7)).thenReturn(new DietaryPreferenceService.MenuDietaryConstraints("VEGAN", List.of(), List.of()));
        for (int i = 0; i < 11; i++) mvc.perform(post(URL).with(user("7").roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isOk());
        verify(preferences, times(11)).menuConstraints(7);
    }

    @Test
    void browserPreflightAllowsTheGuestIdentityHeaderOnlyOnChatAndExposesRetryAfter() throws Exception {
        mvc.perform(options(URL).header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,x-guest-chat-id"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Expose-Headers", "Retry-After"));
        mvc.perform(options(URL).header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
        mvc.perform(options("/api/v1/recipes").header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "x-guest-chat-id"))
                .andExpect(status().isForbidden());
    }

    @Test
    void generatedContractDeclaresOptionalBearerAndGuestRateLimit() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/ai/chat'].post.security[0]").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/ai/chat'].post.security[1].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/ai/chat'].post.responses['429'].headers['Retry-After']").exists());
    }
}
