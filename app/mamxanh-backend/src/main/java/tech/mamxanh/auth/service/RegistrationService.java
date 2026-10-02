package tech.mamxanh.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.mamxanh.auth.dto.request.RegisterRequest;
import tech.mamxanh.auth.dto.response.RegistrationResponse;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.auth.security.AuthProperties;
import tech.mamxanh.auth.security.OneTimeTokenService;
import tech.mamxanh.auth.security.OneTimeTokenService.IssuedToken;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

/** UC-03.1 — register with email and password (AC-03.1–AC-03.3). */
@Service
public class RegistrationService {

    static final String REGISTERED_MESSAGE = "Đăng ký thành công. Vui lòng kiểm tra email để xác minh tài khoản.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OneTimeTokenService tokenService;
    private final AuthProperties authProperties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public RegistrationService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            OneTimeTokenService tokenService, AuthProperties authProperties, ApplicationEventPublisher events,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.authProperties = authProperties;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Creates an ACTIVE account with {@code email_verified = false}, a BCrypt password hash and a
     * 24-hour verification token, then requests the verification email after commit.
     * The request is already validated and its email normalized.
     */
    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_USED);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        User user = User.registerWithPassword(request.email(), passwordEncoder.encode(request.password()),
                request.displayName(), now);
        IssuedToken token = tokenService.issue();
        user.assignEmailVerificationToken(token.hash(), now.plus(authProperties.emailVerificationTtl()), now);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // A concurrent registration won the race on UQ_USER_email.
            throw new AppException(ErrorCode.EMAIL_ALREADY_USED);
        }
        events.publishEvent(new VerificationEmailRequested(user.getEmail(), user.getDisplayName(), token.raw()));
        return new RegistrationResponse(user.getAccountStatus(), user.isEmailVerified(), REGISTERED_MESSAGE);
    }
}
