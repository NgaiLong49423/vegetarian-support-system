package tech.mamxanh.integration.payos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreatePaymentLinkRequest(
        long orderCode,
        int amount,
        String description,
        String cancelUrl,
        String returnUrl,
        String signature
) {
}
