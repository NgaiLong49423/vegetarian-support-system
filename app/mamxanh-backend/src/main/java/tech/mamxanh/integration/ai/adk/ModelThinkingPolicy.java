package tech.mamxanh.integration.ai.adk;

import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.ThinkingLevel;
import tech.mamxanh.recipe.dto.request.AiChatRequest.ExecutionMode;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Technical policy resolving thinking configurations across Google Gemini models.
 * Ensures verified Gemini 3 models apply stable LOW thinking level while preserving
 * safe fallback to UNSPECIFIED upon transient capability mismatches.
 */
public final class ModelThinkingPolicy {

    private static final Set<String> GEMINI_3_MODELS = Set.of(
            "gemini-3.8-flash",
            "gemini-3.5-flash-lite"
    );

    private ModelThinkingPolicy() {
    }

    /**
     * Resolves the recommended {@link ThinkingConfig} for a target model and execution mode.
     *
     * @param modelId       the model identifier (e.g. gemini-3.8-flash)
     * @param executionMode the current AI execution mode (CHATBOT, AGENT, or legacy modes)
     * @return an {@link Optional} containing the configured {@link ThinkingConfig}
     */
    public static Optional<ThinkingConfig> resolveConfig(String modelId, ExecutionMode executionMode) {
        if (modelId == null || modelId.isBlank()) {
            return Optional.of(defaultConfig());
        }

        String normalized = modelId.strip().toLowerCase(Locale.ROOT);
        if (!GEMINI_3_MODELS.contains(normalized)) {
            return Optional.of(defaultConfig());
        }

        // For gemini-3.8-flash and gemini-3.5-flash-lite:
        // Use LOW thinking level for predictable low latency across both Chatbot and Agent modes.
        return Optional.of(ThinkingConfig.builder()
                .thinkingLevel(ThinkingLevel.Known.LOW)
                .build());
    }

    /**
     * Fallback configuration when a model rejects explicit thinking levels.
     */
    public static ThinkingConfig fallbackConfig() {
        return defaultConfig();
    }

    private static ThinkingConfig defaultConfig() {
        return ThinkingConfig.builder()
                .thinkingLevel(ThinkingLevel.Known.THINKING_LEVEL_UNSPECIFIED)
                .build();
    }
}
