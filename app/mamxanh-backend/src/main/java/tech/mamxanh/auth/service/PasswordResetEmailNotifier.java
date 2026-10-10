package tech.mamxanh.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

import tech.mamxanh.auth.security.AuthProperties;
import tech.mamxanh.auth.security.PasswordResetProperties;
import tech.mamxanh.integration.email.EmailSender;

/**
 * Sends the password-reset email after the token is committed, on an async thread (best-effort,
 * ARCHITECTURE section 4.8): a delivery failure is logged without the address or the link, and the
 * user can request another email after the cooldown.
 */
@Component
public class PasswordResetEmailNotifier {

    static final String SUBJECT = "Đặt lại mật khẩu tài khoản Mâm Xanh";
    static final String RESET_PATH = "/dat-lai-mat-khau";

    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailNotifier.class);

    private final EmailSender emailSender;
    private final AuthProperties authProperties;
    private final PasswordResetProperties resetProperties;

    public PasswordResetEmailNotifier(EmailSender emailSender, AuthProperties authProperties,
            PasswordResetProperties resetProperties) {
        this.emailSender = emailSender;
        this.authProperties = authProperties;
        this.resetProperties = resetProperties;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetEmailRequested(PasswordResetEmailRequested event) {
        String link = UriComponentsBuilder.fromUriString(authProperties.frontendBaseUrl())
                .path(RESET_PATH)
                .queryParam("token", event.rawToken())
                .build()
                .toUriString();
        long minutes = resetProperties.tokenTtl().toMinutes();
        String body = """
                Xin chào %s,

                Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản Mâm Xanh của bạn. Vui lòng mở liên kết dưới đây để đặt mật khẩu mới:

                %s

                Liên kết có hiệu lực trong %d phút và chỉ dùng được một lần. Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này; mật khẩu hiện tại vẫn được giữ nguyên.

                Mâm Xanh
                """.formatted(event.displayName(), link, minutes);
        try {
            emailSender.sendPlainText(event.email(), SUBJECT, body);
        } catch (RuntimeException ex) {
            log.warn("Password reset email delivery failed: {}", ex.getClass().getSimpleName());
        }
    }
}
