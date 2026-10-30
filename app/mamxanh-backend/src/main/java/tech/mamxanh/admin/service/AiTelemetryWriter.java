package tech.mamxanh.admin.service;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.admin.repository.AiTelemetryRepository;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.AiUsageEvent;

@Service
public class AiTelemetryWriter {
    public static final Set<String> FEATURES = Set.of("FR-21", "FR-34", "FR-36", "FR-37", "FR-47", "FR-51");
    private final AiTelemetryRepository repository;
    private final AiTelemetryPricing pricing;
    private final Clock clock;
    public AiTelemetryWriter(AiTelemetryRepository repository, AiTelemetryPricing pricing, Clock clock) {
        this.repository = repository; this.pricing = pricing; this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 2)
    public void record(AiUsageEvent event) {
        if (event.eventId() == null || !FEATURES.contains(event.feature()) || event.usage() == null
                || event.occurredAt() == null || event.occurredAt().isAfter(clock.instant())
                || event.userId() != null && event.userId() <= 0) throw new IllegalArgumentException("Invalid telemetry envelope");
        if (event.occurredAt().isBefore(clock.instant().minus(Duration.ofDays(90)))) return;
        if ((event.userId() == null) == (event.anonymousSessionId() == null)) throw new IllegalArgumentException("Invalid telemetry actor");
        if (event.anonymousSessionId() != null) UUID.fromString(event.anonymousSessionId());
        var raw = event.usage();
        if (!AiTokenUsage.MODEL.equals(raw.model()) || !"STANDARD".equals(raw.serviceTier())) {
            throw new IllegalArgumentException("Unsupported telemetry source");
        }
        String status = status(raw);
        var usage = new AiTokenUsage(raw.model(), raw.serviceTier(), nonnegative(raw.promptTokens()),
                nonnegative(raw.responseTokens()), nonnegative(raw.totalTokens()), optional(raw.thinkingTokens()),
                optional(raw.cachedTokens()), optional(raw.toolUsePromptTokens()));
        repository.insert(event, usage, status, pricing.estimate(usage, event.occurredAt(), status));
    }

    public static String status(AiTokenUsage usage) {
        var values = java.util.Arrays.asList(usage.promptTokens(), usage.responseTokens(), usage.totalTokens(),
                usage.thinkingTokens(), usage.cachedTokens(), usage.toolUsePromptTokens());
        if (values.stream().anyMatch(v -> v != null && v < 0)) return "INVALID";
        if (values.stream().allMatch(java.util.Objects::isNull)) return "MISSING";
        if (usage.promptTokens() == null || usage.responseTokens() == null || usage.totalTokens() == null) return "PARTIAL";
        if (usage.cachedTokens() != null && usage.cachedTokens() > usage.promptTokens()) return "INVALID";
        long known = (long) usage.promptTokens() + usage.responseTokens()
                + nonnegative(usage.thinkingTokens()) + nonnegative(usage.toolUsePromptTokens());
        if (usage.totalTokens() < known || values.stream().noneMatch(java.util.Objects::isNull) && usage.totalTokens() != known) return "INVALID";
        return values.stream().anyMatch(java.util.Objects::isNull) ? "PARTIAL" : "COMPLETE";
    }

    private static int nonnegative(Integer value) { return value == null ? 0 : Math.max(0, value); }
    private static Integer optional(Integer value) { return value == null ? null : Math.max(0, value); }
}
