package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.*;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import tech.mamxanh.MutableClock;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

class GuestChatRateLimiterTest {
    private final MutableClock clock = new MutableClock();
    private final GuestChatRateLimiter limiter = new GuestChatRateLimiter(clock);

    @Test void cookieRotationCannotBypassPeerIpLimitAndRetryAfterIsAccurate() {
        for (int i = 0; i < 10; i++) limiter.check(UUID.randomUUID().toString(), "127.0.0.1");
        clock.advance(Duration.ofSeconds(15));
        assertThatThrownBy(() -> limiter.check(UUID.randomUUID().toString(), "127.0.0.1"))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.errorCode()).isEqualTo(ErrorCode.AI_GUEST_RATE_LIMITED);
                    assertThat(ex.retryAfter()).isEqualTo(Duration.ofSeconds(45));
                });
        clock.advance(Duration.ofSeconds(45));
        assertThatCode(() -> limiter.check(UUID.randomUUID().toString(), "127.0.0.1")).doesNotThrowAnyException();
    }

    @Test void changingIpCannotBypassCookieLimitAndUnrelatedGuestsAreIndependent() {
        String cookie = UUID.randomUUID().toString();
        for (int i = 0; i < 10; i++) limiter.check(cookie, "10.0.0." + i);
        assertThatThrownBy(() -> limiter.check(cookie.toUpperCase(), "10.0.0.90"))
                .isInstanceOf(AppException.class);
        assertThatCode(() -> limiter.check(UUID.randomUUID().toString(), "10.0.0.90")).doesNotThrowAnyException();
    }

    @Test void missingOrMalformedCookieNeverCreatesAnAllowance() {
        for (String invalid : new String[]{null, "", "not-a-uuid"}) {
            assertThatThrownBy(() -> limiter.check(invalid, null)).isInstanceOfSatisfying(AppException.class,
                    ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        }
    }

    @Test void concurrentRequestsCannotOverspendTheAllowance() throws Exception {
        AtomicInteger accepted = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tasks = java.util.stream.IntStream.range(0, 30).<java.util.concurrent.Callable<Void>>mapToObj(i -> () -> {
                try { limiter.check(UUID.randomUUID().toString(), "shared-ip"); accepted.incrementAndGet(); }
                catch (AppException ex) { assertThat(ex.errorCode()).isEqualTo(ErrorCode.AI_GUEST_RATE_LIMITED); }
                return null;
            }).toList();
            for (var result : executor.invokeAll(tasks)) result.get();
        }
        assertThat(accepted.get()).isEqualTo(10);
    }
}
