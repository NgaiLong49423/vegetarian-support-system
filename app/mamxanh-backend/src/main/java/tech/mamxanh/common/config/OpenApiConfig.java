package tech.mamxanh.common.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * Security metadata of the generated OpenAPI ({@code /v3/api-docs}). Controllers whose endpoints
 * need a signed-in account declare {@code @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)},
 * so Scalar and generated clients send the access token from {@code POST /api/v1/auth/login} as
 * {@code Authorization: Bearer <token>}. Runtime access rules stay in {@link SecurityConfig}.
 * <p>
 * Every error response is the {@code application/problem+json} body of {@code GlobalExceptionHandler}
 * (docs/api/API.md section 4). Errors every operation of a kind can return are added here: {@code 400}
 * for operations taking input, {@code 401} and {@code 403} for operations needing a session.
 * Operation-specific errors are declared on the controller.
 */
@Configuration
@SecurityScheme(name = OpenApiConfig.BEARER_AUTH, type = SecuritySchemeType.HTTP, scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";
    @Bean
    OpenApiCustomizer apiContractResponsesAndSecurity() {
        return openApi -> {
            if (openApi.getComponents() == null) openApi.setComponents(new Components());
            openApi.getComponents().addSchemas(PROBLEM_DETAIL, problemDetailSchema());
            if (openApi.getPaths() == null) return;
            var recipeViews = openApi.getPaths().get("/api/v1/recipes/{recipeId}/views");
            if (recipeViews != null && recipeViews.getPost() != null) {
                recipeViews.getPost().setSecurity(java.util.List.of(
                        new io.swagger.v3.oas.models.security.SecurityRequirement(),
                        new io.swagger.v3.oas.models.security.SecurityRequirement().addList(BEARER_AUTH)));
            }
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(OpenApiConfig::addErrorResponses));
        };
    }

    static final String PROBLEM_DETAIL = "ProblemDetail";
    private static final String PROBLEM_JSON = "application/problem+json";

    private static void addErrorResponses(Operation operation) {
        ApiResponses responses = operation.getResponses();
        if (responses == null) {
            responses = new ApiResponses();
            operation.setResponses(responses);
        }
        boolean takesInput = operation.getRequestBody() != null
                || (operation.getParameters() != null && !operation.getParameters().isEmpty());
        boolean needsSession = operation.getSecurity() != null
                && operation.getSecurity().stream().anyMatch(requirement -> requirement.containsKey(BEARER_AUTH));
        if (takesInput) {
            responses.putIfAbsent("400", new ApiResponse()
                    .description("VALIDATION_FAILED: malformed body or a field/parameter outside its rules; details in `errors`."));
        }
        if (needsSession) {
            responses.putIfAbsent("401", new ApiResponse()
                    .description("UNAUTHENTICATED: missing, invalid or expired access token."));
            responses.putIfAbsent("403", new ApiResponse()
                    .description("ACCOUNT_LOCKED for a locked account, or the role may not use this operation."));
        }
        responses.forEach((code, response) -> {
            if (code.startsWith("4") || code.startsWith("5")) {
                response.setContent(new Content().addMediaType(PROBLEM_JSON,
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_DETAIL))));
            }
        });
    }

    private static Schema<?> problemDetailSchema() {
        ObjectSchema fieldError = new ObjectSchema();
        fieldError.addProperty("field", new StringSchema());
        fieldError.addProperty("message", new StringSchema());
        ObjectSchema problem = new ObjectSchema();
        problem.description("RFC 9457 problem detail; clients branch on `status` and `code`, never on `detail` text.");
        problem.addProperty("type", new StringSchema());
        problem.addProperty("title", new StringSchema());
        problem.addProperty("status", new IntegerSchema());
        problem.addProperty("detail", new StringSchema());
        problem.addProperty("instance", new StringSchema());
        problem.addProperty("code", new StringSchema().description("Stable `ErrorCode` value."));
        problem.addProperty("errors", new ArraySchema().items(fieldError));
        return problem;
    }
}
