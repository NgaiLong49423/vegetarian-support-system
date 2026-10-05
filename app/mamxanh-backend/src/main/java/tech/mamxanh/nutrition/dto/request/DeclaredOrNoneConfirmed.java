package tech.mamxanh.nutrition.dto.request;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

/**
 * BR-31: a required FR-31 list must contain at least one item or carry the explicit "none"
 * confirmation. A missing list is reported by {@code @NotNull}, not here.
 */
@Documented
@Constraint(validatedBy = DeclaredOrNoneConfirmed.Validator.class)
@Target({ FIELD, PARAMETER, RECORD_COMPONENT })
@Retention(RUNTIME)
public @interface DeclaredOrNoneConfirmed {

    String message();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<DeclaredOrNoneConfirmed, IngredientPreferenceListRequest> {
        @Override
        public boolean isValid(IngredientPreferenceListRequest value, ConstraintValidatorContext context) {
            return value == null || value.confirmsNone() || !value.itemsOrEmpty().isEmpty();
        }
    }
}
