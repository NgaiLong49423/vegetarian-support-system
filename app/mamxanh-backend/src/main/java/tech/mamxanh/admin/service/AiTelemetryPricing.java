package tech.mamxanh.admin.service;

import org.springframework.stereotype.Service;
import tech.mamxanh.admin.dto.AiCostEstimate;
import tech.mamxanh.integration.ai.AiTokenUsage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/** Versioned Standard text-token equivalent; not a Google invoice or application-plan charge.
 * Source: https://ai.google.dev/gemini-api/docs/pricing#gemini-3.8-flash (verified 2026-10-28).
 */
@Service
public class AiTelemetryPricing {
    public AiCostEstimate estimate(AiTokenUsage usage, Instant at, String status) {
        if (!"COMPLETE".equals(status) || !AiTokenUsage.MODEL.equals(usage.model())
                || !"STANDARD".equals(usage.serviceTier()) || usage.thinkingTokens() == null
                || usage.cachedTokens() == null || usage.toolUsePromptTokens() == null
                || usage.toolUsePromptTokens() != 0 || at.isBefore(Instant.parse("2026-10-22T17:00:00Z"))) return null;
        boolean introductory = at.isBefore(Instant.parse("2027-01-01T00:00:00Z"));
        BigDecimal input = new BigDecimal(introductory ? "0.75" : "1.50");
        BigDecimal output = new BigDecimal(introductory ? "3.75" : "7.50");
        BigDecimal cached = new BigDecimal(introductory ? "0.075" : "0.15");
        BigDecimal cost = input.multiply(BigDecimal.valueOf((long) usage.promptTokens() - usage.cachedTokens()))
                .add(cached.multiply(BigDecimal.valueOf(usage.cachedTokens())))
                .add(output.multiply(BigDecimal.valueOf((long) usage.responseTokens() + usage.thinkingTokens())))
                .divide(BigDecimal.valueOf(1_000_000), 12, RoundingMode.HALF_UP);
        return new AiCostEstimate(introductory ? "gemini-standard-2026-10-23" : "gemini-standard-2027-01-01", cost);
    }
}
