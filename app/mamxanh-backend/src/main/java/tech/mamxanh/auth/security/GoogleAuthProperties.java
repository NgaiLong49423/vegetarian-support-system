package tech.mamxanh.auth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * FR-03-D Google login settings ({@code mamxanh.auth.google.*}).
 *
 * @param clientId OAuth client ID of this application ({@code MAMXANH_GOOGLE_CLIENT_ID}); the audience
 *                 every Google ID Token must carry. Public, not a secret; empty disables Google login.
 */
@ConfigurationProperties(prefix = "mamxanh.auth.google")
public record GoogleAuthProperties(@DefaultValue("") String clientId) {
}
