package tech.mamxanh.recipe.dto.request;

import jakarta.validation.constraints.Size;
import java.util.List;

/** Ingredient filters are optional for a standard FR-34 weekly menu. */
public record AiWeeklyMenuRequest(@Size(max = 20) List<Long> ingredientIds) { }
