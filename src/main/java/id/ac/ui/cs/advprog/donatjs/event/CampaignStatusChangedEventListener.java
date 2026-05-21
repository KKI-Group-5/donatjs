package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.repository.SavedCampaignRepository;
import id.ac.ui.cs.advprog.donatjs.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignStatusChangedEventListener {

    private final SubscriptionService subscriptionService;
    private final SavedCampaignRepository savedCampaignRepository;

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

    // M5 — saved-campaign orphan cleanup. Async + own @Transactional so the
    // delete commits independently of the publishing transaction; if the
    // publisher rolls back, the cleanup will just no-op on its next read.
    @Async
    @EventListener
    @Transactional
    public void removeOrphanSavedCampaigns(CampaignStatusChangedEvent event) {
        if (!event.shouldTerminateSubscriptions()) {
            return;
        }
        long removed = savedCampaignRepository.deleteByCampaignId(String.valueOf(event.getCampaignId()));
        if (removed > 0) {
            log.info("Removed {} saved-campaign row(s) for campaign {} -> {}",
                    removed, event.getCampaignId(), event.getNewStatus());
        }
    }
}
