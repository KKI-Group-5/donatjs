package id.ac.ui.cs.advprog.donatjs.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class DisputeDTO {

    private UUID id;
    private UUID userId;
    private String userEmail;
    private String reason;
    private String status;
    private String adminNotes;
    private LocalDateTime createdAt;

    public DisputeDTO() {
        // Default constructor required by JPA/Jackson
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
