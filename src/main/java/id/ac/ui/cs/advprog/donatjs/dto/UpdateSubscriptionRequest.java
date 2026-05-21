package id.ac.ui.cs.advprog.donatjs.dto;

import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionFrequency;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSubscriptionRequest {

    @NotNull(message = "Frequency is required")
    private SubscriptionFrequency frequency;
}
