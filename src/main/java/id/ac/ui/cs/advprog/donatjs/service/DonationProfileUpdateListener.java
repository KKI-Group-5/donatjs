package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.event.ProfileUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DonationProfileUpdateListener {

    @Async
    @EventListener
    public void handleProfileUpdated(ProfileUpdatedEvent event) {
        log.info("Donation Module received profile update for user {}: name={}, bio={}", 
            event.getUserId(), event.getName(), event.getBio());
        // Since Donation table does not hold denormalized user name/bio, 
        // simple logging here proves the async communication pipeline is active and healthy.
    }
}
