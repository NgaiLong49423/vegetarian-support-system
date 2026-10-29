package tech.mamxanh.shopping.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.AbstractIntegrationTest;

/** Full HTTP/security and SQL persistence for the single-recipe AC-53.6 flows. */
@Transactional
class RecipeShoppingListIntegrationTest extends AbstractIntegrationTest {
    private static final String ROOT = "/api/v1/shopping-lists";
    @Autowired JdbcTemplate sql;
    @Autowired jakarta.persistence.EntityManager em;

    long owner, other, expert, first, second, ingredient, excludedIngredient;
    int grams;

    @BeforeEach
    void fixtures() {
        owner = member("CUSTOMER");
        other = member("CUSTOMER");
        expert = member("EXPERT");
        grams = unit("g");
        ingredient = sql.queryForObject(
                "INSERT INTO [INGREDIENT] (name,ingredient_group,source_name,reference_date) OUTPUT INSERTED.ingredient_id VALUES (?,N'Đậu',N'Kiểm thử AC536','2026-10-18')",
                Long.class, "Đậu " + UUID.randomUUID());
        first = recipe();
        second = recipe();
        line(first, "300");
        line(second, "800");
    }

    @AfterEach
    void cleanup() {
        if (owner > 0 || other > 0) {
            sql.update("DELETE FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id IN (SELECT shopping_list_id FROM [SHOPPING_LIST] WHERE user_id IN (?,?))", owner, other);
            sql.update("DELETE FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id IN (SELECT shopping_list_id FROM [SHOPPING_LIST] WHERE user_id IN (?,?))", owner, other);
            sql.update("DELETE FROM [SHOPPING_LIST] WHERE user_id IN (?,?)", owner, other);
        }
        if (expert > 0) {
            sql.update("DELETE FROM [RECIPE_INGREDIENT] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST] WHERE author_id=?)", expert);
            sql.update("DELETE FROM [RECIPE_POST] WHERE author_id=?", expert);
        }
        if (ingredient > 0) sql.update("DELETE FROM [INGREDIENT] WHERE ingredient_id=?", ingredient);
        if (excludedIngredient > 0) sql.update("DELETE FROM [INGREDIENT] WHERE ingredient_id=?", excludedIngredient);
        sql.update("DELETE FROM [USER] WHERE user_id IN (?,?,?)", owner, other, expert);
    }

