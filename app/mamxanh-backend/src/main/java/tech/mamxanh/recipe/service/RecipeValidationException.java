package tech.mamxanh.recipe.service;

import java.util.List;

public class RecipeValidationException extends RuntimeException {
    private final List<FieldError> errors;

    public RecipeValidationException(List<FieldError> errors) {
        super("Dữ liệu công thức chưa hợp lệ.");
        this.errors = List.copyOf(errors);
    }

    public List<FieldError> errors() { return errors; }

    public record FieldError(String field, String message) {}
}
