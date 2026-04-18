package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.Subscription;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Subscription> findByUserIdAndStatus(String userId, Status status);

    Optional<Subscription> findByUserIdAndCampaignIdAndStatus(String userId, Long campaignId, Status status);

    List<Subscription> findByStatusAndNextBillingAtBefore(Status status, LocalDateTime cutoff);

    List<Subscription> findByCampaignId(Long campaignId);
}
