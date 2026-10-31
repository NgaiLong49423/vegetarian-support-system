package tech.mamxanh.subscription.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.auth.entity.User;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.exception.*;
import tech.mamxanh.notification.service.NotificationService;
import tech.mamxanh.subscription.entity.*;
import tech.mamxanh.subscription.repository.AiCreditAdministrationRepository;
import tech.mamxanh.subscription.repository.*;

@ExtendWith(MockitoExtension.class)
class PaymentSettlementServiceTest {
    @Mock PaymentTransactionRepository payments;
    @Mock SubscriptionRepository subscriptions;
    @Mock UserRepository users;
    @Mock AiCreditAdministrationRepository aiCredit;
    @Mock NotificationService notifications;
    final Clock clock = Clock.fixed(Instant.parse("2026-10-17T00:00:00Z"), ZoneOffset.UTC);
    PaymentSettlementService service;
    PaymentTransactionEntity payment;
    @BeforeEach void setup() {
        service = new PaymentSettlementService(payments, subscriptions, users, clock, aiCredit, notifications);
        payment = new PaymentTransactionEntity(1L, "123", 99000);
    }
    void existingOrder() {
        when(payments.findUserIdByOrderCode("123")).thenReturn(Optional.of(1L));
        when(users.findByIdForUpdate(1L)).thenReturn(Optional.of(mock(User.class)));
        when(payments.findByOrderCodeForUpdate("123")).thenReturn(Optional.of(payment));
    }
    @Test void activatesProAndExpiresPreviousPlanBeforeInserting() {
        existingOrder();
        var now = LocalDateTime.now(clock);
        var old = new SubscriptionEntity(1L, SubscriptionTier.PLUS, now.minusDays(1), now.plusDays(29));
        when(subscriptions.findByUserIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(Optional.of(old));
        when(subscriptions.saveAndFlush(any())).thenAnswer(call -> {
            SubscriptionEntity sub = call.getArgument(0); if (sub != old) sub.setId(7L); return sub;
        });
        service.settle("123", 99000, PaymentStatus.PAID);
        assertThat(old.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        var captor = ArgumentCaptor.forClass(SubscriptionEntity.class);
        verify(subscriptions, times(2)).saveAndFlush(captor.capture());
        assertThat(captor.getAllValues().get(0)).isSameAs(old);
        var activated = captor.getAllValues().get(1);
        assertThat(activated.getTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(activated.getEndsAt()).isEqualTo(now.plusDays(30));
        assertThat(payment.getSubscriptionId()).isEqualTo(7L);
        assertThat(payment.getPaidAt()).isEqualTo(now);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        var order = inOrder(users, payments);
        order.verify(users).findByIdForUpdate(1L);
        order.verify(payments).findByOrderCodeForUpdate("123");
    }
    @Test void activatesConfiguredTierAndQuotaSnapshotFromTheOrder() {
        payment = new PaymentTransactionEntity(1L, "123", 123456);
        payment.setSubscriptionTier(SubscriptionTier.PLUS);
        payment.setTokenQuotaSnapshot(345_678L);
        existingOrder();
        when(subscriptions.saveAndFlush(any())).thenAnswer(call -> {
            SubscriptionEntity sub = call.getArgument(0); sub.setId(8L); return sub;
        });

        service.settle("123", 123456, PaymentStatus.PAID);

        var activated = org.mockito.ArgumentCaptor.forClass(SubscriptionEntity.class);
        verify(subscriptions).saveAndFlush(activated.capture());
        assertThat(activated.getValue().getTier()).isEqualTo(SubscriptionTier.PLUS);
        assertThat(activated.getValue().getTokenQuota()).isEqualTo(345_678L);
    }
    @Test void duplicateDoesNotGrantAgainOrOverwritePaidAt() {
        existingOrder(); payment.setStatus(PaymentStatus.PAID); payment.setSubscriptionId(7L);
        var paidAt = LocalDateTime.now(clock).minusHours(1); payment.setPaidAt(paidAt);
        service.settle("123", 99000, PaymentStatus.PAID);
        service.settle("123", 99000, PaymentStatus.CANCELLED);
        verifyNoInteractions(subscriptions);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isEqualTo(paidAt);
    }
    @Test void wrongAmountNeverChangesPlanOrOrder() {
        existingOrder();
        assertThatThrownBy(() -> service.settle("123", 49000, PaymentStatus.PAID))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_SIGNATURE_INVALID);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verifyNoInteractions(subscriptions);
    }
    @Test void cancellationKeepsExistingEntitlement() {
        existingOrder(); service.settle("123", 99000, PaymentStatus.CANCELLED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verifyNoInteractions(subscriptions);
    }
    @Test void unknownSignedProbeDoesNotCreateAnything() {
        service.settle("123", 99000, PaymentStatus.PAID);
        verifyNoInteractions(users, subscriptions);
        verify(payments, never()).save(any());
    }
    @Test void rejectsUnsupportedPrice() {
        existingOrder(); payment.setAmountVnd(1);
        assertThatThrownBy(() -> service.settle("123", 1, PaymentStatus.PAID)).isInstanceOf(AppException.class);
        verifyNoInteractions(subscriptions);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test void verifiedCreditTopUpUsesOrderSnapshotAndDuplicateCallbackCreditsOnce() {
        existingOrder();
        var now = LocalDateTime.now(clock);
        payment.setAmountVnd(10_000);
        payment.setProductType(PaymentTransactionEntity.ProductType.AI_CREDIT);
        payment.setCreditAmountSnapshot(new BigDecimal("10000"));
        payment.setCreditsPer1000VndSnapshot(new BigDecimal("1000"));
        payment.setExpiresAt(now.plusMinutes(15));

        service.settle("123", 10_000, PaymentStatus.PAID, now.plusMinutes(1));
        service.settle("123", 10_000, PaymentStatus.PAID, now.plusMinutes(1));

        verify(aiCredit, times(1)).topUp(1L, "123", new BigDecimal("10000"), now);
        verify(payments).save(payment);
        verifyNoInteractions(subscriptions, notifications);
        assertThat(payment.getProductType()).isEqualTo(PaymentTransactionEntity.ProductType.AI_CREDIT);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isEqualTo(now.plusMinutes(1));
    }

    @Test void lateCreditPaymentRequiresReconciliationWithoutCreditingWallet() {
        existingOrder();
        var now = LocalDateTime.now(clock);
        payment.setAmountVnd(10_000);
        payment.setProductType(PaymentTransactionEntity.ProductType.AI_CREDIT);
        payment.setCreditAmountSnapshot(new BigDecimal("10000"));
        payment.setExpiresAt(now.minusSeconds(1));

        service.settle("123", 10_000, PaymentStatus.PAID, now);
        service.settle("123", 10_000, PaymentStatus.PAID, now);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.NEEDS_RECONCILIATION);
        assertThat(payment.getPaidAt()).isEqualTo(now);
        verify(aiCredit, never()).topUp(anyLong(), anyString(), any(), any());
        verify(notifications, times(1)).aiCreditTopUpNeedsReconciliation(1L, "123", 10_000);
        verifyNoInteractions(subscriptions);
    }

    @Test void delayedWebhookCreditsWhenProviderConfirmsPaymentBeforeExpiry() {
        existingOrder();
        var now = LocalDateTime.now(clock);
        payment.setAmountVnd(10_000);
        var expiresAt = now.minusMinutes(1);
        var paidAt = expiresAt.minusSeconds(1);
        payment.setProductType(PaymentTransactionEntity.ProductType.AI_CREDIT);
        payment.setCreditAmountSnapshot(new BigDecimal("2500.5"));
        payment.setExpiresAt(expiresAt);

        service.settle("123", 10_000, PaymentStatus.PAID, paidAt);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isEqualTo(paidAt);
        verify(aiCredit).topUp(1L, "123", new BigDecimal("2500.5"), now);
        verifyNoInteractions(subscriptions, notifications);
    }

    @Test void cancelledCreditOrderDoesNotChangeWalletOrSubscription() {
        existingOrder();
        payment.setAmountVnd(10_000);
        payment.setProductType(PaymentTransactionEntity.ProductType.AI_CREDIT);
        payment.setCreditAmountSnapshot(new BigDecimal("10000"));

        service.settle("123", 10_000, PaymentStatus.CANCELLED);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verifyNoInteractions(aiCredit, subscriptions, notifications);
    }

    @Test void reconciliationCanCreditAfterProviderConfirmsOnTimePayment() {
        existingOrder();
        var now = LocalDateTime.now(clock);
        payment.setAmountVnd(10_000);
        var expiresAt = now.minusMinutes(1);
        var paidAt = expiresAt.minusSeconds(1);
        payment.setProductType(PaymentTransactionEntity.ProductType.AI_CREDIT);
        payment.setCreditAmountSnapshot(new BigDecimal("10000"));
        payment.setExpiresAt(expiresAt);
        payment.setStatus(PaymentStatus.NEEDS_RECONCILIATION);

        service.settle("123", 10_000, PaymentStatus.PAID, paidAt);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(aiCredit).topUp(1L, "123", new BigDecimal("10000"), now);
        verify(notifications).aiCreditTopUpReconciled(1L, new BigDecimal("10000"));
        verifyNoInteractions(subscriptions);
    }
}
