package tech.mamxanh.integration.ai.adk;

import com.google.adk.agents.LlmAgent;
import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponseUsageMetadata;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import tech.mamxanh.integration.ai.AiTokenUsage;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Executes a single-step generation/classification task via the official Google ADK Java Runtime.
 * Encapsulates LlmAgent + InMemoryRunner lifecycle, respecting ADK callback hooks, byte budget,
 * and timeout boundaries without the overhead of multi-turn tool loops.
 */
public final class AdkSingleStepTaskRunner {
    private static final Logger LOG = LoggerFactory.getLogger(AdkSingleStepTaskRunner.class);
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private final AdkModelGateway gateway;
    private final Clock clock;
    private final Function<String, AdkModelGateway> gatewayFactory;

    public record SingleStepResult(String text, AiTokenUsage usage) {}

    public AdkSingleStepTaskRunner(AdkModelGateway gateway, Clock clock) {
        this(gateway, clock, modelId -> gateway);
    }

    public AdkSingleStepTaskRunner(AdkModelGateway gateway, Clock clock,
                                  Function<String, AdkModelGateway> gatewayFactory) {
        this.gateway = Objects.requireNonNull(gateway, "gateway");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.gatewayFactory = gatewayFactory != null ? gatewayFactory : (modelId -> gateway);
    }

    public String executeTask(Authentication caller, String taskName, String instruction, String promptText,
                              Schema responseSchema, int maxOutputTokens, Duration timeout) {
        return executeTaskWithResult(caller, null, taskName, instruction, promptText, responseSchema, maxOutputTokens, timeout).text();
    }

    public SingleStepResult executeTaskWithResult(Authentication caller, String modelId, String taskName, String instruction,
                                                 String promptText, Schema responseSchema, int maxOutputTokens, Duration timeout) {
        Objects.requireNonNull(instruction, "instruction");
        Objects.requireNonNull(promptText, "promptText");
        Duration effectiveTimeout = timeout != null ? timeout : DEFAULT_TIMEOUT;

        if (!"false".equalsIgnoreCase(System.getenv("ADK_CAPTURE_MESSAGE_CONTENT_IN_SPANS"))) {
            throw new IllegalStateException("Disable ADK message-content tracing before constructing the runner");
        }

        AdkModelGateway targetGateway = (modelId != null && !modelId.isBlank())
                ? gatewayFactory.apply(modelId)
                : gateway;

        var configBuilder = GenerateContentConfig.builder()
                .maxOutputTokens(maxOutputTokens > 0 ? maxOutputTokens : 1024)
                .candidateCount(1);
        if (responseSchema != null) {
            configBuilder.responseMimeType("application/json").responseSchema(responseSchema);
        } else {
            configBuilder.responseMimeType("application/json");
        }

        AtomicReference<String> activeRun = new AtomicReference<>();
        AtomicBoolean halted = new AtomicBoolean();

        var agent = LlmAgent.builder()
                .name(taskName != null ? taskName : "adk_single_step_task")
                .description("Single-step task execution via ADK Runtime")
                .instruction(instruction)
                .model(targetGateway.forMember(caller, () -> activeRun.get() != null && !halted.get()))
                .generateContentConfig(configBuilder.build())
                .beforeModelCallback((context, request) -> {
                    if (request.build().contents().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 64_000) {
                        return io.reactivex.rxjava3.core.Maybe.error(new IllegalStateException("Model context exceeds byte budget"));
                    }
                    return io.reactivex.rxjava3.core.Maybe.empty();
                })
                .build();

        String appName = "mamxanh_task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        var runner = new InMemoryRunner(agent, appName);
        String sessionId = null;
        String runToken = UUID.randomUUID().toString();

        try {
            String callerName = caller != null ? caller.getName() : "system";
            sessionId = runner.sessionService().createSession(runner.appName(), callerName).blockingGet().id();
            activeRun.set(runToken);
            halted.set(false);

            var runConfig = RunConfig.builder()
                    .maxLlmCalls(1)
                    .customMetadata(Map.of("taskId", runToken))
                    .build();

            Content message = Content.fromParts(Part.fromText(promptText));

            List<Event> events = runner.runAsync(callerName, sessionId, message, runConfig)
                    .takeUntil(Flowable.timer(effectiveTimeout.toMillis(), TimeUnit.MILLISECONDS)
                            .flatMap(ignored -> Flowable.error(new java.util.concurrent.TimeoutException("Task execution deadline exceeded"))))
                    .doOnCancel(() -> { if (runToken.equals(activeRun.get())) halted.set(true); })
                    .doOnError(failure -> { if (runToken.equals(activeRun.get())) halted.set(true); })
                    .doFinally(() -> activeRun.compareAndSet(runToken, null))
                    .toList()
                    .blockingGet();

            String text = events.stream()
                    .filter(Event::finalResponse)
                    .map(Event::stringifyContent)
                    .filter(Objects::nonNull)
                    .reduce("", (a, b) -> a + b)
                    .strip();

            GenerateContentResponseUsageMetadata usageMetadata = events.stream()
                    .map(Event::usageMetadata)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .orElse(null);

            AiTokenUsage tokenUsage;
            if (usageMetadata != null) {
                Integer promptTokens = usageMetadata.promptTokenCount().orElse(null);
                Integer candidatesTokens = usageMetadata.candidatesTokenCount().orElse(null);
                Integer totalTokens = usageMetadata.totalTokenCount().orElse(null);
                Integer thinkingTokens = usageMetadata.thoughtsTokenCount().orElse(null);
                Integer cachedTokens = usageMetadata.cachedContentTokenCount().orElse(null);
                Integer toolUseTokens = usageMetadata.toolUsePromptTokenCount().orElse(null);
                tokenUsage = new AiTokenUsage(targetGateway.model(), "STANDARD", promptTokens, candidatesTokens, totalTokens,
                        thinkingTokens, cachedTokens, toolUseTokens);
            } else {
                tokenUsage = new AiTokenUsage(targetGateway.model(), "STANDARD", null, null, null, null, null, null);
            }

            return new SingleStepResult(text, tokenUsage);
        } finally {
            activeRun.compareAndSet(runToken, null);
            if (sessionId != null) {
                try {
                    String callerName = caller != null ? caller.getName() : "system";
                    runner.sessionService().deleteSession(runner.appName(), callerName, sessionId).blockingAwait();
                } catch (Exception ignored) { }
            }
            try {
                runner.close().blockingAwait();
            } catch (Exception ignored) { }
        }
    }

    public String model() {
        return gateway.model();
    }

    public AdkModelGateway gateway() {
        return gateway;
    }
}
