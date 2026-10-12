package tech.mamxanh.subscription.dto.response;

import java.time.LocalDateTime;

public record TransactionHistoryResponse(
        Long paymentTransactionId,
        String orderCode,
        int amountVnd,
        String status,
        LocalDateTime createdAt,
        LocalDateTime paidAt
) {
}
