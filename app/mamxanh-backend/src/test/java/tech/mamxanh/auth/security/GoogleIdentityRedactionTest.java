package tech.mamxanh.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tech.mamxanh.auth.dto.request.GoogleLoginRequest;

/** FR-03-D (#8): Google tokens and profile claims never reach logs through {@code toString()}. */
class GoogleIdentityRedactionTest {

    @Test
    void googleIdentityHidesTheSubjectEmailAndName() {
        String text = new GoogleIdentity("google-sub-123", "an.nguyen@gmail.com", true, "Nguyễn Văn An",
                "https://lh3.googleusercontent.com/a/photo").toString();

        assertThat(text).doesNotContain("google-sub-123", "an.nguyen@gmail.com", "Nguyễn Văn An", "lh3");
    }

    @Test
    void googleLoginRequestHidesTheIdToken() {
        assertThat(new GoogleLoginRequest("header.payload.signature").toString()).doesNotContain("header.payload");
    }
}
