package tech.mamxanh.recipe.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tech.mamxanh.AbstractIntegrationTest;

class PublicRecipeBrowseIntegrationTest extends AbstractIntegrationTest {
    private static final String EMAIL_PREFIX = "issue3-browse-%@test.local";
    @Autowired private JdbcTemplate jdbcTemplate;

    private long authorId;
    private long recentId;
    private long middleId;
    private long oldId;

    @BeforeEach
    void preparePublicRecipes() {
        jdbcTemplate.update("DELETE FROM [RECIPE_REACTION] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?))", EMAIL_PREFIX);
        jdbcTemplate.update("DELETE FROM [RECIPE_VIEW] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?))", EMAIL_PREFIX);
        jdbcTemplate.update("DELETE FROM [COMMENT] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?))", EMAIL_PREFIX);
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?)", EMAIL_PREFIX);
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE ?", EMAIL_PREFIX);
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, avatar_url, role, account_status, email_verified) VALUES (?, ?, ?, 'EXPERT', 'ACTIVE', 1)",
                "issue3-browse-author@test.local", "Tác giả công khai", "https://img.test/avatar.png");
        authorId = jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class,
                "issue3-browse-author@test.local");
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC);
        recentId = insertRecipe("Canh chua mới", "Mô tả tìm kiếm đặc biệt", "PUBLISHED", now.minusHours(1));
        middleId = insertRecipe("Món giữa", "Món đăng năm ngày trước", "PUBLISHED", now.minusDays(5));
        oldId = insertRecipe("Canh chua cũ", "Công thức lâu năm", "PUBLISHED", now.minusDays(10));
        insertRecipe("Canh chua ẩn", "Không được tìm thấy", "HIDDEN", now);
    }

    @Test
    void guestSearchesTitleAndDescriptionAndGetsPublicFieldsOnlyWithDefaultPageSize() throws Exception {
        mockMvc.perform(get("/api/v1/recipes").param("keyword", "TÌM KIẾM ĐẶC BIỆT"))
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

        mockMvc.perform(get("/api/v1/recipes").param("keyword", "CANH CHUA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void defaultsToNewestAndPaginatesAtTwelveUnlessCallerChoosesAnotherSize() throws Exception {
        for (int index = 0; index < 12; index++) {
            insertRecipe("Thêm món " + index, "Mô tả", "PUBLISHED",
                    LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC).minusMinutes(index + 2));
        }
        mockMvc.perform(get("/api/v1/recipes").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.items[0].id").value(recentId));
        mockMvc.perform(get("/api/v1/recipes").param("size", "51"))
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

        mockMvc.perform(get("/api/v1/recipes").param("sort", "MOST_VIEWED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(oldId));
        mockMvc.perform(get("/api/v1/recipes").param("sort", "MOST_VIEWED").param("viewPeriod", "LAST_24_HOURS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(recentId));
        mockMvc.perform(get("/api/v1/recipes").param("sort", "MOST_VIEWED").param("viewPeriod", "LAST_7_DAYS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(middleId));
        mockMvc.perform(get("/api/v1/recipes").param("sort", "MOST_VIEWED").param("viewPeriod", "LAST_30_DAYS"))
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

        mockMvc.perform(get("/api/v1/recipes").param("sort", "MOST_LIKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(oldId))
                .andExpect(jsonPath("$.items[0].likes").value(2))
                .andExpect(jsonPath("$.items[0].likePercentage").value(100));
        mockMvc.perform(get("/api/v1/recipes").param("sort", "MOST_COMMENTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(oldId));
        mockMvc.perform(get("/api/v1/recipes").param("sort", "MOST_ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(oldId));
        mockMvc.perform(get("/api/v1/recipes").param("sort", "TRENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(recentId));
    }

    @Test
    void futurePublishedAtDoesNotCauseInvalidTrendingPowerCalculation() throws Exception {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), java.time.ZoneOffset.UTC);
        long futureId = insertRecipe("Canh chua hẹn giờ", "Bài có thời điểm công khai tương lai", "PUBLISHED",
                now.plusHours(4));

        mockMvc.perform(get("/api/v1/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(futureId));

        mockMvc.perform(get("/api/v1/recipes").param("sort", "TRENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(recentId));
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
