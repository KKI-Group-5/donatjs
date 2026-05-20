package id.ac.ui.cs.advprog.donatjs.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

@Slf4j
@Component
public class ProfileUpdatedEventListener {

    @Async
    @EventListener
    public void syncCampaignModule(ProfileUpdatedEvent event) {
        log.info("Profile sync dispatched to Campaign module for user {}", event.getUserId());
    }

    @Async
    @EventListener
    public void syncDonationModule(ProfileUpdatedEvent event) {
        log.info("Profile sync dispatched to Donation module for user {}", event.getUserId());
    }
}
