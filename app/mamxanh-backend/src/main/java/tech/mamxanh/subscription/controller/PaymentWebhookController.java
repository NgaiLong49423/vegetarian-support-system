package tech.mamxanh.subscription.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.mamxanh.integration.payos.dto.PayOsWebhookPayload;
import tech.mamxanh.subscription.service.SubscriptionService;

import java.util.Map;

/**
 * Public webhook endpoint receiving IPN payment notifications from payOS (FR-13).
 */
@RestController
@RequestMapping(path = "/api/v1/payments", produces = MediaType.APPLICATION_JSON_VALUE)
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final SubscriptionService subscriptionService;

    public PaymentWebhookController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * payOS Webhook callback.
     * Enforces signature verification (AC-13.5), idempotency (AC-13.3), and activation (AC-13.2).
     */
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Verified webhook acknowledged; duplicates and unknown signed probes never grant extra entitlement."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "PAYMENT_SIGNATURE_INVALID or VALIDATION_FAILED: invalid signature, currency, amount or payload.")
    })
    @PostMapping(path = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> handlePayOsWebhook(@RequestBody PayOsWebhookPayload payload) {
        log.info("Received payOS payment webhook");
        subscriptionService.handlePayOsWebhook(payload);
        return ResponseEntity.ok(Map.of("code", "00", "desc", "success"));
    }
}
