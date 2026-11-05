package tech.mamxanh.recipe.dto.response;

import java.util.Map;

/**
 * Event payload for Server-Sent Events (SSE) AI assistant streaming.
 */
public record AiChatStreamEvent(String type, Object data) {

    public static AiChatStreamEvent step(AiChatResponse.AssistantStep step) {
        return new AiChatStreamEvent("step", step);
    }

    public static AiChatStreamEvent confirmation(AiChatResponse.PendingConfirmation confirmation) {
        return new AiChatStreamEvent("confirmation", confirmation);
    }

    public static AiChatStreamEvent delta(String text) {
        return new AiChatStreamEvent("delta", Map.of("delta", text));
    }

    public static AiChatStreamEvent complete(AiChatResponse response) {
        return new AiChatStreamEvent("complete", response);
    }

    public static AiChatStreamEvent error(String message, String status) {
        return new AiChatStreamEvent("error", Map.of("message", message, "status", status));
    }
}
