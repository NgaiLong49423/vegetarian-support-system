package tech.mamxanh.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Q41: display name of an account created from Google. */
class GoogleDisplayNameTest {

    private static final String FALLBACK = "Thành viên Mâm Xanh";

    @Test
    void trimsTheGoogleName() {
        assertThat(GoogleDisplayName.of("  Nguyễn Văn An  ", "an@gmail.com")).isEqualTo("Nguyễn Văn An");
    }

    @Test
    void cutsANameLongerThanFiftyCharactersToFifty() {
        String name = "A".repeat(49) + "BCD";

        assertThat(GoogleDisplayName.of(name, "an@gmail.com")).isEqualTo("A".repeat(49) + "B");
    }

    @Test
    void cutsByCharacterWithoutSplittingEmoji() {
        String name = "🌿".repeat(51);

        String result = GoogleDisplayName.of(name, "an@gmail.com");

        assertThat(result.codePointCount(0, result.length())).isEqualTo(50);
        assertThat(result).isEqualTo("🌿".repeat(50));
    }

    @Test
    void usesTheEmailLocalPartWhenTheNameIsMissing() {
        assertThat(GoogleDisplayName.of(null, "an.nguyen@gmail.com")).isEqualTo("an.nguyen");
        assertThat(GoogleDisplayName.of("   ", "an.nguyen@gmail.com")).isEqualTo("an.nguyen");
    }

    @Test
    void usesTheEmailLocalPartWhenTheNameIsShorterThanThreeCharacters() {
        assertThat(GoogleDisplayName.of(" A ", "an.nguyen@gmail.com")).isEqualTo("an.nguyen");
    }

    @Test
    void usesTheDefaultNameWhenTheLocalPartIsTooShort() {
        assertThat(GoogleDisplayName.of("A", "ab@gmail.com")).isEqualTo(FALLBACK);
    }

    @Test
    void usesTheDefaultNameWhenTheLocalPartIsLongerThanFiftyCharacters() {
        assertThat(GoogleDisplayName.of(null, "a".repeat(51) + "@gmail.com")).isEqualTo(FALLBACK);
    }

    @Test
    void acceptsALocalPartOfExactlyThreeAndFiftyCharacters() {
        assertThat(GoogleDisplayName.of(null, "abc@gmail.com")).isEqualTo("abc");
        assertThat(GoogleDisplayName.of(null, "b".repeat(50) + "@gmail.com")).isEqualTo("b".repeat(50));
    }
}
