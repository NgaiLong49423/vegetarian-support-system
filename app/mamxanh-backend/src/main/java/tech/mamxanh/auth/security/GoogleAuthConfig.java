package tech.mamxanh.auth.security;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

/** Wires Google ID Token verification for FR-03-D (Q25). */
@Configuration
@EnableConfigurationProperties(GoogleAuthProperties.class)
public class GoogleAuthConfig {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthConfig.class);

    @Bean
    GoogleTokenVerifier googleTokenVerifier(GoogleAuthProperties properties) {
        if (properties.clientId() == null || properties.clientId().isBlank()) {
            log.warn("MAMXANH_GOOGLE_CLIENT_ID is not set; Google login rejects every token");
            return idToken -> Optional.empty();
        }
        // Default issuers are Google's: accounts.google.com and https://accounts.google.com.
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(List.of(properties.clientId().strip()))
                .build();
        return new GoogleIdTokenVerifierAdapter(verifier);
    }
}
