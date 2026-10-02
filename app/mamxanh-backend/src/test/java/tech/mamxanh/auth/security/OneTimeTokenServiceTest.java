package tech.mamxanh.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tech.mamxanh.auth.security.OneTimeTokenService.IssuedToken;

class OneTimeTokenServiceTest {

    private final OneTimeTokenService service = new OneTimeTokenService();

    @Test
    void issuesUrlSafe256BitTokensAndStoresOnlyTheirSha256Digest() {
        IssuedToken token = service.issue();

        assertThat(token.raw()).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(token.hash()).hasSize(64).matches("[0-9a-f]+").isNotEqualTo(token.raw());
        assertThat(service.hash(token.raw())).isEqualTo(token.hash());
    }

    @Test
    void issuesDifferentTokensEachTime() {
        assertThat(service.issue().raw()).isNotEqualTo(service.issue().raw());
    }

    @Test
    void hashMatchesKnownSha256Vector() {
        assertThat(service.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void toStringDoesNotExposeTheToken() {
        IssuedToken token = service.issue();
        assertThat(token.toString()).doesNotContain(token.raw()).doesNotContain(token.hash());
    }
}
