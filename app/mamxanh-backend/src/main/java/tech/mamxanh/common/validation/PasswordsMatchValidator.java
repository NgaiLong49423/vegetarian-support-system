package tech.mamxanh.common.validation;

import java.util.Objects;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, PasswordConfirmation> {

    @Override
    public boolean isValid(PasswordConfirmation value, ConstraintValidatorContext context) {
        if (value == null || Objects.equals(value.password(), value.confirmPassword())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("confirmPassword")
                .addConstraintViolation();
        return false;
    }
}
