package tech.mamxanh.recipe.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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
import tools.jackson.databind.node.ObjectNode;

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
    private int blockUnitId;

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
        jdbcTemplate.update("UPDATE [INGREDIENT] SET nutrition_supported = 0 WHERE ingredient_id = ?", ingredientId);
        gramUnitId = jdbcTemplate.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'g'", Integer.class);
        kilogramUnitId = jdbcTemplate.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'kg'", Integer.class);
        countUnitId = jdbcTemplate.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'quả'", Integer.class);
        blockUnitId = jdbcTemplate.queryForObject("SELECT unit_id FROM [UNIT] WHERE code = N'bìa'", Integer.class);
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
    void rejectsIngredientAndUnitIdsOutsideTheCatalogWithoutCreatingRecipe() throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(new RecipeIngredientInput(Long.MAX_VALUE, gramUnitId, new BigDecimal("2"))), null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("ingredients[0].ingredientId")));
        assertNoRecipe();

        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(new RecipeIngredientInput(ingredientId, Integer.MAX_VALUE, new BigDecimal("2"))), null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("ingredients[0].unitId")));
        assertNoRecipe();
    }

    @Test
    void acceptsPositiveGramAndKilogramQuantitiesWithoutHundredGramStep() throws Exception {
        for (String quantity : List.of("80", "120", "150", "100.5", "120.5")) {
            mockMvc.perform(post("/api/v1/recipes")
                            .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request(List.of(ingredient(gramUnitId, quantity)), null))))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(ingredient(kilogramUnitId, "0.15")), null))))
                .andExpect(status().isCreated());

        for (String quantity : List.of("0", "-1", "2.555")) {
            mockMvc.perform(post("/api/v1/recipes")
                            .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request(List.of(ingredient(gramUnitId, quantity)), null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field", hasItem("ingredients[0].quantity")));
        }
    }

    @Test
    void enforcesIngredientLineCountFromOneThroughFifty() throws Exception {
        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(), null))))
                .andExpect(status().isBadRequest());
        assertNoRecipe();

        List<RecipeIngredientInput> fifty = java.util.stream.IntStream.range(0, 50)
                .mapToObj(index -> ingredient(gramUnitId, "1")).toList();
        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(fifty, null))))
                .andExpect(status().isCreated());

        List<RecipeIngredientInput> fiftyOne = java.util.stream.IntStream.range(0, 51)
                .mapToObj(index -> ingredient(gramUnitId, "1")).toList();
        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(fiftyOne, null))))
                .andExpect(status().isBadRequest());
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [RECIPE_POST] WHERE author_id = ? AND title = ?", Integer.class,
                expertId, "Đậu hũ kho cà chua")).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [RECIPE_INGREDIENT] ri JOIN [RECIPE_POST] rp ON rp.recipe_id = ri.recipe_id WHERE rp.author_id = ? AND rp.title = ?",
                Integer.class, expertId, "Đậu hũ kho cà chua")).isEqualTo(50);
    }

    @Test
    void acceptsCountUnitWhenActiveIngredientSpecificConversionExists() throws Exception {
        jdbcTemplate.update("INSERT INTO [INGREDIENT_UNIT_CONVERSION] (ingredient_id, unit_id, grams_per_unit) VALUES (?, ?, 150)",
                ingredientId, blockUnitId);

        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(ingredient(blockUnitId, "2")), null))))
                .andExpect(status().isCreated());

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [RECIPE_INGREDIENT] ri JOIN [RECIPE_POST] rp ON rp.recipe_id = ri.recipe_id WHERE rp.author_id = ? AND ri.ingredient_id = ? AND ri.unit_id = ? AND ri.quantity = 2",
                Integer.class, expertId, ingredientId, blockUnitId)).isEqualTo(1);
    }

    @Test
    void excludesInactiveUnitsFromOptionsAndRejectsThemOnPublish() throws Exception {
        jdbcTemplate.update("UPDATE [UNIT] SET is_active = 0 WHERE unit_id = ?", blockUnitId);
        try {
            mockMvc.perform(get("/api/v1/recipes/form-options"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.units[*].unitId", not(hasItem(blockUnitId))));

            mockMvc.perform(post("/api/v1/recipes")
                            .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request(List.of(ingredient(blockUnitId, "2")), null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[*].field", hasItem("ingredients[0].unitId")));
            assertNoRecipe();
        } finally {
            jdbcTemplate.update("UPDATE [UNIT] SET is_active = 1 WHERE unit_id = ?", blockUnitId);
        }
    }

    @Test
    void publishesCatalogIngredientWithoutNutritionDataAndMarksRecipeIncomplete() throws Exception {
        var created = mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(ingredient(gramUnitId, "80")), null))))
                .andExpect(status().isCreated())
                .andReturn();
        long recipeId = jsonMapper.readTree(created.getResponse().getContentAsString()).path("recipeId").asLong();

        mockMvc.perform(get("/api/v1/recipes/{recipeId}", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nutritionComplete").value(false))
                .andExpect(jsonPath("$.ingredientsWithoutNutrition", hasItem(INGREDIENT_NAME)))
                .andExpect(jsonPath("$.ingredients[0].name").value(INGREDIENT_NAME))
                .andExpect(jsonPath("$.ingredients[0].quantity").value(80));
    }

    @Test
    void publishesValidRecipeWithMediaAndPersistsToRecipeMediaTable() throws Exception {
        jdbcTemplate.update("INSERT INTO [INGREDIENT_UNIT_CONVERSION] (ingredient_id, unit_id, grams_per_unit) VALUES (?, ?, 80)",
                ingredientId, countUnitId);
        List<RecipeMediaInput> media = List.of(
                new RecipeMediaInput("https://blob.test/fr25/a.png", "image/png", false),
                new RecipeMediaInput("https://blob.test/fr25/b.webp", "image/webp", true),
                new RecipeMediaInput("https://blob.test/fr25/c.jpg", "image/jpeg", false));

        Integer mediaCountBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM [RECIPE_MEDIA]", Integer.class);
        var result = mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(ingredient(countUnitId, "2")), media))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.recipeId").isNumber())
                .andReturn();

        long recipeId = jsonMapper.readTree(result.getResponse().getContentAsString()).path("recipeId").asLong();

        Integer mediaCountAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM [RECIPE_MEDIA]", Integer.class);
        org.assertj.core.api.Assertions.assertThat(mediaCountAfter).isEqualTo(mediaCountBefore + 3);

        Integer coverCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [RECIPE_MEDIA] WHERE recipe_id = ? AND is_cover = 1", Integer.class, recipeId);
        org.assertj.core.api.Assertions.assertThat(coverCount).isEqualTo(1);

        String coverUrl = jdbcTemplate.queryForObject(
                "SELECT blob_url FROM [RECIPE_MEDIA] WHERE recipe_id = ? AND is_cover = 1", String.class, recipeId);
        org.assertj.core.api.Assertions.assertThat(coverUrl).isEqualTo("https://blob.test/fr25/b.webp");

        mockMvc.perform(get("/api/v1/recipes/{recipeId}", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.media.length()").value(3))
                .andExpect(jsonPath("$.media[1].blobUrl").value("https://blob.test/fr25/b.webp"))
                .andExpect(jsonPath("$.media[1].cover").value(true));
    }

    @Test
    void publishedRecipeAppearsImmediatelyInPublicSearch() throws Exception {
        String uniqueTitle = "Nấm đùi gà kho tiêu FR25";
        CreateRecipeRequest request = new CreateRecipeRequest(uniqueTitle, "Món ngon đậm vị",
                "Kho nấm với tiêu cho đến khi cạn nước.", DishCategory.BRAISED,
                VegetarianType.VEGAN, Difficulty.EASY, 4, 15, 20, null,
                List.of(ingredient(gramUnitId, "300")), List.of());

        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(get("/api/v1/recipes").param("keyword", "Nấm đùi gà kho tiêu FR25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].title", hasItem(uniqueTitle)))
                .andExpect(jsonPath("$.items[0].status").value("PUBLISHED"));
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
    void rejectsMoreThanFiveMediaItems() throws Exception {
        List<RecipeMediaInput> sixImages = List.of(
                new RecipeMediaInput("https://blob.test/1.png", "image/png", true),
                new RecipeMediaInput("https://blob.test/2.png", "image/png", false),
                new RecipeMediaInput("https://blob.test/3.png", "image/png", false),
                new RecipeMediaInput("https://blob.test/4.png", "image/png", false),
                new RecipeMediaInput("https://blob.test/5.png", "image/png", false),
                new RecipeMediaInput("https://blob.test/6.png", "image/png", false));
        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(ingredient(gramUnitId, "200")), sixImages))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("media")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("Mỗi công thức được có tối đa 5 ảnh.")));
        assertNoRecipe();
    }

    @Test
    void reportsTitleAndInstructionLengthErrorsAfterTrim() throws Exception {
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
    void rejectsNullTitleAndInstructionsBeforeCreatingRecipe() throws Exception {
        ObjectNode payload = (ObjectNode) jsonMapper.readTree(json(request(List.of(ingredient(gramUnitId, "200")), List.of())));
        payload.putNull("title");
        payload.putNull("instructions");

        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("title")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("instructions")));
        assertNoRecipe();
    }

    @Test
    void trimsValidTitleAndInstructionsBeforeValidationAndKeepsOptionalFieldsBlank() throws Exception {
        ObjectNode payload = (ObjectNode) jsonMapper.readTree(json(request(List.of(ingredient(gramUnitId, "200")), List.of())));
        payload.put("title", "  Đậu hũ kho cà chua  ");
        payload.put("instructions", "  Cắt đậu hũ, rim cùng cà chua đến khi thấm vị.  ");

        mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated());

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT title FROM [RECIPE_POST] WHERE author_id = ?", String.class, expertId))
                .isEqualTo("Đậu hũ kho cà chua");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT instructions FROM [RECIPE_POST] WHERE author_id = ?", String.class, expertId))
                .isEqualTo("Cắt đậu hũ, rim cùng cà chua đến khi thấm vị.");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT description FROM [RECIPE_POST] WHERE author_id = ?", String.class, expertId)).isNull();
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT youtube_url FROM [RECIPE_POST] WHERE author_id = ?", String.class, expertId)).isNull();
    }

    @Test
    void publishesRecipeWithoutDescriptionAndReturnsPublicDetailWithFullInstructions() throws Exception {
        var publish = mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(ingredient(gramUnitId, "80")), List.of()))))
                .andExpect(status().isCreated())
                .andReturn();
        long recipeId = jsonMapper.readTree(publish.getResponse().getContentAsString()).get("recipeId").asLong();

        mockMvc.perform(get("/api/v1/recipes/{recipeId}", recipeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipeId").value(recipeId))
                .andExpect(jsonPath("$.title").value("Đậu hũ kho cà chua"))
                .andExpect(jsonPath("$.description").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.instructions").value("Cắt đậu hũ, rim cùng cà chua đến khi thấm vị."))
                .andExpect(jsonPath("$.vegetarianType").value("VEGAN"))
                .andExpect(jsonPath("$.ingredients[0].name").value(INGREDIENT_NAME))
                .andExpect(jsonPath("$.ingredients[0].quantity").value(80))
                .andExpect(jsonPath("$.ingredients[0].unitCode").value("g"))
                .andExpect(jsonPath("$.media").isEmpty());
    }

    @Test
    void publicDetailDoesNotExposeMissingOrUnpublishedRecipes() throws Exception {
        var publish = mockMvc.perform(post("/api/v1/recipes")
                        .with(user(Long.toString(expertId)).authorities(new SimpleGrantedAuthority("ROLE_EXPERT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request(List.of(ingredient(gramUnitId, "80")), null))))
                .andExpect(status().isCreated())
                .andReturn();
        long recipeId = jsonMapper.readTree(publish.getResponse().getContentAsString()).get("recipeId").asLong();
        jdbcTemplate.update("UPDATE [RECIPE_POST] SET status = 'HIDDEN' WHERE recipe_id = ?", recipeId);

        mockMvc.perform(get("/api/v1/recipes/{recipeId}", recipeId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/recipes/{recipeId}", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void generatedOpenApiPublishesTitleAndInstructionLengthConstraints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.schemas.CreateRecipeRequest.properties.title.minLength").value(3))
                .andExpect(jsonPath("$.components.schemas.CreateRecipeRequest.properties.title.maxLength").value(120))
                .andExpect(jsonPath("$.components.schemas.CreateRecipeRequest.properties.instructions.minLength").value(10))
                .andExpect(jsonPath("$.components.schemas.CreateRecipeRequest.properties.instructions.maxLength").value(5000));
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
                .andExpect(jsonPath("$.errors[*].field", hasItem("media")))
                .andExpect(jsonPath("$.errors[*].message", hasItem("Nếu có ảnh, hãy chọn đúng 1 ảnh bìa.")));
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
