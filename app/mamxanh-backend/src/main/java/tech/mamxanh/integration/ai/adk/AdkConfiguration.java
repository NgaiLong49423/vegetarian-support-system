package tech.mamxanh.integration.ai.adk;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.BaseLlmConnection;
import com.google.adk.models.Gemini;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.HttpRetryOptions;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException;
import tech.mamxanh.subscription.service.AiTokenQuotaService;

import java.time.Clock;

@Configuration
public class AdkConfiguration {

    @Bean
    public AdkModelGateway adkModelGateway(
            @Value("${GEMINI_API_KEY:}") String configuredApiKey,
            CurrentUserService users,
            AiTokenQuotaService aiQuota,
            ApplicationEventPublisher events,
            Clock clock) {
        String apiKey = (configuredApiKey != null && !configuredApiKey.isBlank())
                ? configuredApiKey
                : System.getenv("GEMINI_API_KEY");
        BaseLlm model = createLlm(apiKey, AiTokenUsage.MODEL);
        MemberAccountingPolicy policy = new MemberAccountingPolicy(users, aiQuota, events);
        return new AdkModelGateway(model, policy, clock);
    }

    @Bean
    public AdkSingleStepTaskRunner adkSingleStepTaskRunner(
            @Value("${GEMINI_API_KEY:}") String configuredApiKey,
            AdkModelGateway defaultGateway,
            CurrentUserService users,
            AiTokenQuotaService aiQuota,
            ApplicationEventPublisher events,
            Clock clock) {
        String apiKey = (configuredApiKey != null && !configuredApiKey.isBlank())
                ? configuredApiKey
                : System.getenv("GEMINI_API_KEY");
        AdkModelGateway.Policy policy = new MemberAccountingPolicy(users, aiQuota, events).preflightOnly();
        AdkModelGateway singleStepGateway = defaultGateway.withPolicy(policy);
        return new AdkSingleStepTaskRunner(singleStepGateway, clock, modelId -> {
            if (modelId == null || modelId.isBlank() || modelId.equals(defaultGateway.model())) {
                return singleStepGateway;
            }
            BaseLlm model = createLlm(apiKey, modelId);
            return new AdkModelGateway(model, policy, clock);
        });
    }

    private static BaseLlm createLlm(String apiKey, String modelId) {
        if (apiKey == null || apiKey.isBlank()) {
            return new UnconfiguredLlm(modelId);
        }
        Client client = Client.builder()
                .apiKey(apiKey.strip())
                .httpOptions(HttpOptions.builder()
                        .timeout(10_000)
                        .retryOptions(HttpRetryOptions.builder().attempts(1).build())
                        .build())
                .build();
        return new Gemini(modelId, client);
    }

    private static final class UnconfiguredLlm extends BaseLlm {
        UnconfiguredLlm(String model) {
            super(model);
        }

        @Override
        public Flowable<LlmResponse> generateContent(LlmRequest request, boolean stream) {
            return Flowable.error(new AiProviderException("Gemini is not configured"));
        }

        @Override
        public BaseLlmConnection connect(LlmRequest request) {
            throw new UnsupportedOperationException();
        }
    }
}
