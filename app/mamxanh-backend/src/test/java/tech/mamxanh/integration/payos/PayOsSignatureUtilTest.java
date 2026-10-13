package tech.mamxanh.integration.payos;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PayOsSignatureUtilTest {

    private static final String TEST_CHECKSUM_KEY = "test_checksum_key_123456789";

    @Test
    @DisplayName("Should generate deterministic HMAC-SHA256 signature for payment request")
    void shouldGenerateSignatureForPaymentRequest() {
        long orderCode = 1728001000L;
        int amount = 49000;
        String desc = "Goi PLUS Mam Xanh";
        String cancelUrl = "http://localhost:5173/payment/cancel";
        String returnUrl = "http://localhost:5173/payment/success";

        String sig1 = PayOsSignatureUtil.createPaymentRequestSignature(
                orderCode, amount, desc, cancelUrl, returnUrl, TEST_CHECKSUM_KEY);
        String sig2 = PayOsSignatureUtil.createPaymentRequestSignature(
                orderCode, amount, desc, cancelUrl, returnUrl, TEST_CHECKSUM_KEY);

        assertThat(sig1).isNotBlank();
        assertThat(sig1).isEqualTo(sig2);
        assertThat(sig1).hasSize(64); // 256 bits = 64 hex chars
    }

    @Test
    @DisplayName("Should verify authentic webhook payload successfully")
    void shouldVerifyAuthenticWebhookSignature() {
        Map<String, Object> data = new HashMap<>();
        data.put("orderCode", 1728001000L);
        data.put("amount", 49000);
        data.put("description", "Goi PLUS Mam Xanh");
        data.put("currency", "VND");
        data.put("code", "00");

        String queryStr = PayOsSignatureUtil.convertObjToQueryStr(data);
        String validSig = PayOsSignatureUtil.hmacSha256(queryStr, TEST_CHECKSUM_KEY);

        boolean result = PayOsSignatureUtil.verifyWebhookSignature(data, validSig, TEST_CHECKSUM_KEY);
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("AC-13.5: Should reject webhook if amount has been tampered")
    void shouldRejectTamperedAmount() {
        Map<String, Object> originalData = new HashMap<>();
        originalData.put("orderCode", 1728001000L);
        originalData.put("amount", 49000);
        originalData.put("description", "Goi PLUS Mam Xanh");

        String queryStr = PayOsSignatureUtil.convertObjToQueryStr(originalData);
        String validSig = PayOsSignatureUtil.hmacSha256(queryStr, TEST_CHECKSUM_KEY);

        // Tamper the amount
        Map<String, Object> tamperedData = new HashMap<>(originalData);
        tamperedData.put("amount", 99000);

        boolean result = PayOsSignatureUtil.verifyWebhookSignature(tamperedData, validSig, TEST_CHECKSUM_KEY);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("AC-13.5: Should reject webhook if signature is forged or invalid")
    void shouldRejectTamperedSignature() {
        Map<String, Object> data = new HashMap<>();
        data.put("orderCode", 1728001000L);
        data.put("amount", 49000);

        String fakeSignature = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        boolean result = PayOsSignatureUtil.verifyWebhookSignature(data, fakeSignature, TEST_CHECKSUM_KEY);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("AC-13.5: Should safely reject null or empty webhook inputs")
    void shouldRejectNullOrEmptyInputs() {
        assertThat(PayOsSignatureUtil.verifyWebhookSignature(null, "sig", TEST_CHECKSUM_KEY)).isFalse();
        assertThat(PayOsSignatureUtil.verifyWebhookSignature(Map.of(), null, TEST_CHECKSUM_KEY)).isFalse();
        assertThat(PayOsSignatureUtil.verifyWebhookSignature(Map.of(), "", TEST_CHECKSUM_KEY)).isFalse();
        assertThat(PayOsSignatureUtil.verifyWebhookSignature(Map.of(), "sig", null)).isFalse();
        assertThat(PayOsSignatureUtil.verifyWebhookSignature(Map.of(), "sig", "")).isFalse();
    }
}
