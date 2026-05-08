package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.AdminNotification;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.repository.AdminNotificationRepository;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserModerationServiceImpl implements UserModerationService {

    static final int FRAUD_THRESHOLD = 3;

    private final UserRepository userRepository;
    private final AdminNotificationRepository notificationRepository;

    public UserModerationServiceImpl(UserRepository userRepository,
                                     AdminNotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public void reportFraudActivity(String userEmail, String reason) {
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userEmail));

        user.setFraudActivityCount(user.getFraudActivityCount() + 1);

        if (!user.isFlagged() && user.getFraudActivityCount() >= FRAUD_THRESHOLD) {
            user.setFlagged(true);

            String message = String.format(
                    "User '%s' (%s) has been flagged for admin review. " +
                    "Total fraud activities: %d. Latest reason: %s",
                    user.getName(), user.getEmail(),
                    user.getFraudActivityCount(), reason);

            notificationRepository.save(new AdminNotification(user.getEmail(), user.getName(), message));
        }

        userRepository.save(user);
    }

    @Override
    @Transactional
    public void suspendUser(UUID userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        user.setSuspended(true);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void unsuspendUser(UUID userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
        user.setSuspended(false);
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppUser> getFlaggedUsers() {
        return userRepository.findByFlaggedTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppUser> getSuspendedUsers() {
        return userRepository.findBySuspendedTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminNotification> getAdminNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminNotification> getUnreadAdminNotifications() {
        return notificationRepository.findByReadFalseOrderByCreatedAtDesc();
    }

    @Override
    @Transactional
    public void markNotificationRead(Long notificationId) {
        AdminNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found: " + notificationId));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserSuspended(String email) {
        return userRepository.findByEmail(email)
                .map(AppUser::isSuspended)
                .orElse(false);
    }
}
