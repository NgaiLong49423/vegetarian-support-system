package tech.mamxanh.subscription.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.subscription.dto.response.MyAiUsageResponse;
import tech.mamxanh.subscription.entity.AiTokenUsageLedgerEntity;
import tech.mamxanh.subscription.entity.SubscriptionEntity;
import tech.mamxanh.subscription.entity.SubscriptionStatus;
import tech.mamxanh.subscription.entity.SubscriptionTier;
import tech.mamxanh.subscription.repository.AiTokenUsageLedgerRepository;
import tech.mamxanh.subscription.repository.SubscriptionRepository;

/** Synchronous quota accounting; telemetry remains a separate operational stream. */
@Service
public class AiTokenQuotaService {
    public static final long GUEST_QUOTA = 20_000;
    public static final long FREE_QUOTA = 50_000;
    private static final ZoneId FREE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long RETENTION_DAYS = 90;

    private final AiTokenUsageLedgerRepository ledger;
    private final SubscriptionRepository subscriptions;
    private final CurrentUserService users;
    private final Clock clock;

    public AiTokenQuotaService(AiTokenUsageLedgerRepository ledger, SubscriptionRepository subscriptions,
            CurrentUserService users, Clock clock) {
        this.ledger = ledger;
        this.subscriptions = subscriptions;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public void requireAvailable(Long userId, String guestSessionId) {
        Period period = currentPeriod(userId, guestSessionId, clock.instant());
        long consumed = consumed(userId, guestSessionId, period);
        if (consumed >= period.limit()) {
            throw new AppException(ErrorCode.AI_TOKEN_QUOTA_EXCEEDED,
                    "Bạn đã dùng hết hạn mức token của kỳ hiện tại. Hạn mức sẽ được cấp lại vào "
                            + period.end().atOffset(ZoneOffset.UTC).toInstant() + ".");
        }
    }

    @Transactional(readOnly = true)
    public MyAiUsageResponse getMyUsage(long userId) {
        if (users.requireActiveMember().id() != userId) throw new AppException(ErrorCode.ACCESS_DENIED);
        Period period = currentPeriod(userId, null, clock.instant());
        long consumed = consumed(userId, null, period);
        return new MyAiUsageResponse(period.tier(), consumed, period.limit(), Math.max(0, period.limit() - consumed),
                period.start().atOffset(ZoneOffset.UTC).toInstant(), period.end().atOffset(ZoneOffset.UTC).toInstant());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AiUsageEvent event) {
        if (event == null || event.eventId() == null || event.occurredAt() == null
                || (event.userId() == null) == (event.anonymousSessionId() == null)) {
            throw new AppException(ErrorCode.AI_TELEMETRY_UNAVAILABLE);
        }
        if (ledger.existsById(event.eventId().toString())) return;
        if (event.anonymousSessionId() != null) {
            try { UUID.fromString(event.anonymousSessionId()); }
            catch (IllegalArgumentException exception) { throw new AppException(ErrorCode.AI_TELEMETRY_UNAVAILABLE); }
        }
        Period period = currentPeriod(event.userId(), event.anonymousSessionId(), event.occurredAt());
        Usage usage = usage(event.usage());
        LocalDateTime occurredAt = LocalDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        LocalDateTime expiresAt = (event.userId() == null ? occurredAt : period.end())
                .plusDays(RETENTION_DAYS);
        try {
            ledger.saveAndFlush(new AiTokenUsageLedgerEntity(event.eventId().toString(), event.userId(),
                    event.anonymousSessionId(), event.feature(), event.usage() == null ? null : event.usage().model(),
                    usage.status(), usage.tokens(), period.limit(), period.start(), period.end(), occurredAt, expiresAt));
        } catch (DataIntegrityViolationException exception) {
            // Unique event IDs make retries idempotent; a concurrent duplicate is already accounted for.
            if (!ledger.existsById(event.eventId().toString())) throw exception;
        }
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void purgeExpired() {
        ledger.deleteExpired(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }

    private Period currentPeriod(Long userId, String guestSessionId, Instant at) {
        if (userId == null) {
            if (guestSessionId == null) throw new AppException(ErrorCode.VALIDATION_FAILED);
            LocalDateTime end = LocalDateTime.ofInstant(at, ZoneOffset.UTC).plusNanos(1);
            return new Period("GUEST", GUEST_QUOTA, end.minusDays(1), end);
        }
        LocalDateTime now = LocalDateTime.ofInstant(at, ZoneOffset.UTC);
        SubscriptionEntity active = subscriptions.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .filter(subscription -> subscription.isActiveAt(now)).orElse(null);
        if (active != null) {
            return new Period(active.getTier().name(), active.getTokenQuota(), active.getStartsAt(), active.getEndsAt());
        }
        LocalDate localDate = at.atZone(FREE_ZONE).toLocalDate();
        Instant start = localDate.withDayOfMonth(1).atStartOfDay(FREE_ZONE).toInstant();
        Instant end = localDate.withDayOfMonth(1).plusMonths(1).atStartOfDay(FREE_ZONE).toInstant();
        return new Period("FREE", FREE_QUOTA, LocalDateTime.ofInstant(start, ZoneOffset.UTC),
                LocalDateTime.ofInstant(end, ZoneOffset.UTC));
    }

    private long consumed(Long userId, String guestSessionId, Period period) {
        return userId == null ? ledger.sumGuestTokens(guestSessionId)
                : ledger.sumMemberTokens(userId, period.start(), period.end());
    }

    private static Usage usage(AiTokenUsage metadata) {
        if (metadata == null) return new Usage("MISSING", null);
        Integer[] values = {metadata.promptTokens(), metadata.responseTokens(), metadata.totalTokens(),
                metadata.thinkingTokens(), metadata.cachedTokens(), metadata.toolUsePromptTokens()};
        if (java.util.Arrays.stream(values).allMatch(java.util.Objects::isNull)) return new Usage("MISSING", null);
        if (java.util.Arrays.stream(values).filter(java.util.Objects::nonNull).anyMatch(value -> value < 0)
                || metadata.promptTokens() == null || metadata.responseTokens() == null || metadata.totalTokens() == null) {
            return new Usage("INVALID", null);
        }
        long minimumTotal = (long) metadata.promptTokens() + metadata.responseTokens()
                + (metadata.thinkingTokens() == null ? 0 : metadata.thinkingTokens())
                + (metadata.toolUsePromptTokens() == null ? 0 : metadata.toolUsePromptTokens());
        if (metadata.totalTokens() < minimumTotal) return new Usage("INVALID", null);
        return new Usage("RECORDED", metadata.totalTokens().longValue());
    }

    private record Period(String tier, long limit, LocalDateTime start, LocalDateTime end) { }
    private record Usage(String status, Long tokens) { }
}
