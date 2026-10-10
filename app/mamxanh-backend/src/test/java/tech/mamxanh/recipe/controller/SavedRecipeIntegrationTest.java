package tech.mamxanh.recipe.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tech.mamxanh.AbstractIntegrationTest;

class SavedRecipeIntegrationTest extends AbstractIntegrationTest {
    private static final String EMAIL_PREFIX = "issue37-saved-";
    @Autowired private JdbcTemplate jdbcTemplate;

    private long authorId;
    private long memberId;
    private long otherMemberId;
    private long lockedMemberId;
    private long titleMatchId;
    private long descriptionMatchId;
    private long unrelatedId;
    private long hiddenId;
    private long deletedId;

    @BeforeEach
    void prepareFixtures() {
        jdbcTemplate.update("DELETE FROM [SAVED_RECIPE] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?)",
                EMAIL_PREFIX + "%@test.local");
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?)",
                EMAIL_PREFIX + "%@test.local");
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE ?", EMAIL_PREFIX + "%@test.local");

        authorId = insertUser("author", "EXPERT", "ACTIVE");
        memberId = insertUser("member", "CUSTOMER", "ACTIVE");
        otherMemberId = insertUser("other", "CUSTOMER", "ACTIVE");
        lockedMemberId = insertUser("locked", "CUSTOMER", "LOCKED");

        titleMatchId = insertRecipe("Canh chua rau răm", "Bữa cơm gia đình", "PUBLISHED");
        descriptionMatchId = insertRecipe("Đậu hũ rim", "Canh chua chay miền Nam", "PUBLISHED");
        unrelatedId = insertRecipe("Rau xào", "Rau theo mùa xào ngon", "PUBLISHED");
        hiddenId = insertRecipe("Canh chua đang ẩn", "Không còn công khai", "HIDDEN");
        deletedId = insertRecipe("Canh chua đã gỡ", "Bài đã xóa", "DELETED");
    }

    @Test
    void saveAndUnsaveAreIdempotentAndScopedToTheCurrentMember() throws Exception {
        mockMvc.perform(put("/api/v1/saved-recipes/{recipeId}", titleMatchId))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/saved-recipes/{recipeId}", titleMatchId)
                        .with(principal(memberId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/v1/saved-recipes/{recipeId}", titleMatchId)
                        .with(principal(memberId)))
                .andExpect(status().isNoContent());
        org.assertj.core.api.Assertions.assertThat(savedCount(memberId, titleMatchId)).isEqualTo(1);

        mockMvc.perform(get("/api/v1/saved-recipes").with(principal(otherMemberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(delete("/api/v1/saved-recipes/{recipeId}", titleMatchId)
                        .with(principal(otherMemberId)))
                .andExpect(status().isNoContent());
        org.assertj.core.api.Assertions.assertThat(savedCount(memberId, titleMatchId)).isEqualTo(1);

        mockMvc.perform(delete("/api/v1/saved-recipes/{recipeId}", titleMatchId)
                        .with(principal(memberId)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/saved-recipes/{recipeId}", titleMatchId)
                        .with(principal(memberId)))
                .andExpect(status().isNoContent());
        org.assertj.core.api.Assertions.assertThat(savedCount(memberId, titleMatchId)).isZero();
    }

    @Test
    void onlyActiveMembersCanSavePublishedRecipes() throws Exception {
        mockMvc.perform(put("/api/v1/saved-recipes/{recipeId}", hiddenId)
                        .with(principal(memberId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/saved-recipes/{recipeId}", deletedId)
                        .with(principal(memberId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/saved-recipes/{recipeId}", Long.MAX_VALUE)
                        .with(principal(memberId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/saved-recipes/{recipeId}", titleMatchId)
                        .with(principal(lockedMemberId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/saved-recipes/{recipeId}", 0)
                        .with(principal(memberId)))
                .andExpect(status().isBadRequest());
        org.assertj.core.api.Assertions.assertThat(savedCount(memberId, titleMatchId)).isZero();
    }

    @Test
    void listSearchesAvailableTitleAndDescriptionAndPaginatesNewestFirstWithSafeTombstones() throws Exception {
        saveFixture(memberId, titleMatchId, "2026-10-01T08:00:00");
        saveFixture(memberId, descriptionMatchId, "2026-10-02T08:00:00");
        saveFixture(memberId, unrelatedId, "2026-10-03T08:00:00");
        saveFixture(memberId, hiddenId, "2026-10-04T08:00:00");
        saveFixture(memberId, deletedId, "2026-10-05T08:00:00");
        saveFixture(otherMemberId, unrelatedId, "2026-10-06T08:00:00");

        mockMvc.perform(get("/api/v1/saved-recipes").param("keyword", "CANH CHUA")
                        .with(principal(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].recipeId", hasItems((int) titleMatchId, (int) descriptionMatchId)))
                .andExpect(jsonPath("$.content[?(@.recipeId == " + titleMatchId + ")].description")
                        .value(hasItems("Bữa cơm gia đình")));
        mockMvc.perform(get("/api/v1/saved-recipes").param("keyword", "xào ngon")
                        .with(principal(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].recipeId").value(unrelatedId));
        mockMvc.perform(get("/api/v1/saved-recipes").param("keyword", "không tồn tại")
                        .with(principal(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(get("/api/v1/saved-recipes").param("page", "0").param("size", "2")
                        .with(principal(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.content[0].recipeId").value(deletedId))
                .andExpect(jsonPath("$.content[0].available").value(false))
                .andExpect(jsonPath("$.content[0].title").value(nullValue()))
                .andExpect(jsonPath("$.content[0].description").value(nullValue()))
                .andExpect(jsonPath("$.content[0].authorName").value(nullValue()))
                .andExpect(jsonPath("$.content[0].coverUrl").value(nullValue()))
                .andExpect(jsonPath("$.content[0].unavailableMessage").value("Công thức không còn khả dụng"))
                .andExpect(jsonPath("$.content[1].recipeId").value(hiddenId));
        mockMvc.perform(get("/api/v1/saved-recipes").param("page", "1").param("size", "2")
                        .with(principal(memberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].recipeId").value(unrelatedId))
                .andExpect(jsonPath("$.content[1].recipeId").value(descriptionMatchId));
    }

    @Test
    void savedRecipeEndpointsRequireAnActiveMemberAndValidateSearchAndPaging() throws Exception {
        mockMvc.perform(get("/api/v1/saved-recipes")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/saved-recipes/{recipeId}", hiddenId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/saved-recipes").param("page", "-1")
                        .with(principal(memberId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/saved-recipes").param("size", "51")
                        .with(principal(memberId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/saved-recipes").param("keyword", "x".repeat(121))
                        .with(principal(memberId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/saved-recipes").param("size", "0")
                        .with(principal(memberId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void generatedOpenApiDeclaresSavedRecipeReadAndWriteOperationsAsAuthenticated() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/saved-recipes'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/saved-recipes/{recipeId}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/v1/saved-recipes/{recipeId}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/v1/saved-recipes'].get.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/saved-recipes/{recipeId}'].put.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/saved-recipes/{recipeId}'].delete.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/saved-recipes'].get.parameters[?(@.name == 'keyword')]").isNotEmpty());
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor principal(long userId) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private long insertUser(String name, String role, String status) {
        String email = EMAIL_PREFIX + name + "@test.local";
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, ?, ?, ?, 1)",
                email, "Issue 37 " + name, role, status);
        return jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }

    private long insertRecipe(String title, String description, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, description, instructions, dish_category,
                    vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id
                VALUES (?, ?, ?, N'Rửa và chế biến nguyên liệu cho món ăn này.', 'SOUP',
                    'VEGAN', 'EASY', 2, 10, 15, ?, SYSUTCDATETIME())
                """, Long.class, authorId, title, description, status);
    }

    private void saveFixture(long userId, long recipeId, String savedAt) {
        jdbcTemplate.update("INSERT INTO [SAVED_RECIPE] (user_id, recipe_id, saved_at) VALUES (?, ?, ?)",
                userId, recipeId, java.sql.Timestamp.valueOf(savedAt.replace('T', ' ')));
    }

    private int savedCount(long userId, long recipeId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM [SAVED_RECIPE] WHERE user_id = ? AND recipe_id = ?",
                Integer.class, userId, recipeId);
    }
}
