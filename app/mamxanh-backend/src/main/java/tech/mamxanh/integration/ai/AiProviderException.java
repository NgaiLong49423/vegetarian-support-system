package tech.mamxanh.integration.ai;

import com.google.genai.errors.ApiException;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

public class AiProviderException extends RuntimeException {
    public enum FailureKind { CONFIGURATION, TIMEOUT, HTTP, TRANSPORT, INVALID_RESPONSE }

    private final FailureKind kind;
    private final Integer httpStatus;

    public AiProviderException(String message) { this(message, null); }

    public AiProviderException(String message, Throwable cause) {
        this(message, cause, classify(cause), extractHttpStatus(cause));
    }

    public AiProviderException(String message, Throwable cause, FailureKind kind, Integer httpStatus) {
        super(message, cause);
        this.kind = kind;
        this.httpStatus = httpStatus;
    }

    public static AiProviderException invalidResponse(String message) {
        return new AiProviderException(message, null, FailureKind.INVALID_RESPONSE, null);
    }

    public FailureKind kind() { return kind; }

    public Integer httpStatus() { return httpStatus; }

    public String diagnostic() {
        if (getCause() instanceof ApiException api) return "HTTP " + api.code();
        if (httpStatus != null) return "HTTP " + httpStatus;
        if (getCause() == null) return "invalid-response-or-configuration";
        Throwable cause = getCause();
        StringBuilder names = new StringBuilder(cause.getClass().getSimpleName());
        for (int i = 0; i < 5 && cause.getCause() != null && cause.getCause() != cause; i++) {
            cause = cause.getCause();
            names.append('/').append(cause.getClass().getSimpleName());
        }
        return names.toString();
    }

    private static FailureKind classify(Throwable cause) {
        if (cause == null) return FailureKind.CONFIGURATION;
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof TimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof InterruptedIOException) {
                return FailureKind.TIMEOUT;
            }
        }
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof ApiException) {
                return FailureKind.HTTP;
            }
            if (current instanceof IOException || current instanceof InterruptedException) {
                return FailureKind.TRANSPORT;
            }
        }
        return FailureKind.TRANSPORT;
    }

    private static Integer extractHttpStatus(Throwable cause) {
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof ApiException api) {
                return api.code();
            }
        }
        return null;
    }
}
