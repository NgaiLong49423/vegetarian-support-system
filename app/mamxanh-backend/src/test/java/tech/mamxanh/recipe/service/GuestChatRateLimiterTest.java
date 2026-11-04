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
    private final String id = UUID.randomUUID().toString();

    @Test
    void dailyLimitEnforcesThreeCallsPerDayAndResetsOnNewDay() {
        for (int i = 0; i < 3; i++) limiter.check(id, "peer");
        assertThatThrownBy(() -> limiter.check(id, "peer"))
                .isInstanceOfSatisfying(AppException.class, error -> {
                    assertThat(error.errorCode()).isEqualTo(ErrorCode.AI_GUEST_RATE_LIMITED);
                    assertThat(error.retryAfter()).isNotNull();
                    assertThat(error.retryAfter().getSeconds()).isGreaterThan(0);
                });

        // Still within same day
        clock.advance(Duration.ofHours(2));
        assertThatThrownBy(() -> limiter.check(id, "peer")).isInstanceOf(AppException.class);

        // Advance past midnight to next day
        clock.advance(Duration.ofDays(1));
        for (int i = 0; i < 3; i++) limiter.check(id, "peer");
        assertThatThrownBy(() -> limiter.check(id, "peer")).isInstanceOf(AppException.class);
    }

    @Test
    void cookieIdentitySurvivesIpChangesAndCaseChanges() {
        for (int i = 0; i < 3; i++) limiter.check(id, "peer-" + i);
        assertThatThrownBy(() -> limiter.check(id.toUpperCase(), "another-peer"))
                .isInstanceOf(AppException.class);
    }

    @Test
    void rotatingCookiesIsBoundedByTheIndependentCoarseIpWindow() {
        for (int i = 0; i < 60; i++) limiter.check(UUID.randomUUID().toString(), "shared-peer");
        assertThatThrownBy(() -> limiter.check(UUID.randomUUID().toString(), "shared-peer"))
                .isInstanceOf(AppException.class);
        assertThatCode(() -> limiter.check(UUID.randomUUID().toString(), "different-peer"))
                .doesNotThrowAnyException();
    }

    @Test
    void missingOrMalformedCookieNeverCreatesAnAllowance() {
        for (String invalid : new String[]{null, "", "not-a-uuid", "x".repeat(1000)}) {
            assertThatThrownBy(() -> limiter.check(invalid, "peer"))
                    .isInstanceOfSatisfying(AppException.class,
                            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        }
    }

    @Test
    void concurrentRequestsCannotOverrunTheIdentityWindow() throws Exception {
        AtomicInteger accepted = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tasks = java.util.stream.IntStream.range(0, 30).<java.util.concurrent.Callable<Void>>mapToObj(i -> () -> {
                try {
                    limiter.check(id, "peer");
                    accepted.incrementAndGet();
                } catch (AppException ex) {
                    assertThat(ex.errorCode()).isEqualTo(ErrorCode.AI_GUEST_RATE_LIMITED);
                }
                return null;
            }).toList();
            for (var result : executor.invokeAll(tasks)) result.get();
        }
        assertThat(accepted.get()).isEqualTo(3);
    }

    @Test
    void fullStorageFailsClosedAndExpiredEntriesAreReclaimed() {
        for (int i = 0; i < 5000; i++) limiter.check(UUID.randomUUID().toString(), "peer-" + i);
        assertThatThrownBy(() -> limiter.check(id, "new-peer")).isInstanceOf(AppException.class);
        clock.advance(Duration.ofDays(1));
        assertThatCode(() -> limiter.check(id, "new-peer")).doesNotThrowAnyException();
    }
}
