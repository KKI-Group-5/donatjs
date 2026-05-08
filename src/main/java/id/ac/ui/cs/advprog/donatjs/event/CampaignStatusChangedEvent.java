package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CampaignStatusChangedEvent extends ApplicationEvent {

    private final Long campaignId;
    private final CampaignStatus previousStatus;
    private final CampaignStatus newStatus;

    public CampaignStatusChangedEvent(Object source,
                                      Long campaignId,
                                      CampaignStatus previousStatus,
                                      CampaignStatus newStatus) {
        super(source);
        this.campaignId = campaignId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    /**
     * True for status transitions that should terminate active subscriptions
     * tied to this campaign (per Milestone 4 "Automatic Termination" rule).
     * CLOSED is a healthy lifecycle end and does NOT terminate subscriptions —
     * it just stops further donations being accepted.
     */
    public boolean shouldTerminateSubscriptions() {
        return newStatus == CampaignStatus.DELETED
                || newStatus == CampaignStatus.CANCELLED
                || newStatus == CampaignStatus.FRAUD
                || newStatus == CampaignStatus.REJECTED;
    }
}
