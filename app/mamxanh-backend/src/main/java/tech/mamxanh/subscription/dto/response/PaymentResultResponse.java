package tech.mamxanh.subscription.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResultResponse(
        String orderCode, String tier, int amountVnd, String status,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}, format = "date-time")
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime paidAt, boolean activated, MySubscriptionResponse subscription,
        tech.mamxanh.subscription.entity.PaymentTransactionEntity.ProductType productType,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"number", "null"}) BigDecimal creditAmount,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"number", "null"}) BigDecimal creditsPer1000VndSnapshot) {
    public PaymentResultResponse(String orderCode, String tier, int amountVnd, String status,
            LocalDateTime paidAt, boolean activated, MySubscriptionResponse subscription) {
        this(orderCode, tier, amountVnd, status, paidAt, activated, subscription,
                tech.mamxanh.subscription.entity.PaymentTransactionEntity.ProductType.SUBSCRIPTION, null, null);
    }
    @Override
    public String toString() {
        return "PaymentResultResponse[redacted]";
    }
}
