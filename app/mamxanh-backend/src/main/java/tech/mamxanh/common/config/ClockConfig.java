package tech.mamxanh.common.config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single UTC time source. Services take the time from this {@link Clock} instead of calling
 * {@code now()} directly so expiry and cooldown rules can be tested deterministically.
 * Timestamps are stored in DATETIME2 columns as UTC.
 */
@Configuration
public class ClockConfig {
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Bean Validation date rules such as {@code @PastOrPresent} judge "today" by this clock in Vietnam time,
     * the project time zone, not by the server zone (BUG-059: a container in UTC is still on the previous day
     * from 0:00 to 6:59 in Vietnam). Hibernate Validator keeps the clock it receives, so the clock must read
     * the current instant on every call instead of being a fixed copy.
     */
    @Bean
    ValidationConfigurationCustomizer vietnamValidationClock(Clock clock) {
        Clock vietnam = new ZonedClock(clock, VIETNAM);
        return configuration -> configuration.clockProvider(() -> vietnam);
    }

    /** Reads the instant of another clock and reports it in a fixed zone. */
    static final class ZonedClock extends Clock {
        private final Clock source;
        private final ZoneId zone;

        ZonedClock(Clock source, ZoneId zone) {
            this.source = source;
            this.zone = zone;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId other) {
            return new ZonedClock(source, other);
        }

        @Override
        public Instant instant() {
            return source.instant();
        }
    }
}
