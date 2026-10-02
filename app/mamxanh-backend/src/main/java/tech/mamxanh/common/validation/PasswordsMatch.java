package tech.mamxanh.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

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
