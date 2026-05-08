package id.ac.ui.cs.advprog.donatjs.controller;

import id.ac.ui.cs.advprog.donatjs.dto.FraudActivityReportRequest;
import id.ac.ui.cs.advprog.donatjs.model.AdminNotification;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.service.UserModerationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class UserModerationController {

    private final UserModerationService moderationService;

    public UserModerationController(UserModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping("/api/users/report-fraud-activity")
    public ResponseEntity<Void> reportFraudActivity(@Valid @RequestBody FraudActivityReportRequest request) {
        moderationService.reportFraudActivity(request.getUserEmail(), request.getReason());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/admin/users/suspended")
    public ResponseEntity<List<AppUser>> getSuspendedUsers() {
        return ResponseEntity.ok(moderationService.getSuspendedUsers());
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
