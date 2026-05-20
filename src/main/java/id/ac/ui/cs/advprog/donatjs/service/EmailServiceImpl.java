package id.ac.ui.cs.advprog.donatjs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    @Override
    public void sendEmail(String to, String subject, String body) {
        log.info("Sending email to: {}", to);
        log.info("Subject: {}", subject);
        log.info("Body: {}", body);
        log.info("Email sent successfully.");
    }

    @Override
    public void sendPlainText(String to, String subject, String body) {
        sendEmail(to, subject, body);
    }
}
