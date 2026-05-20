package id.ac.ui.cs.advprog.donatjs.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class SubscriptionDebitFailedEvent extends ApplicationEvent {

    private final Long subscriptionId;
    private final String userId;
    private final Long campaignId;
    private final Long amount;
    private final String reason;

    public SubscriptionDebitFailedEvent(Object source,
                                        Long subscriptionId,
                                        String userId,
                                        Long campaignId,
                                        Long amount,
                                        String reason) {
        super(source);
        this.subscriptionId = subscriptionId;
        this.userId = userId;
        this.campaignId = campaignId;
        this.amount = amount;
        this.reason = reason;
    }
}
