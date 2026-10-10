package tech.mamxanh.auth.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableConfigurationProperties({ AuthProperties.class, PasswordResetProperties.class })
public class AuthSecurityConfig {

    /** BCrypt only; passwords are never stored in plaintext or with reversible encryption (NFR-06). */
    @Bean
    PasswordEncoder passwordEncoder(AuthProperties authProperties) {
        return new BCryptPasswordEncoder(authProperties.bcryptStrength());
    }
}
