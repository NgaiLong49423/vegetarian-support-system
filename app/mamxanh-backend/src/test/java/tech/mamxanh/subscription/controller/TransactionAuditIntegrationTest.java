package tech.mamxanh.subscription.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.auth.security.AccessTokenService;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.payos.PayOsClient;
import tech.mamxanh.integration.payos.dto.CreatePaymentLinkResponse;
import tech.mamxanh.integration.payos.dto.PayOsPaymentData;
import tech.mamxanh.subscription.entity.PaymentStatus;
import tech.mamxanh.subscription.entity.SubscriptionTier;
import tech.mamxanh.subscription.service.PaymentSettlementService;
import tech.mamxanh.subscription.service.SubscriptionService;

/** Real SQL Server, real JWT/RBAC, controlled provider: never creates an external order or charge. */
class TransactionAuditIntegrationTest extends AbstractIntegrationTest {
    private static final String URL = "/api/v1/admin/transactions";
    @Autowired JdbcTemplate jdbc;
    @Autowired AccessTokenService tokens;
    @Autowired UserRepository users;
    @Autowired SubscriptionService subscriptions;
    @Autowired PaymentSettlementService settlement;
    @MockitoBean PayOsClient provider;
    List<Long> actors = new ArrayList<>();
    long admin, member, expert;
    String marker;
    @BeforeEach void setup() {
        marker = "audit-" + UUID.randomUUID();
        admin = actor("ADMIN", "Quản trị"); member = actor("CUSTOMER", "Khách An"); expert = actor("EXPERT", "Chuyên gia");
        clock.advance(java.time.Duration.between(clock.instant(), Instant.parse("2026-10-29T10:00:00Z")));
    }
    @AfterEach void cleanup() {
        for (long actor : actors) {
            jdbc.update("DELETE FROM PAYMENT_TRANSACTION WHERE user_id=?", actor);
            jdbc.update("DELETE FROM SUBSCRIPTION WHERE user_id=?", actor);
            jdbc.update("DELETE FROM [USER] WHERE user_id=?", actor);
        }
    }
    @Test void totalsSpanAllFilteredPagesAndOnlyPaidIsRevenue() throws Exception {
        payment(member, 49000, "PAID", "2026-10-28T18:00:00");
        long latest = payment(member, 99000, "PENDING", "2026-10-28T19:00:00");
        payment(member, 49000, "FAILED", "2026-10-28T16:00:00");
        payment(member, 99000, "CANCELLED", "2026-10-28T15:00:00");
        asAdmin(get(URL).param("query", marker).param("size", "1"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.transactions.totalElements").value(4))
            .andExpect(jsonPath("$.transactions.totalPages").value(4))
            .andExpect(jsonPath("$.transactions.content[0].id").value(latest))
            .andExpect(jsonPath("$.summary.revenueVnd").value(49000))
            .andExpect(jsonPath("$.summary.paid").value(1)).andExpect(jsonPath("$.summary.pending").value(1))
            .andExpect(jsonPath("$.summary.failed").value(1)).andExpect(jsonPath("$.summary.cancelled").value(1));
        asAdmin(get(URL).param("query", marker).param("tier", "PLUS").param("status", "PAID"))
            .andExpect(jsonPath("$.summary.total").value(1)).andExpect(jsonPath("$.transactions.content[0].tier").value("PLUS"));
    }
    @Test void creationDateIsInclusiveVietnamTimeAndPagingHasAnIdTieBreaker() throws Exception {
        payment(member, 49000, "PAID", "2026-10-28T16:59:59");
        payment(member, 49000, "PAID", "2026-10-28T17:00:00");
        long last = payment(member, 99000, "PAID", "2026-10-28T17:00:00");
        payment(member, 99000, "PENDING", "2026-10-29T17:00:00");
        asAdmin(get(URL).param("query", marker).param("from", "2026-10-29").param("to", "2026-10-29").param("size", "1"))
            .andExpect(jsonPath("$.summary.total").value(2)).andExpect(jsonPath("$.transactions.content[0].id").value(last))
            .andExpect(jsonPath("$.transactions.content[0].createdAt").value("2026-10-28T17:00:00Z"));
        asAdmin(get(URL).param("query", marker).param("from", "2026-10-29").param("to", "2026-10-29").param("size", "1").param("page", "1"))
            .andExpect(jsonPath("$.transactions.content[0].id").value(last - 1));
    }
    @Test void literalSearchSupportsEmailNameOrderIdAndEscapesSqlWildcards() throws Exception {
        long id = payment(member, 49000, "PENDING", "2026-10-28T18:00:00");
        for (String query : List.of(marker, "Khách An", "TX-" + id))
            asAdmin(get(URL).param("query", query)).andExpect(jsonPath("$.summary.total").value(1));
        for (String query : List.of("%", "_", "[", "!", "' OR 1=1 --"))
            asAdmin(get(URL).param("query", query)).andExpect(status().isOk()).andExpect(jsonPath("$.summary.total").value(0));
    }
    @Test void detailAndExportNeverSettleOrChangeThePurchasedPeriod() throws Exception {
        long id = payment(member, 99000, "PENDING", "2026-10-28T18:00:00");
        String token = token(admin);
        asAdmin(get(URL + "/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.subscriptionId").doesNotExist());
        asAdmin(get(URL + "/export").param("query", marker)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT status FROM PAYMENT_TRANSACTION WHERE payment_transaction_id=?", String.class, id)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SUBSCRIPTION WHERE user_id=?", Integer.class, member)).isZero();
        verifyNoInteractions(provider);
        jdbc.update("UPDATE [USER] SET account_status='LOCKED' WHERE user_id=?", admin);
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }
    @Test void guestCustomerExpertCannotReadListDetailOrExport() throws Exception {
        long id = payment(member, 49000, "PAID", "2026-10-28T18:00:00");
        for (String path : List.of(URL, URL + "/" + id, URL + "/export")) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
            for (long actor : List.of(member, expert)) mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token(actor))).andExpect(status().isForbidden());
        }
    }
    @Test void validatesInputsAndMissingDetail() throws Exception {
        for (String[] pair : List.of(new String[]{"tier","FREE"}, new String[]{"status","SUCCESS"}, new String[]{"page","-1"},
                new String[]{"size","0"}, new String[]{"size","101"}, new String[]{"from","2026-10-30"}, new String[]{"to","1899-01-01"},
                new String[]{"query","x".repeat(201)}, new String[]{"query","a\nb"})) {
            asAdmin(get(URL).param(pair[0],pair[1])).andExpect(status().isBadRequest());
        }
        asAdmin(get(URL).param("from","2026-10-29").param("to","2026-10-28")).andExpect(status().isBadRequest());
        asAdmin(get(URL + "/export").param("tier","FREE").accept("text/csv", "application/problem+json"))
            .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        asAdmin(get(URL + "/0")).andExpect(status().isBadRequest());
        asAdmin(get(URL + "/9223372036854775807")).andExpect(status().isNotFound());
        asAdmin(get(URL).param("query",marker)).andExpect(jsonPath("$.summary.total").value(0)).andExpect(jsonPath("$.summary.revenueVnd").value(0));
    }
    @Test void unknownHistoricalAmountIsNotSilentlyMappedToPlusAndRevenueUsesBigint() throws Exception {
        payment(member, 2_000_000_000, "PAID", "2026-10-28T18:00:00");
        payment(member, 2_000_000_000, "PAID", "2026-10-28T18:00:00");
        asAdmin(get(URL).param("query",marker)).andExpect(jsonPath("$.summary.revenueVnd").value(4_000_000_000L))
            .andExpect(jsonPath("$.transactions.content[0].tier").doesNotExist());
    }
    @Test void csvUsesAllFilteredRowsUtf8QuotingAndFormulaNeutralization() throws Exception {
        jdbc.update("UPDATE [USER] SET display_name=? WHERE user_id=?", "  =SUM(1,2)\"\nNguyễn", member);
        payment(member, 49000, "PAID", "2026-10-28T18:00:00");
        payment(member, 99000, "CANCELLED", "2026-10-28T18:00:00");
        var result = asAdmin(get(URL + "/export").param("query",marker).param("status","PAID"))
            .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/csv"))
            .andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
        assertThat(result.getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).startsWith("\uFEFF")
            .contains("\"'  =SUM(1,2)\"\"\nNguyễn\"").contains("\"PAID\"").doesNotContain("CANCELLED");
    }
    @Test void missingProviderConfigurationDoesNotCreateAPhantomPendingOrder() {
        doThrow(new AppException(ErrorCode.PAYMENT_UNAVAILABLE)).when(provider).validateCheckoutConfiguration();
        assertThatThrownBy(() -> subscriptions.createCheckout(member, SubscriptionTier.PLUS)).isInstanceOf(AppException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PAYMENT_TRANSACTION WHERE user_id=?",Integer.class,member)).isZero();
        verify(provider, never()).createPaymentLink(anyLong(), anyInt(), anyString());
    }
    @Test void checkoutCommitsOrderBeforeProviderAndKeepsItAfterAmbiguousTimeout() {
        when(provider.createPaymentLink(anyLong(),anyInt(),anyString())).thenAnswer(call -> {
            String code = Long.toString(call.getArgument(0));
            // A separate JDBC connection must see the order before the remote operation finishes.
            assertThat(java.util.concurrent.CompletableFuture.supplyAsync(() ->
                    jdbc.queryForObject("SELECT COUNT(*) FROM PAYMENT_TRANSACTION WHERE order_code=?",Integer.class,code))
                    .get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(1);
            throw new AppException(ErrorCode.PAYMENT_UNAVAILABLE);
        });
        assertThatThrownBy(() -> subscriptions.createCheckout(member,SubscriptionTier.PLUS)).isInstanceOf(AppException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM PAYMENT_TRANSACTION WHERE user_id=? AND status='PENDING'",Integer.class,member)).isEqualTo(1);
    }
    @Test void earlyVerifiedSettlementSurvivesCheckoutResponseAndAuditShowsLinkedPurchase() throws Exception {
        when(provider.createPaymentLink(anyLong(),anyInt(),anyString())).thenAnswer(call -> {
            long code=call.getArgument(0); int amount=call.getArgument(1);
            java.util.concurrent.CompletableFuture.runAsync(() ->
                    settlement.settle(Long.toString(code),amount,PaymentStatus.PAID))
                    .get(10, java.util.concurrent.TimeUnit.SECONDS);
            return new CreatePaymentLinkResponse("00","success",new PayOsPaymentData(code,amount,"Nang cap PLUS","https://pay.payos.vn/test","test-qr","link","PENDING"),"sig");
        });
        var checkout=subscriptions.createCheckout(member,SubscriptionTier.PLUS);
        asAdmin(get(URL).param("query",Long.toString(checkout.orderCode())))
            .andExpect(jsonPath("$.summary.paid").value(1)).andExpect(jsonPath("$.transactions.content[0].subscriptionId").isNumber())
            .andExpect(jsonPath("$.transactions.content[0].subscriptionEndsAt").value("2026-11-28T10:00:00Z"));
        settlement.settle(Long.toString(checkout.orderCode()),49000,PaymentStatus.PAID);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SUBSCRIPTION WHERE user_id=?",Integer.class,member)).isEqualTo(1);
    }
    @Test void explicitCreateFailureIsDurableButNeverOverwritesAnEarlyPaidCallback() {
        when(provider.createPaymentLink(anyLong(),anyInt(),anyString())).thenReturn(new CreatePaymentLinkResponse("99","failed",null,""));
        assertThatThrownBy(() -> subscriptions.createCheckout(member,SubscriptionTier.PLUS)).isInstanceOf(AppException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM PAYMENT_TRANSACTION WHERE user_id=?",String.class,member)).isEqualTo("FAILED");
        when(provider.createPaymentLink(anyLong(),anyInt(),anyString())).thenAnswer(call -> {
            settlement.settle(Long.toString(call.getArgument(0)),call.getArgument(1),PaymentStatus.PAID);
            return new CreatePaymentLinkResponse("99","failed",null,"");
        });
        assertThatThrownBy(() -> subscriptions.createCheckout(expert,SubscriptionTier.PRO)).isInstanceOf(AppException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM PAYMENT_TRANSACTION WHERE user_id=?",String.class,expert)).isEqualTo("PAID");
    }

    private long actor(String role,String name) {
        long id=jdbc.queryForObject("INSERT INTO [USER](email,display_name,role,email_verified,account_status) OUTPUT INSERTED.user_id VALUES(?,?,?,1,'ACTIVE')",Long.class,marker+role+"@example.invalid",name,role);
        actors.add(id);return id;
    }
    private long payment(long user,int amount,String status,String created) {
        return jdbc.queryForObject("INSERT INTO PAYMENT_TRANSACTION(user_id,order_code,amount_vnd,status,created_at) OUTPUT INSERTED.payment_transaction_id VALUES(?,?,?,?,?)",Long.class,user,marker+UUID.randomUUID(),amount,status,LocalDateTime.parse(created));
    }
    private String token(long actor) {return tokens.issue(users.findById(actor).orElseThrow()).value();}
    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION,"Bearer "+token(admin)));}
}
