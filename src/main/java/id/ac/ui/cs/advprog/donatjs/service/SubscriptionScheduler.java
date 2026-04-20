package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.CreateDonationRequest;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.Subscription;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionStatus;
import id.ac.ui.cs.advprog.donatjs.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final WalletService walletService;
    private final DonationService donationService;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processSubscriptions() {
        List<Subscription> due = subscriptionRepository
                .findByStatusAndNextDebitDateLessThanEqual(SubscriptionStatus.ACTIVE, LocalDate.now());

        log.info("Subscription scheduler running: {} subscription(s) due", due.size());

        for (Subscription sub : due) {
            try {
                walletService.deductBalance(
                        sub.getUserId(),
                        sub.getAmount().doubleValue(),
                        "Subscription to campaign: " + sub.getCampaignId()
                );

                CreateDonationRequest donationRequest = new CreateDonationRequest();
                donationRequest.setUserId(sub.getUserId());
                donationRequest.setCampaignId(sub.getCampaignId());
                donationRequest.setAmount(sub.getAmount());
                donationRequest.setPaymentMethod(Donation.PaymentMethod.WALLET);
                donationRequest.setType(Donation.DonationType.SUBSCRIPTION);
                donationService.createDonation(donationRequest);

                advanceNextDebitDate(sub);
                subscriptionRepository.save(sub);

                log.info("Subscription debit SUCCESS: subscriptionId={}, userId={}, campaignId={}, amount={}",
                        sub.getId(), sub.getUserId(), sub.getCampaignId(), sub.getAmount());

            } catch (IllegalStateException e) {
                // Insufficient balance — skip this cycle. M5 adds notification logic.
                log.warn("Subscription debit SKIPPED (insufficient balance): subscriptionId={}, userId={}: {}",
                        sub.getId(), sub.getUserId(), e.getMessage());
            }
        }
    }

    private void advanceNextDebitDate(Subscription sub) {
        LocalDate next = switch (sub.getFrequency()) {
            case DAILY   -> sub.getNextDebitDate().plusDays(1);
            case WEEKLY  -> sub.getNextDebitDate().plusWeeks(1);
            case MONTHLY -> sub.getNextDebitDate().plusMonths(1);
        };
        sub.setNextDebitDate(next);
    }
}
