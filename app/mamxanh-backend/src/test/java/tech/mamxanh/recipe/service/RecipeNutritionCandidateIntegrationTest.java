package tech.mamxanh.recipe.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import tech.mamxanh.AbstractIntegrationTest;

/** FR-40 AC-40.3 (BR-40, BR-50): a recipe with incomplete nutrition never enters the nutrition AI menu pool. */
class RecipeNutritionCandidateIntegrationTest extends AbstractIntegrationTest {
    private static final String EMAIL = "issue45-expert@test.local";
    private static final String SUPPORTED = "Issue45 đậu hũ có dinh dưỡng";
    private static final String UNSUPPORTED = "Issue45 nấm tràm chưa có dinh dưỡng";

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private RecipeNutritionCandidateService candidates;

    private long expertId;
    private long supportedId;
    private long unsupportedId;
    private int gramUnitId;

    @BeforeEach
    void prepareFixtures() {
        removeFixtures();
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, N'Issue 45', 'EXPERT', 'ACTIVE', 1)", EMAIL);
        expertId = jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, EMAIL);
        gramUnitId = jdbcTemplate.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'g'", Integer.class);
        supportedId = insertIngredient(SUPPORTED);
        jdbcTemplate.update("""
                UPDATE [INGREDIENT]
                SET energy_kcal_100g = 76, protein_g_100g = 8.0, carbohydrate_g_100g = 1.9,
                    total_fat_g_100g = 4.8, fiber_g_100g = 0.3, calcium_mg_100g = 350.0,
                    iron_mg_100g = 5.4, vitamin_b12_mcg_100g = 0.0000, zinc_mg_100g = 0.8,
                    source_url = 'https://fdc.nal.usda.gov/', nutrition_supported = 1
                WHERE ingredient_id = ?
                """, supportedId);
        unsupportedId = insertIngredient(UNSUPPORTED);
        jdbcTemplate.update("UPDATE [INGREDIENT] SET nutrition_supported = 0 WHERE ingredient_id = ?", unsupportedId);
    }

    @AfterEach
    void removeFixtures() {
        jdbcTemplate.update("DELETE FROM [RECIPE_INGREDIENT] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email = ?))", EMAIL);
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email = ?)", EMAIL);
        jdbcTemplate.update("DELETE FROM [INGREDIENT] WHERE name IN (?, ?)", SUPPORTED, UNSUPPORTED);
        jdbcTemplate.update("DELETE FROM [USER] WHERE email = ?", EMAIL);
    }

    @Test
    void onlyPublishedRecipesWithCompleteNutritionStayInTheAiMenuPool() {
        long complete = insertRecipe("Đậu hũ hấp", "PUBLISHED", supportedId);
        long incomplete = insertRecipe("Nấm tràm xào", "PUBLISHED", supportedId, unsupportedId);
        long hiddenComplete = insertRecipe("Đậu hũ đang ẩn", "HIDDEN", supportedId);

        assertThat(candidates.retainNutritionComplete(List.of(incomplete, complete, hiddenComplete, Long.MAX_VALUE)))
                .containsExactly(complete);
        assertThat(candidates.retainNutritionComplete(List.of())).isEmpty();
    }

    private long insertIngredient(String name) {
        jdbcTemplate.update("INSERT INTO [INGREDIENT] (name, source_name, reference_date) VALUES (?, 'Issue #45 fixture', '2026-10-13')", name);
        return jdbcTemplate.queryForObject("SELECT ingredient_id FROM [INGREDIENT] WHERE name = ?", Long.class, name);
    }

    private long insertRecipe(String title, String status, long... ingredientIds) {
        long recipeId = jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, description, instructions, dish_category,
                    vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id
                VALUES (?, ?, N'Mô tả', N'Rửa và chế biến nguyên liệu cho món ăn này.', 'SOUP',
                    'VEGAN', 'EASY', 2, 10, 15, ?, SYSUTCDATETIME())
                """, Long.class, expertId, title, status);
        for (long ingredientId : ingredientIds) {
            jdbcTemplate.update("INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity) VALUES (?, ?, ?, 100)",
                    recipeId, ingredientId, gramUnitId);
        }
        return recipeId;
    }
}
