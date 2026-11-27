package tech.mamxanh.subscription.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.integration.payos.PayOsClient;
import tech.mamxanh.subscription.entity.PaymentStatus;
import tech.mamxanh.subscription.entity.PaymentTransactionEntity;
import tech.mamxanh.subscription.repository.PaymentTransactionRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Periodically reconciles recent PENDING payOS orders in case the webhook was missed or the user left checkout early.
 */
@Service
public class PendingPaymentReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(PendingPaymentReconciliationJob.class);

    private final PaymentTransactionRepository payments;
    private final PaymentResultService paymentResults;
    private final PayOsClient payOsClient;
    private final Clock clock;

    public PendingPaymentReconciliationJob(
            PaymentTransactionRepository payments,
            PaymentResultService paymentResults,
            PayOsClient payOsClient,
            Clock clock) {
        this.payments = payments;
        this.paymentResults = paymentResults;
        this.payOsClient = payOsClient;
        this.clock = clock;
    }

    /**
     * Reconciles recent PENDING transactions (older than 30 seconds, up to 7 days) against payOS without holding a DB transaction.
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void reconcilePendingOrders() {
        try {
            payOsClient.validateCheckoutConfiguration();
        } catch (AppException ex) {
            return;
        }

        LocalDateTime nowUtc = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        List<PaymentTransactionEntity> pendingOrders = payments
                .findTop50ByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
                        PaymentStatus.PENDING,
                        nowUtc.minusDays(7),
                        nowUtc.minusSeconds(30));

        for (PaymentTransactionEntity order : pendingOrders) {
            try {
                paymentResults.reconcileWithProvider(order);
            } catch (RuntimeException ex) {
                log.warn("Background reconciliation skipped for orderCode={} reason={}",
                        order.getOrderCode(), ex.getClass().getSimpleName());
            }
        }
    }
}
