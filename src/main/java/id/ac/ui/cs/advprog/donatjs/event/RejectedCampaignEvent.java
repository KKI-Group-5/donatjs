package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import org.springframework.context.ApplicationEvent;

public class RejectedCampaignEvent extends ApplicationEvent {

    private final Campaign campaign;

    public RejectedCampaignEvent(Object source, Campaign campaign) {
        super(source);
        this.campaign = campaign;
    }

    public Campaign getCampaign() {
        return campaign;
    }
}
