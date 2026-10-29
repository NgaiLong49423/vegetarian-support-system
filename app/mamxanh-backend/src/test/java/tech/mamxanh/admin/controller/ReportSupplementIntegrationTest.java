package tech.mamxanh.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tech.mamxanh.AbstractIntegrationTest;

/** FR-30 supplements to an open report and the reporter's own report view, on SQL Server (AC-30.1, AC-30.2). */
class ReportSupplementIntegrationTest extends AbstractIntegrationTest {
    private static final String USERS = "issue35s-%@test.local";

    @Autowired private JdbcTemplate jdbcTemplate;

    private long reporterId;
    private long otherMemberId;
    private long authorId;
    private long recipeId;
    private long reportId;

    @BeforeEach
    void prepareFixtures() {
        cleanup();
        reporterId = insertUser("issue35s-reporter@test.local", "CUSTOMER");
        otherMemberId = insertUser("issue35s-other@test.local", "CUSTOMER");
        authorId = insertUser("issue35s-author@test.local", "EXPERT");
        recipeId = jdbcTemplate.queryForObject("""
                INSERT INTO [RECIPE_POST] (author_id, title, instructions, dish_category, vegetarian_type, difficulty,
                    servings, prep_time_min, cook_time_min, status, published_at)
                OUTPUT INSERTED.recipe_id VALUES (?, N'Bún riêu chay', N'Rửa và chế biến nguyên liệu cho món ăn này.',
                    'BRAISED', 'VEGAN', 'EASY', 2, 10, 15, 'PUBLISHED', SYSUTCDATETIME())
                """, Long.class, authorId);
        reportId = insertReport("OPEN");
    }

    @AfterEach
    void cleanupFixtures() {
        cleanup();
    }

