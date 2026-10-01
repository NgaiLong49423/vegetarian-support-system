package tech.mamxanh.auth.entity;

import java.time.Duration;
import java.time.LocalDateTime;

import org.hibernate.annotations.Nationalized;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Identity and authentication view of the {@code USER} table (decision Q18). The {@code auth}
 * module owns account creation/deletion and the security columns mapped here; profile and
 * nutrition columns belong to other modules and are intentionally not mapped. Timestamps are UTC.
 */
@Entity
@Table(name = "\"USER\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "google_subject", length = 255)
    private String googleSubject;

    @Nationalized
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false, length = 20)
    private AccountStatus accountStatus;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    /** SHA-256 hex digest of the current verification token; never the raw token. */
    @Column(name = "email_verification_token", length = 64)
    private String emailVerificationToken;

    @Column(name = "verification_token_expires_at")
    private LocalDateTime verificationTokenExpiresAt;

    /** Consecutive wrong passwords since the last successful login or the end of the last block. */
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    /** End of the temporary login block (NFR-07); independent of {@code accountStatus = LOCKED}. */
    @Column(name = "login_blocked_until")
    private LocalDateTime loginBlockedUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** UC-03.1: new accounts are ACTIVE customers whose email is not yet verified. */
    public static User registerWithPassword(String email, String passwordHash, String displayName,
            LocalDateTime now) {
        User user = new User();
        user.email = email;
        user.passwordHash = passwordHash;
        user.displayName = displayName;
        user.role = Role.CUSTOMER;
        user.accountStatus = AccountStatus.ACTIVE;
        user.emailVerified = false;
        user.createdAt = now;
        user.updatedAt = now;
        return user;
    }

    /** Replaces any previous verification token, so only the newest link stays valid. */
    public void assignEmailVerificationToken(String tokenHash, LocalDateTime expiresAt, LocalDateTime now) {
        this.emailVerificationToken = tokenHash;
        this.verificationTokenExpiresAt = expiresAt;
        this.updatedAt = now;
    }

    /** AC-03.4: verifies the email, consumes the token and leaves {@code accountStatus} unchanged. */
    public void markEmailVerified(LocalDateTime now) {
        this.emailVerified = true;
        this.emailVerificationToken = null;
        this.verificationTokenExpiresAt = null;
        this.updatedAt = now;
    }

    public boolean isLoginBlocked(LocalDateTime now) {
        return loginBlockedUntil != null && now.isBefore(loginBlockedUntil);
    }

    /**
     * AC-03.7/AC-03.8: counts a wrong password. A block that has already expired starts a new
     * count (AC-03.9); reaching {@code maxAttempts} blocks login until {@code now + blockDuration}.
     * The administrative {@code accountStatus} is never changed here.
     *
     * @return {@code true} when this failure started a new block
     */
    public boolean recordFailedLogin(LocalDateTime now, int maxAttempts, Duration blockDuration) {
        if (loginBlockedUntil != null && !now.isBefore(loginBlockedUntil)) {
            failedLoginAttempts = 0;
            loginBlockedUntil = null;
        }
        failedLoginAttempts++;
        updatedAt = now;
        if (failedLoginAttempts >= maxAttempts) {
            loginBlockedUntil = now.plus(blockDuration);
            return true;
        }
        return false;
    }

    /** AC-03.6/AC-03.9: a successful login clears the counter and any expired block. */
    public void recordSuccessfulLogin(LocalDateTime now) {
        if (failedLoginAttempts != 0 || loginBlockedUntil != null) {
            failedLoginAttempts = 0;
            loginBlockedUntil = null;
            updatedAt = now;
        }
    }
}
