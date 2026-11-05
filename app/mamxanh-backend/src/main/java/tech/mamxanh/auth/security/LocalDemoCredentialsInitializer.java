package tech.mamxanh.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Creates local-only BCrypt credentials for all 25 Flyway demo accounts after seed data is loaded. */
@Component
@Profile("local")
class LocalDemoCredentialsInitializer implements ApplicationRunner {
    private static final List<String> DEMO_EMAILS = List.of(
            "demo-customer@mamxanh.local",
            "demo-new-member@mamxanh.local",
            "demo-applicant@mamxanh.local",
            "demo-expert@mamxanh.local",
            "demo-admin@mamxanh.local",
            "demo-customer-04@mamxanh.local",
            "demo-customer-05@mamxanh.local",
            "demo-customer-06@mamxanh.local",
            "demo-customer-07@mamxanh.local",
            "demo-customer-08@mamxanh.local",
            "demo-customer-09@mamxanh.local",
            "demo-customer-10@mamxanh.local",
            "demo-customer-11@mamxanh.local",
            "demo-customer-12@mamxanh.local",
            "demo-customer-13@mamxanh.local",
            "demo-customer-14@mamxanh.local",
            "demo-customer-15@mamxanh.local",
            "demo-customer-16@mamxanh.local",
            "demo-customer-17@mamxanh.local",
            "demo-customer-18@mamxanh.local",
            "demo-customer-19@mamxanh.local",
            "demo-customer-20@mamxanh.local",
            "demo-expert-02@mamxanh.local",
            "demo-expert-03@mamxanh.local",
            "demo-expert-04@mamxanh.local");

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
