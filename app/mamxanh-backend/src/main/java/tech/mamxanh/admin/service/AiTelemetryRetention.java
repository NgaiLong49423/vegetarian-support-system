package tech.mamxanh.admin.service;

import java.time.Clock;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tech.mamxanh.admin.repository.AiTelemetryRepository;

@Service
public class AiTelemetryRetention {
    private static final Logger LOG = LoggerFactory.getLogger(AiTelemetryRetention.class);
    private final AiTelemetryRepository repository;
    private final Clock clock;
    public AiTelemetryRetention(AiTelemetryRepository repository, Clock clock) { this.repository = repository; this.clock = clock; }
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    @EventListener(ApplicationReadyEvent.class)
    public void purge() {
        var cutoff = clock.instant().minus(Duration.ofDays(90));
        try {
            int deleted;
            do { deleted = repository.deleteBatchBefore(cutoff); } while (deleted == 1000);
        } catch (RuntimeException failure) {
            LOG.error("AI telemetry retention failed category={}", failure.getClass().getSimpleName());
        }
    }
}
