package id.ac.ui.cs.advprog.donatjs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class SmtpEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private SmtpEmailService smtpEmailService;

    @BeforeEach
    void setUp() {
        smtpEmailService = new SmtpEmailService(mailSender, "noreply@donatjs.com", "DonatJS");
    }

    @Test
    void testSendEmailSuccess() {
        smtpEmailService.sendEmail("test@example.com", "Subject", "Body");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendPlainTextBlankTo() {
        smtpEmailService.sendPlainText("", "Subject", "Body");
        smtpEmailService.sendPlainText(null, "Subject", "Body");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void testSendEmailException() {
        doThrow(new MailSendException("Failed")).when(mailSender).send(any(SimpleMailMessage.class));

        // Should not throw exception to caller, but catch it and log it
        smtpEmailService.sendEmail("test@example.com", "Subject", "Body");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
