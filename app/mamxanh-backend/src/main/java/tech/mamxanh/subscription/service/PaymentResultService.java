package tech.mamxanh.subscription.service;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.payos.PayOsClient;
import tech.mamxanh.integration.payos.dto.PayOsPaymentInfo;
import tech.mamxanh.subscription.dto.response.PaymentResultResponse;
import tech.mamxanh.subscription.entity.PaymentStatus;
import tech.mamxanh.subscription.entity.PaymentTransactionEntity;
import tech.mamxanh.subscription.repository.PaymentTransactionRepository;
import tech.mamxanh.subscription.repository.SubscriptionRepository;

@Service
public class PaymentResultService {
    private final PaymentTransactionRepository payments;
    private final SubscriptionRepository subscriptions;
    private final SubscriptionService subscriptionService;
    private final PaymentSettlementService settlement;
    private final PayOsClient payOs;
    private final Clock clock;

    public PaymentResultService(PaymentTransactionRepository payments, SubscriptionRepository subscriptions,
            SubscriptionService subscriptionService, PaymentSettlementService settlement, PayOsClient payOs, Clock clock) {
        this.payments = payments;
        this.subscriptions = subscriptions;
        this.subscriptionService = subscriptionService;
        this.settlement = settlement;
        this.payOs = payOs;
        this.clock = clock;
    }

    // Deliberately no outer transaction: provider I/O must not hold SQL locks or stale managed entities.
    public PaymentResultResponse getResult(Long userId, String orderCode) {
        subscriptionService.getMySubscription(userId); // Enforce active Member before reading any order.
        PaymentTransactionEntity payment = ownPayment(userId, orderCode);
        if (payment.getStatus() == PaymentStatus.PENDING || payment.getStatus() == PaymentStatus.NEEDS_RECONCILIATION) {
            PayOsPaymentInfo info = payOs.getPaymentInfo(Long.parseLong(orderCode));
            if (info.orderCode() != Long.parseLong(orderCode) || info.amount() != payment.getAmountVnd()) {
                throw new AppException(ErrorCode.PAYMENT_RESPONSE_INVALID);
            }
            if ("PAID".equals(info.status())) {
                if (info.amountPaid() != payment.getAmountVnd()) throw new AppException(ErrorCode.PAYMENT_RESPONSE_INVALID);
                settlement.settle(orderCode, info.amount(), PaymentStatus.PAID, info.transactionAtUtc());
            } else if (payment.getStatus() == PaymentStatus.PENDING
                    && ("CANCELLED".equals(info.status()) || "EXPIRED".equals(info.status()))) {
                settlement.settle(orderCode, info.amount(), PaymentStatus.CANCELLED, null);
            }
            payment = ownPayment(userId, orderCode);
        }
        var currentSubscription = subscriptionService.getMySubscription(userId);
        if (payment.getProductType() == PaymentTransactionEntity.ProductType.AI_CREDIT) {
            return new PaymentResultResponse(payment.getOrderCode(), "AI_CREDIT", payment.getAmountVnd(),
                    payment.getStatus().name(), payment.getPaidAt(), payment.getStatus() == PaymentStatus.PAID,
                    currentSubscription, PaymentTransactionEntity.ProductType.AI_CREDIT,
                    payment.getCreditAmountSnapshot(), payment.getCreditsPer1000VndSnapshot());
        }
        var purchasedTier = payment.getSubscriptionTier() == null
                ? PaymentSettlementService.tierForAmount(payment.getAmountVnd()) : payment.getSubscriptionTier();
        boolean activated = payment.getStatus() == PaymentStatus.PAID && payment.getSubscriptionId() != null
                && subscriptions.findById(payment.getSubscriptionId())
                .filter(s -> s.getUserId().equals(userId) && s.getTier() == purchasedTier
                        && s.isActiveAt(LocalDateTime.now(clock))).isPresent()
                && currentSubscription.active() && purchasedTier.name().equals(currentSubscription.tier());
        return new PaymentResultResponse(payment.getOrderCode(), purchasedTier.name(),
                payment.getAmountVnd(), payment.getStatus().name(), payment.getPaidAt(), activated,
                currentSubscription, PaymentTransactionEntity.ProductType.SUBSCRIPTION, null, null);
    }

    private PaymentTransactionEntity ownPayment(Long userId, String orderCode) {
        return payments.findByUserIdAndOrderCode(userId, orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_TRANSACTION_NOT_FOUND));
    }
}
