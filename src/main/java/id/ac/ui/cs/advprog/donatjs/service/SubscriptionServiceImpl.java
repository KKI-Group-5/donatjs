package id.ac.ui.cs.advprog.donatjs.service;

import id.ac.ui.cs.advprog.donatjs.dto.CreateDonationRequest;
import id.ac.ui.cs.advprog.donatjs.dto.CreateSubscriptionRequest;
import id.ac.ui.cs.advprog.donatjs.dto.SubscriptionResponse;
import id.ac.ui.cs.advprog.donatjs.model.Donation;
import id.ac.ui.cs.advprog.donatjs.model.Subscription;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionFrequency;
import id.ac.ui.cs.advprog.donatjs.model.Subscription.SubscriptionStatus;
import id.ac.ui.cs.advprog.donatjs.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final WalletService walletService;
    private final DonationService donationService;

    @Override
    @Transactional
    public SubscriptionResponse createSubscription(CreateSubscriptionRequest request) {
        if (subscriptionRepository.existsByUserIdAndCampaignIdAndStatus(
                request.getUserId(), request.getCampaignId(), SubscriptionStatus.ACTIVE)) {
            throw new IllegalStateException("Active subscription already exists for this campaign");
        }

        // Deduct first payment immediately (throws IllegalStateException if insufficient balance)
        walletService.deductBalance(
                request.getUserId(),
                request.getAmount().doubleValue(),
                "Subscription to campaign: " + request.getCampaignId()
        );

        // Record the first debit as a donation entry
        CreateDonationRequest donationRequest = new CreateDonationRequest();
        donationRequest.setUserId(request.getUserId());
        donationRequest.setCampaignId(request.getCampaignId());
        donationRequest.setAmount(request.getAmount());
        donationRequest.setPaymentMethod(Donation.PaymentMethod.WALLET);
        donationRequest.setType(Donation.DonationType.SUBSCRIPTION);
        donationService.createDonation(donationRequest);

        Subscription subscription = Subscription.builder()
                .userId(request.getUserId())
                .campaignId(request.getCampaignId())
                .amount(request.getAmount())
                .frequency(request.getFrequency())
                .status(SubscriptionStatus.ACTIVE)
                .nextDebitDate(calculateNextDebitDate(request.getFrequency()))
                .build();

        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional
    public SubscriptionResponse cancelSubscription(Long subscriptionId, String userId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found: " + subscriptionId));

        if (!subscription.getUserId().equals(userId)) {
            throw new IllegalStateException("You do not own this subscription");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional
    public SubscriptionResponse updateFrequency(Long subscriptionId, String userId, SubscriptionFrequency frequency) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found: " + subscriptionId));

        if (!subscription.getUserId().equals(userId)) {
            throw new IllegalStateException("You do not own this subscription");
        }

        subscription.setFrequency(frequency);
        subscription.setNextDebitDate(calculateNextDebitDate(frequency));
        return SubscriptionResponse.from(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionResponse> getSubscriptionsByUser(String userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(SubscriptionResponse::from)
                .toList();
    }

    private LocalDate calculateNextDebitDate(SubscriptionFrequency frequency) {
        return switch (frequency) {
            case DAILY   -> LocalDate.now().plusDays(1);
            case WEEKLY  -> LocalDate.now().plusWeeks(1);
            case MONTHLY -> LocalDate.now().plusMonths(1);
        };
    }
}
