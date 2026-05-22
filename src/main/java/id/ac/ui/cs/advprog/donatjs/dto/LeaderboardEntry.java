package id.ac.ui.cs.advprog.donatjs.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaderboardEntry {
    private int rank;
    private String userId;
    private String displayName;
    private Long totalAmount;
    private Long donationCount;
    private Long campaignCount;
}
