package tech.mamxanh.admin.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.mamxanh.AbstractIntegrationTest;

/** FR-30 AC-30.4: Administrators see reports grouped by recipe and the supplements of each report, on SQL Server. */
class AdminReportGroupIntegrationTest extends AbstractIntegrationTest {
    private static final String USERS = "issue35g-%@test.local";
    // Far ahead of any other row in the shared database, so these groups are always the newest.
    private static final LocalDateTime BASE = LocalDateTime.of(2100, 1, 1, 8, 0);

    @Autowired private JdbcTemplate jdbcTemplate;

    private long adminId;
    private long customerId;
    private long groupedRecipe;
    private long singleRecipe;
    private long openReport;
    private long inReviewReport;
    private long resolvedReport;
    private long singleReport;

    @BeforeEach
    void prepareFixtures() {
        cleanup();
        adminId = insertUser("issue35g-admin@test.local", "Quản trị viên Thảo", "ADMIN");
        long authorId = insertUser("issue35g-author@test.local", "Chuyên gia Nam", "EXPERT");
        customerId = insertUser("issue35g-c1@test.local", "Thành viên An", "CUSTOMER");
        long second = insertUser("issue35g-c2@test.local", "Thành viên Bảo", "CUSTOMER");
        long third = insertUser("issue35g-c3@test.local", "Thành viên Chi", "CUSTOMER");
        groupedRecipe = insertRecipe(authorId, "Lẩu nấm ba báo cáo");
        singleRecipe = insertRecipe(authorId, "Gỏi cuốn một báo cáo");
        openReport = insertReport(customerId, groupedRecipe, "OPEN", null, BASE.plusHours(1));
        inReviewReport = insertReport(second, groupedRecipe, "IN_REVIEW", null, BASE.plusHours(2));
        resolvedReport = insertReport(third, groupedRecipe, "RESOLVED", "NO_VIOLATION", BASE.plusHours(3));
        singleReport = insertReport(customerId, singleRecipe, "OPEN", null, BASE.plusHours(4));
    }

    @AfterEach
    void cleanupFixtures() {
        cleanup();
    }

