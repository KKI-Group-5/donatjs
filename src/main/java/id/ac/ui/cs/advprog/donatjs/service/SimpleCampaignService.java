package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.event.CampaignNearTargetEvent;
import id.ac.ui.cs.advprog.donatjs.event.CampaignStatusChangedEvent;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.event.CampaignFraudDetectedEvent;
import id.ac.ui.cs.advprog.donatjs.event.CampaignPayoutRequestedEvent;
import id.ac.ui.cs.advprog.donatjs.event.CampaignRefundRequestedEvent;
import id.ac.ui.cs.advprog.donatjs.event.RejectedCampaignEvent;
import id.ac.ui.cs.advprog.donatjs.repository.CampaignRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SimpleCampaignService implements CampaignService {

    private static final Logger log = LoggerFactory.getLogger(SimpleCampaignService.class);

    private final CampaignRepository campaignRepository;
    private final CampaignWalletGateway campaignWalletGateway;
    private final ApplicationEventPublisher eventPublisher;
    private final BigDecimal nearTargetThreshold;

    public SimpleCampaignService(CampaignRepository campaignRepository) {
        this(campaignRepository, new NoopCampaignWalletGateway(), event -> {});
    }

    @Autowired
    public SimpleCampaignService(
            CampaignRepository campaignRepository,
            CampaignWalletGateway campaignWalletGateway,
            ApplicationEventPublisher eventPublisher,
            @Value("${donatjs.email.near-target-threshold:0.98}") BigDecimal nearTargetThreshold) {
        this.campaignRepository = campaignRepository;
        this.campaignWalletGateway = campaignWalletGateway;
        this.eventPublisher = eventPublisher;
        this.nearTargetThreshold = nearTargetThreshold;
    }

    @Override
    public Campaign createCampaign(Campaign campaign) {
        return createCampaign(campaign, campaign.getCreatorId());
    }

    @Override
    public Campaign createCampaign(Campaign campaign, String creatorId) {
        if (campaign.getCreatedAt() == null) {
            campaign.setCreatedAt(LocalDateTime.now());
        }
        if (campaign.getTotalRaised() == null) {
            campaign.setTotalRaised(BigDecimal.ZERO);
        }
        campaign.setStatus(CampaignStatus.WAITING);
        campaign.setCreatorId(creatorId);
        Campaign saved = campaignRepository.save(campaign);
        log.info("Campaign {} created by creator '{}' with title '{}'",
                saved.getId(), creatorId, saved.getTitle());
        return saved;
    }

    @Override
    public List<Campaign> findOpenCampaigns() {
        return campaignRepository.findByStatus(CampaignStatus.OPEN);
    }

    @Override
    public List<Campaign> findAllCampaigns() {
        return campaignRepository.findAll();
    }

    @Override
    public Optional<Campaign> findById(Long id) {
        return campaignRepository.findById(id);
    }

    @Override
    public Campaign updateDescription(Long id, String description) {
        return updateDescription(id, null, false, description);
    }

    @Override
    public Campaign updateDescription(Long id, String actorId, boolean isAdmin, String description) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        validateActorPermission(campaign, actorId, isAdmin);
        if (campaign.getStatus() != CampaignStatus.WAITING && campaign.getStatus() != CampaignStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign cannot be edited in current status");
        }
        campaign.setDescription(description);
        Campaign saved = campaignRepository.save(campaign);
        log.info("Campaign {} description updated by actor '{}'", id, actorId != null ? actorId : "admin");
        return saved;
    }

    @Override
    public void deleteIfNoDonations(Long id) {
        deleteIfNoDonations(id, null, false);
    }

    @Override
    public void deleteIfNoDonations(Long id, String actorId, boolean isAdmin) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        validateActorPermission(campaign, actorId, isAdmin);

        if (campaign.getTotalRaised() != null
                && campaign.getTotalRaised().compareTo(BigDecimal.ZERO) > 0) {
            log.warn("Delete rejected for campaign {}: totalRaised={}", id, campaign.getTotalRaised());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete campaign with donations");
        }
        CampaignStatus previous = campaign.getStatus();
        campaign.setStatus(CampaignStatus.DELETED);
        campaignRepository.save(campaign);
        log.info("Campaign {} marked DELETED by actor '{}'", id, actorId != null ? actorId : "admin");
    }

    @Override
    public Campaign moderateCampaign(Long id, boolean approve) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (campaign.getStatus() != CampaignStatus.WAITING && campaign.getStatus() != CampaignStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only WAITING or OPEN campaign can be moderated");
        }
        CampaignStatus previous = campaign.getStatus();
        CampaignStatus next = approve ? CampaignStatus.OPEN : CampaignStatus.REJECTED;
        campaign.setStatus(next);
        Campaign saved = campaignRepository.save(campaign);

        if (!approve) {
            log.info("Campaign {} REJECTED (creator='{}')", id, campaign.getCreatorId());
            eventPublisher.publishEvent(new RejectedCampaignEvent(this, saved, campaign.getCreatorId()));
        } else {
            log.info("Campaign {} APPROVED (now OPEN)", id);
        }

        return saved;
    }

    @Override
    public Campaign adminUpdateCampaign(Long id, String title, LocalDate deadline, BigDecimal targetAmount) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (title != null && !title.isBlank()) {
            campaign.setTitle(title);
        }
        if (deadline != null) {
            if (!deadline.isAfter(LocalDate.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Deadline must be in the future");
            }
            campaign.setDeadline(deadline);
        }
        if (targetAmount != null) {
            if (targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target amount must be positive");
            }
            campaign.setTargetAmount(targetAmount);
            if (campaign.getTotalRaised() != null
                    && campaign.getTotalRaised().compareTo(campaign.getTargetAmount()) >= 0
                    && campaign.getStatus() == CampaignStatus.OPEN) {
                campaign.setStatus(CampaignStatus.CLOSED);
                log.info("Campaign {} auto-closed by admin update: target already reached", id);
            }
        }
        Campaign saved = campaignRepository.save(campaign);
        log.info("Campaign {} updated by admin", id);
        return saved;
    }

    @Override
    public Campaign recordSuccessfulDonation(Long id, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Donation amount must be positive");
        }
        // computeAndSave ensures the read-check-write cycle is atomic, preventing
        // lost-update races when concurrent donations arrive for the same campaign.
        Campaign updated = campaignRepository.computeAndSave(id, campaign -> {
            if (campaign.getStatus() != CampaignStatus.OPEN) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Campaign is not open for donations");
            }
            BigDecimal current = campaign.getTotalRaised() == null ? BigDecimal.ZERO : campaign.getTotalRaised();
            campaign.setTotalRaised(current.add(amount));
            if (campaign.getTargetAmount() != null
                    && campaign.getTotalRaised().compareTo(campaign.getTargetAmount()) >= 0) {
                campaign.setStatus(CampaignStatus.CLOSED);
            }
            return campaign;
        }).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (updated.getStatus() == CampaignStatus.CLOSED) {
            log.info("Campaign {} auto-closed: target reached (raised={}, target={})",
                    id, updated.getTotalRaised(), updated.getTargetAmount());
        } else {
            log.info("Donation of {} recorded for campaign {}. Total raised: {}",
                    amount, id, updated.getTotalRaised());
        }
        return updated;
    }

    @Override
    public Campaign markAsFraud(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (campaign.getStatus() == CampaignStatus.DELETED
                || campaign.getStatus() == CampaignStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign cannot be marked as fraud");
        }

        campaign.setStatus(CampaignStatus.FRAUD);
        Campaign saved = campaignRepository.save(campaign);
        log.warn("Campaign {} marked as FRAUD", id);
        eventPublisher.publishEvent(new CampaignFraudDetectedEvent(this, saved));

        BigDecimal refundAmount = saved.getTotalRaised() == null ? BigDecimal.ZERO : saved.getTotalRaised();
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            campaignWalletGateway.requestRefund(saved);
            eventPublisher.publishEvent(new CampaignRefundRequestedEvent(this, saved, refundAmount));
            log.info("Refund of {} requested for fraudulent campaign {}", refundAmount, id);
        }

        return saved;
    }

    @Override
    public int processExpiredCampaigns(LocalDate today) {
        List<Campaign> allCampaigns = campaignRepository.findAll();
        int processed = 0;

        for (Campaign candidate : allCampaigns) {
            // Quick pre-filter to skip obviously non-expired campaigns before locking.
            if (candidate.getDeadline() == null || candidate.getDeadline().isAfter(today)) continue;
            if (candidate.getStatus() != CampaignStatus.OPEN
                    && candidate.getStatus() != CampaignStatus.WAITING) continue;

            // computeAndSave makes the expiry check + status transition atomic,
            // preventing double-processing if two threads run deadline automation concurrently
            // or if a donation arrives at the exact same time the deadline is processed.
            boolean[] wasProcessed = {false};
            boolean[] isSuccess = {false};
            BigDecimal[] raised = {BigDecimal.ZERO};

            campaignRepository.computeAndSave(candidate.getId(), current -> {
                if (!isExpiredProcessable(current, today)) return current;
                BigDecimal r = current.getTotalRaised() == null ? BigDecimal.ZERO : current.getTotalRaised();
                boolean success = current.getTargetAmount() != null
                        && r.compareTo(current.getTargetAmount()) >= 0;
                current.setStatus(success ? CampaignStatus.CLOSED : CampaignStatus.CANCELLED);
                wasProcessed[0] = true;
                isSuccess[0] = success;
                raised[0] = r;
                return current;
            });

            if (!wasProcessed[0]) continue;

            // Trigger wallet and notification events outside the repository lock.
            if (isSuccess[0]) {
                campaignWalletGateway.requestPayout(candidate);
                eventPublisher.publishEvent(new CampaignPayoutRequestedEvent(this, candidate, raised[0]));
                log.info("Campaign {} closed (target reached). Payout of {} requested.",
                        candidate.getId(), raised[0]);
            } else if (raised[0].compareTo(BigDecimal.ZERO) > 0) {
                campaignWalletGateway.requestRefund(candidate);
                eventPublisher.publishEvent(new CampaignRefundRequestedEvent(this, candidate, raised[0]));
                log.info("Campaign {} cancelled (target not reached). Refund of {} requested.",
                        candidate.getId(), raised[0]);
            } else {
                log.info("Campaign {} cancelled with no donations.", candidate.getId());
            }
            processed++;
        }

        log.info("Deadline automation completed. Processed {} expired campaign(s).", processed);
        return processed;
    }

    private boolean isExpiredProcessable(Campaign campaign, LocalDate today) {
        if (campaign.getDeadline() == null || today == null) return false;
        boolean isExpired = !campaign.getDeadline().isAfter(today);
        boolean canBeFinalized = campaign.getStatus() == CampaignStatus.OPEN
                || campaign.getStatus() == CampaignStatus.WAITING;
        return isExpired && canBeFinalized;
    }

    private void validateActorPermission(Campaign campaign, String actorId, boolean isAdmin) {
        if (isAdmin) return;
        if (actorId == null || actorId.isBlank()) return;
        if (campaign.getCreatorId() == null || !campaign.getCreatorId().equals(actorId)) {
            log.warn("Permission denied: actor '{}' tried to modify campaign {} owned by '{}'",
                    actorId, campaign.getId(), campaign.getCreatorId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to modify this campaign");
        }
    }

    public static class NoopCampaignWalletGateway implements CampaignWalletGateway {
        @Override
        public void requestPayout(Campaign campaign) {}

        @Override
        public void requestRefund(Campaign campaign) {}
    }
}
