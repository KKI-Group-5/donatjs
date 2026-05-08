package id.ac.ui.cs.advprog.donatjs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;

    public SmtpEmailService(JavaMailSender mailSender,
                            @Value("${donatjs.email.from-address}") String fromAddress,
                            @Value("${donatjs.email.from-name}") String fromName) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        sendPlainText(to, subject, body);
    }

    @Override
    public void sendPlainText(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("Skipping email — recipient is blank. subject={}", subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + fromAddress + ">");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent. to={}, subject={}", to, subject);
        } catch (Exception ex) {
            log.error("Email failed. to={}, subject={}, error={}", to, subject, ex.getMessage());
        }
    }
}
