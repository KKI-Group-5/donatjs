package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.event.RejectedCampaignEvent;
import id.ac.ui.cs.advprog.donatjs.event.RejectedDonationEvent;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActivityListener {

    private final UserRepository userRepository;

    @Async
    @EventListener
    @Transactional
    @SuppressWarnings("null")
    public void handleRejectedDonation(RejectedDonationEvent event) {
        String userIdStr = event.getDonation().getUserId();
        log.info("Handling rejected donation for user: {}", userIdStr);

        userRepository.findById(UUID.fromString(userIdStr)).ifPresent(user -> {
            user.setRejectedDonationCount(user.getRejectedDonationCount() + 1);
            
            // Milestone 4: Add suspension logic here when threshold matches
            if (user.getRejectedDonationCount() + user.getRejectedCampaignCount() >= 3) {
                user.setSuspended(true);
                log.warn("User {} has been SUSPENDED due to high rejection count.", userIdStr);
            }
            
            userRepository.save(user);
        });
    }

    @Async
    @EventListener
    @Transactional
    @SuppressWarnings("null")
    public void handleRejectedCampaign(RejectedCampaignEvent event) {
        String userIdStr = event.getCampaign().getCreatorId();
        if (userIdStr == null || userIdStr.isBlank()) return;
        
        log.info("Handling rejected campaign for user: {}", userIdStr);

        userRepository.findById(UUID.fromString(userIdStr)).ifPresent(user -> {
            user.setRejectedCampaignCount(user.getRejectedCampaignCount() + 1);
            
            // Milestone 4: Add suspension logic here when threshold matches
            if (user.getRejectedDonationCount() + user.getRejectedCampaignCount() >= 3) {
                user.setSuspended(true);
                log.warn("User {} has been SUSPENDED due to high rejection count.", userIdStr);
            }
            
            userRepository.save(user);
        });
    }
}
