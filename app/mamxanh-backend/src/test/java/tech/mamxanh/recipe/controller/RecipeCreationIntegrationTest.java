package tech.mamxanh.recipe.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.recipe.dto.request.CreateRecipeRequest;
import tech.mamxanh.recipe.dto.request.RecipeIngredientInput;
import tech.mamxanh.recipe.dto.request.RecipeMediaInput;
import tech.mamxanh.recipe.entity.RecipeCodes.Difficulty;
import tech.mamxanh.recipe.entity.RecipeCodes.DishCategory;
import tech.mamxanh.recipe.entity.RecipeCodes.VegetarianType;
import tools.jackson.databind.json.JsonMapper;

class RecipeCreationIntegrationTest extends AbstractIntegrationTest {
    private static final String EXPERT_EMAIL = "issue22-expert@test.local";
    private static final String CUSTOMER_EMAIL = "issue22-customer@test.local";
    private static final String ADMIN_EMAIL = "issue22-admin@test.local";
    private static final String INACTIVE_EXPERT_EMAIL = "issue22-inactive-expert@test.local";
    private static final String INGREDIENT_NAME = "Issue22 Ingredient";

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JsonMapper jsonMapper;

    private long expertId;
    private long customerId;
    private long adminId;
    private long inactiveExpertId;
    private long ingredientId;
    private int gramUnitId;
    private int kilogramUnitId;
    private int countUnitId;

    @BeforeEach
    void prepareCatalogAndAccounts() {
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'issue22-%@test.local')");
        jdbcTemplate.update("DELETE FROM [INGREDIENT_UNIT_CONVERSION] WHERE ingredient_id IN (SELECT ingredient_id FROM [INGREDIENT] WHERE name = ?)", INGREDIENT_NAME);
        jdbcTemplate.update("DELETE FROM [INGREDIENT] WHERE name = ?", INGREDIENT_NAME);
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE 'issue22-%@test.local'");

        expertId = insertUser(EXPERT_EMAIL, "EXPERT", "ACTIVE");
        customerId = insertUser(CUSTOMER_EMAIL, "CUSTOMER", "ACTIVE");
        adminId = insertUser(ADMIN_EMAIL, "ADMIN", "ACTIVE");
        inactiveExpertId = insertUser(INACTIVE_EXPERT_EMAIL, "EXPERT", "LOCKED");

