package tech.mamxanh.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.mamxanh.auth.dto.response.MessageResponse;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.auth.security.AuthProperties;
import tech.mamxanh.auth.security.OneTimeTokenService;
import tech.mamxanh.auth.security.OneTimeTokenService.IssuedToken;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

/** UC-03.2 verify email and UC-03.3 resend verification email (AC-03.4, AC-03.5). */
@Service
public class EmailVerificationService {

    static final String RESEND_ACCEPTED_MESSAGE =
            "Nếu email thuộc một tài khoản chưa xác minh, chúng tôi đã gửi liên kết xác minh mới. Vui lòng kiểm tra hộp thư.";

    private final UserRepository userRepository;
    private final OneTimeTokenService tokenService;
    private final AuthProperties authProperties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public EmailVerificationService(UserRepository userRepository, OneTimeTokenService tokenService,
            AuthProperties authProperties, ApplicationEventPublisher events, Clock clock) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.authProperties = authProperties;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Marks the email verified and consumes the token. Unknown, already used, superseded and
     * expired tokens are all rejected with the same error. {@code account_status} is untouched.
     */
    @Transactional
    public void verify(String rawToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = userRepository.findByEmailVerificationToken(tokenService.hash(rawToken))
                .filter(candidate -> candidate.getVerificationTokenExpiresAt().isAfter(now))
                .orElseThrow(() -> new AppException(ErrorCode.VERIFICATION_TOKEN_INVALID));
        user.markEmailVerified(now);
    }

    /**
     * Issues a new token (overwriting the previous one) and requests a new email. Unknown or
     * already verified emails get the same neutral response and no email. Requests closer than
     * the cooldown to the previous email are rejected with {@code RESEND_TOO_SOON}; the time of the
     * previous email is derived from the stored expiry minus the verification TTL.
     */
    @Transactional
    public MessageResponse resend(String email) {
        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty() || found.get().isEmailVerified()) {
            return new MessageResponse(RESEND_ACCEPTED_MESSAGE);
        }
        User user = found.get();
        LocalDateTime now = LocalDateTime.now(clock);
        if (user.getVerificationTokenExpiresAt() != null) {
            LocalDateTime lastSentAt = user.getVerificationTokenExpiresAt()
                    .minus(authProperties.emailVerificationTtl());
            LocalDateTime nextAllowedAt = lastSentAt.plus(authProperties.verificationResendCooldown());
            if (now.isBefore(nextAllowedAt)) {
                throw AppException.retryAfter(ErrorCode.RESEND_TOO_SOON, Duration.between(now, nextAllowedAt));
            }
        }
        IssuedToken token = tokenService.issue();
        user.assignEmailVerificationToken(token.hash(), now.plus(authProperties.emailVerificationTtl()), now);
        events.publishEvent(new VerificationEmailRequested(user.getEmail(), user.getDisplayName(), token.raw()));
        return new MessageResponse(RESEND_ACCEPTED_MESSAGE);
    }
}
