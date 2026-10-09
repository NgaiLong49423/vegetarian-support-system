package tech.mamxanh.auth.security;

/** Google's signing certificates could not be loaded, so no token can be judged valid or invalid. */
public class GoogleVerificationUnavailableException extends RuntimeException {

    public GoogleVerificationUnavailableException(Throwable cause) {
        super("Google signing certificates are unavailable", cause);
    }
}
