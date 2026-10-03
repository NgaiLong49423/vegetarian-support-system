package tech.mamxanh.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables {@code @Async} so slow side effects such as email delivery (ARCHITECTURE section 4.8)
 * do not block the HTTP request thread. Uses Spring Boot's auto-configured task executor.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
