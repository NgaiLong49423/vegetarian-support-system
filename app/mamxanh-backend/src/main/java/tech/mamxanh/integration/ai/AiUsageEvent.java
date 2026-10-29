package tech.mamxanh.integration.ai;

import java.time.Instant;

/** FR-11 handoff: provider token counts only; never prompt or generated content.
 * userId is null for Guest; no synthetic account or guest database entity is created.
 */
public record AiUsageEvent(Long userId, String feature, Instant occurredAt,
        Integer promptTokens, Integer responseTokens, Integer totalTokens) { }
