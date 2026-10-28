package tech.mamxanh.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.auth.security.AccessTokenService;

/** FR-28 report handling by Administrators, on SQL Server (AC-28.1–AC-28.5, AC-49.4). */
class AdminReportIntegrationTest extends AbstractIntegrationTest {
    private static final String USERS = "issue33-%@test.local";

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private AccessTokenService accessTokenService;
    @Autowired private UserRepository userRepository;

    private long adminId;
    private long otherAdminId;
    private long reporterId;
    private long authorId;
    private long recipeId;
    private long reportId;

    @BeforeEach
    void prepareFixtures() {
        cleanup();
        adminId = insertUser("issue33-admin@test.local", "Quản trị viên Minh", "ADMIN");
        otherAdminId = insertUser("issue33-admin2@test.local", "Quản trị viên Lan", "ADMIN");
        reporterId = insertUser("issue33-reporter@test.local", "Người báo cáo Hà", "CUSTOMER");
        authorId = insertUser("issue33-author@test.local", "Chuyên gia Bình", "EXPERT");
        recipeId = insertRecipe(authorId, "Canh nấm hạt sen", "PUBLISHED");
        reportId = insertReport(reporterId, recipeId, "OPEN", "Món có dùng nước mắm cá");
    }

    @AfterEach
    void cleanupFixtures() {
        cleanup();
    }

