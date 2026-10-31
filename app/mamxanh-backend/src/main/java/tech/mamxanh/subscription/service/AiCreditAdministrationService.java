package tech.mamxanh.subscription.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.subscription.repository.AiCreditAdministrationRepository;

/** Records an Admin grant and wallet balance update as one transaction. */
@Service
public class AiCreditAdministrationService {
    private final AiCreditAdministrationRepository repository;
    private final Clock clock;

    public AiCreditAdministrationService(AiCreditAdministrationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public Grant grant(long userId, long administratorId, BigDecimal credits, String reason) {
        BigDecimal amount = credits == null ? BigDecimal.ZERO : credits.stripTrailingZeros();
        String note = reason == null ? "" : reason.strip();
        if (amount.signum() <= 0 || amount.scale() > 9 || note.length() < 10 || note.length() > 500) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "Số AI Credit phải dương và lý do phải có từ 10 đến 500 ký tự.");
        }
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        BigDecimal balance = repository.grant(userId, administratorId, UUID.randomUUID().toString(), amount,
                note, now);
        return new Grant(userId, amount, balance, note, now);
    }

    public record Grant(long userId, BigDecimal credits, BigDecimal balance, String reason,
            LocalDateTime grantedAt) { }
}
