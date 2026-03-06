package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.SavedCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedCampaignRepository extends JpaRepository<SavedCampaign, Long> {

    List<SavedCampaign> findByUserIdOrderBySavedAtDesc(String userId);

    Optional<SavedCampaign> findByUserIdAndCampaignId(String userId, String campaignId);

    boolean existsByUserIdAndCampaignId(String userId, String campaignId);

    void deleteByUserIdAndCampaignId(String userId, String campaignId);
}
