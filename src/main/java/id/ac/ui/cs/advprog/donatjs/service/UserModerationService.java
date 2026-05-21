package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.AdminNotification;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;

import java.util.List;
import java.util.UUID;

public interface UserModerationService {

    void reportFraudActivity(String userEmail, String reason);

    void suspendUser(UUID userId);

    void unsuspendUser(UUID userId);

    List<AppUser> getFlaggedUsers();

    List<AppUser> getSuspendedUsers();

    List<AdminNotification> getAdminNotifications();

    List<AdminNotification> getUnreadAdminNotifications();

    void markNotificationRead(Long notificationId);

    boolean isUserSuspended(String email);
}
