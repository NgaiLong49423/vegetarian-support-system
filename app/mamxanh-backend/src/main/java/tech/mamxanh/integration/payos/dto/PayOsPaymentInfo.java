package tech.mamxanh.integration.payos.dto;

public record PayOsPaymentInfo(long orderCode, int amount, int amountPaid, String status) {
}
