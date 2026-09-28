package tech.mamxanh.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS allowlist for the browser client (docs/api/API.md section 6). Origins come from
 * {@code MAMXANH_CORS_ALLOWED_ORIGINS}; wildcards are not used. Authentication travels in the
 * {@code Authorization} header, so credentials (cookies) are not allowed.
 */
@Configuration
@EnableConfigurationProperties(CorsConfig.CorsProperties.class)
public class CorsConfig {

    @ConfigurationProperties(prefix = "mamxanh.cors")
    public record CorsProperties(List<String> allowedOrigins) {
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(properties.allowedOrigins() == null ? List.of() : properties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        configuration.setExposedHeaders(List.of(HttpHeaders.RETRY_AFTER));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
