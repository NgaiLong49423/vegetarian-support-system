package tech.mamxanh.integration.ai;

import java.time.Instant;

/** FR-11 handoff: provider token counts only; never prompt or generated content. */
public record AiUsageEvent(long userId, String feature, Instant occurredAt,
        Integer promptTokens, Integer responseTokens, Integer totalTokens) { }
