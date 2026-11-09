package tech.mamxanh.integration.ai.adk;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.BaseLlmConnection;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.genai.errors.ApiException;
import com.google.genai.types.FinishReason;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.AiTurnDeadline;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** G1 model boundary. No bean: production accounting must implement Policy before cutover. */
public final class AdkModelGateway extends BaseLlm {
    private static final Logger LOG = LoggerFactory.getLogger(AdkModelGateway.class);
    public static final Duration CALL_TIMEOUT = AiTurnDeadline.OPERATIONAL_MODEL_CALL;
    private final BaseLlm delegate;
    private final Policy policy;
    private final Clock clock;
    private final Duration timeout;
    private final Authentication authentication;
    private final BooleanSupplier runAllowed;
    private final Supplier<Duration> remainingTurnBudget;
    private final AtomicInteger callSequence = new AtomicInteger();

    public AdkModelGateway(BaseLlm delegate, Policy policy, Clock clock) {
        this(delegate, policy, clock, CALL_TIMEOUT);
    }

    AdkModelGateway(BaseLlm delegate, Policy policy, Clock clock, Duration timeout) {
        this(delegate, policy, clock, timeout, null, () -> true);
    }

    private AdkModelGateway(BaseLlm delegate, Policy policy, Clock clock, Duration timeout,
            Authentication authentication, BooleanSupplier runAllowed) {
        this(delegate, policy, clock, timeout, authentication, runAllowed, () -> timeout);
    }

    private AdkModelGateway(BaseLlm delegate, Policy policy, Clock clock, Duration timeout,
            Authentication authentication, BooleanSupplier runAllowed, Supplier<Duration> remainingTurnBudget) {
        super(delegate.model());
        if (timeout.isNegative() || timeout.isZero() || timeout.compareTo(AiTurnDeadline.MAX_MODEL_CALL) > 0) {
            throw new IllegalArgumentException("Invalid model deadline");
        }
        this.delegate = delegate;
        this.policy = policy;
        this.clock = clock;
        this.timeout = timeout;
        this.authentication = authentication;
        this.runAllowed = runAllowed;
        this.remainingTurnBudget = remainingTurnBudget;
    }

    AdkModelGateway forMember(Authentication owner, BooleanSupplier allowed) {
        return new AdkModelGateway(delegate, policy, clock, timeout, owner, allowed, remainingTurnBudget);
    }

    AdkModelGateway withPolicy(Policy replacement) {
        return new AdkModelGateway(delegate, replacement, clock, timeout, authentication, runAllowed, remainingTurnBudget);
    }

    public AdkModelGateway withRemainingTurnBudget(Supplier<Duration> remainingBudget) {
        return new AdkModelGateway(delegate, policy, clock, timeout, authentication, runAllowed, remainingBudget);
    }

    public AdkModelGateway withRunAllowed(BooleanSupplier allowed) {
        return new AdkModelGateway(delegate, policy, clock, timeout, authentication, allowed, remainingTurnBudget);
    }

