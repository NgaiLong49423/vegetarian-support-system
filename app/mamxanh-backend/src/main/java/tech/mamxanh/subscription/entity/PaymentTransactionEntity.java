package tech.mamxanh.subscription.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Maps the {@code PAYMENT_TRANSACTION} table tracking payOS payment requests and webhooks (FR-13).
 */
@Entity
@Table(name = "\"PAYMENT_TRANSACTION\"")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransactionEntity {

    public enum ProductType { SUBSCRIPTION, AI_CREDIT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_transaction_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "order_code", nullable = false, length = 100, unique = true)
    private String orderCode;

    @Column(name = "amount_vnd", nullable = false)
    private int amountVnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", length = 10)
    private SubscriptionTier subscriptionTier;

    @Column(name = "token_quota_snapshot")
    private Long tokenQuotaSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 20)
    private ProductType productType = ProductType.SUBSCRIPTION;

    @Column(name = "credit_amount_snapshot", precision = 38, scale = 9)
    private BigDecimal creditAmountSnapshot;

    @Column(name = "credits_per_1000_vnd_snapshot", precision = 19, scale = 6)
    private BigDecimal creditsPer1000VndSnapshot;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    public PaymentTransactionEntity(Long userId, String orderCode, int amountVnd) {
        this.userId = userId;
        this.orderCode = orderCode;
        this.amountVnd = amountVnd;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now(java.time.Clock.systemUTC());
    }
}
