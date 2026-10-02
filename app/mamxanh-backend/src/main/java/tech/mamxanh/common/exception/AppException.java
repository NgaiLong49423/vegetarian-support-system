package tech.mamxanh.common.exception;

import java.time.Duration;

/**
 * Business exception translated to a {@code ProblemDetail} by {@link GlobalExceptionHandler}.
 * The message is the user-facing {@code detail}; never put secrets, tokens or passwords in it.
 */
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Duration retryAfter;

    public AppException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultDetail(), null);
    }

    public AppException(ErrorCode errorCode, String detail) {
        this(errorCode, detail, null);
    }

    private AppException(ErrorCode errorCode, String detail, Duration retryAfter) {
        super(detail);
        this.errorCode = errorCode;
        this.retryAfter = retryAfter;
    }

    /** Rate-limit style error; the handler adds a {@code Retry-After} header in whole seconds. */
    public static AppException retryAfter(ErrorCode errorCode, Duration retryAfter) {
        return new AppException(errorCode, errorCode.defaultDetail(), retryAfter);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
