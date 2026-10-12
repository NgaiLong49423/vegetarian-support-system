package tech.mamxanh.integration.payos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreatePaymentLinkResponse(
        String code,
        String desc,
        PayOsPaymentData data,
        String signature
) {
    public boolean isSuccess() {
        return "00".equals(code);
    }
}
