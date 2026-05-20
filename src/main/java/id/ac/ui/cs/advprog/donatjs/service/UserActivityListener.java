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
    private final EmailService emailService;

    @Async
    @EventListener
    @Transactional
    @SuppressWarnings("null")
    public void handleRejectedDonation(RejectedDonationEvent event) {
        String userIdStr = event.getDonation().getUserId();
        log.info("Handling rejected donation for user: {}", userIdStr);

        userRepository.findById(UUID.fromString(userIdStr)).ifPresent(user -> {
            user.setRejectedDonationCount(user.getRejectedDonationCount() + 1);
            
            if (!user.isFlaggedForReview() && user.getRejectedDonationCount() + user.getRejectedCampaignCount() >= 3) {
                user.setFlaggedForReview(true);
                log.warn("User {} has been FLAGGED FOR REVIEW due to high rejection count.", userIdStr);
                
                // Send emails
                emailService.sendEmail(user.getEmail(), "Account Warning: High Rejection Rate", 
                    "Your account has been flagged for review due to multiple rejected donations or campaigns.");
                emailService.sendEmail("admin@donatjs.com", "Action Required: Account Flagged", 
                    "User " + user.getEmail() + " (" + userIdStr + ") has been flagged for review after reaching 3 rejections.");
            }
            
            userRepository.save(user);
        });
    }

    @Async
    @EventListener
    @Transactional
    @SuppressWarnings("null")
    public void handleRejectedCampaign(RejectedCampaignEvent event) {
        String userIdStr = event.getCreatorId();
        if (userIdStr == null || userIdStr.isBlank()) return;
        
        log.info("Handling rejected campaign for user: {}", userIdStr);

        userRepository.findById(UUID.fromString(userIdStr)).ifPresent(user -> {
            user.setRejectedCampaignCount(user.getRejectedCampaignCount() + 1);
            
            if (!user.isFlaggedForReview() && user.getRejectedDonationCount() + user.getRejectedCampaignCount() >= 3) {
                user.setFlaggedForReview(true);
                log.warn("User {} has been FLAGGED FOR REVIEW due to high rejection count.", userIdStr);
                
                // Send emails
                emailService.sendEmail(user.getEmail(), "Account Warning: High Rejection Rate", 
                    "Your account has been flagged for review due to multiple rejected donations or campaigns.");
                emailService.sendEmail("admin@donatjs.com", "Action Required: Account Flagged", 
                    "User " + user.getEmail() + " (" + userIdStr + ") has been flagged for review after reaching 3 rejections.");
            }
            
            userRepository.save(user);
        });
    }
}
