package tech.mamxanh.auth.service;

/** Published inside the reset-request transaction; the email is sent only after commit. */
public record PasswordResetEmailRequested(String email, String displayName, String rawToken) {

    @Override
    public String toString() {
        return "PasswordResetEmailRequested[redacted]";
    }
}
