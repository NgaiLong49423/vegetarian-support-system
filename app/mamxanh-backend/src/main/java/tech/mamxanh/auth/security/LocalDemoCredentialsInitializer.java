package tech.mamxanh.auth.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates local-only BCrypt credentials for Flyway demo accounts after seed data is loaded. */
@Component
@Profile("local")
class LocalDemoCredentialsInitializer implements ApplicationRunner {
    private static final List<String> DEMO_EMAILS = List.of(
            "demo-customer@mamxanh.local",
            "demo-new-member@mamxanh.local",
            "demo-applicant@mamxanh.local",
            "demo-expert@mamxanh.local",
            "demo-admin@mamxanh.local");

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String password;

    LocalDemoCredentialsInitializer(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder,
            @Value("${mamxanh.demo.password}") String password) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        int passwordBytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (passwordBytes < 12 || passwordBytes > 72) {
            throw new IllegalStateException("MAMXANH_DEMO_PASSWORD must contain 12 to 72 UTF-8 bytes");
        }
        String placeholders = String.join(",", java.util.Collections.nCopies(DEMO_EMAILS.size(), "?"));
        int updated = jdbcTemplate.update("UPDATE [USER] SET password_hash = ?, email_verified = 1 "
                + "WHERE email IN (" + placeholders + ")", statement -> {
                    statement.setString(1, passwordEncoder.encode(password));
                    for (int index = 0; index < DEMO_EMAILS.size(); index++) {
                        statement.setString(index + 2, DEMO_EMAILS.get(index));
                    }
                });
        if (updated != DEMO_EMAILS.size()) {
            throw new IllegalStateException("Expected all local demo accounts to exist before credential setup");
        }
    }
}
