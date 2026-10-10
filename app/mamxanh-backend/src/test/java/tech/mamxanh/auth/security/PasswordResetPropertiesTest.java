package tech.mamxanh.auth.security;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class PasswordResetPropertiesTest {

    @Test
    void acceptsTheFr03Values() {
        assertThatNoException().isThrownBy(
                () -> new PasswordResetProperties(Duration.ofMinutes(15), Duration.ofSeconds(60), 5));
    }

    @Test
    void rejectsATokenLifetimeShorterThanOneSecond() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PasswordResetProperties(Duration.ZERO, Duration.ofSeconds(60), 5))
                .withMessageContaining("token-ttl");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PasswordResetProperties(Duration.ofMinutes(-15), Duration.ofSeconds(60), 5))
                .withMessageContaining("token-ttl");
    }

    @Test
    void rejectsANegativeCooldown() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PasswordResetProperties(Duration.ofMinutes(15), Duration.ofSeconds(-1), 5))
                .withMessageContaining("email-cooldown");
    }

    @Test
    void rejectsAnHourlyLimitBelowOne() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new PasswordResetProperties(Duration.ofMinutes(15), Duration.ofSeconds(60), 0))
                .withMessageContaining("max-emails-per-hour");
    }
}
