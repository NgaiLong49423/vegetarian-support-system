package tech.mamxanh.subscription.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.integration.ai.AiClient;

/** SQL Server expiry boundaries and real HTTP guards; Gemini is controlled, never billed by tests. */
class AiEntitlementIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired SubscriptionEntitlementService entitlements;
    @MockitoBean private AiClient aiClient;
    private long member;

    @BeforeEach void member() {
        String email = "fr10-" + UUID.randomUUID() + "@example.invalid";
        jdbc.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) VALUES (?, N'FR10 kiểm thử', 'EXPERT', 'ACTIVE', 1)", email);
        member = jdbc.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }

    @AfterEach void cleanup() {
        jdbc.update("DELETE FROM [SUBSCRIPTION] WHERE user_id = ?", member);
        jdbc.update("DELETE FROM [USER] WHERE user_id = ?", member);
    }

    @ParameterizedTest
    @CsvSource({"PLUS,ACTIVE,-1,1,true,false", "PRO,ACTIVE,-1,1,true,true",
            "PLUS,ACTIVE,0,1,true,false", "PRO,ACTIVE,0,1,true,true",
            "PRO,ACTIVE,-1,0,false,false", "PRO,ACTIVE,1,2,false,false",
            "PRO,EXPIRED,-1,1,false,false", "PRO,CANCELLED,-1,1,false,false"})
    void sqlUsesTierStatusAndExactValidityBoundaries(String tier, String status, int starts, int ends,
            boolean plusAllowed, boolean proAllowed) {
        subscribe(tier, status, starts, ends);
        check(() -> entitlements.requirePlusOrPro(member), plusAllowed);
        check(() -> entitlements.requirePro(member), proAllowed);
        verifyNoInteractions(aiClient);
    }

    @Test void freeAndPlusCannotCallPaidFeaturesAndReceiveTheCorrectUpgradeWithoutGemini() throws Exception {
        String assistance = """
                {"contentType":"DESCRIPTION","title":"Canh chay","dishCategory":"SOUP","vegetarianType":"VEGAN",
                 "servings":2,"ingredients":[{"ingredientId":1,"unitId":1,"quantity":100}]}
                """;
        forbidden("/api/v1/recipes/ai-assistance", assistance, "PLUS");
        forbidden("/api/v1/recipes/1/related/ai", "{}", "PLUS");
        for (int pass = 0; pass < 2; pass++) {
            forbidden("/api/v1/recipes/ai/weekly-menu", "{\"ingredientIds\":[1]}", "PRO");
            forbidden("/api/v1/meal-plans/ai-menu/previews", "{\"weekStartDate\":\"2026-09-28\",\"mode\":\"PROFILE\"}", "PRO");
            if (pass == 0) subscribe("PLUS", "ACTIVE", -1, 1);
        }
        verifyNoInteractions(aiClient);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM [SUBSCRIPTION] WHERE user_id=? AND tier='PLUS' AND status='ACTIVE'", Integer.class, member)).isEqualTo(1);
    }

    private void forbidden(String path, String body, String tier) throws Exception {
        mockMvc.perform(post(path).with(user(Long.toString(member)).roles("EXPERT"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("AI_SUBSCRIPTION_REQUIRED"))
                .andExpect(jsonPath("$.requiredTier").value(tier)).andExpect(jsonPath("$.upgradeUrl").value("/goi-ai"));
    }

    private void subscribe(String tier, String status, int starts, int ends) {
        var now = LocalDateTime.now(clock);
        jdbc.update("INSERT INTO [SUBSCRIPTION] (user_id,tier,status,starts_at,ends_at) VALUES (?,?,?,?,?)",
                member, tier, status, now.plusDays(starts), now.plusDays(ends));
    }

    private void check(Runnable call, boolean allowed) {
        if (allowed) assertThatCode(call::run).doesNotThrowAnyException();
        else assertThatThrownBy(call::run).isInstanceOf(AppException.class);
    }
}
