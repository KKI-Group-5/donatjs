package id.ac.ui.cs.advprog.donatjs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserActivityUpdate {
    private String userId;
    private String entityId; // e.g., Campaign ID or Donation ID
    private ActivityType activityType;
    private ActivityStatus status;
    private String reason;

    public enum ActivityType {
        CAMPAIGN, DONATION
    }

    public enum ActivityStatus {
        NORMAL, FRAUD, REJECTED, SUSPICIOUS
    }
}
