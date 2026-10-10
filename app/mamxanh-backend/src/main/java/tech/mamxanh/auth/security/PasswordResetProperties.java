package tech.mamxanh.auth.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * FR-03-E password-reset parameters ({@code mamxanh.auth.password-reset.*}).
 *
 * @param tokenTtl         lifetime of a reset link (FR-03: 15 minutes)
 * @param emailCooldown    minimum gap between two reset emails to one account (Q27: 60 seconds)
 * @param maxEmailsPerHour reset emails allowed per account in one hour (Q27: 5)
 */
@ConfigurationProperties(prefix = "mamxanh.auth.password-reset")
public record PasswordResetProperties(
        @DefaultValue("15m") Duration tokenTtl,
        @DefaultValue("60s") Duration emailCooldown,
        @DefaultValue("5") int maxEmailsPerHour) {

    public PasswordResetProperties {
        if (tokenTtl.isNegative() || tokenTtl.toSeconds() < 1) {
            throw new IllegalArgumentException("mamxanh.auth.password-reset.token-ttl must be at least 1 second");
        }
        if (emailCooldown.isNegative()) {
            throw new IllegalArgumentException("mamxanh.auth.password-reset.email-cooldown must not be negative");
        }
        if (maxEmailsPerHour < 1) {
            throw new IllegalArgumentException("mamxanh.auth.password-reset.max-emails-per-hour must be at least 1");
        }
    }
}
