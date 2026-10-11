package tech.mamxanh.integration.ai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class GeminiAiClientTest {
    @Test
    void reportsMissingConfigurationWithoutMakingAProviderRequest() {
        try (var client = new GeminiAiClient("  ")) {
            assertThatThrownBy(() -> client.generateRecipeText("prompt", List.of(1L)))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class)
                    .hasMessage("Gemini is not configured");
        }
    }

    @Test
    void treatsNullApiKeyAsMissingConfiguration() {
        try (var client = new GeminiAiClient(null)) {
            assertThatThrownBy(() -> client.generateRecipeText("prompt", List.of()))
                    .isInstanceOf(GeminiAiClient.AiProviderException.class);
        }
    }
}
