package tech.mamxanh.recipe.dto.response;

import java.util.List;

public record RecipeFormOptionsResponse(
        List<Choice> dishCategories,
        List<Choice> vegetarianTypes,
        List<Choice> difficulties,
        List<UnitOption> units) {

    public record Choice(String code, String label) {}
    public record UnitOption(Integer unitId, String code, String name, String dimension) {}
}
