package tech.mamxanh.recipe.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CreateRecipeReportRequestTest {
    private static Validator validator;
    private static jakarta.validation.ValidatorFactory factory;

    @BeforeAll
    static void createValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void acceptsEveryCanonicalReasonWithOptionalDescription() {
        for (RecipeReportReasonCode reason : RecipeReportReasonCode.values()) {
            String description = reason == RecipeReportReasonCode.OTHER ? "Mô tả lý do khác" : "";
            assertThat(validator.validate(new CreateRecipeReportRequest(reason, description)))
                    .as("reason %s", reason)
                    .isEmpty();
        }
    }

    @Test
    void acceptsEmptyOptionalDescriptionAndMaximumLengthForOtherReasons() {
        assertThat(validator.validate(new CreateRecipeReportRequest(RecipeReportReasonCode.NON_VEGAN, null))).isEmpty();
        assertThat(validator.validate(new CreateRecipeReportRequest(RecipeReportReasonCode.NON_VEGAN, "x".repeat(500))))
                .isEmpty();
    }

    @Test
    void trimsDescriptionAndRequiresOtherDescriptionBetweenTenAndFiveHundredCharacters() {
        var trimmed = new CreateRecipeReportRequest(RecipeReportReasonCode.OTHER, "  1234567890  ");
        assertThat(trimmed.description()).isEqualTo("1234567890");
        assertThat(validator.validate(trimmed)).isEmpty();
        assertThat(validator.validate(new CreateRecipeReportRequest(RecipeReportReasonCode.OTHER, "123456789")))
                .anySatisfy(error -> assertThat(error.getPropertyPath().toString()).isEqualTo("description"));
        assertThat(validator.validate(new CreateRecipeReportRequest(RecipeReportReasonCode.OTHER, " ")))
                .anySatisfy(error -> assertThat(error.getPropertyPath().toString()).isEqualTo("description"));
        assertThat(validator.validate(new CreateRecipeReportRequest(RecipeReportReasonCode.OTHER, "x".repeat(501))))
                .anySatisfy(error -> assertThat(error.getPropertyPath().toString()).isEqualTo("description"));
        assertThat(validator.validate(new CreateRecipeReportRequest(RecipeReportReasonCode.OTHER, null)))
                .anySatisfy(error -> assertThat(error.getPropertyPath().toString()).isEqualTo("description"));
    }

    @Test
    void classLevelValidatorLeavesNullObjectHandlingToTheRequestBoundary() {
        assertThat(new RecipeReportRequestValidator().isValid(null, null)).isTrue();
    }

    @Test
    void rejectsMissingReasonAndHtmlMarkupInDescription() {
        Set<?> missingReason = validator.validate(new CreateRecipeReportRequest(null, "Description"));
        assertThat(missingReason).isNotEmpty();

        Set<?> markup = validator.validate(new CreateRecipeReportRequest(
                RecipeReportReasonCode.OTHER, "<script>alert(1)</script>"));
        assertThat(markup).isNotEmpty();
    }
}
