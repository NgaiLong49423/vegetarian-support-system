package tech.mamxanh.recipe.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.hasItems;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tech.mamxanh.AbstractIntegrationTest;

class PublicRecipeBrowseIntegrationTest extends AbstractIntegrationTest {
    private static final String EMAIL_PREFIX = "issue3-browse-%@test.local";
    private static final String BROWSE_TEST_KEYWORD = "issue3-browse-token";
    private static final String FILTER_EMAIL = "issue14-filter-author@test.local";
    private static final String FILTER_TOKEN = "issue14-filter-token";
    private static final String FILTER_INGREDIENT_A = "Issue14 Filter Ingredient A";
    private static final String FILTER_INGREDIENT_B = "Issue14 Filter Ingredient B";
    @Autowired private JdbcTemplate jdbcTemplate;

    private long authorId;
    private long recentId;
    private long middleId;
    private long oldId;
    private long filterAuthorId;
    private long ingredientAId;
    private long ingredientBId;
    private int gramUnitId;

    @BeforeEach
    void preparePublicRecipes() {
        jdbcTemplate.update("DELETE FROM [RECIPE_REACTION] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?))", EMAIL_PREFIX);
        jdbcTemplate.update("DELETE FROM [RECIPE_VIEW] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?))", EMAIL_PREFIX);
        jdbcTemplate.update("DELETE FROM [COMMENT] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?))", EMAIL_PREFIX);
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?)", EMAIL_PREFIX);
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE ?", EMAIL_PREFIX);
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email = ?)", FILTER_EMAIL);
        jdbcTemplate.update("DELETE FROM [USER] WHERE email = ?", FILTER_EMAIL);
        jdbcTemplate.update("DELETE FROM [INGREDIENT] WHERE name IN (?, ?)", FILTER_INGREDIENT_A, FILTER_INGREDIENT_B);
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, avatar_url, role, account_status, email_verified) VALUES (?, ?, ?, 'EXPERT', 'ACTIVE', 1)",
                FILTER_EMAIL, "Tác giả lọc", "https://img.test/filter-avatar.png");
        filterAuthorId = jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, FILTER_EMAIL);
        jdbcTemplate.update("INSERT INTO [INGREDIENT] (name, source_name, reference_date) VALUES (?, ?, '2026-10-04')",
                FILTER_INGREDIENT_A, "Issue #14 SQL Server integration fixture");
        jdbcTemplate.update("INSERT INTO [INGREDIENT] (name, source_name, reference_date) VALUES (?, ?, '2026-10-04')",
                FILTER_INGREDIENT_B, "Issue #14 SQL Server integration fixture");
        ingredientAId = jdbcTemplate.queryForObject("SELECT ingredient_id FROM [INGREDIENT] WHERE name = ?", Long.class, FILTER_INGREDIENT_A);
        ingredientBId = jdbcTemplate.queryForObject("SELECT ingredient_id FROM [INGREDIENT] WHERE name = ?", Long.class, FILTER_INGREDIENT_B);
        gramUnitId = jdbcTemplate.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'g'", Integer.class);
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, avatar_url, role, account_status, email_verified) VALUES (?, ?, ?, 'EXPERT', 'ACTIVE', 1)",
                "issue3-browse-author@test.local", "Tác giả công khai", "https://img.test/avatar.png");
        authorId = jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class,
                "issue3-browse-author@test.local");
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC);
        recentId = insertRecipe("Issue3-browse-token Canh chua mới", "Issue3-description-token Mô tả tìm kiếm đặc biệt", "PUBLISHED", now.minusHours(1));
        middleId = insertRecipe("Issue3-browse-token Món giữa", "Món đăng năm ngày trước", "PUBLISHED", now.minusDays(5));
        oldId = insertRecipe("Issue3-browse-token Canh chua cũ", "Công thức lâu năm", "PUBLISHED", now.minusDays(10));
        insertRecipe("Issue3-browse-token Canh chua ẩn", "Không được tìm thấy", "HIDDEN", now);
    }

    @Test
    void guestSearchesTitleAndDescriptionAndGetsPublicFieldsOnlyWithDefaultPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/recipes").param("keyword", "ISSUE3-DESCRIPTION-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(recentId))
                .andExpect(jsonPath("$.items[0].authorName").value("Tác giả công khai"))
                .andExpect(jsonPath("$.items[0].authorAvatarUrl").value("https://img.test/avatar.png"))
                .andExpect(jsonPath("$.items[0].likes").value(0))
                .andExpect(jsonPath("$.items[0].likePercentage").value(nullValue()))
                .andExpect(jsonPath("$.items[0].viewCount").value(0))
                .andExpect(jsonPath("$.items[0].authorEmail").doesNotExist());

        mockMvc.perform(get("/api/v1/recipes").param("keyword", BROWSE_TEST_KEYWORD + " CANH CHUA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void defaultsToNewestAndPaginatesAtTwelveUnlessCallerChoosesAnotherSize() throws Exception {
        long newestId = 0;
        for (int index = 0; index < 12; index++) {
            long recipeId = insertRecipe("Issue3-browse-token Thêm món " + index, "Mô tả", "PUBLISHED",
                    LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC).minusMinutes(index + 2));
            if (index == 0) newestId = recipeId;
        }
        mockMvc.perform(get("/api/v1/recipes").param("keyword", BROWSE_TEST_KEYWORD).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.items[0].id").value(newestId));
        mockMvc.perform(get("/api/v1/recipes").param("keyword", BROWSE_TEST_KEYWORD).param("size", "51"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/recipes").param("sort", "unknown"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mostViewedHonorsTimeWindowAndAllTimeDefaultWithStableTieBreakers() throws Exception {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC);
        for (int index = 0; index < 5; index++) insertView(oldId, now.minusDays(9));
        for (int index = 0; index < 3; index++) insertView(middleId, now.minusDays(4));
        insertView(recentId, LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC).minusHours(2));

        mockMvc.perform(get("/api/v1/recipes").param("keyword", BROWSE_TEST_KEYWORD).param("sort", "MOST_VIEWED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(oldId));
        mockMvc.perform(get("/api/v1/recipes").param("keyword", BROWSE_TEST_KEYWORD).param("sort", "MOST_VIEWED").param("viewPeriod", "LAST_24_HOURS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(recentId));
        mockMvc.perform(get("/api/v1/recipes").param("keyword", BROWSE_TEST_KEYWORD).param("sort", "MOST_VIEWED").param("viewPeriod", "LAST_7_DAYS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(middleId));
        mockMvc.perform(get("/api/v1/recipes").param("keyword", BROWSE_TEST_KEYWORD).param("sort", "MOST_VIEWED").param("viewPeriod", "LAST_30_DAYS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(oldId));
    }

    @Test
    void supportsTheOtherFourModesUsingCurrentReactionCommentAndViewRecords() throws Exception {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC);
        insertReaction(oldId, "issue3-browse-voter1@test.local", "LIKE", now.minusDays(2));
        insertReaction(oldId, "issue3-browse-voter2@test.local", "LIKE", now.minusDays(2));
        for (int index = 0; index < 4; index++) {
            jdbcTemplate.update("INSERT INTO [COMMENT] (recipe_id, user_id, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                    oldId, authorId, "Bình luận " + index, now.minusDays(1), now.minusDays(1));
        }

        mockMvc.perform(get("/api/v1/recipes").param("keyword", BROWSE_TEST_KEYWORD).param("sort", "MOST_LIKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(oldId))
                .andExpect(jsonPath("$.items[0].likes").value(2))
                .andExpect(jsonPath("$.items[0].likePercentage").value(100));
        mockMvc.perform(get("/api/v1/recipes").param("keyword", BROWSE_TEST_KEYWORD).param("sort", "MOST_COMMENTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(oldId));
        mockMvc.perform(get("/api/v1/recipes").param("keyword", BROWSE_TEST_KEYWORD).param("sort", "MOST_ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(oldId));
        mockMvc.perform(get("/api/v1/recipes").param("keyword", BROWSE_TEST_KEYWORD).param("sort", "TRENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(recentId));
    }

    @Test
    void futurePublishedAtDoesNotCauseInvalidTrendingPowerCalculation() throws Exception {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC);
        long futureId = insertRecipe("Issue3-browse-token Canh chua hẹn giờ", "Bài có thời điểm công khai tương lai", "PUBLISHED",
                now.plusHours(4));

        mockMvc.perform(get("/api/v1/recipes").param("keyword", BROWSE_TEST_KEYWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(futureId));

        mockMvc.perform(get("/api/v1/recipes").param("keyword", BROWSE_TEST_KEYWORD).param("sort", "TRENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(recentId));
    }

    @Test
    void filtersPublicRecipesByEachCriterionAndCombinesSelectedIngredientsWithAnd() throws Exception {
        long bothIngredients = insertFilterRecipe("đủ nguyên liệu", "PUBLISHED", "VEGAN", "SOUP", 5, 10,
                ingredientAId, ingredientBId);
        long onlyIngredientA = insertFilterRecipe("chỉ A", "PUBLISHED", "VEGAN", "SOUP", 10, 25, ingredientAId);
        long onlyIngredientB = insertFilterRecipe("chỉ B", "PUBLISHED", "LACTO", "FRIED", 5, 10, ingredientBId);
        insertFilterRecipe("bài ẩn", "HIDDEN", "VEGAN", "SOUP", 5, 10, ingredientAId, ingredientBId);
        String keyword = FILTER_TOKEN;

        mockMvc.perform(get("/api/v1/recipes").param("keyword", keyword).param("vegetarianType", "VEGAN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[*].id", org.hamcrest.Matchers.containsInAnyOrder(bothIngredients, onlyIngredientA)));
        mockMvc.perform(get("/api/v1/recipes").param("keyword", keyword).param("dishCategory", "SOUP"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/recipes").param("keyword", keyword).param("maxTotalTimeMinutes", "15"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[*].id", org.hamcrest.Matchers.containsInAnyOrder(bothIngredients, onlyIngredientB)));
        mockMvc.perform(get("/api/v1/recipes").param("keyword", keyword)
                        .param("ingredientIds", Long.toString(ingredientAId), Long.toString(ingredientBId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(bothIngredients));
        mockMvc.perform(get("/api/v1/recipes").param("keyword", keyword).param("vegetarianType", "VEGAN")
                        .param("dishCategory", "SOUP").param("maxTotalTimeMinutes", "15")
                        .param("ingredientIds", Long.toString(ingredientAId), Long.toString(ingredientBId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(bothIngredients));
        mockMvc.perform(get("/api/v1/recipes").param("keyword", keyword).param("ingredientIds", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/recipes").param("keyword", keyword).param("dishCategory", "NOT_A_CATEGORY"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/recipes").param("keyword", keyword).param("maxTotalTimeMinutes", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatedOpenApiDocumentsAllPublicBrowseFilterParameters() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/recipes'].get.parameters[*].name", hasItems(
                        "keyword", "page", "size", "sort", "viewPeriod", "vegetarianType",
                        "dishCategory", "ingredientIds", "maxTotalTimeMinutes")));
    }

    private long insertRecipe(String title, String description, String status, LocalDateTime publishedAt) {
        jdbcTemplate.update("""
                INSERT INTO [RECIPE_POST]
                (author_id, title, description, instructions, dish_category, vegetarian_type, difficulty,
                 servings, prep_time_min, cook_time_min, status, published_at)
                VALUES (?, ?, ?, N'Nấu đến khi chín và nêm vừa ăn.', 'SOUP', 'VEGAN', 'EASY', 2, 5, 10, ?, ?)
                """, authorId, title, description, status, publishedAt);
        return jdbcTemplate.queryForObject("SELECT recipe_id FROM [RECIPE_POST] WHERE author_id = ? AND title = ?",
                Long.class, authorId, title);
    }

    private long insertFilterRecipe(String title, String status, String vegetarianType, String category,
            int prepMinutes, int cookMinutes, long... ingredients) {
        String fullTitle = FILTER_TOKEN + " " + title;
        jdbcTemplate.update("""
                INSERT INTO [RECIPE_POST]
                (author_id, title, description, instructions, dish_category, vegetarian_type, difficulty,
                 servings, prep_time_min, cook_time_min, status, published_at)
                VALUES (?, ?, N'Issue #14 filter description', N'Nấu đến khi chín và nêm vừa ăn.', ?, ?, 'EASY', 2, ?, ?, ?, ?)
                """, filterAuthorId, fullTitle, category, vegetarianType, prepMinutes, cookMinutes, status,
                LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC));
        long recipeId = jdbcTemplate.queryForObject("SELECT recipe_id FROM [RECIPE_POST] WHERE author_id = ? AND title = ?",
                Long.class, filterAuthorId, fullTitle);
        for (long ingredientId : ingredients) {
            jdbcTemplate.update("INSERT INTO [RECIPE_INGREDIENT] (recipe_id, ingredient_id, unit_id, quantity) VALUES (?, ?, ?, 100)",
                    recipeId, ingredientId, gramUnitId);
        }
        return recipeId;
    }

    private void insertView(long recipeId, LocalDateTime viewedAt) {
        jdbcTemplate.update("INSERT INTO [RECIPE_VIEW] (recipe_id, viewed_at) VALUES (?, ?)", recipeId, viewedAt);
    }

    private void insertReaction(long recipeId, String voterEmail, String type, LocalDateTime at) {
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, 'Issue 3 voter', 'CUSTOMER', 'ACTIVE', 1)",
                voterEmail);
        long voterId = jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, voterEmail);
        jdbcTemplate.update("INSERT INTO [RECIPE_REACTION] (user_id, recipe_id, reaction_type, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
                voterId, recipeId, type, at, at);
    }
}
