package tech.mamxanh.recipe.dto.request;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RecipeReportRequestValidator.class)
public @interface ValidRecipeReport {
    String message() default "Mô tả không hợp lệ với lý do báo cáo đã chọn.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
