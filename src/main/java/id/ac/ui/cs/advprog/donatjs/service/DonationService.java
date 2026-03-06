package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.CreateDonationRequest;
import id.ac.ui.cs.advprog.donatjs.dto.DonationResponse;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.Donation.DonationStatus;
import id.ac.ui.cs.advprog.donatjs.model.Donation.DonationType;
import id.ac.ui.cs.advprog.donatjs.model.Donation.PaymentMethod;
import id.ac.ui.cs.advprog.donatjs.model.PaymentFee;
import id.ac.ui.cs.advprog.donatjs.repository.DonationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DonationService {

    private static final long MAX_DONATION_AMOUNT = 5_000_000L;

    private final DonationRepository donationRepository;

    @Transactional
    public DonationResponse createDonation(CreateDonationRequest request) {

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
            log.warn("Donation REJECTED — exceeds Rp 5,000,000 limit. " +
                            "donationId={}, userId={}, campaignId={}, amount={}",
                    saved.getId(), saved.getUserId(), saved.getCampaignId(), saved.getAmount());
        } else {
            log.info("Donation SUCCESS. donationId={}, userId={}, campaignId={}, amount={}",
                    saved.getId(), saved.getUserId(), saved.getCampaignId(), saved.getAmount());
        }

        return DonationResponse.from(saved);
    }

    @Transactional(readOnly = true)
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
}