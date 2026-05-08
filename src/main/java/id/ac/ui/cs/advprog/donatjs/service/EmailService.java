package id.ac.ui.cs.advprog.donatjs.service;

public interface EmailService {

    /**
     * Send a plain-text email. Implementations must not throw on transport
     * failure — callers fire from event listeners where a thrown exception
     * could disrupt unrelated user-facing flows.
     */
    void sendPlainText(String to, String subject, String body);
}
