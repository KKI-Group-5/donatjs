package id.ac.ui.cs.advprog.donatjs.dto;

import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionFrequency;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSubscriptionRequest {

    @NotNull(message = "User ID is required")
    private String userId;

    @NotNull(message = "Campaign ID is required")
    private Long campaignId;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least Rp 1")
    private Long amount;

    @NotNull(message = "Frequency is required")
    private SubscriptionFrequency frequency;
}