    @Test
    void reportsOnOneRecipeFormOneGroupWithTheirCount() throws Exception {
        asAdmin(get("/api/v1/admin/reports/groups").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.recipe.id == %d)]".formatted(groupedRecipe)).value(hasSize(1)))
                .andExpect(jsonPath(group(groupedRecipe, "recipe.title")).value("Lẩu nấm ba báo cáo"))
                .andExpect(jsonPath(group(groupedRecipe, "recipe.status")).value("PUBLISHED"))
                .andExpect(jsonPath(group(groupedRecipe, "author.displayName")).value("Chuyên gia Nam"))
                .andExpect(jsonPath(group(groupedRecipe, "reportCount")).value(3))
                .andExpect(jsonPath(group(groupedRecipe, "statusCounts.open")).value(1))
                .andExpect(jsonPath(group(groupedRecipe, "statusCounts.inReview")).value(1))
                .andExpect(jsonPath(group(groupedRecipe, "statusCounts.resolved")).value(1))
                .andExpect(jsonPath(group(groupedRecipe, "latestReportAt")).value("2100-01-01T11:00:00"))
                .andExpect(jsonPath(group(singleRecipe, "reportCount")).value(1));
    }

    @Test
    void groupsWithTheNewestReportComeFirst() throws Exception {
        asAdmin(get("/api/v1/admin/reports/groups").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].recipe.id").value(singleRecipe))
                .andExpect(jsonPath("$.content[1].recipe.id").value(groupedRecipe));
    }

    @Test
    void theStatusFilterCountsOnlyMatchingReportsAndDropsEmptyGroups() throws Exception {
        asAdmin(get("/api/v1/admin/reports/groups").param("status", "OPEN").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(group(groupedRecipe, "reportCount")).value(1))
                .andExpect(jsonPath(group(groupedRecipe, "statusCounts.resolved")).value(1))
                .andExpect(jsonPath(group(singleRecipe, "reportCount")).value(1));
        asAdmin(get("/api/v1/admin/reports/groups").param("status", "RESOLVED").param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(group(groupedRecipe, "reportCount")).value(1))
                .andExpect(jsonPath("$.content[?(@.recipe.id == %d)]".formatted(singleRecipe)).isEmpty());
        asAdmin(get("/api/v1/admin/reports/groups").param("status", "CLOSED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        asAdmin(get("/api/v1/admin/reports/groups").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void groupPagesCountRecipesNotReports() throws Exception {
        asAdmin(get("/api/v1/admin/reports/groups").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].recipe.id").value(singleRecipe))
                .andExpect(jsonPath("$.totalElements").value(groupCount()));
        asAdmin(get("/api/v1/admin/reports/groups").param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].recipe.id").value(groupedRecipe));
    }

    @Test
    void eachReportOfAGroupKeepsItsReporterStatusAndDecision() throws Exception {
        asAdmin(get("/api/v1/admin/reports").param("recipeId", Long.toString(groupedRecipe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].id").value(resolvedReport))
                .andExpect(jsonPath("$.content[0].reporter.displayName").value("Thành viên Chi"))
                .andExpect(jsonPath("$.content[0].decision").value("NO_VIOLATION"))
                .andExpect(jsonPath("$.content[1].id").value(inReviewReport))
                .andExpect(jsonPath("$.content[1].status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.content[2].id").value(openReport))
                .andExpect(jsonPath("$.content[2].reporter.email").value("issue35g-c1@test.local"));
        asAdmin(get("/api/v1/admin/reports").param("recipeId", Long.toString(groupedRecipe)).param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(openReport));
        asAdmin(get("/api/v1/admin/reports").param("recipeId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void administratorsReadTheSupplementsOfAReportOldestFirst() throws Exception {
        insertSupplement(openReport, "Ảnh bìa có logo cửa hàng bán nước mắm", BASE.plusHours(6));
        insertSupplement(openReport, "Bước 3 hướng dẫn dùng nước mắm cá", BASE.plusHours(5));

        asAdmin(get("/api/v1/admin/reports/{id}", openReport))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supplements.length()").value(2))
                .andExpect(jsonPath("$.supplements[0].content").value("Bước 3 hướng dẫn dùng nước mắm cá"))
                .andExpect(jsonPath("$.supplements[0].createdAt").value("2100-01-01T13:00:00"))
                .andExpect(jsonPath("$.supplements[1].content").value("Ảnh bìa có logo cửa hàng bán nước mắm"));
        asAdmin(get("/api/v1/admin/reports").param("recipeId", Long.toString(groupedRecipe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[2].supplements.length()").value(2))
                .andExpect(jsonPath("$.content[0].supplements").isEmpty());
        asAdmin(get("/api/v1/admin/reports/{id}", singleReport))
                .andExpect(jsonPath("$.supplements").isEmpty());
    }

    @Test
    void onlyAdministratorsReachReportGroups() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/groups"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mockMvc.perform(get("/api/v1/admin/reports/groups").with(principal(customerId, "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    private static String group(long recipeId, String field) {
        return "$.content[?(@.recipe.id == %d)].%s".formatted(recipeId, field);
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(principal(adminId, "ROLE_ADMIN")));
    }

    private RequestPostProcessor principal(long userId, String role) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority(role));
    }

    private long groupCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT recipe_id) FROM [REPORT] WHERE recipe_id IS NOT NULL", Long.class);
    }

    private void cleanup() {
        String users = "SELECT user_id FROM [USER] WHERE email LIKE '" + USERS + "'";
        jdbcTemplate.update("DELETE FROM [REPORT_SUPPLEMENT] WHERE report_id IN (SELECT report_id FROM [REPORT] WHERE reporter_id IN ("
                + users + "))");
        jdbcTemplate.update("DELETE FROM [REPORT] WHERE reporter_id IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE '" + USERS + "'");
    }

    private long insertUser(String email, String displayName, String role) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [USER] (email, display_name, role, account_status, email_verified)
                OUTPUT INSERTED.user_id VALUES (?, ?, ?, 'ACTIVE', 1)
                """, Long.class, email, displayName, role);
    }

    private long insertRecipe(long author, String title) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type, difficulty,
                    servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id VALUES (?, ?, N'Rửa và chế biến nguyên liệu cho món ăn này.',
                    'HOT_POT', 'VEGAN', 'EASY', 2, 10, 15, 'PUBLISHED', SYSUTCDATETIME())
                """, Long.class, author, title);
    }

    private long insertReport(long reporter, long recipe, String status, String decision, LocalDateTime createdAt) {
        String decisionReason = decision == null ? null : "Đã kiểm tra lại công thức";
        Long handledBy = "OPEN".equals(status) ? null : adminId;
        LocalDateTime handledAt = decision == null ? null : createdAt;
        return jdbcTemplate.queryForObject("""
                INSERT INTO [REPORT] (reporter_id, recipe_id, reason_code, description, status, decision,
                    decision_reason, handled_by, handled_at, created_at, updated_at)
                OUTPUT INSERTED.report_id VALUES (?, ?, 'FOOD_SAFETY_HAZARD', N'Món dùng nước mắm cá', ?, ?, ?, ?, ?, ?, ?)
                """, Long.class, reporter, recipe, status, decision, decisionReason, handledBy, handledAt, createdAt, createdAt);
    }

    private void insertSupplement(long reportId, String content, LocalDateTime createdAt) {
        jdbcTemplate.update("INSERT INTO [REPORT_SUPPLEMENT] (report_id, content, created_at) VALUES (?, ?, ?)",
                reportId, content, createdAt);
    }
}
