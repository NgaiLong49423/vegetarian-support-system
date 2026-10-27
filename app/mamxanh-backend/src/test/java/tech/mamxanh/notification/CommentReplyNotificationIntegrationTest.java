package tech.mamxanh.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import tech.mamxanh.AbstractIntegrationTest;

/** FR-49 reply notifications (AC-49.1–AC-49.3, UC-49.1–UC-49.4, NFR-09) against SQL Server. */
class CommentReplyNotificationIntegrationTest extends AbstractIntegrationTest {
    private static final String EMAIL_PREFIX = "issue51-notify-";
    private static final String REPLY_SUBJECT = "Có phản hồi mới cho bình luận của bạn trên Mâm Xanh";

    @Autowired private JdbcTemplate jdbcTemplate;

    private long author;
    private long memberA;
    private long memberB;
    private long recipe;

    @BeforeEach
    void prepareFixtures() {
        removeFixtures();
        author = insertUser("author", "EXPERT");
        memberA = insertUser("a", "CUSTOMER");
        memberB = insertUser("b", "CUSTOMER");
        recipe = insertRecipe("Canh chua chay");
    }

    @AfterEach
    void removeFixtures() {
        jdbcTemplate.update("DELETE FROM [NOTIFICATION] WHERE user_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?)",
                EMAIL_PREFIX + "%");
        jdbcTemplate.update("""
                DELETE FROM [COMMENT] WHERE recipe_id IN (
                    SELECT recipe_id FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?))
                """, EMAIL_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE ?)",
                EMAIL_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE ?", EMAIL_PREFIX + "%");
    }

