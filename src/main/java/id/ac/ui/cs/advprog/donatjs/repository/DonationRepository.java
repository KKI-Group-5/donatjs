package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.Donation.DonationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Donation> findByCampaignIdAndStatus(Long campaignId, DonationStatus status);

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Donation d
            WHERE d.campaignId = :campaignId
              AND d.status = 'SUCCESS'
            """)
    Long sumSuccessfulAmountByCampaignId(@Param("campaignId") Long campaignId);

    Optional<Donation> findByIdAndUserId(Long id, String userId);

    long countByUserIdAndStatus(String userId, DonationStatus status);
}