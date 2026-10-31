package tech.mamxanh.subscription.dto.response;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record TransactionHistoryResponse(
        Long paymentTransactionId,
        String orderCode,
        int amountVnd,
        String status,
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime createdAt,
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime paidAt,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}) String tier,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}, format = "date-time", description = "UTC expiry of the subscription purchased by this transaction, not the current subscription.")
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime endsAt,
        tech.mamxanh.subscription.entity.PaymentTransactionEntity.ProductType productType,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"number", "null"}, description = "Credit quantity captured at checkout for AI_CREDIT transactions.") BigDecimal creditAmount,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}, format = "date-time")
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime expiresAt,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"number", "null"}) BigDecimal creditsPer1000VndSnapshot
) {
    public TransactionHistoryResponse(Long paymentTransactionId, String orderCode, int amountVnd,
            String status, LocalDateTime createdAt, LocalDateTime paidAt) {
        this(paymentTransactionId, orderCode, amountVnd, status, createdAt, paidAt, null, null,
                tech.mamxanh.subscription.entity.PaymentTransactionEntity.ProductType.SUBSCRIPTION, null, null, null);
    }
    public TransactionHistoryResponse(Long paymentTransactionId, String orderCode, int amountVnd,
            String status, LocalDateTime createdAt, LocalDateTime paidAt, String tier, LocalDateTime endsAt) {
        this(paymentTransactionId, orderCode, amountVnd, status, createdAt, paidAt, tier, endsAt,
                tech.mamxanh.subscription.entity.PaymentTransactionEntity.ProductType.SUBSCRIPTION, null, null, null);
    }
    @Override
    public String toString() {
        return "TransactionHistoryResponse[redacted]";
    }
}
