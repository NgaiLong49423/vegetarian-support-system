package tech.mamxanh.subscription.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.CacheControl;
import tech.mamxanh.subscription.service.PaymentResultService;
import tech.mamxanh.subscription.dto.response.PaymentResultResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import tech.mamxanh.common.config.OpenApiConfig;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.subscription.dto.request.CheckoutRequest;
import tech.mamxanh.subscription.dto.response.CheckoutResponse;
import tech.mamxanh.subscription.dto.response.MySubscriptionResponse;
import tech.mamxanh.subscription.dto.response.MyAiUsageResponse;
import tech.mamxanh.subscription.dto.response.PlanResponse;
import tech.mamxanh.subscription.dto.response.TransactionHistoryResponse;
import tech.mamxanh.subscription.service.SubscriptionService;
import tech.mamxanh.subscription.service.AiTokenQuotaService;

@RestController
@RequestMapping(path = "/api/v1/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    private final PaymentResultService paymentResults;
    private final AiTokenQuotaService aiTokenQuota;

    public SubscriptionController(SubscriptionService subscriptionService, PaymentResultService paymentResults,
            AiTokenQuotaService aiTokenQuota) {
        this.subscriptionService = subscriptionService;
        this.paymentResults = paymentResults;
        this.aiTokenQuota = aiTokenQuota;
    }

    /**
     * AC-13.1: Public endpoint to view all official Phase 1 pricing plans.
     */
    @GetMapping("/plans")
    public ResponseEntity<List<PlanResponse>> getPlans() {
        return ResponseEntity.ok(subscriptionService.getAvailablePlans());
    }

    /**
     * Initializes payment request and returns checkout URL.
     */
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "payOS checkout link for the chosen plan."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED: the tier is not PLUS or PRO."),
            @ApiResponse(responseCode = "403", description = "ACCESS_DENIED: the account is not an active Customer or Expert, or ACCOUNT_LOCKED."),
            @ApiResponse(responseCode = "404", description = "NOT_FOUND: the session account no longer exists."),
            @ApiResponse(responseCode = "502", description = "PAYMENT_RESPONSE_INVALID: the provider response failed verification."),
            @ApiResponse(responseCode = "503", description = "PAYMENT_UNAVAILABLE: missing configuration or provider unavailable; no mock checkout is created."),
            @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR: the payOS payment link could not be created.")
    })
    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> createCheckout(
            Authentication authentication,
            @Valid @RequestBody CheckoutRequest request) {
        Long userId = extractUserId(authentication);
        CheckoutResponse response = subscriptionService.createCheckout(userId, request.tier());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * AC-13.4 & AC-13.7: Returns active subscription status and expiration date.
     */
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current subscription and its expiry."),
            @ApiResponse(responseCode = "403", description = "ACCESS_DENIED: the account is not an active Customer or Expert, or ACCOUNT_LOCKED."),
            @ApiResponse(responseCode = "404", description = "NOT_FOUND: the session account no longer exists.")
    })
    @GetMapping("/my-subscription")
    public ResponseEntity<MySubscriptionResponse> getMySubscription(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(subscriptionService.getMySubscription(userId));
    }

    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current token usage and quota for the caller."),
            @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid."),
            @ApiResponse(responseCode = "403", description = "The account is locked or is not an active Member.")
    })
    @GetMapping("/my-ai-usage")
    public ResponseEntity<MyAiUsageResponse> getMyAiUsage(Authentication authentication) {
        return ResponseEntity.ok(aiTokenQuota.getMyUsage(extractUserId(authentication)));
    }

    /**
     * Returns payment transaction history for current member.
     */
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment transactions of the caller."),
            @ApiResponse(responseCode = "403", description = "ACCESS_DENIED: the account is not an active Customer or Expert, or ACCOUNT_LOCKED."),
            @ApiResponse(responseCode = "404", description = "NOT_FOUND: the session account no longer exists.")
    })
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionHistoryResponse>> getTransactions(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(subscriptionService.getTransactionHistory(userId));
    }

    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Verified order result and current subscription of the caller. Pending orders are reconciled with payOS."),
            @ApiResponse(responseCode = "404", description = "PAYMENT_TRANSACTION_NOT_FOUND: order is missing or belongs to another account."),
            @ApiResponse(responseCode = "502", description = "PAYMENT_RESPONSE_INVALID: provider data cannot be verified."),
            @ApiResponse(responseCode = "503", description = "PAYMENT_UNAVAILABLE: payment provider is not configured or temporarily unavailable.")
    })
    @GetMapping("/transactions/{orderCode}")
    public ResponseEntity<PaymentResultResponse> getPaymentResult(Authentication authentication,
            @PathVariable @Pattern(regexp = "[1-9][0-9]{0,15}") @jakarta.validation.constraints.DecimalMax("9007199254740991") String orderCode) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(paymentResults.getResult(extractUserId(authentication), orderCode));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Phiên đăng nhập không hợp lệ.");
        }
    }
}
