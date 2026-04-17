package id.ac.ui.cs.advprog.donatjs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponse {
    private String subscriptionId;
    private Long campaignId;
    private String campaignTitle;
    private Long amount;
    private String frequency; // e.g., DAILY, WEEKLY, MONTHLY
    private String status;
    private LocalDateTime nextBillingDate;
}
