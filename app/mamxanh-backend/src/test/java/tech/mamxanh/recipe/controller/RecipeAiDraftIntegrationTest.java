package tech.mamxanh.recipe.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.integration.ai.AiClient;

class RecipeAiDraftIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @MockitoBean AiClient ai;
    private long member, ingredient;
    private String ingredientName;
    @BeforeEach void fixture() {
        String token = UUID.randomUUID().toString();
        String email = "draft-" + token + "@example.invalid";
        jdbc.update("INSERT INTO [USER] (email,display_name,role,account_status,email_verified) VALUES (?,N'Draft test','EXPERT','ACTIVE',1)",email);
        member = jdbc.queryForObject("SELECT user_id FROM [USER] WHERE email=?",Long.class,email);
        ingredientName = "Draft ingredient " + token;
        jdbc.update("""
            INSERT INTO [INGREDIENT] (name,ingredient_group,source_name,reference_date,nutrition_supported,status,
                energy_kcal_100g,protein_g_100g,carbohydrate_g_100g,total_fat_g_100g,fiber_g_100g,
                calcium_mg_100g,iron_mg_100g,vitamin_b12_mcg_100g,zinc_mg_100g)
            VALUES (?,'Test','Integration catalog','2026-10-01',1,'ACTIVE',100,1,1,1,1,1,1,1,1)
            """,ingredientName);
        ingredient = jdbc.queryForObject("SELECT ingredient_id FROM [INGREDIENT] WHERE name=?",Long.class,ingredientName);
        when(ai.generateRecipeDraft(anyString(),anyList())).thenReturn(new AiClient.RecommendationResult(result(ingredient),10,20,30));
    }
    @AfterEach void cleanup() {
        jdbc.update("DELETE FROM [SUBSCRIPTION] WHERE user_id=?",member);
        jdbc.update("DELETE FROM [USER] WHERE user_id=?",member);
        jdbc.update("DELETE FROM [INGREDIENT] WHERE ingredient_id=?",ingredient);
    }
    @Test void guestIsUnauthenticatedAndCustomerRoleIsForbidden() throws Exception {
        mockMvc.perform(post(path()).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isUnauthorized());
        jdbc.update("UPDATE [USER] SET role='CUSTOMER' WHERE user_id=?",member);
        mockMvc.perform(post(path()).with(user(Long.toString(member)).roles("CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ai);
    }
    @Test void freePlusExpiredAndFutureProNeverReachGemini() throws Exception {
        deny();
        subscription("PLUS",-1,1); deny();
        jdbc.update("UPDATE [SUBSCRIPTION] SET tier='PRO',ends_at=? WHERE user_id=?",LocalDateTime.now(clock),member); deny();
        jdbc.update("UPDATE [SUBSCRIPTION] SET starts_at=?,ends_at=? WHERE user_id=?",LocalDateTime.now(clock).plusDays(1),LocalDateTime.now(clock).plusDays(31),member); deny();
        verifyNoInteractions(ai);
    }
    @Test void activeProGetsCatalogVerifiedPreviewButNoRecipeIsPersisted() throws Exception {
        subscription("PRO",-1,1);
        mockMvc.perform(post(path()).with(user(Long.toString(member)).roles("EXPERT")).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.ingredients[0].ingredientId").value(ingredient))
                .andExpect(jsonPath("$.ingredients[0].sourceName").value("Integration catalog"))
                .andExpect(jsonPath("$.totalCalories").value(300))
                .andExpect(jsonPath("$.caloriesPerServing").value(150))
                .andExpect(jsonPath("$.targetCaloriesPerServing").value(350));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM [RECIPE_POST] WHERE author_id=?",Integer.class,member)).isZero();
        verify(ai).generateRecipeDraft(anyString(),eq(List.of(ingredient)));
    }
    @Test void malformedInputAndUnresolvedNamesAreRejectedWithoutProviderUse() throws Exception {
        subscription("PRO",-1,1);
        for (String invalid : List.of(body().replace("Món chay thử nghiệm", "  a  "),body().replace("\"servings\":2","\"servings\":0"),body().replace(ingredientName,"unknown-entry-"+UUID.randomUUID()))) {
            mockMvc.perform(post(path()).with(user(Long.toString(member)).roles("EXPERT")).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(ai);
    }
    @Test void unknownModelIngredientIdsAreRejectedAndDoNotSaveAnything() throws Exception {
        subscription("PRO",-1,1);
        when(ai.generateRecipeDraft(anyString(),anyList())).thenReturn(new AiClient.RecommendationResult(result(999999999),1,1,2));
        mockMvc.perform(post(path()).with(user(Long.toString(member)).roles("EXPERT")).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("AI_RESPONSE_INVALID"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM [RECIPE_POST] WHERE author_id=?",Integer.class,member)).isZero();
    }
    @Test void generatedOpenApiHasThePreviewContractAndIsolatedSchemas() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/ai/draft'].post.responses['200']").exists())
                .andExpect(jsonPath("$.components.schemas.RecipeAiDraftIngredient.properties.energyKcalPer100g").exists())
                .andExpect(jsonPath("$.components.schemas.RecipeAiDraftMealType").exists());
    }
    private void deny() throws Exception {
        mockMvc.perform(post(path()).with(user(Long.toString(member)).roles("EXPERT")).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("AI_SUBSCRIPTION_REQUIRED"))
                .andExpect(jsonPath("$.requiredTier").value("PRO"));
    }
    private void subscription(String tier,int start,int end) {
        var now=LocalDateTime.now(clock);
        jdbc.update("INSERT INTO [SUBSCRIPTION] (user_id,tier,status,starts_at,ends_at) VALUES (?,?,'ACTIVE',?,?)",member,tier,now.plusDays(start),now.plusDays(end));
    }
    private String path() { return "/api/v1/recipes/ai/draft"; }
    private String body() { return "{\"title\":\"Món chay thử nghiệm\",\"rawIngredients\":\""+ingredientName+"\",\"mealType\":\"LUNCH\",\"targetCaloriesPerServing\":350,\"servings\":2,\"cookingMethod\":\"STIR_FRY\"}"; }
    private String result(long id) {
        String step="{\"text\":\"Sơ chế và làm chín nguyên liệu đã chọn.\",\"ingredientIds\":["+id+"]}";
        return "{\"description\":\"Món ăn từ danh mục đã chọn.\",\"steps\":["+String.join(",",Collections.nCopies(6,step))
            +"],\"dishCategory\":\"STIR_FRY\",\"vegetarianType\":\"VEGAN\",\"difficulty\":\"EASY\",\"prepTimeMinutes\":5,\"cookTimeMinutes\":10,"
            +"\"ingredients\":[{\"ingredientId\":"+id+",\"quantityGrams\":300}]}";
    }
}
