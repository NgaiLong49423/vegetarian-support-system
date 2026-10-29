package tech.mamxanh.recipe.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.ResultActions;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.auth.security.AuthProperties;
import tech.mamxanh.recipe.service.RecipeViewService;

/** Real SQL Server evidence: no mock repository, security decoder or transaction manager. */
class RecipeViewIntegrationTest extends AbstractIntegrationTest {
    private static final String PREFIX = "issue57-views-%@test.local";
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtEncoder encoder;
    @Autowired AuthProperties authProperties;
    @Autowired RecipeViewService service;
    private long author;
    private long member;
    private long recipe;
    private String guest;

    @BeforeEach void prepare() {
        removeFixtures();
        author = user("author", "EXPERT");
        member = user("member", "CUSTOMER");
        recipe = jdbc.queryForObject("""
                INSERT INTO RECIPE_POST (author_id,title,instructions,dish_category,vegetarian_type,difficulty,
                    servings,prep_time_min,cook_time_min,status,published_at)
                OUTPUT INSERTED.recipe_id VALUES (?, N'Issue57 canh nấm',N'Nấu nguyên liệu cho chín.',
                    'SOUP','VEGAN','EASY',2,10,15,'PUBLISHED',?)
                """, Long.class, author, java.time.LocalDateTime.now(clock).minusDays(60));
        guest = UUID.randomUUID().toString();
    }

    @AfterEach
    void removeFixtures() {
        jdbc.update("DELETE FROM RECIPE_VIEW WHERE recipe_id IN (SELECT recipe_id FROM RECIPE_POST WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?))", PREFIX);
        jdbc.update("DELETE FROM RECIPE_POST WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?)", PREFIX);
        jdbc.update("DELETE FROM [USER] WHERE email LIKE ?", PREFIX);
    }

