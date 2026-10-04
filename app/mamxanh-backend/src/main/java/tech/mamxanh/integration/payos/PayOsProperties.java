package tech.mamxanh.integration.payos;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuration parameters for the payOS payment gateway integration (FR-13).
 *
 * @param clientId     payOS Client ID from merchant dashboard
 * @param apiKey       payOS API Key
 * @param checksumKey  payOS Checksum Key used for HMAC-SHA256 signature generation and verification
 * @param endpoint     payOS API base URL (defaults to https://api-merchant.payos.vn)
 * @param returnUrl    URL to redirect member upon successful payment
 * @param cancelUrl    URL to redirect member upon cancelled payment
 */
@ConfigurationProperties(prefix = "mamxanh.payos")
public record PayOsProperties(
        @DefaultValue("") String clientId,
        @DefaultValue("") String apiKey,
        @DefaultValue("") String checksumKey,
        @DefaultValue("https://api-merchant.payos.vn") String endpoint,
        @DefaultValue("http://localhost:5173/payment/success") String returnUrl,
        @DefaultValue("http://localhost:5173/payment/cancel") String cancelUrl) {

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && checksumKey != null && !checksumKey.isBlank();
    }
}
