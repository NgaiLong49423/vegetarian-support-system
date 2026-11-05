package tech.mamxanh.recipe.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.response.AiChatResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AiChatServiceTest {
    private CurrentUserService users;
    private AdkAssistantEngine adkEngine;
    private AdkShoppingPilotService adkShoppingPilot;
    private AiChatService service;

    @BeforeEach
    void setup() {
        users = mock(CurrentUserService.class);
        adkEngine = mock(AdkAssistantEngine.class);
        adkShoppingPilot = mock(AdkShoppingPilotService.class);
        service = new AiChatService(users, adkEngine, adkShoppingPilot);

        when(users.findAuthenticatedUserId()).thenReturn(Optional.of(7L));
        when(users.requireActiveMember()).thenReturn(new CurrentUserService.CurrentUser(7L, Role.CUSTOMER));
    }

    @Test
    void invalidContextNeverCallsEngine() {
        assertThatThrownBy(() -> service.answer(new AiChatRequest(AiChatRequest.ContextType.RECIPE, null,
                "Câu hỏi", List.of())))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);

        assertThatThrownBy(() -> service.answer(new AiChatRequest(AiChatRequest.ContextType.GENERAL, 19L,
                "Câu hỏi", List.of())))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);

        verifyNoInteractions(adkEngine, adkShoppingPilot);
    }

    @Test
    void historyLongerThanEightTurnsIsRejectedBeforeEngine() {
        List<AiChatRequest.Turn> history = java.util.stream.IntStream.range(0, 9)
                .mapToObj(index -> new AiChatRequest.Turn(AiChatRequest.Speaker.USER, "Question " + index)).toList();

        assertThatThrownBy(() -> service.answer(new AiChatRequest(AiChatRequest.ContextType.GENERAL, null,
                "Câu hỏi hiện tại", history)))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.VALIDATION_FAILED);

        verifyNoInteractions(adkEngine, adkShoppingPilot);
    }

    @Test
    void generalChatRoutesDirectlyToAdkEngine() {
        AiChatRequest request = new AiChatRequest(AiChatRequest.ContextType.GENERAL, null,
                "Cách nấu canh chay?", List.of());
        AiChatResponse expected = new AiChatResponse(AiChatRequest.ContextType.GENERAL, null,
                "Nấu nhỏ lửa.", List.of());
        when(adkEngine.execute(request)).thenReturn(expected);

        AiChatResponse actual = service.answer(request);

        assertThat(actual.answer()).isEqualTo("Nấu nhỏ lửa.");
        verify(adkEngine).execute(request);
    }

    @Test
    void recipeContextChatRoutesDirectlyToAdkEngine() {
        AiChatRequest request = new AiChatRequest(AiChatRequest.ContextType.RECIPE, 19L,
                "Nấu bao lâu?", List.of());
        AiChatResponse expected = new AiChatResponse(AiChatRequest.ContextType.RECIPE, 19L,
                "Khoảng 15 phút.", List.of());
        when(adkEngine.execute(request)).thenReturn(expected);

        AiChatResponse actual = service.answer(request);

        assertThat(actual.recipeId()).isEqualTo(19L);
        assertThat(actual.answer()).isEqualTo("Khoảng 15 phút.");
        verify(adkEngine).execute(request);
    }

    @Test
    void controlActionCancelsOnAdkEngineWhenOwned() {
        UUID sessionId = UUID.randomUUID();
        AiChatRequest cancelRequest = new AiChatRequest(
                AiChatRequest.ContextType.GENERAL, null, "Dừng lại", List.of(),
                null, sessionId, null, null, null, null, null, AiChatRequest.ControlAction.CANCEL);

        when(adkEngine.ownsSession(sessionId, "user:7")).thenReturn(true);
        AiChatResponse expected = new AiChatResponse(AiChatRequest.ContextType.GENERAL, null,
                "Đã dừng lượt trả lời.", List.of());
        when(adkEngine.cancel(cancelRequest, null)).thenReturn(expected);

        AiChatResponse actual = service.answer(cancelRequest);

        assertThat(actual.answer()).isEqualTo("Đã dừng lượt trả lời.");
        verify(adkEngine).cancel(cancelRequest, null);
    }

    @Test
    void cancelByClientRequestIdSignalsAnActiveStreamBeforeSessionIdExists() {
        UUID clientRequestId = UUID.randomUUID();
        AiChatRequest streamRequest = new AiChatRequest(AiChatRequest.ContextType.GENERAL, null,
                "Cách nấu canh chay?", List.of(), null, null, null, null, null, null, clientRequestId);
        AiChatCancellationSignal signal = service.registerStreamCancellation(streamRequest);
        AiChatRequest cancelRequest = new AiChatRequest(AiChatRequest.ContextType.GENERAL, null,
                "Dừng lại", List.of(), null, null, null, null, null, null, clientRequestId,
                AiChatRequest.ControlAction.CANCEL);
        AiChatResponse expected = new AiChatResponse(AiChatRequest.ContextType.GENERAL, null,
                "Đã dừng lượt trả lời.", List.of());
        when(adkEngine.cancel(eq(cancelRequest), same(signal))).thenReturn(expected);

        AiChatResponse actual = service.answer(cancelRequest);

        assertThat(signal.isCancelled()).isTrue();
        assertThat(actual.answer()).isEqualTo("Đã dừng lượt trả lời.");
        verify(adkEngine).cancel(cancelRequest, signal);
        service.releaseStreamCancellation(streamRequest, signal);
    }

    @Test
    void controlActionCancelsOnPilotWhenPilotEngineActive() {
        UUID sessionId = UUID.randomUUID();
        AiChatRequest cancelRequest = new AiChatRequest(
                AiChatRequest.ContextType.GENERAL, null, "Dừng lại", List.of(),
                null, sessionId, null, null, null, null, null, AiChatRequest.ControlAction.CANCEL);

        when(adkEngine.ownsSession(sessionId, "user:7")).thenReturn(false);
        AiChatResponse expected = new AiChatResponse(AiChatRequest.ContextType.GENERAL, null,
                "Đã hủy thao tác.", List.of());
        when(adkShoppingPilot.cancel(cancelRequest)).thenReturn(expected);

        AiChatResponse actual = service.answer(cancelRequest);

        assertThat(actual.answer()).isEqualTo("Đã hủy thao tác.");
        verify(adkShoppingPilot).cancel(cancelRequest);
    }

    @Test
    void sessionAffinityRoutesToShoppingPilotWhenOwned() {
        UUID sessionId = UUID.randomUUID();
        AiChatRequest request = new AiChatRequest(
                AiChatRequest.ContextType.GENERAL, null, "Thêm đậu hũ vào danh sách", List.of(),
                null, sessionId, null, null, null, null, null, null);

        when(adkShoppingPilot.ownsSession(sessionId, "user:7")).thenReturn(true);
        AiChatResponse expected = new AiChatResponse(AiChatRequest.ContextType.GENERAL, null,
                "Đã thêm vào danh sách mua sắm.", List.of());
        when(adkShoppingPilot.execute(request)).thenReturn(expected);

        AiChatResponse actual = service.answer(request);

        assertThat(actual.answer()).isEqualTo("Đã thêm vào danh sách mua sắm.");
        verify(adkShoppingPilot).execute(request);
        verify(adkEngine, never()).execute(any());
    }

    @Test
    void throwsProviderUnavailableWhenAdkEngineIsNull() {
        AiChatService serviceWithoutEngine = new AiChatService(users, null, null);
        AiChatRequest request = new AiChatRequest(AiChatRequest.ContextType.GENERAL, null,
                "Cách nấu canh chay?", List.of());

        assertThatThrownBy(() -> serviceWithoutEngine.answer(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AI_PROVIDER_UNAVAILABLE);
    }

    @Test
    void deleteSessionDelegatesToEngineOwningSession() {
        UUID sessionId = UUID.randomUUID();
        when(adkEngine.ownsSession(sessionId, "user:7")).thenReturn(true);

        service.deleteSession(sessionId, null);

        verify(adkEngine).deleteSession(sessionId, null);
        verify(adkShoppingPilot, never()).deleteSession(any(), any());
    }

    @Test
    void deleteSessionDelegatesToPilotWhenOwnedByPilot() {
        UUID sessionId = UUID.randomUUID();
        when(adkEngine.ownsSession(sessionId, "user:7")).thenReturn(false);
        when(adkShoppingPilot.ownsSession(sessionId, "user:7")).thenReturn(true);

        service.deleteSession(sessionId, null);

        verify(adkShoppingPilot).deleteSession(sessionId, null);
    }
}