        jdbcTemplate.update("INSERT INTO [INGREDIENT] (name, source_name, reference_date) VALUES (?, ?, '2026-10-04')",
                INGREDIENT_NAME, "Issue #22 integration fixture");
        ingredientId = jdbcTemplate.queryForObject("SELECT ingredient_id FROM [INGREDIENT] WHERE name = ?", Long.class, INGREDIENT_NAME);
        gramUnitId = jdbcTemplate.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'g'", Integer.class);
        kilogramUnitId = jdbcTemplate.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'kg'", Integer.class);
        countUnitId = jdbcTemplate.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'quả'", Integer.class);
    }

    @Test
    void publishesValidMassRecipeAndPersistsAuthorAndIngredientFromAuthenticatedPrincipal() throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(ingredient(gramUnitId, "200")), null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.recipeId").isNumber());

        Long recipeId = jdbcTemplate.queryForObject("SELECT recipe_id FROM [RECIPE_POST] WHERE author_id = ? AND title = ?",
                Long.class, expertId, "Đậu hũ kho cà chua");
        org.assertj.core.api.Assertions.assertThat(recipeId).isNotNull();
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT difficulty FROM [RECIPE_POST] WHERE recipe_id = ?", String.class, recipeId)).isEqualTo("EASY");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [RECIPE_INGREDIENT] WHERE recipe_id = ? AND ingredient_id = ?",
                Integer.class, recipeId, ingredientId)).isEqualTo(1);
    }

    @Test
    void rejectsGuestCustomerAdminAndInactiveExpertWithoutCreatingRecipe() throws Exception {
        String payload = json(request(List.of(ingredient(gramUnitId, "200")), null));
        mockMvc.perform(post("/api/v1/recipes").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnauthorized());
        assertForbidden(customerId, "ROLE_CUSTOMER", payload);
        assertForbidden(adminId, "ROLE_ADMIN", payload);
        assertForbidden(inactiveExpertId, "ROLE_EXPERT", payload);
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [RECIPE_POST] WHERE title = ?", Integer.class, "Đậu hũ kho cà chua")).isZero();
    }

    @Test
    void rejectsCountUnitWhenExistingIngredientConversionIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(ingredient(countUnitId, "2")), null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("ingredients[0].unitId"));
        assertNoRecipe();
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [INGREDIENT_UNIT_CONVERSION] WHERE ingredient_id = ? AND unit_id = ?",
                Integer.class, ingredientId, countUnitId)).isZero();
    }

    @Test
    void rejectsGramQuantitiesThatAreNotIntegerMultiplesOfOneHundred() throws Exception {
        for (String quantity : List.of("80", "120", "150", "100.5")) {
            mockMvc.perform(post("/api/v1/recipes")
                            .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request(List.of(ingredient(gramUnitId, quantity)), null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[*].field", hasItem("ingredients[0].quantity")));
        }
        assertNoRecipe();
    }

    @Test
    void acceptsKilogramDecimalsWhenConvertedWeightIsAnIntegerMultipleOfOneHundredGrams() throws Exception {
        for (String quantity : List.of("0.5", "1.5")) {
            mockMvc.perform(post("/api/v1/recipes")
                            .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request(List.of(ingredient(kilogramUnitId, quantity)), null))))
                    .andExpect(status().isCreated());
        }

        Integer savedBeforeInvalidAttempt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [RECIPE_POST] WHERE author_id = ? AND title = ?", Integer.class,
                expertId, "Đậu hũ kho cà chua");
        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(ingredient(kilogramUnitId, "0.15")), null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("ingredients[0].quantity")));
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [RECIPE_POST] WHERE author_id = ? AND title = ?", Integer.class,
                expertId, "Đậu hũ kho cà chua")).isEqualTo(savedBeforeInvalidAttempt);
    }

    @Test
    void acceptsExistingConversionAndThreeImagesWithExactlyOneCover() throws Exception {
        jdbcTemplate.update("INSERT INTO [INGREDIENT_UNIT_CONVERSION] (ingredient_id, unit_id, grams_per_unit) VALUES (?, ?, 80)",
                ingredientId, countUnitId);
        List<RecipeMediaInput> media = List.of(
                new RecipeMediaInput("https://blob.test/issue22/a.png", "image/png", false),
                new RecipeMediaInput("https://blob.test/issue22/b.webp", "image/webp", true),
                new RecipeMediaInput("https://blob.test/issue22/c.jpg", "image/jpeg", false));

        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(ingredient(countUnitId, "2")), media))))
                .andExpect(status().isCreated());

        Long recipeId = jdbcTemplate.queryForObject("SELECT recipe_id FROM [RECIPE_POST] WHERE author_id = ? AND title = ?",
                Long.class, expertId, "Đậu hũ kho cà chua");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [RECIPE_MEDIA] WHERE recipe_id = ?", Integer.class, recipeId)).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [RECIPE_MEDIA] WHERE recipe_id = ? AND is_cover = 1", Integer.class, recipeId)).isEqualTo(1);
    }

    @Test
    void rejectsMissingOrMultipleCoversAndKeepsEmptyMediaValid() throws Exception {
        List<RecipeMediaInput> noCover = List.of(
                new RecipeMediaInput("https://blob.test/issue22/a.png", "image/png", false),
                new RecipeMediaInput("https://blob.test/issue22/b.png", "image/png", false));
        assertMediaRejected(noCover);
        List<RecipeMediaInput> twoCovers = List.of(
                new RecipeMediaInput("https://blob.test/issue22/a.png", "image/png", true),
                new RecipeMediaInput("https://blob.test/issue22/b.png", "image/png", true));
        assertMediaRejected(twoCovers);

        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(ingredient(gramUnitId, "200")), List.of()))))
                .andExpect(status().isCreated());
    }

    @Test
    void reportsTrimmedValidationErrorsAndKeepsOptionalDescriptionAndVideoBlank() throws Exception {
        CreateRecipeRequest invalid = new CreateRecipeRequest("  ab  ", "", "too short", DishCategory.BRAISED,
                VegetarianType.VEGAN, Difficulty.EASY, 2, 10, 0, "", List.of(ingredient(gramUnitId, "200")), List.of());
        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON).content(json(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("title")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("instructions")));
        assertNoRecipe();
    }

    @Test
    void acceptsSupportedYoutubeFormatsAndRejectsWatchLinksWithoutVideoId() throws Exception {
        for (String youtubeUrl : List.of(
                "https://www.youtube.com/watch?v=video-1",
                "https://www.youtube.com/watch?feature=share&v=video-2",
                "https://youtu.be/video-3",
                "https://youtube.com/embed/video-4",
                "https://youtube.com/shorts/video-5")) {
            mockMvc.perform(post("/api/v1/recipes")
                            .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request(List.of(ingredient(gramUnitId, "200")), List.of(), youtubeUrl))))
                    .andExpect(status().isCreated());
        }

        for (String youtubeUrl : List.of(
                "https://www.youtube.com/watch?v=",
                "https://www.youtube.com/watch?feature=share",
                "https://www.youtube.com/watch?x=1%26v=video-6")) {
            mockMvc.perform(post("/api/v1/recipes")
                            .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request(List.of(ingredient(gramUnitId, "200")), List.of(), youtubeUrl))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field", hasItem("youtubeUrl")));
        }

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [RECIPE_POST] WHERE author_id = ?", Integer.class, expertId)).isEqualTo(5);
    }

    @Test
    void servesCurrentDishUnitAndIngredientOptionsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/recipes/form-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dishCategories[*].code", hasItem("BRAISED")))
                .andExpect(jsonPath("$.difficulties[*].code", hasItem("EASY")))
                .andExpect(jsonPath("$.units[*].code", hasItem("g")));
        mockMvc.perform(get("/api/v1/recipes/ingredient-options").param("query", "Issue22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(INGREDIENT_NAME)));
    }

    private void assertForbidden(long userId, String authority, String payload) throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(userId)).authorities(new SimpleGrantedAuthority(authority)))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isForbidden());
    }

    private void assertMediaRejected(List<RecipeMediaInput> media) throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(ingredient(gramUnitId, "200")), media))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("media"));
        assertNoRecipe();
    }

    private void assertNoRecipe() {
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [RECIPE_POST] WHERE author_id = ? AND title = ?", Integer.class,
                expertId, "Đậu hũ kho cà chua")).isZero();
    }

    private long insertUser(String email, String role, String status) {
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, ?, ?, ?, 1)",
                email, "Issue 22 test", role, status);
        return jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }

    private RecipeIngredientInput ingredient(int unitId, String quantity) {
        return new RecipeIngredientInput(ingredientId, unitId, new BigDecimal(quantity));
    }

    private CreateRecipeRequest request(List<RecipeIngredientInput> ingredients, List<RecipeMediaInput> media) {
        return request(ingredients, media, "");
    }

    private CreateRecipeRequest request(List<RecipeIngredientInput> ingredients, List<RecipeMediaInput> media,
            String youtubeUrl) {
        return new CreateRecipeRequest("Đậu hũ kho cà chua", "", "Cắt đậu hũ, rim cùng cà chua đến khi thấm vị.",
                DishCategory.BRAISED, VegetarianType.VEGAN, Difficulty.EASY, 2, 10, 0, youtubeUrl, ingredients, media);
    }

    private String json(Object value) throws Exception { return jsonMapper.writeValueAsString(value); }
}
