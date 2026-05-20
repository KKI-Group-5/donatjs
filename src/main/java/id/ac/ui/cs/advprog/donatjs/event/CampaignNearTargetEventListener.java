package id.ac.ui.cs.advprog.donatjs.event;

import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
import id.ac.ui.cs.advprog.donatjs.repository.SavedCampaignRepository;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import id.ac.ui.cs.advprog.donatjs.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CampaignNearTargetEventListener {

    private final SavedCampaignRepository savedCampaignRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleNearTarget(CampaignNearTargetEvent event) {
        String campaignIdStr = String.valueOf(event.getCampaignId());

        List<SavedCampaign> favorites = savedCampaignRepository.findByCampaignId(campaignIdStr);

        if (favorites.isEmpty()) {
            log.info("Campaign {} crossed near-target threshold but no users have it saved", campaignIdStr);
            return;
        }

        String subject = "\"" + event.getCampaignTitle() + "\" is almost at its target!";
        String body = buildBody(event);

        log.info("Fanning out near-target email to {} user(s) for campaign {}", favorites.size(), campaignIdStr);

        for (SavedCampaign saved : favorites) {
            resolveEmail(saved.getUserId()).ifPresent(addr ->
                    emailService.sendPlainText(addr, subject, body));
        }
    }

    private java.util.Optional<String> resolveEmail(String userIdString) {
        try {
            return userRepository.findById(java.util.Objects.requireNonNull(UUID.fromString(userIdString)))
                    .map(AppUser::getEmail);
        } catch (IllegalArgumentException ex) {
            log.warn("Skipping non-UUID userId on saved campaign: {}", userIdString);
            return java.util.Optional.empty();
        }
    }

    private String buildBody(CampaignNearTargetEvent event) {
        NumberFormat rp = NumberFormat.getNumberInstance(Locale.US);
        return ""
                + "Good news!\n\n"
                + "A campaign on your saved list — \"" + event.getCampaignTitle() + "\" — has just crossed 98% of its target.\n\n"
                + "  Raised:  Rp " + rp.format(event.getTotalRaised()) + "\n"
                + "  Target:  Rp " + rp.format(event.getTargetAmount()) + "\n\n"
                + "It only needs a little more to close. If you'd like to be the donor that pushes it over the line, head to DonatJS and chip in.\n\n"
                + "— DonatJS";
    }
}