    @Test
    void addsOneRecipeToTheSelectedMealPlanListAndDoesNotDuplicate() throws Exception {
        long list = createList(owner, "Lịch ăn", "MEAL_PLAN");

        add(owner, list, first, 4)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shoppingList.id").value(list));
        assertThat(quantity(list)).isEqualByComparingTo("600");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",
                Integer.class, list)).isEqualTo(1);

        add(owner, list, first, 4).andExpect(status().isOk());
        assertThat(quantity(list)).isEqualByComparingTo("600");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",
                Integer.class, list)).isEqualTo(1);
    }

    @Test
    void createsNamedManualListAndAddsRecipeInOneRequest() throws Exception {
        var response = mockMvc.perform(post(ROOT + "/from-recipe")
                .with(user("" + owner).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  Món cuối tuần  \",\"recipeId\":" + first + ",\"servings\":3}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.shoppingList.name").value("Món cuối tuần"))
                .andReturn();
        long list = new tools.jackson.databind.json.JsonMapper().readTree(response.getResponse().getContentAsString())
                .path("shoppingList").path("id").asLong();

        assertThat(sql.queryForObject("SELECT list_kind FROM [SHOPPING_LIST] WHERE shopping_list_id=?",
                String.class, list)).isEqualTo("MANUAL");
        assertThat(quantity(list)).isEqualByComparingTo("450");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",
                Integer.class, list)).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void failedCreateFromRecipeRollsBackTheNewChecklist() throws Exception {
        update("DELETE FROM [RECIPE_INGREDIENT] WHERE recipe_id=?", first);
        mockMvc.perform(post(ROOT + "/from-recipe").with(user("" + owner).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Không được lưu rỗng\",\"recipeId\":" + first + "}"))
                .andExpect(status().isBadRequest());
        assertThat(countLists(owner)).isZero();
    }

    @Test
    void preservesExistingChecklistEditsAndAddsASecondRecipe() throws Exception {
        long list = createList(owner, "Danh sách riêng", "MANUAL");
        add(owner, list, first, null).andExpect(status().isOk());
        long row = item(list);
        update("UPDATE [SHOPPING_LIST_ITEM] SET quantity=10,is_bought=1 WHERE shopping_list_item_id=?", row);
        update("UPDATE [RECIPE_INGREDIENT] SET quantity=999 WHERE recipe_id=?", first);

        add(owner, list, first, null).andExpect(status().isOk());
        assertThat(quantity(list)).isEqualByComparingTo("10");
        assertThat(sql.queryForObject("SELECT is_bought FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_item_id=?",
                Boolean.class, row)).isTrue();

        add(owner, list, second, null).andExpect(status().isOk());
        assertThat(quantity(list)).isEqualByComparingTo("810");
        assertThat(sql.queryForObject("SELECT is_bought FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_item_id=?",
                Boolean.class, row)).isFalse();
        assertThat(sql.queryForObject("SELECT quantity FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE recipe_id=?",
                BigDecimal.class, first)).isEqualByComparingTo("300");
    }

    @Test
    void selectedIngredientIdsIncludeEveryRecipeLineForThatIngredientAcrossUnits() throws Exception {
        int milliliters = unit("ml");
        line(first, "100", milliliters);
        long list = createList(owner, "Danh sách chọn nguyên liệu", "MANUAL");

        selectedAdd(owner, list, first,
                "\"selectedIngredientIds\":[" + ingredient + "],\"manualItems\":[]")
                .andExpect(status().isOk());

        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",
                Integer.class, list)).isEqualTo(2);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=?",
                Integer.class, list)).isEqualTo(2);
    }

    @Test
    void canAddOnlyManualItemsAfterDeselectingEveryRecipeIngredient() throws Exception {
        long list = createList(owner, "Danh sách thủ công", "MANUAL");

        selectedAdd(owner, list, first,
                "\"selectedIngredientIds\":[],\"manualItems\":[{\"name\":\"Rau thơm\",\"quantity\":1,\"unitId\":" + grams + "}]")
                .andExpect(status().isOk());

        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",
                Integer.class, list)).isZero();
        assertThat(sql.queryForObject("SELECT custom_ingredient_name FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=?",
                String.class, list)).isEqualTo("Rau thơm");
    }

    @Test
    void selectedIngredientIdsMustBeUniqueAndBelongToTheRecipe() throws Exception {
        long list = createList(owner, "Danh sách chọn nguyên liệu", "MANUAL");

        selectedAdd(owner, list, first,
                "\"selectedIngredientIds\":[" + ingredient + "," + ingredient + "],\"manualItems\":[]")
                .andExpect(status().isBadRequest());
        selectedAdd(owner, list, first,
                "\"selectedIngredientIds\":[9223372036854775807],\"manualItems\":[]")
                .andExpect(status().isBadRequest());

        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",
                Integer.class, list)).isZero();
    }

    @Test
    void rejectsAnEmptyIngredientSelectionWithoutManualItems() throws Exception {
        long list = createList(owner, "Danh sách chọn nguyên liệu", "MANUAL");

        selectedAdd(owner, list, first,
                "\"selectedIngredientIds\":[],\"manualItems\":[]")
                .andExpect(status().isBadRequest());

        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",
                Integer.class, list)).isZero();
    }

    @Test
    void foreignListIsNotRevealedOrModified() throws Exception {
        long list = createList(owner, "Danh sách của chủ", "MANUAL");
        add(other, list, first, null).andExpect(status().isNotFound());
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",
                Integer.class, list)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"HIDDEN", "DELETED"})
    void unavailableRecipeCannotBeAddedToAnExistingList(String state) throws Exception {
        long list = createList(owner, "Danh sách", "MANUAL");
        update("UPDATE [RECIPE_POST] SET status=? WHERE recipe_id=?", state, first);
        add(owner, list, first, null).andExpect(status().isNotFound());
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",
                Integer.class, list)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"recipeId\":0}", "{\"recipeId\":1,\"servings\":51}"})
    void invalidRecipeRequestsAreRejected(String body) throws Exception {
        long list = createList(owner, "Danh sách", "MANUAL");
        mockMvc.perform(post(ROOT + "/" + list + "/recipes").with(user("" + owner).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",
                Integer.class, list)).isZero();
    }

    @Test
    void guestAdminAndLockedAccountsCannotAddRecipes() throws Exception {
        long list = createList(owner, "Danh sách", "MANUAL");
        mockMvc.perform(post(ROOT + "/" + list + "/recipes").contentType(MediaType.APPLICATION_JSON)
                .content(recipeBody(first, null))).andExpect(status().isUnauthorized());
        long admin = member("ADMIN");
        mockMvc.perform(post(ROOT + "/" + list + "/recipes").with(user("" + admin).roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(recipeBody(first, null))).andExpect(status().isForbidden());
        update("UPDATE [USER] SET account_status='LOCKED' WHERE user_id=?", owner);
        mockMvc.perform(post(ROOT + "/" + list + "/recipes").with(user("" + owner).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(recipeBody(first, null))).andExpect(status().isForbidden());
    }

    @Test
    void selectedIngredientsAndManualItemsAreStoredTogetherWithServingScaling() throws Exception {
        excludedIngredient = sql.queryForObject(
                "INSERT INTO [INGREDIENT] (name,ingredient_group,source_name,reference_date) OUTPUT INSERTED.ingredient_id VALUES (?,N'Đậu',N'Kiểm thử AC536','2026-10-18')",
                Long.class, "Excluded " + UUID.randomUUID());
        long excluded = excludedIngredient;
        update("INSERT INTO [RECIPE_INGREDIENT](recipe_id,ingredient_id,unit_id,quantity) VALUES(?,?,?,?)",
                first, excluded, grams, new BigDecimal("50"));

        var response = mockMvc.perform(post(ROOT + "/from-recipe").with(user("" + owner).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Selected and manual\",\"recipeId\":" + first
                        + ",\"servings\":4,\"selectedIngredientIds\":[" + ingredient
                        + "],\"manualItems\":[" + manualItemBody(grams) + "]}"))
                .andExpect(status().isCreated()).andReturn();
        long list = new tools.jackson.databind.json.JsonMapper().readTree(response.getResponse().getContentAsString())
                .path("shoppingList").path("id").asLong();
        assertThat(sql.queryForObject("SELECT quantity FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=? AND ingredient_id=?",
                BigDecimal.class, list, ingredient)).isEqualByComparingTo("600");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=? AND ingredient_id=?",
                Integer.class, list, excluded)).isZero();
        assertThat(sql.queryForObject("SELECT quantity FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=? AND custom_ingredient_name='Napkins'",
                BigDecimal.class, list)).isEqualByComparingTo("2");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",
                Integer.class, list)).isEqualTo(1);
    }

    @Test
    void manualOnlySelectionOmitsAllRecipeContributions() throws Exception {
        long list = createList(owner, "Manual only", "MANUAL");
        addSelection(list, "[]", "[" + manualItemBody(grams) + "]").andExpect(status().isOk());
        assertThat(quantity(list)).isEqualByComparingTo("2");
        assertThat(sql.queryForObject("SELECT custom_ingredient_name FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=?",
                String.class, list)).isEqualTo("Napkins");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",
                Integer.class, list)).isZero();
    }

    @Test
    void addingManualItemsToAnAlreadyAddedRecipePreservesItsOriginalContribution() throws Exception {
        long list = createList(owner, "Existing source", "MANUAL");
        add(owner, list, first, null).andExpect(status().isOk());
        addSelection(list, "[" + ingredient + "]", "[" + manualItemBody(grams) + "]").andExpect(status().isOk());
        assertThat(sql.queryForObject("SELECT quantity FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=? AND ingredient_id=?",
                BigDecimal.class, list, ingredient)).isEqualByComparingTo("300");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",
                Integer.class, list)).isEqualTo(1);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=? AND custom_ingredient_name='Napkins'",
                Integer.class, list)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"[]", "[%1$d,%1$d]", "[9223372036854775807]"})
    void invalidIngredientSelectionsDoNotModifyTheChecklist(String selection) throws Exception {
        long list = createList(owner, "Unchanged", "MANUAL");
        addSelection(list, selection.formatted(ingredient), "[]").andExpect(status().isBadRequest());
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=?",
                Integer.class, list)).isZero();
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",
                Integer.class, list)).isZero();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void invalidManualItemRollsBackBothNewChecklistAndRecipeContributions() throws Exception {
        mockMvc.perform(post(ROOT + "/from-recipe").with(user("" + owner).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Atomic failure\",\"recipeId\":" + first
                        + ",\"selectedIngredientIds\":[" + ingredient + "],\"manualItems\":["
                        + manualItemBody(Integer.MAX_VALUE) + "]}"))
                .andExpect(status().isBadRequest());
        assertThat(countLists(owner)).isZero();
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE recipe_id=?",
                Integer.class, first)).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions addSelection(long list, String selection, String manual)
            throws Exception {
        return mockMvc.perform(post(ROOT + "/" + list + "/recipes").with(user("" + owner).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"recipeId\":" + first
                        + ",\"selectedIngredientIds\":" + selection + ",\"manualItems\":" + manual + "}"));
    }

    private String manualItemBody(int unitId) {
        return "{\"name\":\"Napkins\",\"quantity\":2,\"unitId\":" + unitId + "}";
    }

    private org.springframework.test.web.servlet.ResultActions add(long member, long list, long recipe, Integer servings)
            throws Exception {
        return mockMvc.perform(post(ROOT + "/" + list + "/recipes").with(user("" + member).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(recipeBody(recipe, servings)));
    }

    private org.springframework.test.web.servlet.ResultActions selectedAdd(long member, long list, long recipe,
            String selectionJson) throws Exception {
        return mockMvc.perform(post(ROOT + "/" + list + "/recipes").with(user("" + member).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipeId\":" + recipe + "," + selectionJson + "}"));
    }

    private String recipeBody(long recipe, Integer servings) {
        return "{\"recipeId\":" + recipe + (servings == null ? "" : ",\"servings\":" + servings) + "}";
    }

    private long createList(long member, String name, String kind) {
        return sql.queryForObject("INSERT INTO [SHOPPING_LIST](user_id,name,list_kind) OUTPUT INSERTED.shopping_list_id VALUES (?,?,?)",
                Long.class, member, name, kind);
    }

    private void update(String statement, Object... values) {
        sql.update(statement, values);
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) em.clear();
    }

    private long member(String role) {
        return sql.queryForObject("INSERT INTO [USER](email,display_name,role,account_status,email_verified) OUTPUT INSERTED.user_id VALUES (?,N'AC536',?,'ACTIVE',1)",
                Long.class, UUID.randomUUID() + "@test.local", role);
    }

    private int unit(String code) {
        return sql.queryForObject("SELECT unit_id FROM [UNIT] WHERE code=?", Integer.class, code);
    }

    private long recipe() {
        return sql.queryForObject("INSERT INTO [RECIPE_POST](author_id,title,instructions,dish_category,vegetarian_type,difficulty,servings,prep_time_min,cook_time_min,status,published_at) OUTPUT INSERTED.recipe_id VALUES (?,N'Nguồn AC536',N'Nấu nguyên liệu chín rồi dùng.','SOUP','VEGAN','EASY',2,10,15,'PUBLISHED',SYSUTCDATETIME())",
                Long.class, expert);
    }

    private void line(long recipe, String quantity) {
        line(recipe, quantity, grams);
    }

    private void line(long recipe, String quantity, int unit) {
        update("INSERT INTO [RECIPE_INGREDIENT](recipe_id,ingredient_id,unit_id,quantity) VALUES(?,?,?,?)",
                recipe, ingredient, unit, new BigDecimal(quantity));
    }

    private long item(long list) {
        return sql.queryForObject("SELECT shopping_list_item_id FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=?", Long.class, list);
    }

    private BigDecimal quantity(long list) {
        return sql.queryForObject("SELECT quantity FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=?", BigDecimal.class, list);
    }

    private int countLists(long member) {
        return sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_LIST] WHERE user_id=?", Integer.class, member);
    }
}
