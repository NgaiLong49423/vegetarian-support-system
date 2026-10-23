package tech.mamxanh.integration.payos;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tech.mamxanh.common.exception.*;
import tech.mamxanh.integration.payos.dto.*;
import tools.jackson.databind.json.JsonMapper;

class DefaultPayOsClientTest {
    private static final String KEY = "test-only-payos-checksum";
    private static final PayOsProperties CONFIGURED = new PayOsProperties("test-client", "test-key", KEY,
            "https://api-merchant.payos.vn", "http://localhost:5173/payment/success", "http://localhost:5173/payment/cancel");
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final DefaultPayOsClient client = new DefaultPayOsClient(CONFIGURED, builder.build());
    private final JsonMapper mapper = JsonMapper.builder().build();

    String signed(Map<String, Object> data) {
        return mapper.writeValueAsString(Map.of("code", "00", "desc", "ok", "data", data,
                "signature", PayOsSignatureUtil.hmacSha256(PayOsSignatureUtil.convertObjToQueryStr(data), KEY)));
    }
    Map<String, Object> linkData() {
        return new HashMap<>(Map.of("orderCode", 123L, "amount", 99000, "currency", "VND", "status", "PENDING",
                "description", "Nang cap PRO", "paymentLinkId", "test-link", "checkoutUrl", "https://pay.payos.vn/web/test-link", "qrCode", "test-qr"));
    }
    void response(String body) {
        server.expect(requestTo("https://api-merchant.payos.vn/v2/payment-requests"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("x-client-id", "test-client"))
                .andExpect(header("x-api-key", "test-key"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }
    @Test void diagnosticsRedactCredentialsAndPaymentPayload() {
        assertThat(CONFIGURED.toString()).doesNotContain(KEY, "test-key", "test-client");
        var payload = new PayOsWebhookPayload("00", "ok", Map.of("accountNumber", "fixture-bank-account"), "fixture-signature");
        assertThat(payload.toString()).doesNotContain("fixture-bank-account", "fixture-signature");
    }

    @Test void signedInnerSuccessCannotBeOverriddenByUnsignedEnvelope() {
        assertThat(new PayOsWebhookPayload("01", "error", Map.of("code", "00"), "signed").isPaymentSuccess()).isTrue();
    }

    @Test void missingCredentialsFailClosedAndRejectMockSignatures() {
        var unconfigured = new DefaultPayOsClient(new PayOsProperties("", "", "", CONFIGURED.endpoint(), CONFIGURED.returnUrl(), CONFIGURED.cancelUrl()), RestClient.builder().build());
        assertThatThrownBy(() -> unconfigured.createPaymentLink(123, 99000, "PRO"))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_UNAVAILABLE);
        assertThatThrownBy(() -> unconfigured.getPaymentInfo(123)).isInstanceOf(AppException.class);
        for (String signature : List.of("mock_signature", "test_valid_signature", "forged")) {
            assertThat(unconfigured.verifyWebhook(new PayOsWebhookPayload("00", "ok", linkData(), signature))).isFalse();
        }
        server.verify();
    }
    @Test void createsRealCheckoutFromSignedMatchingData() {
        response(signed(linkData()));
        var result = client.createPaymentLink(123, 99000, "Nang cap PRO");
        assertThat(result.data().checkoutUrl()).isEqualTo("https://pay.payos.vn/web/test-link");
        assertThat(result.data().qrCode()).isEqualTo("test-qr"); server.verify();
    }
    @Test void rejectsForgedCheckoutResponse() {
        response(mapper.writeValueAsString(Map.of("code", "00", "data", linkData(), "signature", "forged")));
        assertThatThrownBy(() -> client.createPaymentLink(123, 99000, "PRO"))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_RESPONSE_INVALID);
    }
    @Test void rejectsSignedWrongAmountAndSameOriginFakeCheckout() {
        var wrongAmount = linkData(); wrongAmount.put("amount", 49000);
        response(signed(wrongAmount));
        assertThatThrownBy(() -> client.createPaymentLink(123, 99000, "PRO")).isInstanceOf(AppException.class);
    }
    @Test void rejectsSignedReturnUrlInsteadOfHostedCheckout() {
        var fake = linkData(); fake.put("checkoutUrl", CONFIGURED.returnUrl()+"?mock=true"); response(signed(fake));
        assertThatThrownBy(() -> client.createPaymentLink(123, 99000, "PRO")).isInstanceOf(AppException.class);
    }
    @Test void providerFailureIsUnavailableWithoutLeakingCredentialsOrBody() {
        server.expect(requestTo("https://api-merchant.payos.vn/v2/payment-requests")).andRespond(withServerError());
        assertThatThrownBy(() -> client.createPaymentLink(123, 99000, "PRO"))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("errorCode", ErrorCode.PAYMENT_UNAVAILABLE);
    }
    @Test void readsAndVerifiesProviderOrderInfoWithNestedTransactions() {
        var data = Map.<String,Object>of("orderCode", 123, "amount", 99000, "amountPaid", 99000,
                "status", "PAID", "transactions", List.of(Map.of("amount", 99000, "reference", "test-reference")));
        server.expect(requestTo("https://api-merchant.payos.vn/v2/payment-requests/123"))
                .andExpect(method(HttpMethod.GET)).andExpect(header("x-api-key", "test-key"))
                .andRespond(withSuccess(signed(data), MediaType.APPLICATION_JSON));
        assertThat(client.getPaymentInfo(123)).isEqualTo(new PayOsPaymentInfo(123, 99000, 99000, "PAID")); server.verify();
    }
    @Test void rejectsIncorrectProviderOrderIdentity() {
        server.expect(requestTo("https://api-merchant.payos.vn/v2/payment-requests/123"))
                .andRespond(withSuccess(signed(Map.of("orderCode", 999, "amount", 99000, "amountPaid", 99000, "status", "PAID")), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.getPaymentInfo(123)).isInstanceOf(AppException.class);
    }
    @Test void webhookRequiresSignatureAndSignedInnerSuccessCode() {
        var data = Map.<String,Object>of("orderCode", 123, "amount", 99000, "code", "01");
        var payload = new PayOsWebhookPayload("00", "ok", data, PayOsSignatureUtil.hmacSha256(PayOsSignatureUtil.convertObjToQueryStr(data), KEY));
        assertThat(client.verifyWebhook(payload)).isTrue();
        assertThat(payload.isPaymentSuccess()).isFalse();
        assertThat(client.verifyWebhook(null)).isFalse();
        assertThat(client.verifyWebhook(new PayOsWebhookPayload("00", "ok", null, "bad"))).isFalse();
        assertThat(new PayOsWebhookPayload("00", "ok", null, "bad").isPaymentSuccess()).isFalse();
        assertThat(new PayOsWebhookPayload("00", "ok", Map.of("orderCode", 1.5, "amount", 99000.5), "bad").getAmount()).isNull();
        assertThat(new PayOsWebhookPayload("00", "ok", Map.of("orderCode", 1.5), "bad").getOrderCode()).isNull();
    }
}