    @Test
    void aReplyToAnotherMembersCommentCreatesOneUnreadNotificationForThatMember() throws Exception {
        long root = insertComment(memberA, null, "Món này nấu bao lâu ạ?");

        long reply = reply(memberB, root, "Khoảng 20 phút nhé.");

        assertThat(notificationCount(memberA)).isEqualTo(1);
        assertThat(notificationCount(memberB)).isZero();
        assertThat(jdbcTemplate.queryForObject("""
                SELECT CONCAT(notification_type, '|', comment_id, '|', target_path, '|', CONVERT(varchar(1), is_read))
                FROM [NOTIFICATION] WHERE user_id = ?
                """, String.class, memberA))
                .isEqualTo("COMMENT_REPLY|" + reply + "|/cong-thuc/id/" + recipe + "#comment-" + reply + "|0");
        mockMvc.perform(get("/api/v1/me/notifications/unread-count").with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));
        mockMvc.perform(get("/api/v1/me/notifications").with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("COMMENT_REPLY"))
                .andExpect(jsonPath("$.content[0].title").value("Có phản hồi mới cho bình luận của bạn"))
                .andExpect(jsonPath("$.content[0].message")
                        .value("Issue 51 b đã trả lời bình luận của bạn trong công thức “Canh chua chay”."))
                .andExpect(jsonPath("$.content[0].targetPath").value("/cong-thuc/id/" + recipe + "#comment-" + reply))
                .andExpect(jsonPath("$.content[0].read").value(false));
    }

    @Test
    void rootCommentsAndRepliesToOwnCommentsCreateNoNotification() throws Exception {
        String created = mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipe).with(principal(memberA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Bình luận gốc\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        reply(memberA, idOf(created), "Tự bổ sung thêm");

        assertThat(notificationCount(memberA)).isZero();
        assertThat(notificationCount(author)).isZero();
        verify(emailSender, after(300).never()).sendPlainText(anyString(), anyString(), anyString());
    }

    @Test
    void aReplyToALevelFiveCommentNotifiesTheAuthorOfThatComment() throws Exception {
        long level1 = insertComment(memberA, null, "Cấp 1");
        long level2 = insertComment(memberB, level1, "Cấp 2");
        long level3 = insertComment(memberA, level2, "Cấp 3");
        long level4 = insertComment(memberB, level3, "Cấp 4");
        long level5 = insertComment(memberA, level4, "Cấp 5");

        long reply = reply(memberB, level5, "Đồng ý");

        // The reply is stored under the level-4 comment written by B, but B answered A's level-5 comment.
        assertThat(jdbcTemplate.queryForObject("SELECT parent_comment_id FROM [COMMENT] WHERE comment_id = ?", Long.class,
                reply)).isEqualTo(level4);
        assertThat(notificationCount(memberA)).isEqualTo(1);
        assertThat(notificationCount(memberB)).isZero();
    }

    @Test
    void membersSeeOnlyTheirOwnNotificationsNewestFirstAndCanFilterUnread() throws Exception {
        long rootA = insertComment(memberA, null, "Của A");
        long rootB = insertComment(memberB, null, "Của B");
        long first = reply(memberB, rootA, "Trả lời thứ nhất");
        clock.advance(Duration.ofMinutes(1));
        long second = reply(author, rootA, "Trả lời thứ hai");
        reply(memberA, rootB, "A trả lời B");
        long firstNotification = notificationIdFor(memberA, first);
        jdbcTemplate.update("UPDATE [NOTIFICATION] SET is_read = 1, read_at = SYSUTCDATETIME() WHERE notification_id = ?",
                firstNotification);

        mockMvc.perform(get("/api/v1/me/notifications").param("size", "1").with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].targetPath").value("/cong-thuc/id/" + recipe + "#comment-" + second));
        mockMvc.perform(get("/api/v1/me/notifications").param("unreadOnly", "true").with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].read").value(false));
        mockMvc.perform(get("/api/v1/me/notifications").with(principal(memberB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].message")
                        .value("Issue 51 a đã trả lời bình luận của bạn trong công thức “Canh chua chay”."));
    }

    @Test
    void listParametersAreBounded() throws Exception {
        mockMvc.perform(get("/api/v1/me/notifications").param("size", "51").with(principal(memberA)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/me/notifications").param("page", "-1").with(principal(memberA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aMemberMarksOneOrAllOwnNotificationsReadButNotSomeoneElses() throws Exception {
        long rootA = insertComment(memberA, null, "Của A");
        long rootB = insertComment(memberB, null, "Của B");
        long toA1 = reply(memberB, rootA, "Một");
        reply(author, rootA, "Hai");
        long toB = reply(memberA, rootB, "Ba");
        long notificationA1 = notificationIdFor(memberA, toA1);
        long notificationB = notificationIdFor(memberB, toB);

        mockMvc.perform(put("/api/v1/me/notifications/{notificationId}/read", notificationB).with(principal(memberA)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
        assertThat(isRead(notificationB)).isFalse();

        mockMvc.perform(put("/api/v1/me/notifications/{notificationId}/read", notificationA1).with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(notificationA1))
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.readAt").isNotEmpty());
        mockMvc.perform(put("/api/v1/me/notifications/{notificationId}/read", notificationA1).with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
        mockMvc.perform(get("/api/v1/me/notifications/unread-count").with(principal(memberA)))
                .andExpect(jsonPath("$.unreadCount").value(1));

        mockMvc.perform(put("/api/v1/me/notifications/read-all").with(principal(memberA)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/me/notifications/unread-count").with(principal(memberA)))
                .andExpect(jsonPath("$.unreadCount").value(0));
        assertThat(isRead(notificationB)).isFalse();
    }

    @Test
    void guestsCannotReadNotificationsOrPreferences() throws Exception {
        mockMvc.perform(get("/api/v1/me/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me/notifications/unread-count")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/me/notifications/read-all")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me/notification-preferences")).andExpect(status().isUnauthorized());
    }

    @Test
    void aReplyEmailIsSentAfterCommitWhenTheMemberKeepsReplyEmailsOn() throws Exception {
        long root = insertComment(memberA, null, "Món này nấu bao lâu ạ?");
        mockMvc.perform(get("/api/v1/me/notification-preferences").with(principal(memberA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replyEmailEnabled").value(true));

        long reply = reply(memberB, root, "Khoảng 20 phút nhé.");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailSender, timeout(5000)).sendPlainText(eq(EMAIL_PREFIX + "a@test.local"), eq(REPLY_SUBJECT), body.capture());
        // Text shown to Members does not use ";" (lecturer feedback).
        assertThat(body.getValue()).contains("/cong-thuc/id/" + recipe + "#comment-" + reply).doesNotContain(";");
    }

    @Test
    void noReplyEmailIsSentAfterTheMemberTurnsReplyEmailsOff() throws Exception {
        mockMvc.perform(put("/api/v1/me/notification-preferences").with(principal(memberA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"replyEmailEnabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.replyEmailEnabled").value(false));
        assertThat(jdbcTemplate.queryForObject("SELECT reply_email_enabled FROM [USER] WHERE user_id = ?", Boolean.class,
                memberA)).isFalse();
        long root = insertComment(memberA, null, "Món này nấu bao lâu ạ?");

        reply(memberB, root, "Khoảng 20 phút nhé.");

        assertThat(notificationCount(memberA)).isEqualTo(1);
        verify(emailSender, after(500).never()).sendPlainText(anyString(), anyString(), anyString());
    }

    @Test
    void preferencesRejectAMissingValue() throws Exception {
        mockMvc.perform(put("/api/v1/me/notification-preferences").with(principal(memberA))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void aFailingEmailServerDoesNotRollBackTheReplyOrItsNotification() throws Exception {
        doThrow(new IllegalStateException("SMTP down")).when(emailSender)
                .sendPlainText(anyString(), anyString(), anyString());
        long root = insertComment(memberA, null, "Món này nấu bao lâu ạ?");

        long reply = reply(memberB, root, "Khoảng 20 phút nhé.");

        verify(emailSender, timeout(5000)).sendPlainText(anyString(), eq(REPLY_SUBJECT), anyString());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM [COMMENT] WHERE comment_id = ?", Integer.class, reply))
                .isEqualTo(1);
        assertThat(notificationCount(memberA)).isEqualTo(1);
    }

    private long reply(long userId, long parentId, String content) throws Exception {
        String json = mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipe).with(principal(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\",\"parentCommentId\":" + parentId + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return idOf(json);
    }

    private RequestPostProcessor principal(long userId) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private long insertUser(String name, String role) {
        String email = EMAIL_PREFIX + name + "@test.local";
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, ?, ?, 'ACTIVE', 1)",
                email, "Issue 51 " + name, role);
        return jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }

    private long insertRecipe(String title) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, description, instructions, dish_category,
                    vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id
                VALUES (?, ?, N'Mô tả', N'Rửa và chế biến nguyên liệu cho món ăn này.', 'SOUP',
                    'VEGAN', 'EASY', 2, 10, 15, 'PUBLISHED', SYSUTCDATETIME())
                """, Long.class, author, title);
    }

    private long insertComment(long userId, Long parentId, String content) {
        Integer parentDepth = parentId == null ? null
                : jdbcTemplate.queryForObject("SELECT depth FROM [COMMENT] WHERE comment_id = ?", Integer.class, parentId);
        return jdbcTemplate.queryForObject("""
                INSERT INTO [COMMENT] (recipe_id, user_id, parent_comment_id, parent_depth, content, depth, created_at, updated_at)
                OUTPUT INSERTED.comment_id
                VALUES (?, ?, ?, ?, ?, ?, SYSUTCDATETIME(), SYSUTCDATETIME())
                """, Long.class, recipe, userId, parentId, parentDepth, content, parentDepth == null ? 1 : parentDepth + 1);
    }

    private long idOf(String json) {
        return Long.parseLong(json.replaceAll(".*\"commentId\":(\\d+).*", "$1"));
    }

    private int notificationCount(long userId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM [NOTIFICATION] WHERE user_id = ?", Integer.class, userId);
    }

    private long notificationIdFor(long userId, long commentId) {
        return jdbcTemplate.queryForObject("SELECT notification_id FROM [NOTIFICATION] WHERE user_id = ? AND comment_id = ?",
                Long.class, userId, commentId);
    }

    private boolean isRead(long notificationId) {
        return jdbcTemplate.queryForObject("SELECT is_read FROM [NOTIFICATION] WHERE notification_id = ?", Boolean.class,
                notificationId);
    }
}
