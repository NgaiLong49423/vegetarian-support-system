package tech.mamxanh.recipe.dto.request;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Enforces FR-27's conditional description rule at the API request boundary. */
public class RecipeReportRequestValidator implements ConstraintValidator<ValidRecipeReport, CreateRecipeReportRequest> {
    private static final String OTHER_DESCRIPTION_MESSAGE = "Lý do Khác cần mô tả từ 10 đến 500 ký tự.";

    @Override
    public boolean isValid(CreateRecipeReportRequest request, ConstraintValidatorContext context) {
        if (request == null || request.reasonCode() == null || request.reasonCode() != RecipeReportReasonCode.OTHER) {
            return true;
        }

        String description = request.description();
        int length = description == null ? 0 : description.strip().length();
        if (length >= 10 && length <= 500) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(OTHER_DESCRIPTION_MESSAGE)
                .addPropertyNode("description")
                .addConstraintViolation();
        return false;
    }
}
