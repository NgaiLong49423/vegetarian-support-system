package tech.mamxanh.admin.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import tech.mamxanh.integration.ai.AiUsageEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Bounded background recording avoids nested-transaction connection starvation.
 * This executor is owned here, not a bean: Boot's existing email executor remains unchanged.
 */
@Component
public class AiTelemetryListener {
    private static final Logger LOG = LoggerFactory.getLogger(AiTelemetryListener.class);
    private final AiTelemetryWriter writer;
    private final AiTelemetrySpool spool;
    private final Set<UUID> queued = ConcurrentHashMap.newKeySet();
    private final ThreadPoolTaskExecutor executor;
    @org.springframework.beans.factory.annotation.Autowired
    public AiTelemetryListener(AiTelemetryWriter writer, AiTelemetrySpool spool) {
        this(writer, spool, 500);
    }
    AiTelemetryListener(AiTelemetryWriter writer, AiTelemetrySpool spool, int queueCapacity) {
        this.writer = writer;
        this.spool = spool;
        this.executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ai-telemetry-");
        executor.setDaemon(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
    }
    @EventListener
    public void onUsage(AiUsageEvent event) {
        spool.store(event);
        enqueue(event);
    }
    private void enqueue(AiUsageEvent event) {
        if (!queued.add(event.eventId())) return;
        try { executor.execute(() -> persist(event)); }
        catch (RuntimeException failure) {
            queued.remove(event.eventId());
            LOG.error("AI telemetry enqueue failed category={}", failure.getClass().getSimpleName());
        }
    }
    @Scheduled(fixedDelay = 5_000, initialDelay = 5_000)
    @EventListener(ApplicationReadyEvent.class)
    public void replay() {
        try { spool.pending(500).forEach(this::enqueue); }
        catch (java.io.IOException failure) {
            LOG.error("AI telemetry replay failed category={}", failure.getClass().getSimpleName());
        }
    }
    private void persist(AiUsageEvent event) {
        try {
            // A replay scan can finish after another worker has already acknowledged its file.
            if (!spool.isPending(event.eventId())) return;
            writer.record(event);
            spool.acknowledge(event.eventId());
        } catch (RuntimeException | java.io.IOException failure) {
            // Never log event.toString(), exception messages or provider payloads.
            LOG.error("AI telemetry persistence failed category={}", failure.getClass().getSimpleName());
        } finally {
            queued.remove(event.eventId());
        }
    }
    @PreDestroy
    public void close() {
        var pool = executor.getThreadPoolExecutor();
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) pool.shutdownNow();
        } catch (InterruptedException interrupted) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
