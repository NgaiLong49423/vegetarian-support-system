package tech.mamxanh.shopping.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.AbstractIntegrationTest;

/** Full Spring/security and real SQL Server: no list is pre-created for the Meal Plan creation API. */
@Transactional
class MealPlanShoppingListIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate sql;
    long owner,other,expert,admin,locked,first,second,ingredient,plan,foreignPlan;
    int g,ml,block;
    final String route="/api/v1/shopping-lists/from-meal-plan";
    final String request="{\"weekStartDate\":\"2026-10-19\"}";

    @BeforeEach void fixtures() {
        owner=member("CUSTOMER","ACTIVE");other=member("CUSTOMER","ACTIVE");expert=member("EXPERT","ACTIVE");
        admin=member("ADMIN","ACTIVE");locked=member("CUSTOMER","LOCKED");
        g=unit("g");ml=unit("ml");block=unit("bìa");
        ingredient=sql.queryForObject("INSERT INTO [INGREDIENT] (name,ingredient_group,source_name,reference_date) OUTPUT INSERTED.ingredient_id VALUES (?,N'Đậu',N'Kiểm thử FR53','2026-10-18')",Long.class,"Đậu "+UUID.randomUUID());
        first=recipe(2);second=recipe(4);
        line(first,g,"300");line(second,g,"800");
        plan=plan(owner);foreignPlan=plan(other);
        entry(plan,first,"2026-10-19","4");entry(plan,second,"2026-10-20","4");entry(plan,first,"2026-10-21","2");
        entry(foreignPlan,second,"2026-10-19","50");
    }

    @Test void wholeWeekScalesRepeatedRecipesAndPersistsAnOwnedUnboughtChecklist() throws Exception {
        long list=create(request,owner);
        assertThat(sql.queryForObject("SELECT user_id FROM [SHOPPING_LIST] WHERE shopping_list_id=?",Long.class,list)).isEqualTo(owner);
        assertThat(quantity(list)).isEqualByComparingTo("1700"); // 300/2*4 + 800/4*4 + 300/2*2
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=? AND is_bought=0",Integer.class,list)).isEqualTo(1);
        mockMvc.perform(get("/api/v1/shopping-lists/"+list).with(user(Long.toString(owner)).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Đi chợ · thực đơn tuần 19–25/10/2026"))
                .andExpect(jsonPath("$.items[0].ingredientGroup").value("Đậu")).andExpect(jsonPath("$.items[0].quantity").value(1700));
        mockMvc.perform(get("/api/v1/shopping-lists/"+list).with(user(Long.toString(other)).roles("CUSTOMER")))
                .andExpect(status().isNotFound());
        sql.update("UPDATE [RECIPE_INGREDIENT] SET quantity=999 WHERE recipe_id=?",first);
        mockMvc.perform(get("/api/v1/shopping-lists/"+list).with(user(Long.toString(owner)).roles("CUSTOMER")))
                .andExpect(jsonPath("$.items[0].quantity").value(1700));
    }

    @Test void selectedDaysDoNotIncludeOtherDaysOrAnotherMembersMealPlan() throws Exception {
        long list=create("{\"weekStartDate\":\"2026-10-19\",\"selectedDates\":[\"2026-10-19\",\"2026-10-20\"]}",owner);
        assertThat(quantity(list)).isEqualByComparingTo("1400");
        assertThat(countLists(other)).isZero();
        assertThat(sql.queryForObject("SELECT COUNT(*) FROM [MEAL_PLAN_ENTRY] WHERE meal_plan_id=?",Integer.class,foreignPlan)).isEqualTo(1);
    }

    @Test void massAndVolumeWithoutDensityStaySeparateAndTheirSavedIdsAreReported() throws Exception {
        sql.update("DELETE FROM [RECIPE_INGREDIENT] WHERE recipe_id=?",first);
        line(first,g,"300");line(first,ml,"200");
        var result=mockMvc.perform(post(route).with(user(Long.toString(owner)).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(selectedMonday()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.shoppingList.items.length()").value(2))
                .andExpect(jsonPath("$.incompatibleItemIds.length()").value(2)).andReturn();
        // IDs in the creation diagnostics must refer to persisted items of this newly created list.
        var json=tools.jackson.databind.json.JsonMapper.builder().build().readTree(result.getResponse().getContentAsString());
        long list=json.path("shoppingList").path("id").asLong();
        for(var id:json.path("incompatibleItemIds")) assertThat(sql.queryForObject(
                "SELECT shopping_list_id FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_item_id=?",Long.class,id.asLong())).isEqualTo(list);
        assertThat(sql.queryForObject("SELECT quantity FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=? AND unit_id=?",BigDecimal.class,list,g)).isEqualByComparingTo("600");
        assertThat(sql.queryForObject("SELECT quantity FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=? AND unit_id=?",BigDecimal.class,list,ml)).isEqualByComparingTo("400");
    }

    @Test void officialCountRuleIsAppliedBeforeSavingAndMissingNutritionDoesNotBlockCreation() throws Exception {
        sql.update("DELETE FROM [RECIPE_INGREDIENT] WHERE recipe_id=?",first);
        line(first,block,"2");line(first,g,"300");
        sql.update("INSERT INTO [INGREDIENT_UNIT_CONVERSION] (ingredient_id,unit_id,grams_per_unit) VALUES (?,?,150)",ingredient,block);
        long list=create(selectedMonday(),owner);
        assertThat(quantity(list)).isEqualByComparingTo("1200"); // planned 4 / base 2, then FR-54: 4 blocks + 600g
    }

    @Test void unavailableRecipeIsSkippedWithAnEntryWarningAndNoPrivateContent() throws Exception {
        sql.update("UPDATE [RECIPE_POST] SET status='HIDDEN' WHERE recipe_id=?",second);
        mockMvc.perform(post(route).with(user(Long.toString(owner)).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.shoppingList.items[0].quantity").value(900))
                .andExpect(jsonPath("$.warnings[0].code").value("UNAVAILABLE_RECIPE"))
                .andExpect(jsonPath("$.warnings[0].recipeId").value(second))
                .andExpect(jsonPath("$.warnings[0].mealPlanEntryId").isNumber());
    }

    @Test void unavailableOrEmptyOwnSelectionDoesNotFallBackToAnotherOwnersPlanOrCreateAnEmptyList() throws Exception {
        sql.update("UPDATE [RECIPE_POST] SET status='DELETED' WHERE recipe_id IN (?,?)",first,second);
        failure(request,owner);
        sql.update("DELETE FROM [MEAL_PLAN] WHERE meal_plan_id=?",plan);
        failure(request,owner);
        assertThat(countLists(owner)).isZero();assertThat(countLists(other)).isZero();
    }

    @Test void invalidDateSelectionsAreRejectedWithoutWriting() throws Exception {
        for(String body:new String[]{"{\"weekStartDate\":\"2026-10-20\"}",
                "{\"weekStartDate\":\"2026-10-19\",\"selectedDates\":[\"2026-10-18\"]}",
                "{\"weekStartDate\":\"2026-10-19\",\"selectedDates\":[\"2026-10-26\"]}",
                "{\"weekStartDate\":\"2026-10-19\",\"selectedDates\":[\"2026-10-19\",\"2026-10-19\"]}",
                "{\"weekStartDate\":\"2026-10-19\",\"selectedDates\":[\"2026-10-25\"]}"}) failure(body,owner);
        assertThat(countLists(owner)).isZero();
    }

    @Test void rejectsGuestsAdminsAndLockedAccountsButAcceptsAnExpertMembersOwnPlan() throws Exception {
        mockMvc.perform(post(route).contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isUnauthorized());
        for(long member: new long[]{admin,locked}) mockMvc.perform(post(route).with(user(Long.toString(member)).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
        long expertPlan=plan(expert);entry(expertPlan,first,"2026-10-19","2");
        long list=create(request,expert);assertThat(quantity(list)).isEqualByComparingTo("300");
    }

    @Test void sourceRoundingHappensAfterAggregation() throws Exception {
        sql.update("UPDATE [RECIPE_POST] SET servings=3 WHERE recipe_id IN (?,?)",first,second);
        sql.update("UPDATE [RECIPE_INGREDIENT] SET quantity=1.01 WHERE recipe_id IN (?,?)",first,second);
        sql.update("UPDATE [MEAL_PLAN_ENTRY] SET planned_servings=1 WHERE meal_plan_id=?",plan);
        mockMvc.perform(post(route).with(user(Long.toString(owner)).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"weekStartDate\":\"2026-10-19\",\"selectedDates\":[\"2026-10-19\",\"2026-10-20\"]}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.shoppingList.items[0].quantity").value(0.67))
                .andExpect(jsonPath("$.warnings[0].code").value("QUANTITY_ROUNDED"));
        assertThat(countLists(owner)).isEqualTo(1);
    }

    @Test void underflowCannotLeaveAnEmptyOrPartialChecklist() throws Exception {
        sql.update("UPDATE [RECIPE_POST] SET servings=50 WHERE recipe_id=?",first);
        sql.update("UPDATE [RECIPE_INGREDIENT] SET quantity=0.01 WHERE recipe_id=?",first);
        sql.update("UPDATE [MEAL_PLAN_ENTRY] SET planned_servings=0.1 WHERE meal_plan_id=?",plan);
        failure(selectedMonday(),owner);
        assertThat(countLists(owner)).isZero();
    }

    @Test void overflowCannotLeaveAnEmptyOrPartialChecklist() throws Exception {
        sql.update("UPDATE [RECIPE_POST] SET servings=1 WHERE recipe_id=?",first);
        sql.update("UPDATE [RECIPE_INGREDIENT] SET quantity=99999999.99 WHERE recipe_id=?",first);
        sql.update("UPDATE [MEAL_PLAN_ENTRY] SET planned_servings=50 WHERE meal_plan_id=?",plan);
        failure(selectedMonday(),owner);
        assertThat(countLists(owner)).isZero();
    }

    @Test void selectedMealDatesSurviveSavingAndAppearInBothListAndDetailResponses() throws Exception {
        String[] requests = { selectedMonday(),
                "{\"weekStartDate\":\"2026-10-19\",\"selectedDates\":[\"2026-10-21\",\"2026-10-19\"]}" };
        String[] names = { "Đi chợ · thực đơn 19/10/2026", "Đi chợ · thực đơn 19, 21/10/2026" };
        for (int i = 0; i < requests.length; i++) {
            long list = create(requests[i], owner);
            assertThat(sql.queryForObject("SELECT name FROM [SHOPPING_LIST] WHERE shopping_list_id=?", String.class, list))
                    .isEqualTo(names[i]);
            mockMvc.perform(get("/api/v1/shopping-lists/" + list).with(user(Long.toString(owner)).roles("CUSTOMER")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(names[i]));
        }
        mockMvc.perform(get("/api/v1/shopping-lists").with(user(Long.toString(owner)).roles("CUSTOMER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[*].name", org.hamcrest.Matchers.containsInAnyOrder(names)));
    }

    long create(String body,long member) throws Exception {
        var result=mockMvc.perform(post(route).with(user(Long.toString(member)).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andExpect(header().exists("Location")).andReturn();
        return Long.parseLong(result.getResponse().getHeader("Location").substring("/api/v1/shopping-lists/".length()));
    }
    void failure(String body,long member) throws Exception {
        mockMvc.perform(post(route).with(user(Long.toString(member)).roles("CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }
    long member(String role,String status) {return sql.queryForObject("INSERT INTO [USER] (email,display_name,role,account_status,email_verified) OUTPUT INSERTED.user_id VALUES (?,N'FR53 nguồn',?,?,1)",Long.class,"fr53-source-"+UUID.randomUUID()+"@test.local",role,status);}
    int unit(String code) {return sql.queryForObject("SELECT unit_id FROM [UNIT] WHERE code=?",Integer.class,code);}
    long recipe(int servings) {return sql.queryForObject("INSERT INTO [RECIPE_POST] (author_id,title,instructions,dish_category,vegetarian_type,difficulty,servings,prep_time_min,cook_time_min,status,published_at) OUTPUT INSERTED.recipe_id VALUES (?,N'Nguồn FR53',N'Nấu nguyên liệu chín rồi dùng.','SOUP','VEGAN','EASY',?,10,15,'PUBLISHED',SYSUTCDATETIME())",Long.class,expert,servings);}
    void line(long recipe,int unit,String quantity) {sql.update("INSERT INTO [RECIPE_INGREDIENT] (recipe_id,ingredient_id,unit_id,quantity) VALUES (?,?,?,?)",recipe,ingredient,unit,new BigDecimal(quantity));}
    long plan(long member) {return sql.queryForObject("INSERT INTO [MEAL_PLAN] (user_id,week_start_date) OUTPUT INSERTED.meal_plan_id VALUES (?,'2026-10-19')",Long.class,member);}
    void entry(long plan,long recipe,String date,String servings) {sql.update("INSERT INTO [MEAL_PLAN_ENTRY] (meal_plan_id,recipe_id,meal_date,meal_type,planned_servings) VALUES (?,?,?,'LUNCH',?)",plan,recipe,date,new BigDecimal(servings));}
    BigDecimal quantity(long list) {return sql.queryForObject("SELECT quantity FROM [SHOPPING_LIST_ITEM] WHERE shopping_list_id=?",BigDecimal.class,list);}
    int countLists(long member) {return sql.queryForObject("SELECT COUNT(*) FROM [SHOPPING_LIST] WHERE user_id=?",Integer.class,member);}
    String selectedMonday() {return "{\"weekStartDate\":\"2026-10-19\",\"selectedDates\":[\"2026-10-19\"]}";}
}
