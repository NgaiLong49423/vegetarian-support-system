package tech.mamxanh.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Class-level check that {@link PasswordConfirmation#confirmPassword()} equals the password.
 * The violation is reported on the {@code confirmPassword} field.
 */
@Documented
@Constraint(validatedBy = PasswordsMatchValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordsMatch {

    String message() default "Mật khẩu xác nhận chưa khớp.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
