package tech.mamxanh.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.mamxanh.auth.dto.request.LoginRequest;
import tech.mamxanh.auth.dto.response.AccountSummary;
import tech.mamxanh.auth.dto.response.AuthResponse;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.auth.security.AccessTokenService;
import tech.mamxanh.auth.security.AccessTokenService.IssuedAccessToken;
import tech.mamxanh.auth.security.AuthProperties;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

/**
 * UC-03.4 — log in with email and password (AC-03.6–AC-03.9, NFR-07).
 *
 * <p>Order of checks: the temporary block is checked before the password, so a blocked account
 * never reaches BCrypt; then the password; only a correct password reveals {@code ACCOUNT_LOCKED}
 * or {@code EMAIL_NOT_VERIFIED}. Unknown emails are compared against a dummy hash so they take
 * about as long as a wrong password and get the same neutral answer.
 */
@Service
public class LoginService {

    /** BCrypt only reads 72 bytes; FR-03 passwords are at most 72 UTF-8 bytes. */
    static final int BCRYPT_MAX_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccessTokenService accessTokenService;
    private final AuthProperties authProperties;
    private final Clock clock;
    private final String dummyPasswordHash;

    public LoginService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            AccessTokenService accessTokenService, AuthProperties authProperties, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accessTokenService = accessTokenService;
        this.authProperties = authProperties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * Failure counters must survive the exception that reports the failure, hence
     * {@code noRollbackFor}. The row lock from {@link UserRepository#findByEmailForLogin} serializes
     * concurrent attempts on the same account.
     */
    @Transactional(noRollbackFor = AppException.class)
    public AuthResponse login(LoginRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<User> found = userRepository.findByEmailForLogin(request.email());
        if (found.isEmpty()) {
            passwordMatches(request.password(), dummyPasswordHash);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = found.get();
        if (user.isLoginBlocked(now)) {
            throw blocked(now, user.getLoginBlockedUntil());
        }
        if (user.getPasswordHash() == null) {
            // Google-only account: no password can match; answer like an unknown email.
            passwordMatches(request.password(), dummyPasswordHash);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!passwordMatches(request.password(), user.getPasswordHash())) {
            boolean blockStarted = user.recordFailedLogin(now, authProperties.maxFailedLoginAttempts(),
                    authProperties.loginBlockDuration());
            if (blockStarted) {
                throw blocked(now, user.getLoginBlockedUntil());
            }
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (user.getAccountStatus() == AccountStatus.LOCKED) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (!user.isEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        user.recordSuccessfulLogin(now);
        IssuedAccessToken token = accessTokenService.issue(user);
        return new AuthResponse(token.value(), AuthResponse.BEARER, token.expiresInSeconds(),
                AccountSummary.from(user));
    }

    private boolean passwordMatches(String rawPassword, String passwordHash) {
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES) {
            // No stored password is longer, and BCrypt rejects such input instead of comparing it.
            return false;
        }
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    private AppException blocked(LocalDateTime now, LocalDateTime blockedUntil) {
        Duration remaining = Duration.between(now.atZone(clock.getZone()), blockedUntil.atZone(clock.getZone()));
        long minutes = Math.max(1, (remaining.toSeconds() + 59) / 60);
        String detail = "Bạn đã nhập sai mật khẩu " + authProperties.maxFailedLoginAttempts()
                + " lần liên tiếp. Vui lòng thử lại sau " + minutes + " phút.";
        return AppException.retryAfter(ErrorCode.LOGIN_TEMPORARILY_BLOCKED, detail, remaining);
    }
}
