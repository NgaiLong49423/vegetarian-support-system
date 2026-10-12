package tech.mamxanh.subscription.dto.request;

import jakarta.validation.constraints.NotNull;
import tech.mamxanh.subscription.entity.SubscriptionTier;

public record CheckoutRequest(
        @NotNull(message = "Gói đăng ký không được để trống")
        SubscriptionTier tier
) {
}
