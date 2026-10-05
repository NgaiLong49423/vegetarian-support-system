package tech.mamxanh.auth.dto.response;

/** openapi.yaml AuthResponse: a Stateless JWT access token in the JSON body, no refresh token or cookie. */
public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds, AccountSummary account) {

    public static final String BEARER = "Bearer";

    @Override
    public String toString() {
        return "AuthResponse[accessToken=redacted, tokenType=" + tokenType + ", expiresInSeconds="
                + expiresInSeconds + ", account=" + account.id() + "]";
    }
}
