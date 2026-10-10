package tech.mamxanh.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.mamxanh.auth.dto.request.PasswordResetConfirmRequest;
import tech.mamxanh.auth.dto.response.MessageResponse;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.auth.security.OneTimeTokenService;
import tech.mamxanh.auth.security.OneTimeTokenService.IssuedToken;
import tech.mamxanh.auth.security.PasswordResetProperties;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

/** UC-03.6 request a password reset and UC-03.7 set a new password (AC-03.14, Q27). */
@Service
public class PasswordResetService {

    static final String REQUEST_ACCEPTED_MESSAGE =
            "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi đến hộp thư của bạn.";

    private final UserRepository userRepository;
    private final OneTimeTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PasswordResetService(UserRepository userRepository, OneTimeTokenService tokenService,
            PasswordEncoder passwordEncoder, PasswordResetProperties properties, ApplicationEventPublisher events,
            Clock clock) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Always returns the same neutral message. An email is sent only to an ACTIVE account that is
     * outside the 60-second cooldown and under 5 emails in the current hour (Q27); unknown emails,
     * LOCKED accounts and rate-limited requests are silently skipped. The new token overwrites the
     * previous one. The row lock makes the limit check and the token replacement atomic, so
     * concurrent requests for one account send at most one email.
     */
    @Transactional
    public MessageResponse requestReset(String email) {
        LocalDateTime now = LocalDateTime.now(clock);
        userRepository.findByEmailForUpdate(email)
                .filter(user -> user.getAccountStatus() == AccountStatus.ACTIVE)
                .filter(user -> user.passwordResetEmailAllowed(now, properties.emailCooldown(),
                        properties.maxEmailsPerHour()))
                .ifPresent(user -> {
                    IssuedToken token = tokenService.issue();
                    user.assignPasswordResetToken(token.hash(), now.plus(properties.tokenTtl()), now);
                    events.publishEvent(new PasswordResetEmailRequested(user.getEmail(), user.getDisplayName(),
                            token.raw()));
                });
        return new MessageResponse(REQUEST_ACCEPTED_MESSAGE);
    }

    /**
     * Sets the new BCrypt password and consumes the token. Unknown, used, overwritten and expired
     * tokens get the same error. A Google-only account (no password yet) may set one (Q47); the
     * current password is rejected as the new one (Q48) and the token stays usable for a retry.
     * The request is already validated.
     */
    @Transactional
    public void confirmReset(PasswordResetConfirmRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = userRepository.findByPasswordResetTokenForUpdate(tokenService.hash(request.token()))
                .filter(candidate -> candidate.getResetTokenExpiresAt().isAfter(now))
                .orElseThrow(() -> new AppException(ErrorCode.PASSWORD_RESET_TOKEN_INVALID));
        if (user.getAccountStatus() == AccountStatus.LOCKED) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }
        if (user.getPasswordHash() != null && passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.NEW_PASSWORD_SAME_AS_CURRENT);
        }
        user.resetPassword(passwordEncoder.encode(request.newPassword()), now);
    }
}
