package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.CreateSubscriptionRequest;
import id.ac.ui.cs.advprog.donatjs.dto.UpdateSubscriptionRequest;
import id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.model.Subscription;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.Interval;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.Status;
import id.ac.ui.cs.advprog.donatjs.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Engine for managing recurring (daily/weekly/monthly) donations. Subscriptions
 * always debit the user's internal wallet — per the M3 checklist, external
 * payment methods are not allowed for subscriptions.
 *
 * <p>A {@link Scheduled} job ticks every minute and bills every
 * {@link Status#ACTIVE} subscription whose {@code nextBillingAt} is in the
 * past. On success the campaign total is advanced; on insufficient balance the
 * subscription is marked {@link Status#PAUSED} so it stops retrying until the
 * user tops up.</p>
 */
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final CampaignService campaignService;
    private final WalletService walletService;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               CampaignService campaignService,
                               WalletService walletService) {
        this.subscriptionRepository = subscriptionRepository;
        this.campaignService = campaignService;
        this.walletService = walletService;
    }

    // ── Use cases ────────────────────────────────────────────────────────────

    @Transactional
    public Subscription subscribe(String userId, CreateSubscriptionRequest request) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User must be authenticated");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        Campaign campaign = campaignService.findById(request.getCampaignId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Campaign not found: " + request.getCampaignId()));
        if (campaign.getStatus() != CampaignStatus.OPEN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Subscriptions may only be created for OPEN campaigns");
        }

        subscriptionRepository.findByUserIdAndCampaignIdAndStatus(
                        userId, request.getCampaignId(), Status.ACTIVE)
                .ifPresent(s -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "You already have an active subscription for this campaign");
                });

        Subscription subscription = Subscription.builder()
                .userId(userId)
                .campaignId(campaign.getId())
                .campaignTitle(campaign.getTitle())
                .amount(request.getAmount())
                .interval(request.getInterval())
                .status(Status.ACTIVE)
                .nextBillingAt(request.getInterval().advance(LocalDateTime.now()))
                .build();

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Created subscription id={} userId={} campaignId={} interval={} amount={}",
                saved.getId(), userId, saved.getCampaignId(), saved.getInterval(), saved.getAmount());
        return saved;
    }

    @Transactional
    public Subscription updateSubscription(String userId, Long subscriptionId,
                                           UpdateSubscriptionRequest request) {
        Subscription subscription = getOwnedSubscription(userId, subscriptionId);
        if (subscription.getStatus() == Status.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot edit a cancelled subscription");
        }
        if (request.getAmount() != null) {
            if (request.getAmount().signum() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
            }
            subscription.setAmount(request.getAmount());
        }
        if (request.getInterval() != null && request.getInterval() != subscription.getInterval()) {
            subscription.setInterval(request.getInterval());
            subscription.setNextBillingAt(request.getInterval().advance(LocalDateTime.now()));
        }
        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription cancel(String userId, Long subscriptionId) {
        Subscription subscription = getOwnedSubscription(userId, subscriptionId);
        if (subscription.getStatus() == Status.CANCELLED) {
            return subscription;
        }
        subscription.setStatus(Status.CANCELLED);
        Subscription saved = subscriptionRepository.save(subscription);
        log.info("Cancelled subscription id={} userId={}", saved.getId(), userId);
        return saved;
    }

    @Transactional
    public Subscription resume(String userId, Long subscriptionId) {
        Subscription subscription = getOwnedSubscription(userId, subscriptionId);
        if (subscription.getStatus() == Status.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cancelled subscriptions cannot be resumed — create a new one instead");
        }
        subscription.setStatus(Status.ACTIVE);
        subscription.setNextBillingAt(subscription.getInterval().advance(LocalDateTime.now()));
        subscription.setLastFailureMessage(null);
        return subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public List<Subscription> listByUser(String userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ── Scheduler ────────────────────────────────────────────────────────────

    /**
     * Runs every minute. Charges every active subscription whose
     * {@code nextBillingAt} has elapsed. Moved out to a dedicated method so it
     * can also be invoked by tests.
     */
    @Scheduled(fixedDelayString = "${donatjs.subscription.billing.interval-ms:60000}")
    public void processDueBillings() {
        List<Subscription> due = subscriptionRepository
                .findByStatusAndNextBillingAtBefore(Status.ACTIVE, LocalDateTime.now());
        if (due.isEmpty()) {
            return;
        }
        log.info("Processing {} due subscription(s)", due.size());
        for (Subscription subscription : due) {
            try {
                chargeOnce(subscription);
            } catch (RuntimeException ex) {
                log.error("Unexpected failure while billing subscription {}: {}",
                        subscription.getId(), ex.getMessage());
            }
        }
    }

    @Transactional
    protected void chargeOnce(Subscription subscription) {
        Campaign campaign = campaignService.findById(subscription.getCampaignId()).orElse(null);
        if (campaign == null || campaign.getStatus() != CampaignStatus.OPEN) {
            log.info("Auto-cancelling subscription {} — underlying campaign is no longer OPEN",
                    subscription.getId());
            subscription.setStatus(Status.CANCELLED);
            subscription.setLastFailureMessage("Campaign is no longer accepting subscriptions");
            subscriptionRepository.save(subscription);
            return;
        }

        double amount = subscription.getAmount().doubleValue();
        try {
            walletService.deductForDonation(subscription.getUserId(), amount, campaign.getTitle());
            campaignService.recordSuccessfulDonation(subscription.getCampaignId(), subscription.getAmount());

            subscription.setLastBilledAt(LocalDateTime.now());
            subscription.setNextBillingAt(subscription.getInterval().advance(LocalDateTime.now()));
            subscription.setLastFailureMessage(null);
            subscriptionRepository.save(subscription);

            log.info("Subscription {} charged Rp {} successfully", subscription.getId(), amount);
        } catch (InsufficientBalanceException ibe) {
            subscription.setStatus(Status.PAUSED);
            subscription.setLastFailureMessage(ibe.getMessage());
            subscriptionRepository.save(subscription);
            log.warn("Subscription {} paused — insufficient balance: {}",
                    subscription.getId(), ibe.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Subscription getOwnedSubscription(String userId, Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Subscription not found: " + subscriptionId));
        if (!subscription.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You do not own this subscription");
        }
        return subscription;
    }
}
