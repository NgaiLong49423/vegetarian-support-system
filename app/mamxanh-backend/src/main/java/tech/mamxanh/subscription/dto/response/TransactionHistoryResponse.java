package tech.mamxanh.subscription.dto.response;

import java.time.LocalDateTime;

public record TransactionHistoryResponse(
        Long paymentTransactionId,
        String orderCode,
        int amountVnd,
        String status,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}) String tier
) {
    public TransactionHistoryResponse(Long paymentTransactionId, String orderCode, int amountVnd,
            String status, LocalDateTime createdAt, LocalDateTime paidAt) {
        this(paymentTransactionId, orderCode, amountVnd, status, createdAt, paidAt, null);
    }
    @Override
    public String toString() {
        return "TransactionHistoryResponse[redacted]";
    }
}
