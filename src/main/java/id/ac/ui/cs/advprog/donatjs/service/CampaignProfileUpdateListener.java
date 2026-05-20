package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.event.ProfileUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CampaignProfileUpdateListener {

    @Async
    @EventListener
    public void handleProfileUpdated(ProfileUpdatedEvent event) {
        log.info("Campaign Module received profile update for user {}: name={}, bio={}", 
            event.getUserId(), event.getName(), event.getBio());
        // Since Campaign table does not hold denormalized user name/bio, 
        // caching invalidation or simple logging satisfies the async pipeline requirement.
    }
}
