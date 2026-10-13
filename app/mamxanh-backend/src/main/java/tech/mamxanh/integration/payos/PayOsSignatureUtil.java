package tech.mamxanh.integration.payos;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cryptographic utility for payOS HMAC-SHA256 signature generation and webhook verification (FR-13, AC-13.5).
 */
public final class PayOsSignatureUtil {

    private static final Logger log = LoggerFactory.getLogger(PayOsSignatureUtil.class);
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    private PayOsSignatureUtil() {
    }

    /**
     * Creates HMAC-SHA256 signature for payOS create payment link request.
     * PayOS requires the fields to be sorted alphabetically:
     * amount, cancelUrl, description, orderCode, returnUrl.
     */
    public static String createPaymentRequestSignature(
            long orderCode,
            int amount,
            String description,
            String cancelUrl,
            String returnUrl,
            String checksumKey) {
        String dataStr = "amount=" + amount
                + "&cancelUrl=" + cancelUrl
                + "&description=" + description
                + "&orderCode=" + orderCode
                + "&returnUrl=" + returnUrl;
        return hmacSha256(dataStr, checksumKey);
    }

    /**
     * Verifies the authenticity of incoming payOS webhook payload.
     * Sorts all keys in the data map alphabetically, constructs the query string,
     * hashes with checksumKey, and compares in constant time to prevent timing attacks.
     *
     * @param data        the payload's 'data' object/map
     * @param signature   the received signature string
     * @param checksumKey the merchant checksum key
     * @return true if authentic, false if tampered or invalid
     */
    public static boolean verifyWebhookSignature(Map<String, Object> data, String signature, String checksumKey) {
        if (data == null || signature == null || signature.isBlank() || checksumKey == null || checksumKey.isBlank()) {
            log.warn("payOS webhook signature verification rejected: missing data, signature, or checksumKey");
            return false;
        }

        String dataQueryStr = convertObjToQueryStr(data);
        String expectedSignature = hmacSha256(dataQueryStr, checksumKey);

        boolean matches = MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );

        if (!matches) {
            log.warn("SECURITY: payOS webhook signature mismatch! Possible tampering detected.");
        }
        return matches;
    }

    /**
     * Converts a data map to a sorted query string according to payOS specification:
     * keys are sorted alphabetically; null values become empty strings; joined with '&'.
     */
    public static String convertObjToQueryStr(Map<String, Object> data) {
        Map<String, Object> sortedMap = new TreeMap<>(data);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append("&");
            }
            String valueStr = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
            sb.append(entry.getKey()).append("=").append(valueStr);
        }
        return sb.toString();
    }

    /**
     * Computes HMAC-SHA256 hash of data string using secret key, returned as lowercase hex.
     */
    public static String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256", ex);
        }
    }
}
