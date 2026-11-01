package tech.mamxanh.subscription.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreditCheckoutResponse(long orderCode, int amountVnd, BigDecimal creditAmount,
        BigDecimal creditsPer1000VndSnapshot,
        @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") LocalDateTime expiresAt,
        String checkoutUrl, String qrCode) {
    @Override public String toString() { return "CreditCheckoutResponse[redacted]"; }
}
