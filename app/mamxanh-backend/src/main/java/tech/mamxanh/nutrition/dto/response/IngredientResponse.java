package tech.mamxanh.nutrition.dto.response;

import java.time.LocalDate;

public record IngredientResponse(Long id, String name, String ingredientGroup, String sourceName,
                                 String sourceUrl, LocalDate referenceDate, boolean nutritionSupported,
                                 boolean active) { }
