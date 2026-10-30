package tech.mamxanh.subscription.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.subscription.service.AiTokenQuotaService;

class AiTokenQuotaIntegrationTest extends AbstractIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AiTokenQuotaService quota;
    private long firstMember;
    private long secondMember;

    @BeforeEach
    void createMembers() {
        firstMember = createMember();
        secondMember = createMember();
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM AI_TOKEN_USAGE_LEDGER WHERE user_id IN (?, ?)", firstMember, secondMember);
        jdbc.update("DELETE FROM [USER] WHERE user_id IN (?, ?)", firstMember, secondMember);
    }

    @Test
    void usageApiReturnsOnlyTheAuthenticatedMembersCurrentPeriodAndLedgerIsIdempotent() throws Exception {
        var event = new AiUsageEvent(UUID.randomUUID(), firstMember, null, "FR-51", clock.instant(),
                new AiTokenUsage("gemini-3.8-flash", "STANDARD", 100, 200, 310, 10, 0, 0));
        quota.record(event);
        quota.record(event);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM AI_TOKEN_USAGE_LEDGER WHERE event_id = ?",
                Integer.class, event.eventId().toString())).isEqualTo(1);

        mockMvc.perform(get("/api/v1/subscriptions/my-ai-usage").with(user(Long.toString(firstMember)).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tier").value("FREE"))
                .andExpect(jsonPath("$.consumedTokens").value(310))
                .andExpect(jsonPath("$.quotaTokens").value(50_000))
                .andExpect(jsonPath("$.remainingTokens").value(49_690));
        mockMvc.perform(get("/api/v1/subscriptions/my-ai-usage").with(user(Long.toString(secondMember)).roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumedTokens").value(0));
    }

    private long createMember() {
        String email = "fr63-" + UUID.randomUUID() + "@example.invalid";
        jdbc.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) "
                + "VALUES (?, N'FR63 kiểm thử', 'CUSTOMER', 'ACTIVE', 1)", email);
        return jdbc.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }
}
