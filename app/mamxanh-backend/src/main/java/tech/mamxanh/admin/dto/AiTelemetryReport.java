package tech.mamxanh.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AiTelemetryReport(LocalDate from, LocalDate to, String granularity, String timezone,
        Instant retainedFrom, String currency, String costBasis, Totals totals, List<Bucket> buckets) {
    public record Totals(long calls, long promptTokens, long responseTokens, long totalTokens,
            long thinkingTokens, long cachedTokens, long incompleteUsageCalls, long unpricedCalls,
            BigDecimal knownEstimatedCostUsd,
            @io.swagger.v3.oas.annotations.media.Schema(types = {"number", "null"},
                    description = "Unknown when at least one call is unpriced; knownEstimatedCostUsd remains the priced subtotal.")
            BigDecimal estimatedCostUsd) { }
    public record Bucket(LocalDate periodStart, String feature, Totals totals) { }
}
