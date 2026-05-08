package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignStatusChangedEventListener {

    private final SubscriptionService subscriptionService;

    @EventListener
    public void handleStatusChange(CampaignStatusChangedEvent event) {
        if (!event.shouldTerminateSubscriptions()) {
            return;
        }
        String reason = "campaign moved to " + event.getNewStatus();
        int terminated = subscriptionService
                .terminateActiveSubscriptionsForCampaign(event.getCampaignId(), reason);
        if (terminated > 0) {
            log.info("Auto-terminated {} subscription(s) due to campaign {} -> {}",
                    terminated, event.getCampaignId(), event.getNewStatus());
        }
    }
}
