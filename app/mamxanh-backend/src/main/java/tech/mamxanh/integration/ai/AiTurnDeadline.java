package tech.mamxanh.integration.ai;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import io.reactivex.rxjava3.core.Flowable;

/** Monotonic absolute deadline shared by every model call in one chat turn. */
public final class AiTurnDeadline {
    public static final Duration MAX_TURN = Duration.ofSeconds(60);
    public static final Duration MAX_MODEL_CALL = Duration.ofSeconds(30);
    public static final Duration OPERATIONAL_MODEL_CALL = Duration.ofSeconds(20);
    public static final Duration OPERATIONAL_CLASSIFIER_CALL = Duration.ofSeconds(10);
    public static final Duration OPERATIONAL_TURN = Duration.ofSeconds(45);
    public static final Duration HTTP_TRANSPORT_TIMEOUT = MAX_MODEL_CALL;
    public static final Duration SSE_TIMEOUT = Duration.ofSeconds(65);

    private final long deadlineNanos;

    private AiTurnDeadline(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative() || timeout.compareTo(MAX_TURN) > 0) {
            throw new IllegalArgumentException("Turn timeout must be within the 60 second hard cap");
        }
        long now = System.nanoTime();
        long delta = timeout.toNanos();
        this.deadlineNanos = now + delta;
    }

    public static AiTurnDeadline after(Duration timeout) {
        return new AiTurnDeadline(timeout);
    }

    public Duration remaining() {
        long nanos = deadlineNanos - System.nanoTime();
        return nanos <= 0 ? Duration.ZERO : Duration.ofNanos(nanos);
    }

    public Duration modelCallBudget(Duration operationalTimeout) {
        if (operationalTimeout == null || operationalTimeout.isZero() || operationalTimeout.isNegative()
                || operationalTimeout.compareTo(MAX_MODEL_CALL) > 0) {
            throw new IllegalArgumentException("Model call timeout must be within the 30 second hard cap");
        }
        Duration remaining = remaining();
        if (remaining.isZero()) throw new IllegalStateException("Turn deadline exceeded");
        return remaining.compareTo(operationalTimeout) < 0 ? remaining : operationalTimeout;
    }

    public long remainingNanos() {
        return remaining().toNanos();
    }

    public long remainingMillisCeiling() {
        long nanos = remainingNanos();
        return nanos <= 0 ? 0 : TimeUnit.NANOSECONDS.toMillis(nanos - 1) + 1;
    }

    /** Applies one absolute timer; source emissions never restart or extend it. */
    public <T> Flowable<T> enforce(Flowable<T> source, String message) {
        long nanos = remainingNanos();
        if (nanos <= 0) return Flowable.error(new java.util.concurrent.TimeoutException(message));
        return source.takeUntil(Flowable.timer(nanos, TimeUnit.NANOSECONDS)
                .flatMap(ignored -> Flowable.error(new java.util.concurrent.TimeoutException(message))));
    }
}
