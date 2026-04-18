package id.ac.ui.cs.advprog.donatjs.dto;

import id.ac.ui.cs.advprog.donatjs.model.Subscription.Interval;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateSubscriptionRequest {

    @NotNull
    private Long campaignId;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private Interval interval;
}
