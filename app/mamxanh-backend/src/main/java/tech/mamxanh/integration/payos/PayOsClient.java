package tech.mamxanh.integration.payos;

import tech.mamxanh.integration.payos.dto.CreatePaymentLinkResponse;
import tech.mamxanh.integration.payos.dto.PayOsWebhookPayload;

/**
 * Client interface for interacting with payOS payment gateway (FR-13).
 */
public interface PayOsClient {

    /** Local configuration preflight only; fail before creating an order when merchant credentials are absent. */
    void validateCheckoutConfiguration();

    /**
     * Creates a payment link via payOS API /v2/payment-requests. Implementations must verify
     * the signed provider response, order identity, amount and hosted HTTPS checkout URL.
     *
     * @param orderCode   unique numeric order code
     * @param amount      payment amount in VND
     * @param description order description (max 25 characters per payOS rule)
     * @return CreatePaymentLinkResponse containing checkoutUrl and qrCode
     */
    CreatePaymentLinkResponse createPaymentLink(long orderCode, int amount, String description);

    default CreatePaymentLinkResponse createPaymentLink(long orderCode, int amount, String description, int expiredAt) {
        return createPaymentLink(orderCode, amount, description);
    }

    /**
     * Verifies the authenticity of a payOS webhook notification (AC-13.5).
     *
     * @param payload the incoming webhook body
     * @return true if signature is valid, false otherwise
     */
    /** Returns only authenticated, signature-verified provider data, or throws a safe provider error. */
    tech.mamxanh.integration.payos.dto.PayOsPaymentInfo getPaymentInfo(long orderCode);

    boolean verifyWebhook(PayOsWebhookPayload payload);
}
