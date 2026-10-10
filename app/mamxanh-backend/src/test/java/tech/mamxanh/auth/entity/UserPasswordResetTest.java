package tech.mamxanh.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** FR-03-E (#9): password-reset token and email rate-limit rules on {@code USER} (Q27), without a database. */
class UserPasswordResetTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 10, 10, 8, 0);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);
    private static final Duration TTL = Duration.ofMinutes(15);
    private static final int MAX_PER_HOUR = 5;
    private static final String HASH_A = "a".repeat(64);
    private static final String HASH_B = "b".repeat(64);

    private final User user = verifiedUser();

    @Test
    void theFirstResetEmailIsAllowedAndStoresTheTokenDigestForFifteenMinutes() {
        assertThat(user.passwordResetEmailAllowed(T0, COOLDOWN, MAX_PER_HOUR)).isTrue();

        user.assignPasswordResetToken(HASH_A, T0.plus(TTL), T0);

        assertThat(user.getPasswordResetToken()).isEqualTo(HASH_A);
        assertThat(user.getResetTokenExpiresAt()).isEqualTo(T0.plusMinutes(15));
        assertThat(user.getPasswordResetSentAt()).isEqualTo(T0);
        assertThat(user.getPasswordResetWindowCount()).isEqualTo(1);
    }

    @Test
    void anotherEmailWaitsSixtySecondsAfterThePreviousOne() {
        user.assignPasswordResetToken(HASH_A, T0.plus(TTL), T0);

        assertThat(user.passwordResetEmailAllowed(T0.plusSeconds(59), COOLDOWN, MAX_PER_HOUR)).isFalse();
        assertThat(user.passwordResetEmailAllowed(T0.plusSeconds(60), COOLDOWN, MAX_PER_HOUR)).isTrue();
    }

    @Test
    void atMostFiveEmailsAreSentWithinOneHour() {
        for (int minute = 0; minute < 5; minute++) {
            LocalDateTime now = T0.plusMinutes(minute);
            assertThat(user.passwordResetEmailAllowed(now, COOLDOWN, MAX_PER_HOUR)).isTrue();
            user.assignPasswordResetToken(HASH_A, now.plus(TTL), now);
        }

        assertThat(user.passwordResetEmailAllowed(T0.plusMinutes(5), COOLDOWN, MAX_PER_HOUR)).isFalse();
        assertThat(user.passwordResetEmailAllowed(T0.plusMinutes(59).plusSeconds(59), COOLDOWN, MAX_PER_HOUR)).isFalse();
    }

    @Test
    void aNewHourWindowStartsOneHourAfterTheFirstEmailOfThePreviousWindow() {
        for (int minute = 0; minute < 5; minute++) {
            user.assignPasswordResetToken(HASH_A, T0.plusMinutes(minute).plus(TTL), T0.plusMinutes(minute));
        }
        LocalDateTime nextWindow = T0.plusHours(1);

        assertThat(user.passwordResetEmailAllowed(nextWindow, COOLDOWN, MAX_PER_HOUR)).isTrue();
        user.assignPasswordResetToken(HASH_B, nextWindow.plus(TTL), nextWindow);

        assertThat(user.getPasswordResetWindowStartedAt()).isEqualTo(nextWindow);
        assertThat(user.getPasswordResetWindowCount()).isEqualTo(1);
    }

    @Test
    void aNewTokenReplacesThePreviousOne() {
        user.assignPasswordResetToken(HASH_A, T0.plus(TTL), T0);

        user.assignPasswordResetToken(HASH_B, T0.plusMinutes(2).plus(TTL), T0.plusMinutes(2));

        assertThat(user.getPasswordResetToken()).isEqualTo(HASH_B);
        assertThat(user.getResetTokenExpiresAt()).isEqualTo(T0.plusMinutes(17));
    }

    @Test
    void resettingThePasswordStoresTheNewHashAndConsumesTheToken() {
        user.assignPasswordResetToken(HASH_A, T0.plus(TTL), T0);

        user.resetPassword("$2a$12$newhash", T0.plusMinutes(1));

        assertThat(user.getPasswordHash()).isEqualTo("$2a$12$newhash");
        assertThat(user.getPasswordResetToken()).isNull();
        assertThat(user.getResetTokenExpiresAt()).isNull();
        assertThat(user.getUpdatedAt()).isEqualTo(T0.plusMinutes(1));
    }

    @Test
    void resettingThePasswordVerifiesAnUnverifiedEmail() {
        User unverified = User.registerWithPassword("binh@example.com", "$2a$12$old", "Trần Bình", T0);
        unverified.assignEmailVerificationToken("c".repeat(64), T0.plusHours(24), T0);
        unverified.assignPasswordResetToken(HASH_A, T0.plus(TTL), T0);

        unverified.resetPassword("$2a$12$newhash", T0.plusMinutes(1));

        assertThat(unverified.isEmailVerified()).isTrue();
        assertThat(unverified.getEmailVerificationToken()).isNull();
        assertThat(unverified.getVerificationTokenExpiresAt()).isNull();
    }

    @Test
    void resettingThePasswordEndsATemporaryLoginBlock() {
        for (int attempt = 0; attempt < 5; attempt++) {
            user.recordFailedLogin(T0, 5, Duration.ofMinutes(10));
        }
        assertThat(user.isLoginBlocked(T0.plusMinutes(1))).isTrue();

        user.resetPassword("$2a$12$newhash", T0.plusMinutes(1));

        assertThat(user.isLoginBlocked(T0.plusMinutes(1))).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void theRateLimitWindowSurvivesAPasswordReset() {
        user.assignPasswordResetToken(HASH_A, T0.plus(TTL), T0);
        user.resetPassword("$2a$12$newhash", T0.plusSeconds(10));

        assertThat(user.passwordResetEmailAllowed(T0.plusSeconds(30), COOLDOWN, MAX_PER_HOUR)).isFalse();
        assertThat(user.getPasswordResetWindowCount()).isEqualTo(1);
    }

    private static User verifiedUser() {
        User user = User.registerWithPassword("an@example.com", "$2a$12$hash", "Nguyễn An", T0);
        user.markEmailVerified(T0);
        return user;
    }
}
