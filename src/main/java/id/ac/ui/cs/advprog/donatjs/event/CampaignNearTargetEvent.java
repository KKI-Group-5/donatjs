package id.ac.ui.cs.advprog.donatjs.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class CampaignNearTargetEvent extends ApplicationEvent {

    private final Long campaignId;
    private final String campaignTitle;
    private final BigDecimal totalRaised;
    private final BigDecimal targetAmount;

    public CampaignNearTargetEvent(Object source,
                                   Long campaignId,
                                   String campaignTitle,
                                   BigDecimal totalRaised,
                                   BigDecimal targetAmount) {
        super(source);
        this.campaignId = campaignId;
        this.campaignTitle = campaignTitle;
        this.totalRaised = totalRaised;
        this.targetAmount = targetAmount;
    }
}