    @Test void guestReloadsDoNotExtendWindowAndReturnPersistsToReadApis() throws Exception {
        view(null, guest).andExpect(status().isOk()).andExpect(jsonPath("$.counted").value(true));
        clock.advance(Duration.ofMinutes(15));
        view(null, guest).andExpect(status().isOk()).andExpect(jsonPath("$.counted").value(false));
        clock.advance(Duration.ofMinutes(15));
        view(null, guest).andExpect(status().isOk()).andExpect(jsonPath("$.counted").value(false));
        clock.advance(Duration.ofMinutes(5));
        view(null, guest).andExpect(status().isOk()).andExpect(jsonPath("$.counted").value(true))
                .andExpect(jsonPath("$.viewCount").value(2));
        assertCounts(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM RECIPE_VIEW WHERE recipe_id=? AND user_id IS NULL AND LEN(anonymous_viewer_hash)=64", Integer.class, recipe)).isEqualTo(2);
        mockMvc.perform(get("/api/v1/recipes/{recipeId}", recipe)).andExpect(status().isOk())
                .andExpect(jsonPath("$.statistics.viewCount").value(2));
        mockMvc.perform(get("/api/v1/recipes").param("keyword", "Issue57 canh nấm"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].viewCount").value(2));
    }

    @Test void differentAccountsCountSeparatelyAndIgnoreAnonymousBodyIdentity() throws Exception {
        long other = user("other", "CUSTOMER");
        view(member, guest).andExpect(status().isOk()).andExpect(jsonPath("$.counted").value(true));
        view(member, UUID.randomUUID().toString()).andExpect(status().isOk()).andExpect(jsonPath("$.counted").value(false));
        view(other, guest).andExpect(status().isOk()).andExpect(jsonPath("$.counted").value(true));
        assertCounts(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM RECIPE_VIEW WHERE recipe_id=? AND anonymous_viewer_hash IS NULL AND user_id IS NOT NULL", Integer.class, recipe)).isEqualTo(2);
    }

    @Test void simultaneousFirstGuestViewsCreateExactlyOneEvent() throws Exception { concurrent(null); }
    @Test void simultaneousFirstMemberViewsCreateExactlyOneEvent() throws Exception { concurrent(member); }

    @Test void insertionFailureRollsBackCounterAndReleasesTransactionLock() {
        long absentUser = jdbc.queryForObject("SELECT MAX(user_id)+10000 FROM [USER]", Long.class);
        assertThatThrownBy(() -> service.record(recipe, absentUser, null)).isInstanceOf(RuntimeException.class);
        assertCounts(0);
        var result = service.record(recipe, member, null);
        assertThat(result.counted()).isTrue();
        assertCounts(1);
    }

    @Test void hiddenDeletedAndMissingRecipesCannotReceiveViews() throws Exception {
        for (String state : new String[]{"HIDDEN", "DELETED"}) {
            jdbc.update("UPDATE RECIPE_POST SET status=? WHERE recipe_id=?", state, recipe);
            view(null, guest).andExpect(status().isNotFound());
            assertCounts(0);
        }
        mockMvc.perform(post("/api/v1/recipes/9223372036854775807/views").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"anonymousViewerId\":\""+guest+"\"}"))
                .andExpect(status().isNotFound());
    }

    @Test void invalidIdentityAndLockedOrInvalidBearerCannotWrite() throws Exception {
        view(null, null).andExpect(status().isBadRequest());
        String token = token(member);
        jdbc.update("UPDATE [USER] SET account_status='LOCKED' WHERE user_id=?", member);
        mockMvc.perform(post("/api/v1/recipes/{id}/views", recipe).header("Authorization", "Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
        mockMvc.perform(post("/api/v1/recipes/{id}/views", recipe).header("Authorization", "Bearer invalid")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        assertCounts(0);
    }

    @Test void migrationCreatesBothLastEventLookupIndexes() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sys.indexes WHERE object_id=OBJECT_ID('RECIPE_VIEW') AND name IN ('IX_RECIPE_VIEW_member_recipe_time','IX_RECIPE_VIEW_guest_recipe_time')", Integer.class)).isEqualTo(2);
    }

    @Test void recordedViewsDriveAllFourWindowsWithoutReloadInflation() throws Exception {
        long allTime = additionalRecipe("alltime", 60);
        long month = additionalRecipe("month", 60);
        long week = additionalRecipe("week", 60);
        long day = additionalRecipe("day", 60);
        clock.advance(Duration.ofDays(-40));
        recordGuestViews(allTime, 5);
        clock.advance(Duration.ofDays(20));
        recordGuestViews(month, 4);
        clock.advance(Duration.ofDays(15));
        recordGuestViews(week, 3);
        clock.advance(Duration.ofDays(4));
        recordGuestViews(day, 2);
        clock.advance(Duration.ofDays(1));
        view(null, guest).andExpect(status().isOk()).andExpect(jsonPath("$.counted").value(true));
        view(null, guest).andExpect(status().isOk()).andExpect(jsonPath("$.counted").value(false));
        assertCounts(1);
        ranking("MOST_VIEWED", "ALL_TIME", allTime);
        ranking("MOST_VIEWED", "LAST_30_DAYS", month);
        ranking("MOST_VIEWED", "LAST_7_DAYS", week);
        ranking("MOST_VIEWED", "LAST_24_HOURS", day);
        mockMvc.perform(get("/api/v1/recipes/{id}", allTime)).andExpect(status().isOk())
                .andExpect(jsonPath("$.statistics.viewCount").value(5));
        assertThat(jdbc.queryForObject("SELECT view_count FROM RECIPE_POST WHERE recipe_id=?",Integer.class,allTime)).isEqualTo(5);
    }

    @Test void futureEventsAreOutsideEveryMostViewedWindow() throws Exception {
        long valid = additionalRecipe("past", 60);
        recordGuestViews(valid, 1);
        for (int i=0; i<3; i++) jdbc.update("INSERT INTO RECIPE_VIEW (recipe_id,viewed_at) VALUES (?,?)",
                recipe,java.time.LocalDateTime.now(clock).plusDays(1));
        for (String period : new String[]{"ALL_TIME","LAST_24_HOURS","LAST_7_DAYS","LAST_30_DAYS"}) {
            ranking("MOST_VIEWED",period,valid);
        }
    }

    @Test void timeWindowLowerBoundIsInclusiveAndEventsJustBeforeItAreExcluded() throws Exception {
        long included = additionalRecipe("included", 60);
        var now = java.time.LocalDateTime.now(clock);
        for (int hours : new int[]{24,168,720}) {
            jdbc.update("DELETE FROM RECIPE_VIEW WHERE recipe_id IN (?,?)",recipe,included);
            jdbc.update("INSERT INTO RECIPE_VIEW (recipe_id,viewed_at) VALUES (?,?)",included,now.minusHours(hours));
            for (int i=0; i<3; i++) jdbc.update("INSERT INTO RECIPE_VIEW (recipe_id,viewed_at) VALUES (?,?)",
                    recipe,now.minusHours(hours).minusSeconds(1));
            ranking("MOST_VIEWED",hours==24 ? "LAST_24_HOURS" : hours==168 ? "LAST_7_DAYS" : "LAST_30_DAYS",included);
        }
    }

    @Test void recordedViewsFeedActivityAndTrendingWithDifferentFreshnessBehavior() throws Exception {
        long old = additionalRecipe("old", 365);
        long fresh = additionalRecipe("fresh", 5);
        recordGuestViews(old, 2);
        recordGuestViews(fresh, 1);
        ranking("MOST_ACTIVE","ALL_TIME",old);
        ranking("TRENDING","ALL_TIME",fresh);
    }

    @Test void activityUsesSevenDaysWhileTrendingUsesThreeDays() throws Exception {
        long old = additionalRecipe("old", 365);
        long fresh = additionalRecipe("fresh", 5);
        clock.advance(Duration.ofDays(-4));
        recordGuestViews(old, 2);
        clock.advance(Duration.ofDays(4));
        ranking("MOST_ACTIVE","ALL_TIME",old);
        // Neither recipe has 72h events or a <48h bonus; the newer publication wins the tie.
        ranking("TRENDING","ALL_TIME",fresh);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM RECIPE_VIEW WHERE recipe_id=?",Integer.class,fresh)).isZero();
    }

    private long additionalRecipe(String name, int ageDays) {
        return jdbc.queryForObject("""
                INSERT INTO RECIPE_POST (author_id,title,instructions,dish_category,vegetarian_type,difficulty,
                    servings,prep_time_min,cook_time_min,status,published_at)
                OUTPUT INSERTED.recipe_id VALUES (?, ?, N'Nấu nguyên liệu cho chín.',
                    'SOUP','VEGAN','EASY',2,10,15,'PUBLISHED',?)
                """,Long.class,author,"Issue57 thống kê "+name,java.time.LocalDateTime.now(clock).minusDays(ageDays));
    }

    private void recordGuestViews(long id, int count) {
        for (int i=0; i<count; i++) assertThat(service.record(id,null,UUID.randomUUID().toString()).counted()).isTrue();
    }

    private void ranking(String mode, String period, long firstId) throws Exception {
        mockMvc.perform(get("/api/v1/recipes").param("keyword","Issue57").param("sort",mode).param("viewPeriod",period))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(firstId));
    }

    private void concurrent(Long viewer) throws Exception {
        var start = new CountDownLatch(1);
        var calls = new ArrayList<Callable<Integer>>();
        for (int i=0; i<6; i++) calls.add(() -> {
            assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
            return view(viewer, guest).andReturn().getResponse().getStatus();
        });
        try (var executor = Executors.newFixedThreadPool(6)) {
            var futures = calls.stream().map(executor::submit).toList();
            start.countDown();
            for (var future : futures) assertThat(future.get(15, TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertCounts(1);
    }

    private ResultActions view(Long user, String anonymous) throws Exception {
        var request = post("/api/v1/recipes/{id}/views", recipe).contentType(MediaType.APPLICATION_JSON)
                .content(anonymous == null ? "{}" : "{\"anonymousViewerId\":\""+anonymous+"\"}");
        if (user != null) request.header("Authorization", "Bearer "+token(user));
        return mockMvc.perform(request);
    }

    private String token(long userId) {
        var claims = JwtClaimsSet.builder().issuer(authProperties.jwtIssuer()).subject(Long.toString(userId))
                .issuedAt(clock.instant()).expiresAt(clock.instant().plus(Duration.ofHours(1))).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
    }

    private long user(String name, String role) {
        return jdbc.queryForObject("INSERT INTO [USER] (email,display_name,role,account_status,email_verified) OUTPUT INSERTED.user_id VALUES (?,N'Kiểm thử lượt xem',?,'ACTIVE',1)",
                Long.class, "issue57-views-"+name+"@test.local",role);
    }

    private void assertCounts(int expected) {
        assertThat(jdbc.queryForObject("SELECT view_count FROM RECIPE_POST WHERE recipe_id=?",Integer.class,recipe)).isEqualTo(expected);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM RECIPE_VIEW WHERE recipe_id=?",Integer.class,recipe)).isEqualTo(expected);
    }
}
