package tech.mamxanh.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springdoc.core.customizers.OpenApiCustomizer;

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

    /** OpenAPI explicitly distinguishes optional Bearer from required authentication. */
    @Bean
    OpenApiCustomizer recipeViewOptionalAuthentication() {
        return api -> {
            var path = api.getPaths().get("/api/v1/recipes/{recipeId}/views");
            if (path != null && path.getPost() != null) {
                path.getPost().setSecurity(java.util.List.of(
                        new io.swagger.v3.oas.models.security.SecurityRequirement(),
                        new io.swagger.v3.oas.models.security.SecurityRequirement().addList(BEARER_AUTH)));
            }
        };
    }
}
