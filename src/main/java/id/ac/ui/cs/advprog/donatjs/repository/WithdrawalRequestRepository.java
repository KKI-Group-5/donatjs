package id.ac.ui.cs.advprog.donatjs.repository;

import id.ac.ui.cs.advprog.donatjs.model.WithdrawalRequest;
import id.ac.ui.cs.advprog.donatjs.model.WithdrawalRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    boolean existsByDonationIdAndStatus(Long donationId, WithdrawalRequestStatus status);

    List<WithdrawalRequest> findByStatusOrderByRequestedAtDesc(WithdrawalRequestStatus status);

    List<WithdrawalRequest> findByUserIdOrderByRequestedAtDesc(String userId);
}
