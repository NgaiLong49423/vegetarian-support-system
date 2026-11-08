package tech.mamxanh.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.ResultActions;
import tech.mamxanh.AbstractIntegrationTest;

/**
 * FR-49 email for moderation outcomes (UC-49.3): sent after the report decision commits, to the reporter
 * and to the affected author, without any trace of the reporter in the author's email (BR-28, AC-49.4),
 * and without rolling the decision back when delivery fails (AC-49.3).
 */
class ModerationResultEmailIntegrationTest extends AbstractIntegrationTest {
    private static final String EMAIL_PREFIX = "issue51-moderation-";
    private static final String REPORTER_EMAIL = EMAIL_PREFIX + "reporter@test.local";
    private static final String AUTHOR_EMAIL = EMAIL_PREFIX + "author@test.local";
    private static final String RECIPE_TITLE = "Canh nấm hạt sen";
    private static final String REPORT_TEXT = "Món có dùng nước mắm cá";

    @Autowired private JdbcTemplate jdbcTemplate;

    private long adminId;
    private long reporterId;
    private long authorId;
    private long recipeId;
    private long reportId;

    @BeforeEach
    void prepareFixtures() {
        cleanup();
        adminId = insertUser(EMAIL_PREFIX + "admin@test.local", "Quản trị viên Minh", "ADMIN");
        reporterId = insertUser(REPORTER_EMAIL, "Người báo cáo Hà", "CUSTOMER");
        authorId = insertUser(AUTHOR_EMAIL, "Chuyên gia Bình", "EXPERT");
        recipeId = jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type, difficulty,
                    servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id VALUES (?, ?, N'Rửa và chế biến nguyên liệu cho món ăn này.',
                    'BRAISED', 'VEGAN', 'EASY', 2, 10, 15, 'PUBLISHED', SYSUTCDATETIME())
                """, Long.class, authorId, RECIPE_TITLE);
        reportId = jdbcTemplate.queryForObject("""
                INSERT INTO [REPORT] (reporter_id, recipe_id, reason_code, description, status)
                OUTPUT INSERTED.report_id VALUES (?, ?, 'OTHER', ?, 'OPEN')
                """, Long.class, reporterId, recipeId, REPORT_TEXT);
    }

    @AfterEach
    void cleanupFixtures() {
        cleanup();
    }

    @Test
    void theReporterIsEmailedTheResultEvenWithReplyEmailsTurnedOff() throws Exception {
        jdbcTemplate.update("UPDATE [USER] SET reply_email_enabled = 0 WHERE user_id = ?", reporterId);

        claimAndResolve("NO_VIOLATION", "Công thức dùng nước tương, không có nước mắm cá.").andExpect(status().isOk());

        String body = capturedBody(REPORTER_EMAIL, "Báo cáo của bạn đã có kết quả trên Mâm Xanh");
        assertThat(body).contains("Xin chào Người báo cáo Hà,")
                .contains(RECIPE_TITLE)
                .contains("chưa thấy nội dung vi phạm")
                .contains("/cong-thuc/id/" + recipeId)
                .doesNotContain(";");
        verify(emailSender, after(500).never()).sendPlainText(eq(AUTHOR_EMAIL), anyString(), anyString());
    }

    @Test
    void hidingThePostEmailsTheAuthorTheReasonWithoutAnyTraceOfTheReporter() throws Exception {
        claimAndResolve("HIDE_RECIPE", "Món dùng nước mắm cá nên không phải món chay.").andExpect(status().isOk());

        String authorBody = capturedBody(AUTHOR_EMAIL, "Bài công thức của bạn đã bị ẩn trên Mâm Xanh");
        assertThat(authorBody).contains("Xin chào Chuyên gia Bình,")
                .contains(RECIPE_TITLE)
                .contains("Món dùng nước mắm cá nên không phải món chay.")
                .contains("/ho-so/chuyen-gia")
                .doesNotContain("Người báo cáo Hà")
                .doesNotContain(REPORTER_EMAIL)
                .doesNotContain(REPORT_TEXT)
                .doesNotContain(";");
        // The post is no longer public, so the reporter is not sent to a page that cannot open.
        assertThat(capturedBody(REPORTER_EMAIL, "Báo cáo của bạn đã có kết quả trên Mâm Xanh"))
                .contains("đã xử lý nội dung vi phạm")
                .doesNotContain("/cong-thuc/id/");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT target_path FROM [NOTIFICATION] WHERE user_id = ? AND notification_type = 'REPORT_RESULT'",
                String.class, reporterId)).isNull();
    }

    @Test
    void aWarnedAuthorIsEmailedTheWarning() throws Exception {
        claimAndResolve("WARN_AUTHOR", "Hãy ghi rõ loại nước chấm chay trong phần nguyên liệu.").andExpect(status().isOk());

        assertThat(capturedBody(AUTHOR_EMAIL, "Bài công thức của bạn nhận được cảnh báo trên Mâm Xanh"))
                .contains("Hãy ghi rõ loại nước chấm chay trong phần nguyên liệu.")
                .doesNotContain("Người báo cáo Hà");
    }

    @Test
    void aLockedAuthorIsStillEmailedWhyTheAccountWasLocked() throws Exception {
        claimAndResolve("LOCK_AUTHOR", "Tài khoản đăng lại nội dung sai nhãn chay nhiều lần.").andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT account_status FROM [USER] WHERE user_id = ?", String.class, authorId))
                .isEqualTo("LOCKED");
        assertThat(capturedBody(AUTHOR_EMAIL, "Tài khoản của bạn đã bị khóa trên Mâm Xanh"))
                .contains("Tài khoản đăng lại nội dung sai nhãn chay nhiều lần.")
                .doesNotContain("http")
                .doesNotContain("Người báo cáo Hà");
    }

    @Test
    void anUnverifiedAddressGetsNoEmailButKeepsTheInAppNotification() throws Exception {
        jdbcTemplate.update("UPDATE [USER] SET email_verified = 0 WHERE user_id = ?", reporterId);

        claimAndResolve("NO_VIOLATION", "Công thức dùng nước tương, không có nước mắm cá.").andExpect(status().isOk());

        verify(emailSender, after(500).never()).sendPlainText(eq(REPORTER_EMAIL), anyString(), anyString());
        assertThat(notificationCount(reporterId)).isEqualTo(1);
    }

    @Test
    void aRejectedDecisionSendsNoEmail() throws Exception {
        // Not claimed first: the decision is rejected and rolled back, so nothing is announced.
        resolve("HIDE_RECIPE", "Món dùng nước mắm cá nên không phải món chay.").andExpect(status().isConflict());

        verify(emailSender, after(500).never()).sendPlainText(anyString(), anyString(), anyString());
    }

    @Test
    void anEmailFailureKeepsTheDecisionAndTheInAppNotifications() throws Exception {
        doThrow(new IllegalStateException("SMTP down")).when(emailSender)
                .sendPlainText(anyString(), anyString(), anyString());

        claimAndResolve("HIDE_RECIPE", "Món dùng nước mắm cá nên không phải món chay.").andExpect(status().isOk());

        verify(emailSender, timeout(5000)).sendPlainText(eq(AUTHOR_EMAIL), anyString(), anyString());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM [REPORT] WHERE report_id = ?", String.class, reportId))
                .isEqualTo("RESOLVED");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM [RECIPE_POST] WHERE recipe_id = ?", String.class, recipeId))
                .isEqualTo("HIDDEN");
        assertThat(notificationCount(reporterId)).isEqualTo(1);
        assertThat(notificationCount(authorId)).isEqualTo(1);
    }

    private String capturedBody(String to, String subject) {
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender, timeout(5000)).sendPlainText(eq(to), eq(subject), body.capture());
        return body.getValue();
    }

    private ResultActions claimAndResolve(String decision, String reason) throws Exception {
        mockMvc.perform(post("/api/v1/admin/reports/{id}/claim", reportId).with(admin())).andExpect(status().isOk());
        return resolve(decision, reason);
    }

    private ResultActions resolve(String decision, String reason) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/reports/{id}/resolve", reportId)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"%s\",\"reason\":\"%s\"}".formatted(decision, reason)));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return user(Long.toString(adminId)).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private int notificationCount(long userId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM [NOTIFICATION] WHERE user_id = ?", Integer.class, userId);
    }

    private long insertUser(String email, String displayName, String role) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [USER] (email, display_name, role, account_status, email_verified)
                OUTPUT INSERTED.user_id VALUES (?, ?, ?, 'ACTIVE', 1)
                """, Long.class, email, displayName, role);
    }

    private void cleanup() {
        String users = "SELECT user_id FROM [USER] WHERE email LIKE '" + EMAIL_PREFIX + "%'";
        jdbcTemplate.update("DELETE FROM [NOTIFICATION] WHERE user_id IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [REPORT] WHERE reporter_id IN (" + users + ") OR handled_by IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE '" + EMAIL_PREFIX + "%'");
    }
}
