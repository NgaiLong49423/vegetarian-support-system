package tech.mamxanh.common.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

/**
 * Security metadata of the generated OpenAPI ({@code /v3/api-docs}). Controllers whose endpoints
 * need a signed-in account declare {@code @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)},
 * so Scalar and generated clients send the access token from {@code POST /api/v1/auth/login} as
 * {@code Authorization: Bearer <token>}. Runtime access rules stay in {@link SecurityConfig}.
 */
@Configuration
@SecurityScheme(name = OpenApiConfig.BEARER_AUTH, type = SecuritySchemeType.HTTP, scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
}
