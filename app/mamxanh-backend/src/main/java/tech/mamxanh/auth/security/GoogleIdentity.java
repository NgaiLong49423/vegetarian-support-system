package tech.mamxanh.auth.security;

/**
 * Claims taken from a Google ID Token after its signature, expiry, issuer and audience were
 * verified (FR-03-D). Values from the client request itself are never trusted.
 *
 * @param subject       Google account ID ({@code sub}), stored as {@code USER.google_subject}
 * @param email         email claim as sent by Google, not yet normalized
 * @param emailVerified Google's {@code email_verified} claim (Q40 rejects {@code false})
 * @param name          display name claim, may be {@code null}
 * @param pictureUrl    avatar URL claim, may be {@code null}
 */
public record GoogleIdentity(String subject, String email, boolean emailVerified, String name, String pictureUrl) {

    /** Keeps personal data out of logs. */
    @Override
    public String toString() {
        return "GoogleIdentity[redacted]";
    }
}
