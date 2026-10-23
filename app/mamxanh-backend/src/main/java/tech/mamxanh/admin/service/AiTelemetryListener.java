package tech.mamxanh.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import jakarta.annotation.PreDestroy;
import tech.mamxanh.integration.ai.AiUsageEvent;

/** Bounded background recording avoids nested-transaction connection starvation.
 * This executor is owned here, not a bean: Boot's existing email executor remains unchanged.
 */
@Component
public class AiTelemetryListener {
    private static final Logger LOG = LoggerFactory.getLogger(AiTelemetryListener.class);
    private final AiTelemetryWriter writer;
    private final ThreadPoolTaskExecutor executor;
    public AiTelemetryListener(AiTelemetryWriter writer) {
        this.writer = writer;
        this.executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ai-telemetry-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
    }
    @EventListener
    public void onUsage(AiUsageEvent event) {
        try { executor.execute(() -> persist(event)); }
        catch (RuntimeException failure) {
            LOG.error("AI telemetry enqueue failed category={}", failure.getClass().getSimpleName());
        }
    }
    private void persist(AiUsageEvent event) {
        try { writer.record(event); }
        catch (RuntimeException failure) {
            // Never log event.toString(), exception messages or provider payloads.
            LOG.error("AI telemetry persistence failed category={}", failure.getClass().getSimpleName());
        }
    }
    @PreDestroy
    public void close() { executor.shutdown(); }
}
