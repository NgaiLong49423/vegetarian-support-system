package tech.mamxanh.subscription.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import tech.mamxanh.common.exception.AppException;
import tech.mamxanh.common.exception.ErrorCode;
import tech.mamxanh.integration.payos.dto.PayOsWebhookPayload;
import tech.mamxanh.subscription.dto.request.CheckoutRequest;
import tech.mamxanh.subscription.dto.response.CheckoutResponse;
import tech.mamxanh.subscription.dto.response.MySubscriptionResponse;
import tech.mamxanh.subscription.dto.response.PlanResponse;
import tech.mamxanh.subscription.dto.response.TransactionHistoryResponse;
import tech.mamxanh.subscription.entity.SubscriptionTier;
import tech.mamxanh.subscription.service.SubscriptionService;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionController subscriptionController;

    @Test
    @DisplayName("AC-13.1: Should delegate getPlans to SubscriptionService")
    void shouldDelegateGetPlans() {
        List<PlanResponse> mockPlans = List.of(
                new PlanResponse("FREE", 0, "Gói FREE", "Free", List.of("Chatbot"), "Không thời hạn"),
                new PlanResponse("PLUS", 49000, "Gói PLUS", "Plus", List.of("Soạn bài"), "Tháng"),
                new PlanResponse("PRO", 99000, "Gói PRO", "Pro", List.of("Menu tuần"), "Tháng")
        );
        when(subscriptionService.getAvailablePlans()).thenReturn(mockPlans);

        ResponseEntity<List<PlanResponse>> response = subscriptionController.getPlans();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockPlans);
        verify(subscriptionService).getAvailablePlans();
    }

    @Test
    @DisplayName("Should delegate createCheckout to service when user is authenticated")
    void shouldDelegateCreateCheckout() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("1");

        CheckoutResponse mockCheckout = new CheckoutResponse(123456L, 49000, "PLUS", "https://pay.payos.vn/123", "qr");
        when(subscriptionService.createCheckout(1L, SubscriptionTier.PLUS)).thenReturn(mockCheckout);

        ResponseEntity<CheckoutResponse> response = subscriptionController.createCheckout(auth, new CheckoutRequest(SubscriptionTier.PLUS));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(mockCheckout);
        verify(subscriptionService).createCheckout(1L, SubscriptionTier.PLUS);
    }

    @Test
    @DisplayName("Should throw AppException when createCheckout is called without authentication")
    void shouldRejectUnauthenticatedCheckout() {
        assertThatThrownBy(() -> subscriptionController.createCheckout(null, new CheckoutRequest(SubscriptionTier.PLUS)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHENTICATED);
    }

    @Test
    @DisplayName("AC-13.7: Should delegate getMySubscription to service")
    void shouldDelegateGetMySubscription() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("1");

        MySubscriptionResponse mockSub = new MySubscriptionResponse("PLUS", "ACTIVE", true,
                LocalDateTime.now(), LocalDateTime.now().plusDays(30), List.of("AI"));
        when(subscriptionService.getMySubscription(1L)).thenReturn(mockSub);

        ResponseEntity<MySubscriptionResponse> response = subscriptionController.getMySubscription(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockSub);
        verify(subscriptionService).getMySubscription(1L);
    }

    @Test
    @DisplayName("Should delegate getTransactions to service")
    void shouldDelegateGetTransactions() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("1");

        List<TransactionHistoryResponse> mockHistory = List.of(
                new TransactionHistoryResponse(1L, "123", 49000, "PAID", LocalDateTime.now(), LocalDateTime.now())
        );
        when(subscriptionService.getTransactionHistory(1L)).thenReturn(mockHistory);

        ResponseEntity<List<TransactionHistoryResponse>> response = subscriptionController.getTransactions(auth);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockHistory);
        verify(subscriptionService).getTransactionHistory(1L);
    }

    @Test
    @DisplayName("PaymentWebhookController should delegate webhook handling to service")
    void shouldDelegateWebhookHandling() {
        PaymentWebhookController webhookController = new PaymentWebhookController(subscriptionService);
        PayOsWebhookPayload payload = new PayOsWebhookPayload("00", "success", Map.of("orderCode", 123L), "sig");

        ResponseEntity<Map<String, String>> response = webhookController.handlePayOsWebhook(payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("code", "00");
        verify(subscriptionService).handlePayOsWebhook(payload);
    }
}
