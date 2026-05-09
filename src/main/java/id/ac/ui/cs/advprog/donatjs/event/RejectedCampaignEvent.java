package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import org.springframework.context.ApplicationEvent;

public class RejectedCampaignEvent extends ApplicationEvent {
    private final Campaign campaign;
    private final String creatorId;

    public RejectedCampaignEvent(Object source, Campaign campaign, String creatorId) {
        super(source);
        this.campaign = campaign;
        this.creatorId = creatorId;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public String getCreatorId() {
        return creatorId;
    }
}
