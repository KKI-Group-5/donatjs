package id.ac.ui.cs.advprog.donatjs.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(classes = EmailServiceImpl.class)
class EmailServiceImplTest {

    @Autowired
    private EmailService emailService;

    @Test
    void testSendEmail() {
        assertDoesNotThrow(() -> 
            emailService.sendEmail("test@test.com", "Test Subject", "Test Body")
        );
    }
}
