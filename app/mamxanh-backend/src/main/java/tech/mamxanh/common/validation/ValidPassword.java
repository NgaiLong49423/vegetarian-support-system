package tech.mamxanh.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * FR-03 password policy: 8–64 characters, at most 72 UTF-8 bytes (BCrypt limit), at least one
 * uppercase letter, one lowercase letter and one digit; special characters are optional.
 * Each unmet criterion is reported as its own violation so clients can show exactly what is missing.
 */
@Documented
@Constraint(validatedBy = PasswordConstraintValidator.class)
@Target({ ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {

    String message() default "Mật khẩu không đạt yêu cầu.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
