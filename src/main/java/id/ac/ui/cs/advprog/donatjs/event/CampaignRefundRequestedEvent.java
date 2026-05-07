package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

public class CampaignRefundRequestedEvent extends ApplicationEvent {
    private final Campaign campaign;
    private final BigDecimal amount;

    public CampaignRefundRequestedEvent(Object source, Campaign campaign, BigDecimal amount) {
        super(source);
        this.campaign = campaign;
        this.amount = amount;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
