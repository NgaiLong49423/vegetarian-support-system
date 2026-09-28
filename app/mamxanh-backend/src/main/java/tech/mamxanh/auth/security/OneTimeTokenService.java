package tech.mamxanh.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * Issues single-use tokens for email links (verification, password reset). The raw token is
 * 256 bits from {@link SecureRandom}, encoded as URL-safe Base64; only its SHA-256 hex digest is
 * persisted, so a database leak does not reveal usable links.
 */
@Component
public class OneTimeTokenService {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public IssuedToken issue() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new IssuedToken(raw, hash(raw));
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /** Raw token for the email link plus the digest to store. */
    public record IssuedToken(String raw, String hash) {

        @Override
        public String toString() {
            return "IssuedToken[redacted]";
        }
    }
}
