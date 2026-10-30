package tech.mamxanh.recipe.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.integration.ai.AiClient;
import tech.mamxanh.integration.ai.GeminiAiClient.AiProviderException;
import tech.mamxanh.recipe.repository.RelatedRecipeRepository;

class RelatedRecipeIntegrationTest extends AbstractIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RelatedRecipeRepository related;
    @MockitoBean private AiClient ai;
    private long member;
    private long tofu;
    private long tomato;
    private long source;
    private long best;
    private int grams;
    private int kilos;
    private int count;

    @BeforeEach
    void fixtures() {
        jdbc.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email = 'fr47@test.local')");
        jdbc.update("DELETE FROM [SUBSCRIPTION] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email = 'fr47@test.local')");
        jdbc.update("DELETE FROM [USER] WHERE email = 'fr47@test.local'");
        jdbc.update("DELETE FROM [INGREDIENT_UNIT_CONVERSION] WHERE ingredient_id IN (SELECT ingredient_id FROM [INGREDIENT] WHERE name LIKE 'FR47 %')");
        jdbc.update("DELETE FROM [INGREDIENT] WHERE name LIKE 'FR47 %'");
        jdbc.update("DELETE FROM [UNIT] WHERE code = 'fr47-count'");
        jdbc.update("INSERT INTO [USER](email,display_name,role,account_status,email_verified) VALUES ('fr47@test.local','FR47 Member','CUSTOMER','ACTIVE',1)");
        member = jdbc.queryForObject("SELECT user_id FROM [USER] WHERE email = 'fr47@test.local'", Long.class);
        tofu = ingredient("FR47 tofu");
        tomato = ingredient("FR47 tomato");
        grams = jdbc.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = 'g'", Integer.class);
        kilos = jdbc.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = 'kg'", Integer.class);
        jdbc.update("INSERT INTO [UNIT](code,name,dimension,base_factor) VALUES ('fr47-count','FR47 count','COUNT',1)");
        count = jdbc.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = 'fr47-count'", Integer.class);
        source = recipe("source", "BRAISED", "VEGAN", "PUBLISHED");
        line(source, tofu, kilos, 1);
        line(source, tomato, grams, 200);
        best = recipe("best", "BRAISED", "VEGAN", "PUBLISHED");
        line(best, tofu, grams, 300);
        line(best, tomato, grams, 100);
    }

    @Test
    void publicEndpointUsesRealSqlRankingAndNeverCallsGemini() throws Exception {
        long partial = recipe("partial", "BRAISED", "LACTO", "PUBLISHED");
        line(partial, tofu, grams, 100);
        long unrelated = recipe("unrelated", "SOUP", "LACTO", "PUBLISHED");
        line(unrelated, tomato, grams, 100);
        assertThat(related.findCandidates(source, 12, false)).startsWith(best, partial).doesNotContain(source, unrelated);
        mockMvc.perform(get(path())).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(best))
                .andExpect(jsonPath("$.items[0].title").value("FR47 best"));
        verifyNoInteractions(ai);
    }

    @Test
    void countConversionAndTiedPrincipalIngredientsAreRecognized() {
        jdbc.update("INSERT INTO [INGREDIENT_UNIT_CONVERSION](ingredient_id,unit_id,grams_per_unit) VALUES (?,?,400)", tofu, count);
        long converted = recipe("converted", "SOUP", "LACTO", "PUBLISHED");
        line(converted, tofu, count, 2);
        line(converted, tomato, grams, 700);
        assertThat(related.findCandidates(source, 12, false)).contains(converted);
        jdbc.update("UPDATE [RECIPE_INGREDIENT] SET quantity=300 WHERE recipe_id=?", best);
        assertThat(related.findCandidates(source, 1, false)).containsExactly(best);
    }

    @Test
    void anyMissingConversionDisablesPrincipalMatchInsteadOfGuessing() {
        long missing = recipe("missing", "SOUP", "LACTO", "PUBLISHED");
        line(missing, tofu, grams, 1000);
        line(missing, tomato, count, 1);
        assertThat(related.findCandidates(source, 12, false)).doesNotContain(missing);
    }

    @Test
    void repeatedIngredientLinesAreSummedBeforeChoosingThePrincipalIngredient() {
        long repeated = recipe("repeated", "SOUP", "LACTO", "PUBLISHED");
        line(repeated, tofu, grams, 200);
        line(repeated, tofu, grams, 300);
        line(repeated, tomato, grams, 400);
        assertThat(related.findCandidates(source, 12, false)).contains(repeated);
    }

    @Test
    void latestContentVisibilityAndLimitsAreApplied() throws Exception {
        jdbc.update("UPDATE [RECIPE_POST] SET title='FR47 renamed' WHERE recipe_id=?", best);
        mockMvc.perform(get(path()).param("limit", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("FR47 renamed"));
        jdbc.update("UPDATE [RECIPE_POST] SET status='HIDDEN' WHERE recipe_id=?", best);
        assertThat(related.findCandidates(source, 12, false)).doesNotContain(best);
        jdbc.update("UPDATE [RECIPE_POST] SET status='DELETED' WHERE recipe_id=?", source);
        mockMvc.perform(get(path())).andExpect(status().isNotFound());
        mockMvc.perform(get(path()).param("limit", "13")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/recipes/0/related")).andExpect(status().isBadRequest());
    }

    @Test
    void emptyResultDoesNotInventARecipe() throws Exception {
        var states = jdbc.queryForList("SELECT recipe_id,status FROM [RECIPE_POST] WHERE recipe_id<>?", source);
        try {
            jdbc.update("UPDATE [RECIPE_POST] SET status='HIDDEN' WHERE recipe_id<>?", source);
            mockMvc.perform(get(path())).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        } finally {
            states.forEach(state -> jdbc.update("UPDATE [RECIPE_POST] SET status=? WHERE recipe_id=?",
                    state.get("status"), state.get("recipe_id")));
        }
    }

    @Test
    void guestFreeExpiredAndAdminNeverReachProvider() throws Exception {
        mockMvc.perform(post(path() + "/ai")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(path() + "/ai").with(principal())).andExpect(status().isForbidden());
        paid("PRO");
        jdbc.update("UPDATE [SUBSCRIPTION] SET ends_at='2026-01-02' WHERE user_id=?", member);
        mockMvc.perform(post(path() + "/ai").with(principal())).andExpect(status().isForbidden());
        jdbc.update("UPDATE [USER] SET role='ADMIN' WHERE user_id=?", member);
        mockMvc.perform(post(path() + "/ai").with(principal())).andExpect(status().isForbidden());
        verifyNoInteractions(ai);
    }

    @Test
    void activePlusCannotReachTheRelatedRecipeProvider() throws Exception {
        paid("PLUS");
        mockMvc.perform(post(path() + "/ai").with(principal()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AI_SUBSCRIPTION_REQUIRED"))
                .andExpect(jsonPath("$.requiredTier").value("PRO"))
                .andExpect(jsonPath("$.upgradeUrl").value("/goi-ai"));
        verifyNoInteractions(ai);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "EXPERT"})
    void proMembersGetRealRecipesWithBothMemberRoles(String role) throws Exception {
        paid("PRO");
        jdbc.update("UPDATE [USER] SET role=? WHERE user_id=?", role, member);
        when(ai.recommendRecipes(anyString(), anyList(), anyInt())).thenReturn(result(best));
        mockMvc.perform(post(path() + "/ai").with(user(Long.toString(member)).roles(role))).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(best));
        assertThat(related.findCandidates(source, 50, true)).doesNotContain(source);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM [RECIPE_POST] WHERE author_id=?", Integer.class, member)).isEqualTo(2);
    }

    @Test
    void providerFailureAndForeignIdsReturnExplicitErrors() throws Exception {
        paid("PRO");
        when(ai.recommendRecipes(anyString(), anyList(), anyInt())).thenThrow(new AiProviderException("timeout"));
        mockMvc.perform(post(path() + "/ai").with(principal())).andExpect(status().isServiceUnavailable());
        doReturn(result(source)).when(ai).recommendRecipes(anyString(), anyList(), anyInt());
        mockMvc.perform(post(path() + "/ai").with(principal())).andExpect(status().isBadGateway());
    }

    @Test
    void recipeHiddenDuringGeminiIsNotReturned() throws Exception {
        paid("PRO");
        when(ai.recommendRecipes(anyString(), anyList(), anyInt())).thenAnswer(call -> {
            jdbc.update("UPDATE [RECIPE_POST] SET status='HIDDEN' WHERE recipe_id=?", best);
            return result(best);
        });
        mockMvc.perform(post(path() + "/ai").with(principal())).andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void generatedOpenApiDeclaresPublicGetAndProtectedPost() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}/related'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}/related/ai'].post.security[0].bearerAuth").isArray());
    }

    private String path() { return "/api/v1/recipes/" + source + "/related"; }

    private org.springframework.test.web.servlet.request.RequestPostProcessor principal() {
        return user(Long.toString(member)).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private void paid(String tier) {
        jdbc.update("INSERT INTO [SUBSCRIPTION](user_id,tier,status,starts_at,ends_at) VALUES (?,?,'ACTIVE',?,?)",
                member, tier, LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2027, 1, 1, 0, 0));
    }

    private long ingredient(String name) {
        jdbc.update("INSERT INTO [INGREDIENT](name,source_name,reference_date) VALUES (?,'FR47 fixture','2026-09-28')", name);
        return jdbc.queryForObject("SELECT ingredient_id FROM [INGREDIENT] WHERE name=?", Long.class, name);
    }

    private long recipe(String name, String category, String diet, String status) {
        jdbc.update("""
                INSERT INTO [RECIPE_POST](author_id,title,instructions,dish_category,vegetarian_type,difficulty,
                  servings,prep_time_min,cook_time_min,status,published_at)
                VALUES (?,?,'Cook carefully for ten minutes.',?,?,'EASY',2,5,10,?,'2026-09-28')
                """, member, "FR47 " + name, category, diet, status);
        return jdbc.queryForObject("SELECT recipe_id FROM [RECIPE_POST] WHERE author_id=? AND title=?", Long.class, member, "FR47 " + name);
    }

    private void line(long recipe, long ingredient, int unit, int quantity) {
        jdbc.update("INSERT INTO [RECIPE_INGREDIENT](recipe_id,ingredient_id,unit_id,quantity) VALUES (?,?,?,?)", recipe, ingredient, unit, quantity);
    }

    private AiClient.RecommendationResult result(long id) {
        return new AiClient.RecommendationResult("{\"recipeIds\":[" + id + "]}", 100, 10, 110);
    }
}
