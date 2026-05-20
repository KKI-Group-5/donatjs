package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.dto.UserActivityUpdate;
import id.ac.ui.cs.advprog.donatjs.service.UserActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RejectedDonationEventListener {

    private final UserActivityService userActivityService;

    @EventListener
    public void handleRejectedDonation(RejectedDonationEvent event) {
        DonationResponse donation = event.getDonation();
        userActivityService.recordActivity(new UserActivityUpdate(
                donation.getUserId(),
                String.valueOf(donation.getId()),
                UserActivityUpdate.ActivityType.DONATION,
                UserActivityUpdate.ActivityStatus.REJECTED,
                "Donation exceeded the Rp 5,000,000 limit"
        ));

        log.warn("[ADMIN NOTIFICATION] Rejected donation detected: donationId={}, userId={}, campaignId={}, amount={}",
                donation.getId(), donation.getUserId(), donation.getCampaignId(), donation.getAmount());
    }
}
