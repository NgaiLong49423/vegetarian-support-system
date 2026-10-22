package tech.mamxanh.shopping.dto.request;

import java.util.List;
import jakarta.validation.constraints.*;

public record AddRecipeShoppingRequest(@NotEmpty @Size(max = 50) List<@NotNull @Positive Long> recipeIds) { }
