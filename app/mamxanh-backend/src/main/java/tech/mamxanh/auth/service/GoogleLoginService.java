package tech.mamxanh.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import tech.mamxanh.auth.dto.response.AccountSummary;
import tech.mamxanh.auth.dto.response.AuthResponse;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.auth.security.AccessTokenService;
import tech.mamxanh.auth.security.AccessTokenService.IssuedAccessToken;
import tech.mamxanh.auth.security.GoogleIdentity;
import tech.mamxanh.auth.security.GoogleTokenVerifier;
import tech.mamxanh.auth.security.GoogleVerificationUnavailableException;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.common.validation.EmailAddress;

/**
 * UC-03.5 — log in with Google (AC-03.10, Q26, Q39–Q42).
 *
 * <p>The account is found by Google ID first, then by email. A new email creates a verified
 * customer from the Google profile; an existing account with that email is linked unless it is
 * {@code LOCKED} or already linked to another Google ID. A temporary password-login block does not
 * stop Google login and its counter is left untouched (Q42).
 */
@Service
public class GoogleLoginService {

    private static final Logger log = LoggerFactory.getLogger(GoogleLoginService.class);
    private static final int MAX_AVATAR_URL_LENGTH = 2048;

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserRepository userRepository;
    private final AccessTokenService accessTokenService;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public GoogleLoginService(GoogleTokenVerifier googleTokenVerifier, UserRepository userRepository,
            AccessTokenService accessTokenService, Clock clock, PlatformTransactionManager transactionManager) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.userRepository = userRepository;
        this.accessTokenService = accessTokenService;
        this.clock = clock;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /**
     * Verifies the token outside the database transaction (it may fetch Google's certificates). When
     * a parallel first login with the same Google account created the row first, the unique index
     * rejects this insert and a second attempt logs into that row.
     */
    public AuthResponse login(String idToken) {
        GoogleIdentity identity = verify(idToken);
        String email = EmailAddress.normalize(identity.email());
        if (!identity.emailVerified() || !isStorableEmail(email)) {
            throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID);
        }
        try {
            return transaction.execute(status -> loginVerified(identity, email));
        } catch (DataIntegrityViolationException concurrentFirstLogin) {
            return transaction.execute(status -> loginVerified(identity, email));
        }
    }

    private GoogleIdentity verify(String idToken) {
        try {
            return googleTokenVerifier.verify(idToken).orElseThrow(() -> new AppException(ErrorCode.GOOGLE_TOKEN_INVALID));
        } catch (GoogleVerificationUnavailableException unavailable) {
            log.warn("Google login unavailable: {}", unavailable.getCause().getClass().getSimpleName());
            throw new AppException(ErrorCode.GOOGLE_LOGIN_UNAVAILABLE);
        }
    }

    private AuthResponse loginVerified(GoogleIdentity identity, String email) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = userRepository.findByGoogleSubjectForUpdate(identity.subject())
                .map(GoogleLoginService::requireNotLocked)
                .orElseGet(() -> linkOrCreate(identity, email, now));
        IssuedAccessToken token = accessTokenService.issue(user);
        return new AuthResponse(token.value(), AuthResponse.BEARER, token.expiresInSeconds(), AccountSummary.from(user));
    }

    private User linkOrCreate(GoogleIdentity identity, String email, LocalDateTime now) {
        Optional<User> byEmail = userRepository.findByEmailForUpdate(email);
        if (byEmail.isEmpty()) {
            User created = User.registerWithGoogle(email, identity.subject(),
                    GoogleDisplayName.of(identity.name(), email), avatarUrl(identity.pictureUrl()), now);
            return userRepository.saveAndFlush(created);
        }
        User user = requireNotLocked(byEmail.get());
        if (user.getGoogleSubject() != null && !user.getGoogleSubject().equals(identity.subject())) {
            throw new AppException(ErrorCode.GOOGLE_ACCOUNT_CONFLICT);
        }
        user.linkGoogleAccount(identity.subject(), now);
        return userRepository.saveAndFlush(user);
    }

    private static User requireNotLocked(User user) {
        if (user.getAccountStatus() == AccountStatus.LOCKED) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }
        return user;
    }

    private static boolean isStorableEmail(String email) {
        return email != null && email.length() <= EmailAddress.MAX_LENGTH && email.matches(EmailAddress.PATTERN);
    }

    private static String avatarUrl(String pictureUrl) {
        return pictureUrl != null && pictureUrl.length() <= MAX_AVATAR_URL_LENGTH ? pictureUrl : null;
    }
}
