package tech.mamxanh.subscription.entity;

/**
 * Status lifecycle of a payment transaction in the PAYMENT_TRANSACTION table (FR-13).
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    CANCELLED
}
