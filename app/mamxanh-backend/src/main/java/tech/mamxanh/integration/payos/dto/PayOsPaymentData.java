package tech.mamxanh.integration.payos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PayOsPaymentData(
        Long orderCode,
        Integer amount,
        String description,
        String checkoutUrl,
        String qrCode,
        String paymentLinkId,
        String status
) {
}
