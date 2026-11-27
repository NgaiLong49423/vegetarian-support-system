package tech.mamxanh.subscription.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.payos.PayOsClient;
import tech.mamxanh.subscription.entity.PaymentStatus;
import tech.mamxanh.subscription.entity.PaymentTransactionEntity;
import tech.mamxanh.subscription.repository.PaymentTransactionRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PendingPaymentReconciliationJobTest {

    @Mock
    private PaymentTransactionRepository payments;
    @Mock
    private PaymentResultService paymentResults;
    @Mock
    private PayOsClient payOsClient;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-17T00:00:00Z"), ZoneOffset.UTC);
    private PendingPaymentReconciliationJob job;

    @BeforeEach
    void setUp() {
        job = new PendingPaymentReconciliationJob(payments, paymentResults, payOsClient, clock);
    }

    @Test
    void skipsQuietlyWhenPayOsNotConfigured() {
        doThrow(new AppException(ErrorCode.PAYMENT_UNAVAILABLE)).when(payOsClient).validateCheckoutConfiguration();

        job.reconcilePendingOrders();

        verifyNoInteractions(payments, paymentResults);
    }

    @Test
    void reconcilesPendingOrdersAndContinuesOnIndividualFailure() {
        var nowUtc = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        var first = new PaymentTransactionEntity(1L, "111", 199_000);
        var second = new PaymentTransactionEntity(2L, "222", 99_000);

        when(payments.findTop50ByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                eq(PaymentStatus.PENDING), eq(nowUtc.minusDays(7)), eq(nowUtc.minusSeconds(30))))
                .thenReturn(List.of(first, second));
        doThrow(new AppException(ErrorCode.PAYMENT_UNAVAILABLE)).when(paymentResults).reconcileWithProvider(first);

        job.reconcilePendingOrders();

        verify(paymentResults).reconcileWithProvider(first);
        verify(paymentResults).reconcileWithProvider(second);
    }
}
