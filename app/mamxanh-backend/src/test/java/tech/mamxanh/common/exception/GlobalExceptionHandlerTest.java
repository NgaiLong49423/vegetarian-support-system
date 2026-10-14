package tech.mamxanh.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import tech.mamxanh.nutrition.service.NutritionProfileException;

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
    void nutritionEligibilityErrorsKeepTheirBusinessStatusAndCodeOutsideNutritionController() {
        ResponseEntity<Object> response = handler.handleNutritionProfileException(
                new NutritionProfileException(HttpStatus.FORBIDDEN,
                        "NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED",
                        "Cần xác nhận phạm vi hỗ trợ dinh dưỡng trước khi sử dụng chức năng này."),
                new ServletWebRequest(new MockHttpServletRequest("POST", "/api/v1/meal-plans/ai-menu/previews")));

        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(problem.getProperties()).containsEntry("code", "NUTRITION_ELIGIBILITY_CONFIRMATION_REQUIRED");
        assertThat(problem.getDetail()).isEqualTo("Cần xác nhận phạm vi hỗ trợ dinh dưỡng trước khi sử dụng chức năng này.");
        assertThat(problem.getInstance()).hasToString("/api/v1/meal-plans/ai-menu/previews");
    }

    @Test
    void uploadsAboveTheServletMultipartLimitGetTheFileTooLargeCode() throws Exception {
        ResponseEntity<Object> response = handler.handleException(new MaxUploadSizeExceededException(5L * 1024 * 1024),
                new ServletWebRequest(new MockHttpServletRequest("POST", "/api/v1/me/profile/avatar")));

        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(response.getStatusCode().value()).isEqualTo(413);
        assertThat(problem.getProperties()).containsEntry("code", "FILE_TOO_LARGE");
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

    @Test
    void constraintViolationsUseTheValidationProblemResponse() {
        ResponseEntity<Object> response = handler.handleConstraintViolation(
                new ConstraintViolationException("invalid request parameter", Set.of()),
                new ServletWebRequest(new MockHttpServletRequest("GET", "/api/v1/recipes")));

        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(problem.getProperties()).containsEntry("code", "VALIDATION_FAILED");
        assertThat(problem.getInstance()).hasToString("/api/v1/recipes");
        assertThat(problem.getProperties()).containsKey("errors");
    }
}
