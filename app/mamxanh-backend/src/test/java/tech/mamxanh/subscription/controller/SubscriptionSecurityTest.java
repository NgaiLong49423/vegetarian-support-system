package tech.mamxanh.subscription.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.mamxanh.auth.repository.UserRepository;
import tech.mamxanh.common.config.SecurityConfig;
import tech.mamxanh.subscription.service.SubscriptionService;

@WebMvcTest(controllers = {SubscriptionController.class, PaymentWebhookController.class})
@Import(SecurityConfig.class)
class SubscriptionSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void guestCanReadPlans() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/plans"))
                .andExpect(status().isOk());
        verify(subscriptionService).getAvailablePlans();
    }

    @Test
    void webhookReachesSignatureValidationWithoutBearerToken() throws Exception {
        mockMvc.perform(post("/api/v1/payments/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"00\",\"data\":{\"orderCode\":123},\"signature\":\"test\"}"))
                .andExpect(status().isOk());
        verify(subscriptionService).handlePayOsWebhook(any());
    }

    @Test
    void guestCannotCreateCheckout() throws Exception {
        mockMvc.perform(post("/api/v1/subscriptions/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tier\":\"PLUS\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void guestCannotReadOwnSubscriptionOrTransactions() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/my-subscription"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/subscriptions/transactions"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(subscriptionService);
    }
}
