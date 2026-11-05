package tech.mamxanh.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import tech.mamxanh.auth.security.AuthProperties;
import tech.mamxanh.integration.email.EmailSender;

/**
 * Sends the moderation outcome email after the report decision is committed, on an async thread (UC-49.3,
 * best-effort per ARCHITECTURE section 4.8). A delivery failure is logged without the address and never
 * rolls back the decision or its in-app notifications (AC-49.3).
 */
@Component
public class ModerationEmailNotifier {

    private static final Logger log = LoggerFactory.getLogger(ModerationEmailNotifier.class);

    private final EmailSender emailSender;
    private final AuthProperties authProperties;

    public ModerationEmailNotifier(EmailSender emailSender, AuthProperties authProperties) {
        this.emailSender = emailSender;
        this.authProperties = authProperties;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onModerationEmailRequested(ModerationEmailRequested event) {
        String link = event.targetPath() == null ? ""
                : "Xem chi tiết tại liên kết dưới đây:\n\n"
                        + authProperties.frontendBaseUrl().replaceAll("/+$", "") + event.targetPath() + "\n\n";
        String body = """
                Xin chào %s,

                %s

                %sMâm Xanh
                """.formatted(event.displayName(), event.message(), link);
        try {
            emailSender.sendPlainText(event.email(), event.title() + " trên Mâm Xanh", body);
        } catch (RuntimeException ex) {
            log.warn("Moderation email delivery failed: {}", ex.getClass().getSimpleName());
        }
    }
}
