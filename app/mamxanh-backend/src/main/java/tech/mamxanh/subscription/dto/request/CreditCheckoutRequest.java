package tech.mamxanh.subscription.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreditCheckoutRequest(@NotNull @Min(1) Integer amountVnd) { }
