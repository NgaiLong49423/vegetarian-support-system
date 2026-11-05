package tech.mamxanh.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminAiConfigurationResponse(List<PlanSetting> plans, List<ModelSetting> models,
        CreditSetting credit) {
    public record PlanSetting(String tier, int priceVnd, long tokenQuota, Long pendingTokenQuota,
            Instant pendingEffectiveAt) { }

    public record ModelSetting(String modelId, String displayName, boolean enabled,
            boolean freeAllowed, boolean freeDefault,
            boolean plusAllowed, boolean plusDefault,
            boolean proAllowed, boolean proDefault) { }

    public record CreditSetting(BigDecimal creditsPer1000Vnd, long creditsPer1000OverageTokens,
            int minimumTopUpVnd, int maximumTopUpVnd) { }
}
