package tech.mamxanh.auth.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * FR-03 timing, hashing and token parameters ({@code mamxanh.auth.*}).
 *
 * @param emailVerificationTtl      lifetime of an email-verification link (FR-03: 24 hours)
 * @param verificationResendCooldown minimum gap between two verification emails (FR-03: 60 seconds)
 * @param bcryptStrength            BCrypt work factor; FR-03 requires at least 10
 * @param frontendBaseUrl           origin used to build links in emails ({@code MAMXANH_FRONTEND_BASE_URL})
 * @param maxFailedLoginAttempts    consecutive wrong passwords that start a temporary block (NFR-07: 5)
 * @param loginBlockDuration        length of the temporary login block (NFR-07: 10 minutes)
 * @param accessTokenTtl            JWT access-token lifetime; configurable because the value is still
 *                                  under review (docs/api/API.md section 6)
 * @param jwtSecret                 HS256 signing secret ({@code MAMXANH_JWT_SECRET}), at least 32 bytes
 * @param jwtIssuer                 {@code iss} claim written and required on access tokens
 */
@ConfigurationProperties(prefix = "mamxanh.auth")
public record AuthProperties(
        @DefaultValue("24h") Duration emailVerificationTtl,
        @DefaultValue("60s") Duration verificationResendCooldown,
        @DefaultValue("12") int bcryptStrength,
        @DefaultValue("http://localhost:5173") String frontendBaseUrl,
        @DefaultValue("5") int maxFailedLoginAttempts,
        @DefaultValue("10m") Duration loginBlockDuration,
        @DefaultValue("15m") Duration accessTokenTtl,
        String jwtSecret,
        @DefaultValue("mamxanh") String jwtIssuer) {

    /** HS256 needs a key at least as long as its 256-bit output. */
    static final int MIN_JWT_SECRET_BYTES = 32;

    public AuthProperties {
        if (bcryptStrength < 10) {
            throw new IllegalArgumentException("mamxanh.auth.bcrypt-strength must be at least 10 (FR-03)");
        }
        if (maxFailedLoginAttempts < 1) {
            throw new IllegalArgumentException("mamxanh.auth.max-failed-login-attempts must be at least 1");
        }
        if (accessTokenTtl.isNegative() || accessTokenTtl.toSeconds() < 1) {
            throw new IllegalArgumentException("mamxanh.auth.access-token-ttl must be at least 1 second");
        }
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES) {
            throw new IllegalArgumentException("mamxanh.auth.jwt-secret (MAMXANH_JWT_SECRET) must be set to at least "
                    + MIN_JWT_SECRET_BYTES + " bytes");
        }
    }

    /** Keeps the signing secret out of logs and diagnostics. */
    @Override
    public String toString() {
        return "AuthProperties[emailVerificationTtl=" + emailVerificationTtl
                + ", verificationResendCooldown=" + verificationResendCooldown
                + ", bcryptStrength=" + bcryptStrength
                + ", frontendBaseUrl=" + frontendBaseUrl
                + ", maxFailedLoginAttempts=" + maxFailedLoginAttempts
                + ", loginBlockDuration=" + loginBlockDuration
                + ", accessTokenTtl=" + accessTokenTtl
                + ", jwtSecret=[redacted], jwtIssuer=" + jwtIssuer + "]";
    }
}
