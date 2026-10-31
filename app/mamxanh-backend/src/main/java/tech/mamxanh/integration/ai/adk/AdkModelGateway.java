package tech.mamxanh.integration.ai.adk;

import com.google.adk.models.BaseLlm;
import com.google.adk.models.BaseLlmConnection;
import com.google.adk.models.LlmRequest;
import com.google.adk.models.LlmResponse;
import com.google.genai.errors.ApiException;
import com.google.genai.types.FinishReason;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tech.mamxanh.integration.ai.AiTokenUsage;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** G1 model boundary. No bean: production accounting must implement Policy before cutover. */
public final class AdkModelGateway extends BaseLlm {
    public static final Duration CALL_TIMEOUT = Duration.ofSeconds(10);
    private final BaseLlm delegate;
    private final Policy policy;
    private final Clock clock;
    private final Duration timeout;
    private final Authentication authentication;
    private final BooleanSupplier runAllowed;

    public AdkModelGateway(BaseLlm delegate, Policy policy, Clock clock) {
        this(delegate, policy, clock, CALL_TIMEOUT);
    }

    AdkModelGateway(BaseLlm delegate, Policy policy, Clock clock, Duration timeout) {
        this(delegate, policy, clock, timeout, null, () -> true);
    }

    private AdkModelGateway(BaseLlm delegate, Policy policy, Clock clock, Duration timeout,
            Authentication authentication, BooleanSupplier runAllowed) {
        super(delegate.model());
        if (timeout.isNegative() || timeout.isZero() || timeout.compareTo(CALL_TIMEOUT) > 0) {
            throw new IllegalArgumentException("Invalid model deadline");
        }
        this.delegate = delegate;
        this.policy = policy;
        this.clock = clock;
        this.timeout = timeout;
        this.authentication = authentication;
        this.runAllowed = runAllowed;
    }

    AdkModelGateway forMember(Authentication owner, BooleanSupplier allowed) {
        return new AdkModelGateway(delegate, policy, clock, timeout, owner, allowed);
    }

    @Override
    public Flowable<LlmResponse> generateContent(LlmRequest request, boolean stream) {
        if (stream) return Flowable.error(new UnsupportedOperationException("G1 uses non-streaming calls"));
        return Flowable.defer(() -> {
            if (!runAllowed.getAsBoolean()) return Flowable.error(new IllegalStateException("Run stopped"));
            if (request.model().isPresent() && !model().equals(request.model().get())) {
                return Flowable.error(new IllegalArgumentException("Model selection mismatch"));
            }
            // Policy captures the quota period at start; it must not reserve user quota or Credit.
            Call call = new Call(UUID.randomUUID(), model(), clock.instant());
            Object accountingContext = asMember(() -> policy.beforeCall(call));
            return Flowable.defer(() -> delegate.generateContent(request, false))
                    .singleOrError()
                    .onErrorResumeNext(failure -> io.reactivex.rxjava3.core.Single.error(
                            isTransientProviderFailure(failure) ? new RetryableProviderFailure(failure) : failure))
                    .map(response -> {
                        validate(request, response);
                        asMember(() -> { policy.onUsableResponse(call, accountingContext, usage(response)); return null; });
                        return response;
                    }).toFlowable();
        }).retry((attempt, failure) -> attempt == 1 && failure instanceof RetryableProviderFailure)
                .onErrorResumeNext(failure -> Flowable.error(failure instanceof RetryableProviderFailure
                        ? failure.getCause() : failure))
                .subscribeOn(Schedulers.io()).timeout(timeout.toMillis(), TimeUnit.MILLISECONDS);
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
        if (response.errorCode().isPresent() || response.partial().orElse(false)
                || response.finishReason().filter(reason -> reason.knownEnum() != FinishReason.Known.STOP).isPresent()
                || response.content().flatMap(content -> content.parts()).orElse(java.util.List.of()).isEmpty()) {
            throw new IllegalArgumentException("Unusable model response");
        }
        var parts = response.content().orElseThrow().parts().orElseThrow();
        for (var part : parts) {
            if (part.functionCall().isPresent()) {
                var call = part.functionCall().get();
                String name = call.name().orElse("");
                if (!request.tools().containsKey(name) || call.args().isEmpty()) {
                    throw new IllegalArgumentException("Invalid tool call");
                }
            } else if (part.text().filter(text -> !text.isBlank()).isEmpty()) {
                throw new IllegalArgumentException("Unsupported model content");
            }
        }
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
