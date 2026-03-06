package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;

import java.util.List;
import java.util.Optional;

public interface CampaignService {

    Campaign createCampaign(Campaign campaign);

    List<Campaign> findOpenCampaigns();

    Optional<Campaign> findById(Long id);

    Campaign updateDescription(Long id, String description);

    void deleteIfNoDonations(Long id);
}

