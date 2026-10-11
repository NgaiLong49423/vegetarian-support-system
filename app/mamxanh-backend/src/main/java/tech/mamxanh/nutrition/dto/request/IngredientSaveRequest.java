package tech.mamxanh.nutrition.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record IngredientSaveRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 100) String ingredientGroup,
        @NotBlank @Size(max = 200) String sourceName,
        @Size(max = 2048) String sourceUrl,
        @NotNull @PastOrPresent LocalDate referenceDate) { }
