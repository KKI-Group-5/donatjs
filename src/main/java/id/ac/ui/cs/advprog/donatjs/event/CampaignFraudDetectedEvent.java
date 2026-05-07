package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import org.springframework.context.ApplicationEvent;

public class CampaignFraudDetectedEvent extends ApplicationEvent {
    private final Campaign campaign;

    public CampaignFraudDetectedEvent(Object source, Campaign campaign) {
        super(source);
        this.campaign = campaign;
    }

    public Campaign getCampaign() {
        return campaign;
    }
}
