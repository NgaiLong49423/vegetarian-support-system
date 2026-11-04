package tech.mamxanh.recipe.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.mamxanh.auth.security.AccountStatusJwtAuthenticationConverter;
import tech.mamxanh.common.config.ClockConfig;
import tech.mamxanh.common.config.SecurityConfig;
import tech.mamxanh.common.exception.GlobalExceptionHandler;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ContextType;
import tech.mamxanh.recipe.dto.response.AiChatResponse;
import tech.mamxanh.recipe.service.AiChatService;
import tech.mamxanh.recipe.service.GuestChatRateLimiter;

@WebMvcTest(AiChatController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, ClockConfig.class, GuestChatRateLimiter.class, GlobalExceptionHandler.class})
class AiChatAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockitoBean AiChatService service;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean AccountStatusJwtAuthenticationConverter converter;
    private static final String PATH = "/api/v1/recipes/ai/chat";
    private static final String MEMBER_BODY = "{\"contextType\":\"GENERAL\",\"question\":\"Nấu canh thế nào?\",\"history\":[]}";

    @Test void guestRequiresCookieIdentityAndCannotCallOtherAiOperations() throws Exception {
        mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(MEMBER_BODY))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/recipes/ai/recommendations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredientIds\":[1]}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void guestsCanChatThreeTimesThenReceive429BeforeTheChatService() throws Exception {
        when(service.answer(any())).thenReturn(new AiChatResponse(ContextType.GENERAL, null, "Nấu nhỏ lửa.", List.of()));
        String body = MEMBER_BODY.replace("\"history\":[]", "\"history\":[],\"anonymousViewerId\":\"" + UUID.randomUUID() + "\"");
        for (int i = 0; i < 3; i++) {
            mvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.answer").value("Nấu nhỏ lửa."));
        }
        mvc.perform(post(PATH).header("X-Forwarded-For", "1.2.3.4").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.code").value("AI_GUEST_RATE_LIMITED"));
        verify(service, times(3)).answer(any());
    }

    @Test void authenticatedMembersAreNotSubjectToTheGuestCounter() throws Exception {
        when(service.answer(any())).thenReturn(new AiChatResponse(ContextType.GENERAL, null, "Nấu nhỏ lửa.", List.of()));
        for (int i = 0; i < 12; i++) {
            mvc.perform(post(PATH).with(user("7").roles("CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content(MEMBER_BODY))
                    .andExpect(status().isOk());
        }
        verify(service, times(12)).answer(any());
    }

    @Test void invalidBearerCannotFallBackToGuestEvenWithAValidCookie() throws Exception {
        when(decoder.decode("invalid-token")).thenThrow(new BadJwtException("invalid"));
        String body = MEMBER_BODY.replace("\"history\":[]", "\"history\":[],\"anonymousViewerId\":\"" + UUID.randomUUID() + "\"");
        mvc.perform(post(PATH).header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test void lockedBearerCannotUseThePublicGuestPath() throws Exception {
        var token = org.springframework.security.oauth2.jwt.Jwt.withTokenValue("locked-token")
                .header("alg", "HS256").subject("7").build();
        when(decoder.decode("locked-token")).thenReturn(token);
        when(converter.convert(token)).thenThrow(new org.springframework.security.authentication.LockedException("locked"));
        String body = MEMBER_BODY.replace("\"history\":[]", "\"history\":[],\"anonymousViewerId\":\"" + UUID.randomUUID() + "\"");
        mvc.perform(post(PATH).header("Authorization", "Bearer locked-token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
        verifyNoInteractions(service);
    }
}
