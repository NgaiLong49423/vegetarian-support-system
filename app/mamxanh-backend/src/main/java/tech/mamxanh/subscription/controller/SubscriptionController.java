package tech.mamxanh.subscription.controller;

import jakarta.validation.Valid;
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
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.subscription.dto.request.CheckoutRequest;
import tech.mamxanh.subscription.dto.response.CheckoutResponse;
import tech.mamxanh.subscription.dto.response.MySubscriptionResponse;
import tech.mamxanh.subscription.dto.response.PlanResponse;
import tech.mamxanh.subscription.dto.response.TransactionHistoryResponse;
import tech.mamxanh.subscription.service.SubscriptionService;

@RestController
@RequestMapping(path = "/api/v1/subscriptions", produces = MediaType.APPLICATION_JSON_VALUE)
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
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
    @GetMapping("/my-subscription")
    public ResponseEntity<MySubscriptionResponse> getMySubscription(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(subscriptionService.getMySubscription(userId));
    }

    /**
     * Returns payment transaction history for current member.
     */
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionHistoryResponse>> getTransactions(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(subscriptionService.getTransactionHistory(userId));
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
