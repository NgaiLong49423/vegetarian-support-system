package tech.mamxanh.integration.ai.adk;

import com.google.genai.types.ThinkingLevel;
import org.junit.jupiter.api.Test;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ExecutionMode;

import static org.assertj.core.api.Assertions.assertThat;

class ModelThinkingPolicyTest {

    @Test
    void resolvesLowThinkingLevelForGeminiFlashModels() {
        var flashConfig = ModelThinkingPolicy.resolveConfig("gemini-3.8-flash", ExecutionMode.QA);
        assertThat(flashConfig).isPresent();
        assertThat(flashConfig.get().thinkingLevel().orElseThrow().knownEnum())
                .isEqualTo(ThinkingLevel.Known.LOW);

        var liteConfig = ModelThinkingPolicy.resolveConfig("gemini-3.5-flash-lite", ExecutionMode.AUTO);
        assertThat(liteConfig).isPresent();
        assertThat(liteConfig.get().thinkingLevel().orElseThrow().knownEnum())
                .isEqualTo(ThinkingLevel.Known.LOW);
    }

    @Test
    void fallsBackToUnspecifiedForUnrecognizedOrCustomModels() {
        var customConfig = ModelThinkingPolicy.resolveConfig("custom-model-v1", ExecutionMode.QA);
        assertThat(customConfig).isPresent();
        assertThat(customConfig.get().thinkingLevel().orElseThrow().knownEnum())
                .isEqualTo(ThinkingLevel.Known.THINKING_LEVEL_UNSPECIFIED);

        var nullConfig = ModelThinkingPolicy.resolveConfig(null, ExecutionMode.QA);
        assertThat(nullConfig).isPresent();
        assertThat(nullConfig.get().thinkingLevel().orElseThrow().knownEnum())
                .isEqualTo(ThinkingLevel.Known.THINKING_LEVEL_UNSPECIFIED);
    }

    @Test
    void fallbackConfigYieldsUnspecifiedThinkingLevel() {
        var fallback = ModelThinkingPolicy.fallbackConfig();
        assertThat(fallback.thinkingLevel().orElseThrow().knownEnum())
                .isEqualTo(ThinkingLevel.Known.THINKING_LEVEL_UNSPECIFIED);
    }
}
