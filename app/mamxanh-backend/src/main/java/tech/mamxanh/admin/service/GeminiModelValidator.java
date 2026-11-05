package tech.mamxanh.admin.service;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.GetModelConfig;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tech.mamxanh.integration.ai.AiProviderException;
import tech.mamxanh.integration.ai.AiProviderException.FailureKind;

import java.util.List;

@Component
public class GeminiModelValidator {
    private static final int VALIDATION_TIMEOUT_MS = 10_000;
    private final String configuredApiKey;

    public GeminiModelValidator(@Value("${GEMINI_API_KEY:}") String configuredApiKey) {
        this.configuredApiKey = configuredApiKey;
    }

    public boolean verifyModel(String modelId) {
        String apiKey = (configuredApiKey != null && !configuredApiKey.isBlank())
                ? configuredApiKey.strip()
                : System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("Gemini API Client is not configured", null, FailureKind.CONFIGURATION, null);
        }

        try (Client client = Client.builder()
                .apiKey(apiKey)
                .httpOptions(HttpOptions.builder()
                        .timeout(VALIDATION_TIMEOUT_MS)
                        .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                        .build())
                .build()) {

            var model = client.models.get(modelId, GetModelConfig.builder().build());
            if (model == null) {
                throw AiProviderException.invalidResponse("Gemini did not return model metadata");
            }
            return model.supportedActions().orElse(List.of()).contains("generateContent");
        } catch (ApiException e) {
            throw new AiProviderException("Gemini model validation failed", e, FailureKind.HTTP, e.code());
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Gemini model validation failed", e);
        }
    }
}
