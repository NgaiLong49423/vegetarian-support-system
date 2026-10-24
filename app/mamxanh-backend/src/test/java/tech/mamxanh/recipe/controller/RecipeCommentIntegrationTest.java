package tech.mamxanh.recipe.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import tech.mamxanh.AbstractIntegrationTest;

/** FR-46 threaded comments (AC-46.1–AC-46.4, BR-66) against SQL Server. */
class RecipeCommentIntegrationTest extends AbstractIntegrationTest {
    private static final String EMAIL_PREFIX = "issue48-comment-";

    @Autowired private JdbcTemplate jdbcTemplate;

    private long expertAuthor;
    private long memberA;
    private long memberB;
    private long lockedMember;
    private long admin;
    private long recipe;
    private long otherRecipe;
    private long hiddenRecipe;

    @BeforeEach
    void prepareFixtures() {
        removeFixtures();
        expertAuthor = insertUser("author", "EXPERT", "ACTIVE");
        memberA = insertUser("a", "CUSTOMER", "ACTIVE");
        memberB = insertUser("b", "EXPERT", "ACTIVE");
        lockedMember = insertUser("locked", "CUSTOMER", "LOCKED");
        admin = insertUser("admin", "ADMIN", "ACTIVE");
        recipe = insertRecipe("Canh chua chay", "PUBLISHED");
        otherRecipe = insertRecipe("Đậu hũ sốt cà", "PUBLISHED");
        hiddenRecipe = insertRecipe("Món đang ẩn", "HIDDEN");
    }

