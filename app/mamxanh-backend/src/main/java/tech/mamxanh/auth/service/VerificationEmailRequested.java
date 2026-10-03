package tech.mamxanh.auth.service;

/** Published inside the registration/resend transaction; the email is sent only after commit. */
public record VerificationEmailRequested(String email, String displayName, String rawToken) {

    @Override
    public String toString() {
        return "VerificationEmailRequested[redacted]";
    }
}