    @Test
    void reporterAddsATimestampedSupplementToAnOpenReport() throws Exception {
        clock.advance(Duration.ofMinutes(15));

        supplement(reporterId, "  Ảnh thứ hai trong bài cho thấy có dùng mắm tôm.  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplementId").isNumber())
                .andExpect(jsonPath("$.content").value("Ảnh thứ hai trong bài cho thấy có dùng mắm tôm."))
                .andExpect(jsonPath("$.createdAt").value("2026-09-28T03:15:00"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM [REPORT_SUPPLEMENT] WHERE report_id = ?",
                Integer.class, reportId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT description FROM [REPORT] WHERE report_id = ?",
                String.class, reportId)).isEqualTo("Mô tả báo cáo gốc");
    }

    @Test
    void reporterReadsOwnReportWithSupplementsInTimeOrder() throws Exception {
        supplement(reporterId, "Bổ sung lần thứ nhất cho báo cáo.").andExpect(status().isCreated());
        clock.advance(Duration.ofHours(1));
        supplement(reporterId, "Bổ sung lần thứ hai cho báo cáo.").andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/me/reports/{reportId}", reportId).with(principal(reporterId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(reportId))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.reasonCode").value("OTHER"))
                .andExpect(jsonPath("$.description").value("Mô tả báo cáo gốc"))
                .andExpect(jsonPath("$.recipe.id").value(recipeId))
                .andExpect(jsonPath("$.recipe.title").value("Bún riêu chay"))
                .andExpect(jsonPath("$.supplementLimit").value(5))
                .andExpect(jsonPath("$.supplements[0].content").value("Bổ sung lần thứ nhất cho báo cáo."))
                .andExpect(jsonPath("$.supplements[0].createdAt").value("2026-09-28T03:00:00"))
                .andExpect(jsonPath("$.supplements[1].content").value("Bổ sung lần thứ hai cho báo cáo."))
                .andExpect(jsonPath("$.supplements[1].createdAt").value("2026-09-28T04:00:00"))
                .andExpect(jsonPath("$.reporterId").doesNotExist());
    }

    @Test
    void inReviewReportStillAcceptsSupplementsButResolvedReportDoesNot() throws Exception {
        jdbcTemplate.update("UPDATE [REPORT] SET status = 'IN_REVIEW' WHERE report_id = ?", reportId);
        supplement(reporterId, "Thông tin thêm khi đang xem xét.").andExpect(status().isCreated());

        jdbcTemplate.update("UPDATE [REPORT] SET status = 'RESOLVED' WHERE report_id = ?", reportId);
        supplement(reporterId, "Thông tin thêm sau khi đã giải quyết.")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_NOT_OPEN"));
        assertThat(supplementCount()).isEqualTo(1);
    }

    @Test
    void onlyTheReporterCanReadOrSupplementTheReport() throws Exception {
        mockMvc.perform(get("/api/v1/me/reports/{reportId}", reportId))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me/reports/{reportId}", reportId).with(principal(otherMemberId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        supplement(otherMemberId, "Người khác cố bổ sung vào báo cáo.")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        supplement(authorId, "Tác giả cố bổ sung vào báo cáo.")
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/me/reports/{reportId}", Long.MAX_VALUE).with(principal(reporterId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REPORT_NOT_FOUND"));
        assertThat(supplementCount()).isZero();
    }

    @Test
    void supplementContentMustHaveTenToFiveHundredCharactersAfterTrimming() throws Exception {
        for (String body : List.of("{}", "{\"content\":\"   \"}", "{\"content\":\"  ngắn  \"}",
                "{\"content\":\"%s\"}".formatted("x".repeat(501)))) {
            supplementRaw(reporterId, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("content"))
                    .andExpect(jsonPath("$.errors[0].message").value("Nhập nội dung bổ sung từ 10 đến 500 ký tự."));
        }
        assertThat(supplementCount()).isZero();
    }

    @Test
    void aReportTakesAtMostFiveSupplements() throws Exception {
        for (int index = 1; index <= 5; index++) {
            supplement(reporterId, "Bổ sung số %d cho báo cáo.".formatted(index)).andExpect(status().isCreated());
        }

        supplement(reporterId, "Bổ sung số 6 cho báo cáo.")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REPORT_SUPPLEMENT_LIMIT"));
        assertThat(supplementCount()).isEqualTo(5);
    }

    @Test
    void concurrentSupplementsNeverExceedTheLimit() throws Exception {
        for (int index = 1; index <= 4; index++) {
            jdbcTemplate.update("INSERT INTO [REPORT_SUPPLEMENT] (report_id, content) VALUES (?, ?)",
                    reportId, "Bổ sung có sẵn số " + index);
        }
        int requests = 3;
        CyclicBarrier start = new CyclicBarrier(requests);
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        try {
            List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                int number = i;
                futures.add(executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return supplement(reporterId, "Bổ sung đồng thời số " + number).andReturn().getResponse();
                }));
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<MockHttpServletResponse> future : futures) {
                statuses.add(future.get(60, TimeUnit.SECONDS).getStatus());
            }
            assertThat(statuses).containsExactlyInAnyOrder(201, 409, 409);
            assertThat(supplementCount()).isEqualTo(5);
        } finally {
            executor.shutdownNow();
        }
    }

    private ResultActions supplement(long userId, String content) throws Exception {
        return supplementRaw(userId, "{\"content\":\"%s\"}".formatted(content));
    }

    private ResultActions supplementRaw(long userId, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/me/reports/{reportId}/supplements", reportId)
                .with(principal(userId))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int supplementCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM [REPORT_SUPPLEMENT] WHERE report_id = ?",
                Integer.class, reportId);
    }

    private RequestPostProcessor principal(long userId) {
        String role = userId == authorId ? "ROLE_EXPERT" : "ROLE_CUSTOMER";
        return user(Long.toString(userId)).authorities(new SimpleGrantedAuthority(role));
    }

    private long insertReport(String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [REPORT] (reporter_id, recipe_id, reason_code, description, status)
                OUTPUT INSERTED.report_id VALUES (?, ?, 'OTHER', N'Mô tả báo cáo gốc', ?)
                """, Long.class, reporterId, recipeId, status);
    }

    private long insertUser(String email, String role) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO [USER] (email, display_name, role, account_status, email_verified)
                OUTPUT INSERTED.user_id VALUES (?, N'Issue 35 supplement test', ?, 'ACTIVE', 1)
                """, Long.class, email, role);
    }

    private void cleanup() {
        String users = "SELECT user_id FROM [USER] WHERE email LIKE '" + USERS + "'";
        jdbcTemplate.update("DELETE FROM [REPORT_SUPPLEMENT] WHERE report_id IN (SELECT report_id FROM [REPORT] WHERE reporter_id IN ("
                + users + "))");
        jdbcTemplate.update("DELETE FROM [REPORT] WHERE reporter_id IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [RECIPE_POST] WHERE author_id IN (" + users + ")");
        jdbcTemplate.update("DELETE FROM [USER] WHERE email LIKE '" + USERS + "'");
    }
}
