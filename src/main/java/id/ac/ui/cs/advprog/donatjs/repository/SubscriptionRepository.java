package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.Subscription;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Subscription> findByUserIdAndCampaignId(String userId, Long campaignId);

    boolean existsByUserIdAndCampaignIdAndStatus(String userId, Long campaignId, SubscriptionStatus status);

    List<Subscription> findByStatusAndNextDebitDateLessThanEqual(SubscriptionStatus status, LocalDate date);

    List<Subscription> findByCampaignIdAndStatus(Long campaignId, SubscriptionStatus status);
}
