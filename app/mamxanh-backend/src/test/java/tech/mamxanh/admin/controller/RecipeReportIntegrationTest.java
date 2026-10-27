package tech.mamxanh.admin.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tech.mamxanh.AbstractIntegrationTest;

class RecipeReportIntegrationTest extends AbstractIntegrationTest {
    @Autowired private JdbcTemplate jdbcTemplate;

    private long memberId;
    private long authorId;
    private long recipeId;

    @BeforeEach
    void prepareFixtures() {
        jdbcTemplate.update("DELETE FROM [REPORT] WHERE reporter_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'issue31-%@test.local')");
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (SELECT user_id FROM [USER] WHERE email LIKE 'issue31-%@test.local')");
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE 'issue31-%@test.local'");
        memberId = insertUser("issue31-member@test.local", "CUSTOMER", "ACTIVE");
        authorId = insertUser("issue31-author@test.local", "EXPERT", "ACTIVE");
        recipeId = insertRecipe(authorId, "Công thức công khai", "PUBLISHED");
    }

    @AfterEach
    void cleanupFixtures() {
        if (recipeId > 0) {
            jdbcTemplate.update("DELETE FROM [REPORT] WHERE recipe_id = ?", recipeId);
            jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE recipe_id = ?", recipeId);
        }
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE 'issue31-%@test.local'");
    }

