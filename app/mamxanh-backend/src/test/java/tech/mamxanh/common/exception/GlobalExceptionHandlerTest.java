package tech.mamxanh.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void applicationErrorPropertiesAreAddedToTheProblemBody() {
        AppException blocked = new AppException(ErrorCode.DIETARY_PROFILE_INCOMPLETE)
                .withProperty("missing", List.of("VEGETARIAN_TYPE", "AVOID_INGREDIENTS"));

        ResponseEntity<Object> response = handler.handleAppException(blocked,
                new ServletWebRequest(new MockHttpServletRequest("POST", "/api/v1/ai/suggestions")));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getHeaders().containsHeader(HttpHeaders.RETRY_AFTER)).isFalse();
        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(problem.getProperties())
                .containsEntry("code", "DIETARY_PROFILE_INCOMPLETE")
                .containsEntry("missing", List.of("VEGETARIAN_TYPE", "AVOID_INGREDIENTS"));
        assertThat(problem.getDetail()).startsWith("Bạn cần hoàn tất 3 thông tin cơ bản");
    }

    @Test
    void applicationErrorsWithoutPropertiesKeepTheStandardShape() {
        ResponseEntity<Object> response = handler.handleAppException(new AppException(ErrorCode.MEMBER_ACCESS_REQUIRED),
                new ServletWebRequest(new MockHttpServletRequest("GET", "/api/v1/nutrition/dietary-preferences")));

        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(problem.getProperties()).containsOnlyKeys("code");
        assertThat(problem.getInstance()).hasToString("/api/v1/nutrition/dietary-preferences");
    }

    @Test
    void errorCodeMapsHttpStatusToDeterministicErrorCodes() {
        assertThat(ErrorCode.fromStatus(400)).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(ErrorCode.fromStatus(401)).isEqualTo(ErrorCode.UNAUTHENTICATED);
        assertThat(ErrorCode.fromStatus(403)).isEqualTo(ErrorCode.ACCESS_DENIED);
        assertThat(ErrorCode.fromStatus(404)).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(ErrorCode.fromStatus(405)).isEqualTo(ErrorCode.METHOD_NOT_ALLOWED);
        assertThat(ErrorCode.fromStatus(415)).isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
        assertThat(ErrorCode.fromStatus(500)).isEqualTo(ErrorCode.INTERNAL_ERROR);
    }
}