    /** PasswordLoginIntegrationTest deletes every USER row, so no COMMENT row may outlive a test. */
    @AfterEach
    void removeFixtures() {
        // Replies create FR-49 notifications that reference the comment (FK_NOTIFICATION_COMMENT).
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
    void memberCreatesARootCommentAndAnotherMemberRepliesInTheSameThread() throws Exception {
        String created = mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipe).with(principal(memberA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  Món này nấu bao lâu ạ?  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.depth").value(1))
                .andExpect(jsonPath("$.parentCommentId").value(nullValue()))
                .andExpect(jsonPath("$.content").value("Món này nấu bao lâu ạ?"))
                .andExpect(jsonPath("$.author.userId").value(memberA))
                .andExpect(jsonPath("$.author.displayName").value("Issue 48 a"))
                .andExpect(jsonPath("$.deleted").value(false))
                .andExpect(jsonPath("$.edited").value(false))
                .andReturn().getResponse().getContentAsString();
        long rootId = idOf(created);

        clock.advance(Duration.ofMinutes(1));
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipe).with(principal(memberB, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Khoảng 20 phút nhé.\",\"parentCommentId\":" + rootId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.depth").value(2))
                .andExpect(jsonPath("$.parentCommentId").value(rootId));

        mockMvc.perform(get("/api/v1/recipes/{recipeId}/comments", recipe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").value(2))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].commentId").value(rootId))
                .andExpect(jsonPath("$.content[0].replies.length()").value(1))
                .andExpect(jsonPath("$.content[0].replies[0].content").value("Khoảng 20 phút nhé."))
                .andExpect(jsonPath("$.content[0].replies[0].author.userId").value(memberB))
                .andExpect(jsonPath("$.content[0].replies[0].replies.length()").value(0));
    }

    @Test
    void aReplyToALevelFiveCommentStaysAtLevelFiveUnderTheSameParent() throws Exception {
        long level1 = insertComment(recipe, memberA, null, "Cấp 1", "2026-09-28T01:00:00");
        long level2 = insertComment(recipe, memberB, level1, "Cấp 2", "2026-09-28T01:01:00");
        long level3 = insertComment(recipe, memberA, level2, "Cấp 3", "2026-09-28T01:02:00");
        long level4 = insertComment(recipe, memberB, level3, "Cấp 4", "2026-09-28T01:03:00");
        long level5 = insertComment(recipe, memberA, level4, "Cấp 5", "2026-09-28T01:04:00");

        mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipe).with(principal(memberB, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"@Issue 48 a đồng ý\",\"parentCommentId\":" + level5 + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.depth").value(5))
                .andExpect(jsonPath("$.parentCommentId").value(level4));

        assertThat(jdbcTemplate.queryForObject("SELECT MAX(depth) FROM [COMMENT] WHERE recipe_id = ?", Integer.class, recipe))
                .isEqualTo(5);
        mockMvc.perform(get("/api/v1/recipes/{recipeId}/comments", recipe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].replies[0].replies[0].replies[0].replies.length()").value(2))
                .andExpect(jsonPath("$.content[0].replies[0].replies[0].replies[0].replies[1].content").value("@Issue 48 a đồng ý"));
    }

    @Test
    void aReplyMustTargetALiveCommentOfTheSameRecipe() throws Exception {
        long elsewhere = insertComment(otherRecipe, memberA, null, "Ở bài khác", "2026-09-28T01:00:00");
        long removed = insertComment(recipe, memberA, null, "Đã xóa", "2026-09-28T01:00:00");
        jdbcTemplate.update("UPDATE [COMMENT] SET is_deleted = 1 WHERE comment_id = ?", removed);

        for (long parent : new long[] {elsewhere, removed, Long.MAX_VALUE}) {
            mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipe).with(principal(memberB, "ROLE_EXPERT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"Trả lời\",\"parentCommentId\":" + parent + "}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
        }
        assertThat(commentCount(recipe)).isEqualTo(1);
    }

    @Test
    void contentMustHaveOneToOneThousandCharactersAfterTrimming() throws Exception {
        for (String content : new String[] {"   ", "x".repeat(1001)}) {
            mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipe).with(principal(memberA))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"" + content + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("content"));
        }
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipe).with(principal(memberA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("content"));
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipe).with(principal(memberA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\" " + "y".repeat(1000) + " \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("y".repeat(1000)));
    }

    @Test
    void markupIsStoredAndReturnedAsPlainText() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipe).with(principal(memberA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"<script>alert(1)</script>\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("<script>alert(1)</script>"));
    }

    @Test
    void onlyTheOwnerMayEditAndTheEditIsMarked() throws Exception {
        long comment = insertComment(recipe, memberA, null, "Bản gốc", "2026-09-28T01:00:00");

        mockMvc.perform(put("/api/v1/comments/{commentId}", comment).with(principal(memberB, "ROLE_EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Sửa trộm\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT_EDIT_NOT_ALLOWED"));
        assertThat(contentOf(comment)).isEqualTo("Bản gốc");

        clock.advance(Duration.ofMinutes(5));
        mockMvc.perform(put("/api/v1/comments/{commentId}", comment).with(principal(memberA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  Bản đã sửa  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Bản đã sửa"))
                .andExpect(jsonPath("$.edited").value(true));
        mockMvc.perform(get("/api/v1/recipes/{recipeId}/comments", recipe))
                .andExpect(jsonPath("$.content[0].content").value("Bản đã sửa"))
                .andExpect(jsonPath("$.content[0].edited").value(true));

        mockMvc.perform(put("/api/v1/comments/{commentId}", comment).with(principal(memberA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("content"));
    }

    @Test
    void onlyTheOwnerMayDeleteAComment() throws Exception {
        long comment = insertComment(recipe, memberA, null, "Của A", "2026-09-28T01:00:00");

        mockMvc.perform(delete("/api/v1/comments/{commentId}", comment).with(principal(memberB, "ROLE_EXPERT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMENT_EDIT_NOT_ALLOWED"));
        assertThat(isDeleted(comment)).isFalse();

        mockMvc.perform(delete("/api/v1/comments/{commentId}", comment).with(principal(memberA)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/comments/{commentId}", comment).with(principal(memberA)))
                .andExpect(status().isNoContent());
        assertThat(isDeleted(comment)).isTrue();
        mockMvc.perform(put("/api/v1/comments/{commentId}", comment).with(principal(memberA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hồi sinh\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void deletingAParentWithRepliesLeavesATombstoneAndKeepsTheReplies() throws Exception {
        long parent = insertComment(recipe, memberA, null, "Cha", "2026-09-28T01:00:00");
        insertComment(recipe, memberB, parent, "Con 1", "2026-09-28T01:01:00");
        insertComment(recipe, expertAuthor, parent, "Con 2", "2026-09-28T01:02:00");

        mockMvc.perform(delete("/api/v1/comments/{commentId}", parent).with(principal(memberA)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/recipes/{recipeId}/comments", recipe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").value(2))
                .andExpect(jsonPath("$.content[0].commentId").value(parent))
                .andExpect(jsonPath("$.content[0].deleted").value(true))
                .andExpect(jsonPath("$.content[0].content").value(nullValue()))
                .andExpect(jsonPath("$.content[0].author").value(nullValue()))
                .andExpect(jsonPath("$.content[0].replies.length()").value(2))
                .andExpect(jsonPath("$.content[0].replies[0].content").value("Con 1"))
                .andExpect(jsonPath("$.content[0].replies[1].content").value("Con 2"));
    }

    @Test
    void deletedCommentsWithoutLiveRepliesDisappearFromTheList() throws Exception {
        long keptRoot = insertComment(recipe, memberA, null, "Gốc còn", "2026-09-28T01:00:00");
        long leaf = insertComment(recipe, memberB, keptRoot, "Lá", "2026-09-28T01:01:00");
        long goneRoot = insertComment(recipe, memberA, null, "Gốc mất", "2026-09-28T01:02:00");
        long goneReply = insertComment(recipe, memberB, goneRoot, "Con mất", "2026-09-28T01:03:00");

        mockMvc.perform(delete("/api/v1/comments/{commentId}", leaf).with(principal(memberB, "ROLE_EXPERT")))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/comments/{commentId}", goneRoot).with(principal(memberA)))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/comments/{commentId}", goneReply).with(principal(memberB, "ROLE_EXPERT")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/recipes/{recipeId}/comments", recipe))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").value(1))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].commentId").value(keptRoot))
                .andExpect(jsonPath("$.content[0].replies.length()").value(0));
    }

    @Test
    void rootThreadsAreNewestFirstWithRepliesInConversationOrderAndBoundedPaging() throws Exception {
        long older = insertComment(recipe, memberA, null, "Cũ", "2026-09-28T01:00:00");
        long newer = insertComment(recipe, memberB, null, "Mới", "2026-09-28T02:00:00");
        insertComment(recipe, memberB, older, "Trả lời sau", "2026-09-28T03:00:00");
        insertComment(recipe, expertAuthor, older, "Trả lời trước", "2026-09-28T02:30:00");

        mockMvc.perform(get("/api/v1/recipes/{recipeId}/comments", recipe).param("page", "0").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].commentId").value(newer));
        mockMvc.perform(get("/api/v1/recipes/{recipeId}/comments", recipe).param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].commentId").value(older))
                .andExpect(jsonPath("$.content[0].replies[0].content").value("Trả lời trước"))
                .andExpect(jsonPath("$.content[0].replies[1].content").value("Trả lời sau"));
        mockMvc.perform(get("/api/v1/recipes/{recipeId}/comments", recipe).param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mockMvc.perform(get("/api/v1/recipes/{recipeId}/comments", recipe).param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void onlyAnActiveMemberMayWriteComments() throws Exception {
        long comment = insertComment(recipe, memberA, null, "Của A", "2026-09-28T01:00:00");
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipe)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Khách\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/comments/{commentId}", comment)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Khách\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/comments/{commentId}", comment))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipe).with(principal(admin, "ROLE_ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Admin\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_ACCESS_REQUIRED"));
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", recipe).with(principal(lockedMember))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Bị khóa\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_ACCESS_REQUIRED"));
        assertThat(commentCount(recipe)).isEqualTo(1);
    }

    @Test
    void commentsExistOnlyOnPublishedRecipes() throws Exception {
        long hiddenComment = insertComment(hiddenRecipe, memberA, null, "Trên bài ẩn", "2026-09-28T01:00:00");

        mockMvc.perform(get("/api/v1/recipes/{recipeId}/comments", hiddenRecipe))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/recipes/{recipeId}/comments", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/comments", hiddenRecipe).with(principal(memberA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Thử\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
        mockMvc.perform(put("/api/v1/comments/{commentId}", hiddenComment).with(principal(memberA))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"Sửa\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"));
    }

    @Test
    void generatedOpenApiMarksOnlyTheCommentListAsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}/comments'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}/comments'].get.security").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/v1/recipes/{recipeId}/comments'].post.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/comments/{commentId}'].put.security[0].bearerAuth").isArray())
                .andExpect(jsonPath("$.paths['/api/v1/comments/{commentId}'].delete.security[0].bearerAuth").isArray());
    }

    private RequestPostProcessor principal(long userId) {
        return principal(userId, "ROLE_CUSTOMER");
    }

    private RequestPostProcessor principal(long userId, String authority) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority(authority));
    }

    private long insertUser(String name, String role, String accountStatus) {
        String email = EMAIL_PREFIX + name + "@test.local";
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, ?, ?, ?, 1)",
                email, "Issue 48 " + name, role, accountStatus);
        return jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }

    private long insertRecipe(String title, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, description, instructions, dish_category,
                    vegetarian_type, difficulty, servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id
                VALUES (?, ?, N'Mô tả', N'Rửa và chế biến nguyên liệu cho món ăn này.', 'SOUP',
                    'VEGAN', 'EASY', 2, 10, 15, ?, SYSUTCDATETIME())
                """, Long.class, expertAuthor, title, status);
    }

    private long insertComment(long recipeId, long userId, Long parentId, String content, String createdAt) {
        Integer parentDepth = parentId == null ? null
                : jdbcTemplate.queryForObject("SELECT depth FROM [COMMENT] WHERE comment_id = ?", Integer.class, parentId);
        java.sql.Timestamp at = java.sql.Timestamp.valueOf(createdAt.replace('T', ' '));
        return jdbcTemplate.queryForObject("""
                INSERT INTO [COMMENT] (recipe_id, user_id, parent_comment_id, parent_depth, content, depth, created_at, updated_at)
                OUTPUT INSERTED.comment_id
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, Long.class, recipeId, userId, parentId, parentDepth, content,
                parentDepth == null ? 1 : parentDepth + 1, at, at);
    }

    private long idOf(String json) {
        return Long.parseLong(json.replaceAll(".*\"commentId\":(\\d+).*", "$1"));
    }

    private int commentCount(long recipeId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM [COMMENT] WHERE recipe_id = ?", Integer.class, recipeId);
    }

    private String contentOf(long commentId) {
        return jdbcTemplate.queryForObject("SELECT content FROM [COMMENT] WHERE comment_id = ?", String.class, commentId);
    }

    private boolean isDeleted(long commentId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject("SELECT is_deleted FROM [COMMENT] WHERE comment_id = ?",
                Boolean.class, commentId));
    }
}
