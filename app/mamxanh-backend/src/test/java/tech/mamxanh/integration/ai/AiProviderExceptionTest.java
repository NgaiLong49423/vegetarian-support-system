package tech.mamxanh.integration.ai;

import com.google.genai.errors.ApiException;
import org.junit.jupiter.api.Test;
import tech.mamxanh.integration.ai.AiProviderException.FailureKind;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderExceptionTest {

    @Test
    void diagnosticsContainOnlyErrorTypesOrStatusAndNeverProviderMessages() {
        // NFR safety: Exception messages might contain confidential credentials, private prompts, or response bodies.
        // Diagnostic string MUST contain only HTTP status or class names, never exception message content.
        AiProviderException apiException = new AiProviderException("Secret token: AIzaSyD-12345 in prompt",
                new ApiException(403, "PERMISSION_DENIED", "API key not valid. Please pass a valid API key."));

        assertThat(apiException.diagnostic()).isEqualTo("HTTP 403");
        assertThat(apiException.diagnostic()).doesNotContain("AIzaSyD", "Secret token", "API key not valid");
        assertThat(apiException.kind()).isEqualTo(FailureKind.HTTP);
        assertThat(apiException.httpStatus()).isEqualTo(403);
    }

    @Test
    void diagnosticForChainedCausesShowsClassHierarchyWithoutMessages() {
        IOException cause = new IOException("Connection refused to secret-host.internal:8080",
                new TimeoutException("Timeout connecting to secret-host"));
        AiProviderException ex = new AiProviderException("Outermost message", cause);

        assertThat(ex.diagnostic()).isEqualTo("IOException/TimeoutException");
        assertThat(ex.diagnostic()).doesNotContain("secret-host", "8080");
        assertThat(ex.kind()).isEqualTo(FailureKind.TIMEOUT);
    }

    @Test
    void diagnosticForNullCauseIndicatesInvalidResponseOrConfiguration() {
        AiProviderException ex = AiProviderException.invalidResponse("Incomplete JSON received");

        assertThat(ex.diagnostic()).isEqualTo("invalid-response-or-configuration");
        assertThat(ex.kind()).isEqualTo(FailureKind.INVALID_RESPONSE);
        assertThat(ex.httpStatus()).isNull();
    }

    @Test
    void classifiesTimeoutExceptionsCorrectly() {
        AiProviderException timeout = new AiProviderException("Timeout", new TimeoutException());
        assertThat(timeout.kind()).isEqualTo(FailureKind.TIMEOUT);

        AiProviderException httpTimeout = new AiProviderException("HTTP Timeout", new HttpTimeoutException("read timeout"));
        assertThat(httpTimeout.kind()).isEqualTo(FailureKind.TIMEOUT);

        AiProviderException interruptedIo = new AiProviderException("Interrupted IO", new InterruptedIOException());
        assertThat(interruptedIo.kind()).isEqualTo(FailureKind.TIMEOUT);
    }

    @Test
    void classifiesTransportAndConfigurationCorrectly() {
        AiProviderException transport = new AiProviderException("IO Error", new IOException("broken pipe"));
        assertThat(transport.kind()).isEqualTo(FailureKind.TRANSPORT);

        AiProviderException config = new AiProviderException("Not configured");
        assertThat(config.kind()).isEqualTo(FailureKind.CONFIGURATION);
    }
}
