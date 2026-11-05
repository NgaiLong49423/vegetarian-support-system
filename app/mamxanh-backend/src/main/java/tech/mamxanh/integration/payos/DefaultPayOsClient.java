package tech.mamxanh.integration.payos;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.payos.dto.*;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/** Real payOS adapter. Missing credentials fail closed; test doubles belong in tests. */
public class DefaultPayOsClient implements PayOsClient {
    private final PayOsProperties properties;
    private final RestClient restClient;
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    public DefaultPayOsClient(PayOsProperties properties, RestClient restClient) {
        this.properties = Objects.requireNonNull(properties);
        this.restClient = Objects.requireNonNull(restClient);
    }

    @Override
    public void validateCheckoutConfiguration() { requireConfigured(); }

    @Override
    public CreatePaymentLinkResponse createPaymentLink(long orderCode, int amount, String description) {
        return createPaymentLink(orderCode, amount, description, null);
    }

    @Override
    public CreatePaymentLinkResponse createPaymentLink(long orderCode, int amount, String description, int expiredAt) {
        return createPaymentLink(orderCode, amount, description, Integer.valueOf(expiredAt));
    }

    private CreatePaymentLinkResponse createPaymentLink(long orderCode, int amount, String description, Integer expiredAt) {
        requireConfigured();
        String desc = description == null || description.isBlank() ? "Thanh toan goi AI" : description;
        desc = desc.substring(0, Math.min(desc.length(), 25));
        String signature = PayOsSignatureUtil.createPaymentRequestSignature(orderCode, amount, desc,
                properties.cancelUrl(), properties.returnUrl(), properties.checksumKey());
        try {
            PayOsWebhookPayload response = restClient.post().uri(properties.endpoint() + "/v2/payment-requests")
                    .header("x-client-id", properties.clientId()).header("x-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new CreatePaymentLinkRequest(orderCode, amount, desc, properties.cancelUrl(), properties.returnUrl(), expiredAt, signature))
                    .retrieve().body(PayOsWebhookPayload.class);
            Map<String, Object> data = verifiedData(response);
            PayOsPaymentData link = MAPPER.convertValue(data, PayOsPaymentData.class);
            if (exactNumber(data.get("orderCode")).longValueExact() != orderCode
                    || exactNumber(data.get("amount")).intValueExact() != amount
                    || link.orderCode() != orderCode || link.amount() != amount || !"VND".equals(data.get("currency"))
                    || !"PENDING".equals(link.status()) || !safeCheckoutUrl(link.checkoutUrl())
                    || link.paymentLinkId() == null || link.paymentLinkId().isBlank()
                    || link.qrCode() == null || link.qrCode().isBlank()) {
                throw new AppException(ErrorCode.PAYMENT_RESPONSE_INVALID);
            }
            return new CreatePaymentLinkResponse("00", "success", link, response.signature());
        } catch (RestClientException ex) {
            throw new AppException(ErrorCode.PAYMENT_UNAVAILABLE);
        } catch (IllegalArgumentException | ArithmeticException | tools.jackson.core.JacksonException ex) {
            throw new AppException(ErrorCode.PAYMENT_RESPONSE_INVALID);
        }
    }

    @Override
    public PayOsPaymentInfo getPaymentInfo(long orderCode) {
        requireConfigured();
        try {
            PayOsWebhookPayload response = restClient.get().uri(properties.endpoint() + "/v2/payment-requests/{id}", orderCode)
                    .header("x-client-id", properties.clientId()).header("x-api-key", properties.apiKey())
                    .retrieve().body(PayOsWebhookPayload.class);
            Map<String, Object> data = verifiedData(response);
            long returnedOrder = exactNumber(data.get("orderCode")).longValueExact();
            int amount = exactNumber(data.get("amount")).intValueExact();
            int amountPaid = exactNumber(data.get("amountPaid")).intValueExact();
            String status = String.valueOf(data.get("status"));
            if (returnedOrder != orderCode || amount <= 0 || amountPaid < 0
                    || !java.util.Set.of("PENDING", "PROCESSING", "PAID", "CANCELLED", "EXPIRED").contains(status)) {
                throw new AppException(ErrorCode.PAYMENT_RESPONSE_INVALID);
            }
            Object rawTransactions = data.get("transactions");
            java.time.LocalDateTime transactionAt = null;
            if (rawTransactions instanceof Map<?, ?> transactionMap) {
                for (Object transaction : transactionMap.values()) {
                    if (transaction instanceof Map<?, ?> fields) {
                        var candidate = PayOsPaymentInfo.parseTransactionDateTime(fields.get("transactionDateTime"));
                        if (candidate != null && (transactionAt == null || candidate.isAfter(transactionAt))) transactionAt = candidate;
                    }
                }
            } else if (rawTransactions instanceof Iterable<?> transactionList) {
                for (Object transaction : transactionList) {
                    if (transaction instanceof Map<?, ?> fields) {
                        var candidate = PayOsPaymentInfo.parseTransactionDateTime(fields.get("transactionDateTime"));
                        if (candidate != null && (transactionAt == null || candidate.isAfter(transactionAt))) transactionAt = candidate;
                    }
                }
            }
            if (transactionAt == null) transactionAt = PayOsPaymentInfo.parseTransactionDateTime(data.get("transactionDateTime"));
            return new PayOsPaymentInfo(returnedOrder, amount, amountPaid, status, transactionAt);
        } catch (RestClientException ex) {
            throw new AppException(ErrorCode.PAYMENT_UNAVAILABLE);
        } catch (IllegalArgumentException | ArithmeticException ex) {
            throw new AppException(ErrorCode.PAYMENT_RESPONSE_INVALID);
        }
    }

    private java.math.BigDecimal exactNumber(Object value) {
        if (value == null) throw new IllegalArgumentException("Missing number");
        return new java.math.BigDecimal(String.valueOf(value));
    }

    private Map<String, Object> verifiedData(PayOsWebhookPayload response) {
        if (response == null || !"00".equals(response.code()) || !verifyWebhook(response)) {
            throw new AppException(ErrorCode.PAYMENT_RESPONSE_INVALID);
        }
        return response.data();
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) throw new AppException(ErrorCode.PAYMENT_UNAVAILABLE);
    }

    public static boolean safeCheckoutUrl(String url) {
        try {
            URI uri = URI.create(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && "pay.payos.vn".equalsIgnoreCase(uri.getHost())
                    && uri.getUserInfo() == null && (uri.getPort() == -1 || uri.getPort() == 443);
        } catch (IllegalArgumentException | NullPointerException ex) { return false; }
    }

    @Override
    public boolean verifyWebhook(PayOsWebhookPayload payload) {
        return properties.isConfigured() && payload != null
                && PayOsSignatureUtil.verifyWebhookSignature(payload.data(), payload.signature(), properties.checksumKey());
    }
}
