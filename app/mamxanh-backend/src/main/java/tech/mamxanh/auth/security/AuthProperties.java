package tech.mamxanh.auth.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * FR-03 timing and hashing parameters ({@code mamxanh.auth.*}).
 *
 * @param emailVerificationTtl      lifetime of an email-verification link (FR-03: 24 hours)
 * @param verificationResendCooldown minimum gap between two verification emails (FR-03: 60 seconds)
 * @param bcryptStrength            BCrypt work factor; FR-03 requires at least 10
 * @param frontendBaseUrl           origin used to build links in emails ({@code MAMXANH_FRONTEND_BASE_URL})
 */
@ConfigurationProperties(prefix = "mamxanh.auth")
public record AuthProperties(
        @DefaultValue("24h") Duration emailVerificationTtl,
        @DefaultValue("60s") Duration verificationResendCooldown,
        @DefaultValue("12") int bcryptStrength,
        @DefaultValue("http://localhost:5173") String frontendBaseUrl) {

    public AuthProperties {
        if (bcryptStrength < 10) {
            throw new IllegalArgumentException("mamxanh.auth.bcrypt-strength must be at least 10 (FR-03)");
        }
    }
}
