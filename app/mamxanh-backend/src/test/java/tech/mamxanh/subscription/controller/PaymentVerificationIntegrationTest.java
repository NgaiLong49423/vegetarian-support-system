package tech.mamxanh.subscription.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tech.mamxanh.AbstractIntegrationTest;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.auth.security.AccessTokenService;
import tech.mamxanh.integration.payos.PayOsClient;
import tech.mamxanh.integration.payos.dto.PayOsPaymentInfo;
import tech.mamxanh.subscription.entity.PaymentStatus;
import tech.mamxanh.subscription.entity.PaymentTransactionEntity;
import tech.mamxanh.subscription.repository.PaymentTransactionRepository;
import tech.mamxanh.subscription.service.PaymentSettlementService;

/** Real SQL locking/atomicity/ownership; external payOS is controlled, never charged. */
class PaymentVerificationIntegrationTest extends AbstractIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired AccessTokenService tokens;
    @Autowired PaymentTransactionRepository payments;
    @Autowired PaymentSettlementService settlement;
    @Autowired java.time.Clock clock;
    @MockitoBean PayOsClient payOs;
    long owner;
    long other;
    String order;
    final List<Long> createdUsers = new ArrayList<>();

    @BeforeEach void createFixture() {
        owner = user("CUSTOMER");
        other = user("CUSTOMER");
        order = Long.toString(1 + Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 8_000_000_000_000_000L));
        var payment = new PaymentTransactionEntity(owner, order, 99000);
        payment.setCreatedAt(java.time.LocalDateTime.now(clock));
        payments.saveAndFlush(payment);
    }

    @AfterEach void cleanup() {
        for (long id : createdUsers) {
            jdbc.update("DELETE FROM PAYMENT_TRANSACTION WHERE user_id=?", id);
            jdbc.update("DELETE FROM SUBSCRIPTION WHERE user_id=?", id);
            jdbc.update("DELETE FROM AI_CREDIT_LEDGER WHERE user_id=?", id);
            jdbc.update("DELETE FROM AI_CREDIT_WALLET WHERE user_id=?", id);
            jdbc.update("DELETE FROM [USER] WHERE user_id=?", id);
        }
    }

    @Test void concurrentVerifiedCreditTopUpUpdatesWalletAndLedgerExactlyOnce() throws Exception {
        String creditOrder = Long.toString(1 + Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 8_000_000_000_000_000L));
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.minusMinutes(1);
        LocalDateTime paidAt = expiresAt.minusSeconds(1);
        var payment = new PaymentTransactionEntity(owner, creditOrder, 10_000);
        payment.setCreatedAt(now.minusMinutes(20));
        payment.setProductType(PaymentTransactionEntity.ProductType.AI_CREDIT);
        payment.setCreditAmountSnapshot(new BigDecimal("10000"));
        payment.setCreditsPer1000VndSnapshot(new BigDecimal("1000"));
        payment.setExpiresAt(expiresAt);
        payments.saveAndFlush(payment);

        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++) futures.add(executor.submit(() -> {
                try { assertThat(start.await(5, TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                }
                settlement.settle(creditOrder, 10_000, PaymentStatus.PAID, paidAt);
            }));
            start.countDown();
            for (var future : futures) future.get(20, TimeUnit.SECONDS);
        }

        assertThat(jdbc.queryForObject("SELECT balance FROM AI_CREDIT_WALLET WHERE user_id=?", BigDecimal.class, owner))
                .isEqualByComparingTo("10000");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM AI_CREDIT_LEDGER WHERE user_id=? AND transaction_type='TOP_UP' AND reference_id=?",
                Integer.class, owner, creditOrder)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SUBSCRIPTION WHERE user_id=?", Integer.class, owner)).isZero();
        assertThat(payments.findByOrderCode(creditOrder).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test void duplicateConcurrentDeliveriesActivateExactlyOnce() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 4; i++) futures.add(executor.submit(() -> {
                try { assertThat(start.await(5, TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                }
                settlement.settle(order, 99000, PaymentStatus.PAID);
            }));
            start.countDown();
            for (var future : futures) future.get(20, TimeUnit.SECONDS);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SUBSCRIPTION WHERE user_id=?", Integer.class, owner)).isEqualTo(1);
        assertThat(payments.findByOrderCode(order).orElseThrow().getSubscriptionId()).isNotNull();
    }

    @Test void replacementExpiresOldRowBeforeInsertingAndDuplicatePreservesPaidAt() {
        settlement.settle(order, 99000, PaymentStatus.PAID);
        String second = Long.toString(Long.parseLong(order) + 1);
        payments.saveAndFlush(new PaymentTransactionEntity(owner, second, 49000));
        settlement.settle(second, 49000, PaymentStatus.PAID);
        var paidAt = payments.findByOrderCode(second).orElseThrow().getPaidAt();
        settlement.settle(second, 49000, PaymentStatus.PAID);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SUBSCRIPTION WHERE user_id=? AND status='ACTIVE'", Integer.class, owner)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT tier FROM SUBSCRIPTION WHERE user_id=? AND status='ACTIVE'", String.class, owner)).isEqualTo("PLUS");
        assertThat(payments.findByOrderCode(second).orElseThrow().getPaidAt()).isEqualTo(paidAt);
    }

    @Test void anotherOwnerGets404WithoutQueryingProvider() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/transactions/{order}", order)
                        .header("Authorization", "Bearer " + token(other)))
                .andExpect(status().isNotFound());
        verifyNoInteractions(payOs);
    }

    @Test void resultReconcilesProviderDataAndIsNotCacheable() throws Exception {
        when(payOs.getPaymentInfo(Long.parseLong(order))).thenReturn(new PayOsPaymentInfo(Long.parseLong(order), 99000, 99000, "PAID"));
        mockMvc.perform(get("/api/v1/subscriptions/transactions/{order}", order)
                        .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.status").value("PAID")).andExpect(jsonPath("$.activated").value(true))
                .andExpect(jsonPath("$.subscription.tier").value("PRO"));
        assertThat(payments.findByOrderCode(order).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PAID);
        var payment = payments.findByOrderCode(order).orElseThrow();
        assertThat(payment.getCreatedAt()).isBeforeOrEqualTo(payment.getPaidAt());
        mockMvc.perform(get("/api/v1/subscriptions/transactions")
                        .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].createdAt").value(org.hamcrest.Matchers.endsWith("Z")))
                .andExpect(jsonPath("$[0].paidAt").value(org.hamcrest.Matchers.endsWith("Z")))
                .andExpect(jsonPath("$[0].endsAt").value(org.hamcrest.Matchers.endsWith("Z")));
    }

    @Test void guestAdminAndLockedMemberCannotAccessResult() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/transactions/{order}", order)).andExpect(status().isUnauthorized());
        long admin = user("ADMIN");
        mockMvc.perform(get("/api/v1/subscriptions/transactions/{order}", order)
                        .header("Authorization", "Bearer " + token(admin)))
                .andExpect(status().isForbidden());
        String token = token(owner);
        jdbc.update("UPDATE [USER] SET account_status='LOCKED' WHERE user_id=?", owner);
        mockMvc.perform(get("/api/v1/subscriptions/transactions/{order}", order)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        verifyNoInteractions(payOs);
    }

    @Test void underpaymentDoesNotMutateTransactionOrCreateEntitlement() throws Exception {
        when(payOs.getPaymentInfo(Long.parseLong(order))).thenReturn(new PayOsPaymentInfo(Long.parseLong(order), 99000, 1, "PAID"));
        mockMvc.perform(get("/api/v1/subscriptions/transactions/{order}", order)
                        .header("Authorization", "Bearer " + token(owner)))
                .andExpect(status().isBadGateway());
        assertThat(payments.findByOrderCode(order).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM SUBSCRIPTION WHERE user_id=?", Integer.class, owner)).isZero();
    }

    private long user(String role) {
        long id = jdbc.queryForObject("INSERT INTO [USER] (email,display_name,role,account_status,email_verified) OUTPUT INSERTED.user_id VALUES (?,N'Issue 142',?,'ACTIVE',1)",
                Long.class, "issue142-" + UUID.randomUUID() + "@test.local", role);
        createdUsers.add(id);
        return id;
    }

    private String token(long user) {
        return tokens.issue(users.findById(user).orElseThrow()).value();
    }
}
