package tech.mamxanh.auth.security;

import java.util.Optional;

/** Verifies a Google ID Token sent by the Frontend after Google Identity Services sign-in (FR-03-D). */
public interface GoogleTokenVerifier {

    /**
     * @return the verified claims, or empty when the token is malformed, expired, issued for another
     *         client or issuer, not signed by Google, or lacks the subject/email claims
     * @throws GoogleVerificationUnavailableException when Google's signing certificates cannot be loaded
     */
    Optional<GoogleIdentity> verify(String idToken);
}