    @Test
    void authenticatedMemberCreatesPrivateOpenReportWithoutHidingRecipe() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/reports", recipeId)
                        .with(principal(memberId, "ROLE_CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reasonCode":"RECIPE_INFO_OR_DIET_LABEL","description":"  Nhãn chế độ ăn chưa đúng  "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.message").value("Báo cáo đã được tiếp nhận."))
                .andExpect(jsonPath("$.reporterId").doesNotExist());

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [REPORT] WHERE reporter_id = ? AND recipe_id = ? AND status = 'OPEN'",
                Integer.class, memberId, recipeId)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT reason_code FROM [REPORT] WHERE reporter_id = ? AND recipe_id = ?",
                String.class, memberId, recipeId)).isEqualTo("RECIPE_INFO_OR_DIET_LABEL");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT description FROM [REPORT] WHERE reporter_id = ? AND recipe_id = ?",
                String.class, memberId, recipeId)).isEqualTo("Nhãn chế độ ăn chưa đúng");
        org.assertj.core.api.Assertions.assertThat(recipeStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void authenticatedMemberCanSubmitOtherReasonWithTrimmedDescription() throws Exception {
        submit("{\"reasonCode\":\"OTHER\",\"description\":\"  Mô tả hợp lệ tối thiểu  \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));

        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT reason_code FROM [REPORT] WHERE reporter_id = ? AND recipe_id = ?",
                String.class, memberId, recipeId)).isEqualTo("OTHER");
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT description FROM [REPORT] WHERE reporter_id = ? AND recipe_id = ?",
                String.class, memberId, recipeId)).isEqualTo("Mô tả hợp lệ tối thiểu");
    }

    @Test
    void guestCannotSubmitReport() throws Exception {
        mockMvc.perform(post("/api/v1/recipes/{recipeId}/reports", recipeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reasonCode\":\"OTHER\",\"description\":\"Mô tả hợp lệ tối thiểu\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        assertNoReport();
    }

    @Test
    void rejectsUnknownReasonAndInvalidOtherDescriptionWithoutPersisting() throws Exception {
        submit("{}").andExpect(status().isBadRequest());
        submit("{\"reasonCode\":\"NON_VEGAN\",\"description\":\"\"}")
                .andExpect(status().isBadRequest());
        submit("{\"reasonCode\":\"OTHER\"}").andExpect(status().isBadRequest());
        submit("{\"reasonCode\":\"OTHER\",\"description\":\"   \"}")
                .andExpect(status().isBadRequest());
        submit("{\"reasonCode\":\"OTHER\",\"description\":\"ngắn\"}")
                .andExpect(status().isBadRequest());
        submit("{\"reasonCode\":\"OTHER\",\"description\":\"%s\"}".formatted("x".repeat(501)))
                .andExpect(status().isBadRequest());
        assertNoReport();
    }

    @Test
    void rejectsDuplicateOpenReport() throws Exception {
        assertDuplicateBlocked(insertReport("OPEN"));
    }

    @Test
    void rejectsDuplicateInReviewReport() throws Exception {
        assertDuplicateBlocked(insertReport("IN_REVIEW"));
    }

    @Test
    void concurrentDuplicateSubmissionsCreateOneReportAndPointOthersToIt() throws Exception {
        int requests = 4;
        CyclicBarrier start = new CyclicBarrier(requests);
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        try {
            List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                futures.add(executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return submit("{\"reasonCode\":\"SPAM_ADVERTISING\"}").andReturn().getResponse();
                }));
            }
            List<MockHttpServletResponse> responses = new ArrayList<>();
            for (Future<MockHttpServletResponse> future : futures) {
                responses.add(future.get(60, TimeUnit.SECONDS));
            }

            long createdId = jdbcTemplate.queryForObject(
                    "SELECT report_id FROM [REPORT] WHERE reporter_id = ? AND recipe_id = ?", Long.class, memberId, recipeId);
            org.assertj.core.api.Assertions.assertThat(responses).extracting(MockHttpServletResponse::getStatus)
                    .containsExactlyInAnyOrder(201, 409, 409, 409);
            for (MockHttpServletResponse response : responses) {
                if (response.getStatus() == 409) {
                    org.assertj.core.api.Assertions.assertThat(response.getContentAsString(StandardCharsets.UTF_8))
                            .contains("\"code\":\"REPORT_ALREADY_OPEN\"")
                            .contains("\"reportId\":" + createdId);
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void reReportAfterResolvedRequiresNewDescriptionForAnyReason() throws Exception {
        insertReport("RESOLVED");

        submit("{\"reasonCode\":\"SPAM_ADVERTISING\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.field").value("description"));
        submit("{\"reasonCode\":\"SPAM_ADVERTISING\",\"description\":\"  ngắn  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.field").value("description"));
        assertOpenReportCount(0);

        submit("{\"reasonCode\":\"SPAM_ADVERTISING\",\"description\":\"Bài lại chèn liên kết quảng cáo mới\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));
        assertOpenReportCount(1);
    }

    @Test
    void firstReportWithNonOtherReasonStillAllowsEmptyDescription() throws Exception {
        submit("{\"reasonCode\":\"SPAM_ADVERTISING\"}")
                .andExpect(status().isCreated());
        assertOpenReportCount(1);
    }

    private org.springframework.test.web.servlet.ResultActions submit(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/recipes/{recipeId}/reports", recipeId)
                .with(principal(memberId, "ROLE_CUSTOMER"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void assertDuplicateBlocked(long existingReportId) throws Exception {
        submit("{\"reasonCode\":\"OTHER\",\"description\":\"Mô tả hợp lệ tối thiểu\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_ALREADY_OPEN"))
                .andExpect(jsonPath("$.reportId").value(existingReportId));
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [REPORT] WHERE reporter_id = ? AND recipe_id = ?", Integer.class, memberId, recipeId))
                .isEqualTo(1);
    }

    private long insertReport(String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [REPORT] (reporter_id, recipe_id, reason_code, description, status)
                OUTPUT INSERTED.report_id VALUES (?, ?, ?, ?, ?)
                """, Long.class, memberId, recipeId, "OTHER", "Mô tả báo cáo hiện có", status);
    }

    private void assertOpenReportCount(int expected) {
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [REPORT] WHERE reporter_id = ? AND recipe_id = ? AND status = 'OPEN'",
                Integer.class, memberId, recipeId)).isEqualTo(expected);
    }

    private void assertNoReport() {
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM [REPORT] WHERE reporter_id = ? AND recipe_id = ?", Integer.class, memberId, recipeId))
                .isZero();
    }

    private String recipeStatus() {
        return jdbcTemplate.queryForObject("SELECT status FROM [RECIPE_POST] WHERE recipe_id = ?", String.class, recipeId);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor principal(long userId, String role) {
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority(role));
    }

    private long insertUser(String email, String role, String status) {
        jdbcTemplate.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, ?, ?, ?, 1)",
                email, "Issue 31 test", role, status);
        return jdbcTemplate.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }

    private long insertRecipe(long authorId, String title, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type, difficulty,
                    servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id VALUES (?, ?, N'Rửa và chế biến nguyên liệu cho món ăn này.',
                    'BRAISED', 'VEGAN', 'EASY', 2, 10, 15, ?, SYSUTCDATETIME())
                """, Long.class, authorId, title, status);
    }
}
