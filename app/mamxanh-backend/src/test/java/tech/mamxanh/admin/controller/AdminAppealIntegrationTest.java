package tech.mamxanh.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** The Administrator's side of an appeal on a hidden Recipe Post, on SQL Server (FR-28, BR-26, BR-27). */
class AdminAppealIntegrationTest extends AbstractAppealIntegrationTest {
    private static final String REASON = "Đã đối chiếu nguyên liệu, bài đúng nhãn thuần chay.";

    @Test
    void adminListsOpenAppealsNewestFirst() throws Exception {
        long olderRecipe = insertRecipe(authorId, "Gỏi cuốn rau củ", "HIDDEN");
        long older = insertHideReport(olderRecipe, "2026-11-19T08:00:00");
        jdbcTemplate.update("""
                UPDATE [REPORT] SET appeal_status = 'RESUBMITTED', appeal_message = ?, appealed_at = '2026-11-20T09:00:00',
                    appeal_resubmitted_at = '2026-11-20T10:00:00' WHERE report_id = ?
                """, MESSAGE, older);
        jdbcTemplate.update("""
                UPDATE [REPORT] SET appeal_status = 'PENDING', appeal_message = ?, appealed_at = '2026-11-21T09:00:00'
                WHERE report_id = ?
                """, MESSAGE, reportId);
        long closedRecipe = insertRecipe(authorId, "Đậu hũ sốt cà", "HIDDEN");
        long closed = insertHideReport(closedRecipe, "2026-11-19T08:00:00");
        setAppealStatus(closed, "KEPT_HIDDEN");

        mockMvc.perform(get("/api/v1/admin/appeals").param("status", "PENDING", "RESUBMITTED").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.appeal.reportId == %d)]".formatted(closed)).isEmpty())
                .andExpect(jsonPath("$.content[0].appeal.reportId").value(reportId))
                .andExpect(jsonPath("$.content[0].appeal.appealStatus").value("PENDING"))
                .andExpect(jsonPath("$.content[0].appeal.hideReason").value(HIDE_REASON))
                .andExpect(jsonPath("$.content[0].author.displayName").value("Chuyên gia Bình"))
                .andExpect(jsonPath("$.content[0].recipe").doesNotExist())
                .andExpect(jsonPath("$.content[1].appeal.reportId").value(older));

