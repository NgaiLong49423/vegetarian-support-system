package tech.mamxanh.common.exception;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converts every error into {@code application/problem+json} with a stable {@code code}
 * (decision Q14; docs/api/API.md section 4). Field validation errors are listed in
 * {@code errors} as {@code {field, message}} pairs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record FieldErrorDetail(String field, String message) {
    }

    @ExceptionHandler(AppException.class)
    ResponseEntity<Object> handleAppException(AppException ex, WebRequest request) {
        ProblemDetail problem = problem(ex.errorCode(), ex.getMessage(), request);
        HttpHeaders headers = new HttpHeaders();
        if (ex.retryAfter() != null) {
            long seconds = Math.max(1, (ex.retryAfter().toMillis() + 999) / 1000);
            headers.set(HttpHeaders.RETRY_AFTER, Long.toString(seconds));
        }
        return ResponseEntity.status(ex.errorCode().status()).headers(headers).body(problem);
    }

    /** Valid token, but {@code USER.account_status = LOCKED} (decision Q20, NFR-09). */
    @ExceptionHandler(LockedException.class)
    ResponseEntity<Object> handleLocked(LockedException ex, WebRequest request) {
        return build(ErrorCode.ACCOUNT_LOCKED, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> handleAuthentication(AuthenticationException ex, WebRequest request) {
        return build(ErrorCode.UNAUTHENTICATED, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return build(ErrorCode.ACCESS_DENIED, request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception while processing {}", requestPath(request), ex);
        return build(ErrorCode.INTERNAL_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        ProblemDetail problem = problem(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.defaultDetail(),
                request);
        problem.setProperty("errors", errors);
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status()).headers(headers).body(problem);
    }

    /**
     * Framework-generated problems (malformed JSON, unsupported method, unknown route...) get the
     * same shape: a stable {@code code} plus generic text that does not echo internal details.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        ErrorCode code = ErrorCode.fromStatus(statusCode.value());
        ProblemDetail problem = ProblemDetail.forStatus(statusCode);
        problem.setTitle(code.title());
        problem.setDetail(code.defaultDetail());
        problem.setInstance(URI.create(requestPath(request)));
        problem.setProperty("code", code.name());
        return ResponseEntity.status(statusCode).headers(headers).body(problem);
    }

    private ResponseEntity<Object> build(ErrorCode code, WebRequest request) {
        return ResponseEntity.status(code.status()).body(problem(code, code.defaultDetail(), request));
    }

    private static ProblemDetail problem(ErrorCode code, String detail, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setTitle(code.title());
        problem.setInstance(URI.create(requestPath(request)));
        problem.setProperty("code", code.name());
        return problem;
    }

    private static String requestPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return servletRequest.getRequest().getRequestURI();
        }
        return "";
    }
}
