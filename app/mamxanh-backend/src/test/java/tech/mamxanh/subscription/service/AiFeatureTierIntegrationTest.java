package tech.mamxanh.subscription.service;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.integration.ai.AiClient;

/** Feature access is shared across tiers; Member quota remains tier-specific in AiTokenQuotaService. */
@org.springframework.test.context.TestPropertySource(properties = "mamxanh.ai.orchestrator=LEGACY")
class AiFeatureTierIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @MockitoBean private AiClient aiClient;
    private long member;

    @org.junit.jupiter.api.BeforeEach
    void member() {
        String email = "ai-tier-" + UUID.randomUUID() + "@example.invalid";
        jdbc.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, N'FR10 kiểm thử', 'EXPERT', 'ACTIVE', 1)", email);
        member = jdbc.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }

    @org.junit.jupiter.api.AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM [SUBSCRIPTION] WHERE user_id = ?", member);
        jdbc.update("DELETE FROM [USER] WHERE user_id = ?", member);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FREE", "PLUS", "PRO"})
    void weeklyMenuAndExpertAuthoringWorkflowsAreAvailableAcrossTiers(String tier) throws Exception {
        setTier(tier);

        mockMvc.perform(post("/api/v1/recipes/ai/chat").with(user(Long.toString(member)).roles("EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contextType\":\"GENERAL\",\"question\":\"Lập thực đơn tuần 7 ngày theo dinh dưỡng cá nhân\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowUrl").value("/ke-hoach?view=for-you"));

        mockMvc.perform(post("/api/v1/recipes/ai/chat").with(user(Long.toString(member)).roles("EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contextType\":\"GENERAL\",\"question\":\"Viết mô tả cho bài đăng công thức\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowUrl").value("/dang-cong-thuc"));
    }

    private void setTier(String tier) {
        if ("FREE".equals(tier)) return;
        jdbc.update("INSERT INTO [SUBSCRIPTION] (user_id,tier,status,starts_at,ends_at,token_quota) "
                        + "VALUES (?,?,'ACTIVE',DATEADD(day,-1,SYSUTCDATETIME()),DATEADD(day,30,SYSUTCDATETIME()),600000)",
                member, tier);
    }
}
