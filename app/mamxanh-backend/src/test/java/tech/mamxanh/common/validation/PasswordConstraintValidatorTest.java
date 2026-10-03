package tech.mamxanh.common.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordConstraintValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = { "MatKhau123", "Abcdefg1", "Đăngký2026", "Pass word 9!" })
    void acceptsPasswordsMeetingEveryCriterion(String password) {
        assertThat(PasswordConstraintValidator.problems(password)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "Abcde1,           ít nhất 8 ký tự",
            "alllowercase1,    chữ in hoa",
            "ALLUPPERCASE1,    chữ thường",
            "NoDigitsHere,     chữ số",
    })
    void reportsTheMissingCriterion(String password, String expectedFragment) {
        assertThat(PasswordConstraintValidator.problems(password))
                .singleElement()
                .asString()
                .contains(expectedFragment);
    }

    @Test
    void reportsEveryMissingCriterionSeparately() {
        assertThat(PasswordConstraintValidator.problems("abc")).hasSize(3)
                .anySatisfy(message -> assertThat(message).contains("ít nhất 8 ký tự"))
                .anySatisfy(message -> assertThat(message).contains("chữ in hoa"))
                .anySatisfy(message -> assertThat(message).contains("chữ số"));
    }

    @Test
    void rejectsMissingPassword() {
        assertThat(PasswordConstraintValidator.problems(null)).containsExactly("Vui lòng nhập mật khẩu.");
        assertThat(PasswordConstraintValidator.problems("")).containsExactly("Vui lòng nhập mật khẩu.");
    }

    @Test
    void acceptsExactly64AsciiCharactersAndRejects65() {
        String sixtyFour = "Aa1" + "x".repeat(61);
        assertThat(sixtyFour).hasSize(64);
        assertThat(PasswordConstraintValidator.problems(sixtyFour)).isEmpty();
        assertThat(PasswordConstraintValidator.problems(sixtyFour + "x"))
                .singleElement().asString().contains("tối đa 64 ký tự");
    }

    @Test
    void rejectsPasswordsLongerThan72Utf8BytesEvenWhenWithin64Characters() {
        String accented = "Aa1" + "ệ".repeat(30); // 33 characters, 3 + 90 = 93 bytes
        assertThat(PasswordConstraintValidator.problems(accented))
                .singleElement().asString().contains("72 byte");
    }
}
