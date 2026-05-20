package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.AdminNotification;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.AdminNotificationRepository;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class UserModerationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminNotificationRepository notificationRepository;

    @InjectMocks
    private UserModerationServiceImpl service;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setName("Test User");
    }

    @Test
    void reportFraudActivity_incrementsCount() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.reportFraudActivity("test@example.com", "REJECTED_DONATION");

        assertThat(user.getFraudActivityCount()).isEqualTo(1);
        assertThat(user.isFlagged()).isFalse();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void reportFraudActivity_flagsUserAtThreshold() {
        user.setFraudActivityCount(UserModerationServiceImpl.FRAUD_THRESHOLD - 1);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.reportFraudActivity("test@example.com", "REJECTED_CAMPAIGN");

        assertThat(user.getFraudActivityCount()).isEqualTo(UserModerationServiceImpl.FRAUD_THRESHOLD);
        assertThat(user.isFlagged()).isTrue();

        ArgumentCaptor<AdminNotification> captor = ArgumentCaptor.forClass(AdminNotification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUserEmail()).isEqualTo("test@example.com");
        assertThat(captor.getValue().getMessage()).contains("REJECTED_CAMPAIGN");
    }

    @Test
    void reportFraudActivity_doesNotCreateDuplicateNotificationIfAlreadyFlagged() {
        user.setFraudActivityCount(5);
        user.setFlagged(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.reportFraudActivity("test@example.com", "REJECTED_DONATION");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void reportFraudActivity_throwsWhenUserNotFound() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reportFraudActivity("missing@example.com", "REJECTED_DONATION"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void suspendUser_setsSuspendedTrue() {
        UUID id = user.getId();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.suspendUser(id);

        assertThat(user.isSuspended()).isTrue();
    }

    @Test
    void unsuspendUser_setsSuspendedFalse() {
        user.setSuspended(true);
        UUID id = user.getId();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.unsuspendUser(id);

        assertThat(user.isSuspended()).isFalse();
    }

    @Test
    void suspendUser_throwsWhenUserNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.suspendUser(id))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void isUserSuspended_returnsTrueWhenSuspended() {
        user.setSuspended(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThat(service.isUserSuspended("test@example.com")).isTrue();
    }

    @Test
    void isUserSuspended_returnsFalseWhenNotSuspended() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThat(service.isUserSuspended("test@example.com")).isFalse();
    }

    @Test
    void isUserSuspended_returnsFalseWhenUserNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThat(service.isUserSuspended("ghost@example.com")).isFalse();
    }

    @Test
    void getFlaggedUsers_delegatesToRepository() {
        when(userRepository.findByFlaggedTrue()).thenReturn(List.of(user));

        List<AppUser> result = service.getFlaggedUsers();

        assertThat(result).containsExactly(user);
    }

    @Test
    void markNotificationRead_setsReadTrue() {
        AdminNotification notification = new AdminNotification("e@e.com", "Name", "msg");
        notification.setId(1L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.markNotificationRead(1L);

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markNotificationRead_throwsWhenNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markNotificationRead(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
