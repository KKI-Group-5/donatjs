package id.ac.ui.cs.advprog.donatjs.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class CampaignNotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(CampaignNotificationEventListener.class);

    @Async
    @EventListener
    public void handlePayout(CampaignPayoutRequestedEvent event) {
        log.info("Notification: payout requested for campaignId={}, amount={}",
                event.getCampaign().getId(), event.getAmount());
    }

    @Async
    @EventListener
    public void handleRefund(CampaignRefundRequestedEvent event) {
        log.info("Notification: refund requested for campaignId={}, amount={}",
                event.getCampaign().getId(), event.getAmount());
    }

    @Async
    @EventListener
    public void handleFraud(CampaignFraudDetectedEvent event) {
        log.warn("Notification: campaign marked as FRAUD campaignId={}",
                event.getCampaign().getId());
    }
}
