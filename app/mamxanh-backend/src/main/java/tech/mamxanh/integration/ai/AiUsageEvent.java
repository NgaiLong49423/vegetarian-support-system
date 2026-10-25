package tech.mamxanh.integration.ai;

import java.time.Instant;

/** FR-11 handoff: provider token counts only; null userId denotes Guest. Never prompt or generated content. */
public record AiUsageEvent(Long userId, String feature, Instant occurredAt,
        Integer promptTokens, Integer responseTokens, Integer totalTokens) { }
