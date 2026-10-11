package tech.mamxanh.auth.security;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

/**
 * Q25: verifies Google ID Tokens with Google API Client's {@link GoogleIdTokenVerifier}, which checks
 * the RS256 signature against Google's published certificates, the expiry, the issuer
 * ({@code accounts.google.com}) and the audience (this application's client ID).
 */
public class GoogleIdTokenVerifierAdapter implements GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifierAdapter(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public Optional<GoogleIdentity> verify(String idToken) {
        GoogleIdToken token;
        try {
            token = GoogleIdToken.parse(verifier.getJsonFactory(), idToken);
        } catch (IOException | IllegalArgumentException malformed) {
            return Optional.empty();
        }
        try {
            if (!verifier.verify(token)) {
                return Optional.empty();
            }
        } catch (GeneralSecurityException invalidSignature) {
            return Optional.empty();
        } catch (IOException certificatesUnavailable) {
            throw new GoogleVerificationUnavailableException(certificatesUnavailable);
        }
        GoogleIdToken.Payload payload = token.getPayload();
        if (payload.getSubject() == null || payload.getEmail() == null) {
            return Optional.empty();
        }
        return Optional.of(new GoogleIdentity(payload.getSubject(), payload.getEmail(),
                Boolean.TRUE.equals(payload.getEmailVerified()), stringClaim(payload, "name"),
                stringClaim(payload, "picture")));
    }

    private static String stringClaim(GoogleIdToken.Payload payload, String name) {
        return payload.get(name) instanceof String value ? value : null;
    }
}
