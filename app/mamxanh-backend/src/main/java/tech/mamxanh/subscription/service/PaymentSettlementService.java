package tech.mamxanh.subscription.service;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.subscription.entity.PaymentStatus;
import tech.mamxanh.subscription.entity.PaymentTransactionEntity;
import tech.mamxanh.subscription.entity.SubscriptionEntity;
import tech.mamxanh.subscription.entity.SubscriptionStatus;
import tech.mamxanh.subscription.entity.SubscriptionTier;
import tech.mamxanh.subscription.repository.PaymentTransactionRepository;
import tech.mamxanh.subscription.repository.SubscriptionRepository;
import tech.mamxanh.subscription.repository.AiCreditAdministrationRepository;
import tech.mamxanh.notification.service.NotificationService;

/** Database-only settlement, shared by verified webhooks and server-to-server reconciliation. */
@Service
public class PaymentSettlementService {
    private final PaymentTransactionRepository payments;
    private final SubscriptionRepository subscriptions;
    private final UserRepository users;
    private final Clock clock;
    private final AiCreditAdministrationRepository aiCredit;
    private final NotificationService notifications;

    @Autowired
    public PaymentSettlementService(PaymentTransactionRepository payments, SubscriptionRepository subscriptions,
                                    UserRepository users, Clock clock, AiCreditAdministrationRepository aiCredit,
                                    NotificationService notifications) {
        this.payments = payments;
        this.subscriptions = subscriptions;
        this.users = users;
        this.clock = clock;
        this.aiCredit = aiCredit;
        this.notifications = notifications;
    }

    public PaymentSettlementService(PaymentTransactionRepository payments, SubscriptionRepository subscriptions,
                                    UserRepository users, Clock clock) {
        this(payments, subscriptions, users, clock, null, null);
    }

    @Transactional
    public void settle(String orderCode, int amount, PaymentStatus status) {
        settle(orderCode, amount, status, null);
    }

    @Transactional
    public void settle(String orderCode, int amount, PaymentStatus status, LocalDateTime transactionAtUtc) {
        // A signed provider probe/unknown order must never grant any entitlement.
        Long userId = payments.findUserIdByOrderCode(orderCode).orElse(null);
        if (userId == null) return;
        // Account first, then order: serializes different orders and duplicate deliveries for one user.
        users.findByIdForUpdate(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        PaymentTransactionEntity payment = payments.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));
        if (amount != payment.getAmountVnd()) throw new AppException(ErrorCode.PAYMENT_SIGNATURE_INVALID);
        if (payment.getStatus() == PaymentStatus.PAID) return;
        boolean wasReconciliationRequired = payment.getStatus() == PaymentStatus.NEEDS_RECONCILIATION;
        if (status != PaymentStatus.PAID) {
            if (status == PaymentStatus.CANCELLED || status == PaymentStatus.FAILED) payment.setStatus(status);
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (payment.getProductType() == PaymentTransactionEntity.ProductType.AI_CREDIT) {
            LocalDateTime paidAt = transactionAtUtc == null ? now : transactionAtUtc;
            if (payment.getExpiresAt() == null || !paidAt.isBefore(payment.getExpiresAt())) {
                if (wasReconciliationRequired) return;
                payment.setPaidAt(paidAt);
                payment.setStatus(PaymentStatus.NEEDS_RECONCILIATION);
                payments.save(payment);
                if (notifications != null) notifications.aiCreditTopUpNeedsReconciliation(userId,
                        payment.getOrderCode(), payment.getAmountVnd());
                return;
            }
            if (aiCredit == null || payment.getCreditAmountSnapshot() == null) {
                throw new IllegalStateException("AI Credit top-up settlement dependencies or snapshot are missing");
            }
            aiCredit.topUp(userId, payment.getOrderCode(), payment.getCreditAmountSnapshot(), now);
            payment.setPaidAt(paidAt);
            payment.setStatus(PaymentStatus.PAID);
            payments.save(payment);
            if (wasReconciliationRequired && notifications != null) {
                notifications.aiCreditTopUpReconciled(userId, payment.getCreditAmountSnapshot());
            }
            return;
        }
        SubscriptionTier tier = payment.getSubscriptionTier() == null
                ? tierForAmount(amount) : payment.getSubscriptionTier();
        long tokenQuota = payment.getTokenQuotaSnapshot() == null
                ? tier.tokenQuota() : payment.getTokenQuotaSnapshot();
        subscriptions.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE).ifPresent(existing -> {
            existing.setStatus(SubscriptionStatus.EXPIRED);
            existing.setUpdatedAt(now);
            // Flush expiry before inserting the new row to satisfy the filtered unique ACTIVE index.
            subscriptions.saveAndFlush(existing);
        });
        SubscriptionEntity subscription = new SubscriptionEntity(userId, tier, now, now.plusDays(30));
        subscription.setTokenQuota(tokenQuota);
        subscriptions.saveAndFlush(subscription);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(now);
        payment.setSubscriptionId(subscription.getId());
        payments.save(payment);
    }

    public static SubscriptionTier tierForAmount(int amount) {
        for (SubscriptionTier tier : SubscriptionTier.values()) {
            if (tier.priceVnd() == amount) return tier;
        }
        throw new AppException(ErrorCode.PAYMENT_RESPONSE_INVALID);
    }
}
