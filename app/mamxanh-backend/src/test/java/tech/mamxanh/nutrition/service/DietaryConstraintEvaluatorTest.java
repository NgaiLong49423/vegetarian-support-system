package tech.mamxanh.nutrition.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tech.mamxanh.nutrition.dto.response.DietaryPreferencesResponse;
import tech.mamxanh.nutrition.dto.response.IngredientResponse;
import tech.mamxanh.nutrition.entity.OnboardingStatus;
import tech.mamxanh.nutrition.entity.VegetarianType;
import tech.mamxanh.recipe.dto.response.RecipePostResponse;

class DietaryConstraintEvaluatorTest {
    private final DietaryConstraintEvaluator evaluator = new DietaryConstraintEvaluator();

    @Test
    void appliesTheFourDeclaredVegetarianPatternsToRecipeType() {
        var dairy = recipe(1, "LACTO", ingredient(2L, "phô mai"));
        var profile = profile(null, VegetarianType.VEGAN);
        var result = evaluator.evaluate(dairy, profile, Map.of(2L, ingredientRef(2, "Dairy")));
        assertThat(result.hasVegetarianConflict()).isTrue();
        assertThat(result.ingredients().getFirst().conflicts()).doesNotContain(DietaryConstraintEvaluator.Conflict.VEGETARIAN_TYPE);

        assertThat(DietaryConstraintEvaluator.recipeTypeCompatible("LACTO", "LACTO")).isTrue();
        assertThat(DietaryConstraintEvaluator.recipeTypeCompatible("OVO", "LACTO")).isFalse();
        assertThat(DietaryConstraintEvaluator.recipeTypeCompatible("LACTO_OVO", "OVO")).isFalse();
        assertThat(DietaryConstraintEvaluator.recipeTypeCompatible("LACTO_OVO", "LACTO_OVO")).isTrue();
    }

    @Test
    void matchesAvoidedIngredientsExactlyAndKeepsDislikesSeparate() {
        var peanutProfile = profile("đậu phộng", VegetarianType.LACTO_OVO);
        var derivedPeanutIngredient = recipe(3, "VEGAN", ingredient(8L, "dầu đậu phộng"));
        var exactPeanutIngredient = recipe(11, "VEGAN", ingredient(11L, "Đậu phộng"));
        var soybeanMilk = recipe(4, "VEGAN", ingredient(9L, "sữa đậu nành"));
        var cowMilk = recipe(5, "VEGAN", ingredient(10L, "sữa bò"));
        var soyProfile = profile("đậu nành", VegetarianType.LACTO_OVO);
        var soySauce = recipe(12, "VEGAN", ingredient(12L, "Nước tương"));
        var catalog = Map.of(8L, ingredientRef(8, "Oil"), 9L, ingredientRef(9, "Milk"),
                10L, ingredientRef(10, "Dairy"), 11L, ingredientRef(11, "Legume"),
                12L, ingredientRef(12, "Seasoning"));

        assertThat(evaluator.evaluate(derivedPeanutIngredient, peanutProfile, catalog).hasAllergenOrAvoidConflict()).isFalse();
        assertThat(evaluator.evaluate(exactPeanutIngredient, peanutProfile, catalog).hasAllergenOrAvoidConflict()).isTrue();
        assertThat(evaluator.evaluate(soybeanMilk, peanutProfile, catalog).hasAllergenOrAvoidConflict()).isFalse();
        assertThat(evaluator.evaluate(cowMilk, peanutProfile, catalog).hasAllergenOrAvoidConflict()).isFalse();
        assertThat(evaluator.evaluate(soySauce, soyProfile, catalog).hasAllergenOrAvoidConflict()).isFalse();
        assertThat(DietaryConstraintEvaluator.dislikeCount(soySauce, List.of(), List.of("đậu nành"))).isEqualTo(1);
        assertThat(evaluator.evaluate(soybeanMilk, "VEGAN", List.of(), List.of(), List.of(), List.of(), catalog)
                .hasVegetarianConflict()).isFalse();
        assertThat(evaluator.evaluate(cowMilk, "VEGAN", List.of(), List.of(), List.of(), List.of(), catalog)
                .hasVegetarianConflict()).isFalse();
        var labeledAsOther = recipe(7, "VEGAN", ingredient(11L, "phô mai"));
        assertThat(evaluator.evaluate(labeledAsOther, "VEGAN", List.of(), List.of(), List.of(), List.of(),
                Map.of(11L, ingredientRef(11, "Khác"))).hasVegetarianConflict()).isFalse();
    }

