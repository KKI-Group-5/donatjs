package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.event.CampaignNearTargetEvent;
import id.ac.ui.cs.advprog.donatjs.event.CampaignStatusChangedEvent;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.repository.CampaignRepository;
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

    private final CampaignRepository campaignRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final BigDecimal nearTargetThreshold;

    public SimpleCampaignService(CampaignRepository campaignRepository,
                                 ApplicationEventPublisher eventPublisher,
                                 @Value("${donatjs.email.near-target-threshold:0.98}") BigDecimal nearTargetThreshold) {
        this.campaignRepository = campaignRepository;
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
        // Campaigns are published as OPEN immediately so that contributors can
        // discover and donate to them right after creation. Moderation (approve
        // / reject) still exists for admins via moderateCampaign() on any
        // campaign that needs intervention.
        campaign.setStatus(CampaignStatus.OPEN);
        campaign.setCreatorId(creatorId);
        return campaignRepository.save(campaign);
    }

    @Override
    public List<Campaign> findOpenCampaigns() {
        return campaignRepository.findByStatus(id.ac.ui.cs.advprog.donatjs.model.CampaignStatus.OPEN);
    }

    @Override
    public List<Campaign> findByCreatorId(String creatorId) {
        return campaignRepository.findByCreatorId(creatorId);
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
        return campaignRepository.save(campaign);
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
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot delete campaign with donations"
            );
        }
        CampaignStatus previous = campaign.getStatus();
        campaign.setStatus(CampaignStatus.DELETED);
        campaignRepository.save(campaign);
        eventPublisher.publishEvent(new CampaignStatusChangedEvent(
                this, campaign.getId(), previous, CampaignStatus.DELETED));
    }

    @Override
    public Campaign moderateCampaign(Long id, boolean approve) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (campaign.getStatus() != CampaignStatus.WAITING
                && campaign.getStatus() != CampaignStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign is no longer moderatable");
        }
        CampaignStatus previous = campaign.getStatus();
        CampaignStatus next = approve ? CampaignStatus.OPEN : CampaignStatus.REJECTED;
        campaign.setStatus(next);
        Campaign saved = campaignRepository.save(campaign);
        if (previous != next) {
            eventPublisher.publishEvent(new CampaignStatusChangedEvent(this, saved.getId(), previous, next));
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
            }
        }
        return campaignRepository.save(campaign);
    }

    @Override
    public Campaign recordSuccessfulDonation(Long id, BigDecimal amount) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (campaign.getStatus() != CampaignStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign is not open for donations");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Donation amount must be positive");
        }
        BigDecimal previous = campaign.getTotalRaised() == null ? BigDecimal.ZERO : campaign.getTotalRaised();
        BigDecimal updated  = previous.add(amount);
        campaign.setTotalRaised(updated);

        boolean justCrossedThreshold =
                campaign.getTargetAmount() != null
                && !campaign.isNearTargetNotified()
                && previous.compareTo(thresholdAmount(campaign)) < 0
                && updated.compareTo(thresholdAmount(campaign)) >= 0;

        if (justCrossedThreshold) {
            campaign.setNearTargetNotified(true);
        }

        boolean justClosed = campaign.getTargetAmount() != null
                && updated.compareTo(campaign.getTargetAmount()) >= 0
                && campaign.getStatus() == CampaignStatus.OPEN;
        if (justClosed) {
            campaign.setStatus(CampaignStatus.CLOSED);
        }

        Campaign saved = campaignRepository.save(campaign);

        if (justCrossedThreshold) {
            eventPublisher.publishEvent(new CampaignNearTargetEvent(
                    this,
                    saved.getId(),
                    saved.getTitle(),
                    saved.getTotalRaised(),
                    saved.getTargetAmount()));
        }
        if (justClosed) {
            eventPublisher.publishEvent(new CampaignStatusChangedEvent(
                    this, saved.getId(), CampaignStatus.OPEN, CampaignStatus.CLOSED));
        }
        return saved;
    }

    private BigDecimal thresholdAmount(Campaign campaign) {
        return campaign.getTargetAmount()
                .multiply(nearTargetThreshold)
                .setScale(0, RoundingMode.UP);
    }

    private void validateActorPermission(Campaign campaign, String actorId, boolean isAdmin) {
        if (isAdmin) {
            return;
        }
        if (actorId == null || actorId.isBlank()) {
            return;
        }
        if (campaign.getCreatorId() == null || !campaign.getCreatorId().equals(actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to modify this campaign");
        }
    }
}

