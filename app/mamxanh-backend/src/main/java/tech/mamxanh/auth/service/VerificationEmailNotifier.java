package tech.mamxanh.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

import tech.mamxanh.auth.security.AuthProperties;
import tech.mamxanh.integration.email.EmailSender;

/**
 * Sends the verification email after the account/token change is committed, on an async thread
 * (best-effort, ARCHITECTURE section 4.8): a delivery failure is logged without the address or
 * the link and never rolls back registration. The user can request a new email (UC-03.3).
 */
@Component
public class VerificationEmailNotifier {

    static final String SUBJECT = "Xác minh email tài khoản Mâm Xanh";
    static final String VERIFY_PATH = "/xac-minh-email";

    private static final Logger log = LoggerFactory.getLogger(VerificationEmailNotifier.class);

    private final EmailSender emailSender;
    private final AuthProperties authProperties;

    public VerificationEmailNotifier(EmailSender emailSender, AuthProperties authProperties) {
        this.emailSender = emailSender;
        this.authProperties = authProperties;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationEmailRequested(VerificationEmailRequested event) {
        String link = UriComponentsBuilder.fromUriString(authProperties.frontendBaseUrl())
                .path(VERIFY_PATH)
                .queryParam("token", event.rawToken())
                .build()
                .toUriString();
        long hours = authProperties.emailVerificationTtl().toHours();
        String body = """
                Xin chào %s,

                Cảm ơn bạn đã đăng ký tài khoản Mâm Xanh. Vui lòng mở liên kết dưới đây để xác minh email:

                %s

                Liên kết có hiệu lực trong %d giờ và chỉ dùng được một lần. Nếu bạn không đăng ký tài khoản, hãy bỏ qua email này.

                Mâm Xanh
                """.formatted(event.displayName(), link, hours);
        try {
            emailSender.sendPlainText(event.email(), SUBJECT, body);
        } catch (RuntimeException ex) {
            log.warn("Verification email delivery failed: {}", ex.getClass().getSimpleName());
        }
    }
}
