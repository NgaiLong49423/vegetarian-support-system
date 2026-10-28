package tech.mamxanh.mealplan.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import tech.mamxanh.AbstractIntegrationTest;

/**
 * FR-40 AC-40.3 (BR-40, BR-50): a published recipe whose catalog ingredient has no nutrition data stays
 * out of the FR-36 nutrition menu pool, so applying a menu cannot put it on the meal plan.
 */
class IncompleteNutritionAiMenuIntegrationTest extends AbstractIntegrationTest {
    private static final String MEMBER = "issue45-member@test.local";
    private static final String EXPERT = "issue45-expert@test.local";
    private static final String SUPPORTED = "Issue45 đậu hũ có dinh dưỡng";
    private static final String UNSUPPORTED = "Issue45 nấm tràm chưa có dinh dưỡng";
    // Same current week as MutableClock.DEFAULT_START, including Vietnam-local scheduling rules.
    private static final String WEEK = "2026-09-28";

    @Autowired private JdbcTemplate jdbc;

    private long memberId;
    private long completeRecipe;
    private long incompleteRecipe;

    @BeforeEach
    void prepareFixtures() {
        removeFixtures();
        jdbc.update("""
                INSERT INTO [USER] (email, display_name, role, account_status, email_verified, date_of_birth,
                    biological_sex, height_cm, weight_kg, activity_level, health_data_consent, health_data_consent_at,
                    nutrition_eligibility_status, nutrition_eligibility_confirmed_at, nutrition_bmi)
                VALUES (?, N'Issue 45 Member', 'CUSTOMER', 'ACTIVE', 1, '1995-05-05', 'FEMALE', 160.0, 55.0,
                    'LIGHTLY_ACTIVE', 1, SYSUTCDATETIME(), 'ELIGIBLE', SYSUTCDATETIME(), 21.484375)
                """, MEMBER);
        memberId = jdbc.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, MEMBER);
        LocalDateTime now = LocalDateTime.now(clock);
        jdbc.update("INSERT INTO [SUBSCRIPTION] (user_id, tier, status, starts_at, ends_at) VALUES (?, 'PRO', 'ACTIVE', ?, ?)",
                memberId, now.minusDays(1), now.plusDays(30));

        jdbc.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, N'Issue 45 Expert', 'EXPERT', 'ACTIVE', 1)", EXPERT);
        long expertId = jdbc.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, EXPERT);
        int gramUnitId = jdbc.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'g'", Integer.class);
        long supportedId = insertIngredient(SUPPORTED);
        jdbc.update("""
                UPDATE [INGREDIENT]
                SET energy_kcal_100g = 76, protein_g_100g = 8.0, carbohydrate_g_100g = 1.9,
                    total_fat_g_100g = 4.8, fiber_g_100g = 0.3, calcium_mg_100g = 350.0,
                    iron_mg_100g = 5.4, vitamin_b12_mcg_100g = 0.0000, zinc_mg_100g = 0.8,
                    source_url = 'https://fdc.nal.usda.gov/', nutrition_supported = 1
                WHERE ingredient_id = ?
                """, supportedId);
        long unsupportedId = insertIngredient(UNSUPPORTED);
        jdbc.update("UPDATE [INGREDIENT] SET nutrition_supported = 0 WHERE ingredient_id = ?", unsupportedId);
        completeRecipe = insertRecipe(expertId, "Issue45 đậu hũ hấp", gramUnitId, supportedId);
        incompleteRecipe = insertRecipe(expertId, "Issue45 nấm tràm xào", gramUnitId, supportedId, unsupportedId);
    }

    @AfterEach
    void removeFixtures() {
        jdbc.update("DELETE FROM [MEAL_PLAN_ENTRY] WHERE meal_plan_id IN (SELECT meal_plan_id FROM [MEAL_PLAN] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email = ?))", MEMBER);
        jdbc.update("DELETE FROM [MEAL_PLAN] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email = ?)", MEMBER);
        jdbc.update("DELETE FROM [SUBSCRIPTION] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email = ?)", MEMBER);
        jdbc.update("DELETE FROM [RECIPE_INGREDIENT] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email = ?))", EXPERT);
        jdbc.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email = ?)", EXPERT);
        jdbc.update("DELETE FROM [INGREDIENT] WHERE name IN (?, ?)", SUPPORTED, UNSUPPORTED);
        jdbc.update("DELETE FROM [USER] WHERE email IN (?, ?)", MEMBER, EXPERT);
    }

    @Test
    void aMenuWithARecipeMissingNutritionDataIsRejectedAndLeavesThePlanUntouched() throws Exception {
        mockMvc.perform(applyMenu(incompleteRecipe))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));

        assertThat(plannedRecipes()).isZero();
    }

    @Test
    void aMenuWithACompleteRecipeIsApplied() throws Exception {
        mockMvc.perform(applyMenu(completeRecipe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].recipeId").value(completeRecipe));

        assertThat(plannedRecipes()).isOne();
    }

    private MockHttpServletRequestBuilder applyMenu(long recipeId) {
        return put("/api/v1/meal-plans/ai-menu").with(member()).contentType(MediaType.APPLICATION_JSON).content("""
                {"weekStartDate":"%s","mode":"THIS_WEEK","meals":[{"mealDate":"%s","mealType":"LUNCH","recipeId":%d}]}
                """.formatted(WEEK, WEEK, recipeId));
    }

    private RequestPostProcessor member() {
        return user(Long.toString(memberId)).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private long plannedRecipes() {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM [MEAL_PLAN_ENTRY] e JOIN [MEAL_PLAN] p ON p.meal_plan_id = e.meal_plan_id
                WHERE p.user_id = ?
                """, Long.class, memberId);
    }

    private long insertIngredient(String name) {
        jdbc.update("INSERT INTO [INGREDIENT] (name, source_name, reference_date) VALUES (?, 'Issue #45 fixture', '2026-10-13')", name);
        return jdbc.queryForObject("SELECT ingredient_id FROM [INGREDIENT] WHERE name = ?", Long.class, name);
    }

    private long insertRecipe(long authorId, String title, int gramUnitId, long... ingredientIds) {
        long recipeId = jdbc.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, description, instructions, dish_category,
                    vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id
                VALUES (?, ?, N'Mô tả', N'Rửa và chế biến nguyên liệu cho món ăn này.', 'SOUP',
                    'VEGAN', 'EASY', 2, 10, 15, 'PUBLISHED', SYSUTCDATETIME())
                """, Long.class, authorId, title);
        for (long ingredientId : ingredientIds) {
            jdbc.update("INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity) VALUES (?, ?, ?, 100)",
                    recipeId, ingredientId, gramUnitId);
        }
        return recipeId;
    }
}
