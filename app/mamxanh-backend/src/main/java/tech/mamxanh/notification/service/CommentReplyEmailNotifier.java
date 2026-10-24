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
 * Sends the reply email after the reply is committed, on an async thread (AC-49.2, best-effort per
 * ARCHITECTURE section 4.8). A delivery failure is logged without the address and never rolls back the
 * reply or its in-app notification (AC-49.3).
 */
@Component
public class CommentReplyEmailNotifier {

    static final String SUBJECT = "Có phản hồi mới cho bình luận của bạn trên Mâm Xanh";

    private static final Logger log = LoggerFactory.getLogger(CommentReplyEmailNotifier.class);

    private final EmailSender emailSender;
    private final AuthProperties authProperties;

    public CommentReplyEmailNotifier(EmailSender emailSender, AuthProperties authProperties) {
        this.emailSender = emailSender;
        this.authProperties = authProperties;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentReplyEmailRequested(CommentReplyEmailRequested event) {
        String link = authProperties.frontendBaseUrl().replaceAll("/+$", "") + event.targetPath();
        String body = """
                Xin chào %s,

                %s đã trả lời bình luận của bạn trong công thức “%s”. Mở liên kết dưới đây để xem phản hồi:

                %s

                Bạn có thể tắt email này trong Cài đặt hồ sơ trên Mâm Xanh; thông báo trong ứng dụng vẫn được giữ.

                Mâm Xanh
                """.formatted(event.displayName(), event.replierName(), event.recipeTitle(), link);
        try {
            emailSender.sendPlainText(event.email(), SUBJECT, body);
        } catch (RuntimeException ex) {
            log.warn("Comment reply email delivery failed: {}", ex.getClass().getSimpleName());
        }
    }
}