    @Override
    public Flowable<LlmResponse> generateContent(LlmRequest request, boolean stream) {
        long startedNanos = System.nanoTime();
        Duration remaining = remainingTurnBudget.get();
        if (remaining == null || remaining.isZero() || remaining.isNegative()) {
            return Flowable.error(new java.util.concurrent.TimeoutException("Turn deadline exceeded"));
        }
        Duration callBudget = remaining.compareTo(timeout) < 0 ? remaining : timeout;
        UUID traceId = UUID.randomUUID();
        int ordinal = callSequence.incrementAndGet();
        AtomicInteger attempts = new AtomicInteger();
        AtomicBoolean usageRecorded = new AtomicBoolean();
        AtomicBoolean firstPartialLogged = new AtomicBoolean();
        int contextBytes = request.contents().toString().getBytes(StandardCharsets.UTF_8).length;
        int toolCount = request.tools().size();
        Flowable<LlmResponse> response = Flowable.defer(() -> {
            attempts.incrementAndGet();
            if (!runAllowed.getAsBoolean()) return Flowable.error(new IllegalStateException("Run stopped"));
            if (request.model().isPresent() && !model().equals(request.model().get())) {
                return Flowable.error(new IllegalArgumentException("Model selection mismatch"));
            }
            // Policy captures the quota period at start; it must not reserve user quota or Credit.
            Call call = new Call(UUID.randomUUID(), model(), clock.instant());
            Object accountingContext = asMember(() -> policy.beforeCall(call));
            Flowable<LlmResponse> delegated = Flowable.defer(() -> delegate.generateContent(request, stream));
            if (!stream) {
                delegated = delegated.singleOrError()
                        .onErrorResumeNext(failure -> io.reactivex.rxjava3.core.Single.error(
                                isTransientProviderFailure(failure) ? new RetryableProviderFailure(failure) : failure))
                        .toFlowable();
            }
            return delegated.map(modelResponse -> {
                if (modelResponse.partial().orElse(false)) {
                    if (firstPartialLogged.compareAndSet(false, true)) {
                        LOG.info("ADK model first partial traceId={} model={} elapsedMs={}", traceId, model(), elapsedMillis(startedNanos));
                    }
                    if (modelResponse.errorCode().isPresent()) throw new IllegalStateException("Partial model response has an error");
                    return modelResponse;
                }
                try {
                    validate(request, modelResponse);
                } catch (IllegalArgumentException invalidResponse) {
                    // A provider-declared error response can be transient. Retry one non-streaming
                    // call; never replay a stream after partial output may have reached the user.
                    if (!stream && modelResponse.errorCode().isPresent()) {
                        throw new RetryableProviderFailure(invalidResponse);
                    }
                    throw invalidResponse;
                }
                if (usageRecorded.compareAndSet(false, true)) {
                    asMember(() -> { policy.onUsableResponse(call, accountingContext, usage(modelResponse)); return null; });
                }
                return modelResponse;
            });
        });
        if (!stream) {
            response = response.retry((attempt, failure) -> attempt == 1 && failure instanceof RetryableProviderFailure);
        }
        return AiTurnDeadline.after(callBudget).enforce(response, "Model call deadline exceeded")
                .onErrorResumeNext(failure -> Flowable.error(failure instanceof RetryableProviderFailure
                        ? failure.getCause() : failure))
                .subscribeOn(Schedulers.io())
                .doOnNext(modelResponse -> {
                    if (!modelResponse.partial().orElse(false)) {
                        LOG.info("ADK model call completed traceId={} ordinal={} model={} elapsedMs={} attempts={} contextBytes={} toolCount={}",
                                traceId, ordinal, model(), elapsedMillis(startedNanos), attempts.get(), contextBytes, toolCount);
                    }
                })
                .doOnError(failure -> LOG.warn("ADK model call failed traceId={} ordinal={} model={} elapsedMs={} attempts={} contextBytes={} toolCount={} cause={}",
                        traceId, ordinal, model(), elapsedMillis(startedNanos), attempts.get(), contextBytes, toolCount, failureKind(failure)));
    }

    private static long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    private static String failureKind(Throwable failure) {
        for (int depth = 0; failure != null && depth < 8; depth++, failure = failure.getCause()) {
            if (failure instanceof java.util.concurrent.TimeoutException) return "TIMEOUT";
            if (failure instanceof ApiException api) return "HTTP_" + api.code();
            if (failure instanceof IllegalArgumentException) {
                var frame = failure.getStackTrace().length == 0 ? null : failure.getStackTrace()[0];
                return frame == null ? "ILLEGAL_ARGUMENT" : "ILLEGAL_ARGUMENT_" + frame.getClassName() + "_" + frame.getMethodName() + "_L" + frame.getLineNumber();
            }
        }
        return "OTHER";
    }

    private static boolean isTransientProviderFailure(Throwable failure) {
        for (int depth = 0; failure != null && depth < 8; depth++, failure = failure.getCause()) {
            if (failure instanceof ApiException api) return java.util.List.of(502, 503, 504).contains(api.code());
        }
        return false;
    }

    private static final class RetryableProviderFailure extends RuntimeException {
        private RetryableProviderFailure(Throwable cause) { super("Transient model provider failure", cause); }
    }

    private <T> T asMember(Callable<T> action) throws Exception {
        if (authentication == null) return action.call();
        var previous = SecurityContextHolder.getContext();
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        try { return action.call(); }
        finally { SecurityContextHolder.setContext(previous); }
    }

