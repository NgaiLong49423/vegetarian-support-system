package tech.mamxanh.subscription.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.service.CurrentUserService;
import tech.mamxanh.common.exception.ApiException;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.subscription.dto.response.MyAiUsageResponse;
import tech.mamxanh.subscription.entity.AiConfigurationSnapshot;
import tech.mamxanh.subscription.entity.AiTokenUsageLedgerEntity;
import tech.mamxanh.subscription.entity.SubscriptionEntity;
import tech.mamxanh.subscription.entity.SubscriptionStatus;
import tech.mamxanh.subscription.repository.AiConfigurationRepository;
import tech.mamxanh.subscription.repository.AiCreditAdministrationRepository;
import tech.mamxanh.subscription.repository.AiTokenUsageLedgerRepository;
import tech.mamxanh.subscription.repository.SubscriptionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.Locale;
import java.util.UUID;

/** Synchronous quota and AI Credit overage accounting; telemetry remains a separate operational stream. */
@Service
public class AiTokenQuotaService {
    public static final long GUEST_QUOTA = 20_000;
    public static final long DEFAULT_OVERAGE_RATE = 400L;
    private static final ZoneId FREE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final long RETENTION_DAYS = 90;

    private final AiTokenUsageLedgerRepository ledger;
    private final SubscriptionRepository subscriptions;
    private final CurrentUserService users;
    private final AiConfigurationRepository configuration;
    private final AiCreditAdministrationRepository creditWallet;
    private final Clock clock;

    public AiTokenQuotaService(AiTokenUsageLedgerRepository ledger, SubscriptionRepository subscriptions,
            CurrentUserService users, AiConfigurationRepository configuration, Clock clock) {
        this(ledger, subscriptions, users, configuration, null, clock);
    }

