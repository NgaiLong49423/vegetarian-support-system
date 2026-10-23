package tech.mamxanh.subscription.dto.response;

import java.time.LocalDateTime;

public record TransactionHistoryResponse(
        Long paymentTransactionId,
        String orderCode,
        int amountVnd,
        String status,
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime createdAt,
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime paidAt,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}) String tier,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}, format = "date-time", description = "UTC expiry of the subscription purchased by this transaction, not the current subscription.")
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime endsAt
) {
    public TransactionHistoryResponse(Long paymentTransactionId, String orderCode, int amountVnd,
            String status, LocalDateTime createdAt, LocalDateTime paidAt) {
        this(paymentTransactionId, orderCode, amountVnd, status, createdAt, paidAt, null, null);
    }
    @Override
    public String toString() {
        return "TransactionHistoryResponse[redacted]";
    }
}
