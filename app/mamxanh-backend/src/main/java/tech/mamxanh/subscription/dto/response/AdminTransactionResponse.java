package tech.mamxanh.subscription.dto.response;

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
        @io.swagger.v3.oas.annotations.media.Schema(types = {"string", "null"}) String subscriptionStatus) {
    @Override public String toString() { return "AdminTransactionResponse[redacted]"; }
}
