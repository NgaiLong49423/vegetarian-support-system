package tech.mamxanh.subscription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps the {@code PAYMENT_TRANSACTION} table tracking payOS payment requests and webhooks (FR-13).
 */
@Entity
@Table(name = "\"PAYMENT_TRANSACTION\"")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransactionEntity {

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
        this.createdAt = LocalDateTime.now();
    }
}
