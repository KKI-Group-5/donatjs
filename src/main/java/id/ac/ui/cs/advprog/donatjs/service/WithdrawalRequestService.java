package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.WithdrawalRequestResponse;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.WithdrawalRequest;
import id.ac.ui.cs.advprog.donatjs.model.WithdrawalRequestStatus;
import id.ac.ui.cs.advprog.donatjs.repository.DonationRepository;
import id.ac.ui.cs.advprog.donatjs.repository.WithdrawalRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalRequestService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final DonationRepository donationRepository;

    @Transactional
    public WithdrawalRequestResponse requestWithdrawal(Long donationId, String userId, String reason) {
        Donation donation = donationRepository.findByIdAndUserId(donationId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Donation not found with id: " + donationId + " for user: " + userId));

        if (donation.getStatus() != Donation.DonationStatus.SUCCESS) {
            throw new IllegalStateException("Only SUCCESS donations can be submitted for withdrawal.");
        }

        if (withdrawalRequestRepository.existsByDonationIdAndStatus(donationId, WithdrawalRequestStatus.PENDING)) {
            throw new IllegalStateException("A withdrawal request for this donation is already pending.");
        }

        WithdrawalRequest request = WithdrawalRequest.builder()
                .donationId(donationId)
                .userId(userId)
                .status(WithdrawalRequestStatus.PENDING)
                .reason(reason)
                .build();

        log.info("Withdrawal request created for donationId={}, userId={}", donationId, userId);
        return WithdrawalRequestResponse.from(withdrawalRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<WithdrawalRequestResponse> getPendingRequests() {
        return withdrawalRequestRepository
                .findByStatusOrderByRequestedAtDesc(WithdrawalRequestStatus.PENDING)
                .stream()
                .map(WithdrawalRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WithdrawalRequestResponse> getRequestsByUser(String userId) {
        return withdrawalRequestRepository
                .findByUserIdOrderByRequestedAtDesc(userId)
                .stream()
                .map(WithdrawalRequestResponse::from)
                .toList();
    }

    @Transactional
    public WithdrawalRequestResponse approveWithdrawal(Long requestId) {
        WithdrawalRequest request = findPendingOrThrow(requestId);

        Donation donation = donationRepository.findById(request.getDonationId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Donation not found: " + request.getDonationId()));
        donation.setStatus(Donation.DonationStatus.REFUNDED);
        donationRepository.save(donation);

        request.setStatus(WithdrawalRequestStatus.APPROVED);
        request.setProcessedAt(LocalDateTime.now());
        log.info("[ADMIN] Withdrawal request {} APPROVED for donationId={}, userId={}",
                requestId, request.getDonationId(), request.getUserId());
        return WithdrawalRequestResponse.from(withdrawalRequestRepository.save(request));
    }

    @Transactional
    public WithdrawalRequestResponse rejectWithdrawal(Long requestId) {
        WithdrawalRequest request = findPendingOrThrow(requestId);

        request.setStatus(WithdrawalRequestStatus.REJECTED);
        request.setProcessedAt(LocalDateTime.now());
        log.info("[ADMIN] Withdrawal request {} REJECTED for donationId={}, userId={}",
                requestId, request.getDonationId(), request.getUserId());
        return WithdrawalRequestResponse.from(withdrawalRequestRepository.save(request));
    }

    private WithdrawalRequest findPendingOrThrow(Long requestId) {
        WithdrawalRequest request = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Withdrawal request not found: " + requestId));
        if (request.getStatus() != WithdrawalRequestStatus.PENDING) {
            throw new IllegalStateException("Only PENDING requests can be processed.");
        }
        return request;
    }
}
