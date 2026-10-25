package tech.mamxanh.subscription.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tech.mamxanh.subscription.dto.response.*;

class SubscriptionTimeSerializationTest {
    @Test
    void allSubscriptionResponsesExposeUnambiguousUtcInstants() {
        var mapper = JsonMapper.builder().findAndAddModules().build();
        var created = LocalDateTime.of(2026, 10, 20, 5, 7);
        var paid = created.plusMinutes(1);
        var end = paid.plusDays(30);
        var history = new TransactionHistoryResponse(1L, "123", 49000, "PAID", created, paid, "PLUS", end);
        var json = mapper.readTree(mapper.writeValueAsString(history));
        assertThat(json.path("createdAt").asString()).isEqualTo("2026-10-20T05:07:00.000Z");
        assertThat(json.path("paidAt").asString()).isEqualTo("2026-10-20T05:08:00.000Z");
        assertThat(json.path("endsAt").asString()).isEqualTo("2026-11-19T05:08:00.000Z");
        var subscription = new MySubscriptionResponse("PLUS", "ACTIVE", true, paid, end, List.of());
        var result = new PaymentResultResponse("123", "PLUS", 49000, "PAID", paid, true, subscription);
        json = mapper.readTree(mapper.writeValueAsString(result));
        assertThat(json.path("paidAt").asString()).endsWith("Z");
        assertThat(json.path("subscription").path("startsAt").asString()).isEqualTo("2026-10-20T05:08:00.000Z");
        assertThat(json.path("subscription").path("endsAt").asString()).isEqualTo("2026-11-19T05:08:00.000Z");
    }
}
