package tech.mamxanh.recipe.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.mamxanh.AbstractIntegrationTest;

/**
 * AC-04.4, AC-04.7 and BR-64 for the recipe media API: only the active Expert author may replace or
 * remove the images of a Recipe Post, and an Administrator is not an author (Q50).
 */
class RecipeMediaAuthorizationIntegrationTest extends AbstractIntegrationTest {
    private static final String ORIGINAL_COVER = "https://cdn.test/issue11-original-cover.jpg";
    private static final String NEW_COVER = "https://cdn.test/issue11-new-cover.jpg";
    private static final String REPLACE_BODY = """
            {"mediaItems":[{"blobUrl":"%s","mimeType":"image/jpeg","displayOrder":1,"isCover":true}]}
            """.formatted(NEW_COVER);

    @Autowired private JdbcTemplate jdbcTemplate;

    private long ownerId;
    private long otherExpertId;
    private long adminId;
    private long recipeId;
    private long hiddenRecipeId;

    @BeforeEach
    void prepareDatabaseFixtures() {
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'issue11-%@test.local')");
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE 'issue11-%@test.local'");

        ownerId = insertUser("issue11-owner@test.local", "EXPERT");
        otherExpertId = insertUser("issue11-other@test.local", "EXPERT");
        adminId = insertUser("issue11-admin@test.local", "ADMIN");
        recipeId = insertRecipe(ownerId, "PUBLISHED");
        hiddenRecipeId = insertRecipe(ownerId, "HIDDEN");
        jdbcTemplate.update("INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover) VALUES (?, ?, 'image/jpeg', 1, 1)",
                recipeId, ORIGINAL_COVER);
        jdbcTemplate.update("INSERT INTO [RECIPE_MEDIA] (recipe_id, blob_url, mime_type, display_order, is_cover) VALUES (?, ?, 'image/jpeg', 1, 1)",
                hiddenRecipeId, ORIGINAL_COVER);
    }

    @Test
    void anotherExpertCannotReplaceOrDeleteTheImagesOfARecipeTheyDoNotOwn() throws Exception {
        mockMvc.perform(put("/api/v1/recipes/{recipeId}/media", recipeId).with(principal(otherExpertId, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON).content(REPLACE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RECIPE_EDIT_NOT_ALLOWED"));
        mockMvc.perform(delete("/api/v1/recipes/{recipeId}/media", recipeId).with(principal(otherExpertId, "ROLE_EXPERT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RECIPE_EDIT_NOT_ALLOWED"));

        assertThat(mediaUrls(recipeId)).containsExactly(ORIGINAL_COVER);
    }

    @Test
    void administratorCannotUploadReplaceOrDeleteRecipeImages() throws Exception {
        mockMvc.perform(multipart("/api/v1/recipes/media/upload").file(jpeg()).with(principal(adminId, "ROLE_ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/recipes/{recipeId}/media", recipeId).with(principal(adminId, "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(REPLACE_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/recipes/{recipeId}/media", recipeId).with(principal(adminId, "ROLE_ADMIN")))
                .andExpect(status().isForbidden());

        assertThat(mediaUrls(recipeId)).containsExactly(ORIGINAL_COVER);
    }

    @Test
    void imagesOfAMissingRecipeCannotBeReplacedOrDeleted() throws Exception {
        long missingRecipeId = recipeId + 1_000_000;
        mockMvc.perform(put("/api/v1/recipes/{recipeId}/media", missingRecipeId).with(principal(ownerId, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON).content(REPLACE_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
        mockMvc.perform(delete("/api/v1/recipes/{recipeId}/media", missingRecipeId).with(principal(ownerId, "ROLE_EXPERT")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
    }

    @Test
    void imagesOfARecipeHiddenByAnAdministratorStayUnchanged() throws Exception {
        mockMvc.perform(put("/api/v1/recipes/{recipeId}/media", hiddenRecipeId).with(principal(ownerId, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON).content(REPLACE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RECIPE_HIDDEN"));
        mockMvc.perform(delete("/api/v1/recipes/{recipeId}/media", hiddenRecipeId).with(principal(ownerId, "ROLE_EXPERT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RECIPE_HIDDEN"));

        assertThat(mediaUrls(hiddenRecipeId)).containsExactly(ORIGINAL_COVER);
    }

    @Test
    void theExpertAuthorCanUploadReplaceAndDeleteTheirRecipeImages() throws Exception {
        mockMvc.perform(multipart("/api/v1/recipes/media/upload").file(jpeg()).with(principal(ownerId, "ROLE_EXPERT")))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/v1/recipes/{recipeId}/media", recipeId).with(principal(ownerId, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON).content(REPLACE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].blobUrl").value(NEW_COVER));
        assertThat(mediaUrls(recipeId)).containsExactly(NEW_COVER);

        mockMvc.perform(delete("/api/v1/recipes/{recipeId}/media", recipeId).with(principal(ownerId, "ROLE_EXPERT")))
                .andExpect(status().isOk());
        assertThat(mediaUrls(recipeId)).isEmpty();
    }

    @Test
    void generatedOpenApiDeclaresOnlyTheImageChangesAsAuthenticated() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/media/upload'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}/media'].put.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}/media'].delete.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}/media'].get.security").doesNotExist());
    }

    private static MockMultipartFile jpeg() {
        return new MockMultipartFile("file", "issue11.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0});
    }

    private static RequestPostProcessor principal(long userId, String role) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority(role));
    }

    private List<String> mediaUrls(long id) {
        return jdbcTemplate.queryForList("SELECT blob_url FROM [RECIPE_MEDIA] WHERE recipe_id = ? ORDER BY display_order",
                String.class, id);
    }

    private long insertUser(String email, String role) {
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, ?, ?, 'ACTIVE', 1)",
                email, "Issue 11 test", role);
        return jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }

    private long insertRecipe(long authorId, String recipeStatus) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type, difficulty,
                    servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id VALUES (?, N'Đậu hũ hấp nấm', N'Rửa và chế biến nguyên liệu cho món ăn này.',
                    'BRAISED', 'VEGAN', 'EASY', 2, 10, 15, ?, SYSUTCDATETIME())
                """, Long.class, authorId, recipeStatus);
    }
}
