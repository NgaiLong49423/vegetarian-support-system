package tech.mamxanh.integration.ai.adk;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.BaseLlmConnection;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponseUsageMetadata;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdkSingleStepTaskRunnerTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC);

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void executeTaskRunsThroughAdkRuntimeAndReturnsStructuredOutput() {
        var accounting = new TestPolicy();
        var model = new TestModel(req -> textResponse("{\"recipeIds\":[10,20,30]}"));
        var gateway = new AdkModelGateway(model, accounting, CLOCK);
        var runner = new AdkSingleStepTaskRunner(gateway, CLOCK);
        Authentication caller = userAuth("101", "ROLE_CUSTOMER");

        String result = runner.executeTask(caller, "recommend_recipes", "You are a recipe recommender",
                "Suggest 3 vegetarian dishes", null, 512, Duration.ofSeconds(5));

        assertThat(result).isEqualTo("{\"recipeIds\":[10,20,30]}");
        assertThat(model.requests).hasSize(1);
        assertThat(accounting.beforeCallInvocations.get()).isEqualTo(1);
        assertThat(accounting.onUsableResponseInvocations.get()).isEqualTo(1);
    }

    @Test
    void executeTaskRejectsOversizedContextBeforeReachingModel() {
        var accounting = new TestPolicy();
        var model = new TestModel(req -> textResponse("{}"));
        var gateway = new AdkModelGateway(model, accounting, CLOCK);
        var runner = new AdkSingleStepTaskRunner(gateway, CLOCK);
        Authentication caller = userAuth("101", "ROLE_CUSTOMER");

        String hugePrompt = "a".repeat(70_000);

        assertThatThrownBy(() -> runner.executeTask(caller, "huge_task", "Instruction", hugePrompt,
                null, 512, Duration.ofSeconds(5)))
                .isInstanceOf(Exception.class);

        assertThat(model.requests).isEmpty();
        assertThat(accounting.onUsableResponseInvocations.get()).isZero();
    }

    private static Authentication userAuth(String userId, String role) {
        return new UsernamePasswordAuthenticationToken(userId, "token",
                List.of(new SimpleGrantedAuthority(role)));
    }

    private static LlmResponse textResponse(String json) {
        return LlmResponse.builder()
                .content(Content.builder().role("model").parts(List.of(Part.fromText(json))).build())
                .usageMetadata(GenerateContentResponseUsageMetadata.builder()
                        .promptTokenCount(50)
                        .candidatesTokenCount(25)
                        .totalTokenCount(75)
                        .build())
                .build();
    }

    private static final class TestModel extends BaseLlm {
        final List<LlmRequest> requests = new CopyOnWriteArrayList<>();
        final Function<LlmRequest, LlmResponse> handler;

        TestModel(Function<LlmRequest, LlmResponse> handler) {
            super("gemini-3.8-flash");
            this.handler = handler;
        }

        @Override
        public Flowable<LlmResponse> generateContent(LlmRequest request, boolean stream) {
            requests.add(request);
            return Flowable.just(handler.apply(request));
        }

        @Override
        public BaseLlmConnection connect(LlmRequest request) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TestPolicy implements AdkModelGateway.Policy {
        final AtomicInteger beforeCallInvocations = new AtomicInteger();
        final AtomicInteger onUsableResponseInvocations = new AtomicInteger();

        @Override
        public Object beforeCall(AdkModelGateway.Call call) {
            beforeCallInvocations.incrementAndGet();
            return "context";
        }

        @Override
        public void onUsableResponse(AdkModelGateway.Call call, Object context, AdkModelGateway.Usage usage) {
            onUsableResponseInvocations.incrementAndGet();
        }
    }
}
