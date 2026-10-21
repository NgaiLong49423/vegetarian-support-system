package tech.mamxanh.subscription.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.common.exception.*;
import tech.mamxanh.integration.payos.PayOsClient;
import tech.mamxanh.integration.payos.dto.PayOsPaymentInfo;
import tech.mamxanh.subscription.dto.response.MySubscriptionResponse;
import tech.mamxanh.subscription.entity.*;
import tech.mamxanh.subscription.repository.*;

@ExtendWith(MockitoExtension.class)
class PaymentResultServiceTest {
    @Mock PaymentTransactionRepository payments;
    @Mock SubscriptionRepository subscriptions;
    @Mock SubscriptionService subscriptionService;
    @Mock PaymentSettlementService settlement;
    @Mock PayOsClient payOs;
    final Clock clock = Clock.fixed(Instant.parse("2026-10-17T00:00:00Z"), ZoneOffset.UTC);
    PaymentResultService service;
    PaymentTransactionEntity payment;
    @BeforeEach void setup() {
        service = new PaymentResultService(payments, subscriptions, subscriptionService, settlement, payOs, clock);
        payment = new PaymentTransactionEntity(1L, "123", 99000);
        when(subscriptionService.getMySubscription(1L)).thenReturn(MySubscriptionResponse.free());
    }
    void ownOrder() { when(payments.findByUserIdAndOrderCode(1L, "123")).thenReturn(Optional.of(payment)); }
    @Test void anotherUsersOrderIsNotDisclosedOrQueriedAtProvider() {
        assertThatThrownBy(() -> service.getResult(1L, "123")).isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND);
        verifyNoInteractions(payOs, settlement);
    }
    @Test void reconcilesPaidOrderAndReportsActivationFromLinkedSubscription() {
        ownOrder();
        when(payOs.getPaymentInfo(123)).thenReturn(new PayOsPaymentInfo(123, 99000, 99000, "PAID"));
        doAnswer(call -> { payment.setStatus(PaymentStatus.PAID); payment.setSubscriptionId(7L); return null; })
                .when(settlement).settle("123", 99000, PaymentStatus.PAID);
        var now = LocalDateTime.now(clock);
        when(subscriptions.findById(7L)).thenReturn(Optional.of(new SubscriptionEntity(1L, SubscriptionTier.PRO, now, now.plusDays(30))));
        when(subscriptionService.getMySubscription(1L)).thenReturn(new MySubscriptionResponse("PRO", "ACTIVE", true, now, now.plusDays(30), java.util.List.of()));
        var result = service.getResult(1L, "123");
        assertThat(result.status()).isEqualTo("PAID");
        assertThat(result.activated()).isTrue();
        assertThat(result.tier()).isEqualTo("PRO");
    }
    @Test void pendingNeverActivates() {
        ownOrder(); when(payOs.getPaymentInfo(123)).thenReturn(new PayOsPaymentInfo(123, 99000, 0, "PENDING"));
        assertThat(service.getResult(1L, "123").activated()).isFalse();
        verifyNoInteractions(settlement, subscriptions);
    }
    @Test void paidOrderIsNotQueriedAgainAndExpiredEntitlementIsNotClaimedActive() {
        ownOrder(); payment.setStatus(PaymentStatus.PAID); payment.setSubscriptionId(7L);
        var now = LocalDateTime.now(clock);
        when(subscriptions.findById(7L)).thenReturn(Optional.of(new SubscriptionEntity(1L, SubscriptionTier.PRO, now.minusDays(31), now.minusDays(1))));
        assertThat(service.getResult(1L, "123").activated()).isFalse();
        verifyNoInteractions(payOs, settlement);
    }
    @Test void providerMismatchOrUnderpaymentCannotActivate() {
        ownOrder();
        for (var info : java.util.List.of(new PayOsPaymentInfo(999, 99000, 99000, "PAID"),
                new PayOsPaymentInfo(123, 49000, 99000, "PAID"), new PayOsPaymentInfo(123, 99000, 1, "PAID"), new PayOsPaymentInfo(123, 99000, 99001, "PAID"))) {
            when(payOs.getPaymentInfo(123)).thenReturn(info);
            assertThatThrownBy(() -> service.getResult(1L, "123")).isInstanceOf(AppException.class);
        }
        verifyNoInteractions(settlement);
    }
    @Test void providerUnavailableLeavesPendingUnchanged() {
        ownOrder(); when(payOs.getPaymentInfo(123)).thenThrow(new AppException(ErrorCode.PAYMENT_UNAVAILABLE));
        assertThatThrownBy(() -> service.getResult(1L, "123")).isInstanceOf(AppException.class);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        verifyNoInteractions(settlement);
    }
    @Test void providerCancellationIsRecordedWithoutGrantingPlan() {
        ownOrder(); when(payOs.getPaymentInfo(123)).thenReturn(new PayOsPaymentInfo(123, 99000, 0, "CANCELLED"));
        service.getResult(1L, "123"); verify(settlement).settle("123", 99000, PaymentStatus.CANCELLED);
    }
}
