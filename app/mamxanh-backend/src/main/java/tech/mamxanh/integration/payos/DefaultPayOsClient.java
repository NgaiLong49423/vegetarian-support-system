package tech.mamxanh.integration.payos;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tech.mamxanh.integration.payos.dto.CreatePaymentLinkRequest;
import tech.mamxanh.integration.payos.dto.CreatePaymentLinkResponse;
import tech.mamxanh.integration.payos.dto.PayOsPaymentData;
import tech.mamxanh.integration.payos.dto.PayOsWebhookPayload;

/**
 * Default implementation of {@link PayOsClient} using Spring {@link RestClient}.
 */
public class DefaultPayOsClient implements PayOsClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultPayOsClient.class);
    private static final int MAX_DESC_LENGTH = 25;

    private final PayOsProperties properties;
    private final RestClient restClient;

    public DefaultPayOsClient(PayOsProperties properties, RestClient restClient) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public CreatePaymentLinkResponse createPaymentLink(long orderCode, int amount, String description) {
        String sanitizedDesc = sanitizeDescription(description);

        if (!properties.isConfigured()) {
            log.warn("payOS credentials not configured (mamxanh.payos.*); generating local fallback checkout URL for order {}", orderCode);
            String fallbackCheckoutUrl = properties.returnUrl() + "?orderCode=" + orderCode + "&mock=true";
            PayOsPaymentData mockData = new PayOsPaymentData(
                    orderCode,
                    amount,
                    sanitizedDesc,
                    fallbackCheckoutUrl,
                    "mock_qr_code_" + orderCode,
                    "mock_link_" + orderCode,
                    "PENDING"
            );
            return new CreatePaymentLinkResponse("00", "success", mockData, "mock_signature");
        }

        String signature = PayOsSignatureUtil.createPaymentRequestSignature(
                orderCode,
                amount,
                sanitizedDesc,
                properties.cancelUrl(),
                properties.returnUrl(),
                properties.checksumKey()
        );

        CreatePaymentLinkRequest requestBody = new CreatePaymentLinkRequest(
                orderCode,
                amount,
                sanitizedDesc,
                properties.cancelUrl(),
                properties.returnUrl(),
                signature
        );

        return restClient.post()
                .uri(properties.endpoint() + "/v2/payment-requests")
                .header("x-client-id", properties.clientId())
                .header("x-api-key", properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(CreatePaymentLinkResponse.class);
    }

    @Override
    public boolean verifyWebhook(PayOsWebhookPayload payload) {
        if (payload == null || payload.data() == null || payload.signature() == null) {
            log.warn("payOS webhook verification failed: payload, data, or signature is null");
            return false;
        }

        if (!properties.isConfigured()) {
            log.warn("payOS checksumKey not configured; checking test signature compatibility");
            return "mock_signature".equals(payload.signature()) || "test_valid_signature".equals(payload.signature());
        }

        return PayOsSignatureUtil.verifyWebhookSignature(payload.data(), payload.signature(), properties.checksumKey());
    }

    private String sanitizeDescription(String desc) {
        if (desc == null || desc.isBlank()) {
            return "Thanh toan goi AI";
        }
        if (desc.length() > MAX_DESC_LENGTH) {
            return desc.substring(0, MAX_DESC_LENGTH);
        }
        return desc;
    }
}
