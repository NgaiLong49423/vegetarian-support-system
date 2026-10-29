package tech.mamxanh.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;

/**
 * Administrative account locks stay owned by auth and are exposed to moderation as a service.
 * {@code AccountStatusJwtAuthenticationConverter} re-reads the account on every request, so
 * tokens issued before the lock stop working at once (AC-28.4).
 */
@Service
public class AccountModerationService {
    private final UserRepository users;
    private final Clock clock;

    public AccountModerationService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public void lockAccount(long userId) {
        users.findByIdForUpdate(userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND))
                .lockByAdministrator(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }
}
