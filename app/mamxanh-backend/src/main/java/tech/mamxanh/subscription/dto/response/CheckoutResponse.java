package tech.mamxanh.subscription.dto.response;

public record CheckoutResponse(
        long orderCode,
        int amountVnd,
        String tier,
        String checkoutUrl,
        String qrCode
) {
    @Override
    public String toString() {
        return "CheckoutResponse[redacted]";
    }
}
