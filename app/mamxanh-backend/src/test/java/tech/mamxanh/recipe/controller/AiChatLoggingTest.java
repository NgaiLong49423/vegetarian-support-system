package tech.mamxanh.recipe.controller;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
import tech.mamxanh.recipe.dto.request.AiChatRequest;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ContextType;
import tech.mamxanh.recipe.dto.request.AiChatRequest.Speaker;
import tech.mamxanh.recipe.dto.response.AiChatResponse;
import tools.jackson.databind.json.JsonMapper;

class AiChatLoggingTest {
    @Test void diagnosticRepresentationsHideContentWhileJsonContractPreservesIt() {
        var turn = new AiChatRequest.Turn(Speaker.USER,"private-history-marker");
        var request = new AiChatRequest(ContextType.GENERAL,null,"private-question-marker",List.of(turn));
        var response = new AiChatResponse(ContextType.GENERAL,null,"private-answer-marker",List.of());
        assertThat(request.toString()).doesNotContain(request.question(),turn.content());
        assertThat(turn.toString()).doesNotContain(turn.content());
        assertThat(response.toString()).doesNotContain(response.answer());
        var json = new JsonMapper();
        assertThat(json.writeValueAsString(request)).contains(request.question(),turn.content());
        assertThat(json.writeValueAsString(response)).contains(response.answer());
    }
}
