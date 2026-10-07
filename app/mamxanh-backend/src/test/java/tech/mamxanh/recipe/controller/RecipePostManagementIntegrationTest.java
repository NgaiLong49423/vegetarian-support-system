package tech.mamxanh.recipe.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tech.mamxanh.AbstractIntegrationTest;
import tools.jackson.databind.json.JsonMapper;

class RecipePostManagementIntegrationTest extends AbstractIntegrationTest {
    private static final String OWNER_EMAIL = "issue47-owner@test.local";
    private static final String OTHER_EXPERT_EMAIL = "issue47-other@test.local";
    private static final String CUSTOMER_EMAIL = "issue47-customer@test.local";
    private static final String LOCKED_EXPERT_EMAIL = "issue47-locked@test.local";
    private static final String INGREDIENT_NAME = "Issue47 Tofu";
    private static final LocalDate WEEK_START = LocalDate.of(2026, 10, 5);

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JsonMapper jsonMapper;

    private long ownerId;
    private long otherExpertId;
    private long customerId;
    private long lockedExpertId;
    private long publishedRecipeId;
    private long hiddenRecipeId;
    private long otherRecipeId;
    private long ingredientId;
    private int gramUnitId;

    @BeforeEach
    void prepareDatabaseFixtures() {
        jdbcTemplate.update("DELETE FROM [MEAL_PLAN] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'issue47-%@test.local')");
        jdbcTemplate.update("DELETE FROM [SAVED_RECIPE] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'issue47-%@test.local')");
        jdbcTemplate.update("DELETE FROM [RECIPE_REACTION] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'issue47-%@test.local'))");
        jdbcTemplate.update("DELETE FROM [RECIPE_VIEW] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'issue47-%@test.local'))");
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'issue47-%@test.local')");
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE 'issue47-%@test.local'");
        jdbcTemplate.update("DELETE FROM [INGREDIENT_UNIT_CONVERSION] WHERE ingredient_id IN (SELECT ingredient_id FROM [INGREDIENT] WHERE name = ?)", INGREDIENT_NAME);
        jdbcTemplate.update("DELETE FROM [INGREDIENT] WHERE name = ?", INGREDIENT_NAME);

        ownerId = insertUser(OWNER_EMAIL, "EXPERT", "ACTIVE");
        otherExpertId = insertUser(OTHER_EXPERT_EMAIL, "EXPERT", "ACTIVE");
        customerId = insertUser(CUSTOMER_EMAIL, "CUSTOMER", "ACTIVE");
        lockedExpertId = insertUser(LOCKED_EXPERT_EMAIL, "EXPERT", "LOCKED");
        jdbcTemplate.update("INSERT INTO [INGREDIENT] (name, source_name, reference_date) VALUES (?, ?, '2026-10-05')",
                INGREDIENT_NAME, "Issue #47 integration fixture");
        ingredientId = jdbcTemplate.queryForObject("SELECT ingredient_id FROM [INGREDIENT] WHERE name = ?", Long.class, INGREDIENT_NAME);
        gramUnitId = jdbcTemplate.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'g'", Integer.class);

        publishedRecipeId = insertRecipe(ownerId, "Đậu hũ sốt cà", "PUBLISHED");
        hiddenRecipeId = insertRecipe(ownerId, "Nấm kho đang ẩn", "HIDDEN");
        otherRecipeId = insertRecipe(otherExpertId, "Canh của chuyên gia khác", "PUBLISHED");
        jdbcTemplate.update("INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity) VALUES (?, ?, ?, 200)",
                publishedRecipeId, ingredientId, gramUnitId);
        jdbcTemplate.update("INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover) VALUES (?, ?, 'image/jpeg', 1, 1)",
                publishedRecipeId, "https://cdn.test/recipe-cover.jpg");
        jdbcTemplate.update("INSERT INTO [RECIPE_REACTION] (user_id, recipe_id, reaction_type) VALUES (?, ?, 'LIKE')",
                customerId, publishedRecipeId);
        jdbcTemplate.update("INSERT INTO [RECIPE_REACTION] (user_id, recipe_id, reaction_type) VALUES (?, ?, 'DISLIKE')",
                otherExpertId, publishedRecipeId);
        jdbcTemplate.update("INSERT INTO [RECIPE_VIEW] (recipe_id, user_id) VALUES (?, ?)", publishedRecipeId, customerId);
        jdbcTemplate.update("INSERT INTO [SAVED_RECIPE] (user_id, recipe_id) VALUES (?, ?)", customerId, publishedRecipeId);
        jdbcTemplate.update("INSERT INTO [SAVED_RECIPE] (user_id, recipe_id) VALUES (?, ?)", customerId, hiddenRecipeId);

