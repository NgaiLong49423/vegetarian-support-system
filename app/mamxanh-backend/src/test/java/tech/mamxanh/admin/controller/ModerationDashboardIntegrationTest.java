package tech.mamxanh.admin.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.recipe.dto.request.CreateRecipeRequest;
import tech.mamxanh.recipe.dto.request.RecipeIngredientInput;
import tech.mamxanh.recipe.entity.RecipeCodes.Difficulty;
import tech.mamxanh.recipe.entity.RecipeCodes.DishCategory;
import tech.mamxanh.recipe.entity.RecipeCodes.VegetarianType;
import tools.jackson.databind.json.JsonMapper;

/**
 * FR-06: the moderation dashboard ranks reported Recipe Posts, shows the most common reason and finds a post by
 * its title or its author (UC-06.1), and new posts still publish at once while reports are handled (AC-06.4).
 */
class ModerationDashboardIntegrationTest extends AbstractIntegrationTest {
    private static final String USERS = "issue12-%@test.local";
    private static final String INGREDIENT_NAME = "Issue12 Ingredient";
    private static final LocalDateTime BASE = LocalDateTime.of(2100, 1, 1, 8, 0);

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JsonMapper jsonMapper;

    private long adminId;
    private long expertId;
    private long otherExpertId;
    private long busyRecipe;
    private long recentRecipe;
    private long tiedRecipe;
    private long[] customers;

    @BeforeEach
    void prepareFixtures() {
        cleanup();
        adminId = insertUser("issue12-admin@test.local", "Quản trị viên Hà", "ADMIN");
        expertId = insertUser("issue12-hanh@test.local", "Chuyên gia Hạnh Mười Hai", "EXPERT");
        otherExpertId = insertUser("issue12-quang@test.local", "Chuyên gia Quang Mười Hai", "EXPERT");
        customers = new long[] {
            insertUser("issue12-c1@test.local", "Thành viên Một", "CUSTOMER"),
            insertUser("issue12-c2@test.local", "Thành viên Hai", "CUSTOMER"),
            insertUser("issue12-c3@test.local", "Thành viên Ba", "CUSTOMER"),
        };
        busyRecipe = insertRecipe(expertId, "Issue12 Lẩu nấm nhiều báo cáo");
        recentRecipe = insertRecipe(otherExpertId, "Issue12 Gỏi cuốn mới báo cáo");
        tiedRecipe = insertRecipe(otherExpertId, "Issue12 Chè đậu hai lý do");
        insertReport(customers[0], busyRecipe, "FOOD_SAFETY_HAZARD", "RESOLVED", BASE.plusHours(1));
        insertReport(customers[1], busyRecipe, "FOOD_SAFETY_HAZARD", "IN_REVIEW", BASE.plusHours(2));
        insertReport(customers[2], busyRecipe, "SPAM_ADVERTISING", "OPEN", BASE.plusHours(3));
        insertReport(customers[0], tiedRecipe, "SPAM_ADVERTISING", "OPEN", BASE.plusHours(4));
        insertReport(customers[1], tiedRecipe, "OTHER", "OPEN", BASE.plusHours(5));
        insertReport(customers[0], recentRecipe, "COPYRIGHT_VIOLATION", "OPEN", BASE.plusHours(6));
    }

    @AfterEach
    void cleanupFixtures() {
        cleanup();
    }

    @Test
    void mostReportedSortPutsThePostWithMoreMatchingReportsFirst() throws Exception {
        asAdmin(groups().param("query", "Issue12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].recipe.id").value(recentRecipe))
                .andExpect(jsonPath("$.content[1].recipe.id").value(tiedRecipe))
                .andExpect(jsonPath("$.content[2].recipe.id").value(busyRecipe));
        asAdmin(groups().param("query", "Issue12").param("sort", "MOST_REPORTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].recipe.id").value(busyRecipe))
                .andExpect(jsonPath("$.content[0].reportCount").value(3))
                .andExpect(jsonPath("$.content[1].recipe.id").value(tiedRecipe))
                .andExpect(jsonPath("$.content[2].recipe.id").value(recentRecipe));
        asAdmin(groups().param("query", "Issue12").param("sort", "MOST_REPORTED").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].recipe.id").value(tiedRecipe))
                .andExpect(jsonPath("$.content[0].reportCount").value(2))
                .andExpect(jsonPath("$.content[1].recipe.id").value(recentRecipe))
                .andExpect(jsonPath("$.content[2].recipe.id").value(busyRecipe))
                .andExpect(jsonPath("$.content[2].reportCount").value(1));
    }

