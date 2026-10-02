package tech.mamxanh.integration.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class EmailSenderTest {

    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
    private final JavaMailSender javaMailSender = mock(JavaMailSender.class);

    @Test
    void sendsPlainTextFromConfiguredAddress() {
        when(provider.getIfAvailable()).thenReturn(javaMailSender);
        EmailSender sender = new EmailSender(provider, "no-reply@mamxanh.test");

        sender.sendPlainText("an@example.com", "Chủ đề", "Nội dung");

        ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(message.capture());
        assertThat(message.getValue().getFrom()).isEqualTo("no-reply@mamxanh.test");
        assertThat(message.getValue().getTo()).containsExactly("an@example.com");
        assertThat(message.getValue().getSubject()).isEqualTo("Chủ đề");
        assertThat(message.getValue().getText()).isEqualTo("Nội dung");
    }

    @Test
    void skipsSilentlyWhenSmtpIsNotConfigured() {
        when(provider.getIfAvailable()).thenReturn(null);
        EmailSender sender = new EmailSender(provider, "no-reply@mamxanh.test");

        assertThatCode(() -> sender.sendPlainText("an@example.com", "Chủ đề", "Nội dung")).doesNotThrowAnyException();
    }

    @Test
    void skipsWhenSmtpHostIsBlank() {
        JavaMailSenderImpl blankHostSender = spy(new JavaMailSenderImpl());
        blankHostSender.setHost("");
        when(provider.getIfAvailable()).thenReturn(blankHostSender);
        EmailSender sender = new EmailSender(provider, "no-reply@mamxanh.test");

        sender.sendPlainText("an@example.com", "Chủ đề", "Nội dung");

        verify(blankHostSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void skipsWhenSenderAddressIsMissing() {
        when(provider.getIfAvailable()).thenReturn(javaMailSender);
        EmailSender sender = new EmailSender(provider, "");

        sender.sendPlainText("an@example.com", "Chủ đề", "Nội dung");

        verify(javaMailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }
}