        Long mealPlanId = jdbcTemplate.queryForObject(
                "INSERT INTO [MEAL_PLAN] (user_id, week_start_date) OUTPUT INSERTED.meal_plan_id VALUES (?, ?)",
                Long.class, customerId, WEEK_START);
        jdbcTemplate.update("INSERT INTO [MEAL_PLAN_ENTRY] (meal_plan_id, recipe_id, meal_date, meal_type, planned_servings) VALUES (?, ?, ?, 'LUNCH', 2)",
                mealPlanId, publishedRecipeId, WEEK_START.plusDays(1));
    }

    @Test
    void publicRecipeDetailProjectsCurrentAuthorMediaNutritionAndIndependentStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/{recipeId}", publishedRecipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author.displayName").value("Issue 47 test"))
                .andExpect(jsonPath("$.ingredients[0].quantity").value(200))
                .andExpect(jsonPath("$.media[0].blobUrl").value("https://cdn.test/recipe-cover.jpg"))
                .andExpect(jsonPath("$.nutrition.complete").value(false))
                .andExpect(jsonPath("$.nutrition.ingredientsMissingData[0]").value(INGREDIENT_NAME))
                .andExpect(jsonPath("$.statistics.likes").value(1))
                .andExpect(jsonPath("$.statistics.dislikes").value(1))
                .andExpect(jsonPath("$.statistics.reactionCount").value(2))
                .andExpect(jsonPath("$.statistics.viewCount").value(1));
        mockMvc.perform(get("/api/v1/recipes/{recipeId}", hiddenRecipeId))
                .andExpect(status().isNotFound());
    }

    @Test
    void savedRecipeListIsAuthenticatedPrivateAndKeepsUnavailableReferencesSafe() throws Exception {
        mockMvc.perform(get("/api/v1/saved-recipes"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/saved-recipes").with(principal(customerId, "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].available", hasItems(true, false)))
                .andExpect(jsonPath("$.content[?(@.available == false)].title").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.available == false)].unavailableMessage")
                        .value(hasItem("Công thức không còn khả dụng")));
        mockMvc.perform(get("/api/v1/saved-recipes").param("size", "51")
                        .with(principal(customerId, "ROLE_CUSTOMER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyActiveExpertCanListOwnPublishedAndHiddenRecipes() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/mine"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/recipes/mine").with(principal(customerId, "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/recipes/mine").with(principal(lockedExpertId, "ROLE_EXPERT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/recipes/mine").with(principal(ownerId, "ROLE_EXPERT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[*].id", hasItem((int) publishedRecipeId)))
                .andExpect(jsonPath("$.items[*].id", hasItem((int) hiddenRecipeId)))
                .andExpect(jsonPath("$.items[*].id").value(org.hamcrest.Matchers.not(hasItem((int) otherRecipeId))));
    }

    @Test
    void ownerCanViewHiddenRecipeButBothApiAndDatabaseRejectEditingIt() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/{recipeId}/manage", hiddenRecipeId)
                        .with(principal(ownerId, "ROLE_EXPERT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"))
                .andExpect(jsonPath("$.title").value("Nấm kho đang ẩn"));

        mockMvc.perform(put("/api/v1/recipes/{recipeId}", hiddenRecipeId)
                        .with(principal(ownerId, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON).content(validUpdate("Không được mở lại")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RECIPE_HIDDEN"));
        org.assertj.core.api.Assertions.assertThat(title(hiddenRecipeId)).isEqualTo("Nấm kho đang ẩn");
    }

    @Test
    void onlyOwnerCanUpdateAndValidChangesPersistImmediately() throws Exception {
        mockMvc.perform(put("/api/v1/recipes/{recipeId}", publishedRecipeId)
                        .with(principal(otherExpertId, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON).content(validUpdate("Không thuộc quyền")))
                .andExpect(status().isForbidden());
        org.assertj.core.api.Assertions.assertThat(title(publishedRecipeId)).isEqualTo("Đậu hũ sốt cà");

        mockMvc.perform(put("/api/v1/recipes/{recipeId}", publishedRecipeId)
                        .with(principal(ownerId, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON).content(validUpdate("Đậu hũ sốt cà cập nhật")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Đậu hũ sốt cà cập nhật"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
        org.assertj.core.api.Assertions.assertThat(title(publishedRecipeId)).isEqualTo("Đậu hũ sốt cà cập nhật");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT quantity FROM [RECIPE_INGREDIENT] WHERE recipe_id = ?", BigDecimal.class, publishedRecipeId))
                .isEqualByComparingTo("300");
        mockMvc.perform(get("/api/v1/recipes/{recipeId}", publishedRecipeId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Đậu hũ sốt cà cập nhật"));
    }

    @Test
    void invalidUpdateDoesNotChangeRecipeOrIngredients() throws Exception {
        mockMvc.perform(put("/api/v1/recipes/{recipeId}", publishedRecipeId)
                        .with(principal(ownerId, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdate("Tên hợp lệ nhưng conversion sai", 999_999L, gramUnitId)))
                .andExpect(status().isBadRequest());
        org.assertj.core.api.Assertions.assertThat(title(publishedRecipeId)).isEqualTo("Đậu hũ sốt cà");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [RECIPE_INGREDIENT] WHERE recipe_id = ?", Integer.class, publishedRecipeId)).isEqualTo(1);
    }

    @Test
    void deleteSoftTombstonesRecipeAndMealPlanKeepsItsEntry() throws Exception {
        mockMvc.perform(delete("/api/v1/recipes/{recipeId}", publishedRecipeId)
                        .with(principal(otherExpertId, "ROLE_EXPERT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/recipes/{recipeId}", publishedRecipeId)
                        .with(principal(ownerId, "ROLE_EXPERT")))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM [RECIPE_POST] WHERE recipe_id = ?", String.class, publishedRecipeId)).isEqualTo("DELETED");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [MEAL_PLAN_ENTRY] WHERE recipe_id = ?", Integer.class, publishedRecipeId)).isEqualTo(1);
        mockMvc.perform(get("/api/v1/recipes/{recipeId}", publishedRecipeId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/recipes").param("keyword", "Đậu hũ sốt cà"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));

        mockMvc.perform(get("/api/v1/meal-plans").param("weekStartDate", WEEK_START.toString())
                        .with(principal(customerId, "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries.length()").value(1))
                .andExpect(jsonPath("$.entries[0].mealDate").value(WEEK_START.plusDays(1).toString()))
                .andExpect(jsonPath("$.entries[0].mealType").value("LUNCH"))
                .andExpect(jsonPath("$.entries[0].recipeDeleted").value(true))
                .andExpect(jsonPath("$.entries[0].recipeTitle").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.entries[0].unavailableMessage").value("Công thức không còn khả dụng"));
    }

    @Test
    void mealPlanWeekIsPrivateAndRequiresMondayStart() throws Exception {
        mockMvc.perform(get("/api/v1/meal-plans").param("weekStartDate", WEEK_START.toString()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/meal-plans").param("weekStartDate", WEEK_START.plusDays(1).toString())
                        .with(principal(customerId, "ROLE_CUSTOMER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void runtimeOpenApiDescribesManagementAndReadOnlyMealPlanOperations() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/mine'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}'].get.summary").exists())
                .andExpect(jsonPath("$.paths['/api/v1/saved-recipes'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/mine'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}/manage'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}'].put.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}'].delete.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/meal-plans'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/saved-recipes'].get.security[0].bearerAuth").isArray());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor principal(long userId, String role) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority(role));
    }

    private long insertUser(String email, String role, String status) {
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, ?, ?, ?, 1)",
                email, "Issue 47 test", role, status);
        return jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }

    private long insertRecipe(long authorId, String title, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type, difficulty,
                    servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id VALUES (?, ?, N'Rửa và chế biến nguyên liệu cho món ăn này.',
                    'BRAISED', 'VEGAN', 'EASY', 2, 10, 15, ?, SYSUTCDATETIME())
                """, Long.class, authorId, title, status);
    }

    private String title(long recipeId) {
        return jdbcTemplate.queryForObject("SELECT title FROM [RECIPE_POST] WHERE recipe_id = ?", String.class, recipeId);
    }

    private String validUpdate(String title) throws Exception {
        return validUpdate(title, ingredientId, gramUnitId);
    }

    private String validUpdate(String title, Long ingredient, int unit) throws Exception {
        return jsonMapper.writeValueAsString(new UpdatePayload(title, "Mô tả cập nhật", "Nấu món này cùng gia vị đến khi thấm.",
                "BRAISED", "VEGAN", "EASY", 2, 10, 15, "", List.of(new IngredientPayload(ingredient, unit, new BigDecimal("300")))));
    }

    private String json(Object value) throws Exception { return jsonMapper.writeValueAsString(value); }

    private record UpdatePayload(String title, String description, String instructions, String dishCategory,
            String vegetarianType, String difficulty, int servings, int prepTimeMin, int cookTimeMin,
            String youtubeUrl, List<IngredientPayload> ingredients) { }

    private record IngredientPayload(Long ingredientId, Integer unitId, BigDecimal quantity) { }
}
