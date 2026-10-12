package tech.mamxanh.integration.ai;

import java.util.List;

public interface AiClient {
    String generateRecipeText(String prompt, List<Long> allowedIngredientIds);
}
