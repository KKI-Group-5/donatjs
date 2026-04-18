package id.ac.ui.cs.advprog.donatjs.dto;

import id.ac.ui.cs.advprog.donatjs.model.Subscription.Interval;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Payload for PATCHing an existing subscription — either change the cadence,
 * the amount, or both. Any {@code null} field is left untouched.
 */
@Data
public class UpdateSubscriptionRequest {

    private Interval interval;

    private BigDecimal amount;
}