    @Test
    void vegetarianCompatibilityDependsOnRecipeTypeRatherThanIngredientName() {
        var types = List.of("VEGAN", "LACTO", "OVO", "LACTO_OVO");
        var expected = List.of(List.of(true, false, false, false), List.of(true, true, false, false),
                List.of(true, false, true, false), List.of(true, true, true, true));
        for (int profile = 0; profile < types.size(); profile++) {
            for (int recipeType = 0; recipeType < types.size(); recipeType++) {
                var recipe = recipe(1, types.get(recipeType), ingredient(54L, "Trứng gà"));
                var assessment = evaluator.evaluate(recipe, types.get(profile), List.of(), List.of(), List.of(), List.of(),
                        Map.of(54L, ingredientRef(54, "Sữa & Trứng")));
                assertThat(assessment.recipeTypeCompatible()).isEqualTo(expected.get(profile).get(recipeType));
                assertThat(assessment.hasVegetarianConflict()).isEqualTo(!expected.get(profile).get(recipeType));
                assertThat(assessment.ingredients().getFirst().conflicts()).doesNotContain(DietaryConstraintEvaluator.Conflict.VEGETARIAN_TYPE);
            }
        }
    }

    @Test
    void marksUnknownIngredientUnverifiedAndKeepsDislikeAsSeparateSoftConflict() {
        var profile = new DietaryPreferencesResponse(VegetarianType.LACTO_OVO,
                new DietaryPreferencesResponse.IngredientPreferences(true, List.of()),
                new DietaryPreferencesResponse.IngredientPreferences(false,
                        List.of(new DietaryPreferencesResponse.PreferenceItem(12L, "nấm rơm"))),
                null, null, null, OnboardingStatus.COMPLETED,
                new DietaryPreferencesResponse.AiPersonalization(true, List.of()));
        var recipe = recipe(6, "LACTO_OVO", ingredient(12L, "nấm rơm"), ingredient(null, "hương liệu bí ẩn"));
        var result = evaluator.evaluate(recipe, profile, Map.of(12L, ingredientRef(12, "Khác")));

        assertThat(result.hasDislikedIngredients()).isTrue();
        assertThat(result.hasAllergenOrAvoidConflict()).isFalse();
        assertThat(result.unverifiedIngredients()).containsExactly("hương liệu bí ẩn");
        assertThat(result.ingredients().getFirst().conflicts()).containsExactly(DietaryConstraintEvaluator.Conflict.DISLIKE);
    }

    private static DietaryPreferencesResponse profile(String avoidName, VegetarianType type) {
        List<DietaryPreferencesResponse.PreferenceItem> avoid = avoidName == null ? List.of()
                : List.of(new DietaryPreferencesResponse.PreferenceItem(null, avoidName));
        return new DietaryPreferencesResponse(type,
                new DietaryPreferencesResponse.IngredientPreferences(avoid.isEmpty(), avoid),
                new DietaryPreferencesResponse.IngredientPreferences(true, List.of()), null, null, null,
                OnboardingStatus.COMPLETED, new DietaryPreferencesResponse.AiPersonalization(true, List.of()));
    }

    private static RecipePostResponse recipe(long id, String type, RecipePostResponse.Ingredient... lines) {
        return new RecipePostResponse(id, 1, "Author", null, "Recipe " + id, null, "Steps", "SOUP", type,
                "EASY", 2, 10, 10, null, "PUBLISHED", List.of(), List.of(lines), 0, 0, null, 0);
    }

    private static RecipePostResponse.Ingredient ingredient(Long id, String name) {
        return new RecipePostResponse.Ingredient(id, name, null, 1, "g", "gram", BigDecimal.ONE);
    }

    private static IngredientResponse ingredientRef(long id, String group) {
        return new IngredientResponse(id, "Ingredient " + id, group, null, null, null, false, true);
    }
}
