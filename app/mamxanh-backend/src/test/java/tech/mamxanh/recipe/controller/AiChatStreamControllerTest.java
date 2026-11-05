package tech.mamxanh.recipe.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tech.mamxanh.auth.security.AccountStatusJwtAuthenticationConverter;
import tech.mamxanh.common.config.ClockConfig;
import tech.mamxanh.common.config.SecurityConfig;
import tech.mamxanh.common.exception.GlobalExceptionHandler;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ContextType;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ExecutionMode;
import tech.mamxanh.recipe.dto.response.AiChatResponse;
import tech.mamxanh.recipe.dto.response.AiChatStreamEvent;
import tech.mamxanh.recipe.service.AiChatService;
import tech.mamxanh.recipe.service.AiChatCancellationSignal;
import tech.mamxanh.recipe.service.GuestChatRateLimiter;

@WebMvcTest(AiChatController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, ClockConfig.class, GuestChatRateLimiter.class, GlobalExceptionHandler.class})
class AiChatStreamControllerTest {

    @Autowired MockMvc mvc;
    @MockitoBean AiChatService service;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean AccountStatusJwtAuthenticationConverter jwtAuthenticationConverter;

    private static final String STREAM_PATH = "/api/v1/recipes/ai/chat/stream";
    private static final String CANCEL_PATH = "/api/v1/recipes/ai/chat/cancel";

    @Test
    void streamEndpointReturnsEventStreamContentTypeAndInvokesService() throws Exception {
        when(service.registerStreamCancellation(any())).thenReturn(new AiChatCancellationSignal());
        doAnswer(inv -> {
            java.util.function.Consumer<AiChatStreamEvent> consumer = inv.getArgument(1);
            consumer.accept(AiChatStreamEvent.delta("Xin chào "));
            consumer.accept(AiChatStreamEvent.delta("bạn!"));
            consumer.accept(AiChatStreamEvent.complete(new AiChatResponse(
                    ContextType.GENERAL, null, "Xin chào bạn!", List.of(), null,
                    UUID.randomUUID(), ExecutionMode.CHATBOT, UUID.randomUUID(), List.of(),
                    null, null, List.of())));
            return null;
        }).when(service).stream(any(), any(), any());

        String body = """
                {
                    "contextType": "GENERAL",
                    "question": "Chào bạn",
                    "history": []
                }
                """;

        MvcResult result = mvc.perform(post(STREAM_PATH)
                        .with(user("10").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(request().asyncStarted())
                .andReturn();

        mvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));

        verify(service, times(1)).stream(any(), any(), any());
    }

    @Test
    void cancelEndpointReturnsStoppedResponseFromService() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(service.answer(any())).thenReturn(new AiChatResponse(
                ContextType.GENERAL, null, "Đã dừng lượt chạy theo yêu cầu của bạn.", List.of(), null,
                sessionId, ExecutionMode.AGENT, UUID.randomUUID(), List.of(),
                null, null, List.of(), "ADK_ASSISTANT", "STOPPED", null, null));

        String body = """
                {
                    "contextType": "GENERAL",
                    "question": "Dừng",
                    "history": [],
                    "sessionId": "%s"
                }
                """.formatted(sessionId);

        mvc.perform(post(CANCEL_PATH)
                        .with(user("10").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runStatus").value("STOPPED"))
                .andExpect(jsonPath("$.answer").value("Đã dừng lượt chạy theo yêu cầu của bạn."));

        verify(service, times(1)).answer(any());
    }

    @Test
    void cancelEndpointAcceptsClientRequestIdBeforeSessionIdExists() throws Exception {
        UUID clientRequestId = UUID.randomUUID();
        when(service.answer(any())).thenReturn(new AiChatResponse(
                ContextType.GENERAL, null, "Đã dừng lượt chạy theo yêu cầu của bạn.", List.of(), null,
                null, ExecutionMode.AGENT, UUID.randomUUID(), List.of(),
                null, null, List.of(), "ADK_ASSISTANT", "STOPPED", null, null));

        String body = """
                {
                    "contextType": "GENERAL",
                    "question": "Dừng",
                    "history": [],
                    "clientRequestId": "%s",
                    "controlAction": "CANCEL"
                }
                """.formatted(clientRequestId);

        mvc.perform(post(CANCEL_PATH)
                        .with(user("10").roles("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runStatus").value("STOPPED"));

        verify(service).answer(argThat(request -> clientRequestId.equals(request.clientRequestId())
                && request.sessionId() == null
                && request.controlAction() == AiChatRequest.ControlAction.CANCEL));
    }
}
