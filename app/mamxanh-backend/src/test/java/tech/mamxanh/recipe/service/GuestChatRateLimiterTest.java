package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import tech.mamxanh.MutableClock;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

class GuestChatRateLimiterTest {
    private final MutableClock clock = new MutableClock();
    private final GuestChatRateLimiter limiter = new GuestChatRateLimiter(clock);
    private final String id = UUID.randomUUID().toString();

    @Test
    void rollingWindowExpiresEachRequestWithoutDailyQuotaOrMidnightReset() {
        for (int i = 0; i < 5; i++) limiter.acquire(id, "peer");
        clock.advance(Duration.ofSeconds(30));
        for (int i = 0; i < 5; i++) limiter.acquire(id, "peer");
        assertThatThrownBy(() -> limiter.acquire(id, "peer"))
                .isInstanceOfSatisfying(AppException.class, error -> {
                    assertThat(error.errorCode()).isEqualTo(ErrorCode.GUEST_CHAT_RATE_LIMITED);
                    assertThat(error.retryAfter()).isEqualTo(Duration.ofSeconds(30));
                });
        clock.advance(Duration.ofSeconds(30));
        for (int i = 0; i < 5; i++) limiter.acquire(id, "peer");
        assertThatThrownBy(() -> limiter.acquire(id, "peer")).isInstanceOf(AppException.class);
        clock.advance(Duration.ofDays(1));
        assertThatCode(() -> limiter.acquire(id, "peer")).doesNotThrowAnyException();
    }

    @Test
    void cookieIdentitySurvivesIpChangesAndCaseChanges() {
        for (int i = 0; i < 10; i++) limiter.acquire(id, "peer-" + i);
        assertThatThrownBy(() -> limiter.acquire(id.toUpperCase(), "another-peer"))
                .isInstanceOf(AppException.class);
    }

    @Test
    void rotatingCookiesIsBoundedByTheIndependentCoarseIpWindow() {
        for (int i = 0; i < 60; i++) limiter.acquire(UUID.randomUUID().toString(), "shared-peer");
        assertThatThrownBy(() -> limiter.acquire(UUID.randomUUID().toString(), "shared-peer"))
                .isInstanceOf(AppException.class);
        assertThatCode(() -> limiter.acquire(UUID.randomUUID().toString(), "different-peer"))
                .doesNotThrowAnyException();
    }

    @Test
    void missingCookieSharesAnIpFallbackAndMalformedIdentityIsRejected() {
        for (int i = 0; i < 10; i++) limiter.acquire(null, "peer");
        assertThatThrownBy(() -> limiter.acquire(null, "peer")).isInstanceOf(AppException.class);
        for (String invalid : new String[]{"", "not-a-uuid", "x".repeat(1000)}) {
            assertThatThrownBy(() -> limiter.acquire(invalid, "other"))
                    .isInstanceOfSatisfying(AppException.class,
                            error -> assertThat(error.errorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        }
    }

    @Test
    void concurrentRequestsCannotOverrunTheIdentityWindow() throws Exception {
        try (var executor = Executors.newFixedThreadPool(12)) {
            var calls = IntStream.range(0, 40).mapToObj(index -> (Callable<Boolean>) () -> {
                try { limiter.acquire(id, "peer"); return true; }
                catch (AppException error) { return false; }
            }).toList();
            int accepted = 0;
            for (var result : executor.invokeAll(calls)) if (result.get()) accepted++;
            assertThat(accepted).isEqualTo(10);
        }
    }

    @Test
    void fullStorageFailsClosedAndExpiredEntriesAreReclaimed() {
        for (int i = 0; i < 5000; i++) limiter.acquire(UUID.randomUUID().toString(), "peer-" + i);
        assertThatThrownBy(() -> limiter.acquire(id, "new-peer")).isInstanceOf(AppException.class);
        clock.advance(Duration.ofMinutes(1));
        assertThatCode(() -> limiter.acquire(id, "new-peer")).doesNotThrowAnyException();
    }
}