    @Test
    void eachGroupNamesItsMostCommonReasonAmongTheMatchingReports() throws Exception {
        asAdmin(groups().param("query", "Issue12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(group(busyRecipe, "topReasonCode")).value("FOOD_SAFETY_HAZARD"))
                .andExpect(jsonPath(group(busyRecipe, "topReasonCount")).value(2))
                // A tie goes to the reason reported most recently.
                .andExpect(jsonPath(group(tiedRecipe, "topReasonCode")).value("OTHER"))
                .andExpect(jsonPath(group(tiedRecipe, "topReasonCount")).value(1))
                .andExpect(jsonPath(group(recentRecipe, "topReasonCode")).value("COPYRIGHT_VIOLATION"));
        asAdmin(groups().param("query", "Issue12").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(group(busyRecipe, "topReasonCode")).value("SPAM_ADVERTISING"))
                .andExpect(jsonPath(group(busyRecipe, "topReasonCount")).value(1));
    }

    @Test
    void theQueryFindsAPostByItsTitleOrByItsAuthorsNameOrEmail() throws Exception {
        asAdmin(groups().param("query", "  lẩu NẤM nhiều  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].recipe.id").value(busyRecipe));
        asAdmin(groups().param("query", "Quang Mười Hai"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].recipe.id").value(recentRecipe))
                .andExpect(jsonPath("$.content[1].recipe.id").value(tiedRecipe));
        asAdmin(groups().param("query", "issue12-hanh@"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].recipe.id").value(busyRecipe));
        // LIKE wildcards in the query are plain characters, so "%" does not match every post.
        asAdmin(groups().param("query", "Issue12%nhiều"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void anUnknownSortOrAnOverlongQueryIsRejected() throws Exception {
        asAdmin(groups().param("sort", "OLDEST"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        asAdmin(groups().param("query", "a".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void aNewPostPublishesAtOnceWhileReportsAreBeingHandled() throws Exception {
        long ingredientId = insertIngredient();
        int gramUnitId = jdbcTemplate.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'g'", Integer.class);
        CreateRecipeRequest request = new CreateRecipeRequest("Issue12 Canh bí đăng giữa lúc hậu kiểm", "",
                "Nấu bí đỏ với nước dùng rau củ đến khi mềm.", DishCategory.SOUP, VegetarianType.VEGAN, Difficulty.EASY,
                2, 10, 15, "", List.of(new RecipeIngredientInput(ingredientId, gramUnitId, new BigDecimal("300"))), null);

        mockMvc.perform(post("/api/v1/recipes")
                        .with(principal(otherExpertId, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        Long recipeId = jdbcTemplate.queryForObject("SELECT recipe_id FROM [RECIPE_POST] WHERE author_id = ? AND title = ?",
                Long.class, otherExpertId, "Issue12 Canh bí đăng giữa lúc hậu kiểm");
        mockMvc.perform(get("/api/v1/recipes/{id}", recipeId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/recipes").param("keyword", "Canh bí đăng giữa lúc hậu kiểm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$..[?(@.id == %d)]".formatted(recipeId)).isNotEmpty());
        // The open and in-review reports on the other posts of the same author are untouched.
        asAdmin(groups().param("query", "Quang Mười Hai").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void onlyAdministratorsSearchTheDashboard() throws Exception {
        mockMvc.perform(groups().param("query", "Issue12"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(groups().param("query", "Issue12").with(principal(expertId, "ROLE_EXPERT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    private static MockHttpServletRequestBuilder groups() {
        return get("/api/v1/admin/reports/groups").param("size", "100");
    }

    private static String group(long recipeId, String field) {
        return "$.content[?(@.recipe.id == %d)].%s".formatted(recipeId, field);
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(principal(adminId, "ROLE_ADMIN")));
    }

    private RequestPostProcessor principal(long userId, String role) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority(role));
    }

    private void cleanup() {
        String users = "SELECT user_id FROM [USER] WHERE email LIKE '" + USERS + "'";
        jdbcTemplate.update("DELETE FROM [REPORT] WHERE reporter_id IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [INGREDIENT_UNIT_CONVERSION] WHERE ingredient_id IN "
                + "(SELECT ingredient_id FROM [INGREDIENT] WHERE name = ?)", INGREDIENT_NAME);
        jdbcTemplate.update("DELETE FROM [INGREDIENT] WHERE name = ?", INGREDIENT_NAME);
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE '" + USERS + "'");
    }

    private long insertUser(String email, String displayName, String role) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [USER] (email, display_name, role, account_status, email_verified)
                OUTPUT INSERTED.user_id VALUES (?, ?, ?, 'ACTIVE', 1)
                """, Long.class, email, displayName, role);
    }

    private long insertIngredient() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [INGREDIENT] (name, source_name, reference_date)
                OUTPUT INSERTED.ingredient_id VALUES (?, 'Issue #12 integration fixture', '2026-10-26')
                """, Long.class, INGREDIENT_NAME);
    }

    private long insertRecipe(long author, String title) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type, difficulty,
                    servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id VALUES (?, ?, N'Rửa và chế biến nguyên liệu cho món ăn này.',
                    'HOT_POT', 'VEGAN', 'EASY', 2, 10, 15, 'PUBLISHED', SYSUTCDATETIME())
                """, Long.class, author, title);
    }

    private void insertReport(long reporter, long recipe, String reason, String status, LocalDateTime createdAt) {
        boolean resolved = "RESOLVED".equals(status);
        jdbcTemplate.update("""
                INSERT INTO [REPORT] (reporter_id, recipe_id, reason_code, description, status, decision,
                    decision_reason, handled_by, handled_at, created_at, updated_at)
                VALUES (?, ?, ?, N'Mô tả báo cáo kiểm thử', ?, ?, ?, ?, ?, ?, ?)
                """, reporter, recipe, reason, status, resolved ? "NO_VIOLATION" : null,
                resolved ? "Đã kiểm tra lại công thức" : null, "OPEN".equals(status) ? null : adminId,
                resolved ? createdAt : null, createdAt, createdAt);
    }
}
