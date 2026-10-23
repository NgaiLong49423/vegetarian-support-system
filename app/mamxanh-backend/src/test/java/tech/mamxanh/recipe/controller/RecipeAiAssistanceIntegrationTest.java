package tech.mamxanh.recipe.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.integration.ai.AiClient;
import tools.jackson.databind.json.JsonMapper;

class RecipeAiAssistanceIntegrationTest extends AbstractIntegrationTest {
    private static final String PREFIX = "fr21-ai-";
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JsonMapper jsonMapper;
    @MockitoBean private AiClient aiClient;
    private long plusExpertId;
    private long freeExpertId;
    private long ingredientId;
    private int unitId;

    @BeforeEach
    void prepareFixtures() {
        jdbcTemplate.update("DELETE FROM [SUBSCRIPTION] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?)", PREFIX + "%");
        jdbcTemplate.update("DELETE FROM [INGREDIENT] WHERE name = ?", "FR21 integration tofu");
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE ?", PREFIX + "%");
        plusExpertId = insertUser("plus", "EXPERT");
        freeExpertId = insertUser("free", "EXPERT");
        jdbcTemplate.update("INSERT INTO [INGREDIENT] (name, source_name, reference_date) VALUES (?, ?, '2026-09-28')",
                "FR21 integration tofu", "FR21 test fixture");
        ingredientId = jdbcTemplate.queryForObject("SELECT ingredient_id FROM [INGREDIENT] WHERE name = ?", Long.class, "FR21 integration tofu");
        unitId = jdbcTemplate.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'g'", Integer.class);
        jdbcTemplate.update("INSERT INTO [SUBSCRIPTION] (user_id, tier, status, starts_at, ends_at) VALUES (?, 'PLUS', 'ACTIVE', ?, ?)",
                plusExpertId, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0));
        when(aiClient.generateRecipeContent(anyString(), anyList())).thenReturn(new tech.mamxanh.integration.ai.AiClient.RecommendationResult("""
                {"description":"Mô tả công thức thử nghiệm.","steps":[{"text":"Đảo đều nguyên liệu trên chảo nóng.","ingredientIds":[%d]}]}
                """.formatted(ingredientId), 150, 300, 450));
    }

    @Test
    void requiresAuthenticationAndRejectsFreePlanBeforeGemini() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/ai-assistance").contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/recipes/ai-assistance").with(principal(freeExpertId))
                        .contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AI_SUBSCRIPTION_REQUIRED"));
        verify(aiClient, never()).generateRecipeContent(anyString(), anyList());
    }

    @Test
    void activePlusSubscriptionCallsGeminiAndReturnsEditableTextWithoutSavingDraft() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/ai-assistance").with(principal(plusExpertId))
                        .contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentType").value("INSTRUCTIONS"))
                .andExpect(jsonPath("$.content").value(org.hamcrest.Matchers.containsString("FR21 integration tofu")));
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [RECIPE_POST] WHERE author_id = ?", Integer.class, plusExpertId)).isZero();
    }

    @Test
    void expiredSubscriptionIsDeniedByTheSqlServerEntitlementQuery() throws Exception {
        jdbcTemplate.update("UPDATE [SUBSCRIPTION] SET ends_at = ? WHERE user_id = ?",
                LocalDateTime.of(2026, 9, 1, 0, 0), plusExpertId);

        mockMvc.perform(post("/api/v1/recipes/ai-assistance").with(principal(plusExpertId))
                        .contentType(MediaType.APPLICATION_JSON).content(request()))
                .andExpect(status().isForbidden());
        verify(aiClient, never()).generateRecipeContent(anyString(), anyList());
    }

    @Test
    void openApiDocumentsProtectedAssistanceOperation() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/ai-assistance'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/ai-assistance'].post.security[0].bearerAuth").isArray());
    }

    private long insertUser(String suffix, String role) {
        String email = PREFIX + suffix + "@test.local";
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, ?, ?, 'ACTIVE', 1)",
                email, "FR21 expert", role);
        return jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor principal(long userId) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT"));
    }

    private String request() throws Exception {
        return jsonMapper.writeValueAsString(new Request("INSTRUCTIONS", "Đậu phụ sốt", "BRAISED", "VEGAN", 2,
                java.util.List.of(new Ingredient(ingredientId, unitId, 200))));
    }

    private record Request(String contentType, String title, String dishCategory, String vegetarianType,
            int servings, java.util.List<Ingredient> ingredients) { }
    private record Ingredient(long ingredientId, int unitId, int quantity) { }
}
