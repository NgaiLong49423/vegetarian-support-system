package tech.mamxanh.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "AI_TOKEN_USAGE_LEDGER")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiTokenUsageLedgerEntity {
    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "anonymous_session_id", length = 36)
    private String anonymousSessionId;

    @Column(name = "feature", nullable = false, length = 10)
    private String feature;

    @Column(name = "model", length = 80)
    private String model;

    @Column(name = "usage_status", nullable = false, length = 20)
    private String usageStatus;

    @Column(name = "token_count")
    private Long tokenCount;

    @Column(name = "quota_limit", nullable = false)
    private long quotaLimit;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public AiTokenUsageLedgerEntity(String eventId, Long userId, String anonymousSessionId, String feature,
            String model, String usageStatus, Long tokenCount, long quotaLimit, LocalDateTime periodStart,
            LocalDateTime periodEnd, LocalDateTime occurredAt, LocalDateTime expiresAt) {
        this.eventId = eventId;
        this.userId = userId;
        this.anonymousSessionId = anonymousSessionId;
        this.feature = feature;
        this.model = model;
        this.usageStatus = usageStatus;
        this.tokenCount = tokenCount;
        this.quotaLimit = quotaLimit;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.occurredAt = occurredAt;
        this.expiresAt = expiresAt;
    }
}
