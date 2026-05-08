package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.FraudActivityReportRequest;
import id.ac.ui.cs.advprog.donatjs.model.AdminNotification;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.service.UserModerationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "User Moderation", description = "Endpoints for fraud reporting and admin account management")
public class UserModerationController {

    private final UserModerationService moderationService;

    public UserModerationController(UserModerationService moderationService) {
        this.moderationService = moderationService;
    }

    /**
     * Called by Campaign or Donation modules when a user's campaign/donation is rejected or flagged as fraud.
     * Increments the user's fraud activity count and flags the account if the threshold (≥3) is reached.
     */
    @PostMapping("/api/users/report-fraud-activity")
    public ResponseEntity<Void> reportFraudActivity(@Valid @RequestBody FraudActivityReportRequest request) {
        moderationService.reportFraudActivity(request.getUserEmail(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/admin/users/flagged")
    public ResponseEntity<List<AppUser>> getFlaggedUsers() {
        return ResponseEntity.ok(moderationService.getFlaggedUsers());
    }

    @GetMapping("/api/admin/users/suspended")
    public ResponseEntity<List<AppUser>> getSuspendedUsers() {
        return ResponseEntity.ok(moderationService.getFlaggedUsers());
    }

    @PostMapping("/api/admin/users/{userId}/suspend")
    public ResponseEntity<Void> suspendUser(@PathVariable UUID userId) {
        moderationService.suspendUser(userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/admin/users/{userId}/unsuspend")
    public ResponseEntity<Void> unsuspendUser(@PathVariable UUID userId) {
        moderationService.unsuspendUser(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/admin/notifications")
    public ResponseEntity<List<AdminNotification>> getAdminNotifications() {
        return ResponseEntity.ok(moderationService.getAdminNotifications());
    }

    @GetMapping("/api/admin/notifications/unread")
    public ResponseEntity<List<AdminNotification>> getUnreadNotifications() {
        return ResponseEntity.ok(moderationService.getUnreadAdminNotifications());
    }

    @PatchMapping("/api/admin/notifications/{id}/read")
    public ResponseEntity<Void> markNotificationRead(@PathVariable Long id) {
        moderationService.markNotificationRead(id);
        return ResponseEntity.ok().build();
    }
}
