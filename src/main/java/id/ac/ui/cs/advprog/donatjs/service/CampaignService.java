package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CampaignService {

    Campaign createCampaign(Campaign campaign);
    Campaign createCampaign(Campaign campaign, String creatorId);

    List<Campaign> findOpenCampaigns();

    List<Campaign> findAllCampaigns();

    List<Campaign> findByCreatorId(String creatorId);

    Optional<Campaign> findById(Long id);

    Campaign updateDescription(Long id, String description);
    Campaign updateDescription(Long id, String actorId, boolean isAdmin, String description);

    void deleteIfNoDonations(Long id);
    void deleteIfNoDonations(Long id, String actorId, boolean isAdmin);

    Campaign moderateCampaign(Long id, boolean approve);

    Campaign adminUpdateCampaign(Long id, String title, LocalDate deadline, BigDecimal targetAmount);

    Campaign recordSuccessfulDonation(Long id, BigDecimal amount);

    Campaign markAsFraud(Long id);

    int processExpiredCampaigns(LocalDate today);
}

