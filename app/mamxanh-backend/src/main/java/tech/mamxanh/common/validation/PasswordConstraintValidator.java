package tech.mamxanh.common.validation;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordConstraintValidator implements ConstraintValidator<ValidPassword, String> {

    static final int MIN_LENGTH = 8;
    static final int MAX_LENGTH = 64;
    static final int MAX_UTF8_BYTES = 72;

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        List<String> problems = problems(password);
        if (problems.isEmpty()) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        problems.forEach(message -> context.buildConstraintViolationWithTemplate(message).addConstraintViolation());
        return false;
    }

    /** Returns one message per unmet criterion; empty when the password is acceptable. */
    static List<String> problems(String password) {
        List<String> problems = new ArrayList<>();
        if (password == null || password.isEmpty()) {
            problems.add("Vui lòng nhập mật khẩu.");
            return problems;
        }
        int length = password.codePointCount(0, password.length());
        if (length < MIN_LENGTH) {
            problems.add("Mật khẩu cần ít nhất 8 ký tự.");
        }
        if (length > MAX_LENGTH) {
            problems.add("Mật khẩu tối đa 64 ký tự.");
        } else if (password.getBytes(StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) {
            problems.add("Mật khẩu vượt quá 72 byte; ký tự có dấu chiếm nhiều byte hơn, hãy rút ngắn mật khẩu.");
        }
        if (password.codePoints().noneMatch(Character::isUpperCase)) {
            problems.add("Mật khẩu cần ít nhất 1 chữ in hoa.");
        }
        if (password.codePoints().noneMatch(Character::isLowerCase)) {
            problems.add("Mật khẩu cần ít nhất 1 chữ thường.");
        }
        if (password.chars().noneMatch(c -> c >= '0' && c <= '9')) {
            problems.add("Mật khẩu cần ít nhất 1 chữ số.");
        }
        return problems;
    }
}
