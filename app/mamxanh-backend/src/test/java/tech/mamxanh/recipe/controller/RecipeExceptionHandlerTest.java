package tech.mamxanh.recipe.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import tech.mamxanh.recipe.service.RecipeValidationException;

class RecipeExceptionHandlerTest {

    private final RecipeExceptionHandler handler = new RecipeExceptionHandler();

    @Test
    void handlesValidationWithServletWebRequest() {
        RecipeValidationException ex = new RecipeValidationException(
                List.of(new RecipeValidationException.FieldError("title", "Lỗi")));
        ResponseEntity<?> response = handler.handleValidation(ex,
                new ServletWebRequest(new MockHttpServletRequest("POST", "/api/v1/recipes")));
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void handlesValidationWithNonServletWebRequest() {
        RecipeValidationException ex = new RecipeValidationException(
                List.of(new RecipeValidationException.FieldError("title", "Lỗi")));
        WebRequest nonServletRequest = Mockito.mock(WebRequest.class);
        ResponseEntity<?> response = handler.handleValidation(ex, nonServletRequest);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }
}
