package tech.mamxanh.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tech.mamxanh.auth.dto.request.PasswordResetConfirmRequest;
import tech.mamxanh.auth.service.PasswordResetEmailRequested;

/** FR-03-E (#9): reset tokens, passwords and addresses never appear in {@code toString()} output. */
class PasswordResetRedactionTest {

    @Test
    void theEmailEventHidesTheAddressAndTheToken() {
        String text = new PasswordResetEmailRequested("an@example.com", "Nguyễn An", "raw-reset-token").toString();

        assertThat(text).isEqualTo("PasswordResetEmailRequested[redacted]");
    }

    @Test
    void theConfirmRequestHidesTheTokenAndThePasswords() {
        String text = new PasswordResetConfirmRequest("raw-reset-token", "MatKhauMoi456", "MatKhauMoi456").toString();

        assertThat(text).isEqualTo("PasswordResetConfirmRequest[redacted]");
    }
}
