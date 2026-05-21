package id.ac.ui.cs.advprog.donatjs.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RejectedCampaignEventListener {

    private static final Logger log = LoggerFactory.getLogger(RejectedCampaignEventListener.class);

    @Async
    @EventListener
    public void handleRejectedCampaign(RejectedCampaignEvent event) {
        log.warn("Notification: campaign rejected campaignId={}, creatorId={}, title={}",
                event.getCampaign().getId(), event.getCreatorId(), event.getCampaign().getTitle());
    }
}
