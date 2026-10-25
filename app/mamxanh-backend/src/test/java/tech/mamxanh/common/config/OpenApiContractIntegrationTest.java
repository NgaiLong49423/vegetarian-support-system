package tech.mamxanh.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import tech.mamxanh.AbstractIntegrationTest;

/**
 * Issue #114: generated {@code /v3/api-docs} is the runtime contract, so its security metadata must
 * match what {@code SecurityConfig} enforces, and protected operations must declare their auth errors.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OpenApiContractIntegrationTest extends AbstractIntegrationTest {
    private static final List<String> METHODS = List.of("get", "post", "put", "patch", "delete");

    private JsonNode paths;

    @BeforeAll
    void loadContract() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        paths = new ObjectMapper().readTree(json).path("paths");
    }

    /** An operation the security filter rejects without a token must say so, and a public one must not. */
    @Test
    void everyOperationDeclaresBearerAuthExactlyWhenTheRuntimeRequiresASession() throws Exception {
        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, JsonNode> path : paths.properties()) {
            for (String method : METHODS) {
                JsonNode operation = path.getValue().get(method);
                if (operation == null) continue;
                String uri = path.getKey().replaceAll("\\{[^}]+}", "1");
                var builder = request(HttpMethod.valueOf(method.toUpperCase()), uri);
                if (operation.has("requestBody")) builder.contentType(MediaType.APPLICATION_JSON).content("{}");
                int status = mockMvc.perform(builder).andReturn().getResponse().getStatus();
                JsonNode security = operation.path("security");
                // Security alternatives are OR-ed: {} permits an anonymous request even when
                // another alternative supports Bearer (FR-58 records Guest and Member views).
                boolean anonymousAllowed = false;
                for (JsonNode alternative : security) {
                    if (alternative.isObject() && alternative.size() == 0) anonymousAllowed = true;
                }
                boolean documented = security.toString().contains("bearerAuth") && !anonymousAllowed;
                if ((status == 401) != documented) {
                    mismatches.add(method.toUpperCase() + " " + path.getKey() + " (no token → " + status
                            + ", bearerAuth documented: " + documented + ")");
                }
            }
        }
        assertThat(mismatches).isEmpty();
    }

    /** docs/api/API.md section 2 publishes the contract as JSON and YAML; both must be public like Scalar. */
    @Test
    void theYamlContractIsPublicLikeTheJsonContract() throws Exception {
        int status = mockMvc.perform(get("/v3/api-docs.yaml")).andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(200);
    }

    @Test
    void everyProtectedOperationDeclaresUnauthenticatedAndForbiddenAsProblemDetails() {
        List<String> missing = new ArrayList<>();
        forEachOperation((label, operation) -> {
            if (!operation.path("security").toString().contains("bearerAuth")) return;
            for (String code : List.of("401", "403")) {
                JsonNode response = operation.path("responses").path(code);
                if (!response.path("content").has("application/problem+json")) missing.add(label + " " + code);
            }
        });
        assertThat(missing).isEmpty();
    }

    @Test
    void operationsTakingInputDeclareValidationFailures() {
        List<String> missing = new ArrayList<>();
        forEachOperation((label, operation) -> {
            boolean takesInput = operation.has("requestBody") || operation.path("parameters").size() > 0;
            if (takesInput && !operation.path("responses").has("400")) missing.add(label);
        });
        assertThat(missing).isEmpty();
    }

    @Test
    void repairedOperationsDeclareTheErrorsTheirServicesReturn() {
        assertResponses("post", "/api/v1/recipes/media/upload", "201", "503");
        assertResponses("post", "/api/v1/me/profile/avatar", "200", "503");
        assertResponses("get", "/api/v1/admin/ingredients/{ingredientId}", "404");
        assertResponses("post", "/api/v1/admin/ingredients", "409");
        assertResponses("post", "/api/v1/admin/units", "409");
        assertResponses("post", "/api/v1/admin/ingredients/{ingredientId}/unit-conversions/{unitId}", "404", "409");
        assertResponses("delete", "/api/v1/admin/ingredients/{ingredientId}", "409");
        assertResponses("post", "/api/v1/expert-applications", "409");
        assertResponses("get", "/api/v1/admin/expert-applications/{id}", "404");
        assertResponses("post", "/api/v1/admin/expert-applications/{id}/approve", "409");
        assertResponses("post", "/api/v1/subscriptions/checkout", "201");
        assertResponses("post", "/api/v1/auth/register", "201", "409");
        assertResponses("post", "/api/v1/auth/login", "401", "403", "429");
        assertResponses("post", "/api/v1/auth/google", "401", "403", "409", "503");
        assertResponses("post", "/api/v1/auth/email-verifications/resend", "429");
        assertResponses("post", "/api/v1/auth/password-resets/confirm", "403");
    }

    private void assertResponses(String method, String path, String... codes) {
        JsonNode responses = paths.path(path).path(method).path("responses");
        assertThat(responses.isMissingNode()).as(method + " " + path).isFalse();
        for (String code : codes) {
            assertThat(responses.has(code)).as(method.toUpperCase() + " " + path + " declares " + code).isTrue();
        }
    }

    private void forEachOperation(java.util.function.BiConsumer<String, JsonNode> action) {
        for (Map.Entry<String, JsonNode> path : paths.properties()) {
            for (String method : METHODS) {
                JsonNode operation = path.getValue().get(method);
                if (operation != null) action.accept(method.toUpperCase() + " " + path.getKey(), operation);
            }
        }
    }
}
