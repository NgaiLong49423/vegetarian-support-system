package tech.mamxanh.integration.payos.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public record PayOsPaymentInfo(long orderCode, int amount, int amountPaid, String status,
        LocalDateTime transactionAtUtc) {
    public PayOsPaymentInfo(long orderCode, int amount, int amountPaid, String status) {
        this(orderCode, amount, amountPaid, status, null);
    }

    public static LocalDateTime parseTransactionDateTime(Object raw) {
        if (raw == null) return null;
        String value = String.valueOf(raw);
        try {
            return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (java.time.format.DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .atZone(ZoneId.of("Asia/Ho_Chi_Minh")).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
            } catch (java.time.format.DateTimeParseException invalid) {
                return null;
            }
        }
    }
}
