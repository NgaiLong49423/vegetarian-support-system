package tech.mamxanh.integration.ai;

import java.time.Instant;
import java.util.UUID;

/** FR-11 handoff: provider token counts only; never prompt or generated content.
 * userId is null for Guest; no synthetic account or guest database entity is created.
 */
public record AiUsageEvent(UUID eventId, Long userId, String anonymousSessionId, String feature,
        Instant occurredAt, AiTokenUsage usage) {
    public AiUsageEvent(Long userId, String anonymousSessionId, String feature, Instant occurredAt, AiTokenUsage usage) {
        this(UUID.randomUUID(), userId, anonymousSessionId, feature, occurredAt, usage);
    }
    public AiUsageEvent(Long userId, String feature, Instant occurredAt, AiTokenUsage usage) {
        this(userId, null, feature, occurredAt, usage);
    }
    public AiUsageEvent(Long userId, String feature, Instant occurredAt,
            Integer promptTokens, Integer responseTokens, Integer totalTokens) {
        this(userId, feature, occurredAt, AiTokenUsage.legacy(promptTokens, responseTokens, totalTokens));
    }
    public Integer promptTokens() { return usage.promptTokens(); }
    public Integer responseTokens() { return usage.responseTokens(); }
    public Integer totalTokens() { return usage.totalTokens(); }
}
