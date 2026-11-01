package tech.mamxanh.subscription.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.integration.ai.AiTokenUsage;
import tech.mamxanh.integration.ai.AiUsageEvent;
import tech.mamxanh.integration.ai.adk.AdkModelGateway;
import tech.mamxanh.shopping.dto.request.CreateEmptyShoppingListRequest;
import tech.mamxanh.shopping.dto.response.ShoppingListResponse;
import tech.mamxanh.shopping.service.ShoppingListService;
import tech.mamxanh.subscription.service.AiTokenQuotaService;

class AiTokenQuotaIntegrationTest extends AbstractIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AiTokenQuotaService quota;
    @Autowired private ShoppingListService shoppingLists;
    @Autowired private tech.mamxanh.auth.service.CurrentUserService users;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private tech.mamxanh.subscription.repository.AiCreditAdministrationRepository credits;
    private long firstMember;
    private long secondMember;

    @BeforeEach
    void createMembers() {
        firstMember = createMember("CUSTOMER");
        secondMember = createMember("EXPERT");
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        jdbc.update("DELETE FROM SHOPPING_LIST WHERE user_id IN (?, ?)", firstMember, secondMember);
        jdbc.update("DELETE FROM AI_CREDIT_LEDGER WHERE user_id IN (?, ?)", firstMember, secondMember);
        jdbc.update("DELETE FROM AI_CREDIT_WALLET WHERE user_id IN (?, ?)", firstMember, secondMember);
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
        mockMvc.perform(get("/api/v1/subscriptions/my-ai-usage").with(user(Long.toString(secondMember)).roles("EXPERT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consumedTokens").value(0));
    }

    @Test
    void g3SingleTransactionAccountingDebitsExactDecimalCapsAtWalletBalanceRecordsAbsorbedCreditsAndDeduplicatesByCallId() {
        authenticateAs(firstMember, "CUSTOMER");
        // Consume 49,950 of 50,000 free tokens so 50 quota tokens remain
        quota.record(new AiUsageEvent(UUID.randomUUID(), firstMember, null, "FR-51", clock.instant(),
                new AiTokenUsage("gemini-3.8-flash", "STANDARD", 25_000, 24_950, 49_950, 0, 0, 0)));

        // Seed wallet with 10.0000 Credits
        jdbc.update("UPDATE AI_CREDIT_WALLET SET balance = 10.0000, updated_at = SYSUTCDATETIME() WHERE user_id = ?",
                firstMember);

        // FREE tier cannot use PLUS/PRO-only model without entitlement
        assertThatThrownBy(() -> quota.beginCall(firstMember, null, "FR-51", "gemini-3.8-flash", clock.instant()))
                .isInstanceOf(tech.mamxanh.common.exception.ApiException.class);

        // Begin ADK call snapshot with FREE-allowed model
        var snapshot = quota.beginCall(firstMember, null, "FR-51", "gemini-3.5-flash-lite", clock.instant());

        // Complete call with 150 total tokens -> 50 covered by quota, 100 overage tokens -> 40.0000 Credits cost.
        // Wallet has 10.0000 -> debited 10.0000 (25 tokens), system absorbs 30.0000 Credits (75 tokens), balance_after = 0.0000.
        UUID callId = UUID.randomUUID();
        var usage = new AiTokenUsage("gemini-3.5-flash-lite", "STANDARD", 100, 50, 150, 0, 0, 0);
        quota.completeCall(callId, snapshot, usage);
        // Duplicate callId completion is a no-op
        quota.completeCall(callId, snapshot, usage);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM AI_TOKEN_USAGE_LEDGER WHERE event_id = ?",
                Integer.class, callId.toString())).isEqualTo(1);

        BigDecimal walletBalance = jdbc.queryForObject(
                "SELECT balance FROM AI_CREDIT_WALLET WHERE user_id = ?", BigDecimal.class, firstMember);
        assertThat(walletBalance).isEqualByComparingTo("0.0000");

        Map<String, Object> ledgerRow = jdbc.queryForMap(
                "SELECT credit_delta, balance_after, overage_tokens, system_absorbed_tokens, system_absorbed_credits "
                        + "FROM AI_CREDIT_LEDGER WHERE transaction_type = 'OVERAGE' AND reference_id = ?",
                callId.toString());
        assertThat((BigDecimal) ledgerRow.get("credit_delta")).isEqualByComparingTo("-10.0000");
        assertThat((BigDecimal) ledgerRow.get("balance_after")).isEqualByComparingTo("0.0000");
        assertThat(((Number) ledgerRow.get("overage_tokens")).longValue()).isEqualTo(100L);
        assertThat(((Number) ledgerRow.get("system_absorbed_tokens")).longValue()).isEqualTo(75L);
        assertThat((BigDecimal) ledgerRow.get("system_absorbed_credits")).isEqualByComparingTo("30.0000");
    }

    @Test
    void twoMemberAccountsCreateAndReadShoppingListsWithRealDatabaseIdsAndStrictOwnershipIsolation() {
        authenticateAs(firstMember, "CUSTOMER");
        ShoppingListResponse customerCreated = shoppingLists.createEmpty(new CreateEmptyShoppingListRequest("Đi chợ cuối tuần"));
        ShoppingListResponse customerRead = shoppingLists.get(customerCreated.id());
        assertThat(customerCreated.id()).isPositive();
        assertThat(customerRead.id()).isEqualTo(customerCreated.id());
        assertThat(customerRead.name()).isEqualTo("Đi chợ cuối tuần");
        assertThat(customerRead.items()).isEmpty();

        authenticateAs(secondMember, "EXPERT");
        ShoppingListResponse expertCreated = shoppingLists.createEmpty(new CreateEmptyShoppingListRequest("Đi chợ cuối tuần"));
        ShoppingListResponse expertRead = shoppingLists.get(expertCreated.id());
        assertThat(expertCreated.id()).isPositive().isNotEqualTo(customerCreated.id());
        assertThat(expertRead.id()).isEqualTo(expertCreated.id());

        // Cross-account read by ID is rejected
        assertThatThrownBy(() -> shoppingLists.get(customerCreated.id()))
                .isInstanceOf(AppException.class);
    }

    @Test void adkCallbackUsesOneRealTransactionAndRollsBackUsageWhenCreditWriteFails() {
        authenticateAs(firstMember,"CUSTOMER");
        var snapshot=quota.beginCall(firstMember,null,"FR-51","gemini-3.5-flash-lite",clock.instant());
        quota.completeCall(UUID.randomUUID(),snapshot,usage(snapshot,(int)snapshot.quotaLimit()));
        jdbc.update("UPDATE AI_CREDIT_WALLET SET balance=10 WHERE user_id=?",firstMember);
        var policy=new tech.mamxanh.integration.ai.adk.MemberAccountingPolicy(users,quota,null);
        var call=new AdkModelGateway.Call(UUID.randomUUID(),snapshot.model(),clock.instant());
        Object context=policy.beforeCall(call);
        var observed=new java.util.concurrent.atomic.AtomicBoolean();
        org.mockito.Mockito.doAnswer(inv->{
            observed.set(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());
            throw new org.springframework.dao.DataIntegrityViolationException("synthetic credit persistence failure");
        }).when(credits).debitOverage(org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.anyLong(),org.mockito.ArgumentMatchers.any());
        assertThatThrownBy(()->policy.onUsableResponse(call,context,new AdkModelGateway.Usage(usage(snapshot,3),AdkModelGateway.UsageStatus.RECORDED))).isInstanceOf(AppException.class);
        assertThat(observed).isTrue();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM AI_TOKEN_USAGE_LEDGER WHERE event_id=?",Integer.class,call.id().toString())).isZero();
        assertThat(credits.balance(firstMember)).isEqualByComparingTo("10");
    }

    @Test void concurrentCompletionsConsumeQuotaOnceAndDebitOnlyTheIncrementalOverage() {
        authenticateAs(firstMember,"CUSTOMER");
        var snapshot=quota.beginCall(firstMember,null,"FR-51","gemini-3.5-flash-lite",clock.instant());
        quota.completeCall(UUID.randomUUID(),snapshot,usage(snapshot,(int)snapshot.quotaLimit()-1));
        jdbc.update("UPDATE AI_CREDIT_WALLET SET balance=10 WHERE user_id=?",firstMember);
        UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        java.util.concurrent.CompletableFuture.allOf(
                java.util.concurrent.CompletableFuture.runAsync(()->quota.completeCall(a,snapshot,usage(snapshot,3))),
                java.util.concurrent.CompletableFuture.runAsync(()->quota.completeCall(b,snapshot,usage(snapshot,3)))).join();
        quota.completeCall(a,snapshot,usage(snapshot,3));
        assertThat(credits.balance(firstMember)).isEqualByComparingTo("8");
        assertThat(jdbc.queryForObject("SELECT SUM(overage_tokens) FROM AI_CREDIT_LEDGER WHERE user_id=? AND transaction_type='OVERAGE'",Long.class,firstMember)).isEqualTo(5);
    }

    @Test void callSnapshotSurvivesMonthlyResetAndFractionalCreditsAreNotRoundedToWholeTokens() {
        authenticateAs(firstMember,"CUSTOMER");
        var snapshot=quota.beginCall(firstMember,null,"FR-51","gemini-3.5-flash-lite",clock.instant());
        quota.completeCall(UUID.randomUUID(),snapshot,usage(snapshot,(int)snapshot.quotaLimit()));
        jdbc.update("UPDATE AI_CREDIT_WALLET SET balance=0.1 WHERE user_id=?",firstMember);
        clock.advance(java.time.Duration.ofDays(5));
        UUID call=UUID.randomUUID();quota.completeCall(call,snapshot,usage(snapshot,1));
        var row=jdbc.queryForMap("SELECT credit_delta,system_absorbed_credits,system_absorbed_tokens FROM AI_CREDIT_LEDGER WHERE reference_id=?",call.toString());
        assertThat((BigDecimal)row.get("credit_delta")).isEqualByComparingTo("-0.1");
        assertThat((BigDecimal)row.get("system_absorbed_credits")).isEqualByComparingTo("0.3");
        assertThat(row.get("system_absorbed_tokens")).isNull();
        assertThat(jdbc.queryForObject("SELECT period_start FROM AI_TOKEN_USAGE_LEDGER WHERE event_id=?",java.time.LocalDateTime.class,call.toString())).isEqualTo(snapshot.periodStart());
        assertThat(quota.getMyUsage(firstMember).consumedTokens()).isZero();
    }

    private static AiTokenUsage usage(AiTokenQuotaService.CallStartSnapshot snapshot,int tokens) {
        return new AiTokenUsage(snapshot.model(),"STANDARD",tokens,0,tokens,0,0,0);
    }

    private void authenticateAs(long userId, String role) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                Long.toString(userId), null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        SecurityContextHolder.setContext(context);
    }

    private long createMember(String role) {
        String email = "fr63-" + UUID.randomUUID() + "@example.invalid";
        jdbc.update("INSERT INTO [USER] (email, display_name, role, account_status, email_verified) "
                + "VALUES (?, N'FR63 kiểm thử', ?, 'ACTIVE', 1)", email, role);
        return jdbc.queryForObject("SELECT user_id FROM [USER] WHERE email = ?", Long.class, email);
    }
}
