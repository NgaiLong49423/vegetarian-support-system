package tech.mamxanh.integration.payos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tech.mamxanh.integration.payos.dto.CreatePaymentLinkResponse;
import tech.mamxanh.integration.payos.dto.PayOsWebhookPayload;

class DefaultPayOsClientTest {

    @Test
    @DisplayName("Should return fallback mock checkout URL when payOS is unconfigured")
    void shouldReturnFallbackMockWhenUnconfigured() {
        PayOsProperties unconfiguredProps = new PayOsProperties(
                "", "", "", "https://api-merchant.payos.vn",
                "http://localhost:5173/payment/success",
                "http://localhost:5173/payment/cancel"
        );

        DefaultPayOsClient client = new DefaultPayOsClient(unconfiguredProps, RestClient.builder().build());

        CreatePaymentLinkResponse response = client.createPaymentLink(123456L, 49000, "Goi PLUS");

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.data()).isNotNull();
        assertThat(response.data().orderCode()).isEqualTo(123456L);
        assertThat(response.data().amount()).isEqualTo(49000);
        assertThat(response.data().checkoutUrl()).contains("123456");
    }

    @Test
    @DisplayName("Should delegate webhook verification to signature utility")
    void shouldDelegateWebhookVerification() {
        String checksumKey = "key_secret_abc123";
        PayOsProperties configuredProps = new PayOsProperties(
                "client_1", "api_key_1", checksumKey,
                "https://api-merchant.payos.vn",
                "http://localhost:5173/payment/success",
                "http://localhost:5173/payment/cancel"
        );

        DefaultPayOsClient client = new DefaultPayOsClient(configuredProps, RestClient.builder().build());

        Map<String, Object> data = Map.of("orderCode", 9999L, "amount", 99000);
        String queryStr = PayOsSignatureUtil.convertObjToQueryStr(data);
        String validSig = PayOsSignatureUtil.hmacSha256(queryStr, checksumKey);

        PayOsWebhookPayload payload = new PayOsWebhookPayload("00", "success", data, validSig);

        assertThat(client.verifyWebhook(payload)).isTrue();

        PayOsWebhookPayload forgedPayload = new PayOsWebhookPayload("00", "success", data, "invalid_sig");
        assertThat(client.verifyWebhook(forgedPayload)).isFalse();
    }
}
