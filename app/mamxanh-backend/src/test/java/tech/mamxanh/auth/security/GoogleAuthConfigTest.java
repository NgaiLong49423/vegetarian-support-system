package tech.mamxanh.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** FR-03-D (#8): Google login needs the application's Google client ID ({@code MAMXANH_GOOGLE_CLIENT_ID}). */
class GoogleAuthConfigTest {

    private final GoogleAuthConfig config = new GoogleAuthConfig();

    @Test
    void withoutAClientIdEveryTokenIsRejectedWithoutCallingGoogle() {
        GoogleTokenVerifier verifier = config.googleTokenVerifier(new GoogleAuthProperties("  "));

        assertThat(verifier.verify("header.payload.signature")).isEmpty();
    }

    @Test
    void withAClientIdTokensAreVerifiedByGoogleApiClient() {
        GoogleTokenVerifier verifier = config.googleTokenVerifier(
                new GoogleAuthProperties("mamxanh.apps.googleusercontent.com"));

        assertThat(verifier).isInstanceOf(GoogleIdTokenVerifierAdapter.class);
    }
}