    @Test
    void administratorListsRecipeReportsWithReporterAuthorAndStatus() throws Exception {
        long commentReport = insertCommentReport(reporterId, recipeId);

        asAdmin(get("/api/v1/admin/reports").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)].recipe.title".formatted(reportId)).value("Canh nấm hạt sen"))
                .andExpect(jsonPath("$.content[?(@.id == %d)].reporter.email".formatted(reportId)).value("issue33-reporter@test.local"))
                .andExpect(jsonPath("$.content[?(@.id == %d)].author.displayName".formatted(reportId)).value("Chuyên gia Bình"))
                .andExpect(jsonPath("$.content[?(@.id == %d)].status".formatted(reportId)).value("OPEN"))
                .andExpect(jsonPath("$.content[?(@.id == %d)].reasonCode".formatted(reportId)).value("OTHER"))
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(commentReport)).isEmpty());

        asAdmin(get("/api/v1/admin/reports").param("status", "RESOLVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == %d)]".formatted(reportId)).isEmpty());
        asAdmin(get("/api/v1/admin/reports").param("status", "CLOSED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        asAdmin(get("/api/v1/admin/reports/{id}", commentReport))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));
    }

    @Test
    void statusCountsCoverEveryRecipeReportState() throws Exception {
        long before = count("OPEN");
        insertReport(insertUser("issue33-r2@test.local", "Người báo cáo Khoa", "CUSTOMER"), recipeId, "IN_REVIEW", null);

        asAdmin(get("/api/v1/admin/reports/status-counts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.open").value(before))
                .andExpect(jsonPath("$.inReview").value(count("IN_REVIEW")))
                .andExpect(jsonPath("$.resolved").value(count("RESOLVED")));
    }

    @Test
    void onlyAdministratorsReachTheReportQueue() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mockMvc.perform(get("/api/v1/admin/reports/{id}", reportId).with(principal(reporterId, "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(post("/api/v1/admin/reports/{id}/claim", reportId).with(principal(authorId, "ROLE_EXPERT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        assertThat(reportColumn("status")).isEqualTo("OPEN");
    }

    @Test
    void claimMovesOpenReportToInReviewWithTheHandlingAdministrator() throws Exception {
        claim(adminId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.handledBy.id").value(adminId))
                .andExpect(jsonPath("$.handledBy.displayName").value("Quản trị viên Minh"));

        assertThat(jdbcTemplate.queryForObject("SELECT handled_by FROM [REPORT] WHERE report_id = ?", Long.class, reportId))
                .isEqualTo(adminId);
        asAdmin(get("/api/v1/admin/reports").param("status", "IN_REVIEW"))
                .andExpect(jsonPath("$.content[?(@.id == %d)].handledBy.displayName".formatted(reportId))
                        .value("Quản trị viên Minh"));
    }

    @Test
    void legacyInReviewReportWithoutHandlerCanStillBeTakenAndResolved() throws Exception {
        jdbcTemplate.update("UPDATE [REPORT] SET status = 'IN_REVIEW' WHERE report_id = ?", reportId);

        claim(adminId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.handledBy.id").value(adminId));
        resolve(adminId, "NO_VIOLATION", "Báo cáo cũ đã được kiểm tra lại.").andExpect(status().isOk());
    }

    @Test
    void secondClaimIsRejectedSoTwoAdministratorsDoNotHandleTheSameReport() throws Exception {
        claim(adminId).andExpect(status().isOk());

        claim(otherAdminId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_STATUS_CONFLICT"));
        assertThat(jdbcTemplate.queryForObject("SELECT handled_by FROM [REPORT] WHERE report_id = ?", Long.class, reportId))
                .isEqualTo(adminId);
    }

    @Test
    void concurrentClaimsLetExactlyOneAdministratorTakeTheReport() throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
            for (long admin : List.of(adminId, otherAdminId)) {
                futures.add(executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return claim(admin).andReturn().getResponse();
                }));
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<MockHttpServletResponse> future : futures) {
                statuses.add(future.get(60, TimeUnit.SECONDS).getStatus());
            }
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
            assertThat(jdbcTemplate.queryForObject("SELECT handled_by FROM [REPORT] WHERE report_id = ?", Long.class, reportId))
                    .isIn(adminId, otherAdminId);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void noViolationResolvesReportAndKeepsRecipePublic() throws Exception {
        claim(adminId).andExpect(status().isOk());

        resolve(adminId, "NO_VIOLATION", "  Công thức dùng nước tương, không có nước mắm cá.  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.decision").value("NO_VIOLATION"))
                .andExpect(jsonPath("$.decisionReason").value("Công thức dùng nước tương, không có nước mắm cá."))
                .andExpect(jsonPath("$.handledAt").isNotEmpty());

        assertThat(recipeStatus()).isEqualTo("PUBLISHED");
        mockMvc.perform(get("/api/v1/recipes/{id}", recipeId)).andExpect(status().isOk());
        assertThat(notificationCount(authorId)).isZero();
        assertThat(notificationMessage(reporterId, "REPORT_RESULT")).contains("Canh nấm hạt sen").contains("chưa thấy nội dung vi phạm");
    }

    @Test
    void hideRecipeRemovesItFromPublicReadsAndNotifiesTheAuthorWithoutReporterIdentity() throws Exception {
        String inResults = "$..[?(@.id == %d && @.title == 'Canh nấm hạt sen')]".formatted(recipeId);
        for (String searchPath : List.of("/api/v1/recipes", "/api/v1/recipes/discovery")) {
            mockMvc.perform(get(searchPath).param("keyword", "Canh nấm hạt sen")).andExpect(jsonPath(inResults).isNotEmpty());
        }
        claim(adminId).andExpect(status().isOk());

        resolve(adminId, "HIDE_RECIPE", "Món dùng nước mắm cá nên không phải món chay.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.decision").value("HIDE_RECIPE"))
                .andExpect(jsonPath("$.recipe.status").value("HIDDEN"));

        assertThat(recipeStatus()).isEqualTo("HIDDEN");
        mockMvc.perform(get("/api/v1/recipes/{id}", recipeId)).andExpect(status().isNotFound());
        for (String searchPath : List.of("/api/v1/recipes", "/api/v1/recipes/discovery")) {
            mockMvc.perform(get(searchPath).param("keyword", "Canh nấm hạt sen"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath(inResults).isEmpty());
        }

        String authorMessage = notificationMessage(authorId, "MODERATION_DECISION");
        assertThat(authorMessage).contains("Canh nấm hạt sen").contains("Món dùng nước mắm cá nên không phải món chay.");
        assertNoReporterTrace(authorId);
        assertThat(notificationMessage(reporterId, "REPORT_RESULT")).contains("đã xử lý nội dung vi phạm");
    }

    @Test
    void warnAuthorKeepsRecipePublicAndSendsTheReasonToTheAuthor() throws Exception {
        claim(adminId).andExpect(status().isOk());

        resolve(adminId, "WARN_AUTHOR", "Hãy ghi rõ loại nước chấm chay trong phần nguyên liệu.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("WARN_AUTHOR"));

        assertThat(recipeStatus()).isEqualTo("PUBLISHED");
        assertThat(notificationMessage(authorId, "MODERATION_DECISION"))
                .contains("Hãy ghi rõ loại nước chấm chay trong phần nguyên liệu.");
        assertNoReporterTrace(authorId);
    }

    @Test
    void lockAuthorLocksTheAccountAndRejectsItsExistingAccessToken() throws Exception {
        String authorToken = accessTokenService.issue(userRepository.findById(authorId).orElseThrow()).value();
        mockMvc.perform(get("/api/v1/recipes/mine").header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk());
        claim(adminId).andExpect(status().isOk());

        resolve(adminId, "LOCK_AUTHOR", "Tài khoản đăng lại nội dung sai nhãn chay nhiều lần.")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("LOCK_AUTHOR"))
                .andExpect(jsonPath("$.author.accountStatus").value("LOCKED"));

        assertThat(jdbcTemplate.queryForObject("SELECT account_status FROM [USER] WHERE user_id = ?", String.class, authorId))
                .isEqualTo("LOCKED");
        mockMvc.perform(get("/api/v1/recipes/mine").header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
        assertNoReporterTrace(authorId);
    }

    @Test
    void resolveRejectsMissingOrShortReasonAndKeepsTheReportInReview() throws Exception {
        claim(adminId).andExpect(status().isOk());

        for (String body : List.of("{\"decision\":\"HIDE_RECIPE\"}", "{\"decision\":\"HIDE_RECIPE\",\"reason\":\"   \"}",
                "{\"decision\":\"HIDE_RECIPE\",\"reason\":\"ngắn\"}",
                "{\"decision\":\"HIDE_RECIPE\",\"reason\":\"%s\"}".formatted("x".repeat(501))))
        {
            resolveRaw(adminId, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("reason"))
                    .andExpect(jsonPath("$.errors[0].message").value("Nhập lý do kết luận từ 10 đến 500 ký tự."));
        }
        resolveRaw(adminId, "{\"reason\":\"Lý do đủ dài để lưu lại\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("decision"));
        resolveRaw(adminId, "{\"decision\":\"DELETE_EVERYTHING\",\"reason\":\"Lý do đủ dài để lưu lại\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("decision"));

        assertThat(reportColumn("status")).isEqualTo("IN_REVIEW");
        assertThat(reportColumn("decision")).isNull();
        assertThat(recipeStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void onlyTheHandlingAdministratorResolvesAnInReviewReportAndResolvedReportsStayUnchanged() throws Exception {
        resolve(adminId, "NO_VIOLATION", "Chưa tiếp nhận nhưng muốn đóng ngay.")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_STATUS_CONFLICT"));

        claim(adminId).andExpect(status().isOk());
        resolve(otherAdminId, "HIDE_RECIPE", "Quản trị viên khác cố ghi kết luận.")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_STATUS_CONFLICT"));
        assertThat(recipeStatus()).isEqualTo("PUBLISHED");

        resolve(adminId, "NO_VIOLATION", "Nguyên liệu trong bài đều là đồ chay.").andExpect(status().isOk());
        resolve(adminId, "HIDE_RECIPE", "Đổi ý sau khi đã giải quyết báo cáo.")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_STATUS_CONFLICT"));
        claim(otherAdminId).andExpect(status().isConflict());

        assertThat(reportColumn("decision")).isEqualTo("NO_VIOLATION");
        assertThat(reportColumn("decision_reason")).isEqualTo("Nguyên liệu trong bài đều là đồ chay.");
        assertThat(recipeStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void hidingADeletedRecipeIsRejectedWithoutResolvingTheReport() throws Exception {
        claim(adminId).andExpect(status().isOk());
        jdbcTemplate.update("UPDATE [RECIPE_POST] SET status = 'DELETED' WHERE recipe_id = ?", recipeId);

        resolve(adminId, "HIDE_RECIPE", "Bài vi phạm nhưng tác giả đã xóa.")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_DECISION_NOT_APPLICABLE"));

        assertThat(reportColumn("status")).isEqualTo("IN_REVIEW");
        assertThat(recipeStatus()).isEqualTo("DELETED");
    }

    @Test
    void detailShowsReporterAndAuthorAccountsToAdministrators() throws Exception {
        asAdmin(get("/api/v1/admin/reports/{id}", reportId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Món có dùng nước mắm cá"))
                .andExpect(jsonPath("$.reporter.id").value(reporterId))
                .andExpect(jsonPath("$.reporter.displayName").value("Người báo cáo Hà"))
                .andExpect(jsonPath("$.author.id").value(authorId))
                .andExpect(jsonPath("$.author.email").value("issue33-author@test.local"))
                .andExpect(jsonPath("$.author.accountStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.handledBy").doesNotExist());
        asAdmin(get("/api/v1/admin/reports/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));
    }

    private ResultActions claim(long admin) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/reports/{id}/claim", reportId).with(principal(admin, "ROLE_ADMIN")));
    }

    private ResultActions resolve(long admin, String decision, String reason) throws Exception {
        return resolveRaw(admin, "{\"decision\":\"%s\",\"reason\":\"%s\"}".formatted(decision, reason));
    }

    private ResultActions resolveRaw(long admin, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/reports/{id}/resolve", reportId)
                .with(principal(admin, "ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions asAdmin(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return mockMvc.perform(request.with(principal(adminId, "ROLE_ADMIN")));
    }

    private void assertNoReporterTrace(long userId) {
        List<String> texts = jdbcTemplate.queryForList(
                "SELECT title + N' ' + message + N' ' + COALESCE(target_path, N'') FROM [NOTIFICATION] WHERE user_id = ?",
                String.class, userId);
        assertThat(texts).isNotEmpty();
        assertThat(texts).allSatisfy(text -> assertThat(text)
                .doesNotContain("Người báo cáo Hà")
                .doesNotContain("issue33-reporter")
                .doesNotContain("Món có dùng nước mắm cá")
                .doesNotContain(Long.toString(reporterId) + "/"));
    }

    private String notificationMessage(long userId, String type) {
        return jdbcTemplate.queryForObject(
                "SELECT title + N' ' + message FROM [NOTIFICATION] WHERE user_id = ? AND notification_type = ? AND report_id = ?",
                String.class, userId, type, reportId);
    }

    private int notificationCount(long userId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM [NOTIFICATION] WHERE user_id = ?", Integer.class, userId);
    }

    private long count(String status) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [REPORT] WHERE recipe_id IS NOT NULL AND status = ?", Long.class, status);
    }

    private String reportColumn(String column) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM [REPORT] WHERE report_id = ?", String.class, reportId);
    }

    private String recipeStatus() {
        return jdbcTemplate.queryForObject("SELECT status FROM [RECIPE_POST] WHERE recipe_id = ?", String.class, recipeId);
    }

    private RequestPostProcessor principal(long userId, String role) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority(role));
    }

    private void cleanup() {
        String users = "SELECT user_id FROM [USER] WHERE email LIKE '" + USERS + "'";
        jdbcTemplate.update("DELETE FROM [NOTIFICATION] WHERE user_id IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [REPORT] WHERE reporter_id IN (" + users + ") OR handled_by IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [COMMENT] WHERE user_id IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE '" + USERS + "'");
    }

    private long insertUser(String email, String displayName, String role) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [USER] (email, display_name, role, account_status, email_verified)
                OUTPUT INSERTED.user_id VALUES (?, ?, ?, 'ACTIVE', 1)
                """, Long.class, email, displayName, role);
    }

    private long insertRecipe(long author, String title, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type, difficulty,
                    servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id VALUES (?, ?, N'Rửa và chế biến nguyên liệu cho món ăn này.',
                    'BRAISED', 'VEGAN', 'EASY', 2, 10, 15, ?, SYSUTCDATETIME())
                """, Long.class, author, title, status);
    }

    private long insertReport(long reporter, long recipe, String status, String description) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [REPORT] (reporter_id, recipe_id, reason_code, description, status)
                OUTPUT INSERTED.report_id VALUES (?, ?, 'OTHER', ?, ?)
                """, Long.class, reporter, recipe, description, status);
    }

    private long insertCommentReport(long reporter, long recipe) {
        long commentId = jdbcTemplate.queryForObject("""
                INSERT INTO [COMMENT] (recipe_id, user_id, content)
                OUTPUT INSERTED.comment_id VALUES (?, ?, N'Bình luận cần xem xét')
                """, Long.class, recipe, authorId);
        return jdbcTemplate.queryForObject("""
                INSERT INTO [REPORT] (reporter_id, comment_id, reason_code, status)
                OUTPUT INSERTED.report_id VALUES (?, ?, 'SPAM_ADVERTISING', 'OPEN')
                """, Long.class, reporter, commentId);
    }
}
