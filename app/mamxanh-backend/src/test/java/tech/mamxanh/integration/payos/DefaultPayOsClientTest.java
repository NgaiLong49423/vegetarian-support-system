package tech.mamxanh.integration.payos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
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
    @DisplayName("Should call payOS API endpoint when credentials are configured")
    void shouldCallPayOsApiWhenConfigured() {
        PayOsProperties configuredProps = new PayOsProperties(
                "client_1", "api_key_1", "checksum_key_1",
                "https://api-merchant.payos.vn",
                "http://localhost:5173/payment/success",
                "http://localhost:5173/payment/cancel"
        );

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("https://api-merchant.payos.vn/v2/payment-requests"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-client-id", "client_1"))
                .andExpect(header("x-api-key", "api_key_1"))
                .andRespond(withSuccess(
                        """
                        {"code":"00","desc":"success","data":{"orderCode":123456,"amount":49000,"description":"Thanh toan goi AI","checkoutUrl":"https://pay.payos.vn/web/123","qrCode":"qr123","paymentLinkId":"link123","status":"PENDING"},"signature":"sig"}
                        """,
                        MediaType.APPLICATION_JSON
                ));

        DefaultPayOsClient client = new DefaultPayOsClient(configuredProps, builder.build());
        CreatePaymentLinkResponse response = client.createPaymentLink(123456L, 49000, "Thanh toan goi AI rat dai vuot qua hai muoi lam ky tu");

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.data().orderCode()).isEqualTo(123456L);
        server.verify();
    }

    @Test
    @DisplayName("Should sanitize null or blank description to default")
    void shouldSanitizeNullOrBlankDescription() {
        PayOsProperties unconfiguredProps = new PayOsProperties(
                "", "", "", "https://api-merchant.payos.vn",
                "http://localhost:5173/payment/success",
                "http://localhost:5173/payment/cancel"
        );

        DefaultPayOsClient client = new DefaultPayOsClient(unconfiguredProps, RestClient.builder().build());

        CreatePaymentLinkResponse responseNull = client.createPaymentLink(101L, 49000, null);
        assertThat(responseNull.data().description()).isEqualTo("Thanh toan goi AI");

        CreatePaymentLinkResponse responseBlank = client.createPaymentLink(102L, 49000, "   ");
        assertThat(responseBlank.data().description()).isEqualTo("Thanh toan goi AI");
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

        // Null checks
        assertThat(client.verifyWebhook(null)).isFalse();
        assertThat(client.verifyWebhook(new PayOsWebhookPayload("00", "desc", null, validSig))).isFalse();
        assertThat(client.verifyWebhook(new PayOsWebhookPayload("00", "desc", data, null))).isFalse();
    }

    @Test
    @DisplayName("Should support mock signatures when unconfigured")
    void shouldSupportMockSignaturesWhenUnconfigured() {
        PayOsProperties unconfiguredProps = new PayOsProperties(
                "", "", "", "https://api-merchant.payos.vn",
                "http://localhost:5173/payment/success",
                "http://localhost:5173/payment/cancel"
        );

        DefaultPayOsClient client = new DefaultPayOsClient(unconfiguredProps, RestClient.builder().build());
        Map<String, Object> data = Map.of("orderCode", 1L);

        assertThat(client.verifyWebhook(new PayOsWebhookPayload("00", "desc", data, "mock_signature"))).isTrue();
        assertThat(client.verifyWebhook(new PayOsWebhookPayload("00", "desc", data, "test_valid_signature"))).isTrue();
        assertThat(client.verifyWebhook(new PayOsWebhookPayload("00", "desc", data, "other_signature"))).isFalse();
    }

    @Test
    @DisplayName("Should extract fields correctly from PayOsWebhookPayload")
    void shouldExtractPayloadFieldsCorrectly() {
        // String format and null cases
        PayOsWebhookPayload payloadString = new PayOsWebhookPayload(
                "00", "success",
                Map.of("orderCode", "987654", "amount", "49000", "paymentLinkId", "pl_abc", "code", "00"),
                "sig"
        );
        assertThat(payloadString.getOrderCode()).isEqualTo(987654L);
        assertThat(payloadString.getAmount()).isEqualTo(49000);
        assertThat(payloadString.getPaymentLinkId()).isEqualTo("pl_abc");
        assertThat(payloadString.getCode()).isEqualTo("00");
        assertThat(payloadString.isPaymentSuccess()).isTrue();

        // Invalid numbers
        PayOsWebhookPayload invalidPayload = new PayOsWebhookPayload(
                "01", "fail",
                Map.of("orderCode", "not_a_number", "amount", "invalid"),
                "sig"
        );
        assertThat(invalidPayload.getOrderCode()).isNull();
        assertThat(invalidPayload.getAmount()).isNull();
        assertThat(invalidPayload.getPaymentLinkId()).isNull();
        assertThat(invalidPayload.isPaymentSuccess()).isFalse();

        // Null data
        PayOsWebhookPayload nullData = new PayOsWebhookPayload("00", "desc", null, "sig");
        assertThat(nullData.getOrderCode()).isNull();
        assertThat(nullData.getAmount()).isNull();
        assertThat(nullData.getPaymentLinkId()).isNull();
        assertThat(nullData.getCode()).isEqualTo("00");
    }
}