    private void validate(LlmRequest request, LlmResponse response) {
        String invalidResponse = invalidResponseReason(request, response);
        if (invalidResponse != null) {
            LOG.warn("ADK unusable model response reason={}", invalidResponse);
            throw new IllegalArgumentException("Unusable model response: " + invalidResponse);
        }
        var parts = response.content().orElseThrow().parts().orElseThrow();
        boolean usablePart = false;
        for (var part : parts) {
            if (part.functionCall().isPresent()) {
                var call = part.functionCall().get();
                String name = call.name().orElse("");
                // Zero-argument actions such as create_recipe_post legitimately return an empty args map;
                // the scoped tool performs its own authorization and reads the latest trusted context.
                if (!request.tools().containsKey(name)) {
                    throw new IllegalArgumentException("Invalid tool call");
                }
                usablePart = true;
            } else if (part.text().filter(text -> !text.isBlank()).isEmpty()) {
                // Gemini 3 can attach a standalone thoughtSignature part to a valid final text
                // part. It is protocol metadata, not user-visible answer content.
                if (part.thoughtSignature().isEmpty()) throw new IllegalArgumentException("Unsupported model content");
            } else {
                usablePart = true;
            }
        }
        if (!usablePart) throw new IllegalArgumentException("Model response has no usable content");
    }

    private static String invalidResponseReason(LlmRequest request, LlmResponse response) {
        if (response.errorCode().isPresent()) {
            return "ERROR_CODE_" + response.errorCode().orElseThrow().knownEnum().name();
        }
        if (response.partial().orElse(false)) return "PARTIAL_AS_FINAL";
        if (response.finishReason().filter(reason -> reason.knownEnum() != FinishReason.Known.STOP).isPresent()) {
            return "NON_STOP_FINISH_REASON";
        }
        var parts = response.content().flatMap(content -> content.parts()).orElse(java.util.List.of());
        if (parts.isEmpty()) return "EMPTY_CONTENT";
        boolean usablePart = false;
        for (var part : parts) {
            if (part.functionCall().isPresent()) {
                var call = part.functionCall().get();
                if (!request.tools().containsKey(call.name().orElse(""))) return "INVALID_TOOL_CALL";
                usablePart = true;
            } else if (part.text().filter(text -> !text.isBlank()).isEmpty()) {
                if (part.thoughtSignature().isEmpty()) return "UNSUPPORTED_CONTENT";
            } else {
                usablePart = true;
            }
        }
        return usablePart ? null : "NO_USABLE_CONTENT";
    }

    private Usage usage(LlmResponse response) {
        var metadata = response.usageMetadata().orElse(null);
        if (metadata == null) return new Usage(null, UsageStatus.MISSING);
        var tokens = new AiTokenUsage(model(), "STANDARD", metadata.promptTokenCount().orElse(null),
                metadata.candidatesTokenCount().orElse(null), metadata.totalTokenCount().orElse(null),
                metadata.thoughtsTokenCount().orElse(null), metadata.cachedContentTokenCount().orElse(null),
                metadata.toolUsePromptTokenCount().orElse(null));
        var counts = java.util.Arrays.asList(tokens.promptTokens(), tokens.responseTokens(), tokens.totalTokens(),
                tokens.thinkingTokens(), tokens.cachedTokens(), tokens.toolUsePromptTokens());
        if (counts.stream().allMatch(java.util.Objects::isNull)) return new Usage(tokens, UsageStatus.MISSING);
        boolean invalid = tokens.promptTokens() == null || tokens.responseTokens() == null || tokens.totalTokens() == null
                || counts.stream().filter(java.util.Objects::nonNull).anyMatch(value -> value < 0);
        if (!invalid) {
            long minimum = (long) tokens.promptTokens() + tokens.responseTokens()
                    + (tokens.thinkingTokens() == null ? 0 : tokens.thinkingTokens())
                    + (tokens.toolUsePromptTokens() == null ? 0 : tokens.toolUsePromptTokens());
            invalid = tokens.totalTokens() < minimum;
        }
        return new Usage(tokens, invalid ? UsageStatus.INVALID : UsageStatus.RECORDED);
    }

    @Override
    public BaseLlmConnection connect(LlmRequest request) {
        throw new UnsupportedOperationException("G1 does not enable live audio connections");
    }

    public record Call(UUID id, String model, Instant startedAt) { }
    public enum UsageStatus { RECORDED, MISSING, INVALID }
    public record Usage(AiTokenUsage metadata, UsageStatus status) { }

    /** No payloads are passed to accounting. Implementations must deduplicate by call.id(). */
    public interface Policy {
        Object beforeCall(Call call);
        void onUsableResponse(Call call, Object accountingContext, Usage usage);
    }
}
