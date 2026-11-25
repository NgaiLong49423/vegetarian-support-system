package tech.mamxanh.admin.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.mamxanh.AbstractIntegrationTest;

/** Shared SQL Server fixture for appeals on hidden Recipe Posts (FR-28, BR-27). */
abstract class AbstractAppealIntegrationTest extends AbstractIntegrationTest {
    static final String USERS = "appeal-%@test.local";
    static final String HIDE_REASON = "Bài ghi sai nhãn thuần chay vì có nước mắm.";
    static final String INGREDIENT_NAME = "Đậu hũ non appeal fixture";
    static final String MESSAGE = "Em đã dùng nước tương chay, không phải nước mắm, mong Admin xem lại.";

    @Autowired protected JdbcTemplate jdbcTemplate;

    protected long adminId;
    protected long reporterId;
    protected long authorId;
    protected long otherExpertId;
    protected long customerId;
    protected long recipeId;
    protected long reportId;

    @BeforeEach
    void prepareAppealFixtures() {
        cleanup();
        adminId = insertUser("appeal-admin@test.local", "Quản trị viên Minh", "ADMIN");
        reporterId = insertUser("appeal-reporter@test.local", "Người báo cáo Hà", "CUSTOMER");
        authorId = insertUser("appeal-author@test.local", "Chuyên gia Bình", "EXPERT");
        otherExpertId = insertUser("appeal-other@test.local", "Chuyên gia Lan", "EXPERT");
        customerId = insertUser("appeal-customer@test.local", "Thành viên Vy", "CUSTOMER");
        recipeId = insertRecipe(authorId, "Canh nấm hạt sen", "HIDDEN");
        reportId = insertHideReport(recipeId, "2026-11-20T08:00:00");
    }

    @AfterEach
    void cleanupAppealFixtures() {
        cleanup();
    }

    protected RequestPostProcessor principal(long userId, String role) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority(role));
    }

    protected RequestPostProcessor asAuthor() {
        return principal(authorId, "ROLE_EXPERT");
    }

    protected RequestPostProcessor asAdmin() {
        return principal(adminId, "ROLE_ADMIN");
    }

    protected void setAppealStatus(long report, String status) {
        jdbcTemplate.update("""
                UPDATE [REPORT] SET appeal_status = ?, appeal_message = ?, appealed_at = SYSUTCDATETIME()
                WHERE report_id = ?
                """, status, MESSAGE, report);
    }

    protected String appealStatus(long report) {
        return jdbcTemplate.queryForObject("SELECT appeal_status FROM [REPORT] WHERE report_id = ?", String.class, report);
    }

    protected String recipeStatus() {
        return jdbcTemplate.queryForObject("SELECT status FROM [RECIPE_POST] WHERE recipe_id = ?", String.class, recipeId);
    }

    protected long insertUser(String email, String displayName, String role) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [USER] (email, display_name, role, account_status, email_verified)
                OUTPUT INSERTED.user_id VALUES (?, ?, ?, 'ACTIVE', 1)
                """, Long.class, email, displayName, role);
    }

    protected long insertRecipe(long author, String title, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type, difficulty,
                    servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id VALUES (?, ?, N'Rửa và chế biến nguyên liệu cho món ăn này.',
                    'BRAISED', 'VEGAN', 'EASY', 2, 10, 15, ?, SYSUTCDATETIME())
                """, Long.class, author, title, status);
    }

    protected long insertHideReport(long recipe, String handledAt) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [REPORT] (reporter_id, recipe_id, reason_code, description, status, decision,
                    decision_reason, handled_by, handled_at)
                OUTPUT INSERTED.report_id VALUES (?, ?, 'RECIPE_INFO_OR_DIET_LABEL', N'Món có nước mắm cá',
                    'RESOLVED', 'HIDE_RECIPE', ?, ?, ?)
                """, Long.class, reporterId, recipe, HIDE_REASON, adminId, handledAt);
    }

    private void cleanup() {
        String users = "SELECT user_id FROM [USER] WHERE email LIKE '" + USERS + "'";
        jdbcTemplate.update("DELETE FROM [NOTIFICATION] WHERE user_id IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [REPORT] WHERE reporter_id IN (" + users + ") OR handled_by IN (" + users
                + ") OR appeal_handled_by IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [RECIPE_MEDIA] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST] WHERE author_id IN ("
                + users + "))");
        jdbcTemplate.update("DELETE FROM [RECIPE_INGREDIENT] WHERE recipe_id IN (SELECT recipe_id FROM [RECIPE_POST] WHERE author_id IN ("
                + users + "))");
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [INGREDIENT] WHERE name = ?", INGREDIENT_NAME);
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE '" + USERS + "'");
    }
}
