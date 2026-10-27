package tech.mamxanh.admin.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import tech.mamxanh.AbstractIntegrationTest;

/**
 * BUG-059 (Issue #159): the ingredient reference date may be today in Vietnam time, judged by the
 * application clock, even while the server clock in UTC is still on the previous day.
 */
class IngredientReferenceDateIntegrationTest extends AbstractIntegrationTest {
    private static final String NAMES = "Kiểm thử 159 %";

    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM [INGREDIENT] WHERE name LIKE ?", NAMES);
    }

    @Test
    void halfPastMidnightInVietnamAcceptsTheNewVietnamDateWhileUtcIsStillTheDayBefore() throws Exception {
        clock.advance(Duration.ofHours(14).plusMinutes(30)); // 2026-09-28T17:30Z = 2026-09-29 00:30 in Vietnam

        create("Kiểm thử 159 nửa đêm", "2026-09-29")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.referenceDate").value("2026-09-29"));
    }

    @Test
    void aDateAfterTodayInVietnamIsRejectedWithAVietnameseMessage() throws Exception {
        clock.advance(Duration.ofHours(14).plusMinutes(30)); // 2026-09-29 00:30 in Vietnam

        create("Kiểm thử 159 ngày mai", "2026-09-30")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("referenceDate"))
                .andExpect(jsonPath("$.errors[0].message").value("Ngày tham khảo không được sau hôm nay (giờ Việt Nam)."));
    }

    @Test
    void beforeMidnightInVietnamTheNextDayIsStillInTheFuture() throws Exception {
        // MutableClock starts at 2026-09-28T03:00Z = 2026-09-28 10:00 in Vietnam.
        create("Kiểm thử 159 trước nửa đêm", "2026-09-29")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("referenceDate"));
        create("Kiểm thử 159 hôm nay", "2026-09-28")
                .andExpect(status().isCreated());
    }

    @Test
    void theValidationClockMovesWithTheApplicationClockInsteadOfKeepingItsFirstReading() throws Exception {
        create("Kiểm thử 159 lúc 10 giờ", "2026-09-29").andExpect(status().isBadRequest());

        clock.advance(Duration.ofHours(14).plusMinutes(30)); // past midnight in Vietnam

        create("Kiểm thử 159 lúc 0 giờ 30", "2026-09-29").andExpect(status().isCreated());
    }

    private ResultActions create(String name, String referenceDate) throws Exception {
        String body = """
                {"name":"%s","ingredientGroup":"Trái cây","sourceName":"USDA FoodData Central","referenceDate":"%s"}
                """.formatted(name, referenceDate);
        return mockMvc.perform(post("/api/v1/admin/ingredients").with(user("1").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
