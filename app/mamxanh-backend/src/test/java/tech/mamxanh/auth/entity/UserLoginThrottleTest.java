package tech.mamxanh.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** NFR-07 counter rules on {@code USER} (AC-03.7–AC-03.9), independent of the database. */
class UserLoginThrottleTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 1, 8, 0);
    private static final Duration BLOCK = Duration.ofMinutes(10);

    private final User user = User.registerWithPassword("an@example.com", "$2a$12$hash", "Nguyễn An", T0);

    @Test
    void fourWrongPasswordsCountWithoutBlocking() {
        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThat(user.recordFailedLogin(T0.plusSeconds(attempt), 5, BLOCK)).isFalse();
        }

        assertThat(user.getFailedLoginAttempts()).isEqualTo(4);
        assertThat(user.getLoginBlockedUntil()).isNull();
        assertThat(user.isLoginBlocked(T0.plusMinutes(1))).isFalse();
    }

    @Test
    void fifthWrongPasswordBlocksForTenMinutesWithoutLockingTheAccount() {
        for (int attempt = 1; attempt <= 4; attempt++) {
            user.recordFailedLogin(T0, 5, BLOCK);
        }

        assertThat(user.recordFailedLogin(T0, 5, BLOCK)).isTrue();

        assertThat(user.getLoginBlockedUntil()).isEqualTo(T0.plusMinutes(10));
        assertThat(user.isLoginBlocked(T0.plusMinutes(10).minusNanos(1))).isTrue();
        assertThat(user.isLoginBlocked(T0.plusMinutes(10))).isFalse();
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void aWrongPasswordAfterTheBlockExpiresStartsANewCount() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            user.recordFailedLogin(T0, 5, BLOCK);
        }

        assertThat(user.recordFailedLogin(T0.plusMinutes(10), 5, BLOCK)).isFalse();

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(user.getLoginBlockedUntil()).isNull();
    }

    @Test
    void successfulLoginClearsTheCounterAndUpdatesTheTimestamp() {
        user.recordFailedLogin(T0, 5, BLOCK);
        user.recordFailedLogin(T0, 5, BLOCK);

        user.recordSuccessfulLogin(T0.plusMinutes(1));

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLoginBlockedUntil()).isNull();
        assertThat(user.getUpdatedAt()).isEqualTo(T0.plusMinutes(1));
    }

    @Test
    void successfulLoginWithoutPreviousFailuresDoesNotTouchTheRow() {
        user.recordSuccessfulLogin(T0.plusMinutes(1));

        assertThat(user.getUpdatedAt()).isEqualTo(T0);
    }
}
