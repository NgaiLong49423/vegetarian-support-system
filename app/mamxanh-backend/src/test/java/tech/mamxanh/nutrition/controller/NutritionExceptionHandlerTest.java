package tech.mamxanh.nutrition.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import tech.mamxanh.nutrition.dto.request.SaveNutritionProfileRequest;

class NutritionExceptionHandlerTest {
    @Test
    void returnsFieldSpecificValidationErrorsForHeightAndWeight() throws NoSuchMethodException {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "heightCm", "Chiều cao tối thiểu là 100 cm."));
        bindingResult.addError(new FieldError("request", "weightKg", "Cân nặng tối đa là 300 kg."));
        MethodParameter requestParameter = new MethodParameter(
                NutritionController.class.getMethod("saveOwnProfile", SaveNutritionProfileRequest.class), 0);

        var problem = new NutritionExceptionHandler().handleValidation(
                new MethodArgumentNotValidException(requestParameter, bindingResult));

        assertEquals("VALIDATION_FAILED", problem.getProperties().get("code"));
        @SuppressWarnings("unchecked")
        List<NutritionExceptionHandler.FieldError> errors =
                (List<NutritionExceptionHandler.FieldError>) problem.getProperties().get("errors");
        assertEquals(2, errors.size());
        assertTrue(errors.stream().anyMatch(error -> error.field().equals("heightCm")
                && error.message().equals("Chiều cao tối thiểu là 100 cm.")));
        assertTrue(errors.stream().anyMatch(error -> error.field().equals("weightKg")
                && error.message().equals("Cân nặng tối đa là 300 kg.")));
    }
}
