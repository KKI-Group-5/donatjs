package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;

import java.util.List;
import java.util.Optional;

public interface CampaignRepository {

    Campaign save(Campaign campaign);

    List<Campaign> findAll();

    Optional<Campaign> findById(Long id);

    List<Campaign> findByStatus(CampaignStatus status);

    void deleteById(Long id);
}

