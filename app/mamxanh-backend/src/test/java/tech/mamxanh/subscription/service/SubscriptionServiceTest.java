package tech.mamxanh.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
                clock
        );
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
        assertThat(plus.priceVnd()).isEqualTo(49_000);
        assertThat(plus.billingPeriod()).contains("Tháng");

        PlanResponse pro = plans.get(2);
        assertThat(pro.tier()).isEqualTo("PRO");
        assertThat(pro.priceVnd()).isEqualTo(99_000);
        assertThat(pro.billingPeriod()).contains("Tháng");

        // Verify none contains trial, annual discount, or coupon
        for (PlanResponse p : plans) {
            assertThat(p.billingPeriod()).doesNotContain("Năm");
            assertThat(p.description()).doesNotContain("dùng thử");
        }
    }

    @Test
    @DisplayName("AC-13.2: Should activate PLUS subscription when valid IPN payment webhook is received")
    void shouldActivateSubscriptionOnValidWebhookSignatureAndAmount() {
        long orderCode = 123456789L;
        PaymentTransactionEntity tx = new PaymentTransactionEntity(1L, String.valueOf(orderCode), 49_000);
        when(paymentTransactionRepository.findByOrderCode(String.valueOf(orderCode))).thenReturn(Optional.of(tx));

        PayOsWebhookPayload payload = new PayOsWebhookPayload(
                "00", "success",
                Map.of("orderCode", orderCode, "amount", 49_000, "code", "00"),
                "valid_signature"
        );
        when(payOsClient.verifyWebhook(payload)).thenReturn(true);

        SubscriptionEntity savedSub = new SubscriptionEntity(1L, SubscriptionTier.PLUS,
                LocalDateTime.now(clock), LocalDateTime.now(clock).plusDays(30));
        when(subscriptionRepository.save(any(SubscriptionEntity.class))).thenReturn(savedSub);

        subscriptionService.handlePayOsWebhook(payload);

        assertThat(tx.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(tx.getPaidAt()).isEqualTo(LocalDateTime.now(clock));

        ArgumentCaptor<SubscriptionEntity> subCaptor = ArgumentCaptor.forClass(SubscriptionEntity.class);
        verify(subscriptionRepository).save(subCaptor.capture());
        SubscriptionEntity activated = subCaptor.getValue();
        assertThat(activated.getTier()).isEqualTo(SubscriptionTier.PLUS);
        assertThat(activated.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(activated.getEndsAt()).isEqualTo(LocalDateTime.now(clock).plusDays(30));
    }

    @Test
    @DisplayName("AC-13.3: Should handle duplicate webhook IPN idempotently without duplicate subscription")
    void shouldHandleDuplicateWebhookIdempotentlyWithoutExtendingPeriod() {
        long orderCode = 123456789L;
        PaymentTransactionEntity alreadyPaidTx = new PaymentTransactionEntity(1L, String.valueOf(orderCode), 49_000);
        alreadyPaidTx.setStatus(PaymentStatus.PAID);

        when(paymentTransactionRepository.findByOrderCode(String.valueOf(orderCode))).thenReturn(Optional.of(alreadyPaidTx));

        PayOsWebhookPayload payload = new PayOsWebhookPayload(
                "00", "success",
                Map.of("orderCode", orderCode, "amount", 49_000),
                "valid_signature"
        );
        when(payOsClient.verifyWebhook(payload)).thenReturn(true);

        // Call duplicate webhook
        subscriptionService.handlePayOsWebhook(payload);

        // Verify no new subscription saved
        verify(subscriptionRepository, never()).save(any(SubscriptionEntity.class));
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
    @DisplayName("AC-13.5: Should reject webhook and throw exception if signature is invalid")
    void shouldRejectWebhookWithInvalidSignature() {
        PayOsWebhookPayload forgedPayload = new PayOsWebhookPayload(
                "00", "success",
                Map.of("orderCode", 99999L, "amount", 49_000),
                "fake_signature"
        );
        when(payOsClient.verifyWebhook(forgedPayload)).thenReturn(false);

        assertThatThrownBy(() -> subscriptionService.handlePayOsWebhook(forgedPayload))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_SIGNATURE_INVALID);

        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC-13.5: Should reject webhook and set transaction to FAILED if amount is tampered")
    void shouldRejectWebhookWithTamperedAmount() {
        long orderCode = 123456789L;
        PaymentTransactionEntity tx = new PaymentTransactionEntity(1L, String.valueOf(orderCode), 49_000);
        when(paymentTransactionRepository.findByOrderCode(String.valueOf(orderCode))).thenReturn(Optional.of(tx));

        PayOsWebhookPayload tamperedAmountPayload = new PayOsWebhookPayload(
                "00", "success",
                Map.of("orderCode", orderCode, "amount", 99_000), // amount mismatch
                "valid_signature"
        );
        when(payOsClient.verifyWebhook(tamperedAmountPayload)).thenReturn(true);

        assertThatThrownBy(() -> subscriptionService.handlePayOsWebhook(tamperedAmountPayload))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_SIGNATURE_INVALID);

        assertThat(tx.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("AC-13.6: Should retain existing subscription and mark transaction CANCELLED when payment fails")
    void shouldRetainExistingSubscriptionWhenPaymentCancelled() {
        long orderCode = 123456789L;
        PaymentTransactionEntity tx = new PaymentTransactionEntity(1L, String.valueOf(orderCode), 49_000);
        when(paymentTransactionRepository.findByOrderCode(String.valueOf(orderCode))).thenReturn(Optional.of(tx));

        PayOsWebhookPayload failedPayload = new PayOsWebhookPayload(
                "01", "cancelled",
                Map.of("orderCode", orderCode, "amount", 49_000, "code", "01"),
                "valid_signature"
        );
        when(payOsClient.verifyWebhook(failedPayload)).thenReturn(true);

        subscriptionService.handlePayOsWebhook(failedPayload);

        assertThat(tx.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(subscriptionRepository, never()).save(any());
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
        assertThat(response.unlockedFeatures()).contains("AI lập thực đơn tuần 7 ngày");
    }

    @Test
    @DisplayName("Should successfully create checkout and return payOS payment link")
    void shouldCreateCheckoutSuccessfully() {
        Long userId = 1L;
        mockActiveUser(userId);

        when(payOsClient.createPaymentLink(anyLong(), anyInt(), anyString()))
                .thenReturn(new CreatePaymentLinkResponse("00", "success",
                        new PayOsPaymentData(123456L, 49_000, "Nang cap PLUS", "https://pay.payos.vn/123", "qr_123", "plink_1", "PENDING"),
                        "sig_123"));

        CheckoutResponse response = subscriptionService.createCheckout(userId, SubscriptionTier.PLUS);

        assertThat(response).isNotNull();
        assertThat(response.amountVnd()).isEqualTo(49_000);
        assertThat(response.tier()).isEqualTo("PLUS");
        assertThat(response.checkoutUrl()).isEqualTo("https://pay.payos.vn/123");
        verify(paymentTransactionRepository).save(any(PaymentTransactionEntity.class));
    }

    private void mockActiveUser(Long userId) {
        User user = org.mockito.Mockito.mock(User.class);
        org.mockito.Mockito.lenient().when(user.getId()).thenReturn(userId);
        org.mockito.Mockito.lenient().when(user.getAccountStatus()).thenReturn(AccountStatus.ACTIVE);
        org.mockito.Mockito.lenient().when(user.getRole()).thenReturn(Role.CUSTOMER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }
}
