package tech.mamxanh.integration.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Thin wrapper over Spring Mail for the Brevo SMTP relay (ARCHITECTURE section 4.8).
 * SMTP credentials come from {@code SPRING_MAIL_*} environment variables and the sender address
 * from {@code MAMXANH_MAIL_FROM}. When SMTP is not configured (no {@code spring.mail.host}) the
 * message is skipped with a warning, so local development without credentials still works.
 * Recipient addresses and message bodies are never logged because bodies contain one-time links.
 */
@Component
public class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public EmailSender(ObjectProvider<JavaMailSender> mailSender, @Value("${mamxanh.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendPlainText(String to, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from.isBlank()) {
            log.warn("Email delivery is not configured (spring.mail.host / mamxanh.mail.from); skipped '{}'", subject);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }
}
