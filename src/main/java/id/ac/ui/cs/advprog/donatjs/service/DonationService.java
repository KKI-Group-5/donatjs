package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.CreateDonationRequest;
import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.dto.LeaderboardEntry;
import id.ac.ui.cs.advprog.donatjs.event.RejectedDonationEvent;
import id.ac.ui.cs.advprog.donatjs.exception.InsufficientBalanceException;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.Donation.DonationStatus;
import id.ac.ui.cs.advprog.donatjs.model.Donation.DonationType;
import id.ac.ui.cs.advprog.donatjs.model.Donation.PaymentMethod;
import id.ac.ui.cs.advprog.donatjs.model.Campaign;
import id.ac.ui.cs.advprog.donatjs.model.CampaignStatus;
import id.ac.ui.cs.advprog.donatjs.model.AppUser;
import id.ac.ui.cs.advprog.donatjs.model.PaymentFee;
import id.ac.ui.cs.advprog.donatjs.repository.DonationRepository;
import id.ac.ui.cs.advprog.donatjs.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService {

    private static final long MAX_DONATION_AMOUNT = 5_000_000L;

    private final DonationRepository donationRepository;
    private final CampaignService campaignService;
    private final WalletService walletService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;

    @Transactional
    @SuppressWarnings("null")
    public DonationResponse createDonation(CreateDonationRequest request) {

        Campaign campaign = campaignService.findById(request.getCampaignId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Campaign not found: " + request.getCampaignId()));
        if (campaign.getStatus() != CampaignStatus.OPEN) {
            throw new IllegalStateException(
                    "Campaign " + request.getCampaignId() + " is not open for donations.");
        }

        if (request.getType() == DonationType.SUBSCRIPTION
                && request.getPaymentMethod() != PaymentMethod.WALLET) {
            throw new IllegalArgumentException(
                    "Subscription donations may only use WALLET as the payment method.");
        }

        long fee         = PaymentFee.calculateFee(request.getPaymentMethod());
        long totalAmount = PaymentFee.calculateTotal(request.getAmount(), request.getPaymentMethod());

        DonationStatus status = request.getAmount() > MAX_DONATION_AMOUNT
                ? DonationStatus.REJECTED
                : DonationStatus.SUCCESS;

        // Wallet-funded donations debit the wallet before we commit the donation.
        // Subscription donations have already been debited by the subscription service/scheduler.
        // A failed debit (insufficient funds) surfaces as InsufficientBalanceException
        // to the caller; the donation itself is never persisted in that case.
        if (status == DonationStatus.SUCCESS 
                && request.getPaymentMethod() == PaymentMethod.WALLET 
                && request.getType() != DonationType.SUBSCRIPTION) {
            try {
                walletService.deductForDonation(request.getUserId(), totalAmount, campaign.getTitle());
            } catch (InsufficientBalanceException ex) {
                log.warn("Wallet donation rejected — insufficient balance. userId={}, campaignId={}, required={}",
                        request.getUserId(), request.getCampaignId(), totalAmount);
                throw ex;
            }
        }

        Donation donation = Donation.builder()
                .userId(request.getUserId())
                .campaignId(request.getCampaignId())
                .type(request.getType())
                .amount(request.getAmount())
                .notes(request.getNotes())
                .paymentMethod(request.getPaymentMethod())
                .fee(fee)
                .totalAmount(totalAmount)
                .status(status)
                .build();

        Donation saved = donationRepository.save(donation);

        if (status == DonationStatus.REJECTED) {
            log.warn("Donation REJECTED - exceeds Rp 5,000,000 limit. " +
                            "donationId={}, userId={}, campaignId={}, amount={}",
                    saved.getId(), saved.getUserId(), saved.getCampaignId(), saved.getAmount());
            eventPublisher.publishEvent(new RejectedDonationEvent(this, DonationResponse.from(saved)));
        } else {
            log.info("Donation SUCCESS. donationId={}, userId={}, campaignId={}, amount={}",
                    saved.getId(), saved.getUserId(), saved.getCampaignId(), saved.getAmount());
            try {
                campaignService.recordSuccessfulDonation(
                        saved.getCampaignId(), BigDecimal.valueOf(saved.getAmount()));
            } catch (RuntimeException ex) {
                log.error("Failed to update campaign total for donationId={}: {}",
                        saved.getId(), ex.getMessage());
            }
        }

        return DonationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    @SuppressWarnings("null")
    public DonationResponse getDonationById(Long id) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Donation not found with id: " + id));
        return DonationResponse.from(donation);
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> getDonationsByUser(String userId) {
        return donationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(DonationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Long getTotalDonationsByCampaign(Long campaignId) {
        return donationRepository.sumSuccessfulAmountByCampaignId(campaignId);
    }

    @Transactional(readOnly = true)
    public List<DonationResponse> getSuccessfulDonationsByCampaign(Long campaignId) {
        return donationRepository
                .findByCampaignIdAndStatus(campaignId, DonationStatus.SUCCESS)
                .stream()
                .map(DonationResponse::from)
                .toList();
    }

    @Transactional
    public DonationResponse updateDonationNotes(Long donationId, String userId, String notes) {
        Donation donation = donationRepository.findByIdAndUserId(donationId, userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Donation not found with id: " + donationId + " for user: " + userId));

        donation.setNotes(notes);
        return DonationResponse.from(donationRepository.save(donation));
    }

    @Transactional(readOnly = true)
    public long countRejectedDonationsByUser(String userId) {
        return donationRepository.countByUserIdAndStatus(userId, DonationStatus.REJECTED);
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getCampaignLeaderboard(Long campaignId, int limit) {
        List<Object[]> rows = donationRepository.findTopDonatorsByCampaign(
                campaignId, PageRequest.of(0, limit));
        List<LeaderboardEntry> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Object[] r = rows.get(i);
            String userId = (String) r[0];
            result.add(LeaderboardEntry.builder()
                    .rank(i + 1)
                    .userId(userId)
                    .displayName(resolveDisplayName(userId))
                    .totalAmount((Long) r[1])
                    .donationCount((Long) r[2])
                    .build());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<LeaderboardEntry> getOverallLeaderboard(int limit) {
        List<Object[]> rows = donationRepository.findOverallTopDonators(PageRequest.of(0, limit));
        List<LeaderboardEntry> result = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Object[] r = rows.get(i);
            String userId = (String) r[0];
            result.add(LeaderboardEntry.builder()
                    .rank(i + 1)
                    .userId(userId)
                    .displayName(resolveDisplayName(userId))
                    .totalAmount((Long) r[1])
                    .donationCount((Long) r[2])
                    .campaignCount((Long) r[3])
                    .build());
        }
        return result;
    }

    private String resolveDisplayName(String userId) {
        if (userId == null) return "Anonymous";
        try {
            return userRepository.findById(UUID.fromString(userId))
                    .map(AppUser::getName)
                    .filter(n -> n != null && !n.isBlank())
                    .orElse(userId);
        } catch (IllegalArgumentException e) {
            return userId; // not a valid UUID, show as-is
        }
    }

    @Transactional
    public void processRefundForCampaign(Long campaignId) {
        List<Donation> toRefund = donationRepository
                .findByCampaignIdAndStatus(campaignId, DonationStatus.SUCCESS);

        toRefund.forEach(d -> d.setStatus(DonationStatus.REFUNDED));
        donationRepository.saveAll(toRefund);

        log.info("Processed refund for {} donations on campaign {}", toRefund.size(), campaignId);
    }
}