package tech.mamxanh.subscription.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Current persisted AI plan, Gemini catalog and Credit conversion settings. */
public record AiConfigurationSnapshot(List<Plan> plans, List<Model> models, Credit credit) {
    public AiConfigurationSnapshot {
        plans = List.copyOf(plans);
        models = List.copyOf(models);
    }

    public record Plan(String tier, int priceVnd, long tokenQuota, Long pendingTokenQuota,
            Instant pendingEffectiveAt) { }

    public record Model(String modelId, String displayName, boolean enabled,
            boolean freeAllowed, boolean freeDefault,
            boolean plusAllowed, boolean plusDefault,
            boolean proAllowed, boolean proDefault) { }

    public record Credit(BigDecimal creditsPer1000Vnd, long creditsPer1000OverageTokens,
            int minimumTopUpVnd, int maximumTopUpVnd) { }
}
