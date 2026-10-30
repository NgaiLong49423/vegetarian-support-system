package tech.mamxanh.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.auth.entity.AccountStatus;
import tech.mamxanh.auth.entity.Role;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.payos.PayOsClient;
import tech.mamxanh.integration.payos.dto.CreatePaymentLinkResponse;
import tech.mamxanh.integration.payos.dto.PayOsPaymentData;
import tech.mamxanh.integration.payos.dto.PayOsWebhookPayload;
import tech.mamxanh.subscription.dto.response.CheckoutResponse;
import tech.mamxanh.subscription.dto.response.MySubscriptionResponse;
import tech.mamxanh.subscription.dto.response.PlanResponse;
import tech.mamxanh.subscription.entity.PaymentStatus;
import tech.mamxanh.subscription.entity.PaymentTransactionEntity;
import tech.mamxanh.subscription.entity.SubscriptionEntity;
import tech.mamxanh.subscription.entity.SubscriptionStatus;
import tech.mamxanh.subscription.entity.SubscriptionTier;
import tech.mamxanh.subscription.repository.PaymentTransactionRepository;
import tech.mamxanh.subscription.repository.SubscriptionRepository;
import tech.mamxanh.subscription.repository.AiConfigurationRepository;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PayOsClient payOsClient;

    @Mock private PaymentSettlementService settlement;
    @Mock private PaymentOrderService orders;
    @Mock private AiConfigurationRepository aiConfiguration;

    private Clock clock;
    private SubscriptionService subscriptionService;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-10-04T12:00:00Z");

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
        subscriptionService = new SubscriptionService(
                subscriptionRepository,
                paymentTransactionRepository,
                userRepository,
                payOsClient,
                clock, settlement, orders, aiConfiguration
        );
        lenient().when(aiConfiguration.findConfiguration()).thenReturn(new tech.mamxanh.subscription.entity.AiConfigurationSnapshot(
                List.of(new tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Plan("FREE", 0, 50_000, null, null),
                        new tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Plan("PLUS", 99_000, 300_000, null, null),
                        new tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Plan("PRO", 199_000, 600_000, null, null)),
                List.of(new tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Model("gemini-3.5-flash-lite", "Gemini Flash-Lite", true,
                                true, true, true, false, true, false),
                        new tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Model("gemini-3.8-flash", "Gemini Flash", true,
                                false, false, true, true, true, true)),
                new tech.mamxanh.subscription.entity.AiConfigurationSnapshot.Credit(java.math.BigDecimal.valueOf(1000), 400, 10_000, 500_000)));
        lenient().when(aiConfiguration.currentFreeQuota(any())).thenReturn(50_000L);
    }

    @Test
    @DisplayName("AC-13.1: Should return exactly 3 official Phase 1 tiers (FREE, PLUS, PRO) without trial or yearly discount")
    void shouldReturnValidPlansWithoutTrialOrYearlyDiscount() {
        List<PlanResponse> plans = subscriptionService.getAvailablePlans();

        assertThat(plans).hasSize(3);

        PlanResponse free = plans.get(0);
        assertThat(free.tier()).isEqualTo("FREE");
        assertThat(free.priceVnd()).isEqualTo(0);

        PlanResponse plus = plans.get(1);
        assertThat(plus.tier()).isEqualTo("PLUS");
        assertThat(plus.features()).anyMatch(feature -> feature.contains("300.000 token mỗi tháng"));
        assertThat(plus.features()).anyMatch(feature -> feature.contains("Gemini Flash-Lite") && feature.contains("Gemini Flash"));
        assertThat(plus.features()).noneMatch(feature -> feature.contains("Toàn bộ quyền lợi") || feature.contains("AI lập thực đơn"));
        assertThat(plus.priceVnd()).isEqualTo(99_000);
        assertThat(plus.billingPeriod()).contains("Tháng");

        PlanResponse pro = plans.get(2);
        assertThat(pro.tier()).isEqualTo("PRO");
        assertThat(pro.features()).anyMatch(feature -> feature.contains("600.000 token mỗi tháng"));
        assertThat(pro.priceVnd()).isEqualTo(199_000);
        assertThat(pro.billingPeriod()).contains("Tháng");

        // Verify none contains trial, annual discount, or coupon
        for (PlanResponse p : plans) {
            assertThat(p.billingPeriod()).doesNotContain("Năm");
            assertThat(p.description()).doesNotContain("dùng thử");
        }
    }

    @Test
    @DisplayName("AC-13.4: Should auto-downgrade to FREE when subscription validity period expires")
    void shouldDowngradeToFreeWhenSubscriptionExpires() {
        Long userId = 1L;
        mockActiveUser(userId);

        LocalDateTime startsAt = LocalDateTime.now(clock).minusDays(31);
        LocalDateTime endsAt = LocalDateTime.now(clock).minusDays(1); // expired yesterday
        SubscriptionEntity expiredSub = new SubscriptionEntity(userId, SubscriptionTier.PLUS, startsAt, endsAt);

        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(expiredSub));

        MySubscriptionResponse response = subscriptionService.getMySubscription(userId);

        assertThat(response.tier()).isEqualTo("FREE");
        assertThat(expiredSub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(subscriptionRepository).save(expiredSub);
    }

    @Test
    @DisplayName("AC-13.7: Should display active subscription tier and exact expiration date")
    void shouldReturnCurrentSubscriptionWithExpirationDate() {
        Long userId = 1L;
        mockActiveUser(userId);

        LocalDateTime startsAt = LocalDateTime.now(clock).minusDays(5);
        LocalDateTime endsAt = LocalDateTime.now(clock).plusDays(25);
        SubscriptionEntity activeSub = new SubscriptionEntity(userId, SubscriptionTier.PRO, startsAt, endsAt);

        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeSub));

        MySubscriptionResponse response = subscriptionService.getMySubscription(userId);

        assertThat(response.tier()).isEqualTo("PRO");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.active()).isTrue();
        assertThat(response.startsAt()).isEqualTo(startsAt);
        assertThat(response.unlockedFeatures()).contains("Hạn mức 600.000 token mỗi tháng", "Model Gemini: Gemini Flash-Lite, Gemini Flash (mặc định)")
                .anyMatch(feature -> feature.contains("theo vai trò và quyền nghiệp vụ"));
    }

    @Test
    void futureSubscriptionDoesNotUnlockFeaturesOrCancelTheFuturePeriod() {
        mockActiveUser(1L);
        var now = LocalDateTime.now(clock);
        var future = new SubscriptionEntity(1L, SubscriptionTier.PRO, now.plusDays(1), now.plusDays(31));
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(future));

        var response = subscriptionService.getMySubscription(1L);

        assertThat(response.tier()).isEqualTo("FREE");
        assertThat(response.unlockedFeatures()).doesNotContain("Hạn mức 600.000 token mỗi tháng");
        assertThat(future.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void subscriptionUnlocksAtItsExactStartAndExpiresAtItsExactEnd() {
        mockActiveUser(1L);
        var now = LocalDateTime.now(clock);
        var starting = new SubscriptionEntity(1L, SubscriptionTier.PRO, now, now.plusDays(30));
        var ending = new SubscriptionEntity(1L, SubscriptionTier.PRO, now.minusDays(30), now);
        when(subscriptionRepository.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(starting), Optional.of(ending));

        assertThat(subscriptionService.getMySubscription(1L).tier()).isEqualTo("PRO");
        assertThat(subscriptionService.getMySubscription(1L).tier()).isEqualTo("FREE");
        assertThat(ending.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    @DisplayName("Should successfully create checkout and return payOS payment link")
    void shouldCreateCheckoutSuccessfully() {
        Long userId = 1L;
        mockActiveUser(userId);

        when(payOsClient.createPaymentLink(anyLong(), anyInt(), anyString()))
                .thenReturn(new CreatePaymentLinkResponse("00", "success",
                        new PayOsPaymentData(123456L, 99_000, "Nang cap PLUS", "https://pay.payos.vn/123", "qr_123", "plink_1", "PENDING"),
                        "sig_123"));

        CheckoutResponse response = subscriptionService.createCheckout(userId, SubscriptionTier.PLUS);

        assertThat(response).isNotNull();
        assertThat(response.amountVnd()).isEqualTo(99_000);
        assertThat(response.tier()).isEqualTo("PLUS");
        assertThat(response.checkoutUrl()).isEqualTo("https://pay.payos.vn/123");
        var captured = ArgumentCaptor.forClass(PaymentTransactionEntity.class);
        verify(orders).create(captured.capture());
        assertThat(captured.getValue().getCreatedAt()).isEqualTo(LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC));
        assertThat(captured.getValue().getSubscriptionTier()).isEqualTo(SubscriptionTier.PLUS);
        assertThat(captured.getValue().getTokenQuotaSnapshot()).isEqualTo(300_000L);
    }

    @Test
    @DisplayName("Should throw exception when creating checkout with null tier")
    void shouldThrowExceptionWhenCreatingCheckoutWithNullTier() {
        Long userId = 1L;
        assertThatThrownBy(() -> subscriptionService.createCheckout(userId, null))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("chọn gói đăng ký hợp lệ");
    }

    @Test
    @DisplayName("Should mark transaction FAILED when payOS returns unsuccessful response")
    void shouldMarkTransactionFailedWhenPayOsReturnsError() {
        Long userId = 1L;
        mockActiveUser(userId);

        when(payOsClient.createPaymentLink(anyLong(), anyInt(), anyString()))
                .thenReturn(new CreatePaymentLinkResponse("99", "Failed", null, ""));

        assertThatThrownBy(() -> subscriptionService.createCheckout(userId, SubscriptionTier.PLUS))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Không thể khởi tạo liên kết thanh toán payOS");

        verify(orders).create(any(PaymentTransactionEntity.class));
        verify(orders).markFailed(anyString());
    }

    @Test
    @DisplayName("AC-13.4: Should automatically downgrade expired subscription to FREE")
    void shouldDowngradeExpiredSubscriptionToFree() {
        Long userId = 1L;
        mockActiveUser(userId);

        LocalDateTime expiredStartsAt = LocalDateTime.now(clock).minusDays(35);
        LocalDateTime expiredEndsAt = LocalDateTime.now(clock).minusDays(5);
        SubscriptionEntity expiredSub = new SubscriptionEntity(userId, SubscriptionTier.PLUS, expiredStartsAt, expiredEndsAt);

        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(expiredSub));

        MySubscriptionResponse response = subscriptionService.getMySubscription(userId);

        assertThat(response.tier()).isEqualTo("FREE");
        assertThat(expiredSub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(subscriptionRepository).save(expiredSub);
    }

    @Test
    @DisplayName("AC-13.7: Should return configured monthly benefits for active PLUS subscription")
    void shouldReturnPlusFeaturesForPlusSubscription() {
        Long userId = 1L;
        mockActiveUser(userId);

        LocalDateTime startsAt = LocalDateTime.now(clock).minusDays(1);
        LocalDateTime endsAt = LocalDateTime.now(clock).plusDays(29);
        SubscriptionEntity plusSub = new SubscriptionEntity(userId, SubscriptionTier.PLUS, startsAt, endsAt);

        when(subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(plusSub));

        MySubscriptionResponse response = subscriptionService.getMySubscription(userId);

        assertThat(response.tier()).isEqualTo("PLUS");
        assertThat(response.unlockedFeatures()).contains("Hạn mức 300.000 token mỗi tháng", "Model Gemini: Gemini Flash-Lite, Gemini Flash (mặc định)")
                .noneMatch(feature -> feature.contains("Toàn bộ quyền lợi") || feature.contains("AI lập thực đơn"));
    }

    @Test
    @DisplayName("Should cover entity constructors, getters and setters")
    void shouldCoverEntityMethods() {
        LocalDateTime now = LocalDateTime.now(clock);
        SubscriptionEntity sub = new SubscriptionEntity(20L, SubscriptionTier.PRO, now, now.plusDays(30));
        sub.setId(10L);
        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setCreatedAt(now);
        sub.setUpdatedAt(now);

        assertThat(sub.getId()).isEqualTo(10L);
        assertThat(sub.getUserId()).isEqualTo(20L);
        assertThat(sub.getTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getStartsAt()).isEqualTo(now);
        assertThat(sub.getEndsAt()).isEqualTo(now.plusDays(30));
        assertThat(sub.getCreatedAt()).isEqualTo(now);
        assertThat(sub.getUpdatedAt()).isEqualTo(now);

        PaymentTransactionEntity tx = new PaymentTransactionEntity(20L, "ORD123", 99000);
        tx.setId(100L);
        tx.setStatus(PaymentStatus.PAID);
        tx.setSubscriptionId(10L);
        tx.setCreatedAt(now);
        tx.setPaidAt(now);

        assertThat(tx.getId()).isEqualTo(100L);
        assertThat(tx.getUserId()).isEqualTo(20L);
        assertThat(tx.getOrderCode()).isEqualTo("ORD123");
        assertThat(tx.getAmountVnd()).isEqualTo(99000);
        assertThat(tx.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(tx.getSubscriptionId()).isEqualTo(10L);
        assertThat(tx.getCreatedAt()).isEqualTo(now);
        assertThat(tx.getPaidAt()).isEqualTo(now);
    }

    @Test
    void rejectsInvalidSignatureBeforeSettlement() {
        PayOsWebhookPayload payload = new PayOsWebhookPayload("00", "ok", Map.of("orderCode", 123), "bad");
        assertThatThrownBy(() -> subscriptionService.handlePayOsWebhook(payload))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_SIGNATURE_INVALID);
        org.mockito.Mockito.verifyNoInteractions(settlement);
    }

    @Test
    void delegatesOnlyVerifiedVndWebhookToSettlement() {
        PayOsWebhookPayload payload = new PayOsWebhookPayload("00", "ok",
                Map.of("orderCode", 123, "amount", 99000, "currency", "VND", "code", "00"), "signed");
        when(payOsClient.verifyWebhook(payload)).thenReturn(true);
        subscriptionService.handlePayOsWebhook(payload);
        verify(settlement).settle("123", 99000, PaymentStatus.PAID);
    }

    @Test
    void rejectsMalformedOrForeignCurrencyWebhook() {
        PayOsWebhookPayload missing = new PayOsWebhookPayload("00", "ok", Map.of("amount", 99000), "signed");
        when(payOsClient.verifyWebhook(missing)).thenReturn(true);
        assertThatThrownBy(() -> subscriptionService.handlePayOsWebhook(missing)).isInstanceOf(AppException.class);
        PayOsWebhookPayload foreign = new PayOsWebhookPayload("00", "ok",
                Map.of("orderCode", 123, "amount", 99000, "currency", "USD", "code", "00"), "signed");
        when(payOsClient.verifyWebhook(foreign)).thenReturn(true);
        assertThatThrownBy(() -> subscriptionService.handlePayOsWebhook(foreign)).isInstanceOf(AppException.class);
        PayOsWebhookPayload missingCode = new PayOsWebhookPayload("00", "ok",
                Map.of("orderCode", 123, "amount", 99000, "currency", "VND"), "signed");
        when(payOsClient.verifyWebhook(missingCode)).thenReturn(true);
        assertThatThrownBy(() -> subscriptionService.handlePayOsWebhook(missingCode)).isInstanceOf(AppException.class);
        org.mockito.Mockito.verifyNoInteractions(settlement);
    }

    @Test
    void historyUsesEachPurchasedSubscriptionExpiryIncludingExpiredPeriods() {
        mockActiveUser(1L);
        var time = LocalDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
        var oldPayment = new PaymentTransactionEntity(1L, "old", 49000);
        oldPayment.setStatus(PaymentStatus.PAID);
        oldPayment.setSubscriptionId(10L);
        var newPayment = new PaymentTransactionEntity(1L, "new", 99000);
        newPayment.setStatus(PaymentStatus.PAID);
        newPayment.setSubscriptionId(11L);
        var cancelled = new PaymentTransactionEntity(1L, "cancelled", 99000);
        cancelled.setStatus(PaymentStatus.CANCELLED);
        var unlinked = new PaymentTransactionEntity(1L, "legacy", 49000);
        unlinked.setStatus(PaymentStatus.PAID);
        var oldSub = new SubscriptionEntity(1L, SubscriptionTier.PLUS, time.minusDays(60), time.minusDays(30));
        oldSub.setId(10L);
        oldSub.setStatus(SubscriptionStatus.EXPIRED);
        var newSub = new SubscriptionEntity(1L, SubscriptionTier.PRO, time, time.plusDays(30));
        newSub.setId(11L);
        when(paymentTransactionRepository.findAllByUserIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(newPayment, oldPayment, cancelled, unlinked));
        when(subscriptionRepository.findAllById(List.of(11L, 10L))).thenReturn(List.of(oldSub, newSub));
        var history = subscriptionService.getTransactionHistory(1L);
        assertThat(history).extracting(r -> r.endsAt()).containsExactly(time.plusDays(30), time.minusDays(30), null, null);
    }

    private void mockActiveUser(Long userId) {
        User user = org.mockito.Mockito.mock(User.class);
        org.mockito.Mockito.lenient().when(user.getId()).thenReturn(userId);
        org.mockito.Mockito.lenient().when(user.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        org.mockito.Mockito.lenient().when(user.getRole()).thenReturn(Role.CUSTOMER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }
}
