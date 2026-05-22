package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SubscriptionDebitFailedEventListenerTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private SubscriptionDebitFailedEventListener listener;

    @Test
    void handleDebitFailed_sendsEmailToAffectedUser() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(appUser(userId, "u@example.com")));

        listener.handleDebitFailed(new SubscriptionDebitFailedEvent(
                this, 7L, userId.toString(), 42L, 50_000L, "Insufficient balance"));

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPlainText(eq("u@example.com"), subject.capture(), anyString());
        assertThat(subject.getValue()).contains("insufficient balance");
    }

    @Test
    void handleDebitFailed_bodyIncludesAmountAndIds() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(appUser(userId, "u@example.com")));

        listener.handleDebitFailed(new SubscriptionDebitFailedEvent(
                this, 7L, userId.toString(), 42L, 250_000L, "Insufficient balance"));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPlainText(eq("u@example.com"), anyString(), body.capture());
        assertThat(body.getValue()).contains("250,000");
        assertThat(body.getValue()).contains("7");      // subscription id
        assertThat(body.getValue()).contains("42");     // campaign id
    }

    @Test
    void handleDebitFailed_skipsWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        listener.handleDebitFailed(new SubscriptionDebitFailedEvent(
                this, 7L, userId.toString(), 42L, 50_000L, "Insufficient balance"));

        verify(emailService, never()).sendPlainText(anyString(), anyString(), anyString());
    }

    @Test
    void handleDebitFailed_skipsNonUuidUserIdGracefully() {
        listener.handleDebitFailed(new SubscriptionDebitFailedEvent(
                this, 7L, "not-a-uuid", 42L, 50_000L, "Insufficient balance"));

        verify(emailService, never()).sendPlainText(anyString(), anyString(), anyString());
        verify(userRepository, never()).findById(any(UUID.class));
    }

    private static AppUser appUser(UUID id, String email) {
        AppUser u = new AppUser();
        try {
            Field idField = AppUser.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(u, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        u.setEmail(email);
        return u;
    }
}
