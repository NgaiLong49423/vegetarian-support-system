package tech.mamxanh.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class AuthPropertiesTest {

    private static final String SECRET = "unit-test-signing-secret-0123456789abcdef";

    @Test
    void rejectsAMissingOrShortJwtSecret() {
        assertThatIllegalArgumentException().isThrownBy(() -> properties(null, Duration.ofMinutes(60)))
                .withMessageContaining("MAMXANH_JWT_SECRET");
        assertThatIllegalArgumentException().isThrownBy(() -> properties("x".repeat(31), Duration.ofMinutes(60)));
    }

    @Test
    void rejectsAnAccessTokenLifetimeShorterThanOneSecond() {
        assertThatIllegalArgumentException().isThrownBy(() -> properties(SECRET, Duration.ofMillis(500)))
                .withMessageContaining("access-token-ttl");
    }

    @Test
    void neverPrintsTheJwtSecret() {
        assertThat(properties(SECRET, Duration.ofMinutes(60)).toString())
                .doesNotContain(SECRET)
                .contains("jwtSecret=[redacted]", "accessTokenTtl=PT1H");
    }

    private static AuthProperties properties(String jwtSecret, Duration accessTokenTtl) {
        return new AuthProperties(Duration.ofHours(24), Duration.ofSeconds(60), 12, "http://localhost:5173",
                5, Duration.ofMinutes(10), accessTokenTtl, jwtSecret, "mamxanh");
    }
}
