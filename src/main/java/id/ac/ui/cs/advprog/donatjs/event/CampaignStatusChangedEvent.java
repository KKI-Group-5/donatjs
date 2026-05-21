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

    public boolean shouldTerminateSubscriptions() {
        return newStatus == CampaignStatus.DELETED
                || newStatus == CampaignStatus.CANCELLED
                || newStatus == CampaignStatus.FRAUD
                || newStatus == CampaignStatus.REJECTED;
    }
}
