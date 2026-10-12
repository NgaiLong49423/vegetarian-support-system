package tech.mamxanh.common.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Stateless REST security baseline (ARCHITECTURE section 5.1, decisions Q19/Q20/Q22): no server
 * session, no cookies carrying authentication, so CSRF protection is disabled. Protected requests
 * carry a JWT in {@code Authorization: Bearer}, validated by the OAuth2 Resource Server; the JWT
 * converter also enforces {@code USER.account_status}. Public endpoints
 * are listed explicitly; everything else requires authentication. Authentication and
 * authorization failures are delegated to {@code GlobalExceptionHandler} so they share the
 * ProblemDetail format.
 */
@Configuration
public class SecurityConfig {

    private static final String[] PUBLIC_AUTH_POST_ENDPOINTS = {
            "/api/v1/auth/register",
            "/api/v1/auth/email-verifications",
            "/api/v1/auth/email-verifications/resend",
            "/api/v1/auth/login",
            "/api/v1/auth/google",
            "/api/v1/auth/password-resets",
            "/api/v1/auth/password-resets/confirm",
    };

    private static final String[] API_DOCS_ENDPOINTS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/scalar",
            "/scalar/**",
    };

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter) throws Exception {
        AuthenticationEntryPoint problemEntryPoint = (request, response, ex) ->
                exceptionResolver.resolveException(request, response, null, ex);
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_POST_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/recipes/mine").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/recipes/*/manage").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/recipes", "/api/v1/recipes/*", "/api/v1/recipes/*/media").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/recipes/form-options",
                                "/api/v1/recipes/ingredient-options").permitAll()
                        .requestMatchers(API_DOCS_ENDPOINTS).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(problemEntryPoint)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemEntryPoint)
                        .accessDeniedHandler((request, response, ex) ->
                                exceptionResolver.resolveException(request, response, null, ex)));
        return http.build();
    }
}
