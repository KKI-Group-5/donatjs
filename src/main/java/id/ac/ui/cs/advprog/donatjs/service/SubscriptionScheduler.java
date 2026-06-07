package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.CreateDonationRequest;
import id.ac.ui.cs.advprog.donatjs.event.SubscriptionDebitFailedEvent;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.Subscription;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionStatus;
import id.ac.ui.cs.advprog.donatjs.monitoring.SubscriptionDebitJfrEvent;
import id.ac.ui.cs.advprog.donatjs.repository.SubscriptionRepository;
import io.micrometer.core.annotation.Timed;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final SubscriptionRepository subscriptionRepository;
    private final WalletService walletService;
    private final DonationService donationService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    @Timed(value = "subscription.debit.duration",
           description = "Total time to process all due subscription debits in a scheduler run")
    @Scheduled(cron = "0 0 0 * * *")
    public void processSubscriptions() {
        List<Subscription> due = subscriptionRepository
                .findByStatusAndNextDebitDateLessThanEqual(SubscriptionStatus.ACTIVE, LocalDate.now());

        log.info("Subscription scheduler running: {} subscription(s) due", due.size());

        for (Subscription sub : due) {
            SubscriptionDebitJfrEvent jfrEvent = new SubscriptionDebitJfrEvent();
            jfrEvent.subscriptionId = sub.getId() != null ? sub.getId() : 0L;
            jfrEvent.userId        = sub.getUserId();
            jfrEvent.campaignId    = sub.getCampaignId() != null ? sub.getCampaignId() : 0L;
            jfrEvent.amount        = sub.getAmount() != null ? sub.getAmount() : 0L;
            jfrEvent.begin();

            try {
                transactionTemplate.execute(status -> {
                    processSingleSubscription(sub);
                    return null;
                });
                jfrEvent.success = true;
                log.info("Subscription debit SUCCESS: subscriptionId={}, userId={}, campaignId={}, amount={}",
                        sub.getId(), sub.getUserId(), sub.getCampaignId(), sub.getAmount());

            } catch (IllegalStateException e) {
                recordDebitFailure(jfrEvent, sub, e);
                if (isInsufficientBalance(e)) {
                    eventPublisher.publishEvent(new SubscriptionDebitFailedEvent(
                            this,
                            sub.getId(),
                            sub.getUserId(),
                            sub.getCampaignId(),
                            sub.getAmount(),
                            e.getMessage()));
                }
            } catch (EntityNotFoundException | IllegalArgumentException e) {
                recordDebitFailure(jfrEvent, sub, e);
            } finally {
                jfrEvent.commit();
            }
        }
    }

    private void processSingleSubscription(Subscription sub) {
        validateSubscription(sub);

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
    }

    private void validateSubscription(Subscription sub) {
        if (sub.getUserId() == null || sub.getUserId().isBlank()
                || sub.getCampaignId() == null
                || sub.getAmount() == null
                || sub.getFrequency() == null
                || sub.getNextDebitDate() == null) {
            throw new IllegalArgumentException("Subscription " + sub.getId() + " has incomplete debit data");
        }
    }

    private void recordDebitFailure(SubscriptionDebitJfrEvent jfrEvent, Subscription sub, RuntimeException e) {
        String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        jfrEvent.success = false;
        jfrEvent.failureReason = reason;
        log.warn("Subscription debit SKIPPED: subscriptionId={}, userId={}, campaignId={}: {}",
                sub.getId(), sub.getUserId(), sub.getCampaignId(), reason);
    }

    private boolean isInsufficientBalance(IllegalStateException e) {
        String message = e.getMessage();
        return message != null && message.contains("Insufficient balance");
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
