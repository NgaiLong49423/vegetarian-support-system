package tech.mamxanh.common.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single UTC time source. Services take the time from this {@link Clock} instead of calling
 * {@code now()} directly so expiry and cooldown rules can be tested deterministically.
 * Timestamps are stored in DATETIME2 columns as UTC.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
