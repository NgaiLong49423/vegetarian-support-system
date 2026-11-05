package tech.mamxanh.admin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tech.mamxanh.admin.event.AiConfigurationEmailEvent;
import tech.mamxanh.integration.email.EmailSender;

/** Best-effort asynchronous email broadcast after an AI configuration transaction commits. */
@Component
public class AiConfigurationEmailNotifier {
    private static final Logger log = LoggerFactory.getLogger(AiConfigurationEmailNotifier.class);

    private final JdbcTemplate jdbc;
    private final EmailSender emailSender;

    public AiConfigurationEmailNotifier(JdbcTemplate jdbc, EmailSender emailSender) {
        this.jdbc = jdbc;
        this.emailSender = emailSender;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(AiConfigurationEmailEvent event) {
        String body = "Mâm Xanh đã cập nhật cấu hình AI:\n\n" + String.join("\n", event.changes());
        jdbc.query("""
                SELECT email FROM [USER]
                WHERE account_status = 'ACTIVE' AND email_verified = 1 AND role IN ('CUSTOMER','EXPERT')
                """, rs -> {
            try {
                emailSender.sendPlainText(rs.getString("email"), "Cập nhật cấu hình AI Mâm Xanh", body);
            } catch (RuntimeException failure) {
                log.warn("Could not send AI configuration update email; delivery will not affect the saved configuration");
            }
        });
    }
}
