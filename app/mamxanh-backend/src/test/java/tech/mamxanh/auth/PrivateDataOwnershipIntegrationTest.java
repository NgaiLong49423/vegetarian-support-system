package tech.mamxanh.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.mamxanh.AbstractIntegrationTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * AC-23.8 (owner-only by default): private member data is always read for the account in the
 * session; no private operation accepts another account's id, and an Administrator is refused.
 */
class PrivateDataOwnershipIntegrationTest extends AbstractIntegrationTest {
    private static final List<String> PRIVATE_READS = List.of("/api/v1/saved-recipes", "/api/v1/meal-plans",
            "/api/v1/nutrition/profile", "/api/v1/nutrition/dietary-preferences", "/api/v1/me/profile");

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JsonMapper jsonMapper;

    private long memberA;
    private long memberB;
    private long adminId;
    private long recipeSavedByA;
    private long recipeSavedByB;

    @BeforeEach
    void prepareDatabaseFixtures() {
        deleteFixtures();
        memberA = insertUser("a", "CUSTOMER");
        memberB = insertUser("b", "CUSTOMER");
        adminId = insertUser("admin", "ADMIN");
        long expert = insertUser("expert", "EXPERT");
        recipeSavedByA = insertRecipe(expert, "Món A đã lưu");
        recipeSavedByB = insertRecipe(expert, "Món B đã lưu");
        jdbcTemplate.update("INSERT INTO [SAVED_RECIPE] (user_id, recipe_id) VALUES (?, ?)", memberA, recipeSavedByA);
        jdbcTemplate.update("INSERT INTO [SAVED_RECIPE] (user_id, recipe_id) VALUES (?, ?)", memberB, recipeSavedByB);
    }

    /** Other integration classes delete every user, so no recipe may outlive this class (see BUG-014). */
    @AfterEach
    void deleteFixtures() {
        jdbcTemplate.update("DELETE FROM [SAVED_RECIPE] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'issue29-private-%@test.local')");
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'issue29-private-%@test.local')");
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE 'issue29-private-%@test.local'");
    }

    @Test
    void noPrivateOperationLetsTheCallerChooseWhoseDataIsRead() throws Exception {
        JsonNode paths = jsonMapper.readTree(mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse()
                .getContentAsString()).get("paths");
        List<String> idParameters = new ArrayList<>();
        for (String path : PRIVATE_READS) {
            assertThat(paths.has(path)).as(path).isTrue();
            assertThat(path).doesNotContain("{");
            for (JsonNode operation : paths.get(path)) {
                JsonNode parameters = operation.get("parameters");
                if (parameters == null) continue;
                for (JsonNode parameter : parameters) {
                    String name = parameter.get("name").asString().toLowerCase(java.util.Locale.ROOT);
                    if (name.contains("user") || name.contains("member") || name.contains("owner")) {
                        idParameters.add(path + " " + name);
                    }
                }
            }
        }
        assertThat(idParameters).isEmpty();
    }

    @Test
    void anotherAccountIdInTheQueryIsIgnoredAndOnlyTheSessionOwnersDataIsReturned() throws Exception {
        mockMvc.perform(get("/api/v1/saved-recipes").param("userId", Long.toString(memberB))
                        .with(principal(memberA, "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].recipeId").value(recipeSavedByA));
        mockMvc.perform(get("/api/v1/me/profile").param("userId", Long.toString(memberB))
                        .with(principal(memberA, "ROLE_CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(memberA));
    }

    @Test
    void administratorIsRefusedOnMemberPrivateData() throws Exception {
        for (String path : List.of("/api/v1/saved-recipes", "/api/v1/nutrition/profile",
                "/api/v1/nutrition/dietary-preferences", "/api/v1/me/profile")) {
            mockMvc.perform(get(path).param("userId", Long.toString(memberA)).with(principal(adminId, "ROLE_ADMIN")))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v1/meal-plans").param("weekStartDate", "2026-10-05")
                        .with(principal(adminId, "ROLE_ADMIN")))
                .andExpect(status().isForbidden());
    }

    private static RequestPostProcessor principal(long userId, String role) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority(role));
    }

    private long insertUser(String key, String role) {
        String email = "issue29-private-" + key + "@test.local";
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, ?, ?, 'ACTIVE', 1)",
                email, "Issue 29 private " + key, role);
        return jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }

    private long insertRecipe(long authorId, String title) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type, difficulty,
                    servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id VALUES (?, ?, N'Rửa và chế biến nguyên liệu cho món ăn này.',
                    'BRAISED', 'VEGAN', 'EASY', 2, 10, 15, 'PUBLISHED', SYSUTCDATETIME())
                """, Long.class, authorId, title);
    }
}