        mockMvc.perform(get("/api/v1/admin/appeals").param("status", "CLOSED").with(asAdmin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void adminDetailShowsHiddenRecipeContent() throws Exception {
        setAppealStatus(reportId, "PENDING");

        mockMvc.perform(get("/api/v1/admin/appeals/{id}", reportId).with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appeal.appealMessage").value(MESSAGE))
                .andExpect(jsonPath("$.recipe.id").value(recipeId))
                .andExpect(jsonPath("$.recipe.status").value("HIDDEN"))
                .andExpect(jsonPath("$.recipe.instructions").value("Rửa và chế biến nguyên liệu cho món ăn này."));

        long notAppealed = insertHideReport(insertRecipe(authorId, "Gỏi cuốn rau củ", "HIDDEN"), "2026-11-19T08:00:00");
        mockMvc.perform(get("/api/v1/admin/appeals/{id}", notAppealed).with(asAdmin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("APPEAL_NOT_FOUND"));
    }

    @Test
    void restorePublishesRecipeAndNotifiesAuthor() throws Exception {
        setAppealStatus(reportId, "PENDING");

        resolve(asAdmin(), "RESTORE", REASON)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appeal.appealStatus").value("RESTORED"))
                .andExpect(jsonPath("$.appeal.adminReason").value(REASON));

        assertThat(recipeStatus()).isEqualTo("PUBLISHED");
        assertThat(jdbcTemplate.queryForObject("SELECT appeal_handled_by FROM [REPORT] WHERE report_id = ?", Long.class, reportId))
                .isEqualTo(adminId);
        assertThat(notifications()).containsExactly("Bài công thức của bạn đã được mở lại");
        assertThat(notificationMessage()).contains("Canh nấm hạt sen").contains(REASON);
    }

    @Test
    void keepHiddenEndsAppeal() throws Exception {
        setAppealStatus(reportId, "PENDING");

        resolve(asAdmin(), "KEEP_HIDDEN", REASON).andExpect(status().isOk())
                .andExpect(jsonPath("$.appeal.appealStatus").value("KEPT_HIDDEN"));
        assertThat(recipeStatus()).isEqualTo("HIDDEN");
        assertThat(notifications()).containsExactly("Khiếu nại của bạn không được chấp nhận");

        resolve(asAdmin(), "RESTORE", REASON)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPEAL_STATE_INVALID"));
        assertThat(recipeStatus()).isEqualTo("HIDDEN");
    }

    @Test
    void allowEditThenResubmitThenRestore() throws Exception {
        setAppealStatus(reportId, "PENDING");

        resolve(asAdmin(), "ALLOW_EDIT", REASON).andExpect(status().isOk())
                .andExpect(jsonPath("$.appeal.appealStatus").value("EDIT_ALLOWED"));
        assertThat(recipeStatus()).isEqualTo("HIDDEN");
        assertThat(notifications()).containsExactly("Bạn được phép sửa bài công thức đang bị ẩn");

        mockMvc.perform(post("/api/v1/recipes/{id}/appeal/resubmit", recipeId).with(asAuthor()))
                .andExpect(status().isOk());
        resolve(asAdmin(), "ALLOW_EDIT", REASON)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPEAL_STATE_INVALID"));
        resolve(asAdmin(), "RESTORE", REASON).andExpect(status().isOk());
        assertThat(recipeStatus()).isEqualTo("PUBLISHED");
    }

    @Test
    void restoreFailsWhenAuthorDeletedRecipe() throws Exception {
        setAppealStatus(reportId, "PENDING");
        jdbcTemplate.update("UPDATE [RECIPE_POST] SET status = 'DELETED' WHERE recipe_id = ?", recipeId);

        resolve(asAdmin(), "RESTORE", REASON)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("APPEAL_STATE_INVALID"));
        assertThat(recipeStatus()).isEqualTo("DELETED");
        assertThat(appealStatus(reportId)).isEqualTo("PENDING");
        assertThat(notifications()).isEmpty();
    }

    @Test
    void newerHideDecisionSupersedesOlderAppeal() throws Exception {
        setAppealStatus(reportId, "PENDING");
        long newer = insertHideReport(recipeId, "2026-11-22T08:00:00");

        for (String decision : List.of("RESTORE", "KEEP_HIDDEN")) {
            resolve(asAdmin(), decision, REASON)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("APPEAL_STATE_INVALID"));
        }
        assertThat(recipeStatus()).isEqualTo("HIDDEN");
        assertThat(notifications()).isEmpty();
        mockMvc.perform(get("/api/v1/admin/appeals").param("status", "PENDING", "EDIT_ALLOWED", "RESUBMITTED").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.appeal.reportId == %d)]".formatted(reportId)).isEmpty());
        mockMvc.perform(get("/api/v1/recipes/{id}/appeal", recipeId).with(asAuthor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(newer))
                .andExpect(jsonPath("$.appealStatus").doesNotExist());
    }

    @Test
    void decisionsRefusedWhenAuthorDeletedRecipe() throws Exception {
        setAppealStatus(reportId, "PENDING");
        jdbcTemplate.update("UPDATE [RECIPE_POST] SET status = 'DELETED' WHERE recipe_id = ?", recipeId);

        for (String decision : List.of("KEEP_HIDDEN", "ALLOW_EDIT")) {
            resolve(asAdmin(), decision, REASON)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("APPEAL_STATE_INVALID"));
        }
        assertThat(appealStatus(reportId)).isEqualTo("PENDING");
        assertThat(notifications()).isEmpty();
        mockMvc.perform(get("/api/v1/admin/appeals").param("status", "PENDING").with(asAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.appeal.reportId == %d)]".formatted(reportId)).isEmpty());
    }

    @Test
    void concurrentResolveOnlyOneWins() throws Exception {
        setAppealStatus(reportId, "PENDING");
        long otherAdmin = insertUser("appeal-admin2@test.local", "Quản trị viên Lan", "ADMIN");
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
            for (long admin : List.of(adminId, otherAdmin)) {
                futures.add(executor.submit(() -> {
                    start.await(10, TimeUnit.SECONDS);
                    return resolve(principal(admin, "ROLE_ADMIN"), "KEEP_HIDDEN", REASON).andReturn().getResponse();
                }));
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<MockHttpServletResponse> future : futures) {
                statuses.add(future.get(60, TimeUnit.SECONDS).getStatus());
            }
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
            assertThat(notifications()).hasSize(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void nonAdminRejected() throws Exception {
        setAppealStatus(reportId, "PENDING");

        resolve(asAuthor(), "RESTORE", REASON).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(get("/api/v1/admin/appeals").with(principal(customerId, "ROLE_CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/appeals"))
                .andExpect(status().isUnauthorized());
        assertThat(recipeStatus()).isEqualTo("HIDDEN");
    }

    @Test
    void decisionAndReasonValidated() throws Exception {
        setAppealStatus(reportId, "PENDING");

        resolve(asAdmin(), "RESTORE", "quá ngắn").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        resolve(asAdmin(), "DELETE", REASON).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(appealStatus(reportId)).isEqualTo("PENDING");
    }

    private ResultActions resolve(RequestPostProcessor who, String decision, String reason) throws Exception {
        String body = "{\"decision\":\"" + decision + "\",\"reason\":\"" + reason + "\"}";
        return mockMvc.perform(post("/api/v1/admin/appeals/{id}/resolve", reportId).with(who)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private List<String> notifications() {
        return jdbcTemplate.queryForList("SELECT title FROM [NOTIFICATION] WHERE user_id = ? AND report_id = ?",
                String.class, authorId, reportId);
    }

    private String notificationMessage() {
        return jdbcTemplate.queryForObject("SELECT message FROM [NOTIFICATION] WHERE user_id = ? AND report_id = ?",
                String.class, authorId, reportId);
    }
}
