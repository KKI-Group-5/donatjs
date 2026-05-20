package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public interface CampaignRepository {

    Campaign save(Campaign campaign);

    List<Campaign> findAll();

    Optional<Campaign> findById(Long id);

    List<Campaign> findByStatus(CampaignStatus status);

    void deleteById(Long id);

    /**
     * Atomically applies {@code updater} to the stored campaign and persists the result.
     * Prevents lost-update races when multiple threads record donations or run deadline
     * automation concurrently on the same campaign.
     *
     * @return the updated campaign, or empty if no campaign with {@code id} exists
     */
    Optional<Campaign> computeAndSave(Long id, UnaryOperator<Campaign> updater);
}

