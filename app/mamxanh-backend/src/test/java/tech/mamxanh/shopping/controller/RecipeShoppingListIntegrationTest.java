package tech.mamxanh.shopping.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.AbstractIntegrationTest;

/** Full HTTP/security, migration and SQL persistence for AC-53.6 / approved #90 semantics. */
@Transactional
class RecipeShoppingListIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate sql;
    @Autowired jakarta.persistence.EntityManager em;
    long owner,other,expert,first,second,ingredient; int g,ml,block;
    final String route="/api/v1/shopping-lists/from-recipes";
    @BeforeEach void fixtures() {
        owner=member("CUSTOMER");other=member("CUSTOMER");expert=member("EXPERT");
        g=unit("g");ml=unit("ml");block=unit("bìa");
        ingredient=sql.queryForObject("INSERT INTO [INGREDIENT] (name,ingredient_group,source_name,reference_date) OUTPUT INSERTED.ingredient_id VALUES (?,N'Đậu',N'Kiểm thử AC536','2026-10-18')",Long.class,"Đậu "+UUID.randomUUID());
        first=recipe();second=recipe();line(first,g,"300");line(second,g,"800");
    }

    @Test void selectedRecipesCreateOnePersonalListAndTraceOriginalContributions() throws Exception {
        long list=add(owner,first,second);
        assertThat(quantity(list)).isEqualByComparingTo("1100");
        assertThat(sql.queryForObject("SELECT list_kind FROM [SHOPPING_LIST] WHERE shopping_list_id=?",String.class,list)).isEqualTo("PERSONAL");
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",Integer.class,list)).isEqualTo(2);
        mockMvc.perform(get("/api/v1/shopping-lists/"+list).with(user(""+owner).roles("CUSTOMER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].sources.length()").value(2))
            .andExpect(jsonPath("$.items[0].sources[0].originalQuantity").value(300))
            .andExpect(jsonPath("$.items[0].sources[1].originalQuantity").value(800));
        assertThat(add(owner,first,first,second)).isEqualTo(list);
        assertThat(quantity(list)).isEqualByComparingTo("1100");
    }

    @Test void newRecipeAddsToSavedEditedTotalsWithoutRefreshingExistingRecipe() throws Exception {
        long list=add(owner,first);long item=item(list);
        update("UPDATE [SHOPPING_LIST_ITEM] SET quantity=10,is_bought=1 WHERE shopping_list_item_id=?",item);
        update("UPDATE [RECIPE_INGREDIENT] SET quantity=999 WHERE recipe_id=?",first);
        assertThat(add(owner,first)).isEqualTo(list);assertThat(quantity(list)).isEqualByComparingTo("10");
        assertThat(sql.queryForObject("SELECT is_bought FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_item_id=?",Boolean.class,item)).isTrue();
        add(owner,second);assertThat(quantity(list)).isEqualByComparingTo("810");
        assertThat(sql.queryForObject("SELECT is_bought FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_item_id=?",Boolean.class,item)).isFalse();
        assertThat(sql.queryForObject("SELECT quantity FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE recipe_id=?",BigDecimal.class,first)).isEqualByComparingTo("300");
    }

    @ParameterizedTest @ValueSource(strings={"HIDDEN","DELETED"})
    void unavailableSourceKeepsChecklistAndOriginalQuantityButCannotBeAdded(String state) throws Exception {
        long list=add(owner,first);
        update("UPDATE [SHOPPING_LIST_ITEM] SET is_bought=1 WHERE shopping_list_id=?",list);
        update("UPDATE [RECIPE_POST] SET status=?,title=N'Không được lộ tiêu đề' WHERE recipe_id=?",state,first);
        mockMvc.perform(get("/api/v1/shopping-lists/"+list).with(user(""+owner).roles("CUSTOMER")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].bought").value(true))
            .andExpect(jsonPath("$.items[0].sources[0].available").value(false))
            .andExpect(jsonPath("$.items[0].sources[0].title").isEmpty())
            .andExpect(jsonPath("$.items[0].sources[0].originalQuantity").value(300));
        mockMvc.perform(post(route).with(user(""+owner).roles("CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content(body(first,second)))
            .andExpect(status().isNotFound());
        assertThat(quantity(list)).isEqualByComparingTo("300");
    }

    @Test void sourceTitleIsCurrentWithoutChangingSavedChecklist() throws Exception {
        long list=add(owner,first);
        update("UPDATE [RECIPE_POST] SET title=N'Tên mới' WHERE recipe_id=?",first);
        mockMvc.perform(get("/api/v1/shopping-lists/"+list).with(user(""+owner).roles("CUSTOMER")))
            .andExpect(jsonPath("$.items[0].sources[0].title").value("Tên mới"))
            .andExpect(jsonPath("$.items[0].quantity").value(300));
    }

    @Test void deletionAndCleanupRetainRecipeAdditionMarkers() throws Exception {
        long list=add(owner,first);long id=item(list);
        mockMvc.perform(delete("/api/v1/shopping-lists/"+list+"/items/"+id).with(user(""+owner).roles("CUSTOMER"))).andExpect(status().isNoContent());
        add(owner,first);assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=?",Integer.class,list)).isZero();
        add(owner,second);
        update("UPDATE [SHOPPING_LIST_ITEM] SET is_bought=1 WHERE shopping_list_id=?",list);
        mockMvc.perform(delete("/api/v1/shopping-lists/"+list+"/items?bought=true").with(user(""+owner).roles("CUSTOMER"))).andExpect(status().isNoContent());
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=? AND shopping_list_item_id IS NULL",Integer.class,list)).isEqualTo(2);
    }

    @Test void personalListIsSeparateFromMealPlanListsAndFromOtherMembers() throws Exception {
        update("INSERT INTO [SHOPPING_LIST](user_id,name) VALUES (?,N'Lịch ăn')",owner);
        long list=add(owner,first);assertThat(countLists(owner)).isEqualTo(2);
        long foreign=add(other,first);assertThat(foreign).isNotEqualTo(list);
        mockMvc.perform(get("/api/v1/shopping-lists/"+list).with(user(""+other).roles("CUSTOMER"))).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/shopping-lists/"+list+"/items").with(user(""+other).roles("CUSTOMER"))
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\",\"quantity\":1,\"unitId\":"+g+"}")).andExpect(status().isNotFound());
    }

    @Test void safeConversionsPreservePerSourceOriginalUnits() throws Exception {
        update("DELETE FROM [RECIPE_INGREDIENT] WHERE recipe_id=?",second);line(second,block,"2");
        update("INSERT INTO [INGREDIENT_UNIT_CONVERSION](ingredient_id,unit_id,grams_per_unit) VALUES(?,?,150)",ingredient,block);
        long list=add(owner,first,second);assertThat(quantity(list)).isEqualByComparingTo("600");
        mockMvc.perform(get("/api/v1/shopping-lists/"+list).with(user(""+owner).roles("CUSTOMER")))
            .andExpect(jsonPath("$.items[0].sources[1].originalUnitCode").value("bìa"))
            .andExpect(jsonPath("$.items[0].sources[1].originalQuantity").value(2));
    }

    @Test void incompatibleUnitsRemainSeparateAndCanLaterMergeWithExplicitRules() throws Exception {
        update("DELETE FROM [RECIPE_INGREDIENT] WHERE recipe_id=?",second);line(second,ml,"200");
        long list=add(owner,first,second);
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=?",Integer.class,list)).isEqualTo(2);
        long third=recipe();line(third,g,"100");
        update("INSERT INTO [INGREDIENT_UNIT_CONVERSION](ingredient_id,unit_id,grams_per_unit) VALUES(?,?,1)",ingredient,ml);
        add(owner,third);assertThat(quantity(list)).isEqualByComparingTo("600");
        assertThat(sql.queryForObject("SELECT COUNT(DISTINCT shopping_list_item_id) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE shopping_list_id=?",Integer.class,list)).isEqualTo(1);
    }

    @ParameterizedTest @ValueSource(strings={"{}","{\"recipeIds\":[]}","{\"recipeIds\":[0]}","{\"recipeIds\":[null]}"})
    void invalidSelectionCreatesNothing(String body) throws Exception {
        mockMvc.perform(post(route).with(user(""+owner).roles("CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        assertThat(countLists(owner)).isZero();
    }

    @Test void missingRecipeAndOverflowRollbackWholeRequest() throws Exception {
        mockMvc.perform(post(route).with(user(""+owner).roles("CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content(body(first,Long.MAX_VALUE)))
            .andExpect(status().isNotFound());assertThat(countLists(owner)).isZero();
        update("UPDATE [RECIPE_INGREDIENT] SET quantity=99999999.99 WHERE recipe_id=?",second);
        mockMvc.perform(post(route).with(user(""+owner).roles("CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content(body(first,second)))
            .andExpect(status().isBadRequest());assertThat(countLists(owner)).isZero();
    }

    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentFirstAdditionsCreateOneListAndOneRecipeContribution() throws Exception {
        try (var workers=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var start=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Long> action=() -> {start.await();return add(owner,first);};
            var one=workers.submit(action);var two=workers.submit(action);start.countDown();
            assertThat(one.get(30,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(two.get(30,java.util.concurrent.TimeUnit.SECONDS));
            assertThat(countLists(owner)).isEqualTo(1);
            assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_RECIPE_CONTRIBUTION] WHERE recipe_id=?",Integer.class,first)).isEqualTo(1);
        } finally {
            update("DELETE FROM [SHOPPING_LIST] WHERE user_id IN (?,?)",owner,other);
            update("DELETE FROM [RECIPE_POST] WHERE recipe_id IN (?,?)",first,second);
            update("DELETE FROM [INGREDIENT] WHERE ingredient_id=?",ingredient);
            update("DELETE FROM [USER] WHERE user_id IN (?,?,?)",owner,other,expert);
        }
    }

    @Test void recipeWithoutIngredientsDoesNotCreateAnEmptyPersonalList() throws Exception {
        update("DELETE FROM [RECIPE_INGREDIENT] WHERE recipe_id=?",first);
        mockMvc.perform(post(route).with(user(""+owner).roles("CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content(body(first)))
            .andExpect(status().isBadRequest());assertThat(countLists(owner)).isZero();
    }

    @Test void guestAdminAndLockedAccountsCannotCreatePersonalLists() throws Exception {
        mockMvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(body(first))).andExpect(status().isUnauthorized());
        long admin=member("ADMIN");
        mockMvc.perform(post(route).with(user(""+admin).roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body(first))).andExpect(status().isForbidden());
        update("UPDATE [USER] SET account_status='LOCKED' WHERE user_id=?",owner);
        mockMvc.perform(post(route).with(user(""+owner).roles("CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content(body(first))).andExpect(status().isForbidden());
        assertThat(countLists(owner)).isZero();
    }

    long add(long member,long... ids) throws Exception {
        var response=mockMvc.perform(post(route).with(user(""+member).roles("CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content(body(ids)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return new tools.jackson.databind.json.JsonMapper().readTree(response).path("shoppingList").path("id").asLong();
    }
    void update(String statement,Object... values) {
        sql.update(statement,values);
        // JDBC fixture changes emulate a separate committed HTTP request, without reusing cached JPA entities.
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) em.clear();
    }
    String body(long... ids) {return "{\"recipeIds\":"+java.util.Arrays.toString(ids)+"}";}
    long member(String role) {return sql.queryForObject("INSERT INTO [USER](email,display_name,role,account_status,email_verified) OUTPUT INSERTED.user_id VALUES (?,N'AC536',?,'ACTIVE',1)",Long.class,UUID.randomUUID()+"@test.local",role);}
    int unit(String code) {return sql.queryForObject("SELECT unit_id FROM [UNIT] WHERE code=?",Integer.class,code);}
    long recipe() {return sql.queryForObject("INSERT INTO [RECIPE_POST](author_id,title,instructions,dish_category,vegetarian_type,difficulty,servings,prep_time_min,cook_time_min,status,published_at) OUTPUT INSERTED.recipe_id VALUES (?,N'Nguồn AC536',N'Nấu nguyên liệu chín rồi dùng.','SOUP','VEGAN','EASY',2,10,15,'PUBLISHED',SYSUTCDATETIME())",Long.class,expert);}
    void line(long recipe,int unit,String quantity) {update("INSERT INTO [RECIPE_INGREDIENT](recipe_id,ingredient_id,unit_id,quantity) VALUES(?,?,?,?)",recipe,ingredient,unit,new BigDecimal(quantity));}
    long item(long list) {return sql.queryForObject("SELECT shopping_list_item_id FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=?",Long.class,list);}
    BigDecimal quantity(long list) {return sql.queryForObject("SELECT quantity FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=?",BigDecimal.class,list);}
    int countLists(long member) {return sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_LIST] WHERE user_id=?",Integer.class,member);}
}
