package tech.mamxanh.subscription.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/** Administrative read model; timestamps are UTC, and a null tier means unknown historical pricing. */
public record AdminTransactionResponse(long id, String orderCode, long userId, String customerName,
        String customerEmail,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}) String tier,
        int amountVnd, String status, Instant createdAt,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}, format = "date-time") Instant paidAt,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"integer", "null"}, format = "int64") Long subscriptionId,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}, format = "date-time") Instant subscriptionStartsAt,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}, format = "date-time") Instant subscriptionEndsAt,
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}) String subscriptionStatus,
        tech.mamxanh.subscription.entity.PaymentTransactionEntity.ProductType productType,
        BigDecimal creditAmount, BigDecimal creditsPer1000VndSnapshot, Instant expiresAt) {
    public AdminTransactionResponse(long id, String orderCode, long userId, String customerName,
            String customerEmail, String tier, int amountVnd, String status, Instant createdAt,
            Instant paidAt, Long subscriptionId, Instant subscriptionStartsAt,
            Instant subscriptionEndsAt, String subscriptionStatus) {
        this(id, orderCode, userId, customerName, customerEmail, tier, amountVnd, status, createdAt,
                paidAt, subscriptionId, subscriptionStartsAt, subscriptionEndsAt, subscriptionStatus,
                tech.mamxanh.subscription.entity.PaymentTransactionEntity.ProductType.SUBSCRIPTION, null, null, null);
    }
    @Override public String toString() { return "AdminTransactionResponse[redacted]"; }
}
