package tech.mamxanh.common.exception;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Business exception translated to a {@code ProblemDetail} by {@link GlobalExceptionHandler}.
 * The message is the user-facing {@code detail}; never put secrets, tokens or passwords in it.
 */
public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Duration retryAfter;
    private final Map<String, Object> properties = new LinkedHashMap<>();

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

    /** Rate-limit style error with a specific user-facing detail. */
    public static AppException retryAfter(ErrorCode errorCode, String detail, Duration retryAfter) {
        return new AppException(errorCode, detail, retryAfter);
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public Duration retryAfter() {
        return retryAfter;
    }

    /**
     * Adds a machine-readable member to the problem body, for example the list of missing fields.
     * Values must be safe to show to the client.
     */
    public AppException withProperty(String name, Object value) {
        properties.put(name, value);
        return this;
    }

    public Map<String, Object> properties() {
        return Collections.unmodifiableMap(properties);
    }
}