    @Autowired
    public AiTokenQuotaService(AiTokenUsageLedgerRepository ledger, SubscriptionRepository subscriptions,
            CurrentUserService users, AiConfigurationRepository configuration,
            AiCreditAdministrationRepository creditWallet, Clock clock) {
        this.ledger = ledger;
        this.subscriptions = subscriptions;
        this.users = users;
        this.configuration = configuration;
        this.creditWallet = creditWallet;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public void requireAvailable(Long userId, String guestSessionId) {
        Period period = currentPeriod(userId, guestSessionId, clock.instant());
        long consumed = consumed(userId, guestSessionId, period);
        if (consumed < period.limit()) return;
        if (userId != null && creditBalance(userId).signum() > 0) return;
        throw new AppException(ErrorCode.AI_TOKEN_QUOTA_EXCEEDED,
                "Bạn đã dùng hết hạn mức token của kỳ hiện tại. Hạn mức sẽ được cấp lại vào "
                        + period.end().atOffset(ZoneOffset.UTC).toInstant() + ".");
    }

    @Transactional(readOnly = true)
    public boolean hasAvailableBudget(Long userId, String guestSessionId) {
        Period period = currentPeriod(userId, guestSessionId, clock.instant());
        long consumed = consumed(userId, guestSessionId, period);
        if (consumed < period.limit()) return true;
        return userId != null && creditBalance(userId).signum() > 0;
    }

    /**
     * Captures the call-start accounting snapshot (period, quota, model, overage rate) and validates
     * active member identity, model eligibility, and budget availability without reserving tokens/credits.
     */
    @Transactional(readOnly = true)
    public CallStartSnapshot beginCall(Long userId, String guestSessionId, String feature, String modelId, Instant startedAt) {
        Instant callStart = startedAt == null ? clock.instant() : startedAt;
        if (userId != null) {
            var active = users.requireActiveMember();
            if (active.id() != userId) throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        Period period = currentPeriod(userId, guestSessionId, callStart);
        if (userId != null) {
            requireModelAllowed(period.tier(), modelId);
        }
        long consumed = consumed(userId, guestSessionId, period);
        if (consumed >= period.limit() && (userId == null || creditBalance(userId).signum() <= 0)) {
            throw new AppException(ErrorCode.AI_TOKEN_QUOTA_EXCEEDED,
                    "Bạn đã dùng hết hạn mức token của kỳ hiện tại và không còn AI Credit khả dụng. Hạn mức sẽ được cấp lại vào "
                            + period.end().atOffset(ZoneOffset.UTC).toInstant() + ".");
        }
        return new CallStartSnapshot(userId, guestSessionId, feature == null ? "FR-51" : feature,
                period.tier(), period.limit(), period.start(), period.end(), modelId, overageRate(), callStart);
    }

    @Transactional(readOnly = true)
    public MyAiUsageResponse getMyUsage(long userId) {
        if (users.requireActiveMember().id() != userId) throw new AppException(ErrorCode.ACCESS_DENIED);
        Period period = currentPeriod(userId, null, clock.instant());
        long consumed = consumed(userId, null, period);
        return new MyAiUsageResponse(period.tier(), consumed, period.limit(), Math.max(0, period.limit() - consumed),
                period.start().atOffset(ZoneOffset.UTC).toInstant(), period.end().atOffset(ZoneOffset.UTC).toInstant());
    }

    @Transactional(readOnly = true)
    public MyAiUsageResponse getGuestUsage(String guestSessionId) {
        if (guestSessionId == null) throw new AppException(ErrorCode.VALIDATION_FAILED);
        try { UUID.fromString(guestSessionId); }
        catch (IllegalArgumentException exception) { throw new AppException(ErrorCode.VALIDATION_FAILED); }
        Period period = currentPeriod(null, guestSessionId, clock.instant());
        long consumed = consumed(null, guestSessionId, period);
        return new MyAiUsageResponse(period.tier(), consumed, period.limit(), Math.max(0, period.limit() - consumed),
                period.start().atOffset(ZoneOffset.UTC).toInstant(), period.end().atOffset(ZoneOffset.UTC).toInstant());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AiUsageEvent event) {
        if (event == null || event.eventId() == null || event.occurredAt() == null
                || (event.userId() == null) == (event.anonymousSessionId() == null)) {
            throw new AppException(ErrorCode.AI_TELEMETRY_UNAVAILABLE);
        }
        if (event.anonymousSessionId() != null) {
            try { UUID.fromString(event.anonymousSessionId()); }
            catch (IllegalArgumentException exception) { throw new AppException(ErrorCode.AI_TELEMETRY_UNAVAILABLE); }
        }
        Period period = currentPeriod(event.userId(), event.anonymousSessionId(), event.occurredAt());
        String model = event.usage() == null ? null : event.usage().model();
        CallStartSnapshot snapshot = new CallStartSnapshot(event.userId(), event.anonymousSessionId(),
                event.feature(), period.tier(), period.limit(), period.start(), period.end(),
                model, overageRate(), event.occurredAt());
        completeCall(event.eventId(), snapshot, event.usage());
    }

    /**
     * Records token usage against the call-start snapshot and debits overage AI Credit in a single
     * transaction deduplicated by {@code callId}. Missing or invalid metadata never debits quota or Credit.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeCall(UUID callId, CallStartSnapshot snapshot, AiTokenUsage metadata) {
        if (callId == null || snapshot == null || snapshot.startedAt() == null
                || (snapshot.userId() == null) == (snapshot.anonymousSessionId() == null)) {
            throw new AppException(ErrorCode.AI_TELEMETRY_UNAVAILABLE);
        }
        if (snapshot.anonymousSessionId() != null) {
            try { UUID.fromString(snapshot.anonymousSessionId()); }
            catch (IllegalArgumentException exception) { throw new AppException(ErrorCode.AI_TELEMETRY_UNAVAILABLE); }
        }
        String eventId = callId.toString();
        LocalDateTime occurredAt = LocalDateTime.ofInstant(snapshot.startedAt(), ZoneOffset.UTC);
        LocalDateTime expiresAt = (snapshot.userId() == null ? occurredAt : snapshot.periodEnd())
                .plusDays(RETENTION_DAYS);
        Usage usage = usage(metadata);
        String model = metadata != null && metadata.model() != null ? metadata.model() : snapshot.model();

        try {
            if (snapshot.userId() != null && creditWallet != null) {
                creditWallet.ensureAndLockWallet(snapshot.userId(), occurredAt);
            }
            if (ledger.existsById(eventId)) return;

            long consumedBefore = snapshot.userId() == null
                    ? ledger.sumGuestTokens(snapshot.anonymousSessionId())
                    : ledger.sumMemberTokens(snapshot.userId(), snapshot.periodStart(), snapshot.periodEnd());

            ledger.saveAndFlush(new AiTokenUsageLedgerEntity(eventId, snapshot.userId(),
                    snapshot.anonymousSessionId(), snapshot.feature(), model,
                    usage.status(), usage.tokens(), snapshot.quotaLimit(),
                    snapshot.periodStart(), snapshot.periodEnd(), occurredAt, expiresAt));

            if (snapshot.userId() != null && creditWallet != null
                    && "RECORDED".equals(usage.status()) && usage.tokens() != null && usage.tokens() > 0) {
                long quotaRemainingBefore = Math.max(0L, snapshot.quotaLimit() - consumedBefore);
                long quotaCovered = Math.min(usage.tokens(), quotaRemainingBefore);
                long overageTokens = usage.tokens() - quotaCovered;
                if (overageTokens > 0) {
                    long rate = snapshot.creditsPer1000OverageTokens() > 0
                            ? snapshot.creditsPer1000OverageTokens() : DEFAULT_OVERAGE_RATE;
                    BigDecimal requiredCredits = BigDecimal.valueOf(overageTokens)
                            .multiply(BigDecimal.valueOf(rate))
                            .divide(BigDecimal.valueOf(1_000), 9, RoundingMode.HALF_UP);
                    creditWallet.debitOverage(snapshot.userId(), eventId, requiredCredits, overageTokens, rate, occurredAt);
                }
            }
        } catch (DataIntegrityViolationException exception) {
            // Never swallow a wallet/ledger failure after the usage row was flushed in this transaction.
            // Member duplicates are checked while the wallet row is locked before any insert.
            throw new AppException(ErrorCode.AI_TELEMETRY_UNAVAILABLE);
        } catch (DataAccessException exception) {
            throw new AppException(ErrorCode.AI_TELEMETRY_UNAVAILABLE);
        }
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void purgeExpired() {
        ledger.deleteExpired(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }

    private BigDecimal creditBalance(long userId) {
        if (creditWallet == null) return BigDecimal.ZERO;
        BigDecimal balance = creditWallet.balance(userId);
        return balance == null ? BigDecimal.ZERO : balance;
    }

    private long overageRate() {
        AiConfigurationSnapshot snapshot = configuration.findConfiguration();
        if (snapshot == null || snapshot.credit() == null || snapshot.credit().creditsPer1000OverageTokens() <= 0) {
            return DEFAULT_OVERAGE_RATE;
        }
        return snapshot.credit().creditsPer1000OverageTokens();
    }

    private void requireModelAllowed(String tier, String modelId) {
        if (modelId == null || modelId.isBlank()) throw new ApiException(HttpStatus.CONFLICT,"AI_MODEL_SELECTION_REQUIRED","Hãy chọn model hợp lệ trước khi tiếp tục.");
        AiConfigurationSnapshot snapshot = configuration.findConfiguration();
        if (snapshot == null || snapshot.models() == null || snapshot.models().isEmpty()) throw new ApiException(HttpStatus.CONFLICT,"AI_MODEL_SELECTION_REQUIRED","Catalog model chưa sẵn sàng.");
        String normalized = modelId.strip().toLowerCase(Locale.ROOT);
        boolean allowed = snapshot.models().stream()
                .filter(AiConfigurationSnapshot.Model::enabled)
                .filter(model -> switch (tier) {
                    case "PLUS" -> model.plusAllowed();
                    case "PRO" -> model.proAllowed();
                    default -> model.freeAllowed();
                })
                .anyMatch(model -> model.modelId().equalsIgnoreCase(normalized));
        if (!allowed) {
            throw new ApiException(HttpStatus.CONFLICT, "AI_MODEL_SELECTION_REQUIRED",
                    "Model đang chọn không còn được gói của bạn hỗ trợ. Hãy chọn một model khả dụng trong Chatbox.");
        }
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
        return new Period("FREE", configuration.currentFreeQuota(at), LocalDateTime.ofInstant(start, ZoneOffset.UTC),
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

    public record CallStartSnapshot(
            Long userId,
            String anonymousSessionId,
            String feature,
            String tier,
            long quotaLimit,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            String model,
            long creditsPer1000OverageTokens,
            Instant startedAt) { }

    private record Period(String tier, long limit, LocalDateTime start, LocalDateTime end) { }
    private record Usage(String status, Long tokens) { }
}
