package tech.mamxanh.subscription.service;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
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

/** Database-only settlement, shared by verified webhooks and server-to-server reconciliation. */
@Service
public class PaymentSettlementService {
    private final PaymentTransactionRepository payments;
    private final SubscriptionRepository subscriptions;
    private final UserRepository users;
    private final Clock clock;

    public PaymentSettlementService(PaymentTransactionRepository payments, SubscriptionRepository subscriptions,
                                    UserRepository users, Clock clock) {
        this.payments = payments;
        this.subscriptions = subscriptions;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public void settle(String orderCode, int amount, PaymentStatus status) {
        // A signed provider probe/unknown order must never grant any entitlement.
        Long userId = payments.findUserIdByOrderCode(orderCode).orElse(null);
        if (userId == null) return;
        // Account first, then order: serializes different orders and duplicate deliveries for one user.
        users.findByIdForUpdate(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        PaymentTransactionEntity payment = payments.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));
        if (amount != payment.getAmountVnd()) throw new AppException(ErrorCode.PAYMENT_SIGNATURE_INVALID);
        if (payment.getStatus() == PaymentStatus.PAID) return;
        if (status != PaymentStatus.PAID) {
            if (status == PaymentStatus.CANCELLED || status == PaymentStatus.FAILED) payment.setStatus(status);
            return;
        }
        SubscriptionTier tier = tierForAmount(amount);
        LocalDateTime now = LocalDateTime.now(clock);
        subscriptions.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE).ifPresent(existing -> {
            existing.setStatus(SubscriptionStatus.EXPIRED);
            existing.setUpdatedAt(now);
            // Flush expiry before inserting the new row to satisfy the filtered unique ACTIVE index.
            subscriptions.saveAndFlush(existing);
        });
        SubscriptionEntity subscription = subscriptions.saveAndFlush(
                new SubscriptionEntity(userId, tier, now, now.plusDays(30)));
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
