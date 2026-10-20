package tech.mamxanh.subscription.dto.response;

import java.time.LocalDateTime;

public record PaymentResultResponse(
        String orderCode, String tier, int amountVnd, String status,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}, format = "date-time")
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime paidAt, boolean activated, MySubscriptionResponse subscription) {
    @Override
    public String toString() {
        return "PaymentResultResponse[redacted]";
    }
}
