package tech.mamxanh.integration.payos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PayOsWebhookPayload(
        String code,
        String desc,
        Map<String, Object> data,
        String signature
) {
    public Long getOrderCode() {
        if (data == null || !data.containsKey("orderCode")) {
            return null;
        }
        Object val = data.get("orderCode");
        if (val instanceof Number num) {
            return num.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(val));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Integer getAmount() {
        if (data == null || !data.containsKey("amount")) {
            return null;
        }
        Object val = data.get("amount");
        if (val instanceof Number num) {
            return num.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(val));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getPaymentLinkId() {
        if (data == null || !data.containsKey("paymentLinkId")) {
            return null;
        }
        return String.valueOf(data.get("paymentLinkId"));
    }

    public String getCode() {
        if (data == null || !data.containsKey("code")) {
            return code;
        }
        return String.valueOf(data.get("code"));
    }

    public boolean isPaymentSuccess() {
        return "00".equals(code) || ("00".equals(getCode()));
    }
}
