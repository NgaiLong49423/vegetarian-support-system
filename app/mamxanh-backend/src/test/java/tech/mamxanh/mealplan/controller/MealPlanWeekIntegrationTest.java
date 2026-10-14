package tech.mamxanh.mealplan.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.mealplan.service.MealPlanService;
import tech.mamxanh.mealplan.dto.request.AddMealPlanEntryRequest;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import org.springframework.http.MediaType;
import java.util.concurrent.atomic.AtomicInteger;

class MealPlanWeekIntegrationTest extends AbstractIntegrationTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MealPlanService service;
    private long owner;
    private long other;
    private long admin;
    private long locked;

    @BeforeEach
    void fixtures() {
        owner = insertUser("owner", "CUSTOMER", "ACTIVE");
        other = insertUser("other", "EXPERT", "ACTIVE");
        admin = insertUser("admin", "ADMIN", "ACTIVE");
        locked = insertUser("locked", "CUSTOMER", "LOCKED");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM [MEAL_PLAN] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'fr09-week-%@test.local')");
        jdbc.update("DELETE FROM [SAVED_RECIPE] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'fr09-week-%@test.local')");
        jdbc.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'fr09-week-%@test.local')");
        jdbc.update("DELETE FROM [USER] WHERE email LIKE 'fr09-week-%@test.local'");
    }

    @Test
    void readingAnEmptyWeekDoesNotPersistIt() throws Exception {
        mockMvc.perform(get("/api/v1/meal-plans").param("weekStartDate", MONDAY.toString()).with(user(Long.toString(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entries").isEmpty());
        assertThat(count(owner)).isZero();
    }

    @Test
    void flywaySeedsCurrentSharedNutritionReferenceValues() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM [NUTRITION_DAILY_VALUE]", Integer.class)).isEqualTo(9);
        assertThat(jdbc.queryForObject("SELECT reference_value FROM [NUTRITION_DAILY_VALUE] WHERE nutrient_key = 'IRON_MG'", java.math.BigDecimal.class))
                .isEqualByComparingTo("18");
        assertThat(jdbc.queryForObject("SELECT source_url FROM [NUTRITION_DAILY_VALUE] WHERE nutrient_key = 'IRON_MG'", String.class))
                .contains("fda.gov");
    }

    @Test
    void reopeningAWeekKeepsItsIdentityAndEntries() throws Exception {
        open(owner, MONDAY);
        long plan = jdbc.queryForObject("SELECT meal_plan_id FROM [MEAL_PLAN] WHERE user_id = ?", Long.class, owner);
        long recipe = jdbc.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type,
                    difficulty, servings, prep_time_min, cook_time_min, status)
                OUTPUT INSERTED.recipe_id VALUES (?, N'Món thử lịch tuần', N'Nấu món thử.',
                    'SOUP', 'VEGAN', 'EASY', 1, 10, 15, 'PUBLISHED')
                """, Long.class, other);
        jdbc.update("INSERT INTO [MEAL_PLAN_ENTRY] (meal_plan_id, recipe_id, meal_date, meal_type, planned_servings) VALUES (?, ?, ?, 'LUNCH', 1)",
                plan, recipe, MONDAY.plusDays(1));
        open(owner, MONDAY);
        assertThat(count(owner)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT meal_plan_id FROM [MEAL_PLAN] WHERE user_id = ?", Long.class, owner)).isEqualTo(plan);
        mockMvc.perform(get("/api/v1/meal-plans").param("weekStartDate", MONDAY.toString()).with(user(Long.toString(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entries[0].recipeId").value(recipe));
    }

    @Test
    void ownerAndWeekAreIndependentAndQueryCannotChooseAnotherOwner() throws Exception {
        open(other, MONDAY);
        mockMvc.perform(put("/api/v1/meal-plans").param("weekStartDate", MONDAY.toString())
                        .param("userId", Long.toString(other)).with(user(Long.toString(owner))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entries").isEmpty());
        open(owner, LocalDate.of(2027, 1, 4));
        assertThat(count(owner)).isEqualTo(2);
        assertThat(count(other)).isEqualTo(1);
    }

    @Test
    void invalidDateCannotCreateAPlan() throws Exception {
        mockMvc.perform(put("/api/v1/meal-plans").param("weekStartDate", MONDAY.plusDays(1).toString())
                        .with(user(Long.toString(owner))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(count(owner)).isZero();
    }

    @Test
    void guestAdminAndLockedAccountsCannotCreateAPlan() throws Exception {
        mockMvc.perform(put("/api/v1/meal-plans").param("weekStartDate", MONDAY.toString())).andExpect(status().isUnauthorized());
        for (long id : List.of(admin, locked)) {
            mockMvc.perform(put("/api/v1/meal-plans").param("weekStartDate", MONDAY.toString()).with(user(Long.toString(id))))
                    .andExpect(status().isForbidden());
            assertThat(count(id)).isZero();
        }
    }

    @Test
    void concurrentOpenRequestsCreateExactlyOneWeek() throws Exception {
        int workers = 4;
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(workers);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < workers; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                                Long.toString(owner), null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
                        assertThat(service.openWeek(MONDAY).entries()).isEmpty();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(ex);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }
            start.countDown();
            for (var future : futures) future.get(20, TimeUnit.SECONDS);
            assertThat(count(owner)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void multipleRecipesShareAMealButDuplicatesAreRejected() throws Exception {
        long first = recipe("PUBLISHED");
        long second = recipe("PUBLISHED");
        add(owner, first, "LUNCH", 201);
        add(owner, second, "LUNCH", 201);
        add(owner, first, "LUNCH", 409);
        add(owner, first, "DINNER", 201);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM [MEAL_PLAN_ENTRY] WHERE meal_plan_id IN (SELECT meal_plan_id FROM [MEAL_PLAN] WHERE user_id = ?)", Integer.class, owner)).isEqualTo(3);
    }

    @Test
    void unavailableSourceRollsBackCreationOfANewWeek() throws Exception {
        for (String state : List.of("HIDDEN", "DELETED")) {
            add(owner, recipe(state), "LUNCH", 404);
            assertThat(count(owner)).isZero();
        }
        add(owner, Long.MAX_VALUE, "LUNCH", 404);
        assertThat(count(owner)).isZero();
    }

    @Test
    void tombstoneCanBeReplacedAndRemovedWithoutChangingSavedOrSourceRecipes() throws Exception {
        long first = recipe("PUBLISHED");
        long replacement = recipe("PUBLISHED");
        add(owner, first, "LUNCH", 201);
        long entry = entryId(owner, first);
        jdbc.update("INSERT INTO [SAVED_RECIPE] (user_id, recipe_id) VALUES (?, ?)", owner, first);
        jdbc.update("UPDATE [MEAL_PLAN_ENTRY] SET planned_servings = 2.5 WHERE meal_plan_entry_id = ?", entry);
        jdbc.update("UPDATE [RECIPE_POST] SET status = 'HIDDEN' WHERE recipe_id = ?", first);
        mockMvc.perform(get("/api/v1/meal-plans").param("weekStartDate", MONDAY.toString()).with(user(Long.toString(owner))))
                .andExpect(jsonPath("$.entries[0].unavailableMessage").value("Công thức không còn khả dụng"))
                .andExpect(jsonPath("$.entries[0].recipeTitle").isEmpty());
        mockMvc.perform(put(entryPath(entry)).with(user(Long.toString(owner))).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipeId\":" + replacement + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.entryId").value(entry))
                .andExpect(jsonPath("$.plannedServings").value(2.5)).andExpect(jsonPath("$.mealType").value("LUNCH"));
        jdbc.update("UPDATE [RECIPE_POST] SET status = 'DELETED' WHERE recipe_id = ?", replacement);
        mockMvc.perform(delete(entryPath(entry)).with(user(Long.toString(owner)))).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM [SAVED_RECIPE] WHERE user_id = ? AND recipe_id = ?", Integer.class, owner, first)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM [RECIPE_POST] WHERE recipe_id IN (?, ?)", Integer.class, first, replacement)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM [MEAL_PLAN_ENTRY] WHERE meal_plan_entry_id = ?", Integer.class, entry)).isZero();
    }

    @Test
    void foreignEntryAndDuplicateReplacementLeaveDataUnchanged() throws Exception {
        long first = recipe("PUBLISHED");
        long second = recipe("PUBLISHED");
        add(owner, first, "LUNCH", 201);
        add(owner, second, "LUNCH", 201);
        long entry = entryId(owner, first);
        open(other, MONDAY);
        mockMvc.perform(put(entryPath(entry)).with(user(Long.toString(other))).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipeId\":" + second + "}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(entryPath(entry)).with(user(Long.toString(other)))).andExpect(status().isForbidden());
        mockMvc.perform(put(entryPath(entry)).with(user(Long.toString(owner))).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipeId\":" + second + "}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEAL_PLAN_RECIPE_DUPLICATE"));
        assertThat(jdbc.queryForObject("SELECT recipe_id FROM [MEAL_PLAN_ENTRY] WHERE meal_plan_entry_id = ?", Long.class, entry)).isEqualTo(first);
    }

    @Test
    void concurrentDuplicateAddsProduceOneEntryAndThreeConflicts() throws Exception {
        long recipe = recipe("PUBLISHED");
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(4);
        var conflicts = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 4; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timed out");
                        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                                Long.toString(owner), null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
                        service.addEntry(MONDAY, new AddMealPlanEntryRequest(MONDAY, "LUNCH", recipe));
                    } catch (AppException ex) {
                        assertThat(ex.errorCode()).isEqualTo(ErrorCode.MEAL_PLAN_RECIPE_DUPLICATE);
                        conflicts.incrementAndGet();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(ex);
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }
            start.countDown();
            for (var future : futures) future.get(20, TimeUnit.SECONDS);
            assertThat(conflicts.get()).isEqualTo(3);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM [MEAL_PLAN_ENTRY] WHERE recipe_id = ?", Integer.class, recipe)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private String entryPath(long entry) {
        return "/api/v1/meal-plans/" + MONDAY + "/entries/" + entry;
    }

    private long entryId(long member, long recipe) {
        return jdbc.queryForObject("SELECT meal_plan_entry_id FROM [MEAL_PLAN_ENTRY] WHERE recipe_id = ? AND meal_plan_id IN (SELECT meal_plan_id FROM [MEAL_PLAN] WHERE user_id = ?)", Long.class, recipe, member);
    }

    private void add(long member, long recipe, String meal, int status) throws Exception {
        mockMvc.perform(post("/api/v1/meal-plans/" + MONDAY + "/entries").with(user(Long.toString(member)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealDate\":\"" + MONDAY + "\",\"mealType\":\"" + meal + "\",\"recipeId\":" + recipe + "}"))
                .andExpect(status().is(status));
    }

    private long recipe(String status) {
        return jdbc.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type,
                    difficulty, servings, prep_time_min, cook_time_min, status)
                OUTPUT INSERTED.recipe_id VALUES (?, N'Món thử chỉnh lịch', N'Nấu món thử.',
                    'SOUP', 'VEGAN', 'EASY', 1, 10, 15, ?)
                """, Long.class, other, status);
    }

    private void open(long id, LocalDate date) throws Exception {
        mockMvc.perform(put("/api/v1/meal-plans").param("weekStartDate", date.toString()).with(user(Long.toString(id))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.weekStartDate").value(date.toString()));
    }

    private int count(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM [MEAL_PLAN] WHERE user_id = ?", Integer.class, id);
    }

    private long insertUser(String name, String role, String state) {
        return jdbc.queryForObject("""
                INSERT INTO [USER] (email, display_name, role, account_status, email_verified)
                OUTPUT INSERTED.user_id VALUES (?, N'Tài khoản thử lịch tuần', ?, ?, 1)
                """, Long.class, "fr09-week-" + name + "@test.local", role, state);
    }
}
