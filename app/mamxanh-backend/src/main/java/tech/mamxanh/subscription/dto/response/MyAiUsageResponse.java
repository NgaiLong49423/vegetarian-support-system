package tech.mamxanh.subscription.dto.response;

import java.time.Instant;

public record MyAiUsageResponse(String tier, long consumedTokens, long quotaTokens, long remainingTokens,
        Instant periodStartsAt, Instant resetsAt) { }
